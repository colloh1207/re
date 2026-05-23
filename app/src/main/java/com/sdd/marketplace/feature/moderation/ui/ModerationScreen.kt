package com.sdd.marketplace.feature.moderation.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sdd.marketplace.core.ui.theme.SddPink
import com.sdd.marketplace.domain.model.*
import com.sdd.marketplace.feature.moderation.viewmodel.ModerationEvent
import com.sdd.marketplace.feature.moderation.viewmodel.ModerationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModerationScreen(navController: NavController, viewModel: ModerationViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showWarnDialog by remember { mutableStateOf(false) }
    var showSuspendDialog by remember { mutableStateOf(false) }
    var showBanDialog by remember { mutableStateOf(false) }
    var warnReason by remember { mutableStateOf("") }
    var suspendReason by remember { mutableStateOf("") }
    var banReason by remember { mutableStateOf("") }
    var selectedSuspensionType by remember { mutableStateOf(SuspensionType.TEMPORARY_2_DAYS) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ModerationEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                else -> {}
            }
        }
    }

    if (showWarnDialog) {
        AlertDialog(
            onDismissRequest = { showWarnDialog = false },
            icon = { Icon(Icons.Filled.Warning, "Warn", tint = Color(0xFFFF9800)) },
            title = { Text("Issue Warning") },
            text = {
                Column {
                    Text("This will be warning #${uiState.warnings.size + 1}. After 2 warnings, the user is auto-suspended.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(warnReason, { warnReason = it }, label = { Text("Reason") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.issueWarning(warnReason); showWarnDialog = false; warnReason = "" },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))) { Text("Issue Warning") }
            },
            dismissButton = { TextButton(onClick = { showWarnDialog = false }) { Text("Cancel") } }
        )
    }

    if (showSuspendDialog) {
        AlertDialog(
            onDismissRequest = { showSuspendDialog = false },
            icon = { Icon(Icons.Filled.Block, "Suspend", tint = MaterialTheme.colorScheme.error) },
            title = { Text("Suspend User") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(suspendReason, { suspendReason = it }, label = { Text("Reason") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                    Text("Duration:", fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(SuspensionType.TEMPORARY_2_DAYS to "2 Days", SuspensionType.TEMPORARY_3_DAYS to "3 Days").forEach { (type, label) ->
                            FilterChip(selected = selectedSuspensionType == type, onClick = { selectedSuspensionType = type }, label = { Text(label) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.suspendUser(selectedSuspensionType, suspendReason); showSuspendDialog = false; suspendReason = "" },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Suspend") }
            },
            dismissButton = { TextButton(onClick = { showSuspendDialog = false }) { Text("Cancel") } }
        )
    }

    if (showBanDialog) {
        AlertDialog(
            onDismissRequest = { showBanDialog = false },
            icon = { Icon(Icons.Filled.GppBad, "Ban", tint = MaterialTheme.colorScheme.error) },
            title = { Text("Permanent Ban") },
            text = {
                Column {
                    Text("This user will be permanently banned. This cannot be easily reversed.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(banReason, { banReason = it }, label = { Text("Reason") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.banUser(banReason); showBanDialog = false; banReason = "" },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Permanently Ban") }
            },
            dismissButton = { TextButton(onClick = { showBanDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } },
                title = { Text("User Moderation", fontWeight = FontWeight.Bold) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Current Status
            uiState.suspension?.let { suspension ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Current Suspension", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("Type: ${suspension.type.name}", color = MaterialTheme.colorScheme.onErrorContainer)
                        Text("Reason: ${suspension.reason}", color = MaterialTheme.colorScheme.onErrorContainer)
                        suspension.endsAt?.let { Text("Ends: $it", color = MaterialTheme.colorScheme.onErrorContainer) }
                        if (suspension.appealStatus == AppealStatus.PENDING) {
                            Spacer(Modifier.height(8.dp))
                            Text("Appeal: PENDING REVIEW", fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                            suspension.appealNote?.let { Text("Note: $it", style = MaterialTheme.typography.bodySmall) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { viewModel.approveAppeal(suspension.id) }, modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("Approve") }
                                OutlinedButton(onClick = { viewModel.rejectAppeal(suspension.id) }, modifier = Modifier.weight(1f)) { Text("Reject") }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.liftSuspension() }, modifier = Modifier.fillMaxWidth()) { Text("Lift Suspension") }
                    }
                }
            }

            // Warnings History
            Card {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, "Warnings", tint = Color(0xFFFF9800))
                        Spacer(Modifier.width(8.dp))
                        Text("Warnings (${uiState.warnings.size}/2)", fontWeight = FontWeight.Bold)
                    }
                    uiState.warnings.forEach { warning ->
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(50), color = Color(0xFFFF9800).copy(alpha = 0.2f)) {
                                Text("#${warning.warningNumber}", Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(warning.reason, style = MaterialTheme.typography.bodySmall)
                                Text(warning.issuedAt.take(10), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    if (uiState.warnings.isEmpty()) Text("No warnings issued yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Actions
            Text("Actions", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            ModerationActionCard(icon = Icons.Filled.Warning, title = "Issue Warning",
                subtitle = "Send an official warning (${uiState.warnings.size}/2 issued — auto-suspend at 2)",
                color = Color(0xFFFF9800)) { showWarnDialog = true }
            ModerationActionCard(icon = Icons.Filled.Block, title = "Suspend Account",
                subtitle = "Temporarily suspend for 2 or 3 days. User can appeal.",
                color = MaterialTheme.colorScheme.error) { showSuspendDialog = true }
            ModerationActionCard(icon = Icons.Filled.GppBad, title = "Permanent Ban",
                subtitle = "Permanently ban this account from the platform.",
                color = MaterialTheme.colorScheme.error) { showBanDialog = true }
        }
    }
}

@Composable
fun ModerationActionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.1f)) {
                    Icon(icon, title, tint = color, modifier = Modifier.padding(10.dp).size(28.dp))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Filled.ChevronRight, "Go", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
