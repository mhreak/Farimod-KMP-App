package ir.farimod.app.screens

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.navigation.Tabs
import ir.farimod.app.navigation.useFocusEffect
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.network.Api
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.list
import ir.farimod.app.network.str
import ir.farimod.app.state.AuthState
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.CourseRegistrationCard
import ir.farimod.app.ui.MainBackground
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

private const val REG_PAGE_SIZE = 20
private val RegPurple = Color(0xFF6366F1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRegistrationsScreen() {
    val navigation = useNavigation()
    val scope = rememberCoroutineScope()
    val memberId = AuthState.user?.MemberId
    var items by remember { mutableStateOf<List<JsonElement>>(emptyList()) }
    var page by remember { mutableStateOf(1) }
    var hasMore by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf(ToastData()) }
    val listState = rememberLazyListState()
    val pullState = rememberPullToRefreshState()
    val top = statusBarHeight()
    val itemWidth = (windowSize().first - 70.dp) / 2
    val busy = remember { mutableStateOf(false) }

    fun showToast(message: String, type: String = "info") {
        toast = ToastData(true, message, type)
    }

    suspend fun fetchPage(newPage: Int) {
        if (memberId == null) {
            error = "شناسه کاربر موجود نیست"
            return
        }
        if (busy.value) return
        busy.value = true
        loading = true
        error = null
        try {
            val response = Api.get(
                "CourseRegistration/GetRegisteredCoursesOfMember",
                query = mapOf("memberId" to memberId, "currentPage" to newPage, "pageSize" to REG_PAGE_SIZE)
            )
            if (!response.ok) throw IllegalStateException("HTTP error! status: ${response.status}")
            val fresh = response.json["Data"].list
            items = if (newPage == 1) fresh else items + fresh
            page = newPage
            hasMore = fresh.size == REG_PAGE_SIZE && fresh.isNotEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message
            if (newPage == 1) items = emptyList()
        } finally {
            busy.value = false
            loading = false
        }
    }

    useFocusEffect(memberId) {
        if (memberId != null) fetchPage(1)
    }

    LaunchedEffect(error) {
        if (error != null) showToast("خطا در دریافت اطلاعات ثبت‌نام‌ها. لطفاً دوباره تلاش کنید.", "error")
    }

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
    LaunchedEffect(nearEnd, loading, hasMore, items.size) {
        if (nearEnd && !busy.value && !loading && hasMore && items.isNotEmpty()) fetchPage(page + 1)
    }

    val showSkeleton = loading && items.isEmpty() && error == null
    val rows: List<List<JsonElement?>> = if (showSkeleton) {
        List(6) { null as JsonElement? }.chunked(2)
    } else {
        items.map { it as JsonElement? }.chunked(2)
    }
    val circle = RoundedCornerShape(22.dp)

    Box(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        MainBackground()
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = top + 35.dp)
                    .absolutePadding(left = 20.dp, right = 20.dp)
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                AppText(
                    text = "دوره های ثبت نام شده",
                    modifier = Modifier.fillMaxWidth().absolutePadding(left = 56.dp, right = 56.dp).absolutePadding(left = 8.dp, right = 8.dp),
                    fontSize = 22.sp,
                    fontFamily = RNFonts.Yekan_Bakh_ExtraBold,
                    color = Color(0xFF2C3E50),
                    textAlign = TextAlign.Center
                ,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (error != null) {
                    RegMessage(
                        modifier = Modifier.fillMaxSize(),
                        icon = "error",
                        title = "خطا در دریافت اطلاعات",
                        subtitle = "لطفاً اتصال اینترنت خود را بررسی کنید",
                        action = "تلاش مجدد",
                        actionIcon = "refresh",
                        onAction = { scope.launch { fetchPage(1) } }
                    )
                } else {
                    PullToRefreshBox(
                        isRefreshing = refreshing,
                        onRefresh = {
                            scope.launch {
                                refreshing = true
                                fetchPage(1)
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
                                color = Color(0xFF667EEA)
                            )
                        }
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 20.dp)
                        ) {
                            if (!showSkeleton && items.isEmpty() && !loading) {
                                item(key = "empty") {
                                    RegMessage(
                                        modifier = Modifier.fillMaxHeight(),
                                        icon = "receipt-long",
                                        title = "ثبت‌نامی یافت نشد",
                                        subtitle = "شما هنوز در هیچ دوره‌ای ثبت‌نام نکرده‌اید",
                                        action = "مشاهده دوره‌ها",
                                        actionIcon = "search",
                                        onAction = { navigation.navigate("AllCourses") }
                                    )
                                }
                            }
                            items(rows.size, key = { index ->
                                rows[index].firstOrNull()?.str("CourseRegistrationId") ?: "skeleton-$index"
                            }) { index ->
                                Row(
                                    Modifier.fillMaxWidth().absolutePadding(left = 8.dp, right = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    rows[index].forEach { item ->
                                        Box(Modifier.absolutePadding(left = 5.dp, right = 5.dp, bottom = 15.dp).width(itemWidth)) {
                                            if (item == null) RegistrationSkeleton()
                                            else CourseRegistrationCard(item, onPress = {
                                                val id = item.int("CourseId") ?: item.str("CourseId")
                                                navigation.navigate("CourseDetails", mapOf("courseId" to id))
                                            })
                                        }
                                    }
                                }
                            }
                            if (loading) {
                                item(key = "footer") {
                                    Row(
                                        Modifier.fillMaxWidth().padding(20.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = Color(0xFF667EEA),
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
                .touchableOpacity {
                    navigation.navigate("MainTabs", mapOf("screen" to Tabs.Profile))
                }
                .rnShadow(8.dp, circle, RegPurple)
                .background(RegPurple, circle),
            contentAlignment = Alignment.Center
        ) {
            MaterialIcons("arrow-forward", size = 24.dp, color = Color.White)
        }

        Toast(toast.visible, toast.message, toast.type) { toast = toast.copy(visible = false) }
    }
}

@Composable
private fun RegistrationSkeleton() {
    val transition = rememberInfiniteTransition(label = "regSkeleton")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "regSkeletonColor"
    )
    val color = lerp(Color(0xFFE0E0E0), Color(0xFFF5F5F5), t)
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 340.dp)
            .rnShadow(4.dp, shape, Color(0xFF797979))
            .clip(shape)
            .background(FarimodColors.White)
    ) {
        Box(Modifier.fillMaxWidth().height(160.dp).background(color))
        Column(Modifier.padding(10.dp)) {
            RowReverse(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.absolutePadding(left = 6.dp).size(28.dp).clip(RoundedCornerShape(8.dp)).background(color))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Box(Modifier.padding(bottom = 4.dp).fillMaxWidth(0.9f).height(12.dp).background(color))
                    Box(Modifier.padding(bottom = 4.dp).fillMaxWidth(0.6f).height(12.dp).background(color))
                    Box(Modifier.fillMaxWidth(0.4f).height(9.dp).background(color))
                }
            }
            Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 6.dp)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF0F0F0)))
                RowReverse(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.absolutePadding(left = 4.dp).size(13.dp).clip(RoundedCornerShape(7.dp)).background(color))
                    Box(Modifier.width(50.dp).height(10.dp).background(color))
                }
                Box(Modifier.padding(top = 4.dp).align(Alignment.Start).fillMaxWidth(0.7f).height(10.dp).background(color))
            }
            Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF0F0F0)))
                repeat(3) {
                    RowReverse(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.absolutePadding(left = 4.dp).size(13.dp).clip(RoundedCornerShape(7.dp)).background(color))
                        Box(Modifier.absolutePadding(left = 4.dp).width(60.dp).height(10.dp).background(color))
                        Box(Modifier.weight(1f))
                        Box(Modifier.fillMaxWidth(0.35f).height(10.dp).background(color))
                    }
                }
            }
        }
    }
}

@Composable
private fun RegMessage(
    icon: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    actionIcon: String = "search",
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
                    .rnShadow(8.dp, RoundedCornerShape(25.dp), RegPurple)
                    .clip(RoundedCornerShape(25.dp))
                    .background(RegPurple)
                    .touchableOpacity(onClick = onAction)
                    .absolutePadding(left = 20.dp, right = 20.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MaterialIcons(actionIcon, size = 20.dp, color = FarimodColors.White)
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
