package com.messaging.messagingplatform.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messaging.messagingplatform.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isAuthenticated: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val mode: AuthMode = AuthMode.LOGIN,
)

enum class AuthMode { LOGIN, REGISTER }

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val token = authRepository.getToken()
            if (!token.isNullOrBlank()) {
                _uiState.update { it.copy(isAuthenticated = true) }
            }
        }
    }

    fun setMode(mode: AuthMode) {
        _uiState.update { it.copy(mode = mode, error = null) }
    }

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "Username and password are required") }

            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            authRepository.login(username.trim(), password)
                .onSuccess { result ->
                    authRepository.saveToken(result.token, result.userId, result.username)
                    _uiState.update { it.copy(isAuthenticated = true, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "Login failed", isLoading = false)  }
                }
        }
    }

    fun register(username: String, email: String, password: String) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(error = "All fields are required") }

            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            authRepository.register(username.trim(), email.trim(), password)
                .onSuccess { result ->
                    authRepository.saveToken(result.token, result.userId, result.username)
                    _uiState.update { it.copy(isAuthenticated = true, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Registration failed") }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}