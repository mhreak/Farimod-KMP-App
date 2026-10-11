package ir.farimod.app.screens.magdetailes

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.farimod.app.network.ApiConfig
import ir.farimod.app.network.get
import ir.farimod.app.ui.RNTimingEasing
import ir.farimod.app.ui.RatingOption
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.utils.jsToNumber
import ir.farimod.app.utils.jsToString
import ir.farimod.app.utils.jsTruthy
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import coil3.compose.AsyncImage
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import ir.farimod.app.resources.Res
import ir.farimod.app.resources.blog_post_icon
import ir.farimod.app.ui.LocalImage
import ir.farimod.app.ui.ResizeMode

/**
 * Mag detail's own `transformContentReviewToRatingOptions` (not the product-detail defaults).
 * Empty / missing list → کیفیت محتوا، نگارش، کاربردی بودن، جامعیت.
 */
internal fun magTransformContentReviewToRatingOptions(contentReviewList: JsonElement?): List<RatingOption> {
    val items = (contentReviewList as? JsonArray)?.toList()
    if (items == null || items.isEmpty()) {
        return listOf(
            RatingOption(
                id = "content",
                title = "کیفیت محتوا",
                subtitle = "کیفیت و مفید بودن محتوای مقاله",
                contentReviewItemId = "content"
            ),
            RatingOption(
                id = "writing",
                title = "نگارش",
                subtitle = "کیفیت نوشتار و روان بودن متن",
                contentReviewItemId = "writing"
            ),
            RatingOption(
                id = "usefulness",
                title = "کاربردی بودن",
                subtitle = "میزان مفید و قابل استفاده بودن",
                contentReviewItemId = "usefulness"
            ),
            RatingOption(
                id = "comprehensiveness",
                title = "جامعیت",
                subtitle = "پوشش کامل موضوع",
                contentReviewItemId = "comprehensiveness"
            )
        )
    }
    return items
        .filter { item -> item["Active"].let { jsTruthy(it) } }
        .sortedWith { a, b ->
            val d = jsToNumber(a["ShowOrder"]) - jsToNumber(b["ShowOrder"])
            if (a["ShowOrder"] == null || b["ShowOrder"] == null || d.isNaN()) 0
            else if (d < 0) -1 else if (d > 0) 1 else 0
        }
        .map { item ->
            val rawId = (item as? JsonObject)?.get("ContentReviewItemId")
            val average = item["CalculatedAverageRating"]
            RatingOption(
                id = "review_" + (if (rawId == null) "undefined" else jsToString(rawId)),
                title = item["Text"]?.let { jsToString(it) } ?: "",
                subtitle = "",
                contentReviewItemId = item["ContentReviewItemId"],
                showOrder = item["ShowOrder"]?.let { jsToNumber(it) },
                averageRating = if (jsTruthy(average)) jsToNumber(average) else 0.0
            )
        }
}

/** RN negative marginTop: pulls this child up and shrinks the space it occupies. */
internal fun Modifier.magNegativeTop(amount: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val shift = amount.roundToPx()
    layout(placeable.width, (placeable.height - shift).coerceAtLeast(0)) {
        placeable.place(0, -shift)
    }
}

/** RN `borderRightWidth` / `borderRightColor`. */
internal fun Modifier.magRightBorder(width: Dp, color: Color): Modifier = drawBehind {
    val w = width.toPx()
    drawRect(color, topLeft = Offset(size.width - w, 0f), size = Size(w, size.height))
}

/** Featured image with the same validity rules as MagDetailesScreen.hasValidImage, plus onError. */
@Composable
internal fun MagDetailCover(
    url: String?,
    valid: Boolean,
    onError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val resolved = if (valid) ApiConfig.resolveMediaUrl(url) else null
    var failed by remember(url) { mutableStateOf(false) }
    if (!valid || resolved == null || failed) {
        LocalImage(Res.drawable.blog_post_icon, modifier, ResizeMode.cover)
        return
    }
    AsyncImage(
        model = resolved,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        onError = {
            failed = true
            onError()
        }
    )
}

/** Port of the inline BlogDetailSkeleton (opacity 1 ↔ 0.4, 800ms each way). */
@Composable
internal fun MagDetailSkeleton() {
    val transition = rememberInfiniteTransition()
    val opacity by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = RNTimingEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    val shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(Color(0xFFE0E0E0).copy(alpha = opacity))
        )
        Column(
            Modifier
                .fillMaxWidth()
                .magNegativeTop(30.dp)
                .rnShadow(8.dp, shape)
                .clip(shape)
                .background(Color.White)
                .absolutePadding(left = 25.dp, right = 25.dp)
                .padding(top = 30.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.Start
        ) {
            MagDetailBone(Modifier.fillMaxWidth(0.9f).absolutePadding(bottom = 20.dp).height(24.dp), opacity)
            MagDetailBone(Modifier.fillMaxWidth(0.7f).absolutePadding(bottom = 20.dp).height(24.dp), opacity)
            MagDetailBone(Modifier.fillMaxWidth(0.95f).absolutePadding(bottom = 12.dp).height(18.dp), opacity)
            MagDetailBone(Modifier.fillMaxWidth(0.85f).absolutePadding(bottom = 12.dp).height(18.dp), opacity)
            MagDetailBone(Modifier.fillMaxWidth(0.92f).absolutePadding(bottom = 12.dp).height(18.dp), opacity)
            MagDetailBone(Modifier.fillMaxWidth(0.88f).absolutePadding(bottom = 12.dp).height(18.dp), opacity)
            MagDetailBone(Modifier.fillMaxWidth(0.9f).absolutePadding(bottom = 12.dp).height(18.dp), opacity)
            MagDetailBone(Modifier.fillMaxWidth(0.75f).absolutePadding(bottom = 20.dp).height(18.dp), opacity)
            RowReverse(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .drawBehind {
                        drawRect(Color(0xFFF0F0F0), size = Size(size.width, 1.dp.toPx()))
                    }
                    .padding(top = 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(17.dp)
                        .absolutePadding(left = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE0E0E0).copy(alpha = opacity))
                )
                Box(
                    Modifier
                        .width(80.dp)
                        .height(15.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE0E0E0).copy(alpha = opacity))
                )
            }
        }
    }
}

@Composable
private fun MagDetailBone(modifier: Modifier, opacity: Float) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE0E0E0).copy(alpha = opacity))
    )
}
