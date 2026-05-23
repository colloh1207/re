package com.sdd.marketplace.domain.repository

import com.sdd.marketplace.domain.model.Boost
import com.sdd.marketplace.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface BoostRepository {
    fun getMyBoosts(): Flow<List<Boost>>
    suspend fun createBoost(productIds: List<String>, tierId: String, currency: String): Result<Boost>
    suspend fun pollBoostPayment(boostId: String): Result<Boost>
    suspend fun cancelBoost(boostId: String): Result<Unit>
    fun getMyListedProducts(): Flow<List<Product>>
}
