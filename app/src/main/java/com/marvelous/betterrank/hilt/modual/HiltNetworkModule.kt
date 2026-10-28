package com.marvelous.betterrank.hilt.modual

import android.content.Context
import android.content.SharedPreferences
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.marvelous.betterrank.networkMonitor.LiveNetworkMonitor
import com.marvelous.betterrank.networkMonitor.NetworkMonitor
import com.marvelous.betterrank.networkMonitor.NetworkMonitorInterceptor
import com.marvelous.betterrank.retrofit.RequestApi
import com.marvelous.betterrank.util.RemoteConfigManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class HiltNetworkModule {

    companion object {
        const val BASEURL = "https://clients3.google.com/"
        const val SCRAPER_API_URL = "https://api.scraperapi.com/"
        const val SCRAPER_API_TOKEN = "1c1d02a419af31a7b05628b204865146"
        const val OUTSCRAPER_API_URL = "https://api.outscraper.cloud/airbnb-reviews"
        const val OUTSCRAPER_API_KEY = " NjNkN2UwNWMyNzYyNDQxOGFiYmMzNGU3YmJlMGRmOGR8N2VlNmMwYWQ5Ng"
        @Volatile var GPT_API_KEY = ""
        const val BRIGHT_DATA_URL = "https://api.brightdata.com/datasets/v3/scrape"
        @Volatile var BRIGHT_DATA_TOKEN = ""
        const val BRIGHT_DATA_DATA_SET_ID = "gd_ld7ll037kqy322v05"

        fun loadKeysFromRemoteConfig() {
            GPT_API_KEY = RemoteConfigManager.getGptKey()
            BRIGHT_DATA_TOKEN = RemoteConfigManager.getBrightDataToken()
        }
        /** 🔥 Load keys once globally at startup */
//        fun loadKeysFromFirestore() = runBlocking(Dispatchers.IO) {
//            try {
//                val snapshot = FirebaseFirestore.getInstance()
//                    .collection("config")
//                    .document("keys")
//                    .get()
//                    .await()
//
//                GPT_API_KEY = snapshot.getString("gpt") ?: ""
//                BRIGHT_DATA_TOKEN = snapshot.getString("brightdata") ?: ""
//
//                println("🔥 Firestore Keys Loaded → GPT:$GPT_API_KEY | BRIGHT:$BRIGHT_DATA_TOKEN")
//            } catch (e: Exception) {
//                println("❌ Failed to load keys: ${e.message}")
//            }
//        }
    }

//    init {
//        // called one time when the DI graph builds
//        loadKeysFromFirestore()
//    }


    @Provides
    @Singleton
    fun providerOkHttpClient(@ApplicationContext appContext: Context,
                             sharedPreferences: SharedPreferences,
                             networkMonitorInterceptor: NetworkMonitorInterceptor): OkHttpClient
    {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val urlString = originalRequest.url.toString()

                val isBrightData = urlString.contains("brightdata.com")
                val token = if (isBrightData) {
                    BRIGHT_DATA_TOKEN.takeIf { it.isNotBlank() }
                } else {
                    sharedPreferences.getString(HiltSharedPrefModule.TOKEN, "")
                }

                if (token.isNullOrBlank()) {
                    chain.proceed(originalRequest) // no auth header
                } else {
                    chain.proceed(
                        originalRequest.newBuilder()
                            .addHeader("Authorization", "Bearer $token")
                            .build()
                    )
                }
            }
            .addInterceptor(networkMonitorInterceptor)
            .addInterceptor(ChuckerInterceptor.Builder(appContext).build())
            .retryOnConnectionFailure(true)
            .readTimeout(70, TimeUnit.SECONDS)
            .writeTimeout(70, TimeUnit.SECONDS)
            .connectTimeout(70, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun providerRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASEURL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun providerRequestApi(retrofit: Retrofit): RequestApi
    {
        return retrofit.create(RequestApi::class.java)
    }

    @Provides
    fun provideNetworkMonitor(@ApplicationContext appContext: Context): NetworkMonitor {
        return LiveNetworkMonitor(appContext)
    }



}