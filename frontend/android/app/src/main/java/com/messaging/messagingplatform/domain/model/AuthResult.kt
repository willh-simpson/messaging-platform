package com.messaging.messagingplatform.domain.model

data class AuthResult(
    val token: String,
    val userId: String,
    val username: String,
)
