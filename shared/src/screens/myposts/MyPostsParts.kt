package ir.farimod.app.screens.myposts

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import ir.farimod.app.network.Api
import ir.farimod.app.network.ApiConfig
import ir.farimod.app.network.ApiNetworkException
import ir.farimod.app.network.int
import ir.farimod.app.network.list
import ir.farimod.app.network.str
import ir.farimod.app.resources.Res
import ir.farimod.app.resources.blog_post_icon
import ir.farimod.app.ui.LocalImage
import ir.farimod.app.ui.RNTimingEasing
import ir.farimod.app.ui.ResizeMode
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.rnShadow
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

internal const val POSTS_PAGE_SIZE = 20

internal class MyPostsPager {
    var items by mutableStateOf<List<JsonObject>>(emptyList())
    var page by mutableStateOf(1)
    var hasMore by mutableStateOf(true)
    var total by mutableStateOf(0)
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    suspend fun fetch(memberId: String, newPage: Int, pageSize: Int = POSTS_PAGE_SIZE) {
        try {
            loading = true
            error = null
            val response = Api.get(
                "BlogPost/GetAll?filterMemberId=$memberId&currentPage=$newPage&pageSize=$pageSize"
            )
            if (!response.ok) throw Exception("HTTP error! status: ${response.status}")
            val result = response.json
            val data = result.list("Data").mapNotNull { it as? JsonObject }
            items = if (newPage == 1) data else items + data
            total = result.int("Total") ?: 0
            page = newPage
            hasMore = data.size == pageSize && data.isNotEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: if (e is ApiNetworkException) e.message else "error"
            if (newPage == 1) items = emptyList()
            total = 0
        } finally {
            loading = false
        }
    }
}

@Composable
internal fun postsPulseColor(): Color {
    val transition = rememberInfiniteTransition(label = "postsSkel")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = RNTimingEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "postsPulse"
    )
    return lerp(Color(0xFFE0E0E0), Color(0xFFF0F0F0), t)
}

@Composable
private fun PostsPulse(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp,
    fraction: Float? = null,
    radius: Dp = 8.dp
) {
    val shape = RoundedCornerShape(radius)
    val sizeMod = when {
        width != null -> Modifier.width(width)
        fraction != null -> Modifier.fillMaxWidth(fraction)
        else -> Modifier.fillMaxWidth()
    }
    Box(modifier.then(sizeMod).height(height).background(postsPulseColor(), shape))
}

@Composable
internal fun BlogPostCardSkeleton() {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .rnShadow(1.dp, shape, Color.Black)
            .clip(shape)
            .background(Color.White)
    ) {
        PostsPulse(modifier = Modifier.fillMaxWidth(), height = 200.dp, radius = 12.dp)
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.Start) {
            PostsPulse(height = 18.dp, fraction = 0.9f, radius = 4.dp, modifier = Modifier.padding(bottom = 12.dp))
            PostsPulse(height = 16.dp, fraction = 0.7f, radius = 4.dp, modifier = Modifier.padding(bottom = 8.dp))
            RowReverse(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RowReverse(verticalAlignment = Alignment.CenterVertically) {
                    PostsPulse(width = 16.dp, height = 16.dp, radius = 8.dp, modifier = Modifier.absolutePadding(left = 6.dp))
                    PostsPulse(width = 80.dp, height = 14.dp, radius = 4.dp)
                }
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    PostsPulse(width = 16.dp, height = 16.dp, radius = 8.dp, modifier = Modifier.absolutePadding(right = 6.dp))
                    PostsPulse(width = 30.dp, height = 14.dp, radius = 4.dp)
                }
            }
        }
    }
}

internal fun postHasValidImage(item: JsonElement?): Boolean {
    val name = item.str("FeaturedImageFileName")
    val url = item.str("FeaturedImageURL")
    if (name.isNullOrEmpty() || url.isNullOrEmpty()) return false
    if (url.endsWith("/")) return false
    return true
}

@Composable
internal fun BlogPostImage(item: JsonElement) {
    var imageError by androidx.compose.runtime.remember(item.str("FeaturedImageURL")) {
        androidx.compose.runtime.mutableStateOf(false)
    }
    val url = item.str("FeaturedImageURL")
    val showRemote = postHasValidImage(item) && !imageError && !url.isNullOrEmpty()
    if (!showRemote) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            LocalImage(Res.drawable.blog_post_icon, Modifier.fillMaxSize(), ResizeMode.cover)
        }
    } else {
        Box(Modifier.fillMaxWidth().height(200.dp)) {
            AsyncImage(
                model = ApiConfig.resolveMediaUrl(url),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = { imageError = true }
            )
        }
    }
}
