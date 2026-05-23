package com.sdd.marketplace.domain.repository

import com.sdd.marketplace.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    val isAuthenticated: Flow<Boolean>
    suspend fun signInWithEmail(email: String, password: String): Result<User>
    suspend fun verifyOtp(emailOrPhone: String, otp: String, isEmail: Boolean = true, isRecovery: Boolean = false): Result<User>
    suspend fun signUpWithEmail(fullName: String, email: String, password: String, referralCode: String? = null): Result<User>
    suspend fun signInAnonymously(): Result<User>
    suspend fun signOut(): Result<Unit>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun resendEmailOtp(email: String): Result<Unit>
    suspend fun updatePassword(newPassword: String): Result<Unit>
    suspend fun changeEmail(newEmail: String): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
    suspend fun refreshSession(): Result<Unit>
    fun getCurrentUserId(): String?
    fun getCurrentUserEmail(): String?
    fun isGuest(): Boolean
    suspend fun validateReferralCode(code: String): Result<Boolean>
    suspend fun applyReferralCode(code: String): Result<Unit>
}
