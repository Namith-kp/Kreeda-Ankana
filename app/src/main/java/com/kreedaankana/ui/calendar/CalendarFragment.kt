package com.kreedaankana.ui.calendar

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.kreedaankana.data.repository.BookingRepository
import com.kreedaankana.databinding.FragmentCalendarBinding
import com.kreedaankana.ui.booking.BookSlotActivity
import com.kreedaankana.utils.Extensions.formatDisplayDate
import com.kreedaankana.utils.Extensions.getTodayDate
import com.kreedaankana.utils.Extensions.gone
import com.kreedaankana.utils.Extensions.showToast
import com.kreedaankana.utils.Extensions.visible
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private val repository = BookingRepository()
    private lateinit var slotAdapter: SlotGridAdapter
    private var selectedDate = getTodayDate()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCalendar()
        setupRecyclerView()
        setupFab()
        loadBookings(selectedDate)
    }

    private fun setupCalendar() {
        binding.calendarView.minDate = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            selectedDate = sdf.format(cal.time)
            binding.tvSelectedDate.text = formatDisplayDate(selectedDate)
            loadBookings(selectedDate)
        }
        binding.tvSelectedDate.text = formatDisplayDate(selectedDate)
    }

    private fun setupRecyclerView() {
        slotAdapter = SlotGridAdapter()
        binding.rvSlots.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = slotAdapter
        }
    }

    private fun setupFab() {
        binding.fabBookSlot.setOnClickListener {
            startActivity(Intent(requireContext(), BookSlotActivity::class.java))
        }
    }

    private fun loadBookings(date: String) {
        binding.progressBar.visible()
        lifecycleScope.launch {
            val result = repository.getBookingsByDate(date)
            binding.progressBar.gone()
            result.onSuccess { bookings ->
                slotAdapter.submitList(bookings)
                if (bookings.isEmpty()) {
                    binding.tvEmpty.visible()
                    binding.rvSlots.gone()
                } else {
                    binding.tvEmpty.gone()
                    binding.rvSlots.visible()
                }
                binding.tvSlotCount.text = "${bookings.size} slot(s) booked"
            }.onFailure {
                requireContext().showToast("Failed to load bookings")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadBookings(selectedDate)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
