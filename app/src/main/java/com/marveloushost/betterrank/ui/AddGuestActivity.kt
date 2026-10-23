package com.marveloushost.betterrank.ui

import android.app.DatePickerDialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.databinding.ActivityAddGuestBinding
import com.marveloushost.betterrank.generated.callback.OnClickListener
import com.marveloushost.betterrank.model.GuestModel
import com.marveloushost.betterrank.viewModel.AddGuestViewModel
import com.marveloushost.betterrank.viewModel.GuestsViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class AddGuestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddGuestBinding
    private val addGuestViewModel: AddGuestViewModel by viewModels()
    private lateinit var guestsViewModel: GuestsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddGuestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        guestsViewModel = ViewModelProvider(this)[GuestsViewModel::class.java]

        binding.viewModel = addGuestViewModel
        binding.lifecycleOwner = this


        setupBackButton()
        setupDatePickers()
        setupLanguageDropdown()
        setupNationalitySync()
        setupOtherLanguageStub()
        observeSubmitResult()
        applyExtractedNationality(
            intent.getStringExtra("AUTO_NATIONALITY"),
            intent.getStringExtra("AUTO_NATIONALITY_CODE")
        )

        intent.getStringExtra("AUTO_NAME")?.let { addGuestViewModel.fullName = it }
        intent.getStringExtra("AUTO_IN")?.let { addGuestViewModel.checkInDate = it }
        intent.getStringExtra("AUTO_OUT")?.let { addGuestViewModel.checkOutDate = it }
        intent.getStringExtra("AUTO_ADULTS")?.let { addGuestViewModel.adults = it }
        intent.getBooleanExtra("AUTO_HAS_CHILDREN", false).let { addGuestViewModel.hasChildren = it }
        intent.getStringExtra("AUTO_LANGUAGE")?.let {
            applyExtractedLanguage(it)
        }
        intent.getStringExtra("AUTO_PHONE_CODE")?.let {
            addGuestViewModel.phoneCode = it
            binding.ccpContact.setCountryForPhoneCode(it.replace("+", "").toIntOrNull() ?: return@let)
        }
        intent.getStringExtra("AUTO_PHONE_NUMBER")?.let { addGuestViewModel.phoneNumber = it }
        intent.getStringExtra("AUTO_NOTES")?.let { addGuestViewModel.notes = it }

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
                addGuestViewModel.checkInDate = date.format(formatter)
            }
        }

        binding.spinnerCheckOut.setOnClickListener {
            showDatePicker() { date ->
                binding.spinnerCheckOut.setText(date.format(formatter), false)
                addGuestViewModel.checkOutDate = date.format(formatter)
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
            addGuestViewModel.language = selected
        }
    }
    private fun applyExtractedLanguage(rawLanguage: String) {
        val normalized = rawLanguage.trim().lowercase()

        val mapped = when (normalized) {
            "farsi" -> "Persian"
            else -> rawLanguage
        }

        val supported = resources.getStringArray(R.array.languages)

        if (supported.contains(mapped)) {
            // Exact match → select it
            addGuestViewModel.language = mapped
            binding.spinnerLanguage.setText(mapped, false)
        } else {
            // Not supported → use Other
            addGuestViewModel.language = "Other"
            binding.spinnerLanguage.setText("Other", false)
            addGuestViewModel.otherLanguage = rawLanguage
        }
    }


    private fun setupNationalitySync() {
        binding.ccpNationality.setOnCountryChangeListener {
            val country = binding.ccpNationality.selectedCountryName
            addGuestViewModel.nationality = country
            binding.ccpContact.setCountryForNameCode(binding.ccpNationality.selectedCountryNameCode)
        }

        binding.ccpContact.setOnCountryChangeListener {
            addGuestViewModel.phoneCode = binding.ccpContact.selectedCountryCodeWithPlus
        }
    }

    private var otherLanguageEditText: TextInputEditText? = null

    private fun setupOtherLanguageStub() {
        addGuestViewModel.showOtherLanguage.observe(this) { show ->
            if (show) {
                if (otherLanguageEditText == null) {
                    val container = binding.otherLanguageContainer
                    val inflated = container.getChildAt(0) as TextInputLayout
                    otherLanguageEditText = inflated.findViewById(R.id.etOtherLanguage)

                    addGuestViewModel.otherLanguage?.let {
                        otherLanguageEditText?.text = Editable.Factory.getInstance().newEditable(it)
                    }

                    otherLanguageEditText?.addTextChangedListener(object : TextWatcher {
                        override fun afterTextChanged(s: Editable?) {
                            addGuestViewModel.otherLanguage = s?.toString()
                        }

                        override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                        ) {
                        }

                        override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                        ) {
                        }
                    })
                }
            } else {
                otherLanguageEditText = null
                addGuestViewModel.otherLanguage = null
            }
        }
    }


    private fun observeSubmitResult() {
        addGuestViewModel.showMessage.observe(this) { msg ->
            msg?.let {
                addGuestViewModel.clearMessage()

                val guest = GuestModel(
                    id = System.currentTimeMillis().toString(),
                    name = addGuestViewModel.fullName.orEmpty(),
                    villaName = addGuestViewModel.location.orEmpty(),
                    checkIn = addGuestViewModel.formatDateForISO(addGuestViewModel.checkInDate.orEmpty()),
                    checkOut = addGuestViewModel.formatDateForISO(addGuestViewModel.checkOutDate.orEmpty()),
                    nationality = addGuestViewModel.nationality.orEmpty(),
                    language = (if (addGuestViewModel.language == "Other") addGuestViewModel.otherLanguage else addGuestViewModel.language).toString(),
                    otherLanguage = addGuestViewModel.otherLanguage,
                    phoneNumber = addGuestViewModel.phoneNumber.orEmpty(),
                    phoneCode = addGuestViewModel.phoneCode.orEmpty(),
                    hasChildren = addGuestViewModel.hasChildren,
                    notes = addGuestViewModel.notes.orEmpty(),
                    adults = addGuestViewModel.adults.orEmpty()
                )

                guestsViewModel.addGuest(guest)
                finish()

            }
        }
    }
    private fun applyExtractedNationality(name: String?, code: String?) {
        val iso2 = code?.trim()?.uppercase()

        // 1) Prefer ISO code (most reliable)
        if (!iso2.isNullOrBlank() && iso2.length == 2) {
            binding.ccpNationality.setCountryForNameCode(iso2)
            addGuestViewModel.nationality = binding.ccpNationality.selectedCountryName

            // Optional: keep phone picker in sync too
            binding.ccpContact.setCountryForNameCode(iso2)
            addGuestViewModel.phoneCode = binding.ccpContact.selectedCountryCodeWithPlus
            return
        }

        // 2) Fallback: try to derive ISO code from country name using Locale list
        val countryName = name?.trim().orEmpty()
        if (countryName.isBlank()) return

        val matchedIso = java.util.Locale.getISOCountries().firstOrNull { iso ->
            java.util.Locale("", iso).displayCountry.equals(countryName, ignoreCase = true)
        }

        if (matchedIso != null) {
            binding.ccpNationality.setCountryForNameCode(matchedIso)
            addGuestViewModel.nationality = binding.ccpNationality.selectedCountryName

            binding.ccpContact.setCountryForNameCode(matchedIso)
            addGuestViewModel.phoneCode = binding.ccpContact.selectedCountryCodeWithPlus
        } else {
            // Couldn’t map -> at least store the text in VM
            addGuestViewModel.nationality = countryName
        }
    }

}