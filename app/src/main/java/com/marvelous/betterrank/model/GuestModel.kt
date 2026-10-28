package com.marvelous.betterrank.model

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

@Parcelize
data class GuestModel(
    @DocumentId val id: String = "",
    val name: String = "",
    val villaName: String = "",
    val checkIn: String = "",
    val checkOut: String = "",
    val nationality: String = "",
    val language: String = "",
    val otherLanguage: String? = null,
    val phoneNumber: String = "",
    val phoneCode: String = "",
    val hasChildren: Boolean = false,
    val notes: String = "",
    val lastMessage: String = "",
    val lastMessageTime: String = "",
    val adults: String? = null
) : Parcelable