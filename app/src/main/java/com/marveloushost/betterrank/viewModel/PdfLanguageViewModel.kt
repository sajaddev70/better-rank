package com.marveloushost.betterrank.viewModel

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import android.webkit.WebView
import androidx.lifecycle.*
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.customView.AirbnbPdfGenerator
import com.marveloushost.betterrank.model.CityGuideModel
import com.marveloushost.betterrank.model.HomeGuideModel
import com.marveloushost.betterrank.model.HomeModel
import com.marveloushost.betterrank.pdf.MutableSingleLiveEvent
import com.marveloushost.betterrank.pdf.TranslationForegroundService
import com.marveloushost.betterrank.repository.AppRepository
import com.marveloushost.betterrank.repository.TranslationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PdfLanguageViewModel @Inject constructor(
    override val appRepository: AppRepository,
    application: Application,
    private val repo: TranslationRepository
) : BaseViewModel(appRepository, application) {

    // ---------------------------------------------------------
    // HOME DATA
    // ---------------------------------------------------------
    private val _home = MutableLiveData<HomeModel>()
    val home: LiveData<HomeModel> = _home

    private var currentActivity: Activity? = null
    fun setActivity(act: Activity) { currentActivity = act }

    // ---------------------------------------------------------
// TRANSLATION STATE (from Repository)
// ---------------------------------------------------------
    val isTranslating = repo.state.map { it.isRunning }.asLiveData()
    val translationRemainingSeconds = repo.state.map { it.secondsLeft }.asLiveData()
    val translatedLanguage = repo.state.map { it.finishedLang }.asLiveData()

    // ---------------------------------------------------------
// LANGUAGE SELECTOR ENABLE/DISABLE CONTROL
// ---------------------------------------------------------
    val canSelectLanguage = MediatorLiveData<Boolean>().apply {
        addSource(isTranslating) { running ->
            value = !running
        }
        addSource(translatedLanguage) {
            value = true
        }
    }
    val isLoadingPhotos = MutableLiveData(false)



    fun loadHome(homeId: String) {
        viewModelScope.launch {
            appRepository.getHome(homeId)?.let { _home.value = it }
        }
    }

    // ---------------------------------------------------------
    // LANGUAGE UI STATE
    // ---------------------------------------------------------
    val selectedLanguage = MutableLiveData(repo.loadLastLanguage() ?: "English")
    val otherLanguage = MutableLiveData("")
    val isEnglishSelected = selectedLanguage.map { it == "English" }

    fun onLanguageSelected(lang: String) {
        val old = selectedLanguage.value

        selectedLanguage.value = lang
        repo.saveLastLanguage(lang)

        if (old != null && old != lang) {
            resetTranslationState()
        }
    }


    fun onOtherLanguageChanged(text: String) { otherLanguage.value = text }

    fun getCurrentTargetLang(): String? {
        val lang = selectedLanguage.value ?: return null
        return if (lang == "Other") {
            otherLanguage.value?.takeIf { it.isNotBlank() }
        } else lang
    }


    // ---------------------------------------------------------
    // TRANSLATE BUTTON → START FOREGROUND SERVICE
    // ---------------------------------------------------------
    fun translate() {
//        Log.d("yamete", "translate() clicked")

        val lang = getCurrentTargetLang() ?: run {
//            Log.d("yamete", "translate() → targetLang = null")
            return
        }
//        Log.d("yamete", "translate() targetLang = $lang")

        val act = currentActivity ?: run {
//            Log.d("yamete", "translate() → activity = null")
            return
        }

        val h = home.value
        if (h == null) {
//            Log.d("yamete", "translate() → home is NULL")
            showMessage.postValue("Home not loaded yet")
            return
        }

//        Log.d("yamete", "translate() → home loaded OK")

        repo.homeToTranslate = h
//        Log.d("yamete", "translate() → repo.homeToTranslate SET")

        val intent = Intent(act, TranslationForegroundService::class.java)
            .putExtra("lang", lang)

//        Log.d("yamete", "translate() → starting service…")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            act.startForegroundService(intent)
//            Log.d("yamete", "translate() → startForegroundService called")
        } else {
            act.startService(intent)
//            Log.d("yamete", "translate() → startService called")
        }
    }



    // ---------------------------------------------------------
    // PDF GENERATION
    // ---------------------------------------------------------
    private val _downloadPdf = MutableSingleLiveEvent<File>()
    val downloadPdf: LiveData<File> = _downloadPdf

    private val _sharePdf = MutableSingleLiveEvent<File>()
    val sharePdf: LiveData<File> = _sharePdf

    fun download() {
        val h = home.value ?: return
        val lang = getCurrentTargetLang() ?: return
        generatePdf(h, lang, false)
    }

    fun send() {
        val h = home.value ?: return
        val lang = getCurrentTargetLang() ?: return
        generatePdf(h, lang, true)
    }

    private fun generatePdf(originalHome: HomeModel, lang: String, share: Boolean) {
        val act = currentActivity ?: return

        // Try to load translated content
        val translated = repo.loadTranslatedHome(lang)

        val homeToUse =
            if (lang != "English" && translated != null) translated
            else originalHome

        viewModelScope.launch {

            isLoadingPhotos.postValue(true)

            val photos = AirbnbPdfGenerator.loadAllPhotoSectionsFromFirestore(
                currentActivity!!.applicationContext
            )

            isLoadingPhotos.postValue(false)

            AirbnbPdfGenerator.generatePdf(
                activity = act,
                cityGuide = homeToUse.cityGuide ?: CityGuideModel(),
                homeGuide = homeToUse.homeGuide ?: HomeGuideModel(),
                shortDescription = homeToUse.airBnbResponseModel?.detailsText,
                appName = "Better Rank",
                outputFileName = "AirbnbGuide-${lang.lowercase()}.pdf",
                sectionImages = photos,
                onPdfReady = { file ->
                    if (share) _sharePdf.postValue(file)
                    else _downloadPdf.postValue(file)
                },
                onError = {
                    isLoadingPhotos.postValue(false)
                    showMessage.postValue(it)
                }
            )

        }


    }


    // ---------------------------------------------------------
//    // PREVIEW
//    // ---------------------------------------------------------
//    fun preview() {
//        if (isTranslating.value == true) return
//
//        val h = home.value ?: run {
//            showMessage.value = "No home data"
//            return
//        }
//
//        viewModelScope.launch(Dispatchers.Main) {
//            AirbnbPdfGenerator.generateHtml(
//                h.cityGuide ?: CityGuideModel(),
//                h.homeGuide ?: HomeGuideModel(),
//                h.airBnbResponseModel?.detailsText,
//                h.airBnbResponseModel?.image ?: "",
//                "Better Rank"
//            ) { showWebViewPreview(it) }
//        }
//    }

//    private var previewDialog: androidx.appcompat.app.AlertDialog? = null
//
//    private fun showWebViewPreview(html: String) {
//        val act = currentActivity ?: return
//
//        val web = WebView(act).apply {
//            settings.javaScriptEnabled = true
//            settings.loadWithOverviewMode = true
//        }
//
//        val dialog = androidx.appcompat.app.AlertDialog.Builder(act)
//            .setView(web)
//            .create()
//
//        previewDialog = dialog
//        web.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", null)
//        dialog.show()
//    }

    // ---------------------------------------------------------
    // BACK BUTTON
    // ---------------------------------------------------------
    override val _navigateBack = MutableSingleLiveEvent<Unit>()
    override val navigateBack: LiveData<Unit> = _navigateBack
    fun back() = _navigateBack.call()

    override fun onCleared() {
        super.onCleared()
    }

    // ---------------------------------------------------------
    // USER CHANGES LANGUAGE → RESET TRANSLATION UI
    // ---------------------------------------------------------
    fun resetTranslationState() {
        repo.reset()
    }
}
