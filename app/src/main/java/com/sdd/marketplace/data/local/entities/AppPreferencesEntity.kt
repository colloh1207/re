package com.sdd.marketplace.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_preferences")
data class AppPreferencesEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_accounts")
data class SavedAccountEntity(
    @PrimaryKey val userId: String,
    val email: String?,
    val phone: String?,
    val fullName: String,
    val avatarUrl: String?,
    val isActive: Boolean = false,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "referrals")
data class ReferralEntity(
    @PrimaryKey val id: String,
    val referrerId: String,
    val referredEmail: String,
    val status: String,
    val rewardAmount: Double,
    val currency: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    val operation: String,
    val tableName: String,
    val payload: String,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAttemptAt: Long? = null
)
