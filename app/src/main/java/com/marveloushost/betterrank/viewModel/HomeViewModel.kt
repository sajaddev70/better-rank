package com.marveloushost.betterrank.viewModel

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.marveloushost.betterrank.customView.AirbnbPdfGenerator
import com.marveloushost.betterrank.hilt.modual.HiltSharedPrefModule
import com.marveloushost.betterrank.model.AirBnbResponseModel
import com.marveloushost.betterrank.model.CityGuideModel
import com.marveloushost.betterrank.model.HomeGuideModel
import com.marveloushost.betterrank.model.HomeModel
import com.marveloushost.betterrank.model.UserModel
import com.marveloushost.betterrank.pdf.MutableSingleLiveEvent
import com.marveloushost.betterrank.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class HomeViewModel @Inject constructor(appRepository: AppRepository, application: Application) :BaseViewModel(appRepository, application) {

    private val _resultGetUser = MutableLiveData<UserModel>()
    val resultGetUser: LiveData<UserModel> = _resultGetUser
    private val _resultGetHome = MutableLiveData<HomeModel>()
    val resultGetHome: LiveData<HomeModel> = _resultGetHome
    val resultDeleteUser = MutableLiveData<Boolean>()
    private val _cityGuideReady = MutableLiveData<Boolean>(false)
    private val _homeGuideReady = MutableLiveData<Boolean>(false)
    private val _aiAnalysis = MutableLiveData<String>()
    val aiAnalysis: LiveData<String> = _aiAnalysis
    private val _startAIAnalysis = MutableSingleLiveEvent<String>()
    val startAIAnalysis: LiveData<String> = _startAIAnalysis
    private var currentActivity: Activity? = null

    val bothGuidesReady = MediatorLiveData<Boolean>().apply {
        value = false
        addSource(_cityGuideReady) { value = it && (_homeGuideReady.value == true) }
        addSource(_homeGuideReady) { value = it && (_cityGuideReady.value == true) }
    }

    fun getUser(){
        isLoading.value = true
        viewModelScope.launch {
            try {
                val user = appRepository.getUser(appRepository.getDataSharedPref(HiltSharedPrefModule.UID))
                user?.let {
                    _resultGetUser.value=it
                }

            }catch (e:Exception){
//                Log.e("BetterRankException",e.toString())
            }finally {
                isLoading.value = false
            }
        }
    }

    fun getHomes() {
        isLoading.value = true
        viewModelScope.launch {
            try {
                val uid = appRepository.getDataSharedPref(HiltSharedPrefModule.UID)
                val homes = appRepository.getHomes(uid)

                if (homes.isNotEmpty()) {
                    val home = homes.last()
                    _resultGetHome.value = home

                    // Reset flags
                    _cityGuideReady.value = false
                    _homeGuideReady.value = false

                    // -------------------------------
                    // CITY GUIDE (only generate if missing)
                    // -------------------------------
                    viewModelScope.launch {
                        try {
                            if (home.cityGuide == null) {
//                                Log.d("GUIDE", "City Guide is missing → generating...")
                                val guide = appRepository.aiExtractCityGuide(home.airBnbResponseModel!!.toJson())
                                home.cityGuide = guide
                                appRepository.updateCityGuide(home.id, guide)
                            } else {
//                                Log.d("GUIDE", "City Guide already exists → skipping")
                            }
                        } finally {
                            _cityGuideReady.value = true
                        }
                    }

                    // -------------------------------
                    // HOME GUIDE (only generate if missing)
                    // -------------------------------
                    viewModelScope.launch {
                        try {
                            if (home.homeGuide == null) {
//                                Log.d("GUIDE", "Home Guide is missing → generating...")
                                val guide = appRepository.aiExtractHomeGuide(home.airBnbResponseModel!!.toJson())
                                home.homeGuide = guide
                                appRepository.updateHomeGuide(home.id, guide)
                            } else {
//                                Log.d("GUIDE", "Home Guide already exists → skipping")
                            }
                        } finally {
                            _homeGuideReady.value = true
                        }
                    }

                    // AI ANALYSIS — SAME PATTERN
                    viewModelScope.launch {
                        if (home.reviewAnalysis.isNullOrBlank()) {
                            try {
                                val analysis = appRepository.generateAIAnalysisPrompt(home.id)
                                appRepository.updateAIListingAnalysis(home.id, analysis)
//                                Log.d("AI", "AI analysis saved in getHomes()")
                            } catch (e: Exception) {
//                                Log.e("AI", "AI analysis failed", e)
                            }
                        }
                    }
                    // REVIEW ANALYSIS — MOVED HERE
                    analyzeAndSaveReviews(home)
                }
            } catch (e: Exception) {
//                Log.e("BetterRankException", e.toString())
            } finally {
                isLoading.value = false
            }
        }
    }
    private fun analyzeAndSaveReviews(home: HomeModel) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reviews = home.airBnbResponseModel?.reviewsDetails
                    ?: home.airBnbResponseModel?.reviews?.map {
                        AirBnbResponseModel.ReviewDetail(reviewText = it)
                    } ?: emptyList()

//                Log.d("REVANL", "Found ${reviews.size} reviews for home: ${home.id}")

                if (reviews.isEmpty()) {
//                    Log.d("REVANL", "No reviews to analyze")
                    return@launch
                }

                if (!home.reviewAnalysis.isNullOrBlank()) {
//                    Log.d("REVANL", "Review analysis already exists — skipping")
                    return@launch
                }

//                Log.d("REVANL", "Starting GPT review analysis...")
                val analysis = appRepository.analyzeReviews(reviews)
//                Log.d("REVANL", "GPT result: ${analysis.take(200)}...")

//                Log.d("REVANL", "Saving to Firestore...")
                appRepository.updateReviewAnalysis(home.id, analysis)
//                Log.d("REVANL", "Review analysis SAVED for home: ${home.id}")
            } catch (e: Exception) {
//                Log.e("REVANL", "Review analysis failed", e)
            }
        }
    }

    private fun aiExtractCityGuide(homeId: String,airbnbJson: String) {
        viewModelScope.launch {
            try {
                val cityGuide = appRepository.aiExtractCityGuide(airbnbJson)
                _resultGetHome.value?.cityGuide=cityGuide
                updateCityGuide(homeId,cityGuide)
            } catch (e: Exception) {
//                Log.e("BetterRankException", e.toString())
            }
        }
    }

    private fun updateCityGuide(homeId:String,cityGuideModel: CityGuideModel){
        isLoading.value = true
        viewModelScope.launch {
            try {
                val home=appRepository.updateCityGuide(homeId,cityGuideModel)
            } catch (e: Exception) {
//                Log.e("BetterRankException",e.toString())
            }finally {
                isLoading.value = false
            }
        }
    }

    private fun aiExtractHomeGuide(homeId: String,airbnbJson: String) {
        viewModelScope.launch {
            try {
                val homeGuide = appRepository.aiExtractHomeGuide(airbnbJson)
                _resultGetHome.value?.homeGuide=homeGuide
                updateHomeGuide(homeId,homeGuide)
            } catch (e: Exception) {
//                Log.e("BetterRankException", e.toString())
            }
        }
    }

    private fun updateHomeGuide(homeId:String,homeGuideModel: HomeGuideModel){
        isLoading.value = true
        viewModelScope.launch {
            try {
                val home=appRepository.updateHomeGuide(homeId,homeGuideModel)
            } catch (e: Exception) {
//                Log.e("BetterRankException",e.toString())
            }finally {
                isLoading.value = false
            }
        }
    }

    fun deleteUserData(){
        isLoading.value = true
        viewModelScope.launch {
            try {
                resultDeleteUser.value=appRepository.deleteUserData(appRepository.getDataSharedPref(HiltSharedPrefModule.UID))
            } catch (e: Exception) {
//                Log.e("BetterRankException",e.toString())
            }finally {
                isLoading.value = false
            }
        }
    }


    fun setActivity(activity: Activity) {
        this.currentActivity = activity
    }


    fun loadAIAnalysis(homeId: String) {
        viewModelScope.launch {
            try {
                val home = appRepository.getHome(homeId)
                _aiAnalysis.value = home?.aiListingAnalysis ?: "Analyzing..."
            } catch (e: Exception) {
                _aiAnalysis.value = "Failed"
            }
        }
    }
    fun generatePdf(homeId: String) {
        val activity = currentActivity ?: run {
//            Log.e("PDF", "Activity not available")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val home = appRepository.getHome(homeId) ?: return@launch

            if (home.cityGuide == null || home.homeGuide == null) {
//                Log.w("PDF", "Guides not ready")
                return@launch
            }

            // ---- LOAD PHOTOS FROM FIRESTORE ----
            val ctx = activity.applicationContext
            val photos = AirbnbPdfGenerator.loadAllPhotoSectionsFromFirestore(ctx)

            // ---- CACHE PHOTOS LOCALLY ----

            withContext(Dispatchers.Main) {

                AirbnbPdfGenerator.generatePdf(
                    activity = activity,
                    cityGuide = home.cityGuide!!,
                    homeGuide = home.homeGuide!!,
                    shortDescription = home.airBnbResponseModel?.detailsText,
                    appName = "Better Rank",
                    outputFileName = "AirbnbGuide-english.pdf",
                    sectionImages = photos,  // ← REQUIRED
                    onPdfReady = { file ->
//                        Log.d("PDF", "PDF SUCCESS → ${file.absolutePath}")
                    },
                    onError = { error ->
//                        Log.e("PDF", "PDF FAILED → $error")
                    }
                )
            }
        }
    }


}