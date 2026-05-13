package com.kreedaankana.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.kreedaankana.data.model.Team

object SessionManager {
    private const val PREF_NAME = "kreeda_session"
    private const val KEY_TEAM = "registered_team"
    private const val KEY_TEAM_NAME = "team_name"

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveTeam(context: Context, team: Team) {
        getPrefs(context).edit()
            .putString(KEY_TEAM, Gson().toJson(team))
            .putString(KEY_TEAM_NAME, team.teamName)
            .apply()
    }

    fun getTeam(context: Context): Team? {
        val json = getPrefs(context).getString(KEY_TEAM, null) ?: return null
        return try { Gson().fromJson(json, Team::class.java) } catch (e: Exception) { null }
    }

    fun getTeamName(context: Context): String =
        getPrefs(context).getString(KEY_TEAM_NAME, "") ?: ""

    fun isTeamRegistered(context: Context): Boolean =
        getPrefs(context).contains(KEY_TEAM)

    fun clearSession(context: Context) =
        getPrefs(context).edit().clear().apply()

    fun clearTeam(context: Context) {
        getPrefs(context).edit()
            .remove(KEY_TEAM)
            .remove(KEY_TEAM_NAME)
            .apply()
    }
}
