package ir.farimod.app.screens.productdetail

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.network.ApiConfig
import ir.farimod.app.network.bool
import ir.farimod.app.network.double
import ir.farimod.app.network.get
import ir.farimod.app.network.str
import ir.farimod.app.resources.Res
import ir.farimod.app.resources.product_icon
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.LocalImage
import ir.farimod.app.ui.MainBackground
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RatingOption
import ir.farimod.app.ui.ResizeMode
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.pressable
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.ui.windowSize
import ir.farimod.app.utils.jsToString
import ir.farimod.app.utils.jsTruthy
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement

internal val PdPrimary = Color(0xFF667EEA)
internal val PdDark = Color(0xFF2C3E50)
internal val PdSurface = Color.White
internal val PdSuccess = Color(0xFF2ECC71)
internal val PdWarning = Color(0xFFF39C12)
internal val PdError = Color(0xFFE74C3C)
internal val PdInfo = Color(0xFF3498DB)
internal val PdGold = Color(0xFFFFD700)
internal val PdLike = Color(0xFFE91E63)

private val galleryFields = listOf(
    "FeaturedImageURL",
    "FirstImageURL",
    "SecondImageURL",
    "ThirdImageURL",
    "FourthImageURL",
    "FifthImageURL"
)

internal fun productGalleryUrls(product: JsonElement?): List<String?> {
    if (product == null) return listOf(null)
    val urls = galleryFields.mapNotNull { field ->
        val url = product.str(field)
        if (url != null && url != "string" && url.trim().isNotEmpty()) url else null
    }
    return if (urls.isEmpty()) listOf(null) else urls
}

internal fun detailIconColor(icon: String): Color = when (icon) {
    "attach-money" -> Color(0xFF2ECC71)
    "person" -> Color(0xFF3498DB)
    "category" -> Color(0xFF9B59B6)
    "check-circle" -> PdSuccess
    "cancel" -> PdError
    "date-range" -> PdError
    "favorite" -> PdLike
    "description" -> PdInfo
    else -> PdPrimary
}

internal fun transformContentReviewToRatingOptions(list: List<JsonElement>?): List<RatingOption> {
    if (list.isNullOrEmpty()) {
        return listOf(
            RatingOption("quality", "کیفیت محصول", "کیفیت کلی و ساخت", "quality"),
            RatingOption("price", "ارزش خرید", "تناسب قیمت و کیفیت", "price"),
            RatingOption("design", "طراحی", "زیبایی و جذابیت", "design"),
            RatingOption("satisfaction", "رضایت کلی", "رضایت از خرید", "satisfaction")
        )
    }
    return list
        .filter { it.bool("Active") }
        .sortedBy { it.double("ShowOrder") ?: 0.0 }
        .map { item ->
            val reviewId = item["ContentReviewItemId"]
            RatingOption(
                id = "review_${jsToString(reviewId)}",
                title = item.str("Text").orEmpty(),
                subtitle = "",
                contentReviewItemId = reviewId,
                showOrder = item.double("ShowOrder"),
                averageRating = item.double("CalculatedAverageRating") ?: 0.0
            )
        }
}

@Composable
internal fun ProductDetailBackButton(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val click = onClick
    Box(
        modifier
            .offset(y = (-12).dp)
            .size(44.dp)
            .rnShadow(5.dp, CircleShape)
            .clip(CircleShape)
            .background(Color(1f, 1f, 1f, 0.9f))
            .then(if (click != null) Modifier.touchableOpacity { click() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        MaterialIcons("arrow-forward", size = 24.dp, color = Color(0xFF6366F1))
    }
}

@Composable
internal fun ProductDetailMenuButton(modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier
            .size(44.dp)
            .rnShadow(5.dp, CircleShape)
            .clip(CircleShape)
            .background(Color(1f, 1f, 1f, 0.9f))
            .touchableOpacity(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        MaterialIcons("more-vert", size = 24.dp, color = Color(0xFF6366F1))
    }
}

@Composable
private fun PdPulse(modifier: Modifier) {
    val transition = rememberInfiniteTransition()
    val opacity by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
    )
    Box(modifier.graphicsLayer { alpha = opacity }.background(Color(0xFFE0E0E0)))
}

@Composable
internal fun ProductDetailsSkeleton(onBack: () -> Unit) {
    val side = windowSize().first - 40.dp
    Box(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        MainBackground()
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarHeight() + 35.dp)
                    .absolutePadding(left = 20.dp, right = 20.dp)
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                PdPulse(Modifier.width(200.dp).height(26.dp).clip(RoundedCornerShape(13.dp)))
            }
            Box(Modifier.padding(20.dp).fillMaxWidth().height(side)) {
                PdPulse(Modifier.fillMaxSize().clip(RoundedCornerShape(30.dp)))
            }
            Row(
                Modifier
                    .absolutePadding(left = 20.dp, right = 20.dp)
                    .padding(bottom = 20.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(1f, 1f, 1f, 0.9f))
                    .border(1.dp, Color(0.796f, 0.835f, 0.882f, 0.3f), RoundedCornerShape(20.dp))
                    .padding(20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PdPulse(Modifier.width(120.dp).height(20.dp).clip(RoundedCornerShape(10.dp)))
                    Spacer(Modifier.height(8.dp))
                    PdPulse(Modifier.width(100.dp).height(16.dp).clip(RoundedCornerShape(8.dp)))
                }
            }
            RowReverse(
                Modifier
                    .fillMaxWidth()
                    .absolutePadding(left = 20.dp, right = 20.dp)
                    .padding(top = 10.dp, bottom = 30.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PdPulse(
                    Modifier
                        .absolutePadding(left = 15.dp)
                        .size(50.dp)
                        .clip(CircleShape)
                )
                PdPulse(Modifier.width(150.dp).height(24.dp).clip(RoundedCornerShape(12.dp)))
            }
            Column(Modifier.absolutePadding(left = 20.dp, right = 20.dp)) {
                repeat(5) {
                    Box(
                        Modifier
                            .absolutePadding(left = 5.dp, right = 5.dp)
                            .padding(bottom = 14.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0.973f, 0.980f, 0.988f, 0.3f))
                            .border(1.dp, Color(0.796f, 0.835f, 0.882f, 0.4f), RoundedCornerShape(22.dp))
                            .padding(18.dp)
                    ) {
                        RowReverse(verticalAlignment = Alignment.CenterVertically) {
                            PdPulse(
                                Modifier
                                    .absolutePadding(left = 12.dp)
                                    .size(44.dp)
                                    .clip(CircleShape)
                            )
                            PdPulse(Modifier.fillMaxWidth(0.7f).height(17.dp).clip(RoundedCornerShape(8.dp)))
                        }
                    }
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .absolutePadding(left = 20.dp, right = 20.dp)
                    .padding(bottom = 25.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PdPulse(
                    Modifier
                        .padding(bottom = 18.dp)
                        .fillMaxWidth(0.92f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(30.dp))
                )
                PdPulse(Modifier.fillMaxWidth(0.7f).height(48.dp).clip(RoundedCornerShape(25.dp)))
            }
        }
        ProductDetailBackButton(
            modifier = Modifier
                .align(AbsoluteAlignment.TopRight)
                .zIndex(1000f)
                .absoluteBack(statusBarHeight()),
            onClick = onBack
        )
    }
}

private fun Modifier.absoluteBack(statusTop: Dp): Modifier =
    this.absoluteOffset(x = (-20).dp, y = statusTop + 45.dp)

@Composable
internal fun ProductDetailImage(
    url: String?,
    modifier: Modifier,
    resizeMode: ContentScale = ResizeMode.cover
) {
    var imageError by remember(url) { mutableStateOf(false) }
    var loading by remember(url) { mutableStateOf(url != null) }
    Box(modifier) {
        if (url == null || imageError) {
            LocalImage(
                Res.drawable.product_icon,
                Modifier.matchParentSize().background(Color(0xFFF5F5F5)),
                resizeMode
            )
        } else {
            AsyncImage(
                model = ApiConfig.resolveMediaUrl(url),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = resizeMode,
                onSuccess = {
                    loading = false
                    imageError = false
                },
                onError = {
                    imageError = true
                    loading = false
                }
            )
            if (loading) {
                Box(
                    Modifier.matchParentSize().background(Color(0xFFF5F5F5)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFFCCCCCC),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ProductGalleryHeader(
    urls: List<String?>,
    productName: String,
    active: Boolean,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
    onImagePress: (Int) -> Unit,
    overlay: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {}
) {
    val side = windowSize().first - 40.dp
    val multiple = urls.size > 1
    val pagerState = rememberPagerState(initialPage = currentIndex.coerceIn(0, (urls.size - 1).coerceAtLeast(0))) {
        urls.size.coerceAtLeast(1)
    }
    val fromPager = remember { booleanArrayOf(false) }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (!multiple) return@collect
            fromPager[0] = true
            onIndexChange(page)
        }
    }
    LaunchedEffect(currentIndex) {
        if (!multiple) return@LaunchedEffect
        if (fromPager[0]) {
            fromPager[0] = false
            return@LaunchedEffect
        }
        if (pagerState.currentPage != currentIndex) pagerState.animateScrollToPage(currentIndex)
    }

    Box(
        Modifier
            .padding(20.dp)
            .fillMaxWidth()
            .height(side)
            .rnShadow(10.dp, RoundedCornerShape(20.dp), PdPrimary)
            .clip(RoundedCornerShape(20.dp))
    ) {
        if (multiple) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .touchableOpacity(activeOpacity = 0.95f) { onImagePress(page) }
                ) {
                    ProductDetailImage(urls.getOrNull(page), Modifier.fillMaxSize(), ResizeMode.cover)
                }
            }
            if (currentIndex == 0) {
                LinearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0.4f, 0.494f, 0.918f, 0.9f),
                        Color(0.463f, 0.294f, 0.635f, 0.95f)
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.6f)
                ) {
                    Box(Modifier.fillMaxSize().padding(15.dp), contentAlignment = Alignment.BottomCenter) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(1f, 1f, 1f, 0.12f))
                                .border(1.dp, Color(1f, 1f, 1f, 0.25f), RoundedCornerShape(20.dp))
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AppText(
                                text = productName,
                                modifier = Modifier.padding(bottom = 12.dp),
                                fontSize = 24.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Bold,
                                color = PdSurface,
                                textAlign = TextAlign.Center,
                                lineHeight = 32.sp
                            )
                            Row(
                                Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(1f, 1f, 1f, 0.2f))
                                    .border(1.dp, Color(1f, 1f, 1f, 0.3f), RoundedCornerShape(20.dp))
                                    .absolutePadding(left = 12.dp, right = 12.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MaterialIcons(
                                    if (active) "check-circle" else "cancel",
                                    size = 16.dp,
                                    color = PdSurface
                                )
                                AppText(
                                    text = if (active) "فعال" else "غیرفعال",
                                    modifier = Modifier.absolutePadding(left = 6.dp),
                                    fontSize = 14.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                                    color = PdSurface
                                )
                            }
                        }
                    }
                }
            }
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 15.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                urls.forEachIndexed { index, _ ->
                    Box(
                        Modifier
                            .height(8.dp)
                            .width(if (index == currentIndex) 20.dp else 8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (index == currentIndex) Color(1f, 1f, 1f, 0.9f)
                                else Color(1f, 1f, 1f, 0.5f)
                            )
                    )
                }
            }
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .touchableOpacity(activeOpacity = 0.95f) { onImagePress(0) }
            ) {
                ProductDetailImage(urls.firstOrNull(), Modifier.fillMaxSize(), ResizeMode.cover)
            }
        }
        overlay()
    }
}

@Composable
internal fun ProductThumbnailRow(urls: List<String?>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    val display = urls.filter { it != null || urls.size == 1 }
    if (display.size <= 1) return
    Box(
        Modifier
            .absolutePadding(left = 20.dp, right = 20.dp)
            .padding(bottom = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(Color(1f, 1f, 1f, 0.95f))
            .border(1.dp, Color(0.796f, 0.835f, 0.882f, 0.4f), RoundedCornerShape(15.dp))
            .padding(12.dp)
    ) {
        LazyRow(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(display) { index, url ->
                val selected = selectedIndex == index
                Box(
                    Modifier
                        .graphicsLayer {
                            val s = if (selected) 1.05f else 1f
                            scaleX = s
                            scaleY = s
                        }
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            2.dp,
                            if (selected) PdPrimary else Color.Transparent,
                            RoundedCornerShape(12.dp)
                        )
                        .touchableOpacity(activeOpacity = 0.7f) { onSelect(index) }
                ) {
                    ProductDetailImage(url, Modifier.fillMaxSize(), ResizeMode.cover)
                }
            }
        }
    }
}

@Composable
internal fun ProductSmartDetail(
    label: String,
    value: String,
    icon: String,
    maxLength: Int = 30
) {
    if (!jsTruthy(value) || value == "نامشخص" || value == "تاریخ مشخص نشده") return
    val color = detailIconColor(icon)
    val longText = value.length > maxLength
    Box(
        Modifier
            .absolutePadding(left = 5.dp, right = 5.dp)
            .padding(bottom = 14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0.973f, 0.980f, 0.988f, 0.3f))
            .border(1.dp, Color(0.796f, 0.835f, 0.882f, 0.4f), RoundedCornerShape(22.dp))
    ) {
        Column(Modifier.padding(18.dp)) {
            if (longText) {
                DetailLabel(label, icon, color)
                AppText(
                    text = value,
                    modifier = Modifier
                        .absolutePadding(left = 15.dp, right = 15.dp)
                        .padding(top = 15.dp),
                    fontSize = 16.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    color = Color(0xFF374151),
                    textAlign = TextAlign.Justify,
                    lineHeight = 26.sp
                )
            } else {
                RowReverse(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DetailLabel(label, icon, color, Modifier.weight(1f))
                    Box(Modifier.weight(1f).absolutePadding(left = 15.dp)) {
                        AppText(
                            text = value,
                            fontSize = 16.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                            color = Color(0xFF374151),
                            textAlign = TextAlign.Right,
                            lineHeight = 26.sp
                        )
                    }
                }
            }
        }
        Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(color.copy(alpha = 0x60 / 255f))
            )
        }
    }
}

@Composable
private fun DetailLabel(label: String, icon: String, color: Color, modifier: Modifier = Modifier) {
    RowReverse(modifier, verticalAlignment = Alignment.CenterVertically) {
        LinearGradient(
            colors = listOf(color, color.copy(alpha = 0xCC / 255f)),
            modifier = Modifier
                .absolutePadding(left = 12.dp)
                .size(44.dp),
            shape = CircleShape
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MaterialIcons(icon, size = 22.dp, color = PdSurface)
            }
        }
        AppText(
            text = label,
            fontSize = 17.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = PdDark
        )
    }
}

@Composable
internal fun ProductActionSheet(
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val slide = remember { Animatable(0f) }
    val backdrop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        coroutineScope {
            launch { backdrop.animateTo(1f, tween(300)) }
            launch { slide.animateTo(1f, tween(300)) }
        }
    }
    fun close() {
        scope.launch {
            coroutineScope {
                launch { backdrop.animateTo(0f, tween(250)) }
                launch { slide.animateTo(0f, tween(250)) }
            }
            onDismiss()
        }
    }
    Dialog(
        onDismissRequest = { close() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = backdrop.value }
                    .background(Color(0f, 0f, 0f, 0.5f))
                    .pressable { close() }
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = (1f - slide.value) * 300.dp.toPx() },
                verticalArrangement = Arrangement.Bottom
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .rnShadow(10.dp, RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                        .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                        .background(Color.White)
                        .padding(top = 15.dp)
                        .absolutePadding(left = 20.dp, right = 20.dp)
                ) {
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 20.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFE0E0E0))
                    )
                    AppText(
                        text = "عملیات محصول",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 25.dp),
                        fontSize = 18.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = PdDark,
                        textAlign = TextAlign.Center
                    )
                    ActionRow(
                        icon = "edit",
                        iconBg = PdInfo,
                        title = "ویرایش محصول",
                        subtitle = "ویرایش نام، قیمت و مشخصات محصول",
                        onClick = {
                            onEdit()
                            close()
                        }
                    )
                    ActionRow(
                        icon = "delete",
                        iconBg = PdError,
                        title = "حذف محصول",
                        subtitle = "حذف کامل محصول از سیستم",
                        enabled = !isDeleting,
                        onClick = {
                            onDelete()
                            close()
                        }
                    )
                    Box(
                        Modifier
                            .padding(bottom = 30.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color(0xFFF8F9FA))
                            .border(1.dp, Color(0xFFE9ECEF), RoundedCornerShape(15.dp))
                            .touchableOpacity { close() }
                            .absolutePadding(top = 15.dp, bottom = 15.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppText(
                            text = "لغو",
                            fontSize = 16.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            color = Color(0xFF6C757D),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    icon: String,
    iconBg: Color,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .padding(bottom = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(Color(0xFFF8F9FA))
            .border(1.dp, Color(0xFFE9ECEF), RoundedCornerShape(15.dp))
            .touchableOpacity(enabled = enabled, onClick = onClick)
            .absolutePadding(left = 10.dp, right = 10.dp, top = 15.dp, bottom = 15.dp)
    ) {
        RowReverse(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .absolutePadding(left = 15.dp)
                    .size(45.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                MaterialIcons(icon, size = 22.dp, color = Color.White)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                AppText(
                    text = title,
                    modifier = Modifier.padding(bottom = 2.dp),
                    fontSize = 16.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = PdDark,
                    textAlign = TextAlign.Right
                )
                AppText(
                    text = subtitle,
                    fontSize = 13.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    color = Color(0xFF6C757D),
                    textAlign = TextAlign.Right
                )
            }
        }
    }
}

@Composable
internal fun ProductDeleteSheet(
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val slide = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        slide.animateTo(1f, tween(300))
    }
    fun close() {
        scope.launch {
            slide.animateTo(0f, tween(250))
            onDismiss()
        }
    }
    Dialog(
        onDismissRequest = { if (!isDeleting) close() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0f, 0f, 0f, 0.5f))
                    .pressable { if (!isDeleting) close() }
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = (1f - slide.value) * 300.dp.toPx() },
                verticalArrangement = Arrangement.Bottom
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .rnShadow(10.dp, RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                        .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                        .background(Color.White)
                        .padding(top = 15.dp, bottom = 35.dp)
                        .absolutePadding(left = 25.dp, right = 25.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .padding(bottom = 20.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFE0E0E0))
                    )
                    Box(
                        Modifier
                            .padding(bottom = 20.dp)
                            .size(70.dp)
                            .rnShadow(8.dp, CircleShape, PdError)
                            .clip(CircleShape)
                            .background(PdError),
                        contentAlignment = Alignment.Center
                    ) {
                        MaterialIcons("warning", size = 32.dp, color = Color.White)
                    }
                    AppText(
                        text = "تأیید حذف",
                        modifier = Modifier.padding(bottom = 15.dp),
                        fontSize = 20.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = PdDark,
                        textAlign = TextAlign.Center
                    )
                    AppText(
                        text = "آیا از حذف این محصول اطمینان دارید؟\nاین عمل قابل بازگشت نیست.",
                        modifier = Modifier.padding(bottom = 30.dp),
                        fontSize = 16.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        color = Color(0xFF6C757D),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                    RowReverse(
                        Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Color(0xFFF8F9FA))
                                .border(1.dp, Color(0xFFE9ECEF), RoundedCornerShape(15.dp))
                                .touchableOpacity(enabled = !isDeleting) { close() }
                                .absolutePadding(top = 16.dp, bottom = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AppText(
                                text = "لغو",
                                fontSize = 16.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Bold,
                                color = Color(0xFF6C757D)
                            )
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .rnShadow(8.dp, RoundedCornerShape(15.dp), PdError)
                                .clip(RoundedCornerShape(15.dp))
                                .touchableOpacity(enabled = !isDeleting) { onConfirm() }
                        ) {
                            LinearGradient(
                                colors = listOf(PdError, Color(0xFFC0392B)),
                                modifier = Modifier.fillMaxWidth(),
                                start = Offset(0f, 0f),
                                end = Offset(1f, 1f),
                                shape = RoundedCornerShape(15.dp)
                            ) {
                                RowReverse(
                                    Modifier
                                        .fillMaxWidth()
                                        .absolutePadding(left = 20.dp, right = 20.dp, top = 16.dp, bottom = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isDeleting) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        MaterialIcons("delete-forever", size = 20.dp, color = Color.White)
                                    }
                                    AppText(
                                        text = if (isDeleting) "در حال حذف..." else "بله، حذف کن",
                                        fontSize = 16.sp,
                                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
