package com.kreedaankana.ui.calendar

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kreedaankana.R
import com.kreedaankana.data.model.Booking
import com.kreedaankana.databinding.ItemSlotBinding
import com.kreedaankana.utils.Extensions.formatTimeRange

class SlotGridAdapter(
    private val onCancelClick: ((Booking) -> Unit)? = null
) : ListAdapter<Booking, SlotGridAdapter.SlotViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val binding = ItemSlotBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SlotViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SlotViewHolder(private val binding: ItemSlotBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(booking: Booking) {
            binding.tvTeamName.text = booking.teamName
            binding.tvSport.text = "⚽ ${booking.sport}"
            if (booking.venuePlace.isNotEmpty()) {
                binding.tvVenuePlace.visibility = android.view.View.VISIBLE
                binding.tvVenuePlace.text = booking.venuePlace
            } else {
                binding.tvVenuePlace.visibility = android.view.View.GONE
            }
            binding.tvTimeRange.text = formatTimeRange(booking.startTime, booking.endTime)
            binding.tvStatus.text = "BOOKED"

            if (onCancelClick != null) {
                binding.btnCancel.visibility = android.view.View.VISIBLE
                binding.btnCancel.setOnClickListener { onCancelClick.invoke(booking) }
            } else {
                binding.btnCancel.visibility = android.view.View.GONE
            }
        }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Booking>() {
            override fun areItemsTheSame(a: Booking, b: Booking) = a.bookingId == b.bookingId
            override fun areContentsTheSame(a: Booking, b: Booking) = a == b
        }
    }
}
