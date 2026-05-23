package com.sdd.marketplace.feature.static.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
fun PrivacyPolicyScreen(navController: NavController) {
    var agreed by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } },
                title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp)
            ) {
                TermsHeader(
                    icon = Icons.Filled.Shield,
                    title = "Privacy Policy",
                    subtitle = "Effective Date: January 1, 2025",
                    accentColor = Color(0xFF1976D2)
                )
                Spacer(Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2).copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PrivacyTip, "Privacy", tint = Color(0xFF1976D2), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "At Sdd Marketplace, your privacy is our priority. This policy explains what data we collect, why we collect it, and how you can control it.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                PrivacySection("1. Information We Collect", accentColor = Color(0xFF1976D2)) {
                    """We collect information you provide directly:
• Account information: Name, email address, phone number, profile photo
• Identity verification (KYC): Government ID, selfie photos (stored encrypted with AES-256)
• Payment information: Processed via our payment partners — never stored on our servers
• Communications: Messages, reviews, and support tickets you send
• Location: City/state if you choose to add it to your profile

We automatically collect:
• Device information: Device ID, OS version, app version
• Usage data: Pages viewed, features used, time spent
• Technical data: IP address, crash reports, performance data"""
                }
                PrivacySection("2. How We Use Your Information", accentColor = Color(0xFF388E3C)) {
                    """We use your information to:
• Provide, maintain, and improve the App
• Process transactions and send related notifications
• Verify your identity for KYC compliance
• Detect and prevent fraud and abuse
• Send you important service updates and security alerts
• Provide customer support
• Comply with legal obligations

We do NOT:
• Sell your personal data to third parties
• Use your data for targeted advertising without your consent
• Share your financial information with other users"""
                }
                PrivacySection("3. Information Sharing", accentColor = Color(0xFF6C3FB5)) {
                    """We share your information only in these circumstances:
• With sellers/buyers as necessary to complete transactions (name, shipping address)
• With payment processors (Razorpay, PayPal) for transaction processing
• With service providers who help us operate the App (under strict confidentiality agreements)
• When required by law, court order, or to protect our legal rights
• In connection with a business merger or acquisition (with notice to you)

Your public profile information (name, profile photo, ratings) is visible to other users."""
                }
                PrivacySection("4. Data Security", accentColor = Color(0xFF388E3C)) {
                    """We implement industry-standard security measures:
• All data transmitted is encrypted using TLS 1.3
• Passwords are hashed using bcrypt (cost factor 12)
• KYC documents are encrypted with AES-256 at rest and access is logged
• Payment data is processed by PCI-DSS compliant processors
• Device-level database encryption using SQLCipher
• Regular security audits and penetration testing

Despite these measures, no system is 100% secure. We cannot guarantee absolute security and recommend you use strong, unique passwords."""
                }
                PrivacySection("5. Data Retention", accentColor = Color(0xFF1976D2)) {
                    """We retain your data as follows:
• Account data: Until you delete your account + 30 days
• Transaction records: 7 years (legal requirement)
• Chat messages: 1 year after the conversation ends
• KYC documents: 5 years from verification date
• Support tickets: 2 years from closure
• App usage logs: 90 days

After deletion, your data is removed from our active systems within 30 days. Some data may remain in encrypted backups for up to 90 days."""
                }
                PrivacySection("6. Your Rights", accentColor = Color(0xFF6C3FB5)) {
                    """You have the right to:
• Access a copy of your personal data
• Correct inaccurate personal data
• Delete your account and associated data
• Restrict processing of your data
• Data portability — receive your data in a machine-readable format
• Opt out of marketing communications
• Lodge a complaint with your local data protection authority

To exercise any of these rights, go to Settings > Help & Support > Submit Request, or email privacy@sddmarketplace.com. We respond to all requests within 30 days."""
                }
                PrivacySection("7. Cookies & Tracking", accentColor = Color(0xFF1976D2)) {
                    """We use minimal tracking technologies:
• Session tokens: Required for app functionality and authentication
• Analytics: Aggregated, anonymized usage statistics only
• Crash reporting: To identify and fix bugs (no personally identifiable data)
• No advertising trackers or cross-site tracking
• No third-party marketing pixels

You can clear your session by signing out. Functional session tokens cannot be disabled without disabling core app functionality."""
                }
                PrivacySection("8. Children's Privacy", accentColor = Color(0xFFE53935)) {
                    "Sdd Marketplace is not intended for users under 18. We do not knowingly collect data from minors. If we discover we have collected data from a minor without parental consent, we will delete it immediately. Parents who believe their child has created an account should contact us at privacy@sddmarketplace.com."
                }
                PrivacySection("9. Changes to This Policy", accentColor = Color(0xFF6C3FB5)) {
                    "We may update this Privacy Policy periodically. We will notify you of significant changes via the App or email. Continued use of the App after changes constitutes your acceptance of the new policy. The date at the top of this page reflects when the policy was last revised."
                }
                PrivacySection("10. Contact Us", accentColor = Color(0xFF1976D2)) {
                    """Privacy Officer: privacy@sddmarketplace.com
Data Protection Officer: dpo@sddmarketplace.com
Address: Sdd Marketplace Pvt. Ltd., New Delhi, India 110001
Response time: Within 30 business days"""
                }
            }

            TermsAgreeBar(
                agreed = agreed,
                onAgree = { agreed = true },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun PrivacySection(title: String, accentColor: Color = Color(0xFF1976D2), content: () -> String) {
    var expanded by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(
            Modifier.fillMaxWidth()
                .background(accentColor.copy(alpha = 0.09f), RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(Modifier.width(4.dp).height(18.dp).background(accentColor, RoundedCornerShape(2.dp)))
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
                Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.15f))
            ) {
                Text(
                    content(), modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
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
