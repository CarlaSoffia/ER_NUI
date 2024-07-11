package pt.ipleiria.estg.ciic.chatboternui.utils.alerts

import pt.ipleiria.estg.ciic.chatboternui.R

class ToggleInputModeAlert : IAlert {
    override val icon: Int? = null
    override var title: String = "Modo de entrada"
    override val showSupportEmail: Boolean = false
    override val text: String = "Tem a certeza que quer alterar o método de entrada?"
    override val image: Int? = null
    override val confirmButton: IAlertButton = AlertButton(R.drawable.check,"Sim", null)
    override val dismissButton: IAlertButton = AlertButton(R.drawable.close,"Não", null)
}