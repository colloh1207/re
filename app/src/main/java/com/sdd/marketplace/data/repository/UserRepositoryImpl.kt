package com.sdd.marketplace.data.repository

import android.content.Context
import android.net.Uri
import com.sdd.marketplace.BuildConfig
import com.sdd.marketplace.data.local.dao.UserDao
import com.sdd.marketplace.data.local.entities.UserEntity
import com.sdd.marketplace.data.mappers.toDomain
import com.sdd.marketplace.data.remote.dto.UserDto
import com.sdd.marketplace.domain.model.User
import com.sdd.marketplace.domain.repository.AuthRepository
import com.sdd.marketplace.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.auth.Auth
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val storage: Storage,
    private val userDao: UserDao,
    private val auth: Auth,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : UserRepository {

    override fun getUser(userId: String): Flow<User?> = getUserProfile(userId)

    override fun getUserProfile(userId: String): Flow<User?> = flow {
        try {
            val dto = postgrest["users"].select { filter { eq("id", userId) } }.decodeSingleOrNull<UserDto>()
            if (dto != null) { userDao.insertUser(dto.toEntity()); emit(dto.toDomain()) }
            else emitAll(userDao.getUserById(userId).map { it?.toDomain() })
        } catch (e: Exception) {
            Timber.e(e); emitAll(userDao.getUserById(userId).map { it?.toDomain() })
        }
    }

    override fun getMyProfile(): Flow<User?> = flow {
        val userId = auth.currentUserOrNull()?.id ?: return@flow
        emitAll(getUserProfile(userId))
    }

    override suspend fun updateProfile(updates: Map<String, Any>): Result<User> = runCatching {
        val userId = auth.currentUserOrNull()?.id ?: throw Exception("Not authenticated")
        postgrest["users"].update(updates) { filter { eq("id", userId) } }.decodeSingle<UserDto>().toDomain()
    }

    override suspend fun followUser(userId: String): Result<Unit> = runCatching {
        val me = auth.currentUserOrNull()?.id ?: throw Exception("Not authenticated")
        runCatching { postgrest["followers"].insert(mapOf("follower_id" to me, "following_id" to userId)) }
        runCatching {
            postgrest.rpc("increment_follower_count", buildJsonObject {
                put("target_user_id", userId)
            })
        }
    }

    override suspend fun unfollowUser(userId: String): Result<Unit> = runCatching {
        val me = auth.currentUserOrNull()?.id ?: throw Exception("Not authenticated")
        postgrest["followers"].delete { filter { eq("follower_id", me); eq("following_id", userId) } }
        runCatching {
            postgrest.rpc("decrement_follower_count", buildJsonObject {
                put("target_user_id", userId)
            })
        }
    }

    override fun isFollowing(userId: String): Flow<Boolean> = flow {
        val me = auth.currentUserOrNull()?.id ?: return@flow emit(false)
        try {
            val count = postgrest["followers"].select {
                filter { eq("follower_id", me); eq("following_id", userId) }
            }.decodeList<Map<String, String>>().size
            emit(count > 0)
        } catch (e: Exception) { emit(false) }
    }

    override fun getFollowers(userId: String): Flow<List<User>> = flow {
        try {
            // Join followers table with users
            val rows = postgrest["followers"].select {
                filter { eq("following_id", userId) }
            }.decodeList<Map<String, String>>()
            val followerIds = rows.mapNotNull { it["follower_id"] }
            if (followerIds.isEmpty()) { emit(emptyList()); return@flow }
            val users = postgrest["users"].select {
                filter { isIn("id", followerIds) }
            }.decodeList<UserDto>()
            emit(users.map { it.toDomain() })
        } catch (e: Exception) { Timber.e(e); emit(emptyList()) }
    }

    override fun getFollowing(userId: String): Flow<List<User>> = flow {
        try {
            val rows = postgrest["followers"].select {
                filter { eq("follower_id", userId) }
            }.decodeList<Map<String, String>>()
            val followingIds = rows.mapNotNull { it["following_id"] }
            if (followingIds.isEmpty()) { emit(emptyList()); return@flow }
            val users = postgrest["users"].select {
                filter { isIn("id", followingIds) }
            }.decodeList<UserDto>()
            emit(users.map { it.toDomain() })
        } catch (e: Exception) { Timber.e(e); emit(emptyList()) }
    }

    override fun getFollowingIds(): Flow<Set<String>> = flow {
        val me = auth.currentUserOrNull()?.id ?: return@flow emit(emptySet())
        try {
            val rows = postgrest["followers"].select {
                filter { eq("follower_id", me) }
            }.decodeList<Map<String, String>>()
            emit(rows.mapNotNull { it["following_id"] }.toSet())
        } catch (e: Exception) { emit(emptySet()) }
    }

    override suspend fun uploadAvatar(uri: Uri): Result<String> = runCatching {
        val userId = auth.currentUserOrNull()?.id ?: throw Exception("Not authenticated")
        val fileName = "avatars/$userId/avatar.jpg"
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw Exception("Could not read URI: $uri")
        storage.from(BuildConfig.BUCKET_AVATARS).upload(fileName, bytes) {
            upsert = true
        }
        val url = storage.from(BuildConfig.BUCKET_AVATARS).publicUrl(fileName)
        postgrest["users"].update(mapOf("avatar_url" to url)) { filter { eq("id", userId) } }
        url
    }

    override suspend fun updateFcmToken(token: String): Result<Unit> = runCatching {
        val userId = auth.currentUserOrNull()?.id ?: throw Exception("Not authenticated")
        postgrest["users"].update(mapOf("fcm_token" to token)) { filter { eq("id", userId) } }
    }

    override suspend fun setOnlineStatus(isOnline: Boolean): Result<Unit> = runCatching {
        val userId = auth.currentUserOrNull()?.id ?: throw Exception("Not authenticated")
        postgrest["users"].update(mapOf("is_online" to isOnline, "last_seen" to java.time.Instant.now().toString())) {
            filter { eq("id", userId) }
        }
    }

    private fun UserDto.toEntity() = UserEntity(
        id = id, fullName = fullName, email = email, phone = phone, avatarUrl = avatarUrl, bio = bio,
        isVerified = isVerified, isSeller = isSeller, rating = rating, reviewCount = reviewCount,
        followerCount = followerCount, followingCount = followingCount, productCount = productCount,
        soldCount = soldCount, responseRate = responseRate, location = location,
        joinedAt = joinedAt, isOnline = isOnline, lastSeen = lastSeen
    )

    private fun UserEntity.toDomain() = User(
        id = id, fullName = fullName, email = email, phone = phone, avatarUrl = avatarUrl, bio = bio,
        isVerified = isVerified, isSeller = isSeller, rating = rating, reviewCount = reviewCount,
        followerCount = followerCount, followingCount = followingCount, productCount = productCount,
        soldCount = soldCount, responseRate = responseRate, location = location,
        joinedAt = joinedAt, isOnline = isOnline, lastSeen = lastSeen
    )
}
