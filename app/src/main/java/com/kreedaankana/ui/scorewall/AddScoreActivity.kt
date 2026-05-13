package com.kreedaankana.ui.scorewall

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kreedaankana.data.model.ScoreEntry
import com.kreedaankana.data.repository.ScoreRepository
import com.kreedaankana.databinding.ActivityAddScoreBinding
import com.kreedaankana.utils.Extensions.SPORTS
import com.kreedaankana.utils.Extensions.getTodayDate
import com.kreedaankana.utils.Extensions.gone
import com.kreedaankana.utils.Extensions.showToast
import com.kreedaankana.utils.Extensions.visible
import com.kreedaankana.utils.SessionManager
import kotlinx.coroutines.launch
import java.util.*

class AddScoreActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddScoreBinding
    private val repository = ScoreRepository()
    private var selectedDate = getTodayDate()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddScoreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupDropdown()
        setupDatePicker()
        prefillData()
        setupSubmitButton()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Record Match Result"
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupDropdown() {
        binding.autoSport.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, SPORTS)
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
            binding.etWinnerTeam.setText(it.teamName)
            binding.autoSport.setText(it.sport, false)
            binding.etLocation.setText(it.location)
        }
    }

    private fun setupSubmitButton() {
        binding.btnSubmitScore.setOnClickListener {
            val winner = binding.etWinnerTeam.text.toString().trim()
            val loser = binding.etLoserTeam.text.toString().trim()
            val score = binding.etScore.text.toString().trim()
            val sport = binding.autoSport.text.toString().trim()
            val location = binding.etLocation.text.toString().trim()
            val notes = binding.etNotes.text.toString().trim()

            if (winner.isEmpty()) { binding.tilWinner.error = "Required"; return@setOnClickListener }
            if (loser.isEmpty()) { binding.tilLoser.error = "Required"; return@setOnClickListener }
            if (score.isEmpty()) { binding.tilScore.error = "Required (e.g. 3-1)"; return@setOnClickListener }
            if (sport.isEmpty()) { showToast("Select a sport"); return@setOnClickListener }

            binding.tilWinner.error = null; binding.tilLoser.error = null; binding.tilScore.error = null

            val entry = ScoreEntry(
                winnerTeam = winner,
                loserTeam = loser,
                score = score,
                sport = sport,
                matchDate = selectedDate,
                location = location,
                notes = notes
            )

            binding.progressBar.visible()
            binding.btnSubmitScore.isEnabled = false
            lifecycleScope.launch {
                val result = repository.addScore(entry)
                binding.progressBar.gone()
                binding.btnSubmitScore.isEnabled = true
                result.onSuccess {
                    showToast("🏆 Score Recorded!")
                    finish()
                }.onFailure { showToast("Error: ${it.message}") }
            }
        }
    }
}
