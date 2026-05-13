package com.kreedaankana.ui.tournaments

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.kreedaankana.R
import com.kreedaankana.data.model.Tournament

class TournamentsAdapter(
    private var tournaments: List<Tournament>,
    private val onItemClick: (Tournament) -> Unit
) : RecyclerView.Adapter<TournamentsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTournamentName: TextView = view.findViewById(R.id.tvTournamentName)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvSportAndOrganizer: TextView = view.findViewById(R.id.tvSportAndOrganizer)
        val tvPrizePool: TextView = view.findViewById(R.id.tvPrizePool)
        val tvRegisteredCount: TextView = view.findViewById(R.id.tvRegisteredCount)
        val progressTournament: LinearProgressIndicator = view.findViewById(R.id.progressTournament)
        val llProgressSection: LinearLayout = view.findViewById(R.id.llProgressSection)
        val llWinnerSection: LinearLayout = view.findViewById(R.id.llWinnerSection)
        val tvWinnerTeam: TextView = view.findViewById(R.id.tvWinnerTeam)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tournament, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tournament = tournaments[position]
        holder.tvTournamentName.text = tournament.name
        holder.tvStatus.text = tournament.status
        holder.tvSportAndOrganizer.text = "${getSportEmoji(tournament.sport)} ${tournament.sport} | Hosted by ${tournament.creatorTeamName.ifEmpty { "System" }}"

        if (tournament.prizePool.isNotEmpty()) {
            holder.tvPrizePool.text = "🎁 Prize Pool: ${tournament.prizePool}"
            holder.tvPrizePool.visibility = View.VISIBLE
        } else {
            holder.tvPrizePool.visibility = View.GONE
        }

        val registeredSize = tournament.registeredTeamIds.size
        holder.tvRegisteredCount.text = "$registeredSize / ${tournament.maxTeams} Teams"
        
        val progressPercent = (registeredSize * 100) / tournament.maxTeams
        holder.progressTournament.progress = progressPercent

        val ctx = holder.itemView.context
        // Configure Badge Color & visibility according to status
        when (tournament.status) {
            "REGISTRATION" -> {
                holder.tvStatus.text = "REGISTRATION"
                holder.tvStatus.setBackgroundResource(R.drawable.badge_booked)
                holder.tvStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.status_open_bg))
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_open))
                holder.llProgressSection.visibility = View.VISIBLE
                holder.llWinnerSection.visibility = View.GONE
            }
            "ACTIVE" -> {
                holder.tvStatus.text = "LIVE ACTIVE"
                holder.tvStatus.setBackgroundResource(R.drawable.badge_booked)
                holder.tvStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.status_accepted_bg))
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_accepted))
                holder.llProgressSection.visibility = View.GONE
                holder.llWinnerSection.visibility = View.GONE
            }
            "COMPLETED" -> {
                holder.tvStatus.text = "COMPLETED"
                holder.tvStatus.setBackgroundResource(R.drawable.badge_booked)
                holder.tvStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.status_closed_bg))
                holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_closed))
                holder.llProgressSection.visibility = View.GONE
                holder.llWinnerSection.visibility = View.VISIBLE
                holder.tvWinnerTeam.text = "🏆 Champion: ${tournament.winnerTeamName}"
            }
        }

        holder.itemView.setOnClickListener { onItemClick(tournament) }
    }

    override fun getItemCount(): Int = tournaments.size

    fun updateData(newTournaments: List<Tournament>) {
        tournaments = newTournaments
        notifyDataSetChanged()
    }

    private fun getSportEmoji(sport: String): String {
        return when (sport.lowercase()) {
            "cricket" -> "🏏"
            "football", "soccer" -> "⚽"
            "badminton" -> "🏸"
            "basketball" -> "🏀"
            "tennis" -> "🎾"
            "volleyball" -> "🏐"
            else -> "🏆"
        }
    }
}
