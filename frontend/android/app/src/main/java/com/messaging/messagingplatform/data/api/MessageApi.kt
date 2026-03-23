package com.messaging.messagingplatform.data.api

import com.messaging.messagingplatform.data.model.ApiResponse
import com.messaging.messagingplatform.data.model.MessageResponse
import com.messaging.messagingplatform.data.model.PageResponse
import com.messaging.messagingplatform.data.model.PublishMessageRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MessageApi {
    @GET("api/channels/{channel_id}/messages")
    suspend fun getHistory(
        @Path("channel_id") channelId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
    ): ApiResponse<PageResponse<MessageResponse>>

    @POST("api/messages")
    suspend fun publishMessage(@Body request: PublishMessageRequest): ApiResponse<Unit>
}