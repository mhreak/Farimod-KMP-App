package ir.farimod.app.screens

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.navigation.useFocusEffect
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.navigation.useRoute
import ir.farimod.app.network.Api
import ir.farimod.app.network.ApiConfig
import ir.farimod.app.network.bool
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.list
import ir.farimod.app.network.long
import ir.farimod.app.network.str
import ir.farimod.app.network.with
import ir.farimod.app.resources.Res
import ir.farimod.app.resources.new_course
import ir.farimod.app.screens.coursedetails.CourseDetailsSkeleton
import ir.farimod.app.screens.coursedetails.CoursePalette
import ir.farimod.app.screens.coursedetails.courseIconColor
import ir.farimod.app.screens.coursedetails.courseRatingOptions
import ir.farimod.app.screens.coursedetails.courseScheduleText
import ir.farimod.app.screens.coursedetails.formatCourseType
import ir.farimod.app.state.AuthState
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.LocalImage
import ir.farimod.app.ui.MainBackground
import ir.farimod.app.ui.MultiOptionRatingComponent
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RatingOption
import ir.farimod.app.ui.ResizeMode
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.StarDisplay
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.ToastData
import ir.farimod.app.ui.pressable
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.ui.windowSize
import ir.farimod.app.utils.formatPrice
import ir.farimod.app.utils.jsNumberToString
import ir.farimod.app.utils.jsToFixed
import ir.farimod.app.utils.jsToNumber
import ir.farimod.app.utils.jsTruthy
import ir.farimod.app.utils.safeNumber
import ir.farimod.app.utils.toPersianDigits
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

private val CardShape = RoundedCornerShape(22.dp)
private val SheetShape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)

private fun heartScale(t: Float): Float = when {
    t <= 0.3f -> 0.5f + (1.8f - 0.5f) * (t / 0.3f)
    t <= 0.7f -> 1.8f + (1.5f - 1.8f) * ((t - 0.3f) / 0.4f)
    else -> 1.5f + (0.3f - 1.5f) * ((t - 0.7f) / 0.3f)
}

private fun sameMember(course: JsonObject?, memberId: Int?): Boolean {
    val courseMember = course?.get("MemberId")?.long ?: return false
    val userMember = memberId?.toLong() ?: return false
    return courseMember == userMember
}

@Composable
fun CourseDetailsScreen() {
    val navigation = useNavigation()
    val route = useRoute()
    val user = AuthState.user
    val scope = rememberCoroutineScope()
    val top = statusBarHeight()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val (screenW, screenH) = windowSize()

    val courseParam = route.json("courseData")?.get("CourseId")
    val courseId: String? = if (jsTruthy(courseParam)) courseParam.str else route.str("courseId")

    var course by remember { mutableStateOf<JsonObject?>(null) }
    var loading by remember(courseId) { mutableStateOf(courseId != null) }
    var error by remember { mutableStateOf<String?>(null) }
    var imageError by remember { mutableStateOf(false) }
    var fullScreen by remember { mutableStateOf(false) }
    var fullScreenUri by remember { mutableStateOf<String?>(null) }

    var toast by remember { mutableStateOf(ToastData()) }
    var registerState by remember { mutableStateOf<Int?>(null) }
    var likeCount by remember { mutableStateOf(0) }
    var isLiked by remember { mutableStateOf(false) }
    var isLiking by remember { mutableStateOf(false) }
    var userDetailedRatings by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var userRating by remember { mutableStateOf(0.0) }
    var ratingOptions by remember { mutableStateOf<List<RatingOption>>(emptyList()) }

    var showActions by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    val deleteSlide = remember { Animatable(300f) }
    val deleteBackdrop = remember { Animatable(0f) }
    val heartAnim = remember { Animatable(0f) }

    val imageUrl = course.str("FeaturedImageURL")
    val isOwnCourse = sameMember(course, user?.MemberId)

    fun showToast(message: String, type: String = "info") {
        toast = ToastData(visible = true, message = message, type = type)
    }

    suspend fun fetchCourse(id: String) {
        try {
            loading = true
            error = null
            val member = user?.MemberId?.toString() ?: "null"
            val response = Api.get("Course/Get?courseId=$id&currentMemberId=$member")
            if (!response.ok) throw Exception("HTTP error! status: ${response.status}")
            val result = response.json
            val raw = result?.get("Course") as? JsonObject ?: throw Exception("Course")
            val reviews = result["ContentReviewItemList"] ?: JsonArray(emptyList())
            course = raw.with(
                "IsMemberLiked" to result["IsMemberLiked"].bool,
                "ContentReviewItemList" to reviews,
                "LikeCount" to (raw["LikeCount"]?.int ?: raw["LikeCount"]?.long ?: 0),
                "AverageRating" to (raw["Rating"]?.let { jsToNumber(it) } ?: 0.0),
                "RatingCount" to 0,
                "UserRating" to 0
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            error = e.message
            course = null
            showToast("خطا در دریافت اطلاعات دوره. لطفاً دوباره تلاش کنید.", "error")
        } finally {
            loading = false
        }
    }

    suspend fun fetchRegisterState() {
        val member = user?.MemberId ?: return
        val id = courseId ?: return
        try {
            val response = Api.get("CourseRegistration/GetRegisterState?memberId=$member&courseId=$id")
            if (response.status == 404) {
                registerState = null
                return
            }
            if (!response.ok) {
                registerState = null
                return
            }
            registerState = response.json?.int
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            registerState = null
        }
    }

    LaunchedEffect(courseId) {
        if (courseId != null) fetchCourse(courseId)
    }

    LaunchedEffect(imageUrl) { imageError = false }

    LaunchedEffect(course) {
        val current = course ?: return@LaunchedEffect
        likeCount = current["LikeCount"]?.int ?: current["LikeCount"]?.long?.toInt() ?: 0
        isLiked = current["IsMemberLiked"].bool
        ratingOptions = courseRatingOptions(current["ContentReviewItemList"])
        userRating = current["UserRating"]?.let { jsToNumber(it) } ?: 0.0
    }

    useFocusEffect(course, user?.MemberId) {
        if (course != null && user?.MemberId != null) fetchRegisterState()
    }

    fun closeActions() { showActions = false }

    fun openDelete() {
        showDelete = true
        scope.launch {
            deleteSlide.snapTo(300f)
            deleteBackdrop.snapTo(0f)
            coroutineScope {
                launch { deleteBackdrop.animateTo(1f, tween(300)) }
                launch { deleteSlide.animateTo(0f, tween(300)) }
            }
        }
    }

    fun closeDelete() {
        scope.launch {
            coroutineScope {
                launch { deleteBackdrop.animateTo(0f, tween(250)) }
                launch { deleteSlide.animateTo(300f, tween(250)) }
            }
            showDelete = false
        }
    }

    fun handleDeleteCourse() {
        closeActions()
        scope.launch {
            delay(300)
            openDelete()
        }
    }

    fun playHeart(liked: Boolean) {
        scope.launch {
            heartAnim.snapTo(0f)
            heartAnim.animateTo(0.3f, tween(100))
            heartAnim.animateTo(1f, tween(300))
            if (liked) {
                delay(200)
                heartAnim.animateTo(0.8f, tween(200))
                heartAnim.animateTo(0f, tween(600))
            } else {
                heartAnim.animateTo(0f, tween(100))
            }
        }
    }

    fun handleLike() {
        val current = course ?: return
        val member = user?.MemberId ?: return
        if (isLiking) return
        val prevLiked = isLiked
        val prevCount = likeCount
        val nextLiked = !isLiked
        isLiked = nextLiked
        likeCount = prevCount + if (nextLiked) 1 else -1
        isLiking = true
        playHeart(nextLiked)
        scope.launch {
            try {
                val id = courseId ?: current.str("CourseId") ?: current.long("CourseId")?.toString()
                val response = Api.post("Course/Like?id=$id&memberId=$member")
                if (!response.ok) throw Exception("HTTP error! status: ${response.status}")
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (_: Exception) {
                isLiked = prevLiked
                likeCount = prevCount
            } finally {
                isLiking = false
            }
        }
    }

    fun confirmDelete() {
        val id = course?.long("CourseId") ?: course.str("CourseId")
        closeDelete()
        scope.launch {
            try {
                isDeleting = true
                val response = Api.delete("Course/Delete?courseId=$id")
                if (response.ok) {
                    showToast("دوره با موفقیت حذف شد", "success")
                    navigation.navigate("MyTeachingCourses")
                } else {
                    val message = response.json.str("Message")?.takeIf { it.isNotEmpty() } ?: "خطا در حذف دوره"
                    throw Exception(message)
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                showToast(e.message?.takeIf { it.isNotEmpty() } ?: "خطا در حذف دوره", "error")
            } finally {
                isDeleting = false
            }
        }
    }

    val coaches = remember(course) {
        val direct = course.list("Course_Member_List")
        if (direct.isNotEmpty()) {
            direct.map { member ->
                (member.str("MemberName") ?: member.str("Name") ?: "مربی") to member.str("ProfileImageUrl")
            }
        } else {
            course.list("Course_Member_ViewModel_List").map { member ->
                (member.str("MemberName") ?: "مربی") to null
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        MainBackground()

        when {
            loading -> CourseDetailsSkeleton()
            error != null && course == null -> CourseFallback(
                icon = "error",
                title = "خطا در دریافت اطلاعات",
                subtitle = "لطفاً اتصال اینترنت خود را بررسی کنید",
                top = top,
                onBack = { navigation.goBack() },
                retry = if (courseId != null) {
                    { scope.launch { fetchCourse(courseId) } }
                } else null
            )
            course == null -> CourseFallback(
                icon = "info",
                title = "دوره یافت نشد",
                subtitle = "اطلاعات دوره مورد نظر موجود نیست",
                top = top,
                onBack = { navigation.goBack() },
                retry = null
            )
            else -> {
                val data = course!!
                val shownUrl = if (!imageUrl.isNullOrEmpty() && !imageError) imageUrl else null
                Box(
                    Modifier
                        .zIndex(1000f)
                        .absoluteOffset(x = screenW / 2 - 25.dp, y = screenH * 0.4f)
                        .graphicsLayer {
                            val t = heartAnim.value
                            alpha = t
                            translationY = with(density) { -150.dp.toPx() } * t
                            val s = heartScale(t)
                            scaleX = s
                            scaleY = s
                            rotationZ = 15f * t
                        }
                ) {
                    MaterialIcons("favorite", size = 50.dp, color = CoursePalette.like)
                }

                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth()) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = top + 35.dp)
                                    .absolutePadding(left = 20.dp, right = 20.dp)
                                    .padding(bottom = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AppText(
                                    text = "جزئیات دوره آموزشی",
                                    modifier = Modifier.fillMaxWidth().absolutePadding(left = 56.dp, right = 56.dp),
                                    fontSize = 22.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_ExtraBold,
                                    color = Color(0xFF2C3E50),
                                    textAlign = TextAlign.Center
                                ,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                            }

                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(480.dp)
                                    .absolutePadding(left = 20.dp, right = 20.dp, top = 20.dp, bottom = 20.dp)
                                    .rnShadow(20.dp, RoundedCornerShape(30.dp), CoursePalette.primary)
                                    .clip(RoundedCornerShape(30.dp))
                            ) {
                                if (shownUrl != null) {
                                    AsyncImage(
                                        model = ApiConfig.resolveMediaUrl(shownUrl),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFFFDF0FD))
                                            .touchableOpacity(activeOpacity = 0.9f) {
                                                fullScreenUri = shownUrl
                                                fullScreen = true
                                            },
                                        contentScale = ContentScale.Fit,
                                        onError = { imageError = true }
                                    )
                                } else {
                                    LocalImage(
                                        Res.drawable.new_course,
                                        Modifier.fillMaxSize().background(Color(0xFFFDF0FD)),
                                        ResizeMode.cover
                                    )
                                }
                                val likedBg = if (isLiked) Color(0xFFE91E63).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f)
                                RowReverse(
                                    Modifier
                                        .align(AbsoluteAlignment.TopLeft)
                                        .absolutePadding(top = 15.dp, left = 15.dp)
                                        .rnShadow(4.dp, RoundedCornerShape(20.dp), Color.Black)
                                        .background(likedBg, RoundedCornerShape(20.dp))
                                        .touchableOpacity(enabled = !isLiking, activeOpacity = 0.7f) { handleLike() }
                                        .absolutePadding(left = 12.dp, right = 12.dp, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MaterialIcons(
                                        if (isLiked) "favorite" else "favorite-border",
                                        size = 18.dp,
                                        color = if (isLiked) Color.White else Color(0xFFE91E63)
                                    )
                                    AppText(
                                        text = toPersianDigits(likeCount),
                                        modifier = Modifier.absolutePadding(right = 6.dp),
                                        fontSize = 15.sp,
                                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                                        color = if (isLiked) Color.White else Color(0xFFE91E63)
                                    )
                                }
                            }

                            Column(
                                Modifier
                                    .fillMaxWidth(0.9f)
                                    .align(Alignment.CenterHorizontally)
                                    .absolutePadding(top = 12.dp, bottom = 12.dp)
                                    .heightIn(min = 120.dp)
                                    .rnShadow(5.dp, RoundedCornerShape(30.dp), Color.Black)
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(Color(0xF28C67B1))
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                val name = data.str("CourseName")
                                AppText(
                                    text = toPersianDigits(if (name.isNullOrEmpty()) "نام دوره مشخص نشده" else name),
                                    fontSize = 22.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 32.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                                Row(
                                    Modifier
                                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                                        .absolutePadding(left = 14.dp, right = 14.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MaterialIcons("school", size = 16.dp, color = Color.White)
                                    AppText(
                                        text = formatCourseType(data["CourseType"]?.int),
                                        modifier = Modifier.absolutePadding(left = 6.dp),
                                        fontSize = 14.sp,
                                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                                        color = Color.White,
                                        textAlign = TextAlign.Right
                                    )
                                }
                            }

                            RowReverse(
                                Modifier
                                    .fillMaxWidth()
                                    .absolutePadding(left = 20.dp, right = 20.dp)
                                    .padding(top = 10.dp, bottom = 30.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LinearGradient(
                                    colors = listOf(Color(0xFFE91E63), Color(0xFFAD1457)),
                                    modifier = Modifier.absolutePadding(left = 15.dp).size(50.dp).rnShadow(15.dp, CircleShape, Color.Black),
                                    shape = CircleShape
                                ) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        MaterialIcons("info", size = 26.dp, color = Color.White)
                                    }
                                }
                                AppText(
                                    text = "مشخصات دوره",
                                    fontSize = 24.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    color = Color(0xFF2C3E50)
                                )
                            }

                            if (!isOwnCourse && registerState == 2) {
                                RowReverse(
                                    Modifier
                                        .fillMaxWidth()
                                        .absolutePadding(left = 20.dp, right = 20.dp)
                                        .padding(top = 5.dp, bottom = 20.dp)
                                        .border(1.dp, Color(0xFF86EFAC), RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF0FDF4), RoundedCornerShape(12.dp))
                                        .absolutePadding(left = 20.dp, right = 20.dp, top = 12.dp, bottom = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MaterialIcons("check-circle", size = 20.dp, color = Color(0xFF10B981))
                                    Spacer(Modifier.width(8.dp))
                                    AppText(
                                        text = "شما در این دوره ثبت‌نام کرده‌اید",
                                        fontSize = 14.sp,
                                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                                        color = Color(0xFF15803D)
                                    )
                                }
                            }

                            Column(Modifier.absolutePadding(left = 20.dp, right = 20.dp)) {
                                SmartDetail(label = "درباره این دوره", value = data.str("Description"), icon = "description", maxLength = 50)
                                SmartDetail(label = "آدرس", value = data.str("CourseAddress"), icon = "place", maxLength = 25)
                                if (coaches.isNotEmpty()) {
                                    CoachesDetail(coaches, (screenW - 60.dp) / 2.5f)
                                }
                                val amount = data["RegisterAmount"]
                                val amountNum = safeNumber(amount)
                                SmartDetail(
                                    label = "هزینه ثبت نام",
                                    value = if (amountNum <= 0.0) "رایگان" else formatPrice(amount),
                                    icon = "attach-money",
                                    maxLength = 30
                                )
                                ExtraInfo(data)
                                RatingBlock(
                                    data = data,
                                    options = ratingOptions,
                                    userRating = userRating,
                                    userDetailedRatings = userDetailedRatings,
                                    onRatings = { userDetailedRatings = it },
                                    onSubmittedAverage = { avg, ratings ->
                                        userRating = avg
                                        if (ratings != null) userDetailedRatings = ratings
                                        course = data.with("UserRating" to avg)
                                    },
                                    onToast = { msg, type -> showToast(msg, type) }
                                )
                            }

                            if (!isOwnCourse && data["RegisterActive"].bool && registerState != 2) {
                                Box(Modifier.fillMaxWidth().absolutePadding(left = 20.dp, right = 20.dp), contentAlignment = Alignment.Center) {
                                    Box(
                                        Modifier
                                            .fillMaxWidth(0.92f)
                                            .padding(top = 15.dp, bottom = 18.dp)
                                            .rnShadow(20.dp, RoundedCornerShape(30.dp), Color(0xFFE91E63))
                                            .clip(RoundedCornerShape(30.dp))
                                            .touchableOpacity(activeOpacity = 0.8f) {
                                                val price = data["Price"]
                                                navigation.navigate(
                                                    "CourseRegistration",
                                                    mapOf(
                                                        "courseId" to (data.long("CourseId") ?: data.str("CourseId")),
                                                        "courseName" to data.str("Name"),
                                                        "coursePrice" to (if (jsTruthy(price)) (price?.let { jsToNumber(it) }) else 0)
                                                    )
                                                )
                                            }
                                    ) {
                                        LinearGradient(
                                            colors = listOf(Color(0xFFE91E63), Color(0xFFAD1457), Color(0xFF880E4F)),
                                            modifier = Modifier.fillMaxWidth(),
                                            start = Offset(0f, 0f),
                                            end = Offset(1f, 0f)
                                        ) {
                                            Row(
                                                Modifier.fillMaxWidth().absolutePadding(left = 35.dp, right = 35.dp, top = 18.dp, bottom = 18.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                MaterialIcons("book", size = 22.dp, color = Color.White)
                                                AppText(
                                                    text = "ثبت نام در دوره",
                                                    modifier = Modifier.absolutePadding(left = 12.dp),
                                                    fontSize = 19.sp,
                                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                                    color = Color.White,
                                                    textAlign = TextAlign.Right
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(30.dp))
                        }

                        CourseRoundButton(
                            Modifier.align(AbsoluteAlignment.TopRight).zIndex(1000f).absoluteOffset(x = (-20).dp, y = top + 33.dp)
                        ) { navigation.goBack() }
                        if (isOwnCourse) {
                            CourseRoundButton(
                                Modifier.align(AbsoluteAlignment.TopLeft).zIndex(1000f).absoluteOffset(x = 20.dp, y = top + 33.dp),
                                icon = "more-vert",
                                enabled = !isDeleting
                            ) { showActions = true }
                        }
                    }
                }

                Toast(toast.visible, toast.message, toast.type) { toast = toast.copy(visible = false) }
            }
        }

        if (showActions && course != null) {
            ActionSheet(
                onClose = { closeActions() },
                onEdit = {
                    closeActions()
                    scope.launch {
                        delay(300)
                        navigation.navigate(
                            "AddNewCourse",
                            mapOf("courseId" to (course.long("CourseId") ?: course.str("CourseId")))
                        )
                    }
                },
                onStudents = {
                    closeActions()
                    scope.launch {
                        delay(300)
                        navigation.navigate(
                            "CourseStudents",
                            mapOf(
                                "courseId" to (course.long("CourseId") ?: course.str("CourseId")),
                                "courseName" to course.str("Name")
                            )
                        )
                    }
                }
            )
        }

        if (showDelete) {
            DeleteSheet(
                slide = deleteSlide.value,
                backdrop = deleteBackdrop.value,
                deleting = isDeleting,
                onClose = { closeDelete() },
                onConfirm = { confirmDelete() }
            )
        }

        if (fullScreen) {
            Dialog(
                onDismissRequest = { fullScreen = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
                    if (fullScreenUri != null) {
                        AsyncImage(
                            model = ApiConfig.resolveMediaUrl(fullScreenUri),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        LocalImage(Res.drawable.new_course, Modifier.fillMaxSize(), ResizeMode.cover)
                    }
                    Box(
                        Modifier
                            .align(AbsoluteAlignment.TopRight)
                            .absoluteOffset(x = (-20).dp, y = 50.dp)
                            .zIndex(1f)
                            .clip(RoundedCornerShape(25.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .touchableOpacity { fullScreen = false }
                            .padding(10.dp)
                    ) {
                        MaterialIcons("close", size = 30.dp, color = Color.White)
                    }
                }
            }
        }
    }

    // RN defines handleDeleteCourse, but no control in the rendered tree calls it.
    @Suppress("UNUSED_VARIABLE")
    val unusedDeleteEntry: () -> Unit = { handleDeleteCourse() }
}

@Composable
private fun CourseRoundButton(
    modifier: Modifier,
    icon: String = "arrow-forward",
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier
            .size(44.dp)
            .rnShadow(5.dp, CircleShape, Color.Black)
            .background(Color.White.copy(alpha = 0.9f), CircleShape)
            .touchableOpacity(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        MaterialIcons(icon, size = 24.dp, color = Color(0xFF6366F1))
    }
}

@Composable
private fun CourseFallback(
    icon: String,
    title: String,
    subtitle: String,
    top: androidx.compose.ui.unit.Dp,
    onBack: () -> Unit,
    retry: (() -> Unit)?
) {
    Box(Modifier.fillMaxSize()) {
        CourseRoundButton(
            Modifier.align(AbsoluteAlignment.TopRight).zIndex(1000f).absoluteOffset(x = (-20).dp, y = top + 33.dp),
            onClick = onBack
        )
        Column(
            Modifier.fillMaxSize().absolutePadding(left = 40.dp, right = 40.dp, top = 60.dp, bottom = 60.dp),
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
            if (retry != null) {
                RowReverse(
                    Modifier
                        .padding(top = 24.dp)
                        .rnShadow(8.dp, RoundedCornerShape(25.dp), CoursePalette.primary)
                        .background(CoursePalette.primary, RoundedCornerShape(25.dp))
                        .touchableOpacity(onClick = retry)
                        .absolutePadding(left = 20.dp, right = 20.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MaterialIcons("refresh", size = 20.dp, color = FarimodColors.White)
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
    }
}

@Composable
private fun DetailCard(accent: Color, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .absolutePadding(left = 5.dp, right = 5.dp)
            .padding(bottom = 14.dp)
            .clip(CardShape)
            .background(Color(0xFFF8FAFC).copy(alpha = 0.3f))
            .border(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.4f), CardShape)
    ) {
        Box(Modifier.fillMaxWidth()) { content() }
        Box(Modifier.matchParentSize()) {
            Box(
                Modifier
                    .align(AbsoluteAlignment.CenterRight)
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(accent.copy(alpha = 0x60 / 255f))
            )
        }
    }
}

@Composable
private fun IconBubble(icon: String, color: Color) {
    Box(Modifier.absolutePadding(left = 12.dp)) {
        LinearGradient(
            colors = listOf(color, color.copy(alpha = 0xCC / 255f)),
            modifier = Modifier.size(44.dp),
            shape = CircleShape
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MaterialIcons(icon, size = 22.dp, color = Color.White)
            }
        }
    }
}

@Composable
private fun SmartDetail(label: String, value: String?, icon: String, maxLength: Int) {
    if (value.isNullOrEmpty() || value == "نامشخص" || value == "تاریخ مشخص نشده") return
    val color = courseIconColor(icon)
    val long = value.length > maxLength
    DetailCard(color) {
        Column(Modifier.padding(18.dp)) {
            if (long) {
                RowReverse(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(icon, color)
                    AppText(
                        text = label,
                        fontSize = 17.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color(0xFF2C3E50)
                    )
                }
                AppText(
                    text = value,
                    modifier = Modifier.absolutePadding(left = 15.dp, right = 15.dp).padding(top = 15.dp),
                    fontSize = 16.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    color = Color(0xFF374151),
                    textAlign = TextAlign.Right,
                    lineHeight = 26.sp
                )
            } else {
                RowReverse(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RowReverse(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        IconBubble(icon, color)
                        AppText(
                            text = label,
                            fontSize = 17.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            color = Color(0xFF2C3E50)
                        )
                    }
                    AppText(
                        text = value,
                        modifier = Modifier.weight(1f).absolutePadding(left = 15.dp),
                        fontSize = 16.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        color = Color(0xFF374151),
                        textAlign = TextAlign.Right,
                        lineHeight = 26.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CoachesDetail(coaches: List<Pair<String, String?>>, cardWidth: androidx.compose.ui.unit.Dp) {
    val color = courseIconColor("group")
    DetailCard(color) {
        Column(Modifier.padding(18.dp)) {
            RowReverse(verticalAlignment = Alignment.CenterVertically) {
                IconBubble("group", color)
                AppText(
                    text = "مربیان",
                    fontSize = 17.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color(0xFF2C3E50)
                )
            }
            RowReverse(
                Modifier
                    .fillMaxWidth()
                    .absolutePadding(top = 15.dp, bottom = 15.dp)
                    .horizontalScroll(rememberScrollState())
                    .absolutePadding(left = 20.dp, right = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                coaches.forEachIndexed { index, coach ->
                    Column(
                        Modifier
                            .width(cardWidth)
                            .heightIn(min = 160.dp)
                            .border(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.4f), RoundedCornerShape(25.dp))
                            .background(Color(0xFFF8FAFC).copy(alpha = 0.35f), RoundedCornerShape(25.dp))
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .border(3.dp, Color.White, CircleShape)
                        ) {
                            if (!coach.second.isNullOrEmpty()) {
                                AsyncImage(
                                    model = ApiConfig.resolveMediaUrl(coach.second),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                val base = if (index % 2 == 0) CoursePalette.secondary else CoursePalette.tertiary
                                LinearGradient(
                                    colors = listOf(base, base.copy(alpha = 0xAA / 255f)),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        MaterialIcons("person", size = 40.dp, color = Color.White)
                                    }
                                }
                            }
                        }
                        AppText(
                            text = coach.first,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                            fontSize = 13.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            color = Color(0xFF2C3E50),
                            textAlign = TextAlign.Center
                        )
                        Box(
                            Modifier
                                .background(CoursePalette.primary.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                .absolutePadding(left = 10.dp, right = 10.dp, top = 4.dp, bottom = 4.dp)
                        ) {
                            AppText(
                                text = "مربی متخصص",
                                fontSize = 11.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Regular,
                                color = Color(0xFF374151),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExtraInfo(data: JsonObject) {
    val color = courseIconColor("info")
    val start = data.str("ShamsiStartDate")
    val finish = data.str("ShamsiFinishDate")
    val regStart = data.str("ShamsiRegisterStartDate")
    val regFinish = data.str("ShamsiRegisterFinishDate")
    val registerActive = data["RegisterActive"].bool
    DetailCard(color) {
        Column(Modifier.padding(18.dp)) {
            RowReverse(verticalAlignment = Alignment.CenterVertically) {
                IconBubble("info", color)
                AppText(
                    text = "اطلاعات تکمیلی",
                    fontSize = 17.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color(0xFF2C3E50)
                )
            }
            Column(Modifier.absolutePadding(left = 15.dp, right = 15.dp, top = 15.dp, bottom = 15.dp)) {
                InfoRow("event", CoursePalette.primary, "مدت دوره", if (!start.isNullOrEmpty() && !finish.isNullOrEmpty()) {
                    "${toPersianDigits(start)} تا ${toPersianDigits(finish)}"
                } else "نامشخص", Color(0xFF6B7280))
                InfoRow(
                    "how-to-reg",
                    CoursePalette.success,
                    "وضعیت ثبت‌نام",
                    if (registerActive) "فعال" else "غیرفعال",
                    if (registerActive) CoursePalette.success else CoursePalette.error
                )
                if (registerActive && !regStart.isNullOrEmpty() && !regFinish.isNullOrEmpty()) {
                    InfoRow(
                        "schedule",
                        CoursePalette.warning,
                        "مهلت ثبت‌نام",
                        "${toPersianDigits(regStart)} تا ${toPersianDigits(regFinish)}",
                        Color(0xFF6B7280)
                    )
                }
                InfoRow("schedule", CoursePalette.info, "برنامه زمانی", courseScheduleText(data), Color(0xFF6B7280))
            }
        }
    }
}

@Composable
private fun InfoRow(icon: String, iconColor: Color, label: String, value: String, valueColor: Color) {
    RowReverse(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .border(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        MaterialIcons(icon, size = 20.dp, color = iconColor)
        Column(Modifier.weight(1f).absolutePadding(right = 12.dp)) {
            AppText(
                text = label,
                modifier = Modifier.padding(bottom = 4.dp),
                fontSize = 14.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = Color(0xFF374151),
                textAlign = TextAlign.Right
            )
            AppText(
                text = value,
                fontSize = 13.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = valueColor,
                textAlign = TextAlign.Right,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun RatingBlock(
    data: JsonObject,
    options: List<RatingOption>,
    userRating: Double,
    userDetailedRatings: Map<String, Double>,
    onRatings: (Map<String, Double>) -> Unit,
    onSubmittedAverage: (Double, Map<String, Double>?) -> Unit,
    onToast: (String, String) -> Unit
) {
    DetailCard(CoursePalette.gold) {
        Column(Modifier.padding(18.dp)) {
            RowReverse(verticalAlignment = Alignment.CenterVertically) {
                IconBubble("star", CoursePalette.gold)
                AppText(
                    text = "امتیاز دوره",
                    fontSize = 17.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color(0xFF2C3E50)
                )
            }
            Column(
                Modifier
                    .padding(top = 15.dp)
                    .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.3f), RoundedCornerShape(15.dp))
                    .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(15.dp))
                    .padding(15.dp),
                horizontalAlignment = Alignment.Start
            ) {
                MultiOptionRatingComponent(
                    contentId = data.long("CourseId") ?: data.str("CourseId"),
                    averageRating = data["AverageRating"]?.let { jsToNumber(it) } ?: 0.0,
                    ratingCount = data["RatingCount"]?.int ?: 0,
                    initialRating = userRating,
                    initialDetailedRatings = userDetailedRatings,
                    maxStars = 5,
                    size = 24.dp,
                    starColor = CoursePalette.gold,
                    enableMultipleOptions = true,
                    ratingOptions = options,
                    modalTitle = "امتیازدهی دوره آموزشی",
                    submitButtonText = "ثبت امتیاز",
                    cancelButtonText = "لغو",
                    showRatingCount = true,
                    showRatingText = true,
                    animated = true,
                    allowHalfStars = false,
                    modifier = Modifier.align(Alignment.Start),
                    onRatingSubmitted = { result ->
                        if (result.ratings != null) {
                            val avg = result.averageRating ?: 0.0
                            onSubmittedAverage(avg, result.ratings)
                            onToast("امتیاز شما در ${result.ratings.size} بخش با موفقیت ثبت شد", "success")
                        } else {
                            val rating = result.rating ?: 0.0
                            onSubmittedAverage(rating, null)
                            onToast("امتیاز ${toPersianDigits(jsNumberToString(rating))} ستاره ثبت شد", "success")
                        }
                    },
                    onRatingError = { message -> onToast(message.ifEmpty { "خطا در ثبت امتیاز" }, "error") },
                    onRatingChange = { _, detailed ->
                        if (detailed != null) onRatings(detailed)
                    }
                )

                if (options.isNotEmpty() && userDetailedRatings.isNotEmpty()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp)
                            .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(15.dp))
                            .background(Color(0xFFE8F5E8), RoundedCornerShape(15.dp))
                            .padding(15.dp)
                    ) {
                        AppText(
                            text = "امتیازات شما:",
                            modifier = Modifier.padding(bottom = 15.dp),
                            fontSize = 16.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            color = Color(0xFF2C3E50),
                            textAlign = TextAlign.Right
                        )
                        options.filter { (userDetailedRatings[it.id] ?: 0.0) != 0.0 }.forEach { option ->
                            val score = userDetailedRatings[option.id] ?: 0.0
                            UserScoreRow(option.title, score, user = true)
                        }
                    }
                }

                if (options.any { it.averageRating != 0.0 }) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 15.dp)
                            .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFF8E1).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        AppText(
                            text = "میانگین امتیاز سایر کاربران:",
                            modifier = Modifier.padding(bottom = 10.dp),
                            fontSize = 14.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            color = Color(0xFF2C3E50),
                            textAlign = TextAlign.Right
                        )
                        options.filter { it.averageRating != 0.0 }.forEach { option ->
                            UserScoreRow(option.title, option.averageRating, user = false, half = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserScoreRow(title: String, score: Double, user: Boolean, half: Boolean = false) {
    RowReverse(
        Modifier
            .fillMaxWidth()
            .padding(bottom = if (user) 10.dp else 8.dp)
            .border(
                1.dp,
                if (user) Color(0xFFE8F5E8) else Color(0xFFFFD700).copy(alpha = 0.1f),
                RoundedCornerShape(if (user) 12.dp else 8.dp)
            )
            .background(if (user) Color.White else Color.White.copy(alpha = 0.7f), RoundedCornerShape(if (user) 12.dp else 8.dp))
            .padding(if (user) 12.dp else 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppText(
            text = title,
            modifier = Modifier.weight(1f),
            fontSize = if (user) 14.sp else 13.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Right
        )
        RowReverse(
            Modifier.absolutePadding(left = if (user) 15.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StarDisplay(
                rating = score,
                maxStars = 5,
                size = 16.dp,
                color = CoursePalette.gold,
                emptyColor = Color(0xFFE0E0E0),
                showHalfStars = half,
                animated = false
            )
            AppText(
                text = toPersianDigits(if (half) jsToFixed(score, 1) else jsNumberToString(score)),
                fontSize = 14.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = if (user) CoursePalette.success else Color(0xFF666666)
            )
        }
    }
}

@Composable
private fun ActionSheet(onClose: () -> Unit, onEdit: () -> Unit, onStudents: () -> Unit) {
    Box(Modifier.fillMaxSize().zIndex(2000f)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).pressable { onClose() })
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .rnShadow(10.dp, SheetShape, Color.Black)
                .background(Color.White, SheetShape)
                .pressable { }
                .padding(top = 15.dp, bottom = 35.dp)
                .absolutePadding(left = 20.dp, right = 20.dp)
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 20.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color(0xFFE0E0E0), RoundedCornerShape(2.dp))
            )
            AppText(
                text = "عملیات دوره",
                modifier = Modifier.fillMaxWidth().padding(bottom = 25.dp),
                fontSize = 18.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = Color(0xFF2C3E50),
                textAlign = TextAlign.Center
            )
            ActionRow(CoursePalette.info, "edit", "ویرایش دوره", "ویرایش مشخصات و تنظیمات دوره", onEdit)
            ActionRow(Color(0xFF45B7D1), "people", "دانشجویان دوره", "مشاهده لیست ثبت‌نام‌شدگان", onStudents)
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .border(1.dp, Color(0xFFE9ECEF), RoundedCornerShape(15.dp))
                    .background(Color(0xFFF8F9FA), RoundedCornerShape(15.dp))
                    .touchableOpacity { onClose() }
                    .absolutePadding(top = 15.dp, bottom = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                AppText(
                    text = "لغو",
                    fontSize = 16.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color(0xFF6C757D),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ActionRow(color: Color, icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    RowReverse(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .border(1.dp, Color(0xFFE9ECEF), RoundedCornerShape(15.dp))
            .background(Color(0xFFF8F9FA), RoundedCornerShape(15.dp))
            .touchableOpacity { onClick() }
            .absolutePadding(left = 10.dp, right = 10.dp, top = 15.dp, bottom = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .absolutePadding(left = 15.dp)
                .size(45.dp)
                .background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MaterialIcons(icon, size = 22.dp, color = Color.White)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            AppText(
                text = title,
                modifier = Modifier.padding(bottom = 2.dp),
                fontSize = 16.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = Color(0xFF2C3E50),
                textAlign = TextAlign.Right
            )
            AppText(
                text = subtitle,
                fontSize = 13.sp,
                fontFamily = RNFonts.Yekan_Bakh_Regular,
                color = Color(0xFF6C757D),
                textAlign = TextAlign.Right
            )
        }
    }
}

@Composable
private fun DeleteSheet(
    slide: Float,
    backdrop: Float,
    deleting: Boolean,
    onClose: () -> Unit,
    onConfirm: () -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    Box(Modifier.fillMaxSize().zIndex(2100f)) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = backdrop }
                .background(Color.Black.copy(alpha = 0.5f))
                .pressable { onClose() }
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer { translationY = slide * density.density }
                .rnShadow(10.dp, SheetShape, Color.Black)
                .background(Color.White, SheetShape)
                .pressable { }
                .padding(top = 15.dp, bottom = 35.dp)
                .absolutePadding(left = 25.dp, right = 25.dp)
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 20.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(Color(0xFFE0E0E0), RoundedCornerShape(2.dp))
            )
            Column(Modifier.fillMaxWidth().padding(bottom = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .padding(bottom = 20.dp)
                        .size(70.dp)
                        .rnShadow(8.dp, CircleShape, CoursePalette.error)
                        .background(CoursePalette.error, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    MaterialIcons("warning", size = 32.dp, color = Color.White)
                }
                AppText(
                    text = "تأیید حذف",
                    modifier = Modifier.padding(bottom = 15.dp),
                    fontSize = 20.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color(0xFF2C3E50),
                    textAlign = TextAlign.Center
                )
                AppText(
                    text = "آیا از حذف این دوره اطمینان دارید؟\nاین عمل قابل بازگشت نیست و تمام اطلاعات دوره حذف خواهد شد.",
                    fontSize = 16.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    color = Color(0xFF6C757D),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
            RowReverse(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .border(1.dp, Color(0xFFE9ECEF), RoundedCornerShape(15.dp))
                        .background(Color(0xFFF8F9FA), RoundedCornerShape(15.dp))
                        .touchableOpacity(enabled = !deleting) { onClose() }
                        .absolutePadding(top = 16.dp, bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AppText(
                        text = "لغو",
                        fontSize = 16.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color(0xFF6C757D),
                        textAlign = TextAlign.Center
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .rnShadow(8.dp, RoundedCornerShape(15.dp), CoursePalette.error)
                        .clip(RoundedCornerShape(15.dp))
                        .touchableOpacity(enabled = !deleting) { onConfirm() }
                ) {
                    LinearGradient(
                        colors = listOf(CoursePalette.error, Color(0xFFC0392B)),
                        modifier = Modifier.fillMaxWidth(),
                        start = Offset(0f, 0f),
                        end = Offset(1f, 1f)
                    ) {
                        RowReverse(
                            Modifier.fillMaxWidth().absolutePadding(left = 20.dp, right = 20.dp, top = 16.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MaterialIcons(
                                if (deleting) "hourglass-empty" else "delete-forever",
                                size = 20.dp,
                                color = Color.White
                            )
                            Spacer(Modifier.width(10.dp))
                            AppText(
                                text = if (deleting) "در حال حذف..." else "بله، حذف کن",
                                fontSize = 16.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
