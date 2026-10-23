package com.marveloushost.betterrank.util

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await

object RemoteConfigManager {

    private val remoteConfig: FirebaseRemoteConfig by lazy {
        FirebaseRemoteConfig.getInstance().apply {
            setConfigSettingsAsync(
                FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(3600) // 1 hour
                    .build()
            )

            setDefaultsAsync(
                mapOf(
                    "gpt_api_key" to "",
                    "bright_data_token" to ""
                )
            )
        }
    }

    suspend fun fetchAndActivate() {
        try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            // silent fail – cached values will be used
        }
    }

    fun getGptKey(): String =
        remoteConfig.getString("gpt_api_key")

    fun getBrightDataToken(): String =
        remoteConfig.getString("bright_data_token")
}