package com.sdd.marketplace.feature.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdd.marketplace.core.util.ErrorHandler
import com.sdd.marketplace.core.util.NetworkChecker
import com.sdd.marketplace.domain.model.User
import com.sdd.marketplace.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val otpSent: Boolean = false,
    val email: String = "",
    val isEmailOtp: Boolean = true,
    val otpIsForRecovery: Boolean = false,
    val needsEmailVerification: Boolean = false,
    val successMessage: String? = null
)

sealed class AuthEvent {
    data class ShowError(val message: String) : AuthEvent()
    object NavigateToHome : AuthEvent()
    object NavigateToOtp : AuthEvent()
    data class ShowSuccess(val message: String) : AuthEvent()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val networkChecker: NetworkChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>()
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            authRepository.isAuthenticated.collect { isAuth ->
                _uiState.update { it.copy(isAuthenticated = isAuth) }
            }
        }
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
    }

    fun signInWithEmail(email: String, password: String) = viewModelScope.launch {
        if (email.isBlank()) { _uiState.update { it.copy(error = "Please enter your email") }; return@launch }
        if (password.isBlank()) { _uiState.update { it.copy(error = "Please enter your password") }; return@launch }
        if (!networkChecker.isOnline()) { _uiState.update { it.copy(error = "No internet connection. Please check your network.") }; return@launch }
        _uiState.update { it.copy(isLoading = true, error = null) }
        authRepository.signInWithEmail(email, password)
            .onSuccess { _events.emit(AuthEvent.NavigateToHome) }
            .onFailure { e -> _uiState.update { it.copy(error = ErrorHandler.friendlyMessage(e)) } }
        _uiState.update { it.copy(isLoading = false) }
    }

    fun verifyOtp(otp: String) = viewModelScope.launch {
        if (otp.isBlank()) { _uiState.update { it.copy(error = "Please enter the OTP") }; return@launch }
        if (!networkChecker.isOnline()) { _uiState.update { it.copy(error = "No internet connection. Please check your network.") }; return@launch }
        _uiState.update { it.copy(isLoading = true, error = null) }
        val state = _uiState.value
        authRepository.verifyOtp(state.email, otp, state.isEmailOtp, state.otpIsForRecovery)
            .onSuccess { _events.emit(AuthEvent.NavigateToHome) }
            .onFailure { e -> _uiState.update { it.copy(error = ErrorHandler.friendlyMessage(e)) } }
        _uiState.update { it.copy(isLoading = false) }
    }

    fun signUp(fullName: String, email: String, password: String, referralCode: String? = null) = viewModelScope.launch {
        if (fullName.isBlank()) { _uiState.update { it.copy(error = "Please enter your full name") }; return@launch }
        if (email.isBlank()) { _uiState.update { it.copy(error = "Please enter your email") }; return@launch }
        val emailDomain = email.substringAfterLast("@").lowercase().trim()
        if (emailDomain in DISPOSABLE_EMAIL_DOMAINS) {
            _uiState.update { it.copy(error = "Disposable email addresses are not allowed. Please use a real email.") }
            return@launch
        }
        if (!email.contains("@") || !email.contains(".")) {
            _uiState.update { it.copy(error = "Please enter a valid email address.") }
            return@launch
        }
        if (password.length < 6) { _uiState.update { it.copy(error = "Password must be at least 6 characters") }; return@launch }
        if (!networkChecker.isOnline()) { _uiState.update { it.copy(error = "No internet connection. Please check your network.") }; return@launch }
        _uiState.update { it.copy(isLoading = true, error = null, email = email, isEmailOtp = true, otpIsForRecovery = false) }
        authRepository.signUpWithEmail(fullName, email, password, referralCode)
            .onSuccess { _events.emit(AuthEvent.NavigateToOtp) }
            .onFailure { e -> _uiState.update { it.copy(error = ErrorHandler.friendlyMessage(e)) } }
        _uiState.update { it.copy(isLoading = false) }
    }

    fun continueAsGuest() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true, error = null) }
        authRepository.signInAnonymously()
            .onSuccess { _events.emit(AuthEvent.NavigateToHome) }
            .onFailure { _events.emit(AuthEvent.NavigateToHome) }
        _uiState.update { it.copy(isLoading = false) }
    }

    fun sendPasswordReset(email: String) = viewModelScope.launch {
        if (email.isBlank()) { _uiState.update { it.copy(error = "Please enter your email") }; return@launch }
        if (!networkChecker.isOnline()) { _uiState.update { it.copy(error = "No internet connection. Please check your network.") }; return@launch }
        _uiState.update { it.copy(isLoading = true, error = null, email = email, isEmailOtp = true, otpIsForRecovery = true) }
        authRepository.sendPasswordResetEmail(email)
            .onSuccess { _events.emit(AuthEvent.NavigateToOtp) }
            .onFailure { e -> _uiState.update { s -> s.copy(error = ErrorHandler.friendlyMessage(e)) } }
        _uiState.update { it.copy(isLoading = false) }
    }

    fun resendEmailOtp() = viewModelScope.launch {
        val state = _uiState.value
        if (!networkChecker.isOnline()) { _uiState.update { it.copy(error = "No internet connection.") }; return@launch }
        _uiState.update { it.copy(isLoading = true, error = null) }
        if (state.otpIsForRecovery) {
            authRepository.sendPasswordResetEmail(state.email)
                .onFailure { e -> _uiState.update { s -> s.copy(error = ErrorHandler.friendlyMessage(e)) } }
        } else {
            authRepository.resendEmailOtp(state.email)
                .onFailure { e -> _uiState.update { s -> s.copy(error = ErrorHandler.friendlyMessage(e)) } }
        }
        _uiState.update { it.copy(isLoading = false) }
    }

    fun signOut() = viewModelScope.launch { authRepository.signOut() }
    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearSuccess() = _uiState.update { it.copy(successMessage = null, needsEmailVerification = false) }

    companion object {
        private val DISPOSABLE_EMAIL_DOMAINS = setOf(
            "mailinator.com", "guerrillamail.com", "guerrillamail.net", "guerrillamail.org",
            "guerrillamail.biz", "guerrillamail.de", "guerrillamailblock.com", "sharklasers.com",
            "spam4.me", "yopmail.com", "yopmail.fr", "trashmail.com", "trashmail.at", "trashmail.io",
            "trashmail.me", "trashmail.net", "dispostable.com", "mailnull.com", "spamgourmet.com",
            "tempr.email", "discard.email", "maildrop.cc", "mailnesia.com", "spamdecoy.net",
            "spambox.us", "tempinbox.com", "fakeinbox.com", "10minutemail.com", "10minutemail.net",
            "10minutemail.org", "tempmail.com", "tempmail.net", "temp-mail.org", "temp-mail.io",
            "getnada.com", "nada.email", "mohmal.com", "jetable.com", "jetable.net"
        )
    }
}
