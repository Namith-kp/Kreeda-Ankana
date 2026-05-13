package com.kreedaankana.data.model

data class ScoreEntry(
    val scoreId: String = "",
    val winnerTeam: String = "",
    val loserTeam: String = "",
    val score: String = "",
    val sport: String = "",
    val matchDate: String = "",
    val location: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
