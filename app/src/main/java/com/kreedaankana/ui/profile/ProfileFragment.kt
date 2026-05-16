package com.kreedaankana.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kreedaankana.databinding.FragmentProfileBinding
import androidx.navigation.fragment.findNavController
import com.kreedaankana.data.repository.TeamRepository
import com.kreedaankana.ui.auth.LoginActivity
import com.kreedaankana.ui.auth.RegisterTeamActivity
import com.kreedaankana.utils.SessionManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserInfo()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadTeamInfo()
    }

    private fun loadUserInfo() {
        val user = auth.currentUser
        if (user != null) {
            binding.tvProfileEmail.text = user.email
            
            // Try to get name from Firestore
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("fullName")
                        if (!name.isNullOrEmpty()) {
                            binding.tvProfileName.text = name
                        } else {
                            binding.tvProfileName.text = user.displayName ?: "Athlete"
                        }
                    } else {
                        binding.tvProfileName.text = user.displayName ?: "Athlete"
                    }
                }
                .addOnFailureListener {
                    binding.tvProfileName.text = user.displayName ?: "Athlete"
                }
        }
    }

    private fun loadTeamInfo() {
        val user = auth.currentUser ?: return
        
        // Query latest user document to find any team updates (approvals, etc.)
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { userDoc ->
                if (userDoc != null && userDoc.exists()) {
                    val teamId = userDoc.getString("teamId")
                    if (!teamId.isNullOrEmpty()) {
                        // Fetch the team details from teams collection
                        db.collection("teams").document(teamId).get()
                            .addOnSuccessListener { teamDoc ->
                                if (teamDoc != null && teamDoc.exists()) {
                                    val team = teamDoc.toObject(com.kreedaankana.data.model.Team::class.java)
                                    if (team != null) {
                                        // Cache locally
                                        context?.let { SessionManager.saveTeam(it, team) }
                                        
                                        binding.llTeamInfo.visibility = View.VISIBLE
                                        binding.llTeamEmpty.visibility = View.GONE
                                        binding.tvTeamName.text = team.teamName
                                        binding.tvTeamDetails.text = "Captain: ${team.captain} | ${team.sport}"
                                        binding.tvTeamTimeLoc.text = "📍 ${team.location} | ${team.preferredTime}"
                                        populateSquad(team)

                                        // If the user is the captain of this team, load join requests
                                        if (team.captainId == user.uid) {
                                            loadPendingJoinRequests(team)
                                        } else {
                                            hideJoinRequests()
                                        }
                                        return@addOnSuccessListener
                                    }
                                }
                                useLocalOrEmpty()
                            }
                            .addOnFailureListener {
                                useLocalOrEmpty()
                            }
                    } else {
                        context?.let { SessionManager.clearTeam(it) }
                        useLocalOrEmpty()
                    }
                } else {
                    useLocalOrEmpty()
                }
            }
            .addOnFailureListener {
                useLocalOrEmpty()
            }
    }

    private fun useLocalOrEmpty() {
        if (!isAdded) return
        val team = SessionManager.getTeam(requireContext())
        if (team != null) {
            binding.llTeamInfo.visibility = View.VISIBLE
            binding.llTeamEmpty.visibility = View.GONE
            binding.tvTeamName.text = team.teamName
            binding.tvTeamDetails.text = "Captain: ${team.captain} | ${team.sport}"
            binding.tvTeamTimeLoc.text = "📍 ${team.location} | ${team.preferredTime}"
            populateSquad(team)
            
            val user = auth.currentUser
            if (user != null && team.captainId == user.uid) {
                loadPendingJoinRequests(team)
            } else {
                hideJoinRequests()
            }
        } else {
            binding.llTeamInfo.visibility = View.GONE
            binding.llTeamEmpty.visibility = View.VISIBLE
            hideJoinRequests()
        }
    }

    private fun hideJoinRequests() {
        binding.tvJoinRequestsHeader.visibility = View.GONE
        binding.llJoinRequestsContainer.visibility = View.GONE
    }

    private fun loadPendingJoinRequests(team: com.kreedaankana.data.model.Team) {
        db.collection("join_requests")
            .whereEqualTo("teamId", team.id)
            .whereEqualTo("status", "PENDING")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener
                binding.llJoinRequestsContainer.removeAllViews()

                if (snapshot == null || snapshot.isEmpty) {
                    hideJoinRequests()
                } else {
                    binding.tvJoinRequestsHeader.visibility = View.VISIBLE
                    binding.llJoinRequestsContainer.visibility = View.VISIBLE

                    for (doc in snapshot.documents) {
                        val request = doc.toObject(com.kreedaankana.data.model.JoinRequest::class.java)
                        if (request != null) {
                            val requestView = layoutInflater.inflate(com.kreedaankana.R.layout.item_join_request, binding.llJoinRequestsContainer, false)
                            
                            val tvName = requestView.findViewById<android.widget.TextView>(com.kreedaankana.R.id.tv_req_player_name)
                            val tvEmail = requestView.findViewById<android.widget.TextView>(com.kreedaankana.R.id.tv_req_player_email)
                            val tvPref = requestView.findViewById<android.widget.TextView>(com.kreedaankana.R.id.tv_req_player_pref)
                            val tvInitials = requestView.findViewById<android.widget.TextView>(com.kreedaankana.R.id.tv_req_player_initials)
                            val btnApprove = requestView.findViewById<com.google.android.material.button.MaterialButton>(com.kreedaankana.R.id.btn_approve)
                            val btnReject = requestView.findViewById<com.google.android.material.button.MaterialButton>(com.kreedaankana.R.id.btn_reject)

                            tvName.text = request.userName
                            tvEmail.text = request.userEmail
                            tvPref.text = "Prefers: ${request.preferredRole} | Jersey #${request.preferredNumber}"

                            val initials = request.userName.trim().split(" ")
                                .filter { it.isNotEmpty() }
                                .take(2)
                                .map { it.first().uppercase() }
                                .joinToString("")
                            tvInitials.text = if (initials.isNotEmpty()) initials else "?"

                            btnApprove.setOnClickListener {
                                approveJoinRequest(request, team)
                            }

                            btnReject.setOnClickListener {
                                rejectJoinRequest(request)
                            }

                            binding.llJoinRequestsContainer.addView(requestView)
                        }
                    }
                }
            }
            .addOnFailureListener {
                hideJoinRequests()
            }
    }

    private fun approveJoinRequest(request: com.kreedaankana.data.model.JoinRequest, team: com.kreedaankana.data.model.Team) {
        lifecycleScope.launch {
            try {
                // 1. Add player to team's list
                val newPlayer = com.kreedaankana.data.model.Player(
                    name = request.userName,
                    role = request.preferredRole,
                    number = request.preferredNumber
                )
                val updatedPlayers = team.players.toMutableList()
                updatedPlayers.add(newPlayer)
                
                val updatedTeam = team.copy(players = updatedPlayers)

                // 2. Update team inside Firestore
                db.collection("teams").document(team.id).set(updatedTeam).await()

                // 3. Update join_requests status to APPROVED
                db.collection("join_requests").document(request.id).update("status", "APPROVED").await()

                // 4. Update applicant user profile in users collection with teamId & teamName
                db.collection("users").document(request.userId).update(
                    mapOf(
                        "teamId" to team.id,
                        "teamName" to team.teamName
                    )
                ).await()

                Toast.makeText(requireContext(), "✅ Approved request of ${request.userName}!", Toast.LENGTH_SHORT).show()
                loadTeamInfo() // Refresh everything
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "❌ Error approving: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun rejectJoinRequest(request: com.kreedaankana.data.model.JoinRequest) {
        lifecycleScope.launch {
            try {
                db.collection("join_requests").document(request.id).update("status", "REJECTED").await()
                Toast.makeText(requireContext(), "❌ Rejected request of ${request.userName}", Toast.LENGTH_SHORT).show()
                loadTeamInfo() // Refresh everything
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun populateSquad(team: com.kreedaankana.data.model.Team) {
        binding.llSquadContainer.removeAllViews()
        
        if (team.players.isEmpty()) {
            binding.tvSquadHeader.visibility = View.GONE
            binding.llSquadContainer.visibility = View.GONE
        } else {
            binding.tvSquadHeader.visibility = View.VISIBLE
            binding.llSquadContainer.visibility = View.VISIBLE
            val chunkedPlayers = team.players.chunked(2)
            for (rowPair in chunkedPlayers) {
                val rowLayout = android.widget.LinearLayout(requireContext()).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                
                for (player in rowPair) {
                    val playerView = layoutInflater.inflate(com.kreedaankana.R.layout.item_squad_member, rowLayout, false)
                    playerView.layoutParams = android.widget.LinearLayout.LayoutParams(
                        0,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                    
                    val tvName = playerView.findViewById<android.widget.TextView>(com.kreedaankana.R.id.tv_squad_player_name)
                    val tvRole = playerView.findViewById<android.widget.TextView>(com.kreedaankana.R.id.tv_squad_player_role)
                    val tvJersey = playerView.findViewById<android.widget.TextView>(com.kreedaankana.R.id.tv_squad_player_jersey)
                    val tvInitials = playerView.findViewById<android.widget.TextView>(com.kreedaankana.R.id.tv_player_initials)
                    val flAvatar = playerView.findViewById<android.widget.FrameLayout>(com.kreedaankana.R.id.fl_avatar_container)
                    
                    tvName.text = player.name
                    tvRole.text = player.role
                    
                    if (player.number.isNotEmpty()) {
                        tvJersey.text = "#${player.number}"
                        tvJersey.visibility = View.VISIBLE
                    } else {
                        tvJersey.visibility = View.GONE
                    }
                    
                    val initials = player.name.trim().split(" ")
                        .filter { it.isNotEmpty() }
                        .take(2)
                        .map { it.first().uppercase() }
                        .joinToString("")
                    tvInitials.text = if (initials.isNotEmpty()) initials else "?"
                    
                    val drawableRes = when {
                        player.role.contains("Captain", ignoreCase = true) -> com.kreedaankana.R.drawable.circle_bg_gold
                        player.role.contains("Wicketkeeper", ignoreCase = true) || player.role.contains("Goalkeeper", ignoreCase = true) -> com.kreedaankana.R.drawable.circle_bg_red
                        player.role.contains("All-Rounder", ignoreCase = true) || player.role.contains("Midfielder", ignoreCase = true) -> com.kreedaankana.R.drawable.circle_bg_gold
                        player.role.contains("Bowler", ignoreCase = true) || player.role.contains("Defender", ignoreCase = true) -> com.kreedaankana.R.drawable.circle_bg_red
                        else -> com.kreedaankana.R.drawable.circle_bg_blue
                    }
                    flAvatar.setBackgroundResource(drawableRes)
                    
                    rowLayout.addView(playerView)
                }
                
                // Add a dummy view to balance the row if it has an odd number of items
                if (rowPair.size == 1) {
                    val spacer = android.view.View(requireContext())
                    spacer.layoutParams = android.widget.LinearLayout.LayoutParams(
                        0,
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        1f
                    )
                    rowLayout.addView(spacer)
                }
                
                binding.llSquadContainer.addView(rowLayout)
            }
        }
    }

    private fun setupListeners() {
        binding.btnLogout.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout") { _, _ ->
                    auth.signOut()
                    SessionManager.clearSession(requireContext())
                    val intent = Intent(requireContext(), LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnMyBookings.setOnClickListener {
            findNavController().navigate(com.kreedaankana.R.id.action_profile_to_myBookings)
        }

        binding.btnMyChallenges.setOnClickListener {
            findNavController().navigate(com.kreedaankana.R.id.action_profile_to_myChallenges)
        }

        binding.btnSettings.setOnClickListener {
            Toast.makeText(requireContext(), "Coming Soon: Settings", Toast.LENGTH_SHORT).show()
        }

        binding.btnEditTeam.setOnClickListener {
            val intent = Intent(requireContext(), RegisterTeamActivity::class.java)
            startActivity(intent)
        }

        binding.btnRegisterTeamPrompt.setOnClickListener {
            val intent = Intent(requireContext(), RegisterTeamActivity::class.java)
            startActivity(intent)
        }

        binding.btnJoinTeamPrompt.setOnClickListener {
            val intent = Intent(requireContext(), com.kreedaankana.ui.team.BrowseTeamsActivity::class.java)
            startActivity(intent)
        }

        binding.btnDeleteTeam.setOnClickListener {
            val team = SessionManager.getTeam(requireContext()) ?: return@setOnClickListener
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Delete Team Profile")
                .setMessage("Are you sure you want to delete your registered team? This action is permanent.")
                .setPositiveButton("Delete") { _, _ ->
                    binding.btnDeleteTeam.isEnabled = false
                    binding.btnEditTeam.isEnabled = false
                    lifecycleScope.launch {
                        val repository = TeamRepository()
                        val result = repository.deleteTeam(team.id)
                        binding.btnDeleteTeam.isEnabled = true
                        binding.btnEditTeam.isEnabled = true
                        result.onSuccess {
                            SessionManager.clearTeam(requireContext())
                            Toast.makeText(requireContext(), "✅ Team Profile Deleted Successfully!", Toast.LENGTH_SHORT).show()
                            loadTeamInfo()
                        }.onFailure { error ->
                            Toast.makeText(requireContext(), "❌ Error: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
