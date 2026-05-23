package com.sdd.marketplace.feature.settings.ui.screens

import androidx.compose.animation.*
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
fun ChangePasswordScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                    navController.popBackStack()
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
                title = { Text("Change Password", fontWeight = FontWeight.Bold) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
            AnimatedContent(targetState = uiState.otpSent, label = "step") { otpSent ->
                if (!otpSent) {
                    Column {
                        Icon(Icons.Filled.Lock, "Password", tint = SddPink, modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Reset Your Password",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        uiState.currentUserEmail?.let { email ->
                            Text(
                                "We'll send a 6-digit verification code to:\n$email",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } ?: Text(
                            "We'll send a 6-digit verification code to your registered email address.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(32.dp))
                        SddButton(
                            "Send Verification Code",
                            onClick = { viewModel.sendPasswordResetOtp() },
                            isLoading = uiState.isLoading
                        )
                    }
                } else {
                    Column {
                        Icon(Icons.Filled.MarkEmailRead, "Email", tint = SddPink, modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Enter Verification Code",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Enter the 6-digit code sent to ${uiState.currentUserEmail ?: "your email"}, then set your new password.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(24.dp))
                        SddTextField(
                            otp, { otp = it.take(6) }, "6-Digit Code",
                            leadingIcon = { Icon(Icons.Filled.Security, "OTP") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                            )
                        )
                        Spacer(Modifier.height(12.dp))
                        SddTextField(
                            newPassword, { newPassword = it }, "New Password",
                            isPassword = true,
                            leadingIcon = { Icon(Icons.Filled.Lock, "Password") }
                        )
                        Spacer(Modifier.height(12.dp))
                        SddTextField(
                            confirmPassword, { confirmPassword = it }, "Confirm New Password",
                            isPassword = true,
                            leadingIcon = { Icon(Icons.Filled.Lock, "Confirm") }
                        )
                        if (newPassword.isNotEmpty() && confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
                            Spacer(Modifier.height(4.dp))
                            Text("Passwords do not match", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(16.dp))
                        SddButton(
                            "Change Password",
                            onClick = { viewModel.changePassword(otp, newPassword) },
                            isLoading = uiState.isLoading,
                            enabled = otp.length == 6 && newPassword.length >= 8 && newPassword == confirmPassword
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.sendPasswordResetOtp() }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Resend Code", color = SddPink)
                        }
                    }
                }
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
