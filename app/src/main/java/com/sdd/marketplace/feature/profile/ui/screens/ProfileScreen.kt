package com.sdd.marketplace.feature.profile.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.sdd.marketplace.domain.model.Product
import com.sdd.marketplace.domain.model.Review
import com.sdd.marketplace.domain.model.User
import com.sdd.marketplace.feature.profile.viewmodel.ProfileEvent
import com.sdd.marketplace.feature.profile.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val products = viewModel.userProducts.collectAsLazyPagingItems()
    val soldProducts = viewModel.soldProducts.collectAsLazyPagingItems()
    val savedItems = viewModel.savedItems.collectAsLazyPagingItems()
    var showMenuSheet by remember { mutableStateOf(false) }
    var showBlockConfirm by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileEvent.NavigateToChat -> navController.navigate(Screen.ChatDetail.createRoute(event.chatId))
                is ProfileEvent.NavigateToLogin -> navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
                else -> {}
            }
        }
    }

    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.uploadAvatar(it) }
    }

    if (showBlockConfirm) {
        AlertDialog(
            onDismissRequest = { showBlockConfirm = false },
            icon = { Icon(Icons.Filled.Block, "Block", tint = MaterialTheme.colorScheme.error) },
            title = { Text("Block User?") },
            text = { Text("${uiState.user?.fullName} won't be able to message you or see your listings.") },
            confirmButton = {
                Button(onClick = { viewModel.blockUser(); showBlockConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Block") }
            },
            dismissButton = { TextButton(onClick = { showBlockConfirm = false }) { Text("Cancel") } }
        )
    }

    if (showReportDialog) {
        ReportUserDialogProfile(
            onReport = { cat, desc -> viewModel.reportUser(cat, desc); showReportDialog = false },
            onDismiss = { showReportDialog = false }
        )
    }

    if (showMenuSheet && uiState.isCurrentUser) {
        ProfileMenuSheet(
            navController = navController,
            onDismiss = { showMenuSheet = false }
        )
    }

    Column(Modifier.fillMaxSize()) {
        if (!uiState.isCurrentUser) {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } },
                title = { Text(uiState.user?.fullName ?: "Profile", fontWeight = FontWeight.Bold) },
                actions = {
                    Box {
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(leadingIcon = { Icon(Icons.Filled.Block, "Block", tint = MaterialTheme.colorScheme.error) },
                                text = { Text("Block User", color = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; showBlockConfirm = true })
                            DropdownMenuItem(leadingIcon = { Icon(Icons.Filled.Flag, "Report", tint = MaterialTheme.colorScheme.error) },
                                text = { Text("Report User", color = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; showReportDialog = true })
                        }
                    }
                }
            )
        } else {
            TopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) { Icon(Icons.Outlined.Settings, "Settings") }
                    IconButton(onClick = { showMenuSheet = true }) { Icon(Icons.Filled.Menu, "Menu") }
                }
            )
        }

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SddPink)
            }
            return
        }

        LazyColumn(Modifier.fillMaxSize()) {
            // Profile Header
            item {
                Box(
                    Modifier.fillMaxWidth()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(SddPink.copy(alpha = 0.15f), MaterialTheme.colorScheme.surface)
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box {
                            AsyncImage(
                                model = uiState.user?.avatarUrl,
                                contentDescription = uiState.user?.fullName,
                                modifier = Modifier.size(96.dp).clip(CircleShape).border(3.dp, SddPink.copy(alpha = 0.5f), CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            if (uiState.isCurrentUser) {
                                Box(
                                    Modifier.size(30.dp).clip(CircleShape).background(SddPink)
                                        .align(Alignment.BottomEnd).clickable { avatarLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Filled.CameraAlt, "Change Photo", tint = Color.White, modifier = Modifier.size(16.dp)) }
                            }
                            if (uiState.user?.isOnline == true) {
                                Box(Modifier.size(14.dp).clip(CircleShape).background(OnlineGreen).border(2.dp, Color.White, CircleShape).align(Alignment.TopEnd))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(uiState.user?.fullName ?: "User", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            if (uiState.user?.isVerified == true) {
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Filled.Verified, "Verified", tint = SddPink, modifier = Modifier.size(20.dp))
                            }
                        }
                        uiState.user?.let { user ->
                            if (!user.email.isNullOrBlank()) {
                                Text("@${user.email?.substringBefore("@") ?: user.fullName.lowercase().replace(" ", "_")}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StarRating(uiState.user?.rating ?: 0.0, size = 14.dp)
                            Spacer(Modifier.width(4.dp))
                            Text("${String.format("%.1f", uiState.user?.rating ?: 0.0)} (${uiState.user?.reviewCount ?: 0} Reviews)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        uiState.user?.bio?.let { bio ->
                            Spacer(Modifier.height(8.dp))
                            Text(bio, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 3)
                        }
                        uiState.user?.location?.let { location ->
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.LocationOn, "Location", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(location, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        uiState.user?.joinedAt?.let { joined ->
                            if (joined.isNotBlank()) {
                                Spacer(Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Outlined.CalendarMonth, "Joined", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Joined ${joined.take(7)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))

                        // Stats row
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            ProfileStatItem("${uiState.user?.productCount ?: 0}", "Listings",
                                onClick = { viewModel.selectTab(0) })
                            ProfileStatDivider()
                            ProfileStatItem(formatCount(uiState.user?.followerCount ?: 0), "Followers",
                                onClick = { navController.navigate(Screen.Followers.createRoute(viewModel.userId, "followers")) })
                            ProfileStatDivider()
                            ProfileStatItem(formatCount(uiState.user?.followingCount ?: 0), "Following",
                                onClick = { navController.navigate(Screen.Followers.createRoute(viewModel.userId, "following")) })
                            ProfileStatDivider()
                            ProfileStatItem("${uiState.user?.soldCount ?: 0}", "Sold",
                                onClick = { viewModel.selectTab(1) })
                        }
                        Spacer(Modifier.height(16.dp))

                        // Action buttons
                        if (uiState.isCurrentUser) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { navController.navigate(Screen.EditProfile.route) },
                                    border = BorderStroke(1.5.dp, SddPink),
                                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)
                                ) { Icon(Icons.Outlined.Edit, "Edit", tint = SddPink, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Edit Profile", color = SddPink) }
                                OutlinedButton(
                                    onClick = { navController.navigate(Screen.KycVerification.route) },
                                    border = BorderStroke(1.5.dp, if (uiState.user?.isVerified == true) SuccessGreen else SddPink),
                                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(if (uiState.user?.isVerified == true) Icons.Filled.Verified else Icons.Outlined.Shield, "Verify",
                                        tint = if (uiState.user?.isVerified == true) SuccessGreen else SddPink, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (uiState.user?.isVerified == true) "Verified" else "Get Verified",
                                        color = if (uiState.user?.isVerified == true) SuccessGreen else SddPink)
                                }
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { viewModel.followUnfollow() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (uiState.isFollowing) MaterialTheme.colorScheme.surfaceVariant else SddPink
                                    ),
                                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(if (uiState.isFollowing) Icons.Filled.PersonRemove else Icons.Filled.PersonAdd, "Follow",
                                        tint = if (uiState.isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (uiState.isFollowing) "Following" else "Follow",
                                        color = if (uiState.isFollowing) MaterialTheme.colorScheme.onSurfaceVariant else Color.White)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.messageUser() },
                                    border = BorderStroke(1.5.dp, SddPink),
                                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Outlined.Message, "Message", tint = SddPink, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Message", color = SddPink)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { navController.navigate(Screen.SellerShop.createRoute(viewModel.userId)) },
                                border = BorderStroke(1.dp, SddPink),
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Outlined.Store, "Shop", tint = SddPink, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("View Seller Shop", color = SddPink)
                            }
                        }
                    }
                }
            }

            // Seller Stats (own profile)
            if (uiState.isCurrentUser) {
                item {
                    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("My Stats", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth()) {
                                StatCard("Total Sales", "₹${formatCount(uiState.totalSales.toInt())}", "+18% this month", SuccessGreen, Modifier.weight(1f))
                                Spacer(Modifier.width(8.dp))
                                StatCard("Items Sold", "${uiState.user?.soldCount ?: 0}", "+24% this month", SddPink, Modifier.weight(1f))
                                Spacer(Modifier.width(8.dp))
                                StatCard("Response Rate", "${uiState.user?.responseRate ?: 0}%", "Very Fast", SuccessGreen, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Achievements preview
            if (uiState.isCurrentUser && uiState.achievements.isNotEmpty()) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Achievements", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            TextButton(onClick = { navController.navigate(Screen.Achievements.route) }) { Text("See All", color = SddPink) }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(uiState.achievements.take(4)) { achievement ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
                                    Box(Modifier.size(56.dp).clip(CircleShape).background(SddPink.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                                        Text(achievement.emoji, fontSize = 28.sp)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(achievement.title, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Medium, maxLines = 2)
                                    Text(achievement.date, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }

            // Tab bar
            item {
                val tabs = if (uiState.isCurrentUser)
                    listOf("Listings", "Sold", "Saved", "Reviews")
                else
                    listOf("Shop", "Reviews")

                ScrollableTabRow(
                    selectedTabIndex = uiState.selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = SddPink,
                    edgePadding = 16.dp
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = uiState.selectedTab == index,
                            onClick = { viewModel.selectTab(index) },
                            text = { Text(tab, fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }
            }

            // Tab content
            when (uiState.selectedTab) {
                0 -> {
                    // Listings tab
                    if (products.loadState.refresh is LoadState.Loading) {
                        items(4) { ShimmerProductCard(Modifier.fillMaxWidth().padding(4.dp)) }
                    } else if (products.itemCount == 0) {
                        item {
                            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                EmptyState(
                                    title = if (uiState.isCurrentUser) "No listings yet" else "No products",
                                    subtitle = if (uiState.isCurrentUser) "Tap + to post your first item" else "This seller has no products yet"
                                )
                            }
                        }
                    } else {
                        item {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.heightIn(min = 200.dp, max = 1200.dp),
                                contentPadding = PaddingValues(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(products.itemCount) { index ->
                                    products[index]?.let { product ->
                                        ProfileProductCard(
                                            product = product,
                                            isOwner = uiState.isCurrentUser,
                                            onClick = { navController.navigate(Screen.ProductDetail.createRoute(product.id)) },
                                            onEdit = { navController.navigate(Screen.EditProduct.createRoute(product.id)) },
                                            onDelete = { viewModel.deleteProduct(product.id) },
                                            onMarkSold = { viewModel.markAsSold(product.id) },
                                            onArchive = { viewModel.archiveProduct(product.id) },
                                            onBoost = { navController.navigate(Screen.Boost.createRoute(product.id)) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Sold tab
                    if (uiState.isCurrentUser) {
                        if (soldProducts.itemCount == 0) {
                            item {
                                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                    EmptyState("No sold items", "Your sold items will appear here")
                                }
                            }
                        } else {
                            item {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2), modifier = Modifier.heightIn(min = 200.dp, max = 1200.dp),
                                    contentPadding = PaddingValues(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(soldProducts.itemCount) { index ->
                                        soldProducts[index]?.let { product ->
                                            ProductCard(imageUrl = product.images.firstOrNull(), title = product.title, price = product.price,
                                                sellerName = uiState.user?.fullName ?: "", sellerAvatarUrl = uiState.user?.avatarUrl,
                                                isVerified = uiState.user?.isVerified == true, isFavorite = false, onFavoriteClick = {},
                                                onClick = { navController.navigate(Screen.ProductDetail.createRoute(product.id)) })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Saved items (only for current user)
                    if (uiState.isCurrentUser) {
                        if (savedItems.itemCount == 0) {
                            item {
                                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                    EmptyState("No saved items", "Items you wishlist will appear here")
                                }
                            }
                        } else {
                            item {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2), modifier = Modifier.heightIn(min = 200.dp, max = 1200.dp),
                                    contentPadding = PaddingValues(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(savedItems.itemCount) { index ->
                                        savedItems[index]?.let { product ->
                                            ProductCard(imageUrl = product.images.firstOrNull(), title = product.title, price = product.price,
                                                sellerName = product.seller?.fullName ?: "", sellerAvatarUrl = product.seller?.avatarUrl,
                                                isVerified = product.seller?.isVerified == true, isFavorite = true, onFavoriteClick = {},
                                                onClick = { navController.navigate(Screen.ProductDetail.createRoute(product.id)) })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    if (uiState.reviews.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                EmptyState("No reviews yet", "Reviews will appear here")
                            }
                        }
                    }
                    items(uiState.reviews) { review ->
                        ReviewCard(review, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun ProfileStatItem(value: String, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 12.dp)
    ) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ProfileStatDivider() {
    Box(Modifier.height(32.dp).width(1.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
}

@Composable
fun StatCard(label: String, value: String, sub: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f)), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
            Text(sub, fontSize = 10.sp, color = color.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun ProfileProductCard(product: Product, isOwner: Boolean, onClick: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit, onMarkSold: () -> Unit, onArchive: () -> Unit, onBoost: () -> Unit = {}) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = { Icon(Icons.Filled.Delete, "Delete", tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Product?") },
            text = { Text("This will permanently delete '${product.title}'. This action cannot be undone.") },
            confirmButton = { Button(onClick = { onDelete(); showDeleteConfirm = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") } }
        )
    }

    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Column {
            Box {
                AsyncImage(model = product.images.firstOrNull(), contentDescription = product.title, modifier = Modifier.fillMaxWidth().height(130.dp), contentScale = ContentScale.Crop)
                if (product.isSold) {
                    Box(Modifier.fillMaxWidth().height(130.dp).background(Color.Black.copy(alpha = 0.4f))) {
                        Surface(color = MaterialTheme.colorScheme.error, shape = RoundedCornerShape(6.dp), modifier = Modifier.align(Alignment.Center)) {
                            Text("SOLD", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }
                if (isOwner) {
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp).align(Alignment.TopEnd).padding(4.dp)) {
                            Icon(Icons.Filled.MoreVert, "More", tint = Color.White, modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape).padding(2.dp).size(18.dp))
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(leadingIcon = { Icon(Icons.Outlined.Edit, "Edit", tint = SddPink) }, text = { Text("Edit", color = SddPink) }, onClick = { showMenu = false; onEdit() })
                            if (!product.isSold) {
                                DropdownMenuItem(leadingIcon = { Icon(Icons.Outlined.CheckCircle, "Sold", tint = SuccessGreen) }, text = { Text("Mark as Sold", color = SuccessGreen) }, onClick = { showMenu = false; onMarkSold() })
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Filled.Rocket, "Boost", tint = androidx.compose.ui.graphics.Color(0xFFFF9800)) },
                                    text = { Text("Boost Listing", color = androidx.compose.ui.graphics.Color(0xFFFF9800)) },
                                    onClick = { showMenu = false; onBoost() }
                                )
                            }
                            DropdownMenuItem(leadingIcon = { Icon(Icons.Outlined.Archive, "Archive") }, text = { Text("Archive") }, onClick = { showMenu = false; onArchive() })
                            DropdownMenuItem(leadingIcon = { Icon(Icons.Outlined.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }, text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = { showMenu = false; showDeleteConfirm = true })
                        }
                    }
                }
            }
            Column(Modifier.padding(8.dp)) {
                Text(product.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("₹${String.format("%.0f", product.price)}", color = SddPink, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Visibility, "Views", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(" ${product.viewCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.FavoriteBorder, "Favorites", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(" ${product.favoriteCount}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (isOwner) {
                        Surface(color = if (product.isSold) MaterialTheme.colorScheme.errorContainer else SuccessGreen.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                            Text(if (product.isSold) "Sold" else "Active", fontSize = 10.sp, color = if (product.isSold) MaterialTheme.colorScheme.error else SuccessGreen, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewCard(review: Review, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = review.reviewer?.avatarUrl, contentDescription = null, modifier = Modifier.size(36.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(review.reviewer?.fullName ?: "User", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    Text(review.createdAt.take(10), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StarRating(review.rating.toDouble(), size = 14.dp)
            }
            Spacer(Modifier.height(8.dp))
            Text(review.comment, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (review.isVerifiedPurchase) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Verified, "Verified", tint = SuccessGreen, modifier = Modifier.size(12.dp))
                    Text(" Verified Purchase", fontSize = 11.sp, color = SuccessGreen)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileMenuSheet(navController: NavController, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
            Text("My Account", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
            Divider()
            val items = listOf(
                Triple(Icons.Outlined.Person, "View Full Profile", { onDismiss() }),
                Triple(Icons.Outlined.Store, "My Shop", { navController.navigate(Screen.SellerShop.createRoute("")); onDismiss() }),
                Triple(Icons.Outlined.Sell, "My Listings", { onDismiss() }),
                Triple(Icons.Outlined.CheckCircle, "Mark as Sold", { onDismiss() }),
                Triple(Icons.Outlined.EmojiEvents, "Achievements", { navController.navigate(Screen.Achievements.route); onDismiss() }),
                Triple(Icons.Outlined.ShoppingBag, "Orders & Purchases", { navController.navigate(Screen.Orders.route); onDismiss() }),
                Triple(Icons.Outlined.Favorite, "Saved Items", { navController.navigate(Screen.Wishlist.route); onDismiss() }),
                Triple(Icons.Outlined.LocalOffer, "Offers & Coupons", { onDismiss() }),
                Triple(Icons.Outlined.StarBorder, "My Reviews", { navController.navigate(Screen.MyReviews.route); onDismiss() }),
                Triple(Icons.Outlined.AccountBalanceWallet, "Wallet", { navController.navigate(Screen.Wallet.route); onDismiss() }),
                Triple(Icons.Outlined.Settings, "Settings", { navController.navigate(Screen.Settings.route); onDismiss() }),
                Triple(Icons.Outlined.Help, "Help & Support", { navController.navigate(Screen.HelpSupport.route); onDismiss() }),
                Triple(Icons.Outlined.CardGiftcard, "Invite & Earn", { navController.navigate(Screen.InviteEarn.route); onDismiss() }),
                Triple(Icons.Outlined.Send, "Telegram Bot", { navController.navigate(Screen.TelegramBot.route); onDismiss() })
            )
            items.forEach { (icon, label, action) ->
                ListItem(
                    headlineContent = { Text(label) },
                    leadingContent = { Icon(icon, label, tint = SddPink) },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, "") },
                    modifier = Modifier.clickable { action() }
                )
                Divider(Modifier.padding(horizontal = 56.dp))
            }
        }
    }
}

@Composable
fun ReportUserDialogProfile(
    onReport: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedReason by remember { mutableStateOf("Spam") }
    var description by remember { mutableStateOf("") }
    val reasons = listOf("Spam", "Fake Profile", "Inappropriate Content", "Scam", "Harassment", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Flag, "Report", tint = MaterialTheme.colorScheme.error) },
        title = { Text("Report User") },
        text = {
            Column {
                reasons.forEach { reason ->
                    Row(Modifier.clickable { selectedReason = reason }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedReason == reason, onClick = { selectedReason = reason }, colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.error))
                        Text(reason, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(description, { description = it }, label = { Text("Additional details (optional)") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)
            }
        },
        confirmButton = {
            Button(onClick = { onReport(selectedReason, description) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Submit Report") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> "${String.format("%.1f", count / 1_000_000.0)}M"
    count >= 1_000 -> "${String.format("%.1f", count / 1_000.0)}K"
    else -> "$count"
}
