package com.marvelous.betterrank.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    fun formatMessageDate(timestamp: Long): String {
        val now = Calendar.getInstance()
        val msg = Calendar.getInstance().apply { timeInMillis = timestamp }

        val yearNow = now.get(Calendar.YEAR)
        val yearMsg = msg.get(Calendar.YEAR)

        val dayNow = now.get(Calendar.DAY_OF_YEAR)
        val dayMsg = msg.get(Calendar.DAY_OF_YEAR)

        return when {
            yearNow == yearMsg && dayNow == dayMsg ->
                "Today"

            yearNow == yearMsg && dayNow - dayMsg == 1 ->
                "Yesterday"

            else -> {
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }

    fun formatMessageTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
