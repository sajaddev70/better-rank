package com.marvelous.betterrank.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.marvelous.betterrank.R
import com.marvelous.betterrank.ui.Main
import java.util.Date


class AlarmSoundService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var reminderId: String

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        reminderId = intent?.getStringExtra("reminderId") ?: return START_NOT_STICKY
        val title = intent.getStringExtra("title") ?: "title"
        val message = intent.getStringExtra("message") ?: ""
        val startAtMillis = intent.getLongExtra("startAt", 0L)
        val startAtDate = Date(startAtMillis)
        val reminded = intent.getBooleanExtra("reminded", false)

        // نوتیف برای بستن آلارم
        val dismissIntent = Intent(this, AlarmDismissReceiver::class.java).apply {
            putExtra("reminderId", reminderId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            reminderId.hashCode(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // کانال نوتیف
        val channelId = "reminder_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            channel.setSound(null, null)
            channel.enableVibration(true)
            manager.createNotificationChannel(channel)
        }

        val contentIntent = Intent(this, Main::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            reminderId.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .addAction(R.drawable.ic_tip, "OK", dismissPendingIntent)
            .build()

        wakeScreen()

        startForeground(reminderId.hashCode(), notification)

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.alarm).apply {
                isLooping = true
                start()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @Suppress("DEPRECATION")
    private fun wakeScreen() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isInteractive) {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP
            } else {
                PowerManager.FULL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP
            }

            val wakeLock = powerManager.newWakeLock(flags, "reminder:wakelock")
            wakeLock.acquire(5000L)
            wakeLock.release()
        }
    }
}


/*class AlarmSoundService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private lateinit var reminderId: String

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        reminderId = intent?.getStringExtra("reminderId") ?: return START_NOT_STICKY
        val title = intent.getStringExtra("title") ?: "یادآور"
        val description = intent.getStringExtra("description") ?: ""
        val startAtMillis = intent.getLongExtra("startAt", 0L)
        val startAtDate = Date(startAtMillis)
        val isRecurring = intent.getBooleanExtra("isRecurring", false)
        val repeatIntervalUnit = intent.getStringExtra("repeatIntervalUnit") ?: RepeatIntervalUnit.Hour.value
        val repeatEveryN = intent.getIntExtra("repeatEveryN", 1)

        // PendingIntent برای توقف از نوتیف
        val dismissIntent = Intent(this, AlarmDismissReceiver::class.java).apply {
            putExtra("reminderId", reminderId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            this,
            reminderId.hashCode(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification Channel
        val channelId = "reminder_channel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "یادآورها",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            channel.setSound(null, null)
            channel.enableVibration(true)
            manager.createNotificationChannel(channel)
        }

        // FullScreenIntent
        val fullScreenIntent = Intent(this, ReminderAlarm::class.java).apply {
            putExtra("reminderId", reminderId)
            putExtra("title", title)
            putExtra("description", description)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            reminderId.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .addAction(R.drawable.ic_check, "متوجه شدم", dismissPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()

        startForeground(reminderId.hashCode(), notification)

        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.alarm).apply {
                isLooping = true
                start()
            }
        }

        if (isRecurring) {

            ReminderScheduler.scheduleReminder(this, ReminderModel(
                reminderId,
                title,
                description,
                startAt = startAtDate,
                isRecurring = true,
                repeatIntervalUnit = repeatIntervalUnit,
                repeatEveryN = repeatEveryN
            ))
        }

        return START_STICKY
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}*/

