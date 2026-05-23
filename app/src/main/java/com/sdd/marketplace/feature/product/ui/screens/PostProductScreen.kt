package com.sdd.marketplace.feature.product.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sdd.marketplace.core.ui.components.*
import com.sdd.marketplace.core.ui.theme.*
import com.sdd.marketplace.core.navigation.Screen
import com.sdd.marketplace.feature.kyc.viewmodel.KycViewModel
import com.sdd.marketplace.feature.product.viewmodel.LocationSuggestion
import com.sdd.marketplace.feature.product.viewmodel.PostProductEvent
import com.sdd.marketplace.feature.product.viewmodel.PostProductUiState
import com.sdd.marketplace.feature.product.viewmodel.PostProductViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostProductScreen(
    onNavigateBack: () -> Unit,
    onPostSuccess: () -> Unit,
    onBoostListing: (productId: String) -> Unit = {},
    onNavigateToKyc: () -> Unit = {},
    viewModel: PostProductViewModel = hiltViewModel(),
    kycViewModel: KycViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val kycState by kycViewModel.uiState.collectAsState()
    var showSuccessScreen by remember { mutableStateOf(false) }
    var successProductId by remember { mutableStateOf("") }

    // KYC is only required for high-value or high-risk categories, not all listings
    val isKycApproved = kycState.submission?.status?.name == "APPROVED"
    val highRiskCategories = setOf("vehicles", "cars", "real_estate", "rentals", "apartments", "property", "rent")
    val currentCategory = uiState.category.lowercase()
    val currentPrice = uiState.price.toDoubleOrNull() ?: 0.0
    val requiresKyc = !isKycApproved &&
        (currentCategory in highRiskCategories || currentPrice >= 5000.0)

    if (requiresKyc && uiState.step >= 2) {
        KycRequiredGate(onNavigateToKyc = onNavigateToKyc, onNavigateBack = onNavigateBack)
        return
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PostProductEvent.PostSuccess -> { successProductId = event.productId; showSuccessScreen = true }
                else -> {}
            }
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> uris.forEach { viewModel.addImage(it) } }

    if (showSuccessScreen) {
        PostSuccessScreen(onNavigateHome = onPostSuccess, onBoostListing = { onBoostListing(successProductId) })
        return
    }

    // Max images warning dialog
    if (uiState.maxImagesReached) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissMaxImagesWarning() },
            icon = { Icon(Icons.Filled.PhotoLibrary, "Photos", tint = SddPink) },
            title = { Text("Maximum Photos Reached") },
            text = { Text("You can add a maximum of 5 photos per listing. Remove an existing photo to add a new one.") },
            confirmButton = {
                Button(onClick = { viewModel.dismissMaxImagesWarning() }, colors = ButtonDefaults.buttonColors(containerColor = SddPink)) {
                    Text("Got it")
                }
            }
        )
    }

    // Country mismatch warning dialog
    uiState.countryMismatchWarning?.let { warning ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissCountryMismatch() },
            icon = { Icon(Icons.Filled.Warning, "Warning", tint = Color(0xFFFF9800)) },
            title = { Text("Country Mismatch Detected") },
            text = {
                Column {
                    Text(warning, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    if (uiState.isUnderReview) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFFFF9800))
                        Spacer(Modifier.height(4.dp))
                        Text("Listing under review (5 minutes)...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.dismissCountryMismatch() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))) {
                    Text("I Understand")
                }
            }
        )
    }

    if (uiState.showLocationSheet) {
        LocationSearchSheet(
            suggestions = uiState.locationSuggestions,
            isSearching = uiState.isSearchingLocation,
            onSearch = { viewModel.searchLocation(it) },
            onSelect = { viewModel.selectLocation(it) },
            onDismiss = { viewModel.hideLocationSheet() }
        )
    }

    if (uiState.showDeliverySheet) {
        DeliveryOptionsSheet(
            selected = uiState.deliveryOptions,
            onToggle = { viewModel.toggleDeliveryOption(it) },
            onDismiss = { viewModel.hideDeliverySheet() }
        )
    }

    if (uiState.showReturnPolicySheet) {
        ReturnPolicySheet(
            selected = uiState.returnPolicy,
            onSelect = { viewModel.updateReturnPolicy(it); viewModel.hideReturnPolicySheet() },
            onDismiss = { viewModel.hideReturnPolicySheet() }
        )
    }

    if (uiState.showPreview) {
        ProductPreviewSheet(uiState = uiState, onDismiss = { viewModel.togglePreview() }, onPost = { viewModel.submitProduct() })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "Back") }
                },
                title = {
                    Text(
                        when (uiState.step) {
                            1 -> "Product Details"
                            2 -> "Shipping & Options"
                            else -> "Preview & Post"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    TextButton(onClick = { viewModel.togglePreview() }) {
                        Text("Preview", color = SddPink, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LinearProgressIndicator(
                progress = { uiState.step / 3f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = SddPink,
                trackColor = SddPink.copy(alpha = 0.2f)
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                repeat(3) { i ->
                    val isActive = i + 1 <= uiState.step
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            Modifier.size(24.dp).clip(CircleShape)
                                .background(if (isActive) SddPink else SddPink.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (i + 1 < uiState.step) {
                                Icon(Icons.Filled.Check, "", tint = Color.White, modifier = Modifier.size(14.dp))
                            } else {
                                Text("${i + 1}", color = if (isActive) Color.White else SddPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (i < 2) {
                            Divider(
                                Modifier.weight(1f).padding(horizontal = 4.dp),
                                color = if (i + 1 < uiState.step) SddPink else SddPink.copy(alpha = 0.2f),
                                thickness = 2.dp
                            )
                        }
                    }
                }
            }

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                when (uiState.step) {
                    1 -> Step1Content(uiState, viewModel) { imageLauncher.launch("image/*") }
                    2 -> Step2Content(uiState, viewModel)
                    3 -> Step3PreviewContent(uiState, viewModel)
                }

                uiState.error?.let { err ->
                    Card(
                        Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ErrorOutline, "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                if (uiState.isUploading) {
                    Card(Modifier.fillMaxWidth().padding(16.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Uploading photos... ${(uiState.uploadProgress * 100).toInt()}%", fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { uiState.uploadProgress },
                                modifier = Modifier.fillMaxWidth(),
                                color = SddPink
                            )
                        }
                    }
                }

                Card(
                    Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SddPink.copy(alpha = 0.05f))
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, "Safe", tint = SddPink)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Safe & Secure", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text("Your listing will be reviewed to ensure a safe marketplace.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(Modifier.height(80.dp))
            }

            Surface(shadowElevation = 8.dp) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (uiState.step > 1) {
                        SddOutlineButton("Back", onClick = { viewModel.prevStep() }, modifier = Modifier.weight(1f))
                    }
                    SddButton(
                        text = when (uiState.step) {
                            1 -> "Continue"
                            2 -> "Preview Listing"
                            else -> "Post Product"
                        },
                        onClick = {
                            when (uiState.step) {
                                1, 2 -> viewModel.nextStep()
                                else -> viewModel.submitProduct()
                            }
                        },
                        isLoading = uiState.isLoading,
                        modifier = Modifier.weight(if (uiState.step > 1) 2f else 1f)
                    )
                }
            }
        }
    }
}

@Composable
fun Step1Content(uiState: PostProductUiState, viewModel: PostProductViewModel, onAddImage: () -> Unit) {
    Column(Modifier.padding(16.dp)) {
        Text("Photos", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Add up to 5 photos. First photo is the cover.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text("${uiState.selectedImages.size}/5", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (uiState.selectedImages.size >= 5) MaterialTheme.colorScheme.error else SddPink)
        }
        Spacer(Modifier.height(12.dp))

        if (uiState.selectedImages.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().height(160.dp).clickable { onAddImage() },
                colors = CardDefaults.cardColors(containerColor = SddPink.copy(alpha = 0.05f)),
                border = BorderStroke(2.dp, SddPink.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(Icons.Outlined.AddAPhoto, "Add Photo", tint = SddPink, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Tap to add photos", color = SddPink, fontWeight = FontWeight.Medium)
                    Text("JPG, PNG supported", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Box(
                        Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)).background(SddPink.copy(alpha = 0.1f)).clickable { onAddImage() },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Add, "Add", tint = SddPink, modifier = Modifier.size(28.dp))
                            Text("Add more", fontSize = 11.sp, color = SddPink)
                        }
                    }
                }
                items(uiState.selectedImages.size) { i ->
                    val uri = uiState.selectedImages[i]
                    Box(Modifier.size(100.dp)) {
                        AsyncImage(model = uri, contentDescription = "Image $i", modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                        if (i == 0) {
                            Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)).padding(4.dp)) {
                                Text("Cover", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                            }
                        }
                        IconButton(onClick = { viewModel.removeImage(uri) }, modifier = Modifier.size(24.dp).align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.5f), CircleShape)) {
                            Icon(Icons.Filled.Close, "Remove", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Basic Information", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))

        SddTextField(value = uiState.title, onValueChange = { viewModel.updateTitle(it) }, label = "Product Title *",
            leadingIcon = { Icon(Icons.Outlined.Edit, "Title") })
        Spacer(Modifier.height(12.dp))

        var catExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
            OutlinedTextField(
                value = uiState.category.ifBlank { "Select Category *" }, onValueChange = {}, readOnly = true,
                label = { Text("Category *") }, leadingIcon = { Icon(Icons.Outlined.GridView, "Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SddPink)
            )
            ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                listOf("Women", "Men", "Home & Living", "Beauty & Health", "Electronics", "Sports", "Kids", "Books", "Vehicles", "Other").forEach { cat ->
                    DropdownMenuItem(text = { Text(cat) }, onClick = { viewModel.updateCategory(cat); catExpanded = false })
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        var condExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = condExpanded, onExpandedChange = { condExpanded = it }) {
            OutlinedTextField(
                value = uiState.condition.ifBlank { "Select Condition *" }, onValueChange = {}, readOnly = true,
                label = { Text("Condition *") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = condExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SddPink)
            )
            ExposedDropdownMenu(expanded = condExpanded, onDismissRequest = { condExpanded = false }) {
                listOf("Brand New", "Like New", "Good", "Fair", "For Parts").forEach { cond ->
                    DropdownMenuItem(text = { Text(cond) }, onClick = { viewModel.updateCondition(cond); condExpanded = false })
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SddTextField(value = uiState.brand, onValueChange = { viewModel.updateBrand(it) }, label = "Brand (Optional)",
            leadingIcon = { Icon(Icons.Outlined.Label, "Brand") })
        Spacer(Modifier.height(20.dp))

        Text("Pricing", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SddTextField(value = uiState.price, onValueChange = { viewModel.updatePrice(it) }, label = "Price *",
                modifier = Modifier.weight(1f),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
            SddTextField(value = uiState.discountPrice, onValueChange = { viewModel.updateDiscountPrice(it) }, label = "MRP (Optional)",
                modifier = Modifier.weight(1f),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
        }
        Spacer(Modifier.height(8.dp))
        var showCurrencyMenu by remember { mutableStateOf(false) }
        val selectedCurrency = com.sdd.marketplace.core.util.CurrencyUtils.getInfo(uiState.currency)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AttachMoney, "Currency", tint = SddPink, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Currency:", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(8.dp))
            Box {
                FilterChip(
                    selected = true,
                    onClick  = { showCurrencyMenu = true },
                    label    = { Text("${selectedCurrency.flag} ${selectedCurrency.code} (${selectedCurrency.symbol})") },
                    trailingIcon = { Icon(Icons.Default.ExpandMore, "Change", Modifier.size(16.dp)) },
                    colors   = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SddPink.copy(alpha = 0.1f),
                        selectedLabelColor     = SddPink
                    )
                )
                DropdownMenu(expanded = showCurrencyMenu, onDismissRequest = { showCurrencyMenu = false }) {
                    com.sdd.marketplace.core.util.CurrencyUtils.supported.forEach { info ->
                        DropdownMenuItem(
                            text    = { Text("${info.flag} ${info.code} — ${info.name}", style = MaterialTheme.typography.bodySmall) },
                            onClick = { viewModel.updateCurrency(info.code); showCurrencyMenu = false }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { viewModel.toggleNegotiable() }) {
            Checkbox(checked = uiState.isNegotiable, onCheckedChange = { viewModel.toggleNegotiable() }, colors = CheckboxDefaults.colors(checkedColor = SddPink))
            Text("Price is negotiable", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(20.dp))

        Text("Description *", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        SddTextField(value = uiState.description, onValueChange = { viewModel.updateDescription(it) },
            label = "Describe your product in detail...", singleLine = false)
        Spacer(Modifier.height(20.dp))

        Text("Tags (Optional)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        var tagInput by remember { mutableStateOf("") }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SddTextField(value = tagInput, onValueChange = { tagInput = it }, label = "Add tags (e.g. summer, fashion)", modifier = Modifier.weight(1f))
            IconButton(onClick = { viewModel.addTag(tagInput); tagInput = "" }, enabled = tagInput.isNotBlank()) {
                Icon(Icons.Filled.Add, "Add Tag", tint = if (tagInput.isNotBlank()) SddPink else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (uiState.tags.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.tags) { tag ->
                    FilterChip(
                        selected = true, onClick = { viewModel.removeTag(tag) },
                        label = { Text(tag) },
                        trailingIcon = { Icon(Icons.Filled.Close, "Remove", modifier = Modifier.size(14.dp)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SddPink.copy(alpha = 0.1f), selectedLabelColor = SddPink)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Step2Content(uiState: PostProductUiState, viewModel: PostProductViewModel) {
    Column(Modifier.padding(16.dp)) {
        Text("Location & Shipping", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column {
                ListItem(
                    headlineContent = { Text("Product Location") },
                    supportingContent = {
                        Text(
                            uiState.location.ifBlank { "Tap to search and select location" },
                            color = if (uiState.location.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    leadingContent = { Icon(Icons.Outlined.LocationOn, "Location", tint = SddPink) },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, "", tint = SddPink) },
                    modifier = Modifier.clickable { viewModel.showLocationSheet() }
                )
                Divider(Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("Delivery Options") },
                    supportingContent = {
                        Text(
                            if (uiState.deliveryOptions.isEmpty()) "Tap to select delivery options"
                            else uiState.deliveryOptions.joinToString(", "),
                            color = if (uiState.deliveryOptions.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            maxLines = 2
                        )
                    },
                    leadingContent = { Icon(Icons.Outlined.LocalShipping, "Delivery", tint = SddPink) },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, "", tint = SddPink) },
                    modifier = Modifier.clickable { viewModel.showDeliverySheet() }
                )
                Divider(Modifier.padding(horizontal = 16.dp))
                ListItem(
                    headlineContent = { Text("Return Policy") },
                    supportingContent = {
                        Text(
                            uiState.returnPolicy.ifBlank { "Tap to set return policy" },
                            color = if (uiState.returnPolicy.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    leadingContent = { Icon(Icons.Outlined.Refresh, "Return", tint = SddPink) },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, "", tint = SddPink) },
                    modifier = Modifier.clickable { viewModel.showReturnPolicySheet() }
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("More Options", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))
        Card(shape = RoundedCornerShape(16.dp)) {
            ListItem(
                headlineContent = { Text("Brand New Item") },
                supportingContent = { Text("Mark as new if never used") },
                leadingContent = { Icon(Icons.Outlined.NewReleases, "New", tint = SddPink) },
                trailingContent = { Switch(checked = uiState.isNew, onCheckedChange = { viewModel.toggleNew() }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SddPink)) }
            )
            Divider(Modifier.padding(horizontal = 16.dp))
            ListItem(
                headlineContent = { Text("Boost Listing") },
                supportingContent = { Text("Increase visibility in search results") },
                leadingContent = { Icon(Icons.Outlined.Bolt, "Boost", tint = SddPink) },
                trailingContent = { Switch(checked = uiState.isBoosted, onCheckedChange = { viewModel.toggleBoosted() }, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = SddPink)) }
            )
        }
        Spacer(Modifier.height(20.dp))

        Card(colors = CardDefaults.cardColors(containerColor = SddPink.copy(alpha = 0.05f)), shape = RoundedCornerShape(12.dp)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lightbulb, "", tint = SddPink, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Pricing Tip", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Research similar products to help you price it right. Competitive pricing sells faster.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun Step3PreviewContent(uiState: PostProductUiState, viewModel: PostProductViewModel) {
    Column(Modifier.padding(16.dp)) {
        Text("Review Your Listing", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text("Make sure everything looks right before posting", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        if (uiState.selectedImages.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.selectedImages.size) { i ->
                    AsyncImage(model = uiState.selectedImages[i], contentDescription = "Preview $i",
                        modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(uiState.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    Surface(color = SddPink.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text(uiState.category, color = SddPink, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("₹${uiState.price}", color = SddPink, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    if (uiState.discountPrice.isNotBlank() && (uiState.discountPrice.toDoubleOrNull() ?: 0.0) > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text("₹${uiState.discountPrice}", color = MaterialTheme.colorScheme.onSurfaceVariant, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                    }
                    if (uiState.isNegotiable) {
                        Spacer(Modifier.width(8.dp))
                        Surface(color = SuccessGreen.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                            Text("Negotiable", color = SuccessGreen, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(8.dp))
                PreviewRow(Icons.Outlined.CheckCircle, "Condition", uiState.condition)
                if (uiState.location.isNotBlank()) PreviewRow(Icons.Outlined.LocationOn, "Location", uiState.location)
                if (uiState.deliveryOptions.isNotEmpty()) PreviewRow(Icons.Outlined.LocalShipping, "Delivery", uiState.deliveryOptions.joinToString(", "))
                if (uiState.returnPolicy.isNotBlank()) PreviewRow(Icons.Outlined.Refresh, "Returns", uiState.returnPolicy)
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(8.dp))
                Text("Description", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(uiState.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 5)
            }
        }
    }
}

@Composable
fun PreviewRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, label, tint = SddPink, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("$label: ", fontWeight = FontWeight.Medium, fontSize = 13.sp)
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchSheet(
    suggestions: List<LocationSuggestion>,
    isSearching: Boolean,
    onSearch: (String) -> Unit,
    onSelect: (LocationSuggestion) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(query) {
        kotlinx.coroutines.delay(300)
        if (query.isNotBlank()) onSearch(query)
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 16.dp).fillMaxWidth()) {
            Text("Search Location", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search for a city, area or address...") },
                leadingIcon = { Icon(Icons.Outlined.Search, "Search") },
                trailingIcon = {
                    if (isSearching) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = SddPink, strokeWidth = 2.dp)
                    else if (query.isNotBlank()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Clear, "Clear") }
                },
                singleLine = true, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SddPink)
            )
            Spacer(Modifier.height(8.dp))
            Text("Powered by OpenStreetMap", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))

            if (query.isBlank()) {
                val popularLocations = listOf("Mumbai, India", "Delhi, India", "Bangalore, India", "Hyderabad, India", "Chennai, India", "Kolkata, India", "Pune, India", "Ahmedabad, India")
                Text("Popular Cities", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(popularLocations) { loc ->
                        FilterChip(
                            selected = false, onClick = {
                                val parts = loc.split(",")
                                onSelect(LocationSuggestion(displayName = loc, shortName = loc, lat = 0.0, lng = 0.0))
                            },
                            label = { Text(loc.split(",").first()) },
                            leadingIcon = { Icon(Icons.Outlined.LocationOn, "Location", modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
            }

            LazyColumn(Modifier.heightIn(max = 320.dp)) {
                if (suggestions.isEmpty() && query.isNotBlank() && !isSearching) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            Text("No results found for \"$query\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                items(suggestions) { suggestion ->
                    ListItem(
                        headlineContent = { Text(suggestion.shortName, fontWeight = FontWeight.Medium) },
                        supportingContent = { Text(suggestion.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall) },
                        leadingContent = { Icon(Icons.Outlined.LocationOn, "Location", tint = SddPink) },
                        modifier = Modifier.clickable { onSelect(suggestion) }
                    )
                    Divider()
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryOptionsSheet(
    selected: List<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val options = listOf(
        "Free Delivery" to Icons.Outlined.LocalShipping,
        "Paid Delivery" to Icons.Outlined.Payment,
        "Pickup Only" to Icons.Outlined.Store,
        "Negotiable" to Icons.Outlined.Handshake
    )

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Text("Delivery Options", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text("Select all that apply", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))

            options.forEach { (option, icon) ->
                val isSelected = selected.contains(option)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onToggle(option) },
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) SddPink.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface),
                    border = if (isSelected) BorderStroke(1.5.dp, SddPink) else null,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, option, tint = if (isSelected) SddPink else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(option, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = if (isSelected) SddPink else MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        if (isSelected) Icon(Icons.Filled.CheckCircle, "Selected", tint = SddPink, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            SddButton("Confirm", onClick = onDismiss)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnPolicySheet(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val policies = listOf(
        "No Returns" to "All sales are final. No returns accepted.",
        "7 Days Return" to "Buyer can return within 7 days of delivery.",
        "15 Days Return" to "Buyer can return within 15 days of delivery.",
        "30 Days Return" to "Buyer can return within 30 days of delivery.",
        "Exchange Only" to "Exchange accepted within 7 days. No refunds.",
        "Negotiable" to "Return policy can be discussed with buyer."
    )

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(16.dp).fillMaxWidth()) {
            Text("Return Policy", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(4.dp))
            Text("Select a return policy for your listing", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))

            policies.forEach { (policy, description) ->
                val isSelected = selected == policy
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onSelect(policy) },
                    colors = CardDefaults.cardColors(containerColor = if (isSelected) SddPink.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface),
                    border = if (isSelected) BorderStroke(1.5.dp, SddPink) else null,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        RadioButton(selected = isSelected, onClick = { onSelect(policy) }, colors = RadioButtonDefaults.colors(selectedColor = SddPink))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(policy, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal, color = if (isSelected) SddPink else MaterialTheme.colorScheme.onSurface)
                            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductPreviewSheet(
    uiState: PostProductUiState,
    onDismiss: () -> Unit,
    onPost: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Listing Preview", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Close") }
            }
            Divider()
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                if (uiState.selectedImages.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(uiState.selectedImages.size) { i ->
                            AsyncImage(model = uiState.selectedImages[i], contentDescription = "Preview",
                                modifier = Modifier.fillParentMaxWidth(0.8f).height(220.dp).clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                Text(uiState.title.ifBlank { "Product Title" }, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("₹${uiState.price}", color = SddPink, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                    if (uiState.discountPrice.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text("₹${uiState.discountPrice}", color = MaterialTheme.colorScheme.onSurfaceVariant, textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (uiState.category.isNotBlank()) Surface(color = SddPink.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp)) { Text(uiState.category, color = SddPink, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }
                    if (uiState.condition.isNotBlank()) Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) { Text(uiState.condition, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }
                }
                Spacer(Modifier.height(16.dp))
                if (uiState.description.isNotBlank()) {
                    Text("Description", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(uiState.description, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(24.dp))
                SddButton("Post Listing", onClick = onPost, isLoading = uiState.isLoading)
                Spacer(Modifier.height(8.dp))
                SddOutlineButton("Edit Listing", onClick = onDismiss)
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun PostSuccessScreen(onNavigateHome: () -> Unit, onBoostListing: () -> Unit = {}) {
    Column(
        Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(120.dp).clip(CircleShape).background(SuccessGreen.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.CheckCircle, "Success", tint = SuccessGreen, modifier = Modifier.size(72.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Listing Posted!", fontWeight = FontWeight.Bold, fontSize = 26.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Your product has been listed successfully! Buyers can now find and purchase your item.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        Card(colors = CardDefaults.cardColors(containerColor = SddPink.copy(alpha = 0.05f)), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                listOf(
                    Icons.Outlined.Visibility to "Your listing is now live",
                    Icons.Outlined.Notifications to "You'll be notified when someone is interested",
                    Icons.Outlined.Chat to "Buyers can message you directly"
                ).forEach { (icon, text) ->
                    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, text, tint = SddPink, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Card(
            colors   = CardDefaults.cardColors(containerColor = SddPink.copy(alpha = 0.06f)),
            shape    = RoundedCornerShape(16.dp),
            border   = BorderStroke(1.dp, SddPink.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⚡ Reach more buyers, faster!", fontWeight = FontWeight.Bold, color = SddPink)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Boost your listing from just \$1.99 and get guaranteed premium placement across the app.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick  = onBoostListing,
                    modifier = Modifier.fillMaxWidth(),
                    colors   = ButtonDefaults.buttonColors(containerColor = SddPink),
                    shape    = RoundedCornerShape(10.dp)
                ) {
                    Text("Boost This Listing 🚀", fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SddButton("Go to Home", onClick = onNavigateHome)
    }
}

@Composable
fun KycRequiredGate(
    onNavigateToKyc: () -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post a Listing") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Outlined.VerifiedUser,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = SddPink
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "Identity Verification Required",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "To keep our marketplace safe and trusted, you need to complete identity verification before posting a listing.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))
            listOf(
                Icons.Outlined.Shield to "Protects buyers and sellers",
                Icons.Outlined.Lock to "Keeps your account secure",
                Icons.Outlined.CheckCircle to "Quick & easy process"
            ).forEach { (icon, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null, tint = SddPink, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(32.dp))
            SddButton(
                text = "Verify My Identity",
                onClick = onNavigateToKyc,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onNavigateBack) {
                Text("Maybe Later", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
