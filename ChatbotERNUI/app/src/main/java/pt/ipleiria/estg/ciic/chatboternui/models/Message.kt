package pt.ipleiria.estg.ciic.chatboternui.models

import java.time.LocalDateTime
data class Message(
    var id: Long=0,
    var text: String?=null,
    var time: LocalDateTime? = LocalDateTime.now(),
    var isChatbot: Boolean = false,
    var animate: Boolean = false
)
