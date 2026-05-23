package com.sdd.marketplace.domain.repository

import com.sdd.marketplace.domain.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getNotifications(): Flow<List<Notification>>
    fun getUnreadCount(): Flow<Int>
    suspend fun markRead(notificationId: String): Result<Unit>
    suspend fun markAllRead(): Result<Unit>
    suspend fun deleteNotification(notificationId: String): Result<Unit>
}
