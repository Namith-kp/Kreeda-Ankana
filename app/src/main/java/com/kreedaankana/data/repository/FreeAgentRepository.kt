package com.kreedaankana.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kreedaankana.data.model.FreeAgent
import kotlinx.coroutines.tasks.await

class FreeAgentRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val agentsCollection = firestore.collection("free_agents")

    suspend fun registerAsFreeAgent(agent: FreeAgent): Result<Unit> {
        return try {
            val docRef = agentsCollection.document()
            val agentWithId = agent.copy(agentId = docRef.id)
            docRef.set(agentWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFreeAgents(): Result<List<FreeAgent>> {
        return try {
            val snapshot = agentsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get().await()
            Result.success(snapshot.toObjects(FreeAgent::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
