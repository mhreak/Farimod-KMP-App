package ir.farimod.app.network

import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.plugins.timeout
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import ir.farimod.app.storage.AuthTokenStorage
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

/** Thrown when the request could not be completed at all (React Native: "Network request failed"). */
class ApiNetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** A multipart file part (React Native: `formData.append(name, { uri, name, type })`). */
class ApiFilePart(
    val fieldName: String,
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray
)

/**
 * Thin `fetch`-like client mirroring the way the React Native app talks to the backend.
 *
 * - [get]/[post]/[put]/[delete] return an [ApiResponse] regardless of HTTP status (like `fetch`),
 *   they only throw [ApiNetworkException] when the network call itself fails.
 * - `path` is relative to https://my.farimod.ir/api/MobileApp/ and may already contain a query string.
 * - Pass `auth = true` to send `Authorization: Bearer <token>` (only where the RN code does it).
 */
object Api {
    private val storage by lazy { AuthTokenStorage() }
    private val client get() = FarimodHttpClient.client
    private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun HttpRequestBuilder.prepare(
        path: String,
        query: Map<String, Any?>,
        auth: Boolean,
        timeoutMs: Long?
    ) {
        val cleanPath = if (path.startsWith("http://") || path.startsWith("https://")) {
            path
        } else {
            path.removePrefix("/").removePrefix("api/MobileApp/")
        }
        url(cleanPath)
        url {
            for ((k, v) in query) {
                if (v != null) parameters.append(k, v.toString())
            }
        }
        header(HttpHeaders.Accept, "*/*")
        if (auth) {
            val token = storage.getToken()
            if (!token.isNullOrEmpty()) header(HttpHeaders.Authorization, "Bearer $token")
        }
        if (timeoutMs != null) {
            timeout {
                requestTimeoutMillis = timeoutMs
                socketTimeoutMillis = timeoutMs
            }
        }
    }

    private suspend fun wrap(block: suspend () -> HttpResponse): ApiResponse {
        try {
            val r = block()
            val text = try { r.bodyAsText() } catch (_: Exception) { "" }
            return ApiResponse(r.status.value, text)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val m = (e.message ?: "").lowercase()
            if (m.contains("timeout") || m.contains("timed out")) throw ApiNetworkException("Request timeout", e)
            throw ApiNetworkException("Network request failed", e)
        }
    }

    suspend fun get(
        path: String,
        query: Map<String, Any?> = emptyMap(),
        auth: Boolean = false,
        timeoutMs: Long? = null
    ): ApiResponse = wrap {
        client.get { prepare(path, query, auth, timeoutMs) }
    }

    suspend fun post(
        path: String,
        body: JsonElement? = null,
        query: Map<String, Any?> = emptyMap(),
        auth: Boolean = false,
        timeoutMs: Long? = null
    ): ApiResponse = wrap {
        client.post {
            prepare(path, query, auth, timeoutMs)
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
        }
    }

    suspend fun put(
        path: String,
        body: JsonElement? = null,
        query: Map<String, Any?> = emptyMap(),
        auth: Boolean = false,
        timeoutMs: Long? = null
    ): ApiResponse = wrap {
        client.put {
            prepare(path, query, auth, timeoutMs)
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
        }
    }

    suspend fun delete(
        path: String,
        query: Map<String, Any?> = emptyMap(),
        auth: Boolean = false,
        timeoutMs: Long? = null
    ): ApiResponse = wrap {
        client.delete { prepare(path, query, auth, timeoutMs) }
    }

    /** multipart/form-data POST (or PUT) — used for image / video / document uploads. */
    suspend fun upload(
        path: String,
        files: List<ApiFilePart>,
        fields: Map<String, String> = emptyMap(),
        query: Map<String, Any?> = emptyMap(),
        auth: Boolean = false,
        method: String = "POST",
        timeoutMs: Long? = 120_000
    ): ApiResponse = wrap {
        val content = MultiPartFormDataContent(
            formData {
                for ((k, v) in fields) append(k, v)
                for (f in files) {
                    append(
                        f.fieldName,
                        f.bytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, f.mimeType)
                            append(HttpHeaders.ContentDisposition, "filename=\"${f.fileName}\"")
                        }
                    )
                }
            }
        )
        if (method.equals("PUT", true)) {
            client.put {
                prepare(path, query, auth, timeoutMs)
                setBody(content)
            }
        } else {
            client.post {
                prepare(path, query, auth, timeoutMs)
                setBody(content)
            }
        }
    }
}

/** Result of an [Api] call (React Native: the `Response` returned by `fetch`, with body already read). */
class ApiResponse(val status: Int, val text: String) {
    val ok: Boolean get() = status in 200..299
    private val parsed: JsonElement? by lazy {
        if (text.isBlank()) null
        else try { Json { ignoreUnknownKeys = true; isLenient = true }.parseToJsonElement(text) } catch (_: Exception) { null }
    }

    /** Parsed JSON body, or null when the body is empty / not JSON (React Native: `await response.json()` would throw). */
    val json: JsonElement? get() = parsed

    /** Parsed JSON body or an empty object. Handy when RN code does `data.Message || ...`. */
    val jsonOrEmpty: JsonElement get() = parsed ?: JsonObject(emptyMap())
}

@Suppress("unused")
private val keepJsonNull = JsonNull
