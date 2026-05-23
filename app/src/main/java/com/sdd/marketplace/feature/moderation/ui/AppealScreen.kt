package com.sdd.marketplace.feature.moderation.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sdd.marketplace.core.ui.theme.SddPink
import com.sdd.marketplace.feature.moderation.viewmodel.SuspensionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppealScreen(
    onSubmitted: () -> Unit,
    onBack: () -> Unit,
    viewModel: SuspensionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.appealSubmitted) {
        if (uiState.appealSubmitted) {
            snackbarHostState.showSnackbar("Appeal submitted. We'll review it within 24-48 hours.")
            onSubmitted()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
                title = { Text("Submit Appeal", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Row {
                        Icon(Icons.Filled.Info, "Info", tint = SddPink)
                        Spacer(Modifier.width(8.dp))
                        Text("About the Appeal Process", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Your appeal will be reviewed by our moderation team within 24-48 hours. " +
                        "If approved, you will be required to complete KYC verification before your account is fully restored. " +
                        "Please provide an honest, detailed explanation.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Text("Your Appeal Statement", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = uiState.appealNote,
                onValueChange = { viewModel.onAppealNoteChanged(it) },
                modifier = Modifier.fillMaxWidth().height(180.dp),
                placeholder = { Text("Explain why you believe this suspension was made in error, or what you will do differently. Be honest and specific.") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SddPink),
                maxLines = 10
            )
            Text(
                "${uiState.appealNote.length}/1000 characters",
                style = MaterialTheme.typography.labelSmall,
                color = if (uiState.appealNote.length > 1000) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))) {
                Column(Modifier.padding(12.dp)) {
                    Text("Important", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "• False or misleading appeals may result in a permanent ban\n" +
                        "• You can only submit one appeal per suspension\n" +
                        "• KYC verification will be mandatory if your appeal is approved",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = { viewModel.submitAppeal() },
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.appealNote.length in 50..1000 && !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = SddPink)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = androidx.compose.ui.graphics.Color.White)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Submit Appeal")
            }
            Text(
                "Your appeal must be at least 50 characters.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
