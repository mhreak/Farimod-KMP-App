package ir.farimod.app.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.hooks.useAutoScroll
import ir.farimod.app.navigation.useIsFocused
import kotlinx.coroutines.launch
import coil3.compose.AsyncImage
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.hooks.useHomePageSlides
import ir.farimod.app.navigation.Navigation
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.network.ApiConfig
import ir.farimod.app.network.bool
import ir.farimod.app.network.get
import ir.farimod.app.network.jsonOf
import ir.farimod.app.network.str
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.touchableOpacity
import kotlinx.serialization.json.JsonElement

@Composable
fun HomeSlides(showToast: (String, String) -> Unit) {
    val navigation = useNavigation()
    val state = useHomePageSlides()
    LaunchedEffect(state.error) {
        if (state.error != null) {
            showToast("خطا در دریافت اطلاعات اسلایدر. لطفاً دوباره تلاش کنید.", "error")
        }
    }
    when {
        state.loading -> HomeHeaderSliderSkeleton()
        state.error != null -> HomeSlideError(state.refetch)
        else -> HomeSlidePager(state.data, navigation, showToast)
    }
}

@Composable
private fun HomeSlideError(onRetry: () -> Unit) {
    Column(
        Modifier
            .padding(bottom = 12.dp)
            .absolutePadding(left = 16.dp, right = 16.dp)
            .fillMaxWidth()
            .height(200.dp)
            .background(Color(0xFFF5F5F5), RoundedCornerShape(20.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MaterialIcons(name = "error", size = 48.dp, color = Color(0xFF9E9E9E))
        AppText(
            text = "خطا در دریافت اسلایدر",
            modifier = Modifier.padding(top = 12.dp),
            fontSize = 16.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center
        )
        HomeRetryButton(onRetry)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeSlidePager(
    slides: List<JsonElement>,
    navigation: Navigation,
    showToast: (String, String) -> Unit
) {
    if (slides.isEmpty()) return
    val total = slides.size
    val pagerState = rememberPagerState { total.coerceAtLeast(1) }
    var current by remember { mutableIntStateOf(0) }
    val focused = useIsFocused()
    val scope = rememberCoroutineScope()
    useAutoScroll(
        setPage = { page ->
            if (page in 0 until pagerState.pageCount) pagerState.animateScrollToPage(page)
        },
        totalPages = if (focused) total else 0,
        intervalMs = 5000L,
        setCurrentPage = { current = it }
    )
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { current = it }
    }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            Modifier
                .padding(bottom = 12.dp)
                .fillMaxWidth()
                .height(200.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp),
                pageSpacing = 12.dp
            ) { page ->
                val slide = slides[page]
                val clickable = slide.isHomeSlideClickable()
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .then(
                            if (clickable) {
                                Modifier.touchableOpacity(activeOpacity = 0.8f) {
                                    handleHomeSlideClick(slide, navigation, showToast)
                                }
                            } else {
                                Modifier
                            }
                        )
                ) {
                    val resolved = ApiConfig.resolveMediaUrl(slide["ImageURL"].str)
                    if (resolved != null) {
                        AsyncImage(
                            model = resolved,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
            if (total > 1) {
                Box(
                    Modifier
                        .align(AbsoluteAlignment.CenterLeft)
                        .zIndex(2f)
                        .absoluteOffset(x = 24.dp)
                        .touchableOpacity(activeOpacity = 0.7f) {
                            val next = if (current < total - 1) current + 1 else 0
                            scope.launch { pagerState.animateScrollToPage(next) }
                        }
                ) { HomeSlideArrow("chevron-left") }
                Box(
                    Modifier
                        .align(AbsoluteAlignment.CenterRight)
                        .zIndex(2f)
                        .absoluteOffset(x = (-24).dp)
                        .touchableOpacity(activeOpacity = 0.7f) {
                            val prev = if (current > 0) current - 1 else total - 1
                            scope.launch { pagerState.animateScrollToPage(prev) }
                        }
                ) { HomeSlideArrow("chevron-right") }
            }
        }
    }
}

@Composable
private fun HomeSlideArrow(name: String) {
    Box(
        Modifier
            .size(32.dp)
            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        MaterialIcons(name = name, size = 26.dp, color = Color.White)
    }
}

private fun handleHomeSlideClick(
    slide: JsonElement,
    navigation: Navigation,
    showToast: (String, String) -> Unit
) {
    try {
        if (!slide["ClickTrigger"].bool) return
        val id = slide.slideEntityId()
        when (slide.slideClickTrigger()) {
            1 -> openSlideTarget(
                id = id,
                missing = "شناسه کاربر مشخص نیست",
                fallback = "خطا در باز کردن پروفایل کاربر",
                loading = "در حال بارگذاری پروفایل...",
                showToast = showToast
            ) {
                navigation.navigate(
                    "UserProfile",
                    mapOf<String, Any?>("userData" to jsonOf("MemberId" to id, "Name" to "کاربر $id"))
                )
            }
            2 -> openSlideTarget(
                id = id,
                missing = "شناسه دوره مشخص نیست",
                fallback = "خطا در باز کردن جزئیات دوره",
                loading = "در حال بارگذاری دوره...",
                showToast = showToast
            ) {
                navigation.navigate(
                    "CourseDetails",
                    mapOf<String, Any?>(
                        "courseData" to jsonOf("CourseId" to id, "CourseName" to "دوره $id"),
                        "courseId" to id
                    )
                )
            }
            3 -> openSlideTarget(
                id = id,
                missing = "شناسه محصول مشخص نیست",
                fallback = "خطا در باز کردن جزئیات محصول",
                loading = "در حال بارگذاری محصول...",
                showToast = showToast
            ) {
                navigation.navigate(
                    "ProductDetails",
                    mapOf<String, Any?>(
                        "productData" to jsonOf("ProductId" to id, "ProductName" to "محصول $id"),
                        "productId" to id
                    )
                )
            }
            4 -> openSlideTarget(
                id = id,
                missing = "شناسه گالری مشخص نیست",
                fallback = "خطا در باز کردن گالری",
                loading = "در حال بارگذاری گالری...",
                showToast = showToast
            ) {
                navigation.navigate(
                    "GalleryItem",
                    mapOf<String, Any?>("galleryId" to id, "title" to "گالری $id")
                )
            }
            5 -> openSlideTarget(
                id = id,
                missing = "شناسه نمونه کار مشخص نیست",
                fallback = "خطا در باز کردن نمونه کار",
                loading = "در حال بارگذاری نمونه کار...",
                showToast = showToast
            ) {
                navigation.navigate(
                    "PortfolioDetail",
                    mapOf<String, Any?>("portfolioId" to id, "title" to "نمونه کار $id")
                )
            }
            else -> showToast("نوع لینک نامشخص است", "warning")
        }
    } catch (_: Exception) {
        showToast("خطا در باز کردن صفحه", "error")
    }
}

private fun openSlideTarget(
    id: Int,
    missing: String,
    fallback: String,
    loading: String,
    showToast: (String, String) -> Unit,
    navigate: () -> Unit
) {
    try {
        if (id == 0) throw IllegalStateException(missing)
        showToast(loading, "info")
        navigate()
    } catch (e: Exception) {
        showToast(e.message ?: fallback, "error")
    }
}
