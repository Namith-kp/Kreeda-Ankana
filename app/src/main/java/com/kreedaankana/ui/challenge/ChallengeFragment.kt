package com.kreedaankana.ui.challenge

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.kreedaankana.R
import com.kreedaankana.data.model.Challenge
import com.kreedaankana.data.model.Tournament
import com.kreedaankana.data.repository.ChallengeRepository
import com.kreedaankana.data.repository.TournamentRepository
import com.kreedaankana.databinding.FragmentChallengeBinding
import com.kreedaankana.ui.tournaments.CreateTournamentActivity
import com.kreedaankana.ui.tournaments.TournamentDetailsActivity
import com.kreedaankana.ui.tournaments.TournamentsAdapter
import com.kreedaankana.utils.Extensions.gone
import com.kreedaankana.utils.Extensions.showToast
import com.kreedaankana.utils.Extensions.visible
import com.kreedaankana.utils.SessionManager
import kotlinx.coroutines.launch

class ChallengeFragment : Fragment() {

    private var _binding: FragmentChallengeBinding? = null
    private val binding get() = _binding!!
    private fun getSafeBinding() = _binding

    private val challengeRepository = ChallengeRepository()
    private val tournamentRepository = TournamentRepository()

    private lateinit var challengeAdapter: ChallengeAdapter
    private lateinit var tournamentsAdapter: TournamentsAdapter

    private var currentTab = 0 // 0 for Challenges, 1 for Tournaments
    private var cachedChallenges: List<Challenge> = emptyList()
    private var cachedTournaments: List<Tournament> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentChallengeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTabLayout()
        setupRecyclerViews()
        setupFab()
        observeChallenges()
    }

    override fun onResume() {
        super.onResume()
        if (currentTab == 1) {
            loadTournaments()
        }
    }

    private fun setupTabLayout() {
        binding.tabLayout.removeAllTabs()
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Match Challenges"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Tournaments Arena"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.position?.let { position ->
                    currentTab = position
                    switchTab(position)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerViews() {
        // Setup Match Challenges Adapter
        challengeAdapter = ChallengeAdapter(
            currentTeamName = SessionManager.getTeamName(requireContext()),
            onAccept = { challenge ->
                val myTeam = SessionManager.getTeamName(requireContext())
                if (myTeam.isEmpty()) {
                    requireContext().showToast("Register your team first!")
                } else {
                    lifecycleScope.launch {
                        val result = challengeRepository.acceptChallenge(challenge.challengeId, myTeam)
                        result.onSuccess { requireContext().showToast("✅ Challenge Accepted!") }
                            .onFailure { requireContext().showToast("Error: ${it.message}") }
                    }
                }
            },
            onChat = { challenge ->
                val myTeam = SessionManager.getTeamName(requireContext())
                val opponentName = if (challenge.teamName == myTeam) challenge.acceptedByTeam else challenge.teamName
                val intent = Intent(requireContext(), com.kreedaankana.ui.chat.ChatActivity::class.java).apply {
                    putExtra(com.kreedaankana.ui.chat.ChatActivity.EXTRA_CHALLENGE_ID, challenge.challengeId)
                    putExtra(com.kreedaankana.ui.chat.ChatActivity.EXTRA_OPPONENT_NAME, opponentName)
                }
                startActivity(intent)
            },
            onLiveScore = { challenge ->
                val myTeam = SessionManager.getTeamName(requireContext())
                val isParticipant = challenge.teamName == myTeam || challenge.acceptedByTeam == myTeam
                val intent = Intent(requireContext(), com.kreedaankana.ui.scorewall.LiveScoreActivity::class.java).apply {
                    putExtra(com.kreedaankana.ui.scorewall.LiveScoreActivity.EXTRA_CHALLENGE_ID, challenge.challengeId)
                    putExtra(com.kreedaankana.ui.scorewall.LiveScoreActivity.EXTRA_TEAM1_NAME, challenge.teamName)
                    putExtra(com.kreedaankana.ui.scorewall.LiveScoreActivity.EXTRA_TEAM2_NAME, challenge.acceptedByTeam)
                    putExtra(com.kreedaankana.ui.scorewall.LiveScoreActivity.EXTRA_IS_PARTICIPANT, isParticipant)
                }
                startActivity(intent)
            }
        )

        // Setup Tournaments Adapter
        tournamentsAdapter = TournamentsAdapter(emptyList()) { tournament ->
            val intent = Intent(requireContext(), TournamentDetailsActivity::class.java).apply {
                putExtra("TOURNAMENT_ID", tournament.id)
            }
            startActivity(intent)
        }

        binding.rvChallenges.layoutManager = LinearLayoutManager(requireContext())
        // Start with challengeAdapter
        binding.rvChallenges.adapter = challengeAdapter
    }

    private fun setupFab() {
        binding.fabPostChallenge.setOnClickListener {
            if (!SessionManager.isTeamRegistered(requireContext())) {
                requireContext().showToast("Register your team first!")
                return@setOnClickListener
            }
            if (currentTab == 0) {
                startActivity(Intent(requireContext(), PostChallengeActivity::class.java))
            } else {
                startActivity(Intent(requireContext(), CreateTournamentActivity::class.java))
            }
        }
    }

    private fun switchTab(tabPosition: Int) {
        if (tabPosition == 0) {
            // Challenges
            binding.rvChallenges.adapter = challengeAdapter
            binding.fabPostChallenge.setText("Post Challenge")
            binding.fabPostChallenge.setIconResource(R.drawable.ic_challenge)
            updateChallengeUI(cachedChallenges)
        } else {
            // Tournaments
            binding.rvChallenges.adapter = tournamentsAdapter
            binding.fabPostChallenge.setText("Host Tournament")
            binding.fabPostChallenge.setIconResource(R.drawable.ic_trophy)
            loadTournaments()
        }
    }

    private fun observeChallenges() {
        getSafeBinding()?.progressBar?.visible()
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                challengeRepository.getChallengesRealtime().collect { challenges ->
                    cachedChallenges = challenges
                    if (currentTab == 0) {
                        updateChallengeUI(challenges)
                    }
                }
            } catch (e: Exception) {
                getSafeBinding()?.progressBar?.gone()
                if (isAdded) {
                    requireContext().showToast("Failed to load challenges")
                }
            }
        }
    }

    private fun updateChallengeUI(challenges: List<Challenge>) {
        getSafeBinding()?.let { b ->
            b.progressBar.gone()
            challengeAdapter.submitList(challenges)
            if (challenges.isEmpty()) {
                b.tvEmpty.text = "No challenges yet!\nBe the first to post a challenge."
                b.tvEmpty.visible()
                b.rvChallenges.gone()
            } else {
                b.tvEmpty.gone()
                b.rvChallenges.visible()
            }
        }
    }

    private fun loadTournaments() {
        val b = getSafeBinding() ?: return
        b.progressBar.visible()
        b.rvChallenges.gone()
        b.tvEmpty.gone()

        viewLifecycleOwner.lifecycleScope.launch {
            tournamentRepository.getTournaments()
                .onSuccess { list ->
                    cachedTournaments = list
                    if (currentTab == 1) {
                        b.progressBar.gone()
                        tournamentsAdapter.updateData(list)
                        if (list.isEmpty()) {
                            b.tvEmpty.text = "No tournaments hosted yet!\nHost a tournament now."
                            b.tvEmpty.visible()
                            b.rvChallenges.gone()
                        } else {
                            b.tvEmpty.gone()
                            b.rvChallenges.visible()
                        }
                    }
                }
                .onFailure { error ->
                    if (currentTab == 1) {
                        b.progressBar.gone()
                        if (isAdded) {
                            requireContext().showToast("Failed to load tournaments: ${error.message}")
                        }
                    }
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
