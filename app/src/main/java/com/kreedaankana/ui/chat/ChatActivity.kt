package com.kreedaankana.ui.chat

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kreedaankana.utils.SessionManager
import com.kreedaankana.data.repository.ChatRepository
import com.kreedaankana.databinding.ActivityChatBinding
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val chatRepository = ChatRepository()
    
    
    private lateinit var chatAdapter: ChatAdapter
    private var challengeId: String = ""
    private var currentTeamName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        challengeId = intent.getStringExtra(EXTRA_CHALLENGE_ID) ?: ""
        val opponentTeamName = intent.getStringExtra(EXTRA_OPPONENT_NAME) ?: "Chat"

        binding.toolbar.title = opponentTeamName
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        currentTeamName = SessionManager.getTeamName(this)
        setupRecyclerView()
        listenForMessages()

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                sendMessage(text)
            }
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(currentTeamName)
        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.layoutManager = layoutManager
        binding.rvMessages.adapter = chatAdapter
    }

    private fun listenForMessages() {
        if (challengeId.isEmpty()) return
        
        chatRepository.listenForMessages(challengeId) { messages ->
            chatAdapter.submitList(messages) {
                if (messages.isNotEmpty()) {
                    binding.rvMessages.smoothScrollToPosition(messages.size - 1)
                }
            }
        }
    }

    private fun sendMessage(text: String) {
        if (challengeId.isEmpty() || currentTeamName.isEmpty()) return
        
        binding.etMessage.text.clear()
        
        lifecycleScope.launch {
            chatRepository.sendMessage(challengeId, currentTeamName, text).onFailure {
                Toast.makeText(this@ChatActivity, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val EXTRA_CHALLENGE_ID = "extra_challenge_id"
        const val EXTRA_OPPONENT_NAME = "extra_opponent_name"
    }
}
