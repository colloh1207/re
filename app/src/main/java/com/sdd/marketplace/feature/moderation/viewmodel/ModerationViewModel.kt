package com.sdd.marketplace.feature.moderation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdd.marketplace.domain.model.*
import com.sdd.marketplace.domain.repository.ModerationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ModerationUiState(
    val warnings: List<ModerationWarning> = emptyList(),
    val suspension: UserSuspension? = null,
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null
)

data class SuspensionUiState(
    val suspension: UserSuspension? = null,
    val isLoading: Boolean = true,
    val appealNote: String = "",
    val isSubmittingAppeal: Boolean = false,
    val appealSubmitted: Boolean = false,
    val error: String? = null
)

sealed class ModerationEvent {
    data class ShowMessage(val message: String) : ModerationEvent()
    object AppealSubmitted : ModerationEvent()
}

@HiltViewModel
class ModerationViewModel @Inject constructor(
    private val moderationRepository: ModerationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val targetUserId: String = savedStateHandle["userId"] ?: ""

    private val _uiState = MutableStateFlow(ModerationUiState())
    val uiState: StateFlow<ModerationUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ModerationEvent>()
    val events: SharedFlow<ModerationEvent> = _events.asSharedFlow()

    init {
        if (targetUserId.isNotBlank()) {
            loadWarnings()
            loadSuspension()
        }
    }

    private fun loadWarnings() = viewModelScope.launch {
        moderationRepository.getWarnings(targetUserId).collect { warnings ->
            _uiState.update { it.copy(warnings = warnings) }
        }
    }

    private fun loadSuspension() = viewModelScope.launch {
        moderationRepository.getSuspension(targetUserId).collect { suspension ->
            _uiState.update { it.copy(suspension = suspension) }
        }
    }

    fun issueWarning(reason: String, messageContent: String? = null) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        moderationRepository.issueWarning(targetUserId, reason, messageContent)
            .onSuccess { warning ->
                _events.emit(ModerationEvent.ShowMessage("Warning #${warning.warningNumber} issued"))
                loadWarnings()
            }
            .onFailure { _events.emit(ModerationEvent.ShowMessage("Failed: ${it.message}")) }
        _uiState.update { it.copy(isLoading = false) }
    }

    fun suspendUser(type: SuspensionType, reason: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        moderationRepository.suspendUser(targetUserId, type, reason)
            .onSuccess { _events.emit(ModerationEvent.ShowMessage("User suspended")) }
            .onFailure { _events.emit(ModerationEvent.ShowMessage("Failed: ${it.message}")) }
        _uiState.update { it.copy(isLoading = false) }
    }

    fun banUser(reason: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        moderationRepository.banUser(targetUserId, reason)
            .onSuccess { _events.emit(ModerationEvent.ShowMessage("User permanently banned")) }
            .onFailure { _events.emit(ModerationEvent.ShowMessage("Failed: ${it.message}")) }
        _uiState.update { it.copy(isLoading = false) }
    }

    fun liftSuspension() = viewModelScope.launch {
        moderationRepository.liftSuspension(targetUserId)
            .onSuccess { _events.emit(ModerationEvent.ShowMessage("Suspension lifted")) }
    }

    fun approveAppeal(suspensionId: String) = viewModelScope.launch {
        moderationRepository.approveAppeal(suspensionId)
            .onSuccess { _events.emit(ModerationEvent.ShowMessage("Appeal approved — KYC required before access restored")) }
    }

    fun rejectAppeal(suspensionId: String) = viewModelScope.launch {
        moderationRepository.rejectAppeal(suspensionId)
            .onSuccess { _events.emit(ModerationEvent.ShowMessage("Appeal rejected")) }
    }
}

@HiltViewModel
class SuspensionViewModel @Inject constructor(
    private val moderationRepository: ModerationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuspensionUiState())
    val uiState: StateFlow<SuspensionUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ModerationEvent>()
    val events: SharedFlow<ModerationEvent> = _events.asSharedFlow()

    init { loadMySuspension() }

    private fun loadMySuspension() = viewModelScope.launch {
        moderationRepository.getMySuspension().collect { suspension ->
            _uiState.update { it.copy(suspension = suspension, isLoading = false) }
        }
    }

    fun onAppealNoteChanged(note: String) = _uiState.update { it.copy(appealNote = note) }

    fun submitAppeal() = viewModelScope.launch {
        val suspension = _uiState.value.suspension ?: return@launch
        val note = _uiState.value.appealNote.trim()
        if (note.isBlank()) { _uiState.update { it.copy(error = "Please write your appeal note") }; return@launch }
        _uiState.update { it.copy(isSubmittingAppeal = true, error = null) }
        moderationRepository.submitAppeal(suspension.id, note)
            .onSuccess {
                _uiState.update { it.copy(appealSubmitted = true, isSubmittingAppeal = false) }
                _events.emit(ModerationEvent.ShowMessage("Appeal submitted. We'll review it within 24 hours."))
                _events.emit(ModerationEvent.AppealSubmitted)
            }
            .onFailure { e ->
                _uiState.update { it.copy(isSubmittingAppeal = false, error = e.message) }
            }
    }
}
