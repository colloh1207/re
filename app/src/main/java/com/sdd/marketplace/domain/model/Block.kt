package com.sdd.marketplace.domain.model

data class Block(
    val id: String,
    val blockerId: String,
    val blockedId: String,
    val blockedUser: User?,
    val reason: String?,
    val createdAt: String
)

data class Report(
    val id: String,
    val reporterId: String,
    val reportedUserId: String?,
    val reportedProductId: String?,
    val reportedUser: User?,
    val category: ReportCategory,
    val description: String,
    val status: ReportStatus,
    val evidenceUrls: List<String>,
    val adminNotes: String?,
    val createdAt: String,
    val resolvedAt: String?
)

enum class ReportCategory(val label: String) {
    SPAM("Spam or Misleading"),
    FRAUD("Fraud or Scam"),
    INAPPROPRIATE_CONTENT("Inappropriate Content"),
    COUNTERFEIT("Counterfeit Product"),
    HARASSMENT("Harassment or Abuse"),
    FAKE_LISTING("Fake Listing"),
    COPYRIGHT("Copyright Violation"),
    OTHER("Other")
}

enum class ReportStatus(val label: String) {
    PENDING("Pending Review"),
    UNDER_REVIEW("Under Review"),
    RESOLVED("Resolved"),
    DISMISSED("Dismissed")
}

data class SupportTicket(
    val id: String,
    val userId: String,
    val category: SupportCategory,
    val subject: String,
    val description: String,
    val status: TicketStatus,
    val priority: TicketPriority,
    val attachmentUrls: List<String>,
    val adminResponse: String?,
    val rating: Int?,
    val createdAt: String,
    val updatedAt: String
)

enum class SupportCategory(val label: String) {
    ACCOUNT("Account Issues"),
    PAYMENT("Payment & Billing"),
    ORDER("Order Problems"),
    PRODUCT("Product Issues"),
    SHIPPING("Shipping & Delivery"),
    RETURNS("Returns & Refunds"),
    TECHNICAL("Technical Bug"),
    KYC("Verification Issues"),
    OTHER("Other")
}

enum class TicketStatus(val label: String) {
    OPEN("Open"),
    IN_PROGRESS("In Progress"),
    WAITING_REPLY("Waiting for Your Reply"),
    RESOLVED("Resolved"),
    CLOSED("Closed")
}

enum class TicketPriority(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    URGENT("Urgent")
}
