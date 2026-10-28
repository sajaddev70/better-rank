package com.marvelous.betterrank.pdf

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object PdfGenerationTrigger {
    private val _trigger = MutableLiveData<String>()
    val trigger: LiveData<String> = _trigger

    fun trigger(homeId: String) {
        _trigger.value = homeId
    }
}