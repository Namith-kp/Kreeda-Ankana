package com.kreedaankana.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.view.View
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.*

object Extensions {

    fun Context.showToast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    fun View.showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) =
        Snackbar.make(this, message, duration).show()

    fun View.visible() { visibility = View.VISIBLE }
    fun View.gone() { visibility = View.GONE }
    fun View.invisible() { visibility = View.INVISIBLE }

    fun Context.isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun getTodayDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    fun formatDisplayDate(dateStr: String): String {
        return try {
            val inputSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputSdf = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
            val date = inputSdf.parse(dateStr) ?: return dateStr
            outputSdf.format(date)
        } catch (e: Exception) { dateStr }
    }

    fun formatTimeRange(start: String, end: String): String = "$start – $end"

    val SPORTS = listOf("Volleyball", "Cricket", "Football", "Kabaddi", "Kho-Kho", "Other")

    val TIME_SLOTS = listOf(
        "06:00", "07:00", "08:00", "09:00", "10:00", "11:00",
        "12:00", "13:00", "14:00", "15:00", "16:00", "17:00",
        "18:00", "19:00", "20:00", "21:00"
    )

    fun generateAiChallenge(sport: String, date: String, time: String, teamName: String): String {
        val messages = listOf(
            "⚡ $teamName is fired up and ready for a $sport showdown on $date at $time! Any brave team willing to step up? 🏆",
            "🔥 Calling all $sport warriors! $teamName challenges any team for a friendly match on $date at $time. Accept if you dare! 💪",
            "🏅 $teamName has reserved the ground for $sport on $date at $time. Looking for a worthy opponent. Who's in? 🎯",
            "⚔️ $teamName vs ???  $sport battle on $date at $time. Don't miss out on the action! 🌟",
            "🚀 $teamName is on the ground for $sport on $date starting $time. Reply to accept this challenge! 🏟️"
        )
        return messages.random()
    }

    fun generateMatchSuggestion(sport: String, teams: List<String>): String {
        if (teams.isEmpty()) return "No teams found for $sport yet. Be the first!"
        val opponent = teams.random()
        return "🤖 AI Suggestion: Challenge '$opponent' for $sport! They have a similar profile and are likely available."
    }
}
