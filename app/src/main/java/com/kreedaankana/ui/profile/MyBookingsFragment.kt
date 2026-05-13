package com.kreedaankana.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.kreedaankana.data.model.Booking
import com.kreedaankana.data.model.Challenge
import com.kreedaankana.data.repository.BookingRepository
import com.kreedaankana.data.repository.ChallengeRepository
import com.kreedaankana.databinding.FragmentMyBookingsBinding
import com.kreedaankana.ui.calendar.SlotGridAdapter
import com.kreedaankana.utils.Extensions.gone
import com.kreedaankana.utils.Extensions.showToast
import com.kreedaankana.utils.Extensions.visible
import com.kreedaankana.utils.SessionManager
import kotlinx.coroutines.launch

class MyBookingsFragment : Fragment() {

    private var _binding: FragmentMyBookingsBinding? = null
    private val binding get() = _binding!!
    private val repository = BookingRepository()
    private lateinit var adapter: SlotGridAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyBookingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        
        setupRecyclerView()
        loadMyBookings()
    }

    private fun setupRecyclerView() {
        adapter = SlotGridAdapter(onCancelClick = { booking ->
            showCancelConfirmation(booking)
        })
        binding.rvBookings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBookings.adapter = adapter
    }

    private fun showCancelConfirmation(booking: Booking) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Cancel Booking")
            .setMessage("Are you sure you want to cancel this booking?")
            .setPositiveButton("Yes, Cancel") { _, _ ->
                performCancellation(booking)
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun performCancellation(booking: Booking) {
        binding.progressBar.visible()
        lifecycleScope.launch {
            val result = repository.cancelBooking(booking.bookingId)
            binding.progressBar.gone()
            result.onSuccess {
                requireContext().showToast("Booking cancelled successfully")
                loadMyBookings() // Refresh list
            }.onFailure {
                requireContext().showToast("Failed to cancel booking")
            }
        }
    }

    private fun loadMyBookings() {
        val team = SessionManager.getTeam(requireContext())
        if (team == null) {
            binding.tvEmpty.text = "Register your team first to see bookings!"
            binding.tvEmpty.visible()
            return
        }

        binding.progressBar.visible()
        lifecycleScope.launch {
            val result = repository.getBookingsByTeam(team.id)
            binding.progressBar.gone()
            result.onSuccess { bookings ->
                adapter.submitList(bookings)
                if (bookings.isEmpty()) binding.tvEmpty.visible() else binding.tvEmpty.gone()
            }.onFailure {
                requireContext().showToast("Failed to load your bookings")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
