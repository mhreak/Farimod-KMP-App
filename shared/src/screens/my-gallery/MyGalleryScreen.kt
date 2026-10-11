package ir.farimod.app.screens

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.material3.Text
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.navigation.Tabs
import ir.farimod.app.navigation.useFocusEffect
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.network.Api
import ir.farimod.app.network.int
import ir.farimod.app.network.list
import ir.farimod.app.network.str
import ir.farimod.app.state.AuthState
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.MainBackground
import ir.farimod.app.ui.NetworkImage
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.rnTextStyle
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.ui.RNAlert
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

private const val MY_GALLERY_PAGE_SIZE = 20

/**
 * Port of `screens/my-gallery/MyGalleryScreen.tsx`.
 * Back goes to the profile tab. Add opens AddGallery. A card opens ManageGalleryItems.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyGalleryScreen() {
    val navigation = useNavigation()
    val scope = rememberCoroutineScope()
    val user = AuthState.user
    val statusTop = statusBarHeight()

    var galleries by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var currentPage by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var isLoadingMore by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val pullState = rememberPullToRefreshState()

    suspend fun fetchGalleries(page: Int, resetList: Boolean) {
        try {
            if (resetList) isLoading = true else isLoadingMore = true
            val memberId = user?.MemberId ?: 0
            if (memberId == 0) {
                isLoading = false
                isLoadingMore = false
                return
            }
            val response = Api.get(
                "ImageGallery/GetAll",
                query = mapOf(
                    "filterMemberId" to memberId,
                    "currentPage" to page,
                    "pageSize" to MY_GALLERY_PAGE_SIZE
                )
            )
            if (!response.ok) throw Exception("HTTP error! status: ${response.status}")
            val data = response.json ?: throw Exception("JSON")
            val items = data.list("Items").mapNotNull { it as? JsonObject }
            galleries = if (resetList) items else galleries + items
            currentPage = data.int("CurrentPage") ?: page
            totalPages = data.int("TotalPages") ?: 1
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            RNAlert.alert("خطا", "خطا در دریافت گالری‌ها. لطفاً دوباره تلاش کنید")
        } finally {
            isLoading = false
            isLoadingMore = false
            isRefreshing = false
        }
    }

    useFocusEffect(user?.MemberId) {
        fetchGalleries(1, true)
    }

    fun loadMore() {
        if (!isLoadingMore && !isLoading && currentPage < totalPages) {
            isLoadingMore = true
            scope.launch { fetchGalleries(currentPage + 1, false) }
        }
    }

    LaunchedEffect(scroll, currentPage, totalPages, isLoadingMore, isLoading, galleries.size) {
        snapshotFlow { scroll.value to scroll.maxValue }.collect { (value, max) ->
            if (!isLoadingMore && !isLoading && currentPage < totalPages && galleries.isNotEmpty()) {
                if (max == 0 || value >= (max * 0.5f)) loadMore()
            }
        }
    }

    if (isLoading && galleries.isEmpty()) {
        Box(Modifier.fillMaxSize()) {
            MainBackground()
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = FarimodColors.Primary)
                AppText(
                    text = "در حال بارگذاری...",
                    modifier = Modifier.padding(top = 10.dp),
                    fontSize = 16.sp,
                    color = FarimodColors.Medium
                )
            }
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        MainBackground()
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = statusTop + 35.dp, bottom = 20.dp)
                    .absolutePadding(left = 20.dp, right = 20.dp)
            ) {
                Box(
                    Modifier
                        .align(AbsoluteAlignment.CenterLeft)
                        .padding(top = 8.dp)
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                        .touchableOpacity {
                            navigation.navigate(
                                "AddGallery",
                                mapOf(
                                    "memberId" to user?.MemberId,
                                    "memberName" to ""
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    MaterialIcons(name = "add", size = 26.dp, color = Color.White)
                }
                AppText(
                    text = "گالری های من",
                    modifier = Modifier.fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(top = 40.dp)
                        .absolutePadding(left = 56.dp, right = 56.dp),
                    fontSize = 22.sp,
                    fontFamily = RNFonts.Yekan_Bakh_ExtraBold,
                    color = Color(0xFF2C3E50),
                    textAlign = TextAlign.Center
                ,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        fetchGalleries(1, true)
                    }
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = pullState,
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullState,
                        isRefreshing = isRefreshing,
                        modifier = Modifier.align(Alignment.TopCenter),
                        containerColor = Color.White,
                        color = FarimodColors.Primary
                    )
                }
            ) {
                if (galleries.isEmpty()) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .absolutePadding(top = 60.dp, bottom = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        MaterialIcons(name = "photo-library", size = 80.dp, color = Color(0xFFCCCCCC))
                        AppText(
                            text = "هنوز گالری ای ندارید",
                            modifier = Modifier.padding(top = 20.dp),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF666666),
                            textAlign = TextAlign.Center
                        )
                        AppText(
                            text = "برای افزودن گالری جدید روی دکمه + بزنید",
                            modifier = Modifier.padding(top = 8.dp),
                            fontSize = 14.sp,
                            color = Color(0xFF999999),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scroll)
                            .padding(15.dp)
                    ) {
                        galleries.chunked(2).forEach { pair ->
                            Row(Modifier.fillMaxWidth()) {
                                pair.forEach { item ->
                                    Box(Modifier.weight(1f).padding(8.dp).height(200.dp)) {
                                        MyGalleryCard(item) {
                                            navigation.navigate(
                                                "ManageGalleryItems",
                                                mapOf(
                                                    "galleryId" to item.int("ImageGalleryId"),
                                                    "galleryTitle" to item.str("Title")
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (isLoadingMore) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .absolutePadding(top = 20.dp, bottom = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = FarimodColors.Primary,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                }
            }
        }

        Box(
            Modifier
                .align(AbsoluteAlignment.TopRight)
                .zIndex(1000f)
                .offset(x = (-20).dp, y = statusTop + 80.dp - 17.dp)
                .size(50.dp)
                .rnShadow(5.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
                .touchableOpacity {
                    navigation.navigate("MainTabs", mapOf("screen" to Tabs.Profile))
                },
            contentAlignment = Alignment.Center
        ) {
            MaterialIcons(name = "arrow-forward", size = 26.dp, color = Color(0xFF6366F1))
        }
    }
}

@Composable
private fun MyGalleryCard(item: JsonObject, onPress: () -> Unit) {
    val url = item.str("FeaturedImageURL")
    Box(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(20.dp))
            .touchableOpacity { onPress() }
    ) {
        if (!url.isNullOrBlank()) {
            NetworkImage(url, Modifier.fillMaxSize())
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                MaterialIcons(name = "image", size = 60.dp, color = Color(0xFFCCCCCC))
            }
        }
        LinearGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f), Color.Black.copy(alpha = 0.8f)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(15.dp)
                .padding(bottom = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = item.str("Title") ?: "",
                style = rnTextStyle(
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 0.5.sp
                ).copy(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.8f),
                        offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                        blurRadius = 3f
                    )
                ),
                textAlign = TextAlign.Center
            )
            Row(
                Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MyGalleryStat("image", Color.White, item.int("ImageCount") ?: 0)
                MyGalleryStat("favorite", Color(0xFFFF6B6B), item.int("LikeCount") ?: 0)
            }
        }
    }
}

@Composable
private fun MyGalleryStat(icon: String, iconColor: Color, value: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MaterialIcons(name = icon, size = 14.dp, color = iconColor)
        Text(
            text = value.toString(),
            style = rnTextStyle(
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            ).copy(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.8f),
                    offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                    blurRadius = 2f
                )
            )
        )
    }
}
