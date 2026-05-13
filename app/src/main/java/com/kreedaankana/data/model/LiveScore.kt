package com.kreedaankana.data.model

data class LiveScore(
    val challengeId: String = "",
    val team1Name: String = "",
    val team2Name: String = "",
    val team1Score: Int = 0,
    val team2Score: Int = 0,
    val status: String = "ongoing", // ongoing, completed
    val lastUpdated: Long = System.currentTimeMillis()
)
