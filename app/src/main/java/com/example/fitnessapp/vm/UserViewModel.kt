package com.example.fitnessapp.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.data.RepoProvider
import com.example.fitnessapp.data.UserEntity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class UserViewModel : ViewModel() {
    private val repo = RepoProvider.repo
    
    val user: StateFlow<UserEntity?> = 
        Session.userId.flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repo.observeUser(id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
