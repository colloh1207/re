package com.sdd.marketplace.feature.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdd.marketplace.domain.model.Review
import com.sdd.marketplace.domain.repository.AuthRepository
import com.sdd.marketplace.domain.repository.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyReviewsUiState(
    val receivedReviews: List<Review> = emptyList(),
    val givenReviews: List<Review> = emptyList(),
    val averageRating: Double = 0.0,
    val isLoading: Boolean = true
)

@HiltViewModel
class MyReviewsViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyReviewsUiState())
    val uiState: StateFlow<MyReviewsUiState> = _uiState.asStateFlow()

    init {
        loadReviews()
    }

    private fun loadReviews() = viewModelScope.launch {
        val userId = authRepository.getCurrentUserId() ?: return@launch
        launch {
            reviewRepository.getUserReviews(userId).collect { reviews ->
                val avg = if (reviews.isEmpty()) 0.0 else reviews.map { it.rating }.average()
                _uiState.update { it.copy(receivedReviews = reviews, averageRating = avg, isLoading = false) }
            }
        }
        launch {
            reviewRepository.getMyReviews().collect { reviews ->
                _uiState.update { it.copy(givenReviews = reviews) }
            }
        }
    }
}
