package com.sdd.marketplace.feature.auth.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sdd.marketplace.core.ui.components.*
import com.sdd.marketplace.core.ui.theme.SddLightPink
import com.sdd.marketplace.core.ui.theme.SddPink
import com.sdd.marketplace.feature.auth.viewmodel.AuthEvent
import com.sdd.marketplace.feature.auth.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToForgot: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToOtp: (String) -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showTermsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthEvent.NavigateToHome -> onNavigateToHome()
                is AuthEvent.NavigateToOtp -> onNavigateToOtp(uiState.email)
                is AuthEvent.ShowError -> {}
                is AuthEvent.ShowSuccess -> {}
            }
        }
    }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    if (showTermsDialog) {
        TermsConditionsDialog(onDismiss = { showTermsDialog = false })
    }

    Box(
        modifier = Modifier.fillMaxSize().background(SddLightPink),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
            SddLogo()
            Spacer(Modifier.height(24.dp))
            Text(
                "Welcome Back",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Login to continue your shopping journey",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(24.dp)) {
                    SddTextField(
                        value = email, onValueChange = { email = it },
                        label = "Email Address",
                        leadingIcon = { Icon(Icons.Outlined.Email, "Email") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    SddTextField(
                        value = password, onValueChange = { password = it },
                        label = "Password", isPassword = true,
                        leadingIcon = { Icon(Icons.Outlined.Lock, "Password") }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Forgot Password?", color = SddPink, fontWeight = FontWeight.Medium,
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToForgot),
                        textAlign = TextAlign.End
                    )
                    Spacer(Modifier.height(20.dp))
                    SddButton(
                        text = "Login",
                        onClick = { viewModel.signInWithEmail(email, password) },
                        isLoading = uiState.isLoading,
                        enabled = email.isNotBlank() && password.isNotBlank()
                    )
                    uiState.error?.let { err ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            err,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Row {
                Text("Don't have an account? ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Register", color = SddPink, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onNavigateToRegister)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Continue as Guest",
                color = SddPink, fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { viewModel.continueAsGuest() }
            )
            Spacer(Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "By continuing you agree to our ",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Terms & Conditions",
                    color = SddPink,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable { showTermsDialog = true }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
