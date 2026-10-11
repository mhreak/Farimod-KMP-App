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
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.list
import ir.farimod.app.network.long
import ir.farimod.app.network.str
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.navigation.useRoute
import ir.farimod.app.screens.courses.CourseCardSkeleton
import ir.farimod.app.screens.courses.CoursePrimary
import ir.farimod.app.screens.courses.CoursesPagination
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.CourseCard
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
import kotlin.math.ceil

private const val COURSE_PAGE_SIZE = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllCoursesScreen() {
    val navigation = useNavigation()
    val route = useRoute()
    val scope = rememberCoroutineScope()
    val filteredMemberId = route.int("filteredMemberId") ?: route.long("filteredMemberId")?.toInt()
    val filteredMemberName = route.str("filteredMemberName")
    val filterType = route.str("filterType")

    var courses by remember { mutableStateOf<List<JsonElement>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableIntStateOf(1) }
    var filters by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }
    var ready by remember { mutableStateOf(false) }
    var initialLoading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf(ToastData()) }
    val listState = rememberLazyListState()
    val pullState = rememberPullToRefreshState()
    val top = statusBarHeight()
    val itemWidth = (windowSize().first - 70.dp) / 2
    val totalPages = if (total <= 0) 0 else ceil(total / COURSE_PAGE_SIZE.toDouble()).toInt()

    fun showToast(message: String, type: String = "info") {
        toast = ToastData(true, message, type)
    }

    suspend fun fetchCourses(targetPage: Int, active: Map<String, Any?>) {
        loading = true
        error = null
        val query = linkedMapOf<String, Any?>(
            "filterActive" to true,
            "currentPage" to targetPage,
            "pageSize" to COURSE_PAGE_SIZE
        )
        val memberId = active["filterMemberId"]
        if (memberId != null && memberId.toString().isNotEmpty()) query["filterMemberId"] = memberId
        try {
            val response = Api.get("Course/GetAll", query = query)
            if (!response.ok) throw ApiNetworkException("HTTP error! status: ${response.status}")
            courses = response.json["Data"].list
            total = response.json["Total"].int ?: 0
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message
            courses = emptyList()
            total = 0
        } finally {
            loading = false
            initialLoading = false
        }
    }

    LaunchedEffect(filteredMemberId, filterType) {
        val initial = linkedMapOf<String, Any?>()
        if (filteredMemberId != null && filterType == "member") {
            initial["filterMemberId"] = filteredMemberId
            showToast("نمایش دوره‌های ${filteredMemberName.orEmpty()}", "info")
        }
        filters = initial
        ready = true
    }

    LaunchedEffect(page, filters, ready) {
        if (ready) fetchCourses(page, filters)
    }

    LaunchedEffect(error) {
        if (error != null) showToast("خطا در دریافت اطلاعات دوره‌ها. لطفاً دوباره تلاش کنید.", "error")
    }

    val title = if (filteredMemberId != null && !filteredMemberName.isNullOrEmpty()) {
        "دوره‌های $filteredMemberName"
    } else {
        "دوره‌های آموزشی"
    }
    val showSkeleton = loading || initialLoading
    val list: List<JsonElement?> = if (showSkeleton && error == null) {
        List(COURSE_PAGE_SIZE) { null }
    } else {
        courses
    }
    val rows = list.chunked(2)
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
                    text = title,
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

            if (error != null) {
                CourseMessage(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    icon = "error",
                    title = "خطا در دریافت اطلاعات",
                    subtitle = "لطفاً اتصال اینترنت خود را بررسی کنید",
                    action = "تلاش مجدد",
                    onAction = { scope.launch { fetchCourses(page, filters) } }
                )
            } else {
                Column(Modifier.weight(1f).fillMaxWidth()) {
                    PullToRefreshBox(
                        isRefreshing = refreshing,
                        onRefresh = {
                            scope.launch {
                                refreshing = true
                                fetchCourses(page, filters)
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
                                color = CoursePrimary
                            )
                        }
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 20.dp)
                        ) {
                            if (!showSkeleton && courses.isEmpty()) {
                                item(key = "empty") {
                                    CourseMessage(
                                        modifier = Modifier.fillMaxHeight(),
                                        icon = "school",
                                        title = "هیچ دوره‌ای موجود نیست",
                                        subtitle = "در حال حاضر دوره‌ای برای نمایش وجود ندارد"
                                    )
                                }
                            }
                            items(rows.size, key = { index ->
                                val first = rows[index].firstOrNull()
                                first?.str("CourseId") ?: first?.int("CourseId")?.toString() ?: "skeleton-$index"
                            }) { index ->
                                val row = rows[index]
                                Row(
                                    Modifier.fillMaxWidth().absolutePadding(left = 8.dp, right = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    row.forEach { course ->
                                        Box(
                                            Modifier
                                                .absolutePadding(left = 5.dp, right = 5.dp, bottom = 15.dp)
                                                .width(itemWidth)
                                        ) {
                                            if (course == null) {
                                                CourseCardSkeleton()
                                            } else {
                                                CourseCard(course, onPress = {
                                                    val id = course.int("CourseId") ?: course.str("CourseId")
                                                    navigation.navigate("CourseDetails", mapOf("courseId" to id))
                                                })
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!loading && error == null && !initialLoading && totalPages > 1) {
                        CoursesPagination(page, totalPages, { page = it })
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

        Toast(toast.visible, toast.message, toast.type) { toast = toast.copy(visible = false) }
    }
}

@Composable
private fun CourseMessage(
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
                    .rnShadow(8.dp, RoundedCornerShape(25.dp), CoursePrimary)
                    .clip(RoundedCornerShape(25.dp))
                    .background(CoursePrimary)
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
