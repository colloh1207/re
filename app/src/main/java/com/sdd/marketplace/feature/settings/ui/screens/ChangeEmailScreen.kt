package com.sdd.marketplace.feature.settings.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sdd.marketplace.core.ui.components.*
import com.sdd.marketplace.core.ui.theme.SddPink
import com.sdd.marketplace.feature.settings.viewmodel.SettingsEvent
import com.sdd.marketplace.feature.settings.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeEmailScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var newEmail by remember { mutableStateOf("") }
    var emailSent by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ShowMessage -> {
                    emailSent = true
                    snackbarHostState.showSnackbar(event.message)
                }
                is SettingsEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } },
                title = { Text("Change Email", fontWeight = FontWeight.Bold) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            if (!emailSent) {
                Icon(Icons.Filled.Email, "Email", tint = SddPink, modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(16.dp))
                Text("Update Email Address", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                uiState.currentUserEmail?.let { current ->
                    Text("Current email: $current", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    "Enter your new email address. We'll send a verification link to confirm the change.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
                SddTextField(
                    newEmail, { newEmail = it }, "New Email Address",
                    leadingIcon = { Icon(Icons.Filled.Email, "Email") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                    )
                )
                Spacer(Modifier.height(16.dp))
                SddButton(
                    "Send Verification Link",
                    onClick = { viewModel.changeEmail(newEmail) },
                    isLoading = uiState.isLoading,
                    enabled = android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches() &&
                              newEmail != uiState.currentUserEmail
                )
            } else {
                Icon(Icons.Filled.MarkEmailRead, "Email sent", tint = SddPink, modifier = Modifier.size(64.dp).align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(16.dp))
                Text("Verification Email Sent!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(Modifier.height(8.dp))
                Text(
                    "We've sent a verification link to:\n$newEmail\n\nClick the link in your email to confirm your new address. Check your spam folder if you don't see it.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = { viewModel.changeEmail(newEmail) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Resend Email", color = SddPink)
                }
                Spacer(Modifier.height(8.dp))
                SddOutlineButton("Back to Settings", onClick = { navController.popBackStack() })
            }
            uiState.error?.let { err ->
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Error, "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
