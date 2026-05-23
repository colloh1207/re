package com.sdd.marketplace.feature.static.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.navigation.NavController
import com.sdd.marketplace.core.ui.theme.SddPink
import com.sdd.marketplace.core.ui.theme.SuccessGreen
import com.sdd.marketplace.feature.settings.viewmodel.SettingsEvent
import com.sdd.marketplace.feature.settings.viewmodel.SettingsViewModel

@Composable
fun ReportBugScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var stepsToReproduce by remember { mutableStateOf("") }
    var expectedBehavior by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf("medium") }
    var submitted by remember { mutableStateOf(false) }
    var ticketId by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ShowMessage -> {
                    ticketId = "BUG-${System.currentTimeMillis() % 100000}"
                    submitted = true
                }
                else -> {}
            }
        }
    }

    if (submitted) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.CheckCircle, "Submitted", tint = SuccessGreen, modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(20.dp))
            Text("Bug Report Submitted!", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("Ticket ID: $ticketId", fontWeight = FontWeight.Medium, color = SddPink)
            Spacer(Modifier.height(8.dp))
            Text(
                "Our engineering team will investigate and fix the issue. Thank you for helping us improve!",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = SddPink.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        "Severity: ${severity.replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("Title: $title", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(containerColor = SddPink)
            ) { Text("Back") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                submitted = false
                title = ""; description = ""; stepsToReproduce = ""; expectedBehavior = ""; severity = "medium"
                viewModel.clearMessages()
            }) { Text("Submit Another Report") }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report a Bug", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = SddPink.copy(alpha = 0.08f))) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.BugReport, "Bug", tint = SddPink)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Help us improve by describing the bug you encountered.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Bug Title *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = title.isBlank(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SddPink)
            )
            Text("Severity", fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "low" to "Low",
                    "medium" to "Medium",
                    "high" to "High",
                    "critical" to "Critical"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = severity == key,
                        onClick = { severity = key },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = when (key) {
                                "critical" -> MaterialTheme.colorScheme.error
                                "high"     -> Color(0xFFFF6B35)
                                else       -> SddPink
                            },
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("What happened? *") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                isError = description.isBlank(),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SddPink)
            )
            OutlinedTextField(
                value = stepsToReproduce,
                onValueChange = { stepsToReproduce = it },
                label = { Text("Steps to reproduce (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                placeholder = { Text("1. Go to ...\n2. Tap on ...\n3. See error") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SddPink)
            )
            OutlinedTextField(
                value = expectedBehavior,
                onValueChange = { expectedBehavior = it },
                label = { Text("Expected behavior (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SddPink)
            )
            Card {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "Device Info (Auto-detected)",
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "App v1.0.0 • Android ${android.os.Build.VERSION.RELEASE} • ${android.os.Build.MODEL}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Button(
                onClick = {
                    val fullDesc = buildString {
                        appendLine("[${severity.uppercase()}] $title")
                        appendLine()
                        append(description)
                        if (expectedBehavior.isNotBlank()) {
                            appendLine(); appendLine()
                            appendLine("Expected: $expectedBehavior")
                        }
                        appendLine(); appendLine()
                        append("Device: Android ${android.os.Build.VERSION.RELEASE} • ${android.os.Build.MODEL} • App v1.0.0")
                    }
                    viewModel.submitBugReport(fullDesc, stepsToReproduce)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && description.isNotBlank() && !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = SddPink)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Submit Bug Report")
                }
            }
            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
