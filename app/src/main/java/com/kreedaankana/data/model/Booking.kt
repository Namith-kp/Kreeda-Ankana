package com.kreedaankana.data.model

data class Booking(
    val bookingId: String = "",
    val teamId: String = "",
    val teamName: String = "",
    val sport: String = "",
    val venuePlace: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
