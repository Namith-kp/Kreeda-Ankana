package com.kreedaankana.data.model

data class ChatMessage(
    val messageId: String = "",
    val challengeId: String = "",
    val senderTeamName: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)
