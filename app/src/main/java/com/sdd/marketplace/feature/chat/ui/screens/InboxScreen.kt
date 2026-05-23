package com.sdd.marketplace.feature.chat.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.sdd.marketplace.core.navigation.Screen
import com.sdd.marketplace.core.ui.components.*
import com.sdd.marketplace.core.ui.theme.*
import com.sdd.marketplace.domain.model.*
import com.sdd.marketplace.feature.chat.viewmodel.InboxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(navController: NavController, viewModel: InboxViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showBlockConfirm by remember { mutableStateOf<String?>(null) }
    var showReportSheet by remember { mutableStateOf<String?>(null) }

    showBlockConfirm?.let { userId ->
        AlertDialog(
            onDismissRequest = { showBlockConfirm = null },
            icon = { Icon(Icons.Filled.Block, "Block", tint = MaterialTheme.colorScheme.error) },
            title = { Text("Block User?") },
            text = { Text("They won't be able to message you or see your listings. You can unblock them in Settings.") },
            confirmButton = {
                Button(onClick = { viewModel.blockUser(userId); showBlockConfirm = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Block") }
            },
            dismissButton = { TextButton(onClick = { showBlockConfirm = null }) { Text("Cancel") } }
        )
    }

    showReportSheet?.let { userId ->
        ReportUserDialog(userId = userId, onReport = { cat, desc -> viewModel.reportUser(userId, cat, desc); showReportSheet = null }, onDismiss = { showReportSheet = null })
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text("Inbox", fontWeight = FontWeight.Bold) },
            actions = {
                IconButton(onClick = { }) { Icon(Icons.Outlined.FilterList, "Filter") }
            }
        )

        OutlinedTextField(
            value = uiState.searchQuery, onValueChange = { viewModel.onSearchQueryChanged(it) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder = { Text("Search messages or users...") },
            leadingIcon = { Icon(Icons.Outlined.Search, "Search") },
            trailingIcon = {
                if (uiState.searchQuery.isNotBlank()) IconButton(onClick = { viewModel.onSearchQueryChanged("") }) { Icon(Icons.Filled.Clear, "Clear") }
            },
            singleLine = true, shape = RoundedCornerShape(25.dp),
            colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, focusedBorderColor = SddPink)
        )

        // User search results
        if (uiState.searchQuery.length >= 2 && uiState.userSearchResults.isNotEmpty()) {
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Column {
                    Text("Users", Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontSize = 12.sp, color = SddPink, fontWeight = FontWeight.Bold)
                    uiState.userSearchResults.take(5).forEach { user ->
                        Row(Modifier.fillMaxWidth().clickable {
                            // TODO: open or create chat with this user
                        }.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box {
                                AsyncImage(model = user.avatarUrl, contentDescription = null, modifier = Modifier.size(36.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                                if (user.isOnline) Box(Modifier.size(10.dp).clip(CircleShape).background(OnlineGreen).align(Alignment.BottomEnd))
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(user.fullName, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(if (user.isOnline) "Online" else "Offline", fontSize = 11.sp, color = if (user.isOnline) OnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (user.isVerified) VerifiedBadge()
                        }
                    }
                }
            }
        }

        val filters = listOf("All", "Unread", "Orders", "Offers")
        ScrollableTabRow(selectedTabIndex = filters.indexOf(uiState.selectedFilter), edgePadding = 16.dp,
            containerColor = MaterialTheme.colorScheme.background, contentColor = SddPink) {
            filters.forEach { filter ->
                Tab(selected = uiState.selectedFilter == filter, onClick = { viewModel.setFilter(filter) }, text = { Text(filter) })
            }
        }

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SddPink) }
        } else if (uiState.filteredChats.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.ChatBubbleOutline, "No Chats", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No messages yet", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Browse products and message sellers to start chatting!", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(uiState.filteredChats, key = { it.id }) { chat ->
                    ChatListItem(
                        chat = chat,
                        currentUserId = "",
                        onClick = { navController.navigate(Screen.ChatDetail.createRoute(chat.id)) },
                        onLongPress = { }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatListItem(chat: Chat, currentUserId: String, onClick: () -> Unit, onLongPress: () -> Unit) {
    val otherUser = chat.participants.firstOrNull { it.id != currentUserId }
    val hasUnread = chat.unreadCount > 0

    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box {
            AsyncImage(model = otherUser?.avatarUrl, contentDescription = null,
                modifier = Modifier.size(52.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            if (otherUser?.isOnline == true) {
                Box(Modifier.size(13.dp).clip(CircleShape).background(OnlineGreen)
                    .align(Alignment.BottomEnd).background(MaterialTheme.colorScheme.surface.copy(alpha = 0f)))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(otherUser?.fullName ?: "Unknown", fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal, fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (otherUser?.isVerified == true) { Spacer(Modifier.width(2.dp)); VerifiedBadge() }
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val lastContent = when {
                    chat.lastMessage?.isUnsent == true -> "Message unsent"
                    chat.lastMessage?.type == MessageType.IMAGE -> "📷 Image"
                    chat.lastMessage?.type == MessageType.LOCATION -> "📍 Location"
                    else -> chat.lastMessage?.content ?: "Say hi!"
                }
                Text(lastContent, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp,
                    color = if (hasUnread) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal, modifier = Modifier.weight(1f))
            }
            chat.product?.let {
                Text("About: ${it.title}", fontSize = 11.sp, color = SddPink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(chat.updatedAt.takeLast(5), fontSize = 11.sp, color = if (hasUnread) SddPink else MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            if (hasUnread) {
                Box(Modifier.size(20.dp).clip(CircleShape).background(SddPink), contentAlignment = Alignment.Center) {
                    Text(if (chat.unreadCount > 99) "99+" else "${chat.unreadCount}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    HorizontalDivider(Modifier.padding(start = 80.dp), thickness = 0.5.dp)
}
