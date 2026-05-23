package com.sdd.marketplace.domain.repository

import com.sdd.marketplace.domain.model.*
import kotlinx.coroutines.flow.Flow

interface BlockRepository {
    fun getBlockedUsers(): Flow<List<Block>>
    suspend fun blockUser(userId: String, reason: String?): Result<Unit>
    suspend fun unblockUser(userId: String): Result<Unit>
    suspend fun isBlocked(userId: String): Result<Boolean>
    suspend fun reportUser(
        reportedUserId: String,
        category: ReportCategory,
        description: String,
        evidenceUrls: List<String>
    ): Result<Unit>
    suspend fun reportProduct(
        productId: String,
        category: ReportCategory,
        description: String
    ): Result<Unit>
    suspend fun submitSupportTicket(
        category: SupportCategory,
        subject: String,
        description: String,
        attachmentUrls: List<String>
    ): Result<SupportTicket>
    fun getMySupportTickets(): Flow<List<SupportTicket>>
    suspend fun rateApp(rating: Int, note: String?): Result<Unit>
    suspend fun submitBugReport(description: String, steps: String?, attachmentUrls: List<String>): Result<Unit>
}
