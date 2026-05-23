package com.sdd.marketplace.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChatDto(
    @SerialName("id") val id: String = "",
    @SerialName("participants") val participants: List<UserDto> = emptyList(),
    @SerialName("last_message") val lastMessage: MessageDto? = null,
    @SerialName("unread_count") val unreadCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("product_id") val productId: String? = null,
    @SerialName("product") val product: ProductDto? = null
)

@Serializable
data class MessageDto(
    @SerialName("id") val id: String = "",
    @SerialName("chat_id") val chatId: String = "",
    @SerialName("sender_id") val senderId: String = "",
    @SerialName("sender") val sender: UserDto? = null,
    @SerialName("content") val content: String = "",
    @SerialName("type") val type: String = "TEXT",
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
    @SerialName("location_address") val locationAddress: String? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("is_delivered") val isDelivered: Boolean = false,
    @SerialName("sent_at") val sentAt: String = "",
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("reply_to_message_id") val replyToMessageId: String? = null,
    @SerialName("reply_to_content") val replyToContent: String? = null,
    @SerialName("reply_to_sender_name") val replyToSenderName: String? = null
)

@Serializable
data class SendMessageRequest(
    @SerialName("chat_id") val chatId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("content") val content: String,
    @SerialName("type") val type: String = "TEXT",
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
    @SerialName("location_address") val locationAddress: String? = null,
    @SerialName("reply_to_message_id") val replyToMessageId: String? = null
)
