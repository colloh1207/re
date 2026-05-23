package com.sdd.marketplace.feature.auth.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sdd.marketplace.core.ui.components.SddButton
import com.sdd.marketplace.core.ui.theme.SddLightPink
import com.sdd.marketplace.core.ui.theme.SddPink
import com.sdd.marketplace.core.util.ErrorHandler
import com.sdd.marketplace.feature.auth.viewmodel.AuthEvent
import com.sdd.marketplace.feature.auth.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun OtpVerifyScreen(
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var otp by remember { mutableStateOf("") }
    var resendTimer by remember { mutableIntStateOf(60) }
    var resendCount by remember { mutableIntStateOf(0) }
    var timerRunning by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.events.collect {
            when (it) {
                is AuthEvent.NavigateToHome -> onNavigateToHome()
                else -> {}
            }
        }
    }

    LaunchedEffect(timerRunning) {
        if (timerRunning) {
            while (resendTimer > 0) {
                delay(1000L)
                resendTimer--
            }
            timerRunning = false
        }
    }

    val emailDisplay = uiState.email.ifBlank { "your email" }
    val title = if (uiState.otpIsForRecovery) "Reset Password" else "Verify Email"

    Column(
        modifier = Modifier.fillMaxSize().background(SddLightPink).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Enter the 6-digit code sent to", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(emailDisplay, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            "Check your inbox and spam folder.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        OtpInputField(otp = otp, onOtpChanged = { if (it.length <= 6) otp = it })

        Spacer(Modifier.height(24.dp))
        if (timerRunning || resendTimer > 0) {
            val minutes = resendTimer / 60
            val seconds = resendTimer % 60
            val timerText = if (minutes > 0)
                "${minutes}:${seconds.toString().padStart(2, '0')}"
            else
                "00:${seconds.toString().padStart(2, '0')}"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Resend code in ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(timerText, color = SddPink, fontWeight = FontWeight.Bold)
            }
            if (resendCount >= 3) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Too many attempts. Please wait before trying again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            TextButton(onClick = {
                resendCount++
                otp = ""
                resendTimer = if (resendCount >= 3) 300 else 60
                timerRunning = true
                viewModel.resendEmailOtp()
            }) {
                Text("Resend Code", color = SddPink, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(32.dp))
        SddButton(
            text = if (uiState.otpIsForRecovery) "Verify & Continue" else "Verify & Sign In",
            onClick = { viewModel.verifyOtp(otp) },
            isLoading = uiState.isLoading,
            enabled = otp.length == 6
        )

        uiState.error?.let { rawError ->
            val friendlyMessage = ErrorHandler.friendlyMessage(rawError)
            Card(
                modifier = Modifier.padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.ErrorOutline, null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        friendlyMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        if (!uiState.otpIsForRecovery) {
            TextButton(onClick = { viewModel.continueAsGuest() }) {
                Text("Continue as Guest", color = SddPink)
            }
        }
    }
}

@Composable
fun OtpInputField(otp: String, onOtpChanged: (String) -> Unit) {
    BasicTextField(
        value = otp, onValueChange = onOtpChanged,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(6) { index ->
                    val char = otp.getOrNull(index)
                    Box(
                        modifier = Modifier.size(48.dp).border(
                            2.dp,
                            if (index == otp.length) SddPink else Color.Gray.copy(alpha = 0.4f),
                            RoundedCornerShape(8.dp)
                        ).background(Color.White, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            char?.toString() ?: "",
                            fontSize = 22.sp, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    )
}
