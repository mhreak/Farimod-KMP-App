package ir.farimod.app.utils

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun localUtcOffsetMinutes(epochMs: Long): Int =
    java.util.TimeZone.getDefault().getOffset(epochMs) / 60000
