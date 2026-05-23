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
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SddFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var auth: Auth
    @Inject lateinit var postgrest: Postgrest

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("New FCM token received")
        saveFcmToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Timber.d("FCM message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val title = remoteMessage.notification?.title
            ?: data["title"]
            ?: getDefaultTitle(data["type"])
        val body = remoteMessage.notification?.body
            ?: data["body"]
            ?: ""

        if (title.isNotBlank() || body.isNotBlank()) {
            showNotification(title = title, body = body, data = data)
        }
    }

    private fun getDefaultTitle(type: String?): String = when (type) {
        "message"      -> "New Message"
        "order"        -> "Order Update"
        "offer"        -> "New Offer"
        "rating"       -> "New Review"
        "follow"       -> "New Follower"
        "sale"         -> "Item Sold!"
        "kyc"          -> "Verification Update"
        else           -> "Sdd Marketplace"
    }

    private fun saveFcmToken(token: String) {
        serviceScope.launch {
            try {
                val userId = auth.currentUserOrNull()?.id ?: return@launch
                postgrest["users"].update(mapOf("fcm_token" to token)) {
                    filter { eq("id", userId) }
                }
                Timber.d("FCM token saved to Supabase")
            } catch (e: Exception) {
                Timber.w(e, "Failed to save FCM token")
            }
        }
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        createNotificationChannels()

        val channelId = when (data["type"]) {
            "message"      -> CHANNEL_MESSAGES
            "order", "sale" -> CHANNEL_ORDERS
            "offer"        -> CHANNEL_OFFERS
            "rating"       -> CHANNEL_RATINGS
            else           -> CHANNEL_GENERAL
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notification_type", data["type"] ?: "general")
            putExtra("notification_id", data["id"] ?: "")
            putExtra("reference_id", data["reference_id"] ?: "")
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(
                if (channelId == CHANNEL_MESSAGES || channelId == CHANNEL_OFFERS)
                    NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(
                System.currentTimeMillis().toInt(),
                notification
            )
        } catch (e: SecurityException) {
            Timber.w(e, "Notification permission not granted")
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannels(listOf(
                NotificationChannel(CHANNEL_MESSAGES, "Messages", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Chat messages from buyers and sellers"
                    enableVibration(true)
                },
                NotificationChannel(CHANNEL_ORDERS, "Orders & Sales", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Order updates, payment confirmations, and sale alerts"
                },
                NotificationChannel(CHANNEL_OFFERS, "Offers", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "New offers on your listings"
                    enableVibration(true)
                },
                NotificationChannel(CHANNEL_RATINGS, "Reviews & Ratings", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "New reviews and ratings received"
                },
                NotificationChannel(CHANNEL_GENERAL, "General", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "General app notifications"
                }
            ))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    companion object {
        const val CHANNEL_MESSAGES = "sdd_messages"
        const val CHANNEL_ORDERS   = "sdd_orders"
        const val CHANNEL_OFFERS   = "sdd_offers"
        const val CHANNEL_RATINGS  = "sdd_ratings"
        const val CHANNEL_GENERAL  = "sdd_general"

        fun saveFcmTokenStatic(auth: Auth, postgrest: Postgrest, token: String) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val userId = auth.currentUserOrNull()?.id ?: return@launch
                    postgrest["users"].update(mapOf("fcm_token" to token)) {
                        filter { eq("id", userId) }
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Static FCM save failed")
                }
            }
        }
    }
}
