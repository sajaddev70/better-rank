package com.marveloushost.betterrank.model

data class MessageHistoryModel(
    var id: String = "",
    var guestId: String = "",
    var titles: List<String> = emptyList(),   // ["Host & Welcome", "Address & Access"]
    var sentAt: Long = System.currentTimeMillis()
)




sealed class MessageItem {
    data class DateSeparator(val date: String) : MessageItem()
    data class MessageBubble(
        val title: String,
        val timestamp: Long
    ) : MessageItem()
}


