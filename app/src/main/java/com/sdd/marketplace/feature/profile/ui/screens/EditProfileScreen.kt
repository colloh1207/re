package com.sdd.marketplace.feature.profile.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.sdd.marketplace.core.ui.components.*
import com.sdd.marketplace.core.ui.theme.SddPink
import com.sdd.marketplace.feature.profile.viewmodel.EditProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel: EditProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLocationSheet by remember { mutableStateOf(false) }
    var locationQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is com.sdd.marketplace.feature.profile.viewmodel.EditProfileEvent.SaveSuccess -> navController.popBackStack()
                is com.sdd.marketplace.feature.profile.viewmodel.EditProfileEvent.ShowError -> {}
            }
        }
    }

    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.setAvatarUri(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } },
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { viewModel.saveProfile() }, enabled = !uiState.isLoading) {
                        if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = SddPink, strokeWidth = 2.dp)
                        else Text("Save", color = SddPink, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            // Avatar
            Box(Modifier.align(Alignment.CenterHorizontally)) {
                AsyncImage(
                    model = uiState.avatarUri ?: uiState.currentAvatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier.size(100.dp).clip(CircleShape).border(3.dp, SddPink.copy(0.5f), CircleShape),
                    contentScale = ContentScale.Crop
                )
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(SddPink)
                        .align(Alignment.BottomEnd).clickable { avatarLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.CameraAlt, "Change", tint = Color.White, modifier = Modifier.size(18.dp)) }
            }
            Spacer(Modifier.height(8.dp))
            Text("Tap to change profile photo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(24.dp))

            Text("Basic Information", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            SddTextField(value = uiState.fullName, onValueChange = { viewModel.updateFullName(it) }, label = "Full Name *", leadingIcon = { Icon(Icons.Outlined.Person, "Name") })
            Spacer(Modifier.height(12.dp))
            SddTextField(value = uiState.username, onValueChange = { viewModel.updateUsername(it) }, label = "Username", leadingIcon = { Icon(Icons.Outlined.AlternateEmail, "Username") })
            Spacer(Modifier.height(12.dp))
            SddTextField(value = uiState.bio, onValueChange = { viewModel.updateBio(it) }, label = "Bio (max 150 characters)", singleLine = false, leadingIcon = { Icon(Icons.Outlined.Info, "Bio") })
            Spacer(Modifier.height(20.dp))

            Text("Contact Information", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            SddTextField(value = uiState.phone, onValueChange = { viewModel.updatePhone(it) }, label = "Phone Number",
                leadingIcon = { Icon(Icons.Outlined.Phone, "Phone") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.location, onValueChange = { viewModel.updateLocation(it) },
                label = { Text("Location") },
                leadingIcon = { Icon(Icons.Outlined.LocationOn, "Location", tint = SddPink) },
                trailingIcon = { Icon(Icons.Outlined.Search, "Search", tint = SddPink, modifier = Modifier.clickable { showLocationSheet = true }) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SddPink, focusedLabelColor = SddPink)
            )
            Spacer(Modifier.height(12.dp))
            SddTextField(value = uiState.website, onValueChange = { viewModel.updateWebsite(it) }, label = "Website (Optional)",
                leadingIcon = { Icon(Icons.Outlined.Language, "Website") })
            Spacer(Modifier.height(20.dp))

            Text("Seller Information", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            SddTextField(value = uiState.shopName, onValueChange = { viewModel.updateShopName(it) }, label = "Shop Name (Optional)",
                leadingIcon = { Icon(Icons.Outlined.Store, "Shop") })
            Spacer(Modifier.height(12.dp))
            SddTextField(value = uiState.shopDescription, onValueChange = { viewModel.updateShopDescription(it) },
                label = "Shop Description (Optional)", singleLine = false,
                leadingIcon = { Icon(Icons.Outlined.Description, "Description") })
            Spacer(Modifier.height(20.dp))

            Text("Privacy Settings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(12.dp))
            Card(shape = RoundedCornerShape(16.dp)) {
                Column {
                    ListItem(
                        headlineContent = { Text("Show Email Address") },
                        supportingContent = { Text("Other users can see your email") },
                        trailingContent = { Switch(checked = uiState.showEmail, onCheckedChange = { viewModel.toggleShowEmail() }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SddPink)) }
                    )
                    Divider(Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("Show Phone Number") },
                        supportingContent = { Text("Other users can see your phone") },
                        trailingContent = { Switch(checked = uiState.showPhone, onCheckedChange = { viewModel.toggleShowPhone() }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SddPink)) }
                    )
                    Divider(Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text("Show Online Status") },
                        supportingContent = { Text("Others can see when you're online") },
                        trailingContent = { Switch(checked = uiState.showOnlineStatus, onCheckedChange = { viewModel.toggleShowOnlineStatus() }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SddPink)) }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            uiState.error?.let { err ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(8.dp)) {
                    Text(err, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(12.dp))
            }

            SddButton("Save Profile", onClick = { viewModel.saveProfile() }, isLoading = uiState.isLoading)
            Spacer(Modifier.height(32.dp))
        }
    }
}
