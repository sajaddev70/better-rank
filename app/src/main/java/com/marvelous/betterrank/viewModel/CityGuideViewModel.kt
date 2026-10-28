package com.marvelous.betterrank.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.marvelous.betterrank.model.CityGuideModel
import com.marvelous.betterrank.model.HomeModel
import com.marvelous.betterrank.pdf.PdfGenerationTrigger
import com.marvelous.betterrank.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class CityGuideViewModel @Inject constructor(
    appRepository: AppRepository,
    application: Application
) : BaseViewModel(appRepository, application) {

    private val _resultGetHome = MutableLiveData<HomeModel>()
    val resultGetHome = _resultGetHome
    private val _resultUpdateCityGuide = MutableLiveData<CityGuideModel>()
    val resultUpdateCityGuide = _resultUpdateCityGuide

//    fun getHome(homeId: String) {
//        if (homeId.isBlank()) {
//            showMessage.value = "Invalid home ID"
//            isLoading.value = false
//            return
//        }
//
//        isLoading.value = true
//        viewModelScope.launch {
//            try {
//                val home = appRepository.getHome(homeId)
//                home?.let {
//                    _resultGetHome.value = it
//                    if (it.cityGuide == null) {
//                        aiExtractCityGuide(it.id, it.airBnbResponseModel!!.toJson())
//                    } else {
//                        _resultUpdateCityGuide.value = it.cityGuide!!
//                        isLoading.value = false
//                    }
//                } ?: run {
//                    showMessage.value = "Home not found"
//                    isLoading.value = false
//                }
//            } catch (e: Exception) {
//                Log.e("BetterRankException", e.toString())
//                showMessage.value = "Failed to load home"
//                isLoading.value = false
//            }
//        }
//    }

//    private fun aiExtractCityGuide(homeId: String, airbnbJson: String) {
//        viewModelScope.launch {
//            try {
//                val cityGuide = appRepository.aiExtractCityGuide(airbnbJson)
//                _resultUpdateCityGuide.value = cityGuide
//                updateCityGuide(homeId, cityGuide)
//            } catch (e: Exception) {
//                Log.e("BetterRankException", e.toString())
//                showMessage.value = "AI extraction failed"
//            } finally {
//                isLoading.value = false
//            }
//        }
//    }

    fun getHome(homeId: String) {
        isLoading.value = true
        viewModelScope.launch {
            try {
                val home = appRepository.getHome(homeId) ?: return@launch
                _resultGetHome.value = home
                _resultUpdateCityGuide.value = home.cityGuide ?: CityGuideModel()
//                _resultUpdateCityGuide.value = home.cityGuide!!  // Already exists
            } catch (e: Exception) {
                showMessage.value = "Failed to load"
            } finally {
                isLoading.value = false
            }
        }
    }

    fun updateCityGuide(homeId: String, cityGuideModel: CityGuideModel) {
        if (homeId.isBlank()) return

        viewModelScope.launch {
            try {
                appRepository.updateCityGuide(homeId, cityGuideModel)
                triggerPdfGeneration()
            } catch (e: Exception) {
//                Log.e("BetterRankException", e.toString())
                showMessage.value = "Save failed"
            }
        }
    }

    fun triggerPdfGeneration() {
        val homeId = _resultGetHome.value?.id ?: return
        if (homeId.isNotBlank()) {
            PdfGenerationTrigger.trigger(homeId)
        }
    }
}