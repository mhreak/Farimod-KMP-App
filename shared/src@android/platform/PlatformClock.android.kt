package ir.farimod.app.platform

actual fun rnNowMillis(): Long = System.currentTimeMillis()

actual fun rnLocalOffsetMillis(epochMillis: Long): Long =
    java.util.TimeZone.getDefault().getOffset(epochMillis).toLong()
