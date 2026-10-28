package com.marvelous.betterrank.viewModel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.marvelous.betterrank.model.GuestModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class GuestsViewModel @Inject constructor(
    application: Application,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore
    private val userId: String
        get() = auth.currentUser?.uid ?: throw IllegalStateException("No user")
    private val guestsRef = db.collection("users").document(userId).collection("guests")
    private val _guests = MutableLiveData<List<GuestModel>>(emptyList())
    val guests: LiveData<List<GuestModel>> = _guests
    private val _viewMode = MutableLiveData(ViewMode.BY_GUESTS)
    val viewMode: LiveData<ViewMode> = _viewMode
    private val _showMessage = MutableLiveData<String?>()
    val showMessage: LiveData<String?> = _showMessage
    var searchQuery: MutableLiveData<String> = MutableLiveData("")
        private set
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    private val _isEmpty = MutableLiveData(false)
    val isEmpty: LiveData<Boolean> = _isEmpty
    private val _openAddGuestScreen = MutableLiveData<Unit>()
    val openAddGuestScreen: LiveData<Unit> = _openAddGuestScreen
    private val _currentMonth = MutableLiveData(LocalDate.now().withDayOfMonth(1))
    val currentMonth: LiveData<LocalDate> = _currentMonth
    private val _navigateToEdit = MutableLiveData<String?>()
    val navigateToEdit: LiveData<String?> = _navigateToEdit
    private val _navigateToMessage = MutableLiveData<GuestModel?>()
    val navigateToMessage: LiveData<GuestModel?> = _navigateToMessage


    init {
        listenToGuests()
    }

    val filteredGuests = guests.switchMap { allGuests ->
        searchQuery.switchMap { query ->
            val filtered = if (query.isBlank()) {
                allGuests
            } else {
                allGuests.filter {
                    it.name.contains(query, true) ||
                            it.villaName.contains(query, true)
                }
            }
            MutableLiveData(filtered)
        }
    }

    val isEmptyFiltered = filteredGuests.switchMap { list ->
        MutableLiveData(list.isEmpty())
    }


    private fun listenToGuests() {
        guestsRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
//                Log.w("GuestsVM", "Listen failed.", e)
                return@addSnapshotListener
            }
            val list = snapshot?.toObjects(GuestModel::class.java) ?: emptyList()
            // SORT: newest first (id = timestamp)
            val sorted = list.sortedByDescending { it.id }
            _guests.postValue(sorted)
        }
    }


    fun setViewMode(mode: ViewMode) {
        if (_viewMode.value != mode) {
            _viewMode.value = mode
        }
    }

    enum class ViewMode { CALENDAR, BY_GUESTS }

    // Search Query



    fun setSearchQuery(query: String) {
        searchQuery.value = query.trim()
    }

    // Simulate loading
    init {
        _isLoading.value = true
        // Fake delay
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            _isLoading.value = false
            _isEmpty.value = guests.value?.isEmpty() ?: true
        }, 800)
    }

    fun loadGuests() {
        // Just refresh mock data
        _isLoading.value = true
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            _guests.value = _guests.value?.shuffled()  // shuffle for fun
            _isLoading.value = false
        }, 600)
    }
    public fun openAddGuestScreen() {
        _openAddGuestScreen.value = Unit
    }

    fun nextMonth() {
        _currentMonth.value = _currentMonth.value?.plusMonths(1)
    }

    fun prevMonth() {
        _currentMonth.value = _currentMonth.value?.minusMonths(1)
    }

    // Filtered events for current month (after guests & currentMonth)
    val calendarEventsForMonth = currentMonth.switchMap { month ->
        MutableLiveData(
            guests.value?.filter { guest ->
                val checkIn = LocalDate.parse(guest.checkIn, DateTimeFormatter.ISO_DATE_TIME)
                val checkOut = LocalDate.parse(guest.checkOut, DateTimeFormatter.ISO_DATE_TIME)
                val monthStart = month.withDayOfMonth(1)
                val monthEnd = monthStart.plusMonths(1).minusDays(1)
                // Overlap: !(end before month || start after month)
                !(checkOut.isBefore(monthStart) || checkIn.isAfter(monthEnd))
            } ?: emptyList()
        )
    }

    // For calendar mode — use month-filtered events
    val calendarEvents = calendarEventsForMonth



    fun clearSearchQuery() {
        searchQuery.value = ""
    }
    fun addGuest(guest: GuestModel) {
        val docRef = guestsRef.document(guest.id)
        docRef.set(guest)
            .addOnSuccessListener {
//                Log.d("GuestsVM", "Guest saved")
            }
            .addOnFailureListener { e ->
//                Log.e("GuestsVM", "Save failed", e)
                // Keep local copy even if Firestore fails
                val current = _guests.value?.toMutableList() ?: mutableListOf()
                if (!current.any { it.id == guest.id }) {
                    current.add(0, guest)
                    _guests.postValue(current)
                }
            }
    }
    fun onEditClick(guestId: String) {
        _navigateToEdit.value = guestId
    }
    fun clearNavigateToEdit() {
        _navigateToEdit.value = null
    }
    fun updateGuest(guest: GuestModel) {
        guestsRef.document(guest.id).set(guest)
            .addOnSuccessListener { Log.d("GuestsVM", "Guest updated") }
            .addOnFailureListener { Log.e("GuestsVM", "Update failed", it) }
    }


    fun onMessageClick(guest: GuestModel){
        _navigateToMessage.value = guest
    }
    fun clearNavigateToMessage(){
        _navigateToMessage.value = null
    }



}