package com.sdd.marketplace.feature.orders.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import coil.compose.AsyncImage
import com.sdd.marketplace.core.ui.theme.SddPink
import com.sdd.marketplace.domain.model.*
import com.sdd.marketplace.feature.orders.viewmodel.OrderEvent
import com.sdd.marketplace.feature.orders.viewmodel.OrderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(navController: NavController, viewModel: OrderViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val order = uiState.selectedOrder
    var showCancelDialog by remember { mutableStateOf(false) }
    var showRefundDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is OrderEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is OrderEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
                else -> {}
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Order") },
            text = {
                Column {
                    Text("Please tell us why you're cancelling.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(cancelReason, { cancelReason = it }, label = { Text("Reason") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = { showCancelDialog = false; order?.let { viewModel.cancelOrder(it.id, cancelReason) } },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Cancel Order") }
            },
            dismissButton = { TextButton(onClick = { showCancelDialog = false }) { Text("Keep Order") } }
        )
    }

    if (showRefundDialog) {
        AlertDialog(
            onDismissRequest = { showRefundDialog = false },
            title = { Text("Request Refund") },
            text = {
                Column {
                    Text("Describe why you need a refund. Our team will review within 5 business days.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(cancelReason, { cancelReason = it }, label = { Text("Reason") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = { showRefundDialog = false; order?.let { viewModel.requestRefund(it.id, cancelReason) } },
                    colors = ButtonDefaults.buttonColors(containerColor = SddPink)) { Text("Submit Request") }
            },
            dismissButton = { TextButton(onClick = { showRefundDialog = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } },
                title = { Text("Order Details", fontWeight = FontWeight.Bold) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (order == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SddPink) }
        } else {
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
                // Order Header
                Card(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Order #${order.id.take(8).uppercase()}", fontWeight = FontWeight.Bold)
                                Text(order.createdAt.take(10), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            OrderStatusChip(order.status)
                        }
                    }
                }

                // Product Info
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = order.product?.images?.firstOrNull(), contentDescription = null,
                            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(order.product?.title ?: "Product", fontWeight = FontWeight.SemiBold)
                            Text("Quantity: ${order.quantity}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${String.format("%.0f", order.totalAmount)}", color = SddPink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }

                // Order Tracking Timeline
                uiState.tracking?.let { tracking ->
                    Spacer(Modifier.height(16.dp))
                    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Order Tracking", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            tracking.events.forEachIndexed { index, event ->
                                Row {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(Modifier.size(24.dp).clip(CircleShape).background(SddPink), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Filled.Check, "Done", tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                        if (index < tracking.events.size - 1) {
                                            Divider(Modifier.width(2.dp).height(40.dp), color = SddPink.copy(alpha = 0.3f))
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.padding(bottom = 16.dp)) {
                                        Text(event.status.label, fontWeight = FontWeight.SemiBold)
                                        Text(event.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(event.timestamp.take(16), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                // Shipping Address
                order.shippingAddress?.let { addr ->
                    Spacer(Modifier.height(16.dp))
                    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Row { Icon(Icons.Outlined.LocationOn, "Address", tint = SddPink); Spacer(Modifier.width(8.dp)); Text("Delivery Address", fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.height(8.dp))
                            Text(addr.fullName, fontWeight = FontWeight.Medium)
                            Text("${addr.addressLine1}${if (!addr.addressLine2.isNullOrBlank()) ", ${addr.addressLine2}" else ""}")
                            Text("${addr.city}, ${addr.state} ${addr.postalCode}")
                            Text(addr.country)
                            Text(addr.phone, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Payment Info
                Spacer(Modifier.height(16.dp))
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Payment Summary", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Item Total"); Text("₹${String.format("%.0f", order.unitPrice * order.quantity)}") }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Platform Fee"); Text("₹${String.format("%.0f", order.totalAmount - order.unitPrice * order.quantity)}") }
                        Divider(Modifier.padding(vertical = 8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Paid", fontWeight = FontWeight.Bold)
                            Text("₹${String.format("%.0f", order.totalAmount)}", fontWeight = FontWeight.Bold, color = SddPink)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Via ${order.paymentGateway?.label ?: "N/A"} · ${order.paymentStatus.label}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        order.paymentTransactionId?.let { Text("TXN: $it", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }

                // Actions
                Spacer(Modifier.height(16.dp))
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (order.status == OrderStatus.DELIVERED && order.paymentStatus != PaymentStatus.REFUNDED) {
                        Button(
                            onClick = {
                                viewModel.confirmDelivery(order.id)
                                navController.navigate(com.sdd.marketplace.core.navigation.Screen.Coupons.route)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SddPink)
                        ) {
                            Icon(Icons.Filled.CheckCircle, "Confirm"); Spacer(Modifier.width(8.dp)); Text("Mark as Received")
                        }
                        OutlinedButton(onClick = { showRefundDialog = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Request Refund", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (order.status == OrderStatus.PENDING || order.status == OrderStatus.CONFIRMED) {
                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) { Text("Cancel Order", color = MaterialTheme.colorScheme.error) }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
