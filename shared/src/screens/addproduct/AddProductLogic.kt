package ir.farimod.app.screens.addproduct

import ir.farimod.app.network.Api
import ir.farimod.app.network.ApiFilePart
import ir.farimod.app.network.ApiNetworkException
import ir.farimod.app.network.get
import ir.farimod.app.ui.ImageItem
import ir.farimod.app.utils.civilFromDays
import ir.farimod.app.utils.jsNow
import ir.farimod.app.utils.jsTruthy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonElement

internal const val PRODUCT_IMAGE_INITIAL_PREFIX = "product-initial-index-"

internal fun isServerProductImage(image: ImageItem): Boolean =
    image.id.startsWith(PRODUCT_IMAGE_INITIAL_PREFIX)

internal fun productImageSlot(image: ImageItem): Int? {
    if (!isServerProductImage(image)) return null
    return image.id.removePrefix(PRODUCT_IMAGE_INITIAL_PREFIX).toIntOrNull()
}

internal data class ProductImageSlot(
    val slot: Int,
    val isNew: Boolean,
    val item: ImageItem?
)

internal fun assignProductSlots(
    current: List<ImageItem>,
    edit: JsonElement?,
    isEdit: Boolean
): List<ProductImageSlot> {
    val fresh = current.filter { !isServerProductImage(it) }
    var freshIndex = 0
    return (1..5).map { slot ->
        val old = current.find { it.id == "$PRODUCT_IMAGE_INITIAL_PREFIX$slot" }
        val fileField = when (slot) {
            1 -> "FirstImageFileName"
            2 -> "SecondImageFileName"
            3 -> "ThirdImageFileName"
            4 -> "FourthImageFileName"
            else -> "FifthImageFileName"
        }
        when {
            old != null -> ProductImageSlot(slot, isNew = false, item = old)
            freshIndex < fresh.size -> {
                val image = fresh[freshIndex]
                freshIndex += 1
                ProductImageSlot(slot, isNew = true, item = image)
            }
            else -> ProductImageSlot(
                slot,
                isNew = false,
                item = null
            ).also {
                // hadImageOnServer is only used to mark removed slots; uploads skip them.
                if (isEdit && edit != null && jsTruthy(edit[fileField])) Unit
            }
        }
    }
}

internal fun removedServerSlots(previous: List<ImageItem>, next: List<ImageItem>): List<Int> {
    return previous.mapNotNull { prev ->
        if (!isServerProductImage(prev)) return@mapNotNull null
        if (next.any { it.id == prev.id }) return@mapNotNull null
        productImageSlot(prev)
    }
}

internal suspend fun deleteProductImage(productId: String, type: Int) {
    try {
        Api.post(
            "Product/DeleteProductImage",
            query = mapOf("productId" to productId, "type" to type)
        )
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
    }
}

private fun imageExt(uri: String): String {
    val raw = uri.substringAfterLast('.', "jpg").substringBefore('?').substringBefore('#').lowercase()
    return raw.ifEmpty { "jpg" }
}

private suspend fun postProductImage(productId: String, type: Int, name: String, mime: String, bytes: ByteArray) =
    Api.upload(
        path = "Product/AddProductImage",
        files = listOf(ApiFilePart("ProductImageFile", name, mime, bytes)),
        query = mapOf("productId" to productId, "type" to type),
        method = "POST",
        auth = false,
        timeoutMs = 60_000
    )

internal suspend fun uploadOneProductImage(productId: String, type: Int, item: ImageItem, fallbackName: String) {
    val ext = imageExt(item.uri)
    val mime = if (ext == "png") "image/png" else "image/jpeg"
    val name = item.name?.takeIf { it.isNotEmpty() } ?: "$fallbackName.$ext"
    val bytes = item.readBytes?.invoke() ?: throw Exception("خطا در خواندن فایل")
    try {
        val response = postProductImage(productId, type, name, mime, bytes)
        if (!response.ok) throw Exception("آپلود ناموفق: ${response.status} - ${response.text}")
        if (response.json == null) {
            throw Exception(if (response.text.isBlank()) "JSON Parse error: Unexpected end of input" else "JSON Parse error: Unexpected character")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        try {
            val response = postProductImage(productId, type, name, mime, bytes)
            if (!response.ok) throw Exception("آپلود ناموفق: ${response.status}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiNetworkException) {
            val message = (e.message ?: "").lowercase()
            if (message.contains("timeout")) throw Exception("زمان آپلود تمام شد")
            throw Exception("خطا در ارتباط با سرور")
        }
    }
}

internal suspend fun uploadProductImages(
    productId: String,
    featured: List<ImageItem>,
    slots: List<ProductImageSlot>,
    showToast: (String, String) -> Unit
) {
    try {
        uploadAssignedProductImages(productId, featured, slots, showToast)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        showToast("خطا در فرآیند آپلود عکس‌ها", "error")
        throw e
    }
}

private suspend fun uploadAssignedProductImages(
    productId: String,
    featured: List<ImageItem>,
    slots: List<ProductImageSlot>,
    showToast: (String, String) -> Unit
) {
    val results = mutableListOf<Boolean>()
    if (featured.isNotEmpty() && !isServerProductImage(featured[0])) {
        showToast("در حال آپلود عکس شاخص...", "info")
        try {
            val now = jsNow()
            uploadOneProductImage(productId, 0, featured[0], "featured_$now")
            results += true
            showToast("عکس شاخص آپلود شد", "success")
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            results += false
            showToast("خطا در آپلود عکس شاخص", "error")
        }
    }
    val fresh = slots.filter { it.isNew && it.item != null }
    for ((index, slot) in fresh.withIndex()) {
        val item = slot.item ?: continue
        try {
            showToast("در حال آپلود عکس ${index + 1} از ${fresh.size}...", "info")
            uploadOneProductImage(productId, slot.slot, item, "product_${slot.slot}_${jsNow()}")
            results += true
            if (index < fresh.lastIndex) delay(1000)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            results += false
        }
    }
    val ok = results.count { it }
    val total = results.size
    if (ok == total && total > 0) {
        showToast("همه عکس‌ها با موفقیت آپلود شدند", "success")
    } else if (ok > 0) {
        showToast("$ok از $total عکس آپلود شد", "warning")
    } else if (total > 0) {
        throw Exception("هیچ عکسی آپلود نشد")
    }
}

internal fun productIsoNow(epoch: Long = jsNow()): String {
    val msPerDay = 86_400_000L
    val days = epoch.floorDiv(msPerDay)
    val msOfDay = epoch - days * msPerDay
    val (year, month, day) = civilFromDays(days)
    val hour = (msOfDay / 3_600_000L).toInt()
    val minute = ((msOfDay % 3_600_000L) / 60_000L).toInt()
    val second = ((msOfDay % 60_000L) / 1000L).toInt()
    val millis = (msOfDay % 1000L).toInt()
    return "$year-${pad2(month)}-${pad2(day)}T${pad2(hour)}:${pad2(minute)}:${pad2(second)}.${pad3(millis)}Z"
}

private fun pad2(n: Int): String = n.toString().padStart(2, '0')
private fun pad3(n: Int): String = n.toString().padStart(3, '0')

internal fun groupThousands(digits: String): String {
    val clean = digits.filter { it.isDigit() }
    if (clean.isEmpty()) return ""
    return clean.reversed().chunked(3).joinToString(",").reversed()
}
