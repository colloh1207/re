package com.sdd.marketplace.feature.profile.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sdd.marketplace.core.ui.theme.SddPink
import com.sdd.marketplace.feature.profile.viewmodel.ProfileEvent
import com.sdd.marketplace.feature.profile.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(navController: NavController, viewModel: ProfileViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val privacy = uiState.user?.privacySettings
    val snackbarHostState = remember { SnackbarHostState() }

    var showLocation by remember(privacy) { mutableStateOf(privacy?.showLocation ?: true) }
    var showBio by remember(privacy) { mutableStateOf(privacy?.showBio ?: true) }
    var showPhone by remember(privacy) { mutableStateOf(privacy?.showPhone ?: false) }
    var countryFilter by remember(privacy) { mutableStateOf(privacy?.countryFilter ?: "") }
    var allowMessages by remember(privacy) { mutableStateOf(privacy?.allowMessagesFrom ?: "everyone") }
    var hasChanges by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.ShowMessage -> {
                    hasChanges = false
                    snackbarHostState.showSnackbar(event.message)
                }
                is ProfileEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                else -> {}
            }
        }
    }

    fun markChanged() { hasChanges = true }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } },
                title = { Text("Privacy Settings", fontWeight = FontWeight.Bold) },
                actions = {
                    if (hasChanges) {
                        Button(
                            onClick = {
                                viewModel.updateProfile(mapOf(
                                    "privacy_settings" to mapOf(
                                        "show_location" to showLocation,
                                        "show_bio" to showBio,
                                        "show_phone" to showPhone,
                                        "country_filter" to countryFilter.ifBlank { null },
                                        "allow_messages_from" to allowMessages
                                    )
                                ))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SddPink),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = androidx.compose.ui.graphics.Color.White, strokeWidth = 2.dp)
                            } else {
                                Text("Save", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            PrivacySectionHeader("Profile Visibility")
            PrivacyToggleRow(
                icon = Icons.Outlined.LocationOn,
                title = "Show Location",
                subtitle = "Let buyers see your city/region on your profile",
                checked = showLocation,
                onCheckedChange = { showLocation = it; markChanged() }
            )
            PrivacyToggleRow(
                icon = Icons.Outlined.Info,
                title = "Show Bio",
                subtitle = "Display your bio on your public profile",
                checked = showBio,
                onCheckedChange = { showBio = it; markChanged() }
            )
            PrivacyToggleRow(
                icon = Icons.Outlined.Phone,
                title = "Show Phone Number",
                subtitle = "Allow verified buyers to see your phone number",
                checked = showPhone,
                onCheckedChange = { showPhone = it; markChanged() }
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            PrivacySectionHeader("Messaging")

            val messageOptions = listOf(
                "everyone" to "Everyone",
                "followers" to "Followers only",
                "verified" to "Verified users only"
            )
            messageOptions.forEach { (value, label) ->
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { allowMessages = value; markChanged() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(label, fontWeight = FontWeight.Medium)
                    }
                    RadioButton(
                        selected = allowMessages == value,
                        onClick = { allowMessages = value; markChanged() },
                        colors = RadioButtonDefaults.colors(selectedColor = SddPink)
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            PrivacySectionHeader("Product Discovery")

            Padding16 {
                Text("Country Filter", fontWeight = FontWeight.Medium)
                Text(
                    "Only show your products to buyers in this country. Leave blank to show to everyone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = countryFilter,
                    onValueChange = { countryFilter = it; markChanged() },
                    label = { Text("Country code (e.g. KE, IN, US, NG)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SddPink)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "When set, buyers outside this country won't see your products in their feed.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            PrivacySectionHeader("Location Data")
            Padding16 {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Security, "Privacy", tint = SddPink)
                            Spacer(Modifier.width(8.dp))
                            Text("How we use your location", fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Your precise location is never shared with other users. We only share your city/region when enabled above.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun PrivacySectionHeader(title: String) {
    Text(title, Modifier.padding(horizontal = 16.dp, vertical = 8.dp), fontWeight = FontWeight.Bold, color = SddPink)
}

@Composable
fun Padding16(content: @Composable () -> Unit) {
    Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) { content() }
}

@Composable
fun PrivacyToggleRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, title, tint = SddPink, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = SddPink, checkedTrackColor = SddPink.copy(alpha = 0.4f))
        )
    }
}
