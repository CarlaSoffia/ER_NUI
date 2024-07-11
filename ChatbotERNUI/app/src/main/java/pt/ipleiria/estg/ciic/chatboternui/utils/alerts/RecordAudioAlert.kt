package pt.ipleiria.estg.ciic.chatboternui.utils.alerts

import pt.ipleiria.estg.ciic.chatboternui.R

class RecordAudioAlert : IAlert {
    override val icon: Int? = null
    override var title: String = "A gravar o áudio..."
    override val showSupportEmail: Boolean = false
    override val text: String = ""
    override val image: Int? = null
    override val confirmButton: IAlertButton = AlertButton(R.drawable.stop_button,"Parar", null)
    override val dismissButton: IAlertButton = AlertButton(R.drawable.close,"Sair", null)
}