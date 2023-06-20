# This files contains your custom actions which can be used to run
# custom Python code.
# To run the actions server run in command line: rasa run actions

# Libraries import
from typing import Any, Text, Dict, List
import random
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
from keras.utils import pad_sequences
from rasa_sdk.events import SlotSet, ActionExecuted, EventType

# Load initial data
load_dotenv()
API_URL = str(os.getenv('API_URL'))
models_dir = glob.glob(os.path.join('./SA/', '*.h5'))
models_dir = sorted(models_dir, reverse=True)
MODEL_NAME = models_dir[0]
num_words = 10000
POS = 0
NEG = 1
NEU = 2
LABELS = ["angry","disgust","fear","guilt","happy","sad","shame"]

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
    iteration = {}
    if str(message).find("[Error]") != -1:
        # Invalid usage_id -> need to create newnteration
        if str(message).find("iteration usage id") != -1:
            iteration, error = requestIteration(token, macAddress, group)  
            # Error in POST iteration
            if iteration == {}:
                return {}, {}, error
            else:
                return requestSpeech(iteration["id"], iteration["usage_id"], accuracy, text, predictions, macAddress, group, token, iteration)
    return iteration, message, {}

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

# Function: Decides the group and idx based on the detected emotion
def decideGroup(emotion):
    if emotion == "happy":
        return "Positive", POS
    elif emotion == "shame":
        return "Neutral", NEU
    else:
        return "Negative", NEG

def questionToAskGeriatric(length):
    if length % 2 == 0 and length >= 0 and length <= 28:
        return (length // 2) + 1

def questionToAskOxford(length):
    if length % 2 == 0 and length >= 0 and length <= 56:
        return (length // 2) + 1
        
def hasGeriatricQuestionnaire(token):
    # Mid questionnaire - responses speeches are  not created here  
    lastDateQuestionnaire = request("GET","geriatricQuestionnaires",token)
    finished_geriatric_questionnaire = True
    if str(lastDateQuestionnaire).find("[Error]") == -1:
        if len(lastDateQuestionnaire) == 0:
            finished_geriatric_questionnaire = False
    else:
        weekPassedSinceLastQuestionnaire = has_week_passed(lastDateQuestionnaire[0]["created_at"])
        # Repeat the questionnaire
        if weekPassedSinceLastQuestionnaire:
            finished_geriatric_questionnaire = False
    return finished_geriatric_questionnaire      

def has_an_hour_passed(date):
    now = datetime.now()
    return now.hour != date.hour
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
        finished_geriatric_questionnaire = tracker.get_slot("finished_geriatric_questionnaire")   
        last_check_hour_geriatric_questionnaire = False 
        #last_check_hour_geriatric_questionnaire = tracker.get_slot("last_check_hour_geriatric_questionnaire")
        #if last_check_hour_geriatric_questionnaire is None:
        #    last_check_hour_geriatric_questionnaire = datetime.now()
        #else:
        #    last_check_hour_geriatric_questionnaire = datetime.strptime(last_check_hour_geriatric_questionnaire, "%Y-%m-%d %H:%M:%S.%f")
        #oneHourPassed = has_an_hour_passed(last_check_hour_geriatric_questionnaire)
        #last_check_hour_geriatric_questionnaire = str(last_check_hour_geriatric_questionnaire)
        #token = (tracker.latest_message)["metadata"]["token"]
        #if oneHourPassed==True:
            #finished_geriatric_questionnaire = hasGeriatricQuestionnaire(token)
        #else:
        #    finished_geriatric_questionnaire = tracker.get_slot("finished_geriatric_questionnaire")        
        geriatric_questionnaire_form = tracker.active_loop.get("name") == "geriatric_questionnaire_form"
        if geriatric_questionnaire_form:
            return [ActionExecuted("action_listen")]  
        text = (tracker.latest_message)['text'] 
        if text == None:
            return [ActionExecuted("action_listen")]
        sentiment = predictSentiment(text)
        macAddress = (tracker.latest_message)["metadata"]["macAddress"]
        iterations = tracker.get_slot("iterations")
        group, idx = decideGroup(sentiment["emotion"])
        #if iterations == None: 
        #    iterations = [{},{},{}] 
        #if iterations[idx] == {}: 
        #    iteration, error = requestIteration(token, macAddress, group)
        #    if iteration == {}:
        #        dispatcher.utter_message(str(error))
        #        return [ActionExecuted("action_listen"),SlotSet("finished_geriatric_questionnaire",finished_geriatric_questionnaire),SlotSet("last_check_hour_geriatric_questionnaire",last_check_hour_geriatric_questionnaire)]
        #    else:
        #        iterations[idx] = iteration    
        #iteration, _, error = requestSpeech(iterations[idx]["id"], iterations[idx]["usage_id"], sentiment["accuracy"], text, sentiment["predictions"], macAddress, group, token)
        #if iteration != {}:
        #    iterations[idx] = iteration
        
        return [ActionExecuted("action_listen"),SlotSet("iterations",iterations),SlotSet("finished_geriatric_questionnaire",finished_geriatric_questionnaire),SlotSet("last_check_hour_geriatric_questionnaire",last_check_hour_geriatric_questionnaire)]   
   

### Geriatric Depression Questionnaire ------------------------------------------------------------

# Action: Submit the Geriatric Depression Questionnaire 
class ActionSubmitGeriatricQuestionnaire(Action):
    def name(self) -> Text:
        return "submit_geriatric_questionnaire_form"
    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[EventType]:
        # send to API
        responses_geriatric_questionnaire = []
        for res in tracker.get_slot("responses_geriatric_questionnaire"):
            res = str(res).replace("'","\"")
            res = str(res).replace("True","true")
            res = str(res).replace("False","false")
            responses_geriatric_questionnaire.append(str(res).replace("'","\""))
        token = (tracker.latest_message)["metadata"]["token"] 
        SlotSet("responses_geriatric_questionnaire",None)
        SlotSet("geriatric_questionnaire_points",0.0)
        SlotSet("finished_geriatric_questionnaire",True)
        SlotSet("response_question_geriatric_questionnaire",None)
        SlotSet("why_question_geriatric_questionnaire",None)
        #message = request("POST","geriatricQuestionnaires",token,json.dumps({"points":tracker.get_slot("geriatric_questionnaire_points"),"responses":responses_geriatric_questionnaire}))
        #if str(message).find("[Error]") == -1:
        #    SlotSet("responses_geriatric_questionnaire",None)
        #    SlotSet("geriatric_questionnaire_points",0.0)
        #    SlotSet("finished_geriatric_questionnaire",True)
        #    SlotSet("response_question_geriatric_questionnaire",None)
        #    SlotSet("why_question_geriatric_questionnaire",None)
        #dispatcher.utter_message(str(message))

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
        questions = ["Sente-se satisfeito(a) com a sua vida?",
                     "Abandonou muitos dos seus interesses e actividades?",
                     "Acha que falta significado na sua vida?",
                     "Costuma se sentir aborrecido(a) com frequência?",
                     "Sente-se de bom humor na maior parte do tempo?",
                     "Tem medo que algo de mau lhe aconteça?",
                     "Na maior parte do tempo, sente-se feliz?",
                     "Sente-se frequentemente abandonado(a)?",
                     "Prefere ficar em casa em vez de sair e experimentar coisas novas?",
                     "Sente que tem mais problemas de memória do que outras pessoas da sua idade?",
                     "Acredita que é maravilhoso estar vivo(a)?",
                     "Recentemente, sentiu-se como inútil ou incapaz?",
                     "No momento, está a sentir-se cheio de energia?",
                     "Sente que perdeu a sua esperança?",
                     "Acha que as outras pessoas estão melhores que si?"]
        responses_geriatric_questionnaire = tracker.get_slot("responses_geriatric_questionnaire")
        if responses_geriatric_questionnaire == None:
            idx = 0
        else:
            idx = questionToAskGeriatric(len(responses_geriatric_questionnaire))-1
        if idx > 14:
            return []
        dispatcher.utter_message(questions[idx])

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
        if slot_value not in ["Sim","Não"]:
            return {"response_question_geriatric_questionnaire": None}
        
        responses_geriatric_questionnaire = tracker.get_slot("responses_geriatric_questionnaire")
        if responses_geriatric_questionnaire == None : 
            responses_geriatric_questionnaire = [] 
            question = 1
        else:
            question = questionToAskGeriatric(len(responses_geriatric_questionnaire))
        questionsPointsNo = [1,5,7,11,13]
        # Create speech --------
        sentiment = predictSentiment(slot_value)
        token = (tracker.latest_message)["metadata"]["token"]
        macAddress = (tracker.latest_message)["metadata"]["macAddress"]
        iterations = tracker.get_slot("iterations")
        group, idx = decideGroup(sentiment["emotion"])
        if iterations == None: 
            iterations = [{},{},{}] 
        #if iterations[idx] == {}: 
        #    iteration, error = requestIteration(token, macAddress, group)
        #    if iteration == {}:
        #        dispatcher.utter_message(error) 
        #    else:
        #       iterations[idx] = iteration           
        #iteration, message, error = requestSpeech(iterations[idx]["id"], iterations[idx]["usage_id"], sentiment["accuracy"], slot_value, sentiment["predictions"], macAddress, group, token)
        #if iteration != {}:
        #    iterations[idx] = iteration
        # -------------------
        newResponse = {
                        "question": question,
                        "response":slot_value,
                        "speech_id":1,#message["id"]
                        "is_why": False
                    }
        responses_geriatric_questionnaire.append(newResponse)
        if (slot_value == "Não" and question in questionsPointsNo) or (slot_value == "Sim" and question not in questionsPointsNo):
            return {"iterations":iterations, "response_question_geriatric_questionnaire": slot_value,"responses_geriatric_questionnaire": responses_geriatric_questionnaire, "geriatric_questionnaire_points":tracker.get_slot("geriatric_questionnaire_points")+1.0, "why_question_geriatric_questionnaire": None}
        if (slot_value == "Sim" and question in questionsPointsNo) or (slot_value == "Não" and question not in questionsPointsNo):            
            return {"iterations":iterations, "response_question_geriatric_questionnaire": slot_value,"responses_geriatric_questionnaire": responses_geriatric_questionnaire, "why_question_geriatric_questionnaire": None}

    def validate_why_question_geriatric_questionnaire(
        self,
        slot_value: Any,
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain:  Dict[Text, Any],
    ) -> Dict[Text, Any]:
        
        # Create speech --------
        sentiment = predictSentiment(slot_value)
        token = (tracker.latest_message)["metadata"]["token"]
        macAddress = (tracker.latest_message)["metadata"]["macAddress"]
        iterations = tracker.get_slot("iterations")
        group, idx = decideGroup(sentiment["emotion"])
        #if iterations == None: 
        #    iterations = [{},{},{}] 
        #if iterations[idx] == {}: 
        #    iteration, error = requestIteration(token, macAddress, group)
        #    if iteration == {}:
        #        dispatcher.utter_message(error)
        #    else:
        #        iterations[idx] = iteration      
        #iteration, message, error = requestSpeech(iterations[idx]["id"], iterations[idx]["usage_id"], sentiment["accuracy"], slot_value, sentiment["predictions"], macAddress, group, token)
        #if iteration != {}:
        #    iterations[idx] = iteration
        # -------------------
        
        responses_geriatric_questionnaire = tracker.get_slot("responses_geriatric_questionnaire")
        last = len(responses_geriatric_questionnaire)-1
        newResponse = {
                        "question": responses_geriatric_questionnaire[last]["question"],
                        "response":slot_value,
                        "speech_id":2,#message["id"]
                        "is_why": True
                    }
        responses_geriatric_questionnaire.append(newResponse)
        if len(responses_geriatric_questionnaire) == 30:
            return {"iterations":iterations, "why_question_geriatric_questionnaire": slot_value, "responses_geriatric_questionnaire": responses_geriatric_questionnaire, "response_question_geriatric_questionnaire": "", "finished_geriatric_questionnaire":True}
        return {"iterations":iterations, "why_question_geriatric_questionnaire": slot_value, "responses_geriatric_questionnaire": responses_geriatric_questionnaire, "response_question_geriatric_questionnaire": None,"finished_geriatric_questionnaire":False}

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
        questions = ["Sente-se insatisfeito(a) com a sua maneira de ser?",
                     "Dedica-se inteiramente aos outros?",
                     "Sente que a vida é muito gratificante?",
                     "Tem sentimentos muito calorosos em relação a quase toda a gente?",
                     "Sente que raramente acorda com a sensação de ter 'carregado as baterias'?",
                     "Acha que é pouco otimista relativamente ao futuro?",
                     "Acredita que facilmente retira prazer das coisas que faz?",
                     "Sente que está sempre comprometido(a) ou envolvido(a)?",
                     "Acredita que a vida é boa?",
                     "Acha que o mundo é um mau sítio?",
                     "Você ri muito?",
                     "Está muito satisfeito(a) com tudo na sua vida?",
                     "Acha que não é atraente?",
                     "Há diferenças entre aquilo que gostava de fazer e o que tem feito?",
                     "É muito feliz?",
                     "Acredita que as coisas à sua volta são encantadoras?",
                     "Tem muita facilidade em animar os outros?",
                     "Acha que se adapta sempre a tudo o que quer?",
                     "Sente que não tem controlo sobre a sua vida?",
                     "Sente-se capaz de enfrentar qualquer desafio?",
                     "Está sempre completamente atento ao que o rodeia?",
                     "Sente regulamente alegria e exaltação?",
                     "Acha díficil tomar decisões?",
                     "Sente que não encontra sentido e significado na sua vida?",
                     "Sente que tem sempre muita energia?",
                     "Tem sempre uma influência positiva nos acontecimentos?",
                     "Acredita que se diverte pouco junto de outras pessoas?",
                     "Acha que está pouco saudável?",
                     "Tem muitas memórias infelizes do seu passado?"
                     ]
        responses_oxford_happiness_questionnaire = tracker.get_slot("responses_oxford_happiness_questionnaire")
        if responses_oxford_happiness_questionnaire == None:
            idx = 0
        else:
            idx = questionToAskOxford(len(responses_oxford_happiness_questionnaire))-1
        if idx > 28:
            return []
        dispatcher.utter_message(questions[idx])
      

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
        questionsReversed = [1, 5, 6, 10, 13, 14, 19, 23, 24, 27, 28, 29]
        mappingLikert = ["Discordo fortemente", "Discordo moderadamente", "Discordo levemente","Concordo levemente", "Concordo moderadamente", "Concordo fortemente"]
        replacement = [6, 5, 4, 3, 2, 1]
        if slot_value not in mappingLikert:
            return {"responses_oxford_happiness_questionnaire": None}
        
        responses_oxford_happiness_questionnaire = tracker.get_slot("responses_oxford_happiness_questionnaire")
        
        if responses_oxford_happiness_questionnaire == None : 
            responses_oxford_happiness_questionnaire = [] 
            question = 1
        else:
            question = questionToAskOxford(len(responses_oxford_happiness_questionnaire))
        points = tracker.get_slot("oxford_happiness_questionnaire_points")
        if question in questionsReversed:
            points = points + replacement[mappingLikert.index(slot_value)]
        else:
            points = points + (mappingLikert.index(slot_value) + 1)
        # Create speech --------
        sentiment = predictSentiment(slot_value)
        token = (tracker.latest_message)["metadata"]["token"]
        macAddress = (tracker.latest_message)["metadata"]["macAddress"]
        iterations = tracker.get_slot("iterations")
        group, idx = decideGroup(sentiment["emotion"])
        if iterations == None: 
            iterations = [{},{},{}] 
        #if iterations[idx] == {}: 
        #    iteration, error = requestIteration(token, macAddress, group)
        #    if iteration == {}:
        #        dispatcher.utter_message(error) 
        #    else:
        #       iterations[idx] = iteration           
        #iteration, message, error = requestSpeech(iterations[idx]["id"], iterations[idx]["usage_id"], sentiment["accuracy"], slot_value, sentiment["predictions"], macAddress, group, token)
        #if iteration != {}:
        #    iterations[idx] = iteration
        # -------------------
        newResponse = {
                        "question": question,
                        "response":slot_value,
                        "speech_id":1,#message["id"]
                        "is_why": False
                    }
        responses_oxford_happiness_questionnaire.append(newResponse)
        return {"iterations":iterations, "oxford_happiness_questionnaire_points": points, "responses_oxford_happiness_questionnaire": slot_value,"responses_oxford_happiness_questionnaire": responses_oxford_happiness_questionnaire, "why_question_oxford_happiness_questionnaire": None}

    def validate_why_question_oxford_happiness_questionnaire(
        self,
        slot_value: Any,
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain:  Dict[Text, Any],
    ) -> Dict[Text, Any]:
        
        # Create speech --------
        sentiment = predictSentiment(slot_value)
        token = (tracker.latest_message)["metadata"]["token"]
        macAddress = (tracker.latest_message)["metadata"]["macAddress"]
        iterations = tracker.get_slot("iterations")
        group, idx = decideGroup(sentiment["emotion"])
        #if iterations == None: 
        #    iterations = [{},{},{}] 
        #if iterations[idx] == {}: 
        #    iteration, error = requestIteration(token, macAddress, group)
        #    if iteration == {}:
        #        dispatcher.utter_message(error)
        #    else:
        #        iterations[idx] = iteration      
        #iteration, message, error = requestSpeech(iterations[idx]["id"], iterations[idx]["usage_id"], sentiment["accuracy"], slot_value, sentiment["predictions"], macAddress, group, token)
        #if iteration != {}:
        #    iterations[idx] = iteration
        # -------------------
        
        responses_oxford_happiness_questionnaire = tracker.get_slot("responses_oxford_happiness_questionnaire")
        last = len(responses_oxford_happiness_questionnaire)-1
        newResponse = {
                        "question": responses_oxford_happiness_questionnaire[last]["question"],
                        "response":slot_value,
                        "speech_id":2,#message["id"]
                        "is_why": True
                    }
        responses_oxford_happiness_questionnaire.append(newResponse)
        if len(responses_oxford_happiness_questionnaire) == 58:
            points = tracker.get_slot("oxford_happiness_questionnaire_points") / 29
            return {"iterations":iterations, "oxford_happiness_questionnaire_points": points, "why_question_oxford_happiness_questionnaire": slot_value, "responses_oxford_happiness_questionnaire": responses_oxford_happiness_questionnaire, "response_question_oxford_happiness_questionnaire": "", "finished_oxford_happiness_questionnaire":True}
        return {"iterations":iterations, "why_question_oxford_happiness_questionnaire": slot_value, "responses_oxford_happiness_questionnaire": responses_oxford_happiness_questionnaire, "response_question_oxford_happiness_questionnaire": None,"finished_oxford_happiness_questionnaire":False}

    
# Action: Submit the form
class ActionSubmitOxfordHappinessQuestionnaire(Action):
    def name(self) -> Text:
        return "submit_oxford_happiness_questionnaire"
    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[EventType]:
        # send to API
        responses_geriatric_questionnaire = []
        for res in tracker.get_slot("responses_geriatric_questionnaire"):
            res = str(res).replace("'","\"")
            res = str(res).replace("True","true")
            res = str(res).replace("False","false")
            responses_geriatric_questionnaire.append(str(res).replace("'","\""))
        token = (tracker.latest_message)["metadata"]["token"] 
        SlotSet("responses_geriatric_questionnaire",None)
        SlotSet("geriatric_questionnaire_points",0.0)
        SlotSet("finished_geriatric_questionnaire",True)
        SlotSet("response_question_geriatric_questionnaire",None)
        SlotSet("why_question_geriatric_questionnaire",None)
        #message = request("POST","geriatricQuestionnaires",token,json.dumps({"points":tracker.get_slot("geriatric_questionnaire_points"),"responses":responses_geriatric_questionnaire}))
        #if str(message).find("[Error]") == -1:
        #    SlotSet("responses_geriatric_questionnaire",None)
        #    SlotSet("geriatric_questionnaire_points",0.0)
        #    SlotSet("finished_geriatric_questionnaire",True)
        #    SlotSet("response_question_geriatric_questionnaire",None)
        #    SlotSet("why_question_geriatric_questionnaire",None)
        #dispatcher.utter_message(str(message))