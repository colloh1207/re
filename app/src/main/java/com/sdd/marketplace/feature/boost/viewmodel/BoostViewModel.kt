package com.sdd.marketplace.feature.boost.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdd.marketplace.core.util.CurrencyUtils
import com.sdd.marketplace.domain.model.Boost
import com.sdd.marketplace.domain.model.BoostPaymentStatus
import com.sdd.marketplace.domain.model.BoostStatus
import com.sdd.marketplace.domain.model.BoostTier
import com.sdd.marketplace.domain.model.Product
import com.sdd.marketplace.domain.repository.BoostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BoostStep { PRODUCT_SELECTION, TIER_SELECTION, CHECKOUT, WEBVIEW_PAYMENT, POLLING, SUCCESS }

data class BoostUiState(
    val step: BoostStep = BoostStep.PRODUCT_SELECTION,
    val myProducts: List<Product> = emptyList(),
    val selectedProductIds: Set<String> = emptySet(),
    val selectedTier: BoostTier? = null,
    val currency: CurrencyUtils.CurrencyInfo = CurrencyUtils.detectCurrency(),
    val currentBoost: Boost? = null,
    val paystackAuthUrl: String? = null,
    val isLoading: Boolean = false,
    val isPolling: Boolean = false,
    val pollAttempts: Int = 0,
    val error: String? = null,
    val successBoost: Boost? = null,
    val preSelectedProductId: String? = null,
)

sealed class BoostEvent {
    object NavigateBack  : BoostEvent()
    object PaymentFailed : BoostEvent()
    object PollingTimeout : BoostEvent()
}

@HiltViewModel
class BoostViewModel @Inject constructor(
    private val boostRepository: BoostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BoostUiState())
    val uiState: StateFlow<BoostUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<BoostEvent>()
    val events: SharedFlow<BoostEvent> = _events.asSharedFlow()

    init { loadMyProducts() }

    fun preSelectProduct(productId: String) {
        _uiState.update {
            it.copy(
                selectedProductIds    = setOf(productId),
                preSelectedProductId  = productId,
                step                  = BoostStep.TIER_SELECTION
            )
        }
    }

    private fun loadMyProducts() = viewModelScope.launch {
        boostRepository.getMyListedProducts().collect { products ->
            _uiState.update { it.copy(myProducts = products) }
        }
    }

    fun toggleProductSelection(productId: String) {
        val current = _uiState.value.selectedProductIds.toMutableSet()
        if (!current.remove(productId)) current.add(productId)
        _uiState.update { it.copy(selectedProductIds = current) }
    }

    fun selectTier(tier: BoostTier) = _uiState.update { it.copy(selectedTier = tier) }

    fun updateCurrency(code: String) =
        _uiState.update { it.copy(currency = CurrencyUtils.getInfo(code)) }

    fun goToTierSelection() {
        if (_uiState.value.selectedProductIds.isEmpty()) {
            _uiState.update { it.copy(error = "Select at least one product to boost") }
            return
        }
        _uiState.update { it.copy(step = BoostStep.TIER_SELECTION, error = null) }
    }

    fun goToCheckout() {
        if (_uiState.value.selectedTier == null) {
            _uiState.update { it.copy(error = "Select a boost tier to continue") }
            return
        }
        _uiState.update { it.copy(step = BoostStep.CHECKOUT, error = null) }
    }

    fun goBack() = viewModelScope.launch {
        val state = _uiState.value
        val prev: BoostStep? = when (state.step) {
            BoostStep.TIER_SELECTION -> if (state.preSelectedProductId != null) null else BoostStep.PRODUCT_SELECTION
            BoostStep.CHECKOUT       -> BoostStep.TIER_SELECTION
            else                     -> null
        }
        if (prev != null) _uiState.update { it.copy(step = prev) }
        else _events.emit(BoostEvent.NavigateBack)
    }

    fun createBoostAndPay() = viewModelScope.launch {
        val state = _uiState.value
        val tier  = state.selectedTier ?: return@launch
        _uiState.update { it.copy(isLoading = true, error = null) }

        boostRepository.createBoost(
            productIds = state.selectedProductIds.toList(),
            tierId     = tier.id,
            currency   = state.currency.code
        ).onSuccess { boost ->
            _uiState.update {
                it.copy(
                    isLoading       = false,
                    currentBoost    = boost,
                    paystackAuthUrl = boost.paystackAuthUrl ?: "",
                    step            = BoostStep.WEBVIEW_PAYMENT
                )
            }
        }.onFailure { err ->
            _uiState.update { it.copy(isLoading = false, error = err.message ?: "Failed to start payment") }
        }
    }

    fun onPaymentRedirect(url: String) {
        // Intercept Paystack callback — start polling
        _uiState.update { it.copy(step = BoostStep.POLLING, pollAttempts = 0) }
        startPolling()
    }

    fun startPolling() = viewModelScope.launch {
        val boostId = _uiState.value.currentBoost?.id ?: return@launch
        _uiState.update { it.copy(isPolling = true) }
        repeat(12) { attempt ->
            delay(3_000)
            boostRepository.pollBoostPayment(boostId).onSuccess { boost ->
                _uiState.update { it.copy(currentBoost = boost, pollAttempts = attempt + 1) }
                when {
                    boost.paymentStatus == BoostPaymentStatus.PAID ||
                    boost.status == BoostStatus.ACTIVE -> {
                        _uiState.update { it.copy(isPolling = false, step = BoostStep.SUCCESS, successBoost = boost) }
                        return@launch
                    }
                    boost.paymentStatus == BoostPaymentStatus.FAILED ||
                    boost.status == BoostStatus.CANCELLED -> {
                        _uiState.update { it.copy(isPolling = false, error = "Payment unsuccessful. Please try again.") }
                        _events.emit(BoostEvent.PaymentFailed)
                        return@launch
                    }
                    else -> { /* still pending */ }
                }
            }
        }
        _uiState.update { it.copy(isPolling = false) }
        _events.emit(BoostEvent.PollingTimeout)
    }

    fun retryPayment() = _uiState.update { it.copy(step = BoostStep.CHECKOUT, error = null) }
    fun dismissError() = _uiState.update { it.copy(error = null) }
}
