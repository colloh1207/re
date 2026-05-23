package com.sdd.marketplace.feature.followers.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.sdd.marketplace.core.navigation.Screen
import com.sdd.marketplace.core.ui.components.VerifiedBadge
import com.sdd.marketplace.core.ui.theme.SddPink
import com.sdd.marketplace.domain.model.User
import com.sdd.marketplace.feature.followers.viewmodel.FollowersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowersScreen(
    navController: NavController,
    viewModel: FollowersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } },
                title = { Text(uiState.userName, fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab, contentColor = SddPink) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0; viewModel.loadFollowers() }) {
                    Text("Followers (${uiState.followerCount})", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1; viewModel.loadFollowing() }) {
                    Text("Following (${uiState.followingCount})", modifier = Modifier.padding(12.dp))
                }
            }
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SddPink) }
            } else {
                val list = if (selectedTab == 0) uiState.followers else uiState.following
                if (list.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Group, "No users", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            Text(if (selectedTab == 0) "No followers yet" else "Not following anyone", fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    LazyColumn {
                        items(list, key = { it.id }) { user ->
                            FollowerItem(
                                user = user,
                                isFollowing = uiState.followingIds.contains(user.id),
                                onFollow = { viewModel.toggleFollow(user.id) },
                                onClick = { navController.navigate(Screen.Profile.createRoute(user.id)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FollowerItem(user: User, isFollowing: Boolean, onFollow: () -> Unit, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.avatarUrl, contentDescription = user.fullName,
            modifier = Modifier.size(48.dp).clip(CircleShape), contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(user.fullName, fontWeight = FontWeight.SemiBold)
                if (user.isVerified) { Spacer(Modifier.width(4.dp)); VerifiedBadge() }
            }
            Text("${user.followerCount} followers · ${user.productCount} listings", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(
            onClick = onFollow,
            border = androidx.compose.foundation.BorderStroke(1.dp, SddPink),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
        ) {
            Text(if (isFollowing) "Following" else "Follow", color = SddPink, fontSize = 12.sp)
        }
    }
}
