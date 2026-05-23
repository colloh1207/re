package com.sdd.marketplace.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderDto(
    @SerialName("id") val id: String = "",
    @SerialName("buyer_id") val buyerId: String = "",
    @SerialName("buyer") val buyer: UserDto? = null,
    @SerialName("seller_id") val sellerId: String = "",
    @SerialName("seller") val seller: UserDto? = null,
    @SerialName("product_id") val productId: String = "",
    @SerialName("product") val product: ProductDto? = null,
    @SerialName("quantity") val quantity: Int = 1,
    @SerialName("unit_price") val unitPrice: Double = 0.0,
    @SerialName("total_amount") val totalAmount: Double = 0.0,
    @SerialName("currency") val currency: String = "INR",
    @SerialName("status") val status: String = "pending",
    @SerialName("payment_status") val paymentStatus: String = "pending",
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("payment_gateway") val paymentGateway: String? = null,
    @SerialName("payment_transaction_id") val paymentTransactionId: String? = null,
    @SerialName("shipping_address") val shippingAddress: ShippingAddressDto? = null,
    @SerialName("tracking_number") val trackingNumber: String? = null,
    @SerialName("tracking_url") val trackingUrl: String? = null,
    @SerialName("notes") val notes: String? = null,
    @SerialName("buyer_terms_accepted") val buyerTermsAccepted: Boolean = false,
    @SerialName("buyer_terms_accepted_at") val buyerTermsAcceptedAt: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class ShippingAddressDto(
    @SerialName("full_name") val fullName: String = "",
    @SerialName("address_line1") val addressLine1: String = "",
    @SerialName("address_line2") val addressLine2: String? = null,
    @SerialName("city") val city: String = "",
    @SerialName("state") val state: String = "",
    @SerialName("postal_code") val postalCode: String = "",
    @SerialName("country") val country: String = "",
    @SerialName("phone") val phone: String = ""
)

@Serializable
data class PaymentMethodDto(
    @SerialName("id") val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("gateway") val gateway: String = "",
    @SerialName("display_name") val displayName: String = "",
    @SerialName("masked_identifier") val maskedIdentifier: String = "",
    @SerialName("is_default") val isDefault: Boolean = false,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("gateway_token") val gatewayToken: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class OrderTrackingEventDto(
    @SerialName("status") val status: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("timestamp") val timestamp: String = "",
    @SerialName("location") val location: String? = null
)
