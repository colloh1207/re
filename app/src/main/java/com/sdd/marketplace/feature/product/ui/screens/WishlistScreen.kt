package com.sdd.marketplace.feature.product.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.sdd.marketplace.core.navigation.Screen
import com.sdd.marketplace.core.ui.components.*
import com.sdd.marketplace.core.ui.theme.SddPink
import com.sdd.marketplace.domain.model.Product
import com.sdd.marketplace.domain.repository.FavoriteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WishlistViewModel @Inject constructor(private val favoriteRepository: FavoriteRepository) : ViewModel() {
    val favorites: StateFlow<List<Product>> = MutableStateFlow<List<Product>>(emptyList()).also { flow ->
        viewModelScope.launch { favoriteRepository.getFavorites().collect { flow.value = it } }
    }
    fun removeFavorite(productId: String) = viewModelScope.launch { favoriteRepository.removeFavorite(productId) }
}

@Composable
fun WishlistScreen(navController: NavController, viewModel: WishlistViewModel = hiltViewModel()) {
    val favorites by viewModel.favorites.collectAsState()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Filled.ArrowBack, "Back") } },
            title = { Text("Wishlist", fontWeight = FontWeight.Bold) }
        )
        if (favorites.isEmpty()) {
            EmptyState("Wishlist is empty", "Save products you love by tapping the ♥ button", icon = { Icon(Icons.Filled.FavoriteBorder, "Empty", modifier = Modifier.size(64.dp)) })
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(favorites) { product ->
                    ProductCard(
                        imageUrl = product.images.firstOrNull(), title = product.title, price = product.price,
                        sellerName = product.seller?.fullName ?: "", sellerAvatarUrl = product.seller?.avatarUrl,
                        isVerified = product.seller?.isVerified == true,
                        isFavorite = true,
                        onFavoriteClick = { viewModel.removeFavorite(product.id) },
                        onClick = { navController.navigate(Screen.ProductDetail.createRoute(product.id)) }
                    )
                }
            }
        }
    }
}
