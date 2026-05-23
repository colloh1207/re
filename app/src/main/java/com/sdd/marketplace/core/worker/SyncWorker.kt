package com.sdd.marketplace.core.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sdd.marketplace.core.util.NetworkChecker
import com.sdd.marketplace.data.local.dao.SyncQueueDao
import com.sdd.marketplace.domain.repository.ProductRepository
import com.sdd.marketplace.domain.repository.UserRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import timber.log.Timber

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
    private val syncQueueDao: SyncQueueDao,
    private val postgrest: Postgrest,
    private val networkChecker: NetworkChecker
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        if (!networkChecker.isOnline()) {
            Timber.d("SyncWorker: Offline — skipping sync")
            return Result.retry()
        }
        return try {
            Timber.d("SyncWorker: Starting background sync")
            drainSyncQueue()
            productRepository.getFeaturedProducts().first()
            userRepository.setOnlineStatus(true)
            Timber.d("SyncWorker: Background sync completed")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "SyncWorker: Background sync failed")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun drainSyncQueue() {
        syncQueueDao.clearFailed()
        val pending = syncQueueDao.getPending()
        Timber.d("SyncWorker: Processing ${pending.size} queued operations")

        for (item in pending) {
            try {
                when (item.operation) {
                    "INSERT" -> {
                        val payload = parsePayload(item.payload)
                        postgrest[item.tableName].insert(payload)
                        syncQueueDao.delete(item.id)
                        Timber.d("SyncWorker: Synced INSERT to ${item.tableName}")
                    }
                    "UPSERT" -> {
                        val payload = parsePayload(item.payload)
                        postgrest[item.tableName].upsert(payload)
                        syncQueueDao.delete(item.id)
                        Timber.d("SyncWorker: Synced UPSERT to ${item.tableName}")
                    }
                    "UPDATE" -> {
                        val payload = parsePayload(item.payload)
                        val id = payload["id"] as? String
                        if (id != null) {
                            postgrest[item.tableName].update(payload) {
                                filter { eq("id", id) }
                            }
                            syncQueueDao.delete(item.id)
                            Timber.d("SyncWorker: Synced UPDATE to ${item.tableName}")
                        } else {
                            syncQueueDao.incrementRetry(item.id)
                        }
                    }
                    "DELETE" -> {
                        val payload = parsePayload(item.payload)
                        val id = payload["id"] as? String
                        if (id != null) {
                            postgrest[item.tableName].delete {
                                filter { eq("id", id) }
                            }
                            syncQueueDao.delete(item.id)
                            Timber.d("SyncWorker: Synced DELETE to ${item.tableName}")
                        } else {
                            syncQueueDao.incrementRetry(item.id)
                        }
                    }
                    else -> {
                        Timber.w("SyncWorker: Unknown operation ${item.operation} — skipping")
                        syncQueueDao.delete(item.id)
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "SyncWorker: Failed to sync ${item.operation} to ${item.tableName} (attempt ${item.retryCount + 1})")
                syncQueueDao.incrementRetry(item.id)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parsePayload(payload: String): Map<String, Any?> {
        return try {
            val element = Json.parseToJsonElement(payload)
            val map = mutableMapOf<String, Any?>()
            if (element is kotlinx.serialization.json.JsonObject) {
                for ((key, value) in element) {
                    map[key] = when (value) {
                        is kotlinx.serialization.json.JsonPrimitive -> {
                            when {
                                value.isString -> value.content
                                value.content == "true" || value.content == "false" -> value.content.toBoolean()
                                value.content.contains(".") -> value.content.toDoubleOrNull() ?: value.content
                                else -> value.content.toLongOrNull() ?: value.content
                            }
                        }
                        is kotlinx.serialization.json.JsonNull -> null
                        else -> value.toString()
                    }
                }
            }
            map
        } catch (e: Exception) {
            Timber.w(e, "SyncWorker: Failed to parse payload")
            emptyMap()
        }
    }

    companion object {
        const val WORK_NAME = "sdd_background_sync"
        const val WORK_NAME_IMMEDIATE = "sdd_immediate_sync"
    }
}
