package com.marvelous.betterrank.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.marvelous.betterrank.adapter.CalendarDayItem
import com.marvelous.betterrank.adapter.CalendarRowAdapter
import com.marvelous.betterrank.adapter.PillType
import com.marvelous.betterrank.databinding.FragmentCalendarBinding
import com.marvelous.betterrank.ui.Guests
import com.marvelous.betterrank.viewModel.GuestsViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private val viewModel by lazy { (parentFragment as Guests).viewModel }
    private val adapter = CalendarRowAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        binding.recyclerCalendar.adapter = adapter
        binding.recyclerCalendar.layoutManager = LinearLayoutManager(context)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.currentMonth.observe(viewLifecycleOwner) { month ->
            binding.tvMonthTitle.text = month.month.name.replaceFirstChar { it.uppercase() } + " " + month.year
        }

        viewModel.calendarEventsForMonth.observe(viewLifecycleOwner) { events ->
            val currentMonth = viewModel.currentMonth.value ?: LocalDate.now()
            val yearMonth = YearMonth.from(currentMonth)
            val daysInMonth = yearMonth.lengthOfMonth()

            val firstDayOfWeek = currentMonth.withDayOfMonth(1).get(ChronoField.DAY_OF_WEEK) % 7  // 0=Sun
            val items = mutableListOf<CalendarDayItem>()

            // Padding for first row (always build grid)
            repeat(firstDayOfWeek) { items.add(CalendarDayItem(0)) }

            // Always add all days, even if no events
            for (day in 1..daysInMonth) {
                val date = currentMonth.withDayOfMonth(day)

                val guest = events.firstOrNull { guest ->
                    val checkIn = LocalDate.parse(guest.checkIn, DateTimeFormatter.ISO_DATE_TIME)
                    val checkOut = LocalDate.parse(guest.checkOut, DateTimeFormatter.ISO_DATE_TIME).minusDays(1)

                    !date.isBefore(checkIn) && !date.isAfter(checkOut)
                }

                val pillType = if (guest == null) {
                    PillType.NONE
                } else {
                    val checkIn = LocalDate.parse(guest.checkIn, DateTimeFormatter.ISO_DATE_TIME)
                    val checkOut = LocalDate.parse(guest.checkOut, DateTimeFormatter.ISO_DATE_TIME).minusDays(1)

                    when {
                        date == checkIn && date == checkOut -> PillType.SINGLE
                        date == checkIn -> PillType.START
                        date == checkOut -> PillType.END
                        else -> PillType.MIDDLE
                    }
                }

                items.add(
                    CalendarDayItem(
                        dayNumber = day,
                        guestName = guest?.name,
                        pillType = pillType
                    )
                )
            }


            // Pad to multiple of 7
            while (items.size % 7 != 0) items.add(CalendarDayItem(0))

            val rows = items.chunked(7)
            adapter.submitList(rows)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}