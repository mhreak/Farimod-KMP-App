package ir.farimod.app.platform

/** JavaScript `Date.now()` — milliseconds since the Unix epoch (UTC). */
expect fun rnNowMillis(): Long

/**
 * Offset (in milliseconds) of the device time zone from UTC at the given instant
 * (JavaScript: `-new Date(epochMillis).getTimezoneOffset() * 60000`). Used to emulate `Date#getHours()`, `getDate()`...
 */
expect fun rnLocalOffsetMillis(epochMillis: Long): Long
