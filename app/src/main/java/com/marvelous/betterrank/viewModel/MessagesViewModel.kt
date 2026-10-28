package com.marvelous.betterrank.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.marvelous.betterrank.model.GuestModel
import com.marvelous.betterrank.model.MessageSummaryModel
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject

@HiltViewModel
class MessagesViewModel @Inject constructor() : ViewModel() {

    private val db = Firebase.firestore
    private val userId: String
        get() = FirebaseAuth.getInstance().currentUser!!.uid

    // Original messages (loaded from Firestore)
    private val _messages = MutableLiveData<List<MessageSummaryModel>>()
    val messages: LiveData<List<MessageSummaryModel>> = _messages

    // Filtered messages shown in UI
    val searchQuery = MutableLiveData("")
    private val _filteredMessages = MutableLiveData<List<MessageSummaryModel>>()
    val filteredMessages: LiveData<List<MessageSummaryModel>> = _filteredMessages

    // Navigation to chat
    private val _openChat = MutableLiveData<GuestModel?>()
    val openChat: LiveData<GuestModel?> = _openChat

    fun openChat(guest: GuestModel) {
        _openChat.value = guest
    }

    fun clearOpenChat() {
        _openChat.value = null
    }

    fun loadMessages(guests: List<GuestModel>) {
//        Log.d("MSG", "Guests count = ${guests.size}")

        if (guests.isEmpty()) {
//            Log.d("MSG", "No guests → nothing to load")
            _messages.value = emptyList()
            _filteredMessages.value = emptyList()
            return
        }

        val result = mutableListOf<MessageSummaryModel>()
        var pending = guests.size

        guests.forEach { guest ->

//            Log.d("MSG", "Checking guest: ${guest.id} / ${guest.name}")

            db.collection("users").document(userId)
                .collection("guests").document(guest.id)
                .collection("messages")
                .orderBy("sentAt", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener { snap ->

//                    Log.d("MSG", "Found ${snap.size()} messages for guest ${guest.id}")

                    val doc = snap.documents.firstOrNull()
                    if (doc != null) {
                        val titles = (doc.get("titles") as? List<*>)?.filterIsInstance<String>()
                        val lastTitle = titles?.firstOrNull()
                        val lastTime = doc.getLong("sentAt")

//                        Log.d("MSG", "lastTitle = $lastTitle, lastTime = $lastTime")

                        result += MessageSummaryModel(
                            guest = guest,
                            lastTitle = lastTitle,
                            lastTime = lastTime
                        )
                    } else {
//                        Log.d("MSG", "No messages for ${guest.id}")
                        // ✅ STILL add guest, just with null fields
                        result += MessageSummaryModel(
                            guest = guest,
                            lastTitle = null,
                            lastTime = null
                        )
                    }
                }
                .addOnCompleteListener {
                    pending--
                    if (pending == 0) {
//                        Log.d("MSG", "Final list size = ${result.size}")
//                        result.forEach { Log.d("MSG", "-> ${it.guest.name}: ${it.lastTitle}") }

                        val sorted = result
                            .sortedByDescending { it.lastTime ?: 0L }  // guests with no msg at bottom

                        _messages.value = sorted
                        applyFilter()
                    }
                }
        }
    }






    /** Apply text filter */
    fun applyFilter() {
        val query = searchQuery.value?.trim()?.lowercase() ?: ""

        if (query.isEmpty()) {
            _filteredMessages.value = _messages.value
            return
        }

        val base = _messages.value ?: emptyList()

        _filteredMessages.value = base.filter { item ->
            val name = item.guest.name.lowercase()
            val last = item.lastTitle?.lowercase() ?: ""

            name.contains(query) || last.contains(query)
        }
    }

    fun onSearchTextChanged(text: String) {
        searchQuery.value = text
        applyFilter()
    }
}
