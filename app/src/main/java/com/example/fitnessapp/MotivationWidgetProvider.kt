package com.example.fitnessapp

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class MotivationWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        refreshAll(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            refreshAll(context)
        }
    }

    companion object {
        private const val ACTION_REFRESH_WIDGET = "com.example.fitnessapp.action.REFRESH_WIDGET"
        private const val PREFS_NAME = "fitness_app_prefs"
        private const val KEY_USER_ID = "user_id"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun requestRefresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MotivationWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(componentName)
            if (ids.isNotEmpty()) {
                manager.notifyAppWidgetViewDataChanged(ids, R.id.widget_root)
                refreshAll(context)
            }
        }

        private fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MotivationWidgetProvider::class.java)
            val widgetIds = manager.getAppWidgetIds(componentName)
            if (widgetIds.isEmpty()) return

            scope.launch {
                val ui = loadWidgetUi(context)
                widgetIds.forEach { widgetId ->
                    manager.updateAppWidget(widgetId, buildRemoteViews(context, ui))
                }
            }
        }

        private suspend fun loadWidgetUi(context: Context): WidgetUiState {
            val userId = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_USER_ID, null)

            if (userId.isNullOrBlank()) {
                return WidgetUiState(
                    title = "UcandoIt",
                    subtitle = "Entra no app para veres o teu progresso.",
                    hydrationText = "-- / -- ml",
                    hydrationProgress = 0,
                    caloriesText = "-- kcal",
                    caloriesProgress = 0,
                    trainingText = "-- / --",
                    trainingProgress = 0,
                    message = "Vamos começar o dia com foco."
                )
            }

            val db = FirebaseDatabase.getInstance().reference
            val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val profile = runCatching {
                db.child("profiles").child(userId).get().await()
            }.getOrNull()
            val hydration = runCatching {
                db.child("hydration").child(userId).child(todayKey).get().await()
            }.getOrNull()
            val meals = runCatching {
                db.child("meals").child(userId).child(todayKey).get().await()
            }.getOrNull()
            val workouts = runCatching {
                db.child("workouts").child(userId).get().await()
            }.getOrNull()

            val goal = profile?.child("goal")?.getValue(String::class.java).orEmpty()
            val hydrationGoal = hydration?.child("goalMl")?.getValue(Int::class.java) ?: 2500
            val hydrationConsumed = hydration?.child("consumedMl")?.getValue(Int::class.java) ?: 0
            val todayCalories = meals?.children?.sumOf { meal ->
                meal.child("calories").getValue(Int::class.java) ?: 0
            } ?: 0

            val caloriesTarget = when (goal) {
                "Perder Peso", "Perder Gordura" -> 1800
                "Ganhar massa muscular" -> 2600
                else -> 2200
            }

            val weeklyWorkoutGoal = when (goal) {
                "Perder Peso", "Perder Gordura" -> 4
                "Ganhar massa muscular" -> 4
                else -> 3
            }

            val weekWorkoutDays = workouts
                ?.children
                ?.mapNotNull { workout ->
                    val createdAt = workout.child("createdAtEpochMs").getValue(Long::class.java) ?: return@mapNotNull null
                    if (isCurrentWeek(createdAt)) dayKey(createdAt) else null
                }
                ?.distinct()
                ?.size
                ?: 0

            val hydrationProgress = ((hydrationConsumed / hydrationGoal.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
            val caloriesProgress = ((todayCalories / caloriesTarget.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
            val trainingProgress = ((weekWorkoutDays / weeklyWorkoutGoal.toFloat()) * 100f).roundToInt().coerceIn(0, 100)

            val message = when {
                hydrationConsumed >= hydrationGoal && weekWorkoutDays >= weeklyWorkoutGoal ->
                    "Dia forte. Água e treinos no ponto."
                hydrationConsumed < hydrationGoal / 2 ->
                    "Bebe água e volta ao ritmo."
                weekWorkoutDays == 0 ->
                    "Marca o primeiro treino da semana."
                todayCalories in (caloriesTarget * 0.8).toInt()..caloriesTarget ->
                    "Boa gestão de energia hoje."
                else ->
                    "Continua. Pequenos passos contam."
            }

            return WidgetUiState(
                title = greetingTitle(),
                subtitle = "Resumo rápido do teu dia",
                hydrationText = "$hydrationConsumed / $hydrationGoal ml",
                hydrationProgress = hydrationProgress,
                caloriesText = "$todayCalories / $caloriesTarget kcal",
                caloriesProgress = caloriesProgress,
                trainingText = "$weekWorkoutDays / $weeklyWorkoutGoal treinos",
                trainingProgress = trainingProgress,
                message = message
            )
        }

        private fun buildRemoteViews(context: Context, state: WidgetUiState): RemoteViews {
            return RemoteViews(context.packageName, R.layout.widget_motivation).apply {
                setTextViewText(R.id.widget_title, state.title)
                setTextViewText(R.id.widget_subtitle, state.subtitle)
                setTextViewText(R.id.widget_message, state.message)
                setTextViewText(R.id.widget_hydration_value, state.hydrationText)
                setTextViewText(R.id.widget_calories_value, state.caloriesText)
                setTextViewText(R.id.widget_training_value, state.trainingText)
                setProgressBar(R.id.widget_hydration_progress, 100, state.hydrationProgress, false)
                setProgressBar(R.id.widget_calories_progress, 100, state.caloriesProgress, false)
                setProgressBar(R.id.widget_training_progress, 100, state.trainingProgress, false)

                setOnClickPendingIntent(R.id.widget_refresh, refreshPendingIntent(context))
                val openApp = PendingIntent.getActivity(
                    context,
                    9102,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.widget_root, openApp)
            }
        }

        private fun refreshPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MotivationWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            return PendingIntent.getBroadcast(
                context,
                9101,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun greetingTitle(): String {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 5..11 -> "Bom dia"
                in 12..17 -> "Boa tarde"
                else -> "Boa noite"
            }
        }

        private fun isCurrentWeek(epochMs: Long): Boolean {
            val target = Calendar.getInstance().apply { timeInMillis = epochMs }
            val startOfWeek = Calendar.getInstance().apply {
                firstDayOfWeek = Calendar.MONDAY
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val endOfWeek = Calendar.getInstance().apply {
                timeInMillis = startOfWeek.timeInMillis
                add(Calendar.DAY_OF_YEAR, 7)
            }
            return target.timeInMillis in startOfWeek.timeInMillis until endOfWeek.timeInMillis
        }

        private fun dayKey(epochMs: Long): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(epochMs))
        }
    }
}

private data class WidgetUiState(
    val title: String,
    val subtitle: String,
    val hydrationText: String,
    val hydrationProgress: Int,
    val caloriesText: String,
    val caloriesProgress: Int,
    val trainingText: String,
    val trainingProgress: Int,
    val message: String
)

