package com.messaging.messagingplatform.data.model

import com.google.gson.annotations.SerializedName

/**
 * Wrapper for every backend response to support common/com.messaging.common.dto.ApiResponse.
 */
data class ApiResponse<T>(
    @SerializedName("success")  val success: Boolean,
    @SerializedName("data")     val data: T,
    @SerializedName("message")  val message: String,
)

data class PageResponse<T>(
    @SerializedName("content")          val content: List<T>,
    @SerializedName("total_elements")   val totalElements: Int,
    @SerializedName("total_pages")      val totalPages: Int,
)

data class AuthResponse(
    @SerializedName("token")    val token: String,
    @SerializedName("user_id")  val userId: String,
    @SerializedName("username") val username: String,
)

data class ChannelResponse(
    @SerializedName("channel_id")   val channelId: String,
    @SerializedName("name")         val name: String,
    @SerializedName("description")  val description: String?,
    @SerializedName("member_count") val memberCount: Int,
)

data class MessageResponse(
    @SerializedName("message_id")       val messageId: String,
    @SerializedName("channel_id")       val channelId: String,
    @SerializedName("author_id")        val authorId: String,
    @SerializedName("author_username")  val authorUsername: String,
    @SerializedName("content")          val content: String,
    @SerializedName("created_at")       val createdAt: String,
)

/**
 * Mirrors delivery-service/com.messaging.deliveryservice.api.dto.MessageDeliveryPayload.
 */
data class MessageDeliveryPayload(
    @SerializedName("message_id")       val messageId: String,
    @SerializedName("channel_id")       val channelId: String,
    @SerializedName("author_id")        val authorId: String,
    @SerializedName("author_username")  val authorUsername: String,
    @SerializedName("content")          val content: String,
    @SerializedName("created_at")       val createdAt: String,
)

/*
 * Request bodies
 */

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
)

data class RegisterRequest(
    @SerializedName("username") val username: String,
    @SerializedName("email")    val email: String,
    @SerializedName("password") val password: String,
)

data class PublishMessageRequest(
    @SerializedName("channel_id")   val channelId: String,
    @SerializedName("content")      val content: String,
)

data class CreateChannelRequest(
    @SerializedName("name")         val name: String,
    @SerializedName("description")  val description: String?,
)