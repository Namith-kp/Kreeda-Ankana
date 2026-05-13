package com.kreedaankana.data.model

data class FreeAgent(
    val agentId: String = "",
    val playerName: String = "",
    val sport: String = "",
    val role: String = "",
    val skillLevel: String = "",
    val availability: String = "",
    val location: String = "",
    val contactInfo: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
