package com.kreedaankana.ui.freeagents

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kreedaankana.data.model.FreeAgent
import com.kreedaankana.databinding.ItemFreeAgentBinding

class FreeAgentAdapter(
    private val onContact: (FreeAgent) -> Unit
) : ListAdapter<FreeAgent, FreeAgentAdapter.FreeAgentViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FreeAgentViewHolder {
        val binding = ItemFreeAgentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FreeAgentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FreeAgentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FreeAgentViewHolder(private val binding: ItemFreeAgentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(agent: FreeAgent) {
            binding.tvPlayerName.text = agent.playerName
            binding.tvSport.text = agent.sport
            binding.tvRole.text = agent.role
            binding.tvSkillLevel.text = "Skill: ${agent.skillLevel}"
            binding.tvAvailability.text = "Available: ${agent.availability}"
            binding.tvLocation.text = "Location: ${agent.location}"

            binding.btnContact.setOnClickListener {
                onContact(agent)
            }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<FreeAgent>() {
            override fun areItemsTheSame(oldItem: FreeAgent, newItem: FreeAgent) = oldItem.agentId == newItem.agentId
            override fun areContentsTheSame(oldItem: FreeAgent, newItem: FreeAgent) = oldItem == newItem
        }
    }
}
