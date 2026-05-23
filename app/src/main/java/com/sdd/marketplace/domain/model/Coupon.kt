package com.sdd.marketplace.domain.model

data class Coupon(
    val id: String,
    val code: String,
    val userId: String,
    val discountType: CouponDiscountType,
    val discountValue: Double,
    val minOrderValue: Double,
    val maxDiscount: Double?,
    val isUsed: Boolean,
    val expiresAt: String,
    val createdAt: String,
    val orderId: String?,
    val description: String
)

enum class CouponDiscountType { PERCENTAGE, FIXED }

data class ModerationWarning(
    val id: String,
    val userId: String,
    val reason: String,
    val warningNumber: Int,
    val issuedAt: String,
    val issuedBy: String,
    val messageContent: String?
)

data class UserSuspension(
    val id: String,
    val userId: String,
    val type: SuspensionType,
    val reason: String,
    val startedAt: String,
    val endsAt: String?,
    val appealNote: String?,
    val appealStatus: AppealStatus,
    val appealSubmittedAt: String?,
    val reviewedAt: String?,
    val reviewedBy: String?
)

enum class SuspensionType { TEMPORARY_2_DAYS, TEMPORARY_3_DAYS, PERMANENT }
enum class AppealStatus { NONE, PENDING, APPROVED, REJECTED }
