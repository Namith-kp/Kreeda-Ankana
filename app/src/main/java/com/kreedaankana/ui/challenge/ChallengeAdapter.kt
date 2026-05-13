package com.kreedaankana.ui.challenge

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kreedaankana.R
import com.kreedaankana.data.model.Challenge
import com.kreedaankana.databinding.ItemChallengeBinding
import com.kreedaankana.utils.Extensions.formatDisplayDate

class ChallengeAdapter(
    private val currentTeamName: String,
    private val onAccept: (Challenge) -> Unit,
    private val onCancel: ((Challenge) -> Unit)? = null,
    private val onChat: ((Challenge) -> Unit)? = null,
    private val onLiveScore: ((Challenge) -> Unit)? = null
) : ListAdapter<Challenge, ChallengeAdapter.ChallengeViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val binding = ItemChallengeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChallengeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChallengeViewHolder(private val binding: ItemChallengeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(challenge: Challenge) {
            val ctx = binding.root.context
            binding.tvTeamName.text = "⚔️ ${challenge.teamName}"
            binding.tvMessage.text = challenge.message
            binding.tvSport.text = "🏅 ${challenge.sport}"
            binding.tvDate.text = "📅 ${formatDisplayDate(challenge.proposedDate)}"
            binding.tvTime.text = "⏰ ${challenge.proposedTime}"
            if (challenge.location.isNotEmpty()) {
                binding.tvLocation.text = "📍 ${challenge.location}"
            }

            when (challenge.status) {
                "open" -> {
                    binding.tvStatus.text = "OPEN"
                    binding.tvStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.status_open_bg))
                    binding.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_open))
                    val isOwner = challenge.teamName == currentTeamName
                    val canAccept = !isOwner && currentTeamName.isNotEmpty()
                    
                    binding.btnAccept.visibility = if (!isOwner) android.view.View.VISIBLE else android.view.View.GONE
                    binding.btnAccept.isEnabled = canAccept
                    binding.btnAccept.alpha = if (canAccept) 1f else 0.4f
                    binding.btnAccept.setOnClickListener { onAccept(challenge) }
                    binding.btnChat.visibility = android.view.View.GONE
                    binding.btnLiveScore.visibility = android.view.View.GONE

                    if (isOwner && onCancel != null) {
                        binding.btnCancel.visibility = android.view.View.VISIBLE
                        binding.btnCancel.setOnClickListener { onCancel.invoke(challenge) }
                    } else {
                        binding.btnCancel.visibility = android.view.View.GONE
                    }
                }
                "accepted" -> {
                    binding.tvStatus.text = "ACCEPTED ✅"
                    binding.tvStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.status_accepted_bg))
                    binding.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_accepted))
                    binding.btnAccept.visibility = android.view.View.VISIBLE
                    binding.btnAccept.isEnabled = false
                    binding.btnAccept.text = "Accepted by ${challenge.acceptedByTeam}"
                    binding.btnCancel.visibility = android.view.View.GONE
                    
                    val isParticipant = challenge.teamName == currentTeamName || challenge.acceptedByTeam == currentTeamName
                    if (isParticipant) {
                        if (onChat != null) {
                            binding.btnChat.visibility = android.view.View.VISIBLE
                            binding.btnChat.setOnClickListener { onChat.invoke(challenge) }
                        } else {
                            binding.btnChat.visibility = android.view.View.GONE
                        }
                        
                        if (onLiveScore != null) {
                            binding.btnLiveScore.visibility = android.view.View.VISIBLE
                            binding.btnLiveScore.setOnClickListener { onLiveScore.invoke(challenge) }
                        } else {
                            binding.btnLiveScore.visibility = android.view.View.GONE
                        }
                    } else {
                        binding.btnChat.visibility = android.view.View.GONE
                        binding.btnLiveScore.visibility = android.view.View.GONE
                    }
                }
                else -> {
                    binding.tvStatus.text = "CLOSED"
                    binding.tvStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.status_closed_bg))
                    binding.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_closed))
                    binding.btnAccept.visibility = android.view.View.VISIBLE
                    binding.btnAccept.isEnabled = false
                    binding.btnCancel.visibility = android.view.View.GONE
                    binding.btnChat.visibility = android.view.View.GONE
                    binding.btnLiveScore.visibility = android.view.View.GONE
                }
            }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Challenge>() {
            override fun areItemsTheSame(a: Challenge, b: Challenge) = a.challengeId == b.challengeId
            override fun areContentsTheSame(a: Challenge, b: Challenge) = a == b
        }
    }
}
