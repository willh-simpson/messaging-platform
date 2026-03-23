package com.messaging.messagingplatform.domain.model

data class Channel(
    val channelId: String,
    val name: String,
    val description: String?,
    val memberCount: Int,
)