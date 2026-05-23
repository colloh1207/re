package com.sdd.marketplace.data.repository

import com.sdd.marketplace.core.util.ErrorHandler
import com.sdd.marketplace.core.util.NetworkChecker
import com.sdd.marketplace.data.local.dao.AppPreferencesDao
import com.sdd.marketplace.data.local.dao.SavedAccountDao
import com.sdd.marketplace.data.local.dao.UserDao
import com.sdd.marketplace.data.local.entities.AppPreferencesEntity
import com.sdd.marketplace.data.local.entities.SavedAccountEntity
import com.sdd.marketplace.data.mappers.toDomain
import com.sdd.marketplace.data.remote.dto.UserDto
import com.sdd.marketplace.domain.model.User
import com.sdd.marketplace.domain.repository.AuthRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.Phone
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest,
    private val userDao: UserDao,
    private val appPreferencesDao: AppPreferencesDao,
    private val savedAccountDao: SavedAccountDao,
    private val networkChecker: NetworkChecker
) : AuthRepository {

    override val currentUser: Flow<User?> = auth.sessionStatus
        .map { _ ->
            try {
                val session = auth.currentSessionOrNull() ?: return@map null
                val userId = session.user?.id ?: return@map null
                fetchUserProfile(userId)
            } catch (e: Exception) {
                Timber.e(e, "Error getting current user")
                null
            }
        }

    override val isAuthenticated: Flow<Boolean> = auth.sessionStatus
        .map { auth.currentSessionOrNull() != null }

    override suspend fun signInWithEmail(email: String, password: String): Result<User> = runCatching {
        networkChecker.requireOnline().getOrThrow()
        try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        } catch (e: Exception) {
            throw Exception(ErrorHandler.friendlyMessage(e))
        }
        val userId = auth.currentUserOrNull()?.id
            ?: throw Exception("Authentication failed. Please check your email and password.")
        val user = fetchUserProfile(userId) ?: ensureUserProfile(userId, email = email)
        saveAccount(user)
        user
    }

    override suspend fun verifyOtp(
        emailOrPhone: String,
        otp: String,
        isEmail: Boolean,
        isRecovery: Boolean
    ): Result<User> = runCatching {
        networkChecker.requireOnline().getOrThrow()
        try {
            if (isEmail) {
                val type = if (isRecovery) OtpType.Email.RECOVERY else OtpType.Email.SIGNUP
                auth.verifyEmailOtp(type = type, email = emailOrPhone, token = otp)
            } else {
                auth.verifyPhoneOtp(type = OtpType.Phone.SMS, phone = emailOrPhone, token = otp)
            }
        } catch (e: Exception) {
            throw Exception(ErrorHandler.friendlyMessage(e))
        }
        val userId = auth.currentUserOrNull()?.id
            ?: throw Exception("OTP verification failed. Please try again.")
        if (isEmail) {
            fetchUserProfile(userId) ?: ensureUserProfile(userId, email = emailOrPhone)
        } else {
            fetchUserProfile(userId) ?: ensureUserProfile(userId)
        }
    }

    override suspend fun signUpWithEmail(
        fullName: String, email: String, password: String, referralCode: String?
    ): Result<User> = runCatching {
        networkChecker.requireOnline().getOrThrow()
        try {
            auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = buildJsonObject {
                    put("full_name", fullName)
                }
            }
        } catch (e: Exception) {
            throw Exception(ErrorHandler.friendlyMessage(e))
        }

        val userId = auth.currentUserOrNull()?.id
            ?: auth.currentSessionOrNull()?.user?.id

        if (userId != null) {
            val user = ensureUserProfile(userId, fullName = fullName, email = email)
            if (!referralCode.isNullOrBlank()) {
                applyReferralCode(referralCode)
            }
            saveAccount(user)
            user
        } else {
            User(
                id = "pending_verification",
                fullName = fullName, email = email,
                avatarUrl = null, bio = null, isVerified = false, isSeller = true,
                rating = 0.0, reviewCount = 0, followerCount = 0, followingCount = 0,
                productCount = 0, soldCount = 0, responseRate = 100, location = null,
                joinedAt = "", isOnline = false, lastSeen = null
            )
        }
    }

    override suspend fun signInAnonymously(): Result<User> = runCatching {
        try {
            auth.signInAnonymously()
        } catch (e: Exception) {
            throw Exception(ErrorHandler.friendlyMessage(e))
        }
        val userId = auth.currentUserOrNull()?.id ?: throw Exception("Anonymous sign in failed")
        ensureUserProfile(userId, fullName = "Guest")
    }

    override suspend fun signOut(): Result<Unit> = runCatching {
        val userId = getCurrentUserId()
        try {
            auth.signOut()
        } catch (e: Exception) {
            Timber.w(e, "Sign out error (ignored)")
        }
        userDao.clearAll()
        if (userId != null) {
            savedAccountDao.clearActive()
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        networkChecker.requireOnline().getOrThrow()
        try {
            auth.resetPasswordForEmail(email)
        } catch (e: Exception) {
            throw Exception(ErrorHandler.friendlyMessage(e))
        }
    }

    override suspend fun resendEmailOtp(email: String): Result<Unit> = runCatching {
        networkChecker.requireOnline().getOrThrow()
        try {
            auth.resendEmail(OtpType.Email.SIGNUP, email)
        } catch (e: Exception) {
            throw Exception(ErrorHandler.friendlyMessage(e))
        }
    }

    override suspend fun updatePassword(newPassword: String): Result<Unit> = runCatching {
        networkChecker.requireOnline().getOrThrow()
        try {
            auth.updateUser { password = newPassword }
        } catch (e: Exception) {
            throw Exception(ErrorHandler.friendlyMessage(e))
        }
    }

    override suspend fun changeEmail(newEmail: String): Result<Unit> = runCatching {
        networkChecker.requireOnline().getOrThrow()
        try {
            auth.updateUser { email = newEmail }
        } catch (e: Exception) {
            throw Exception(ErrorHandler.friendlyMessage(e))
        }
    }

    override suspend fun deleteAccount(): Result<Unit> = runCatching {
        networkChecker.requireOnline().getOrThrow()
        val userId = getCurrentUserId() ?: throw Exception("Not authenticated")
        try {
            postgrest["users"].delete { filter { eq("id", userId) } }
        } catch (e: Exception) {
            Timber.w(e, "Error deleting user profile (proceeding with sign out)")
        }
        try {
            auth.signOut()
        } catch (e: Exception) {
            Timber.w(e, "Error signing out after delete")
        }
        userDao.clearAll()
        savedAccountDao.delete(userId)
    }

    override suspend fun refreshSession(): Result<Unit> = runCatching {
        try {
            auth.refreshCurrentSession()
        } catch (e: Exception) {
            throw Exception(ErrorHandler.friendlyMessage(e))
        }
    }

    override fun getCurrentUserId(): String? = auth.currentUserOrNull()?.id

    override fun getCurrentUserEmail(): String? = auth.currentUserOrNull()?.email

    override fun isGuest(): Boolean {
        val user = auth.currentUserOrNull() ?: return false
        return user.email == null && user.phone == null
    }

    override suspend fun validateReferralCode(code: String): Result<Boolean> = runCatching {
        if (code.isBlank()) return@runCatching false
        try {
            val result = postgrest["users"].select {
                filter { eq("referral_code", code.uppercase()) }
            }.decodeSingleOrNull<UserDto>()
            result != null
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun applyReferralCode(code: String): Result<Unit> = runCatching {
        if (code.isBlank()) return@runCatching
        val userId = getCurrentUserId() ?: return@runCatching
        try {
            postgrest["users"].update(mapOf("referred_by" to code.uppercase())) {
                filter { eq("id", userId) }
            }
            appPreferencesDao.set(AppPreferencesEntity("referral_applied", code.uppercase()))
        } catch (e: Exception) {
            Timber.w(e, "Could not apply referral code")
        }
    }

    private suspend fun saveAccount(user: User) {
        try {
            savedAccountDao.clearActive()
            savedAccountDao.insert(
                SavedAccountEntity(
                    userId = user.id,
                    email = user.email,
                    fullName = user.fullName,
                    avatarUrl = user.avatarUrl,
                    isActive = true
                )
            )
        } catch (e: Exception) {
            Timber.w(e, "Could not save account")
        }
    }

    private suspend fun fetchUserProfile(userId: String): User? {
        return try {
            val dto = postgrest["users"].select {
                filter { eq("id", userId) }
            }.decodeSingleOrNull<UserDto>()
            dto?.toDomain()
        } catch (e: Exception) {
            Timber.e(e, "Error fetching user profile")
            null
        }
    }

    private suspend fun ensureUserProfile(
        userId: String, fullName: String = "User",
        email: String? = null
    ): User {
        val existing = fetchUserProfile(userId)
        if (existing != null) return existing

        val username = (email?.substringBefore("@") ?: "user").lowercase()
            .replace(Regex("[^a-z0-9_]"), "_")

        val referralCode = "SDD-${userId.take(6).uppercase()}"

        val newUser = mapOf(
            "id" to userId, "full_name" to fullName,
            "email" to email,
            "username" to username, "referral_code" to referralCode,
            "is_verified" to false, "is_seller" to true,
            "rating" to 0.0, "review_count" to 0,
            "follower_count" to 0, "following_count" to 0,
            "product_count" to 0, "sold_count" to 0, "response_rate" to 100
        ).filter { it.value != null }

        try {
            postgrest["users"].upsert(newUser)
        } catch (e: Exception) {
            Timber.e(e, "Error creating user profile")
        }
        return fetchUserProfile(userId) ?: User(
            id = userId, fullName = fullName, email = email,
            avatarUrl = null, bio = null, isVerified = false, isSeller = true,
            rating = 0.0, reviewCount = 0, followerCount = 0, followingCount = 0,
            productCount = 0, soldCount = 0, responseRate = 100, location = null,
            joinedAt = "", isOnline = false, lastSeen = null
        )
    }
}

private fun buildJsonObject(builder: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit) =
    kotlinx.serialization.json.buildJsonObject(builder)

private fun kotlinx.serialization.json.JsonObjectBuilder.put(key: String, value: String?) {
    if (value != null) put(key, kotlinx.serialization.json.JsonPrimitive(value))
}

private fun kotlinx.serialization.json.JsonObjectBuilder.put(key: String, value: Boolean) {
    put(key, kotlinx.serialization.json.JsonPrimitive(value))
}

private fun kotlinx.serialization.json.JsonObjectBuilder.put(key: String, value: Double) {
    put(key, kotlinx.serialization.json.JsonPrimitive(value))
}

private fun kotlinx.serialization.json.JsonObjectBuilder.put(key: String, value: Int) {
    put(key, kotlinx.serialization.json.JsonPrimitive(value))
}
