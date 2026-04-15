package com.example.fitnessapp.vm

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

object Session {
    private const val PREFS_NAME = "fitness_app_prefs"
    private const val KEY_USER_ID = "user_id"
    private lateinit var prefs: SharedPreferences

    val userId = MutableStateFlow<String?>(null)

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        userId.value = prefs.getString(KEY_USER_ID, null)
    }

    fun setSession(id: String?) {
        userId.value = id
        prefs.edit().putString(KEY_USER_ID, id).apply()
    }
    
    fun clear() {
        setSession(null)
    }
}
