package com.marveloushost.betterrank.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.marveloushost.betterrank.R
import androidx.recyclerview.widget.RecyclerView
import com.marveloushost.betterrank.model.MessageItem
import com.marveloushost.betterrank.model.MessageSummaryModel
import com.marveloushost.betterrank.util.DateUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessagesAdapter(
    private val onClick: (MessageSummaryModel) -> Unit
) : RecyclerView.Adapter<MessagesAdapter.VH>() {

    private var items: List<MessageSummaryModel> = emptyList()

    fun submitList(list: List<MessageSummaryModel>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name = v.findViewById<TextView>(R.id.tvGuestName)
        val last = v.findViewById<TextView>(R.id.tvLastMessage)
        val time = v.findViewById<TextView>(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message_summary, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, pos: Int) {
        val item = items[pos]

        holder.name.text = item.guest.name
        holder.last.text = item.lastTitle ?: "No messages yet"

        val ts = item.lastTime
        if (ts != null) {
            holder.time.text =
                "${DateUtils.formatMessageDate(ts)} • ${DateUtils.formatMessageTime(ts)}"
        } else {
            holder.time.text = ""   // ✅ clear recycled value
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }


    override fun getItemCount() = items.size
}

