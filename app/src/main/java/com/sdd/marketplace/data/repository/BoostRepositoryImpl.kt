package com.sdd.marketplace.data.repository

import android.content.Context
import com.sdd.marketplace.data.mappers.toDomain
import com.sdd.marketplace.data.remote.dto.BoostDto
import com.sdd.marketplace.data.remote.dto.ProductDto
import com.sdd.marketplace.domain.model.Boost
import com.sdd.marketplace.domain.model.BoostPaymentStatus
import com.sdd.marketplace.domain.model.BoostStatus
import com.sdd.marketplace.domain.model.Product
import com.sdd.marketplace.domain.repository.BoostRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.client.call.body
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

@Singleton
class BoostRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val functions: Functions,
    private val auth: Auth,
    @ApplicationContext private val context: Context
) : BoostRepository {

    override fun getMyBoosts(): Flow<List<Boost>> = flow {
        val userId = auth.currentUserOrNull()?.id ?: return@flow
        val list = postgrest["boosts"]
            .select {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<BoostDto>()
            .map { it.toDomain() }
        emit(list)
    }.catch { Timber.e(it); emit(emptyList()) }

    override suspend fun createBoost(
        productIds: List<String>,
        tierId: String,
        currency: String
    ): Result<Boost> = runCatching {
        val response = functions.invoke("create-boost") {
            body = buildJsonObject {
                put("product_ids", buildJsonArray { productIds.forEach { add(it) } })
                put("tier_id", tierId)
                put("currency", currency)
            }
        }
        lenientJson.decodeFromString<BoostDto>(response.body()).toDomain()
    }

    override suspend fun pollBoostPayment(boostId: String): Result<Boost> = runCatching {
        val response = functions.invoke("poll-boost-payment") {
            body = buildJsonObject { put("boost_id", boostId) }
        }
        lenientJson.decodeFromString<BoostDto>(response.body()).toDomain()
    }

    override suspend fun cancelBoost(boostId: String): Result<Unit> = runCatching {
        val userId = auth.currentUserOrNull()?.id ?: throw Exception("Not authenticated")
        postgrest["boosts"].update({ set("status", "cancelled") }) {
            filter {
                eq("id", boostId)
                eq("user_id", userId)
            }
        }
    }

    override fun getMyListedProducts(): Flow<List<Product>> = flow {
        val userId = auth.currentUserOrNull()?.id ?: return@flow
        val list = postgrest["products"]
            .select {
                filter {
                    eq("seller_id", userId)
                    eq("is_sold", false)
                }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<ProductDto>()
            .map { it.toDomain() }
        emit(list)
    }.catch { Timber.e(it); emit(emptyList()) }

    private fun BoostDto.toDomain() = Boost(
        id                    = id,
        userId                = userId,
        productIds            = productIds ?: emptyList(),
        tierId                = tierId,
        tierName              = tierName ?: tierId.replaceFirstChar { it.uppercase() },
        status                = BoostStatus.values().firstOrNull { s -> s.name.lowercase() == status.lowercase() } ?: BoostStatus.PENDING,
        paymentStatus         = BoostPaymentStatus.values().firstOrNull { s -> s.name.lowercase() == paymentStatus.lowercase() } ?: BoostPaymentStatus.PENDING,
        paymentReference      = paymentReference,
        paystackAuthUrl       = paystackAuthUrl,
        currency              = currency,
        amountPaid            = amountPaid ?: 0.0,
        viewsCount            = viewsCount ?: 0,
        impressionsGuaranteed = impressionsGuaranteed ?: 0,
        startedAt             = startedAt,
        expiresAt             = expiresAt,
        createdAt             = createdAt
    )
}
