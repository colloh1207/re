package com.sdd.marketplace.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BoostDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("product_ids") val productIds: List<String>? = null,
    @SerialName("tier_id") val tierId: String,
    @SerialName("tier_name") val tierName: String? = null,
    val status: String = "pending",
    @SerialName("payment_status") val paymentStatus: String = "pending",
    @SerialName("payment_reference") val paymentReference: String? = null,
    @SerialName("paystack_auth_url") val paystackAuthUrl: String? = null,
    val currency: String = "USD",
    @SerialName("amount_paid") val amountPaid: Double? = null,
    @SerialName("views_count") val viewsCount: Int? = null,
    @SerialName("impressions_guaranteed") val impressionsGuaranteed: Int? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)
