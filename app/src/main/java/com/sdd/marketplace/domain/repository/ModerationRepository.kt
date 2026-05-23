package com.sdd.marketplace.domain.repository

import com.sdd.marketplace.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ModerationRepository {
    fun getWarnings(userId: String): Flow<List<ModerationWarning>>
    fun getSuspension(userId: String): Flow<UserSuspension?>
    fun getMySuspension(): Flow<UserSuspension?>
    suspend fun issueWarning(userId: String, reason: String, messageContent: String?): Result<ModerationWarning>
    suspend fun suspendUser(userId: String, type: SuspensionType, reason: String): Result<UserSuspension>
    suspend fun banUser(userId: String, reason: String): Result<UserSuspension>
    suspend fun liftSuspension(userId: String): Result<Unit>
    suspend fun submitAppeal(suspensionId: String, note: String): Result<Unit>
    suspend fun approveAppeal(suspensionId: String): Result<Unit>
    suspend fun rejectAppeal(suspensionId: String): Result<Unit>
    fun getReportedUsers(): Flow<List<com.sdd.marketplace.domain.model.User>>
    suspend fun checkAutoSuspend(userId: String): Result<Boolean>
    suspend fun checkMessageForViolations(content: String): ViolationCheckResult
}

data class ViolationCheckResult(
    val hasViolation: Boolean,
    val violationType: ViolationType?,
    val detectedContent: String?
)

enum class ViolationType { PHONE_NUMBER, LINK, CARD_NUMBER, PROFANITY }
