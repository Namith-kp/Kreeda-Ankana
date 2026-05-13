package com.kreedaankana.ui.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.kreedaankana.R
import com.kreedaankana.data.model.Player
import com.kreedaankana.data.model.Team
import com.kreedaankana.data.repository.TeamRepository
import com.kreedaankana.databinding.ActivityRegisterTeamBinding
import com.kreedaankana.utils.Extensions.SPORTS
import com.kreedaankana.utils.Extensions.gone
import com.kreedaankana.utils.Extensions.showToast
import com.kreedaankana.utils.Extensions.visible
import com.kreedaankana.utils.SessionManager
import kotlinx.coroutines.launch

class RegisterTeamActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterTeamBinding
    private val repository = TeamRepository()
    private var existingTeamId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterTeamBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupSportDropdown()
        setupTimeDropdown()
        prefillExistingData()
        setupRegisterButton()
        setupCaptainNameListener()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (SessionManager.isTeamRegistered(this)) "Edit Team" else "Register Team"
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupSportDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, SPORTS)
        binding.autoSport.setAdapter(adapter)

        binding.autoSport.setOnItemClickListener { parent, _, position, _ ->
            val selectedSport = parent.getItemAtPosition(position).toString()
            populatePlayersForSport(selectedSport)
        }
    }

    private fun setupTimeDropdown() {
        val times = listOf("Morning (6AM–9AM)", "Mid-Morning (9AM–12PM)",
            "Afternoon (12PM–3PM)", "Evening (4PM–6PM)", "Night (6PM–9PM)", "Flexible")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, times)
        binding.autoPreferredTime.setAdapter(adapter)
    }

    private fun prefillExistingData() {
        val team = SessionManager.getTeam(this) ?: return
        existingTeamId = team.id
        binding.etTeamName.setText(team.teamName)
        binding.etCaptainName.setText(team.captain)
        binding.etCaptainPhone.setText(team.captainPhone)
        binding.autoSport.setText(team.sport, false)
        binding.etLocation.setText(team.location)
        binding.autoPreferredTime.setText(team.preferredTime, false)

        if (team.sport.isNotEmpty()) {
            populatePlayersForSport(team.sport, team.players)
        }
    }

    private fun populatePlayersForSport(sport: String, existingPlayers: List<Player> = emptyList()) {
        binding.cardPlayers.visible()
        binding.llPlayersContainer.removeAllViews()

        val (numPlayers, roles) = when (sport) {
            "Cricket" -> Pair(11, listOf("Batsman", "Bowler", "All-Rounder", "Wicketkeeper", "Captain"))
            "Football" -> Pair(11, listOf("Goalkeeper", "Defender", "Midfielder", "Forward", "Captain"))
            "Volleyball" -> Pair(6, listOf("Setter", "Outside Hitter", "Opposite", "Middle Blocker", "Libero", "Captain"))
            "Kabaddi" -> Pair(7, listOf("Raider", "Defender", "All-Rounder", "Captain"))
            "Kho-Kho" -> Pair(12, listOf("Chaser", "Runner", "All-Rounder", "Captain"))
            else -> Pair(11, listOf("Player", "Captain", "Vice Captain"))
        }

        for (i in 0 until numPlayers) {
            val rowView = layoutInflater.inflate(R.layout.item_player_input, binding.llPlayersContainer, false)
            val etName = rowView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_player_name)
            val etNumber = rowView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_player_number)
            val autoRole = rowView.findViewById<android.widget.AutoCompleteTextView>(R.id.auto_player_role)

            // Setup Adapter
            val roleAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, roles)
            autoRole.setAdapter(roleAdapter)

            // Populate existing or default values
            if (i < existingPlayers.size) {
                val player = existingPlayers[i]
                etName.setText(player.name)
                etNumber.setText(player.number)
                autoRole.setText(player.role, false)
            } else {
                etNumber.setText((i + 1).toString())
                val defaultRole = when (sport) {
                    "Cricket" -> when (i) {
                        0 -> "Captain"
                        1 -> "Wicketkeeper"
                        in 2..5 -> "Batsman"
                        in 6..7 -> "All-Rounder"
                        else -> "Bowler"
                    }
                    "Football" -> when (i) {
                        0 -> "Captain"
                        1 -> "Goalkeeper"
                        in 2..5 -> "Defender"
                        in 6..8 -> "Midfielder"
                        else -> "Forward"
                    }
                    "Volleyball" -> when (i) {
                        0 -> "Captain"
                        1 -> "Setter"
                        2 -> "Outside Hitter"
                        3 -> "Opposite"
                        4 -> "Middle Blocker"
                        else -> "Libero"
                    }
                    "Kabaddi" -> when (i) {
                        0 -> "Captain"
                        in 1..3 -> "Raider"
                        in 4..5 -> "Defender"
                        else -> "All-Rounder"
                    }
                    "Kho-Kho" -> when (i) {
                        0 -> "Captain"
                        in 1..5 -> "Chaser"
                        in 6..10 -> "Runner"
                        else -> "All-Rounder"
                    }
                    else -> if (i == 0) "Captain" else "Player"
                }
                autoRole.setText(defaultRole, false)

                // Autofill captain row
                if (i == 0) {
                    val captainName = binding.etCaptainName.text.toString().trim()
                    if (captainName.isNotEmpty()) {
                        etName.setText(captainName)
                    }
                }
            }

            binding.llPlayersContainer.addView(rowView)
        }
    }

    private fun setupCaptainNameListener() {
        binding.etCaptainName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val container = binding.llPlayersContainer
                if (container.childCount > 0) {
                    val firstRow = container.getChildAt(0)
                    val etName = firstRow.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_player_name)
                    if (etName != null && etName.text.toString().trim().isEmpty()) {
                        etName.setText(s)
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupRegisterButton() {
        binding.btnRegister.text = if (SessionManager.isTeamRegistered(this)) "Update Team" else "Register Team"
        binding.btnRegister.setOnClickListener {
            val teamName = binding.etTeamName.text.toString().trim()
            val captainName = binding.etCaptainName.text.toString().trim()
            val captainPhone = binding.etCaptainPhone.text.toString().trim()
            val sport = binding.autoSport.text.toString().trim()
            val location = binding.etLocation.text.toString().trim()
            val preferredTime = binding.autoPreferredTime.text.toString().trim()

            if (teamName.isEmpty()) { binding.tilTeamName.error = "Required"; return@setOnClickListener }
            if (captainPhone.isEmpty() || captainPhone.length < 10) { binding.tilCaptainPhone.error = "Valid phone required"; return@setOnClickListener }
            if (sport.isEmpty()) { showToast("Select a sport"); return@setOnClickListener }
            if (location.isEmpty()) { binding.tilLocation.error = "Required"; return@setOnClickListener }

            binding.tilTeamName.error = null
            binding.tilCaptainPhone.error = null
            binding.tilLocation.error = null

            // Read dynamic players list
            val playersList = mutableListOf<Player>()
            val count = binding.llPlayersContainer.childCount
            for (i in 0 until count) {
                val rowView = binding.llPlayersContainer.getChildAt(i)
                val etName = rowView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_player_name)
                val etNumber = rowView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_player_number)
                val autoRole = rowView.findViewById<android.widget.AutoCompleteTextView>(R.id.auto_player_role)

                val name = etName.text.toString().trim()
                val number = etNumber.text.toString().trim()
                val role = autoRole.text.toString().trim()

                if (name.isNotEmpty()) {
                    playersList.add(Player(name = name, role = role, number = number))
                }
            }

            val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val team = Team(
                id = existingTeamId ?: "",
                teamName = teamName,
                captainId = currentUid,
                captain = captainName,
                captainPhone = captainPhone,
                sport = sport,
                location = location,
                preferredTime = preferredTime,
                players = playersList
            )

            binding.progressBar.visible()
            binding.btnRegister.isEnabled = false

            lifecycleScope.launch {
                // Ensure team name is unique within the selected sport category
                val nameTaken = repository.isTeamNameTaken(teamName, sport, existingTeamId)
                if (nameTaken) {
                    binding.progressBar.gone()
                    binding.btnRegister.isEnabled = true
                    binding.tilTeamName.error = "This team name is already registered for $sport"
                    showToast("❌ This team name is already taken for $sport. Please choose another one.")
                    return@launch
                }
                binding.tilTeamName.error = null

                val result = repository.registerTeam(team)
                binding.progressBar.gone()
                binding.btnRegister.isEnabled = true

                result.onSuccess { registeredTeam ->
                    // Update user profile in Firestore to link them to their new team
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser != null) {
                        try {
                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(currentUser.uid)
                                .update(mapOf(
                                    "teamId" to registeredTeam.id,
                                    "teamName" to registeredTeam.teamName
                                ))
                        } catch (e: Exception) {
                            // Ignored if user doc does not have matching update structure
                        }
                    }
                    SessionManager.saveTeam(this@RegisterTeamActivity, registeredTeam)
                    showToast(if (existingTeamId != null) "✅ Team Updated Successfully!" else "✅ Team Registered Successfully!")
                    finish()
                }.onFailure { error ->
                    showToast("❌ Error: ${error.message}")
                }
            }
        }
    }
}
