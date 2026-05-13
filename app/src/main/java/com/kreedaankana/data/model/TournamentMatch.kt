package com.kreedaankana.data.model

data class TournamentMatch(
    val id: String = "",
    val tournamentId: String = "",
    val roundName: String = "", // "Quarterfinals", "Semifinals", "Finals"
    val team1Id: String = "",
    val team1Name: String = "",
    val team2Id: String = "",
    val team2Name: String = "",
    val score1: Int = 0,
    val score2: Int = 0,
    val winnerId: String = "",
    val winnerName: String = "",
    val nextMatchId: String = "", // Linked node for tree traversal
    val matchOrder: Int = 0, // Positional layout sorting
    val isCompleted: Boolean = false
)
