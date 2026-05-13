package com.kreedaankana.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.kreedaankana.R
import com.kreedaankana.databinding.FragmentHomeBinding
import com.kreedaankana.ui.auth.RegisterTeamActivity
import com.kreedaankana.ui.booking.BookSlotActivity
import com.kreedaankana.ui.challenge.PostChallengeActivity
import com.kreedaankana.ui.scorewall.AddScoreActivity
import com.kreedaankana.utils.Extensions.formatDisplayDate
import com.kreedaankana.utils.Extensions.getTodayDate
import com.kreedaankana.utils.SessionManager

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupClickListeners()
        
        // Handle top insets to ensure content is below status bar but tight
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            // Apply only half of the status bar height as padding to keep it very tight
            // or just the full height if needed. Let's use the full height for safety but no extra.
            binding.tvDate.parent.let { parent ->
                if (parent is View) {
                    parent.setPadding(parent.paddingLeft, systemBars.top, parent.paddingRight, parent.paddingBottom)
                }
            }
            insets
        }
    }

    private fun setupUI() {
        binding.tvDate.text = formatDisplayDate(getTodayDate())
        val team = SessionManager.getTeam(requireContext())
        if (team != null) {
            binding.tvGreeting.text = "Welcome back,\n${team.teamName}! 🏆"
            binding.tvSport.text = team.sport
            binding.tvLocation.text = team.location
            binding.cardTeamInfo.visibility = View.VISIBLE
            binding.cardNoTeamCta.visibility = View.GONE
            binding.btnRegisterTeam.text = "Update Team Profile"
        } else {
            binding.tvGreeting.text = "Welcome to\nKreeda-Ankana! 🏟️"
            binding.cardTeamInfo.visibility = View.GONE
            binding.cardNoTeamCta.visibility = View.VISIBLE
            binding.btnRegisterTeam.text = "Register Your Team"
        }
    }

    override fun onResume() {
        super.onResume()
        setupUI()
    }

    private fun setupClickListeners() {
        binding.btnRegisterTeam.setOnClickListener {
            startActivity(Intent(requireContext(), RegisterTeamActivity::class.java))
        }
        binding.btnHomeBrowseTeams.setOnClickListener {
            startActivity(Intent(requireContext(), com.kreedaankana.ui.team.BrowseTeamsActivity::class.java))
        }
        binding.btnHomeCreateTeam.setOnClickListener {
            startActivity(Intent(requireContext(), RegisterTeamActivity::class.java))
        }
        binding.cardBookSlot.setOnClickListener {
            startActivity(Intent(requireContext(), BookSlotActivity::class.java))
        }
        binding.cardChallenge.setOnClickListener {
            startActivity(Intent(requireContext(), PostChallengeActivity::class.java))
        }
        binding.cardScoreWall.setOnClickListener {
            startActivity(Intent(requireContext(), AddScoreActivity::class.java))
        }
        binding.cardFreeAgents.setOnClickListener {
            startActivity(Intent(requireContext(), com.kreedaankana.ui.freeagents.FreeAgentsActivity::class.java))
        }
        binding.cardTournaments.setOnClickListener {
            startActivity(Intent(requireContext(), com.kreedaankana.ui.tournaments.TournamentsActivity::class.java))
        }

        // Featured Banners
        binding.cardFeaturedCricketCup.setOnClickListener {
            startActivity(Intent(requireContext(), com.kreedaankana.ui.tournaments.TournamentsActivity::class.java))
        }
        binding.cardFeaturedFloodlightPromo.setOnClickListener {
            val intent = Intent(requireContext(), BookSlotActivity::class.java).apply {
                putExtra("SELECTED_SPORT", "Cricket")
            }
            startActivity(intent)
        }
        binding.cardFeaturedFreeAgents.setOnClickListener {
            startActivity(Intent(requireContext(), com.kreedaankana.ui.freeagents.FreeAgentsActivity::class.java))
        }

        // Explore by Sport Chips
        binding.chipSportCricket.setOnClickListener {
            val intent = Intent(requireContext(), BookSlotActivity::class.java).apply {
                putExtra("SELECTED_SPORT", "Cricket")
            }
            startActivity(intent)
        }
        binding.chipSportFootball.setOnClickListener {
            val intent = Intent(requireContext(), BookSlotActivity::class.java).apply {
                putExtra("SELECTED_SPORT", "Football")
            }
            startActivity(intent)
        }
        binding.chipSportBadminton.setOnClickListener {
            val intent = Intent(requireContext(), BookSlotActivity::class.java).apply {
                putExtra("SELECTED_SPORT", "Badminton")
            }
            startActivity(intent)
        }
        binding.chipSportBasketball.setOnClickListener {
            val intent = Intent(requireContext(), BookSlotActivity::class.java).apply {
                putExtra("SELECTED_SPORT", "Basketball")
            }
            startActivity(intent)
        }
        binding.chipSportVolleyball.setOnClickListener {
            val intent = Intent(requireContext(), BookSlotActivity::class.java).apply {
                putExtra("SELECTED_SPORT", "Volleyball")
            }
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
