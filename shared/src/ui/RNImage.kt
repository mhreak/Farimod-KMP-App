package ir.farimod.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import ir.farimod.app.network.ApiConfig
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** RN `resizeMode` → Compose [ContentScale]. */
object ResizeMode {
    val cover = ContentScale.Crop
    val contain = ContentScale.Fit
    val stretch = ContentScale.FillBounds
    val center = ContentScale.Inside
}

/**
 * RN `<Image source={{ uri }} style resizeMode />` for remote / local-file images.
 * Relative server paths are resolved against https://my.farimod.ir/ (RN code usually passes absolute URLs already).
 * While loading / on error nothing is drawn (same as RN `Image`).
 */
@Composable
fun NetworkImage(
    uri: String?,
    modifier: Modifier = Modifier,
    resizeMode: ContentScale = ResizeMode.cover,
    fallback: DrawableResource? = null,
    alpha: Float = 1f
) {
    val isLocal = uri != null && uri.contains("://") && !uri.startsWith("http://") && !uri.startsWith("https://")
    val resolved = if (isLocal) uri else ApiConfig.resolveMediaUrl(uri)
    if (resolved == null) {
        if (fallback != null) {
            Image(painterResource(fallback), null, modifier, contentScale = resizeMode, alpha = alpha)
        } else Box(modifier)
        return
    }
    AsyncImage(
        model = resolved,
        contentDescription = null,
        modifier = modifier,
        contentScale = resizeMode,
        alpha = alpha
    )
}

/** RN `<Image source={require("../../assets/x.png")} />` — a bundled drawable (composeResources/drawable). */
@Composable
fun LocalImage(
    resource: DrawableResource,
    modifier: Modifier = Modifier,
    resizeMode: ContentScale = ResizeMode.cover,
    alpha: Float = 1f
) {
    Image(painterResource(resource), null, modifier, contentScale = resizeMode, alpha = alpha)
}
