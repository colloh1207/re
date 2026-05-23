package com.sdd.marketplace.domain.model

data class Order(
    val id: String,
    val buyerId: String,
    val buyer: User?,
    val sellerId: String,
    val seller: User?,
    val productId: String,
    val product: Product?,
    val quantity: Int,
    val unitPrice: Double,
    val totalAmount: Double,
    val currency: String,
    val status: OrderStatus,
    val paymentStatus: PaymentStatus,
    val paymentMethod: String?,
    val paymentGateway: PaymentGateway?,
    val paymentTransactionId: String?,
    val shippingAddress: ShippingAddress?,
    val trackingNumber: String?,
    val trackingUrl: String?,
    val notes: String?,
    val buyerTermsAccepted: Boolean,
    val buyerTermsAcceptedAt: String?,
    val createdAt: String,
    val updatedAt: String
)

data class ShippingAddress(
    val fullName: String,
    val addressLine1: String,
    val addressLine2: String?,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String,
    val phone: String
)

enum class OrderStatus(val label: String, val color: Long) {
    PENDING("Pending", 0xFFFF9800),
    CONFIRMED("Confirmed", 0xFF2196F3),
    PROCESSING("Processing", 0xFF9C27B0),
    SHIPPED("Shipped", 0xFF00BCD4),
    OUT_FOR_DELIVERY("Out for Delivery", 0xFF8BC34A),
    DELIVERED("Delivered", 0xFF4CAF50),
    CANCELLED("Cancelled", 0xFFF44336),
    REFUND_REQUESTED("Refund Requested", 0xFFFF5722),
    REFUNDED("Refunded", 0xFF607D8B),
    DISPUTED("Disputed", 0xFFE91E63)
}

enum class PaymentStatus(val label: String) {
    PENDING("Pending"),
    PROCESSING("Processing"),
    SUCCESS("Success"),
    FAILED("Failed"),
    REFUNDED("Refunded"),
    PARTIALLY_REFUNDED("Partially Refunded")
}

enum class PaymentGateway(val label: String) {
    RAZORPAY("Razorpay"),
    PAYPAL("PayPal"),
    STRIPE("Stripe"),
    COD("Cash on Delivery")
}

data class PaymentMethod(
    val id: String,
    val userId: String,
    val gateway: PaymentGateway,
    val displayName: String,
    val maskedIdentifier: String,
    val isDefault: Boolean,
    val isVerified: Boolean,
    val gatewayToken: String?,
    val createdAt: String
)

data class OrderTracking(
    val orderId: String,
    val events: List<TrackingEvent>
)

data class TrackingEvent(
    val status: OrderStatus,
    val description: String,
    val timestamp: String,
    val location: String?
)
