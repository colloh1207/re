package com.sdd.marketplace.feature.search.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sdd.marketplace.core.navigation.Screen
import com.sdd.marketplace.core.ui.components.*
import com.sdd.marketplace.core.ui.theme.SddPink
import com.sdd.marketplace.feature.search.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isSearchFocused by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        // Search TopBar
        TopAppBar(
            title = {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { viewModel.onQueryChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search products, sellers...") },
                    leadingIcon = { Icon(Icons.Filled.Search, "Search") },
                    trailingIcon = {
                        if (uiState.query.isNotBlank()) {
                            IconButton(onClick = { viewModel.onQueryChanged("") }) {
                                Icon(Icons.Filled.Clear, "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(25.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SddPink)
                )
            }
        )

        if (!uiState.isSearching) {
            // Recent searches
            LazyColumn(Modifier.fillMaxSize()) {
                if (uiState.recentSearches.isNotEmpty()) {
                    item {
                        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Recent Searches", fontWeight = FontWeight.Bold)
                            Text("Clear", color = SddPink, modifier = Modifier.clickable { viewModel.clearRecentSearches() })
                        }
                    }
                    items(uiState.recentSearches) { search ->
                        ListItem(
                            headlineContent = { Text(search) },
                            leadingContent = { Icon(Icons.Outlined.History, "Recent") },
                            modifier = Modifier.clickable { viewModel.search(search) }
                        )
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        } else if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SddPink)
            }
        } else if (uiState.results.isEmpty() && uiState.sellerResults.isEmpty()) {
            EmptyState("No results found", "Try different keywords")
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                if (uiState.sellerResults.isNotEmpty()) {
                    item { Text("Sellers", fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp)) }
                    items(uiState.sellerResults) { seller ->
                        ListItem(
                            headlineContent = { Text(seller.fullName) },
                            supportingContent = { Text("${seller.productCount} products") },
                            leadingContent = { },
                            trailingContent = { if (seller.isVerified) VerifiedBadge() },
                            modifier = Modifier.clickable { navController.navigate(Screen.Profile.createRoute(seller.id)) }
                        )
                    }
                }
                if (uiState.results.isNotEmpty()) {
                    item { Text("Products", fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp)) }
                    item {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.height(600.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(uiState.results) { product ->
                                ProductCard(
                                    imageUrl = product.images.firstOrNull(),
                                    title = product.title, price = product.price,
                                    sellerName = product.seller?.fullName ?: "",
                                    sellerAvatarUrl = product.seller?.avatarUrl,
                                    isVerified = product.seller?.isVerified == true,
                                    isFavorite = false, onFavoriteClick = { },
                                    onClick = { navController.navigate(Screen.ProductDetail.createRoute(product.id)) }
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
