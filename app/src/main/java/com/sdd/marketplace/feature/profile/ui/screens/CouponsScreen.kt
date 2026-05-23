package com.sdd.marketplace.feature.profile.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sdd.marketplace.core.ui.theme.SddPink
import com.sdd.marketplace.domain.model.Coupon
import com.sdd.marketplace.domain.model.CouponDiscountType
import com.sdd.marketplace.feature.profile.viewmodel.CouponViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponsScreen(navController: NavController, viewModel: CouponViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } },
                title = { Text("My Coupons", fontWeight = FontWeight.Bold) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Valid") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("All") })
            }

            val displayCoupons = if (selectedTab == 0) uiState.validCoupons else uiState.allCoupons

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SddPink) }
            } else if (displayCoupons.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.LocalOffer, "No Coupons", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("No ${if (selectedTab == 0) "valid" else ""} coupons yet", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Mark orders as received to earn discount coupons!", textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(displayCoupons) { coupon ->
                        CouponCard(coupon = coupon, onCopy = {
                            clipboardManager.setText(AnnotatedString(coupon.code))
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun CouponCard(coupon: Coupon, onCopy: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (coupon.isUsed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(0.dp)) {
            // Left strip
            Box(Modifier.width(8.dp).fillMaxHeight(),
                contentAlignment = Alignment.Center) {
                Surface(color = if (coupon.isUsed) MaterialTheme.colorScheme.outline else SddPink, modifier = Modifier.fillMaxSize()) {}
            }
            Column(Modifier.weight(1f).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.LocalOffer, "Coupon", tint = if (coupon.isUsed) MaterialTheme.colorScheme.onSurfaceVariant else SddPink, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(coupon.description, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(8.dp), color = if (coupon.isUsed) MaterialTheme.colorScheme.surfaceVariant else SddPink.copy(alpha = 0.1f)) {
                        Text(coupon.code, Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            fontWeight = FontWeight.ExtraBold, fontSize = 18.sp,
                            color = if (coupon.isUsed) MaterialTheme.colorScheme.onSurfaceVariant else SddPink)
                    }
                    Spacer(Modifier.width(8.dp))
                    if (!coupon.isUsed) {
                        IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Outlined.ContentCopy, "Copy", tint = SddPink, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.End) {
                        val discountText = when (coupon.discountType) {
                            CouponDiscountType.PERCENTAGE -> "${coupon.discountValue.toInt()}% OFF"
                            CouponDiscountType.FIXED -> "₹${coupon.discountValue.toInt()} OFF"
                        }
                        Text(discountText, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp,
                            color = if (coupon.isUsed) MaterialTheme.colorScheme.onSurfaceVariant else SddPink)
                        coupon.maxDiscount?.let { Text("Max ₹${it.toInt()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Min order: ₹${coupon.minOrderValue.toInt()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (coupon.isUsed) {
                        Text("USED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Expires: ${coupon.expiresAt.take(10)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
