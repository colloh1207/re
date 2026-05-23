package com.sdd.marketplace.feature.home.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.sdd.marketplace.domain.model.Category
import com.sdd.marketplace.domain.model.Product
import com.sdd.marketplace.domain.repository.FavoriteRepository
import com.sdd.marketplace.domain.repository.NotificationRepository
import com.sdd.marketplace.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Random
import javax.inject.Inject

data class HomeUiState(
    val featuredProducts: List<Product> = emptyList(),
    val sponsoredProducts: List<Product> = emptyList(),
    val rotationProducts: List<Product> = emptyList(),
    val nearbyProducts: List<Product> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedCategory: String? = null,
    val searchQuery: String = "",
    val isRefreshing: Boolean = false,
    val unreadNotifications: Int = 0,
    val unreadMessages: Int = 0,
    val error: String? = null,
    val userLat: Double? = null,
    val userLng: Double? = null,
    val nearMeRadius: Float = 25f,
    val locationPermissionGranted: Boolean = false,
    val showLocationRationale: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val favoriteRepository: FavoriteRepository,
    private val notificationRepository: NotificationRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _searchQuery = MutableStateFlow("")

    val products: Flow<PagingData<Product>> = combine(
        _selectedCategory, _searchQuery
    ) { category, query -> Pair(category, query) }
        .flatMapLatest { (category, query) ->
            productRepository.getProducts(category = category, searchQuery = query.ifBlank { null })
        }
        .cachedIn(viewModelScope)

    init {
        loadInitialData()
        observeNotifications()
        subscribeToRealtimeProducts()
        startRotationTimer()
    }

    private fun loadInitialData() = viewModelScope.launch {
        productRepository.getFeaturedProducts().collect { featured ->
            val boosted = featured.filter { it.isBoosted }
            val sponsored = featured.filter { !it.isBoosted }
            val rotation = computeRotation(boosted + sponsored)
            _uiState.update { it.copy(
                featuredProducts = boosted,
                sponsoredProducts = sponsored,
                rotationProducts = rotation
            )}
        }
    }

    private fun computeRotation(products: List<Product>): List<Product> {
        val slotMinutes = 30L
        val seed = System.currentTimeMillis() / (slotMinutes * 60_000L)
        return products.distinctBy { it.id }.shuffled(Random(seed))
    }

    private fun startRotationTimer() = viewModelScope.launch {
        val slotMs = 30L * 60_000L
        while (true) {
            val now = System.currentTimeMillis()
            val nextSlot = (now / slotMs + 1) * slotMs
            delay(nextSlot - now)
            val state = _uiState.value
            val all = (state.featuredProducts + state.sponsoredProducts)
            if (all.isNotEmpty()) {
                _uiState.update { it.copy(rotationProducts = computeRotation(all)) }
            }
        }
    }

    private fun observeNotifications() = viewModelScope.launch {
        notificationRepository.getUnreadCount().collect { count ->
            _uiState.update { it.copy(unreadNotifications = count) }
        }
    }

    private fun subscribeToRealtimeProducts() = viewModelScope.launch {
        try {
            productRepository.subscribeToRealtimeProducts()
                .debounce(3_000L)
                .collect {
                    productRepository.getFeaturedProducts().take(1).collect { featured ->
                        val boosted = featured.filter { p -> p.isBoosted }
                        val sponsored = featured.filter { p -> !p.isBoosted }
                        _uiState.update { s -> s.copy(featuredProducts = boosted, sponsoredProducts = sponsored) }
                    }
                }
        } catch (e: Exception) { Timber.e(e, "Realtime subscription error") }
    }

    fun selectCategory(category: String?) {
        _selectedCategory.value = category
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun refreshProducts() = viewModelScope.launch {
        _uiState.update { it.copy(isRefreshing = true) }
        loadInitialData()
        _uiState.update { it.copy(isRefreshing = false) }
    }

    fun refreshRotation() {
        val state = _uiState.value
        val all = (state.featuredProducts + state.sponsoredProducts)
        if (all.isNotEmpty()) {
            _uiState.update { it.copy(rotationProducts = computeRotation(all)) }
        }
    }

    fun toggleFavorite(productId: String) = viewModelScope.launch {
        favoriteRepository.toggleFavorite(productId)
    }

    @SuppressLint("MissingPermission")
    fun onLocationPermissionGranted() = viewModelScope.launch {
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val location = fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
            location?.let { loc ->
                _uiState.update { it.copy(userLat = loc.latitude, userLng = loc.longitude, locationPermissionGranted = true) }
                loadNearbyProducts(loc.latitude, loc.longitude, _uiState.value.nearMeRadius.toDouble())
            }
        } catch (e: Exception) {
            Timber.e(e, "Location error")
            _uiState.update { it.copy(locationPermissionGranted = false) }
        }
    }

    fun onLocationPermissionDenied() {
        _uiState.update { it.copy(locationPermissionGranted = false, showLocationRationale = false) }
    }

    fun showLocationRationale() = _uiState.update { it.copy(showLocationRationale = true) }
    fun dismissLocationRationale() = _uiState.update { it.copy(showLocationRationale = false) }

    fun onNearMeRadiusChanged(radius: Float) {
        _uiState.update { it.copy(nearMeRadius = radius) }
        val state = _uiState.value
        if (state.userLat != null && state.userLng != null) {
            viewModelScope.launch { loadNearbyProducts(state.userLat, state.userLng, radius.toDouble()) }
        }
    }

    private fun loadNearbyProducts(lat: Double, lng: Double, radiusKm: Double) = viewModelScope.launch {
        productRepository.getNearbyProducts(lat, lng, radiusKm).collect { products ->
            _uiState.update { it.copy(nearbyProducts = products) }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
