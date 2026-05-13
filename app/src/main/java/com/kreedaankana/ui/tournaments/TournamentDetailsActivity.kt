package com.kreedaankana.ui.tournaments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.kreedaankana.R
import com.kreedaankana.data.model.Tournament
import com.kreedaankana.data.model.TournamentMatch
import com.kreedaankana.data.repository.TournamentRepository
import com.kreedaankana.utils.SessionManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TournamentDetailsActivity : AppCompatActivity() {

    private val repository = TournamentRepository()
    private val auth = FirebaseAuth.getInstance()
    private var tournamentId: String = ""
    private var currentTournament: Tournament? = null

    // Layout views
    private lateinit var toolbar: MaterialToolbar
    private lateinit var btnTabStandings: MaterialButton
    private lateinit var btnTabBrackets: MaterialButton
    private lateinit var scrollStandings: ScrollView
    private lateinit var scrollBrackets: HorizontalScrollView

    // Standings tab views
    private lateinit var tvDetailName: TextView
    private lateinit var tvDetailSport: TextView
    private lateinit var tvDetailStatus: TextView
    private lateinit var rvRegisteredTeams: RecyclerView
    private lateinit var btnRegisterTeam: MaterialButton

    // Brackets tab views
    private lateinit var colQuarterfinals: LinearLayout
    private lateinit var rvQuarterfinals: RecyclerView
    private lateinit var colSemifinals: LinearLayout
    private lateinit var rvSemifinals: RecyclerView
    private lateinit var colFinals: LinearLayout
    private lateinit var rvFinals: RecyclerView

    private lateinit var progressBar: ProgressBar

    // Adapters
    private lateinit var teamsAdapter: RegisteredTeamsAdapter
    private lateinit var quarterAdapter: MatchNodeAdapter
    private lateinit var semiAdapter: MatchNodeAdapter
    private lateinit var finalAdapter: MatchNodeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournament_details)

        tournamentId = intent.getStringExtra("TOURNAMENT_ID") ?: ""
        if (tournamentId.isEmpty()) {
            Toast.makeText(this, "Invalid Tournament ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupToolbar()
        setupTabSwitching()
        setupRecyclerViews()
        loadTournamentDetails()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        btnTabStandings = findViewById(R.id.btnTabStandings)
        btnTabBrackets = findViewById(R.id.btnTabBrackets)
        scrollStandings = findViewById(R.id.scrollStandings)
        scrollBrackets = findViewById(R.id.scrollBrackets)

        tvDetailName = findViewById(R.id.tvDetailName)
        tvDetailSport = findViewById(R.id.tvDetailSport)
        tvDetailStatus = findViewById(R.id.tvDetailStatus)
        rvRegisteredTeams = findViewById(R.id.rvRegisteredTeams)
        btnRegisterTeam = findViewById(R.id.btnRegisterTeam)

        colQuarterfinals = findViewById(R.id.colQuarterfinals)
        rvQuarterfinals = findViewById(R.id.rvQuarterfinals)
        colSemifinals = findViewById(R.id.colSemifinals)
        rvSemifinals = findViewById(R.id.rvSemifinals)
        colFinals = findViewById(R.id.colFinals)
        rvFinals = findViewById(R.id.rvFinals)

        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupTabSwitching() {
        btnTabStandings.setOnClickListener {
            selectTab(isStandings = true)
        }
        btnTabBrackets.setOnClickListener {
            selectTab(isStandings = false)
        }
    }

    private fun selectTab(isStandings: Boolean) {
        if (isStandings) {
            scrollStandings.visibility = View.VISIBLE
            scrollBrackets.visibility = View.GONE

            btnTabStandings.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
            btnTabStandings.setTextColor(ContextCompat.getColor(this, R.color.text_on_primary))

            btnTabBrackets.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            btnTabBrackets.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        } else {
            scrollStandings.visibility = View.GONE
            scrollBrackets.visibility = View.VISIBLE

            btnTabBrackets.setBackgroundColor(ContextCompat.getColor(this, R.color.primary))
            btnTabBrackets.setTextColor(ContextCompat.getColor(this, R.color.text_on_primary))

            btnTabStandings.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            btnTabStandings.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        }
    }

    private fun setupRecyclerViews() {
        // Teams Adapter
        teamsAdapter = RegisteredTeamsAdapter(emptyList())
        rvRegisteredTeams.layoutManager = LinearLayoutManager(this)
        rvRegisteredTeams.adapter = teamsAdapter

        // Brackets Adapters
        quarterAdapter = MatchNodeAdapter(emptyList()) { match -> onMatchNodeClicked(match) }
        rvQuarterfinals.layoutManager = LinearLayoutManager(this)
        rvQuarterfinals.adapter = quarterAdapter

        semiAdapter = MatchNodeAdapter(emptyList()) { match -> onMatchNodeClicked(match) }
        rvSemifinals.layoutManager = LinearLayoutManager(this)
        rvSemifinals.adapter = semiAdapter

        finalAdapter = MatchNodeAdapter(emptyList()) { match -> onMatchNodeClicked(match) }
        rvFinals.layoutManager = LinearLayoutManager(this)
        rvFinals.adapter = finalAdapter
    }

    private fun loadTournamentDetails() {
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            // Load Tournament Metadata
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val snapshot = db.collection("tournaments").document(tournamentId).get().await()
                if (snapshot.exists()) {
                    val tournament = snapshot.toObject(Tournament::class.java)
                    if (tournament != null) {
                        currentTournament = tournament
                        bindTournamentMetadata(tournament)
                        loadTournamentMatches(tournament)
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@TournamentDetailsActivity, "Error loading details: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            progressBar.visibility = View.GONE
        }
    }

    private fun bindTournamentMetadata(tournament: Tournament) {
        tvDetailName.text = tournament.name
        tvDetailSport.text = "${getSportEmoji(tournament.sport)} ${tournament.sport} Tournament"

        val registeredSize = tournament.registeredTeamIds.size
        tvDetailStatus.text = "STATUS: ${tournament.status} ($registeredSize / ${tournament.maxTeams} Teams Enrolled)"

        teamsAdapter.updateData(tournament.registeredTeamNames)

        if (tournament.status == "REGISTRATION") {
            btnRegisterTeam.visibility = View.VISIBLE
            btnRegisterTeam.setOnClickListener { showRegisterTeamDialog() }
        } else {
            btnRegisterTeam.visibility = View.GONE
        }
    }

    private fun loadTournamentMatches(tournament: Tournament) {
        if (tournament.status == "REGISTRATION") {
            // Matches haven't been generated yet
            colQuarterfinals.visibility = View.GONE
            colSemifinals.visibility = View.GONE
            colFinals.visibility = View.GONE
            return
        }

        lifecycleScope.launch {
            repository.getTournamentMatches(tournamentId)
                .onSuccess { matches ->
                    val quarters = matches.filter { it.roundName == "Quarterfinals" }
                    val semis = matches.filter { it.roundName == "Semifinals" }
                    val finals = matches.filter { it.roundName == "Finals" }

                    if (quarters.isNotEmpty()) {
                        colQuarterfinals.visibility = View.VISIBLE
                        quarterAdapter.updateData(quarters)
                    } else {
                        colQuarterfinals.visibility = View.GONE
                    }

                    if (semis.isNotEmpty()) {
                        colSemifinals.visibility = View.VISIBLE
                        semiAdapter.updateData(semis)
                    } else {
                        colSemifinals.visibility = View.GONE
                    }

                    if (finals.isNotEmpty()) {
                        colFinals.visibility = View.VISIBLE
                        finalAdapter.updateData(finals)
                    } else {
                        colFinals.visibility = View.GONE
                    }
                }
                .onFailure { error ->
                    Toast.makeText(this@TournamentDetailsActivity, "Failed matches load: ${error.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showRegisterTeamDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_register_team, null)
        val etTeamName = dialogView.findViewById<TextInputEditText>(R.id.etTeamName)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnJoin = dialogView.findViewById<Button>(R.id.btnJoin)

        // Pre-populate if saved in sharedprefs
        val savedTeam = SessionManager.getTeamName(this)
        if (savedTeam.isNotEmpty()) {
            etTeamName.setText(savedTeam)
        }

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener { alertDialog.dismiss() }

        btnJoin.setOnClickListener {
            val teamName = etTeamName.text?.toString()?.trim() ?: ""
            if (teamName.isEmpty()) {
                etTeamName.error = "Team Name is required!"
                return@setOnClickListener
            }

            alertDialog.dismiss()
            enrollTeam(teamName)
        }

        alertDialog.show()
    }

    private fun enrollTeam(teamName: String) {
        progressBar.visibility = View.VISIBLE
        val teamId = auth.currentUser?.uid ?: ""

        lifecycleScope.launch {
            repository.registerTeamForTournament(tournamentId, teamId, teamName)
                .onSuccess {
                    Toast.makeText(this@TournamentDetailsActivity, "Team enrolled successfully!", Toast.LENGTH_SHORT).show()
                    loadTournamentDetails()
                }
                .onFailure { error ->
                    Toast.makeText(this@TournamentDetailsActivity, "Enrollment failed: ${error.message}", Toast.LENGTH_LONG).show()
                }
            progressBar.visibility = View.GONE
        }
    }

    private fun onMatchNodeClicked(match: TournamentMatch) {
        val tournament = currentTournament ?: return
        val currentUserId = auth.currentUser?.uid ?: ""

        // Check if the current user is the tournament organizer (the creator)
        if (currentUserId != tournament.creatorId) {
            Toast.makeText(this, "Match scores can only be updated by the Host/Organizer!", Toast.LENGTH_SHORT).show()
            return
        }

        // Allow entering score
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_update_score, null)
        val tvLabelTeam1 = dialogView.findViewById<TextView>(R.id.tvLabelTeam1)
        val tvLabelTeam2 = dialogView.findViewById<TextView>(R.id.tvLabelTeam2)
        val etTeam1Score = dialogView.findViewById<TextInputEditText>(R.id.etTeam1Score)
        val etTeam2Score = dialogView.findViewById<TextInputEditText>(R.id.etTeam2Score)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnSubmit = dialogView.findViewById<Button>(R.id.btnSubmit)

        tvLabelTeam1.text = "${match.team1Name} Score"
        tvLabelTeam2.text = "${match.team2Name} Score"

        if (match.isCompleted) {
            etTeam1Score.setText(match.score1.toString())
            etTeam2Score.setText(match.score2.toString())
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSubmit.setOnClickListener {
            val s1Str = etTeam1Score.text?.toString()?.trim() ?: ""
            val s2Str = etTeam2Score.text?.toString()?.trim() ?: ""

            if (s1Str.isEmpty() || s2Str.isEmpty()) {
                Toast.makeText(this, "Please enter scores for both teams!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val s1 = s1Str.toInt()
            val s2 = s2Str.toInt()

            if (s1 == s2) {
                Toast.makeText(this, "Elimination matches cannot end in a draw! Declare a clear winner.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val winner = if (s1 > s2) match.team1Name else match.team2Name
            dialog.dismiss()

            updateScoreInRepository(match, s1, s2, winner)
        }

        dialog.show()
    }

    private fun updateScoreInRepository(match: TournamentMatch, s1: Int, s2: Int, winner: String) {
        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            repository.updateMatchScore(match, s1, s2, winner)
                .onSuccess {
                    Toast.makeText(this@TournamentDetailsActivity, "Score recorded. Winner advanced!", Toast.LENGTH_SHORT).show()
                    loadTournamentDetails()
                }
                .onFailure { error ->
                    Toast.makeText(this@TournamentDetailsActivity, "Error saving score: ${error.message}", Toast.LENGTH_LONG).show()
                }
            progressBar.visibility = View.GONE
        }
    }

    private fun getSportEmoji(sport: String): String {
        return when (sport.lowercase()) {
            "cricket" -> "🏏"
            "football", "soccer" -> "⚽"
            "badminton" -> "🏸"
            "basketball" -> "🏀"
            "tennis" -> "🎾"
            "volleyball" -> "🏐"
            else -> "🏆"
        }
    }
}
