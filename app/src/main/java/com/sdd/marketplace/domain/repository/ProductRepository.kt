package com.sdd.marketplace.domain.repository

import androidx.paging.PagingData
import com.sdd.marketplace.domain.model.Category
import com.sdd.marketplace.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun getProducts(category: String? = null, searchQuery: String? = null): Flow<PagingData<Product>>
    fun getFeaturedProducts(): Flow<List<Product>>
    fun getTrendingProducts(): Flow<List<Product>>
    fun getNearbyProducts(lat: Double, lng: Double, radiusKm: Double): Flow<List<Product>>
    fun getSellerProducts(sellerId: String): Flow<PagingData<Product>>
    fun getSoldProducts(sellerId: String): Flow<PagingData<Product>>
    fun getSavedProducts(userId: String): Flow<PagingData<Product>>
    fun getRelatedProducts(productId: String, category: String): Flow<List<Product>>
    fun getCategories(): Flow<List<Category>>
    suspend fun getProduct(productId: String): Result<Product>
    suspend fun createProduct(product: Product, imagePaths: List<String>): Result<Product>
    suspend fun updateProduct(product: Product): Result<Product>
    suspend fun deleteProduct(productId: String): Result<Unit>
    suspend fun markAsSold(productId: String): Result<Unit>
    suspend fun archiveProduct(productId: String): Result<Unit>
    suspend fun boostProduct(productId: String): Result<Unit>
    suspend fun incrementViewCount(productId: String)
    fun searchProducts(query: String): Flow<PagingData<Product>>
    fun subscribeToRealtimeProducts(): Flow<Unit>
}
