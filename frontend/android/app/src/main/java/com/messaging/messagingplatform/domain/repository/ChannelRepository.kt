package com.messaging.messagingplatform.domain.repository

import com.messaging.messagingplatform.domain.model.Channel

interface ChannelRepository {
    suspend fun getChannels(): Result<List<Channel>>
    suspend fun createChannel(name: String, description: String?): Result<Channel>
}