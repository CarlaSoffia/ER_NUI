# This files contains your custom actions which can be used to run
# custom Python code.
#
# See this guide on how to implement these action:
# https://rasa.com/docs/rasa/custom-actions
# https://www.postman.com/bold-firefly-183322/workspace/tcc-chatbot-rasa/request/4600719-1b3df0b1-4730-487e-827e-d13497c8c41d

# This is a simple example for a custom action which utters "Hello World!"

from typing import Any, Text, Dict, List
import random

#
import os
import json
import time
import glob
import pickle
import requests
from tensorflow import keras
from datetime import datetime
from dotenv import load_dotenv
from rasa_sdk import Action, Tracker
from rasa_sdk.executor import CollectingDispatcher
from rasa_sdk import Tracker, FormValidationAction
from keras.preprocessing.sequence import pad_sequences
from rasa_sdk.events import SlotSet, ActionExecuted, EventType


load_dotenv()
API_URL = str(os.getenv('API_URL'))
models_dir = glob.glob(os.path.join('./SA/', '*.h5'))
models_dir = sorted(models_dir, reverse=True)
MODEL_NAME = models_dir[0]
num_words = 10000
POS = 0
NEG = 1
NEU = 2
LABELS = ["angry","disgust","fear","guilt","happy","sad","shame"]# some labels changed but position remains -> anger, disgust, fear, guilt, joy, sadness, shame

######################################################### FUNCTIONS #########################################################
# Function: Sends request to LARAVEL API
def request(type, url, token, data=None, ContentJson=True):
    if token == None:
        return "[Error] - No token"   
    if data != None and ContentJson:
        headers = {"Authorization": "Bearer " + str(token), "Accept": "application/json", 'Content-Type': 'application/json'}
        r = requests.request(type, API_URL+url, headers=headers, data=data)
    elif ContentJson==False:
        headers = {"Authorization": "Bearer " + str(token), "Accept": "application/json"}
        r = requests.request(type, API_URL+url, headers=headers, data=data)
    else:
        headers = {"Authorization": "Bearer " + str(token), "Accept": "application/json"}
        r = requests.request(type, API_URL+url, headers=headers)   
    if r.status_code == 403 or r.status_code == 401 or r.status_code == 422:
        return "[Error]["+str(r.status_code)+"] - "  + str(r.json()["message"])
    return r.json()["data"]

# Function: Handles iteration    
def requestIteration(token, macAddress, group):
    message = request("POST","iterations",token,json.dumps({'macAddress': macAddress,'emotion':group,"type":'best'}))
    if str(message).find("[Error]") == -1:
         return {"id":message["id"],"usage_id":message["usage_id"]},""
    return {},message

# Function: Handles speech
def requestSpeech(id, usage_id, accuracy, text, predictions, macAddress, group, token, iteration={}):
    date = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    payload = {'iteration_id':str(id), 'iteration_usage_id':str(usage_id), 'datesSpeeches[0]': str(date), 'accuraciesSpeeches[0]':str(accuracy), 'textsSpeeches[0]': text, 'preditionsSpeeches[0]':predictions}
    # Send speech to LARAVEL API
    message = request("POST","speeches",token,payload,False)  
    if str(message).find("[Error]") != -1:
        # Invalid usage_id -> need to create newnteration
        if str(message).find("iteration usage id") != -1:
            iteration, error = requestIteration(token, macAddress, group)  
            # Error in POST iteration
            if iteration == {}:
                return error, {}
            else:
                return requestSpeech(iteration["id"], iteration["usage_id"], accuracy, text, predictions, macAddress, group, token, iteration)
    return message, iteration

# Function: Verifies if a week has passed from a given epoch time
def has_week_passed(epoch_time):
    now = time.time()
    if (now - epoch_time) >= 604800:
        return True
    else:
        return False

# Function: Predicts emotion based on text
def predictSentiment(text):
    sentiment={}
    model = keras.models.load_model(MODEL_NAME)    
    with open('./SA/tokenizer.pickle', 'rb') as handle:
        tokenizer = pickle.load(handle)
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
    return sentiment

########################################################### ATIONS ###########################################################

# Action: Verify if questionnaire must be filled again - 1 week interval 
class ActionSessionStart(Action):
    def name(self) -> Text:
        return "action_session_start"
    
    async def run(self, dispatcher: CollectingDispatcher,
                  tracker: Tracker,
                  domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:

        # Your action code goes here
        # the session should begin with a `session_started` event
        token = (tracker.events[0]["value"]["token"])
        if token != None: 
            lastDateQuestionnaire = request("GET","geriatricQuestionnaires",token)
            if str(lastDateQuestionnaire).find("[Error]") == -1:
                if len(lastDateQuestionnaire) == 0:
                    return [SlotSet("finishedQuestionnaire",False),ActionExecuted("action_listen")]
                else:
                    weekPassedSinceLastQuestionnaire = has_week_passed(lastDateQuestionnaire[0]["created_at"])
                    if weekPassedSinceLastQuestionnaire:
                        return [SlotSet("finishedQuestionnaire",False),ActionExecuted("action_listen")]         
        return [ActionExecuted("action_listen")]
    
# Action: Submit the form
class ActionSubmitQuestionForm(Action):
    def name(self) -> Text:
        return "submit_question_form"
    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[EventType]:
        # send to API
        responses = []
        for res in tracker.get_slot("responses"):
            responses.append(str(res).replace("'","\""))
        token = (tracker.latest_message)["metadata"]["token"] 
        message = request("POST","geriatricQuestionnaires",token,json.dumps({"points":tracker.get_slot("depressionQuestionnairePoints"),"responses":responses}))
        if str(message).find("[Error]") == -1:
            SlotSet("responses",None)
            SlotSet("depressionQuestionnairePoints",0.0)
            SlotSet("finishedQuestionnaire",False)
            SlotSet("response_question",None)
            SlotSet("why_question",None)
        dispatcher.utter_message(str(message))
        
# Action: Ask Why for Question
class ActionAskWhyQuestion(Action):
    def name(self) -> Text:
        return "action_ask_why_question"
    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[EventType]:
         questions = ["Por favor, explique o porquê.", "Detalhe o porquê da sua resposta, por favor.",
         "Por favor, explique a sua resposta.",  "Explique-me o motivo, por favor."]
         idx = random.randint(0,len(questions)-1)
         dispatcher.utter_message(questions[idx])
         return []

# Action: Dynamically show questions 
class AskQuestionQuestionnaire(Action):
    def name(self) -> Text:
        return "action_ask_question_form_response_question"
    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[EventType]:
        
        questions = ["Sente-se satisfeito com a sua vida?",
                     "Abandonou muitos dos seus interesses e actividades?",
                     "Acha que falta significado na sua vida?",
                     "Costuma se sentir aborrecido com frequência?",
                     "Sente-se de bom humor na maior parte do tempo?",
                     "Tem medo que algo de mau lhe aconteça?",
                     "Na maior parte do tempo, sente-se feliz?",
                     "Sente-se frequentemente abandonado?",
                     "Prefere ficar em casa em vez de sair e experimentar coisas novas?",
                     "Sente que tem mais problemas de memória do que outras pessoas da sua idade?",
                     "Acredita que é maravilhoso estar vivo?",
                     "Recentemente, sentiu-se como inútil ou incapaz?",
                     "No momento, está a sentir-se cheio de energia?",
                     "Sente que perdeu a sua esperança?",
                     "Acha que as outras pessoas estão melhores que si?"]
        responses = tracker.get_slot("responses")
        if responses == None:
            idx = 0
        else:
            idx = len(responses)
        if idx > 14:
            return []
        dispatcher.utter_message(questions[idx])

# Action: Responds to the user greeting
class RespondGreeting(Action):
    def name(self) -> Text:
        return "action_respond_greeting"
    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> Dict[Text, Any]:
        dispatcher.utter_message((tracker.latest_message)['text'])
        
# Action: Collects the user's response to a questionnaire's question
class ValidateQuestionForm(FormValidationAction):
    def name(self) -> Text:
        return "validate_question_form"
    
    async def required_slots(
        self,
        domain_slots: List[Text],
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain:  Dict[Text, Any],
    ) -> List[Text]:
        additional_slots = ["response_question"]
        responses = tracker.get_slot("responses")
        if responses == None:
            return domain_slots
        
        size = len(responses)-1
        response = responses[size]["response"]
        idx = responses[size]["question"]
        if idx == 0:
            return domain_slots
        additional_slots.append("why_question")
        return additional_slots + domain_slots
    
    def validate_response_question(
        self,
        slot_value: Any,
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain:  Dict[Text, Any],
    ) -> Dict[Text, Any]:
        responses = tracker.get_slot("responses")
        if responses == None : 
            responses = [] 
            question = 1
        else:
            question = len(responses)+1
        questionsPointsNo = [1,5,7,11,13]
        
        newResponse = {
                        "question": question,
                        "response":slot_value,
                        "why":""
                    }
        responses.append(newResponse)
        if (slot_value == "Não" and question in questionsPointsNo) or (slot_value == "Sim" and question not in questionsPointsNo):
            return {"response_question": slot_value,"responses": responses, "depressionQuestionnairePoints":tracker.get_slot("depressionQuestionnairePoints")+1.0, "why_question": None}
        if (slot_value == "Sim" and question in questionsPointsNo) or (slot_value == "Não" and question not in questionsPointsNo):            
            return {"response_question": slot_value,"responses": responses, "why_question": None}

    def validate_why_question(
        self,
        slot_value: Any,
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain:  Dict[Text, Any],
    ) -> Dict[Text, Any]:
        question = len(tracker.get_slot("responses"))
        responses = tracker.get_slot("responses")
        response = responses[question-1]["response"]            
        responses[question-1]["why"] = slot_value
        if question == 15:
            return {"why_question": slot_value, "responses": responses, "response_question": response, "finishedQuestionnaire":True}
        return {"why_question": slot_value, "responses": responses, "response_question": None,"finishedQuestionnaire":True}
    
# Action: retrieve entities from text    
class ActionRetrieveEntities(Action):
    def name(self) -> Text:
        return "action_retrieve_entities"

    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:
        entities = tracker.latest_message.get("entities", [])
        for entity in entities:
            dispatcher.utter_message(f"Entity: {entity['entity']}, Value: {entity['value']}")
        return []

# Action: apply sentiment analysis model to every user input
class CustomActionListen(Action):
    def name(self) -> Text:
        return "action_listen"

    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:
        text = (tracker.latest_message)['text']
        if text == None:
            return [ActionExecuted("action_listen")]
        sentiment = predictSentiment(text)

        token = (tracker.latest_message)["metadata"]["token"]
        macAddress = (tracker.latest_message)["metadata"]["macAddress"]
        iterations = tracker.get_slot("iterations")
        if sentiment["emotion"] == "happy":
            group = "Positive"
            idx = POS
        elif sentiment["emotion"] == "shame":
            group = "Neutral"
            idx = NEU
        else:
            group = "Negative"
            idx = NEG
        if iterations == None: 
            iterations = [{},{},{}] 
        if iterations[idx] == {}: 
            iterations[idx], error = requestIteration(token, macAddress, group)
            if iterations[idx] == {}:
                dispatcher.utter_message(error)
                return [ActionExecuted("action_listen")]
        id = iterations[idx]["id"]
        usage_id = iterations[idx]["usage_id"]
        message, iteration = requestSpeech(id, usage_id, sentiment["accuracy"], text, sentiment["predictions"], macAddress, group, token)
        if iteration != {}:
            iterations[idx] = iteration
        return [SlotSet("iterations",iterations),ActionExecuted("action_listen")]   