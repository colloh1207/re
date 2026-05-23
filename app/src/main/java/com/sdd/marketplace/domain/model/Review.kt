package com.sdd.marketplace.domain.model

data class Review(
    val id: String,
    val productId: String,
    val reviewerId: String,
    val reviewer: User?,
    val sellerId: String,
    val rating: Int,
    val comment: String,
    val isVerifiedPurchase: Boolean,
    val createdAt: String,
    val helpfulCount: Int,
    val isHelpfulByCurrentUser: Boolean = false,
    val replies: List<ReviewReply> = emptyList()
)

data class ReviewReply(
    val id: String,
    val reviewId: String,
    val authorId: String,
    val author: User?,
    val isSeller: Boolean,
    val content: String,
    val createdAt: String
)

data class ReviewSummary(
    val averageRating: Double,
    val totalReviews: Int,
    val ratingBreakdown: Map<Int, Int>
)

data class Notification(
    val id: String,
    val userId: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val data: Map<String, String>,
    val isRead: Boolean,
    val createdAt: String
)

enum class NotificationType {
    MESSAGE,
    LIKE,
    FOLLOW,
    SALE,
    OFFER,
    ORDER_UPDATE,
    REVIEW,
    REVIEW_REPLY,
    HELPFUL_VOTE,
    KYC_UPDATE,
    PAYMENT,
    SYSTEM
}
