package pt.ipleiria.estg.ciic.chatboternui.utils.alerts

import pt.ipleiria.estg.ciic.chatboternui.R

class ChatbotUnavailableAlert : IAlert {
    override val icon: Int = R.drawable.error
    override var title: String = "MIMO indisponível"
    override val text: String = "O agente conversacional MIMO está indisponível."
    override val image: Int? = null
    override val showSupportEmail: Boolean = true
    override val confirmButton: IAlertButton = AlertButton(R.drawable.logout, "Sair", null)
    override val dismissButton: IAlertButton? = null
}