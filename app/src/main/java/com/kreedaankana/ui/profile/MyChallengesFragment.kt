package com.kreedaankana.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.kreedaankana.data.model.Challenge
import com.kreedaankana.data.repository.ChallengeRepository
import com.kreedaankana.databinding.FragmentMyChallengesBinding
import com.kreedaankana.ui.challenge.ChallengeAdapter
import com.kreedaankana.utils.Extensions.gone
import com.kreedaankana.utils.Extensions.showToast
import com.kreedaankana.utils.Extensions.visible
import com.kreedaankana.utils.SessionManager
import kotlinx.coroutines.launch

class MyChallengesFragment : Fragment() {

    private var _binding: FragmentMyChallengesBinding? = null
    private val binding get() = _binding!!
    private val repository = ChallengeRepository()
    private lateinit var adapter: ChallengeAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyChallengesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        
        setupRecyclerView()
        loadMyChallenges()
    }

    private fun setupRecyclerView() {
        val teamName = SessionManager.getTeamName(requireContext())
        adapter = ChallengeAdapter(
            currentTeamName = teamName,
            onAccept = { /* Not applicable here */ },
            onCancel = { challenge ->
                showCancelConfirmation(challenge)
            }
        )
        binding.rvChallenges.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChallenges.adapter = adapter
    }

    private fun showCancelConfirmation(challenge: Challenge) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Cancel Challenge")
            .setMessage("Are you sure you want to remove this challenge?")
            .setPositiveButton("Yes, Remove") { _, _ ->
                performDeletion(challenge)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun performDeletion(challenge: Challenge) {
        binding.progressBar.visible()
        lifecycleScope.launch {
            val result = repository.deleteChallenge(challenge.challengeId)
            binding.progressBar.gone()
            result.onSuccess {
                requireContext().showToast("Challenge removed successfully")
                loadMyChallenges() // Refresh list
            }.onFailure {
                requireContext().showToast("Failed to remove challenge")
            }
        }
    }

    private fun loadMyChallenges() {
        val team = SessionManager.getTeam(requireContext())
        if (team == null) {
            binding.tvEmpty.text = "Register your team first to see challenges!"
            binding.tvEmpty.visible()
            return
        }

        binding.progressBar.visible()
        lifecycleScope.launch {
            val result = repository.getChallengesByTeam(team.id)
            binding.progressBar.gone()
            result.onSuccess { challenges ->
                adapter.submitList(challenges)
                if (challenges.isEmpty()) binding.tvEmpty.visible() else binding.tvEmpty.gone()
            }.onFailure {
                requireContext().showToast("Failed to load your challenges")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
