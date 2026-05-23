package com.sdd.marketplace.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdd.marketplace.core.navigation.Screen
import com.sdd.marketplace.core.ui.theme.SddPink

@Composable
fun SddBottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavBarItem(
            icon = Icons.Filled.Home, outlineIcon = Icons.Outlined.Home,
            label = "Home", isSelected = currentRoute == Screen.Home.route,
            onClick = { onNavigate(Screen.Home.route) }
        )
        NavBarItem(
            icon = Icons.Filled.ChatBubble, outlineIcon = Icons.Outlined.ChatBubble,
            label = "Chats", isSelected = currentRoute == Screen.Inbox.route,
            onClick = { onNavigate(Screen.Inbox.route) }
        )
        // Center Post FAB
        NavigationBarItem(
            icon = {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(SddPink),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Add, "Post", tint = Color.White, modifier = Modifier.size(28.dp)) }
            },
            label = { Text("Post", fontSize = 10.sp) },
            selected = currentRoute == Screen.PostProduct.route,
            onClick = { onNavigate(Screen.PostProduct.route) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
        )
        NavBarItem(
            icon = Icons.Filled.Person, outlineIcon = Icons.Outlined.Person,
            label = "Profile", isSelected = currentRoute?.startsWith("profile") == true,
            onClick = { onNavigate(Screen.Profile.createRoute()) }
        )
        NavBarItem(
            icon = Icons.Filled.Search, outlineIcon = Icons.Outlined.Search,
            label = "Search", isSelected = currentRoute == Screen.Search.route,
            onClick = { onNavigate(Screen.Search.route) }
        )
    }
}

@Composable
private fun RowScope.NavBarItem(
    icon: ImageVector, outlineIcon: ImageVector,
    label: String, isSelected: Boolean, onClick: () -> Unit
) {
    val color by animateColorAsState(
        targetValue = if (isSelected) SddPink else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMedium), label = "navColor"
    )
    NavigationBarItem(
        icon = { Icon(if (isSelected) icon else outlineIcon, label, tint = color) },
        label = { Text(label, color = color, fontSize = 10.sp) },
        selected = isSelected,
        onClick = onClick,
        colors = NavigationBarItemDefaults.colors(
            indicatorColor = SddPink.copy(alpha = 0.1f),
            selectedIconColor = SddPink,
            selectedTextColor = SddPink
        )
    )
}
