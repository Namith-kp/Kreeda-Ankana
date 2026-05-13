package com.kreedaankana.ui.scorewall

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kreedaankana.data.model.ScoreEntry
import com.kreedaankana.databinding.ItemScoreBinding
import com.kreedaankana.utils.Extensions.formatDisplayDate

class ScoreAdapter : ListAdapter<ScoreEntry, ScoreAdapter.ScoreViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScoreViewHolder {
        val binding = ItemScoreBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScoreViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScoreViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ScoreViewHolder(private val binding: ItemScoreBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: ScoreEntry) {
            binding.tvWinner.text = "🏆 ${entry.winnerTeam}"
            binding.tvLoser.text = entry.loserTeam
            binding.tvScore.text = entry.score
            binding.tvSport.text = "🏅 ${entry.sport}"
            binding.tvDate.text = "📅 ${formatDisplayDate(entry.matchDate)}"
            if (entry.location.isNotEmpty()) binding.tvLocation.text = "📍 ${entry.location}"
            if (entry.notes.isNotEmpty()) binding.tvNotes.text = "📝 ${entry.notes}"
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<ScoreEntry>() {
            override fun areItemsTheSame(a: ScoreEntry, b: ScoreEntry) = a.scoreId == b.scoreId
            override fun areContentsTheSame(a: ScoreEntry, b: ScoreEntry) = a == b
        }
    }
}
