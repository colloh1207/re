package com.sdd.marketplace.data.repository

import com.sdd.marketplace.data.remote.dto.*
import com.sdd.marketplace.data.mappers.toDomain
import com.sdd.marketplace.domain.model.*
import com.sdd.marketplace.domain.repository.BlockRepository
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlockRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth,
    private val functions: Functions
) : BlockRepository {

    override fun getBlockedUsers(): Flow<List<Block>> = flow {
        val userId = auth.currentUserOrNull()?.id ?: return@flow
        val dtos = postgrest["blocks"].select { filter { eq("blocker_id", userId) } }.decodeList<BlockDto>()
        emit(dtos.map { dto ->
            Block(id = dto.id, blockerId = dto.blockerId, blockedId = dto.blockedId,
                blockedUser = dto.blockedUser?.toDomain(), reason = dto.reason, createdAt = dto.createdAt)
        })
    }.catch { Timber.e(it) }

    override suspend fun blockUser(userId: String, reason: String?): Result<Unit> = runCatching {
        val me = auth.currentUserOrNull()?.id ?: throw Exception("Not authenticated")
        val body = buildJsonObject { put("blocked_id", userId); reason?.let { put("reason", it) } }
        functions.invoke("block-user", body = body)
        Unit
    }

    override suspend fun unblockUser(userId: String): Result<Unit> = runCatching {
        val me = auth.currentUserOrNull()?.id ?: throw Exception("Not authenticated")
        postgrest["blocks"].delete { filter { eq("blocker_id", me); eq("blocked_id", userId) } }
    }

    override suspend fun isBlocked(userId: String): Result<Boolean> = runCatching {
        val me = auth.currentUserOrNull()?.id ?: return@runCatching false
        val count = postgrest["blocks"].select {
            filter { eq("blocker_id", me); eq("blocked_id", userId) }
        }.decodeList<BlockDto>().size
        count > 0
    }

    override suspend fun reportUser(
        reportedUserId: String, category: ReportCategory,
        description: String, evidenceUrls: List<String>
    ): Result<Unit> = runCatching {
        val body = buildJsonObject {
            put("reported_user_id", reportedUserId)
            put("category", category.name.lowercase())
            put("description", description)
            put("evidence_urls", buildJsonArray { evidenceUrls.forEach { add(it) } })
        }
        functions.invoke("report-user", body = body)
        Unit
    }

    override suspend fun reportProduct(
        productId: String, category: ReportCategory, description: String
    ): Result<Unit> = runCatching {
        val body = buildJsonObject {
            put("reported_product_id", productId)
            put("category", category.name.lowercase())
            put("description", description)
        }
        functions.invoke("report-user", body = body)
        Unit
    }

    override suspend fun submitSupportTicket(
        category: SupportCategory, subject: String,
        description: String, attachmentUrls: List<String>
    ): Result<SupportTicket> = runCatching {
        val userId = auth.currentUserOrNull()?.id ?: throw Exception("Not authenticated")
        val row = mapOf(
            "user_id" to userId, "category" to category.name.lowercase(),
            "subject" to subject, "description" to description,
            "attachment_urls" to attachmentUrls, "status" to "open", "priority" to "medium"
        )
        val dto = postgrest["support_tickets"].insert(row).decodeSingle<SupportTicketDto>()
        dto.toDomain()
    }

    override fun getMySupportTickets(): Flow<List<SupportTicket>> = flow {
        val userId = auth.currentUserOrNull()?.id ?: return@flow
        val dtos = postgrest["support_tickets"].select {
            filter { eq("user_id", userId) }
            order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
        }.decodeList<SupportTicketDto>()
        emit(dtos.map { it.toDomain() })
    }.catch { Timber.e(it) }

    override suspend fun rateApp(rating: Int, note: String?): Result<Unit> = runCatching {
        val userId = auth.currentUserOrNull()?.id ?: throw Exception("Not authenticated")
        val row = mapOf("user_id" to userId, "rating" to rating, "note" to (note ?: ""))
        postgrest["app_ratings"].upsert(row) { onConflict = "user_id" }
    }

    override suspend fun submitBugReport(
        description: String, steps: String?, attachmentUrls: List<String>
    ): Result<Unit> = runCatching {
        val userId = auth.currentUserOrNull()?.id ?: throw Exception("Not authenticated")
        val row = mapOf(
            "user_id" to userId, "description" to description,
            "steps" to (steps ?: ""), "attachment_urls" to attachmentUrls
        )
        postgrest["bug_reports"].insert(row)
    }
}
