package com.sdd.marketplace.feature.profile.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdd.marketplace.domain.repository.AuthRepository
import com.sdd.marketplace.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val fullName: String = "",
    val username: String = "",
    val bio: String = "",
    val phone: String = "",
    val location: String = "",
    val website: String = "",
    val shopName: String = "",
    val shopDescription: String = "",
    val currentAvatarUrl: String? = null,
    val avatarUri: Uri? = null,
    val showEmail: Boolean = false,
    val showPhone: Boolean = false,
    val showOnlineStatus: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class EditProfileEvent {
    object SaveSuccess : EditProfileEvent()
    data class ShowError(val message: String) : EditProfileEvent()
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditProfileEvent>()
    val events: SharedFlow<EditProfileEvent> = _events.asSharedFlow()

    init {
        loadCurrentProfile()
    }

    private fun loadCurrentProfile() = viewModelScope.launch {
        userRepository.getMyProfile().collect { user ->
            user?.let {
                _uiState.update { s ->
                    s.copy(
                        fullName = it.fullName,
                        username = it.email?.substringBefore("@") ?: "",
                        bio = it.bio ?: "",
                        phone = it.phone ?: "",
                        location = it.location ?: "",
                        currentAvatarUrl = it.avatarUrl
                    )
                }
            }
        }
    }

    fun updateFullName(value: String) = _uiState.update { it.copy(fullName = value) }
    fun updateUsername(value: String) = _uiState.update { it.copy(username = value.lowercase().replace(" ", "_")) }
    fun updateBio(value: String) = _uiState.update { it.copy(bio = value.take(150)) }
    fun updatePhone(value: String) = _uiState.update { it.copy(phone = value) }
    fun updateLocation(value: String) = _uiState.update { it.copy(location = value) }
    fun updateWebsite(value: String) = _uiState.update { it.copy(website = value) }
    fun updateShopName(value: String) = _uiState.update { it.copy(shopName = value) }
    fun updateShopDescription(value: String) = _uiState.update { it.copy(shopDescription = value) }
    fun setAvatarUri(uri: Uri) = _uiState.update { it.copy(avatarUri = uri) }
    fun toggleShowEmail() = _uiState.update { it.copy(showEmail = !it.showEmail) }
    fun toggleShowPhone() = _uiState.update { it.copy(showPhone = !it.showPhone) }
    fun toggleShowOnlineStatus() = _uiState.update { it.copy(showOnlineStatus = !it.showOnlineStatus) }

    fun saveProfile() = viewModelScope.launch {
        if (_uiState.value.fullName.isBlank()) {
            _uiState.update { it.copy(error = "Name is required") }
            return@launch
        }
        _uiState.update { it.copy(isLoading = true, error = null) }

        val state = _uiState.value
        val avatarUri = state.avatarUri
        if (avatarUri != null) {
            userRepository.uploadAvatar(avatarUri)
        }

        val updates = mutableMapOf<String, Any>(
            "full_name" to state.fullName.trim(),
            "bio" to state.bio.trim(),
            "location" to state.location.trim()
        )
        if (state.phone.isNotBlank()) updates["phone"] = state.phone.trim()

        userRepository.updateProfile(updates)
            .onSuccess { _events.emit(EditProfileEvent.SaveSuccess) }
            .onFailure { e ->
                _uiState.update { it.copy(error = e.message ?: "Failed to save profile") }
                _events.emit(EditProfileEvent.ShowError(e.message ?: "Failed to save profile"))
            }
        _uiState.update { it.copy(isLoading = false) }
    }
}
