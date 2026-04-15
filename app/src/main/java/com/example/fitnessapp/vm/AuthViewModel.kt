package com.example.fitnessapp.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fitnessapp.data.RepoProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val error: String? = null
)

class AuthViewModel : ViewModel() {
    private val repo = RepoProvider.repo

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value = AuthState(isLoading = true)
            try {
                val uid = repo.login(email.trim(), pass)
                if (uid == null) {
                    _state.value = AuthState(error = "Credenciais inválidas")
                    return@launch
                }
                Session.setSession(uid)
                _state.value = AuthState()
                onSuccess()
            } catch (e: Exception) {
                _state.value = AuthState(error = e.message ?: "Erro ao entrar")
            }
        }
    }

    fun register(name: String, email: String, pass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _state.value = AuthState(isLoading = true)
            try {
                val uid = repo.register(name.trim(), email.trim(), pass)
                Session.setSession(uid)
                _state.value = AuthState()
                onSuccess()
            } catch (e: Exception) {
                _state.value = AuthState(error = e.message ?: "Erro ao registar")
            }
        }
    }
}
