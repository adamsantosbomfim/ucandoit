package com.example.fitnessapp

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class HydrationReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val enabled = HydrationReminderManager.remindersEnabled(context)
        val (consumedMl, goalMl) = HydrationReminderManager.currentHydration(context)

        if (enabled && consumedMl < goalMl) {
            val openAppIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                5011,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(
                context,
                HydrationReminderManager.notificationChannelId()
            )
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Hora de beber agua")
                .setContentText(HydrationReminderManager.reminderMessage(context))
                .setStyle(NotificationCompat.BigTextStyle().bigText(HydrationReminderManager.reminderMessage(context)))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            NotificationManagerCompat.from(context).notify(5012, notification)
        }

        HydrationReminderManager.scheduleNextReminder(context)
    }
}
