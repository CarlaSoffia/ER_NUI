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

# Action: Submit the form
class ActionSubmitQuestionForm(Action):
    def name(self) -> Text:
        return "submit_question_form"
    def run(self, dispatcher: CollectingDispatcher,
            tracker: Tracker,
            domain: Dict[Text, Any]) -> List[EventType]:
        print(tracker.get_slot("responses"))
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
            domain: Dict[Text, Any]) -> List[Dict[Text, Any]]:
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
        questionsPointsNo = [1,5,7,11,13]
        responses = tracker.get_slot("responses")
        if responses == None:
            return domain_slots
        
        size = len(responses)-1
        response = responses[size]["response"]
        idx = responses[size]["question"]
        if idx == 0:
            return domain_slots
        if idx==15 and response == "Não":
            return domain_slots
        
        if (response == "Não" and idx in questionsPointsNo) or (response == "Sim" and idx not in questionsPointsNo):
            additional_slots.append("why_question") 
        if (response == "Não" and idx not in questionsPointsNo) or (response == "Sim" and idx in questionsPointsNo):
            additional_slots.append("response_question") 
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
            if question == 15:
                return {"response_question": slot_value,"responses": responses, "why_question": None, "finishedQuestionnaire":True}
            return {"response_question": None,"responses": responses, "why_question": None}

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
        newResponse = {
                         "question": question,
                         "response":response,
                         "why":slot_value
                       }
        responses.append(newResponse)
        if question == 15 and responses[question-1]["response"] == "Sim":
            return {"why_question": slot_value, "responses": responses, "response_question": response, "finishedQuestionnaire":True}
        return {"why_question": slot_value, "responses": responses, "response_question": None}
    
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
