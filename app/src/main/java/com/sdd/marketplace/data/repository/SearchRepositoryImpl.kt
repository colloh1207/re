package com.sdd.marketplace.data.repository

import android.content.Context
import com.sdd.marketplace.data.mappers.toDomain
import com.sdd.marketplace.data.remote.dto.ProductDto
import com.sdd.marketplace.data.remote.dto.UserDto
import com.sdd.marketplace.domain.repository.SearchRepository
import com.sdd.marketplace.domain.repository.SearchResults
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    @ApplicationContext private val context: Context
) : SearchRepository {

    private val prefs = context.getSharedPreferences("search_prefs", Context.MODE_PRIVATE)
    private val _recentSearches = MutableStateFlow<List<String>>(
        prefs.getStringSet("recent", emptySet())?.toList() ?: emptyList()
    )

    override suspend fun searchAll(query: String): Result<SearchResults> = runCatching {
        val products = postgrest["products"].select {
            filter {
                or {
                    ilike("title", "%$query%")
                    ilike("description", "%$query%")
                    ilike("category", "%$query%")
                }
            }
            limit(20)
        }.decodeList<ProductDto>()

        val sellers = postgrest["users"].select {
            filter {
                and {
                    eq("is_seller", true)
                    ilike("full_name", "%$query%")
                }
            }
            limit(10)
        }.decodeList<UserDto>()

        SearchResults(products = products.map { it.toDomain() }, sellers = sellers.map { it.toDomain() })
    }

    override fun getRecentSearches(): Flow<List<String>> = _recentSearches.asStateFlow()

    override suspend fun addRecentSearch(query: String) {
        val current = _recentSearches.value.toMutableList()
        current.remove(query)
        current.add(0, query)
        val trimmed = current.take(10)
        _recentSearches.value = trimmed
        prefs.edit().putStringSet("recent", trimmed.toSet()).apply()
    }

    override suspend fun clearRecentSearches() {
        _recentSearches.value = emptyList()
        prefs.edit().remove("recent").apply()
    }

    override fun getSearchSuggestions(query: String): Flow<List<String>> = flow {
        if (query.isBlank()) { emit(emptyList()); return@flow }
        try {
            val products = postgrest["products"].select {
                filter { ilike("title", "%$query%") }
                limit(5)
            }.decodeList<ProductDto>()
            emit(products.map { it.title })
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
}
