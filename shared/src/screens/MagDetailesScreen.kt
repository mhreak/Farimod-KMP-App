package ir.farimod.app.screens

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.hooks.useToast
import ir.farimod.app.navigation.useFocusEffect
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.navigation.useRoute
import ir.farimod.app.network.Api
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.str
import ir.farimod.app.network.with
import ir.farimod.app.screens.mag.MagBlogEventBus
import ir.farimod.app.screens.magdetailes.MagDetailCover
import ir.farimod.app.screens.magdetailes.MagDetailSkeleton
import ir.farimod.app.screens.magdetailes.MagHtmlContent
import ir.farimod.app.screens.magdetailes.magNegativeTop
import ir.farimod.app.screens.magdetailes.magRightBorder
import ir.farimod.app.screens.magdetailes.magTransformContentReviewToRatingOptions
import ir.farimod.app.state.AuthState
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.MainBackground
import ir.farimod.app.ui.MultiOptionRatingComponent
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RatingOption
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.StarDisplay
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.pressable
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.rnTiming
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.utils.jsToFixed
import ir.farimod.app.utils.jsToNumber
import ir.farimod.app.utils.jsToString
import ir.farimod.app.utils.jsTruthy
import ir.farimod.app.utils.toPersianDigits
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val MagDetailPrimary = Color(0xFF667EEA)
private val MagDetailSuccess = Color(0xFF2ECC71)
private val MagDetailWarning = Color(0xFFF39C12)
private val MagDetailError = Color(0xFFE74C3C)
private val MagDetailInfo = Color(0xFF3498DB)
private val MagDetailGold = Color(0xFFFFD700)
private val magHtmlTag = Regex("<[^>]+>", RegexOption.IGNORE_CASE)

/**
 * Port of `screens/MagDetailesScreen.tsx`.
 * Route params: `title` (unused in the header, same as RN) and `blogId`.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MagDetailesScreen() {
    val navigation = useNavigation()
    val route = useRoute()
    val scope = rememberCoroutineScope()
    val toast = useToast()
    val user = AuthState.user
    val topInset = statusBarHeight()
    val blogId = route.params["blogId"]

    var blogPost by remember { mutableStateOf<JsonObject?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var actionVisible by remember { mutableStateOf(false) }
    var deleteVisible by remember { mutableStateOf(false) }
    var userDetailedRatings by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var dynamicRatingOptions by remember { mutableStateOf<List<RatingOption>>(emptyList()) }
    var likeCount by remember { mutableStateOf(0) }
    var isLiked by remember { mutableStateOf(false) }
    var isLiking by remember { mutableStateOf(false) }
    var imageError by remember { mutableStateOf(false) }

    val actionSlide = remember { Animatable(0f) }
    val actionFade = remember { Animatable(0f) }
    val deleteSlide = remember { Animatable(0f) }
    val deleteFade = remember { Animatable(0f) }

    suspend fun fetchBlogPost(id: Any?) {
        loading = true
        error = null
        try {
            val current = AuthState.user
                ?: throw IllegalStateException("Cannot read properties of null (reading 'MemberId')")
            val response = Api.get(
                "BlogPost/Get",
                query = mapOf(
                    "blogPostId" to jsToString(id),
                    "currentMemberId" to jsToString(current.MemberId)
                )
            )
            if (!response.ok) throw Exception("HTTP error! status: ${response.status}")
            val result = response.json ?: throw Exception(
                if (response.text.isBlank()) "JSON Parse error: Unexpected end of input" else "JSON Parse error: Unexpected character"
            )
            val blog = result["BlogPost"] as? JsonObject
                ?: throw Exception("Cannot convert undefined or null to object")
            val reviews = result["ContentReviewItemList"] ?: kotlinx.serialization.json.JsonArray(emptyList())
            blogPost = blog.with(
                "ContentReviewItemList" to reviews,
                "IsMemberLiked" to result["IsMemberLiked"].let { jsTruthy(it) },
                "UserRating" to 0,
                "UserDetailedRatings" to JsonObject(emptyMap()),
                "RatingCount" to 0
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message
            blogPost = null
        } finally {
            loading = false
        }
    }

    useFocusEffect(blogId) {
        if (jsTruthy(blogId)) fetchBlogPost(blogId)
    }

    LaunchedEffect(blogPost) {
        val post = blogPost ?: return@LaunchedEffect
        val likes = post["LikeCount"]
        likeCount = if (jsTruthy(likes)) jsToNumber(likes).toInt() else 0
        isLiked = jsTruthy(post["IsMemberLiked"])
        dynamicRatingOptions = magTransformContentReviewToRatingOptions(post["ContentReviewItemList"])
    }

    LaunchedEffect(error) {
        if (error != null) {
            toast.showToast("خطا در دریافت اطلاعات مقاله. لطفاً دوباره تلاش کنید.", "error")
        }
    }

    LaunchedEffect(actionVisible) {
        if (actionVisible) {
            actionSlide.snapTo(0f)
            actionFade.snapTo(0f)
            coroutineScope {
                launch { actionFade.animateTo(1f, rnTiming(300)) }
                launch { actionSlide.animateTo(1f, rnTiming(300)) }
            }
        }
    }
    LaunchedEffect(deleteVisible) {
        if (deleteVisible) {
            deleteSlide.snapTo(0f)
            deleteFade.snapTo(0f)
            coroutineScope {
                launch { deleteFade.animateTo(1f, rnTiming(300)) }
                launch { deleteSlide.animateTo(1f, rnTiming(300)) }
            }
        }
    }

    fun hasValidImage(post: JsonObject?): Boolean {
        if (post == null || imageError) return false
        val file = post.str("FeaturedImageFileName")
        val url = post.str("FeaturedImageURL")
        if (file.isNullOrEmpty() || url.isNullOrEmpty()) return false
        if (url.endsWith("/")) return false
        return true
    }

    fun onRefresh() {
        scope.launch {
            refreshing = true
            if (jsTruthy(blogId)) fetchBlogPost(blogId)
            refreshing = false
        }
    }

    val memberId = user?.MemberId
    val postMemberId = blogPost?.get("MemberId")
    val isOwnPost = blogPost != null && memberId != null && postMemberId != null &&
        jsToNumber(postMemberId) == memberId.toDouble()

    fun openActions() {
        actionVisible = true
    }

    fun closeActions(after: (() -> Unit)? = null) {
        scope.launch {
            coroutineScope {
                launch { actionFade.animateTo(0f, rnTiming(250)) }
                launch { actionSlide.animateTo(0f, rnTiming(250)) }
            }
            actionVisible = false
            after?.invoke()
        }
    }

    fun closeDelete(after: (() -> Unit)? = null) {
        scope.launch {
            coroutineScope {
                launch { deleteFade.animateTo(0f, rnTiming(250)) }
                launch { deleteSlide.animateTo(0f, rnTiming(250)) }
            }
            deleteVisible = false
            after?.invoke()
        }
    }

    fun handleEditPost() {
        val post = blogPost ?: return
        navigation.navigate(
            "AddNewPost",
            mapOf("isEdit" to true, "blogData" to post)
        )
    }

    fun handleDeletePost() {
        closeActions()
        scope.launch {
            delay(300)
            deleteVisible = true
        }
    }

    fun handleLike() {
        val post = blogPost ?: return
        val memberId = user?.MemberId ?: return
        if (isLiking) return
        val prevLiked = isLiked
        val prevCount = likeCount
        val newLiked = !prevLiked
        val newCount = prevCount + if (!prevLiked) 1 else -1

        isLiking = true
        isLiked = newLiked
        likeCount = newCount

        val postId = post.int("BlogPostId") ?: post.str("BlogPostId")?.toIntOrNull()
        if (postId != null) {
            MagBlogEventBus.notifyBlogPostLiked(postId, newLiked, newCount)
        }

        scope.launch {
            try {
                val response = Api.post(
                    "BlogPost/Like",
                    query = mapOf(
                        "id" to jsToString(post["BlogPostId"]),
                        "memberId" to memberId
                    )
                )
                if (!response.ok) throw Exception("HTTP error! status: ${response.status}")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                isLiked = prevLiked
                likeCount = prevCount
                if (postId != null) {
                    MagBlogEventBus.notifyBlogPostLiked(postId, prevLiked, prevCount)
                }
                toast.showToast("خطا در ثبت لایک", "error")
            } finally {
                isLiking = false
            }
        }
    }

    fun confirmDeletePost() {
        val post = blogPost ?: return
        closeDelete()
        scope.launch {
            try {
                isDeleting = true
                val response = Api.delete(
                    "BlogPost/Delete",
                    query = mapOf("blogPostId" to jsToString(post["BlogPostId"]))
                )
                if (response.ok) {
                    toast.showToast("پست با موفقیت حذف شد", "success")
                    navigation.navigate("MyPosts")
                } else {
                    val errorData = response.json ?: throw Exception(
                        if (response.text.isBlank()) "JSON Parse error: Unexpected end of input" else "JSON Parse error: Unexpected character"
                    )
                    val message = errorData.str("Message")
                    throw Exception(if (!message.isNullOrEmpty()) message else "خطا در حذف پست")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toast.showToast(e.message?.takeIf { it.isNotEmpty() } ?: "خطا در حذف پست", "error")
            } finally {
                isDeleting = false
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        MainBackground()

        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = topInset + 35.dp, bottom = 20.dp)
                    .absolutePadding(left = 20.dp, right = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                AppText(
                    text = " ",
                    fontSize = 22.sp,
                    fontFamily = RNFonts.Yekan_Bakh_ExtraBold,
                    color = Color(0xFF2C3E50),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().absolutePadding(left = 56.dp, right = 56.dp)
                ,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }

            val pullState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { onRefresh() },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = pullState,
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullState,
                        isRefreshing = refreshing,
                        modifier = Modifier.align(Alignment.TopCenter),
                        containerColor = Color.White,
                        color = MagDetailPrimary
                    )
                }
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    when {
                        loading -> MagDetailSkeleton()
                        error != null -> MagDetailMessage(
                            icon = "error",
                            title = "خطا در دریافت اطلاعات",
                            subtitle = "لطفاً اتصال اینترنت خود را بررسی کنید",
                            retry = { if (jsTruthy(blogId)) scope.launch { fetchBlogPost(blogId) } }
                        )
                        blogPost == null -> MagDetailMessage(
                            icon = "article",
                            title = "مقاله یافت نشد",
                            subtitle = "مقاله مورد نظر موجود نیست",
                            retry = null
                        )
                        else -> {
                            val post = blogPost!!
                            val validImage = hasValidImage(post)
                            Column(Modifier.fillMaxWidth()) {
                                Box(Modifier.fillMaxWidth().height(250.dp)) {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .touchableOpacity(activeOpacity = 0.9f, enabled = validImage) { }
                                    ) {
                                        MagDetailCover(
                                            url = post.str("FeaturedImageURL"),
                                            valid = validImage,
                                            onError = { imageError = true },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    if (validImage) {
                                        Box(
                                            Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .height(60.dp)
                                                .background(
                                                    androidx.compose.ui.graphics.Brush.verticalGradient(
                                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f))
                                                    )
                                                )
                                        )
                                    }
                                    if (isOwnPost) {
                                        val badgeColor = if (post["Active"].let { jsTruthy(it) }) MagDetailSuccess else MagDetailWarning
                                        Box(
                                            Modifier
                                                .align(AbsoluteAlignment.TopRight)
                                                .absoluteOffset(x = (-15).dp, y = 15.dp)
                                                .zIndex(2f)
                                                .rnShadow(4.dp, RoundedCornerShape(15.dp))
                                                .background(badgeColor, RoundedCornerShape(15.dp))
                                                .absolutePadding(left = 12.dp, right = 12.dp, top = 6.dp, bottom = 6.dp)
                                        ) {
                                            AppText(
                                                text = if (jsTruthy(post["Active"])) "منتشر شده" else "پیش‌نویس",
                                                fontSize = 12.sp,
                                                fontFamily = RNFonts.Yekan_Bakh_Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    val likeBg = if (isLiked) Color(0xCCE91E63) else Color(0xCCFFFFFF)
                                    val likeFg = if (isLiked) Color.White else Color(0xFFE91E63)
                                    Box(
                                        Modifier
                                            .align(AbsoluteAlignment.TopLeft)
                                            .absoluteOffset(x = 15.dp, y = 15.dp)
                                            .zIndex(2f)
                                            .rnShadow(4.dp, RoundedCornerShape(20.dp))
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(likeBg)
                                            .touchableOpacity(activeOpacity = 0.7f, enabled = !isLiking) { handleLike() }
                                            .absolutePadding(left = 12.dp, right = 12.dp, top = 8.dp, bottom = 8.dp)
                                    ) {
                                        RowReverse(verticalAlignment = Alignment.CenterVertically) {
                                            MaterialIcons(
                                                name = if (isLiked) "favorite" else "favorite-border",
                                                size = 18.dp,
                                                color = likeFg
                                            )
                                            AppText(
                                                text = toPersianDigits(likeCount.toString()),
                                                fontSize = 15.sp,
                                                fontFamily = RNFonts.Yekan_Bakh_Bold,
                                                color = likeFg,
                                                modifier = Modifier.absolutePadding(right = 6.dp)
                                            )
                                        }
                                    }
                                }

                                val sheetShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .magNegativeTop(30.dp)
                                        .zIndex(1f)
                                        .rnShadow(8.dp, sheetShape)
                                        .clip(sheetShape)
                                        .background(Color.White)
                                        .absolutePadding(left = 25.dp, right = 25.dp)
                                        .padding(top = 30.dp, bottom = 40.dp)
                                ) {
                                    AppText(
                                        text = toPersianDigits(post["Title"]),
                                        fontSize = 22.sp,
                                        fontFamily = RNFonts.Yekan_Bakh_ExtraBold,
                                        color = Color(0xFF2C3E50),
                                        textAlign = TextAlign.Right,
                                        lineHeight = 32.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .absolutePadding(bottom = 20.dp)
                                    )
                                    val rawContent = post.str("Content") ?: ""
                                    val trimmed = rawContent.trim()
                                    if (trimmed.isNotEmpty()) {
                                        if (magHtmlTag.containsMatchIn(trimmed)) {
                                            MagHtmlContent(
                                                trimmed,
                                                Modifier
                                                    .fillMaxWidth()
                                                    .absolutePadding(bottom = 24.dp)
                                            )
                                        } else {
                                            AppText(
                                                text = toPersianDigits(trimmed),
                                                fontSize = 16.sp,
                                                fontFamily = RNFonts.Yekan_Bakh_Regular,
                                                color = Color(0xFF34495E),
                                                textAlign = TextAlign.Right,
                                                lineHeight = 28.sp,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .absolutePadding(bottom = 30.dp)
                                            )
                                        }
                                    }
                                    if (jsTruthy(post["MemberName"])) {
                                        RowReverse(
                                            Modifier
                                                .fillMaxWidth()
                                                .absolutePadding(bottom = 25.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFFF3EEFC))
                                                .magRightBorder(4.dp, Color(0xFF866BFF))
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                Modifier
                                                    .size(40.dp)
                                                    .absolutePadding(left = 12.dp)
                                                    .background(Color.White.copy(alpha = 0.9f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                MaterialIcons("person", size = 18.dp, color = Color(0xFF816BFF))
                                            }
                                            AppText(
                                                text = toPersianDigits(post["MemberName"]),
                                                fontSize = 16.sp,
                                                fontFamily = RNFonts.Yekan_Bakh_Bold,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                    val categoriesStr = post.str("BlogPostCategoriesStr")
                                    if (!categoriesStr.isNullOrBlank()) {
                                        val categories = categoriesStr.split('،')
                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                            FlowRow(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .absolutePadding(bottom = 25.dp),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                categories.forEach { category ->
                                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                                        val chip = RoundedCornerShape(20.dp)
                                                        Box(
                                                            Modifier
                                                                .background(Color(0xFFE3F2FD), chip)
                                                                .border(1.dp, Color(0xFFBBDEFB), chip)
                                                                .absolutePadding(left = 12.dp, right = 12.dp, top = 6.dp, bottom = 6.dp)
                                                        ) {
                                                            AppText(
                                                                text = toPersianDigits(category.trim()),
                                                                fontSize = 13.sp,
                                                                fontFamily = RNFonts.Yekan_Bakh_Regular,
                                                                color = Color(0xFF1976D2),
                                                                textAlign = TextAlign.Center
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    val ratingShape = RoundedCornerShape(20.dp)
                                    Column(
                                        Modifier
                                            .fillMaxWidth()
                                            .absolutePadding(bottom = 25.dp)
                                            .rnShadow(2.dp, ratingShape)
                                            .border(1.dp, Color(0xFFDFDFDF), ratingShape)
                                            .background(Color.White, ratingShape)
                                            .padding(20.dp)
                                    ) {
                                        val ratingRaw = post["Rating"]
                                        MultiOptionRatingComponent(
                                            contentId = post.int("BlogPostId") ?: post.str("BlogPostId"),
                                            averageRating = if (jsTruthy(ratingRaw)) jsToNumber(ratingRaw) else 0.0,
                                            ratingCount = 0,
                                            initialRating = if (jsTruthy(post["UserRating"])) jsToNumber(post["UserRating"]) else 0.0,
                                            initialDetailedRatings = userDetailedRatings,
                                            maxStars = 5,
                                            size = 24.dp,
                                            starColor = MagDetailGold,
                                            enableMultipleOptions = true,
                                            ratingOptions = dynamicRatingOptions,
                                            modalTitle = "امتیازدهی مقاله",
                                            submitButtonText = "ثبت امتیاز",
                                            cancelButtonText = "لغو",
                                            showRatingCount = true,
                                            showRatingText = true,
                                            animated = true,
                                            allowHalfStars = false,
                                            modifier = Modifier.fillMaxWidth(),
                                            onRatingChange = { _, detailed ->
                                                if (detailed != null) userDetailedRatings = detailed
                                            },
                                            onRatingError = { message ->
                                                toast.showToast(
                                                    message.takeIf { it.isNotEmpty() } ?: "خطا در ثبت امتیاز",
                                                    "error"
                                                )
                                            },
                                            onRatingSubmitted = { result ->
                                                if (result.ratings != null) {
                                                    val submitted = result.ratings
                                                    userDetailedRatings = submitted
                                                    val ratingsJson = buildJsonObject {
                                                        submitted.forEach { (k, v) -> put(k, v) }
                                                    }
                                                    blogPost = blogPost?.with(
                                                        "UserRating" to (result.averageRating ?: 0.0),
                                                        "UserDetailedRatings" to ratingsJson
                                                    )
                                                    toast.showToast(
                                                        "امتیاز شما در ${result.ratings.size} بخش با موفقیت ثبت شد",
                                                        "success"
                                                    )
                                                } else {
                                                    blogPost = blogPost?.with("UserRating" to (result.rating ?: 0.0))
                                                    toast.showToast(
                                                        "امتیاز ${toPersianDigits(jsToString(result.rating))} ستاره ثبت شد",
                                                        "success"
                                                    )
                                                }
                                            }
                                        )

                                        val userRows = dynamicRatingOptions.filter { (userDetailedRatings[it.id] ?: 0.0) > 0.0 }
                                        if (dynamicRatingOptions.isNotEmpty() && userRows.isNotEmpty()) {
                                            Column(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 20.dp)
                                                    .background(Color(0xFFE8F5E8), RoundedCornerShape(15.dp))
                                                    .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(15.dp))
                                                    .padding(15.dp)
                                            ) {
                                                AppText(
                                                    text = "امتیازات شما:",
                                                    fontSize = 16.sp,
                                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                                    color = Color(0xFF2C3E50),
                                                    textAlign = TextAlign.Right,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .absolutePadding(bottom = 15.dp)
                                                )
                                                userRows.forEach { option ->
                                                    val score = userDetailedRatings[option.id] ?: 0.0
                                                    MagRatingRow(
                                                        title = option.title,
                                                        score = score,
                                                        scoreColor = MagDetailSuccess,
                                                        halfStars = false,
                                                        cardColor = Color.White,
                                                        cardBorder = Color(0xFFE8F5E8),
                                                        bottom = 10.dp
                                                    )
                                                }
                                            }
                                        }

                                        val avgRows = dynamicRatingOptions.filter { it.averageRating > 0.0 }
                                        if (dynamicRatingOptions.isNotEmpty() && avgRows.isNotEmpty()) {
                                            Column(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 20.dp)
                                                    .background(Color(0xFFF8F9FA), RoundedCornerShape(15.dp))
                                                    .border(1.dp, Color(0xFFE9ECEF), RoundedCornerShape(15.dp))
                                                    .padding(15.dp)
                                            ) {
                                                AppText(
                                                    text = "میانگین امتیازات:",
                                                    fontSize = 16.sp,
                                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                                    color = Color(0xFF2C3E50),
                                                    textAlign = TextAlign.Right,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .absolutePadding(bottom = 15.dp)
                                                )
                                                avgRows.forEach { option ->
                                                    MagRatingRow(
                                                        title = option.title,
                                                        score = option.averageRating,
                                                        scoreText = toPersianDigits(jsToFixed(option.averageRating, 1)),
                                                        scoreColor = Color(0xFF666666),
                                                        halfStars = true,
                                                        cardColor = Color.White,
                                                        cardBorder = Color(0xFFF0F0F0),
                                                        bottom = 12.dp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    RowReverse(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(top = 20.dp)
                                            .drawBehind {
                                                drawRect(
                                                    Color(0xFFF0F0F0),
                                                    size = Size(size.width, 1.dp.toPx())
                                                )
                                            }
                                            .padding(top = 1.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        RowReverse(
                                            Modifier.absolutePadding(bottom = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            MaterialIcons("calendar-month", size = 17.dp, color = Color(0xFF666666))
                                            AppText(
                                                text = toPersianDigits(post["ShamsiInsertDate"]),
                                                fontSize = 15.sp,
                                                fontFamily = RNFonts.Yekan_Bakh_Regular,
                                                color = Color(0xFF666666),
                                                modifier = Modifier.absolutePadding(right = 10.dp)
                                            )
                                        }
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
                .absoluteOffset(x = (-20).dp, y = topInset + 33.dp)
                .zIndex(1000f)
                .size(44.dp)
                .rnShadow(5.dp, CircleShape)
                .background(Color.White.copy(alpha = 0.9f), CircleShape)
                .touchableOpacity { navigation.goBack() },
            contentAlignment = Alignment.Center
        ) {
            MaterialIcons("arrow-forward", size = 24.dp, color = Color(0xFF6366F1))
        }

        if (isOwnPost && !loading) {
            Box(
                Modifier
                    .align(AbsoluteAlignment.TopLeft)
                    .absoluteOffset(x = 20.dp, y = topInset + 33.dp)
                    .zIndex(1000f)
                    .size(44.dp)
                    .rnShadow(5.dp, CircleShape)
                    .background(Color.White.copy(alpha = 0.9f), CircleShape)
                    .touchableOpacity(enabled = !isDeleting) { openActions() },
                contentAlignment = Alignment.Center
            ) {
                MaterialIcons("more-vert", size = 24.dp, color = Color(0xFF6366F1))
            }
        }

        if (actionVisible) {
            val density = LocalDensity.current
            Dialog(
                onDismissRequest = { closeActions() },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = actionFade.value }
                            .background(Color.Black.copy(alpha = 0.5f))
                            .pressable { closeActions() }
                    )
                    MagPostActionsSheet(
                        modifier = Modifier.graphicsLayer {
                            translationY = (1f - actionSlide.value) * 300f * density.density
                        },
                        isDeleting = isDeleting,
                        onEdit = {
                            closeActions()
                            scope.launch {
                                delay(300)
                                handleEditPost()
                            }
                        },
                        onDelete = { handleDeletePost() },
                        onCancel = { closeActions() }
                    )
                }
            }
        }

        if (deleteVisible) {
            val density = LocalDensity.current
            Dialog(
                onDismissRequest = { if (!isDeleting) closeDelete() },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = deleteFade.value }
                            .background(Color.Black.copy(alpha = 0.5f))
                            .pressable { if (!isDeleting) closeDelete() }
                    )
                    MagDeleteSheet(
                        modifier = Modifier.graphicsLayer {
                            translationY = (1f - deleteSlide.value) * 300f * density.density
                        },
                        isDeleting = isDeleting,
                        onCancel = { closeDelete() },
                        onConfirm = { confirmDeletePost() }
                    )
                }
            }
        }

        Toast(toast.toastVisible, toast.toastMessage, toast.toastType) { toast.setToastVisible(false) }
    }
}

@Composable
private fun MagRatingRow(
    title: String,
    score: Double,
    scoreColor: Color,
    halfStars: Boolean,
    cardColor: Color,
    cardBorder: Color,
    bottom: androidx.compose.ui.unit.Dp,
    scoreText: String = toPersianDigits(jsToString(score))
) {
    val shape = RoundedCornerShape(12.dp)
    RowReverse(
        Modifier
            .fillMaxWidth()
            .absolutePadding(bottom = bottom)
            .background(cardColor, shape)
            .border(1.dp, cardBorder, shape)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            AppText(
                text = title,
                fontSize = 14.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = Color(0xFF2C3E50),
                textAlign = TextAlign.Right,
                modifier = Modifier.absolutePadding(bottom = 4.dp)
            )
        }
        RowReverse(
            Modifier.absolutePadding(left = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StarDisplay(
                rating = score,
                maxStars = 5,
                size = 16.dp,
                color = MagDetailGold,
                emptyColor = Color(0xFFE0E0E0),
                showHalfStars = halfStars,
                animated = false
            )
            AppText(
                text = scoreText,
                fontSize = 14.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = scoreColor
            )
        }
    }
}

@Composable
private fun MagDetailMessage(
    icon: String,
    title: String,
    subtitle: String,
    retry: (() -> Unit)?
) {
    Column(
        Modifier
            .fillMaxWidth()
            .absolutePadding(left = 40.dp, right = 40.dp, top = 60.dp, bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MaterialIcons(icon, size = 80.dp, color = Color(0xFF9E9E9E))
        AppText(
            text = title,
            fontSize = 20.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp)
        )
        AppText(
            text = subtitle,
            fontSize = 16.sp,
            fontFamily = RNFonts.Yekan_Bakh_Regular,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(top = 12.dp)
        )
        if (retry != null) {
            val shape = RoundedCornerShape(25.dp)
            RowReverse(
                Modifier
                    .padding(top = 24.dp)
                    .rnShadow(8.dp, shape, MagDetailPrimary)
                    .clip(shape)
                    .background(MagDetailPrimary)
                    .touchableOpacity(onClick = retry)
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
}

@Composable
private fun MagPostActionsSheet(
    modifier: Modifier,
    isDeleting: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    val shape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)
    Column(
        modifier
            .fillMaxWidth()
            .pressable { }
            .rnShadow(10.dp, shape)
            .clip(shape)
            .background(Color.White)
            .absolutePadding(left = 20.dp, right = 20.dp)
            .padding(top = 15.dp, bottom = 35.dp)
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .absolutePadding(bottom = 20.dp)
                .size(width = 40.dp, height = 4.dp)
                .background(Color(0xFFE0E0E0), RoundedCornerShape(2.dp))
        )
        AppText(
            text = "عملیات پست",
            fontSize = 18.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .absolutePadding(bottom = 25.dp)
        )
        MagActionRow(
            icon = "edit",
            iconBg = MagDetailInfo,
            title = "ویرایش پست",
            subtitle = "ویرایش عنوان و محتوای پست",
            enabled = true,
            onClick = onEdit
        )
        MagActionRow(
            icon = "delete",
            iconBg = MagDetailError,
            title = "حذف پست",
            subtitle = "حذف کامل پست از سیستم",
            enabled = !isDeleting,
            onClick = onDelete
        )
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Color(0xFFF8F9FA))
                .border(1.dp, Color(0xFFE9ECEF), RoundedCornerShape(15.dp))
                .touchableOpacity { onCancel() }
                .absolutePadding(top = 15.dp, bottom = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            AppText(
                text = "لغو",
                fontSize = 16.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = Color(0xFF6C757D)
            )
        }
    }
}

@Composable
private fun MagActionRow(
    icon: String,
    iconBg: Color,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(15.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .absolutePadding(bottom = 10.dp)
            .clip(shape)
            .background(Color(0xFFF8F9FA))
            .border(1.dp, Color(0xFFE9ECEF), shape)
            .touchableOpacity(enabled = enabled, onClick = onClick)
            .absolutePadding(left = 10.dp, right = 10.dp, top = 15.dp, bottom = 15.dp)
    ) {
        RowReverse(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(45.dp)
                    .absolutePadding(left = 15.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                MaterialIcons(icon, size = 22.dp, color = Color.White)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                AppText(
                    text = title,
                    fontSize = 16.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color(0xFF2C3E50),
                    textAlign = TextAlign.Right,
                    modifier = Modifier.absolutePadding(bottom = 2.dp)
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
}

@Composable
private fun MagDeleteSheet(
    modifier: Modifier,
    isDeleting: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val shape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)
    Column(
        modifier
            .fillMaxWidth()
            .pressable { }
            .rnShadow(10.dp, shape)
            .clip(shape)
            .background(Color.White)
            .absolutePadding(left = 25.dp, right = 25.dp)
            .padding(top = 15.dp, bottom = 35.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .absolutePadding(bottom = 20.dp)
                .size(width = 40.dp, height = 4.dp)
                .background(Color(0xFFE0E0E0), RoundedCornerShape(2.dp))
        )
        Box(
            Modifier
                .absolutePadding(bottom = 20.dp)
                .size(70.dp)
                .rnShadow(8.dp, CircleShape, MagDetailError)
                .background(MagDetailError, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MaterialIcons("warning", size = 32.dp, color = Color.White)
        }
        AppText(
            text = "تأیید حذف",
            fontSize = 20.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center,
            modifier = Modifier.absolutePadding(bottom = 15.dp)
        )
        AppText(
            text = "آیا از حذف این پست اطمینان دارید؟\nاین عمل قابل بازگشت نیست.",
            fontSize = 16.sp,
            fontFamily = RNFonts.Yekan_Bakh_Regular,
            color = Color(0xFF6C757D),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.absolutePadding(bottom = 30.dp)
        )
        RowReverse(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color(0xFFF8F9FA))
                    .border(1.dp, Color(0xFFE9ECEF), RoundedCornerShape(15.dp))
                    .touchableOpacity(enabled = !isDeleting) { onCancel() }
                    .absolutePadding(top = 16.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                AppText(
                    text = "لغو",
                    fontSize = 16.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color(0xFF6C757D)
                )
            }
            val confirmShape = RoundedCornerShape(15.dp)
            Box(
                Modifier
                    .weight(1f)
                    .rnShadow(8.dp, confirmShape, MagDetailError)
                    .clip(confirmShape)
                    .touchableOpacity(enabled = !isDeleting) { onConfirm() }
            ) {
                LinearGradient(
                    colors = listOf(MagDetailError, Color(0xFFC0392B)),
                    start = Offset(0f, 0f),
                    end = Offset(1f, 1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RowReverse(
                        Modifier
                            .fillMaxWidth()
                            .absolutePadding(left = 20.dp, right = 20.dp, top = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MaterialIcons(
                            name = if (isDeleting) "hourglass-empty" else "delete-forever",
                            size = 20.dp,
                            color = Color.White
                        )
                        AppText(
                            text = if (isDeleting) "در حال حذف..." else "بله، حذف کن",
                            fontSize = 16.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
