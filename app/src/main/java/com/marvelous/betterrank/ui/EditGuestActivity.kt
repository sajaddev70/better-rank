package com.marvelous.betterrank.ui

import android.app.DatePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.marvelous.betterrank.BR.guest
import com.marvelous.betterrank.R
import com.marvelous.betterrank.databinding.ActivityEditGuestBinding
import com.marvelous.betterrank.model.GuestModel
import com.marvelous.betterrank.viewModel.EditGuestViewModel
import com.marvelous.betterrank.viewModel.GuestsViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class EditGuestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditGuestBinding
    private val editViewModel: EditGuestViewModel by viewModels()

    private lateinit var guestsViewModel: GuestsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySystemBarInsets()
//        Log.e("EditGuest", "onCreate")
        binding = ActivityEditGuestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        guestsViewModel = ViewModelProvider(this)[GuestsViewModel::class.java]
        binding.viewModel = editViewModel
        binding.lifecycleOwner = this

        val guestId = intent.getStringExtra("GUEST_ID") ?: run {
//            Log.e("EditGuest", "No GUEST_ID")
            finish()
            return
        }

        // Wait for guest to appear in list
        guestsViewModel.guests.observe(this) { allGuests ->
            val guest = allGuests.find { it.id == guestId }
            if (guest != null) {
                editViewModel.loadGuest(guest)
//                Log.d("EditGuest", "Guest loaded: ${guest.name}")
                // Optional: remove observer after loading
                guestsViewModel.guests.removeObservers(this)
            }
        }


        // Set default nationality from CCP
        editViewModel.nationality = binding.ccpNationality.selectedCountryName

        setupBackButton()
        setupDatePickers()
        setupLanguageDropdown()
        setupNationalitySync()
        setupOtherLanguageStub()
        observeSubmitResult()
    }

    private fun setupBackButton() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun setupDatePickers() {
        val formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        val today = LocalDate.now()

        binding.spinnerCheckIn.setOnClickListener {
            showDatePicker() { date ->
                binding.spinnerCheckIn.setText(date.format(formatter), false)
                editViewModel.checkInDate = date.format(formatter)
            }
        }

        binding.spinnerCheckOut.setOnClickListener {
            showDatePicker() { date ->
                binding.spinnerCheckOut.setText(date.format(formatter), false)
                editViewModel.checkOutDate = date.format(formatter)
            }
        }

        // Enable click
        binding.spinnerCheckIn.isClickable = true
        binding.spinnerCheckOut.isClickable = true
    }

    private fun showDatePicker(onDateSet: (LocalDate) -> Unit) {
        val today = LocalDate.now()
        val picker = DatePickerDialog(this, { _, year, month, day ->
            onDateSet(LocalDate.of(year, month + 1, day))
        }, today.year, today.monthValue - 1, today.dayOfMonth)


        picker.show()
    }

    private fun setupLanguageDropdown() {
        val languages = resources.getStringArray(R.array.languages)
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, languages)
        binding.spinnerLanguage.setAdapter(adapter)
        binding.spinnerLanguage.setOnItemClickListener { _, _, position, _ ->
            val selected = languages[position]
            editViewModel.language = selected
        }
    }

    private fun setupNationalitySync() {
        binding.ccpNationality.setOnCountryChangeListener {
            val country = binding.ccpNationality.selectedCountryName
            editViewModel.nationality = country
            binding.ccpContact.setCountryForNameCode(binding.ccpNationality.selectedCountryNameCode)
        }

        binding.ccpContact.setOnCountryChangeListener {
            editViewModel.phoneCode = binding.ccpContact.selectedCountryCodeWithPlus
        }
    }

    private var otherLanguageEditText: TextInputEditText? = null

    private fun setupOtherLanguageStub() {
        editViewModel.showOtherLanguage.observe(this) { show ->
            if (show) {
                if (otherLanguageEditText == null) {
                    val container = binding.otherLanguageContainer
                    val inflated = container.getChildAt(0) as TextInputLayout
                    otherLanguageEditText = inflated.findViewById(R.id.etOtherLanguage)

                    editViewModel.otherLanguage?.let {
                        otherLanguageEditText?.text = Editable.Factory.getInstance().newEditable(it)
                    }

                    otherLanguageEditText?.addTextChangedListener(object : TextWatcher {
                        override fun afterTextChanged(s: Editable?) {
                            editViewModel.otherLanguage = s?.toString()
                        }
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    })
                }
            } else {
                otherLanguageEditText = null
                editViewModel.otherLanguage = null
            }
        }
    }


    private fun observeSubmitResult() {
        editViewModel.guestUpdated.observe(this) { updatedGuest ->
            guestsViewModel.updateGuest(updatedGuest)
            editViewModel.clearMessage()
            finish()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
//        Log.e("EditGuest", "onDestroy")
    }

}