package com.sdd.marketplace.domain.repository

import com.sdd.marketplace.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserProfile(userId: String): Flow<User?>
    fun getMyProfile(): Flow<User?>
    fun getUser(userId: String): Flow<User?>
    suspend fun updateProfile(updates: Map<String, Any>): Result<User>
    suspend fun followUser(userId: String): Result<Unit>
    suspend fun unfollowUser(userId: String): Result<Unit>
    fun isFollowing(userId: String): Flow<Boolean>
    fun getFollowers(userId: String): Flow<List<User>>
    fun getFollowing(userId: String): Flow<List<User>>
    fun getFollowingIds(): Flow<Set<String>>
    suspend fun uploadAvatar(uri: android.net.Uri): Result<String>
    suspend fun updateFcmToken(token: String): Result<Unit>
    suspend fun setOnlineStatus(isOnline: Boolean): Result<Unit>
}
