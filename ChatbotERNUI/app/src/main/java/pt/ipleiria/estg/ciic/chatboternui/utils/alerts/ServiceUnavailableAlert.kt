package pt.ipleiria.estg.ciic.chatboternui.utils.alerts
import pt.ipleiria.estg.ciic.chatboternui.R

class ServiceUnavailableAlert : IAlert {
    override val icon: Int = R.drawable.error
    override var title: String = "Serviço indisponível"
    override val text: String = "Parece que o serviço está indisponível."
    override val showSupportEmail: Boolean = true
    override val image: Int? = null
    override val confirmButton: IAlertButton = AlertButton(R.drawable.logout,"Sair", null)
    override val dismissButton: IAlertButton? = null
}