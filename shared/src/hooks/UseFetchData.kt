package ir.farimod.app.hooks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import ir.farimod.app.network.Api
import ir.farimod.app.network.get
import ir.farimod.app.network.list
import ir.farimod.app.utils.jsTruthy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement

/**
 * Return value of RN `useFetchData` → `{ data, loading, error, refetch }`.
 * `data` items are the API JSON objects (RN plain objects).
 */
class FetchDataState(
    val data: List<JsonElement>,
    val loading: Boolean,
    val error: String?,
    /** RN: `refetch()` (fire and forget; state is updated when the request completes). */
    val refetch: () -> Unit
)

/**
 * Port of `app/hooks/useFetchData.ts`.
 *
 * @param url path relative to the mobile API (RN: `${appConfig.mobileApi}Portfolio/GetAllLast`,
 *            may contain a query string, e.g. "MemberGroup/GetAll?currentPage=1&pageSize=100").
 * @param errorMessage message stored in `error` when the response is not ok / has no `Data`.
 * @param filterOrTransform optional transform applied to `result.Data` (an array) before it is stored.
 */
@Composable
fun useFetchData(
    url: String,
    errorMessage: String,
    filterOrTransform: ((List<JsonElement>) -> List<JsonElement>)? = null
): FetchDataState {
    var data by remember { mutableStateOf<List<JsonElement>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    val transformRef by rememberUpdatedState(filterOrTransform)
    val scope = rememberCoroutineScope()

    // RN: fetchData(signal) — `useCallback` with [url, errorMessage]
    suspend fun fetchData() {
        try {
            loading = true
            error = null

            val response = Api.get(url)
            if (!currentCoroutineContext().isActive) return

            // RN: `await response.json()` throws when the body is not JSON
            val result = response.json ?: throw Exception("JSON Parse error: Unexpected end of input")

            // RN: `response.ok && result?.Data`
            val rawData = result["Data"]

            if (response.ok && jsTruthy(rawData)) {
                val items = rawData.list
                val processedData = transformRef?.invoke(items) ?: items
                data = processedData
            } else {
                error = errorMessage
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = "خطا در ارتباط با سرور"
        } finally {
            if (currentCoroutineContext().isActive) {
                loading = false
            }
        }
    }

    LaunchedEffect(url, errorMessage) {
        fetchData()
    }

    val refetch: () -> Unit = remember(url, errorMessage) {
        { scope.launch { fetchData() }; Unit }
    }

    return FetchDataState(data = data, loading = loading, error = error, refetch = refetch)
}
