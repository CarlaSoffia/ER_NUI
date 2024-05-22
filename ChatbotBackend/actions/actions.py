# This files contains your custom actions which can be used to run
# custom Python code.
# To run the actions server run in command line: rasa run actions

# Libraries import
from typing import Any, Text, Dict, List
import os
import json
import glob
import deepl
import pickle
import requests
from tensorflow import keras
from dotenv import load_dotenv
from rasa_sdk import Action, Tracker
from rasa_sdk.events import UserUtteranceReverted
from rasa_sdk.executor import CollectingDispatcher
from rasa_sdk import Tracker, FormValidationAction
from tensorflow.keras.preprocessing.sequence import pad_sequences
from rasa_sdk.events import SlotSet, ActionExecuted, EventType
from nltk.tokenize import word_tokenize
from nltk.stem.snowball import SnowballStemmer
from transformers import AutoTokenizer, AutoModelForCausalLM

# Load initial data
load_dotenv()
DEEPL_KEY = os.getenv("DEEPL_KEY")
API_URL = os.getenv("API_URL")
models_dir = glob.glob(os.path.join('./SA/', '*.h5'))
models_dir = sorted(models_dir)
MODEL_NAME = models_dir[0]
num_words = 2500
# NOTE: 
# These are the original model labels ['anger' 'disgust' 'fear' 'guilt' 'joy' 'sadness' 'shame']
# Are mapped by index to the emotions defined in SmartEmotion DB
LABELS = ["angry","disgust","fear","guilt","happy","sad","shame"]

# constants
geriatric_questionnaires_valid_short_responses = ["Sim","Não"]
oxford_questionnaires_valid_short_responses = ["Discordo fortemente", "Discordo moderadamente", "Discordo levemente","Concordo levemente", "Concordo moderadamente", "Concordo fortemente"]
GERIATRIC_QUEST = 'GeriatricQuestionnaire'
GERIATRIC_QUEST_ID = 14
OXFORD_QUEST = 'OxfordHappinessQuestionnaire'
OXFORD_QUEST_ID = 28
NO_ERM = {"ERM": "false"}
######################################################### QUESTIONS #########################################################
def request(endpoint): 
    response = requests.request("GET", API_URL+"/api/questionnaires/"+endpoint, headers={}, data={})
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

##################################################### DEEPL TRANSLATOR #####################################################

def loadDeepLTranslator():
    return deepl.Translator(DEEPL_KEY)

translator = loadDeepLTranslator()

def translateTextDeepL(text):
    result = translator.translate_text(text, target_lang="PT-PT")
    return result.text

######################################################### LLM MODEL #########################################################
# Load tokenizer and model
def loadLLMModel():
    tokenizer = AutoTokenizer.from_pretrained('microsoft/DialoGPT-small', padding_side='left')
    model = AutoModelForCausalLM.from_pretrained('./LLM')
    return model, tokenizer

llmModel, llmTokenizer = loadLLMModel()

# Function to generate response
def generateLLMResponse(user_input, translate = True):
    if translate:
        user_input = translateTextDeepL(user_input)
    # Encode user input
    bot_input_ids = llmTokenizer.encode(user_input + llmTokenizer.eos_token, return_tensors='pt')

    # Generate response
    chat_history_ids = llmModel.generate(
        bot_input_ids, max_length=500,
        pad_token_id=llmTokenizer.eos_token_id,
        no_repeat_ngram_size=3,
        do_sample=True,
        top_k=10,
        top_p=0.7,
        temperature=0.8
    )

    # Decode and return response
    response = llmTokenizer.decode(chat_history_ids[:, bot_input_ids.shape[-1]:][0], skip_special_tokens=True)
    if translate:
            response = translateTextDeepL(response)
    return response

######################################################### SA MODEL #########################################################
def loadModel():
    model = keras.models.load_model(MODEL_NAME)    
    with open('./SA/tokenizerPT.pickle', 'rb') as handle:
        unpickler = pickle.Unpickler(handle)
        tokenizerPT = unpickler.load()
        handle.close()
    with open('./SA/stopwords/portuguese', 'rb') as handle:
        stopwords_pt = set(handle.read().splitlines())    
    stemmer = SnowballStemmer("portuguese")
    return model, tokenizerPT, stopwords_pt, stemmer

model, tokenizerPT, stopwords_pt, stemmer = loadModel()

def preprocess_texts(text_list):
    preprocessed_texts = []
    for text in text_list:
        tokens = word_tokenize(text, language='portuguese')
        filtered_tokens = [word.lower() for word in tokens if word.isalpha() and word.lower() not in stopwords_pt]
        stemmed_tokens = [stemmer.stem(word) for word in filtered_tokens]
        preprocessed_texts.append(stemmed_tokens)
    return preprocessed_texts

def predictSentiment(text):
    preprocessed_quote = preprocess_texts([text])
    tokenized_quote = tokenizerPT.texts_to_sequences(preprocessed_quote)
    padded_quote = pad_sequences(tokenized_quote, maxlen=num_words, padding='post', truncating='post')
    predictions = model.predict(padded_quote)[0]
    maxVal = predictions.argmax()
    sentiment = {
        "accuracy"      :   float("{:.2f}".format(predictions[maxVal]*100)),
        "emotion"       :   LABELS[maxVal],
        "predictions"   :   ""
    }
    for idx,pred in enumerate(predictions):
        emotion = LABELS[idx]
        sentiment["predictions"] += emotion + "#" + "{:.2f}".format(pred*100) +";"
    sentiment["predictions"] = sentiment["predictions"][:-1]    
    return sentiment

######################################################### FUNCTIONS #########################################################
def questionToAskGeriatric(counter):
    if counter % 2 == 0 and counter >= 0 and counter <= 28:
        return (counter // 2) + 1

def questionToAskOxford(counter):
    if counter % 2 == 0 and counter >= 0 and counter <= 56:
        return (counter // 2) + 1
    
def userInMiddleOfQuestionnaires(tracker: Tracker):
    # If the user sent the codes to start the questionnaires, then the input is not analyzed with the SA model  
    if 'metadata' not in tracker.latest_message:
        return False
    if 'questionnaire' in tracker.latest_message['metadata']:
        return True
    return False

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
                    "questionnaire": questionnaire,
                    "question": question,
                    "response": slot_value,
                    "is_why": False
                }
    responses_questionnaire_counter = responses_questionnaire_counter + 1
    return newResponse, question

def createQuestionnaireResponseWhy(responses_questionnaire_counter, slot_value, questionnaire):
    if questionnaire == GERIATRIC_QUEST:
        question = questionToAskGeriatric(responses_questionnaire_counter-1)
    if questionnaire == OXFORD_QUEST: 
        question = questionToAskOxford(responses_questionnaire_counter-1)
    newResponse = {
                    "questionnaire": questionnaire,
                    "question": question,
                    "response":slot_value,
                    "is_why": True
                }
    return newResponse

def createPointsMessage(points, questionnaire):
    message = None
    if questionnaire == GERIATRIC_QUEST:
        message = get_message_by_mapping(mappingsGeriatricQuestionnaire, points)
    if questionnaire == OXFORD_QUEST: 
        message = get_message_by_mapping(mappingsOxfordHappinessQuestionnaire, points)
    finalPoints = {
        "questionnaire": questionnaire,
        "points" : points
    }
    return finalPoints, message

########################################################### ACTIONS ###########################################################

class CustomActionListen(Action):
    def name(self) -> Text:
        return "action_listen"

    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:
        userInQuestionnaire = userInMiddleOfQuestionnaires(tracker)
        if userInQuestionnaire == True:
            return [ActionExecuted("action_listen")]
        text = (tracker.latest_message)['text']
        if text == None:
            return [ActionExecuted("action_listen")]
        dispatcher.utter_message(json_message=predictSentiment(text))
        dispatcher.utter_message(generateLLMResponse(text, False))
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
         dispatcher.utter_message(response="utter_ask_why", json_message=NO_ERM)
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
        dispatcher.utter_message(get_question_by_number(questionsGeriatricQuestionnaire,idx+1), json_message=NO_ERM)

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
            dispatcher.utter_message(json_message=newResponse)
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
        dispatcher.utter_message(json_message=newResponse)
        responses_geriatric_questionnaire_counter = responses_geriatric_questionnaire_counter + 1
        questionsTotal = GERIATRIC_QUEST_ID+1
        counterTotal = questionsTotal + questionsTotal
        # add this to db and fetch
        if responses_geriatric_questionnaire_counter == counterTotal:
            points = tracker.get_slot("geriatric_questionnaire_points")
            json_points, message = createPointsMessage(points, GERIATRIC_QUEST)
            dispatcher.utter_message(message, json_message=json_points)
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
         dispatcher.utter_message(response="utter_ask_why", json_message=NO_ERM)
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
        dispatcher.utter_message(get_question_by_number(questionsOxfordHappinessQuestionnaire,idx+1), json_message=NO_ERM)
      

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
            dispatcher.utter_message(json_message=newResponse)
    
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
        dispatcher.utter_message(json_message=newResponse)        
        responses_oxford_happiness_questionnaire_counter = responses_oxford_happiness_questionnaire_counter + 1
        questionsTotal = OXFORD_QUEST_ID+1
        counterTotal = questionsTotal + questionsTotal
        if responses_oxford_happiness_questionnaire_counter == counterTotal:
            points = tracker.get_slot("oxford_happiness_questionnaire_points") / questionsTotal
            json_points, message = createPointsMessage(points, OXFORD_QUEST)
            dispatcher.utter_message(message, json_message=json_points)
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