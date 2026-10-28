package com.marvelous.betterrank.viewModel

import androidx.databinding.Bindable
import androidx.databinding.Observable
import androidx.databinding.PropertyChangeRegistry
import androidx.lifecycle.*
import com.marvelous.betterrank.BR
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.properties.Delegates.observable

@HiltViewModel
class AddGuestViewModel @Inject constructor() : ViewModel(), Observable {

    private val _isDateRangeValid = MutableLiveData(true)
    val isDateRangeValid: LiveData<Boolean> = _isDateRangeValid

    private val _dateError = MutableLiveData<String?>()
    val dateError: LiveData<String?> = _dateError

    private val callbacks = PropertyChangeRegistry()

    private var _fullName: String? = null
    @get:Bindable
    var fullName: String?
        get() = _fullName
        set(value) {
            if (_fullName != value) {
                _fullName = value?.trim()
                notifyPropertyChanged(BR.fullName)
                validateForm()
            }
        }

    private var _adults: String? = null
    @get:Bindable
    var adults: String?
        get() = _adults
        set(value) {
            if (_adults != value) {
                _adults = value?.trim()
                notifyPropertyChanged(BR.adults)
                validateForm()
            }
        }

    private var _location: String? = null
    @get:Bindable
    var location: String?
        get() = _location
        set(value) {
            if (_location != value) {
                _location = value?.trim()
                notifyPropertyChanged(BR.location)
                validateForm()
            }
        }

    private var _checkInDate: String? = null
    @get:Bindable
    var checkInDate: String?
        get() = _checkInDate
        set(value) {
            if (_checkInDate != value) {
                _checkInDate = value
                notifyPropertyChanged(BR.checkInDate)
                updateDuration()
                validateForm()
            }
        }

    private var _checkOutDate: String? = null
    @get:Bindable
    var checkOutDate: String?
        get() = _checkOutDate
        set(value) {
            if (_checkOutDate != value) {
                _checkOutDate = value
                notifyPropertyChanged(BR.checkOutDate)
                updateDuration()
                validateForm()
            }
        }

    private var _duration: String? = null
    @get:Bindable
    var duration: String?
        get() = _duration
        set(value) {
            if (_duration != value) {
                _duration = value?.filter { it.isDigit() }
                notifyPropertyChanged(BR.duration)
                updateDatesFromDuration()
                validateForm()
            }
        }

    private var _hasChildren: Boolean = false
    @get:Bindable
    var hasChildren: Boolean
        get() = _hasChildren
        set(value) {
            if (_hasChildren != value) {
                _hasChildren = value
                notifyPropertyChanged(BR.hasChildren)
            }
        }

    private var _language: String? = null
    @get:Bindable
    var language: String?
        get() = _language
        set(value) {
            if (_language != value) {
                _language = value
                notifyPropertyChanged(BR.language)
                _showOtherLanguage.value = value == "Other"
                validateForm()
            }
        }

    private var _otherLanguage: String? = null
    @get:Bindable
    var otherLanguage: String?
        get() = _otherLanguage
        set(value) {
            if (_otherLanguage != value) {
                _otherLanguage = value?.trim()
                notifyPropertyChanged(BR.otherLanguage)
            }
        }

    private val _showOtherLanguage = MutableLiveData<Boolean>(false)
    val showOtherLanguage: LiveData<Boolean> = _showOtherLanguage

    private var _phoneNumber: String? = null
    @get:Bindable
    var phoneNumber: String?
        get() = _phoneNumber
        set(value) {
            if (_phoneNumber != value) {
                _phoneNumber = value?.trim()
                notifyPropertyChanged(BR.phoneNumber)
                validateForm()
            }
        }

    private var _notes: String? = null
    @get:Bindable
    var notes: String?
        get() = _notes
        set(value) {
            if (_notes != value) {
                _notes = value?.trim()
                notifyPropertyChanged(BR.notes)
            }
        }
    @get:Bindable
    var nationality: String? = "United States"
        set(value) {
            if (field != value) {
                field = value
                notifyPropertyChanged(BR.nationality)
                validateForm()
            }
        }

    @get:Bindable
    var phoneCode: String? = "+1"
        set(value) {
            if (field != value) {
                field = value
                notifyPropertyChanged(BR.phoneCode)
            }
        }

    private val _isSubmitting = MutableLiveData<Boolean>(false)
    val isSubmitting: LiveData<Boolean> = _isSubmitting

    private val _showMessage = MutableLiveData<String?>()
    val showMessage: LiveData<String?> = _showMessage

    private val _isFormValid = MutableLiveData<Boolean>(false)
    val isFormValid: LiveData<Boolean> = _isFormValid

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")

//    init {
//        val today = LocalDate.now()
//        val tomorrow = today.plusDays(1)
//        _checkInDate = today.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
//        _checkOutDate = tomorrow.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))
//        notifyPropertyChanged(BR.checkInDate)
//        notifyPropertyChanged(BR.checkOutDate)
//        validateForm()
//    }

    private fun updateDuration() {
        val inDate = _checkInDate ?: return
        val outDate = _checkOutDate ?: return

        try {
            val checkIn = LocalDate.parse(inDate, dateFormatter)
            val checkOut = LocalDate.parse(outDate, dateFormatter)

            if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
                _duration = null
                notifyPropertyChanged(BR.duration)

                _isDateRangeValid.value = false
                _dateError.value = "Invalid checkout date"

                validateForm()
                return
            }

            val nights = ChronoUnit.DAYS.between(checkIn, checkOut).toInt()
            _duration = nights.toString()
            notifyPropertyChanged(BR.duration)

            _isDateRangeValid.value = true
            _dateError.value = null

            validateForm()

        } catch (e: Exception) {
            _isDateRangeValid.value = false
            _dateError.value = "Invalid date"
        }
    }


    private fun updateDatesFromDuration() {
        val inDate = _checkInDate ?: return
        val dur = _duration?.toIntOrNull() ?: return
        if (dur <= 0) return
        try {
            val checkIn = LocalDate.parse(inDate, dateFormatter)
            val checkOut = checkIn.plusDays(dur.toLong())
            val newOut = checkOut.format(dateFormatter)
            if (_checkOutDate != newOut) {
                _checkOutDate = newOut
                notifyPropertyChanged(BR.checkOutDate)
                validateForm()
            }
        } catch (e: Exception) { /* ignore */ }
    }


    private fun isValidDuration(): Boolean {
        val dur = _duration?.toIntOrNull() ?: 0
        return dur > 0
    }

    private fun validateForm(): Boolean {
        val valid = _fullName?.isNotBlank() == true &&
                _checkInDate?.isNotBlank() == true &&
                _checkOutDate?.isNotBlank() == true &&
                _phoneNumber?.isNotBlank() == true &&
                nationality?.isNotBlank() == true &&
                _language?.isNotBlank() == true &&
                _adults?.isNotBlank() == true &&
                (_language != "Other" || _otherLanguage?.isNotBlank() == true) &&
                isValidDuration() &&
                (_isDateRangeValid.value == true)

        _isFormValid.value = valid
        return valid
    }


    fun submitGuest() {
//        android.util.Log.d("AddGuestVM", "submitGuest() called")
        if (_isSubmitting.value == true) {
//            android.util.Log.d("AddGuestVM", "Already submitting, ignoring.")
            return
        }
        _isSubmitting.value = true
//        android.util.Log.d("AddGuestVM", "isSubmitting = true")

        if (!validateForm()) {
//            android.util.Log.d("AddGuestVM", "Validation failed")
            _isSubmitting.value = false
            return
        }

//        android.util.Log.d("AddGuestVM", "Validation passed. Emitting success.")
        _showMessage.value = "Guest added successfully!"
        _isSubmitting.value = false
    }


    internal fun formatDateForISO(date: String): String {
        return try {
            val parsed = LocalDate.parse(date, dateFormatter)
            parsed.format(DateTimeFormatter.ISO_LOCAL_DATE) + "T12:00:00Z"
        } catch (e: Exception) { date.replace("/", "-") + "T12:00:00Z" }
    }

    fun clearMessage() { _showMessage.value = null }

    // === Observable Implementation ===
    override fun addOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback) {
        callbacks.add(callback)
    }

    override fun removeOnPropertyChangedCallback(callback: Observable.OnPropertyChangedCallback) {
        callbacks.remove(callback)
    }

    private fun notifyPropertyChanged(fieldId: Int) {
        callbacks.notifyCallbacks(this, fieldId, null)
    }
}