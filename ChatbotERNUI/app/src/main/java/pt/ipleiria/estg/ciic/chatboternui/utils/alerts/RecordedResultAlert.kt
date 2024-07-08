package pt.ipleiria.estg.ciic.chatboternui.utils.alerts

import pt.ipleiria.estg.ciic.chatboternui.R

class RecordedResultAlert : IAlert {
    override val icon: Int? = null
    override val title: String = "Mensagem"
    override val showSupportEmail: Boolean = false
    override val text: String = ""
    override val image: Int? = null
    override val confirmButton: IAlertButton = AlertButton(R.drawable.send,"Enviar", null)
    override val dismissButton: IAlertButton = AlertButton(R.drawable.close,"Sair", null)
}