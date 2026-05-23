package com.sdd.marketplace.domain.model

data class Chat(
    val id: String,
    val participants: List<User>,
    val lastMessage: Message?,
    val unreadCount: Int,
    val createdAt: String,
    val updatedAt: String,
    val productId: String?,
    val product: Product?
)

data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val sender: User?,
    val content: String,
    val type: MessageType,
    val imageUrl: String?,
    val latitude: Double?,
    val longitude: Double?,
    val locationAddress: String?,
    val isRead: Boolean,
    val isDelivered: Boolean,
    val sentAt: String,
    val editedAt: String?,
    val isDeleted: Boolean = false,
    val isUnsent: Boolean = false,
    val replyToId: String? = null,
    val replyToContent: String? = null,
    val replyToSenderName: String? = null
)

enum class MessageType {
    TEXT,
    IMAGE,
    LOCATION,
    OFFER,
    SYSTEM
}
