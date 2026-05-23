package com.sdd.marketplace.data.repository

import com.sdd.marketplace.domain.model.*
import com.sdd.marketplace.domain.repository.CouponRepository
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
import kotlin.random.Random

@Serializable
data class CouponDto(
    val id: String = "",
    val code: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("discount_type") val discountType: String = "PERCENTAGE",
    @SerialName("discount_value") val discountValue: Double = 10.0,
    @SerialName("min_order_value") val minOrderValue: Double = 0.0,
    @SerialName("max_discount") val maxDiscount: Double? = null,
    @SerialName("is_used") val isUsed: Boolean = false,
    @SerialName("expires_at") val expiresAt: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("order_id") val orderId: String? = null,
    val description: String = ""
)

@Singleton
class CouponRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : CouponRepository {

    private val postgrest get() = supabase.postgrest
    private val auth get() = supabase.auth

    override fun getMyCoupons(): Flow<List<Coupon>> = flow {
        try {
            val userId = auth.currentUserOrNull()?.id ?: run { emit(emptyList()); return@flow }
            val dtos = postgrest["coupons"].select {
                filter { eq("user_id", userId) }
                order("created_at", Order.DESCENDING)
            }.decodeList<CouponDto>()
            emit(dtos.map { it.toDomain() })
        } catch (e: Exception) { Timber.e(e); emit(emptyList()) }
    }

    override fun getValidCoupons(): Flow<List<Coupon>> = flow {
        try {
            val userId = auth.currentUserOrNull()?.id ?: run { emit(emptyList()); return@flow }
            val dtos = postgrest["coupons"].select {
                filter { eq("user_id", userId); eq("is_used", false) }
                order("created_at", Order.DESCENDING)
            }.decodeList<CouponDto>()
            emit(dtos.filter { it.expiresAt > java.time.Instant.now().toString() }.map { it.toDomain() })
        } catch (e: Exception) { Timber.e(e); emit(emptyList()) }
    }

    override suspend fun generateDeliveryCoupon(orderId: String): Result<Coupon> = runCatching {
        val userId = auth.currentUserOrNull()?.id ?: throw Exception("Not authenticated")
        val code = "THANKS${Random.nextInt(10000, 99999)}"
        val expiresAt = java.time.Instant.now().plusSeconds(30 * 24 * 3600L).toString()
        val dto = postgrest["coupons"].insert(mapOf(
            "code" to code,
            "user_id" to userId,
            "discount_type" to "PERCENTAGE",
            "discount_value" to 10.0,
            "min_order_value" to 100.0,
            "max_discount" to 50.0,
            "expires_at" to expiresAt,
            "order_id" to orderId,
            "description" to "Thank you for your purchase! 10% off your next order."
        )).decodeSingle<CouponDto>()
        dto.toDomain()
    }

    override suspend fun applyCoupon(couponCode: String, orderValue: Double): Result<Double> = runCatching {
        val userId = auth.currentUserOrNull()?.id ?: throw Exception("Not authenticated")
        val coupon = postgrest["coupons"].select {
            filter { eq("code", couponCode); eq("user_id", userId); eq("is_used", false) }
            limit(1)
        }.decodeList<CouponDto>().firstOrNull() ?: throw Exception("Invalid or expired coupon")
        if (orderValue < coupon.minOrderValue) throw Exception("Minimum order value is ${coupon.minOrderValue}")
        val discount = when (coupon.discountType) {
            "PERCENTAGE" -> {
                val d = orderValue * coupon.discountValue / 100.0
                if (coupon.maxDiscount != null) minOf(d, coupon.maxDiscount) else d
            }
            else -> coupon.discountValue
        }
        discount
    }

    override suspend fun markCouponUsed(couponId: String): Result<Unit> = runCatching {
        postgrest["coupons"].update(mapOf("is_used" to true)) { filter { eq("id", couponId) } }
    }

    private fun CouponDto.toDomain() = Coupon(
        id, code, userId,
        runCatching { CouponDiscountType.valueOf(discountType) }.getOrDefault(CouponDiscountType.PERCENTAGE),
        discountValue, minOrderValue, maxDiscount, isUsed, expiresAt, createdAt, orderId, description
    )
}
