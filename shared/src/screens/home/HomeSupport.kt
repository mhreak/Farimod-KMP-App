package ir.farimod.app.screens.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.network.bool
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.touchableOpacity
import kotlinx.serialization.json.JsonElement

internal fun homeTryNavigate(
    showToast: (String, String) -> Unit,
    failMessage: String,
    block: () -> Unit
) {
    try {
        block()
    } catch (_: Exception) {
        showToast(failMessage, "error")
    }
}

internal object HomeAutoScrollIntervals {
    const val slides = 5000L
    const val courses = 5000L
    const val products = 6000L
    const val members = 7000L
    const val portfolios = 10000L
    const val galleries = 8000L
    const val blogPosts = 9000L
    const val memberGroups = 6500L
}

internal fun homePageCount(itemCount: Int, perPage: Int): Int {
    if (itemCount <= 0 || perPage <= 0) return 0
    return (itemCount + perPage - 1) / perPage
}

internal fun List<JsonElement>.homeChunks(size: Int, reversed: Boolean): List<List<JsonElement>> {
    val source = if (reversed) asReversed() else this
    if (source.isEmpty()) return emptyList()
    return source.chunked(size)
}

internal fun JsonElement.homeInt(key: String): Int = this[key].int ?: 0

/** RN `a || b` for ids: 0 / missing is falsy. */
internal fun JsonElement.homeIdOr(primary: String, fallback: String): Int {
    val a = this[primary]
    val b = this[fallback]
    val chosen = if (a.bool) a else b
    return chosen.int ?: 0
}

internal fun JsonElement.slideEntityId(): Int = homeIdOr("EntityId", "ShowOrder")

internal fun JsonElement.slideClickTrigger(): Int = homeInt("ClickTrigger")

internal fun JsonElement.isHomeSlideClickable(): Boolean = slideClickTrigger() in 1..5

/** RN negative marginTop: shifts the child up and shrinks the space it occupies. */
fun Modifier.homeNegativeMarginTop(margin: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val shift = margin.roundToPx()
    val height = (placeable.height - shift).coerceAtLeast(0)
    layout(placeable.width, height) {
        placeable.place(0, -shift)
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.homeSpringPress(
    enabled: Boolean = true,
    activeOpacity: Float = 0.8f,
    onClick: () -> Unit
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale = remember { Animatable(1f) }
    LaunchedEffect(pressed) {
        scale.animateTo(
            if (pressed) 0.95f else 1f,
            spring(dampingRatio = 0.5f, stiffness = 100f)
        )
    }
    this
        .graphicsLayer {
            scaleX = scale.value
            scaleY = scale.value
            alpha = if (pressed && enabled) activeOpacity else 1f
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

@Composable
internal fun HomeSectionHeader(
    label: String,
    dotColor: Color,
    loading: Boolean,
    hasError: Boolean,
    onViewAll: () -> Unit
) {
    RowReverse(
        modifier = Modifier
            .padding(top = 15.dp, bottom = 8.dp)
            .absolutePadding(left = 16.dp, right = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowReverse(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).background(dotColor))
            AppText(
                text = label,
                modifier = Modifier.absolutePadding(right = 10.dp),
                fontSize = 20.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold
            )
        }
        if (!hasError) {
            if (loading) {
                HomeSkeleton(Modifier.size(width = 80.dp, height = 30.dp), cornerRadius = 15.dp)
            } else {
                Row(
                    modifier = Modifier.touchableOpacity { onViewAll() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppText(
                        text = "مشاهده همه",
                        fontSize = 14.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = FarimodColors.Primary,
                        textAlign = TextAlign.Right
                    )
                    MaterialIcons(
                        name = "chevron-left",
                        size = 18.dp,
                        color = FarimodColors.Primary,
                        modifier = Modifier.absolutePadding(right = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun HomeErrorBlock(message: String, onRetry: () -> Unit) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .absolutePadding(top = 20.dp, bottom = 20.dp)
            .fillMaxWidth()
            .heightIn(min = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MaterialIcons(name = "error", size = 48.dp, color = Color(0xFF9E9E9E))
        AppText(
            text = message,
            modifier = Modifier.padding(top = 12.dp),
            fontSize = 14.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center
        )
        HomeRetryButton(onRetry)
    }
}

@Composable
internal fun HomeRetryButton(onRetry: () -> Unit) {
    RowReverse(
        modifier = Modifier
            .padding(top = 12.dp)
            .touchableOpacity { onRetry() }
            .background(FarimodColors.Primary, RoundedCornerShape(8.dp))
            .absolutePadding(left = 16.dp, right = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MaterialIcons(name = "refresh", size = 20.dp, color = FarimodColors.White)
        AppText(
            text = "تلاش مجدد",
            modifier = Modifier.absolutePadding(right = 8.dp),
            fontSize = 12.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = FarimodColors.White
        )
    }
}

@Composable
internal fun HomeEmptyBlock(icon: String, message: String, height: Dp) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .absolutePadding(top = 20.dp, bottom = 20.dp)
            .fillMaxWidth()
            .heightIn(min = height),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MaterialIcons(name = icon, size = 48.dp, color = Color(0xFF9E9E9E))
        AppText(
            text = message,
            modifier = Modifier.padding(top = 12.dp),
            fontSize = 16.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Horizontal card strip. `reverseLayout` keeps the first item on the right,
 * matching the home screen's right-to-left rows, without paging or auto-scroll.
 */
@Composable
internal fun HomeCardRow(
    modifier: Modifier = Modifier,
    itemSpacing: Dp = 10.dp,
    contentPaddingHorizontal: Dp = 10.dp,
    content: LazyListScope.() -> Unit
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        reverseLayout = false,
        horizontalArrangement = Arrangement.spacedBy(itemSpacing),
        contentPadding = PaddingValues(start = contentPaddingHorizontal, end = contentPaddingHorizontal),
        content = content
    )
}
