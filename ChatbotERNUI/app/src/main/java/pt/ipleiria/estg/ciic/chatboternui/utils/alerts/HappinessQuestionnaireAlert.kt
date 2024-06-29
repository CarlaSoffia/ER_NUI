package pt.ipleiria.estg.ciic.chatboternui.utils.alerts

import pt.ipleiria.estg.ciic.chatboternui.R

class HappinessQuestionnaireAlert: IAlert {
    override val icon: Int = R.drawable.questionnaire
    override val title: String = "Questionário de felicidade"
    override val text: String = "Este questionário permite avaliar o seu nível de felicidade.\n" +
            "\n" +
            "Contém 58 perguntas: 29 de resposta curta e 29 para justificar as respostas curtas.\n" +
            "\n" +
            "Gostaria de responder ao questionário agora?"
    override val showSupportEmail: Boolean = false
    override val image: Int? = null
    override val confirmButton: IAlertButton = AlertButton(R.drawable.check, "Sim", null)
    override val dismissButton: IAlertButton = AlertButton(R.drawable.close, "Não", null)
}