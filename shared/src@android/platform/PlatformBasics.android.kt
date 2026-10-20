package ir.farimod.app.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/** Set once from MainActivity so that non-composable helpers (openUrl, pickers, billing) can reach a Context & Activity. */
object AndroidAppContext {
    lateinit var appContext: Context
    var currentActivity: android.app.Activity? = null
}

actual val isIOS: Boolean = false

actual fun openUrl(url: String): Boolean {
    return try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        AndroidAppContext.appContext.startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }
}

actual fun canOpenUrl(url: String): Boolean {
    return try {
        val uri = Uri.parse(url)
        val scheme = uri.scheme?.lowercase()
        // resolveActivity returns null for https unless every browser is queried.
        if (scheme == "http" || scheme == "https") return true
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.resolveActivity(AndroidAppContext.appContext.packageManager) != null
    } catch (e: Exception) {
        false
    }
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

actual fun getAppVersionName(): String {
    return try {
        val context = AndroidAppContext.appContext
        val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName ?: "1.0.0"
    } catch (_: Exception) {
        "1.0.0"
    }
}
