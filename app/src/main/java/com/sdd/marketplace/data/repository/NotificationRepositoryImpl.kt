package com.sdd.marketplace.data.repository

import com.sdd.marketplace.data.local.dao.NotificationDao
import com.sdd.marketplace.data.mappers.toDomain
import com.sdd.marketplace.data.remote.dto.NotificationDto
import com.sdd.marketplace.domain.model.Notification
import com.sdd.marketplace.domain.repository.AuthRepository
import com.sdd.marketplace.domain.repository.NotificationRepository
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val notificationDao: NotificationDao,
    private val authRepository: AuthRepository
) : NotificationRepository {

    override fun getNotifications(): Flow<List<Notification>> = flow {
        val userId = authRepository.getCurrentUserId() ?: return@flow emit(emptyList())
        try {
            val dtos = postgrest["notifications"].select {
                filter { eq("user_id", userId) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
            }.decodeList<NotificationDto>()
            emit(dtos.map { it.toDomain() })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override fun getUnreadCount(): Flow<Int> =
        notificationDao.getUnreadCount()

    override suspend fun markRead(notificationId: String): Result<Unit> = runCatching {
        postgrest["notifications"].update(mapOf("is_read" to true)) {
            filter { eq("id", notificationId) }
        }
        notificationDao.markRead(notificationId)
    }

    override suspend fun markAllRead(): Result<Unit> = runCatching {
        val userId = authRepository.getCurrentUserId() ?: throw Exception("Not authenticated")
        postgrest["notifications"].update(mapOf("is_read" to true)) {
            filter { eq("user_id", userId) }
        }
        notificationDao.markAllRead()
    }

    override suspend fun deleteNotification(notificationId: String): Result<Unit> = runCatching {
        postgrest["notifications"].delete { filter { eq("id", notificationId) } }
        notificationDao.deleteNotification(notificationId)
    }
}
