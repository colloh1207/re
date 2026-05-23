package com.sdd.marketplace.domain.repository

import com.sdd.marketplace.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    fun getFavorites(): Flow<List<Product>>
    fun isFavorite(productId: String): Flow<Boolean>
    suspend fun addFavorite(productId: String): Result<Unit>
    suspend fun removeFavorite(productId: String): Result<Unit>
    suspend fun toggleFavorite(productId: String): Result<Boolean>
}
