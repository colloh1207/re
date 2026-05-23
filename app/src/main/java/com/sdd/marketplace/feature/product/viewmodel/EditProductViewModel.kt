package com.sdd.marketplace.feature.product.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdd.marketplace.domain.model.Product
import com.sdd.marketplace.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProductUiState(
    val title: String = "",
    val description: String = "",
    val category: String = "",
    val brand: String = "",
    val condition: String = "",
    val price: String = "",
    val discountPrice: String = "",
    val location: String = "",
    val isNegotiable: Boolean = false,
    val existingImages: List<String> = emptyList(),
    val newImages: List<Uri> = emptyList(),
    val isLoadingProduct: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class EditProductEvent {
    object UpdateSuccess : EditProductEvent()
    data class ShowError(val message: String) : EditProductEvent()
}

@HiltViewModel
class EditProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val productId: String = savedStateHandle["productId"] ?: ""
    private var currentProduct: Product? = null

    private val _uiState = MutableStateFlow(EditProductUiState())
    val uiState: StateFlow<EditProductUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditProductEvent>()
    val events: SharedFlow<EditProductEvent> = _events.asSharedFlow()

    init {
        if (productId.isNotBlank()) loadProduct()
    }

    private fun loadProduct() = viewModelScope.launch {
        productRepository.getProduct(productId)
            .onSuccess { product ->
                currentProduct = product
                _uiState.update {
                    it.copy(
                        title = product.title,
                        description = product.description,
                        category = product.category,
                        brand = product.brand ?: "",
                        condition = product.condition,
                        price = product.price.toString(),
                        discountPrice = product.discountPrice?.toString() ?: "",
                        location = product.location ?: "",
                        isNegotiable = product.isNegotiable,
                        existingImages = product.images,
                        isLoadingProduct = false
                    )
                }
            }
            .onFailure { _uiState.update { it.copy(error = it.error, isLoadingProduct = false) } }
    }

    fun updateTitle(v: String) = _uiState.update { it.copy(title = v) }
    fun updateDescription(v: String) = _uiState.update { it.copy(description = v) }
    fun updateCategory(v: String) = _uiState.update { it.copy(category = v) }
    fun updateCondition(v: String) = _uiState.update { it.copy(condition = v) }
    fun updatePrice(v: String) = _uiState.update { it.copy(price = v.filter { c -> c.isDigit() || c == '.' }) }
    fun updateDiscountPrice(v: String) = _uiState.update { it.copy(discountPrice = v.filter { c -> c.isDigit() || c == '.' }) }
    fun updateLocation(v: String) = _uiState.update { it.copy(location = v) }
    fun toggleNegotiable() = _uiState.update { it.copy(isNegotiable = !it.isNegotiable) }
    fun addNewImage(uri: Uri) = _uiState.update { it.copy(newImages = it.newImages + uri) }
    fun removeExistingImage(url: String) = _uiState.update { it.copy(existingImages = it.existingImages.filter { img -> img != url }) }

    fun updateProduct() = viewModelScope.launch {
        val state = _uiState.value
        if (state.title.isBlank()) { _uiState.update { it.copy(error = "Title is required") }; return@launch }
        if (state.price.isBlank()) { _uiState.update { it.copy(error = "Price is required") }; return@launch }

        _uiState.update { it.copy(isLoading = true, error = null) }
        val base = currentProduct ?: return@launch
        val updatedProduct = base.copy(
            title = state.title.trim(),
            description = state.description.trim(),
            category = state.category,
            brand = state.brand.ifBlank { null },
            condition = state.condition,
            price = state.price.toDoubleOrNull() ?: base.price,
            discountPrice = state.discountPrice.toDoubleOrNull(),
            location = state.location.ifBlank { null },
            isNegotiable = state.isNegotiable,
            images = state.existingImages
        )
        productRepository.updateProduct(updatedProduct)
            .onSuccess { _events.emit(EditProductEvent.UpdateSuccess) }
            .onFailure { e -> _uiState.update { it.copy(error = e.message ?: "Failed to update product") }; _events.emit(EditProductEvent.ShowError(e.message ?: "Error")) }
        _uiState.update { it.copy(isLoading = false) }
    }
}
