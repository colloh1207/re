package com.sdd.marketplace.feature.settings.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.sdd.marketplace.core.navigation.Screen
import com.sdd.marketplace.core.ui.theme.SddPink
import com.sdd.marketplace.data.local.dao.SavedAccountDao
import com.sdd.marketplace.data.local.entities.SavedAccountEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SwitchAccountViewModel @Inject constructor(
    private val savedAccountDao: SavedAccountDao
) : ViewModel() {
    val accounts: StateFlow<List<SavedAccountEntity>> = savedAccountDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun switchTo(userId: String) = viewModelScope.launch {
        savedAccountDao.clearActive()
        savedAccountDao.setActive(userId)
    }

    fun removeAccount(userId: String) = viewModelScope.launch {
        savedAccountDao.delete(userId)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwitchAccountScreen(
    navController: NavController,
    viewModel: SwitchAccountViewModel = hiltViewModel()
) {
    val accounts by viewModel.accounts.collectAsState()
    val activeAccount = accounts.firstOrNull { it.isActive }
    val otherAccounts = accounts.filter { !it.isActive }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } },
                title = { Text("Switch Account", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item {
                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = SddPink.copy(alpha = 0.08f))) {
                    Row(Modifier.padding(16.dp)) {
                        Icon(Icons.Filled.Info, "Info", tint = SddPink)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "You can have up to 2 accounts on this device. Add a second account to switch between them easily.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            if (activeAccount != null) {
                item {
                    Text("Current Account", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    AccountCard(account = activeAccount, isActive = true, onClick = null, onRemove = null)
                    Spacer(Modifier.height(20.dp))
                }
            }

            if (otherAccounts.isNotEmpty()) {
                item {
                    Text("Other Accounts", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                }
                items(otherAccounts) { account ->
                    AccountCard(
                        account = account,
                        isActive = false,
                        onClick = {
                            viewModel.switchTo(account.userId)
                            navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } }
                        },
                        onRemove = { viewModel.removeAccount(account.userId) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (accounts.size < 2) {
                item {
                    if (otherAccounts.isEmpty()) {
                        Text("Add Account", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                    }
                    Card(shape = RoundedCornerShape(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth().clickable { navController.navigate(Screen.Login.route) }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(48.dp).clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.AddCircleOutline, "Add", tint = SddPink, modifier = Modifier.size(32.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Add another account", fontWeight = FontWeight.Medium, color = SddPink)
                                Text("Sign in with a different email or phone", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Each device supports a maximum of 2 accounts.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AccountCard(
    account: SavedAccountEntity,
    isActive: Boolean,
    onClick: (() -> Unit)?,
    onRemove: (() -> Unit)?
) {
    Card(shape = RoundedCornerShape(12.dp)) {
        Row(
            Modifier.fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = account.avatarUrl,
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(account.fullName, fontWeight = FontWeight.SemiBold)
                Text(
                    account.email ?: account.phone ?: "Unknown",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isActive) {
                    Spacer(Modifier.height(2.dp))
                    Text("Active", fontSize = 11.sp, color = SddPink, fontWeight = FontWeight.Medium)
                }
            }
            if (isActive) {
                Icon(Icons.Filled.CheckCircle, "Active", tint = SddPink)
            } else {
                Row {
                    if (onRemove != null) {
                        IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.RemoveCircleOutline, "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        }
                    }
                    Icon(Icons.Filled.ChevronRight, "Switch", tint = SddPink)
                }
            }
        }
    }
}
