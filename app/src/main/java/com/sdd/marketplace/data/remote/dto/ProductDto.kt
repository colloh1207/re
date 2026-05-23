package com.sdd.marketplace.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProductDto(
    @SerialName("id") val id: String = "",
    @SerialName("title") val title: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("price") val price: Double = 0.0,
    @SerialName("discount_price") val discountPrice: Double? = null,
    @SerialName("currency") val currency: String = "INR",
    @SerialName("category") val category: String = "",
    @SerialName("brand") val brand: String? = null,
    @SerialName("condition") val condition: String = "",
    @SerialName("stock_quantity") val stockQuantity: Int = 1,
    @SerialName("images") val images: List<String> = emptyList(),
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("attributes") val attributes: Map<String, String> = emptyMap(),
    @SerialName("seller_id") val sellerId: String = "",
    @SerialName("seller") val seller: UserDto? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
    @SerialName("delivery_options") val deliveryOptions: List<String> = emptyList(),
    @SerialName("return_policy") val returnPolicy: String? = null,
    @SerialName("is_negotiable") val isNegotiable: Boolean = false,
    @SerialName("is_featured") val isFeatured: Boolean = false,
    @SerialName("is_boosted") val isBoosted: Boolean = false,
    @SerialName("is_new") val isNew: Boolean = true,
    @SerialName("is_sold") val isSold: Boolean = false,
    @SerialName("view_count") val viewCount: Int = 0,
    @SerialName("favorite_count") val favoriteCount: Int = 0,
    @SerialName("rating") val rating: Double = 0.0,
    @SerialName("review_count") val reviewCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)

@Serializable
data class CreateProductRequest(
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
    @SerialName("price") val price: Double,
    @SerialName("discount_price") val discountPrice: Double? = null,
    @SerialName("category") val category: String,
    @SerialName("brand") val brand: String? = null,
    @SerialName("condition") val condition: String,
    @SerialName("stock_quantity") val stockQuantity: Int,
    @SerialName("images") val images: List<String>,
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("attributes") val attributes: Map<String, String> = emptyMap(),
    @SerialName("seller_id") val sellerId: String,
    @SerialName("location") val location: String? = null,
    @SerialName("latitude") val latitude: Double? = null,
    @SerialName("longitude") val longitude: Double? = null,
    @SerialName("delivery_options") val deliveryOptions: List<String> = emptyList(),
    @SerialName("return_policy") val returnPolicy: String? = null,
    @SerialName("is_negotiable") val isNegotiable: Boolean = false,
    @SerialName("is_new") val isNew: Boolean = true
)
