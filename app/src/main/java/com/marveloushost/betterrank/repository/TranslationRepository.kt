package com.marveloushost.betterrank.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.marveloushost.betterrank.model.HomeModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TranslationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    data class State(
        val isRunning: Boolean = false,
        val secondsLeft: Int = 0,
        val finishedLang: String? = null,
        val targetLang: String? = null
    )

    val state = MutableStateFlow(State())

    var homeToTranslate: HomeModel? = null
    var translatedHome: HomeModel? = null

    private val prefs = context.getSharedPreferences("translations", Context.MODE_PRIVATE)

    fun saveTranslatedHome(lang: String, home: HomeModel) {
        prefs.edit()
            .putString("home_$lang", Gson().toJson(home))
            .apply()
    }

    fun loadTranslatedHome(lang: String): HomeModel? {
        val json = prefs.getString("home_$lang", null) ?: return null
        return try { Gson().fromJson(json, HomeModel::class.java) } catch (e: Exception) { null }
    }


    fun start(lang: String) {
//        Log.d("yamete", "repo.start(lang=$lang) CALLED")
//        Log.d("yamete", "repo.homeToTranslate BEFORE = $homeToTranslate")

        translatedHome = null

        state.value = State(
            isRunning = true,
            secondsLeft = 240,
            finishedLang = null,
            targetLang = lang
        )

//        Log.d("yamete", "repo.start DONE → state=${state.value}")
    }



    fun setProgress(sec: Int) {
        state.value = state.value.copy(secondsLeft = sec)
    }

    fun setTranslatedHome(home: HomeModel, lang: String) {
        translatedHome = home
        state.value = State(
            isRunning = false,
            secondsLeft = 0,
            finishedLang = lang,
            targetLang = lang
        )
    }
    fun saveLastLanguage(lang: String) {
        prefs.edit().putString("last_lang", lang).apply()
    }

    fun loadLastLanguage(): String? {
        return prefs.getString("last_lang", null)
    }


    fun reset() {
        homeToTranslate = null
        translatedHome = null
        state.value = State()
    }
}
