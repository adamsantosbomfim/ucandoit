package com.example.fitnessapp.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.data.MealEntity
import com.example.fitnessapp.data.RepoProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel : ViewModel() {
    private val repo = RepoProvider.repo

    val mealHistory: StateFlow<Map<String, List<MealEntity>>> =
        Session.userId.flatMapLatest { id ->
            if (id == null) flowOf(emptyMap())
            else repo.observeAllMealsHistory(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}
