package com.marvelous.betterrank.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class NoteModel (
    var id: String = "",
    var userId: String="",
    var title: String="",
    var message: String="",
    var reminded:Boolean=false,
    var startAt: Long=0,
    var createdAt: Long? = System.currentTimeMillis(),
    var isExpanded: Boolean = false
): Parcelable