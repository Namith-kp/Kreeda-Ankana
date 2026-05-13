package com.kreedaankana.ui.scorewall

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kreedaankana.data.repository.LiveScoreRepository
import com.kreedaankana.databinding.ActivityLiveScoreBinding
import kotlinx.coroutines.launch

class LiveScoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLiveScoreBinding
    private val liveScoreRepository = LiveScoreRepository()
    
    private var challengeId: String = ""
    private var team1Name: String = ""
    private var team2Name: String = ""
    private var team1Score: Int = 0
    private var team2Score: Int = 0
    private var isParticipant: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLiveScoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        challengeId = intent.getStringExtra(EXTRA_CHALLENGE_ID) ?: ""
        team1Name = intent.getStringExtra(EXTRA_TEAM1_NAME) ?: "Team 1"
        team2Name = intent.getStringExtra(EXTRA_TEAM2_NAME) ?: "Team 2"
        isParticipant = intent.getBooleanExtra(EXTRA_IS_PARTICIPANT, false)

        binding.tvTeam1Name.text = team1Name
        binding.tvTeam2Name.text = team2Name

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        if (!isParticipant) {
            binding.btnTeam1Minus.isEnabled = false
            binding.btnTeam1Plus.isEnabled = false
            binding.btnTeam2Minus.isEnabled = false
            binding.btnTeam2Plus.isEnabled = false
            binding.btnFinishMatch.visibility = android.view.View.GONE
        }

        setupClickListeners()
        listenForScoreUpdates()
    }

    private fun setupClickListeners() {
        binding.btnTeam1Plus.setOnClickListener { updateScoreLocally(1, 0) }
        binding.btnTeam1Minus.setOnClickListener { updateScoreLocally(-1, 0) }
        binding.btnTeam2Plus.setOnClickListener { updateScoreLocally(0, 1) }
        binding.btnTeam2Minus.setOnClickListener { updateScoreLocally(0, -1) }
        
        binding.btnFinishMatch.setOnClickListener {
            // Ideally, here we would move it to ScoreEntry repository and close the challenge
            Toast.makeText(this, "Match Finished", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun updateScoreLocally(team1Delta: Int, team2Delta: Int) {
        if (team1Score + team1Delta >= 0) team1Score += team1Delta
        if (team2Score + team2Delta >= 0) team2Score += team2Delta
        
        updateScoreUI()
        
        lifecycleScope.launch {
            liveScoreRepository.updateScore(challengeId, team1Name, team2Name, team1Score, team2Score)
        }
    }

    private fun updateScoreUI() {
        binding.tvTeam1Score.text = team1Score.toString()
        binding.tvTeam2Score.text = team2Score.toString()
    }

    private fun listenForScoreUpdates() {
        if (challengeId.isEmpty()) return
        
        liveScoreRepository.listenForLiveScore(challengeId) { liveScore ->
            if (liveScore != null) {
                team1Score = liveScore.team1Score
                team2Score = liveScore.team2Score
                updateScoreUI()
            }
        }
    }

    companion object {
        const val EXTRA_CHALLENGE_ID = "extra_challenge_id"
        const val EXTRA_TEAM1_NAME = "extra_team1_name"
        const val EXTRA_TEAM2_NAME = "extra_team2_name"
        const val EXTRA_IS_PARTICIPANT = "extra_is_participant"
    }
}
