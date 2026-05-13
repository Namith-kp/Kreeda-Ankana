package com.kreedaankana.ui.freeagents

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kreedaankana.data.repository.FreeAgentRepository
import com.kreedaankana.databinding.ActivityFreeAgentsBinding
import kotlinx.coroutines.launch

class FreeAgentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFreeAgentsBinding
    private val repository = FreeAgentRepository()
    private lateinit var adapter: FreeAgentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFreeAgentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        setupRecyclerView()
        loadFreeAgents()

        binding.fabRegister.setOnClickListener {
            startActivity(Intent(this, RegisterFreeAgentActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadFreeAgents()
    }

    private fun setupRecyclerView() {
        adapter = FreeAgentAdapter { agent ->
            Toast.makeText(this, "Contact: ${agent.contactInfo}", Toast.LENGTH_LONG).show()
        }
        binding.rvFreeAgents.layoutManager = LinearLayoutManager(this)
        binding.rvFreeAgents.adapter = adapter
    }

    private fun loadFreeAgents() {
        binding.progressBar.visibility = android.view.View.VISIBLE
        lifecycleScope.launch {
            repository.getFreeAgents().onSuccess { agents ->
                binding.progressBar.visibility = android.view.View.GONE
                adapter.submitList(agents)
                if (agents.isEmpty()) {
                    binding.tvEmpty.visibility = android.view.View.VISIBLE
                    binding.rvFreeAgents.visibility = android.view.View.GONE
                } else {
                    binding.tvEmpty.visibility = android.view.View.GONE
                    binding.rvFreeAgents.visibility = android.view.View.VISIBLE
                }
            }.onFailure {
                binding.progressBar.visibility = android.view.View.GONE
                Toast.makeText(this@FreeAgentsActivity, "Failed to load free agents", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
