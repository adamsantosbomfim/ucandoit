package com.example.fitnessapp.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.data.HydrationEntity
import com.example.fitnessapp.data.MealEntity
import com.example.fitnessapp.data.RepoProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NutritionViewModel : ViewModel() {
    private val repo = RepoProvider.repo

    val meals: StateFlow<List<MealEntity>> =
        Session.userId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repo.observeMeals(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val caloriesToday: StateFlow<Int> =
        meals.map { it.sumOf { m -> m.calories } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val hydration: StateFlow<HydrationEntity> =
        Session.userId.flatMapLatest { id ->
            if (id == null) flowOf(HydrationEntity())
            else repo.observeHydrationToday(id).map { it ?: HydrationEntity() }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HydrationEntity())

    fun addMeal(title: String, calories: Int, time: String, note: String?) {
        val uid = Session.userId.value ?: return
        viewModelScope.launch {
            meals.value
                .firstOrNull { it.title.equals(title, ignoreCase = true) }
                ?.let { existingMeal ->
                    repo.deleteMeal(uid, existingMeal.id)
                }

            repo.addMeal(uid, title, calories, time, note)
        }
    }

    fun addWater(amountMl: Int, goalMl: Int) {
        val uid = Session.userId.value ?: return
        val current = hydration.value
        val updated = current.copy(
            consumedMl = (current.consumedMl + amountMl).coerceAtMost(goalMl),
            goalMl = goalMl,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        viewModelScope.launch { repo.upsertHydrationToday(uid, updated) }
    }

    fun resetWater(goalMl: Int) {
        val uid = Session.userId.value ?: return
        val updated = HydrationEntity(
            consumedMl = 0,
            goalMl = goalMl,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        viewModelScope.launch { repo.upsertHydrationToday(uid, updated) }
    }

    fun deleteMeal(meal: MealEntity) {
        val uid = Session.userId.value ?: return
        // Fix: Use meal.id instead of meal.title
        viewModelScope.launch { repo.deleteMeal(uid, meal.id) }
    }
}
