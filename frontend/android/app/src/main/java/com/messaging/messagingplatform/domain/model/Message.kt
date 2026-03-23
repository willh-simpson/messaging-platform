package com.messaging.messagingplatform.domain.model

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class Message @OptIn(ExperimentalTime::class) constructor(
    val messageId: String,

    val channelId: String,
    val authorId: String,
    val authorUsername: String,
    val content: String,

    val createdAt: Instant,
)