package com.marvelous.betterrank.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.marvelous.betterrank.model.NoteModel
import androidx.core.net.toUri


object ReminderScheduler {

    fun syncReminders(context: Context, reminders: List<NoteModel>) {
        val now = System.currentTimeMillis()

        reminders.forEach { reminder ->
            if (!reminder.reminded) {
                cancelReminder(context, reminder.id)
                return@forEach
            }
            else if (reminder.startAt <= now) {
                cancelReminder(context, reminder.id)
                return@forEach
            }
            else {
                cancelReminder(context, reminder.id)
                scheduleReminder(context, reminder)
                return@forEach
            }
        }
    }

    private fun scheduleReminder(context: Context, reminder: NoteModel) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = reminder.startAt

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("reminderId", reminder.id)
            putExtra("title", reminder.title)
            putExtra("message", reminder.message)
            putExtra("startAt", reminder.startAt)
            putExtra("reminded", reminder.reminded)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAt,
                            pendingIntent
                        )
                    } else {
                        openExactAlarmPermissionPage(context)
                    }
                }
                else-> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAt,
                        pendingIntent
                    )
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun cancelReminder(context: Context, reminderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun openExactAlarmPermissionPage(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = "package:${context.packageName}".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
                val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = "package:${context.packageName}".toUri()
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            }
        }
    }
}



