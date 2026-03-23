package com.messaging.messagingplatform.data.repository

import com.messaging.messagingplatform.data.api.ChannelApi
import com.messaging.messagingplatform.data.model.CreateChannelRequest
import com.messaging.messagingplatform.domain.model.Channel
import com.messaging.messagingplatform.domain.repository.ChannelRepository
import javax.inject.Inject

class ChannelRepositoryImpl @Inject constructor(
    private val channelApi: ChannelApi
) : ChannelRepository {
    override suspend fun getChannels(): Result<List<Channel>> =
        runCatching {
            channelApi.getChannels().data.content.map { data ->
                Channel(
                    channelId = data.channelId,
                    name = data.name,
                    description = data.description,
                    memberCount = data.memberCount
                )
            }
        }

    override suspend fun createChannel(name: String, description: String?): Result<Channel> =
        runCatching {
            val data = channelApi.createChannel(CreateChannelRequest(name, description)).data

            Channel(
                channelId = data.channelId,
                name = data.name,
                description = data.description,
                memberCount = data.memberCount,
            )
        }
}