package com.sdd.marketplace.feature.settings.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sdd.marketplace.core.navigation.Screen
import com.sdd.marketplace.core.ui.theme.SddPink
import com.sdd.marketplace.feature.settings.viewmodel.SettingsEvent
import com.sdd.marketplace.feature.settings.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showNotificationsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.NavigateToLogin ->
                    navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                is SettingsEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is SettingsEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            icon = { Icon(Icons.Filled.Notifications, "Notifications", tint = SddPink) },
            title = { Text("Notification Settings", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("All Notifications", fontWeight = FontWeight.Medium)
                            Text("Enable or disable all notifications", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = uiState.notificationsEnabled,
                            onCheckedChange = { viewModel.toggleNotifications(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = SddPink, checkedTrackColor = SddPink.copy(alpha = 0.4f))
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Push Notifications", fontWeight = FontWeight.Medium)
                            Text("Messages, orders, offers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = uiState.pushEnabled && uiState.notificationsEnabled,
                            onCheckedChange = { viewModel.togglePushNotifications(it) },
                            enabled = uiState.notificationsEnabled,
                            colors = SwitchDefaults.colors(checkedThumbColor = SddPink, checkedTrackColor = SddPink.copy(alpha = 0.4f))
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Email Notifications", fontWeight = FontWeight.Medium)
                            Text("Deals, updates, newsletters", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = uiState.emailNotificationsEnabled && uiState.notificationsEnabled,
                            onCheckedChange = { viewModel.toggleEmailNotifications(it) },
                            enabled = uiState.notificationsEnabled,
                            colors = SwitchDefaults.colors(checkedThumbColor = SddPink, checkedTrackColor = SddPink.copy(alpha = 0.4f))
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Offer Alerts", fontWeight = FontWeight.Medium)
                            Text("Get notified when someone makes an offer", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = uiState.offersNotificationsEnabled && uiState.notificationsEnabled,
                            onCheckedChange = { viewModel.toggleOffersNotifications(it) },
                            enabled = uiState.notificationsEnabled,
                            colors = SwitchDefaults.colors(checkedThumbColor = SddPink, checkedTrackColor = SddPink.copy(alpha = 0.4f))
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Reviews & Ratings", fontWeight = FontWeight.Medium)
                            Text("Get notified when you receive a review", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = uiState.ratingsNotificationsEnabled && uiState.notificationsEnabled,
                            onCheckedChange = { viewModel.toggleRatingsNotifications(it) },
                            enabled = uiState.notificationsEnabled,
                            colors = SwitchDefaults.colors(checkedThumbColor = SddPink, checkedTrackColor = SddPink.copy(alpha = 0.4f))
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) { Text("Done", color = SddPink) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } },
                title = { Text("Settings", fontWeight = FontWeight.Bold) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {
            SettingsSection("Account") {
                SettingsItem(Icons.Outlined.Lock, "Change Password", "Update your password securely") {
                    navController.navigate(Screen.ChangePassword.route)
                }
                SettingsItem(Icons.Outlined.Email, "Change Email", "Update your email address") {
                    navController.navigate(Screen.ChangeEmail.route)
                }
                SettingsItem(Icons.Outlined.SwitchAccount, "Switch Account", "Manage multiple accounts") {
                    navController.navigate(Screen.SwitchAccount.route)
                }
                SettingsItem(Icons.Outlined.Block, "Blocked Users", "Manage users you've blocked") {
                    navController.navigate(Screen.BlockedUsers.route)
                }
                SettingsItem(Icons.Outlined.VerifiedUser, "KYC Verification", "Verify your identity to become a trusted seller") {
                    navController.navigate(Screen.KycVerification.route)
                }
            }
            SettingsSection("Preferences") {
                SettingsItem(Icons.Outlined.Lock, "Privacy Settings", "Control who sees your location, bio and country") {
                    navController.navigate(Screen.PrivacySettings.route)
                }
                SettingsItem(Icons.Outlined.Language, "Language", "Change app language") {
                    navController.navigate(Screen.ChangeLanguage.route)
                }
                SettingsItem(
                    Icons.Outlined.Notifications,
                    "Notifications",
                    if (uiState.notificationsEnabled) "Notifications are on" else "Notifications are off"
                ) { showNotificationsDialog = true }
                SettingsItem(Icons.Outlined.LocalOffer, "My Coupons", "View your earned coupon codes") {
                    navController.navigate(Screen.Coupons.route)
                }
            }
            SettingsSection("Legal") {
                SettingsItem(Icons.Outlined.Gavel, "Terms & Conditions", "Read our terms of service") {
                    navController.navigate(Screen.TermsConditions.route)
                }
                SettingsItem(Icons.Outlined.PrivacyTip, "Privacy Policy", "How we handle your data") {
                    navController.navigate(Screen.PrivacyPolicy.route)
                }
                SettingsItem(Icons.Outlined.Store, "Seller Terms", "Terms for sellers on our platform") {
                    navController.navigate(Screen.SellerTerms.route)
                }
                SettingsItem(Icons.Outlined.ShoppingCart, "Buyer Terms", "Terms for buyers on our platform") {
                    navController.navigate(Screen.BuyerTerms.route)
                }
            }
            SettingsSection("Support") {
                SettingsItem(Icons.Outlined.Help, "Help & Support", "Get help with your account or orders") {
                    navController.navigate(Screen.HelpSupport.route)
                }
                SettingsItem(Icons.Outlined.BugReport, "Report a Bug", "Help us improve the app") {
                    navController.navigate(Screen.ReportBug.route)
                }
                SettingsItem(Icons.Outlined.Star, "Rate Us", "Share your experience") {
                    navController.navigate(Screen.RateApp.route)
                }
            }
            SettingsSection("Account Actions") {
                SettingsItem(
                    Icons.Outlined.Logout, "Sign Out", "Sign out of your account",
                    tint = MaterialTheme.colorScheme.error
                ) {
                    navController.navigate(Screen.ConfirmLogout.route)
                }
                SettingsItem(
                    Icons.Outlined.DeleteForever, "Delete Account", "Permanently delete your account",
                    tint = MaterialTheme.colorScheme.error
                ) {
                    navController.navigate(Screen.DeleteAccount.route)
                }
            }
            Spacer(Modifier.height(32.dp))
            Text(
                "Sdd Marketplace v1.0.0", fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            color = SddPink, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), shape = RoundedCornerShape(12.dp)) {
            Column { content() }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector, title: String, subtitle: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).background(
                if (tint == MaterialTheme.colorScheme.error) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                else SddPink.copy(alpha = 0.1f),
                RoundedCornerShape(10.dp)
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, title, tint = if (tint == MaterialTheme.colorScheme.error) tint else SddPink, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, color = tint)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Filled.ChevronRight, "Navigate", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
