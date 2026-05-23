package com.sdd.marketplace.feature.static.ui

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
import androidx.navigation.NavController
import com.sdd.marketplace.core.ui.theme.SddPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsConditionsScreen(navController: NavController) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var generalAgreed by remember { mutableStateOf(false) }
    var sellerAgreed by remember { mutableStateOf(false) }
    var buyerAgreed by remember { mutableStateOf(false) }
    val tabs = listOf("General", "Sellers", "Buyers")

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } },
                title = { Text("Terms & Conditions", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab, contentColor = SddPink) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index, onClick = { selectedTab = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(title)
                                when (index) {
                                    0 -> if (generalAgreed) Icon(Icons.Filled.CheckCircle, "Agreed", tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                                    1 -> if (sellerAgreed) Icon(Icons.Filled.CheckCircle, "Agreed", tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                                    2 -> if (buyerAgreed) Icon(Icons.Filled.CheckCircle, "Agreed", tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    )
                }
            }
            when (selectedTab) {
                0 -> GeneralTermsContent(agreed = generalAgreed, onAgree = { generalAgreed = true })
                1 -> SellerTermsContent(agreed = sellerAgreed, onAgree = { sellerAgreed = true })
                2 -> BuyerTermsContent(agreed = buyerAgreed, onAgree = { buyerAgreed = true })
            }
        }
    }
}

@Composable
fun GeneralTermsContent(agreed: Boolean = false, onAgree: () -> Unit = {}) {
    val scrollState = rememberScrollState()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp)) {
            TermsHeader(
                icon = Icons.Filled.Gavel,
                title = "General Terms & Conditions",
                subtitle = "Last updated: January 1, 2025",
                accentColor = Color(0xFF6C3FB5)
            )
            Spacer(Modifier.height(16.dp))
            TermsSection("1. Acceptance of Terms", accentColor = Color(0xFF6C3FB5)) {
                """By accessing or using the Sdd Marketplace application ("App"), you agree to be bound by these Terms and Conditions ("Terms"). If you do not agree to these Terms, please do not use the App. These Terms apply to all users, including buyers, sellers, and visitors.

We reserve the right to modify these Terms at any time. Continued use of the App after modifications constitutes your acceptance of the new Terms. You will be notified of significant changes via the App or by email."""
            }
            TermsSection("2. Account Registration", accentColor = Color(0xFF6C3FB5)) {
                """You must create an account to use most features of the App. You agree to:
• Provide accurate, complete, and current information
• Keep your login credentials secure and confidential
• Notify us immediately of any unauthorized access to your account
• Be responsible for all activities that occur under your account
• Not create accounts using false identities or for fraudulent purposes
• A maximum of 2 (two) accounts per device is permitted

Users under the age of 18 may not create an account or use the App without verifiable parental consent."""
            }
            TermsSection("3. Prohibited Conduct", accentColor = Color(0xFFE53935)) {
                """You agree not to:
• Post false, misleading, or fraudulent content
• Harass, threaten, or abuse other users
• Use the App for illegal activities
• Circumvent any security features of the App
• Scrape, reverse engineer, or copy the App's code or content
• Create fake reviews or manipulate ratings
• Use automated systems to access the App without permission
• Spam or send unsolicited communications
• Engage in market manipulation or price fixing
• Sell counterfeit, stolen, or prohibited items"""
            }
            TermsSection("4. Intellectual Property", accentColor = Color(0xFF6C3FB5)) {
                """All content on the App, including logos, text, graphics, and software, is the property of Sdd Marketplace or its content suppliers. You may not reproduce, distribute, or create derivative works without our written consent.

By posting content on the App, you grant us a non-exclusive, royalty-free, worldwide license to use, display, and distribute that content in connection with our services."""
            }
            TermsSection("5. Dispute Resolution", accentColor = Color(0xFF1976D2)) {
                """Any disputes between users must first be attempted to be resolved directly between the parties. If resolution cannot be reached, users may escalate to our dispute resolution team through the Help & Support section.

We reserve the right to mediate disputes but are not obligated to do so. Our decisions in moderation disputes are final."""
            }
            TermsSection("6. Limitation of Liability", accentColor = Color(0xFFE53935)) {
                """To the maximum extent permitted by applicable law, Sdd Marketplace shall not be liable for any indirect, incidental, special, consequential, or punitive damages arising from your use of the App.

Our total liability to you for any claims arising from your use of the App shall not exceed the amount you paid to us in the 12 months preceding the claim."""
            }
            TermsSection("7. Governing Law", accentColor = Color(0xFF6C3FB5)) {
                "These Terms are governed by the laws of India. Any legal action arising from these Terms shall be subject to the exclusive jurisdiction of courts in New Delhi, India."
            }
            TermsSection("8. Contact", accentColor = Color(0xFF1976D2)) {
                "For questions about these Terms, contact us at legal@sddmarketplace.com or through the Help & Support section of the App."
            }
        }
        TermsAgreeBar(agreed = agreed, onAgree = onAgree, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun SellerTermsContent(agreed: Boolean = false, onAgree: () -> Unit = {}) {
    val scrollState = rememberScrollState()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp)) {
            TermsHeader(
                icon = Icons.Filled.Store,
                title = "Seller Terms & Conditions",
                subtitle = "Last updated: January 1, 2025",
                accentColor = SddPink
            )
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = SddPink.copy(alpha = 0.08f)), shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, "Info", tint = SddPink, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("These additional terms apply to all sellers. General Terms also apply.", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(12.dp))
            TermsSection("1. Seller Eligibility", accentColor = Color(0xFF388E3C)) {
                """To sell on Sdd Marketplace you must:
• Be at least 18 years of age
• Provide accurate personal and business information
• Comply with all applicable laws in your jurisdiction
• Complete KYC (Know Your Customer) verification for certain transaction limits
• Maintain a valid payment method for receiving payouts
• Not have been previously banned from the platform"""
            }
            TermsSection("2. Listing Requirements", accentColor = SddPink) {
                """All product listings must:
• Be accurate, truthful, and not misleading
• Include clear, representative photographs of the actual item
• State the correct condition (New, Like New, Good, Fair, Poor)
• Comply with applicable product safety and labeling regulations
• Not include prohibited or restricted items (see Prohibited Items Policy)
• Be priced in good faith at market value

New sellers are limited to 2 listings per day for the first 30 days. This limit may be increased based on account standing."""
            }
            TermsSection("3. Transaction Obligations", accentColor = Color(0xFF1976D2)) {
                """Upon accepting an order, sellers must:
• Confirm the order within 24 hours
• Ship items within the stated handling time (maximum 5 business days)
• Provide valid tracking information
• Package items securely to prevent damage in transit
• Honor all stated return and refund policies
• Communicate promptly with buyers about any issues

Failure to fulfill confirmed orders may result in seller account suspension."""
            }
            TermsSection("4. Seller Fees & Payouts", accentColor = Color(0xFF388E3C)) {
                """Sdd Marketplace charges:
• A 5% transaction fee on all completed sales
• A 2% payment processing fee
• No listing fees for standard listings

Payouts are processed within 3-5 business days after order confirmation. Sellers must maintain a valid payout method. Sdd Marketplace reserves the right to withhold payouts during dispute investigations."""
            }
            TermsSection("5. Returns & Refunds", accentColor = Color(0xFF1976D2)) {
                "Sellers must honor the return policy stated in their listings. Minimum return window is 7 days for items not as described. Sellers are responsible for return shipping costs if the item was not as described. Repeated refund requests may indicate listing quality issues and may affect seller standing."
            }
            TermsSection("6. Prohibited Items", accentColor = Color(0xFFE53935)) {
                """The following items are strictly prohibited:
• Counterfeit, replica, or unauthorized goods
• Weapons, firearms, or dangerous materials
• Illegal drugs or controlled substances
• Items that infringe on intellectual property
• Adult content or services
• Live animals
• Human remains or body parts
• Items subject to recall or safety bans

Violations may result in immediate account termination and legal action."""
            }
            TermsSection("7. Seller Ratings & Reviews", accentColor = SddPink) {
                """Sellers are rated by verified buyers. You must not:
• Incentivize positive reviews
• Threaten or harass buyers who leave negative reviews
• Manipulate review scores through fake purchases

Sellers with consistent ratings below 2.0 stars may have listings removed."""
            }
            TermsSection("8. Account Suspension", accentColor = Color(0xFFE53935)) {
                """Seller accounts may be suspended or permanently banned for:
• Multiple confirmed fraud reports
• Consistent failure to fulfill orders
• Selling prohibited items
• Rating manipulation
• Terms violations

Suspended sellers may appeal through our dispute resolution process."""
            }
        }
        TermsAgreeBar(agreed = agreed, onAgree = onAgree, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun BuyerTermsContent(agreed: Boolean = false, onAgree: () -> Unit = {}) {
    val scrollState = rememberScrollState()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp)) {
            TermsHeader(
                icon = Icons.Filled.ShoppingCart,
                title = "Buyer Terms & Conditions",
                subtitle = "Last updated: January 1, 2025",
                accentColor = Color(0xFF1976D2)
            )
            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2).copy(alpha = 0.08f)), shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Info, "Info", tint = Color(0xFF1976D2), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("These additional terms apply to all buyers. General Terms also apply.", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(12.dp))
            TermsSection("1. Purchase Agreement", accentColor = Color(0xFF1976D2)) {
                """When you place an order on Sdd Marketplace:
• You are entering into a binding purchase agreement with the seller
• You agree to pay the full listed price plus applicable fees
• You confirm all order details (size, color, quantity) are correct
• You acknowledge our Buyer Protection policy terms

Orders cannot be cancelled after they have been confirmed by the seller, except where permitted by our cancellation policy."""
            }
            TermsSection("2. Payment", accentColor = Color(0xFF388E3C)) {
                """You must provide a valid payment method. By submitting an order:
• You authorize Sdd Marketplace to charge your payment method
• You agree not to dispute valid charges with your bank
• You understand that payment is processed securely through our payment partners
• You may save payment methods for future use

Fraudulent chargebacks may result in account suspension and legal action."""
            }
            TermsSection("3. Buyer Protection", accentColor = Color(0xFF388E3C)) {
                """Our Buyer Protection covers you when:
• An item is significantly not as described
• An item does not arrive within the stated delivery window
• A seller does not ship after payment
• An item arrives damaged due to inadequate packaging

Buyer Protection does not cover buyer's remorse, change of mind, or issues with correctly described items."""
            }
            TermsSection("4. Reviews & Feedback", accentColor = Color(0xFF1976D2)) {
                """You may leave reviews for sellers after completing a purchase. Reviews must:
• Be honest and based on your genuine experience
• Not contain offensive language or personal attacks
• Not be submitted as part of any incentive scheme

Review eligibility: Accounts must be at least 3 weeks old to post reviews. This helps maintain review authenticity and prevents fake feedback."""
            }
            TermsSection("5. Prohibited Buyer Conduct", accentColor = Color(0xFFE53935)) {
                """Buyers must not:
• Purchase items with intent to defraud sellers
• File false "item not received" or "not as described" claims
• Use stolen or fraudulent payment methods
• Abuse return policies
• Harass sellers for faster delivery
• Leave retaliatory or false negative reviews"""
            }
            TermsSection("6. Dispute Resolution", accentColor = Color(0xFF1976D2)) {
                """If you have a problem with your order:
1. First, contact the seller directly through the in-app chat
2. If unresolved after 48 hours, open a dispute through Order Detail > Request Refund
3. Our team will review evidence from both parties within 5 business days
4. Our decision is final but may be appealed within 14 days"""
            }
        }
        TermsAgreeBar(agreed = agreed, onAgree = onAgree, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun TermsHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, accentColor: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.10f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = accentColor)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun TermsAgreeBar(agreed: Boolean, onAgree: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        if (agreed) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.CheckCircle, "Agreed", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("You have agreed to these terms", fontWeight = FontWeight.Medium, color = Color(0xFF4CAF50))
            }
        } else {
            Button(
                onClick = onAgree,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SddPink),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Check, "Agree", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("I Have Read & Agree to These Terms", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun TermsSection(title: String, accentColor: Color = SddPink, content: () -> String) {
    var expanded by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(
            Modifier.fillMaxWidth()
                .background(accentColor.copy(alpha = 0.08f), RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    Modifier.width(4.dp).height(18.dp).background(accentColor, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.width(10.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = accentColor)
            }
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                "Toggle", tint = accentColor, modifier = Modifier.size(20.dp)
            )
        }
        if (expanded) {
            Surface(
                Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.15f))
            ) {
                Text(
                    content(),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        } else {
            Divider(color = accentColor.copy(alpha = 0.12f))
        }
        Spacer(Modifier.height(6.dp))
    }
}
