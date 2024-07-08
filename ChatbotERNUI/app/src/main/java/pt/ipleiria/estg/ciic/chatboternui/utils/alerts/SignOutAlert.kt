package pt.ipleiria.estg.ciic.chatboternui.utils.alerts

import pt.ipleiria.estg.ciic.chatboternui.R

class SignOutAlert : IAlert {
    override val icon: Int? = null
    override val title: String = "Temos pena de o ver ir embora..."
    override val showSupportEmail: Boolean = false
    override val text: String = "Tem a certeza de que pretende terminar a sessão?"
    override val image: Int = R.drawable.session_background
    override val confirmButton: IAlertButton = AlertButton(R.drawable.logout,"Sim", null)
    override val dismissButton: IAlertButton = AlertButton(R.drawable.close,"Não", null)
}