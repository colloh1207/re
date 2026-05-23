package com.sdd.marketplace.feature.profile.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.sdd.marketplace.domain.model.Product
import com.sdd.marketplace.domain.model.ReportCategory
import com.sdd.marketplace.domain.model.Review
import com.sdd.marketplace.domain.model.User
import com.sdd.marketplace.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Achievement(val emoji: String, val title: String, val date: String)

data class ProfileUiState(
    val user: User? = null,
    val isCurrentUser: Boolean = false,
    val isFollowing: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedTab: Int = 0,
    val reviews: List<Review> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val totalSales: Double = 0.0
)

sealed class ProfileEvent {
    data class NavigateToChat(val chatId: String) : ProfileEvent()
    object NavigateToLogin : ProfileEvent()
    data class ShowError(val message: String) : ProfileEvent()
    data class ShowMessage(val message: String) : ProfileEvent()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val productRepository: ProductRepository,
    private val chatRepository: ChatRepository,
    private val reviewRepository: ReviewRepository,
    private val blockRepository: BlockRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val profileUserId: String? = savedStateHandle["userId"]
    private val currentUserId get() = authRepository.getCurrentUserId()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProfileEvent>()
    val events: SharedFlow<ProfileEvent> = _events.asSharedFlow()

    val userId get() = profileUserId ?: currentUserId ?: ""

    val userProducts: Flow<PagingData<Product>> = productRepository
        .getSellerProducts(userId)
        .cachedIn(viewModelScope)

    val soldProducts: Flow<PagingData<Product>> = productRepository
        .getSoldProducts(userId)
        .cachedIn(viewModelScope)

    val savedItems: Flow<PagingData<Product>> = productRepository
        .getSavedProducts(currentUserId ?: "")
        .cachedIn(viewModelScope)

    init {
        loadProfile()
    }

    private fun loadProfile() = viewModelScope.launch {
        val targetId = userId
        if (targetId.isBlank()) {
            _uiState.update { it.copy(isLoading = false) }
            return@launch
        }
        val isSelf = targetId == currentUserId
        _uiState.update { it.copy(isCurrentUser = isSelf) }

        userRepository.getUserProfile(targetId).collect { user ->
            _uiState.update { it.copy(user = user, isLoading = false) }
        }

        if (!isSelf) {
            userRepository.isFollowing(targetId).collect { isFollowing ->
                _uiState.update { it.copy(isFollowing = isFollowing) }
            }
        }

        launch {
            reviewRepository.getUserReviews(targetId).collect { reviews ->
                _uiState.update { it.copy(reviews = reviews) }
            }
        }

        if (isSelf) {
            _uiState.update {
                it.copy(
                    achievements = listOf(
                        Achievement("🏆", "Top Seller", "May 2024"),
                        Achievement("⚡", "Fast Responder", "Apr 2024"),
                        Achievement("💯", "100+ Sales", "Mar 2024"),
                        Achievement("⭐", "5 Star Seller", "Feb 2024"),
                        Achievement("🌟", "Verified Seller", "Jan 2024"),
                        Achievement("🎁", "First Sale", "Dec 2023")
                    ),
                    totalSales = 125430.0
                )
            }
        }
    }

    fun followUnfollow() = viewModelScope.launch {
        val targetId = userId
        if (_uiState.value.isFollowing) {
            userRepository.unfollowUser(targetId)
        } else {
            userRepository.followUser(targetId)
        }
        _uiState.update { it.copy(isFollowing = !it.isFollowing) }
    }

    fun messageUser() = viewModelScope.launch {
        val targetId = userId
        chatRepository.getOrCreateChat(targetId)
            .onSuccess { chat -> _events.emit(ProfileEvent.NavigateToChat(chat.id)) }
            .onFailure { _events.emit(ProfileEvent.ShowError(it.message ?: "Error")) }
    }

    fun selectTab(index: Int) = _uiState.update { it.copy(selectedTab = index) }

    fun uploadAvatar(uri: Uri) = viewModelScope.launch {
        userRepository.uploadAvatar(uri)
            .onSuccess { _events.emit(ProfileEvent.ShowMessage("Profile photo updated!")) }
            .onFailure { _uiState.update { s -> s.copy(error = it.message) } }
    }

    fun requestVerification() = viewModelScope.launch {}

    fun signOut() = viewModelScope.launch {
        authRepository.signOut()
        _events.emit(ProfileEvent.NavigateToLogin)
    }

    fun deleteProduct(productId: String) = viewModelScope.launch {
        productRepository.deleteProduct(productId)
            .onSuccess { _events.emit(ProfileEvent.ShowMessage("Product deleted")) }
            .onFailure { _events.emit(ProfileEvent.ShowError(it.message ?: "Error deleting product")) }
    }

    fun markAsSold(productId: String) = viewModelScope.launch {
        productRepository.markAsSold(productId)
            .onSuccess { _events.emit(ProfileEvent.ShowMessage("Marked as sold")) }
            .onFailure { _events.emit(ProfileEvent.ShowError(it.message ?: "Error")) }
    }

    fun archiveProduct(productId: String) = viewModelScope.launch {
        productRepository.archiveProduct(productId)
            .onSuccess { _events.emit(ProfileEvent.ShowMessage("Product archived")) }
            .onFailure { _events.emit(ProfileEvent.ShowError(it.message ?: "Error")) }
    }

    fun blockUser() = viewModelScope.launch {
        blockRepository.blockUser(userId, null)
            .onSuccess { _events.emit(ProfileEvent.ShowMessage("User blocked")) }
            .onFailure { _events.emit(ProfileEvent.ShowError(it.message ?: "Error")) }
    }

    fun reportUser(category: String, description: String) = viewModelScope.launch {
        blockRepository.reportUser(userId, ReportCategory.SPAM, description, emptyList())
            .onSuccess { _events.emit(ProfileEvent.ShowMessage("Report submitted. Thank you.")) }
            .onFailure { _events.emit(ProfileEvent.ShowError(it.message ?: "Error")) }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    fun updateProfile(updates: Map<String, Any>) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        userRepository.updateProfile(updates)
            .onSuccess { user ->
                _uiState.update { it.copy(user = user, isLoading = false) }
                _events.emit(ProfileEvent.ShowMessage("Profile updated successfully"))
            }
            .onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
                _events.emit(ProfileEvent.ShowError(e.message ?: "Update failed"))
            }
    }
}
