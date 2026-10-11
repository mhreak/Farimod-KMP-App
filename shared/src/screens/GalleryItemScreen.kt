package ir.farimod.app.screens

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.hooks.useToast
import ir.farimod.app.navigation.Tabs
import ir.farimod.app.navigation.useFocusEffect
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.navigation.useRoute
import ir.farimod.app.network.Api
import ir.farimod.app.network.bool
import ir.farimod.app.network.double
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.list
import ir.farimod.app.network.str
import ir.farimod.app.network.with
import ir.farimod.app.platform.PlatformBackHandler
import ir.farimod.app.platform.rememberMediaPicker
import ir.farimod.app.state.AuthState
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.ImageInputList
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.MainBackground
import ir.farimod.app.ui.MultiOptionRatingComponent
import ir.farimod.app.ui.NetworkImage
import ir.farimod.app.ui.RNAlert
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RatingOption
import ir.farimod.app.ui.ResizeMode
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.StarDisplay
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.rememberSafeAreaInsets
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.rnSpring
import ir.farimod.app.ui.rnTiming
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.utils.jsToFixed
import ir.farimod.app.utils.jsToString
import ir.farimod.app.utils.toPersianDigits
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

private val GalleryPrimary = Color(0xFF667EEA)
private val GallerySecondary = Color(0xFFFF6B6B)
private val GalleryInfo = Color(0xFF3498DB)
private val GalleryGold = Color(0xFFFFD700)
private val GallerySuccess = Color(0xFF2ECC71)
private val GalleryError = Color(0xFFE74C3C)

/**
 * Port of `screens/GalleryItemScreen.tsx`.
 * Params: title, galleryId. Like posts ImageGallery/Like. Delete uses ImageGallery/Delete?id=.
 * The RN menu that opens "add image" / delete is not rendered, so those sheets stay closed
 * unless [showActionModal] is set; the sheet UI is still ported.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryItemScreen() {
    val navigation = useNavigation()
    val route = useRoute()
    val toast = useToast()
    val scope = rememberCoroutineScope()
    val picker = rememberMediaPicker()
    val insets = rememberSafeAreaInsets()
    val user = AuthState.user
    val statusTop = statusBarHeight()

    val galleryId = route.int("galleryId")
    val galleryTitle = route.str("title") ?: "گالری"

    var galleryData by remember { mutableStateOf<JsonObject?>(null) }
    var imageUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var refreshing by remember { mutableStateOf(false) }
    var likeCount by remember { mutableIntStateOf(0) }
    var isLiked by remember { mutableStateOf(false) }
    var isLiking by remember { mutableStateOf(false) }
    val likeScale = remember { Animatable(1f) }
    var ratingOptions by remember { mutableStateOf<List<RatingOption>>(emptyList()) }
    var userDetailedRatings by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }

    var modalVisible by remember { mutableStateOf(false) }
    var fullScreen by remember { mutableStateOf(false) }
    var currentImageIndex by remember { mutableIntStateOf(0) }
    var viewerSession by remember { mutableIntStateOf(0) }
    var showAction by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    val actionSlide = remember { Animatable(0f) }
    val actionBackdrop = remember { Animatable(0f) }
    val deleteSlide = remember { Animatable(0f) }
    val deleteBackdrop = remember { Animatable(0f) }

    val isOwnGallery = galleryData != null && user != null &&
        galleryData?.int("MemberId") == user.MemberId

    suspend fun fetchGallery(id: Int) {
        try {
            val memberId = user?.MemberId ?: 0
            val response = Api.get(
                "ImageGallery/Get",
                query = mapOf("id" to id, "currentMemberId" to memberId)
            )
            if (!response.ok) throw Exception("HTTP error! status: ${response.status}")
            val result = response.json ?: throw Exception("JSON")
            val gallery = result["ImageGallery"] as? JsonObject
                ?: throw Exception("ImageGallery")
            val reviews = result["ContentReviewItemList"] ?: JsonArray(emptyList())
            galleryData = gallery.with(
                "ContentReviewItemList" to reviews,
                "IsMemberLiked" to result["IsMemberLiked"].bool,
                "UserRating" to 0,
                "RatingCount" to 0
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            galleryData = null
        }
    }

    useFocusEffect(galleryId, user?.MemberId) {
        if (galleryId != null) fetchGallery(galleryId)
    }

    LaunchedEffect(galleryData) {
        val data = galleryData ?: return@LaunchedEffect
        likeCount = data.int("LikeCount") ?: 0
        isLiked = data["IsMemberLiked"].bool
        ratingOptions = galleryItemRatingOptions(data["ContentReviewItemList"])
        imageUris = data.list("ImageGalleryItemList").map { it.str("ImageURL") ?: "" }
    }

    fun showToast(message: String, type: String = "info") {
        toast.showToast(message, type)
    }

    fun closeAction(after: () -> Unit = {}) {
        scope.launch {
            coroutineScope {
                launch { actionBackdrop.animateTo(0f, rnTiming(250)) }
                launch { actionSlide.animateTo(0f, rnTiming(250)) }
            }
            showAction = false
            after()
        }
    }

    fun closeDelete(after: () -> Unit = {}) {
        scope.launch {
            coroutineScope {
                launch { deleteBackdrop.animateTo(0f, rnTiming(250)) }
                launch { deleteSlide.animateTo(0f, rnTiming(250)) }
            }
            showDelete = false
            after()
        }
    }

    fun selectImageSource() {
        modalVisible = true
    }

    fun openViewer(uri: String) {
        val index = imageUris.indexOf(uri).let { if (it < 0) 0 else it }
        currentImageIndex = index
        scope.launch {
            delay(50)
            viewerSession++
            fullScreen = true
        }
    }

    fun handleLike() {
        val data = galleryData ?: return
        val memberId = user?.MemberId ?: return
        if (isLiking || memberId == 0) return
        val id = data.int("ImageGalleryId") ?: return
        isLiking = true
        val prevLiked = isLiked
        val prevCount = likeCount
        val next = !prevLiked
        isLiked = next
        likeCount = prevCount + if (next) 1 else -1
        scope.launch {
            likeScale.animateTo(0.6f, rnTiming(100))
            likeScale.animateTo(1.3f, rnSpring(200f, 4f))
            likeScale.animateTo(1f, rnSpring(200f, 6f))
        }
        scope.launch {
            try {
                val response = Api.post(
                    "ImageGallery/Like",
                    query = mapOf("id" to id, "memberId" to memberId)
                )
                if (!response.ok) throw Exception("HTTP error! status: ${response.status}")
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                isLiked = prevLiked
                likeCount = prevCount
                showToast("خطا در ثبت لایک", "error")
            } finally {
                isLiking = false
            }
        }
    }

    fun confirmDeleteGallery() {
        val id = galleryData?.int("ImageGalleryId") ?: return
        closeDelete()
        scope.launch {
            try {
                isDeleting = true
                val response = Api.delete("ImageGallery/Delete", query = mapOf("id" to id))
                if (response.ok) {
                    showToast("گالری با موفقیت حذف شد", "success")
                    navigation.navigate("MainTabs", mapOf("screen" to Tabs.Home))
                } else {
                    val message = response.json.str("Message")?.takeIf { it.isNotEmpty() } ?: "خطا در حذف گالری"
                    throw Exception(message)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.message?.takeIf { it.isNotEmpty() } ?: "خطا در حذف گالری", "error")
            } finally {
                isDeleting = false
            }
        }
    }

    val scroll = rememberScrollState()
    val pullState = rememberPullToRefreshState()

    PlatformBackHandler(fullScreen || showDelete || showAction || modalVisible) {
        when {
            fullScreen -> fullScreen = false
            showDelete -> closeDelete()
            showAction -> closeAction()
            modalVisible -> modalVisible = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        MainBackground()
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = statusTop + 35.dp, bottom = 50.dp)
                    .absolutePadding(left = 20.dp, right = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppText(
                    text = galleryTitle,
                    modifier = Modifier.fillMaxWidth().absolutePadding(left = 56.dp, right = 56.dp),
                    fontSize = 22.sp,
                    fontFamily = RNFonts.Yekan_Bakh_ExtraBold,
                    color = Color(0xFF2C3E50),
                    textAlign = TextAlign.Center
                ,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }

            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = {
                    scope.launch {
                        refreshing = true
                        if (galleryId != null) fetchGallery(galleryId)
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
                        color = GalleryPrimary
                    )
                }
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll)
                ) {
                    ImageInputList(
                        imageUris = imageUris,
                        onAddImage = { uri -> if (uri != null) imageUris = imageUris + uri },
                        onRemoveImage = { uri -> imageUris = imageUris.filter { it != uri } },
                        onImagePress = { openViewer(it) },
                        isReadOnly = true
                    )
                    if (!isOwnGallery && galleryData != null) {
                        GalleryItemInfo(
                            data = galleryData!!,
                            likeCount = likeCount,
                            isLiked = isLiked,
                            isLiking = isLiking,
                            likeScale = likeScale.value,
                            onLike = { handleLike() },
                            options = ratingOptions,
                            userRatings = userDetailedRatings,
                            onRatings = { userDetailedRatings = it },
                            onSubmittedCount = { count ->
                                showToast("امتیاز شما در $count بخش با موفقیت ثبت شد", "success")
                            },
                            onSubmittedSingle = { rating ->
                                showToast("امتیاز ${toPersianDigits(jsToString(rating))} ستاره ثبت شد", "success")
                            },
                            onRatingError = { showToast(it.ifEmpty { "خطا در ثبت امتیاز" }, "error") }
                        )
                    }
                }
            }
        }

        Box(
            Modifier
                .align(AbsoluteAlignment.TopRight)
                .zIndex(1000f)
                .offset(x = (-20).dp, y = statusTop + 45.dp - 12.dp)
                .size(50.dp)
                .rnShadow(5.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
                .touchableOpacity { navigation.goBack() },
            contentAlignment = Alignment.Center
        ) {
            MaterialIcons(name = "arrow-forward", size = 26.dp, color = Color(0xFF6366F1))
        }

        if (isOwnGallery && modalVisible) {
            GalleryPickerSheet(
                bottomInset = insets.bottom,
                onDismiss = { modalVisible = false },
                onGallery = {
                    picker.pickImages(
                        onPermissionDenied = {
                            RNAlert.alert("توجه", "متأسفانه، برای این عملیات نیاز به دسترسی گالری داریم!")
                        },
                        onResult = { picked ->
                            val media = picked.firstOrNull() ?: return@pickImages
                            imageUris = imageUris + media.uri
                            modalVisible = false
                        }
                    )
                },
                onCamera = {
                    picker.takePhoto(
                        onPermissionDenied = {
                            RNAlert.alert("توجه", "متأسفانه، برای این عملیات نیاز به دسترسی دوربین داریم!")
                        },
                        onResult = { picked ->
                            val media = picked.firstOrNull() ?: return@takePhoto
                            imageUris = imageUris + media.uri
                            modalVisible = false
                        }
                    )
                }
            )
        }

        if (showAction) {
            GalleryActionSheet(
                backdrop = actionBackdrop.value,
                slide = actionSlide.value,
                onClose = { closeAction() },
                onAdd = { closeAction { selectImageSource() } }
            )
        }

        if (showDelete) {
            GalleryDeleteSheet(
                backdrop = deleteBackdrop.value,
                slide = deleteSlide.value,
                isDeleting = isDeleting,
                onClose = { closeDelete() },
                onConfirm = { confirmDeleteGallery() }
            )
        }

        if (fullScreen) {
            key(viewerSession) {
                GalleryFullscreen(
                    uris = imageUris,
                    initialIndex = currentImageIndex,
                    onIndex = { currentImageIndex = it },
                    onClose = { fullScreen = false }
                )
            }
        }

        Toast(toast.toastVisible, toast.toastMessage, toast.toastType) { toast.setToastVisible(false) }
    }
}

private fun galleryItemRatingOptions(contentReviewList: kotlinx.serialization.json.JsonElement?): List<RatingOption> {
    val items = contentReviewList.list.mapNotNull { it as? JsonObject }
    if (items.isEmpty()) {
        return listOf(
            RatingOption("content", "کیفیت تصاویر", "کیفیت و زیبایی تصاویر"),
            RatingOption("creativity", "خلاقیت", "میزان خلاقیت و ابتکار"),
            RatingOption("composition", "ترکیب‌بندی", "کیفیت ترکیب‌بندی تصاویر"),
            RatingOption("overall", "کیفیت کلی", "ارزیابی کلی گالری")
        )
    }
    return items
        .filter { it["Active"].bool }
        .sortedBy { it["ShowOrder"].double ?: 0.0 }
        .map { item ->
            val reviewId = item["ContentReviewItemId"].int
            RatingOption(
                id = "review_${reviewId ?: item.str("ContentReviewItemId").orEmpty()}",
                title = item.str("Text") ?: "",
                subtitle = "",
                contentReviewItemId = reviewId,
                showOrder = item["ShowOrder"].double,
                averageRating = item["CalculatedAverageRating"].double ?: 0.0
            )
        }
}

@Composable
private fun GalleryItemInfo(
    data: JsonObject,
    likeCount: Int,
    isLiked: Boolean,
    isLiking: Boolean,
    likeScale: Float,
    onLike: () -> Unit,
    options: List<RatingOption>,
    userRatings: Map<String, Double>,
    onRatings: (Map<String, Double>) -> Unit,
    onSubmittedCount: (Int) -> Unit,
    onSubmittedSingle: (Double) -> Unit,
    onRatingError: (String) -> Unit
) {
    val card = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .absolutePadding(left = 15.dp, right = 15.dp, top = 20.dp, bottom = 20.dp)
            .rnShadow(5.dp, card)
            .clip(card)
            .background(Color.White)
            .padding(20.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            RowReverse(
                Modifier
                    .graphicsLayer {
                        scaleX = likeScale
                        scaleY = likeScale
                    }
                    .rnShadow(3.dp, RoundedCornerShape(25.dp))
                    .clip(RoundedCornerShape(25.dp))
                    .background(if (isLiked) GallerySecondary else Color(0xFFF0F0F0))
                    .touchableOpacity(enabled = !isLiking, activeOpacity = 0.7f, onClick = onLike)
                    .absolutePadding(left = 20.dp, right = 20.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MaterialIcons(
                    name = if (isLiked) "favorite" else "favorite-border",
                    size = 24.dp,
                    color = if (isLiked) Color.White else GallerySecondary
                )
                AppText(
                    text = toPersianDigits(likeCount),
                    modifier = Modifier.absolutePadding(right = 8.dp),
                    fontSize = 16.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = if (isLiked) Color.White else GallerySecondary
                )
            }
            RowReverse(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 15.dp)
                    .drawBehind {
                        drawLine(
                            Color(0xFFF0F0F0),
                            Offset(0f, 0f),
                            Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .padding(top = 15.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GalleryStat(
                    "photo",
                    "${toPersianDigits((data.int("ImageCount") ?: 0).toString())} تصویر"
                )
                GalleryStat("calendar-month", toPersianDigits(data.str("ShamsiInsertDate") ?: ""))
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(15.dp))
                .background(Color(0xFFF8F9FA))
                .border(1.dp, Color(0xFFE9ECEF), RoundedCornerShape(15.dp))
                .padding(15.dp)
        ) {
            MultiOptionRatingComponent(
                contentId = data.int("ImageGalleryId"),
                averageRating = data["Rating"].double ?: 0.0,
                ratingCount = data.int("RatingCount") ?: 0,
                initialRating = data["UserRating"].double ?: 0.0,
                initialDetailedRatings = userRatings,
                maxStars = 5,
                size = 24.dp,
                starColor = GalleryGold,
                enableMultipleOptions = true,
                ratingOptions = options,
                modalTitle = "امتیازدهی گالری",
                submitButtonText = "ثبت امتیاز",
                cancelButtonText = "لغو",
                showRatingCount = true,
                showRatingText = true,
                animated = true,
                allowHalfStars = false,
                onRatingSubmitted = { result ->
                    if (result.ratings != null) {
                        onRatings(result.ratings)
                        onSubmittedCount(result.ratings.size)
                    } else if (result.rating != null) {
                        onSubmittedSingle(result.rating)
                    }
                },
                onRatingError = { onRatingError(it) },
                onRatingChange = { _, detailed ->
                    if (detailed != null) onRatings(detailed)
                }
            )

            if (options.isNotEmpty() && userRatings.isNotEmpty()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color(0xFFE8F5E8))
                        .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(15.dp))
                        .padding(15.dp)
                ) {
                    AppText(
                        text = "امتیازات شما:",
                        modifier = Modifier.fillMaxWidth().padding(bottom = 15.dp),
                        fontSize = 16.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color(0xFF2C3E50),
                        textAlign = TextAlign.Right
                    )
                    options.filter { (userRatings[it.id] ?: 0.0) > 0.0 }.forEach { option ->
                        val score = userRatings[option.id] ?: 0.0
                        GalleryRatingRow(
                            title = option.title,
                            score = toPersianDigits(jsToString(score)),
                            scoreColor = GallerySuccess,
                            rating = score,
                            half = false,
                            rowColor = Color.White,
                            rowBorder = Color(0xFFE8F5E8),
                            starGap = 8.dp
                        )
                    }
                }
            }

            if (options.any { it.averageRating > 0.0 }) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 15.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, Color(0xFFF0F0F0), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    AppText(
                        text = "میانگین امتیاز در هر بخش:",
                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        fontSize = 14.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color(0xFF2C3E50),
                        textAlign = TextAlign.Right
                    )
                    options.filter { it.averageRating > 0.0 }.forEach { option ->
                        GalleryRatingRow(
                            title = option.title,
                            score = toPersianDigits(jsToFixed(option.averageRating, 1)),
                            scoreColor = Color(0xFF666666),
                            rating = option.averageRating,
                            half = true,
                            rowColor = Color(0xFFF8F9FA),
                            rowBorder = Color.Transparent,
                            starGap = 0.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryStat(icon: String, text: String) {
    RowReverse(verticalAlignment = Alignment.CenterVertically) {
        MaterialIcons(name = icon, size = 20.dp, color = GalleryInfo)
        AppText(
            text = text,
            modifier = Modifier.absolutePadding(right = 8.dp),
            fontSize = 14.sp,
            fontFamily = RNFonts.Yekan_Bakh_Regular,
            color = Color(0xFF666666)
        )
    }
}

@Composable
private fun GalleryRatingRow(
    title: String,
    score: String,
    scoreColor: Color,
    rating: Double,
    half: Boolean,
    rowColor: Color,
    rowBorder: Color,
    starGap: androidx.compose.ui.unit.Dp
) {
    RowReverse(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(if (half) 8.dp else 12.dp))
            .background(rowColor)
            .border(1.dp, rowBorder, RoundedCornerShape(if (half) 8.dp else 12.dp))
            .padding(if (half) 8.dp else 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            AppText(
                text = title,
                fontSize = if (half) 12.sp else 14.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = Color(0xFF2C3E50),
                textAlign = TextAlign.Right
            )
        }
        RowReverse(
            Modifier.absolutePadding(left = if (half) 10.dp else 15.dp),
            horizontalArrangement = if (starGap > 0.dp) Arrangement.spacedBy(starGap) else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StarDisplay(
                rating = rating,
                maxStars = 5,
                size = 16.dp,
                color = GalleryGold,
                emptyColor = Color(0xFFE0E0E0),
                showHalfStars = half,
                animated = false
            )
            AppText(
                text = score,
                modifier = if (half) Modifier.absolutePadding(right = 4.dp) else Modifier,
                fontSize = if (half) 14.sp else 14.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = scoreColor
            )
        }
    }
}

@Composable
private fun GalleryPickerSheet(
    bottomInset: androidx.compose.ui.unit.Dp,
    onDismiss: () -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .zIndex(20f)
            .background(Color.Black.copy(alpha = 0.6f))
            .touchableOpacity(activeOpacity = 1f, onClick = onDismiss)
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                .background(Color.White)
                .padding(top = 12.dp)
                .absolutePadding(left = 20.dp, right = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            GallerySheetHandle("انتخاب روش")
            Row(
                Modifier
                    .fillMaxWidth()
                    .absolutePadding(left = 10.dp, right = 10.dp)
                    .padding(bottom = 25.dp),
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                GalleryPickerOption(
                    Modifier.weight(1f),
                    "photo-library",
                    Color(0xFF6366F1),
                    "گالری",
                    "انتخاب از تصاویر موجود",
                    onGallery
                )
                GalleryPickerOption(
                    Modifier.weight(1f),
                    "camera-alt",
                    Color(0xFF10B981),
                    "دوربین",
                    "گرفتن عکس جدید",
                    onCamera
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .absolutePadding(left = 5.dp, right = 5.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color(0xFFFEE2E2))
                    .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(15.dp))
                    .touchableOpacity(onClick = onDismiss)
                    .absolutePadding(top = 16.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                AppText(
                    text = "لغو",
                    fontSize = 16.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color(0xFFDC2626),
                    textAlign = TextAlign.Center
                )
            }
            Box(Modifier.fillMaxWidth().height(bottomInset).background(Color.White))
        }
    }
}

@Composable
private fun GalleryPickerOption(
    modifier: Modifier,
    icon: String,
    iconBg: Color,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .clip(shape)
            .background(Color(0xFFF9FAFB))
            .border(1.dp, Color(0xFFE5E7EB), shape)
            .touchableOpacity(activeOpacity = 0.8f, onClick = onClick)
            .absolutePadding(left = 15.dp, right = 15.dp, top = 25.dp, bottom = 25.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(70.dp)
                .rnShadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            MaterialIcons(name = icon, size = 32.dp, color = Color.White)
        }
        AppText(
            text = label,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            fontSize = 16.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = Color(0xFF1F2937),
            textAlign = TextAlign.Center
        )
        AppText(
            text = description,
            fontSize = 12.sp,
            fontFamily = RNFonts.Yekan_Bakh_Regular,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun GallerySheetHandle(title: String) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 25.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .padding(bottom = 15.dp)
                .size(width = 40.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFD1D5DB))
        )
        AppText(
            text = title,
            fontSize = 20.sp,
            fontFamily = RNFonts.Yekan_Bakh_ExtraBold,
            color = Color(0xFF1F2937),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GalleryActionSheet(
    backdrop: Float,
    slide: Float,
    onClose: () -> Unit,
    onAdd: () -> Unit
) {
    Box(Modifier.fillMaxSize().zIndex(21f)) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = backdrop }
                .background(Color.Black.copy(alpha = 0.5f))
                .touchableOpacity(activeOpacity = 1f, onClick = onClose)
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer { translationY = (1f - slide) * 300.dp.toPx() }
                .rnShadow(10.dp, RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                .background(Color.White)
                .padding(top = 15.dp, bottom = 35.dp)
                .absolutePadding(left = 20.dp, right = 20.dp)
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 15.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFD1D5DB))
            )
            AppText(
                text = "عملیات گالری",
                modifier = Modifier.fillMaxWidth().padding(bottom = 25.dp),
                fontSize = 18.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = Color(0xFF2C3E50),
                textAlign = TextAlign.Center
            )
            RowReverse(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color(0xFFF8F9FA))
                    .border(1.dp, Color(0xFFE9ECEF), RoundedCornerShape(15.dp))
                    .touchableOpacity(onClick = onAdd)
                    .absolutePadding(left = 10.dp, right = 10.dp, top = 15.dp, bottom = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .absolutePadding(left = 15.dp)
                        .size(45.dp)
                        .clip(CircleShape)
                        .background(GallerySuccess),
                    contentAlignment = Alignment.Center
                ) {
                    MaterialIcons(name = "add-a-photo", size = 22.dp, color = Color.White)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    AppText(
                        text = "افزودن تصویر",
                        fontSize = 16.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color(0xFF2C3E50),
                        textAlign = TextAlign.Right
                    )
                    AppText(
                        text = "افزودن تصویر جدید به گالری",
                        fontSize = 13.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        color = Color(0xFF6C757D),
                        textAlign = TextAlign.Right
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color(0xFFF8F9FA))
                    .border(1.dp, Color(0xFFE9ECEF), RoundedCornerShape(15.dp))
                    .touchableOpacity(onClick = onClose)
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
private fun GalleryDeleteSheet(
    backdrop: Float,
    slide: Float,
    isDeleting: Boolean,
    onClose: () -> Unit,
    onConfirm: () -> Unit
) {
    Box(Modifier.fillMaxSize().zIndex(22f)) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = backdrop }
                .background(Color.Black.copy(alpha = 0.5f))
                .touchableOpacity(activeOpacity = 1f, onClick = onClose)
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer { translationY = (1f - slide) * 300.dp.toPx() }
                .rnShadow(10.dp, RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                .background(Color.White)
                .padding(top = 15.dp, bottom = 35.dp)
                .absolutePadding(left = 25.dp, right = 25.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .padding(bottom = 15.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFD1D5DB))
            )
            Box(
                Modifier
                    .padding(bottom = 20.dp)
                    .size(70.dp)
                    .rnShadow(8.dp, CircleShape, GalleryError)
                    .clip(CircleShape)
                    .background(GalleryError),
                contentAlignment = Alignment.Center
            ) {
                MaterialIcons(name = "warning", size = 32.dp, color = Color.White)
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
                text = "آیا از حذف این گالری اطمینان دارید؟\nاین عمل قابل بازگشت نیست.",
                modifier = Modifier.padding(bottom = 30.dp),
                fontSize = 16.sp,
                fontFamily = RNFonts.Yekan_Bakh_Regular,
                color = Color(0xFF6C757D),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            RowReverse(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color(0xFFF8F9FA))
                        .border(1.dp, Color(0xFFE9ECEF), RoundedCornerShape(15.dp))
                        .touchableOpacity(enabled = !isDeleting, onClick = onClose)
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
                        .rnShadow(8.dp, RoundedCornerShape(15.dp), GalleryError)
                        .clip(RoundedCornerShape(15.dp))
                        .touchableOpacity(enabled = !isDeleting, onClick = onConfirm)
                ) {
                    LinearGradient(
                        colors = listOf(GalleryError, Color(0xFFC0392B)),
                        modifier = Modifier.fillMaxWidth(),
                        start = Offset(0f, 0f),
                        end = Offset(1f, 1f)
                    ) {
                        RowReverse(
                            Modifier.fillMaxWidth().absolutePadding(left = 20.dp, right = 20.dp, top = 16.dp, bottom = 16.dp),
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

@Composable
private fun GalleryFullscreen(
    uris: List<String>,
    initialIndex: Int,
    onIndex: (Int) -> Unit,
    onClose: () -> Unit
) {
    val pager = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (uris.size - 1).coerceAtLeast(0)),
        pageCount = { uris.size.coerceAtLeast(1) }
    )
    LaunchedEffect(pager) {
        delay(150)
        if (uris.isNotEmpty()) {
            pager.scrollToPage(initialIndex.coerceIn(0, uris.lastIndex))
        }
    }
    LaunchedEffect(pager) {
        androidx.compose.runtime.snapshotFlow { pager.currentPage }.collect { onIndex(it) }
    }
    Box(
        Modifier
            .fillMaxSize()
            .zIndex(30f)
            .background(Color.Black.copy(alpha = 0.9f))
    ) {
        if (uris.isNotEmpty()) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    NetworkImage(
                        uris.getOrNull(page),
                        Modifier.fillMaxSize(),
                        resizeMode = ResizeMode.contain
                    )
                }
            }
        }
        Box(
            Modifier
                .align(AbsoluteAlignment.TopRight)
                .zIndex(1f)
                .offset(x = (-20).dp, y = 50.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .touchableOpacity(onClick = onClose)
                .padding(10.dp)
        ) {
            MaterialIcons(name = "close", size = 30.dp, color = Color.White)
        }
    }
}
