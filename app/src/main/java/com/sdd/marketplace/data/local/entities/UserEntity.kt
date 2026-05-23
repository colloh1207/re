package com.sdd.marketplace.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val email: String?,
    val phone: String?,
    val avatarUrl: String?,
    val bio: String?,
    val isVerified: Boolean,
    val isSeller: Boolean,
    val rating: Double,
    val reviewCount: Int,
    val followerCount: Int,
    val followingCount: Int,
    val productCount: Int,
    val soldCount: Int,
    val responseRate: Int,
    val location: String?,
    val joinedAt: String,
    val isOnline: Boolean,
    val lastSeen: String?,
    val cachedAt: Long = System.currentTimeMillis()
)
