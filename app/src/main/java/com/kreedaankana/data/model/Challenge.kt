package com.kreedaankana.data.model

data class Challenge(
    val challengeId: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val message: String = "",
    val sport: String = "",
    val proposedDate: String = "",
    val proposedTime: String = "",
    val location: String = "",
    val status: String = "open", // open / accepted / closed
    val acceptedByTeam: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
