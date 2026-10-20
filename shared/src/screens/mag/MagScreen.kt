package ir.farimod.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.hooks.useToast
import ir.farimod.app.navigation.useFocusEffect
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.navigation.useRoute
import ir.farimod.app.network.int
import ir.farimod.app.network.str
import ir.farimod.app.screens.mag.MagBlogImage
import ir.farimod.app.screens.mag.MagBlogCategoriesState
import ir.farimod.app.screens.mag.MagBlogEventBus
import ir.farimod.app.screens.mag.MagBlogPostCardSkeleton
import ir.farimod.app.screens.mag.MagBlogPostsState
import ir.farimod.app.screens.mag.magFilterValueActive
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.FilterModal
import ir.farimod.app.ui.FilterOptions
import ir.farimod.app.ui.FilterSection
import ir.farimod.app.ui.MainBackground
import ir.farimod.app.ui.PickerOption
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.ui.windowSize
import ir.farimod.app.utils.jsToString
import ir.farimod.app.utils.jsTruthy
import ir.farimod.app.utils.toPersianDigits
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

private const val MAG_PAGE_SIZE = 20

private val MagPrimary = Color(0xFF667EEA)
private val MagTertiary = Color(0xFF4ECDC4)

private data class MagFeedRow(val key: String, val post: JsonObject?)

/**
 * Port of `screens/mag/MagScreen.tsx` (tab "وبلاگ", and the `MagScreen` route).
 * Route params (when opened for a member): filteredMemberId, filteredMemberName, filterType = "member".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MagScreen() {
    val navigation = useNavigation()
    val route = useRoute()
    val scope = rememberCoroutineScope()
    val toast = useToast()
    val posts = remember { MagBlogPostsState() }
    val categoriesState = remember { MagBlogCategoriesState() }
    val (screenWidth, _) = windowSize()
    val topInset = statusBarHeight()

    val filteredMemberId = route.params["filteredMemberId"]
    val filteredMemberName = route.str("filteredMemberName")
    val filterType = route.str("filterType")

    var filterModalVisible by remember { mutableStateOf(false) }
    var appliedFilters by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }
    var hasActiveFilters by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }

    DisposableEffect(categoriesState) {
        onDispose { categoriesState.dispose() }
    }

    useFocusEffect {
        if (jsTruthy(filteredMemberId) && filterType == "member") {
            val memberFilter = mapOf<String, Any?>("filterMemberId" to filteredMemberId)
            appliedFilters = memberFilter
            hasActiveFilters = true
            posts.fetchBlogPosts(1, MAG_PAGE_SIZE, memberFilter)
        } else {
            posts.fetchBlogPosts(1, MAG_PAGE_SIZE, appliedFilters)
        }
        categoriesState.fetchCategories()
    }

    LaunchedEffect(MagBlogEventBus.lastLikedBlogPostId, MagBlogEventBus.lastLikeCount) {
        val id = MagBlogEventBus.lastLikedBlogPostId ?: return@LaunchedEffect
        val count = MagBlogEventBus.lastLikeCount ?: return@LaunchedEffect
        val liked = MagBlogEventBus.lastLikedState
        posts.updateBlogPostLike(id, liked, count)
    }

    LaunchedEffect(filteredMemberId) {
        if (jsTruthy(filteredMemberId) && filterType == "member") {
            val memberFilter = mapOf<String, Any?>("filterMemberId" to filteredMemberId)
            appliedFilters = memberFilter
            hasActiveFilters = true
            toast.showToast("نمایش مقالات ${filteredMemberName ?: "undefined"}", "info")
        }
    }

    LaunchedEffect(posts.error) {
        if (posts.error != null) {
            toast.showToast("خطا در دریافت اطلاعات مقالات. لطفاً دوباره تلاش کنید.", "error")
        }
    }
    LaunchedEffect(categoriesState.error) {
        if (categoriesState.error != null) {
            toast.showToast("خطا در دریافت دسته‌بندی‌ها. لطفاً دوباره تلاش کنید.", "error")
        }
    }

    fun headerTitle(): String {
        return if (jsTruthy(filteredMemberId) && !filteredMemberName.isNullOrEmpty()) "مقالات" else "وبلاگ و مقالات مد"
    }

    val handleBlogPress: (JsonObject) -> Unit = { blogData ->
        navigation.navigate(
            "MagDetailes",
            mapOf(
                "title" to blogData.str("Title"),
                "blogId" to (blogData.int("BlogPostId") ?: blogData.str("BlogPostId"))
            )
        )
    }

    fun onRefresh() {
        scope.launch {
            refreshing = true
            posts.fetchBlogPosts(1, MAG_PAGE_SIZE, appliedFilters)
            refreshing = false
        }
    }

    fun handleLoadMore() {
        if (!posts.loading && posts.hasMore) {
            scope.launch { posts.loadMore() }
        }
    }

    val handleApplyFilters: (Map<String, Any?>) -> Unit = { newFilters ->
        appliedFilters = newFilters
        val hasFilters = newFilters.keys.any { key -> magFilterValueActive(newFilters[key]) }
        hasActiveFilters = hasFilters
        if (hasFilters) toast.showToast("فیلترها اعمال شد", "success")
        scope.launch { posts.fetchBlogPosts(1, MAG_PAGE_SIZE, newFilters) }
    }

    fun clearAllFilters() {
        appliedFilters = emptyMap()
        hasActiveFilters = false
        scope.launch { posts.fetchBlogPosts(1, MAG_PAGE_SIZE, emptyMap()) }
        toast.showToast("فیلترها پاک شد", "info")
    }

    val filterOptions = remember(categoriesState.categories) {
        FilterOptions(
            title = "فیلتر مقالات",
            icon = "article",
            sections = listOf(
                FilterSection(
                    title = "دسته‌بندی",
                    type = "selection",
                    key = "categoryId",
                    icon = "category",
                    options = listOf(PickerOption(label = "همه", value = "all")) +
                        categoriesState.categories.map { category ->
                            val id = category["BlogPostCategoryId"]
                            PickerOption(
                                label = category.str("Name") ?: "",
                                value = if (id == null) "" else jsToString(id)
                            )
                        }
                )
            )
        )
    }

    val skeletonRows = remember {
        List(MAG_PAGE_SIZE) { index -> MagFeedRow("skeleton-$index", null) }
    }
    val rows = if (posts.loading && posts.data.isEmpty()) {
        skeletonRows
    } else {
        posts.data.mapIndexed { index, post ->
            val id = post.str("BlogPostId")
            MagFeedRow(if (!id.isNullOrEmpty()) "$id-$index" else "blog-$index", post)
        }
    }

    val listState = rememberLazyListState()
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            if (total == 0) return@derivedStateOf false
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            val visible = info.visibleItemsInfo.size.coerceAtLeast(1)
            (total - 1 - last) <= visible * 0.3f
        }
    }
    LaunchedEffect(nearEnd, rows.size, posts.loading) {
        if (nearEnd) handleLoadMore()
    }

    val itemWidth = screenWidth - 40.dp
    val cardShape = RoundedCornerShape(20.dp)

    Box(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        MainBackground()

        FilterModal(
            visible = filterModalVisible,
            onClose = { filterModalVisible = false },
            onApplyFilters = handleApplyFilters,
            filterType = "blog",
            initialFilters = appliedFilters,
            customFilterOptions = filterOptions
        )

        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = topInset + 35.dp, bottom = 20.dp)
                    .absolutePadding(left = 20.dp, right = 20.dp)
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AppText(
                        text = headerTitle(),
                        fontSize = 22.sp,
                        fontFamily = RNFonts.Yekan_Bakh_ExtraBold,
                        color = Color(0xFF2C3E50),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().absolutePadding(left = 56.dp, right = 56.dp)
                    ,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                }
                Box(
                    Modifier
                        .align(AbsoluteAlignment.CenterLeft)
                        .size(50.dp)
                        .touchableOpacity { filterModalVisible = true }
                ) {
                    val iconShape = CircleShape
                    Box(
                        Modifier
                            .fillMaxSize()
                            .rnShadow(5.dp, iconShape)
                            .background(
                                if (hasActiveFilters) MagPrimary else Color(0xE6FFFFFF),
                                iconShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        MaterialIcons(
                            name = "filter-list",
                            size = 24.dp,
                            color = if (hasActiveFilters) Color.White else Color(0xFF6366F1)
                        )
                    }
                    if (hasActiveFilters) {
                        Box(
                            Modifier
                                .align(AbsoluteAlignment.TopRight)
                                .absoluteOffset(x = (-8).dp, y = 8.dp)
                                .size(12.dp)
                                .background(Color(0xFFFF6B6B), CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }
                }
                if (hasActiveFilters) {
                    Box(
                        Modifier
                            .align(AbsoluteAlignment.CenterRight)
                            .size(40.dp)
                            .rnShadow(5.dp, CircleShape)
                            .background(Color(0xE6FFFFFF), CircleShape)
                            .touchableOpacity { clearAllFilters() },
                        contentAlignment = Alignment.Center
                    ) {
                        MaterialIcons(name = "clear", size = 20.dp, color = Color(0xFFFF6B6B))
                    }
                }
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (posts.error != null) {
                    MagErrorState(onRetry = {
                        scope.launch { posts.fetchBlogPosts(1, MAG_PAGE_SIZE, appliedFilters) }
                    })
                } else {
                    val pullState = rememberPullToRefreshState()
                    PullToRefreshBox(
                        isRefreshing = refreshing,
                        onRefresh = { onRefresh() },
                        modifier = Modifier.fillMaxSize(),
                        state = pullState,
                        indicator = {
                            PullToRefreshDefaults.Indicator(
                                state = pullState,
                                isRefreshing = refreshing,
                                modifier = Modifier.align(Alignment.TopCenter),
                                containerColor = Color.White,
                                color = MagPrimary
                            )
                        }
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            items(rows, key = { it.key }) { row ->
                                Box(
                                    Modifier
                                        .width(itemWidth)
                                        .padding(bottom = 20.dp)
                                ) {
                                    val post = row.post
                                    if (post == null) {
                                        MagBlogPostCardSkeleton()
                                    } else {
                                        Column(
                                            Modifier
                                                .fillMaxWidth()
                                                .rnShadow(1.dp, cardShape)
                                                .clip(cardShape)
                                                .background(Color.White)
                                                .touchableOpacity(activeOpacity = 0.8f) { handleBlogPress(post) }
                                        ) {
                                            MagBlogImage(post)
                                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                                AppText(
                                                    text = toPersianDigits(post["Title"]),
                                                    fontSize = 18.sp,
                                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                                    color = Color(0xFF2C3E50),
                                                    textAlign = TextAlign.Right,
                                                    lineHeight = 26.sp,
                                                    maxLines = 3,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .absolutePadding(bottom = 12.dp)
                                                )
                                                RowReverse(
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .absolutePadding(bottom = 8.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RowReverse(verticalAlignment = Alignment.CenterVertically) {
                                                        MaterialIcons("calendar-month", size = 16.dp, color = Color(0xFF666666))
                                                        AppText(
                                                            text = toPersianDigits(post["ShamsiInsertDate"]),
                                                            fontSize = 14.sp,
                                                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                                                            color = Color(0xFF666666),
                                                            modifier = Modifier.absolutePadding(right = 6.dp)
                                                        )
                                                    }
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        val likes = post["LikeCount"]
                                                        MaterialIcons("favorite", size = 16.dp, color = Color(0xFFFF6B6B))
                                                        AppText(
                                                            text = toPersianDigits(if (jsTruthy(likes)) likes else 0),
                                                            fontSize = 14.sp,
                                                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                                                            color = Color(0xFF666666),
                                                            modifier = Modifier.absolutePadding(left = 6.dp)
                                                        )
                                                    }
                                                }
                                                val categoryName = post.str("BlogPostCategoryName")
                                                if (!categoryName.isNullOrEmpty()) {
                                                    RowReverse(
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .padding(top = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        MaterialIcons("folder", size = 16.dp, color = MagTertiary)
                                                        AppText(
                                                            text = categoryName,
                                                            fontSize = 13.sp,
                                                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                                                            color = MagTertiary,
                                                            modifier = Modifier.absolutePadding(right = 6.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (!posts.loading && rows.isEmpty()) {
                                item(key = "empty") {
                                    MagEmptyState()
                                }
                            }
                            if (posts.loading) {
                                item(key = "footer") {
                                    Row(
                                        Modifier.padding(20.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = MagPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        AppText(
                                            text = "در حال بارگذاری...",
                                            fontSize = 14.sp,
                                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                                            color = Color(0xFF666666),
                                            modifier = Modifier.absolutePadding(left = 10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // backButton: top = statusBar + 45, right = 20, inner marginTop -12
        Box(
            Modifier
                .align(AbsoluteAlignment.TopRight)
                .absoluteOffset(x = (-20).dp, y = topInset + 33.dp)
                .zIndex(1000f)
                .size(44.dp)
                .rnShadow(5.dp, CircleShape)
                .background(Color(0xE6FFFFFF), CircleShape)
                .touchableOpacity {
                    navigation.navigate("MainTabs", mapOf("screen" to "خانه"))
                },
            contentAlignment = Alignment.Center
        ) {
            MaterialIcons(name = "arrow-forward", size = 24.dp, color = Color(0xFF6366F1))
        }

        Toast(toast.toastVisible, toast.toastMessage, toast.toastType) { toast.setToastVisible(false) }
    }
}

@Composable
private fun MagEmptyState() {
    Column(
        Modifier
            .fillMaxWidth()
            .absolutePadding(left = 40.dp, right = 40.dp, top = 60.dp, bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MaterialIcons("article", size = 48.dp, color = Color(0xFF9E9E9E))
        AppText(
            text = "هیچ مقاله‌ای موجود نیست",
            fontSize = 20.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp)
        )
        AppText(
            text = "در حال حاضر مقاله‌ای برای نمایش وجود ندارد",
            fontSize = 16.sp,
            fontFamily = RNFonts.Yekan_Bakh_Regular,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun MagErrorState(onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .absolutePadding(left = 40.dp, right = 40.dp, top = 60.dp, bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MaterialIcons("error", size = 80.dp, color = Color(0xFF9E9E9E))
        AppText(
            text = "خطا در دریافت اطلاعات",
            fontSize = 20.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp)
        )
        AppText(
            text = "لطفاً اتصال اینترنت خود را بررسی کنید",
            fontSize = 16.sp,
            fontFamily = RNFonts.Yekan_Bakh_Regular,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(top = 12.dp)
        )
        val shape = RoundedCornerShape(25.dp)
        RowReverse(
            Modifier
                .padding(top = 24.dp)
                .rnShadow(8.dp, shape, MagPrimary)
                .clip(shape)
                .background(MagPrimary)
                .touchableOpacity(onClick = onRetry)
                .absolutePadding(left = 20.dp, right = 20.dp, top = 12.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MaterialIcons("refresh", size = 20.dp, color = Color.White)
            AppText(
                text = "تلاش مجدد",
                fontSize = 16.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = Color.White,
                modifier = Modifier.absolutePadding(right = 8.dp)
            )
        }
    }
}
