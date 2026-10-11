package ir.farimod.app.platform

import androidx.compose.runtime.Composable
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual val isIOS: Boolean = true

actual fun openUrl(url: String): Boolean {
    val nsUrl = NSURL.URLWithString(url) ?: return false
    if (!UIApplication.sharedApplication.canOpenURL(nsUrl)) return false
    UIApplication.sharedApplication.openURL(nsUrl, emptyMap<Any?, Any?>(), null)
    return true
}

actual fun canOpenUrl(url: String): Boolean {
    val nsUrl = NSURL.URLWithString(url) ?: return false
    return UIApplication.sharedApplication.canOpenURL(nsUrl)
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS has no hardware back button; swipe-back is handled by StackHost.
}
