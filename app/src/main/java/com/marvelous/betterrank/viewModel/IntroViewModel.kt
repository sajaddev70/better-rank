package com.marvelous.betterrank.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.marvelous.betterrank.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class IntroViewModel @Inject constructor(
    val appRepository: AppRepository
) : ViewModel() {

    val currentPosition = MutableLiveData(0)
    val showMessage = MutableLiveData<String>()

    private var onComplete: (() -> Unit)? = null

    fun setOnCompleteListener(listener: () -> Unit) {
        onComplete = listener
    }

    fun previousPage() {
        currentPosition.value = (currentPosition.value ?: 0).coerceAtLeast(0) - 1
    }

    fun nextPage() {
        val pos = currentPosition.value ?: 0
        if (pos < 2) {
            currentPosition.value = pos + 1
        } else {
            onComplete?.invoke()
        }
    }
}