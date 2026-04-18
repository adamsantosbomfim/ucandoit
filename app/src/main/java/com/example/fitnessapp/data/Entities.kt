package com.example.fitnessapp.data

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserEntity(
    val name: String = "",
    val email: String = ""
)

@IgnoreExtraProperties
data class ProfileEntity(
    val age: Int = 0,
    val goal: String = "Manter Físico",
    val heightCm: Int = 0,
    val weightKg: Float = 0f,
    val notificationsEnabled: Boolean = true,
    val avatarPath: String = "",
    val hydrationReminderMinutes: Int = 0
)

@IgnoreExtraProperties
data class MealEntity(
    var id: String = "",
    val title: String = "",
    val calories: Int = 0,
    val time: String = "",
    val note: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class HydrationEntity(
    val consumedMl: Int = 0,
    val goalMl: Int = 2500,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class WorkoutEntity(
    var id: String = "",
    val title: String = "",
    val durationMin: Int = 0,
    val caloriesBurned: Int = 0,
    val dateTime: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class BodyMetricsEntity(
    var id: String = "",
    val weightKg: Float = 0f,
    val waistCm: Float = 0f,
    val chestCm: Float = 0f,
    val armCm: Float = 0f,
    val note: String = "",
    val photoPath: String = "",
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

