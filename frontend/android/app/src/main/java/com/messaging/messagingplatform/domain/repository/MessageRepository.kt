package com.messaging.messagingplatform.domain.repository

import com.messaging.messagingplatform.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    suspend fun getHistory(channelId: String): Result<List<Message>>
    suspend fun sendMessage(channelId: String, content: String): Result<Unit>
    fun observeMessages(channelId: String, token: String): Flow<Message>

    fun disconnectWebSocket()
}