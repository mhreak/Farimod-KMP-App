package ir.farimod.app.screens.mag

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import ir.farimod.app.network.ApiConfig
import ir.farimod.app.network.str
import ir.farimod.app.resources.Res
import ir.farimod.app.resources.blog_post_icon
import ir.farimod.app.ui.LocalImage
import ir.farimod.app.ui.ResizeMode
import kotlinx.serialization.json.JsonObject

/**
 * Port of `screens/mag/component/BlogImageComponent.tsx`.
 * Shows `blog_post_icon` when the featured image is missing, ends with "/", or fails to load.
 */
@Composable
internal fun MagBlogImage(item: JsonObject) {
    val imageUrl = item.str("FeaturedImageURL")
    val fileName = item.str("FeaturedImageFileName")
    var imageError by remember(imageUrl) { mutableStateOf(false) }
    val hasValidImage = !fileName.isNullOrEmpty() && !imageUrl.isNullOrEmpty() && !imageUrl.endsWith("/")

    if (!hasValidImage || imageError) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color(0xFFF5F5F5))
        ) {
            LocalImage(
                Res.drawable.blog_post_icon,
                Modifier.fillMaxSize(),
                ResizeMode.cover
            )
        }
        return
    }

    val resolved = ApiConfig.resolveMediaUrl(imageUrl)
    Box(Modifier.fillMaxWidth().height(200.dp)) {
        if (resolved == null) {
            Box(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
                LocalImage(Res.drawable.blog_post_icon, Modifier.fillMaxSize(), ResizeMode.cover)
            }
        } else {
            AsyncImage(
                model = resolved,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = { imageError = true }
            )
        }
    }
}
