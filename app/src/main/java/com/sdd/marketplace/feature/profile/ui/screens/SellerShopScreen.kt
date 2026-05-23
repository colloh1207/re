package com.sdd.marketplace.feature.profile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.sdd.marketplace.core.navigation.Screen
import com.sdd.marketplace.core.ui.components.*
import com.sdd.marketplace.core.ui.theme.*
import com.sdd.marketplace.feature.profile.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerShopScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val products = viewModel.userProducts.collectAsLazyPagingItems()
    var selectedTab by remember { mutableIntStateOf(0) }
    var sortBy by remember { mutableStateOf("Newest") }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } },
                title = { Text("Seller Shop", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Outlined.Search, "Search") }
                    IconButton(onClick = {}) { Icon(Icons.Outlined.Share, "Share", tint = SddPink) }
                }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Box(
                    Modifier.fillMaxWidth().height(180.dp).background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(SddPink.copy(alpha = 0.3f), SddPink.copy(alpha = 0.05f)))
                    )
                ) {
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                        AsyncImage(model = uiState.user?.avatarUrl, contentDescription = null, modifier = Modifier.size(80.dp).clip(CircleShape).border(3.dp, Color.White, CircleShape), contentScale = ContentScale.Crop)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(uiState.user?.fullName ?: "Seller", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                            if (uiState.user?.isVerified == true) {
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Filled.Verified, "Verified", tint = SddPink, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            item {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        SellerShopStat("${uiState.user?.productCount ?: 0}", "Listings", SddPink)
                        Box(Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.outline.copy(0.3f)))
                        SellerShopStat("${uiState.user?.soldCount ?: 0}", "Sold", SuccessGreen)
                        Box(Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.outline.copy(0.3f)))
                        SellerShopStat("${String.format("%.1f", uiState.user?.rating ?: 0.0)}⭐", "Rating", StarYellow)
                        Box(Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.outline.copy(0.3f)))
                        SellerShopStat("${uiState.user?.responseRate ?: 0}%", "Response", SddPink)
                    }
                    uiState.user?.bio?.let { bio ->
                        Spacer(Modifier.height(16.dp))
                        Text(bio, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!uiState.isCurrentUser) {
                            Button(
                                onClick = { viewModel.followUnfollow() },
                                colors = ButtonDefaults.buttonColors(containerColor = if (uiState.isFollowing) MaterialTheme.colorScheme.surfaceVariant else SddPink),
                                modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)
                            ) { Text(if (uiState.isFollowing) "Following" else "Follow", color = if (uiState.isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else Color.White) }
                            OutlinedButton(
                                onClick = { viewModel.messageUser() },
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, SddPink),
                                modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)
                            ) { Icon(Icons.Outlined.Message, "Message", tint = SddPink, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Message", color = SddPink) }
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("All Products", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { expanded = true }) {
                            Text("Sort: $sortBy", color = SddPink, fontSize = 13.sp)
                            Icon(Icons.Filled.ArrowDropDown, "Sort", tint = SddPink)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("Newest", "Price: Low to High", "Price: High to Low", "Most Popular").forEach { sort ->
                                DropdownMenuItem(text = { Text(sort) }, onClick = { sortBy = sort; expanded = false })
                            }
                        }
                    }
                }
            }

            if (products.loadState.refresh is LoadState.Loading) {
                items(4) {
                    ShimmerProductCard(Modifier.fillMaxWidth().padding(4.dp))
                }
            } else if (products.itemCount == 0) {
                item {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        EmptyState("No products", "This seller has no active listings.")
                    }
                }
            } else {
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.heightIn(min = 200.dp, max = 2000.dp),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(products.itemCount) { index ->
                            products[index]?.let { product ->
                                ProductCard(
                                    imageUrl = product.images.firstOrNull(),
                                    title = product.title,
                                    price = product.price,
                                    sellerName = uiState.user?.fullName ?: "",
                                    sellerAvatarUrl = uiState.user?.avatarUrl,
                                    isVerified = uiState.user?.isVerified == true,
                                    isFavorite = false,
                                    onFavoriteClick = {},
                                    onClick = { navController.navigate(Screen.ProductDetail.createRoute(product.id)) }
                                )
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
fun SellerShopStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
