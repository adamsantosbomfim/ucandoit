package com.example.fitnessapp

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object HydrationReminderManager {
    private const val PREFS_NAME = "hydration_reminder_prefs"
    private const val KEY_DATE = "hydration_date"
    private const val KEY_CONSUMED_ML = "hydration_consumed_ml"
    private const val KEY_GOAL_ML = "hydration_goal_ml"
    private const val KEY_NOTIFICATIONS_ENABLED = "hydration_notifications_enabled"
    private const val CHANNEL_ID = "hydration_reminders"
    private const val CHANNEL_NAME = "Lembretes de Agua"
    private const val REQUEST_CODE = 4012

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Lembretes para beber agua durante o dia"
        }
        manager.createNotificationChannel(channel)
    }

    fun syncState(context: Context, consumedMl: Int, goalMl: Int, notificationsEnabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_DATE, todayKey())
            .putInt(KEY_CONSUMED_ML, consumedMl)
            .putInt(KEY_GOAL_ML, goalMl)
            .putBoolean(KEY_NOTIFICATIONS_ENABLED, notificationsEnabled)
            .apply()

        scheduleNextReminder(context)
    }

    fun scheduleNextReminder(context: Context) {
        ensureChannel(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, false)
        val today = todayKey()
        val savedDate = prefs.getString(KEY_DATE, today)
        val goalMl = prefs.getInt(KEY_GOAL_ML, 2500).coerceAtLeast(1000)
        val consumedMl = if (savedDate == today) prefs.getInt(KEY_CONSUMED_ML, 0) else 0

        if (!notificationsEnabled || !notificationsPermissionGranted(context)) {
            cancelReminder(context)
            return
        }

        val triggerAtMillis = if (consumedMl >= goalMl) {
            nextMorningAt(8, 0)
        } else {
            nextReminderDuringDay()
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = reminderPendingIntent(context)
        alarmManager.cancel(pendingIntent)
        runCatching {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }.onFailure {
            // Fallback for devices/restrictions where idle scheduling is blocked.
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.cancel(reminderPendingIntent(context))
    }

    fun remindersEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NOTIFICATIONS_ENABLED, false)
    }

    fun currentHydration(context: Context): Pair<Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val today = todayKey()
        val savedDate = prefs.getString(KEY_DATE, today)
        val consumedMl = if (savedDate == today) prefs.getInt(KEY_CONSUMED_ML, 0) else 0
        val goalMl = prefs.getInt(KEY_GOAL_ML, 2500).coerceAtLeast(1000)
        return consumedMl to goalMl
    }

    fun notificationChannelId(): String = CHANNEL_ID

    fun reminderMessage(context: Context): String {
        val (consumedMl, goalMl) = currentHydration(context)
        val remainingMl = (goalMl - consumedMl).coerceAtLeast(0)
        val messages = listOf(
            "Hora de beber agua. Um copo agora ja ajuda a manter o foco e a energia.",
            "Seu corpo agradece: toma agua agora e aproxima-te da tua meta de hoje.",
            "Pequeno lembrete fitness: hidrata-te agora para continuar no ritmo certo.",
            "Bora beber agua? Faltam so ${remainingMl} ml para bater a meta de hoje."
        )
        return messages[(System.currentTimeMillis() / 1000L % messages.size).toInt()]
    }

    fun sendTestNotification(context: Context) {
        ensureChannel(context)
        if (!notificationsPermissionGranted(context)) return

        val openAppIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            5013,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val message = reminderMessage(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Hora de beber agua")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(5014, notification)
    }

    private fun reminderPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, HydrationReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextReminderDuringDay(): Long {
        val calendar = Calendar.getInstance()
        when {
            calendar.get(Calendar.HOUR_OF_DAY) < 8 -> {
                calendar.set(Calendar.HOUR_OF_DAY, 8)
                calendar.set(Calendar.MINUTE, 0)
            }
            calendar.get(Calendar.HOUR_OF_DAY) >= 22 -> {
                calendar.timeInMillis = nextMorningAt(8, 0)
                return calendar.timeInMillis
            }
            else -> {
                calendar.add(Calendar.HOUR_OF_DAY, 2)
                if (calendar.get(Calendar.HOUR_OF_DAY) >= 22) {
                    calendar.timeInMillis = nextMorningAt(8, 0)
                    return calendar.timeInMillis
                }
            }
        }
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun nextMorningAt(hour: Int, minute: Int): Long {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun todayKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun notificationsPermissionGranted(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }
}
