package com.sdd.marketplace.feature.review.ui

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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sdd.marketplace.core.ui.components.SddButton
import com.sdd.marketplace.core.ui.components.SddTextField
import com.sdd.marketplace.core.ui.theme.SddPink
import com.sdd.marketplace.feature.review.viewmodel.RateSellerEvent
import com.sdd.marketplace.feature.review.viewmodel.RateSellerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateSellerScreen(
    navController: NavController,
    viewModel: RateSellerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is RateSellerEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is RateSellerEvent.NavigateBack -> navController.popBackStack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                },
                title = { Text("Rate Seller", fontWeight = FontWeight.Bold) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            if (uiState.isSubmitted) {
                SubmittedSuccess()
            } else {
                ReviewHeader(sellerName = uiState.sellerName)
                StarRatingRow(
                    selected = uiState.selectedRating,
                    onSelect = { viewModel.setRating(it) }
                )
                RatingLabel(uiState.selectedRating)
                SddTextField(
                    value = uiState.comment,
                    onValueChange = { viewModel.setComment(it) },
                    label = "Share your experience (optional)",
                    minLines = 4,
                    maxLines = 8
                )
                ReviewGuidelines()
                uiState.error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                SddButton(
                    text = if (uiState.isLoading) "Submitting…" else "Submit Review",
                    onClick = { viewModel.submitReview() },
                    enabled = uiState.selectedRating > 0 && !uiState.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ReviewHeader(sellerName: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Filled.StoreMallDirectory, "Seller", tint = SddPink, modifier = Modifier.size(56.dp))
        Text(
            "How was your experience with\n$sellerName?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            "Your review helps other buyers make informed decisions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StarRatingRow(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..5).forEach { star ->
            IconButton(onClick = { onSelect(star) }, modifier = Modifier.size(48.dp)) {
                Icon(
                    if (star <= selected) Icons.Filled.Star else Icons.Outlined.StarOutline,
                    contentDescription = "$star star",
                    tint = if (star <= selected) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@Composable
private fun RatingLabel(rating: Int) {
    val label = when (rating) {
        1 -> "😞 Poor"
        2 -> "😐 Fair"
        3 -> "🙂 Good"
        4 -> "😊 Very Good"
        5 -> "🤩 Excellent!"
        else -> "Tap a star to rate"
    }
    Text(
        label,
        fontWeight = if (rating > 0) FontWeight.SemiBold else FontWeight.Normal,
        color = if (rating > 0) SddPink else MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 16.sp
    )
}

@Composable
private fun ReviewGuidelines() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Review guidelines", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            listOf(
                "Be honest and constructive",
                "Describe the item condition and seller communication",
                "Avoid personal information or offensive language"
            ).forEach { tip ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Check, "", tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp).padding(top = 2.dp))
                    Text(tip, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SubmittedSuccess() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 60.dp)
    ) {
        Icon(Icons.Filled.CheckCircle, "Success", tint = Color(0xFF4CAF50), modifier = Modifier.size(80.dp))
        Text("Review Submitted!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Thank you for helping the community!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
