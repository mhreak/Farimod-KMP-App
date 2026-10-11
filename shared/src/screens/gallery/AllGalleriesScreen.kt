package ir.farimod.app.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.material3.Text
import coil3.compose.AsyncImage
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.hooks.useToast
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.navigation.useRoute
import ir.farimod.app.network.Api
import ir.farimod.app.network.ApiConfig
import ir.farimod.app.network.double
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.list
import ir.farimod.app.network.str
import ir.farimod.app.resources.Res
import ir.farimod.app.resources.sample_clothe2
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.LocalImage
import ir.farimod.app.ui.MainBackground
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.rnTextStyle
import ir.farimod.app.ui.rnTiming
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.ui.windowSize
import ir.farimod.app.utils.jsTruthy
import ir.farimod.app.utils.toPersianDigits
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

private val GalleriesPrimary = Color(0xFF667EEA)
private val GalleriesPrimaryDark = Color(0xFF764BA2)
private const val GALLERIES_PAGE_SIZE = 20

private sealed interface AllGalleriesCell {
    data class Real(val item: JsonObject) : AllGalleriesCell
    data class Skel(val id: Int) : AllGalleriesCell
    data object Pad : AllGalleriesCell
}

/**
 * Port of `screens/gallery/AllGalleriesScreen.tsx` plus `gallery/ui` and `gallery/component`.
 *
 * The RN file registers a filter effect before `currentPage` is declared (temporal dead zone) and a second
 * effect that refetches without the member filter. This port keeps `filterMemberId` on page changes, refresh,
 * and retry whenever `filterType == "member"`, which is the filter effect's behavior.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AllGalleriesScreen() {
    val navigation = useNavigation()
    val route = useRoute()
    val toast = useToast()
    val scope = rememberCoroutineScope()
    val statusTop = statusBarHeight()
    val screenWidth = windowSize().first
    val itemWidth = (screenWidth - 60.dp) / 2

    val filteredMemberId = route.params["filteredMemberId"]
    val filteredMemberName = route.str("filteredMemberName")
    val filterType = route.str("filterType")
    val memberFilter: Any? =
        if (jsTruthy(filteredMemberId) && filterType == "member") filteredMemberId else null

    var galleries by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var totalPages by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var galleriesError by remember { mutableStateOf<String?>(null) }
    var currentPage by remember { mutableIntStateOf(1) }
    var refreshing by remember { mutableStateOf(false) }
    var requestGen by remember { mutableIntStateOf(0) }

    suspend fun fetchGalleries(page: Int, filterMemberId: Any?) {
        val gen = ++requestGen
        try {
            loading = true
            galleriesError = null
            val query = buildMap<String, Any?> {
                put("currentPage", page)
                put("pageSize", GALLERIES_PAGE_SIZE)
                if (filterMemberId != null) put("filterMemberId", filterMemberId)
            }
            val response = Api.get("ImageGallery/GetAll", query = query)
            if (gen != requestGen) return
            if (!response.ok) throw Exception("HTTP error! status: ${response.status}")
            val result = response.json ?: throw Exception("JSON")
            galleries = result.list("Items").mapNotNull { it as? JsonObject }
            totalPages = result.int("TotalPages") ?: 0
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (gen != requestGen) return
            galleriesError = e.message
            galleries = emptyList()
            totalPages = 0
        } finally {
            if (gen == requestGen) loading = false
        }
    }

    LaunchedEffect(currentPage, filteredMemberId, filterType) {
        if (memberFilter != null) {
            toast.showToast("نمایش گالری ${filteredMemberName ?: "undefined"}", "info")
        }
        fetchGalleries(currentPage, memberFilter)
    }

    LaunchedEffect(galleriesError) {
        if (galleriesError != null) {
            toast.showToast("خطا در دریافت اطلاعات گالری‌ها. لطفاً دوباره تلاش کنید.", "error")
        }
    }

    val cells: List<AllGalleriesCell> = if (loading) {
        List(GALLERIES_PAGE_SIZE) { AllGalleriesCell.Skel(it) }
    } else {
        val real = galleries.map { AllGalleriesCell.Real(it) }
        if (real.size % 2 == 1) real + AllGalleriesCell.Pad else real
    }
    val scroll = rememberScrollState()
    val pullState = rememberPullToRefreshState()

    Box(Modifier.fillMaxSize()) {
        MainBackground()

        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = statusTop + 35.dp, bottom = 20.dp)
                    .absolutePadding(left = 20.dp, right = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppText(
                    text = "گالری‌ها",
                    modifier = Modifier.fillMaxWidth().absolutePadding(left = 56.dp, right = 56.dp),
                    fontSize = 22.sp,
                    fontFamily = RNFonts.Yekan_Bakh_ExtraBold,
                    color = Color(0xFF2C3E50),
                    textAlign = TextAlign.Center
                ,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (galleriesError != null) {
                    AllGalleriesError(onRetry = {
                        scope.launch { fetchGalleries(currentPage, memberFilter) }
                    })
                } else {
                    Column(Modifier.fillMaxSize()) {
                        PullToRefreshBox(
                            isRefreshing = refreshing,
                            onRefresh = {
                                scope.launch {
                                    refreshing = true
                                    fetchGalleries(currentPage, memberFilter)
                                    refreshing = false
                                }
                            },
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            state = pullState,
                            indicator = {
                                PullToRefreshDefaults.Indicator(
                                    state = pullState,
                                    isRefreshing = refreshing,
                                    modifier = Modifier.align(Alignment.TopCenter),
                                    containerColor = Color.White,
                                    color = GalleriesPrimary
                                )
                            }
                        ) {
                            if (!loading && galleries.isEmpty()) {
                                AllGalleriesEmpty()
                            } else {
                                Column(
                                    Modifier
                                        .fillMaxSize()
                                        .verticalScroll(scroll)
                                        .padding(15.dp)
                                        .padding(bottom = 20.dp)
                                ) {
                                    cells.chunked(2).forEach { row ->
                                        RowReverse(Modifier.fillMaxWidth()) {
                                            row.forEach { cell ->
                                                Box(
                                                    Modifier
                                                        .padding(8.dp)
                                                        .width(itemWidth)
                                                        .height(200.dp)
                                                ) {
                                                    when (cell) {
                                                        is AllGalleriesCell.Skel -> AllGalleriesSkeleton()
                                                        AllGalleriesCell.Pad -> Box(Modifier.fillMaxSize())
                                                        is AllGalleriesCell.Real -> AllGalleriesCard(cell.item) {
                                                            navigation.navigate(
                                                                "GalleryItem",
                                                                mapOf(
                                                                    "title" to cell.item.str("Title"),
                                                                    "galleryId" to cell.item.int("ImageGalleryId")
                                                                )
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

                        if (!loading && totalPages > 1) {
                            AllGalleriesPagination(
                                currentPage = currentPage,
                                totalPages = totalPages,
                                onPageChange = { currentPage = it },
                                modifier = Modifier.padding(bottom = 20.dp)
                            )
                        }
                    }
                }
            }
        }

        Box(
            Modifier
                .align(AbsoluteAlignment.TopRight)
                .zIndex(1000f)
                .offset(x = (-20).dp, y = statusTop + 45.dp - 17.dp)
                .size(50.dp)
                .rnShadow(5.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
                .touchableOpacity { navigation.goBack() },
            contentAlignment = Alignment.Center
        ) {
            MaterialIcons(name = "arrow-forward", size = 26.dp, color = Color(0xFF6366F1))
        }

        Toast(toast.toastVisible, toast.toastMessage, toast.toastType) { toast.setToastVisible(false) }
    }
}

@Composable
private fun AllGalleriesCard(item: JsonObject, onPress: () -> Unit) {
    val url = item.str("FeaturedImageURL")
    val rating = item["Rating"].double
    val showRating = rating != null && rating != 0.0
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .touchableOpacity { onPress() }
    ) {
        AllGalleriesCover(url, Modifier.fillMaxSize())
        LinearGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.8f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(15.dp)
                .padding(bottom = 5.dp)
        ) {
            Text(
                text = item.str("Title") ?: "",
                modifier = Modifier.fillMaxWidth(),
                style = rnTextStyle(
                    fontSize = 16.sp,
                    color = Color.White,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp
                ).copy(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.8f),
                        offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                        blurRadius = 3f
                    )
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        RowReverse(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(10.dp)
                .zIndex(2f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            AllGalleriesChip(
                icon = "favorite",
                iconColor = Color(0xFFFF6B6B),
                text = toPersianDigits(item.int("LikeCount") ?: 0),
                textColor = Color(0xFFFF6B6B)
            )
            if (showRating) {
                AllGalleriesChip(
                    icon = "star",
                    iconColor = Color(0xFFFFD700),
                    text = toPersianDigits(rating),
                    textColor = Color(0xFFFFD700)
                )
            }
        }
    }
}

@Composable
private fun AllGalleriesChip(icon: String, iconColor: Color, text: String, textColor: Color) {
    val shape = RoundedCornerShape(15.dp)
    RowReverse(
        Modifier
            .rnShadow(4.dp, shape)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.9f))
            .absolutePadding(left = 8.dp, right = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MaterialIcons(name = icon, size = 18.dp, color = iconColor)
        AppText(
            text = text,
            modifier = Modifier.absolutePadding(right = 4.dp),
            fontSize = 12.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = textColor,
            textAlign = TextAlign.Right
        )
    }
}

@Composable
private fun AllGalleriesCover(url: String?, modifier: Modifier) {
    var failed by remember(url) { mutableStateOf(false) }
    if (url.isNullOrBlank() || failed) {
        LocalImage(Res.drawable.sample_clothe2, modifier, resizeMode = ContentScale.Crop)
    } else {
        AsyncImage(
            model = ApiConfig.resolveMediaUrl(url) ?: url,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Crop,
            onError = { failed = true }
        )
    }
}

@Composable
private fun AllGalleriesSkeleton() {
    Box(Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp))) {
        AllGalleriesBone(Modifier.fillMaxSize(), 20.dp)
        RowReverse(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(10.dp)
                .zIndex(2f),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AllGalleriesSkelChip(20.dp)
            AllGalleriesSkelChip(25.dp)
        }
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(15.dp)
                .padding(bottom = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            AllGalleriesBone(Modifier.fillMaxWidth(0.7f).height(16.dp), 8.dp)
        }
    }
}

@Composable
private fun AllGalleriesSkelChip(textWidth: Dp) {
    RowReverse(
        Modifier
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White.copy(alpha = 0.9f))
            .absolutePadding(left = 8.dp, right = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AllGalleriesBone(Modifier.size(16.dp), 8.dp)
        AllGalleriesBone(
            Modifier.absolutePadding(right = 4.dp).width(textWidth).height(12.dp),
            8.dp
        )
    }
}

@Composable
private fun AllGalleriesBone(modifier: Modifier, radius: Dp) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            anim.animateTo(1f, rnTiming(900))
            anim.animateTo(0f, rnTiming(900))
        }
    }
    val color = lerp(Color(0xFFE0E0E0), Color(0xFFF0F0F0), anim.value)
    Box(modifier.clip(RoundedCornerShape(radius)).background(color))
}

@Composable
private fun AllGalleriesEmpty() {
    Column(
        Modifier
            .fillMaxSize()
            .absolutePadding(left = 40.dp, right = 40.dp, top = 60.dp, bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MaterialIcons(name = "photo-library", size = 80.dp, color = Color(0xFF9E9E9E))
        AppText(
            text = "هیچ گالری‌ای موجود نیست",
            modifier = Modifier.padding(top = 20.dp),
            fontSize = 20.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center
        )
        AppText(
            text = "در حال حاضر گالری‌ای برای نمایش وجود ندارد",
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
private fun AllGalleriesError(onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .absolutePadding(left = 40.dp, right = 40.dp, top = 60.dp, bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MaterialIcons(name = "error", size = 80.dp, color = Color(0xFF9E9E9E))
        AppText(
            text = "خطا در دریافت اطلاعات",
            modifier = Modifier.padding(top = 20.dp),
            fontSize = 20.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = Color(0xFF2C3E50),
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
        val shape = RoundedCornerShape(25.dp)
        RowReverse(
            Modifier
                .padding(top = 24.dp)
                .rnShadow(8.dp, shape, GalleriesPrimary)
                .clip(shape)
                .background(GalleriesPrimary)
                .touchableOpacity(onClick = onRetry)
                .absolutePadding(left = 20.dp, right = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MaterialIcons(name = "refresh", size = 20.dp, color = FarimodColors.White)
            AppText(
                text = "تلاش مجدد",
                modifier = Modifier.absolutePadding(right = 8.dp),
                fontSize = 16.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = FarimodColors.White
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AllGalleriesPagination(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (totalPages <= 1) return
    val maxVisible = 5
    var start = maxOf(1, currentPage - maxVisible / 2)
    var end = minOf(totalPages, start + maxVisible - 1)
    if (end - start + 1 < maxVisible) start = maxOf(1, end - maxVisible + 1)
    end = minOf(totalPages, start + maxVisible - 1)

    Column(
        modifier
            .fillMaxWidth()
            .absolutePadding(left = 20.dp, right = 20.dp, top = 20.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val wrapShape = RoundedCornerShape(25.dp)
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            FlowRow(
                Modifier
                    .rnShadow(1.dp, wrapShape)
                    .clip(wrapShape)
                    .background(Color.White.copy(alpha = 0.95f))
                    .absolutePadding(left = 16.dp, right = 16.dp, top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.Center
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    if (currentPage > 1) {
                        AllGalleriesNavButton { onPageChange(currentPage - 1) }
                    }
                    for (page in start..end) {
                        AllGalleriesPageButton(
                            page = page,
                            active = page == currentPage,
                            onPress = onPageChange
                        )
                    }
                    if (currentPage < totalPages) {
                        AllGalleriesNavButton(next = true) { onPageChange(currentPage + 1) }
                    }
                }
            }
        }
    }
}

@Composable
private fun AllGalleriesNavButton(next: Boolean = false, onClick: () -> Unit) {
    val shape = CircleShape
    Box(
        Modifier
            .absolutePadding(left = 6.dp, right = 6.dp, top = 4.dp, bottom = 4.dp)
            .size(40.dp)
            .rnShadow(6.dp, shape, GalleriesPrimary)
            .clip(shape)
            .touchableOpacity(onClick = onClick)
    ) {
        LinearGradient(
            colors = listOf(GalleriesPrimary, GalleriesPrimaryDark),
            modifier = Modifier.fillMaxSize(),
            shape = shape
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MaterialIcons(
                    name = if (next) "keyboard-arrow-left" else "keyboard-arrow-right",
                    size = 20.dp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun AllGalleriesPageButton(page: Int, active: Boolean, onPress: (Int) -> Unit) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val shape = CircleShape
    Box(
        Modifier
            .absolutePadding(left = 4.dp, right = 4.dp, top = 4.dp, bottom = 4.dp)
            .size(40.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .then(if (active) Modifier.rnShadow(6.dp, shape, GalleriesPrimary) else Modifier)
            .clip(shape)
            .touchableOpacity(activeOpacity = 0.7f) {
                scope.launch {
                    scale.animateTo(0.8f, rnTiming(100))
                    scale.animateTo(1f, rnTiming(150))
                    onPress(page)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(shape)
                .background(if (active) GalleriesPrimary else Color.Transparent)
                .border(1.dp, if (active) GalleriesPrimary else Color(0xFFE0E0E0), shape),
            contentAlignment = Alignment.Center
        ) {
            AppText(
                text = toPersianDigits(page),
                fontSize = 16.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = if (active) Color.White else Color(0xFF666666),
                textAlign = TextAlign.Center
            )
        }
    }
}
