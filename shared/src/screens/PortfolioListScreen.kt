package ir.farimod.app.screens

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.navigation.Tabs
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.network.str
import ir.farimod.app.screens.portfolio.PORTFOLIO_PAGE_SIZE
import ir.farimod.app.screens.portfolio.PortfolioCircleButton
import ir.farimod.app.screens.portfolio.PortfolioDark
import ir.farimod.app.screens.portfolio.PortfolioEmptyState
import ir.farimod.app.screens.portfolio.PortfolioErrorState
import ir.farimod.app.screens.portfolio.PortfolioFeed
import ir.farimod.app.screens.portfolio.PortfolioGreyIcon
import ir.farimod.app.screens.portfolio.PortfolioGridCard
import ir.farimod.app.screens.portfolio.PortfolioGridSkeleton
import ir.farimod.app.screens.portfolio.PortfolioIndigo
import ir.farimod.app.screens.portfolio.PortfolioLoadingFooter
import ir.farimod.app.screens.portfolio.PortfolioPageBg
import ir.farimod.app.screens.portfolio.PortfolioPrimary
import ir.farimod.app.screens.portfolio.PortfolioSuccess
import ir.farimod.app.screens.portfolio.myPortfolioQuery
import ir.farimod.app.screens.portfolio.portfolioItemKey
import ir.farimod.app.screens.portfolio.portfolioPressId
import ir.farimod.app.state.AuthState
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.MainBackground
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.ToastData
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.ui.windowSize
import ir.farimod.app.utils.jsTruthy
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioListScreen() {
    val navigation = useNavigation()
    val user = AuthState.user
    val scope = rememberCoroutineScope()
    val feed = remember { PortfolioFeed() }
    val statusTop = statusBarHeight()
    val screenW = windowSize().first
    val itemWidth = (screenW - 40.dp) / 2
    val memberId = user?.MemberId

    var toast by remember { mutableStateOf(ToastData()) }
    var refreshing by remember { mutableStateOf(false) }

    fun showToast(message: String, type: String = "info") {
        toast = ToastData(true, message, type)
    }

    suspend fun fetchPortfolios(page: Int) {
        feed.fetch(page, PORTFOLIO_PAGE_SIZE, myPortfolioQuery(page, PORTFOLIO_PAGE_SIZE, memberId))
    }

    LaunchedEffect(memberId) {
        if (user != null) fetchPortfolios(1)
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
            fetchPortfolios(1)
            refreshing = false
        }
    }

    fun handleLoadMore() {
        if (!feed.loading && feed.hasMore) {
            scope.launch { fetchPortfolios(feed.page + 1) }
        }
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
        if (nearEnd && user != null) handleLoadMore()
    }

    if (user == null) {
        Box(Modifier.fillMaxSize().background(PortfolioPageBg)) {
            MainBackground()
            PortfolioCircleButton(
                modifier = Modifier
                    .zIndex(1000f)
                    .align(AbsoluteAlignment.TopRight)
                    .absoluteOffset(x = (-20).dp, y = statusTop + 33.dp),
                onClick = { navigation.goBack() }
            ) {
                MaterialIcons("arrow-forward", size = 24.dp, color = PortfolioIndigo)
            }
            Column(
                Modifier.fillMaxSize().absolutePadding(left = 40.dp, right = 40.dp, top = 60.dp, bottom = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                MaterialIcons("login", size = 80.dp, color = PortfolioGreyIcon)
                AppText(
                    text = "لطفا وارد حساب کاربری خود شوید",
                    fontSize = 20.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = PortfolioDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 20.dp)
                )
                AppText(
                    text = "برای مشاهده نمونه کارها باید وارد حساب کاربری شوید",
                    fontSize = 16.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    color = PortfolioGreyIcon,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
        return
    }

    Box(Modifier.fillMaxSize().background(PortfolioPageBg)) {
        MainBackground()
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = statusTop + 35.dp)
                    .absolutePadding(left = 20.dp, right = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                AppText(
                    text = "نمونه کارهای من",
                    modifier = Modifier.fillMaxWidth().absolutePadding(left = 56.dp, right = 56.dp),
                    fontSize = 22.sp,
                    fontFamily = RNFonts.Yekan_Bakh_ExtraBold,
                    color = PortfolioDark,
                    textAlign = TextAlign.Center
                ,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (feed.error != null) {
                    PortfolioErrorState(onRetry = { scope.launch { fetchPortfolios(1) } })
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
                                        row.forEach { cell ->
                                            Box(Modifier.width(itemWidth).absolutePadding(left = 5.dp, right = 5.dp)) {
                                                if (cell == null) {
                                                    PortfolioGridSkeleton(height = 280.dp, pulseOpacity = true)
                                                } else {
                                                    PortfolioGridCard(cell, showStatus = true) {
                                                        handlePortfolioPress(cell)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (feed.loading && feed.items.isNotEmpty()) {
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

        Box(
            Modifier
                .zIndex(1000f)
                .align(AbsoluteAlignment.TopLeft)
                .absoluteOffset(x = 20.dp, y = statusTop + 33.dp)
                .size(44.dp)
                .rnShadow(5.dp, CircleShape, PortfolioSuccess)
                .touchableOpacity { navigation.navigate("AddPortfolio") }
        ) {
            LinearGradient(
                colors = listOf(PortfolioSuccess, Color(0xFF27AE60)),
                modifier = Modifier.fillMaxSize(),
                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                end = androidx.compose.ui.geometry.Offset(1f, 1f),
                shape = CircleShape
            ) {
                MaterialIcons(
                    "add",
                    size = 24.dp,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Toast(toast.visible, toast.message, toast.type) { toast = toast.copy(visible = false) }
    }
}
