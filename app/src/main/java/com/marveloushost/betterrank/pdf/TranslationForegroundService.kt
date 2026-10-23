package com.marveloushost.betterrank.pdf

import android.Manifest
import android.app.Notification
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.marveloushost.betterrank.R
import com.marveloushost.betterrank.model.HomeModel
import com.marveloushost.betterrank.repository.TranslationRepository
import com.marveloushost.betterrank.translation.TranslationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import javax.inject.Inject

@AndroidEntryPoint
class TranslationForegroundService : LifecycleService() {

    @Inject lateinit var repo: TranslationRepository
    private var job: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
//        Log.d("yamete", "Service STARTED")

        val lang = intent?.getStringExtra("lang")
//        Log.d("yamete", "Service lang = $lang")

        if (lang == null) {
//            Log.d("yamete", "Service ERROR → lang==null, stopping")
            return START_NOT_STICKY
        }

        startForeground(1001, createNotification())
//        Log.d("yamete", "Foreground started")

        repo.start(lang)
//        Log.d("yamete", "repo.start(lang) done → repo.state = ${repo.state.value}")


        // ==========================================================
        //              GPT + COUNTDOWN PARALLEL LOGIC
        // ==========================================================
        job = CoroutineScope(Dispatchers.IO).launch {
//            Log.d("yamete", "Service coroutine started")

            val home = repo.homeToTranslate
            if (home == null) {
//                Log.d("yamete", "ERROR: repo.homeToTranslate is NULL!")
                stopSelf()
                return@launch
            }

            // -----------------------------
            // GPT starts immediately
            // -----------------------------
            val gptJob = async {
//                Log.d("yamete", "GPT job START → lang=$lang")
                val result = TranslationManager.translateHomeGuide(home, lang)
//                Log.d("yamete", "GPT job FINISHED!")
                result
            }

            // -----------------------------
            // Countdown starts immediately
            // -----------------------------
            val countdownJob = async {
                for (i in 240 downTo 0) {
                    repo.setProgress(i)
                    if (i % 30 == 0)
//                        Log.d("yamete", "Countdown: $i")
                    delay(1000)
                }
//                Log.d("yamete", "Countdown finished (0)")
                null  // result of countdown
            }

            // ==================================================
            //       Whichever finishes first goes here
            // ==================================================
            val winner = select<HomeModel?> {
                gptJob.onAwait { translation ->
//                    Log.d("yamete", "SELECT → GPT won the race")
                    translation
                }

                countdownJob.onAwait {
//                    Log.d("yamete", "SELECT → Countdown won the race")
                    null
                }
            }

            // ==================================================
            //           GPT finished FIRST
            // ==================================================
            if (winner != null) {
//                Log.d("yamete", "FINAL → GPT FIRST → stopping countdown")
                countdownJob.cancel()

                repo.setTranslatedHome(winner, lang)
                repo.saveTranslatedHome(lang, winner)
                sendFinishNotification(lang)
                stopSelf()
                return@launch
            }

            // ==================================================
            //         COUNTDOWN finished first
            //          GPT still running → WAIT
            // ==================================================
//            Log.d("yamete", "Waiting for GPT AFTER countdown…")
            val realTranslation = gptJob.await()

//            Log.d("yamete", "GPT FINALLY finished after countdown")

            repo.setTranslatedHome(realTranslation, lang)
            sendFinishNotification(lang)
            stopSelf()
        }

        return START_STICKY
    }


    // ==========================================================
    //                   NOTIFICATION METHODS
    // ==========================================================

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "pdf_channel")
            .setSmallIcon(R.drawable.ic_small_logo)
            .setContentTitle("Translating PDF…")
            .setContentText("Please wait…")
            .setOngoing(true)
            .build()
    }

    private fun sendFinishNotification(lang: String) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
//                Log.d("yamete", "Notification blocked → POST_NOTIFICATIONS denied")
                return
            }
        }

        val notification = NotificationCompat.Builder(this, "pdf_channel")
            .setSmallIcon(R.drawable.ic_small_logo)
            .setContentTitle("PDF Ready")
            .setContentText("The home PDF guide in $lang is ready")
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(2001, notification)
//        Log.d("yamete", "Finish notification sent")
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
//        Log.d("yamete", "Service DESTROYED")
    }
}
