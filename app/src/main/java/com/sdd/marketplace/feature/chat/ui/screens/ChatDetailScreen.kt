package com.sdd.marketplace.feature.chat.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.sdd.marketplace.core.navigation.Screen
import com.sdd.marketplace.core.ui.components.VerifiedBadge
import com.sdd.marketplace.core.ui.theme.*
import com.sdd.marketplace.domain.model.*
import com.sdd.marketplace.feature.chat.viewmodel.ChatDetailEvent
import com.sdd.marketplace.feature.chat.viewmodel.ChatDetailViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.roundToInt

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(navController: NavController, viewModel: ChatDetailViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showBlockDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var selectedMessage by remember { mutableStateOf<Message?>(null) }

    val otherUser = uiState.chat?.participants?.firstOrNull { it.id != uiState.currentUserId }
    val isOnline = uiState.partnerOnline

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ChatDetailEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is ChatDetailEvent.ScrollToBottom -> {
                    if (uiState.messages.isNotEmpty()) listState.animateScrollToItem(uiState.messages.size - 1)
                }
            }
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) listState.animateScrollToItem(uiState.messages.size - 1)
    }

    if (showBlockDialog) {
        otherUser?.let { user ->
            AlertDialog(
                onDismissRequest = { showBlockDialog = false },
                icon = { Icon(Icons.Filled.Block, "Block", tint = MaterialTheme.colorScheme.error) },
                title = { Text("Block ${user.fullName}?") },
                text = { Text("They won't be able to message you or see your listings.") },
                confirmButton = {
                    Button(onClick = { viewModel.blockUser(user.id); showBlockDialog = false; navController.popBackStack() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Block") }
                },
                dismissButton = { TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") } }
            )
        }
    }

    if (showReportDialog) {
        otherUser?.let { user ->
            ReportUserDialog(userId = user.id,
                onReport = { cat, desc -> viewModel.reportUser(user.id, cat, desc); showReportDialog = false },
                onDismiss = { showReportDialog = false })
        }
    }

    selectedMessage?.let { msg ->
        MessageContextMenu(
            message = msg,
            isMine = msg.senderId == uiState.currentUserId,
            onReply = { viewModel.startReplying(msg); selectedMessage = null },
            onEdit = { if (msg.senderId == uiState.currentUserId && msg.type == MessageType.TEXT) { viewModel.startEditing(msg); selectedMessage = null } },
            onDelete = { viewModel.deleteMessage(msg.id); selectedMessage = null },
            onUnsend = { viewModel.unsendMessage(msg.id); selectedMessage = null },
            onDismiss = { selectedMessage = null }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } },
                title = {
                    Row(Modifier.clickable(onClick = { otherUser?.id?.let { navController.navigate(Screen.Profile.createRoute(it)) } }),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            AsyncImage(model = otherUser?.avatarUrl, contentDescription = null,
                                modifier = Modifier.size(36.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            if (isOnline) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(OnlineGreen)
                                    .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                    .align(Alignment.BottomEnd))
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(otherUser?.fullName ?: "Chat", fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                                if (otherUser?.isVerified == true) { Spacer(Modifier.width(4.dp)); VerifiedBadge() }
                            }
                            Text(
                                when {
                                    uiState.partnerTyping -> "Typing..."
                                    isOnline -> "Online"
                                    else -> otherUser?.lastSeen?.let { "Last seen ${it.take(10)}" } ?: "Offline"
                                },
                                color = if (isOnline) OnlineGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp, maxLines = 1
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { }) { Icon(Icons.Outlined.Phone, "Call", tint = SddPink) }
                    Box {
                        IconButton(onClick = { showMenu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(leadingIcon = { Icon(Icons.Filled.Person, "Profile") }, text = { Text("View Profile") },
                                onClick = { showMenu = false; otherUser?.id?.let { navController.navigate(Screen.Profile.createRoute(it)) } })
                            DropdownMenuItem(leadingIcon = { Icon(Icons.Filled.Block, "Block", tint = MaterialTheme.colorScheme.error) },
                                text = { Text("Block User", color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; showBlockDialog = true })
                            DropdownMenuItem(leadingIcon = { Icon(Icons.Filled.Flag, "Report", tint = MaterialTheme.colorScheme.error) },
                                text = { Text("Report User", color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; showReportDialog = true })
                            DropdownMenuItem(leadingIcon = { Icon(Icons.Outlined.Notifications, "Mute") }, text = { Text("Mute Notifications") }, onClick = { showMenu = false })
                            DropdownMenuItem(leadingIcon = { Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error) },
                                text = { Text("Delete Chat", color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false })
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column {
                uiState.chat?.product?.let { product ->
                    Surface(color = SddPink.copy(alpha = 0.1f)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable {
                            navController.navigate(Screen.ProductDetail.createRoute(product.id))
                        }, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ShoppingBag, "Product", tint = SddPink, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("About: ${product.title}", fontSize = 12.sp, color = SddPink, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 1)
                            Text("₹${String.format("%.0f", product.price)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SddPink)
                        }
                    }
                }

                // Violation Warning
                uiState.violationWarning?.let { warning ->
                    Surface(color = Color(0xFFFFF3E0)) {
                        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, "Warning", tint = Color(0xFFFF9800), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(warning, fontSize = 11.sp, color = Color(0xFFE65100), modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.dismissViolationWarning() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Filled.Close, "Dismiss", modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                // Reply preview
                uiState.replyingTo?.let { replyMsg ->
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(3.dp).height(40.dp).background(SddPink, RoundedCornerShape(2.dp)))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(replyMsg.sender?.fullName ?: "You", fontSize = 12.sp, color = SddPink, fontWeight = FontWeight.Bold)
                                Text(
                                    if (replyMsg.isUnsent) "Message unsent" else replyMsg.content.take(80),
                                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2
                                )
                            }
                            IconButton(onClick = { viewModel.cancelReplying() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Close, "Cancel reply", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Edit indicator
                uiState.editingMessage?.let {
                    Surface(color = SddPink.copy(alpha = 0.1f)) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Edit, "Editing", tint = SddPink, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Editing message", fontSize = 12.sp, color = SddPink, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.cancelEditing() }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Filled.Close, "Cancel", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                if (showAttachmentMenu) {
                    Surface(tonalElevation = 4.dp) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                            AttachmentOption(Icons.Outlined.Image, "Gallery") { viewModel.sendImage(); showAttachmentMenu = false }
                            AttachmentOption(Icons.Outlined.CameraAlt, "Camera") { showAttachmentMenu = false }
                            AttachmentOption(Icons.Outlined.LocationOn, "Location") {
                                coroutineScope.launch {
                                    try {
                                        val client = LocationServices.getFusedLocationProviderClient(context)
                                        val loc = client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                                        val lat = loc?.latitude ?: 28.6139
                                        val lng = loc?.longitude ?: 77.2090
                                        viewModel.sendLocation(lat, lng, "My Location")
                                    } catch (e: Exception) {
                                        viewModel.sendLocation(28.6139, 77.2090, "Location")
                                    }
                                }
                                showAttachmentMenu = false
                            }
                            AttachmentOption(Icons.Outlined.ShoppingBag, "Product") { navController.navigate(Screen.Search.route); showAttachmentMenu = false }
                        }
                    }
                }

                ChatInputBar(
                    messageText = uiState.messageText,
                    isEditing = uiState.editingMessage != null,
                    onMessageChanged = { viewModel.onMessageTextChanged(it) },
                    onSend = { viewModel.sendMessage() },
                    onAttach = { showAttachmentMenu = !showAttachmentMenu }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            items(uiState.messages, key = { it.id }) { message ->
                SwipeableMessageRow(
                    message = message,
                    isMine = message.senderId == uiState.currentUserId,
                    onSwipe = { viewModel.startReplying(message) },
                    onLongPress = { selectedMessage = message }
                )
            }
            if (uiState.partnerTyping) { item { TypingIndicator() } }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
fun SwipeableMessageRow(message: Message, isMine: Boolean, onSwipe: () -> Unit, onLongPress: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = offsetX, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "swipe")
    var hasTriggered by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { offsetX = 0f; hasTriggered = false },
                    onDragCancel = { offsetX = 0f; hasTriggered = false },
                    onHorizontalDrag = { _, delta ->
                        val direction = if (isMine) -1f else 1f
                        val newOffset = (offsetX + delta * direction).coerceIn(0f, 72f)
                        offsetX = newOffset
                        if (newOffset >= 60f && !hasTriggered) {
                            hasTriggered = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSwipe()
                        }
                    }
                )
            }
    ) {
        if (animatedOffset > 10f) {
            Box(
                Modifier.align(if (isMine) Alignment.CenterStart else Alignment.CenterEnd).padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Reply, "Reply", tint = SddPink.copy(alpha = (animatedOffset / 72f)), modifier = Modifier.size(20.dp))
            }
        }
        Box(
            Modifier.offset { IntOffset((if (isMine) -animatedOffset else animatedOffset).roundToInt(), 0) }
        ) {
            MessageBubble(message = message, isMine = isMine, onLongPress = onLongPress)
        }
    }
}

@Composable
fun MessageContextMenu(message: Message, isMine: Boolean, onReply: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onUnsend: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column {
                if (!message.isUnsent && !message.isDeleted) {
                    ListItem(headlineContent = { Text("Reply") }, leadingContent = { Icon(Icons.Filled.Reply, "Reply", tint = SddPink) }, modifier = Modifier.clickable { onReply() })
                    if (isMine && message.type == MessageType.TEXT && !message.isUnsent) {
                        ListItem(headlineContent = { Text("Edit") }, leadingContent = { Icon(Icons.Outlined.Edit, "Edit", tint = SddPink) }, modifier = Modifier.clickable { onEdit() })
                    }
                }
                if (isMine && !message.isUnsent) {
                    ListItem(headlineContent = { Text("Unsend", color = MaterialTheme.colorScheme.error) },
                        leadingContent = { Icon(Icons.Outlined.RemoveCircle, "Unsend", tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable { onUnsend() })
                }
                ListItem(headlineContent = { Text("Delete for me", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable { onDelete() })
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun MessageBubble(message: Message, isMine: Boolean, onLongPress: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start) {
        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start, modifier = Modifier.widthIn(max = 290.dp)) {
            // Reply-to context
            message.replyToContent?.let { replyContent ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isMine) SddPink.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(bottom = 2.dp).fillMaxWidth(0.9f)
                ) {
                    Row(Modifier.padding(6.dp)) {
                        Box(Modifier.width(3.dp).fillMaxHeight().background(if (isMine) Color.White else SddPink, RoundedCornerShape(2.dp)))
                        Spacer(Modifier.width(6.dp))
                        Column {
                            Text(message.replyToSenderName ?: "Reply", fontSize = 11.sp, color = if (isMine) Color.White else SddPink, fontWeight = FontWeight.Bold)
                            Text(replyContent.take(60), fontSize = 11.sp, color = if (isMine) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            when {
                message.isUnsent -> UnsendMessageBubble(message, isMine)
                message.isDeleted -> DeletedMessageBubble(isMine)
                message.type == MessageType.LOCATION -> LocationMessageBubble(message, isMine, onLongPress)
                message.type == MessageType.IMAGE -> ImageMessageBubble(message, isMine, onLongPress)
                else -> TextMessageBubble(message, isMine, onLongPress, haptic)
            }
        }
    }
}

@Composable
fun UnsendMessageBubble(message: Message, isMine: Boolean) {
    Box(modifier = Modifier.background(
        color = if (isMine) SddPink.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (isMine) 16.dp else 4.dp, bottomEnd = if (isMine) 4.dp else 16.dp)
    ).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.RemoveCircle, "Unsent", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            Spacer(Modifier.width(4.dp))
            Text("Message unsent", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 13.sp)
        }
    }
}

@Composable
fun DeletedMessageBubble(isMine: Boolean) {
    Box(modifier = Modifier.background(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = RoundedCornerShape(16.dp)
    ).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Delete, "Deleted", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Spacer(Modifier.width(4.dp))
            Text("This message was deleted", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontSize = 13.sp)
        }
    }
}

@Composable
fun TextMessageBubble(message: Message, isMine: Boolean, onLongPress: () -> Unit, haptic: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    Box(modifier = Modifier
        .background(
            color = if (isMine) SddPink else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (isMine) 16.dp else 4.dp, bottomEnd = if (isMine) 4.dp else 16.dp)
        )
        .pointerInput(Unit) {
            detectTapGestures(onLongPress = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress); onLongPress()
            })
        }
        .padding(horizontal = 12.dp, vertical = 8.dp)) {
        Column {
            Text(message.content, color = if (isMine) Color.White else MaterialTheme.colorScheme.onSurface)
            if (message.editedAt != null) {
                Text("edited", fontSize = 9.sp, color = if (isMine) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }
            Row(modifier = Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                Text(message.sentAt.takeLast(5), fontSize = 10.sp, color = if (isMine) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant)
                if (isMine) {
                    Spacer(Modifier.width(2.dp))
                    Icon(if (message.isRead) Icons.Filled.DoneAll else Icons.Filled.Done, "Read",
                        tint = if (message.isRead) Color.Cyan else Color.White.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}

@Composable
fun ImageMessageBubble(message: Message, isMine: Boolean, onLongPress: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Box(modifier = Modifier
        .background(if (isMine) SddPink else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
        .pointerInput(Unit) {
            detectTapGestures(onLongPress = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress); onLongPress()
            })
        }
        .padding(4.dp)) {
        Column {
            AsyncImage(model = message.imageUrl, contentDescription = "Image", modifier = Modifier.size(200.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
            Row(Modifier.padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                Text(message.sentAt.takeLast(5), fontSize = 10.sp, color = if (isMine) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant)
                if (isMine) { Spacer(Modifier.width(2.dp)); Icon(if (message.isRead) Icons.Filled.DoneAll else Icons.Filled.Done, "Read", tint = if (message.isRead) Color.Cyan else Color.White.copy(alpha = 0.7f), modifier = Modifier.size(12.dp)) }
            }
        }
    }
}

@Composable
fun LocationMessageBubble(message: Message, isMine: Boolean, onLongPress: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val lat = message.latitude ?: 28.6139; val lng = message.longitude ?: 77.2090
    Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.width(240.dp).pointerInput(Unit) {
        detectTapGestures(onLongPress = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress); onLongPress()
        })
    }) {
        Column {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, "Location", tint = SddPink)
                Text("Location", fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Text(message.sentAt.takeLast(5), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(0.dp))) {
                GoogleMap(modifier = Modifier.fillMaxSize(),
                    cameraPositionState = rememberCameraPositionState { position = com.google.android.gms.maps.model.CameraPosition.fromLatLngZoom(LatLng(lat, lng), 14f) },
                    uiSettings = MapUiSettings(zoomControlsEnabled = false, scrollGesturesEnabled = false, zoomGesturesEnabled = false)) {
                    Marker(state = MarkerState(position = LatLng(lat, lng)))
                }
            }
            Column(Modifier.padding(8.dp)) {
                Text(message.locationAddress ?: "Location", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("Open in Maps", color = SddPink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(Modifier.padding(start = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                repeat(3) { i ->
                    val infiniteTransition = rememberInfiniteTransition(label = "typing$i")
                    val alpha by infiniteTransition.animateFloat(initialValue = 0.3f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(animation = tween(500, delayMillis = i * 150), repeatMode = RepeatMode.Reverse), label = "alpha$i")
                    Box(Modifier.size(6.dp).clip(CircleShape).background(SddPink.copy(alpha = alpha)))
                }
            }
        }
    }
}

@Composable
fun AttachmentOption(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick).padding(8.dp)) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(SddPink.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = SddPink, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ChatInputBar(messageText: String, isEditing: Boolean, onMessageChanged: (String) -> Unit, onSend: () -> Unit, onAttach: () -> Unit) {
    Surface(shadowElevation = 8.dp) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onAttach) {
                Icon(if (isEditing) Icons.Filled.Edit else Icons.Filled.Add, "Attach", tint = SddPink)
            }
            OutlinedTextField(
                value = messageText, onValueChange = onMessageChanged,
                modifier = Modifier.weight(1f),
                placeholder = { Text(if (isEditing) "Edit message..." else "Type a message...") },
                singleLine = false, maxLines = 4,
                shape = RoundedCornerShape(25.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SddPink)
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onSend,
                modifier = Modifier.size(48.dp).clip(CircleShape).background(if (messageText.isBlank() && !isEditing) SddPink.copy(alpha = 0.4f) else SddPink),
                enabled = messageText.isNotBlank() || isEditing
            ) {
                Icon(if (isEditing) Icons.Filled.Check else Icons.Filled.Send, "Send", tint = Color.White)
            }
        }
    }
}
