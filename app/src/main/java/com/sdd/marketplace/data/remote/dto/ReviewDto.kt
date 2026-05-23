package com.sdd.marketplace.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReviewDto(
    @SerialName("id") val id: String = "",
    @SerialName("product_id") val productId: String = "",
    @SerialName("reviewer_id") val reviewerId: String = "",
    @SerialName("reviewer") val reviewer: UserDto? = null,
    @SerialName("seller_id") val sellerId: String = "",
    @SerialName("rating") val rating: Int = 5,
    @SerialName("comment") val comment: String = "",
    @SerialName("is_verified_purchase") val isVerifiedPurchase: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("helpful_count") val helpfulCount: Int = 0,
    @SerialName("is_helpful_by_current_user") val isHelpfulByCurrentUser: Boolean = false,
    @SerialName("replies") val replies: List<ReviewReplyDto> = emptyList()
)

@Serializable
data class ReviewReplyDto(
    @SerialName("id") val id: String = "",
    @SerialName("review_id") val reviewId: String = "",
    @SerialName("author_id") val authorId: String = "",
    @SerialName("author") val author: UserDto? = null,
    @SerialName("is_seller") val isSeller: Boolean = false,
    @SerialName("content") val content: String = "",
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class NotificationDto(
    @SerialName("id") val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("type") val type: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("body") val body: String = "",
    @SerialName("data") val data: Map<String, String> = emptyMap(),
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
)
