package com.sdd.marketplace.feature.home.ui.screens

import android.Manifest
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.sdd.marketplace.core.navigation.Screen
import com.sdd.marketplace.core.ui.components.*
import com.sdd.marketplace.core.ui.theme.*
import com.sdd.marketplace.domain.model.Category
import com.sdd.marketplace.domain.model.Product
import com.sdd.marketplace.feature.home.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val products = viewModel.products.collectAsLazyPagingItems()
    val pullRefreshState = rememberPullToRefreshState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("All", "Near Me", "Featured", "Trending")

    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION) { granted ->
        if (granted) viewModel.onLocationPermissionGranted() else viewModel.onLocationPermissionDenied()
    }

    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(Unit) { viewModel.refreshProducts(); pullRefreshState.endRefresh() }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            if (locationPermission.status.isGranted) {
                viewModel.onLocationPermissionGranted()
            } else {
                locationPermission.launchPermissionRequest()
            }
        }
    }

    if (uiState.showLocationRationale) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissLocationRationale() },
            icon = { Icon(Icons.Filled.LocationOn, "Location", tint = SddPink) },
            title = { Text("Location Access Required") },
            text = { Text("To show nearby listings, we need your location. This helps you find products close to you.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.dismissLocationRationale()
                        locationPermission.launchPermissionRequest()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SddPink)
                ) { Text("Allow Location") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissLocationRationale() }) { Text("Not now") }
            }
        )
    }

    Box(Modifier.fillMaxSize().background(SddLightPink)) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.ShoppingBag, "Logo", tint = SddPink, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Marketplace", color = SddPink, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    }
                },
                actions = {
                    BadgedBox(badge = {
                        if (uiState.unreadNotifications > 0) Badge { Text("${uiState.unreadNotifications}") }
                    }) {
                        IconButton(onClick = { navController.navigate(Screen.Notifications.route) }) {
                            Icon(Icons.Outlined.Notifications, "Notifications")
                        }
                    }
                    IconButton(onClick = { navController.navigate(Screen.Wishlist.route) }) {
                        Icon(Icons.Outlined.FavoriteBorder, "Wishlist")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SddLightPink)
            )

            TabRow(selectedTabIndex = selectedTab, containerColor = SddLightPink, contentColor = SddPink) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index, onClick = { selectedTab = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (title == "Near Me") {
                                    Icon(Icons.Outlined.NearMe, "NearMe", modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                }
                                if (title == "Featured") {
                                    Icon(Icons.Filled.Star, "Featured", modifier = Modifier.size(14.dp), tint = Color(0xFFFFB300))
                                    Spacer(Modifier.width(4.dp))
                                }
                                if (title == "Trending") {
                                    Icon(Icons.Filled.TrendingUp, "Trending", modifier = Modifier.size(14.dp), tint = Color(0xFF4CAF50))
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(title)
                            }
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> AllProductsTab(uiState, products, viewModel, navController)
                1 -> NearMeTab(uiState, viewModel, navController, locationPermission.status.isGranted, onRequestPermission = {
                    if (locationPermission.status.isGranted) viewModel.onLocationPermissionGranted()
                    else locationPermission.launchPermissionRequest()
                })
                2 -> FeaturedTab(uiState, viewModel, navController)
                3 -> TrendingTab(uiState, viewModel, navController)
            }
        }
        PullToRefreshContainer(state = pullRefreshState, modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
fun AllProductsTab(
    uiState: com.sdd.marketplace.feature.home.viewmodel.HomeUiState,
    products: androidx.paging.compose.LazyPagingItems<Product>,
    viewModel: HomeViewModel,
    navController: NavController
) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = uiState.searchQuery, onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search products, brands...") },
                    leadingIcon = { Icon(Icons.Filled.Search, "Search") },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) IconButton(onClick = { viewModel.onSearchQueryChanged("") }) { Icon(Icons.Filled.Clear, "Clear") }
                    },
                    singleLine = true, shape = RoundedCornerShape(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White, focusedBorderColor = SddPink, unfocusedBorderColor = Color.Transparent)
                )
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(SddPink).clickable { navController.navigate(Screen.Search.route) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Tune, "Filter", tint = Color.White)
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Categories", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("See all", color = SddPink, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { navController.navigate(Screen.Search.route) })
                }
                Spacer(Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(defaultCategories) { cat ->
                        CategoryItem(cat, uiState.selectedCategory == cat.id, onClick = {
                            viewModel.selectCategory(if (uiState.selectedCategory == cat.id) null else cat.id)
                        })
                    }
                }
            }
        }

        if (uiState.sponsoredProducts.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Campaign, "Sponsored", tint = Color(0xFF7C4DFF), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sponsored", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(uiState.sponsoredProducts) { product ->
                        SponsoredProductCard(
                            product = product,
                            onClick = { navController.navigate(Screen.ProductDetail.createRoute(product.id)) },
                            onFavoriteClick = { viewModel.toggleFavorite(product.id) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        if (uiState.featuredProducts.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Bolt, "Boosted", tint = Color(0xFFFFB300), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Boosted Listings", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Text("See all", color = SddPink, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { })
                }
            }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(uiState.featuredProducts) { product ->
                        BoostedProductCard(
                            product = product,
                            onClick = { navController.navigate(Screen.ProductDetail.createRoute(product.id)) },
                            onFavoriteClick = { viewModel.toggleFavorite(product.id) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("All Products", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("${products.itemCount} items", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        item {
            when {
                products.loadState.refresh is LoadState.Loading -> {
                    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.height(400.dp), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(6) { ShimmerProductCard(Modifier.fillMaxWidth()) }
                    }
                }
                products.loadState.refresh is LoadState.Error -> {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        ErrorMessage("Failed to load products") { products.retry() }
                    }
                }
                products.itemCount == 0 -> {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        EmptyState("No products found", "Try a different category or search")
                    }
                }
                else -> {
                    LazyVerticalGrid(columns = GridCells.Fixed(2), modifier = Modifier.height(1200.dp), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(products.itemCount) { index ->
                            products[index]?.let { p ->
                                ProductCard(
                                    imageUrl = p.images.firstOrNull(), title = p.title, price = p.price,
                                    sellerName = p.seller?.fullName ?: "Seller", sellerAvatarUrl = p.seller?.avatarUrl,
                                    isVerified = p.seller?.isVerified == true, isFavorite = false,
                                    onFavoriteClick = { viewModel.toggleFavorite(p.id) },
                                    onClick = { navController.navigate(Screen.ProductDetail.createRoute(p.id)) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun SponsoredProductCard(product: Product, onClick: () -> Unit, onFavoriteClick: () -> Unit) {
    Box(modifier = Modifier.width(160.dp)) {
        ProductCard(
            imageUrl = product.images.firstOrNull(), title = product.title, price = product.price,
            sellerName = product.seller?.fullName ?: "Seller", sellerAvatarUrl = product.seller?.avatarUrl,
            isVerified = product.seller?.isVerified == true, isFavorite = false,
            onFavoriteClick = onFavoriteClick, onClick = onClick,
            modifier = Modifier.width(160.dp)
        )
        Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
            color = Color(0xFF7C4DFF),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                "Sponsored", color = Color.White, fontSize = 9.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun BoostedProductCard(product: Product, onClick: () -> Unit, onFavoriteClick: () -> Unit) {
    Box(modifier = Modifier.width(160.dp)) {
        ProductCard(
            imageUrl = product.images.firstOrNull(), title = product.title, price = product.price,
            sellerName = product.seller?.fullName ?: "Seller", sellerAvatarUrl = product.seller?.avatarUrl,
            isVerified = product.seller?.isVerified == true, isFavorite = false,
            onFavoriteClick = onFavoriteClick, onClick = onClick,
            modifier = Modifier.width(160.dp)
        )
        Surface(
            modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
            color = Color(0xFFFFB300),
            shape = RoundedCornerShape(6.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Bolt, "Boost", tint = Color.White, modifier = Modifier.size(10.dp))
                Spacer(Modifier.width(2.dp))
                Text("Boosted", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearMeTab(
    uiState: com.sdd.marketplace.feature.home.viewmodel.HomeUiState,
    viewModel: HomeViewModel,
    navController: NavController,
    locationGranted: Boolean,
    onRequestPermission: () -> Unit = {}
) {
    LazyColumn(Modifier.fillMaxSize()) {
        if (!locationGranted) {
            item {
                Card(Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = SddPink.copy(alpha = 0.08f))) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.LocationOff, "No Location", tint = SddPink, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Location Required", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Allow location access to discover products near you.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.showLocationRationale()
                                onRequestPermission()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SddPink),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.MyLocation, "Location", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Allow Location Access")
                        }
                    }
                }
            }
        } else {
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MyLocation, "Location", tint = SddPink, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Products near you", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.weight(1f))
                        Text("${uiState.nearMeRadius.toInt()} km", color = SddPink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Radius: ${uiState.nearMeRadius.toInt()} km", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Slider(
                        value = uiState.nearMeRadius, onValueChange = { viewModel.onNearMeRadiusChanged(it) },
                        valueRange = 5f..100f, steps = 18,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(thumbColor = SddPink, activeTrackColor = SddPink)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("5 km", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("100 km", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (uiState.nearbyProducts.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.SearchOff, "None", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("No listings nearby", fontWeight = FontWeight.Bold)
                            Text("Try increasing the radius", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                item {
                    Text(
                        "${uiState.nearbyProducts.size} listings within ${uiState.nearMeRadius.toInt()} km",
                        Modifier.padding(horizontal = 16.dp, vertical = 4.dp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.height((((uiState.nearbyProducts.size + 1) / 2) * 260 + 40).dp.coerceAtMost(1200.dp)),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.nearbyProducts.size) { index ->
                            val product = uiState.nearbyProducts[index]
                            ProductCard(
                                imageUrl = product.images.firstOrNull(), title = product.title, price = product.price,
                                sellerName = product.seller?.fullName ?: "Seller", sellerAvatarUrl = product.seller?.avatarUrl,
                                isVerified = product.seller?.isVerified == true, isFavorite = false,
                                onFavoriteClick = { viewModel.toggleFavorite(product.id) },
                                onClick = { navController.navigate(Screen.ProductDetail.createRoute(product.id)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun FeaturedTab(
    uiState: com.sdd.marketplace.feature.home.viewmodel.HomeUiState,
    viewModel: HomeViewModel,
    navController: NavController
) {
    LazyColumn(Modifier.fillMaxSize()) {
        item { Spacer(Modifier.height(8.dp)) }

        if (uiState.sponsoredProducts.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.background(
                            Brush.horizontalGradient(listOf(Color(0xFF7C4DFF), Color(0xFFB388FF))),
                            RoundedCornerShape(8.dp)
                        ).padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("SPONSORED", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Premium Listings", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(8.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(uiState.sponsoredProducts) { product ->
                        SponsoredProductCard(
                            product = product,
                            onClick = { navController.navigate(Screen.ProductDetail.createRoute(product.id)) },
                            onFavoriteClick = { viewModel.toggleFavorite(product.id) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        if (uiState.featuredProducts.isEmpty() && uiState.sponsoredProducts.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SddPink)
                        Spacer(Modifier.height(16.dp))
                        Text("Loading featured products...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else if (uiState.featuredProducts.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.background(
                            Brush.horizontalGradient(listOf(Color(0xFFFFB300), Color(0xFFFF8F00))),
                            RoundedCornerShape(8.dp)
                        ).padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Bolt, "Boost", tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("BOOSTED", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Boosted Products", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.height((((uiState.featuredProducts.size + 1) / 2) * 280 + 40).dp.coerceAtMost(1200.dp)),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.featuredProducts.size) { index ->
                        val product = uiState.featuredProducts[index]
                        BoostedProductCard(
                            product = product,
                            onClick = { navController.navigate(Screen.ProductDetail.createRoute(product.id)) },
                            onFavoriteClick = { viewModel.toggleFavorite(product.id) }
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun TrendingTab(
    uiState: com.sdd.marketplace.feature.home.viewmodel.HomeUiState,
    viewModel: HomeViewModel,
    navController: NavController
) {
    LazyColumn(Modifier.fillMaxSize()) {
        item { Spacer(Modifier.height(8.dp)) }

        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.TrendingUp, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Trending Now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                TextButton(onClick = { viewModel.refreshRotation() }) {
                    Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(14.dp), tint = SddPink)
                    Spacer(Modifier.width(4.dp))
                    Text("Refresh", color = SddPink, fontSize = 12.sp)
                }
            }
        }

        item {
            Text(
                "Listings rotate every 30 minutes — fresh picks just for you",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )
            Spacer(Modifier.height(8.dp))
        }

        if (uiState.rotationProducts.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = SddPink)
                        Text("Loading trending products…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.height(
                        (((uiState.rotationProducts.size + 1) / 2) * 280 + 40).dp.coerceAtMost(2000.dp)
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    userScrollEnabled = false
                ) {
                    items(uiState.rotationProducts.size) { index ->
                        val product = uiState.rotationProducts[index]
                        BoostedProductCard(
                            product = product,
                            onClick = { navController.navigate(Screen.ProductDetail.createRoute(product.id)) },
                            onFavoriteClick = { viewModel.toggleFavorite(product.id) }
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun CategoryItem(category: Category, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(if (isSelected) SddPink else SddPink.copy(alpha = 0.1f)).clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(getCategoryIcon(category.name), category.name, tint = if (isSelected) Color.White else SddPink, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(category.name, fontSize = 11.sp, color = if (isSelected) SddPink else MaterialTheme.colorScheme.onBackground)
    }
}

private fun getCategoryIcon(name: String): ImageVector = when (name.lowercase()) {
    "popular" -> Icons.Outlined.Star
    "women" -> Icons.Outlined.Checkroom
    "men" -> Icons.Outlined.Person
    "home" -> Icons.Outlined.Home
    "beauty" -> Icons.Outlined.Favorite
    "electronics" -> Icons.Outlined.PhoneAndroid
    "furniture" -> Icons.Outlined.Weekend
    "vehicles", "cars" -> Icons.Outlined.DirectionsCar
    "real_estate", "property" -> Icons.Outlined.House
    "rentals", "rent", "apartments" -> Icons.Outlined.VpnKey
    "sports" -> Icons.Outlined.SportsBasketball
    "gaming" -> Icons.Outlined.SportsEsports
    "books" -> Icons.Outlined.MenuBook
    "toys", "kids" -> Icons.Outlined.SmartToy
    "food", "drinks" -> Icons.Outlined.Restaurant
    "health" -> Icons.Outlined.LocalHospital
    "pets" -> Icons.Outlined.Pets
    "services" -> Icons.Outlined.MiscellaneousServices
    "jobs" -> Icons.Outlined.Work
    "art", "crafts" -> Icons.Outlined.Palette
    "music" -> Icons.Outlined.MusicNote
    "travel" -> Icons.Outlined.Flight
    "garden" -> Icons.Outlined.LocalFlorist
    else -> Icons.Outlined.GridView
}

private val defaultCategories = listOf(
    Category("popular", "Popular", "star", null),
    Category("women", "Women", "dress", null),
    Category("men", "Men", "tshirt", null),
    Category("electronics", "Electronics", "phone", null),
    Category("beauty", "Beauty", "heart", null),
    Category("home", "Home", "home", null),
    Category("furniture", "Furniture", "chair", null),
    Category("vehicles", "Vehicles", "car", null),
    Category("real_estate", "Real Estate", "house", null),
    Category("rentals", "Rentals", "key", null),
    Category("sports", "Sports", "sports", null),
    Category("gaming", "Gaming", "games", null),
    Category("books", "Books", "book", null),
    Category("toys", "Toys & Kids", "toys", null),
    Category("food", "Food & Drinks", "food", null),
    Category("health", "Health", "health", null),
    Category("pets", "Pets", "pets", null),
    Category("services", "Services", "work", null),
    Category("jobs", "Jobs", "briefcase", null),
    Category("art", "Art & Crafts", "art", null),
    Category("music", "Music", "music", null),
    Category("travel", "Travel", "travel", null),
    Category("garden", "Garden", "garden", null),
    Category("all", "All", "grid", null)
)
