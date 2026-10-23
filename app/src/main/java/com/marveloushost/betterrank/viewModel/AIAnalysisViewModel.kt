package com.marveloushost.betterrank.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.marveloushost.betterrank.pdf.MutableSingleLiveEvent
import com.marveloushost.betterrank.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AIAnalysisViewModel @Inject constructor(
    override val appRepository: AppRepository,
    application: Application
) : BaseViewModel(appRepository, application) {

    private val _analysis = MutableLiveData<String>()
    val analysis: LiveData<String> = _analysis
    private val _finishActivity = MutableSingleLiveEvent<Unit>()
    val finishActivity: LiveData<Unit> = _finishActivity

    fun loadaiAnalysis(homeId: String) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val home = appRepository.getHome(homeId)
                val saved = home?.aiListingAnalysis
                if (saved.isNullOrBlank()) {
                    _analysis.value = "Analysis not ready. Try again in a few seconds."
                } else {
                    _analysis.value = saved
//                    Log.d("AI", "Loaded saved analysis")
                }
            } catch (e: Exception) {
                _analysis.value = "Failed"
            } finally {
                isLoading.value = false
            }
        }
    }
    fun finishActivity() = _finishActivity.call()
}