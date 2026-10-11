package ir.farimod.app.screens.addnewpost

import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import ir.farimod.app.network.ApiFilePart
import ir.farimod.app.network.ApiNetworkException
import ir.farimod.app.network.ApiResponse
import ir.farimod.app.network.FarimodHttpClient
import kotlinx.coroutines.CancellationException

/**
 * Multipart BlogPost/Add or BlogPost/Edit.
 *
 * RN `FormData.append('cateogoryIdList', id)` is called once per category (the same field name repeats).
 * [ir.farimod.app.network.Api.upload] takes `Map<String, String>` and cannot repeat a key, so this helper
 * posts through the same [FarimodHttpClient] and appends every pair in order.
 */
internal suspend fun submitBlogPostMultipart(
    edit: Boolean,
    fields: List<Pair<String, String>>,
    file: ApiFilePart?
): ApiResponse {
    val path = if (edit) "BlogPost/Edit" else "BlogPost/Add"
    try {
        val content = MultiPartFormDataContent(
            formData {
                for ((k, v) in fields) append(k, v)
                if (file != null) {
                    append(
                        file.fieldName,
                        file.bytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, file.mimeType)
                            append(
                                HttpHeaders.ContentDisposition,
                                "filename=\"${file.fileName}\""
                            )
                        }
                    )
                }
            }
        )
        val response = if (edit) {
            FarimodHttpClient.client.put {
                url(path)
                header(HttpHeaders.Accept, "application/json")
                timeout {
                    requestTimeoutMillis = 120_000
                    socketTimeoutMillis = 120_000
                }
                setBody(content)
            }
        } else {
            FarimodHttpClient.client.post {
                url(path)
                header(HttpHeaders.Accept, "application/json")
                timeout {
                    requestTimeoutMillis = 120_000
                    socketTimeoutMillis = 120_000
                }
                setBody(content)
            }
        }
        val text = try {
            response.bodyAsText()
        } catch (_: Exception) {
            ""
        }
        return ApiResponse(response.status.value, text)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        val m = (e.message ?: "").lowercase()
        if (m.contains("timeout") || m.contains("timed out")) throw ApiNetworkException("Request timeout", e)
        throw ApiNetworkException("Network request failed", e)
    }
}
