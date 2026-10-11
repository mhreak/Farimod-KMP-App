package ir.farimod.app.screens.products

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.network.ApiConfig
import ir.farimod.app.network.get
import ir.farimod.app.network.str
import ir.farimod.app.resources.Res
import ir.farimod.app.resources.product_icon
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.LocalImage
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.ResizeMode
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.utils.formatPrice
import ir.farimod.app.utils.jsTruthy
import ir.farimod.app.utils.safeNumber
import ir.farimod.app.utils.safeString
import ir.farimod.app.utils.toPersianDigits
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlin.math.roundToInt

internal const val PRODUCTS_PAGE_SIZE = 20

internal val ProductsPrimary = Color(0xFF667EEA)
internal val ProductsPrimaryDark = Color(0xFF764BA2)
internal val ProductsDark = Color(0xFF2C3E50)

internal fun productDiscountPercent(price: Double, specialPrice: Double): Int {
    if (specialPrice <= 0.0 || price <= 0.0) return 0
    return ((price - specialPrice) / price * 100.0).roundToInt()
}

internal fun productCardImageUrl(item: JsonElement, imageError: Boolean): String? {
    if (imageError) return null
    val featured = item.str("FeaturedImageURL")
    if (jsTruthy(featured)) return ApiConfig.resolveMediaUrl(featured)
    val fileName = item.str("FeaturedImageFileName")?.takeIf { jsTruthy(it) }
        ?: item.str("ProductImageFileName")?.takeIf { jsTruthy(it) }
    if (fileName != null) {
        return ApiConfig.resolveMediaUrl("${ApiConfig.BASE_URL}Product/GetProductImage/$fileName")
    }
    return null
}

@Composable
internal fun ProductsSkeletonBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val opacity by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse)
    )
    Box(modifier.background(Color(0xFFE1E5E9)).graphicsLayer { alpha = opacity })
}

@Composable
internal fun ProductsCardSkeleton() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
    ) {
        ProductsSkeletonBox(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFFF5F5F5))
        )
        Column(
            Modifier
                .fillMaxWidth()
                .height(85.dp)
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .absolutePadding(top = 4.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ProductsSkeletonBox(
                    Modifier
                        .fillMaxWidth(0.8f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                )
                ProductsSkeletonBox(
                    Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth(0.6f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
            }
            Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                ProductsSkeletonBox(
                    Modifier
                        .size(width = 80.dp, height = 16.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@Composable
internal fun ProductsProductCard(
    item: JsonElement,
    onPress: (JsonElement) -> Unit,
    modifier: Modifier = Modifier
) {
    var imageError by remember(item) { mutableStateOf(false) }
    val price = safeNumber(item["Price"])
    val specialPrice = safeNumber(item["SpecialSalePrice"])
    val discount = productDiscountPercent(price, specialPrice)
    val imageUrl = productCardImageUrl(item, imageError)

    Column(
        modifier
            .fillMaxWidth()
            .touchableOpacity(activeOpacity = 0.8f) { onPress(item) }
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFFF5F5F5))
        ) {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ResizeMode.cover,
                    onSuccess = { if (imageError) imageError = false },
                    onError = { imageError = true }
                )
            } else {
                LocalImage(Res.drawable.product_icon, Modifier.fillMaxSize(), ResizeMode.cover)
            }
            if (discount > 0) {
                Box(
                    Modifier
                        .align(AbsoluteAlignment.TopRight)
                        .zIndex(2f)
                        .absolutePadding(top = 8.dp, right = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFF6B6B))
                        .absolutePadding(left = 6.dp, right = 6.dp, top = 3.dp, bottom = 3.dp)
                ) {
                    AppText(
                        text = "${toPersianDigits(discount.toString())}% تخفیف",
                        fontSize = 10.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
            if (!item["Active"].let { jsTruthy(it) }) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .zIndex(3f)
                        .background(Color(0f, 0f, 0f, 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    AppText(
                        text = "غیرفعال",
                        fontSize = 12.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .height(85.dp)
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            AppText(
                text = safeString(item["ProductName"], "نام محصول"),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 32.dp),
                fontSize = 14.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = Color(0xFF333333),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (discount > 0) {
                    AppText(
                        text = formatPrice(price),
                        modifier = Modifier.padding(bottom = 2.dp),
                        fontSize = 11.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        color = Color(0xFF999999),
                        textAlign = TextAlign.Center,
                        textDecoration = TextDecoration.LineThrough
                    )
                    AppText(
                        text = formatPrice(specialPrice),
                        fontSize = 14.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color(0xFFFF6B6B),
                        textAlign = TextAlign.Center
                    )
                } else {
                    AppText(
                        text = formatPrice(price),
                        fontSize = 14.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = ProductsPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
internal fun ProductsEmptyState(title: String, subtitle: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .absolutePadding(left = 40.dp, right = 40.dp, top = 60.dp, bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MaterialIcons("shopping-bag", size = 80.dp, color = Color(0xFF9E9E9E))
        AppText(
            text = title,
            modifier = Modifier.padding(top = 20.dp),
            fontSize = 20.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = ProductsDark,
            textAlign = TextAlign.Center
        )
        AppText(
            text = subtitle,
            modifier = Modifier.padding(top = 12.dp),
            fontSize = 16.sp,
            fontFamily = RNFonts.Yekan_Bakh_Regular,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

@Composable
internal fun ProductsErrorState(onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .absolutePadding(left = 40.dp, right = 40.dp, top = 60.dp, bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MaterialIcons("error", size = 80.dp, color = Color(0xFF9E9E9E))
        AppText(
            text = "خطا در دریافت اطلاعات",
            modifier = Modifier.padding(top = 20.dp),
            fontSize = 20.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = ProductsDark,
            textAlign = TextAlign.Center
        )
        AppText(
            text = "لطفاً اتصال اینترنت خود را بررسی کنید",
            modifier = Modifier.padding(top = 12.dp),
            fontSize = 16.sp,
            fontFamily = RNFonts.Yekan_Bakh_Regular,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        RowReverse(
            modifier = Modifier
                .padding(top = 24.dp)
                .rnShadow(8.dp, RoundedCornerShape(25.dp), ProductsPrimary)
                .clip(RoundedCornerShape(25.dp))
                .background(ProductsPrimary)
                .touchableOpacity { onRetry() }
                .absolutePadding(left = 20.dp, right = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MaterialIcons("refresh", size = 20.dp, color = FarimodColors.White)
            AppText(
                text = "تلاش مجدد",
                modifier = Modifier.absolutePadding(right = 8.dp),
                fontSize = 16.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = FarimodColors.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProductsPageButton(page: Int, isActive: Boolean, scale: Float, onPress: () -> Unit) {
    val shape = CircleShape
    Box(
        Modifier
            .absolutePadding(left = 4.dp, right = 4.dp, top = 4.dp, bottom = 4.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (isActive) Modifier.rnShadow(6.dp, shape, ProductsPrimary) else Modifier
            )
            .size(40.dp)
            .clip(shape)
            .background(if (isActive) ProductsPrimary else Color.Transparent)
            .border(1.dp, if (isActive) ProductsPrimary else Color(0xFFE0E0E0), shape)
            .touchableOpacity(activeOpacity = 0.7f, onClick = onPress),
        contentAlignment = Alignment.Center
    ) {
        AppText(
            text = page.toString(),
            fontSize = 16.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = if (isActive) Color.White else Color(0xFF666666),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProductsNavButton(icon: String, onPress: () -> Unit) {
    Box(
        Modifier
            .absolutePadding(left = 6.dp, right = 6.dp, top = 4.dp, bottom = 4.dp)
            .touchableOpacity(activeOpacity = 0.7f, onClick = onPress)
    ) {
        LinearGradient(
            colors = listOf(ProductsPrimary, ProductsPrimaryDark),
            modifier = Modifier
                .size(40.dp)
                .rnShadow(6.dp, CircleShape, ProductsPrimary),
            start = Offset(0f, 0f),
            end = Offset(1f, 1f),
            shape = CircleShape
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MaterialIcons(icon, size = 20.dp, color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProductsPagination(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    destinationScale: Boolean = false
) {
    if (totalPages <= 1) return
    val scope = rememberCoroutineScope()
    val maxVisible = 5
    var start = maxOf(1, currentPage - maxVisible / 2)
    val end = minOf(totalPages, start + maxVisible - 1)
    if (end - start + 1 < maxVisible) start = maxOf(1, end - maxVisible + 1)

    var animatingPage by remember { mutableStateOf<Int?>(null) }
    val sharedScale = remember { Animatable(1f) }

    fun go(page: Int) {
        if (page == currentPage) return
        if (!destinationScale) {
            onPageChange(page)
            return
        }
        scope.launch {
            animatingPage = page
            sharedScale.snapTo(1f)
            sharedScale.animateTo(0.8f, tween(100))
            sharedScale.animateTo(1f, tween(150))
            animatingPage = null
            onPageChange(page)
        }
    }

    fun pageScale(page: Int): Float = if (destinationScale && animatingPage == page) sharedScale.value else 1f

    Column(
        modifier
            .padding(bottom = 40.dp)
            .absolutePadding(left = 20.dp, right = 20.dp, top = 20.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            FlowRow(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .rnShadow(1.dp, RoundedCornerShape(25.dp))
                    .clip(RoundedCornerShape(25.dp))
                    .background(Color(1f, 1f, 1f, 0.95f))
                    .absolutePadding(left = 16.dp, right = 16.dp, top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.Center
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    if (currentPage > 1) {
                        ProductsNavButton("keyboard-arrow-right") { go(currentPage - 1) }
                    }
                    if (start > 1) {
                        if (destinationScale) {
                            ProductsPageButton(1, currentPage == 1, pageScale(1)) { go(1) }
                        } else {
                            AnimatedPageButton(1, currentPage == 1) { go(1) }
                        }
                        if (start > 2) Ellipsis()
                    }
                    for (page in start..end) {
                        if (destinationScale) {
                            ProductsPageButton(page, page == currentPage, pageScale(page)) { go(page) }
                        } else {
                            AnimatedPageButton(page, page == currentPage) { go(page) }
                        }
                    }
                    if (end < totalPages) {
                        if (end < totalPages - 1) Ellipsis()
                        if (destinationScale) {
                            ProductsPageButton(totalPages, currentPage == totalPages, pageScale(totalPages)) { go(totalPages) }
                        } else {
                            AnimatedPageButton(totalPages, currentPage == totalPages) { go(totalPages) }
                        }
                    }
                    if (currentPage < totalPages) {
                        ProductsNavButton("keyboard-arrow-left") { go(currentPage + 1) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedPageButton(page: Int, isActive: Boolean, onPress: () -> Unit) {
    val scale = remember(page) { Animatable(1f) }
    val scope = rememberCoroutineScope()
    ProductsPageButton(page, isActive, scale.value) {
        if (isActive) return@ProductsPageButton
        scope.launch {
            scale.animateTo(0.8f, tween(100))
            scale.animateTo(1f, tween(150))
            onPress()
        }
    }
}

@Composable
private fun Ellipsis() {
    Box(
        Modifier
            .absolutePadding(left = 4.dp, right = 4.dp)
            .size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        AppText(
            text = "...",
            fontSize = 18.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = Color(0xFF999999),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun ProductsCircleButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .offset(y = (-12).dp)
            .size(44.dp)
            .rnShadow(5.dp, CircleShape)
            .clip(CircleShape)
            .background(Color(1f, 1f, 1f, 0.9f))
            .touchableOpacity(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
