# This files contains your custom actions which can be used to run
# custom Python code.
#
# See this guide on how to implement these action:
# https://rasa.com/docs/rasa/custom-actions


# This is a simple example for a custom action which utters "Hello World!"

from typing import Any, Text, Dict, List
import random

#
from rasa_sdk import Action, Tracker
from rasa_sdk.executor import CollectingDispatcher
from rasa_sdk.events import SlotSet
from rasa_sdk import Tracker, FormValidationAction
from rasa_sdk.events import EventType
#

class AskWhyuestionnaire(Action):
    def name(self) -> Text:
        return "action_ask_question_form_why_question"
    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[EventType]:
         questions = ["Por favor, explique o porquê.", "Detalhe o porquê da sua resposta, por favor."
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
        
        idx = int(tracker.get_slot("questionNumber"))
        dispatcher.utter_message(questions[idx])
        if idx + 1 == 16:
            dispatcher.utter_message("Obrigado por responder ao questionário")
            return []
        else:
            return [SlotSet("questionNumber", idx+1)]
    
# Action: Responds to the user greeting
class RespondGreeting(Action):
    def name(self) -> Text:
        return "action_respond_greeting"
    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:
        dispatcher.utter_message((tracker.latest_message)['text'])

# Action: Collects the user's response to a questionnaire's question
class ValidateQuestionForm(FormValidationAction):
    def name(self) -> Text:
        return "validate_question_form"
    def validate_response_question(
        self,
        slot_value: Any,
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain:  Dict[Text, Any],
    ) -> Dict[Text, Any]:
    
        questionsPointsNo = ["utter_question_1","utter_question_5","utter_question_7","utter_question_11","utter_question_13"]
        intentDeny = ["Não", "não","Nao", "nao","Não.", "não.","Nao.", "nao."] 
        intentAgree = ["Sim.", "sim.","Sim", "sim"]
        lastBotUtterance = ""

        if slot_value not in intentDeny and slot_value not in intentAgree:
            return {"response_question": None}
        
        for event in reversed(tracker.events):
           if event["event"] == "bot":
               try:
                   lastBotUtterance = event["metadata"]["utter_action"]
                   break
               except:
                   pass
        print(tracker.get_slot("depressionQuestionnairePoints"))  
        print("Response to question: "+slot_value)
        if (slot_value in intentDeny and lastBotUtterance in questionsPointsNo) or (slot_value not in intentDeny and lastBotUtterance not in questionsPointsNo):
            value = tracker.get_slot("depressionQuestionnairePoints")+1.0
            SlotSet("depressionQuestionnairePoints", value) 
        return {"response_question": slot_value}
    
    def validate_why_question(
        self,
        slot_value: Any,
        dispatcher: CollectingDispatcher,
        tracker: Tracker,
        domain:  Dict[Text, Any],
    ) -> Dict[Text, Any]:
        print("Answer to why:", slot_value)
        if len(slot_value)>0:
            return {"why_question": slot_value}
        return {"why_question": None}  

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
