package com.sdd.marketplace.data.local.dao

import androidx.room.*
import com.sdd.marketplace.data.local.entities.FavoriteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE userId = :userId")
    fun getFavoritesByUser(userId: String): Flow<List<FavoriteEntity>>

    @Query("SELECT COUNT(*) FROM favorites WHERE productId = :productId AND userId = :userId")
    fun isFavorite(productId: String, userId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE productId = :productId AND userId = :userId")
    suspend fun removeFavorite(productId: String, userId: String)

    @Query("DELETE FROM favorites WHERE userId = :userId")
    suspend fun clearFavorites(userId: String)
}
