package ir.farimod.app.platform

import platform.Foundation.timeIntervalSince1970
import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localTimeZone

actual fun rnNowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()

actual fun rnLocalOffsetMillis(epochMillis: Long): Long {
    val date = NSDate.dateWithTimeIntervalSince1970(epochMillis / 1000.0)
    return NSTimeZone.localTimeZone.secondsFromGMTForDate(date) * 1000L
}
