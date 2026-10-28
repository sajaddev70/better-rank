package com.marvelous.betterrank.viewModel

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.marvelous.betterrank.hilt.modual.HiltSharedPrefModule
import com.marvelous.betterrank.model.AirBnbResponseModel
import com.marvelous.betterrank.model.HomeModel
import com.marvelous.betterrank.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class HomeRegisterViewModel
@Inject constructor(
    appRepository: AppRepository,
    application: Application
) : BaseViewModel(appRepository, application) {

    var getAirbnbInfo = MutableLiveData<AirBnbResponseModel>()
    var createHome = MutableLiveData<Boolean>()

    fun validationAndGetAirbnbInfo(url: String) {
        val validationResult = validator.getAirbnbInfoValidation(url) // no name
        if (validationResult.isValid) {
            getAirbnbInfo(url)
        } else {
            showMessage.value = validationResult.error
        }
    }

    private fun getAirbnbInfo(url: String) {
//        Log.d("BRIGHTDATA_START", "Fetching data for URL: $url")

        viewModelScope.launch {
            isLoading.value = true
//            Log.d("BRIGHTDATA_LOADING", "Loading state set to TRUE")

            val result = kotlinx.coroutines.withTimeoutOrNull(65_000) {   // <-- ⏳ HARD TIMEOUT
                appRepository.getAirbnbInfo(url = url)
            }

            if (result == null) {
                // TIMEOUT OCCURRED
//                Log.e("BRIGHTDATA_TIMEOUT", "Request timed out after 65 seconds")
                showMessage.value = "Request timed out. Please try again."
                isLoading.value = false
                return@launch
            }

            // OTHERWISE → handle normal success/failure
            result.fold(
                onSuccess = { response ->
//                    Log.d("BRIGHTDATA_RESPONSE", "HTTP ${response.code()} received")

                    if (response.code() == 200) {
                        response.body()?.let { body ->
                            getAirbnbInfo.value = body
//                            Log.d("BRIGHTDATA_SUCCESS", "Data successfully parsed and stored")
                        } ?: run {
                            showMessage.value = "Empty data received"
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "No error body"
//                        Log.e("BRIGHTDATA_ERROR", "HTTP ${response.code()}\n$errorBody")
                        showMessage.value = when (response.code()) {
                            401, 403 -> "Airbnb service authorization failed. Please try again later."
                            else -> "Server error: ${response.code()}"
                        }
                    }
                },
                onFailure = { error ->
//                    Log.e("BRIGHTDATA_FAILURE", "Network or parsing failed", error)
                    errorHandler(error)
                }
            )

            isLoading.value = false
//            Log.d("BRIGHTDATA_LOADING", "Loading state set to FALSE")
        }
    }


    fun validationAndCreateHome(airBnbResponseModel: AirBnbResponseModel) {
        val homeModel = HomeModel(
            userId = appRepository.getDataSharedPref(HiltSharedPrefModule.UID),
            airBnbResponseModel = airBnbResponseModel
        )
        val validationResult = validator.createHomeValidation(homeModel)
        if (validationResult.isValid) {
            createHome(homeModel)
        } else {
            showMessage.value = validationResult.error
        }
    }

    private fun createHome(homeModel: HomeModel) {
        isLoading.value = true
        viewModelScope.launch {
            try {
                val home = appRepository.createHome(homeModel)
                appRepository.saveDataSharedPref(HiltSharedPrefModule.HOME, "1")
                createHome.value = true
            } catch (e: Exception) {
//                Log.e("BetterRankException", e.toString())
            } finally {
                isLoading.value = false
            }
        }
    }

    fun withoutQuery(url: String): String {
        val baseUrl = url.substringBefore("?").trim()
        return if (baseUrl.isNotEmpty()) {
            baseUrl
        } else {
            url
        }
    }

}