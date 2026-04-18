package com.example.fitnessapp.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.data.BodyMetricsEntity
import com.example.fitnessapp.data.RepoProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class BodyMetricsViewModel : ViewModel() {
    private val repo = RepoProvider.repo

    val history: StateFlow<List<BodyMetricsEntity>> =
        Session.userId.flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repo.observeBodyMetrics(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addEntry(
        weightKg: Float,
        waistCm: Float,
        chestCm: Float,
        armCm: Float,
        note: String,
        photoPath: String
    ) {
        val uid = Session.userId.value ?: return
        viewModelScope.launch {
            repo.addBodyMetrics(uid, weightKg, waistCm, chestCm, armCm, note, photoPath)
        }
    }

    fun updateEntry(entry: BodyMetricsEntity) {
        val uid = Session.userId.value ?: return
        viewModelScope.launch {
            repo.updateBodyMetrics(uid, entry)
        }
    }

    fun deleteEntry(entryId: String) {
        val uid = Session.userId.value ?: return
        viewModelScope.launch {
            repo.deleteBodyMetrics(uid, entryId)
        }
    }
}
