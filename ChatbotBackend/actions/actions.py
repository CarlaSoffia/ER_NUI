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

######################################################### FUNCTIONS #########################################################


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
    return json.dumps(sentiment)

def questionToAskGeriatric(length):
    if length % 2 == 0 and length >= 0 and length <= 28:
        return (length // 2) + 1

def questionToAskOxford(length):
    if length % 2 == 0 and length >= 0 and length <= 56:
        return (length // 2) + 1
    

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
        # Input is the code to start forms
        if tracker.latest_message['intent']['name'] == "Start_Oxford_Happiness_Questionnaire" or tracker.latest_message['intent']['name'] == "Start_Geriatric_Questionnaire":
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
        # send to API
        responses_geriatric_questionnaire = []
        for res in tracker.get_slot("responses_geriatric_questionnaire"):
            res = str(res).replace("'","\"")
            res = str(res).replace("True","true")
            res = str(res).replace("False","false")
            responses_geriatric_questionnaire.append(str(res).replace("'","\""))
        SlotSet("responses_geriatric_questionnaire",None)
        SlotSet("geriatric_questionnaire_points",0.0)
        SlotSet("finished_geriatric_questionnaire",True)
        SlotSet("response_question_geriatric_questionnaire",None)
        SlotSet("why_question_geriatric_questionnaire",None)

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
        newResponse = {
                        "question": question,
                        "response":slot_value,
                        "is_why": False
                    }
        responses_geriatric_questionnaire.append(newResponse)
        dispatcher.utter_message(json.dumps(newResponse))
        if (slot_value == "Não" and question in questionsPointsNo) or (slot_value == "Sim" and question not in questionsPointsNo):
            return {"response_question_geriatric_questionnaire": slot_value,"responses_geriatric_questionnaire": responses_geriatric_questionnaire, "geriatric_questionnaire_points":tracker.get_slot("geriatric_questionnaire_points")+1.0, "why_question_geriatric_questionnaire": None}
        if (slot_value == "Sim" and question in questionsPointsNo) or (slot_value == "Não" and question not in questionsPointsNo):            
            return {"response_question_geriatric_questionnaire": slot_value,"responses_geriatric_questionnaire": responses_geriatric_questionnaire, "why_question_geriatric_questionnaire": None}

    def validate_why_question_geriatric_questionnaire(
        self,
        slot_value: Any,
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain:  Dict[Text, Any],
    ) -> Dict[Text, Any]:
        
        # Create speech --------     
        responses_geriatric_questionnaire = tracker.get_slot("responses_geriatric_questionnaire")
        last = len(responses_geriatric_questionnaire)-1
        newResponse = {
                        "question": responses_geriatric_questionnaire[last]["question"],
                        "response":slot_value,
                        "is_why": True
                    }
        responses_geriatric_questionnaire.append(newResponse)
        dispatcher.utter_message(json.dumps(newResponse))
        if len(responses_geriatric_questionnaire) == 30:
            points = tracker.get_slot("geriatric_questionnaire_points")
            if points > 0 and points <= 5:
                message = "Baseado nas suas respostas que forneceu não apresenta sinais de depressão."
            elif points >= 6 and points <= 10: 
                message = "Com base nas respostas que forneceu apresenta apenas leves sinais de depressão."
            else:
                message = "Tendo em conta as suas respostas apresenta sintomas de depressão graves. Comunique com a sua família e considere marcar uma consulta com o seu médico de família."    
            
            finalPoints = {
                "points" : points, 
                "message": message
            }
            dispatcher.utter_message(json.dumps(finalPoints))

            return {"why_question_geriatric_questionnaire": slot_value, "responses_geriatric_questionnaire": responses_geriatric_questionnaire, "response_question_geriatric_questionnaire": "", "finished_geriatric_questionnaire":True}
        return {"why_question_geriatric_questionnaire": slot_value, "responses_geriatric_questionnaire": responses_geriatric_questionnaire, "response_question_geriatric_questionnaire": None,"finished_geriatric_questionnaire":False}

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
            print(responses_oxford_happiness_questionnaire)
            question = questionToAskOxford(len(responses_oxford_happiness_questionnaire))
        points = tracker.get_slot("oxford_happiness_questionnaire_points")
        if question in questionsReversed:
            points = points + replacement[mappingLikert.index(slot_value)]
        else:
            points = points + (mappingLikert.index(slot_value) + 1)
        # Create speech --------
        newResponse = {
                        "question": question,
                        "response":slot_value,
                        "is_why": False
                    }
        responses_oxford_happiness_questionnaire.append(newResponse)
        dispatcher.utter_message(json.dumps(newResponse))
        return {"oxford_happiness_questionnaire_points": points, "response_question_oxford_happiness_questionnaire": slot_value,"responses_oxford_happiness_questionnaire": responses_oxford_happiness_questionnaire, "why_question_oxford_happiness_questionnaire": None}

    def validate_why_question_oxford_happiness_questionnaire(
        self,
        slot_value: Any,
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain:  Dict[Text, Any],
    ) -> Dict[Text, Any]:
        
        # Create speech --------
        responses_oxford_happiness_questionnaire = tracker.get_slot("responses_oxford_happiness_questionnaire")
        last = len(responses_oxford_happiness_questionnaire)-1
        newResponse = {
                        "question": responses_oxford_happiness_questionnaire[last]["question"],
                        "response":slot_value,
                        "is_why": True
                    }
        responses_oxford_happiness_questionnaire.append(newResponse)
        dispatcher.utter_message(json.dumps(newResponse))
        if len(responses_oxford_happiness_questionnaire) == 58:
            points = tracker.get_slot("oxford_happiness_questionnaire_points") / 29
            if points >= 1 and points < 2:
                message = "Baseado nas suas respostas sente-se triste e provavelmente está a ver-se a si e à sua situação atual pior do que realmente é. Aconselho que comunique os seus sentimentos e medos à sua família e caso haja necessidade marque uma consulta junto do seu médico."
            elif points >= 2 and points < 3: 
                message = "Tendo em conta as suas respostas está um pouco triste. Tente conversar com a sua família e fazer alguma atividade que o deixe feliz."
            elif points >= 3 and points < 4: 
                message = "Considerando as respostas que forneceu demonstra sentimentos de neutralidade." 
            elif points >= 4 and points < 5: 
                message = "Baseado nas respostas fornecidas está feliz. Sorria e pensamentos positivos!" 
            else: 
                message = "Parabéns! Com base nas suas respostas está muito feliz!"

            finalPoints = {
                "points" : points,
                "message": message
            }
            dispatcher.utter_message(json.dumps(finalPoints))
            return {"oxford_happiness_questionnaire_points": points, "why_question_oxford_happiness_questionnaire": slot_value, "responses_oxford_happiness_questionnaire": responses_oxford_happiness_questionnaire, "response_question_oxford_happiness_questionnaire": "", "finished_oxford_happiness_questionnaire":True}
        return {"why_question_oxford_happiness_questionnaire": slot_value, "responses_oxford_happiness_questionnaire": responses_oxford_happiness_questionnaire, "response_question_oxford_happiness_questionnaire": None,"finished_oxford_happiness_questionnaire":False}

    
# Action: Submit the form
class ActionSubmitOxfordHappinessQuestionnaire(Action):
    def name(self) -> Text:
        return "submit_oxford_happiness_questionnaire"
    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[EventType]:
        # send to API
        responses_oxford_happiness_questionnaire = []
        for res in tracker.get_slot("responses_oxford_happiness_questionnaire"):
            res = str(res).replace("'","\"")
            res = str(res).replace("True","true")
            res = str(res).replace("False","false")
            responses_oxford_happiness_questionnaire.append(str(res).replace("'","\""))
        SlotSet("responses_oxford_happiness_questionnaire",None)
        SlotSet("oxford_happiness_questionnaire_points",0.0)
        SlotSet("finished_oxford_happiness_questionnaire",True)
        SlotSet("response_question_oxford_happiness_questionnaire",None)
        SlotSet("why_question_oxford_happiness_questionnaire",None)