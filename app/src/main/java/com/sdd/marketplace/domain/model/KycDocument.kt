package com.sdd.marketplace.domain.model

data class KycSubmission(
    val id: String,
    val userId: String,
    val status: KycStatus,
    val submittedAt: String,
    val reviewedAt: String?,
    val reviewedBy: String?,
    val rejectionReason: String?,
    val documents: List<KycDocument>,
    val personalInfo: KycPersonalInfo
)

data class KycDocument(
    val id: String,
    val submissionId: String,
    val type: KycDocumentType,
    val frontImageUrl: String,
    val backImageUrl: String?,
    val selfieUrl: String?,
    val documentNumber: String?,
    val issuingCountry: String?,
    val expiryDate: String?,
    val uploadedAt: String
)

data class KycPersonalInfo(
    val legalFullName: String,
    val dateOfBirth: String,
    val gender: String,
    val nationality: String,
    val addressLine1: String,
    val addressLine2: String?,
    val city: String,
    val state: String,
    val postalCode: String,
    val country: String,
    val taxId: String?
)

enum class KycStatus(val label: String, val color: Long) {
    NOT_SUBMITTED("Not Verified", 0xFF9E9E9E),
    PENDING("Under Review", 0xFFFF9800),
    APPROVED("Verified", 0xFF4CAF50),
    REJECTED("Rejected", 0xFFF44336),
    RESUBMIT_REQUIRED("Resubmit Required", 0xFFFF5722)
}

enum class KycDocumentType(val label: String) {
    NATIONAL_ID("National ID"),
    PASSPORT("Passport"),
    DRIVERS_LICENSE("Driver's License"),
    VOTER_ID("Voter ID"),
    PAN_CARD("PAN Card"),
    AADHAAR("Aadhaar Card"),
    BUSINESS_REGISTRATION("Business Registration"),
    GST_CERTIFICATE("GST Certificate")
}
