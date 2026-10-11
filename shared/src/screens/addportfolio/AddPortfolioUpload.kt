package ir.farimod.app.screens.addportfolio

import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import ir.farimod.app.network.ApiFilePart
import ir.farimod.app.network.ApiNetworkException
import ir.farimod.app.network.ApiResponse
import ir.farimod.app.network.FarimodHttpClient
import ir.farimod.app.network.str
import ir.farimod.app.ui.ImageItem
import ir.farimod.app.utils.currentTimeMillis
import ir.farimod.app.utils.jsTruthy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject

internal data class PortfolioImageSlot(
    val slot: Int,
    val uri: String,
    val name: String,
    val isNew: Boolean,
    val isRemovedOld: Boolean,
    val readBytes: (suspend () -> ByteArray)? = null
)

private fun fileNameField(slot: Int): String = when (slot) {
    1 -> "FirstImageFileName"
    2 -> "SecondImageFileName"
    3 -> "ThirdImageFileName"
    4 -> "FourthImageFileName"
    else -> "FifthImageFileName"
}

private fun isServerImage(id: String?): Boolean = id != null && id.startsWith("portfolio-initial-index-")

/** RN `computeSlotAssignments`. */
internal fun computePortfolioSlots(
    current: List<ImageItem>,
    edit: JsonObject?,
    isEdit: Boolean
): List<PortfolioImageSlot> {
    val newImages = current.filter { !isServerImage(it.id) }
    var newIndex = 0
    val slots = ArrayList<PortfolioImageSlot>(5)
    for (slot in 1..5) {
        val old = current.find { it.id == "portfolio-initial-index-$slot" }
        val field = fileNameField(slot)
        val hadOnServer = isEdit && edit != null && jsTruthy(edit[field])
        if (old != null) {
            val serverName = edit.str(field)?.takeIf { it.isNotEmpty() }
            slots.add(
                PortfolioImageSlot(
                    slot = slot,
                    uri = old.uri,
                    name = serverName ?: (old.name ?: ""),
                    isNew = false,
                    isRemovedOld = false
                )
            )
            continue
        }
        val fresh = newImages.getOrNull(newIndex)
        if (fresh != null) {
            newIndex += 1
            slots.add(
                PortfolioImageSlot(
                    slot = slot,
                    uri = fresh.uri,
                    name = fresh.name ?: "",
                    isNew = true,
                    isRemovedOld = false,
                    readBytes = fresh.readBytes
                )
            )
            continue
        }
        slots.add(
            PortfolioImageSlot(
                slot = slot,
                uri = "",
                name = "",
                isNew = false,
                isRemovedOld = hadOnServer
            )
        )
    }
    return slots
}

internal fun imageMime(uri: String): String {
    val ext = uri.substringAfterLast('.', "jpg").substringBefore('?').lowercase()
    return if (ext == "png") "image/png" else "image/jpeg"
}

internal fun imageExtension(uri: String): String {
    val ext = uri.substringAfterLast('.', "jpg").substringBefore('?').lowercase()
    return if (ext == "png") "png" else "jpg"
}

/**
 * Multipart POST used by Portfolio/UploadImage.
 * An empty body (no file) is the RN clear-slot request.
 */
internal suspend fun postPortfolioMultipart(path: String, file: ApiFilePart?): ApiResponse {
    try {
        val content = MultiPartFormDataContent(
            formData {
                if (file != null) {
                    append(
                        file.fieldName,
                        file.bytes,
                        Headers.build {
                            append(HttpHeaders.ContentType, file.mimeType)
                            append(HttpHeaders.ContentDisposition, "filename=\"${file.fileName}\"")
                        }
                    )
                }
            }
        )
        val response = FarimodHttpClient.client.post {
            url(path)
            header(HttpHeaders.Accept, "application/json")
            timeout {
                requestTimeoutMillis = 120_000
                socketTimeoutMillis = 120_000
            }
            setBody(content)
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

private suspend fun uploadWithRetry(path: String, file: ApiFilePart?, maxRetries: Int = 2): Boolean {
    for (attempt in 1..maxRetries) {
        try {
            val response = postPortfolioMultipart(path, file)
            if (response.ok) return true
            if (attempt == maxRetries) return false
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            if (attempt == maxRetries) return false
        }
        if (attempt < maxRetries) delay(5000L * attempt)
    }
    return false
}

/** RN `uploadImages`: featured type 0, then slots 1..5, 1500ms between calls. */
internal suspend fun uploadPortfolioImages(
    portfolioId: Int,
    featured: List<ImageItem>,
    slots: List<PortfolioImageSlot>
) {
    val featuredImage = featured.firstOrNull()
    if (featuredImage != null) {
        val isNew = !isServerImage(featuredImage.id)
        if (featuredImage.uri.isNotEmpty() && isNew) {
            try {
                val ext = imageExtension(featuredImage.uri)
                val bytes = featuredImage.bytesOrNull()
                if (bytes != null) {
                    val part = ApiFilePart(
                        "ImageFile",
                        featuredImage.name?.takeIf { it.isNotEmpty() } ?: "featured.$ext",
                        imageMime(featuredImage.uri),
                        bytes
                    )
                    uploadWithRetry(
                        "Portfolio/UploadImage?portfolioId=$portfolioId&type=0",
                        part
                    )
                    delay(1500)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }
    for (slot in slots) {
        if (slot.isRemovedOld) {
            try {
                uploadWithRetry(
                    "Portfolio/UploadImage?portfolioId=$portfolioId&type=${slot.slot}",
                    null
                )
                delay(1500)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
            continue
        }
        if (slot.isNew && slot.uri.isNotEmpty()) {
            try {
                val ext = imageExtension(slot.uri)
                val bytes = slot.readBytes?.invoke()
                if (bytes != null) {
                    val name = slot.name.takeIf { it.isNotEmpty() }
                        ?: "portfolio-${slot.slot}-${currentTimeMillis()}.$ext"
                    uploadWithRetry(
                        "Portfolio/UploadImage?portfolioId=$portfolioId&type=${slot.slot}",
                        ApiFilePart("ImageFile", name, imageMime(slot.uri), bytes)
                    )
                    delay(1500)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }
}

/** `new Date().toISOString()` */
internal fun portfolioIsoNow(epoch: Long = currentTimeMillis()): String {
    var z = epoch / 86_400_000L
    val tod = ((epoch % 86_400_000L) + 86_400_000L) % 86_400_000L
    z += 719468
    val era = if (z >= 0) z / 146097 else (z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    var y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = if (mp < 10) mp + 3 else mp - 9
    if (m <= 2) y += 1
    val hh = tod / 3_600_000
    val mm = (tod % 3_600_000) / 60_000
    val ss = (tod % 60_000) / 1000
    val millis = tod % 1000
    fun p(n: Long, w: Int = 2) = n.toString().padStart(w, '0')
    return "$y-${p(m)}-${p(d)}T${p(hh)}:${p(mm)}:${p(ss)}.${p(millis, 3)}Z"
}