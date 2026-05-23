package com.sdd.marketplace.feature.boost.ui

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sdd.marketplace.core.ui.theme.SddPink
import com.sdd.marketplace.core.util.CurrencyUtils
import com.sdd.marketplace.domain.model.BoostTier
import com.sdd.marketplace.domain.model.BoostTiers
import com.sdd.marketplace.domain.model.Product
import com.sdd.marketplace.feature.boost.viewmodel.BoostEvent
import com.sdd.marketplace.feature.boost.viewmodel.BoostStep
import com.sdd.marketplace.feature.boost.viewmodel.BoostViewModel
import kotlinx.coroutines.flow.collectLatest

private val TierColors = listOf(
    listOf(Color(0xFF6C757D), Color(0xFF495057)),  // Starter — slate
    listOf(Color(0xFF2196F3), Color(0xFF1565C0)),  // Basic — blue
    listOf(Color(0xFF4CAF50), Color(0xFF2E7D32)),  // Standard — green
    listOf(Color(0xFF9C27B0), Color(0xFF6A1B9A)),  // Premium — purple
    listOf(Color(0xFFFF9800), Color(0xFFE65100)),  // Business — orange
    listOf(Color(0xFFFFD700), Color(0xFFF57F17)),  // Elite — gold
)

@Composable
fun BoostScreen(
    preSelectedProductId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    viewModel: BoostViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(preSelectedProductId) {
        if (!preSelectedProductId.isNullOrBlank()) {
            viewModel.preSelectProduct(preSelectedProductId)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is BoostEvent.NavigateBack   -> onNavigateBack()
                is BoostEvent.PaymentFailed  -> { /* error shown in state */ }
                is BoostEvent.PollingTimeout -> { /* error shown in state */ }
            }
        }
    }

    Scaffold(
        topBar = {
            BoostTopBar(
                step       = state.step,
                onBack     = { viewModel.goBack() },
                showBack   = state.step != BoostStep.SUCCESS
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    slideInHorizontally(tween(280)) { it / 3 } + fadeIn(tween(200)) togetherWith
                    slideOutHorizontally(tween(280)) { -it / 3 } + fadeOut(tween(150))
                },
                label = "boost_step"
            ) { step ->
                when (step) {
                    BoostStep.PRODUCT_SELECTION -> ProductSelectionStep(
                        products           = state.myProducts,
                        selectedIds        = state.selectedProductIds,
                        onToggle           = viewModel::toggleProductSelection,
                        onNext             = viewModel::goToTierSelection,
                        error              = state.error,
                        onDismissError     = viewModel::dismissError
                    )
                    BoostStep.TIER_SELECTION -> TierSelectionStep(
                        selectedTier  = state.selectedTier,
                        currency      = state.currency,
                        onSelectTier  = viewModel::selectTier,
                        onNext        = viewModel::goToCheckout
                    )
                    BoostStep.CHECKOUT -> CheckoutStep(
                        state         = state,
                        onCurrencyChange = viewModel::updateCurrency,
                        onPay         = viewModel::createBoostAndPay,
                        isLoading     = state.isLoading,
                        error         = state.error,
                        onDismissError = viewModel::dismissError
                    )
                    BoostStep.WEBVIEW_PAYMENT -> BoostWebViewStep(
                        url           = state.paystackAuthUrl ?: "",
                        onCallback    = viewModel::onPaymentRedirect,
                        onBack        = { viewModel.retryPayment() }
                    )
                    BoostStep.POLLING -> PollingStep(
                        attempts      = state.pollAttempts,
                        onRetry       = { viewModel.startPolling() },
                        error         = state.error
                    )
                    BoostStep.SUCCESS -> BoostSuccessStep(
                        boost         = state.successBoost,
                        currency      = state.currency,
                        onHome        = onNavigateHome
                    )
                }
            }
        }
    }
}

// ─── Top Bar ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoostTopBar(step: BoostStep, onBack: () -> Unit, showBack: Boolean) {
    val title = when (step) {
        BoostStep.PRODUCT_SELECTION -> "Select Products"
        BoostStep.TIER_SELECTION    -> "Choose Boost Plan"
        BoostStep.CHECKOUT          -> "Review & Pay"
        BoostStep.WEBVIEW_PAYMENT   -> "Secure Payment"
        BoostStep.POLLING           -> "Verifying Payment"
        BoostStep.SUCCESS           -> "Boost Active!"
    }
    TopAppBar(
        title = {
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (step != BoostStep.SUCCESS && step != BoostStep.POLLING) {
                    val current = step.ordinal + 1
                    Text("Step $current of 3", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBackIosNew, "Back") }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}

// ─── Step 1: Product Selection ────────────────────────────────────────────────

@Composable
private fun ProductSelectionStep(
    products: List<Product>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onNext: () -> Unit,
    error: String?,
    onDismissError: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        if (products.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Inventory2, "No products",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(16.dp))
                    Text("No active listings found", fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Text("Post a product first to boost it.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            Text(
                "Choose the products you want to promote. Boosted products rank higher in search and get featured placement.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                items(products, key = { it.id }) { product ->
                    val selected = selectedIds.contains(product.id)
                    ProductPickerCard(product, selected) { onToggle(product.id) }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        if (error != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.padding(16.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ErrorOutline, "Error",
                        tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(error, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismissError, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, "Dismiss", modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
            enabled = selectedIds.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = SddPink),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                if (selectedIds.isEmpty()) "Select a product" else "Boost ${selectedIds.size} product${if (selectedIds.size > 1) "s" else ""}  →",
                fontWeight = FontWeight.Bold, fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun ProductPickerCard(product: Product, selected: Boolean, onToggle: () -> Unit) {
    Card(
        onClick   = onToggle,
        shape     = RoundedCornerShape(12.dp),
        border    = if (selected) BorderStroke(2.dp, SddPink) else null,
        colors    = CardDefaults.cardColors(
            containerColor = if (selected) SddPink.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(if (selected) 0.dp else 1.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model              = product.images.firstOrNull(),
                contentDescription = product.title,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(product.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    CurrencyUtils.format(product.price, product.currency),
                    color = SddPink, fontWeight = FontWeight.Bold, fontSize = 13.sp
                )
                if (product.isBoosted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bolt, "Boosted", tint = Color(0xFFFF9800), modifier = Modifier.size(14.dp))
                        Text(" Already boosted", fontSize = 11.sp, color = Color(0xFFFF9800))
                    }
                }
            }
            Checkbox(checked = selected, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = SddPink))
        }
    }
}

// ─── Step 2: Tier Selection ────────────────────────────────────────────────────

@Composable
private fun TierSelectionStep(
    selectedTier: BoostTier?,
    currency: CurrencyUtils.CurrencyInfo,
    onSelectTier: (BoostTier) -> Unit,
    onNext: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Text(
            "More impressions = more buyers. Pick a plan that fits your goals.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        ) {
            itemsIndexed(BoostTiers.all, key = { _, tier -> tier.id }) { index, tier ->
                TierCard(
                    tier       = tier,
                    colors     = TierColors[index],
                    currency   = currency,
                    isSelected = selectedTier?.id == tier.id,
                    onSelect   = { onSelectTier(tier) }
                )
                Spacer(Modifier.height(12.dp))
            }
        }
        Button(
            onClick  = onNext,
            enabled  = selectedTier != null,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = SddPink),
            shape    = RoundedCornerShape(12.dp)
        ) {
            Text(
                if (selectedTier == null) "Select a plan" else "Continue with ${selectedTier.name}  →",
                fontWeight = FontWeight.Bold, fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun TierCard(
    tier: BoostTier,
    colors: List<Color>,
    currency: CurrencyUtils.CurrencyInfo,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val scale by animateFloatAsState(if (isSelected) 1.02f else 1f, tween(180), label = "scale")
    val borderAlpha by animateFloatAsState(if (isSelected) 1f else 0f, tween(180), label = "border")

    Card(
        onClick    = onSelect,
        shape      = RoundedCornerShape(16.dp),
        border     = BorderStroke(2.dp, colors[0].copy(alpha = borderAlpha)),
        elevation  = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp),
        modifier   = Modifier.fillMaxWidth()
    ) {
        Column {
            // Gradient header
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(colors))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(tier.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            if (tier.badgeLabel.isNotEmpty()) {
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    color = Color.White.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(tier.badgeLabel, color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(
                            "${formatImpressionsShort(tier.impressions)} impressions · ${tier.durationDays} days",
                            color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            CurrencyUtils.convertFromUsd(tier.priceUsd, currency.code),
                            color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp
                        )
                        Text("one-time", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                }
            }
            // Feature list
            Column(Modifier.padding(12.dp)) {
                tier.features.forEach { feature ->
                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, "✓",
                            tint = colors[0], modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(feature, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (isSelected) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RadioButtonChecked, "Selected", tint = colors[0], modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Selected", color = colors[0], fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

private fun formatImpressionsShort(n: Int) = when {
    n >= 1_000_000 -> "${n / 1_000_000}M"
    n >= 1_000     -> "${n / 1_000}K"
    else           -> "$n"
}

// ─── Step 3: Checkout ─────────────────────────────────────────────────────────

@Composable
private fun CheckoutStep(
    state: com.sdd.marketplace.feature.boost.viewmodel.BoostUiState,
    onCurrencyChange: (String) -> Unit,
    onPay: () -> Unit,
    isLoading: Boolean,
    error: String?,
    onDismissError: () -> Unit
) {
    val tier = state.selectedTier ?: return
    var showCurrencyPicker by remember { mutableStateOf(false) }
    val localPrice = CurrencyUtils.convertFromUsd(tier.priceUsd, state.currency.code)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Order summary
            Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Order Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    SummaryRow(Icons.Outlined.Inventory2, "Products", "${state.selectedProductIds.size} selected")
                    SummaryRow(Icons.Default.Bolt, "Plan", "${tier.name} — ${tier.durationDays} days")
                    SummaryRow(Icons.Outlined.Visibility, "Impressions", formatImpressionsShort(tier.impressions) + " guaranteed")
                    SummaryRow(Icons.Outlined.Schedule, "Duration", "${tier.durationDays} days from payment")
                }
            }
        }
        item {
            // Currency picker
            Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(1.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Currency", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedCard(
                        onClick  = { showCurrencyPicker = true },
                        shape    = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(state.currency.flag, fontSize = 22.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${state.currency.code} — ${state.currency.name}",
                                    fontWeight = FontWeight.Medium)
                                Text("1 USD = ${CurrencyUtils.format(state.currency.usdRate, state.currency.code)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.ExpandMore, "Change", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
        item {
            // Price card
            Card(
                shape  = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SddPink.copy(alpha = 0.06f))
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Total due today", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(localPrice, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = SddPink)
                        Text("≈ $${tier.priceUsd} USD", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Lock, "Secure", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                        Text("Secured by", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Paystack", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00C3F7))
                    }
                }
            }
        }
        item {
            if (error != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(10.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, "Error",
                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(error, color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = onDismissError, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, "Dismiss", modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
        item {
            Button(
                onClick  = onPay,
                enabled  = !isLoading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C3F7)),
                shape    = RoundedCornerShape(14.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Preparing payment...", fontWeight = FontWeight.Bold, color = Color.White)
                } else {
                    Icon(Icons.Default.CreditCard, "Pay", tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pay $localPrice with Paystack", fontWeight = FontWeight.Bold,
                        color = Color.White, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "By proceeding, you agree to our Boost terms. Refunds are not available for active boosts.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showCurrencyPicker) {
        CurrencyPickerSheet(
            selected  = state.currency.code,
            onSelect  = { code -> onCurrencyChange(code); showCurrencyPicker = false },
            onDismiss = { showCurrencyPicker = false }
        )
    }
}

@Composable
private fun SummaryRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, label, modifier = Modifier.size(18.dp), tint = SddPink)
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
    }
}

// ─── Currency Picker Bottom Sheet ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyPickerSheet(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text("Select Currency", fontWeight = FontWeight.Bold, fontSize = 18.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        LazyColumn(
            contentPadding     = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(CurrencyUtils.supported, key = { it.code }) { info ->
                val isSelected = info.code == selected
                Card(
                    onClick = { onSelect(info.code) },
                    shape   = RoundedCornerShape(10.dp),
                    colors  = CardDefaults.cardColors(
                        containerColor = if (isSelected) SddPink.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                    ),
                    border  = if (isSelected) BorderStroke(1.5.dp, SddPink) else null
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(info.flag, fontSize = 22.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(info.code, fontWeight = FontWeight.SemiBold)
                            Text(info.name, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (isSelected) Icon(Icons.Default.CheckCircle, "Selected", tint = SddPink)
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

// ─── Step 4: WebView ──────────────────────────────────────────────────────────

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BoostWebViewStep(url: String, onCallback: (String) -> Unit, onBack: () -> Unit) {
    if (url.isBlank()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.ErrorOutline, "Error",
                    modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                Text("Payment URL unavailable", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = onBack) { Text("Go back") }
            }
        }
        return
    }
    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val loadUrl = request.url.toString()
                        // Intercept Paystack callback (success or cancel)
                        if (loadUrl.contains("trxref=") || loadUrl.contains("reference=") ||
                            loadUrl.contains("/callback") || loadUrl.contains("sddapp://")) {
                            onCallback(loadUrl)
                            return true
                        }
                        return false
                    }
                }
                loadUrl(url)
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

// ─── Step 5: Polling ──────────────────────────────────────────────────────────

@Composable
private fun PollingStep(attempts: Int, onRetry: () -> Unit, error: String?) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha"
    )

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            repeat(3) { i ->
                val delay = i * 300
                val size by rememberInfiniteTransition(label = "ring$i").animateFloat(
                    initialValue = 80f, targetValue = 150f + i * 20f,
                    animationSpec = infiniteRepeatable(tween(1200 + delay), RepeatMode.Restart),
                    label = "ring_size$i"
                )
                val ringAlpha by rememberInfiniteTransition(label = "a$i").animateFloat(
                    initialValue = 0.4f, targetValue = 0f,
                    animationSpec = infiniteRepeatable(tween(1200 + delay), RepeatMode.Restart),
                    label = "ring_alpha$i"
                )
                Box(
                    Modifier
                        .size(size.dp)
                        .clip(CircleShape)
                        .background(SddPink.copy(alpha = ringAlpha * 0.3f))
                )
            }
            Box(
                Modifier.size(80.dp).clip(CircleShape).background(SddPink),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CreditCard, "Payment",
                    tint = Color.White, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.height(32.dp))
        Text("Verifying your payment…", fontWeight = FontWeight.Bold, fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha))
        Spacer(Modifier.height(8.dp))
        Text(
            if (attempts == 0) "This usually takes a few seconds."
            else "Attempt ${attempts + 1} of 12…",
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
        )
        if (error != null) {
            Spacer(Modifier.height(24.dp))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = onRetry) {
                        Icon(Icons.Default.Refresh, "Retry", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Check again")
                    }
                }
            }
        }
    }
}

// ─── Step 6: Success ──────────────────────────────────────────────────────────

@Composable
private fun BoostSuccessStep(
    boost: com.sdd.marketplace.domain.model.Boost?,
    currency: CurrencyUtils.CurrencyInfo,
    onHome: () -> Unit
) {
    val tier = BoostTiers.all.firstOrNull { it.id == boost?.tierId }
    val scale by animateFloatAsState(1f, spring(dampingRatio = 0.4f), label = "scale")

    LazyColumn(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        item {
            Box(
                Modifier.size(120.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFF4CAF50).copy(0.3f), Color(0xFF4CAF50).copy(0.05f)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Bolt, "Boost active!",
                    tint = Color(0xFF4CAF50), modifier = Modifier.size(64.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("🚀 Your Boost is Live!", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp,
                textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Your products now have premium placement. Watch the views roll in!",
                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
        }
        if (tier != null && boost != null) {
            item {
                Card(
                    shape  = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.07f))
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        listOf(
                            Icons.Default.Bolt        to "Plan: ${tier.name}",
                            Icons.Outlined.Visibility to "Impressions: ${formatImpressionsShort(tier.impressions)} guaranteed",
                            Icons.Outlined.Schedule   to "Active for ${tier.durationDays} days",
                            Icons.Default.TrendingUp  to "Ranking: ${if (tier.sortOrder >= 4) "${(tier.sortOrder + 1) * 2}×" else "${tier.sortOrder + 1}×"} search boost"
                        ).forEach { (icon, text) ->
                            Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(icon, text, tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        item {
            Button(
                onClick  = onHome,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = SddPink),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Home, "Home", tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Back to Home", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }
        }
    }
}
