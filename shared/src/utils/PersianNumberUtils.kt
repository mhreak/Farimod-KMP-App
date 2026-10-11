package ir.farimod.app.utils

/**
 * Utility functions for Persian number conversion and formatting.
 * Replaces RN's `toPersianDigits` and `toEnglishDigits`.
 */
object PersianNumberUtils {
    private const val PERSIAN_DIGITS = "۰۱۲۳۴۵۶۷۸۹"
    private const val ENGLISH_DIGITS = "0123456789"

    fun toPersianDigits(input: String?): String {
        if (input.isNullOrEmpty()) return ""
        val builder = StringBuilder(input.length)
        for (ch in input) {
            val index = ENGLISH_DIGITS.indexOf(ch)
            if (index != -1) {
                builder.append(PERSIAN_DIGITS[index])
            } else {
                builder.append(ch)
            }
        }
        return builder.toString()
    }

    fun toPersianDigits(number: Number?): String {
        if (number == null) return ""
        return toPersianDigits(number.toString())
    }

    fun toEnglishDigits(input: String?): String {
        if (input.isNullOrEmpty()) return ""
        val builder = StringBuilder(input.length)
        for (ch in input) {
            val index = PERSIAN_DIGITS.indexOf(ch)
            if (index != -1) {
                builder.append(ENGLISH_DIGITS[index])
            } else {
                builder.append(ch)
            }
        }
        return builder.toString()
    }

    /**
     * Formats price with comma separators in Persian digits, e.g. "۱,۲۵۰,۰۰۰ تومان"
     */
    fun formatPrice(amount: Long, unit: String = "تومان"): String {
        val formatted = buildString {
            val str = amount.toString()
            val len = str.length
            for (i in 0 until len) {
                append(str[i])
                val remaining = len - i - 1
                if (remaining > 0 && remaining % 3 == 0) {
                    append(',')
                }
            }
        }
        return "${toPersianDigits(formatted)} $unit"
    }

    fun formatPrice(amount: String, unit: String = "تومان"): String {
        val parsed = toEnglishDigits(amount).replace(",", "").toLongOrNull() ?: return amount
        return formatPrice(parsed, unit)
    }
}

fun Int.toPersianDigits(): String = PersianNumberUtils.toPersianDigits(this)
fun Long.toPersianDigits(): String = PersianNumberUtils.toPersianDigits(this)
fun String.toPersianDigits(): String = PersianNumberUtils.toPersianDigits(this)
fun Number.toPersianDigits(): String = PersianNumberUtils.toPersianDigits(this)
