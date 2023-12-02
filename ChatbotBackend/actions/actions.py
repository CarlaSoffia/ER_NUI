# This files contains your custom actions which can be used to run
# custom Python code.
# To run the actions server run in command line: rasa run actions

# Libraries import
from typing import Any, Text, Dict, List
import os
import json
import glob
import pickle
import requests
from tensorflow import keras
from dotenv import load_dotenv
from rasa_sdk import Action, Tracker
from rasa_sdk.executor import CollectingDispatcher
from rasa_sdk import Tracker, FormValidationAction
from keras.utils import pad_sequences
from rasa_sdk.events import SlotSet, ActionExecuted, EventType

# Load initial data
load_dotenv()
models_dir = glob.glob(os.path.join('./SA/', '*.h5'))
models_dir = sorted(models_dir, reverse=True)
MODEL_NAME = models_dir[0]
num_words = 5000
LABELS = ["angry","disgust","fear","guilt","happy","sad","shame"]

# constants
geriatric_questionnaires_valid_short_responses = ["Sim","Não"]
oxford_questionnaires_valid_short_responses = ["Discordo fortemente", "Discordo moderadamente", "Discordo levemente","Concordo levemente", "Concordo moderadamente", "Concordo fortemente"]
GERIATRIC_QUEST = 'GeriatricQuestionnaire'
GERIATRIC_QUEST_ID = 14
OXFORD_QUEST = 'OxfordHappinessQuestionnaire'
OXFORD_QUEST_ID = 28

######################################################### QUESTIONS #########################################################
def request(endpoint):    
    response = requests.request("GET", "http://laravel.test/api/questionnaires/"+endpoint, headers={}, data={})
    data = json.loads(response.text)['data']
    mappings = data['results_mappings']
    questions = data['questions']
    return questions, mappings

def get_question_by_number(questions, number):
    for question in questions:
        if question['number'] == number:
            return question['question']

def get_message_by_mapping(mappings, points):
    message = None
    for mapping in mappings:
        if points < mapping['points_min']:
            continue
        else:
            if mapping['points_max_inclusive']:
                if points <= mapping['points_max']:
                    message = mapping['message']
            else:
                if points < mapping['points_max']:
                    message = mapping['message']
    return message
        
questionsGeriatricQuestionnaire, mappingsGeriatricQuestionnaire = request(GERIATRIC_QUEST)
questionsOxfordHappinessQuestionnaire, mappingsOxfordHappinessQuestionnaire = request(OXFORD_QUEST)
######################################################### SA MODEL #########################################################
def loadModel():
    model = keras.models.load_model(MODEL_NAME)    
    with open('./SA/tokenizer.pickle', 'rb') as handle:
        tokenizer = pickle.load(handle)
    return model, tokenizer    

model, tokenizer  = loadModel()

def predictSentiment(text):
    sentiment={}
    text_sequence = tokenizer.texts_to_sequences([text])
    text_padded = pad_sequences(text_sequence, maxlen=num_words, padding='post', truncating='post')

    predictions = model.predict(text_padded)[0]
    maxVal = predictions.argmax()
    sentiment["accuracy"] =  float("{:.2f}".format(predictions[maxVal]*100))
    sentiment["emotion"] = LABELS[maxVal]
    sentiment["predictions"] = ""
    for idx,pred in enumerate(predictions):
        emotion = LABELS[idx]
        sentiment["predictions"] += emotion + "#" + "{:.2f}".format(pred*100) +";"
    sentiment["predictions"] = sentiment["predictions"][:-1]    
    return json.dumps(sentiment)

######################################################### FUNCTIONS #########################################################
def questionToAskGeriatric(counter):
    if counter % 2 == 0 and counter >= 0 and counter <= 28:
        return (counter // 2) + 1

def questionToAskOxford(counter):
    if counter % 2 == 0 and counter >= 0 and counter <= 56:
        return (counter // 2) + 1

def canPerformSAAnalysis(tracker: Tracker):
    # If the user sent the codes to start the questionnaires, then the input is not analyzed with the SA model  
    if tracker.latest_message['intent']['name'] == "Start_Oxford_Happiness_Questionnaire" or tracker.latest_message['intent']['name'] == "Start_Geriatric_Questionnaire":
        return False
    
    # If the user sent the questionnaires short/predefined answers, then the input is not analyzed with the SA model
    last_bot_event = next(e for e in reversed(tracker.events) if e["event"] == "bot")
    if "utter_action" in last_bot_event["metadata"] and last_bot_event["metadata"]["utter_action"] == "utter_ask_why":
        return False
    
    # If the user is in loop in the same question (because the short answer is wrong), then the input is not analyzed with the SA model
    prev_last_bot_event_text = None
    for e in reversed(tracker.events):
        if prev_last_bot_event_text != None and "text" in e and prev_last_bot_event_text == e["text"]:
            return False
        if prev_last_bot_event_text == None and e["event"] == "bot" and "text" in e:
            prev_last_bot_event_text = e["text"] 
    return True  

def handleQuestion(tracker: Tracker, counter_slot, questionnaire, idxMax):
    responses_questionnaire_counter = int(tracker.get_slot(counter_slot))
    if responses_questionnaire_counter == 0:
        idx = 0
    else:
        if questionnaire == GERIATRIC_QUEST:
            idx = questionToAskGeriatric(responses_questionnaire_counter)
        if questionnaire == OXFORD_QUEST: 
            idx = questionToAskOxford(responses_questionnaire_counter)
        idx = idx-1    
    if idx > idxMax:
        return []
    return idx

def createQuestionnaireResponse(responses_questionnaire_counter, slot_value, questionnaire):
    if questionnaire == GERIATRIC_QUEST and slot_value not in geriatric_questionnaires_valid_short_responses:
        return None, None
    if questionnaire == OXFORD_QUEST and slot_value not in oxford_questionnaires_valid_short_responses:     
        return None, None
    if responses_questionnaire_counter == 0:
        question = 1
    else:
        if questionnaire == GERIATRIC_QUEST:
            question = questionToAskGeriatric(responses_questionnaire_counter)
        if questionnaire == OXFORD_QUEST: 
            question = questionToAskOxford(responses_questionnaire_counter)
    newResponse = {
                    "question": question,
                    "response": slot_value,
                    "is_why": False
                }
    responses_questionnaire_counter = responses_questionnaire_counter + 1
    return json.dumps(newResponse), question

def createQuestionnaireResponseWhy(responses_questionnaire_counter, slot_value, questionnaire):
    if questionnaire == GERIATRIC_QUEST:
        question = questionToAskGeriatric(responses_questionnaire_counter-1)
    if questionnaire == OXFORD_QUEST: 
        question = questionToAskOxford(responses_questionnaire_counter-1)
    newResponse = {
                    "question": question,
                    "response":slot_value,
                    "is_why": True
                }
    return json.dumps(newResponse)

def createPointsMessage(points, questionnaire):
    message = None
    if questionnaire == GERIATRIC_QUEST:
        message = get_message_by_mapping(mappingsGeriatricQuestionnaire, points)
    if questionnaire == OXFORD_QUEST: 
        message = get_message_by_mapping(mappingsOxfordHappinessQuestionnaire, points)
    finalPoints = {
        "points" : points, 
        "message": message
    }
    return json.dumps(finalPoints)

########################################################### ACTIONS ###########################################################

# Action: Responds to the user greeting
class RespondGreeting(Action):
    def name(self) -> Text:
        return "action_respond_greeting"
    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> Dict[Text, Any]:
        dispatcher.utter_message((tracker.latest_message)['text'])

# Action: apply sentiment analysis model to every user input
class CustomActionListen(Action):
    def name(self) -> Text:
        return "action_listen"

    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:
        
        if canPerformSAAnalysis(tracker) == False:
            return [ActionExecuted("action_listen")]

        text = (tracker.latest_message)['text']
        if text == None:
            return [ActionExecuted("action_listen")]
        sentiment = predictSentiment(text)
        dispatcher.utter_message(sentiment)

        return [ActionExecuted("action_listen")]   
   

### Geriatric Depression Questionnaire ------------------------------------------------------------

# Action: Submit the Geriatric Depression Questionnaire 
class ActionSubmitGeriatricQuestionnaire(Action):
    def name(self) -> Text:
        return "submit_geriatric_questionnaire_form"
    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[EventType]:
        return [
            SlotSet("responses_geriatric_questionnaire_counter",0.0),
            SlotSet("geriatric_questionnaire_points",0.0),
            SlotSet("response_question_geriatric_questionnaire",None),
            SlotSet("why_question_geriatric_questionnaire",None)
        ]

# Action: Ask Why for Question
class ActionAskWhyQuestionGeriatricQuestionnaire(Action):
    def name(self) -> Text:
        return "action_ask_geriatric_questionnaire_form_why_question_geriatric_questionnaire"
    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[EventType]:
         dispatcher.utter_message(response="utter_ask_why")
         return []

# Action: Dynamically show questions 
class ActionAskResponseQuestionGeriatricQuestionnaire(Action):
    def name(self) -> Text:
        return "action_ask_geriatric_questionnaire_form_response_question_geriatric_questionnaire"
    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[EventType]:
        idx = handleQuestion(tracker, "responses_geriatric_questionnaire_counter", GERIATRIC_QUEST, GERIATRIC_QUEST_ID)
        if idx == []:
            return []
        dispatcher.utter_message(get_question_by_number(questionsGeriatricQuestionnaire,idx+1))

# Action: Collects the user's response to a questionnaire's question
class ValidateQuestionForm(FormValidationAction):
    def name(self) -> Text:
        return "validate_geriatric_questionnaire_form"
    
    def validate_response_question_geriatric_questionnaire(
        self,
        slot_value: Any,
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain:  Dict[Text, Any],
    ) -> Dict[Text, Any]:
        questionsPointsNo = [1,5,7,11,13]
        responses_geriatric_questionnaire_counter = int(tracker.get_slot("responses_geriatric_questionnaire_counter"))
        newResponse, question = createQuestionnaireResponse(responses_geriatric_questionnaire_counter, slot_value, GERIATRIC_QUEST)
        if newResponse == None:
            return {"response_question_geriatric_questionnaire": None}
        else:
            dispatcher.utter_message(newResponse)
        responses_geriatric_questionnaire_counter = responses_geriatric_questionnaire_counter + 1
        if (slot_value == "Não" and question in questionsPointsNo) or (slot_value == "Sim" and question not in questionsPointsNo):
            return {"response_question_geriatric_questionnaire": slot_value,"responses_geriatric_questionnaire_counter": responses_geriatric_questionnaire_counter, "geriatric_questionnaire_points":tracker.get_slot("geriatric_questionnaire_points")+1.0, "why_question_geriatric_questionnaire": None}
        if (slot_value == "Sim" and question in questionsPointsNo) or (slot_value == "Não" and question not in questionsPointsNo):            
            return {"response_question_geriatric_questionnaire": slot_value,"responses_geriatric_questionnaire_counter": responses_geriatric_questionnaire_counter, "why_question_geriatric_questionnaire": None}

    def validate_why_question_geriatric_questionnaire(
        self,
        slot_value: Any,
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain:  Dict[Text, Any],
    ) -> Dict[Text, Any]:
        responses_geriatric_questionnaire_counter = int(tracker.get_slot("responses_geriatric_questionnaire_counter"))
        newResponse = createQuestionnaireResponseWhy(responses_geriatric_questionnaire_counter, slot_value, GERIATRIC_QUEST)
        dispatcher.utter_message(newResponse)
        responses_geriatric_questionnaire_counter = responses_geriatric_questionnaire_counter + 1
        questionsTotal = GERIATRIC_QUEST_ID+1
        counterTotal = questionsTotal + questionsTotal
        # add this to db and fetch
        if responses_geriatric_questionnaire_counter == counterTotal:
            points = tracker.get_slot("geriatric_questionnaire_points")
            message = createPointsMessage(points, GERIATRIC_QUEST)
            dispatcher.utter_message(message)
            return {"why_question_geriatric_questionnaire": slot_value, "responses_geriatric_questionnaire_counter": responses_geriatric_questionnaire_counter, "response_question_geriatric_questionnaire": ""}
        return {"why_question_geriatric_questionnaire": slot_value, "responses_geriatric_questionnaire_counter": responses_geriatric_questionnaire_counter, "response_question_geriatric_questionnaire": None}

### Oxford Happiness Questionnaire ------------------------------------------------------------

# Action: Ask Why for Question
class ActionAskWhyQuestionOxfordQuestionnaire(Action):
    def name(self) -> Text:
        return "action_ask_oxford_happiness_questionnaire_why_question_oxford_happiness_questionnaire"
    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[EventType]:
         dispatcher.utter_message(response="utter_ask_why")
         return []

# Action: Dynamically show questions 
class ActionAskResponseQuestionOxfordQuestionnaire(Action):
    def name(self) -> Text:
        return "action_ask_oxford_happiness_questionnaire_response_question_oxford_happiness_questionnaire"
    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[EventType]:
        idx = handleQuestion(tracker, "responses_oxford_happiness_questionnaire_counter", OXFORD_QUEST, OXFORD_QUEST_ID)
        if idx == []:
            return []
        dispatcher.utter_message(get_question_by_number(questionsOxfordHappinessQuestionnaire,idx+1))
      

# Action: Collects the user's response to a questionnaire's question
class ValidateOxfordHappinessQuestionnaire(FormValidationAction):
    def name(self) -> Text:
        return "validate_oxford_happiness_questionnaire"
    
    def validate_response_question_oxford_happiness_questionnaire(
        self,
        slot_value: Any,
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain:  Dict[Text, Any],
    ) -> Dict[Text, Any]:
        
        responses_oxford_happiness_questionnaire_counter = int(tracker.get_slot("responses_oxford_happiness_questionnaire_counter"))
        newResponse, question = createQuestionnaireResponse(responses_oxford_happiness_questionnaire_counter, slot_value, OXFORD_QUEST)
        if newResponse == None:
            return {"response_question_oxford_happiness_questionnaire": None}
        else:
            dispatcher.utter_message(newResponse)
    
        questionsReversed = [1, 5, 6, 10, 13, 14, 19, 23, 24, 27, 28, 29]
        replacement = [6, 5, 4, 3, 2, 1]
        points = tracker.get_slot("oxford_happiness_questionnaire_points")
        if question in questionsReversed:
            points = points + replacement[oxford_questionnaires_valid_short_responses.index(slot_value)]
        else:
            points = points + (oxford_questionnaires_valid_short_responses.index(slot_value) + 1)

        responses_oxford_happiness_questionnaire_counter = responses_oxford_happiness_questionnaire_counter + 1
        return {"oxford_happiness_questionnaire_points": points, "response_question_oxford_happiness_questionnaire": slot_value,"responses_oxford_happiness_questionnaire_counter": responses_oxford_happiness_questionnaire_counter, "why_question_oxford_happiness_questionnaire": None}

    def validate_why_question_oxford_happiness_questionnaire(
        self,
        slot_value: Any,
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain:  Dict[Text, Any],
    ) -> Dict[Text, Any]:
        responses_oxford_happiness_questionnaire_counter = int(tracker.get_slot("responses_oxford_happiness_questionnaire_counter"))
        newResponse = createQuestionnaireResponseWhy(responses_oxford_happiness_questionnaire_counter, slot_value, OXFORD_QUEST)
        dispatcher.utter_message(newResponse)        
        responses_oxford_happiness_questionnaire_counter = responses_oxford_happiness_questionnaire_counter + 1
        questionsTotal = OXFORD_QUEST_ID+1
        counterTotal = questionsTotal + questionsTotal
        if responses_oxford_happiness_questionnaire_counter == counterTotal:
            points = tracker.get_slot("oxford_happiness_questionnaire_points") / questionsTotal
            message = createPointsMessage(points, OXFORD_QUEST)
            dispatcher.utter_message(message)
            return {"oxford_happiness_questionnaire_points": points, "why_question_oxford_happiness_questionnaire": slot_value, "responses_oxford_happiness_questionnaire_counter": responses_oxford_happiness_questionnaire_counter, "response_question_oxford_happiness_questionnaire": ""}
        return {"why_question_oxford_happiness_questionnaire": slot_value, "responses_oxford_happiness_questionnaire_counter": responses_oxford_happiness_questionnaire_counter, "response_question_oxford_happiness_questionnaire": None}

    
# Action: Submit the form
class ActionSubmitOxfordHappinessQuestionnaire(Action):
    def name(self) -> Text:
        return "submit_oxford_happiness_questionnaire"
    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[EventType]:
        return [
            SlotSet("responses_oxford_happiness_questionnaire_counter",0.0),
            SlotSet("oxford_happiness_questionnaire_points",0.0),
            SlotSet("response_question_oxford_happiness_questionnaire",None),
            SlotSet("why_question_oxford_happiness_questionnaire",None)
        ]