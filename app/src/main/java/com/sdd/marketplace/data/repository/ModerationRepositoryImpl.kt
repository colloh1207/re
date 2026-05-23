package com.sdd.marketplace.data.repository

import com.sdd.marketplace.domain.model.*
import com.sdd.marketplace.domain.repository.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class WarningDto(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val reason: String = "",
    @SerialName("warning_number") val warningNumber: Int = 1,
    @SerialName("issued_at") val issuedAt: String = "",
    @SerialName("issued_by") val issuedBy: String = "",
    @SerialName("message_content") val messageContent: String? = null
)

@Serializable
data class SuspensionDto(
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    val type: String = "TEMPORARY_2_DAYS",
    val reason: String = "",
    @SerialName("started_at") val startedAt: String = "",
    @SerialName("ends_at") val endsAt: String? = null,
    @SerialName("appeal_note") val appealNote: String? = null,
    @SerialName("appeal_status") val appealStatus: String = "NONE",
    @SerialName("appeal_submitted_at") val appealSubmittedAt: String? = null,
    @SerialName("reviewed_at") val reviewedAt: String? = null,
    @SerialName("reviewed_by") val reviewedBy: String? = null
)

@Singleton
class ModerationRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : ModerationRepository {

    private val postgrest get() = supabase.postgrest
    private val auth get() = supabase.auth

    override fun getWarnings(userId: String): Flow<List<ModerationWarning>> = flow {
        try {
            val dtos = postgrest["moderation_warnings"].select {
                filter { eq("user_id", userId) }
                order("issued_at", Order.DESCENDING)
            }.decodeList<WarningDto>()
            emit(dtos.map { it.toDomain() })
        } catch (e: Exception) { Timber.e(e); emit(emptyList()) }
    }

    override fun getSuspension(userId: String): Flow<UserSuspension?> = flow {
        try {
            val dto = postgrest["user_suspensions"].select {
                filter { eq("user_id", userId); eq("appeal_status", "NONE") }
                order("started_at", Order.DESCENDING)
                limit(1)
            }.decodeList<SuspensionDto>().firstOrNull()
            emit(dto?.toDomain())
        } catch (e: Exception) { Timber.e(e); emit(null) }
    }

    override fun getMySuspension(): Flow<UserSuspension?> = flow {
        try {
            val userId = auth.currentUserOrNull()?.id ?: run { emit(null); return@flow }
            val dto = postgrest["user_suspensions"].select {
                filter { eq("user_id", userId) }
                order("started_at", Order.DESCENDING)
                limit(1)
            }.decodeList<SuspensionDto>().firstOrNull()
            emit(dto?.toDomain())
        } catch (e: Exception) { Timber.e(e); emit(null) }
    }

    override suspend fun issueWarning(userId: String, reason: String, messageContent: String?): Result<ModerationWarning> = runCatching {
        val currentWarnings = postgrest["moderation_warnings"].select { filter { eq("user_id", userId) } }
            .decodeList<WarningDto>().size
        val warningNum = currentWarnings + 1
        val adminId = auth.currentUserOrNull()?.id ?: ""
        val dto = postgrest["moderation_warnings"].insert(mapOf(
            "user_id" to userId, "reason" to reason,
            "warning_number" to warningNum, "issued_by" to adminId,
            "message_content" to messageContent
        )).decodeSingle<WarningDto>()
        if (warningNum >= 2) {
            suspendUser(userId, SuspensionType.TEMPORARY_2_DAYS, "Auto-suspended after $warningNum warnings")
        }
        postgrest["users"].update(mapOf("warning_count" to warningNum)) { filter { eq("id", userId) } }
        dto.toDomain()
    }

    override suspend fun suspendUser(userId: String, type: SuspensionType, reason: String): Result<UserSuspension> = runCatching {
        val endsAt = when (type) {
            SuspensionType.TEMPORARY_2_DAYS -> java.time.Instant.now().plusSeconds(172800).toString()
            SuspensionType.TEMPORARY_3_DAYS -> java.time.Instant.now().plusSeconds(259200).toString()
            SuspensionType.PERMANENT -> null
        }
        val status = when (type) {
            SuspensionType.PERMANENT -> "PERMANENTLY_BANNED"
            else -> "TEMPORARILY_SUSPENDED"
        }
        val dto = postgrest["user_suspensions"].insert(mapOf(
            "user_id" to userId, "type" to type.name, "reason" to reason, "ends_at" to endsAt
        )).decodeSingle<SuspensionDto>()
        postgrest["users"].update(mapOf("suspension_status" to status, "suspended_until" to endsAt)) {
            filter { eq("id", userId) }
        }
        dto.toDomain()
    }

    override suspend fun banUser(userId: String, reason: String): Result<UserSuspension> =
        suspendUser(userId, SuspensionType.PERMANENT, reason)

    override suspend fun liftSuspension(userId: String): Result<Unit> = runCatching {
        postgrest["user_suspensions"].update(mapOf("appeal_status" to "APPROVED")) {
            filter { eq("user_id", userId) }
        }
        postgrest["users"].update(mapOf("suspension_status" to "NONE", "suspended_until" to null, "warning_count" to 0)) {
            filter { eq("id", userId) }
        }
    }

    override suspend fun submitAppeal(suspensionId: String, note: String): Result<Unit> = runCatching {
        postgrest["user_suspensions"].update(mapOf(
            "appeal_note" to note,
            "appeal_status" to "PENDING",
            "appeal_submitted_at" to java.time.Instant.now().toString()
        )) { filter { eq("id", suspensionId) } }
        val userId = postgrest["user_suspensions"].select { filter { eq("id", suspensionId) } }
            .decodeSingle<SuspensionDto>().userId
        postgrest["users"].update(mapOf("suspension_status" to "APPEAL_PENDING")) {
            filter { eq("id", userId) }
        }
    }

    override suspend fun approveAppeal(suspensionId: String): Result<Unit> = runCatching {
        val dto = postgrest["user_suspensions"].select { filter { eq("id", suspensionId) } }
            .decodeSingle<SuspensionDto>()
        postgrest["user_suspensions"].update(mapOf(
            "appeal_status" to "APPROVED",
            "reviewed_at" to java.time.Instant.now().toString()
        )) { filter { eq("id", suspensionId) } }
        postgrest["users"].update(mapOf(
            "suspension_status" to "APPEAL_APPROVED",
            "suspended_until" to null,
            "kyc_status" to "not_submitted"
        )) { filter { eq("id", dto.userId) } }
    }

    override suspend fun rejectAppeal(suspensionId: String): Result<Unit> = runCatching {
        postgrest["user_suspensions"].update(mapOf(
            "appeal_status" to "REJECTED",
            "reviewed_at" to java.time.Instant.now().toString()
        )) { filter { eq("id", suspensionId) } }
    }

    override fun getReportedUsers(): Flow<List<com.sdd.marketplace.domain.model.User>> = flow {
        emit(emptyList())
    }

    override suspend fun checkAutoSuspend(userId: String): Result<Boolean> = runCatching {
        val badReviewCount = postgrest["reviews"].select {
            filter { eq("seller_id", userId); lte("rating", 2) }
        }.decodeList<Map<String, kotlinx.serialization.json.JsonElement>>().size
        val reportCount = try {
            postgrest["user_reports"].select {
                filter { eq("reported_user_id", userId) }
            }.decodeList<Map<String, kotlinx.serialization.json.JsonElement>>().distinctBy { it["reporter_id"] }.size
        } catch (e: Exception) { 0 }
        val shouldSuspend = badReviewCount >= 3 && reportCount >= 2
        if (shouldSuspend) {
            suspendUser(userId, SuspensionType.TEMPORARY_3_DAYS, "Auto-suspended: $badReviewCount bad reviews and $reportCount reports")
        }
        shouldSuspend
    }

    override suspend fun checkMessageForViolations(content: String): ViolationCheckResult {
        val phoneRegex = Regex("""(\+?\d[\d\s\-().]{8,}\d)""")
        val linkRegex = Regex("""(https?://|www\.)\S+""", RegexOption.IGNORE_CASE)
        val cardRegex = Regex("""\b\d{4}[\s\-]?\d{4}[\s\-]?\d{4}[\s\-]?\d{4}\b""")
        return when {
            linkRegex.containsMatchIn(content) -> ViolationCheckResult(true, ViolationType.LINK, linkRegex.find(content)?.value)
            phoneRegex.containsMatchIn(content) -> ViolationCheckResult(true, ViolationType.PHONE_NUMBER, phoneRegex.find(content)?.value)
            cardRegex.containsMatchIn(content) -> ViolationCheckResult(true, ViolationType.CARD_NUMBER, "****")
            else -> ViolationCheckResult(false, null, null)
        }
    }

    private fun WarningDto.toDomain() = ModerationWarning(id, userId, reason, warningNumber, issuedAt, issuedBy, messageContent)
    private fun SuspensionDto.toDomain() = UserSuspension(
        id, userId,
        runCatching { SuspensionType.valueOf(type) }.getOrDefault(SuspensionType.TEMPORARY_2_DAYS),
        reason, startedAt, endsAt, appealNote,
        runCatching { AppealStatus.valueOf(appealStatus) }.getOrDefault(AppealStatus.NONE),
        appealSubmittedAt, reviewedAt, reviewedBy
    )
}
