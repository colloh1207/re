package com.sdd.marketplace.feature.telegram.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sdd.marketplace.core.ui.components.SddButton
import com.sdd.marketplace.core.ui.components.SddTextField
import com.sdd.marketplace.core.ui.theme.SddPink
import com.sdd.marketplace.feature.telegram.viewmodel.TelegramBotViewModel
import com.sdd.marketplace.feature.telegram.viewmodel.TelegramConnectionState

private val TelegramBlue = Color(0xFF0088CC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelegramBotScreen(
    navController: NavController,
    viewModel: TelegramBotViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var token by remember { mutableStateOf("") }
    var showTokenInput by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                title = { Text("Telegram Bot", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            Box(
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(TelegramBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Send, "Telegram", tint = Color.White, modifier = Modifier.size(40.dp))
            }

            Spacer(Modifier.height(16.dp))
            Text("Telegram Integration", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Connect your Telegram bot to manage your marketplace listings directly from Telegram.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(24.dp))

            when (uiState.connectionState) {
                TelegramConnectionState.CONNECTED -> ConnectedState(
                    botUsername = uiState.botUsername,
                    onDisconnect = { viewModel.disconnectBot() },
                    isLoading = uiState.isLoading
                )
                TelegramConnectionState.VALIDATING -> ValidatingState()
                else -> DisconnectedState(
                    token = token,
                    onTokenChange = { token = it },
                    showInput = showTokenInput,
                    onShowInput = { showTokenInput = true },
                    onConnect = { viewModel.connectBot(token) },
                    isLoading = uiState.isLoading,
                    error = uiState.error
                )
            }

            Spacer(Modifier.height(24.dp))
            HowItWorksSection()
            Spacer(Modifier.height(24.dp))
            BotCapabilitiesSection()
            Spacer(Modifier.height(24.dp))
            SetupInstructionsSection()
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ConnectedState(botUsername: String?, onDisconnect: () -> Unit, isLoading: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, "Connected", tint = Color(0xFF4CAF50), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Bot Connected", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50), fontSize = 18.sp)
            }
            if (!botUsername.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("@$botUsername", fontWeight = FontWeight.SemiBold, color = TelegramBlue)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Your Telegram bot is active. Send /help to your bot to see available commands.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onDisconnect,
                enabled = !isLoading,
                colors = ButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.error,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = MaterialTheme.colorScheme.error.copy(alpha = 0.38f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.LinkOff, "Disconnect", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Disconnect Bot")
                }
            }
        }
    }
}

@Composable
private fun ValidatingState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TelegramBlue.copy(alpha = 0.08f))
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = TelegramBlue)
            Spacer(Modifier.height(12.dp))
            Text("Validating bot token...", fontWeight = FontWeight.Medium)
            Text("Please wait while we verify your bot", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DisconnectedState(
    token: String, onTokenChange: (String) -> Unit,
    showInput: Boolean, onShowInput: () -> Unit,
    onConnect: () -> Unit, isLoading: Boolean, error: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LinkOff, "Disconnected", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("No Bot Connected", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))

            if (!showInput) {
                SddButton(
                    text = "Connect Telegram Bot",
                    onClick = onShowInput,
                    containerColor = TelegramBlue
                )
            } else {
                SddTextField(
                    value = token,
                    onValueChange = onTokenChange,
                    label = "Bot Token (from @BotFather)",
                    leadingIcon = { Icon(Icons.Outlined.Key, "Token") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password),
                    isPassword = false
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Format: 1234567890:ABCdefGHIjklMNOpqrsTUVwxyz",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                SddButton(
                    text = "Validate & Connect",
                    onClick = onConnect,
                    isLoading = isLoading,
                    enabled = token.contains(":") && token.length > 20,
                    containerColor = TelegramBlue
                )
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun HowItWorksSection() {
    Column(Modifier.fillMaxWidth()) {
        Text("How It Works", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))
        listOf(
            Triple(Icons.Filled.SmartToy, "Create a Bot", "Open Telegram and message @BotFather. Use /newbot to create your bot and get a token."),
            Triple(Icons.Filled.VpnKey, "Connect Token", "Paste your bot token here. We validate and encrypt it before storing securely."),
            Triple(Icons.Filled.Send, "Start Using", "Message your bot to post products, edit listings, and manage your shop.")
        ).forEachIndexed { index, (icon, title, desc) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(TelegramBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = TelegramBlue, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (index < 2) {
                Box(Modifier.padding(start = 18.dp).size(width = 2.dp, height = 8.dp).background(TelegramBlue.copy(alpha = 0.3f)))
            }
        }
    }
}

@Composable
private fun BotCapabilitiesSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TelegramBlue.copy(alpha = 0.06f)),
        border = BorderStroke(1.dp, TelegramBlue.copy(alpha = 0.2f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Bot Commands", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(12.dp))
            listOf(
                "/post" to "Post a new product listing",
                "/edit [id]" to "Edit an existing listing",
                "/delete [id]" to "Delete a listing",
                "/sold [id]" to "Mark a product as sold",
                "/mylistings" to "View all your active listings",
                "/help" to "Show all available commands"
            ).forEach { (cmd, desc) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = TelegramBlue.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                        Text(cmd, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), color = TelegramBlue, fontWeight = FontWeight.Medium, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SetupInstructionsSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Security, "Security", tint = Color(0xFFFF8F00), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Security & Privacy", fontWeight = FontWeight.Bold, color = Color(0xFFFF8F00))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Your bot token is encrypted using AES-256 before being stored. It is only accessible via our secure Edge Function and is never exposed in the app or API responses. KYC verification is required to use this feature.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}
