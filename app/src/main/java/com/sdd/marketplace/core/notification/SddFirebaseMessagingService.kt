package com.sdd.marketplace.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sdd.marketplace.MainActivity
import timber.log.Timber

class SddFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("New FCM token: $token")
        // Save token to Supabase user profile
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Timber.d("FCM message received from: ${remoteMessage.from}")

        remoteMessage.notification?.let { notification ->
            showNotification(
                title = notification.title ?: "Sdd Marketplace",
                body = notification.body ?: "",
                data = remoteMessage.data
            )
        }
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        createNotificationChannels()

        val channelId = when (data["type"]) {
            "message" -> CHANNEL_MESSAGES
            "order" -> CHANNEL_ORDERS
            else -> CHANNEL_GENERAL
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_data", HashMap(data))
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_notification_overlay)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES, "Messages", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Chat messages from other users" }

            val ordersChannel = NotificationChannel(
                CHANNEL_ORDERS, "Orders", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Order updates and notifications" }

            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL, "General", NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "General app notifications" }

            notificationManager.createNotificationChannels(listOf(messagesChannel, ordersChannel, generalChannel))
        }
    }

    companion object {
        const val CHANNEL_MESSAGES = "sdd_messages"
        const val CHANNEL_ORDERS = "sdd_orders"
        const val CHANNEL_GENERAL = "sdd_general"
    }
}
