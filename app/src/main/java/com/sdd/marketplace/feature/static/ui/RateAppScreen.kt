package com.sdd.marketplace.feature.static.ui

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.android.play.core.review.ReviewManagerFactory
import com.sdd.marketplace.core.ui.theme.SddPink
import com.sdd.marketplace.core.ui.theme.StarYellow
import com.sdd.marketplace.feature.settings.viewmodel.SettingsEvent
import com.sdd.marketplace.feature.settings.viewmodel.SettingsViewModel

@Composable
fun RateAppScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedRating by remember { mutableIntStateOf(0) }
    var feedback by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            if (event is SettingsEvent.ShowMessage) submitted = true
        }
    }

    if (submitted) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Favorite, "Thank You", tint = SddPink, modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(24.dp))
            Text("Thank You!", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Your $selectedRating-star rating has been saved.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (selectedRating >= 4) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "The Play Store review dialog was shown to share publicly.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(containerColor = SddPink)
            ) { Text("Back to Settings") }
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rate App", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))
            Icon(Icons.Filled.ShoppingBag, "App", tint = SddPink, modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                "Enjoying SDD Marketplace?",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Tap a star to rate your experience",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { star ->
                    IconButton(onClick = { selectedRating = star }, modifier = Modifier.size(56.dp)) {
                        Icon(
                            if (star <= selectedRating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "Star $star",
                            tint = if (star <= selectedRating) StarYellow else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = selectedRating > 0,
                enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 2 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        listOf("Poor 😞", "Fair 😐", "Good 🙂", "Great 😃", "Excellent! 🤩")[selectedRating - 1],
                        color = SddPink,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = feedback,
                onValueChange = { feedback = it },
                label = { Text("Tell us more (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SddPink)
            )
            if (selectedRating in 1..3 && selectedRating > 0) {
                Spacer(Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Low rating? Submit a bug report to help us fix specific issues.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    viewModel.rateApp(selectedRating, feedback)
                    if (selectedRating >= 4) {
                        val activity = context as? Activity
                        if (activity != null) {
                            val manager = ReviewManagerFactory.create(context)
                            manager.requestReviewFlow().addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    manager.launchReviewFlow(activity, task.result)
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedRating > 0 && !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = SddPink)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Submit Rating")
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { navController.popBackStack() }) { Text("Maybe Later") }
            uiState.error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
