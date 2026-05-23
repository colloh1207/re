package com.sdd.marketplace.domain.repository

import com.sdd.marketplace.domain.model.*
import kotlinx.coroutines.flow.Flow

interface KycRepository {
    fun getMyKycStatus(): Flow<KycSubmission?>
    suspend fun submitKyc(
        personalInfo: KycPersonalInfo,
        documents: List<Pair<KycDocumentType, android.net.Uri>>
    ): Result<KycSubmission>
    suspend fun resubmitKyc(
        submissionId: String,
        documents: List<Pair<KycDocumentType, android.net.Uri>>
    ): Result<KycSubmission>
    suspend fun uploadDocument(uri: android.net.Uri, folder: String): Result<String>
}
