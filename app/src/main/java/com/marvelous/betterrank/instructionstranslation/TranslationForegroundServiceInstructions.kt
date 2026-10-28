//package com.marvelous.betterrank.instructionstranslation
//
//import android.Manifest
//import android.app.Notification
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.os.Build
//import android.util.Log
//import androidx.core.app.NotificationCompat
//import androidx.core.app.NotificationManagerCompat
//import androidx.core.content.ContextCompat
//import androidx.lifecycle.LifecycleService
//import com.marvelous.betterrank.R
//import dagger.hilt.android.AndroidEntryPoint
//import kotlinx.coroutines.*
//import kotlinx.coroutines.selects.select
//import javax.inject.Inject
//
//@AndroidEntryPoint
//class TranslationForegroundServiceInstructions : LifecycleService() {
//
//    @Inject lateinit var repo: TranslationRepositoryInstructions
//    private var job: Job? = null
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        super.onStartCommand(intent, flags, startId)
//
//        val lang = intent?.getStringExtra("lang")
//        val raw = intent?.getStringExtra("rawText")
//
//        if (lang == null || raw == null) {
//            stopSelf()
//            return START_NOT_STICKY
//        }
//
//        repo.setRunning(lang)
//        repo.startTranslation(raw, lang)
//
//        startForeground(4001, createNotification())
//
//        job = CoroutineScope(Dispatchers.IO).launch {
//
//            val gptJob = async {
//                InstructionsTranslator.translate(raw, lang)
//            }
//
//            val countdownJob = async {
//                for (i in 240 downTo 0) {
//                    repo.setCountdown(i)
//                    delay(1000)
//                }
//                null
//            }
//
//            val winner = select<String?> {
//                gptJob.onAwait {
//                    countdownJob.cancel()
//                    it
//                }
//                countdownJob.onAwait { null }
//            }
//
//            val result = winner ?: gptJob.await()
//
//            repo.saveTranslatedResult(result)
//            repo.setDone()
//
//            sendFinishNotification(lang)
//            stopSelf()
//        }
//
//        return START_STICKY
//    }
//
//    private fun createNotification(): Notification =
//        NotificationCompat.Builder(this, "translation_channel")
//            .setSmallIcon(R.drawable.ic_small_logo)
//            .setContentTitle("Translating instructions…")
//            .setContentText("Please wait")
//            .setOngoing(true)
//            .build()
//
//    private fun sendFinishNotification(lang: String) {
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
//            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
//            != PackageManager.PERMISSION_GRANTED
//        ) return
//
//        val notif = NotificationCompat.Builder(this, "translation_channel")
//            .setSmallIcon(R.drawable.ic_small_logo)
//            .setContentTitle("Translation Ready")
//            .setContentText("Your $lang instructions are ready.")
//            .setAutoCancel(true)
//            .build()
//
//        NotificationManagerCompat.from(this).notify(4002, notif)
//    }
//
//    override fun onDestroy() {
//        job?.cancel()
//        super.onDestroy()
//    }
//}
