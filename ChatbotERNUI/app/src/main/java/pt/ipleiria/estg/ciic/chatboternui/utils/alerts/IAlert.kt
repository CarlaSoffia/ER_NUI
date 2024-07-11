package pt.ipleiria.estg.ciic.chatboternui.utils.alerts

import pt.ipleiria.estg.ciic.chatboternui.utils.IBaseActivity

interface IAlert {
    val icon: Int?
    var title: String
    val text: String
    val image: Int?
    val showSupportEmail: Boolean
    val confirmButton : IAlertButton
    val dismissButton : IAlertButton?
}