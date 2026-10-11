package ir.farimod.app.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/** Set once from MainActivity so that non-composable helpers (openUrl, pickers) can reach a Context. */
object AndroidAppContext {
    lateinit var appContext: Context
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
