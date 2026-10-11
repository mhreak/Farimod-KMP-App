package ir.farimod.app.utils

import ir.farimod.app.platform.rnLocalOffsetMillis
import ir.farimod.app.platform.rnNowMillis

/**
 * Pure-Kotlin port of `jalaali-js` (https://github.com/jalaali/jalaali-js, MIT) — the algorithm is ported 1:1
 * (same `breaks` table, same `jalCal` / `j2d` / `d2j` / `g2d` / `d2g` helpers, JS `~~` division semantics).
 *
 * JavaScript `Date` objects are represented as epoch milliseconds ([Long]); helpers that read/write the *local*
 * calendar fields (`getFullYear()`, `new Date(y, m, d, ...)`) use the device time zone.
 */

data class JalaaliDate(val jy: Int, val jm: Int, val jd: Int)
data class GregorianDate(val gy: Int, val gm: Int, val gd: Int)
data class JalCalResult(val leap: Int, val gy: Int, val march: Int)
data class JalaaliWeek(val saturday: JalaaliDate, val friday: JalaaliDate)

private val jalaaliBreaks = intArrayOf(
    -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
    1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178
)

/** JS `~~(a / b)` for integers = truncating division (Kotlin `/`). */
private fun div(a: Int, b: Int): Int = a / b

/** JS `a - ~~(a / b) * b` = Kotlin `%`. */
private fun mod(a: Int, b: Int): Int = a - (a / b) * b

// ─────────────────────────────────────────────────────────────────────────────
// jalaali-js public API
// ─────────────────────────────────────────────────────────────────────────────

/** `toJalaali(gy, gm, gd)` */
fun toJalaali(gy: Int, gm: Int, gd: Int): JalaaliDate = d2j(g2d(gy, gm, gd))

/** `toJalaali(date)` — [epochMillis] is a JS Date; the LOCAL calendar fields are used (like `getFullYear()` etc.). */
fun toJalaali(epochMillis: Long): JalaaliDate {
    val (gy, gm, gd) = localYmdOf(epochMillis)
    return toJalaali(gy, gm, gd)
}

/** `toGregorian(jy, jm, jd)` */
fun toGregorian(jy: Int, jm: Int, jd: Int): GregorianDate = d2g(j2d(jy, jm, jd))

/** `isValidJalaaliDate(jy, jm, jd)` */
fun isValidJalaaliDate(jy: Int, jm: Int, jd: Int): Boolean =
    jy >= -61 && jy <= 3177 &&
        jm >= 1 && jm <= 12 &&
        jd >= 1 && jd <= jalaaliMonthLength(jy, jm)

/** `isLeapJalaaliYear(jy)` */
fun isLeapJalaaliYear(jy: Int): Boolean = jalCalLeap(jy) == 0

/** `jalaaliMonthLength(jy, jm)` */
fun jalaaliMonthLength(jy: Int, jm: Int): Int {
    if (jm <= 6) return 31
    if (jm <= 11) return 30
    if (isLeapJalaaliYear(jy)) return 30
    return 29
}

/** `jalaaliToDateObject(jy, jm, jd, h, m, s, ms)` → epoch millis of the local date-time. */
fun jalaaliToDateObject(
    jy: Int, jm: Int, jd: Int,
    h: Int = 0, m: Int = 0, s: Int = 0, ms: Int = 0
): Long {
    val g = toGregorian(jy, jm, jd)
    return localDateTimeToEpoch(g.gy, g.gm, g.gd, h, m, s, ms)
}

/** `jalaaliWeek(jy, jm, jd)` — Saturday → Friday week containing the given Jalaali date. */
fun jalaaliWeek(jy: Int, jm: Int, jd: Int): JalaaliWeek {
    val dayOfWeek = localDayOfWeek(jalaaliToDateObject(jy, jm, jd)) // JS getDay(): 0 = Sunday
    val startDayDifference = if (dayOfWeek == 6) 0 else -(dayOfWeek + 1)
    val endDayDifference = 6 + startDayDifference
    return JalaaliWeek(
        saturday = d2j(j2d(jy, jm, jd + startDayDifference)),
        friday = d2j(j2d(jy, jm, jd + endDayDifference))
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// jalaali-js internals
// ─────────────────────────────────────────────────────────────────────────────

/**
 * This function determines if the Jalaali (Persian) year is leap (366-day long) or is the common year (365 days).
 * Returns number of years since the last leap year (0 to 4).
 */
fun jalCalLeap(jy: Int): Int {
    val bl = jalaaliBreaks.size
    var jp = jalaaliBreaks[0]
    var jm: Int
    var jump = 0
    var n: Int
    var leap: Int

    if (jy < jp || jy >= jalaaliBreaks[bl - 1]) throw IllegalArgumentException("Invalid Jalaali year $jy")

    for (i in 1 until bl) {
        jm = jalaaliBreaks[i]
        jump = jm - jp
        if (jy < jm) break
        jp = jm
    }
    n = jy - jp

    if (jump - n < 6) n = n - jump + div(jump + 4, 33) * 33
    leap = mod(mod(n + 1, 33) - 1, 4)
    if (leap == -1) leap = 4
    return leap
}

/**
 * This function determines if the Jalaali (Persian) year is leap and calculates the date of the first day of
 * the year in the Gregorian calendar (`march` = day of March, `gy` = Gregorian year).
 * With [withoutLeap] = true only `gy` and `march` are meaningful (`leap` = 0).
 */
fun jalCal(jy: Int, withoutLeap: Boolean = false): JalCalResult {
    val bl = jalaaliBreaks.size
    val gy = jy + 621
    var leapJ = -14
    var jp = jalaaliBreaks[0]
    var jm: Int
    var jump = 0
    var leap: Int
    var n: Int

    if (jy < jp || jy >= jalaaliBreaks[bl - 1]) throw IllegalArgumentException("Invalid Jalaali year $jy")

    // Find the limiting years for the Jalaali year jy.
    for (i in 1 until bl) {
        jm = jalaaliBreaks[i]
        jump = jm - jp
        if (jy < jm) break
        leapJ = leapJ + div(jump, 33) * 8 + div(mod(jump, 33), 4)
        jp = jm
    }
    n = jy - jp

    // Find the number of leap years from AD 621 to the beginning of the current Jalaali year in the Persian calendar.
    leapJ = leapJ + div(n, 33) * 8 + div(mod(n, 33) + 3, 4)
    if (mod(jump, 33) == 4 && jump - n == 4) leapJ += 1

    // And the same in the Gregorian calendar (until the year gy).
    val leapG = div(gy, 4) - div((div(gy, 100) + 1) * 3, 4) - 150

    // Determine the Gregorian date of Farvardin the 1st.
    val march = 20 + leapJ - leapG

    // return with gy and march when we don't need leap
    if (withoutLeap) return JalCalResult(leap = 0, gy = gy, march = march)

    // Find how many years have passed since the last leap year.
    if (jump - n < 6) n = n - jump + div(jump + 4, 33) * 33
    leap = mod(mod(n + 1, 33) - 1, 4)
    if (leap == -1) leap = 4

    return JalCalResult(leap = leap, gy = gy, march = march)
}

/** Converts a date of the Jalaali calendar to the Julian Day number. */
fun j2d(jy: Int, jm: Int, jd: Int): Int {
    val r = jalCal(jy, true)
    return g2d(r.gy, 3, r.march) + (jm - 1) * 31 - div(jm, 7) * (jm - 7) + jd - 1
}

/** Converts the Julian Day number to a date in the Jalaali calendar. */
fun d2j(jdn: Int): JalaaliDate {
    val gy = d2g(jdn).gy // Calculate Gregorian year (gy).
    var jy = gy - 621
    val r = jalCal(jy, false)
    val jdn1f = g2d(gy, 3, r.march)
    var k: Int
    val jm: Int
    val jd: Int

    // Find number of days that passed since 1 Farvardin.
    k = jdn - jdn1f
    if (k >= 0) {
        if (k <= 185) {
            // The first 6 months.
            return JalaaliDate(jy, 1 + div(k, 31), mod(k, 31) + 1)
        } else {
            // The remaining months.
            k -= 186
        }
    } else {
        // Previous Jalaali year.
        jy -= 1
        k += 179
        if (r.leap == 1) k += 1
    }
    jm = 7 + div(k, 30)
    jd = mod(k, 30) + 1
    return JalaaliDate(jy, jm, jd)
}

/** Calculates the Julian Day number from Gregorian or Julian calendar dates. */
fun g2d(gy: Int, gm: Int, gd: Int): Int {
    var d = div((gy + div(gm - 8, 6) + 100100) * 1461, 4) +
        div(153 * mod(gm + 9, 12) + 2, 5) +
        gd - 34840408
    d = d - div(div(gy + 100100 + div(gm - 8, 6), 100) * 3, 4) + 752
    return d
}

/** Calculates Gregorian and Julian calendar dates from the Julian Day number. */
fun d2g(jdn: Int): GregorianDate {
    var j = 4 * jdn + 139361631
    j = j + div(div(4 * jdn + 183187720, 146097) * 3, 4) * 4 - 3908
    val i = div(mod(j, 1461), 4) * 5 + 308
    val gd = div(mod(i, 153), 5) + 1
    val gm = mod(div(i, 153), 12) + 1
    val gy = div(j, 1461) - 100100 + div(8 - gm, 6)
    return GregorianDate(gy, gm, gd)
}

// ─────────────────────────────────────────────────────────────────────────────
// JS `Date` helpers (epoch millis)
// ─────────────────────────────────────────────────────────────────────────────

private const val MS_PER_DAY = 86_400_000L

/** Days since 1970-01-01 for a proleptic Gregorian date (month 1..12). */
fun daysFromCivil(y: Int, m: Int, d: Int): Long {
    val yy: Long = (if (m <= 2) y - 1 else y).toLong()
    val era: Long = (if (yy >= 0) yy else yy - 399) / 400
    val yoe: Long = yy - era * 400
    val mp: Long = ((m + 9) % 12).toLong()
    val doy: Long = (153 * mp + 2) / 5 + d - 1
    val doe: Long = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146097 + doe - 719468
}

/** Inverse of [daysFromCivil] → (year, month 1..12, day). */
fun civilFromDays(days: Long): Triple<Int, Int, Int> {
    val z: Long = days + 719468
    val era: Long = (if (z >= 0) z else z - 146096) / 146097
    val doe: Long = z - era * 146097
    val yoe: Long = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val y: Long = yoe + era * 400
    val doy: Long = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp: Long = (5 * doy + 2) / 153
    val d: Long = doy - (153 * mp + 2) / 5 + 1
    val m: Long = if (mp < 10) mp + 3 else mp - 9
    return Triple((y + if (m <= 2) 1 else 0).toInt(), m.toInt(), d.toInt())
}

/** `Date.now()` */
fun jsNow(): Long = rnNowMillis()

/** `date.getUTCFullYear() / getUTCMonth()+1 / getUTCDate()` */
fun utcYmdOf(epochMillis: Long): Triple<Int, Int, Int> =
    civilFromDays((epochMillis).floorDiv(MS_PER_DAY))

/** `date.getFullYear() / getMonth()+1 / getDate()` (device time zone). */
fun localYmdOf(epochMillis: Long): Triple<Int, Int, Int> =
    utcYmdOf(epochMillis + rnLocalOffsetMillis(epochMillis))

/** `date.getHours()` / `getMinutes()` (device time zone). */
fun localHourMinuteOf(epochMillis: Long): Pair<Int, Int> {
    val local = epochMillis + rnLocalOffsetMillis(epochMillis)
    val msOfDay = local - (local).floorDiv(MS_PER_DAY) * MS_PER_DAY
    return (msOfDay / 3_600_000L).toInt() to ((msOfDay % 3_600_000L) / 60_000L).toInt()
}

/** `date.getDay()` — 0 = Sunday (device time zone). */
fun localDayOfWeek(epochMillis: Long): Int {
    val days = (epochMillis + rnLocalOffsetMillis(epochMillis)).floorDiv(MS_PER_DAY)
    return (((days + 4) % 7 + 7) % 7).toInt() // 1970-01-01 was a Thursday
}

/** `Date.UTC(y, m - 1, d, h, mi, s, ms)` (month 1..12). */
fun utcDateTimeToEpoch(y: Int, m: Int, d: Int, h: Int = 0, mi: Int = 0, s: Int = 0, ms: Int = 0): Long =
    daysFromCivil(y, m, d) * MS_PER_DAY + h * 3_600_000L + mi * 60_000L + s * 1000L + ms

/** `new Date(y, m - 1, d, h, mi, s, ms)` (month 1..12, device time zone). */
fun localDateTimeToEpoch(y: Int, m: Int, d: Int, h: Int = 0, mi: Int = 0, s: Int = 0, ms: Int = 0): Long {
    val asUtc = utcDateTimeToEpoch(y, m, d, h, mi, s, ms)
    val first = asUtc - rnLocalOffsetMillis(asUtc)
    return asUtc - rnLocalOffsetMillis(first)
}

// ─────────────────────────────────────────────────────────────────────────────
// Converters used by RN `AppDatePicker.tsx` / `SimpleDatePicker.js` (jdf-style algorithm, NOT jalaali-js)
// ─────────────────────────────────────────────────────────────────────────────

/** `gregorianToPersian` of AppDatePicker/SimpleDatePicker → [jy, jm, jd]. */
fun rnGregorianToPersian(gyIn: Int, gm: Int, gd: Int): List<Int> {
    var gy = gyIn
    val gDM = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)

    var jy: Int
    if (gy > 1600) {
        jy = 979
        gy -= 1600
    } else {
        jy = 0
        gy -= 621
    }

    val gy2 = if (gm > 2) gy + 1 else gy
    var days = (365 * gy) + ((gy2 + 3).floorDiv(4)) - ((gy2 + 99).floorDiv(100)) +
        ((gy2 + 399).floorDiv(400)) - 80 + gd + gDM[gm - 1]

    jy += 33 * days.floorDiv(12053)
    days = days.mod(12053)

    jy += 4 * days.floorDiv(1461)
    days = days.mod(1461)

    if (days > 365) {
        jy += (days - 1).floorDiv(365)
        days = (days - 1).mod(365)
    }

    val jm: Int
    val jd: Int
    if (days < 186) {
        jm = 1 + days.floorDiv(31)
        jd = 1 + days.mod(31)
    } else {
        jm = 7 + (days - 186).floorDiv(30)
        jd = 1 + (days - 186).mod(30)
    }
    return listOf(jy, jm, jd)
}

/** `persianToGregorian` of AppDatePicker (without the Date object) → (gy, gm, gd). */
fun rnPersianToGregorian(jyIn: Int, jm: Int, jd: Int): GregorianDate {
    var jy = jyIn
    var gy: Int
    var gm: Int
    val gd: Int

    val jy2 = if (jy > 979) 1600 else 621
    jy -= if (jy > 979) 979 else 0

    var days = (365 * jy) + (jy.floorDiv(33) * 8) + (((jy % 33) + 3).floorDiv(4)) + 78 + jd

    if (jm < 7) {
        days += (jm - 1) * 31
    } else {
        days += (jm - 7) * 30 + 186
    }

    gy = 400 * days.floorDiv(146097)
    days %= 146097

    var flag = true
    if (days >= 36525) {
        days--
        gy += 100 * days.floorDiv(36524)
        days %= 36524
        if (days >= 365) days++
        else flag = false
    }

    if (flag) {
        gy += 4 * days.floorDiv(1461)
        days %= 1461
        if (days >= 366) {
            flag = false
            days--
            gy += days.floorDiv(365)
            days %= 365
        }
    }

    gy += jy2

    val salA = intArrayOf(
        0, 31,
        if ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0)) 29 else 28,
        31, 30, 31, 30, 31, 31, 30, 31, 30, 31
    )

    gm = 0
    while (gm < 13 && days > salA[gm]) {
        days -= salA[gm]
        gm++
    }

    gd = days
    return GregorianDate(gy, gm, gd)
}

/** Today's date in the Jalali calendar using the RN picker algorithm → [jy, jm, jd] (device time zone). */
fun rnTodayPersian(): List<Int> {
    val (gy, gm, gd) = localYmdOf(rnNowMillis())
    return rnGregorianToPersian(gy, gm, gd)
}
