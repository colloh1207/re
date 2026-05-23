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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sdd.marketplace.core.ui.components.*
import com.sdd.marketplace.core.ui.theme.SddLightPink
import com.sdd.marketplace.core.ui.theme.SddPink
import com.sdd.marketplace.feature.auth.viewmodel.AuthEvent
import com.sdd.marketplace.feature.auth.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToOtp: (String) -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }
    var showReferralField by remember { mutableStateOf(false) }
    var agreedToTerms by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AuthEvent.NavigateToHome -> onNavigateToHome()
                is AuthEvent.NavigateToOtp -> onNavigateToOtp(uiState.email)
                else -> {}
            }
        }
    }

    if (showTermsDialog) {
        TermsConditionsDialog(onDismiss = { showTermsDialog = false })
    }

    Box(modifier = Modifier.fillMaxSize().background(SddLightPink)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))
            SddLogo()
            Spacer(Modifier.height(16.dp))
            Text(
                "Create Account",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Join Sdd and start shopping",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(24.dp)) {
                    SddTextField(
                        value = fullName, onValueChange = { fullName = it },
                        label = "Full Name",
                        leadingIcon = { Icon(Icons.Outlined.Person, "Name") }
                    )
                    Spacer(Modifier.height(12.dp))
                    SddTextField(
                        value = email, onValueChange = { email = it },
                        label = "Email Address",
                        leadingIcon = { Icon(Icons.Outlined.Email, "Email") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    Spacer(Modifier.height(12.dp))
                    SddTextField(
                        value = password, onValueChange = { password = it },
                        label = "Password (min. 6 characters)",
                        isPassword = true,
                        leadingIcon = { Icon(Icons.Outlined.Lock, "Password") }
                    )
                    Spacer(Modifier.height(12.dp))

                    if (!showReferralField) {
                        TextButton(
                            onClick = { showReferralField = true },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Outlined.CardGiftcard, "Referral", tint = SddPink, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Have a referral code? (Optional)", color = SddPink, style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        SddTextField(
                            value = referralCode,
                            onValueChange = { referralCode = it.uppercase() },
                            label = "Referral Code (Optional)",
                            leadingIcon = { Icon(Icons.Outlined.CardGiftcard, "Referral", tint = SddPink) }
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Enter a friend's referral code to unlock rewards when you complete your first purchase.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = agreedToTerms,
                            onCheckedChange = { agreedToTerms = it },
                            colors = CheckboxDefaults.colors(checkedColor = SddPink)
                        )
                        Text("I agree to the ")
                        Text(
                            "Terms & Conditions",
                            color = SddPink,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { showTermsDialog = true }
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    SddButton(
                        text = "Create Account",
                        onClick = {
                            viewModel.signUp(fullName, email, password, referralCode.ifBlank { null })
                        },
                        isLoading = uiState.isLoading,
                        enabled = agreedToTerms && fullName.isNotBlank() && email.isNotBlank() && password.isNotBlank()
                    )

                    uiState.error?.let { err ->
                        Spacer(Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.ErrorOutline, "Error",
                                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    err, color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Row {
                Text("Already have an account? ")
                Text(
                    "Sign In", color = SddPink, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onNavigateToLogin)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Continue as Guest",
                color = SddPink, fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { viewModel.continueAsGuest() }
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
