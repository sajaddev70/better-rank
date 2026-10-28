package com.marvelous.betterrank.ui

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import com.marvelous.betterrank.R
import com.marvelous.betterrank.databinding.PdfLanguageSelectionBinding
import com.marvelous.betterrank.viewModel.PdfLanguageViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class PdfLanguageSelection : BaseActivity() {

    private val binding: PdfLanguageSelectionBinding by lazy {
        DataBindingUtil.setContentView(this, R.layout.pdf_language_selection)
    }

    private val vm: PdfLanguageViewModel by viewModels()
    private var pendingPdf: File? = null

    private val translationBox get() = binding.translationBox
    private val homeId: String by lazy { intent.getStringExtra("homeId").orEmpty() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vm.setActivity(this)
        vm.loadHome(homeId)

        initView()
        initClicks()
        observeVm()
    }

    // ---------------------------------------------------------
    // INIT UI
    // ---------------------------------------------------------
    override fun initView() {
        binding.viewModel = vm
        binding.lifecycleOwner = this

        setupSpinner()
        setupOtherInput()


        vm.canSelectLanguage.observe(this) { enabled ->

            // Access the actual end-icon view
            val endIconView = binding.languageInputLayout
                .findViewById<View>(com.google.android.material.R.id.text_input_end_icon)

            if (!enabled) {

                // Cancel queued dropdown requests
                binding.spinnerLanguage.dismissDropDown()
                binding.spinnerLanguage.clearFocus()

                // Fully block spinner
                binding.spinnerLanguage.apply {
                    isEnabled = false
                    isClickable = false
                    isFocusable = false
                    isFocusableInTouchMode = false
                    isLongClickable = false
                    setOnTouchListener { _, _ -> true }
                }

                // Fully block TextInputLayout
                binding.languageInputLayout.apply {
                    isEnabled = false
                    isClickable = false
                    setOnTouchListener { _, _ -> true }
                }

                // BLOCK THE END ICON ITSELF (the missing piece)
                endIconView?.apply {
                    isClickable = false
                    isFocusable = false
                    isEnabled = false
                    setOnClickListener { } // remove internal click
                    setOnTouchListener { _, _ -> true } // consume touches
                }

                // Block entire area
                binding.leftHalf.setOnTouchListener { _, _ -> true }

            } else {

                // restore everything
                binding.spinnerLanguage.apply {
                    isEnabled = true
                    isClickable = true
                    isFocusable = true
                    isFocusableInTouchMode = true
                    isLongClickable = true
                    setOnTouchListener(null)
                }

                binding.languageInputLayout.apply {
                    isEnabled = true
                    isClickable = true
                    setOnTouchListener(null)
                }

                endIconView?.apply {
                    isClickable = true
                    isFocusable = true
                    isEnabled = true
                    setOnTouchListener(null)
                }

                binding.leftHalf.setOnTouchListener(null)

                restoreSpinnerAdapter()
            }
        }









    }

    override fun onClickView() {}

    private fun setupSpinner() {
        val list = resources.getStringArray(R.array.languages)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
        binding.spinnerLanguage.setAdapter(adapter)

        vm.selectedLanguage.value?.let {
            binding.spinnerLanguage.setText(it, false)
        }

        binding.spinnerLanguage.setOnItemClickListener { _, _, pos, _ ->
            val chosen = list[pos]
            val old = vm.selectedLanguage.value
            val finishedLang = vm.translatedLanguage.value

            vm.onLanguageSelected(chosen)



            // If leaving a translated language → hide box
            if (finishedLang != null && finishedLang != chosen) {
                hideTranslationBox()
            }

            updateButtonStates()
        }
    }

    private fun setupOtherInput() {
        val edit = binding.otherLanguageInput.findViewById<AppCompatEditText>(R.id.et_other_language)

        vm.selectedLanguage.observe(this) { lang ->
            binding.otherLanguageInput.visibility =
                if (lang == "Other") View.VISIBLE else View.GONE
        }

        vm.otherLanguage.observe(this) { t ->
            if (edit.text.toString() != t) {
                edit.setText(t)
                edit.setSelection(edit.text?.length ?: 0)
            }
        }

        edit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                vm.onOtherLanguageChanged(s?.toString() ?: "")
                updateButtonStates()
            }
        })
    }

    // ---------------------------------------------------------
    // OBSERVERS
    // ---------------------------------------------------------
    private fun observeVm() {

        vm.translationRemainingSeconds.observe(this) { sec ->
            translationBox.findViewById<TextView>(R.id.tvCounter).text = sec.toString()
        }

        vm.isTranslating.observe(this) { running ->
            val done = vm.translatedLanguage.value
            when {
                running -> showTranslatingMode()
                done != null -> showFinishedMode(done)
                else -> hideTranslationBox()
            }
            updateButtonStates()
        }

        vm.translatedLanguage.observe(this) { lang ->
            if (lang != null && vm.isTranslating.value == false) {
                showFinishedMode(lang)
            }
            updateButtonStates()
        }

        // ------------------------------
        // ★ IMPORTANT FIX (download)
        // ------------------------------
        vm.downloadPdf.observe(this) { file ->
            hideLoadingOverlay()     // hide FIRST
            savePdf(file)            // then save + toast
        }

        // ------------------------------
        // ★ IMPORTANT FIX (share)
        // ------------------------------
        vm.sharePdf.observe(this) { file ->
            hideLoadingOverlay()     // hide FIRST
            sharePdf(file)           // then open chooser
        }

        vm.showMessage.observe(this) {
            hideLoadingOverlay()
            showMessage(it)
        }

        vm.navigateBack.observe(this) { finish() }
    }

    // ---------------------------------------------------------
    // TRANSLATION BOX HANDLING
    // ---------------------------------------------------------
    private fun showTranslatingMode() {
        val lang = vm.getCurrentTargetLang() ?: ""
        translationBox.visibility = View.VISIBLE

        translationBox.findViewById<TextView>(R.id.tvMessage).text =
            "We're translating the home PDF guide to $lang.\nIt will take around 4 minutes.\nPlease Wait..."

        translationBox.findViewById<View>(R.id.tvCounter).visibility = View.VISIBLE

        binding.btnTranslate.isEnabled = false
        binding.btnSend.isEnabled = false
        binding.btnDownload.isEnabled = false
    }

    private fun showFinishedMode(lang: String) {
        translationBox.visibility = View.VISIBLE

        translationBox.findViewById<TextView>(R.id.tvMessage).text =
            "The home PDF guide in $lang is ready"

        translationBox.findViewById<View>(R.id.tvCounter).visibility = View.GONE

        binding.btnTranslate.isEnabled = false
        binding.btnDownload.isEnabled = true
        binding.btnSend.isEnabled = true
    }

    private fun hideTranslationBox() {
        translationBox.visibility = View.GONE
        updateButtonStates()
    }

    // ---------------------------------------------------------
    // BUTTON STATE LOGIC
    // ---------------------------------------------------------
    private fun updateButtonStates() {
        val selectedLang = vm.selectedLanguage.value ?: "English"
        val otherText = vm.otherLanguage.value.orEmpty()
        val isTranslating = vm.isTranslating.value == true
        val finishedLang = vm.translatedLanguage.value

        val isFinished = finishedLang != null && finishedLang == selectedLang
        val isEnglish = selectedLang == "English"

        val canTranslate =
            !isEnglish &&
                    !isTranslating &&
                    !isFinished &&
                    (selectedLang != "Other" || otherText.isNotBlank())

        binding.btnTranslate.isEnabled = canTranslate
        binding.btnTranslate.alpha = if (canTranslate) 1f else 0.3f

        val canAction = (!isTranslating && isEnglish) || isFinished

        listOf(binding.btnDownload, binding.btnSend).forEach { btn ->
            btn.isEnabled = canAction
            btn.alpha = if (canAction) 1f else 0.3f
        }
    }

    // ---------------------------------------------------------
    // CLICK EVENTS
    // ---------------------------------------------------------
    private fun initClicks() {
        binding.btnTranslate.setOnClickListener { vm.translate() }
        binding.btnDownload.setOnClickListener {
            showLoadingOverlay()
            vm.download()
        }

        binding.btnSend.setOnClickListener {
            showLoadingOverlay()
            vm.send()
        }

        binding.btnBack.setOnClickListener { vm.back() }
    }

    // ---------------------------------------------------------
    // PDF SAVE + SHARE
    // ---------------------------------------------------------
    private fun savePdf(file: File) {
        showMessage("PDF saved to: ${file.absolutePath}")

        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.READ_MEDIA_DOCUMENTS")
                != PackageManager.PERMISSION_GRANTED
            ) {
                pendingPdf = file
                requestPermissions(arrayOf("android.permission.READ_MEDIA_DOCUMENTS"), 500)
                return
            }
            saveToDownloadsApi29(file)
            return
        }

        if (Build.VERSION.SDK_INT >= 29) {
            saveToDownloadsApi29(file)
            return
        }

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            pendingPdf = file
            requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 501)
            return
        }

        saveToDownloadsLegacy(file)
    }

    private fun saveToDownloadsLegacy(file: File) {
        try {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val dest = File(dir, file.name)
            file.copyTo(dest, overwrite = true)
        } catch (e: Exception) {
            showMessage("Save failed")
        }
    }

    private fun saveToDownloadsApi29(file: File) {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let {
                contentResolver.openOutputStream(it)?.use { out ->
                    file.inputStream().use { input -> input.copyTo(out) }
                }
            }

        } catch (e: Exception) {
            showMessage("Save failed")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            pendingPdf?.let {
                savePdf(it)
                pendingPdf = null
            }
        } else {
            showMessage("Permission denied")
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun sharePdf(file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                this, "$packageName.fileprovider", file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            // chooser opens AFTER overlay already hidden
            startActivity(Intent.createChooser(intent, "Share PDF via"))

        } catch (e: Exception) {
            showMessage("Share failed")
        }
    }

    private fun restoreSpinnerAdapter() {
        val list = resources.getStringArray(R.array.languages)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, list)
        binding.spinnerLanguage.setAdapter(adapter)
    }


    // ---------------------------------------------------------
    // onResume — keep UI consistent
    // ---------------------------------------------------------
    override fun onResume() {
        super.onResume()
        updateButtonStates()
    }
    private fun showLoadingOverlay() {
        binding.downloadOverlay.visibility = View.VISIBLE
        binding.downloadProgress.visibility = View.VISIBLE
    }

    private fun hideLoadingOverlay() {
        binding.downloadOverlay.visibility = View.GONE
        binding.downloadProgress.visibility = View.GONE
    }

}
