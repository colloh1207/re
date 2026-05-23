package com.sdd.marketplace.feature.orders.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdd.marketplace.domain.model.*
import com.sdd.marketplace.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrdersUiState(
    val orders: List<Order> = emptyList(),
    val selectedOrder: Order? = null,
    val tracking: OrderTracking? = null,
    val paymentMethods: List<PaymentMethod> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedTab: Int = 0,
    val checkoutUrl: String? = null,
    val paymentStatus: PaymentStatus? = null
)

sealed class OrderEvent {
    data class ShowError(val message: String) : OrderEvent()
    data class ShowMessage(val message: String) : OrderEvent()
    data class NavigateToPayment(val url: String) : OrderEvent()
    object OrderPlaced : OrderEvent()
}

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val orderRepository: OrderRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val orderId: String? = savedStateHandle["orderId"]
    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()
    private val _events = MutableSharedFlow<OrderEvent>()
    val events: SharedFlow<OrderEvent> = _events.asSharedFlow()

    init {
        loadOrders()
        loadPaymentMethods()
        orderId?.let { loadOrderDetail(it) }
    }

    fun loadOrders(asBuyer: Boolean = true) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        orderRepository.getMyOrders(asBuyer).collect { orders ->
            _uiState.update { it.copy(orders = orders, isLoading = false) }
        }
    }

    fun loadOrderDetail(id: String) = viewModelScope.launch {
        orderRepository.getOrder(id)
            .onSuccess { order ->
                _uiState.update { it.copy(selectedOrder = order) }
                loadTracking(id)
            }
            .onFailure { _uiState.update { s -> s.copy(error = it.message) } }
    }

    private fun loadTracking(id: String) = viewModelScope.launch {
        orderRepository.getOrderTracking(id)
            .onSuccess { tracking -> _uiState.update { it.copy(tracking = tracking) } }
    }

    private fun loadPaymentMethods() = viewModelScope.launch {
        orderRepository.getMyPaymentMethods().collect { methods ->
            _uiState.update { it.copy(paymentMethods = methods) }
        }
    }

    fun cancelOrder(orderId: String, reason: String) = viewModelScope.launch {
        orderRepository.cancelOrder(orderId, reason)
            .onSuccess { _events.emit(OrderEvent.ShowMessage("Order cancelled successfully")) }
            .onFailure { _events.emit(OrderEvent.ShowError(it.message ?: "Error")) }
    }

    fun confirmDelivery(orderId: String) = viewModelScope.launch {
        orderRepository.confirmDelivery(orderId)
            .onSuccess { _events.emit(OrderEvent.ShowMessage("Delivery confirmed. Please leave a review!")) }
            .onFailure { _events.emit(OrderEvent.ShowError(it.message ?: "Error")) }
    }

    fun requestRefund(orderId: String, reason: String) = viewModelScope.launch {
        orderRepository.requestRefund(orderId, reason)
            .onSuccess { _events.emit(OrderEvent.ShowMessage("Refund request submitted")) }
            .onFailure { _events.emit(OrderEvent.ShowError(it.message ?: "Error")) }
    }

    fun initiatePayment(orderId: String, gateway: PaymentGateway) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        orderRepository.initiatePayment(orderId, gateway)
            .onSuccess { url -> _events.emit(OrderEvent.NavigateToPayment(url)) }
            .onFailure { _events.emit(OrderEvent.ShowError(it.message ?: "Payment failed")) }
        _uiState.update { it.copy(isLoading = false) }
    }

    fun pollPaymentStatus(orderId: String) = viewModelScope.launch {
        orderRepository.pollPaymentStatus(orderId)
            .onSuccess { status -> _uiState.update { it.copy(paymentStatus = status) } }
    }

    fun selectTab(tab: Int) = _uiState.update { it.copy(selectedTab = tab) }
    fun clearError() = _uiState.update { it.copy(error = null) }
}
