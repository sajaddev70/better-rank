package com.marveloushost.betterrank.hilt

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.pdf.TranslationState
import com.google.firebase.FirebaseApp
import com.marveloushost.betterrank.hilt.modual.HiltNetworkModule
import com.marveloushost.betterrank.hilt.modual.HiltNetworkModule.Companion.BRIGHT_DATA_TOKEN
import com.marveloushost.betterrank.hilt.modual.HiltNetworkModule.Companion.GPT_API_KEY
import com.marveloushost.betterrank.util.RemoteConfigManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class HiltApplication : Application() {

    @Inject lateinit var translationState: TranslationState

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        CoroutineScope(Dispatchers.IO).launch {
            RemoteConfigManager.fetchAndActivate()
            HiltNetworkModule.loadKeysFromRemoteConfig()
            println("🔐 GPT=${GPT_API_KEY.take(6)}*** | BRIGHT=${BRIGHT_DATA_TOKEN.take(6)}***")

        }

        createNotificationChannel()
        observeTranslationState()
    }

    private fun observeTranslationState() {
        translationState.translatedLanguage
            .onEach { lang ->
                if (lang != null) {
                    sendFinishNotification(lang)
                }
            }
            .launchIn(CoroutineScope(Dispatchers.Default))
    }

    private fun sendFinishNotification(lang: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val builder = NotificationCompat.Builder(this, "pdf_channel")
            .setSmallIcon(R.drawable.ic_small_logo)
            .setContentTitle("PDF Ready")
            .setContentText("The home PDF guide in $lang is ready")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        NotificationManagerCompat.from(this).notify(2001, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Existing PDF channel
            val pdfChannel = NotificationChannel(
                "pdf_channel",
                "PDF Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )

            // NEW REQUIRED CHANNEL for translation service
            val instructionsChannel = NotificationChannel(
                "instructions_channel",
                "Instruction Translation",
                NotificationManager.IMPORTANCE_HIGH
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(pdfChannel)
            manager.createNotificationChannel(instructionsChannel)
        }
    }

}
