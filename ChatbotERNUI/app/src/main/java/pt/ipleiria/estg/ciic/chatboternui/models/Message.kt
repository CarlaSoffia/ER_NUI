package pt.ipleiria.estg.ciic.chatboternui.models

import java.util.*
data class Message(
    var text: String?=null,
    var client_id: String? =null,
    var accuracy: Double = 0.0,
    var time: Long = Calendar.getInstance().timeInMillis,
    var isChatbot: Boolean = false
)
