package com.sdd.marketplace.data.local.dao

import androidx.room.*
import com.sdd.marketplace.data.local.entities.AppPreferencesEntity
import com.sdd.marketplace.data.local.entities.ReferralEntity
import com.sdd.marketplace.data.local.entities.SavedAccountEntity
import com.sdd.marketplace.data.local.entities.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppPreferencesDao {
    @Query("SELECT value FROM app_preferences WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(pref: AppPreferencesEntity)

    @Query("DELETE FROM app_preferences WHERE `key` = :key")
    suspend fun delete(key: String)
}

@Dao
interface SavedAccountDao {
    @Query("SELECT * FROM saved_accounts ORDER BY isActive DESC, addedAt DESC")
    fun getAll(): Flow<List<SavedAccountEntity>>

    @Query("SELECT * FROM saved_accounts ORDER BY isActive DESC, addedAt DESC")
    suspend fun getAllSync(): List<SavedAccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: SavedAccountEntity)

    @Query("UPDATE saved_accounts SET isActive = 0")
    suspend fun clearActive()

    @Query("UPDATE saved_accounts SET isActive = 1 WHERE userId = :userId")
    suspend fun setActive(userId: String)

    @Query("DELETE FROM saved_accounts WHERE userId = :userId")
    suspend fun delete(userId: String)

    @Query("SELECT COUNT(*) FROM saved_accounts")
    suspend fun count(): Int
}

@Dao
interface ReferralDao {
    @Query("SELECT * FROM referrals WHERE referrerId = :userId ORDER BY createdAt DESC")
    fun getByReferrer(userId: String): Flow<List<ReferralEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(referral: ReferralEntity)

    @Query("SELECT COUNT(*) FROM referrals WHERE referrerId = :userId AND status = 'completed'")
    suspend fun countCompleted(userId: String): Int

    @Query("SELECT SUM(rewardAmount) FROM referrals WHERE referrerId = :userId AND status = 'completed'")
    suspend fun totalEarnings(userId: String): Double?
}

@Dao
interface SyncQueueDao {
    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC LIMIT 50")
    suspend fun getPending(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE sync_queue SET retryCount = retryCount + 1, lastAttemptAt = :now WHERE id = :id")
    suspend fun incrementRetry(id: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM sync_queue WHERE retryCount >= 5")
    suspend fun clearFailed()
}
