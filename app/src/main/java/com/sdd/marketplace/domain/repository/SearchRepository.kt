package com.sdd.marketplace.domain.repository

import com.sdd.marketplace.domain.model.Product
import com.sdd.marketplace.domain.model.User
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    suspend fun searchAll(query: String): Result<SearchResults>
    fun getRecentSearches(): Flow<List<String>>
    suspend fun addRecentSearch(query: String)
    suspend fun clearRecentSearches()
    fun getSearchSuggestions(query: String): Flow<List<String>>
}

data class SearchResults(
    val products: List<Product>,
    val sellers: List<User>
)
