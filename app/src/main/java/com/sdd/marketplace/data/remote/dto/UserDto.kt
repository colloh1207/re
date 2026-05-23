package com.sdd.marketplace.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    @SerialName("id") val id: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("email") val email: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("is_seller") val isSeller: Boolean = false,
    @SerialName("rating") val rating: Double = 0.0,
    @SerialName("review_count") val reviewCount: Int = 0,
    @SerialName("follower_count") val followerCount: Int = 0,
    @SerialName("following_count") val followingCount: Int = 0,
    @SerialName("product_count") val productCount: Int = 0,
    @SerialName("sold_count") val soldCount: Int = 0,
    @SerialName("response_rate") val responseRate: Int = 0,
    @SerialName("location") val location: String? = null,
    @SerialName("joined_at") val joinedAt: String = "",
    @SerialName("is_online") val isOnline: Boolean = false,
    @SerialName("last_seen") val lastSeen: String? = null
)
