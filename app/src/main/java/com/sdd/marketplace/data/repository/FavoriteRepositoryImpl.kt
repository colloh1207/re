package com.sdd.marketplace.data.repository

import com.sdd.marketplace.data.local.dao.FavoriteDao
import com.sdd.marketplace.data.local.dao.ProductDao
import com.sdd.marketplace.data.local.entities.FavoriteEntity
import com.sdd.marketplace.data.mappers.toDomain
import com.sdd.marketplace.domain.model.Product
import com.sdd.marketplace.domain.repository.AuthRepository
import com.sdd.marketplace.domain.repository.FavoriteRepository
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val favoriteDao: FavoriteDao,
    private val productDao: ProductDao,
    private val authRepository: AuthRepository
) : FavoriteRepository {

    override fun getFavorites(): Flow<List<Product>> = flow {
        val userId = authRepository.getCurrentUserId() ?: return@flow emit(emptyList())
        val favorites = favoriteDao.getFavoritesByUser(userId).first()
        val products = favorites.mapNotNull { fav ->
            productDao.getProductById(fav.productId)?.toDomain()
        }
        emit(products)
    }

    override fun isFavorite(productId: String): Flow<Boolean> {
        val userId = authRepository.getCurrentUserId() ?: return flowOf(false)
        return favoriteDao.isFavorite(productId, userId).map { it > 0 }
    }

    override suspend fun addFavorite(productId: String): Result<Unit> = runCatching {
        val userId = authRepository.getCurrentUserId() ?: throw Exception("Not authenticated")
        favoriteDao.addFavorite(FavoriteEntity(productId = productId, userId = userId))
        postgrest["favorites"].insert(mapOf("user_id" to userId, "product_id" to productId))
    }

    override suspend fun removeFavorite(productId: String): Result<Unit> = runCatching {
        val userId = authRepository.getCurrentUserId() ?: throw Exception("Not authenticated")
        favoriteDao.removeFavorite(productId, userId)
        postgrest["favorites"].delete {
            filter { eq("user_id", userId); eq("product_id", productId) }
        }
    }

    override suspend fun toggleFavorite(productId: String): Result<Boolean> = runCatching {
        val userId = authRepository.getCurrentUserId() ?: throw Exception("Not authenticated")
        val count = favoriteDao.isFavorite(productId, userId).first()
        if (count > 0) {
            removeFavorite(productId)
            false
        } else {
            addFavorite(productId)
            true
        }
    }
}
