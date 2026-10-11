package ir.farimod.app.screens.mag

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ir.farimod.app.network.Api
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.list
import ir.farimod.app.utils.jsTruthy
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject

/**
 * Port of `screens/mag/hooks/useBlogPostsWithPagination.tsx`.
 * The hook's own page size is 10 (used by [loadMore]). MagScreen passes 20 on explicit fetches.
 */
internal class MagBlogPostsState {
    var data by mutableStateOf<List<JsonObject>>(emptyList())
    var page by mutableStateOf(1)
    var hasMore by mutableStateOf(true)
    var total by mutableStateOf(0)
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var filters by mutableStateOf<Map<String, Any?>>(emptyMap())

    private var requestIdRef = 0
    private var loadingRef = false

    suspend fun fetchBlogPosts(
        newPage: Int = 1,
        pageSize: Int = 10,
        filterParams: Map<String, Any?> = emptyMap()
    ) {
        if (loadingRef) return

        val requestId = ++requestIdRef
        loadingRef = true
        loading = true
        error = null

        try {
            val query = LinkedHashMap<String, Any?>()
            query["filterActive"] = "true"
            query["currentPage"] = newPage.toString()
            query["pageSize"] = pageSize.toString()
            if (jsTruthy(filterParams["filterTitle"])) query["filterTitle"] = filterParams["filterTitle"]
            if (jsTruthy(filterParams["filterCategoryId"])) query["filterCategoryId"] = filterParams["filterCategoryId"]
            if (jsTruthy(filterParams["filterMemberId"])) query["filterMemberId"] = filterParams["filterMemberId"]

            val response = Api.get("BlogPost/GetAll", query = query)
            if (!response.ok) throw Exception("HTTP ${response.status}")

            if (requestId != requestIdRef) return

            val result = response.json
            val newItems = result.list("Data").mapNotNull { it as? JsonObject }

            data = if (newPage == 1) newItems else data + newItems
            total = result.int("Total") ?: 0
            page = newPage
            filters = filterParams
            hasMore = newItems.size == pageSize
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (requestId != requestIdRef) return
            error = e.message ?: "Unknown error"
            if (newPage == 1) {
                data = emptyList()
                total = 0
            }
        } finally {
            loadingRef = false
            loading = false
        }
    }

    suspend fun loadMore() {
        if (!loadingRef && hasMore) {
            fetchBlogPosts(page + 1, 10, filters)
        }
    }
}

/** Port of `screens/mag/hooks/useBlogCategories.tsx`. */
internal class MagBlogCategoriesState {
    var categories by mutableStateOf<List<JsonObject>>(emptyList())
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    private var loadingRef = false
    private var disposed = false

    fun dispose() {
        disposed = true
    }

    suspend fun fetchCategories() {
        if (loadingRef) return
        loadingRef = true
        loading = true
        error = null
        try {
            val response = Api.get(
                "BlogPostCategory",
                query = mapOf(
                    "filterActive" to "true",
                    "currentPage" to "1",
                    "pageSize" to "20"
                )
            )
            if (!response.ok) throw Exception("HTTP ${response.status}")
            val data = response.json.list("Data").mapNotNull { it as? JsonObject }
            if (!disposed) categories = data
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!disposed) {
                error = e.message ?: "Unknown error"
                categories = emptyList()
            }
        } finally {
            loadingRef = false
            if (!disposed) loading = false
        }
    }
}

/** RN `value !== false && value !== '' && value !== 'all' && value !== undefined && value !== null`. */
internal fun magFilterValueActive(value: Any?): Boolean = when (value) {
    null -> false
    is Boolean -> value
    is String -> value.isNotEmpty() && value != "all"
    else -> true
}
