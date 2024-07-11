package pt.ipleiria.estg.ciic.chatboternui.utils.alerts

import pt.ipleiria.estg.ciic.chatboternui.R

class DepressionQuestionnaireAlert: IAlert {
    override val icon: Int = R.drawable.questionnaire
    override var title: String = "Questionário de depressão"
    override val text: String = "Este questionário permite avaliar o seu nível de depressão.\n" +
            "\nContém 30 perguntas: 15 de resposta curta e 15 para justificar as respostas curtas.\n" +
            "\nGostaria de responder ao questionário agora?"
    override val showSupportEmail: Boolean = false
    override val image: Int? = null
    override val confirmButton: IAlertButton = AlertButton(R.drawable.check, "Sim", null)
    override val dismissButton: IAlertButton = AlertButton(R.drawable.close, "Não", null)}