package com.marveloushost.betterrank.model

data class MessageSummaryModel(
    val guest: GuestModel,
    val lastTitle: String?,
    val lastTime: Long?
)