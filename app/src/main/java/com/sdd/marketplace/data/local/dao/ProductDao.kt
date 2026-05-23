package com.sdd.marketplace.data.local.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.sdd.marketplace.data.local.entities.ProductEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY createdAt DESC")
    fun getPagedProducts(): PagingSource<Int, ProductEntity>

    @Query("SELECT * FROM products WHERE category = :category ORDER BY createdAt DESC")
    fun getProductsByCategory(category: String): PagingSource<Int, ProductEntity>

    @Query("SELECT * FROM products WHERE isFeatured = 1 ORDER BY createdAt DESC LIMIT 20")
    fun getFeaturedProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE sellerId = :sellerId ORDER BY createdAt DESC")
    fun getSellerProducts(sellerId: String): PagingSource<Int, ProductEntity>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchProducts(query: String): PagingSource<Int, ProductEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProduct(id: String)

    @Query("SELECT * FROM products WHERE sellerId = :sellerId AND isSold = 1 ORDER BY updatedAt DESC")
    fun getSoldProducts(sellerId: String): PagingSource<Int, ProductEntity>

    @Query("SELECT p.* FROM products p INNER JOIN favorites f ON p.id = f.productId WHERE f.userId = :userId")
    fun getSavedProducts(userId: String): PagingSource<Int, ProductEntity>

    @Query("SELECT * FROM products WHERE category = :category AND id != :excludeId AND isSold = 0 ORDER BY viewCount DESC LIMIT 8")
    fun getRelatedProducts(category: String, excludeId: String): Flow<List<ProductEntity>>

    @Query("DELETE FROM products WHERE cachedAt < :olderThan")
    suspend fun deleteOldCache(olderThan: Long)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getCount(): Int
}
