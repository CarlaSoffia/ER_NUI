package pt.ipleiria.estg.ciic.chatboternui.utils.alerts

import androidx.activity.ComponentActivity
import pt.ipleiria.estg.ciic.chatboternui.R

class IncorrectCredentialsAlert : IAlert{
    override val icon: Int = R.drawable.warning
    override var title: String = "Credenciais incorretas"
    override val text: String = "As suas credenciais não estão corretas.\n\nVerifique-as e tente iniciar sessão novamente mais tarde."
    override val showSupportEmail: Boolean = false
    override val image: Int? = null
    override val confirmButton: IAlertButton = AlertButton(R.drawable.next, "Repetir", null)
    override val dismissButton: IAlertButton? = null
}