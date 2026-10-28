package com.marvelous.betterrank.instructionstranslation

import android.content.Context
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationRepositoryInstructions @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // -------------------- STATE --------------------
    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating.asStateFlow()

    private val _countdown = MutableStateFlow(240)
    val countdown: StateFlow<Int> = _countdown.asStateFlow()

    private val _translatedText = MutableStateFlow<String?>(null)
    val translatedText: StateFlow<String?> = _translatedText.asStateFlow()

    private val _targetLanguage = MutableStateFlow<String?>(null)
    val targetLanguage: StateFlow<String?> = _targetLanguage.asStateFlow()

    private val _rawText = MutableStateFlow<String?>(null)
    val rawText: StateFlow<String?> = _rawText.asStateFlow()

    private val _currentGuestId = MutableStateFlow<String?>(null)
    val currentGuestId: StateFlow<String?> = _currentGuestId.asStateFlow()
    private val _selectedIndices = MutableStateFlow<List<Int>>(emptyList())
    val selectedIndices = _selectedIndices.asStateFlow()

    private val _selectedLanguage = MutableStateFlow<String?>(null)
    val selectedLanguage = _selectedLanguage.asStateFlow()


    // -------------------- PUBLIC API --------------------

    fun startTranslation(raw: String, lang: String, guestId: String) {
        _rawText.value = raw
        _targetLanguage.value = lang
        _translatedText.value = null
        _countdown.value = 240
        _isTranslating.value = true
        _currentGuestId.value = guestId


        saveState()
    }


    fun stopTranslation() {
        _isTranslating.value = false
        _rawText.value = null
        _targetLanguage.value = null
        _translatedText.value = null
        _countdown.value = 240
        _currentGuestId.value = null
        _selectedIndices.value = emptyList()
        _selectedLanguage.value = null

        SharedPrefsInstructions.clearState(context)
    }


    fun setCountdown(value: Int) {
        _countdown.value = value
        saveState()
    }

    fun setResult(translated: String) {
        _translatedText.value = translated
        saveState()
    }

    // Service helper for: repo.setRunning(lang)
    fun setRunning(lang: String) {
        _isTranslating.value = true
        _targetLanguage.value = lang
        saveState()
    }

    // Called by service BEFORE gpt starts (fetch raw selected English)
    fun getSelectedEnglishText(): String {
        return _rawText.value ?: ""
    }

    // Called by service AFTER GPT finishes
    fun saveTranslatedResult(result: String) {
        _translatedText.value = result
        saveState()
    }

    // Service calls after completion
    fun setDone() {
        _isTranslating.value = false
        saveState()
    }



    // -------------------- SHARED PREFS --------------------

    private fun saveState() {
        SharedPrefsInstructions.saveState(
            ctx = context,
            running = _isTranslating.value,
            countdown = _countdown.value,
            raw = _rawText.value,
            translated = _translatedText.value,
            lang = _targetLanguage.value,
            guestId = _currentGuestId.value,
            selectedIndices = _selectedIndices.value,
            selectedLanguage = _selectedLanguage.value
        )
    }

    fun isServiceActive(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        // Deprecated, but still returns the caller's own services, which is all we need here
        @Suppress("DEPRECATION")
        return manager.getRunningServices(Int.MAX_VALUE)
            .any { it.service.className == InstructionsTranslationService::class.java.name }
    }


    fun restoreState() {
        val running = SharedPrefsInstructions.readRunning(context)
        val translated = SharedPrefsInstructions.readTranslated(context)
        val guestId = SharedPrefsInstructions.readGuestId(context)

        // CASE A) Ready state → restore even if service is dead
        if (translated != null) {
            _isTranslating.value = false
            _translatedText.value = translated
            _currentGuestId.value = guestId
            _selectedIndices.value = SharedPrefsInstructions.readSelectedIndices(context)
            _selectedLanguage.value = SharedPrefsInstructions.readSelectedLanguage(context)
            _rawText.value = SharedPrefsInstructions.readRaw(context)
            _targetLanguage.value = SharedPrefsInstructions.readLang(context)
            _countdown.value = SharedPrefsInstructions.readCountdown(context)
            return
        }

        // CASE B) Nothing running and nothing translated → ignore
        if (!running) return

        // CASE C) Running but service dead (crash, reboot, killed by OS)
        if (!isServiceActive(context)) {
            // translation did NOT finish → reset
            stopTranslation()
            return
        }

        // CASE D) Service alive and translation in progress
        _isTranslating.value = true
        _translatedText.value = null
        _currentGuestId.value = guestId
        _countdown.value = SharedPrefsInstructions.readCountdown(context)
        _rawText.value = SharedPrefsInstructions.readRaw(context)
        _targetLanguage.value = SharedPrefsInstructions.readLang(context)
        _selectedIndices.value = SharedPrefsInstructions.readSelectedIndices(context)
        _selectedLanguage.value = SharedPrefsInstructions.readSelectedLanguage(context)
    }




    fun consumeTranslatedText() {
        _translatedText.value = null
        _selectedLanguage.value = null
        saveState()
    }
    fun setSelectedIndices(list: List<Int>) {
        _selectedIndices.value = list
        saveState()
    }

    fun setSelectedLanguage(lang: String?) {
        _selectedLanguage.value = lang
        saveState()
    }


}
