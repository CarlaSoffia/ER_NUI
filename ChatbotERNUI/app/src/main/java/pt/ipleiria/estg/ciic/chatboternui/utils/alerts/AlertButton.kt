package pt.ipleiria.estg.ciic.chatboternui.utils.alerts

class AlertButton(
    override val icon: Int,
    override val title: String,
    override var onClick: (() -> Unit)?
) : IAlertButton {
}