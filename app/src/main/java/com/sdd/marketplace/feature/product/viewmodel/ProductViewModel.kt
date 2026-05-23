package com.sdd.marketplace.feature.product.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdd.marketplace.domain.model.*
import com.sdd.marketplace.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductDetailUiState(
    val product: Product? = null,
    val seller: User? = null,
    val reviews: List<Review> = emptyList(),
    val reviewSummary: ReviewSummary? = null,
    val relatedProducts: List<Product> = emptyList(),
    val isFavorite: Boolean = false,
    val isFollowingSeller: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedImageIndex: Int = 0,
    val selectedColor: String? = null,
    val canWriteReview: Boolean = false
)

sealed class ProductDetailEvent {
    data class NavigateToChat(val chatId: String) : ProductDetailEvent()
    data class ShowError(val message: String) : ProductDetailEvent()
    data class ShowMessage(val message: String) : ProductDetailEvent()
    data class NavigateToOrder(val orderId: String) : ProductDetailEvent()
    object ProductAddedToCart : ProductDetailEvent()
}

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val favoriteRepository: FavoriteRepository,
    private val reviewRepository: ReviewRepository,
    private val userRepository: UserRepository,
    private val chatRepository: ChatRepository,
    private val blockRepository: BlockRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val productId: String = savedStateHandle["productId"] ?: ""
    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<ProductDetailEvent>()
    val events: SharedFlow<ProductDetailEvent> = _events.asSharedFlow()

    init { if (productId.isNotBlank()) loadProduct() }

    private fun loadProduct() = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        productRepository.getProduct(productId)
            .onSuccess { product ->
                _uiState.update { it.copy(product = product, isLoading = false) }
                loadSellerInfo(product.sellerId)
                loadReviews()
                runCatching { productRepository.incrementViewCount(productId) }
            }
            .onFailure { _uiState.update { s -> s.copy(error = it.message, isLoading = false) } }
        launch {
            favoriteRepository.isFavorite(productId).collect { isFav ->
                _uiState.update { it.copy(isFavorite = isFav) }
            }
        }
        launch {
            reviewRepository.canWriteReview(productId)
                .onSuccess { can -> _uiState.update { it.copy(canWriteReview = can) } }
        }
    }

    private fun loadSellerInfo(sellerId: String) = viewModelScope.launch {
        userRepository.getUserProfile(sellerId).collect { user ->
            _uiState.update { it.copy(seller = user) }
        }
        userRepository.isFollowing(sellerId).collect { isFollowing ->
            _uiState.update { it.copy(isFollowingSeller = isFollowing) }
        }
    }

    private fun loadReviews() = viewModelScope.launch {
        reviewRepository.getProductReviews(productId).collect { reviews ->
            _uiState.update { it.copy(reviews = reviews) }
        }
    }

    fun toggleFavorite() = viewModelScope.launch { favoriteRepository.toggleFavorite(productId) }
    fun selectImage(index: Int) = _uiState.update { it.copy(selectedImageIndex = index) }
    fun selectColor(color: String) = _uiState.update { it.copy(selectedColor = color) }

    fun messageSeller() = viewModelScope.launch {
        val sellerId = _uiState.value.product?.sellerId ?: return@launch
        chatRepository.getOrCreateChat(sellerId, productId)
            .onSuccess { chat -> _events.emit(ProductDetailEvent.NavigateToChat(chat.id)) }
            .onFailure { _events.emit(ProductDetailEvent.ShowError(it.message ?: "Error")) }
    }

    fun followSeller() = viewModelScope.launch {
        val sellerId = _uiState.value.product?.sellerId ?: return@launch
        if (_uiState.value.isFollowingSeller) userRepository.unfollowUser(sellerId)
        else userRepository.followUser(sellerId)
        _uiState.update { it.copy(isFollowingSeller = !it.isFollowingSeller) }
    }

    fun writeReview(productId: String, sellerId: String, rating: Int, comment: String) = viewModelScope.launch {
        reviewRepository.writeReview(productId, sellerId, rating, comment)
            .onSuccess { _events.emit(ProductDetailEvent.ShowMessage("Review submitted!")); loadReviews() }
            .onFailure { _events.emit(ProductDetailEvent.ShowError(it.message ?: "Error submitting review")) }
    }

    fun markHelpful(reviewId: String, currentlyHelpful: Boolean) = viewModelScope.launch {
        if (currentlyHelpful) reviewRepository.unmarkHelpful(reviewId)
        else reviewRepository.markHelpful(reviewId)
        loadReviews()
    }

    fun replyToReview(reviewId: String, content: String) = viewModelScope.launch {
        reviewRepository.replyToReview(reviewId, content)
            .onSuccess { _events.emit(ProductDetailEvent.ShowMessage("Reply posted!")); loadReviews() }
            .onFailure { _events.emit(ProductDetailEvent.ShowError(it.message ?: "Error posting reply")) }
    }

    fun buyNow() = viewModelScope.launch {
        // Navigate to order placement flow
        _events.emit(ProductDetailEvent.ShowMessage("Proceeding to checkout..."))
    }

    fun reportListing(category: ReportCategory, description: String) = viewModelScope.launch {
        blockRepository.reportProduct(productId, category, description)
            .onSuccess { _events.emit(ProductDetailEvent.ShowMessage("Report submitted. Thank you!")) }
            .onFailure { _events.emit(ProductDetailEvent.ShowError(it.message ?: "Error")) }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
