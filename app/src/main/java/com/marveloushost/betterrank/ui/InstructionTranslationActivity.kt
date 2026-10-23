package com.marveloushost.betterrank.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.marveloushost.betterrank.databinding.ActivityInstructionTranslationBinding
import com.marveloushost.betterrank.instructionstranslation.InstructionsTranslationService
import com.marveloushost.betterrank.instructionstranslation.TranslationRepositoryInstructions
import com.marveloushost.betterrank.model.GuestModel
import com.marveloushost.betterrank.model.HomeGuideModel
import com.marveloushost.betterrank.viewModel.SendInstructionsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class InstructionTranslationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInstructionTranslationBinding
    private val vm: SendInstructionsViewModel by viewModels()

    @Inject lateinit var repo: TranslationRepositoryInstructions

    private lateinit var guest: GuestModel
    private lateinit var homeGuide: HomeGuideModel

    private var listenersEnabled = false   // 🔥 Key fix

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityInstructionTranslationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        guest = intent.getParcelableExtra("guest")!!
        homeGuide = intent.getParcelableExtra("guide")!!

        binding.vm = vm
        binding.guestName = guest.name
        binding.guestLanguage = guest.language ?: "Guest Language"

        // restore repo state first
        repo.restoreState()
//        Log.e("DEBUG", "Restored translated = ${repo.translatedText.value}")

        listenersEnabled = false

        setupLanguage()
        setupButtons()
        restoreSelections()
        restoreStateUI()

// Now UI is correct
        listenersEnabled = true

// AFTER enabling listeners
        addInputListeners()
        setupObservers()
    }

    // -------------------------------------------------------------------------
    // LANGUAGE SETUP
    // -------------------------------------------------------------------------
    private fun setupLanguage() {
        val guestLang = guest.language ?: "English"

        if (repo.selectedLanguage.value == null) {
            binding.rbEnglish.isChecked = true
            repo.setSelectedLanguage("English")
        }

        if (guestLang.equals("english", true)) {
            binding.rgLanguage.visibility = View.GONE
        } else {
            binding.rgLanguage.visibility = View.VISIBLE
            binding.rbGuestLang.text = guestLang
        }
    }

    // -------------------------------------------------------------------------
    // BUTTONS
    // -------------------------------------------------------------------------
    private fun setupButtons() {
        binding.btnTranslate.setOnClickListener { startTranslation() }
        binding.btnSend.setOnClickListener { sendResult() }
        binding.btnBack.setOnClickListener { finish() }
    }

    // -------------------------------------------------------------------------
    // RESTORE UI
    // -------------------------------------------------------------------------
    private fun restoreStateUI() {
        val sameGuest = repo.currentGuestId.value == guest.id

        when {
            repo.isTranslating.value && sameGuest -> enterTranslatingMode()
            repo.translatedText.value != null && sameGuest -> enterReadyMode(repo.translatedText.value!!)
            else -> binding.translationBoxRoot.isVisible = false
        }

        updateButtons()
    }

    private fun restoreSelections() {
        val boxes = getCheckboxList()

        repo.selectedIndices.value.forEach { idx ->
            if (idx in boxes.indices) boxes[idx].isChecked = true
        }

        repo.selectedLanguage.value?.let {
            if (it == "English") binding.rbEnglish.isChecked = true
            else binding.rbGuestLang.isChecked = true
        }
    }

    // -------------------------------------------------------------------------
    // START TRANSLATION
    // -------------------------------------------------------------------------
    private fun startTranslation() {
        val raw = vm.buildMessage(homeGuide, binding)
        val lang = binding.rbGuestLang.text.toString()

        Intent(this, InstructionsTranslationService::class.java).apply {
            putExtra("rawText", raw)
            putExtra("lang", lang)
            putExtra("guestId", guest.id)
            startService(this)
        }

        enterTranslatingMode()
    }

    // -------------------------------------------------------------------------
    // SEND RESULT
    // -------------------------------------------------------------------------
    private fun sendResult() {
        val text = repo.translatedText.value ?: vm.buildMessage(homeGuide, binding)
        val titles = getSelectedTitles()

        repo.stopTranslation()

        val result = Intent().apply {
            putExtra("messageText", text)
            putExtra("titles", ArrayList(titles))
        }
        setResult(Activity.RESULT_OK, result)
        finish()
    }

    // -------------------------------------------------------------------------
    // OBSERVERS
    // -------------------------------------------------------------------------
    private fun setupObservers() {

        lifecycleScope.launch {
            repo.countdown.collect {
                if (repo.isTranslating.value)
                    binding.tvCounter.text = it.toString()
            }
        }

        lifecycleScope.launch {
            repo.translatedText.collect { result ->
                if (result != null) enterReadyMode(result)
            }
        }

        lifecycleScope.launch {
            repo.isTranslating.collect {
                binding.translationBoxRoot.isVisible = it || repo.translatedText.value != null
                updateButtons()
            }
        }

        // CHECKBOX + RADIO LISTENERS (AFTER listenersEnabled = true)
        addInputListeners()
    }

    // -------------------------------------------------------------------------
    // LISTENERS
    // -------------------------------------------------------------------------
    private fun addInputListeners() {

        binding.rgLanguage.setOnCheckedChangeListener { _, _ ->
            if (!listenersEnabled) return@setOnCheckedChangeListener

            val lang = if (binding.rbEnglish.isChecked) "English" else guest.language
            repo.setSelectedLanguage(lang)

            if (repo.translatedText.value != null) clearReadyState()
            updateButtons()
        }

        getCheckboxList().forEach { cb ->
            cb.setOnCheckedChangeListener { _, _ ->
                if (!listenersEnabled) return@setOnCheckedChangeListener

                val selected = getCheckboxList().mapIndexedNotNull { idx, cb2 ->
                    if (cb2.isChecked) idx else null
                }
                repo.setSelectedIndices(selected)

                if (repo.translatedText.value != null) clearReadyState()
                updateButtons()
            }
        }
    }

    // -------------------------------------------------------------------------
    // MODES
    // -------------------------------------------------------------------------
    private fun enterTranslatingMode() {
        showTranslationBox()

        disableInputs()
        binding.tvMessage.text = "Translating…"

        binding.btnTranslate.isEnabled = false
        binding.btnTranslate.alpha = 0.3f

        binding.btnSend.isEnabled = false
        binding.btnSend.alpha = 0.3f
    }

    private fun enterReadyMode(result: String) {
        showTranslationBox()

        enableInputs()
        binding.tvMessage.text = "Translation ready"
        binding.tvCounter.isVisible = false

        binding.btnTranslate.isEnabled = false
        binding.btnTranslate.alpha = 0.3f

        binding.btnSend.isEnabled = true
        binding.btnSend.alpha = 1f
    }

    private fun clearReadyState() {
        repo.consumeTranslatedText()
        binding.translationBoxRoot.isVisible = false
        updateButtons()
    }

    private fun showTranslationBox() {
        binding.translationBoxRoot.isVisible = true
        binding.tvCounter.isVisible = true
    }

    // -------------------------------------------------------------------------
    // INPUT ENABLE/DISABLE
    // -------------------------------------------------------------------------
    private fun disableInputs() {
        getCheckboxList().forEach { it.isEnabled = false }
        binding.rbEnglish.isEnabled = false
        binding.rbGuestLang.isEnabled = false
    }

    private fun enableInputs() {
        getCheckboxList().forEach { it.isEnabled = true }
        binding.rbEnglish.isEnabled = true
        binding.rbGuestLang.isEnabled = true
    }

    // -------------------------------------------------------------------------
    // BUTTON ENABLE LOGIC
    // -------------------------------------------------------------------------
    private fun updateButtons() {
        val any = getCheckboxList().any { it.isChecked }
        val guestLang = binding.rbGuestLang.isChecked
        val english = binding.rbEnglish.isChecked
        val hasResult = repo.translatedText.value != null
        val running = repo.isTranslating.value

        binding.btnSend.isEnabled = any && (english || (guestLang && hasResult))
        binding.btnSend.alpha = if (binding.btnSend.isEnabled) 1f else 0.3f

        binding.btnTranslate.isEnabled = any && guestLang && !hasResult && !running
        binding.btnTranslate.alpha = if (binding.btnTranslate.isEnabled) 1f else 0.3f
    }

    // -------------------------------------------------------------------------
    // UTILS
    // -------------------------------------------------------------------------
    private fun getCheckboxList() = listOf(
        binding.cb1, binding.cb2, binding.cb3, binding.cb4, binding.cb5,
        binding.cb6, binding.cb7, binding.cb8, binding.cb9
    )

    private fun getSelectedTitles(): List<String> {
        val titles = listOf(
            "Host & Welcome", "Contact Info", "Property Basics",
            "Address & Access", "Check-in & Access", "House Rules",
            "Check-out", "Amenities", "Neighborhood Info"
        )
        val boxes = getCheckboxList()
        return titles.filterIndexed { idx, _ -> boxes[idx].isChecked }
    }
}
