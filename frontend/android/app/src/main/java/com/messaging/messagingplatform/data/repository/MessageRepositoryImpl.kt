package com.messaging.messagingplatform.data.repository

import com.messaging.messagingplatform.BuildConfig
import com.messaging.messagingplatform.data.api.MessageApi
import com.messaging.messagingplatform.data.model.PublishMessageRequest
import com.messaging.messagingplatform.data.websocket.StompClient
import com.messaging.messagingplatform.domain.model.Message
import com.messaging.messagingplatform.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class MessageRepositoryImpl @Inject constructor(
    private val messageApi: MessageApi,
    private val stompClient: StompClient,
) : MessageRepository {
    @OptIn(ExperimentalTime::class)
    override suspend fun getHistory(channelId: String): Result<List<Message>> =
        runCatching {
            messageApi.getHistory(channelId).data.content
                .map { data ->
                    Message(
                        messageId = data.messageId,
                        channelId = data.channelId,
                        authorId = data.authorId,
                        authorUsername = data.authorUsername,
                        content = data.content,
                        createdAt = Instant.parse(data.createdAt)
                    )
                }
                .reversed() // backend returns newest first, so display needs to be oldest first
        }

    override suspend fun sendMessage(channelId: String, content: String): Result<Unit> =
        runCatching {
            messageApi.publishMessage(PublishMessageRequest(channelId, content))
        }

    @OptIn(ExperimentalTime::class)
    override fun observeMessages(channelId: String, token: String): Flow<Message> =
        stompClient
            .observeChannel(BuildConfig.WS_BASE_URL, token, channelId)
            .map { data ->
                Message(
                    messageId = data.messageId,
                    channelId = data.channelId,
                    authorId = data.authorId,
                    authorUsername = data.authorUsername,
                    content = data.content,
                    createdAt = Instant.parse(data.createdAt),
                )
            }

    override fun disconnectWebSocket() = stompClient.disconnect()
}