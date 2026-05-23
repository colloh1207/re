package com.sdd.marketplace.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BlockDto(
    @SerialName("id") val id: String = "",
    @SerialName("blocker_id") val blockerId: String = "",
    @SerialName("blocked_id") val blockedId: String = "",
    @SerialName("blocked_user") val blockedUser: UserDto? = null,
    @SerialName("reason") val reason: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class ReportDto(
    @SerialName("id") val id: String = "",
    @SerialName("reporter_id") val reporterId: String = "",
    @SerialName("reported_user_id") val reportedUserId: String? = null,
    @SerialName("reported_product_id") val reportedProductId: String? = null,
    @SerialName("category") val category: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("status") val status: String = "pending",
    @SerialName("evidence_urls") val evidenceUrls: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class SupportTicketDto(
    @SerialName("id") val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("category") val category: String = "",
    @SerialName("subject") val subject: String = "",
    @SerialName("description") val description: String = "",
    @SerialName("status") val status: String = "open",
    @SerialName("priority") val priority: String = "medium",
    @SerialName("attachment_urls") val attachmentUrls: List<String> = emptyList(),
    @SerialName("admin_response") val adminResponse: String? = null,
    @SerialName("rating") val rating: Int? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = ""
)
