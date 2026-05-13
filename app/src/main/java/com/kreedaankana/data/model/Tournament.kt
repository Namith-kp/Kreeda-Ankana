package com.kreedaankana.data.model

data class Tournament(
    val id: String = "",
    val name: String = "",
    val sport: String = "",
    val maxTeams: Int = 8, // Support 4, 8, or 16 for perfect bracket sizing
    val status: String = "REGISTRATION", // REGISTRATION, ACTIVE, COMPLETED
    val creatorId: String = "",
    val creatorTeamName: String = "",
    val registeredTeamIds: List<String> = emptyList(),
    val registeredTeamNames: List<String> = emptyList(),
    val winnerTeamName: String = "",
    val prizePool: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
