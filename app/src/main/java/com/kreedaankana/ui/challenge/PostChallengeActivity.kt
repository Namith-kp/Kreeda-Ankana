package com.kreedaankana.ui.challenge

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kreedaankana.data.model.Challenge
import com.kreedaankana.data.repository.ChallengeRepository
import com.kreedaankana.data.repository.TeamRepository
import com.kreedaankana.databinding.ActivityPostChallengeBinding
import com.kreedaankana.utils.Extensions.SPORTS
import com.kreedaankana.utils.Extensions.TIME_SLOTS
import com.kreedaankana.utils.Extensions.generateAiChallenge
import com.kreedaankana.utils.Extensions.getTodayDate
import com.kreedaankana.utils.Extensions.gone
import com.kreedaankana.utils.Extensions.showToast
import com.kreedaankana.utils.Extensions.visible
import com.kreedaankana.utils.SessionManager
import kotlinx.coroutines.launch
import java.util.*

class PostChallengeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostChallengeBinding
    private val repository = ChallengeRepository()
    private val teamRepo = TeamRepository()
    private var selectedDate = getTodayDate()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostChallengeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupDropdowns()
        setupDatePicker()
        prefillData()
        setupAiGenerator()
        setupPostButton()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Post a Challenge"
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupDropdowns() {
        binding.autoSport.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, SPORTS)
        )
        binding.autoProposedTime.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, TIME_SLOTS)
        )
        binding.tvSelectedDate.text = selectedDate
    }

    private fun setupDatePicker() {
        binding.btnPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                selectedDate = "%04d-%02d-%02d".format(y, m + 1, d)
                binding.tvSelectedDate.text = selectedDate
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun prefillData() {
        SessionManager.getTeam(this)?.let {
            binding.etTeamName.setText(it.teamName)
            binding.autoSport.setText(it.sport, false)
            binding.etLocation.setText(it.location)
        }
        val selectedSport = intent.getStringExtra("SELECTED_SPORT")
        if (!selectedSport.isNullOrEmpty()) {
            binding.autoSport.setText(selectedSport, false)
        }
    }

    private fun setupAiGenerator() {
        binding.btnGenerateAi.setOnClickListener {
            val teamName = binding.etTeamName.text.toString().trim().ifEmpty { "Your Team" }
            val sport = binding.autoSport.text.toString().trim().ifEmpty { "Sport" }
            val time = binding.autoProposedTime.text.toString().trim().ifEmpty { "TBD" }
            val generated = generateAiChallenge(sport, selectedDate, time, teamName)
            binding.etMessage.setText(generated)
            binding.cardAiSuggestion.visible()
            binding.tvAiSuggestion.text = "✨ AI-generated challenge message! Feel free to edit it."
        }

        binding.btnFindOpponent.setOnClickListener {
            val sport = binding.autoSport.text.toString().trim()
            if (sport.isEmpty()) { showToast("Select a sport first"); return@setOnClickListener }
            binding.progressAi.visible()
            lifecycleScope.launch {
                val result = teamRepo.getTeamsBySport(sport)
                binding.progressAi.gone()
                result.onSuccess { teams ->
                    val names = teams.map { it.teamName }
                    val suggestion = com.kreedaankana.utils.Extensions.generateMatchSuggestion(sport, names)
                    binding.cardAiSuggestion.visible()
                    binding.tvAiSuggestion.text = suggestion
                }.onFailure { showToast("AI suggestion failed") }
            }
        }
    }

    private fun setupPostButton() {
        binding.btnPostChallenge.setOnClickListener {
            val teamName = binding.etTeamName.text.toString().trim()
            val message = binding.etMessage.text.toString().trim()
            val sport = binding.autoSport.text.toString().trim()
            val time = binding.autoProposedTime.text.toString().trim()
            val location = binding.etLocation.text.toString().trim()

            if (teamName.isEmpty()) { binding.tilTeamName.error = "Required"; return@setOnClickListener }
            if (message.isEmpty()) { binding.tilMessage.error = "Add a challenge message"; return@setOnClickListener }
            if (sport.isEmpty()) { showToast("Select a sport"); return@setOnClickListener }

            binding.tilTeamName.error = null; binding.tilMessage.error = null

            val challenge = Challenge(
                teamId = SessionManager.getTeam(this)?.id ?: "",
                teamName = teamName,
                message = message,
                sport = sport,
                proposedDate = selectedDate,
                proposedTime = time,
                location = location
            )

            binding.progressBar.visible()
            binding.btnPostChallenge.isEnabled = false
            lifecycleScope.launch {
                val result = repository.postChallenge(challenge)
                binding.progressBar.gone()
                binding.btnPostChallenge.isEnabled = true
                result.onSuccess {
                    showToast("🔥 Challenge Posted!")
                    finish()
                }.onFailure { showToast("Error: ${it.message}") }
            }
        }
    }
}
