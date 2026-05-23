package com.sdd.marketplace.data.repository

import android.content.Context
import android.net.Uri
import com.sdd.marketplace.core.util.CryptoUtils
import com.sdd.marketplace.data.remote.dto.*
import com.sdd.marketplace.domain.model.*
import com.sdd.marketplace.domain.repository.KycRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.auth.Auth
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KycRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth,
    private val storage: Storage,
    @ApplicationContext private val context: Context
) : KycRepository {

    override fun getMyKycStatus(): Flow<KycSubmission?> = flow {
        val userId = auth.currentUserOrNull()?.id ?: return@flow
        val dto = postgrest["kyc_submissions"].select {
            filter { eq("user_id", userId) }
            order("submitted_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            limit(1)
        }.decodeSingleOrNull<KycSubmissionDto>()
        if (dto != null) {
            val docs = postgrest["kyc_documents"].select {
                filter { eq("submission_id", dto.id) }
            }.decodeList<KycDocumentDto>()
            emit(dto.toDomain(docs))
        } else emit(null)
    }.catch { Timber.e(it); emit(null) }

    override suspend fun submitKyc(
        personalInfo: KycPersonalInfo,
        documents: List<Pair<KycDocumentType, Uri>>
    ): Result<KycSubmission> = runCatching {
        val userId = auth.currentUserOrNull()?.id ?: throw Exception("Not authenticated")
        val submissionRow = mapOf(
            "user_id" to userId, "status" to "pending",
            "legal_full_name" to personalInfo.legalFullName,
            "date_of_birth" to personalInfo.dateOfBirth, "gender" to personalInfo.gender,
            "nationality" to personalInfo.nationality, "address_line1" to personalInfo.addressLine1,
            "address_line2" to (personalInfo.addressLine2 ?: ""),
            "city" to personalInfo.city, "state" to personalInfo.state,
            "postal_code" to personalInfo.postalCode, "country" to personalInfo.country,
            "tax_id" to (personalInfo.taxId ?: "")
        )
        val submission = postgrest["kyc_submissions"].insert(submissionRow).decodeSingle<KycSubmissionDto>()
        val uploadedDocs = documents.map { (type, uri) ->
            val url = uploadDocument(uri, "kyc/${userId}/${submission.id}").getOrThrow()
            mapOf("submission_id" to submission.id, "type" to type.name.lowercase(), "front_image_url" to url)
        }
        postgrest["kyc_documents"].insert(uploadedDocs)
        postgrest["users"].update({ set("kyc_status", "pending") }) { filter { eq("id", userId) } }
        val docs = postgrest["kyc_documents"].select {
            filter { eq("submission_id", submission.id) }
        }.decodeList<KycDocumentDto>()
        submission.toDomain(docs)
    }

    override suspend fun resubmitKyc(
        submissionId: String, documents: List<Pair<KycDocumentType, Uri>>
    ): Result<KycSubmission> = runCatching {
        val userId = auth.currentUserOrNull()?.id ?: throw Exception("Not authenticated")
        postgrest["kyc_submissions"].update({ set("status", "pending") }) { filter { eq("id", submissionId) } }
        val uploadedDocs = documents.map { (type, uri) ->
            val url = uploadDocument(uri, "kyc/${userId}/${submissionId}").getOrThrow()
            mapOf("submission_id" to submissionId, "type" to type.name.lowercase(), "front_image_url" to url)
        }
        postgrest["kyc_documents"].delete { filter { eq("submission_id", submissionId) } }
        postgrest["kyc_documents"].insert(uploadedDocs)
        val sub = postgrest["kyc_submissions"].select { filter { eq("id", submissionId) } }.decodeSingle<KycSubmissionDto>()
        val docs = postgrest["kyc_documents"].select { filter { eq("submission_id", submissionId) } }.decodeList<KycDocumentDto>()
        sub.toDomain(docs)
    }

    override suspend fun uploadDocument(uri: Uri, folder: String): Result<String> = runCatching {
        val bucket = storage.from("kyc-documents")
        val fileName = "$folder/${UUID.randomUUID()}.enc"
        val rawBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw Exception("Could not read URI: $uri")
        // AES-256-GCM encrypt before upload — key stays in Android Keystore
        val encryptedBytes = CryptoUtils.encrypt(CryptoUtils.KEY_KYC, rawBytes)
            .toByteArray(Charsets.UTF_8)
        CryptoUtils.wipe(rawBytes)
        bucket.upload(fileName, encryptedBytes) {
            upsert = false
        }
        bucket.publicUrl(fileName)
    }

    private fun KycSubmissionDto.toDomain(docs: List<KycDocumentDto>) = KycSubmission(
        id = id, userId = userId,
        status = KycStatus.values().firstOrNull { it.name.lowercase() == status } ?: KycStatus.NOT_SUBMITTED,
        submittedAt = submittedAt, reviewedAt = reviewedAt, reviewedBy = reviewedBy,
        rejectionReason = rejectionReason,
        documents = docs.map { d ->
            KycDocument(id = d.id, submissionId = d.submissionId,
                type = KycDocumentType.values().firstOrNull { it.name.lowercase() == d.type } ?: KycDocumentType.NATIONAL_ID,
                frontImageUrl = d.frontImageUrl, backImageUrl = d.backImageUrl,
                selfieUrl = d.selfieUrl, documentNumber = d.documentNumber,
                issuingCountry = d.issuingCountry, expiryDate = d.expiryDate, uploadedAt = d.uploadedAt)
        },
        personalInfo = KycPersonalInfo(
            legalFullName = legalFullName, dateOfBirth = dateOfBirth, gender = gender,
            nationality = nationality, addressLine1 = addressLine1, addressLine2 = addressLine2,
            city = city, state = state, postalCode = postalCode, country = country, taxId = taxId
        )
    )
}
