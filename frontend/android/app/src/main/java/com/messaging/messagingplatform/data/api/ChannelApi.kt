package com.messaging.messagingplatform.data.api

import com.messaging.messagingplatform.data.model.ApiResponse
import com.messaging.messagingplatform.data.model.ChannelResponse
import com.messaging.messagingplatform.data.model.CreateChannelRequest
import com.messaging.messagingplatform.data.model.PageResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ChannelApi {
    @GET("api/channels")
    suspend fun getChannels(): ApiResponse<PageResponse<ChannelResponse>>

    @POST("api/channels")
    suspend fun createChannel(@Body request: CreateChannelRequest): ApiResponse<ChannelResponse>
}