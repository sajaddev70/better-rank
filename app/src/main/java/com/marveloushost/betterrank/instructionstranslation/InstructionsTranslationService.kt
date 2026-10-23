package com.marveloushost.betterrank.instructionstranslation


import android.Manifest
import android.app.Notification
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.marveloushost.betterrank.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.selects.select
import javax.inject.Inject

@AndroidEntryPoint
class InstructionsTranslationService : LifecycleService() {

    @Inject lateinit var repo: TranslationRepositoryInstructions

    private var job: Job? = null

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        Log.d("INSTR_SERVICE", "Service STARTED")

        val lang = intent?.getStringExtra("lang")
        val rawText = intent?.getStringExtra("rawText")
        val guestId = intent?.getStringExtra("guestId")

        if (lang == null || rawText == null) {
//            Log.e("INSTR_SERVICE", "ERROR → lang or rawText is NULL. Stopping.")
            stopSelf()
            return START_NOT_STICKY
        }

//        Log.d("INSTR_SERVICE", "Received → lang=$lang  textLength=${rawText.length}")

        repo.startTranslation(rawText, lang, guestId!!)

        startForeground(3001, createNotification())
//        Log.d("INSTR_SERVICE", "Foreground started")

        // ----------------------------------------------------------
        //           GPT + COUNTDOWN PARALLEL LOGIC
        // ----------------------------------------------------------
        job = CoroutineScope(Dispatchers.IO).launch {

            // GPT translation job
            val gptJob = async {
//                Log.d("INSTR_SERVICE", "GPT job START → lang=$lang")
                val result = InstructionsTranslator.translate(rawText, lang)
//                Log.d("INSTR_SERVICE", "GPT job FINISHED")
                result
            }

            // Countdown job
            val countdownJob = async {
                for (i in repo.countdown.value downTo 0) {
                    Log.d("COUNTDOWN", "i=$i")
                    repo.setCountdown(i)
                    delay(1000)
                }
//                Log.d("INSTR_SERVICE", "Countdown finished")
                null
            }

            // WAIT FOR WINNER
            val winner = select<String?> {
                gptJob.onAwait { translated ->
//                    Log.d("INSTR_SERVICE", "SELECT → GPT won")
                    translated
                }
                countdownJob.onAwait {
//                    Log.d("INSTR_SERVICE", "SELECT → Countdown won")
                    null
                }
            }

            // GPT finished first
            if (winner != null) {
//                Log.d("INSTR_SERVICE", "GPT FIRST → cancelling countdown")
                countdownJob.cancel()

                repo.setResult(winner)
                sendFinishNotification(lang)

                stopSelf()
                return@launch
            }

            // Countdown finished first → wait for GPT
//            Log.d("INSTR_SERVICE", "Waiting for GPT after countdown…")
            val finalTranslation = gptJob.await()

            repo.setResult(finalTranslation)
            sendFinishNotification(lang)

            stopSelf()
        }

        return START_STICKY
    }

    // ==================================================================
    //                            NOTIFICATION
    // ==================================================================

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "instructions_channel")
            .setSmallIcon(R.drawable.ic_small_logo)
            .setContentTitle("Translating instructions…")
            .setContentText("Please wait while assistant prepares your text.")
            .setOngoing(true)
            .build()
    }

    private fun sendFinishNotification(lang: String) {

        // Android 13+ notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
//                Log.d("INSTR_SERVICE", "POST_NOTIFICATIONS denied → skip notif")
                return
            }
        }

        val notif = NotificationCompat.Builder(this, "instructions_channel")
            .setSmallIcon(R.drawable.ic_small_logo)
            .setContentTitle("Instructions Ready")
            .setContentText("Your $lang instructions are now ready to send.")
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(3002, notif)
//        Log.d("INSTR_SERVICE", "Finish notification sent")
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
//        Log.d("INSTR_SERVICE", "Service DESTROYED")
    }
}
