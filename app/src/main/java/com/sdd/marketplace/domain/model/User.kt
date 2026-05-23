package com.sdd.marketplace.domain.model

data class User(
    val id: String,
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
    val latitude: Double? = null,
    val longitude: Double? = null,
    val joinedAt: String,
    val isOnline: Boolean,
    val lastSeen: String?,
    val kycStatus: String = "not_submitted",
    val isBlocked: Boolean = false,
    val accountAge: Long = 0L,
    val deviceId: String? = null,
    val deviceFingerprint: String? = null,
    val registrationCountry: String? = null,
    val preferredLanguage: String = "en",
    val suspensionStatus: SuspensionStatus = SuspensionStatus.NONE,
    val suspendedUntil: String? = null,
    val warningCount: Int = 0,
    val privacySettings: PrivacySettings = PrivacySettings()
)

data class PrivacySettings(
    val showLocation: Boolean = true,
    val showBio: Boolean = true,
    val showPhone: Boolean = false,
    val countryFilter: String? = null,
    val allowMessagesFrom: String = "everyone"
)

enum class SuspensionStatus {
    NONE, WARNING_1, WARNING_2, TEMPORARILY_SUSPENDED, PERMANENTLY_BANNED, APPEAL_PENDING, APPEAL_APPROVED
}
