package com.kreedaankana.ui.team

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kreedaankana.R
import com.kreedaankana.data.model.JoinRequest
import com.kreedaankana.data.model.Team
import com.kreedaankana.data.repository.TeamRepository
import com.kreedaankana.databinding.ActivityBrowseTeamsBinding
import com.kreedaankana.utils.Extensions.gone
import com.kreedaankana.utils.Extensions.showToast
import com.kreedaankana.utils.Extensions.visible
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class BrowseTeamsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBrowseTeamsBinding
    private val teamRepo = TeamRepository()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var allTeams = listOf<Team>()
    private var requestedTeamIds = mutableSetOf<String>()
    private lateinit var teamAdapter: TeamAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowseTeamsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupSearchListener()
        loadTeamsAndRequests()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        teamAdapter = TeamAdapter()
        binding.rvTeams.layoutManager = LinearLayoutManager(this)
        binding.rvTeams.adapter = teamAdapter
    }

    private fun setupSearchListener() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterTeams(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadTeamsAndRequests() {
        binding.progressBar.visible()
        binding.llEmptyState.gone()

        lifecycleScope.launch {
            try {
                // Get all teams
                val teamResult = teamRepo.getAllTeams()
                
                // Get current user's join requests
                val userId = auth.currentUser?.uid ?: ""
                val requestSnapshot = db.collection("join_requests")
                    .whereEqualTo("userId", userId)
                    .get().await()
                
                requestedTeamIds.clear()
                for (doc in requestSnapshot.documents) {
                    val status = doc.getString("status") ?: "PENDING"
                    if (status == "PENDING" || status == "APPROVED") {
                        val teamId = doc.getString("teamId")
                        if (teamId != null) {
                            requestedTeamIds.add(teamId)
                        }
                    }
                }

                teamResult.onSuccess { teams ->
                    // Exclude any team where the user is already the captain or creator
                    allTeams = teams.filter { it.captainId != userId }
                    filterTeams(binding.etSearch.text.toString())
                }.onFailure { error ->
                    showToast("Error loading teams: ${error.message}")
                }
            } catch (e: Exception) {
                showToast("Error: ${e.message}")
            } finally {
                binding.progressBar.gone()
            }
        }
    }

    private fun filterTeams(query: String) {
        val filtered = if (query.isEmpty()) {
            allTeams
        } else {
            allTeams.filter {
                it.teamName.contains(query, ignoreCase = true) ||
                it.sport.contains(query, ignoreCase = true) ||
                it.location.contains(query, ignoreCase = true)
            }
        }

        teamAdapter.submitList(filtered)
        if (filtered.isEmpty()) {
            binding.llEmptyState.visible()
        } else {
            binding.llEmptyState.gone()
        }
    }

    private fun showJoinRequestDialog(team: Team) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_join_request, null)
        val autoRole = dialogView.findViewById<AutoCompleteTextView>(R.id.auto_req_role)
        val etJersey = dialogView.findViewById<TextInputEditText>(R.id.et_req_jersey)

        // Setup dynamic roles dropdown based on team sport
        val roles = when (team.sport) {
            "Cricket" -> listOf("Batsman", "Bowler", "All-Rounder", "Wicketkeeper")
            "Football" -> listOf("Goalkeeper", "Defender", "Midfielder", "Forward")
            "Volleyball" -> listOf("Setter", "Outside Hitter", "Opposite", "Middle Blocker", "Libero")
            "Kabaddi" -> listOf("Raider", "Defender", "All-Rounder")
            "Kho-Kho" -> listOf("Chaser", "Runner", "All-Rounder")
            else -> listOf("Player")
        }
        val roleAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roles)
        autoRole.setAdapter(roleAdapter)
        if (roles.isNotEmpty()) {
            autoRole.setText(roles[0], false)
        }

        AlertDialog.Builder(this)
            .setTitle("Request to Join ${team.teamName}")
            .setView(dialogView)
            .setPositiveButton("Submit Request") { _, _ ->
                val preferredRole = autoRole.text.toString().trim()
                val preferredJersey = etJersey.text.toString().trim()

                submitJoinRequest(team, preferredRole, preferredJersey)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun submitJoinRequest(team: Team, role: String, jersey: String) {
        val user = auth.currentUser ?: return
        binding.progressBar.visible()

        lifecycleScope.launch {
            try {
                // Fetch the user's document first to get their official registered name
                val userDoc = db.collection("users").document(user.uid).get().await()
                val fullName = if (userDoc != null && userDoc.exists()) {
                    userDoc.getString("fullName") ?: (user.displayName ?: "Athlete")
                } else {
                    user.displayName ?: "Athlete"
                }

                val reqId = db.collection("join_requests").document().id
                val request = JoinRequest(
                    id = reqId,
                    teamId = team.id,
                    teamName = team.teamName,
                    userId = user.uid,
                    userName = fullName,
                    userEmail = user.email ?: "",
                    sport = team.sport,
                    preferredRole = role,
                    preferredNumber = jersey,
                    status = "PENDING"
                )

                db.collection("join_requests").document(reqId).set(request).await()
                showToast("✅ Request sent! Waiting for Captain's approval.")
                loadTeamsAndRequests() // Refresh state
            } catch (e: Exception) {
                showToast("Error sending request: ${e.message}")
            } finally {
                binding.progressBar.gone()
            }
        }
    }

    private inner class TeamAdapter : RecyclerView.Adapter<TeamAdapter.ViewHolder>() {

        private var items = listOf<Team>()

        fun submitList(newList: List<Team>) {
            items = newList
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_browse_team_row, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount() = items.size

        inner class ViewHolder(view: View) : ViewHolderClass(view) {
            val tvTeamName: TextView = view.findViewById(R.id.tv_team_name)
            val tvSportBadge: TextView = view.findViewById(R.id.tv_sport_badge)
            val tvTeamCaptain: TextView = view.findViewById(R.id.tv_team_captain)
            val tvTeamLocation: TextView = view.findViewById(R.id.tv_team_location)
            val btnJoinTeam: MaterialButton = view.findViewById(R.id.btn_join_team)

            fun bind(team: Team) {
                tvTeamName.text = team.teamName
                tvSportBadge.text = team.sport
                tvTeamCaptain.text = "Captain: ${team.captain}"
                tvTeamLocation.text = "📍 ${team.location} | ${team.preferredTime}"

                if (requestedTeamIds.contains(team.id)) {
                    btnJoinTeam.text = "Requested (Pending)"
                    btnJoinTeam.isEnabled = false
                } else {
                    btnJoinTeam.text = "Request to Join"
                    btnJoinTeam.isEnabled = true
                    btnJoinTeam.setOnClickListener {
                        showJoinRequestDialog(team)
                    }
                }
            }
        }
    }
}

open class ViewHolderClass(view: View) : RecyclerView.ViewHolder(view)
