package com.kreedaankana.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kreedaankana.data.model.ChatMessage
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val chatCollection = firestore.collection("chats")

    suspend fun sendMessage(challengeId: String, senderTeamName: String, text: String): Result<Unit> {
        return try {
            val messageId = UUID.randomUUID().toString()
            val message = ChatMessage(
                messageId = messageId,
                challengeId = challengeId,
                senderTeamName = senderTeamName,
                text = text,
                timestamp = System.currentTimeMillis()
            )
            chatCollection.document(messageId).set(message).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenForMessages(challengeId: String, onUpdate: (List<ChatMessage>) -> Unit) {
        chatCollection
            .whereEqualTo("challengeId", challengeId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }
                
                val messages = mutableListOf<ChatMessage>()
                snapshot?.documents?.forEach { doc ->
                    doc.toObject(ChatMessage::class.java)?.let { messages.add(it) }
                }
                onUpdate(messages)
            }
    }
}
