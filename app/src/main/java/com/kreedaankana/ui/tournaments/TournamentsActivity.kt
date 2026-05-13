package com.kreedaankana.ui.tournaments

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.kreedaankana.R
import com.kreedaankana.data.model.Tournament
import com.kreedaankana.data.repository.TournamentRepository
import kotlinx.coroutines.launch

class TournamentsActivity : AppCompatActivity() {

    private val repository = TournamentRepository()
    private lateinit var adapter: TournamentsAdapter
    private var allTournaments: List<Tournament> = emptyList()

    private lateinit var toolbar: MaterialToolbar
    private lateinit var chipGroup: ChipGroup
    private lateinit var rvTournaments: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var fabCreateTournament: ExtendedFloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tournaments)

        initViews()
        setupToolbar()
        setupRecyclerView()
        setupListeners()
        loadTournaments()
    }

    override fun onResume() {
        super.onResume()
        loadTournaments()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        chipGroup = findViewById(R.id.chipGroup)
        rvTournaments = findViewById(R.id.rvTournaments)
        progressBar = findViewById(R.id.progressBar)
        tvEmpty = findViewById(R.id.tvEmpty)
        fabCreateTournament = findViewById(R.id.fabCreateTournament)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = TournamentsAdapter(emptyList()) { tournament ->
            val intent = Intent(this, TournamentDetailsActivity::class.java).apply {
                putExtra("TOURNAMENT_ID", tournament.id)
            }
            startActivity(intent)
        }
        rvTournaments.layoutManager = LinearLayoutManager(this)
        rvTournaments.adapter = adapter
    }

    private fun setupListeners() {
        fabCreateTournament.setOnClickListener {
            startActivity(Intent(this, CreateTournamentActivity::class.java))
        }

        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            filterTournaments(checkedIds.firstOrNull())
        }
    }

    private fun loadTournaments() {
        progressBar.visibility = View.VISIBLE
        rvTournaments.visibility = View.GONE
        tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            repository.getTournaments()
                .onSuccess { list ->
                    allTournaments = list
                    filterTournaments(chipGroup.checkedChipId)
                }
                .onFailure { error ->
                    Toast.makeText(this@TournamentsActivity, "Failed to load: ${error.message}", Toast.LENGTH_LONG).show()
                }
            progressBar.visibility = View.GONE
        }
    }

    private fun filterTournaments(checkedChipId: Int?) {
        val filteredList = when (checkedChipId) {
            R.id.chipRegistration -> allTournaments.filter { it.status == "REGISTRATION" }
            R.id.chipActive -> allTournaments.filter { it.status == "ACTIVE" }
            R.id.chipCompleted -> allTournaments.filter { it.status == "COMPLETED" }
            else -> allTournaments // Default is all
        }

        adapter.updateData(filteredList)

        if (filteredList.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvTournaments.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvTournaments.visibility = View.VISIBLE
        }
    }
}
