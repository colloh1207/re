package com.sdd.marketplace.domain.model

data class BoostTier(
    val id: String,
    val name: String,
    val impressions: Int,
    val durationDays: Int,
    val priceUsd: Double,
    val features: List<String>,
    val isPopular: Boolean = false,
    val badgeLabel: String = "",
    val sortOrder: Int
)

data class Boost(
    val id: String,
    val userId: String,
    val productIds: List<String>,
    val tierId: String,
    val tierName: String,
    val status: BoostStatus,
    val paymentStatus: BoostPaymentStatus,
    val paymentReference: String?,
    val paystackAuthUrl: String?,
    val currency: String,
    val amountPaid: Double,
    val viewsCount: Int,
    val impressionsGuaranteed: Int,
    val startedAt: String?,
    val expiresAt: String?,
    val createdAt: String
)

enum class BoostStatus(val label: String) {
    PENDING("Pending Payment"),
    ACTIVE("Active"),
    EXPIRED("Expired"),
    CANCELLED("Cancelled")
}

enum class BoostPaymentStatus(val label: String) {
    PENDING("Awaiting Payment"),
    PAID("Payment Confirmed"),
    FAILED("Payment Failed"),
    REFUNDED("Refunded")
}

object BoostTiers {
    val all = listOf(
        BoostTier(
            id = "starter", name = "Starter", impressions = 500, durationDays = 3,
            priceUsd = 1.99,
            features = listOf(
                "500 guaranteed impressions",
                "3 days active",
                "Category placement",
                "Basic view analytics"
            ),
            sortOrder = 1
        ),
        BoostTier(
            id = "basic", name = "Basic", impressions = 2_500, durationDays = 7,
            priceUsd = 4.99,
            features = listOf(
                "2,500 guaranteed impressions",
                "7 days active",
                "Priority category placement",
                "Click & view tracking",
                "2× search ranking boost"
            ),
            sortOrder = 2
        ),
        BoostTier(
            id = "standard", name = "Standard", impressions = 7_500, durationDays = 14,
            priceUsd = 9.99,
            features = listOf(
                "7,500 guaranteed impressions",
                "14 days active",
                "Homepage featured row",
                "Full analytics dashboard",
                "3× search ranking boost",
                "\"Boosted\" badge on listing"
            ),
            isPopular = true, badgeLabel = "Most Popular",
            sortOrder = 3
        ),
        BoostTier(
            id = "premium", name = "Premium", impressions = 20_000, durationDays = 30,
            priceUsd = 24.99,
            features = listOf(
                "20,000 guaranteed impressions",
                "30 days active",
                "Top-of-feed placement",
                "Real-time analytics",
                "5× search ranking boost",
                "Boost + Verified badges",
                "Push notification to followers"
            ),
            badgeLabel = "Best Value",
            sortOrder = 4
        ),
        BoostTier(
            id = "business", name = "Business", impressions = 75_000, durationDays = 60,
            priceUsd = 49.99,
            features = listOf(
                "75,000 guaranteed impressions",
                "60 days active",
                "Sticky homepage banner",
                "Priority customer support",
                "8× search ranking boost",
                "All premium badges",
                "Push blast to all nearby users",
                "Category exclusivity window"
            ),
            sortOrder = 5
        ),
        BoostTier(
            id = "elite", name = "Elite", impressions = 250_000, durationDays = 90,
            priceUsd = 99.99,
            features = listOf(
                "250,000 guaranteed impressions",
                "90 days active",
                "App splash screen placement",
                "Dedicated account manager",
                "10× search ranking boost",
                "All badges + Gold frame",
                "SMS + push notification blast",
                "Featured in email digest",
                "Category exclusivity for duration"
            ),
            badgeLabel = "Elite",
            sortOrder = 6
        )
    )
}
