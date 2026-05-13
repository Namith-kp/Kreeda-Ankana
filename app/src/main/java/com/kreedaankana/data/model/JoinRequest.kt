package com.kreedaankana.data.model

data class JoinRequest(
    val id: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val sport: String = "",
    val preferredRole: String = "",
    val preferredNumber: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val timestamp: Long = System.currentTimeMillis()
)
