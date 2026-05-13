package com.kreedaankana.ui.scorewall

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kreedaankana.data.repository.ScoreRepository
import com.kreedaankana.databinding.FragmentScoreWallBinding
import com.kreedaankana.utils.Extensions.SPORTS
import com.kreedaankana.utils.Extensions.gone
import com.kreedaankana.utils.Extensions.showToast
import com.kreedaankana.utils.Extensions.visible
import kotlinx.coroutines.launch

class ScoreWallFragment : Fragment() {

    private var _binding: FragmentScoreWallBinding? = null
    private val binding get() = _binding!!
    private val repository = ScoreRepository()
    private lateinit var adapter: ScoreAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentScoreWallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFilter()
        setupFab()
        loadScores()
    }

    private fun setupRecyclerView() {
        adapter = ScoreAdapter()
        binding.rvScores.layoutManager = LinearLayoutManager(requireContext())
        binding.rvScores.adapter = adapter
    }

    private fun setupFilter() {
        val filterOptions = listOf("All Sports") + SPORTS
        val filterAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, filterOptions)
        binding.autoFilter.setAdapter(filterAdapter)
        binding.autoFilter.setText("All Sports", false)
        binding.autoFilter.setOnItemClickListener { _, _, position, _ ->
            val selected = filterOptions[position]
            if (selected == "All Sports") loadScores() else loadScoresBySport(selected)
        }
    }

    private fun setupFab() {
        binding.fabAddScore.setOnClickListener {
            startActivity(Intent(requireContext(), AddScoreActivity::class.java))
        }
    }

    private fun loadScores() {
        binding.progressBar.visible()
        lifecycleScope.launch {
            val result = repository.getAllScores()
            binding.progressBar.gone()
            result.onSuccess { scores ->
                adapter.submitList(scores)
                toggleEmpty(scores.isEmpty())
            }.onFailure { requireContext().showToast("Failed to load scores") }
        }
    }

    private fun loadScoresBySport(sport: String) {
        binding.progressBar.visible()
        lifecycleScope.launch {
            val result = repository.getScoresBySport(sport)
            binding.progressBar.gone()
            result.onSuccess { scores ->
                adapter.submitList(scores)
                toggleEmpty(scores.isEmpty())
            }.onFailure { requireContext().showToast("Failed to load scores") }
        }
    }

    private fun toggleEmpty(empty: Boolean) {
        if (empty) { 
            binding.llEmpty.visible()
            binding.rvScores.gone() 
        } else { 
            binding.llEmpty.gone()
            binding.rvScores.visible() 
        }
    }

    override fun onResume() {
        super.onResume()
        loadScores()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
