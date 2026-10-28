package com.marvelous.betterrank.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.marvelous.betterrank.pdf.MutableSingleLiveEvent
import com.marvelous.betterrank.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class ReviewsViewModel @Inject constructor(
    override val appRepository: AppRepository,
    application: Application
) : BaseViewModel(appRepository, application) {

    private val _navigateToAnalysis = MutableSingleLiveEvent<String>()
    val navigateToAnalysis: LiveData<String> = _navigateToAnalysis

    fun analyzeReviews(homeId: String) {
        viewModelScope.launch {
            setLoading(true) // internal LiveData if needed

            delay(10000) // simulate network like Home fragment

            _navigateToAnalysis.value = homeId

            setLoading(false)
        }
    }

}