package ir.farimod.app.screens

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.navigation.useRoute
import ir.farimod.app.network.str
import ir.farimod.app.screens.portfolio.PORTFOLIO_PAGE_SIZE
import ir.farimod.app.screens.portfolio.PortfolioCircleButton
import ir.farimod.app.screens.portfolio.PortfolioEmptyState
import ir.farimod.app.screens.portfolio.PortfolioErrorState
import ir.farimod.app.screens.portfolio.PortfolioFeed
import ir.farimod.app.screens.portfolio.PortfolioGridCard
import ir.farimod.app.screens.portfolio.PortfolioGridSkeleton
import ir.farimod.app.screens.portfolio.PortfolioIndigo
import ir.farimod.app.screens.portfolio.PortfolioLoadingFooter
import ir.farimod.app.screens.portfolio.PortfolioPageBg
import ir.farimod.app.screens.portfolio.PortfolioPrimary
import ir.farimod.app.screens.portfolio.allPortfolioQuery
import ir.farimod.app.screens.portfolio.portfolioItemKey
import ir.farimod.app.screens.portfolio.portfolioPressId
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.FilterModal
import ir.farimod.app.ui.FilterOptions
import ir.farimod.app.ui.FilterSection
import ir.farimod.app.ui.MainBackground
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.ToastData
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.windowSize
import ir.farimod.app.utils.jsTruthy
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllPortfolioScreen() {
    val navigation = useNavigation()
    val route = useRoute()
    val scope = rememberCoroutineScope()
    val feed = remember { PortfolioFeed() }
    val statusTop = statusBarHeight()
    val screenW = windowSize().first
    val itemWidth = (screenW - 40.dp) / 2

    val filteredMemberId = route.params["filteredMemberId"]
    val filterType = route.str("filterType")
    val filteredMemberName = route.str("filteredMemberName") ?: "undefined"

    var filterModalVisible by remember { mutableStateOf(false) }
    var appliedFilters by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }
    var hasActiveFilters by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf(ToastData()) }
    var refreshing by remember { mutableStateOf(false) }

    fun showToast(message: String, type: String = "info") {
        toast = ToastData(true, message, type)
    }

    suspend fun fetchPortfolios(page: Int, filters: Map<String, Any?>) {
        feed.fetch(page, PORTFOLIO_PAGE_SIZE, allPortfolioQuery(page, PORTFOLIO_PAGE_SIZE, filters), filters)
    }

    LaunchedEffect(filteredMemberId, filterType) {
        if (jsTruthy(filteredMemberId) && filterType == "member") {
            val memberFilter = mapOf<String, Any?>("filterMemberId" to filteredMemberId)
            appliedFilters = memberFilter
            hasActiveFilters = true
            fetchPortfolios(1, memberFilter)
            showToast("نمایش نمونه کارهای $filteredMemberName", "info")
        } else {
            fetchPortfolios(1, emptyMap())
        }
    }

    LaunchedEffect(feed.error) {
        if (feed.error != null) {
            showToast("خطا در دریافت اطلاعات نمونه کارها. لطفاً دوباره تلاش کنید.", "error")
        }
    }

    fun handlePortfolioPress(item: JsonObject) {
        try {
            val portfolioId = portfolioPressId(item)
            if (!jsTruthy(portfolioId)) {
                showToast("خطا: شناسه نمونه کار نامعتبر است", "error")
                return
            }
            val title = item.str("Title")?.takeIf { it.isNotEmpty() } ?: "نمونه کار"
            navigation.navigate(
                "PortfolioDetail",
                mapOf("title" to title, "portfolioId" to portfolioId)
            )
        } catch (_: Exception) {
            showToast("خطا در باز کردن نمونه کار", "error")
        }
    }

    fun onRefresh() {
        scope.launch {
            refreshing = true
            fetchPortfolios(1, appliedFilters)
            refreshing = false
        }
    }

    fun handleLoadMore() {
        if (!feed.loading && feed.hasMore) {
            scope.launch { fetchPortfolios(feed.page + 1, feed.filters) }
        }
    }

    val handleApplyFilters: (Map<String, Any?>) -> Unit = { newFilters ->
        appliedFilters = newFilters
        val hasFilters = newFilters.keys.any { key ->
            val value = newFilters[key]
            value != false && value != "" && value != "all" && value != null
        }
        hasActiveFilters = hasFilters
        if (hasFilters) showToast("فیلترها اعمال شد", "success")
        scope.launch { fetchPortfolios(1, newFilters) }
    }

    fun clearAllFilters() {
        appliedFilters = emptyMap()
        hasActiveFilters = false
        scope.launch { fetchPortfolios(1, emptyMap()) }
        showToast("فیلترها پاک شد", "info")
    }

    val showingSkeleton = feed.loading && feed.items.isEmpty()
    val rows = remember(feed.items, showingSkeleton) {
        val source: List<JsonObject?> = if (showingSkeleton) List(PORTFOLIO_PAGE_SIZE) { null } else feed.items
        source.chunked(2)
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
    LaunchedEffect(nearEnd, rows.size, feed.loading) {
        if (nearEnd) handleLoadMore()
    }

    val filterOptions = remember {
        FilterOptions(
            title = "فیلتر نمونه کارها",
            icon = "brush",
            sections = listOf(
                FilterSection(
                    key = "title",
                    title = "جستجو بر اساس نام",
                    type = "text",
                    icon = "search",
                    placeholder = "نام نمونه کار را وارد کنید..."
                )
            )
        )
    }

    Box(Modifier.fillMaxSize().background(PortfolioPageBg)) {
        MainBackground()
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = statusTop + 35.dp)
                    .absolutePadding(left = 20.dp, right = 20.dp)
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AppText(
                        text = "همه نمونه کارها",
                        modifier = Modifier.fillMaxWidth().absolutePadding(left = 56.dp, right = 56.dp),
                        fontSize = 22.sp,
                        fontFamily = RNFonts.Yekan_Bakh_ExtraBold,
                        color = Color(0xFF2C3E50),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                    Box(Modifier.align(AbsoluteAlignment.CenterLeft).size(50.dp)) {
                        PortfolioCircleButton(
                            size = 50.dp,
                            background = if (hasActiveFilters) PortfolioPrimary else Color.White.copy(alpha = 0.9f),
                            onClick = { filterModalVisible = true }
                        ) {
                            MaterialIcons(
                                "filter-list",
                                size = 24.dp,
                                color = if (hasActiveFilters) Color.White else PortfolioIndigo
                            )
                        }
                        if (hasActiveFilters) {
                            Box(
                                Modifier
                                    .align(AbsoluteAlignment.TopRight)
                                    .absoluteOffset(x = (-8).dp, y = 8.dp)
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF6B6B))
                                    .border(2.dp, Color.White, CircleShape)
                            )
                        }
                    }
                    if (hasActiveFilters) {
                        PortfolioCircleButton(
                            modifier = Modifier.align(AbsoluteAlignment.CenterRight),
                            size = 40.dp,
                            onClick = { clearAllFilters() }
                        ) {
                            MaterialIcons("clear", size = 20.dp, color = Color(0xFFFF6B6B))
                        }
                    }
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (feed.error != null) {
                    PortfolioErrorState(onRetry = { scope.launch { fetchPortfolios(1, appliedFilters) } })
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
                                color = PortfolioPrimary
                            )
                        }
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues.Absolute(left = 10.dp, top = 10.dp, right = 10.dp, bottom = 20.dp)
                        ) {
                            if (!showingSkeleton && feed.items.isEmpty() && !feed.loading) {
                                item { PortfolioEmptyState() }
                            } else {
                                itemsIndexed(rows, key = { index, row ->
                                    row.firstOrNull()?.let { portfolioItemKey(it, index * 2) } ?: "skeleton-row-$index"
                                }) { _, row ->
                                    Row(
                                        Modifier.fillMaxWidth().padding(bottom = 20.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start)
                                    ) {
                                        row.forEachIndexed { col, cell ->
                                            Box(Modifier.width(itemWidth).absolutePadding(left = 5.dp, right = 5.dp)) {
                                                if (cell == null) {
                                                    PortfolioGridSkeleton(height = 320.dp, pulseOpacity = false)
                                                } else {
                                                    PortfolioGridCard(cell, showStatus = false) {
                                                        handlePortfolioPress(cell)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (feed.loading) {
                                item { PortfolioLoadingFooter() }
                            }
                        }
                    }
                }
            }
        }

        PortfolioCircleButton(
            modifier = Modifier
                .zIndex(1000f)
                .align(AbsoluteAlignment.TopRight)
                .absoluteOffset(x = (-20).dp, y = statusTop + 33.dp),
            onClick = { navigation.goBack() }
        ) {
            MaterialIcons("arrow-forward", size = 24.dp, color = PortfolioIndigo)
        }

        FilterModal(
            visible = filterModalVisible,
            onClose = { filterModalVisible = false },
            onApplyFilters = handleApplyFilters,
            filterType = "portfolio",
            initialFilters = appliedFilters,
            customFilterOptions = filterOptions
        )

        Toast(toast.visible, toast.message, toast.type) { toast = toast.copy(visible = false) }
    }
}
