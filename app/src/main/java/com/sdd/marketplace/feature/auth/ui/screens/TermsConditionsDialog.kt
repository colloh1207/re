package com.sdd.marketplace.feature.auth.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sdd.marketplace.core.ui.theme.SddPink

@Composable
fun TermsConditionsDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Terms & Conditions", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, "Close")
                    }
                }
                HorizontalDivider()

                var selectedTab by remember { mutableIntStateOf(0) }
                val tabs = listOf("General", "Sellers", "Buyers")
                TabRow(selectedTabIndex = selectedTab, contentColor = SddPink) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 13.sp) }
                        )
                    }
                }

                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    when (selectedTab) {
                        0 -> DialogTermsContent(
                            title = "General Terms & Conditions",
                            accentColor = Color(0xFF6C3FB5),
                            sections = generalTermsSections()
                        )
                        1 -> DialogTermsContent(
                            title = "Seller Terms & Conditions",
                            accentColor = SddPink,
                            sections = sellerTermsSections()
                        )
                        2 -> DialogTermsContent(
                            title = "Buyer Terms & Conditions",
                            accentColor = Color(0xFF1976D2),
                            sections = buyerTermsSections()
                        )
                    }
                }

                HorizontalDivider()
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SddPink),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Close", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DialogTermsContent(title: String, accentColor: Color, sections: List<Pair<String, String>>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Gavel, null, tint = accentColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, fontWeight = FontWeight.Bold, color = accentColor, fontSize = 13.sp)
        }
    }
    Spacer(Modifier.height(12.dp))
    sections.forEach { (sectionTitle, content) ->
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Row(
                Modifier.fillMaxWidth()
                    .background(accentColor.copy(alpha = 0.08f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(3.dp).height(16.dp).background(accentColor, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(8.dp))
                Text(sectionTitle, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = accentColor)
            }
            Surface(
                Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.15f))
            ) {
                Text(
                    content,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

private fun generalTermsSections() = listOf(
    "1. Acceptance of Terms" to "By using Sdd Marketplace you agree to these Terms. Continued use after modifications constitutes acceptance of updated Terms.",
    "2. Account Registration" to "Provide accurate information, keep credentials secure, and be responsible for all account activities. Maximum 2 accounts per device.",
    "3. Prohibited Conduct" to "No false content, harassment, illegal activities, fake reviews, spam, counterfeit items, or circumvention of security features.",
    "4. Intellectual Property" to "All platform content belongs to Sdd Marketplace. By posting, you grant us a license to display your content.",
    "5. Limitation of Liability" to "Sdd Marketplace is not liable for indirect or consequential damages. Total liability is limited to amounts paid in the preceding 12 months.",
    "6. Governing Law" to "These Terms are governed by applicable law. Contact us at legal@sddmarketplace.com for questions."
)

private fun sellerTermsSections() = listOf(
    "1. Seller Eligibility" to "Must be 18+, provide accurate information, comply with local laws, and complete KYC for certain transaction limits.",
    "2. Listing Requirements" to "Listings must be accurate, include real photos, state correct condition, and be priced in good faith. No prohibited items.",
    "3. Transaction Obligations" to "Confirm orders within 24 hours, ship within 5 business days, provide tracking, and communicate with buyers.",
    "4. Seller Fees" to "5% transaction fee + 2% payment processing fee. Payouts within 3-5 business days after order confirmation.",
    "5. Prohibited Items" to "No counterfeit goods, weapons, illegal drugs, items infringing IP, adult content, or recalled items.",
    "6. Account Suspension" to "Accounts may be suspended for fraud, failure to fulfill orders, selling prohibited items, or terms violations."
)

private fun buyerTermsSections() = listOf(
    "1. Purchase Agreement" to "Placing an order creates a binding purchase agreement with the seller at the full listed price plus fees.",
    "2. Payment" to "You authorize payment processing and agree not to dispute valid charges. Fraudulent chargebacks may result in suspension.",
    "3. Buyer Protection" to "Covers items significantly not as described, non-arrival, or seller non-shipment. Does not cover buyer's remorse.",
    "4. Reviews" to "Reviews must be honest. Accounts must be 3 weeks old to post. No incentivized or false reviews.",
    "5. Prohibited Conduct" to "No fraudulent purchases, false claims, stolen payment methods, return policy abuse, or seller harassment.",
    "6. Dispute Resolution" to "Contact seller first, then open a dispute via Order Detail if unresolved after 48 hours. Our team reviews within 5 business days."
)
