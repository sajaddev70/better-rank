package com.marveloushost.betterrank.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.marveloushost.betterrank.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReviewAnalysisViewModel @Inject constructor(
    appRepository: AppRepository,
    application: Application
) : BaseViewModel(appRepository, application) {

    private val _analysis = MutableLiveData<String>()
    val analysis: LiveData<String> = _analysis

    fun loadAnalysis(homeId: String) {
//        Log.d("REVANL", "loadAnalysis() called for homeId: $homeId")
        isLoading.value = true
        viewModelScope.launch {
            try {
                var analysis: String? = null
                var attempts = 0

                while (analysis.isNullOrBlank() && attempts < 3) {
                    val home = appRepository.getHome(homeId)
                    analysis = home?.reviewAnalysis
                    if (analysis.isNullOrBlank()) {
                        attempts++
//                        Log.d("REVANL", "Attempt $attempts: No analysis yet — retrying...")
                        delay(2000)
                    }
                }

                if (analysis.isNullOrBlank()) {
                    _analysis.value = "Analysis not ready. Please try again."
//                    Log.d("REVANL", "Final: No analysis after 3 retries")
                } else {
                    _analysis.value = analysis
//                    Log.d("REVANL", "Final: Analysis loaded successfully")
                }
            } catch (e: Exception) {
                _analysis.value = "Failed to load."
//                Log.e("REVANL", "Load failed", e)
            } finally {
                isLoading.value = false
            }
        }
    }
}