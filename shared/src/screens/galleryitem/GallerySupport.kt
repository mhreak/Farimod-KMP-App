package ir.farimod.app.screens.galleryitem

import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.str
import ir.farimod.app.utils.JsDate
import ir.farimod.app.utils.currentTimeMillis
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * `new Date().toISOString()` (UTC, millisecond precision, trailing Z).
 */
fun galleryUtcIsoNow(): String {
    val ms = currentTimeMillis()
    val milli = floorMod(ms, 1000L).toInt()
    var secs = floorDiv(ms, 1000L)
    val second = floorMod(secs, 60L).toInt()
    secs = floorDiv(secs, 60L)
    val minute = floorMod(secs, 60L).toInt()
    secs = floorDiv(secs, 60L)
    val hour = floorMod(secs, 24L).toInt()
    val days = floorDiv(secs, 24L)
    val (year, month, day) = civilFromUnixDays(days)
    return buildString {
        append(year.toString().padStart(4, '0'))
        append('-')
        append(month.toString().padStart(2, '0'))
        append('-')
        append(day.toString().padStart(2, '0'))
        append('T')
        append(hour.toString().padStart(2, '0'))
        append(':')
        append(minute.toString().padStart(2, '0'))
        append(':')
        append(second.toString().padStart(2, '0'))
        append('.')
        append(milli.toString().padStart(3, '0'))
        append('Z')
    }
}

/** `new Date(insertDate).getTime()` for sorting galleries newest-first. Invalid dates sort last. */
fun galleryInsertMillis(item: JsonObject): Double {
    val raw = item.str("InsertDate") ?: return Double.NEGATIVE_INFINITY
    val date = JsDate.parse(raw)
    return if (date.isValid) date.getTime() else Double.NEGATIVE_INFINITY
}

/**
 * ImageGallery/Add may return a bare number or an object. Mirrors the RN id probe
 * (ImageGalleryId, imageGalleryId, data/Data, id/Id). 0 means "not a real id".
 */
fun galleryCreatedId(element: JsonElement?): Int? {
    if (element == null) return null
    if (element is kotlinx.serialization.json.JsonPrimitive) {
        return element.int
    }
    fun from(obj: JsonElement?): Int? {
        if (obj == null) return null
        val keys = listOf("ImageGalleryId", "imageGalleryId", "id", "Id")
        for (key in keys) {
            val value = obj[key].int
            if (value != null && value != 0) return value
        }
        return null
    }
    from(element)?.let { return it }
    from(element["data"])?.let { return it }
    from(element["Data"])?.let { return it }
    return element.int("ImageGalleryId")
}

private fun floorDiv(a: Long, b: Long): Long {
    var q = a / b
    if (a % b != 0L && (a < 0L) != (b < 0L)) q--
    return q
}

private fun floorMod(a: Long, b: Long): Long = a - floorDiv(a, b) * b

/** Howard Hinnant civil_from_days, input = days since Unix epoch (UTC). */
private fun civilFromUnixDays(days: Long): Triple<Int, Int, Int> {
    val z = days + 719468
    val era = if (z >= 0) z / 146097 else (z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    var y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = (doy - (153 * mp + 2) / 5 + 1).toInt()
    val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
    if (month <= 2) y += 1
    return Triple(y.toInt(), month, d)
}
