package com.messaging.messagingplatform.domain.repository

import com.messaging.messagingplatform.domain.model.AuthResult

interface AuthRepository {
    suspend fun login(username: String, password: String): Result<AuthResult>
    suspend fun register(username: String, email: String, password: String): Result<AuthResult>

    suspend fun saveToken(token: String, userId: String, username: String)
    suspend fun getToken(): String?

    suspend fun getUserId(): String?
    suspend fun getUsername(): String?

    suspend fun clearSession()
}