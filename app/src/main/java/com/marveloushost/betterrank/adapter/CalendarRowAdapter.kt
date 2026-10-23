package com.marveloushost.betterrank.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.marveloushost.betterrank.databinding.ItemCalendarRowBinding
import com.marveloushost.betterrank.R

enum class PillType { NONE, START, MIDDLE, END, SINGLE }

data class CalendarDayItem(
    val dayNumber: Int,
    val guestName: String? = null,
    val pillType: PillType = PillType.NONE
)

// Adapter for week rows (7 days each)
class CalendarRowAdapter : ListAdapter<List<CalendarDayItem>, CalendarRowAdapter.ViewHolder>(RowDiffCallback()) {

    class ViewHolder(val binding: ItemCalendarRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCalendarRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val d = getItem(position)
        val b = holder.binding

        // Bind day numbers
        bindDay(b.dayCard1, b.tvDayNumber1, d.getOrNull(0))
        bindDay(b.dayCard2, b.tvDayNumber2, d.getOrNull(1))
        bindDay(b.dayCard3, b.tvDayNumber3, d.getOrNull(2))
        bindDay(b.dayCard4, b.tvDayNumber4, d.getOrNull(3))
        bindDay(b.dayCard5, b.tvDayNumber5, d.getOrNull(4))
        bindDay(b.dayCard6, b.tvDayNumber6, d.getOrNull(5))
        bindDay(b.dayCard7, b.tvDayNumber7, d.getOrNull(6))

        // Bind pills overlay row
        bindPill(b.pill1, d.getOrNull(0))
        bindPill(b.pill2, d.getOrNull(1))
        bindPill(b.pill3, d.getOrNull(2))
        bindPill(b.pill4, d.getOrNull(3))
        bindPill(b.pill5, d.getOrNull(4))
        bindPill(b.pill6, d.getOrNull(5))
        bindPill(b.pill7, d.getOrNull(6))

        // Hide separators only inside a continuous bar
        b.separator1.visibility = if (connects(d.getOrNull(0), d.getOrNull(1))) View.INVISIBLE else View.VISIBLE
        b.separator2.visibility = if (connects(d.getOrNull(1), d.getOrNull(2))) View.INVISIBLE else View.VISIBLE
        b.separator3.visibility = if (connects(d.getOrNull(2), d.getOrNull(3))) View.INVISIBLE else View.VISIBLE
        b.separator4.visibility = if (connects(d.getOrNull(3), d.getOrNull(4))) View.INVISIBLE else View.VISIBLE
        b.separator5.visibility = if (connects(d.getOrNull(4), d.getOrNull(5))) View.INVISIBLE else View.VISIBLE
        b.separator6.visibility = if (connects(d.getOrNull(5), d.getOrNull(6))) View.INVISIBLE else View.VISIBLE
    }

    private fun connects(a: CalendarDayItem?, b: CalendarDayItem?): Boolean {
        if (a == null || b == null) return false
        if (a.guestName.isNullOrEmpty() || a.guestName != b.guestName) return false

        return when (a.pillType) {
            PillType.START -> (b.pillType == PillType.MIDDLE || b.pillType == PillType.END)
            PillType.MIDDLE -> (b.pillType == PillType.MIDDLE || b.pillType == PillType.END)
            else -> false
        }
    }







    private fun bindDay(
        card: androidx.cardview.widget.CardView,
        tvDay: TextView,
        item: CalendarDayItem?
    ) {
        if (item == null || item.dayNumber <= 0) {
            card.visibility = View.INVISIBLE
            return
        }

        card.visibility = View.VISIBLE
        tvDay.text = item.dayNumber.toString()
    }

    private fun bindPill(pill: TextView, item: CalendarDayItem?) {
        pill.visibility = View.VISIBLE  // <-- ALWAYS VISIBLE
        pill.text = ""

        if (item == null || item.pillType == PillType.NONE) {
            pill.setBackgroundColor(Color.TRANSPARENT)
            return
        }

        when (item.pillType) {
            PillType.SINGLE -> {
                pill.setBackgroundResource(R.drawable.pill_single)
                pill.setTextColor(Color.WHITE)
                pill.text = item.guestName ?: ""
            }
            PillType.START -> {
                pill.setBackgroundResource(R.drawable.pill_left)
                pill.setTextColor(Color.WHITE)
                pill.text = item.guestName ?: ""
            }
            PillType.MIDDLE -> {
                pill.setBackgroundResource(R.drawable.pill_middle)
            }
            PillType.END -> {
                pill.setBackgroundResource(R.drawable.pill_right)
            }

            PillType.NONE -> TODO()
        }
    }






    private class RowDiffCallback : DiffUtil.ItemCallback<List<CalendarDayItem>>() {
        override fun areItemsTheSame(old: List<CalendarDayItem>, new: List<CalendarDayItem>): Boolean =
            old === new

        override fun areContentsTheSame(old: List<CalendarDayItem>, new: List<CalendarDayItem>): Boolean =
            old == new
    }
}