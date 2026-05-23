package com.sdd.marketplace.domain.repository

import com.sdd.marketplace.domain.model.*
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun getMyOrders(asBuyer: Boolean = true): Flow<List<Order>>
    suspend fun getOrder(orderId: String): Result<Order>
    suspend fun createOrder(
        productId: String,
        quantity: Int,
        shippingAddress: ShippingAddress,
        paymentMethodId: String,
        buyerTermsAccepted: Boolean
    ): Result<Order>
    suspend fun cancelOrder(orderId: String, reason: String): Result<Unit>
    suspend fun requestRefund(orderId: String, reason: String): Result<Unit>
    suspend fun confirmDelivery(orderId: String): Result<Unit>
    suspend fun getOrderTracking(orderId: String): Result<OrderTracking>
    fun getMyPaymentMethods(): Flow<List<PaymentMethod>>
    suspend fun addPaymentMethod(gateway: PaymentGateway, token: String): Result<PaymentMethod>
    suspend fun removePaymentMethod(methodId: String): Result<Unit>
    suspend fun setDefaultPaymentMethod(methodId: String): Result<Unit>
    suspend fun initiatePayment(orderId: String, gateway: PaymentGateway): Result<String>
    suspend fun verifyPayment(
        orderId: String,
        gatewayOrderId: String,
        gatewayPaymentId: String,
        signature: String?
    ): Result<Order>
    suspend fun pollPaymentStatus(orderId: String): Result<PaymentStatus>
}
