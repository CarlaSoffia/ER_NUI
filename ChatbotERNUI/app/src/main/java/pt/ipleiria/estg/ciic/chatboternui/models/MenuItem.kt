package pt.ipleiria.estg.ciic.chatboternui.models

data class MenuItem(
    val id: String,
    val title: String,
    val icon: Int,
    var onClick: () -> Unit,
    var addDivider: Boolean
)
