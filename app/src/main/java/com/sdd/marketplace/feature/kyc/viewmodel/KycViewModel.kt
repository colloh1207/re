package com.sdd.marketplace.feature.kyc.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdd.marketplace.domain.model.*
import com.sdd.marketplace.domain.repository.KycRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KycUiState(
    val submission: KycSubmission? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val step: Int = 0,
    val personalInfo: KycPersonalInfo = KycPersonalInfo("", "", "", "", "", null, "", "", "", "", null),
    val selectedDocType: KycDocumentType = KycDocumentType.NATIONAL_ID,
    val frontImageUri: Uri? = null,
    val backImageUri: Uri? = null,
    val selfieUri: Uri? = null,
    val showLivenessCamera: Boolean = false,
    val livenessCompleted: Boolean = false
)

sealed class KycEvent {
    data class ShowMessage(val message: String) : KycEvent()
}

@HiltViewModel
class KycViewModel @Inject constructor(private val kycRepository: KycRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(KycUiState())
    val uiState: StateFlow<KycUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<KycEvent>()
    val events: SharedFlow<KycEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            kycRepository.getMyKycStatus().collect { submission ->
                _uiState.update { it.copy(submission = submission) }
            }
        }
    }

    fun updatePersonalInfo(info: KycPersonalInfo) = _uiState.update { it.copy(personalInfo = info) }
    fun selectDocType(type: KycDocumentType) = _uiState.update { it.copy(selectedDocType = type) }
    fun setFrontImage(uri: Uri) = _uiState.update { it.copy(frontImageUri = uri) }
    fun setBackImage(uri: Uri) = _uiState.update { it.copy(backImageUri = uri) }
    fun setSelfieImage(uri: Uri) = _uiState.update { it.copy(selfieUri = uri, livenessCompleted = true) }
    fun nextStep() = _uiState.update { it.copy(step = it.step + 1) }
    fun prevStep() = _uiState.update { if (it.step > 0) it.copy(step = it.step - 1) else it }

    fun startLivenessCheck() = _uiState.update { it.copy(showLivenessCamera = true) }

    fun onLivenessComplete(uri: Uri) {
        _uiState.update { it.copy(
            selfieUri = uri,
            livenessCompleted = true,
            showLivenessCamera = false
        )}
    }

    fun onLivenessCancelled() = _uiState.update { it.copy(showLivenessCamera = false) }

    fun retakeLiveness() = _uiState.update { it.copy(
        selfieUri = null,
        livenessCompleted = false,
        showLivenessCamera = true
    )}

    fun submitKyc() = viewModelScope.launch {
        val state = _uiState.value
        val frontUri = state.frontImageUri ?: return@launch
        if (!state.livenessCompleted || state.selfieUri == null) {
            _uiState.update { it.copy(error = "Please complete the liveness check before submitting.") }
            return@launch
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        val docs = mutableListOf(Pair(state.selectedDocType, frontUri))
        state.backImageUri?.let { docs.add(Pair(state.selectedDocType, it)) }
        kycRepository.submitKyc(state.personalInfo, docs)
            .onSuccess { _events.emit(KycEvent.ShowMessage("KYC submitted successfully. Review takes 1-3 business days.")) }
            .onFailure { _uiState.update { s -> s.copy(error = it.message) } }
        _uiState.update { it.copy(isLoading = false) }
    }
}
