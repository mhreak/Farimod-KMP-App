package ir.farimod.app.utils

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Port of `app/utils/converters.ts` (same function names / semantics, incl. the quirks of the RN code).
 *
 * Values that RN receives as "any" (numbers, strings, null, `undefined`, JSON values) are accepted as `Any?`.
 * `null` == JS `null` / `undefined`.
 */

// ══════════════════════════════════════════════════════════════════════════════
// JS runtime helpers (String(x), Number(x), truthiness, toFixed, toLocaleString, parseInt)
// ══════════════════════════════════════════════════════════════════════════════

/** Unwraps a [JsonElement] into the matching Kotlin value (JsonNull → null, string → String, number → Double, bool → Boolean). */
private fun jsUnwrap(v: Any?): Any? = when (v) {
    null, is JsonNull -> null
    is JsonPrimitive -> when {
        v.isString -> v.content
        v.content == "true" -> true
        v.content == "false" -> false
        else -> v.content.toDoubleOrNull() ?: v.content
    }
    else -> v
}

/** Decimal digits (no trailing/leading zeros) and the position `n` of the decimal point (value = 0.DIGITS × 10^n). */
private fun decimalDigits(a: Double): Pair<String, Int> {
    val s = a.toString()
    val eIdx = s.indexOfFirst { it == 'E' || it == 'e' }
    val mant = if (eIdx >= 0) s.substring(0, eIdx) else s
    val exp = if (eIdx >= 0) s.substring(eIdx + 1).removePrefix("+").toInt() else 0
    val dot = mant.indexOf('.')
    val intPart = if (dot >= 0) mant.substring(0, dot) else mant
    val frac = if (dot >= 0) mant.substring(dot + 1) else ""
    var digits = intPart + frac
    var pointPos = intPart.length + exp
    val lead = digits.indexOfFirst { it != '0' }
    if (lead < 0) return "0" to 1
    digits = digits.substring(lead)
    pointPos -= lead
    digits = digits.trimEnd('0')
    return digits to pointPos
}

/** (integer digits, fraction digits) of a non-negative finite double, exactly as the shortest round-trip representation. */
private fun plainDecimal(a: Double): Pair<String, String> {
    if (a == 0.0) return "0" to ""
    val (digits, n) = decimalDigits(a)
    val intStr = when {
        n <= 0 -> "0"
        n >= digits.length -> digits + "0".repeat(n - digits.length)
        else -> digits.substring(0, n)
    }
    val fracStr = when {
        n >= digits.length -> ""
        n <= 0 -> "0".repeat(-n) + digits
        else -> digits.substring(n)
    }
    return intStr to fracStr
}

private fun incrementDecimalString(s: String): String {
    val chars = s.toCharArray()
    var i = chars.size - 1
    while (i >= 0) {
        if (chars[i] == '9') {
            chars[i] = '0'
            i--
        } else {
            chars[i] = chars[i] + 1
            return chars.concatToString()
        }
    }
    return "1" + chars.concatToString()
}

/** JS `String(number)`. */
fun jsNumberToString(d: Double): String {
    if (d.isNaN()) return "NaN"
    if (d.isInfinite()) return if (d > 0) "Infinity" else "-Infinity"
    if (d == 0.0) return "0"
    val neg = d < 0
    val (digits, n) = decimalDigits(abs(d))
    val k = digits.length
    val body = when {
        n in k..21 -> digits + "0".repeat(n - k)
        n in 1..21 -> digits.substring(0, n) + "." + digits.substring(n)
        n in -5..0 -> "0." + "0".repeat(-n) + digits
        else -> {
            val e = n - 1
            val sign = if (e < 0) "-" else "+"
            val mantissa = if (k == 1) digits else "${digits[0]}.${digits.substring(1)}"
            mantissa + "e" + sign + abs(e)
        }
    }
    return if (neg) "-$body" else body
}

/** JS `String(value)` for the values the RN code deals with. */
fun jsToString(value: Any?): String {
    return when (val v = jsUnwrap(value)) {
        null -> "null"
        is String -> v
        is Boolean -> v.toString()
        is Int -> v.toString()
        is Long -> v.toString()
        is Double -> jsNumberToString(v)
        is Float -> jsNumberToString(v.toDouble())
        is Number -> jsNumberToString(v.toDouble())
        is JsonArray -> v.joinToString(",") { el -> if (el is JsonNull) "" else jsToString(el) }
        is JsonObject -> "[object Object]"
        is JsDate -> v.toString()
        else -> v.toString()
    }
}

private val numberRegex = Regex("^[+-]?(\\d+\\.?\\d*|\\.\\d+)([eE][+-]?\\d+)?$")

private fun jsStringToNumber(s: String): Double {
    val t = s.trim()
    if (t.isEmpty()) return 0.0
    if (t.length > 2 && t[0] == '0') {
        val radix = when (t[1]) {
            'x', 'X' -> 16
            'b', 'B' -> 2
            'o', 'O' -> 8
            else -> 0
        }
        if (radix != 0) return t.substring(2).toLongOrNull(radix)?.toDouble() ?: Double.NaN
    }
    when (t) {
        "Infinity", "+Infinity" -> return Double.POSITIVE_INFINITY
        "-Infinity" -> return Double.NEGATIVE_INFINITY
    }
    if (!numberRegex.matches(t)) return Double.NaN
    return t.toDoubleOrNull() ?: Double.NaN
}

/** JS `Number(value)`. */
fun jsToNumber(value: Any?): Double {
    return when (val v = jsUnwrap(value)) {
        null -> 0.0
        is Boolean -> if (v) 1.0 else 0.0
        is Int -> v.toDouble()
        is Long -> v.toDouble()
        is Double -> v
        is Float -> v.toDouble()
        is Number -> v.toDouble()
        is String -> jsStringToNumber(v)
        is JsonArray -> when (v.size) {
            0 -> 0.0
            1 -> jsToNumber(jsToString(v[0]).let { if (v[0] is JsonNull) "" else it })
            else -> Double.NaN
        }
        is JsDate -> v.getTime()
        else -> Double.NaN
    }
}

/** JS truthiness (`!!value`). */
fun jsTruthy(value: Any?): Boolean {
    return when (val v = jsUnwrap(value)) {
        null -> false
        is Boolean -> v
        is String -> v.isNotEmpty()
        is Int -> v != 0
        is Long -> v != 0L
        is Double -> v != 0.0 && !v.isNaN()
        is Float -> v != 0f && !v.isNaN()
        is Number -> v.toDouble() != 0.0
        else -> true
    }
}

/** JS `n.toFixed(digits)`. */
fun jsToFixed(d: Double, digits: Int = 0): String {
    if (d.isNaN()) return "NaN"
    if (abs(d) >= 1e21 || d.isInfinite()) return jsNumberToString(d)
    val neg = d < 0
    val (i, f) = plainDecimal(abs(d))
    val roundUp = f.length > digits && f[digits] >= '5'
    val fr = if (f.length >= digits) f.substring(0, digits) else f.padEnd(digits, '0')
    var all = i + fr
    if (roundUp) all = incrementDecimalString(all)
    val intLen = all.length - digits
    val res = if (digits > 0) all.substring(0, intLen) + "." + all.substring(intLen) else all
    return if (neg) "-$res" else res
}

/** JS `n.toLocaleString()` (en-US: comma grouping, at most 3 fraction digits). */
fun jsToLocaleString(d: Double): String {
    if (d.isNaN()) return "NaN"
    if (d.isInfinite()) return if (d > 0) "∞" else "-∞"
    val neg = d < 0 || (d == 0.0 && 1.0 / d < 0)
    val a = abs(d)
    val (intStr, fracRaw) = if (a >= 1e21) plainDecimal(a).let { it.first to "" } else {
        val fixed = jsToFixed(a, 3)
        val dot = fixed.indexOf('.')
        fixed.substring(0, dot) to fixed.substring(dot + 1)
    }
    val frac = fracRaw.trimEnd('0')
    val grouped = buildString {
        for ((idx, ch) in intStr.withIndex()) {
            if (idx > 0 && (intStr.length - idx) % 3 == 0) append(',')
            append(ch)
        }
    }
    return (if (neg) "-" else "") + grouped + (if (frac.isNotEmpty()) ".$frac" else "")
}

/** JS `parseInt(s, 10)`; null == NaN. */
fun jsParseInt(s: String?): Int? {
    if (s == null) return null
    val t = s.trimStart()
    var i = 0
    var neg = false
    if (i < t.length && (t[i] == '+' || t[i] == '-')) {
        neg = t[i] == '-'
        i++
    }
    val start = i
    while (i < t.length && t[i] in '0'..'9') i++
    if (i == start) return null
    val v = t.substring(start, i).toLongOrNull() ?: return null
    return (if (neg) -v else v).toInt()
}

// ══════════════════════════════════════════════════════════════════════════════
// JS Date (local time, like Hermes `new Date(...)`)
// ══════════════════════════════════════════════════════════════════════════════

private fun daysFromCivil(yIn: Long, m: Int, d: Int): Long {
    val y = if (m <= 2) yIn - 1 else yIn
    val era = if (y >= 0) y / 400 else (y - 399) / 400
    val yoe = y - era * 400
    val doy = (153 * (m + (if (m > 2) -3 else 9)) + 2) / 5 + d - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097 + doe - 719468
}

private fun civilFromDaysConv(zIn: Long): Triple<Int, Int, Int> {
    val z = zIn + 719468
    val era = if (z >= 0) z / 146097 else (z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    var y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = (doy - (153 * mp + 2) / 5 + 1).toInt()
    val m = (if (mp < 10) mp + 3 else mp - 9).toInt()
    if (m <= 2) y += 1
    return Triple(y.toInt(), m, d)
}

private const val MS_PER_DAY = 86_400_000L

private fun isLeapGregorian(y: Int) = (y % 4 == 0 && y % 100 != 0) || y % 400 == 0

private fun daysInMonthGregorian(y: Int, m: Int): Int = when (m) {
    1, 3, 5, 7, 8, 10, 12 -> 31
    4, 6, 9, 11 -> 30
    else -> if (isLeapGregorian(y)) 29 else 28
}

/**
 * Minimal JS `Date` (instant + local-time getters). An invalid date has `getTime()` = NaN.
 * Create with [JsDate.parse] (`new Date(string)`), [JsDate.now] (`new Date()`), [JsDate.local] (`new Date(y, m, d, ...)`).
 */
class JsDate private constructor(private val ms: Double) {
    val isValid: Boolean get() = !ms.isNaN()

    fun getTime(): Double = ms

    private fun localMillis(): Long = ms.toLong() + localUtcOffsetMinutes(ms.toLong()) * 60_000L
    private fun localDays(): Long = floorDivL(localMillis(), MS_PER_DAY)
    private fun civil(): Triple<Int, Int, Int> = civilFromDaysConv(localDays())

    fun getFullYear(): Int = civil().first
    /** 0-based like JS. */
    fun getMonth(): Int = civil().second - 1
    fun getDate(): Int = civil().third
    /** 0 = Sunday. */
    fun getDay(): Int = (((localDays() + 4) % 7 + 7) % 7).toInt()
    fun getHours(): Int = (floorModL(localMillis(), MS_PER_DAY) / 3_600_000L).toInt()
    fun getMinutes(): Int = (floorModL(localMillis(), 3_600_000L) / 60_000L).toInt()
    fun getSeconds(): Int = (floorModL(localMillis(), 60_000L) / 1000L).toInt()

    override fun toString(): String = if (isValid) {
        "${getFullYear()}-${(getMonth() + 1).toString().padStart(2, '0')}-${getDate().toString().padStart(2, '0')}" +
            "T${getHours().toString().padStart(2, '0')}:${getMinutes().toString().padStart(2, '0')}:${getSeconds().toString().padStart(2, '0')}"
    } else "Invalid Date"

    companion object {
        private val invalid = JsDate(Double.NaN)

        fun now(): JsDate = JsDate(currentTimeMillis().toDouble())

        fun fromEpochMs(ms: Double): JsDate = JsDate(ms)

        /** `new Date(year, monthIndex0, day, hours, minutes, seconds, ms)` in local time (values may overflow like in JS). */
        fun local(year: Int, monthIndex: Int, day: Int, hours: Int = 0, minutes: Int = 0, seconds: Int = 0, millis: Int = 0): JsDate {
            val yy = year.toLong() + floorDivL(monthIndex.toLong(), 12L)
            val mm = floorModL(monthIndex.toLong(), 12L).toInt()
            val days = daysFromCivil(yy, mm + 1, 1) + (day - 1)
            val localMs = days * MS_PER_DAY + hours * 3_600_000L + minutes * 60_000L + seconds * 1000L + millis
            return fromLocalMillis(localMs)
        }

        private fun fromLocalMillis(localMs: Long): JsDate {
            val off1 = localUtcOffsetMinutes(localMs)
            var utc = localMs - off1 * 60_000L
            val off2 = localUtcOffsetMinutes(utc)
            if (off2 != off1) utc = localMs - off2 * 60_000L
            return JsDate(utc.toDouble())
        }

        private val isoRegex = Regex(
            "^([+-]\\d{6}|\\d{4})(?:-(\\d{2})(?:-(\\d{2}))?)?(?:[T ](\\d{2}):(\\d{2})(?::(\\d{2})(?:[.,](\\d+))?)?)?\\s*(Z|[+-]\\d{2}(?::?\\d{2})?)?$"
        )
        private val slashRegex = Regex("^(\\d{1,4})/(\\d{1,2})/(\\d{1,4})(?:[ T](\\d{1,2}):(\\d{2})(?::(\\d{2}))?)?$")

        /** `new Date(string)`. Returns an invalid date when the string can not be parsed. */
        fun parse(input: String): JsDate {
            val s = input.trim()
            isoRegex.matchEntire(s)?.let { m ->
                val g = m.groups
                val year = g[1]!!.value.toInt()
                val month = g[2]?.value?.toInt() ?: 1
                val day = g[3]?.value?.toInt() ?: 1
                val hasTime = g[4] != null
                val hour = g[4]?.value?.toInt() ?: 0
                val minute = g[5]?.value?.toInt() ?: 0
                val second = g[6]?.value?.toInt() ?: 0
                val milli = g[7]?.value?.padEnd(3, '0')?.substring(0, 3)?.toInt() ?: 0
                val tz = g[8]?.value
                if (month !in 1..12 || day !in 1..daysInMonthGregorian(year, month)) return invalid
                if (hour > 24 || minute > 59 || second > 59 || (hour == 24 && (minute != 0 || second != 0 || milli != 0))) return invalid
                val localLike = daysFromCivil(year.toLong(), month, day) * MS_PER_DAY +
                    hour * 3_600_000L + minute * 60_000L + second * 1000L + milli
                return when {
                    tz == null && !hasTime -> JsDate(localLike.toDouble()) // date-only forms are UTC
                    tz == null -> fromLocalMillis(localLike)
                    tz == "Z" -> JsDate(localLike.toDouble())
                    else -> {
                        val sign = if (tz[0] == '-') -1 else 1
                        val digits = tz.substring(1).replace(":", "")
                        val oh = digits.substring(0, 2).toInt()
                        val om = if (digits.length >= 4) digits.substring(2, 4).toInt() else 0
                        JsDate((localLike - sign * (oh * 60 + om) * 60_000L).toDouble())
                    }
                }
            }
            slashRegex.matchEntire(s)?.let { m ->
                val g = m.groups
                val a = g[1]!!.value
                val b = g[2]!!.value.toInt()
                val c = g[3]!!.value
                val year: Int
                val month: Int
                val day: Int
                if (a.length == 4) {
                    year = a.toInt(); month = b; day = c.toInt()
                } else {
                    month = a.toInt(); day = b; year = c.toInt()
                }
                val hour = g[4]?.value?.toInt() ?: 0
                val minute = g[5]?.value?.toInt() ?: 0
                val second = g[6]?.value?.toInt() ?: 0
                if (month !in 1..12 || day !in 1..31 || hour > 23 || minute > 59 || second > 59) return invalid
                return local(year, month - 1, day, hour, minute, second)
            }
            return invalid
        }
    }
}

private fun floorDivL(a: Long, b: Long): Long {
    var q = a / b
    if ((a % b != 0L) && ((a < 0) != (b < 0))) q--
    return q
}

private fun floorModL(a: Long, b: Long): Long = a - floorDivL(a, b) * b

private fun fd(a: Int, b: Int): Int = floorDivL(a.toLong(), b.toLong()).toInt()

/** `typeof x === "string" ? new Date(x) : x` — null when [value] is neither a string nor a [JsDate] (JS would throw). */
private fun toJsDate(value: Any?): JsDate? = when (val v = jsUnwrap(value)) {
    is JsDate -> v
    is String -> JsDate.parse(v)
    else -> null
}

// ══════════════════════════════════════════════════════════════════════════════
// converters.ts — basic helpers
// ══════════════════════════════════════════════════════════════════════════════

/** RN: `toPersianDigits(input)`; null / undefined / "" → "۰". */
fun toPersianDigits(input: Any?): String {
    val unwrapped = jsUnwrap(input)
    if (unwrapped == null || (unwrapped is String && unwrapped.isEmpty())) {
        return "۰"
    }
    val persianDigits = "۰۱۲۳۴۵۶۷۸۹"
    return try {
        val stringValue = if (unwrapped is String) unwrapped else jsToString(unwrapped)
        buildString(stringValue.length) {
            for (ch in stringValue) {
                if (ch in '0'..'9') append(persianDigits[ch - '0']) else append(ch)
            }
        }
    } catch (e: Exception) {
        "۰"
    }
}

/** RN: `safeNumber(value, defaultValue = 0)`. */
fun safeNumber(value: Any?, defaultValue: Number = 0): Double {
    val v = jsUnwrap(value)
    if (v == null || (v is String && v.isEmpty())) {
        return defaultValue.toDouble()
    }
    val numValue = if (v is Number) v.toDouble() else jsToNumber(v)
    return if (numValue.isNaN()) defaultValue.toDouble() else numValue
}

/** RN: `formatPrice(price, currency = "تومان")` — `${toPersianDigits(n.toLocaleString())} ${currency}`. */
fun formatPrice(price: Any?, currency: String = "تومان"): String {
    val safePrice = safeNumber(price)
    return "${toPersianDigits(jsToLocaleString(safePrice))} $currency"
}

/** RN: `safeStringIncludes(str, searchString)`. */
fun safeStringIncludes(str: Any?, searchString: String): Boolean {
    if (!jsTruthy(str)) return false
    return try {
        jsToString(str).contains(searchString)
    } catch (e: Exception) {
        false
    }
}

/** RN: `safeString(value, defaultValue = "")`. */
fun safeString(value: Any?, defaultValue: String = ""): String {
    if (jsUnwrap(value) == null) {
        return defaultValue
    }
    return try {
        jsToString(value)
    } catch (e: Exception) {
        defaultValue
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// DATE CONVERSION FUNCTIONS
// ══════════════════════════════════════════════════════════════════════════════

/** `{ year, month, day, formatted }` returned by `gregorianToPersian` / `getCurrentPersianDate`. */
data class PersianDate(val year: Int, val month: Int, val day: Int, val formatted: String)

/** `{ years, months, days, totalDays }` returned by `calculatePersianDateDifference`. */
data class PersianDateDifference(val years: Int, val months: Int, val days: Int, val totalDays: Int)

private fun isGregorianLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

private fun isPersianLeapYear(year: Int): Boolean {
    val breaks = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210, 1635, 2060, 2097,
        2192, 2262, 2324, 2394, 2456, 3178
    )
    var jp = breaks[0]
    var jump = 0
    for (j in 1 until breaks.size) {
        val jm = breaks[j]
        jump = jm - jp
        if (year < jm) break
        jp = jm
    }
    val n = year - jp

    if (n < jump) {
        if (jump - n < 6) return false
        val n1 = n + 1
        if (jump - n1 < 6) return n1 % 4 == 0
        return n % 4 == 0
    }
    return false
}

/** RN: `gregorianToPersian(date)` — [gregorianDate] is a [JsDate] or a date string. */
fun gregorianToPersian(gregorianDate: Any?): PersianDate? {
    try {
        if (!jsTruthy(gregorianDate)) return null

        val date = toJsDate(gregorianDate) ?: return null

        if (!date.isValid) return null

        val gy = date.getFullYear()
        val gm = date.getMonth() + 1
        val gd = date.getDate()

        val gDM = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)

        var jy = if (gy <= 1600) 0 else 979
        if (gy > 1600) jy = 1342

        val gy2 = if (gy > 1600) gy - 1600 else gy - 621
        val days = 365 * gy2 +
            fd(gy2 + 3, 4) +
            fd(gy2 + 99, 100) -
            fd(gy2 + 399, 400) -
            80 +
            gd +
            (if (gm > 2) gDM[gm - 1] + (if (gm > 2 && isGregorianLeapYear(gy)) 1 else 0) else gDM[gm - 1])

        jy += 33 * fd(days, 12053)
        val days2 = days % 12053

        jy += 4 * fd(days2, 1461)
        var days3 = days2 % 1461

        if (days3 >= 366) {
            jy += fd(days3 - 1, 365)
            days3 = (days3 - 1) % 365
        }

        val jm: Int
        val jd: Int

        if (days3 < 186) {
            jm = 1 + fd(days3, 31)
            jd = 1 + (days3 % 31)
        } else {
            jm = 7 + fd(days3 - 186, 30)
            jd = 1 + ((days3 - 186) % 30)
        }

        val formatted = "${toPersianDigits(jy)}/${toPersianDigits(jm.toString().padStart(2, '0'))}/${toPersianDigits(jd.toString().padStart(2, '0'))}"

        return PersianDate(jy, jm, jd, formatted)
    } catch (error: Exception) {
        return null
    }
}

/** RN: `persianToGregorian(year, month, day)`. */
fun persianToGregorian(persianYear: Int, persianMonth: Int, persianDay: Int): JsDate? {
    try {
        if (persianYear < 1 || persianMonth < 1 || persianMonth > 12 || persianDay < 1) {
            return null
        }

        val jy = persianYear
        val jm = persianMonth
        val jd = persianDay

        val jy2 = if (jy > 979) 1600 else 621
        val jy3 = if (jy > 979) jy - 979 else jy

        val jp = fd(jy3, 33)
        val jp2 = jy3 % 33
        val jp3 = fd(jp2, 4)

        val days = 365 * jy3 +
            8 * jp +
            fd(jp3 + 3, 4) +
            78 +
            jd +
            (if (jm < 7) (jm - 1) * 31 else (jm - 7) * 30 + 186)

        var gy = if (days <= 366) jy2 else jy2 + floor((days - 366) / 365.25).toInt()
        var gd = days -
            (365 * (gy - jy2) +
                fd(gy - jy2 + 3, 4) +
                fd(gy - jy2 + 99, 100) -
                fd(gy - jy2 + 399, 400))

        val isLeap = isGregorianLeapYear(gy)
        val salA = intArrayOf(
            0,
            31,
            if (isLeap) 29 else 28,
            31,
            30,
            31,
            30,
            31,
            31,
            30,
            31,
            30,
            31,
        )

        var gm = 0
        while (gm < 13 && gd > salA[gm]) {
            gd -= salA[gm]
            gm++
        }

        if (gm > 12) {
            gm = 1
            gy++
            gd = 1
        }

        return JsDate.local(gy, gm - 1, gd)
    } catch (error: Exception) {
        return null
    }
}

private val persianMonthNames = listOf(
    "",
    "فروردین",
    "اردیبهشت",
    "خرداد",
    "تیر",
    "مرداد",
    "شهریور",
    "مهر",
    "آبان",
    "آذر",
    "دی",
    "بهمن",
    "اسفند",
)

private val persianDayNames = listOf(
    "یکشنبه",
    "دوشنبه",
    "سه‌شنبه",
    "چهارشنبه",
    "پنج‌شنبه",
    "جمعه",
    "شنبه",
)

/**
 * RN: `formatPersianDate(date, format = "medium", includeTime = false)`.
 * NOTE: the RN implementation has a typo (`parsianDate.getHours()`) that throws a ReferenceError when
 * `includeTime = true`; the surrounding try/catch then returns "". That behaviour is preserved.
 */
fun formatPersianDate(date: Any?, format: String = "medium", includeTime: Boolean = false): String {
    try {
        if (!jsTruthy(date)) return ""

        val parsedDate = toJsDate(date) ?: return ""
        if (!parsedDate.isValid) return ""

        val persianDate = gregorianToPersian(parsedDate) ?: return ""

        var formattedDate = ""

        when (format) {
            "short" -> {
                formattedDate = "${toPersianDigits(persianDate.year)}/${toPersianDigits(persianDate.month.toString().padStart(2, '0'))}/${toPersianDigits(persianDate.day.toString().padStart(2, '0'))}"
            }

            "medium" -> {
                formattedDate = "${toPersianDigits(persianDate.day)} ${persianMonthNames[persianDate.month]} ${toPersianDigits(persianDate.year)}"
            }

            "long" -> {
                val dayOfWeek = parsedDate.getDay()
                formattedDate = "${persianDayNames[dayOfWeek]}، ${toPersianDigits(persianDate.day)} ${persianMonthNames[persianDate.month]} ${toPersianDigits(persianDate.year)}"
            }

            "full" -> {
                val dayOfWeekFull = parsedDate.getDay()
                formattedDate = "${persianDayNames[dayOfWeekFull]}، ${toPersianDigits(persianDate.day)} ${persianMonthNames[persianDate.month]} سال ${toPersianDigits(persianDate.year)}"
            }
        }

        if (includeTime) {
            // RN: `parsianDate.getHours()` → ReferenceError → caught below → ""
            throw IllegalStateException("parsianDate is not defined")
        }

        return formattedDate
    } catch (error: Exception) {
        return ""
    }
}

/** RN: `getCurrentPersianDate()`. */
fun getCurrentPersianDate(): PersianDate? = gregorianToPersian(JsDate.now())

/** RN: `parsePersianDateString("YYYY/MM/DD")` (Persian or English digits). */
fun parsePersianDateString(persianDateString: String?): JsDate? {
    try {
        if (persianDateString.isNullOrEmpty()) return null

        val englishDateString = buildString {
            for (ch in persianDateString) {
                if (ch in '۰'..'۹') append((ch - '۰' + '0'.code).toChar()) else append(ch)
            }
        }

        val parts = englishDateString.split("/")
        if (parts.size != 3) return null

        val year = jsParseInt(parts[0])
        val month = jsParseInt(parts[1])
        val day = jsParseInt(parts[2])

        if (year == null || month == null || day == null) return null

        return persianToGregorian(year, month, day)
    } catch (error: Exception) {
        return null
    }
}

/** RN: `calculatePersianDateDifference(date1, date2)`. */
fun calculatePersianDateDifference(date1: Any?, date2: Any?): PersianDateDifference? {
    try {
        val d1 = toJsDate(date1) ?: return null
        val d2 = toJsDate(date2) ?: return null

        if (!d1.isValid || !d2.isValid) return null

        val persian1 = gregorianToPersian(d1)
        val persian2 = gregorianToPersian(d2)

        if (persian1 == null || persian2 == null) return null

        val totalDays = abs(floor((d2.getTime() - d1.getTime()) / (1000.0 * 60 * 60 * 24))).roundToInt()

        var years = abs(persian2.year - persian1.year)
        var months = abs(persian2.month - persian1.month)
        var days = abs(persian2.day - persian1.day)

        if (days < 0) {
            months--
            days += 30
        }
        if (months < 0) {
            years--
            months += 12
        }

        return PersianDateDifference(years, months, days, totalDays)
    } catch (error: Exception) {
        return null
    }
}

/** RN: `isValidPersianDate(year, month, day)`. */
fun isValidPersianDate(year: Int, month: Int, day: Int): Boolean {
    return try {
        if (year < 1 || month < 1 || month > 12 || day < 1) return false

        val maxDays = if (month <= 6) 31 else if (month <= 11) 30 else if (isPersianLeapYear(year)) 30 else 29

        day <= maxDays
    } catch (error: Exception) {
        false
    }
}

/** RN: `getPersianMonthName(month)` — "" when out of range. */
fun getPersianMonthName(month: Int): String = persianMonthNames.getOrNull(month)?.takeIf { it.isNotEmpty() } ?: ""

/** RN: `getPersianDayName(date)`. */
fun getPersianDayName(date: JsDate): String = persianDayNames.getOrNull(date.getDay()) ?: ""
