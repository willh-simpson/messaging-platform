package com.messaging.messagingplatform.data.api

import com.messaging.messagingplatform.data.model.ApiResponse
import com.messaging.messagingplatform.data.model.AuthResponse
import com.messaging.messagingplatform.data.model.LoginRequest
import com.messaging.messagingplatform.data.model.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<AuthResponse>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): ApiResponse<AuthResponse>
}