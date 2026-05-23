package com.sdd.marketplace.feature.review.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdd.marketplace.core.util.ErrorHandler
import com.sdd.marketplace.core.util.NetworkChecker
import com.sdd.marketplace.domain.repository.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RateSellerUiState(
    val sellerId: String = "",
    val sellerName: String = "",
    val productId: String = "",
    val selectedRating: Int = 0,
    val comment: String = "",
    val isLoading: Boolean = false,
    val isSubmitted: Boolean = false,
    val error: String? = null
)

sealed class RateSellerEvent {
    data class ShowMessage(val message: String) : RateSellerEvent()
    object NavigateBack : RateSellerEvent()
}

@HiltViewModel
class RateSellerViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val networkChecker: NetworkChecker,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(RateSellerUiState())
    val uiState: StateFlow<RateSellerUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<RateSellerEvent>()
    val events: SharedFlow<RateSellerEvent> = _events.asSharedFlow()

    init {
        val sellerId = savedStateHandle.get<String>("sellerId") ?: ""
        val sellerName = savedStateHandle.get<String>("sellerName") ?: "Seller"
        val productId = savedStateHandle.get<String>("productId") ?: ""
        _uiState.update { it.copy(sellerId = sellerId, sellerName = sellerName, productId = productId) }
    }

    fun setRating(rating: Int) = _uiState.update { it.copy(selectedRating = rating) }

    fun setComment(comment: String) = _uiState.update { it.copy(comment = comment) }

    fun submitReview() = viewModelScope.launch {
        val state = _uiState.value
        if (state.selectedRating == 0) {
            _uiState.update { it.copy(error = "Please select a star rating before submitting.") }
            return@launch
        }
        if (!networkChecker.isOnline()) {
            _uiState.update { it.copy(error = "No internet connection. Please check your network.") }
            return@launch
        }
        _uiState.update { it.copy(isLoading = true, error = null) }
        reviewRepository.writeReview(
            productId = state.productId,
            sellerId = state.sellerId,
            rating = state.selectedRating,
            comment = state.comment.trim()
        ).onSuccess {
            _uiState.update { s -> s.copy(isLoading = false, isSubmitted = true) }
            _events.emit(RateSellerEvent.ShowMessage("Review submitted! Thank you for your feedback."))
            kotlinx.coroutines.delay(1500)
            _events.emit(RateSellerEvent.NavigateBack)
        }.onFailure { e ->
            _uiState.update { s ->
                s.copy(isLoading = false, error = ErrorHandler.friendlyMessage(e))
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
