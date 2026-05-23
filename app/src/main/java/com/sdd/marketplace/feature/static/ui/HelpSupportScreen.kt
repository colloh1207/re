package com.sdd.marketplace.feature.static.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sdd.marketplace.core.ui.components.SddButton
import com.sdd.marketplace.core.ui.theme.SddPink
import com.sdd.marketplace.domain.model.SupportCategory
import com.sdd.marketplace.feature.settings.viewmodel.SettingsEvent
import com.sdd.marketplace.feature.settings.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(navController: NavController, viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } },
                title = { Text("Help & Support", fontWeight = FontWeight.Bold) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab, contentColor = SddPink) {
                listOf("FAQ", "Submit Request", "My Tickets").forEachIndexed { i, title ->
                    Tab(selected = selectedTab == i, onClick = { selectedTab = i }, text = { Text(title) })
                }
            }
            when (selectedTab) {
                0 -> FaqContent()
                1 -> SubmitRequestContent(viewModel, uiState)
                2 -> MyTicketsContent()
            }
        }
    }
}

@Composable
fun FaqContent() {
    val faqs = listOf(
        "How do I track my order?" to "Go to your Profile → Orders → select your order. You'll see real-time tracking updates from the seller.",
        "How do I get a refund?" to "Open your order, tap 'Request Refund', and describe the issue. We aim to resolve refund requests within 5 business days.",
        "How do I verify my identity (KYC)?" to "Go to Settings → KYC Verification. Upload your government-issued ID and a selfie. Review takes 1-3 business days.",
        "Why can't I write a review?" to "To maintain authentic reviews, only accounts older than 3 weeks who have completed a purchase can write reviews.",
        "How many accounts can I have?" to "You can have up to 2 accounts per device. Additional accounts may be restricted.",
        "How do I become a trusted seller?" to "Complete your KYC verification in Settings. Once approved, you'll receive a verified badge on your profile.",
        "How do I connect a payment method?" to "Go to Profile → Payment Methods → Add New. We support Razorpay, PayPal, and more.",
        "What is the fee structure for sellers?" to "We charge 5% transaction fee + 2% payment processing fee on completed sales. No listing fees.",
        "How do I report a scam or fraud?" to "Tap the three dots on any listing or profile and select 'Report'. Our safety team reviews all reports within 24 hours.",
        "Can I change my username or email?" to "You can change your email in Settings → Change Email. Email changes require verification."
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        faqs.forEach { (question, answer) ->
            TermsSection(question) { answer }
        }
    }
}

@Composable
fun SubmitRequestContent(viewModel: SettingsViewModel, uiState: com.sdd.marketplace.feature.settings.viewmodel.SettingsUiState) {
    var category by remember { mutableStateOf(SupportCategory.OTHER) }
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var ticketId by remember { mutableStateOf("") }

    if (submitted) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.CheckCircle, "Submitted", tint = com.sdd.marketplace.core.ui.theme.SuccessGreen, modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(20.dp))
            Text("Request Submitted!", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("Ticket ID: $ticketId", fontWeight = FontWeight.Medium, color = SddPink)
            Spacer(Modifier.height(8.dp))
            Text(
                "Our support team will respond to your request within 24–48 hours. You'll receive a notification when we reply.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = SddPink.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Category: ${category.label}", style = MaterialTheme.typography.bodySmall)
                    Text("Subject: $subject", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = { submitted = false; subject = ""; description = "" }) {
                Text("Submit Another Request")
            }
        }
        return
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
        Text("Submit a Support Request", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Our support team typically responds within 24–48 hours.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Text("Category", fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        ExposedDropdownMenuBox(expanded = showCategoryDropdown, onExpandedChange = { showCategoryDropdown = it }) {
            OutlinedTextField(
                value = category.label, onValueChange = {}, readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                shape = RoundedCornerShape(12.dp)
            )
            ExposedDropdownMenu(expanded = showCategoryDropdown, onDismissRequest = { showCategoryDropdown = false }) {
                SupportCategory.values().forEach { cat ->
                    DropdownMenuItem(text = { Text(cat.label) }, onClick = { category = cat; showCategoryDropdown = false })
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = subject, onValueChange = { subject = it },
            label = { Text("Subject") }, modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SddPink)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = description, onValueChange = { description = it },
            label = { Text("Description") }, modifier = Modifier.fillMaxWidth(),
            minLines = 4, maxLines = 8, shape = RoundedCornerShape(12.dp),
            placeholder = { Text("Describe your issue in detail...") },
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SddPink)
        )
        Spacer(Modifier.height(20.dp))
        SddButton(
            "Submit Request",
            onClick = {
                ticketId = "SDD-${System.currentTimeMillis() % 100000}"
                viewModel.submitSupportRequest(subject, description, category)
                submitted = true
            },
            isLoading = uiState.isLoading,
            enabled = subject.isNotBlank() && description.isNotBlank()
        )
        uiState.error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
fun MyTicketsContent() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.ConfirmationNumber, "Tickets", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text("No support tickets yet", fontWeight = FontWeight.Medium)
            Text("Your submitted requests will appear here", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportBugScreen(navController: NavController, viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var description by remember { mutableStateOf("") }
    var steps by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ShowMessage -> { snackbarHostState.showSnackbar(event.message); navController.popBackStack() }
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } },
                title = { Text("Report a Bug", fontWeight = FontWeight.Bold) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp).verticalScroll(rememberScrollState())) {
            Text("Help Us Improve", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text("Describe the bug you encountered. Your feedback helps us make the app better for everyone.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("What went wrong?") }, modifier = Modifier.fillMaxWidth(),
                minLines = 3, maxLines = 6, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SddPink)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = steps, onValueChange = { steps = it },
                label = { Text("Steps to reproduce (optional)") }, modifier = Modifier.fillMaxWidth(),
                minLines = 3, maxLines = 6, shape = RoundedCornerShape(12.dp),
                placeholder = { Text("1. Go to...\n2. Tap on...\n3. See error") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SddPink)
            )
            Spacer(Modifier.height(20.dp))
            SddButton("Submit Bug Report", onClick = { viewModel.submitBugReport(description, steps) },
                isLoading = uiState.isLoading, enabled = description.isNotBlank())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateAppScreen(navController: NavController, viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var rating by remember { mutableIntStateOf(0) }
    var note by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ShowMessage -> { snackbarHostState.showSnackbar(event.message); navController.popBackStack() }
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } },
                title = { Text("Rate Us", fontWeight = FontWeight.Bold) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))
            Icon(Icons.Filled.Store, "App", tint = SddPink, modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(16.dp))
            Text("How are we doing?", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Your feedback helps us improve and is greatly appreciated.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { star ->
                    IconButton(onClick = { rating = star }, modifier = Modifier.size(48.dp)) {
                        Icon(
                            if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            "Star $star",
                            tint = if (star <= rating) com.sdd.marketplace.core.ui.theme.StarYellow else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
            if (rating > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    when (rating) { 1 -> "Very Poor 😞"; 2 -> "Poor 😕"; 3 -> "Average 🙂"; 4 -> "Good 😊"; else -> "Excellent! 🤩" },
                    fontWeight = FontWeight.SemiBold, color = SddPink
                )
            }
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text("Leave a note (optional)") }, modifier = Modifier.fillMaxWidth(),
                minLines = 3, maxLines = 5, shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Tell us what you love or what we can improve...") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SddPink)
            )
            Spacer(Modifier.height(24.dp))
            SddButton("Submit Rating", onClick = { viewModel.rateApp(rating, note) },
                isLoading = uiState.isLoading, enabled = rating > 0)
        }
    }
}
