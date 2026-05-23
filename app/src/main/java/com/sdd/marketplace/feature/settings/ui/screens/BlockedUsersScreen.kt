package com.sdd.marketplace.feature.settings.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.sdd.marketplace.core.ui.theme.SddPink
import com.sdd.marketplace.domain.model.Block
import com.sdd.marketplace.feature.settings.viewmodel.BlockedUsersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersScreen(
    navController: NavController,
    viewModel: BlockedUsersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                title = { Text("Blocked Users", fontWeight = FontWeight.Bold) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = SddPink
                    )
                }
                uiState.blockedUsers.isEmpty() -> {
                    EmptyBlockedState(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            InfoBanner()
                            Spacer(Modifier.height(4.dp))
                        }
                        items(uiState.blockedUsers, key = { it.id }) { block ->
                            BlockedUserCard(
                                block = block,
                                isUnblocking = uiState.unblockingIds.contains(block.blockedId),
                                onUnblock = { viewModel.unblockUser(block.blockedId) }
                            )
                        }
                    }
                }
            }
            uiState.error?.let { err ->
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) { Text("Dismiss") }
                    }
                ) { Text(err) }
            }
        }
    }
}

@Composable
private fun InfoBanner() {
    Surface(
        color = SddPink.copy(alpha = 0.08f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Info, "Info", tint = SddPink, modifier = Modifier.size(18.dp))
            Text(
                "Blocked users cannot message you or see your listings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BlockedUserCard(
    block: Block,
    isUnblocking: Boolean,
    onUnblock: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }
    val user = block.blockedUser

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            icon = { Icon(Icons.Filled.PersonAdd, "Unblock", tint = SddPink) },
            title = { Text("Unblock ${user?.fullName ?: "User"}?") },
            text = { Text("They will be able to message you and see your listings again.") },
            confirmButton = {
                Button(
                    onClick = { onUnblock(); showConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = SddPink)
                ) { Text("Unblock") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(48.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (user?.avatarUrl != null) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Filled.Person, "Avatar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    user?.fullName ?: "Unknown User",
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    "@${user?.username ?: block.blockedId.take(8)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                block.reason?.let { reason ->
                    if (reason.isNotBlank()) {
                        Text(
                            "Reason: $reason",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
            if (isUnblocking) {
                CircularProgressIndicator(Modifier.size(20.dp), color = SddPink, strokeWidth = 2.dp)
            } else {
                OutlinedButton(
                    onClick = { showConfirm = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SddPink),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SddPink),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.PersonAdd, "Unblock", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Unblock", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun EmptyBlockedState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Outlined.PersonOff, "No blocked users",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No blocked users",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Users you block will appear here. You can unblock them at any time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
