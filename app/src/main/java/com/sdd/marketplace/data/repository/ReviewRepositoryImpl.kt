package com.sdd.marketplace.data.repository

import com.sdd.marketplace.data.mappers.toDomain
import com.sdd.marketplace.data.remote.dto.ReviewDto
import com.sdd.marketplace.data.remote.dto.ReviewReplyDto
import com.sdd.marketplace.domain.model.*
import com.sdd.marketplace.domain.repository.AuthRepository
import com.sdd.marketplace.domain.repository.ReviewRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth,
    private val authRepository: AuthRepository
) : ReviewRepository {

    override fun getProductReviews(productId: String): Flow<List<Review>> = flow {
        try {
            val currentUserId = auth.currentUserOrNull()?.id
            val dtos = postgrest["reviews"].select {
                filter { eq("product_id", productId) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }.decodeList<ReviewDto>()
            val helpfulSet = if (currentUserId != null) {
                runCatching {
                    postgrest["review_helpful"].select {
                        filter { eq("user_id", currentUserId) }
                    }.decodeList<Map<String, String>>().mapNotNull { it["review_id"] }.toSet()
                }.getOrElse { emptySet() }
            } else emptySet()
            emit(dtos.map { dto ->
                val replies = runCatching {
                    postgrest["review_replies"].select { filter { eq("review_id", dto.id) } }
                        .decodeList<ReviewReplyDto>().map { r ->
                            ReviewReply(id = r.id, reviewId = r.reviewId, authorId = r.authorId,
                                author = r.author?.toDomain(), isSeller = r.isSeller, content = r.content, createdAt = r.createdAt)
                        }
                }.getOrElse { emptyList() }
                dto.toDomain().copy(
                    isHelpfulByCurrentUser = helpfulSet.contains(dto.id),
                    replies = replies
                )
            })
        } catch (e: Exception) { Timber.e(e); emit(emptyList()) }
    }

    override fun getSellerReviews(sellerId: String): Flow<List<Review>> = flow {
        try {
            val dtos = postgrest["reviews"].select {
                filter { eq("seller_id", sellerId) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }.decodeList<ReviewDto>()
            emit(dtos.map { it.toDomain() })
        } catch (e: Exception) { Timber.e(e); emit(emptyList()) }
    }

    override suspend fun getReviewSummary(productId: String): ReviewSummary? = runCatching {
        val reviews = postgrest["reviews"].select { filter { eq("product_id", productId) } }.decodeList<ReviewDto>()
        val avg = if (reviews.isEmpty()) 0.0 else reviews.sumOf { it.rating }.toDouble() / reviews.size
        val breakdown = (1..5).associateWith { rating -> reviews.count { it.rating == rating } }
        ReviewSummary(averageRating = avg, totalReviews = reviews.size, ratingBreakdown = breakdown)
    }.getOrNull()

    override suspend fun writeReview(productId: String, sellerId: String, rating: Int, comment: String): Result<Review> = runCatching {
        val userId = authRepository.getCurrentUserId() ?: throw Exception("Not authenticated")
        // check account age via edge function
        val isVerifiedPurchase = runCatching {
            postgrest["orders"].select {
                filter { eq("buyer_id", userId); eq("product_id", productId); eq("status", "delivered") }
            }.decodeList<Map<String, String>>().isNotEmpty()
        }.getOrElse { false }
        postgrest["reviews"].insert(mapOf(
            "product_id" to productId, "reviewer_id" to userId, "seller_id" to sellerId,
            "rating" to rating, "comment" to comment, "is_verified_purchase" to isVerifiedPurchase
        )).decodeSingle<ReviewDto>().toDomain()
    }

    override suspend fun canWriteReview(productId: String): Result<Boolean> = runCatching {
        val userId = authRepository.getCurrentUserId() ?: return@runCatching false
        // Account must be 3+ weeks old, no duplicate review
        val existingReview = postgrest["reviews"].select {
            filter { eq("reviewer_id", userId); eq("product_id", productId) }
        }.decodeList<ReviewDto>()
        existingReview.isEmpty()
    }

    override suspend fun markHelpful(reviewId: String): Result<Unit> = runCatching {
        val userId = authRepository.getCurrentUserId() ?: throw Exception("Not authenticated")
        postgrest["review_helpful"].insert(mapOf("review_id" to reviewId, "user_id" to userId))
        postgrest.rpc("increment_review_helpful", buildJsonObject { put("p_review_id", reviewId) })
        Unit
    }

    override suspend fun unmarkHelpful(reviewId: String): Result<Unit> = runCatching {
        val userId = authRepository.getCurrentUserId() ?: throw Exception("Not authenticated")
        postgrest["review_helpful"].delete { filter { eq("review_id", reviewId); eq("user_id", userId) } }
        postgrest.rpc("decrement_review_helpful", buildJsonObject { put("p_review_id", reviewId) })
        Unit
    }

    override suspend fun replyToReview(reviewId: String, content: String): Result<ReviewReply> = runCatching {
        val userId = authRepository.getCurrentUserId() ?: throw Exception("Not authenticated")
        // Determine if user is the seller
        val review = postgrest["reviews"].select { filter { eq("id", reviewId) } }.decodeSingle<ReviewDto>()
        val isSeller = review.sellerId == userId
        val dto = postgrest["review_replies"].insert(mapOf(
            "review_id" to reviewId, "author_id" to userId,
            "is_seller" to isSeller, "content" to content
        )).decodeSingle<ReviewReplyDto>()
        ReviewReply(id = dto.id, reviewId = dto.reviewId, authorId = dto.authorId,
            author = dto.author?.toDomain(), isSeller = dto.isSeller, content = dto.content, createdAt = dto.createdAt)
    }

    override suspend fun getReviewReplies(reviewId: String): Flow<List<ReviewReply>> = flow {
        val dtos = postgrest["review_replies"].select { filter { eq("review_id", reviewId) } }.decodeList<ReviewReplyDto>()
        emit(dtos.map { ReviewReply(it.id, it.reviewId, it.authorId, it.author?.toDomain(), it.isSeller, it.content, it.createdAt) })
    }

    override fun getUserReviews(userId: String): Flow<List<Review>> = flow {
        try {
            val dtos = postgrest["reviews"].select {
                filter { eq("seller_id", userId) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }.decodeList<ReviewDto>()
            emit(dtos.map { it.toDomain() })
        } catch (e: Exception) { Timber.e(e); emit(emptyList()) }
    }

    override fun getMyReviews(): Flow<List<Review>> = flow {
        try {
            val userId = auth.currentUserOrNull()?.id ?: run { emit(emptyList()); return@flow }
            val dtos = postgrest["reviews"].select {
                filter { eq("reviewer_id", userId) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }.decodeList<ReviewDto>()
            emit(dtos.map { it.toDomain() })
        } catch (e: Exception) { Timber.e(e); emit(emptyList()) }
    }

    override suspend fun deleteReview(reviewId: String): Result<Unit> = runCatching {
        postgrest["reviews"].delete { filter { eq("id", reviewId) } }
    }
}
