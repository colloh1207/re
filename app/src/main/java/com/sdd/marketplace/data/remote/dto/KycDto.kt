package com.sdd.marketplace.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KycSubmissionDto(
    @SerialName("id") val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("status") val status: String = "not_submitted",
    @SerialName("submitted_at") val submittedAt: String = "",
    @SerialName("reviewed_at") val reviewedAt: String? = null,
    @SerialName("reviewed_by") val reviewedBy: String? = null,
    @SerialName("rejection_reason") val rejectionReason: String? = null,
    @SerialName("legal_full_name") val legalFullName: String = "",
    @SerialName("date_of_birth") val dateOfBirth: String = "",
    @SerialName("gender") val gender: String = "",
    @SerialName("nationality") val nationality: String = "",
    @SerialName("address_line1") val addressLine1: String = "",
    @SerialName("address_line2") val addressLine2: String? = null,
    @SerialName("city") val city: String = "",
    @SerialName("state") val state: String = "",
    @SerialName("postal_code") val postalCode: String = "",
    @SerialName("country") val country: String = "",
    @SerialName("tax_id") val taxId: String? = null
)

@Serializable
data class KycDocumentDto(
    @SerialName("id") val id: String = "",
    @SerialName("submission_id") val submissionId: String = "",
    @SerialName("type") val type: String = "",
    @SerialName("front_image_url") val frontImageUrl: String = "",
    @SerialName("back_image_url") val backImageUrl: String? = null,
    @SerialName("selfie_url") val selfieUrl: String? = null,
    @SerialName("document_number") val documentNumber: String? = null,
    @SerialName("issuing_country") val issuingCountry: String? = null,
    @SerialName("expiry_date") val expiryDate: String? = null,
    @SerialName("uploaded_at") val uploadedAt: String = ""
)
