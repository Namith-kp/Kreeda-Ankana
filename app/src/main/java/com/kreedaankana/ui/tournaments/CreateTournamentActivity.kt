package com.kreedaankana.ui.tournaments

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.kreedaankana.R
import com.kreedaankana.data.model.Tournament
import com.kreedaankana.data.repository.TournamentRepository
import com.kreedaankana.utils.SessionManager
import kotlinx.coroutines.launch

class CreateTournamentActivity : AppCompatActivity() {

    private val repository = TournamentRepository()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var etName: TextInputEditText
    private lateinit var etPrizePool: TextInputEditText
    private lateinit var autoSport: AutoCompleteTextView
    private lateinit var autoMaxTeams: AutoCompleteTextView
    private lateinit var btnCreate: MaterialButton
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_tournament)

        initViews()
        setupToolbar()
        setupDropdowns()
        setupListeners()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        etName = findViewById(R.id.etName)
        etPrizePool = findViewById(R.id.etPrizePool)
        autoSport = findViewById(R.id.autoSport)
        autoMaxTeams = findViewById(R.id.autoMaxTeams)
        btnCreate = findViewById(R.id.btnCreate)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupDropdowns() {
        val sports = listOf("Cricket", "Football", "Badminton", "Basketball", "Tennis", "Volleyball")
        val sportAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sports)
        autoSport.setAdapter(sportAdapter)

        val capacities = listOf("4 Teams", "8 Teams")
        val capacityAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, capacities)
        autoMaxTeams.setAdapter(capacityAdapter)
    }

    private fun setupListeners() {
        btnCreate.setOnClickListener {
            publishTournament()
        }
    }

    private fun publishTournament() {
        val name = etName.text?.toString()?.trim() ?: ""
        val sport = autoSport.text?.toString()?.trim() ?: ""
        val maxTeamsStr = autoMaxTeams.text?.toString()?.trim() ?: ""

        if (name.isEmpty()) {
            etName.error = "Tournament name is required!"
            return
        }

        if (sport.isEmpty()) {
            Toast.makeText(this, "Please select a Sport Arena!", Toast.LENGTH_SHORT).show()
            return
        }

        if (maxTeamsStr.isEmpty()) {
            Toast.makeText(this, "Please select bracket capacity size!", Toast.LENGTH_SHORT).show()
            return
        }

        val prizePool = etPrizePool.text?.toString()?.trim() ?: ""

        val maxTeams = if (maxTeamsStr.contains("4")) 4 else 8
        val creatorId = auth.currentUser?.uid ?: ""

        // Extract registered team from Session Manager if available, else standard fallback
        val sessionTeamName = SessionManager.getTeamName(this)
        val hostTeamName = if (sessionTeamName.isNotEmpty()) sessionTeamName else "Elite Sports Club"

        val tournament = Tournament(
            name = name,
            sport = sport,
            maxTeams = maxTeams,
            status = "REGISTRATION",
            creatorId = creatorId,
            creatorTeamName = hostTeamName,
            prizePool = prizePool
        )

        progressBar.visibility = View.VISIBLE
        btnCreate.isEnabled = false

        lifecycleScope.launch {
            repository.createTournament(tournament)
                .onSuccess {
                    Toast.makeText(this@CreateTournamentActivity, "Tournament published successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .onFailure { error ->
                    Toast.makeText(this@CreateTournamentActivity, "Failed: ${error.message}", Toast.LENGTH_LONG).show()
                    progressBar.visibility = View.GONE
                    btnCreate.isEnabled = true
                }
        }
    }
}
