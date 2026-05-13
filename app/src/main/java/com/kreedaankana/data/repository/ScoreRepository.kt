package com.kreedaankana.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kreedaankana.data.model.ScoreEntry
import kotlinx.coroutines.tasks.await

class ScoreRepository {

    private val db = FirebaseFirestore.getInstance()
    private val scoresCollection = db.collection("scores")

    suspend fun addScore(entry: ScoreEntry): Result<ScoreEntry> {
        return try {
            val docRef = scoresCollection.document()
            val entryWithId = entry.copy(scoreId = docRef.id)
            docRef.set(entryWithId).await()
            Result.success(entryWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllScores(): Result<List<ScoreEntry>> {
        return try {
            val snapshot = scoresCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            Result.success(snapshot.toObjects(ScoreEntry::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getScoresBySport(sport: String): Result<List<ScoreEntry>> {
        return try {
            val snapshot = scoresCollection
                .whereEqualTo("sport", sport)
                .get().await()
            Result.success(snapshot.toObjects(ScoreEntry::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getScoresByTeam(teamName: String): Result<List<ScoreEntry>> {
        return try {
            val wins = scoresCollection.whereEqualTo("winnerTeam", teamName).get().await()
            val losses = scoresCollection.whereEqualTo("loserTeam", teamName).get().await()
            val all = (wins.toObjects(ScoreEntry::class.java) + losses.toObjects(ScoreEntry::class.java))
                .sortedByDescending { it.createdAt }
            Result.success(all)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
