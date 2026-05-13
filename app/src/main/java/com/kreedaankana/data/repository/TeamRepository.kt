package com.kreedaankana.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kreedaankana.data.model.Team
import kotlinx.coroutines.tasks.await

class TeamRepository {

    private val db = FirebaseFirestore.getInstance()
    private val teamsCollection = db.collection("teams")

    suspend fun isTeamNameTaken(teamName: String, sport: String, excludeTeamId: String? = null): Boolean {
        return try {
            val snapshot = teamsCollection
                .whereEqualTo("teamName", teamName)
                .whereEqualTo("sport", sport)
                .get().await()
            for (document in snapshot.documents) {
                if (excludeTeamId == null || document.id != excludeTeamId) {
                    return true
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun registerTeam(team: Team): Result<Team> {
        return try {
            val docRef = if (team.id.isEmpty()) teamsCollection.document() else teamsCollection.document(team.id)
            val teamWithId = if (team.id.isEmpty()) team.copy(id = docRef.id) else team
            docRef.set(teamWithId).await()
            Result.success(teamWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTeam(team: Team): Result<Unit> {
        return try {
            if (team.id.isEmpty()) return Result.failure(Exception("Team ID cannot be empty for update"))
            teamsCollection.document(team.id).set(team).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTeam(teamId: String): Result<Unit> {
        return try {
            if (teamId.isEmpty()) return Result.failure(Exception("Team ID cannot be empty for delete"))
            teamsCollection.document(teamId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllTeams(): Result<List<Team>> {
        return try {
            val snapshot = teamsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            val teams = snapshot.toObjects(Team::class.java)
            Result.success(teams)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTeamById(teamId: String): Result<Team?> {
        return try {
            val doc = teamsCollection.document(teamId).get().await()
            val team = doc.toObject(Team::class.java)
            Result.success(team)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTeamsBySport(sport: String): Result<List<Team>> {
        return try {
            val snapshot = teamsCollection
                .whereEqualTo("sport", sport)
                .get().await()
            val teams = snapshot.toObjects(Team::class.java)
            Result.success(teams)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
