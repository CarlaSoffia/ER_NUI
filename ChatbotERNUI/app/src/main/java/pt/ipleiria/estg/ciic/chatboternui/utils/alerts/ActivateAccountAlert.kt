package pt.ipleiria.estg.ciic.chatboternui.utils.alerts

import pt.ipleiria.estg.ciic.chatboternui.R

class ActivateAccountAlert :  IAlert {
    override val icon: Int = R.drawable.success
    override var title: String = "Conta criada com sucesso!"
    override val text: String = "Para utilizar o nosso sistema, a sua conta tem de ser activada.\n" +
            "\n" +
            "Ao ativar a sua conta, aceita que os seus dados sejam utilizados para fins académicos e de investigação.\n" +
            "\n" +
            "Gostaria de ativar a sua conta agora?"
    override val showSupportEmail: Boolean = false
    override val image: Int? = null
    override val confirmButton: IAlertButton = AlertButton(R.drawable.check, "Sim", null)
    override val dismissButton: IAlertButton = AlertButton(R.drawable.close, "Não", null)
}