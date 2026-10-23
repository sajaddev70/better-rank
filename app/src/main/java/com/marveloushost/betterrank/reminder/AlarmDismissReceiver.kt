package com.marveloushost.betterrank.reminder

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmDismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra("reminderId") ?: return

        val stopIntent = Intent(context, AlarmSoundService::class.java)
        context.stopService(stopIntent)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(reminderId.hashCode())
    }
}
