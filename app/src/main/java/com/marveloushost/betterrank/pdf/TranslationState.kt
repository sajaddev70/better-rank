package com.marveloushost.betterrank.pdf

import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationState @Inject constructor() {

    val isTranslating = MutableStateFlow(false)
    val remainingSeconds = MutableStateFlow(0)
    val translatedLanguage = MutableStateFlow<String?>(null)
    val targetLanguage = MutableStateFlow<String?>(null)
}