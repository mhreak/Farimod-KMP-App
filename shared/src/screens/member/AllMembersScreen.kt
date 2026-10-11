package ir.farimod.app.screens

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.network.Api
import ir.farimod.app.network.ApiNetworkException
import ir.farimod.app.network.bool
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.jsonOf
import ir.farimod.app.network.list
import ir.farimod.app.network.str
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.navigation.useRoute
import ir.farimod.app.screens.member.MemberPersonCard
import ir.farimod.app.screens.member.MemberPersonSkeleton
import ir.farimod.app.screens.member.MemberPrimary
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.FilterModal
import ir.farimod.app.ui.FilterOptions
import ir.farimod.app.ui.FilterSection
import ir.farimod.app.ui.MainBackground
import ir.farimod.app.ui.PickerOption
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.ToastData
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.ui.windowSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement

private const val MEMBER_PAGE_SIZE = 15
private const val MEMBER_SKELETONS = 6

private class MemberRows(
    var items: List<JsonElement> = emptyList(),
    var page: Int = 1,
    var hasMore: Boolean = true,
    var loading: Boolean = false,
    var error: String? = null,
    var filters: Map<String, Any?> = emptyMap(),
    var initialDone: Boolean = false,
    var busy: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllMembersScreen() {
    val navigation = useNavigation()
    val route = useRoute()
    val scope = rememberCoroutineScope()
    val pager = remember { MemberRows() }
    var items by remember { mutableStateOf<List<JsonElement>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var hasMore by remember { mutableStateOf(true) }
    var initialDone by remember { mutableStateOf(false) }
    var groups by remember { mutableStateOf<List<JsonElement>>(emptyList()) }
    var filterVisible by remember { mutableStateOf(false) }
    var applied by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }
    var hasActive by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf(ToastData()) }
    val listState = rememberLazyListState()
    val pullState = rememberPullToRefreshState()
    val top = statusBarHeight()
    val itemWidth = (windowSize().first - 64.dp) / 2

    fun showToast(message: String, type: String = "info") {
        toast = ToastData(true, message, type)
    }

    suspend fun fetchMembers(newPage: Int, loadMore: Boolean, filterParams: Map<String, Any?>) {
        if (pager.busy) return
        pager.busy = true
        loading = true
        error = null
        val query = linkedMapOf<String, Any?>(
            "currentPage" to newPage,
            "pageSize" to MEMBER_PAGE_SIZE
        )
        val name = filterParams["filterName"]?.toString().orEmpty()
        if (name.isNotEmpty()) query["filterName"] = name
        val groupId = filterParams["filterMemberGroupId"]?.toString().orEmpty()
        if (groupId.isNotEmpty() && groupId != "all") query["filterMemberGroupId"] = groupId
        try {
            val response = Api.get("Member/GetAll", query = query)
            if (!response.ok) throw ApiNetworkException("HTTP error! status: ${response.status}")
            val fresh = response.json["Data"].list
            items = if (loadMore) items + fresh else fresh
            pager.page = newPage
            pager.filters = filterParams
            hasMore = fresh.size >= MEMBER_PAGE_SIZE
            if (!loadMore) initialDone = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "HTTP error"
            if (!loadMore) {
                items = emptyList()
                hasMore = true
                initialDone = true
            }
        } finally {
            pager.busy = false
            loading = false
        }
    }

    val filterGroupId = route.params["filterGroupId"]
    val filterKey = when (filterGroupId) {
        null -> ""
        is Number -> {
            val d = filterGroupId.toDouble()
            if (d == 0.0) "" else if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()
        }
        else -> filterGroupId.toString().takeIf { it.isNotEmpty() && it != "0" && it != "null" } ?: ""
    }

    LaunchedEffect(Unit) {
        try {
            val response = Api.get("MemberGroup/GetAll", query = mapOf("currentPage" to 1, "pageSize" to 100))
            if (response.ok) groups = response.json["Data"].list
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(filterKey) {
        val initial = if (filterKey.isNotEmpty()) mapOf("filterMemberGroupId" to filterKey) else emptyMap()
        applied = initial
        hasActive = filterKey.isNotEmpty()
        fetchMembers(1, false, initial)
    }

    LaunchedEffect(error) {
        if (error != null) showToast("خطا در دریافت اطلاعات اعضا. لطفاً دوباره تلاش کنید.", "error")
    }

    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            if (total == 0) return@derivedStateOf false
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            val visible = info.visibleItemsInfo.size.coerceAtLeast(1)
            (total - 1 - last) <= visible * 0.5f
        }
    }
    LaunchedEffect(nearEnd, loading, hasMore, items.size, initialDone) {
        if (nearEnd && !pager.busy && hasMore && initialDone && items.isNotEmpty()) {
            fetchMembers(pager.page + 1, true, pager.filters)
        }
    }

    val filterOptions = FilterOptions(
        title = "جستجو و فیلتر اعضا",
        icon = "people",
        sections = listOf(
            FilterSection(
                key = "memberGroupId",
                options = listOf(PickerOption("همه گروه‌ها", "all")) + groups
                    .filter { it["Active"].bool }
                    .map { group ->
                        val id = group.int("MemberGroupId")?.toString()
                            ?: group.str("MemberGroupId")
                            ?: ""
                        PickerOption("${group.str("GroupName").orEmpty()} ", id)
                    }
            )
        )
    )

    val showSkeleton = loading && !initialDone
    val source: List<JsonElement> = if (showSkeleton) {
        List(MEMBER_SKELETONS) { index -> jsonOf("id" to "skeleton-$index") }
    } else if (error != null && items.isEmpty()) {
        emptyList()
    } else {
        items
    }
    val rows = source.chunked(2)
    val circle = RoundedCornerShape(22.dp)

    Box(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        MainBackground()
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = top + 42.dp)
                    .absolutePadding(left = 20.dp, right = 20.dp)
            ) {
                AppText(
                    text = "اعضای فریمد",
                    modifier = Modifier.fillMaxWidth().align(Alignment.Center).absolutePadding(left = 56.dp, right = 56.dp),
                    fontSize = 22.sp,
                    fontFamily = RNFonts.Yekan_Bakh_ExtraBold,
                    color = Color(0xFF2C3E50),
                    textAlign = TextAlign.Center
                ,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                Box(
                    Modifier
                        .align(AbsoluteAlignment.CenterLeft)
                        .size(50.dp)
                        .touchableOpacity { filterVisible = true }
                        .then(
                            if (hasActive) Modifier.background(MemberPrimary, CircleShape)
                            else Modifier.rnShadow(5.dp, CircleShape).background(Color.White.copy(alpha = 0.9f), CircleShape)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    MaterialIcons(
                        "search",
                        size = 24.dp,
                        color = if (hasActive) Color.White else Color(0xFF6366F1)
                    )
                    if (hasActive) {
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
            }

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = {
                        scope.launch {
                            refreshing = true
                            initialDone = false
                            hasMore = true
                            fetchMembers(1, false, pager.filters.ifEmpty { applied })
                            refreshing = false
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    state = pullState,
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullState,
                            isRefreshing = refreshing,
                            modifier = Modifier.align(Alignment.TopCenter),
                            containerColor = Color.White,
                            color = MemberPrimary
                        )
                    }
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 20.dp)
                    ) {
                        if (!showSkeleton && source.isEmpty() && (initialDone || !loading)) {
                            item(key = "empty") {
                                MemberEmpty(
                                    modifier = Modifier.fillMaxHeight(),
                                    failed = error != null,
                                    onRetry = {
                                        scope.launch { fetchMembers(1, false, pager.filters) }
                                    }
                                )
                            }
                        }
                        items(rows.size, key = { index ->
                            val row = rows[index]
                            val id = row.firstOrNull()?.str("id")
                            if (id?.startsWith("skeleton") == true) "row-skeleton-$index"
                            else "row-" + row.joinToString("-") { it.str("MemberId") ?: it.int("MemberId")?.toString() ?: "x" }
                        }) { index ->
                            val row = rows[index]
                            val skeletonRow = row.any { it.str("id")?.startsWith("skeleton") == true }
                            RowReverse(
                                Modifier
                                    .fillMaxWidth()
                                    .absolutePadding(left = 8.dp, right = 8.dp)
                                    .padding(bottom = 20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                row.forEach { cell ->
                                    Box(Modifier.absolutePadding(left = 8.dp, right = 8.dp).width(itemWidth)) {
                                        if (skeletonRow) MemberPersonSkeleton() else MemberPersonCard(cell) { member ->
                                            navigation.navigate("UserProfile", mapOf("userData" to member))
                                        }
                                    }
                                }
                                if (row.size == 1) {
                                    Spacer(Modifier.absolutePadding(left = 8.dp, right = 8.dp).width(itemWidth))
                                }
                            }
                        }
                        if (loading && initialDone && hasMore) {
                            item(key = "footer") {
                                Row(
                                    Modifier.fillMaxWidth().padding(20.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MemberPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    AppText(
                                        text = "در حال بارگذاری...",
                                        modifier = Modifier.absolutePadding(left = 10.dp),
                                        fontSize = 14.sp,
                                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                                        color = Color(0xFF666666),
                                        textAlign = TextAlign.Right
                                    )
                                }
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
                .absoluteOffset(x = (-20).dp, y = top + 33.dp)
                .size(44.dp)
                .touchableOpacity { navigation.goBack() }
                .rnShadow(5.dp, circle)
                .background(Color.White.copy(alpha = 0.9f), circle),
            contentAlignment = Alignment.Center
        ) {
            MaterialIcons("arrow-forward", size = 24.dp, color = Color(0xFF6366F1))
        }

        FilterModal(
            visible = filterVisible,
            onClose = { filterVisible = false },
            onApplyFilters = { next ->
                applied = next
                val active = next.values.any { value ->
                    value != false && value != "" && value != "all" && value != null
                }
                hasActive = active
                showToast(if (active) "فیلتر اعمال شد" else "فیلتر پاک شد", if (active) "success" else "info")
                scope.launch {
                    initialDone = false
                    fetchMembers(1, false, next)
                }
            },
            filterType = "members",
            initialFilters = applied,
            customFilterOptions = filterOptions
        )

        Toast(toast.visible, toast.message, toast.type) { toast = toast.copy(visible = false) }
    }
}

@Composable
private fun MemberEmpty(modifier: Modifier, failed: Boolean, onRetry: () -> Unit) {
    Column(
        modifier.absolutePadding(left = 40.dp, right = 40.dp, top = 60.dp, bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MaterialIcons("people", size = 80.dp, color = Color(0xFF9E9E9E))
        AppText(
            text = "هیچ عضوی موجود نیست",
            modifier = Modifier.padding(top = 20.dp),
            fontSize = 20.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center
        )
        AppText(
            text = if (failed) "خطا در بارگذاری اطلاعات." else "در حال حاضر عضوی برای نمایش وجود ندارد",
            modifier = Modifier.padding(top = 12.dp),
            fontSize = 16.sp,
            fontFamily = RNFonts.Yekan_Bakh_Regular,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        if (failed) {
            RowReverse(
                Modifier
                    .padding(top = 24.dp)
                    .rnShadow(8.dp, RoundedCornerShape(25.dp), MemberPrimary)
                    .clip(RoundedCornerShape(25.dp))
                    .background(MemberPrimary)
                    .touchableOpacity(onClick = onRetry)
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
}
