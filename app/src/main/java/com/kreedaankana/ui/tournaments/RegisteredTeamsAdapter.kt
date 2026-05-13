package com.kreedaankana.ui.tournaments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kreedaankana.R

class RegisteredTeamsAdapter(
    private var teamNames: List<String>
) : RecyclerView.Adapter<RegisteredTeamsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSeedRank: TextView = view.findViewById(R.id.tvSeedRank)
        val tvTeamName: TextView = view.findViewById(R.id.tvTeamName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_registered_team, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val teamName = teamNames[position]
        holder.tvSeedRank.text = (position + 1).toString()
        holder.tvTeamName.text = teamName
    }

    override fun getItemCount(): Int = teamNames.size

    fun updateData(newTeamNames: List<String>) {
        teamNames = newTeamNames
        notifyDataSetChanged()
    }
}
