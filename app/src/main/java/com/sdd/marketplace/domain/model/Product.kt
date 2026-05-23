package com.sdd.marketplace.domain.model

data class Product(
    val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val discountPrice: Double?,
    val currency: String = "INR",
    val category: String,
    val brand: String?,
    val condition: String,
    val stockQuantity: Int,
    val images: List<String>,
    val tags: List<String>,
    val attributes: Map<String, String>,
    val sellerId: String,
    val seller: User?,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
    val deliveryOptions: List<String>,
    val returnPolicy: String?,
    val isNegotiable: Boolean,
    val isFeatured: Boolean,
    val isBoosted: Boolean,
    val isNew: Boolean,
    val isSold: Boolean,
    val viewCount: Int,
    val favoriteCount: Int,
    val rating: Double,
    val reviewCount: Int,
    val createdAt: String,
    val updatedAt: String
)

data class Category(
    val id: String,
    val name: String,
    val iconName: String,
    val description: String?
)

enum class ProductCondition(val label: String) {
    NEW("New"),
    LIKE_NEW("Like New"),
    GOOD("Good"),
    FAIR("Fair"),
    POOR("Poor")
}

enum class DeliveryOption(val label: String) {
    FREE_DELIVERY("Free Delivery"),
    PAID_DELIVERY("Paid Delivery"),
    PICKUP_ONLY("Pickup Only"),
    NEGOTIABLE("Negotiable")
}
