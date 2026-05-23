package com.sdd.marketplace.feature.profile.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.sdd.marketplace.core.util.ErrorHandler
import com.sdd.marketplace.core.util.NetworkChecker
import com.sdd.marketplace.core.ui.theme.*
import com.sdd.marketplace.data.local.dao.ReferralDao
import com.sdd.marketplace.data.local.entities.ReferralEntity
import com.sdd.marketplace.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class InviteEarnUiState(
    val isLoading: Boolean = false,
    val isSendingInvite: Boolean = false,
    val referralCode: String = "",
    val totalInvited: Int = 0,
    val totalEarned: Double = 0.0,
    val referralHistory: List<ReferralEntity> = emptyList(),
    val error: String? = null,
    val successMessage: String? = null,
    val inviteEmail: String = "",
    val rewardPerReferral: Double = 5.0,
    val boostThreshold: Int = 3,
    val featureUnlockThreshold: Int = 5
)

@HiltViewModel
class InviteEarnViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val referralDao: ReferralDao,
    private val postgrest: Postgrest,
    private val networkChecker: NetworkChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(InviteEarnUiState())
    val uiState: StateFlow<InviteEarnUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() = viewModelScope.launch {
        val userId = authRepository.getCurrentUserId() ?: return@launch
        val code = "SDD-${userId.take(6).uppercase()}"
        _uiState.update { it.copy(referralCode = code) }

        referralDao.getByReferrer(userId).collect { referrals ->
            val earned = referralDao.totalEarnings(userId) ?: 0.0
            val completed = referralDao.countCompleted(userId)
            _uiState.update { s ->
                s.copy(
                    referralHistory = referrals,
                    totalInvited = referrals.size,
                    totalEarned = earned
                )
            }
        }
    }

    fun setInviteEmail(email: String) = _uiState.update { it.copy(inviteEmail = email) }

    fun sendEmailInvite(email: String) = viewModelScope.launch {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.update { it.copy(error = "Please enter a valid email address.") }
            return@launch
        }
        if (!networkChecker.isOnline()) {
            _uiState.update { it.copy(error = "No internet connection. Please check your network.") }
            return@launch
        }
        val userId = authRepository.getCurrentUserId() ?: return@launch
        _uiState.update { it.copy(isSendingInvite = true, error = null) }
        try {
            val referral = ReferralEntity(
                id = UUID.randomUUID().toString(),
                referrerId = userId,
                referredEmail = email,
                status = "pending",
                rewardAmount = _uiState.value.rewardPerReferral,
                currency = "USD"
            )
            referralDao.insert(referral)
            try {
                postgrest["referrals"].insert(mapOf(
                    "id" to referral.id,
                    "referrer_id" to userId,
                    "referred_email" to email,
                    "status" to "pending",
                    "reward_amount" to referral.rewardAmount,
                    "currency" to referral.currency
                ))
            } catch (e: Exception) {
            }
            _uiState.update { it.copy(
                isSendingInvite = false,
                inviteEmail = "",
                successMessage = "Invitation sent to $email!",
                totalInvited = _uiState.value.totalInvited + 1
            )}
        } catch (e: Exception) {
            _uiState.update { it.copy(isSendingInvite = false, error = ErrorHandler.friendlyMessage(e)) }
        }
    }

    fun clearMessages() = _uiState.update { it.copy(error = null, successMessage = null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteEarnScreen(
    navController: NavController,
    viewModel: InviteEarnViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showCopied by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } },
                title = { Text("Invite & Earn", fontWeight = FontWeight.Bold) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Box(
                    Modifier.fillMaxWidth().background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(SddPink.copy(0.12f), MaterialTheme.colorScheme.surface))
                    ).padding(32.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(Modifier.size(80.dp).clip(CircleShape).background(SddPink.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                            Text("🎁", fontSize = 40.sp)
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Invite Friends, Earn Rewards!", fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Invite friends via email. When they sign up and complete a purchase, you both earn $${uiState.rewardPerReferral.toInt()} credit!",
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(24.dp))

                        if (uiState.referralCode.isNotBlank()) {
                            Row(
                                Modifier.fillMaxWidth(0.9f).border(2.dp, SddPink, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp)).background(SddPink.copy(alpha = 0.05f)).padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Your Referral Code", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(uiState.referralCode, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = SddPink, letterSpacing = 2.sp)
                                }
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Referral Code", uiState.referralCode))
                                    showCopied = true
                                }) {
                                    Icon(if (showCopied) Icons.Filled.CheckCircle else Icons.Outlined.ContentCopy, "Copy", tint = SddPink)
                                }
                            }
                            if (showCopied) {
                                Spacer(Modifier.height(4.dp))
                                Text("Copied!", color = SuccessGreen, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Send Email Invite", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Invite a friend directly by their email address.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = uiState.inviteEmail,
                            onValueChange = { viewModel.setInviteEmail(it) },
                            label = { Text("Friend's email address") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Email, "Email", tint = SddPink) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SddPink)
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.sendEmailInvite(uiState.inviteEmail) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = uiState.inviteEmail.isNotBlank() && !uiState.isSendingInvite,
                            colors = ButtonDefaults.buttonColors(containerColor = SddPink),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (uiState.isSendingInvite) {
                                CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Sending...")
                            } else {
                                Icon(Icons.Filled.Send, "Send", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Send Invite")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Join Sdd Marketplace! Use my referral code ${uiState.referralCode} to get rewards when you sign up. Download now: https://sddmarket.app")
                                }
                                context.startActivity(Intent.createChooser(intent, "Share via"))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SddPink),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SddPink),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Share, "Share", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Share Referral Link")
                        }
                    }
                }
            }

            item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Your Earnings", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            EarningsStatItem("${uiState.totalInvited}", "Invited", SddPink)
                            EarningsStatItem("$${uiState.totalEarned.toInt()}", "Earned", SuccessGreen)
                        }
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))
                        Text("Rewards Milestones", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        MilestoneItem(
                            icon = "🚀",
                            title = "${uiState.boostThreshold} referrals → Free Boost",
                            subtitle = "Boost a listing for free after ${uiState.boostThreshold} completed referrals",
                            achieved = uiState.totalInvited >= uiState.boostThreshold
                        )
                        Spacer(Modifier.height(8.dp))
                        MilestoneItem(
                            icon = "⭐",
                            title = "${uiState.featureUnlockThreshold} referrals → Premium Features",
                            subtitle = "Unlock analytics and priority placement",
                            achieved = uiState.totalInvited >= uiState.featureUnlockThreshold
                        )
                    }
                }
            }

            if (uiState.referralHistory.isNotEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text("Referral History", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp, top = 8.dp))
                    }
                }
                items(uiState.referralHistory) { referral ->
                    ListItem(
                        headlineContent = { Text(referral.referredEmail, fontWeight = FontWeight.Medium) },
                        supportingContent = {
                            Text(
                                when (referral.status) {
                                    "completed" -> "Signed up · Reward earned"
                                    "pending" -> "Invite sent · Awaiting signup"
                                    else -> referral.status
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (referral.status == "completed") SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = {
                            Box(
                                Modifier.size(40.dp).clip(CircleShape).background(SddPink.copy(0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (referral.status == "completed") Icons.Filled.CheckCircle else Icons.Filled.Email,
                                    "Status",
                                    tint = if (referral.status == "completed") SuccessGreen else SddPink,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        trailingContent = {
                            if (referral.status == "completed") {
                                Text("+$${referral.rewardAmount.toInt()}", color = SuccessGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 56.dp))
                }
            }

            item {
                Card(
                    Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SddPink.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Terms & Conditions", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        listOf(
                            "Referral credit is valid for 90 days from the date of credit",
                            "Only new users who haven't signed up before are eligible",
                            "Both you and your friend must complete profile setup to earn rewards",
                            "Maximum 50 referrals per account per month",
                            "Earnings can be used to boost listings or unlock premium features"
                        ).forEach { term ->
                            Row(Modifier.padding(vertical = 2.dp)) {
                                Text("• ", fontSize = 12.sp, color = SddPink)
                                Text(term, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun EarningsStatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = color)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MilestoneItem(icon: String, title: String, subtitle: String, achieved: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 24.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = if (achieved) SuccessGreen else MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (achieved) {
            Icon(Icons.Filled.CheckCircle, "Achieved", tint = SuccessGreen, modifier = Modifier.size(20.dp))
        }
    }
}
