package com.sdd.marketplace.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val discountPrice: Double?,
    val currency: String,
    val category: String,
    val brand: String?,
    val condition: String,
    val stockQuantity: Int,
    val imagesJson: String,
    val tagsJson: String,
    val sellerId: String,
    val sellerName: String,
    val sellerAvatarUrl: String?,
    val sellerIsVerified: Boolean,
    val location: String?,
    val latitude: Double?,
    val longitude: Double?,
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
    val updatedAt: String,
    val cachedAt: Long = System.currentTimeMillis()
)
