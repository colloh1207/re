package com.sdd.marketplace.feature.profile.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sdd.marketplace.core.ui.components.SddButton
import com.sdd.marketplace.core.ui.components.SddOutlineButton
import com.sdd.marketplace.core.ui.theme.*

data class Transaction(
    val id: String,
    val type: String,
    val description: String,
    val amount: Double,
    val date: String,
    val isCredit: Boolean,
    val status: String = "Completed"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(navController: NavController) {
    var showAddMoneySheet by remember { mutableStateOf(false) }
    var showWithdrawSheet by remember { mutableStateOf(false) }
    val balance = 2847.50
    val transactions = remember {
        listOf(
            Transaction("1", "Sale", "Sold: Blue Denim Jacket", 1200.0, "Today, 2:30 PM", true),
            Transaction("2", "Withdrawal", "Bank Transfer - HDFC ****1234", -500.0, "Yesterday, 10:00 AM", false),
            Transaction("3", "Sale", "Sold: Nike Shoes Size 42", 850.0, "23 May, 4:15 PM", true),
            Transaction("4", "Refund", "Refund: Red Kurta Set", -450.0, "21 May, 9:00 AM", false),
            Transaction("5", "Sale", "Sold: Vintage Watch", 3200.0, "20 May, 1:45 PM", true),
            Transaction("6", "Bonus", "Referral Bonus - Invited Priya", 100.0, "18 May, 8:00 AM", true),
            Transaction("7", "Sale", "Sold: Cotton Saree", 750.0, "15 May, 3:20 PM", true),
            Transaction("8", "Withdrawal", "UPI Transfer - GooglePay", -2000.0, "12 May, 11:30 AM", false)
        )
    }

    if (showAddMoneySheet) {
        AddMoneySheet(onDismiss = { showAddMoneySheet = false })
    }
    if (showWithdrawSheet) {
        WithdrawSheet(onDismiss = { showWithdrawSheet = false }, balance = balance)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } },
                title = { Text("My Wallet", fontWeight = FontWeight.Bold) },
                actions = { IconButton(onClick = {}) { Icon(Icons.Outlined.HelpOutline, "Help", tint = SddPink) } }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Box(
                    Modifier.fillMaxWidth().background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(listOf(SddPink, SddPink.copy(alpha = 0.7f)))
                    ).padding(24.dp)
                ) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.AccountBalanceWallet, "Wallet", tint = Color.White, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Available Balance", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                        Text("₹${String.format("%,.2f", balance)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 36.sp)
                        Spacer(Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { showAddMoneySheet = true }, colors = ButtonDefaults.buttonColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Filled.Add, "Add", tint = SddPink, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Money", color = SddPink, fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(onClick = { showWithdrawSheet = true }, border = BorderStroke(1.5.dp, Color.White), shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Filled.ArrowDownward, "Withdraw", tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Withdraw", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    WalletStatCard("Total Earned", "₹18,450", SuccessGreen, Icons.Outlined.TrendingUp, Modifier.weight(1f))
                    WalletStatCard("Total Withdrawn", "₹12,500", SddPink, Icons.Outlined.ArrowDownward, Modifier.weight(1f))
                }
            }

            item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.05f))) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, "Security", tint = SuccessGreen)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Secure Payments", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Text("Your wallet is protected with bank-level encryption.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Recent Transactions", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    TextButton(onClick = {}) { Text("Filter", color = SddPink) }
                }
            }

            items(transactions) { tx ->
                TransactionItem(tx)
                Divider(Modifier.padding(horizontal = 72.dp))
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun WalletStatCard(label: String, value: String, color: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(icon, label, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
            }
        }
    }
}

@Composable
fun TransactionItem(tx: Transaction) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(if (tx.isCredit) SuccessGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.errorContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (tx.isCredit) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                tx.type,
                tint = if (tx.isCredit) SuccessGreen else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(tx.description, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1)
            Text(tx.date, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                "${if (tx.isCredit) "+" else "-"}₹${String.format("%,.0f", Math.abs(tx.amount))}",
                fontWeight = FontWeight.Bold, fontSize = 15.sp,
                color = if (tx.isCredit) SuccessGreen else MaterialTheme.colorScheme.error
            )
            Surface(color = if (tx.status == "Completed") SuccessGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(4.dp)) {
                Text(tx.status, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = if (tx.status == "Completed") SuccessGreen else MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMoneySheet(onDismiss: () -> Unit) {
    var amount by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(24.dp).fillMaxWidth()) {
            Text("Add Money", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() } },
                label = { Text("Enter Amount (₹)") }, modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                shape = RoundedCornerShape(12.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SddPink))
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("100", "500", "1000", "2000", "5000")) { preset ->
                    FilterChip(selected = amount == preset, onClick = { amount = preset }, label = { Text("₹$preset") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SddPink.copy(alpha = 0.1f), selectedLabelColor = SddPink))
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Payment Methods", fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            listOf("UPI / GPay / PhonePe", "Debit / Credit Card", "Net Banking").forEach { method ->
                ListItem(headlineContent = { Text(method) }, leadingContent = { Icon(Icons.Outlined.Payment, method, tint = SddPink) }, modifier = Modifier.clickable {})
                Divider()
            }
            Spacer(Modifier.height(16.dp))
            SddButton("Pay ₹${amount.ifBlank { "0" }}", onClick = onDismiss, enabled = amount.isNotBlank() && (amount.toIntOrNull() ?: 0) > 0)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WithdrawSheet(onDismiss: () -> Unit, balance: Double) {
    var amount by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(24.dp).fillMaxWidth()) {
            Text("Withdraw Money", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text("Available: ₹${String.format("%,.2f", balance)}", color = SddPink, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(20.dp))
            OutlinedTextField(value = amount, onValueChange = { amount = it.filter { c -> c.isDigit() } },
                label = { Text("Enter Amount (₹)") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SddPink))
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Withdrawal Destination", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    ListItem(headlineContent = { Text("HDFC Bank ****1234") }, supportingContent = { Text("Primary Account") }, leadingContent = { Icon(Icons.Outlined.AccountBalance, "Bank", tint = SddPink) })
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Withdrawals processed within 2-3 business days.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            SddButton("Withdraw ₹${amount.ifBlank { "0" }}", onClick = onDismiss, enabled = amount.isNotBlank() && (amount.toIntOrNull() ?: 0) > 0 && (amount.toDoubleOrNull() ?: 0.0) <= balance)
            Spacer(Modifier.height(32.dp))
        }
    }
}
