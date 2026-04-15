package com.example.fitnessapp.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.data.ProfileEntity
import com.example.fitnessapp.data.RepoProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModel : ViewModel() {
    private val repo = RepoProvider.repo

    val profile: StateFlow<ProfileEntity?> =
        Session.userId.flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repo.observeProfile(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(age: Int, goal: String, heightCm: Int, weightKg: Float, notifications: Boolean) {
        val uid = Session.userId.value ?: return
        viewModelScope.launch {
            val profileData = ProfileEntity(
                age = age,
                goal = goal,
                heightCm = heightCm,
                weightKg = weightKg,
                notificationsEnabled = notifications
            )
            repo.upsertProfile(uid, profileData)
        }
    }

    fun logout() {
        Session.userId.value = null
    }
}
