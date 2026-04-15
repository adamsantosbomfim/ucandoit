package com.example.fitnessapp.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.data.ProfileEntity
import com.example.fitnessapp.data.RepoProvider
import com.example.fitnessapp.data.WorkoutEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class TrainingViewModel : ViewModel() {
    private val repo = RepoProvider.repo

    val workouts: StateFlow<List<WorkoutEntity>> =
        Session.userId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repo.observeWorkouts(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _aiSuggestion = MutableStateFlow<String?>(null)
    val aiSuggestion: StateFlow<String?> = _aiSuggestion

    init {
        generateAiSuggestion()
    }

    fun generateAiSuggestion() {
        val uid = Session.userId.value ?: return
        viewModelScope.launch {
            val profile = repo.observeProfile(uid).first()
            val recentWorkouts = workouts.value.take(5)
            
            _aiSuggestion.value = suggestWorkout(profile, recentWorkouts)
        }
    }

    private fun suggestWorkout(profile: ProfileEntity?, recent: List<WorkoutEntity>): String {
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        
        // Simple logic acting as "AI" based on profile and history
        return when {
            dayOfWeek == Calendar.SUNDAY -> "Hoje é dia de descanso ativo. Que tal uma caminhada leve de 30 min?"
            profile?.goal == "Perder Peso" -> "Foco em Cardio! Sugestão: 40 min de passadeira ou bicicleta."
            profile?.goal == "Ganhar massa muscular" -> {
                val lastTitle = recent.firstOrNull()?.title?.lowercase() ?: ""
                if (lastTitle.contains("peito")) "Hoje o foco é Costas e Bíceps para equilibrar o treino de ontem."
                else "Hoje o foco é Peito e Tríceps. Vamos puxar ferro!"
            }
            else -> "Treino de Corpo Inteiro: 4 séries de Agachamento, Flexões e Prancha."
        }
    }

    fun addWorkout(
        title: String,
        durationMin: Int,
        kcal: Int,
        dateTime: String,
        createdAtEpochMs: Long = System.currentTimeMillis()
    ) {
        val uid = Session.userId.value ?: return
        viewModelScope.launch {
            repo.addWorkout(uid, title, durationMin, kcal, dateTime, createdAtEpochMs)
        }
    }

    fun deleteWorkout(workout: WorkoutEntity) {
        val uid = Session.userId.value ?: return
        viewModelScope.launch { repo.deleteWorkout(uid, workout.id) }
    }

    fun updateWorkout(workoutId: String, title: String, durationMin: Int, kcal: Int, dateTime: String) {
        val uid = Session.userId.value ?: return
        viewModelScope.launch { repo.updateWorkout(uid, workoutId, title, durationMin, kcal, dateTime) }
    }
}
