package ir.farimod.app.utils

/** JS `Date.now()`. */
expect fun currentTimeMillis(): Long

/** Offset of the device time zone from UTC (in minutes) at the given instant (JS `-getTimezoneOffset()`). */
expect fun localUtcOffsetMinutes(epochMs: Long): Int
