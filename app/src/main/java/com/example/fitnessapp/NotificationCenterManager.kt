package com.example.fitnessapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AppNotificationItem(
    val title: String,
    val message: String,
    val createdAtEpochMs: Long,
    val read: Boolean
)

object NotificationCenterManager {
    private const val PREFS_NAME = "notification_center_prefs"
    private const val KEY_ITEMS = "items"
    private const val MAX_ITEMS = 10

    fun addNotification(context: Context, title: String, message: String) {
        val updated = loadNotifications(context).toMutableList()
        updated.add(
            0,
            AppNotificationItem(
                title = title,
                message = message,
                createdAtEpochMs = System.currentTimeMillis(),
                read = false
            )
        )
        saveNotifications(context, updated.take(MAX_ITEMS))
    }

    fun latestNotifications(context: Context): List<AppNotificationItem> {
        return loadNotifications(context).take(MAX_ITEMS)
    }

    fun unreadCount(context: Context): Int {
        return loadNotifications(context).count { !it.read }
    }

    fun markAllAsRead(context: Context) {
        val updated = loadNotifications(context).map { it.copy(read = true) }
        saveNotifications(context, updated)
    }

    private fun loadNotifications(context: Context): List<AppNotificationItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_ITEMS, "[]") ?: "[]"
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    AppNotificationItem(
                        title = item.optString("title"),
                        message = item.optString("message"),
                        createdAtEpochMs = item.optLong("createdAtEpochMs"),
                        read = item.optBoolean("read", true)
                    )
                )
            }
        }
    }

    private fun saveNotifications(context: Context, items: List<AppNotificationItem>) {
        val array = JSONArray()
        items.take(MAX_ITEMS).forEach { item ->
            array.put(
                JSONObject().apply {
                    put("title", item.title)
                    put("message", item.message)
                    put("createdAtEpochMs", item.createdAtEpochMs)
                    put("read", item.read)
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ITEMS, array.toString())
            .apply()
    }
}
