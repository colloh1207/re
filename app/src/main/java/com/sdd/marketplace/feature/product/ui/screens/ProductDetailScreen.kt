package com.sdd.marketplace.feature.product.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.sdd.marketplace.core.navigation.Screen
import com.sdd.marketplace.core.ui.components.*
import com.sdd.marketplace.core.ui.theme.*
import com.sdd.marketplace.domain.model.*
import com.sdd.marketplace.feature.product.viewmodel.ProductDetailEvent
import com.sdd.marketplace.feature.product.viewmodel.ProductViewModel
import com.sdd.marketplace.feature.orders.ui.BuyerTermsDialog

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductDetailScreen(navController: NavController, viewModel: ProductViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    var showBuyerTerms by remember { mutableStateOf(false) }
    var showWriteReview by remember { mutableStateOf(false) }
    var reviewRating by remember { mutableIntStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }
    var replyingToReviewId by remember { mutableStateOf<String?>(null) }
    var replyText by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProductDetailEvent.NavigateToChat -> navController.navigate(Screen.ChatDetail.createRoute(event.chatId))
                else -> {}
            }
        }
    }

    if (showBuyerTerms) {
        BuyerTermsDialog(
            onAccept = { showBuyerTerms = false; viewModel.buyNow() },
            onDecline = { showBuyerTerms = false }
        )
    }

    if (showWriteReview) {
        AlertDialog(
            onDismissRequest = { showWriteReview = false },
            title = { Text("Write a Review", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Your Rating", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { star ->
                            IconButton(onClick = { reviewRating = star }, modifier = Modifier.size(36.dp)) {
                                Icon(if (star <= reviewRating) Icons.Filled.Star else Icons.Outlined.StarBorder, "Star $star",
                                    tint = if (star <= reviewRating) StarYellow else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = reviewComment, onValueChange = { reviewComment = it },
                        label = { Text("Your Review") }, modifier = Modifier.fillMaxWidth(),
                        minLines = 3, maxLines = 6,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SddPink)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    uiState.product?.let { viewModel.writeReview(it.id, it.sellerId, reviewRating, reviewComment) }
                    showWriteReview = false
                }, enabled = reviewComment.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = SddPink)) { Text("Submit Review") }
            },
            dismissButton = { TextButton(onClick = { showWriteReview = false }) { Text("Cancel") } }
        )
    }

    replyingToReviewId?.let { reviewId ->
        AlertDialog(
            onDismissRequest = { replyingToReviewId = null },
            title = { Text("Reply to Review") },
            text = {
                OutlinedTextField(replyText, { replyText = it }, label = { Text("Your reply") },
                    modifier = Modifier.fillMaxWidth(), minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SddPink))
            },
            confirmButton = {
                Button(onClick = { viewModel.replyToReview(reviewId, replyText); replyingToReviewId = null; replyText = "" },
                    enabled = replyText.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = SddPink)) { Text("Reply") }
            },
            dismissButton = { TextButton(onClick = { replyingToReviewId = null }) { Text("Cancel") } }
        )
    }

    if (uiState.isLoading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SddPink) }; return }

    uiState.product?.let { product ->
        Scaffold(
            bottomBar = {
                Surface(shadowElevation = 8.dp) {
                    Column {
                        if (product.isSold) {
                            Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("This item has been sold", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                            }
                        } else {
                            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                SddOutlineButton("Add to Wishlist", onClick = { viewModel.toggleFavorite() }, modifier = Modifier.weight(1f))
                                SddButton("Buy Now", onClick = { showBuyerTerms = true }, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        ) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                // Image Gallery
                item {
                    Box {
                        HorizontalPager(state = rememberPagerState(pageCount = { product.images.size.coerceAtLeast(1) }), modifier = Modifier.fillMaxWidth().height(300.dp)) { page ->
                            AsyncImage(model = product.images.getOrNull(page), contentDescription = product.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                        Row(Modifier.fillMaxWidth().align(Alignment.TopStart).padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape).padding(4.dp))
                            }
                            Row {
                                IconButton(onClick = { }) { Icon(Icons.Filled.Share, "Share", tint = Color.White) }
                                Box {
                                    var showMenu by remember { mutableStateOf(false) }
                                    IconButton(onClick = { showMenu = true }) { Icon(Icons.Filled.MoreVert, "More", tint = Color.White) }
                                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                        DropdownMenuItem(leadingIcon = { Icon(Icons.Filled.Flag, "Report", tint = MaterialTheme.colorScheme.error) },
                                            text = { Text("Report Listing", color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; showReportDialog = true })
                                    }
                                }
                            }
                        }
                        IconButton(onClick = { viewModel.toggleFavorite() }, modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)) {
                            Icon(if (uiState.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "Favorite",
                                tint = if (uiState.isFavorite) SddPink else Color.White)
                        }
                        Text("${product.images.size.coerceAtLeast(1)} photos", color = Color.White, fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp)).padding(horizontal = 8.dp, vertical = 2.dp))
                    }
                }
                // Product Info
                item {
                    Column(Modifier.padding(16.dp)) {
                        if (product.isFeatured) {
                            Surface(color = SddPink.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp)) {
                                Text("Best Seller", color = SddPink, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        Text(product.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("₹${String.format("%.0f", product.price)}", color = SddPink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            product.discountPrice?.let {
                                Spacer(Modifier.width(8.dp))
                                Text("₹${String.format("%.0f", it)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                                val discount = ((it - product.price) / it * 100).toInt()
                                Spacer(Modifier.width(4.dp))
                                Surface(color = SuccessGreen.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                    Text("${discount}% OFF", color = SuccessGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StarRating(product.rating)
                            Spacer(Modifier.width(4.dp))
                            Text("${product.rating} (${product.reviewCount} Reviews)", fontSize = 12.sp)
                            Spacer(Modifier.width(12.dp))
                            Text("• ${product.viewCount}+ Views", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                // Features Row
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        FeaturePill(Icons.Outlined.Shield, "Premium\nQuality"); FeaturePill(Icons.Outlined.Refresh, "7 Days\nReturn")
                        FeaturePill(Icons.Outlined.LocalShipping, "Free\nDelivery"); FeaturePill(Icons.Outlined.Lock, "Secure\nPayment")
                    }
                    Spacer(Modifier.height(8.dp))
                }
                // Product Details
                item {
                    Column(Modifier.padding(16.dp)) {
                        Text("Product Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(product.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth()) {
                            DetailChip("Condition", product.condition)
                            Spacer(Modifier.width(8.dp))
                            DetailChip("Stock", "${product.stockQuantity} left")
                            Spacer(Modifier.width(8.dp))
                            if (product.isNegotiable) DetailChip("Price", "Negotiable")
                        }
                    }
                }
                // Seller Info Card
                uiState.seller?.let { seller ->
                    item {
                        SellerInfoCard(
                            seller = seller,
                            isFollowing = uiState.isFollowingSeller,
                            onFollow = { viewModel.followSeller() },
                            onMessage = { viewModel.messageSeller() },
                            onViewFollowers = { navController.navigate(Screen.Followers.createRoute(seller.id)) },
                            navController = navController
                        )
                    }
                }
                // Reviews Section
                item {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Ratings & Reviews", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            TextButton(onClick = { showWriteReview = true }) { Text("Write Review", color = SddPink) }
                        }
                        uiState.reviewSummary?.let { summary ->
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${String.format("%.1f", summary.averageRating)}", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Filled.Star, "Star", tint = StarYellow, modifier = Modifier.size(24.dp))
                            }
                            Text("(${summary.totalReviews} Reviews)", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            (5 downTo 1).forEach { star ->
                                val count = summary.ratingBreakdown[star] ?: 0
                                val fraction = if (summary.totalReviews > 0) count.toFloat() / summary.totalReviews else 0f
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text("$star", fontSize = 12.sp, modifier = Modifier.width(12.dp))
                                    Spacer(Modifier.width(4.dp))
                                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)), color = SddPink, trackColor = SddPink.copy(alpha = 0.2f))
                                    Spacer(Modifier.width(4.dp))
                                    Text("$count", fontSize = 12.sp, modifier = Modifier.width(24.dp))
                                }
                            }
                        }
                    }
                }
                items(uiState.reviews) { review ->
                    ReviewItemEnhanced(
                        review = review,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        onHelpful = { viewModel.markHelpful(review.id, review.isHelpfulByCurrentUser) },
                        onReply = { replyingToReviewId = review.id }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ErrorMessage(uiState.error ?: "Product not found") { navController.popBackStack() }
    }
}

@Composable
fun DetailChip(label: String, value: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun FeaturePill(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, tint = SddPink, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SellerInfoCard(seller: User, isFollowing: Boolean, onFollow: () -> Unit, onMessage: () -> Unit, onViewFollowers: () -> Unit, navController: NavController) {
    Card(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Seller Information", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = seller.avatarUrl, contentDescription = seller.fullName, modifier = Modifier.size(56.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(seller.fullName, fontWeight = FontWeight.Bold)
                        if (seller.isVerified) { Spacer(Modifier.width(4.dp)); VerifiedBadge() }
                    }
                    Text(if (seller.isVerified) "Verified Seller" else "Seller", color = SddPink, fontSize = 12.sp)
                }
                OutlinedButton(onClick = onFollow, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, SddPink)) {
                    Text(if (isFollowing) "Following" else "Follow", color = SddPink)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                SellerStat("${seller.rating}", "Rating")
                SellerStat(seller.followerCount.toString(), "Followers", onClick = onViewFollowers)
                SellerStat("${seller.productCount}", "Products")
                SellerStat("${seller.responseRate}%", "Response")
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onMessage, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, SddPink)) {
                    Icon(Icons.Outlined.Message, "Message", tint = SddPink, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Message", color = SddPink)
                }
                OutlinedButton(onClick = { navController.navigate(Screen.Profile.createRoute(seller.id)) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, SddPink)) {
                    Icon(Icons.Outlined.Store, "Shop", tint = SddPink, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("View Shop", color = SddPink)
                }
            }
        }
    }
}

@Composable
fun SellerStat(value: String, label: String, onClick: (() -> Unit)? = null) {
    val mod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Column(mod, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, fontSize = 11.sp, color = if (onClick != null) SddPink else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ReviewItemEnhanced(review: Review, modifier: Modifier = Modifier, onHelpful: () -> Unit, onReply: () -> Unit) {
    var showReplies by remember { mutableStateOf(false) }
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = review.reviewer?.avatarUrl, contentDescription = review.reviewer?.fullName, modifier = Modifier.size(36.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(review.reviewer?.fullName ?: "User", fontWeight = FontWeight.Medium)
                if (review.isVerifiedPurchase) Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Verified, "Verified", tint = SuccessGreen, modifier = Modifier.size(12.dp))
                    Text(" Verified Purchase", fontSize = 11.sp, color = SuccessGreen)
                }
            }
            Spacer(Modifier.weight(1f))
            Text(review.createdAt.take(10), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(4.dp))
        StarRating(review.rating.toDouble(), size = 14.dp)
        Spacer(Modifier.height(4.dp))
        Text(review.comment, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Helpful button
            TextButton(onClick = onHelpful, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                Icon(if (review.isHelpfulByCurrentUser) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp, "Helpful",
                    tint = if (review.isHelpfulByCurrentUser) SddPink else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Helpful (${review.helpfulCount})", fontSize = 12.sp, color = if (review.isHelpfulByCurrentUser) SddPink else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onReply, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                Icon(Icons.Outlined.Reply, "Reply", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Reply", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (review.replies.isNotEmpty()) {
                TextButton(onClick = { showReplies = !showReplies }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("${review.replies.size} ${if (showReplies) "▲" else "▼"}", fontSize = 12.sp, color = SddPink)
                }
            }
        }
        if (showReplies) {
            review.replies.forEach { reply ->
                Row(Modifier.padding(start = 16.dp, top = 8.dp)) {
                    Divider(Modifier.width(2.dp).height(40.dp), color = SddPink.copy(alpha = 0.3f))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(model = reply.author?.avatarUrl, contentDescription = null, modifier = Modifier.size(24.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            Spacer(Modifier.width(6.dp))
                            Text(reply.author?.fullName ?: "User", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            if (reply.isSeller) {
                                Spacer(Modifier.width(4.dp))
                                Surface(color = SddPink, shape = RoundedCornerShape(4.dp)) {
                                    Text("Seller", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }
                        }
                        Text(reply.content, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(reply.createdAt.take(10), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Divider(Modifier.padding(top = 8.dp))
    }
}

@Composable
fun ReviewItem(review: Review, modifier: Modifier = Modifier) {
    ReviewItemEnhanced(review = review, modifier = modifier, onHelpful = {}, onReply = {})
}
