package pt.ipleiria.estg.ciic.chatboternui.models

import java.time.LocalDateTime
data class Message(
    var id: Long=0,
    var text: String?=null,
    var client_id: String? =null,
    var accuracy: Double = 0.0,
    var time: LocalDateTime = LocalDateTime.now(),
    var isChatbot: Boolean = false
)
