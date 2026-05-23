package com.sdd.marketplace.feature.orders.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sdd.marketplace.core.ui.theme.SddPink

@Composable
fun BuyerTermsDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    var hasScrolledToBottom by remember { mutableStateOf(false) }
    var accepted by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(scrollState.value) {
        if (scrollState.value >= scrollState.maxValue - 50) hasScrolledToBottom = true
    }

    Dialog(
        onDismissRequest = onDecline,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                // Header
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Gavel, "Terms", tint = SddPink)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Buyer Terms & Conditions", fontWeight = FontWeight.Bold)
                        Text("Please read before placing your order", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Divider()

                // Scrollable content
                Column(
                    Modifier.weight(1f).verticalScroll(scrollState).padding(20.dp)
                ) {
                    Text("By placing this order, you agree to the following:", fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(12.dp))

                    val clauses = listOf(
                        "Purchase Agreement" to "This order constitutes a binding contract with the seller. Once confirmed by the seller, cancellation may not be possible.",
                        "Payment Authorization" to "You authorize Sdd Marketplace to charge your selected payment method for the total amount shown. The charge is final upon seller confirmation.",
                        "Accurate Information" to "You confirm that all delivery information, including shipping address and contact details, is accurate and complete. Sdd Marketplace is not liable for delivery failures due to incorrect information.",
                        "Product Expectations" to "You have reviewed the product listing, including photos, description, condition, and return policy, and agree that the product matches your expectations based on the listing.",
                        "Delivery & Risk" to "Risk of loss or damage passes to you upon delivery. You must inspect the item upon receipt and report any issues within 48 hours via the app.",
                        "Returns & Refunds" to "Returns are subject to the seller's stated return policy. Buyer Protection applies only where the item is significantly not as described or fails to arrive. Buyer's remorse is not covered.",
                        "Dispute Process" to "Any disputes must first be raised with the seller. If unresolved within 48 hours, you may open a formal dispute through the app. All decisions by Sdd Marketplace in dispute resolution are final.",
                        "Prohibited Misuse" to "You agree not to file fraudulent 'item not received' or 'not as described' claims. False claims may result in account suspension and legal action.",
                        "Privacy & Data" to "Your contact and payment information will be shared with the seller only as necessary to fulfill this order, per our Privacy Policy.",
                        "Governing Law" to "This agreement is governed by the laws of India. Disputes arising from this purchase shall be subject to the jurisdiction of courts in New Delhi, India."
                    )
                    clauses.forEachIndexed { i, (title, text) ->
                        Text("${i + 1}. $title", fontWeight = FontWeight.SemiBold, color = SddPink)
                        Spacer(Modifier.height(4.dp))
                        Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                        Spacer(Modifier.height(12.dp))
                    }
                    if (!hasScrolledToBottom) {
                        Text("↓ Scroll down to read all terms", color = SddPink, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }

                Divider()
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = accepted, onCheckedChange = { accepted = it }, colors = CheckboxDefaults.colors(checkedColor = SddPink))
                        Text("I have read and agree to all the buyer terms and conditions")
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onDecline, modifier = Modifier.weight(1f)) { Text("Decline") }
                        Button(
                            onClick = onAccept,
                            modifier = Modifier.weight(1f),
                            enabled = accepted && hasScrolledToBottom,
                            colors = ButtonDefaults.buttonColors(containerColor = SddPink)
                        ) { Text("Accept & Continue") }
                    }
                }
            }
        }
    }
}
