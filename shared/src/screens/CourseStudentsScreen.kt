package ir.farimod.app.screens

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.navigation.Tabs
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.navigation.useRoute
import ir.farimod.app.network.Api
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.list
import ir.farimod.app.network.str
import ir.farimod.app.screens.coursestudents.DeleteStudentSheet
import ir.farimod.app.screens.coursestudents.StudentCardSkeleton
import ir.farimod.app.screens.coursestudents.StudentDark
import ir.farimod.app.screens.coursestudents.StudentDrawer
import ir.farimod.app.screens.coursestudents.StudentMedium
import ir.farimod.app.screens.coursestudents.StudentPersonCard
import ir.farimod.app.screens.coursestudents.StudentPrimary
import ir.farimod.app.state.AuthState
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.MainBackground
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.ToastData
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.rnTiming
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement

private const val STUDENT_PAGE_SIZE = 20

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseStudentsScreen() {
    val navigation = useNavigation()
    val route = useRoute()
    val scope = rememberCoroutineScope()
    val user = AuthState.user
    val courseId = route.long("courseId")?.takeIf { it != 0L }
    var students by remember { mutableStateOf<List<JsonElement>>(emptyList()) }
    var page by remember { mutableStateOf(1) }
    var hasMore by remember { mutableStateOf(true) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<JsonElement?>(null) }
    var drawer by remember { mutableStateOf(false) }
    var deleteOpen by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf(ToastData()) }
    val fade = remember { Animatable(0f) }
    val slide = remember { Animatable(50f) }
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val pullState = rememberPullToRefreshState()
    val top = statusBarHeight()
    val headerTop = if (top > 0.dp) top + 20.dp else 45.dp
    val busy = remember { mutableStateOf(false) }

    fun showToast(message: String, type: String = "info") {
        toast = ToastData(true, message, type)
    }

    fun goHome() {
        navigation.navigate("MainTabs", mapOf("screen" to Tabs.Home))
    }

    suspend fun fetchStudents(newPage: Int, loadMore: Boolean) {
        val memberId = user?.MemberId
        if (memberId == null || courseId == null) {
            error = "اطلاعات کاربر یا دوره موجود نیست"
            return
        }
        if (busy.value) return
        busy.value = true
        loading = true
        error = null
        try {
            val response = Api.get(
                "CourseRegistration/GetRegistrationsOfCoursesOfMember",
                query = mapOf(
                    "memberId" to memberId,
                    "courseId" to courseId,
                    "currentPage" to newPage,
                    "pageSize" to STUDENT_PAGE_SIZE
                )
            )
            if (!response.ok) throw IllegalStateException("HTTP error! status: ${response.status}")
            val fresh = response.json["Data"].list
            students = if (loadMore) students + fresh else fresh
            page = newPage
            hasMore = fresh.size == STUDENT_PAGE_SIZE && fresh.isNotEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message
            if (!loadMore) students = emptyList()
        } finally {
            busy.value = false
            loading = false
        }
    }

    LaunchedEffect(courseId) {
        if (courseId == null) {
            showToast("اطلاعات دوره یافت نشد", "error")
            delay(2000)
            goHome()
            return@LaunchedEffect
        }
        fetchStudents(1, false)
        kotlinx.coroutines.coroutineScope {
            launch { fade.animateTo(1f, rnTiming(600)) }
            launch { slide.animateTo(0f, rnTiming(600)) }
        }
    }

    LaunchedEffect(error) {
        if (error != null) showToast(error ?: "", "error")
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
    LaunchedEffect(nearEnd, loading, hasMore, students.size) {
        if (nearEnd && !busy.value && !loading && hasMore && students.isNotEmpty()) {
            fetchStudents(page + 1, true)
        }
    }

    val rows = students.chunked(2)

    Box(Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        MainBackground()
        if (courseId == null) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = StudentPrimary)
                AppText(
                    text = "در حال بازگشت...",
                    modifier = Modifier.padding(top = 20.dp),
                    fontSize = 18.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = StudentDark,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = headerTop)
                        .absolutePadding(left = 20.dp, right = 20.dp)
                        .padding(bottom = 20.dp)
                        .graphicsLayer {
                            alpha = fade.value
                            translationY = slide.value * density.density
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AppText(
                        text = "دانشجویان دوره",
                        modifier = Modifier.fillMaxWidth().absolutePadding(left = 56.dp, right = 56.dp).padding(10.dp).padding(bottom = 6.dp),
                        fontSize = 22.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = StudentDark,
                        textAlign = TextAlign.Center
                    ,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                }
                if (loading && students.isEmpty()) {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp)
                    ) {
                        items(2) {
                            RowReverse(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StudentCardSkeleton()
                                StudentCardSkeleton()
                            }
                        }
                    }
                } else {
                    PullToRefreshBox(
                        isRefreshing = refreshing,
                        onRefresh = {
                            refreshing = true
                            scope.launch {
                                fetchStudents(1, false)
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
                                color = StudentPrimary
                            )
                        }
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp)
                        ) {
                            if (!loading && students.isEmpty()) {
                                item(key = "empty") {
                                    Column(
                                        Modifier.fillMaxHeight().absolutePadding(left = 40.dp, right = 40.dp).padding(top = 100.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        MaterialIcons("people-outline", size = 80.dp, color = Color(0xFFECF0F1))
                                        AppText(
                                            text = "هنوز هیچ دانشجویی ثبت‌نام نکرده",
                                            modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
                                            fontSize = 18.sp,
                                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                                            color = StudentDark,
                                            textAlign = TextAlign.Center
                                        )
                                        AppText(
                                            text = "وقتی دانشجویان در این دوره ثبت‌نام کنند، اینجا نمایش داده می‌شوند",
                                            fontSize = 15.sp,
                                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                                            color = StudentMedium,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 24.sp
                                        )
                                    }
                                }
                            }
                            items(rows.size, key = { index ->
                                rows[index].firstOrNull()?.str("CourseRegistrationId") ?: "row-$index"
                            }) { index ->
                                val row = rows[index]
                                RowReverse(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    row.forEachIndexed { cellIndex, student ->
                                        StudentPersonCard(student, index * 2 + cellIndex) {
                                            selected = it
                                            drawer = true
                                        }
                                    }
                                }
                            }
                            if (loading && hasMore) {
                                item(key = "footer") {
                                    Box(Modifier.fillMaxWidth().absolutePadding(top = 20.dp, bottom = 20.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = StudentPrimary,
                                            strokeWidth = 2.dp
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
                .zIndex(10f)
                .absoluteOffset(x = (-20).dp, y = headerTop)
                .size(44.dp)
                .touchableOpacity(activeOpacity = 0.8f) { goHome() }
                .rnShadow(8.dp, CircleShape, StudentPrimary)
                .background(Color.White.copy(alpha = 0.95f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MaterialIcons("arrow-forward", size = 24.dp, color = StudentPrimary)
        }

        StudentDrawer(
            visible = drawer,
            student = selected,
            onClose = { drawer = false },
            onDelete = { student ->
                drawer = false
                scope.launch {
                    delay(300)
                    selected = student
                    deleteOpen = true
                }
            }
        )
        DeleteStudentSheet(
            visible = deleteOpen,
            deleting = deleting,
            onCancel = { if (!deleting) deleteOpen = false },
            onConfirm = {
                val student = selected ?: return@DeleteStudentSheet
                val memberId = user?.MemberId ?: return@DeleteStudentSheet
                val registrationId = student.int("CourseRegistrationId") ?: student.str("CourseRegistrationId")
                scope.launch {
                    deleting = true
                    try {
                        val response = Api.put(
                            "CourseRegistration/DeleteCourseRegistration",
                            query = mapOf(
                                "memberId" to memberId,
                                "courseRegistrationId" to registrationId
                            )
                        )
                        if (!response.ok) throw IllegalStateException("خطا در حذف دانشجو")
                        deleteOpen = false
                        showToast("دانشجو با موفقیت حذف شد", "success")
                        launch { fetchStudents(1, false) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        showToast(e.message ?: "خطا در حذف دانشجو", "error")
                    } finally {
                        deleting = false
                        selected = null
                    }
                }
            }
        )
        Toast(toast.visible, toast.message, toast.type) { toast = toast.copy(visible = false) }
    }
}
