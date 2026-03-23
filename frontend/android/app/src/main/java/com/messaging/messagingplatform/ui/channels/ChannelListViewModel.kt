package com.messaging.messagingplatform.ui.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messaging.messagingplatform.domain.model.Channel
import com.messaging.messagingplatform.domain.repository.AuthRepository
import com.messaging.messagingplatform.domain.repository.ChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChannelListUiState(
    val channels: List<Channel> = emptyList(),
    val username: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val showCreateDialog: Boolean = false,
)

@HiltViewModel
class ChannelListViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChannelListUiState())
    val uiState: StateFlow<ChannelListUiState> = _uiState.asStateFlow()

    init {
        loadChannels()
        loadUsername()
    }

    private fun loadUsername() {
        viewModelScope.launch {
            val username = authRepository.getUsername() ?: ""

            _uiState.update { it.copy(username = username) }
        }
    }

    fun loadChannels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            channelRepository.getChannels()
                .onSuccess { channels ->
                    _uiState.update { it.copy(channels = channels, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun createChannel(name: String, description: String?) {
        if (name.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showCreateDialog = false) }

            channelRepository.createChannel(name.trim(), description?.trim())
                .onSuccess { channel ->
                    _uiState.update { state ->
                        state.copy(
                            channels = state.channels + channel,
                            isLoading = false,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    fun setShowCreateDialog(show: Boolean) {
        _uiState.update { it.copy(showCreateDialog = show) }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.clearSession()

            onComplete()
        }
    }
}