package com.sdd.marketplace.feature.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdd.marketplace.core.util.ErrorHandler
import com.sdd.marketplace.core.util.NetworkChecker
import com.sdd.marketplace.data.local.dao.AppPreferencesDao
import com.sdd.marketplace.data.local.entities.AppPreferencesEntity
import com.sdd.marketplace.domain.repository.AuthRepository
import com.sdd.marketplace.domain.repository.BlockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null,
    val otpSent: Boolean = false,
    val currentStep: Int = 0,
    val currentUserEmail: String? = null,
    val notificationsEnabled: Boolean = true,
    val pushEnabled: Boolean = true,
    val emailNotificationsEnabled: Boolean = true,
    val offersNotificationsEnabled: Boolean = true,
    val ratingsNotificationsEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false
)

sealed class SettingsEvent {
    object NavigateToLogin : SettingsEvent()
    data class ShowMessage(val message: String) : SettingsEvent()
    data class ShowError(val message: String) : SettingsEvent()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val blockRepository: BlockRepository,
    private val networkChecker: NetworkChecker,
    private val appPreferencesDao: AppPreferencesDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events: SharedFlow<SettingsEvent> = _events.asSharedFlow()

    init {
        _uiState.update { it.copy(currentUserEmail = authRepository.getCurrentUserEmail()) }
        loadNotificationPrefs()
    }

    private fun loadNotificationPrefs() = viewModelScope.launch {
        val notif    = appPreferencesDao.get("notif_all") != "false"
        val push     = appPreferencesDao.get("notif_push") != "false"
        val email    = appPreferencesDao.get("notif_email") != "false"
        val offers   = appPreferencesDao.get("notif_offers") != "false"
        val ratings  = appPreferencesDao.get("notif_ratings") != "false"
        _uiState.update {
            it.copy(
                notificationsEnabled = notif,
                pushEnabled = push,
                emailNotificationsEnabled = email,
                offersNotificationsEnabled = offers,
                ratingsNotificationsEnabled = ratings
            )
        }
    }

    fun sendPasswordResetOtp() = viewModelScope.launch {
        val email = authRepository.getCurrentUserEmail()
        if (email.isNullOrBlank()) {
            _uiState.update { it.copy(error = "No email address on file. Please contact support.") }
            return@launch
        }
        if (!networkChecker.isOnline()) {
            _uiState.update { it.copy(error = "No internet connection. Please check your network.") }
            return@launch
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        authRepository.sendPasswordResetEmail(email)
            .onSuccess { _uiState.update { s -> s.copy(otpSent = true, isLoading = false) } }
            .onFailure { e ->
                _uiState.update { s -> s.copy(error = ErrorHandler.friendlyMessage(e), isLoading = false) }
            }
    }

    fun changePassword(otp: String, newPassword: String) = viewModelScope.launch {
        if (!networkChecker.isOnline()) {
            _uiState.update { it.copy(error = "No internet connection. Please check your network.") }
            return@launch
        }
        if (otp.length != 6) {
            _uiState.update { it.copy(error = "Please enter the 6-digit code from your email.") }
            return@launch
        }
        if (newPassword.length < 8) {
            _uiState.update { it.copy(error = "Password must be at least 8 characters.") }
            return@launch
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        val email = authRepository.getCurrentUserEmail() ?: ""
        authRepository.verifyEmailOtp(email, otp, isRecovery = true)
            .onSuccess {
                authRepository.updatePassword(newPassword)
                    .onSuccess { _events.emit(SettingsEvent.ShowMessage("Password changed successfully!")) }
                    .onFailure { e -> _uiState.update { s -> s.copy(error = ErrorHandler.friendlyMessage(e)) } }
            }
            .onFailure { e -> _uiState.update { s -> s.copy(error = ErrorHandler.friendlyMessage(e)) } }
        _uiState.update { it.copy(isLoading = false) }
    }

    fun changeEmail(newEmail: String) = viewModelScope.launch {
        if (!networkChecker.isOnline()) {
            _uiState.update { it.copy(error = "No internet connection. Please check your network.") }
            return@launch
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            _uiState.update { it.copy(error = "Please enter a valid email address.") }
            return@launch
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        authRepository.changeEmail(newEmail)
            .onSuccess {
                _uiState.update { s -> s.copy(isLoading = false) }
                _events.emit(SettingsEvent.ShowMessage("Verification link sent to $newEmail. Please check your inbox."))
            }
            .onFailure { e ->
                _uiState.update { s -> s.copy(error = ErrorHandler.friendlyMessage(e), isLoading = false) }
            }
    }

    fun deleteAccount(reason: String) = viewModelScope.launch {
        if (!networkChecker.isOnline()) {
            _uiState.update { it.copy(error = "No internet connection. Please check your network.") }
            return@launch
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        authRepository.deleteAccount()
            .onSuccess { _events.emit(SettingsEvent.NavigateToLogin) }
            .onFailure { e ->
                _uiState.update { s -> s.copy(error = ErrorHandler.friendlyMessage(e), isLoading = false) }
            }
    }

    fun signOut() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        authRepository.signOut()
        _uiState.update { it.copy(isLoading = false) }
        _events.emit(SettingsEvent.NavigateToLogin)
    }

    fun toggleNotifications(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
        savePref("notif_all", enabled)
    }

    fun togglePushNotifications(enabled: Boolean) {
        _uiState.update { it.copy(pushEnabled = enabled) }
        savePref("notif_push", enabled)
    }

    fun toggleEmailNotifications(enabled: Boolean) {
        _uiState.update { it.copy(emailNotificationsEnabled = enabled) }
        savePref("notif_email", enabled)
    }

    fun toggleOffersNotifications(enabled: Boolean) {
        _uiState.update { it.copy(offersNotificationsEnabled = enabled) }
        savePref("notif_offers", enabled)
    }

    fun toggleRatingsNotifications(enabled: Boolean) {
        _uiState.update { it.copy(ratingsNotificationsEnabled = enabled) }
        savePref("notif_ratings", enabled)
    }

    private fun savePref(key: String, value: Boolean) = viewModelScope.launch {
        appPreferencesDao.set(AppPreferencesEntity(key, value.toString()))
    }

    fun rateApp(rating: Int, note: String) = viewModelScope.launch {
        if (!networkChecker.isOnline()) {
            _events.emit(SettingsEvent.ShowMessage("Thank you for your feedback!"))
            return@launch
        }
        _uiState.update { it.copy(isLoading = true) }
        blockRepository.rateApp(rating, note)
            .onSuccess { _events.emit(SettingsEvent.ShowMessage("Thank you for your feedback!")) }
            .onFailure { _events.emit(SettingsEvent.ShowMessage("Thank you for your feedback!")) }
        _uiState.update { it.copy(isLoading = false) }
    }

    fun submitBugReport(description: String, steps: String) = viewModelScope.launch {
        if (!networkChecker.isOnline()) {
            _uiState.update { it.copy(error = "No internet connection. Please try again when online.") }
            return@launch
        }
        _uiState.update { it.copy(isLoading = true) }
        blockRepository.submitBugReport(description, steps, emptyList())
            .onSuccess { _events.emit(SettingsEvent.ShowMessage("Bug report submitted. Thank you!")) }
            .onFailure { e -> _uiState.update { s -> s.copy(error = ErrorHandler.friendlyMessage(e)) } }
        _uiState.update { it.copy(isLoading = false) }
    }

    fun submitSupportRequest(
        subject: String,
        description: String,
        category: com.sdd.marketplace.domain.model.SupportCategory
    ) = viewModelScope.launch {
        if (!networkChecker.isOnline()) {
            _uiState.update { it.copy(error = "No internet connection. Please try again when online.") }
            return@launch
        }
        _uiState.update { it.copy(isLoading = true) }
        blockRepository.submitSupportTicket(category, subject, description, emptyList())
            .onSuccess { _events.emit(SettingsEvent.ShowMessage("Support request submitted successfully.")) }
            .onFailure { e -> _uiState.update { s -> s.copy(error = ErrorHandler.friendlyMessage(e)) } }
        _uiState.update { it.copy(isLoading = false) }
    }

    fun clearMessages() = _uiState.update { it.copy(error = null, success = null) }
}
