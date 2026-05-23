package com.sdd.marketplace.data.repository

import androidx.paging.*
import com.sdd.marketplace.BuildConfig
import com.sdd.marketplace.data.local.dao.ProductDao
import com.sdd.marketplace.data.mappers.toDomain
import com.sdd.marketplace.data.mappers.toEntity
import com.sdd.marketplace.data.remote.dto.CreateProductRequest
import com.sdd.marketplace.data.remote.dto.ProductDto
import com.sdd.marketplace.domain.model.Category
import com.sdd.marketplace.domain.model.Product
import com.sdd.marketplace.domain.repository.ProductRepository
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepositoryImpl @Inject constructor(
    private val postgrest: Postgrest,
    private val storage: Storage,
    private val productDao: ProductDao
) : ProductRepository {

    override fun getProducts(category: String?, searchQuery: String?): Flow<PagingData<Product>> {
        return Pager(
            config = PagingConfig(pageSize = 20, prefetchDistance = 5)
        ) {
            if (!searchQuery.isNullOrBlank())
                productDao.searchProducts(searchQuery)
            else if (!category.isNullOrBlank())
                productDao.getProductsByCategory(category)
            else
                productDao.getPagedProducts()
        }.flow.map { pagingData -> pagingData.map { it.toDomain() } }
            .onStart { refreshProducts(category) }
    }

    override fun getFeaturedProducts(): Flow<List<Product>> =
        productDao.getFeaturedProducts()
            .map { list -> list.map { it.toDomain() } }
            .onStart { refreshFeaturedProducts() }

    override fun getTrendingProducts(): Flow<List<Product>> =
        productDao.getFeaturedProducts().map { list -> list.map { it.toDomain() } }

    override fun getNearbyProducts(lat: Double, lng: Double, radiusKm: Double): Flow<List<Product>> =
        productDao.getAllProducts().map { list ->
            list.filter { it.latitude != null && it.longitude != null }
                .take(20)
                .map { it.toDomain() }
        }

    override fun getSellerProducts(sellerId: String): Flow<PagingData<Product>> =
        Pager(config = PagingConfig(pageSize = 20)) {
            productDao.getSellerProducts(sellerId)
        }.flow.map { pd -> pd.map { it.toDomain() } }

    override fun getCategories(): Flow<List<Category>> = flow {
        emit(defaultCategories())
    }

    override suspend fun getProduct(productId: String): Result<Product> = runCatching {
        val dto = postgrest["products"].select {
            filter { eq("id", productId) }
        }.decodeSingleOrNull<ProductDto>() ?: throw Exception("Product not found")
        val product = dto.toDomain()
        productDao.insertProduct(product.toEntity())
        product
    }

    override suspend fun createProduct(product: Product, imagePaths: List<String>): Result<Product> = runCatching {
        val imageUrls = imagePaths.mapIndexed { index, path ->
            val file = File(path)
            val fileName = "products/${product.sellerId}/${System.currentTimeMillis()}_$index.jpg"
            storage.from(BuildConfig.BUCKET_PRODUCTS).upload(fileName, file.readBytes()) {
                upsert = true
            }
            storage.from(BuildConfig.BUCKET_PRODUCTS).publicUrl(fileName)
        }
        val request = CreateProductRequest(
            title = product.title,
            description = product.description,
            price = product.price,
            discountPrice = product.discountPrice,
            category = product.category,
            brand = product.brand,
            condition = product.condition,
            stockQuantity = product.stockQuantity,
            images = imageUrls,
            tags = product.tags,
            attributes = product.attributes,
            sellerId = product.sellerId,
            location = product.location,
            latitude = product.latitude,
            longitude = product.longitude,
            deliveryOptions = product.deliveryOptions,
            returnPolicy = product.returnPolicy,
            isNegotiable = product.isNegotiable,
            isNew = product.isNew
        )
        val created = postgrest["products"].insert(request) { select() }
            .decodeSingle<ProductDto>()
        val domainProduct = created.toDomain()
        productDao.insertProduct(domainProduct.toEntity())
        domainProduct
    }

    override suspend fun updateProduct(product: Product): Result<Product> = runCatching {
        postgrest["products"].update(mapOf(
            "title" to product.title,
            "description" to product.description,
            "price" to product.price,
            "category" to product.category,
            "condition" to product.condition,
            "is_negotiable" to product.isNegotiable
        )) {
            filter { eq("id", product.id) }
            select()
        }.decodeSingle<ProductDto>().toDomain().also {
            productDao.insertProduct(it.toEntity())
        }
    }

    override suspend fun deleteProduct(productId: String): Result<Unit> = runCatching {
        postgrest["products"].delete { filter { eq("id", productId) } }
        productDao.deleteProduct(productId)
    }

    override suspend fun markAsSold(productId: String): Result<Unit> = runCatching {
        postgrest["products"].update(mapOf("is_sold" to true)) {
            filter { eq("id", productId) }
        }
    }

    override suspend fun archiveProduct(productId: String): Result<Unit> = runCatching {
        postgrest["products"].update(mapOf("is_archived" to true)) {
            filter { eq("id", productId) }
        }
    }

    override suspend fun boostProduct(productId: String): Result<Unit> = runCatching {
        postgrest["products"].update(mapOf("is_boosted" to true)) {
            filter { eq("id", productId) }
        }
    }

    override suspend fun incrementViewCount(productId: String) {
        try {
            postgrest.rpc("increment_product_views", buildJsonObject { put("product_id", productId) })
        } catch (e: Exception) {
            Timber.e(e, "Error incrementing view count")
        }
    }

    override fun getSoldProducts(sellerId: String): Flow<PagingData<Product>> =
        Pager(config = PagingConfig(pageSize = 20)) {
            productDao.getSoldProducts(sellerId)
        }.flow.map { pd -> pd.map { it.toDomain() } }
            .onStart { refreshSoldProducts(sellerId) }

    override fun getSavedProducts(userId: String): Flow<PagingData<Product>> =
        Pager(config = PagingConfig(pageSize = 20)) {
            productDao.getSavedProducts(userId)
        }.flow.map { pd -> pd.map { it.toDomain() } }
            .onStart { refreshSavedProducts(userId) }

    override fun getRelatedProducts(productId: String, category: String): Flow<List<Product>> = flow {
        try {
            val dtos = postgrest["products"].select {
                filter { eq("category", category); neq("id", productId) }
                limit(8)
                order("view_count", Order.DESCENDING)
            }.decodeList<ProductDto>()
            emit(dtos.map { it.toDomain() })
        } catch (e: Exception) { Timber.e(e); emit(emptyList()) }
    }

    override fun searchProducts(query: String): Flow<PagingData<Product>> =
        Pager(config = PagingConfig(pageSize = 20)) {
            productDao.searchProducts(query)
        }.flow.map { pd -> pd.map { it.toDomain() } }

    private suspend fun refreshProducts(category: String?) {
        try {
            val builder = postgrest["products"].select {
                if (!category.isNullOrBlank()) filter { eq("category", category) }
                limit(40)
                order("created_at", Order.DESCENDING)
            }
            val dtos = builder.decodeList<ProductDto>()
            productDao.insertProducts(dtos.map { it.toDomain().toEntity() })
        } catch (e: Exception) {
            Timber.e(e, "Error refreshing products")
        }
    }

    private suspend fun refreshFeaturedProducts() {
        try {
            val dtos = postgrest["products"].select {
                filter { eq("is_featured", true) }
                limit(20)
                order("created_at", Order.DESCENDING)
            }.decodeList<ProductDto>()
            productDao.insertProducts(dtos.map { it.toDomain().toEntity() })
        } catch (e: Exception) {
            Timber.e(e, "Error refreshing featured products")
        }
    }

    private suspend fun refreshSoldProducts(sellerId: String) {
        try {
            val dtos = postgrest["products"].select {
                filter { eq("seller_id", sellerId); eq("is_sold", true) }
                limit(40)
                order("updated_at", Order.DESCENDING)
            }.decodeList<ProductDto>()
            productDao.insertProducts(dtos.map { it.toDomain().toEntity() })
        } catch (e: Exception) { Timber.e(e, "Error refreshing sold products") }
    }

    private suspend fun refreshSavedProducts(userId: String) {
        try {
            val favoriteIds = postgrest["favorites"].select {
                filter { eq("user_id", userId) }
            }.decodeList<Map<String, String>>().mapNotNull { it["product_id"] }
            if (favoriteIds.isNotEmpty()) {
                val dtos = postgrest["products"].select {
                    filter { isIn("id", favoriteIds) }
                    limit(40)
                    order("created_at", Order.DESCENDING)
                }.decodeList<ProductDto>()
                productDao.insertProducts(dtos.map { it.toDomain().toEntity() })
            }
        } catch (e: Exception) { Timber.e(e, "Error refreshing saved products") }
    }

    override fun subscribeToRealtimeProducts(): Flow<Unit> = flow {
        try {
            val realtime = try {
                io.github.jan.supabase.realtime.Realtime
                emitAll(
                    flow {
                        while (true) {
                            kotlinx.coroutines.delay(30_000)
                            refreshFeaturedProducts()
                            emit(Unit)
                        }
                    }
                )
            } catch (e: Exception) { emitAll(emptyFlow()) }
        } catch (e: Exception) { Timber.e(e) }
    }

    private fun defaultCategories() = listOf(
        Category("popular", "Popular", "star", null),
        Category("women", "Women", "dress", null),
        Category("men", "Men", "tshirt", null),
        Category("home", "Home", "home", null),
        Category("beauty", "Beauty", "heart", null),
        Category("electronics", "Electronics", "phone", null),
        Category("all", "All", "grid", null)
    )
}
