package com.sdd.marketplace.feature.search.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sdd.marketplace.domain.model.Product
import com.sdd.marketplace.domain.model.User
import com.sdd.marketplace.domain.repository.SearchRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Product> = emptyList(),
    val sellerResults: List<User> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSearching: Boolean = false,
    val selectedPriceMin: Double? = null,
    val selectedPriceMax: Double? = null,
    val selectedCategory: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _query = MutableStateFlow("")

    init {
        loadRecentSearches()
        observeQuery()
    }

    private fun loadRecentSearches() = viewModelScope.launch {
        searchRepository.getRecentSearches().collect { recent ->
            _uiState.update { it.copy(recentSearches = recent) }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeQuery() = viewModelScope.launch {
        _query.debounce(300).distinctUntilChanged().collect { query ->
            if (query.isBlank()) {
                _uiState.update { it.copy(results = emptyList(), sellerResults = emptyList(), isSearching = false) }
                return@collect
            }
            _uiState.update { it.copy(isLoading = true, isSearching = true) }
            searchRepository.searchAll(query)
                .onSuccess { results ->
                    _uiState.update { it.copy(
                        results = results.products,
                        sellerResults = results.sellers,
                        isLoading = false
                    )}
                }
                .onFailure { _uiState.update { s -> s.copy(error = it.message, isLoading = false) } }
        }
    }

    fun onQueryChanged(query: String) {
        _query.value = query
        _uiState.update { it.copy(query = query) }
        if (query.isNotBlank()) {
            viewModelScope.launch {
                searchRepository.getSearchSuggestions(query).collect { suggestions ->
                    _uiState.update { it.copy(suggestions = suggestions) }
                }
            }
        }
    }

    fun search(query: String) = viewModelScope.launch {
        if (query.isBlank()) return@launch
        searchRepository.addRecentSearch(query)
        onQueryChanged(query)
    }

    fun clearRecentSearches() = viewModelScope.launch { searchRepository.clearRecentSearches() }

    fun setPriceFilter(min: Double?, max: Double?) = _uiState.update {
        it.copy(selectedPriceMin = min, selectedPriceMax = max)
    }

    fun setCategoryFilter(category: String?) = _uiState.update { it.copy(selectedCategory = category) }
}
