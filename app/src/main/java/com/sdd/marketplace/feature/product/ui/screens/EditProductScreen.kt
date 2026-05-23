package com.sdd.marketplace.feature.product.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import com.sdd.marketplace.core.ui.components.*
import com.sdd.marketplace.core.ui.theme.*
import com.sdd.marketplace.feature.product.viewmodel.EditProductEvent
import com.sdd.marketplace.feature.product.viewmodel.EditProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    onNavigateBack: () -> Unit,
    onEditSuccess: () -> Unit,
    viewModel: EditProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EditProductEvent.UpdateSuccess -> onEditSuccess()
                is EditProductEvent.ShowError -> {}
            }
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { viewModel.addNewImage(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
                title = { Text("Edit Listing", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { viewModel.updateProduct() }, enabled = !uiState.isLoading) {
                        if (uiState.isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = SddPink, strokeWidth = 2.dp)
                        else Text("Save", color = SddPink, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoadingProduct) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SddPink) }
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Photos", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Box(Modifier.size(90.dp).clip(RoundedCornerShape(12.dp)).background(SddPink.copy(0.1f)).clickable { imageLauncher.launch("image/*") }, contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Add, "Add", tint = SddPink, modifier = Modifier.size(28.dp))
                            Text("Add", fontSize = 11.sp, color = SddPink)
                        }
                    }
                }
                items(uiState.existingImages.size) { i ->
                    Box(Modifier.size(90.dp)) {
                        AsyncImage(model = uiState.existingImages[i], contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                        if (i == 0) {
                            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(0.5f), RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)).padding(3.dp)) {
                                Text("Cover", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
                            }
                        }
                        IconButton(onClick = { viewModel.removeExistingImage(uiState.existingImages[i]) }, modifier = Modifier.size(22.dp).align(Alignment.TopEnd).background(Color.Black.copy(0.5f), CircleShape)) {
                            Icon(Icons.Filled.Close, "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                }
                items(uiState.newImages.size) { i ->
                    Box(Modifier.size(90.dp)) {
                        AsyncImage(model = uiState.newImages[i], contentDescription = null, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                        Surface(color = SddPink.copy(0.9f), shape = RoundedCornerShape(topStart = 0.dp, topEnd = 12.dp, bottomEnd = 0.dp, bottomStart = 12.dp), modifier = Modifier.align(Alignment.TopStart)) {
                            Text("NEW", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))

            Text("Product Details", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            SddTextField(value = uiState.title, onValueChange = { viewModel.updateTitle(it) }, label = "Product Title *", leadingIcon = { Icon(Icons.Outlined.Edit, "Title") })
            Spacer(Modifier.height(12.dp))

            var catExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = catExpanded, onExpandedChange = { catExpanded = it }) {
                OutlinedTextField(value = uiState.category.ifBlank { "Select Category" }, onValueChange = {}, readOnly = true, label = { Text("Category *") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(catExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SddPink))
                ExposedDropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                    listOf("Women", "Men", "Home & Living", "Beauty & Health", "Electronics", "Sports", "Kids", "Books", "Vehicles", "Other").forEach { cat ->
                        DropdownMenuItem(text = { Text(cat) }, onClick = { viewModel.updateCategory(cat); catExpanded = false })
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            var condExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = condExpanded, onExpandedChange = { condExpanded = it }) {
                OutlinedTextField(value = uiState.condition.ifBlank { "Select Condition" }, onValueChange = {}, readOnly = true, label = { Text("Condition *") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(condExpanded) }, modifier = Modifier.fillMaxWidth().menuAnchor(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SddPink))
                ExposedDropdownMenu(expanded = condExpanded, onDismissRequest = { condExpanded = false }) {
                    listOf("Brand New", "Like New", "Good", "Fair", "For Parts").forEach { cond ->
                        DropdownMenuItem(text = { Text(cond) }, onClick = { viewModel.updateCondition(cond); condExpanded = false })
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SddTextField(value = uiState.price, onValueChange = { viewModel.updatePrice(it) }, label = "Price ₹ *", modifier = Modifier.weight(1f), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
                SddTextField(value = uiState.discountPrice, onValueChange = { viewModel.updateDiscountPrice(it) }, label = "MRP ₹", modifier = Modifier.weight(1f), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal))
            }
            Spacer(Modifier.height(12.dp))
            SddTextField(value = uiState.description, onValueChange = { viewModel.updateDescription(it) }, label = "Description *", singleLine = false)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { viewModel.toggleNegotiable() }) {
                Checkbox(checked = uiState.isNegotiable, onCheckedChange = { viewModel.toggleNegotiable() }, colors = CheckboxDefaults.colors(checkedColor = SddPink))
                Text("Price is negotiable")
            }
            Spacer(Modifier.height(12.dp))
            SddTextField(value = uiState.location, onValueChange = { viewModel.updateLocation(it) }, label = "Location", leadingIcon = { Icon(Icons.Outlined.LocationOn, "Location") })
            Spacer(Modifier.height(24.dp))

            uiState.error?.let { err ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(8.dp)) {
                    Text(err, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(12.dp))
            }
            SddButton("Save Changes", onClick = { viewModel.updateProduct() }, isLoading = uiState.isLoading)
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = { /* TODO: show delete confirm */ }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.error), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Icon(Icons.Outlined.Delete, "Delete", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Delete Listing")
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}
