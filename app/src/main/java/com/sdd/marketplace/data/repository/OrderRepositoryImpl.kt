package com.sdd.marketplace.data.repository

import com.sdd.marketplace.data.mappers.toDomain
import com.sdd.marketplace.data.remote.dto.*
import com.sdd.marketplace.domain.model.*
import com.sdd.marketplace.domain.repository.OrderRepository
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.functions.functions
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val auth: Auth,
    private val functions: Functions
) : OrderRepository {

    override fun getMyOrders(asBuyer: Boolean): Flow<List<Order>> = flow {
        val userId = auth.currentUserOrNull()?.id ?: return@flow
        val column = if (asBuyer) "buyer_id" else "seller_id"
        val dtos = postgrest["orders"].select {
            filter { eq(column, userId) }
            order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
        }.decodeList<OrderDto>()
        emit(dtos.map { it.toDomain() })
    }.catch { Timber.e(it) }

    override suspend fun getOrder(orderId: String): Result<Order> = runCatching {
        postgrest["orders"].select {
            filter { eq("id", orderId) }
        }.decodeSingle<OrderDto>().toDomain()
    }

    override suspend fun createOrder(
        productId: String, quantity: Int,
        shippingAddress: ShippingAddress,
        paymentMethodId: String,
        buyerTermsAccepted: Boolean
    ): Result<Order> = runCatching {
        val userId = auth.currentUserOrNull()?.id ?: throw Exception("Not authenticated")
        val body = buildJsonObject {
            put("buyer_id", userId); put("product_id", productId)
            put("quantity", quantity); put("payment_method_id", paymentMethodId)
            put("buyer_terms_accepted", buyerTermsAccepted)
            put("shipping_address", buildJsonObject {
                put("full_name", shippingAddress.fullName)
                put("address_line1", shippingAddress.addressLine1)
                shippingAddress.addressLine2?.let { put("address_line2", it) }
                put("city", shippingAddress.city); put("state", shippingAddress.state)
                put("postal_code", shippingAddress.postalCode)
                put("country", shippingAddress.country); put("phone", shippingAddress.phone)
            })
        }
        val result = functions.invoke("create-order", body)
        Json.decodeFromString<OrderDto>(result.bodyAsText()).toDomain()
    }

    override suspend fun cancelOrder(orderId: String, reason: String): Result<Unit> = runCatching {
        postgrest["orders"].update({ set("status", "cancelled"); set("cancel_reason", reason) }) {
            filter { eq("id", orderId) }
        }
    }

    override suspend fun requestRefund(orderId: String, reason: String): Result<Unit> = runCatching {
        postgrest["orders"].update({ set("status", "refund_requested"); set("refund_reason", reason) }) {
            filter { eq("id", orderId) }
        }
    }

    override suspend fun confirmDelivery(orderId: String): Result<Unit> = runCatching {
        postgrest["orders"].update({ set("status", "delivered") }) { filter { eq("id", orderId) } }
    }

    override suspend fun getOrderTracking(orderId: String): Result<OrderTracking> = runCatching {
        val events = postgrest["order_tracking"].select {
            filter { eq("order_id", orderId) }
            order("timestamp", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
        }.decodeList<OrderTrackingEventDto>()
        OrderTracking(orderId, events.map {
            TrackingEvent(
                status = OrderStatus.values().firstOrNull { s -> s.name.lowercase() == it.status } ?: OrderStatus.PENDING,
                description = it.description, timestamp = it.timestamp, location = it.location
            )
        })
    }

    override fun getMyPaymentMethods(): Flow<List<PaymentMethod>> = flow {
        val userId = auth.currentUserOrNull()?.id ?: return@flow
        val dtos = postgrest["payment_methods"].select { filter { eq("user_id", userId) } }
            .decodeList<PaymentMethodDto>()
        emit(dtos.map { it.toDomain() })
    }.catch { Timber.e(it) }

    override suspend fun addPaymentMethod(gateway: PaymentGateway, token: String): Result<PaymentMethod> = runCatching {
        val userId = auth.currentUserOrNull()?.id ?: throw Exception("Not authenticated")
        val body = buildJsonObject {
            put("user_id", userId); put("gateway", gateway.name.lowercase()); put("token", token)
        }
        val result = functions.invoke("add-payment-method", body = body)
        Json.decodeFromString<PaymentMethodDto>(result.bodyAsText()).toDomain()
    }

    override suspend fun removePaymentMethod(methodId: String): Result<Unit> = runCatching {
        postgrest["payment_methods"].delete { filter { eq("id", methodId) } }
    }

    override suspend fun setDefaultPaymentMethod(methodId: String): Result<Unit> = runCatching {
        val userId = auth.currentUserOrNull()?.id ?: throw Exception("Not authenticated")
        postgrest["payment_methods"].update({ set("is_default", false) }) { filter { eq("user_id", userId) } }
        postgrest["payment_methods"].update({ set("is_default", true) }) { filter { eq("id", methodId) } }
    }

    override suspend fun initiatePayment(orderId: String, gateway: PaymentGateway): Result<String> = runCatching {
        val body = buildJsonObject { put("order_id", orderId); put("gateway", gateway.name.lowercase()) }
        val result = functions.invoke("initiate-payment", body = body)
        Json.parseToJsonElement(result.bodyAsText()).jsonObject["checkout_url"]?.jsonPrimitive?.content ?: throw Exception("No checkout URL")
    }

    override suspend fun verifyPayment(orderId: String, gatewayOrderId: String, gatewayPaymentId: String, signature: String?): Result<Order> = runCatching {
        val body = buildJsonObject {
            put("order_id", orderId); put("gateway_order_id", gatewayOrderId)
            put("gateway_payment_id", gatewayPaymentId); signature?.let { put("signature", it) }
        }
        val result = functions.invoke("verify-payment", body = body)
        Json.decodeFromString<OrderDto>(result.bodyAsText()).toDomain()
    }

    override suspend fun pollPaymentStatus(orderId: String): Result<PaymentStatus> = runCatching {
        val result = postgrest["orders"].select { filter { eq("id", orderId) } }.decodeSingle<OrderDto>()
        PaymentStatus.values().firstOrNull { it.name.lowercase() == result.paymentStatus } ?: PaymentStatus.PENDING
    }

    private fun OrderDto.toDomain() = Order(
        id = id, buyerId = buyerId, buyer = buyer?.toDomain(), sellerId = sellerId,
        seller = seller?.toDomain(), productId = productId, product = product?.toDomain(),
        quantity = quantity, unitPrice = unitPrice, totalAmount = totalAmount, currency = currency,
        status = OrderStatus.values().firstOrNull { it.name.lowercase() == status.replace("_", "") }
            ?: OrderStatus.PENDING,
        paymentStatus = PaymentStatus.values().firstOrNull { it.name.lowercase() == paymentStatus.replace("_", "") }
            ?: PaymentStatus.PENDING,
        paymentMethod = paymentMethod,
        paymentGateway = paymentGateway?.let { PaymentGateway.values().firstOrNull { g -> g.name.lowercase() == it } },
        paymentTransactionId = paymentTransactionId,
        shippingAddress = shippingAddress?.toDomain(),
        trackingNumber = trackingNumber, trackingUrl = trackingUrl, notes = notes,
        buyerTermsAccepted = buyerTermsAccepted, buyerTermsAcceptedAt = buyerTermsAcceptedAt,
        createdAt = createdAt, updatedAt = updatedAt
    )

    private fun ShippingAddressDto.toDomain() = ShippingAddress(
        fullName = fullName, addressLine1 = addressLine1, addressLine2 = addressLine2,
        city = city, state = state, postalCode = postalCode, country = country, phone = phone
    )

    private fun PaymentMethodDto.toDomain() = PaymentMethod(
        id = id, userId = userId,
        gateway = PaymentGateway.values().firstOrNull { it.name.lowercase() == gateway } ?: PaymentGateway.COD,
        displayName = displayName, maskedIdentifier = maskedIdentifier,
        isDefault = isDefault, isVerified = isVerified, gatewayToken = gatewayToken, createdAt = createdAt
    )
}
