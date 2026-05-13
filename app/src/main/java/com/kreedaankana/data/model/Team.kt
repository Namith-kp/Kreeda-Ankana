package com.kreedaankana.data.model

data class Player(
    val name: String = "",
    val role: String = "",
    val number: String = ""
)

data class Team(
    val id: String = "",
    val teamName: String = "",
    val captainId: String = "",
    val captain: String = "",
    val captainPhone: String = "",
    val sport: String = "",
    val location: String = "",
    val preferredTime: String = "",
    val players: List<Player> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)
