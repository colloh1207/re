package com.sdd.marketplace.feature.profile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.sdd.marketplace.core.ui.components.EmptyState
import com.sdd.marketplace.core.ui.components.StarRating
import com.sdd.marketplace.core.ui.theme.*
import com.sdd.marketplace.domain.model.Review
import com.sdd.marketplace.feature.profile.viewmodel.MyReviewsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReviewsScreen(
    navController: NavController,
    viewModel: MyReviewsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } },
                title = { Text("My Reviews", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Summary card
            Card(
                Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SddPink.copy(alpha = 0.06f))
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(String.format("%.1f", uiState.averageRating), fontWeight = FontWeight.Bold, fontSize = 28.sp, color = SddPink)
                        StarRating(uiState.averageRating, size = 16.dp)
                        Text("Average", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(Modifier.width(1.dp).height(50.dp).background(MaterialTheme.colorScheme.outline.copy(0.3f)))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${uiState.receivedReviews.size}", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = SddPink)
                        Text("Received", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(Modifier.width(1.dp).height(50.dp).background(MaterialTheme.colorScheme.outline.copy(0.3f)))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${uiState.givenReviews.size}", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = SddPink)
                        Text("Given", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface, contentColor = SddPink) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Received (${uiState.receivedReviews.size})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Given (${uiState.givenReviews.size})") })
            }

            val reviews = if (selectedTab == 0) uiState.receivedReviews else uiState.givenReviews

            if (reviews.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        title = if (selectedTab == 0) "No reviews yet" else "You haven't reviewed anything",
                        subtitle = if (selectedTab == 0) "Reviews from buyers will appear here" else "After buying, leave a review for sellers"
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(reviews) { review ->
                        ReviewDetailCard(review = review, showProductInfo = true)
                        Divider(Modifier.padding(horizontal = 16.dp))
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun ReviewDetailCard(review: Review, showProductInfo: Boolean = false) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = review.reviewer?.avatarUrl, contentDescription = null, modifier = Modifier.size(44.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(review.reviewer?.fullName ?: "Anonymous", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StarRating(review.rating.toDouble(), size = 14.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(review.createdAt.take(10), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (review.isVerifiedPurchase) {
                Surface(color = SuccessGreen.copy(0.12f), shape = RoundedCornerShape(4.dp)) {
                    Row(Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Verified, "Verified", tint = SuccessGreen, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("Verified", fontSize = 11.sp, color = SuccessGreen)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(review.comment, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.ThumbUp, "Helpful", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(" ${review.helpfulCount} found this helpful", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (review.replies.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            review.replies.forEach { reply ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (reply.isSeller) SddPink.copy(0.06f) else MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(8.dp)) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                        AsyncImage(model = reply.author?.avatarUrl, contentDescription = null, modifier = Modifier.size(28.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(reply.author?.fullName ?: "User", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                if (reply.isSeller) { Spacer(Modifier.width(4.dp)); Surface(color = SddPink.copy(0.1f), shape = RoundedCornerShape(4.dp)) { Text("Seller", fontSize = 10.sp, color = SddPink, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) } }
                            }
                            Text(reply.content, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
