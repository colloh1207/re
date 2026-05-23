package com.sdd.marketplace.domain.repository

import com.sdd.marketplace.domain.model.Coupon
import kotlinx.coroutines.flow.Flow

interface CouponRepository {
    fun getMyCoupons(): Flow<List<Coupon>>
    fun getValidCoupons(): Flow<List<Coupon>>
    suspend fun generateDeliveryCoupon(orderId: String): Result<Coupon>
    suspend fun applyCoupon(couponCode: String, orderValue: Double): Result<Double>
    suspend fun markCouponUsed(couponId: String): Result<Unit>
}
