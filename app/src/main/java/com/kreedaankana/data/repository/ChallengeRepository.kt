package com.kreedaankana.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kreedaankana.data.model.Challenge
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChallengeRepository {

    private val db = FirebaseFirestore.getInstance()
    private val rtdb = FirebaseDatabase.getInstance()
    private val challengesCollection = db.collection("challenges")
    private val challengesRef = rtdb.getReference("challenges")

    suspend fun postChallenge(challenge: Challenge): Result<Challenge> {
        return try {
            val docRef = challengesCollection.document()
            val challengeWithId = challenge.copy(challengeId = docRef.id)
            docRef.set(challengeWithId).await()
            // Also write to RTDB for real-time updates
            challengesRef.child(challengeWithId.challengeId).setValue(challengeWithId).await()
            Result.success(challengeWithId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getChallengesRealtime(): Flow<List<Challenge>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val challenges = mutableListOf<Challenge>()
                for (child in snapshot.children) {
                    child.getValue(Challenge::class.java)?.let { challenges.add(it) }
                }
                challenges.sortByDescending { it.createdAt }
                trySend(challenges)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        challengesRef.addValueEventListener(listener)
        awaitClose { challengesRef.removeEventListener(listener) }
    }

    suspend fun acceptChallenge(challengeId: String, acceptingTeamName: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "status" to "accepted",
                "acceptedByTeam" to acceptingTeamName
            )
            challengesCollection.document(challengeId).update(updates).await()
            challengesRef.child(challengeId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun closeChallenge(challengeId: String): Result<Unit> {
        return try {
            challengesCollection.document(challengeId).update("status", "closed").await()
            challengesRef.child(challengeId).child("status").setValue("closed").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChallengesByTeam(teamId: String): Result<List<Challenge>> {
        return try {
            val snapshot = challengesCollection
                .whereEqualTo("teamId", teamId)
                .get().await()
            Result.success(snapshot.toObjects(Challenge::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteChallenge(challengeId: String): Result<Unit> {
        return try {
            challengesCollection.document(challengeId).delete().await()
            challengesRef.child(challengeId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
