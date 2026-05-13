package com.kreedaankana.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.kreedaankana.data.model.LiveScore
import kotlinx.coroutines.tasks.await

class LiveScoreRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val liveScoreCollection = firestore.collection("live_scores")

    fun listenForLiveScore(challengeId: String, onUpdate: (LiveScore?) -> Unit) {
        liveScoreCollection.document(challengeId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                
                val liveScore = snapshot?.toObject(LiveScore::class.java)
                onUpdate(liveScore)
            }
    }

    suspend fun updateScore(
        challengeId: String,
        team1Name: String,
        team2Name: String,
        team1Score: Int,
        team2Score: Int
    ): Result<Unit> {
        return try {
            val liveScore = LiveScore(
                challengeId = challengeId,
                team1Name = team1Name,
                team2Name = team2Name,
                team1Score = team1Score,
                team2Score = team2Score,
                lastUpdated = System.currentTimeMillis()
            )
            liveScoreCollection.document(challengeId).set(liveScore, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
