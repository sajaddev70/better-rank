package com.marveloushost.betterrank.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.marveloushost.betterrank.model.HomeGuideModel
import com.marveloushost.betterrank.model.HomeModel
import com.marveloushost.betterrank.pdf.PdfGenerationTrigger
import com.marveloushost.betterrank.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeGuideViewModel @Inject constructor(
    override val appRepository: AppRepository,
    application: Application
) : BaseViewModel(appRepository, application) {

    private val _resultGetHome = MutableLiveData<HomeModel>()
    val resultGetHome = _resultGetHome

    private val _resultUpdateHomeGuide = MutableLiveData<HomeGuideModel>()
    val resultUpdateHomeGuide = _resultUpdateHomeGuide

    val currentPosition = MutableLiveData(0)
    val totalPages = 9

    private val _isLastPage = MutableLiveData<Boolean>()
    val isLastPage: LiveData<Boolean> = _isLastPage

    val isLastPageBoolean: Boolean
        get() = (currentPosition.value ?: 0) >= totalPages - 1

    init {
        currentPosition.observeForever { pos ->
            _isLastPage.value = (pos ?: 0) >= totalPages - 1
        }
    }

    private val _onCompleteTriggered = MutableLiveData<Unit>()
    val onCompleteTriggered: LiveData<Unit> = _onCompleteTriggered

    fun previousPage() {
        val pos = currentPosition.value ?: 0
        if (pos > 0) {
            currentPosition.value = pos - 1
        }
    }


    fun nextPage() {
        val pos = currentPosition.value ?: 0
        if (pos < totalPages - 1) {
            currentPosition.value = pos + 1
        } else {
            currentPosition.value = totalPages - 1
            _onCompleteTriggered.value = Unit
        }
    }



    fun getHome(homeId: String) {
        isLoading.value = true
        viewModelScope.launch {
            try {
                val home = appRepository.getHome(homeId) ?: return@launch
                _resultGetHome.value = home
                _resultUpdateHomeGuide.value = home.homeGuide ?: HomeGuideModel()
//                _resultUpdateHomeGuide.value = home.homeGuide!!  // Already exists
            } catch (e: Exception) {
                showMessage.value = "Failed to load"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun toggleContactChannel(channel: String) {
        val guide = _resultUpdateHomeGuide.value ?: return
        val list = guide.contactChannels.toMutableList()

        if (list.contains(channel)) list.remove(channel) else list.add(channel)
        guide.contactChannels = list

        _resultUpdateHomeGuide.value = guide
    }


    internal fun updateHomeGuide(homeId: String, homeGuideModel: HomeGuideModel) {
        if (homeId.isBlank()) return

        viewModelScope.launch {
            try {
                appRepository.updateHomeGuide(homeId, homeGuideModel)
                triggerPdfGeneration()
            } catch (e: Exception) {
//                Log.e("BetterRankException", e.toString())
            }
        }
    }

    fun onCityGuideUpdated(homeId: String) {
        viewModelScope.launch {
            triggerPdfGeneration()
        }
    }

    internal fun triggerPdfGeneration() {
        val homeId = _resultGetHome.value?.id ?: return
        if (homeId.isNotBlank()) {
            PdfGenerationTrigger.trigger(homeId)
        }
    }

    fun setParking(value: String) {
        val guide = resultUpdateHomeGuide.value ?: return
        guide.parking = value
        resultUpdateHomeGuide.value = guide
    }


    fun setPropertyType(value: String) {
        val guide = resultUpdateHomeGuide.value ?: return
        guide.propertyType = value
        resultUpdateHomeGuide.value = guide
    }


    val currentPositionText: String
        get() = "${currentPosition.value?.plus(1) ?: 1} of $totalPages"
}