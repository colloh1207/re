package com.sdd.marketplace.feature.profile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sdd.marketplace.core.ui.theme.*

data class AchievementItem(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    val date: String,
    val isUnlocked: Boolean,
    val progress: Float = 1f,
    val progressText: String = "",
    val category: String = "General"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(navController: NavController) {
    var selectedCategory by remember { mutableStateOf("All") }

    val allAchievements = listOf(
        AchievementItem("1", "🏆", "Top Seller", "Achieved 50+ sales this month", "May 2024", true, category = "Sales"),
        AchievementItem("2", "⚡", "Fast Responder", "Replied to all messages within 1 hour", "Apr 2024", true, category = "Service"),
        AchievementItem("3", "💯", "100 Sales Club", "Completed 100 successful sales", "Mar 2024", true, category = "Sales"),
        AchievementItem("4", "⭐", "5 Star Seller", "Maintained 4.9+ rating for 30 days", "Feb 2024", true, category = "Ratings"),
        AchievementItem("5", "🌟", "Verified Seller", "Completed KYC verification", "Jan 2024", true, category = "Trust"),
        AchievementItem("6", "🎁", "First Sale", "Completed your first sale", "Dec 2023", true, category = "Milestones"),
        AchievementItem("7", "🤝", "Community Pillar", "Helped 10 new sellers", "Nov 2023", true, category = "Community"),
        AchievementItem("8", "📸", "Photo Pro", "Posted 50 high-quality listings", "Oct 2023", true, category = "Listings"),
        AchievementItem("9", "🦋", "Social Butterfly", "Got 500 followers", "Sep 2023", true, category = "Social"),
        AchievementItem("10", "💎", "Elite Seller", "Complete 500 sales", "Locked", false, progress = 0.72f, progressText = "360/500 sales", category = "Sales"),
        AchievementItem("11", "🚀", "Power Seller", "Earn ₹1 Lakh in sales", "Locked", false, progress = 0.65f, progressText = "₹65,000 / ₹1,00,000", category = "Sales"),
        AchievementItem("12", "👑", "Marketplace Legend", "1000 total sales", "Locked", false, progress = 0.36f, progressText = "360/1000 sales", category = "Milestones"),
        AchievementItem("13", "🌍", "Global Seller", "Sell to buyers in 5+ cities", "Locked", false, progress = 0.4f, progressText = "2/5 cities", category = "Sales"),
        AchievementItem("14", "❤️", "Fan Favorite", "Get 1000 followers", "Locked", false, progress = 0.53f, progressText = "530/1000 followers", category = "Social"),
    )

    val categories = listOf("All", "Sales", "Service", "Ratings", "Trust", "Milestones", "Community", "Listings", "Social")
    val filteredAchievements = if (selectedCategory == "All") allAchievements else allAchievements.filter { it.category == selectedCategory }
    val unlocked = filteredAchievements.count { it.isUnlocked }
    val total = filteredAchievements.size

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } },
                title = { Text("Achievements", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Box(
                    Modifier.fillMaxWidth().background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(SddPink.copy(alpha = 0.12f), MaterialTheme.colorScheme.surface))
                    ).padding(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(Modifier.size(80.dp).clip(CircleShape).background(SddPink.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                            Text("🏆", fontSize = 40.sp)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("$unlocked of ${allAchievements.size} Achieved", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { allAchievements.count { it.isUnlocked }.toFloat() / allAchievements.size },
                            modifier = Modifier.fillMaxWidth(0.7f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                            color = SddPink
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("${(allAchievements.count { it.isUnlocked }.toFloat() / allAchievements.size * 100).toInt()}% Complete", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${allAchievements.count { it.isUnlocked }}", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = SddPink)
                                Text("Unlocked", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Box(Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.outline.copy(0.3f)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${allAchievements.count { !it.isUnlocked }}", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Remaining", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Box(Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.outline.copy(0.3f)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Gold", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = StarYellow)
                                Text("Tier", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item {
                ScrollableTabRow(selectedTabIndex = categories.indexOf(selectedCategory), edgePadding = 16.dp, containerColor = MaterialTheme.colorScheme.background, contentColor = SddPink) {
                    categories.forEach { cat ->
                        Tab(selected = selectedCategory == cat, onClick = { selectedCategory = cat }, text = { Text(cat) })
                    }
                }
            }

            item {
                if (filteredAchievements.any { it.isUnlocked }) {
                    Text("Unlocked", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                }
            }

            items(filteredAchievements.filter { it.isUnlocked }) { achievement ->
                AchievementCard(achievement)
            }

            item {
                if (filteredAchievements.any { !it.isUnlocked }) {
                    Text("In Progress", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                }
            }

            items(filteredAchievements.filter { !it.isUnlocked }) { achievement ->
                AchievementCard(achievement)
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun AchievementCard(achievement: AchievementItem) {
    Card(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).alpha(if (achievement.isUnlocked) 1f else 0.85f),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked) SddPink.copy(alpha = 0.06f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(56.dp).clip(CircleShape).background(if (achievement.isUnlocked) SddPink.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (achievement.isUnlocked) {
                    Text(achievement.emoji, fontSize = 28.sp)
                } else {
                    Text(achievement.emoji, fontSize = 28.sp, modifier = Modifier.alpha(0.4f))
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(achievement.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (achievement.isUnlocked) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Filled.CheckCircle, "Unlocked", tint = SuccessGreen, modifier = Modifier.size(16.dp))
                    }
                }
                Text(achievement.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (achievement.isUnlocked) {
                    Spacer(Modifier.height(4.dp))
                    Text(achievement.date, fontSize = 11.sp, color = SddPink)
                } else {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { achievement.progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = SddPink)
                    Spacer(Modifier.height(4.dp))
                    Text(achievement.progressText, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
