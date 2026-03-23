package com.messaging.messagingplatform.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.messaging.messagingplatform.domain.model.Message
import com.messaging.messagingplatform.domain.repository.AuthRepository
import com.messaging.messagingplatform.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val currentUserId: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun init(channelId: String) {
        viewModelScope.launch {
            val userId = authRepository.getUserId() ?: ""
            val token = authRepository.getToken() ?: ""

            _uiState.update { it.copy(currentUserId = userId) }

            // layer live messages on top of chat history
            loadHistory(channelId)
            connectWebSocket(channelId, token)
        }
    }

    private fun loadHistory(channelId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            messageRepository.getHistory(channelId)
                .onSuccess { messages ->
                    _uiState.update { it.copy(messages = messages, isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    private fun connectWebSocket(channelId: String, token: String) {
        viewModelScope.launch {
            messageRepository
                .observeMessages(channelId, token)
                .catch { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
                .collect { incoming ->
                    _uiState.update { state ->
                        // skip message if it's already in the list to make sure messages aren't duplicated
                        if (state.messages.any { it.messageId == incoming.messageId }) {
                            state
                        } else {
                            state.copy(messages = state.messages + incoming)
                        }
                    }
                }
        }
    }

    fun sendMessage(channelId: String, content: String) {
        if(content.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }

            messageRepository.sendMessage(channelId, content.trim())
                .onFailure { e ->
                    _uiState.update { it.copy(error = "Failed to send: ${e.message}") }
                }

            _uiState.update { it.copy(isSending = false) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()

        messageRepository.disconnectWebSocket()
    }
}