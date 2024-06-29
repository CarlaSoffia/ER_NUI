package pt.ipleiria.estg.ciic.chatboternui.utils.alerts

 interface IAlertButton {
     val icon: Int
     val title: String
     var onClick: (() -> Unit)?
}