package ir.farimod.app.screens

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import ir.farimod.app.network.double
import ir.farimod.app.network.get
import ir.farimod.app.network.list
import ir.farimod.app.network.str
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.screens.membergroup.GroupPrimary
import ir.farimod.app.screens.membergroup.MemberGroupSkeleton
import ir.farimod.app.screens.membergroup.MemberGroupTile
import ir.farimod.app.screens.membergroup.groupIdOf
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.MainBackground
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.ToastData
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement

private const val GROUP_PAGE_SIZE = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllMemberGroupsScreen() {
    val navigation = useNavigation()
    val scope = rememberCoroutineScope()
    var groups by remember { mutableStateOf<List<JsonElement>>(emptyList()) }
    var page by remember { mutableStateOf(1) }
    var hasMore by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf(ToastData()) }
    val listState = rememberLazyListState()
    val pullState = rememberPullToRefreshState()
    val top = statusBarHeight()
    val busy = remember { mutableStateOf(false) }

    fun showToast(message: String, type: String = "info") {
        toast = ToastData(true, message, type)
    }

    suspend fun fetchGroups(newPage: Int, loadMore: Boolean) {
        if (busy.value) return
        busy.value = true
        loading = true
        error = null
        try {
            val response = Api.get(
                "MemberGroup/GetAll",
                query = mapOf("currentPage" to newPage, "pageSize" to GROUP_PAGE_SIZE)
            )
            if (!response.ok) throw ApiNetworkException("HTTP error! status: ${response.status}")
            val active = response.json["Data"].list.filter { group ->
                val count = group["MemberCount"].double ?: -1.0
                group["Active"].bool && count >= 0.0
            }
            groups = if (loadMore) groups + active else active
            page = newPage
            hasMore = active.size == GROUP_PAGE_SIZE && active.isNotEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "خطای نامشخص"
            if (!loadMore) groups = emptyList()
        } finally {
            busy.value = false
            loading = false
        }
    }

    LaunchedEffect(Unit) { fetchGroups(1, false) }

    LaunchedEffect(error) {
        if (error != null) showToast("خطا در دریافت اطلاعات گروه‌ها. لطفاً دوباره تلاش کنید.", "error")
    }

    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            if (total == 0) return@derivedStateOf false
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            val visible = info.visibleItemsInfo.size.coerceAtLeast(1)
            (total - 1 - last) <= visible * 0.4f
        }
    }
    LaunchedEffect(nearEnd, loading, hasMore, groups.size) {
        if (nearEnd && !busy.value && !loading && hasMore && groups.isNotEmpty()) {
            fetchGroups(page + 1, true)
        }
    }

    val skeleton = loading && groups.isEmpty() && error == null
    val rows: List<List<JsonElement?>> = if (skeleton) {
        List(9) { null as JsonElement? }.chunked(3)
    } else {
        groups.map { it as JsonElement? }.chunked(3)
    }
    val circle = RoundedCornerShape(22.dp)

    Box(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        MainBackground()
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = top + 35.dp)
                    .absolutePadding(left = 20.dp, right = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                AppText(
                    text = "گروه‌های اصلی",
                    modifier = Modifier.fillMaxWidth().absolutePadding(left = 56.dp, right = 56.dp),
                    fontSize = 22.sp,
                    fontFamily = RNFonts.Yekan_Bakh_ExtraBold,
                    color = Color(0xFF2C3E50),
                    textAlign = TextAlign.Center
                ,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(40.dp))

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (error != null) {
                    GroupMessage(
                        modifier = Modifier.fillMaxSize(),
                        icon = "error",
                        title = "خطا در دریافت اطلاعات",
                        subtitle = "لطفاً اتصال اینترنت خود را بررسی کنید",
                        action = "تلاش مجدد",
                        onAction = { scope.launch { fetchGroups(1, false) } }
                    )
                } else {
                    PullToRefreshBox(
                        isRefreshing = refreshing,
                        onRefresh = {
                            scope.launch {
                                refreshing = true
                                page = 1
                                hasMore = true
                                fetchGroups(1, false)
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
                                color = GroupPrimary
                            )
                        }
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 20.dp)
                        ) {
                            if (!skeleton && groups.isEmpty() && !loading) {
                                item(key = "empty") {
                                    GroupMessage(
                                        modifier = Modifier.fillMaxHeight(),
                                        icon = "group",
                                        title = "هیچ گروهی موجود نیست",
                                        subtitle = "در حال حاضر گروهی برای نمایش وجود ندارد"
                                    )
                                }
                            }
                            items(rows.size, key = { index -> "row-$index" }) { index ->
                                val row = rows[index]
                                RowReverse(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    row.forEach { cell ->
                                        Box(Modifier.weight(1f)) {
                                            if (cell == null) {
                                                MemberGroupSkeleton()
                                            } else {
                                                MemberGroupTile(cell) { group ->
                                                    val id = groupIdOf(group)
                                                    navigation.navigate(
                                                        "AllMembers",
                                                        mapOf(
                                                            "filterGroupId" to id,
                                                            "filterGroupName" to group.str("GroupName")
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    repeat(3 - row.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                            if (loading && groups.isNotEmpty()) {
                                item(key = "footer") {
                                    Row(
                                        Modifier.fillMaxWidth().padding(20.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = GroupPrimary,
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
        }

        Box(
            Modifier
                .align(AbsoluteAlignment.TopRight)
                .zIndex(1000f)
                .absoluteOffset(x = (-20).dp, y = top + 33.dp)
                .size(44.dp)
                .touchableOpacity { navigation.goBack() }
                .rnShadow(2.dp, circle)
                .background(Color.White.copy(alpha = 0.9f), circle),
            contentAlignment = Alignment.Center
        ) {
            MaterialIcons("arrow-forward", size = 24.dp, color = Color(0xFF6366F1))
        }

        Toast(toast.visible, toast.message, toast.type) { toast = toast.copy(visible = false) }
    }
}

@Composable
private fun GroupMessage(
    icon: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {}
) {
    Column(
        modifier.absolutePadding(left = 40.dp, right = 40.dp, top = 60.dp, bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MaterialIcons(icon, size = 80.dp, color = Color(0xFF9E9E9E))
        AppText(
            text = title,
            modifier = Modifier.padding(top = 20.dp),
            fontSize = 20.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = Color(0xFF2C3E50),
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
        if (action != null) {
            RowReverse(
                Modifier
                    .padding(top = 24.dp)
                    .rnShadow(3.dp, RoundedCornerShape(25.dp), GroupPrimary)
                    .clip(RoundedCornerShape(25.dp))
                    .background(GroupPrimary)
                    .touchableOpacity(onClick = onAction)
                    .absolutePadding(left = 20.dp, right = 20.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MaterialIcons("refresh", size = 20.dp, color = FarimodColors.White)
                AppText(
                    text = action,
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
