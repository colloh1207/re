package com.sdd.marketplace.domain.repository

import com.sdd.marketplace.domain.model.Review
import com.sdd.marketplace.domain.model.ReviewReply
import com.sdd.marketplace.domain.model.ReviewSummary
import kotlinx.coroutines.flow.Flow

interface ReviewRepository {
    fun getProductReviews(productId: String): Flow<List<Review>>
    fun getSellerReviews(sellerId: String): Flow<List<Review>>
    fun getUserReviews(userId: String): Flow<List<Review>>
    fun getMyReviews(): Flow<List<Review>>
    suspend fun getReviewSummary(productId: String): ReviewSummary?
    suspend fun writeReview(productId: String, sellerId: String, rating: Int, comment: String): Result<Review>
    suspend fun canWriteReview(productId: String): Result<Boolean>
    suspend fun markHelpful(reviewId: String): Result<Unit>
    suspend fun unmarkHelpful(reviewId: String): Result<Unit>
    suspend fun replyToReview(reviewId: String, content: String): Result<ReviewReply>
    suspend fun getReviewReplies(reviewId: String): Flow<List<ReviewReply>>
    suspend fun deleteReview(reviewId: String): Result<Unit>
}
