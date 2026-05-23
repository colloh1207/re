package com.sdd.marketplace.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarUrl: String?,
    val content: String,
    val type: String,
    val imageUrl: String?,
    val latitude: Double?,
    val longitude: Double?,
    val locationAddress: String?,
    val isRead: Boolean,
    val isDelivered: Boolean,
    val isDeleted: Boolean = false,
    val isUnsent: Boolean = false,
    val sentAt: String,
    val editedAt: String?,
    val replyToId: String? = null,
    val replyToContent: String? = null,
    val replyToSenderName: String? = null
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val participantIds: String,
    val participantNamesJson: String,
    val lastMessageContent: String?,
    val lastMessageSentAt: String?,
    val lastMessageType: String?,
    val unreadCount: Int,
    val productId: String?,
    val productTitle: String?,
    val productImageUrl: String?,
    val createdAt: String,
    val updatedAt: String
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val type: String,
    val title: String,
    val body: String,
    val dataJson: String,
    val isRead: Boolean,
    val createdAt: String
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val productId: String,
    val userId: String,
    val addedAt: Long = System.currentTimeMillis()
)
