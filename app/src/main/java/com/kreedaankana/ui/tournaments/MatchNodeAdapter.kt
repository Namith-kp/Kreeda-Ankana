package com.kreedaankana.ui.tournaments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.kreedaankana.R
import com.kreedaankana.data.model.TournamentMatch

class MatchNodeAdapter(
    private var matches: List<TournamentMatch>,
    private val onMatchClick: (TournamentMatch) -> Unit
) : RecyclerView.Adapter<MatchNodeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rlTeam1: RelativeLayout = view.findViewById(R.id.rlTeam1)
        val tvTeam1Name: TextView = view.findViewById(R.id.tvTeam1Name)
        val tvTeam1Score: TextView = view.findViewById(R.id.tvTeam1Score)

        val rlTeam2: RelativeLayout = view.findViewById(R.id.rlTeam2)
        val tvTeam2Name: TextView = view.findViewById(R.id.tvTeam2Name)
        val tvTeam2Score: TextView = view.findViewById(R.id.tvTeam2Score)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_match_node, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val match = matches[position]
        
        val team1 = match.team1Name.ifEmpty { "TBD" }
        val team2 = match.team2Name.ifEmpty { "TBD" }

        holder.tvTeam1Name.text = team1
        holder.tvTeam2Name.text = team2

        val context = holder.itemView.context

        if (match.isCompleted) {
            holder.tvTeam1Score.text = match.score1.toString()
            holder.tvTeam2Score.text = match.score2.toString()

            // Winner visual indicators
            if (match.winnerName == match.team1Name && match.team1Name.isNotEmpty()) {
                holder.tvTeam1Name.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                holder.tvTeam1Score.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                holder.tvTeam2Name.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                holder.tvTeam2Score.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            } else if (match.winnerName == match.team2Name && match.team2Name.isNotEmpty()) {
                holder.tvTeam2Name.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                holder.tvTeam2Score.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                holder.tvTeam1Name.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
                holder.tvTeam1Score.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            } else {
                resetColors(holder, context)
            }
        } else {
            holder.tvTeam1Score.text = "-"
            holder.tvTeam2Score.text = "-"
            resetColors(holder, context)
        }

        holder.itemView.setOnClickListener {
            // Only allow clicking if both teams are ready/seeded
            if (match.team1Name.isNotEmpty() && match.team2Name.isNotEmpty()) {
                onMatchClick(match)
            }
        }
    }

    private fun resetColors(holder: ViewHolder, context: android.content.Context) {
        holder.tvTeam1Name.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        holder.tvTeam2Name.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        holder.tvTeam1Score.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
        holder.tvTeam2Score.setTextColor(ContextCompat.getColor(context, R.color.text_primary))
    }

    override fun getItemCount(): Int = matches.size

    fun updateData(newMatches: List<TournamentMatch>) {
        matches = newMatches
        notifyDataSetChanged()
    }
}
