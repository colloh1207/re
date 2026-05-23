package com.sdd.marketplace.feature.notifications.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.sdd.marketplace.core.ui.components.EmptyState
import com.sdd.marketplace.core.ui.theme.SddPink
import com.sdd.marketplace.domain.model.Notification
import com.sdd.marketplace.domain.model.NotificationType
import com.sdd.marketplace.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    val uiState = MutableStateFlow(NotificationsUiState())

    init {
        viewModelScope.launch {
            notificationRepository.getNotifications().collect { notifications ->
                uiState.update { it.copy(notifications = notifications, isLoading = false) }
            }
        }
    }

    fun markRead(id: String) = viewModelScope.launch { notificationRepository.markRead(id) }
    fun markAllRead() = viewModelScope.launch { notificationRepository.markAllRead() }
}

@Composable
fun NotificationsScreen(navController: NavController, viewModel: NotificationsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } },
            title = { Text("Notifications", fontWeight = FontWeight.Bold) },
            actions = {
                TextButton(onClick = { viewModel.markAllRead() }) { Text("Mark all read", color = SddPink) }
            }
        )

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SddPink) }
        } else if (uiState.notifications.isEmpty()) {
            EmptyState("No notifications", "You're all caught up!", icon = { Icon(Icons.Outlined.Notifications, "Empty", modifier = Modifier.size(64.dp)) })
        } else {
            LazyColumn {
                items(uiState.notifications, key = { it.id }) { notification ->
                    NotificationItem(notification = notification, onClick = { viewModel.markRead(notification.id) })
                    Divider()
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: Notification, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(notification.title, fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Normal) },
        supportingContent = { Text(notification.body) },
        leadingContent = {
            Icon(
                when (notification.type) {
                    NotificationType.MESSAGE -> Icons.Filled.Message
                    NotificationType.LIKE -> Icons.Filled.Favorite
                    NotificationType.FOLLOW -> Icons.Filled.PersonAdd
                    NotificationType.SALE -> Icons.Filled.ShoppingBag
                    NotificationType.OFFER -> Icons.Filled.LocalOffer
                    else -> Icons.Filled.Notifications
                },
                notification.type.name,
                tint = SddPink
            )
        },
        trailingContent = { if (!notification.isRead) Box(Modifier.size(8.dp).background(SddPink, CircleShape)) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
