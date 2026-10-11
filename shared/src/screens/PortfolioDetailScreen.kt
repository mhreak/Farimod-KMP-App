package ir.farimod.app.screens

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.navigation.useFocusEffect
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.navigation.useRoute
import ir.farimod.app.network.Api
import ir.farimod.app.network.ApiNetworkException
import ir.farimod.app.network.str
import ir.farimod.app.screens.portfolio.PortfolioCircleButton
import ir.farimod.app.screens.portfolio.PortfolioIndigo
import ir.farimod.app.screens.portfolio.PortfolioPageBg
import ir.farimod.app.screens.portfoliodetail.DetailDark
import ir.farimod.app.screens.portfoliodetail.DetailPink
import ir.farimod.app.screens.portfoliodetail.DetailPrimary
import ir.farimod.app.screens.portfoliodetail.PortfolioActionSheet
import ir.farimod.app.screens.portfoliodetail.PortfolioDeleteSheet
import ir.farimod.app.screens.portfoliodetail.PortfolioDesignerCard
import ir.farimod.app.screens.portfoliodetail.PortfolioDetailEmpty
import ir.farimod.app.screens.portfoliodetail.PortfolioDetailError
import ir.farimod.app.screens.portfoliodetail.PortfolioDetailModel
import ir.farimod.app.screens.portfoliodetail.PortfolioDetailSkeleton
import ir.farimod.app.screens.portfoliodetail.PortfolioGallery
import ir.farimod.app.screens.portfoliodetail.PortfolioImageDialog
import ir.farimod.app.screens.portfoliodetail.PortfolioMainCard
import ir.farimod.app.screens.portfoliodetail.PortfolioRatingBlock
import ir.farimod.app.screens.portfoliodetail.PortfolioReviewBlock
import ir.farimod.app.screens.portfoliodetail.parsePortfolioDetail
import ir.farimod.app.screens.portfoliodetail.transformContentReview
import ir.farimod.app.state.AuthState
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.MainBackground
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RatingOption
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.ToastData
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.ui.windowSize
import ir.farimod.app.utils.jsNumberToString
import ir.farimod.app.utils.jsTruthy
import ir.farimod.app.utils.toPersianDigits
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioDetailScreen() {
    val navigation = useNavigation()
    val route = useRoute()
    val scope = rememberCoroutineScope()
    val user = AuthState.user
    val routePortfolioId = route.int("portfolioId")
    val statusTop = statusBarHeight()
    val (screenW, screenH) = windowSize()
    val density = LocalDensity.current

    var portfolio by remember { mutableStateOf<PortfolioDetailModel?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf(ToastData()) }
    var refreshing by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var showActionModal by remember { mutableStateOf(false) }
    var showDeleteModal by remember { mutableStateOf(false) }
    var showImageModal by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableStateOf(0) }
    var errorImages by remember { mutableStateOf(setOf<Int>()) }
    var userRatings by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var ratingOptions by remember { mutableStateOf<List<RatingOption>>(emptyList()) }
    var likeCount by remember { mutableStateOf(0) }
    var isLiked by remember { mutableStateOf(false) }
    var isLiking by remember { mutableStateOf(false) }

    val actionSlide = remember { Animatable(0f) }
    val actionBackdrop = remember { Animatable(0f) }
    val deleteSlide = remember { Animatable(0f) }
    val deleteBackdrop = remember { Animatable(0f) }
    val heart = remember { Animatable(0f) }

    fun showToast(message: String, type: String = "info") {
        toast = ToastData(true, message, type)
    }

    suspend fun fetchPortfolio(id: Int) {
        loading = true
        error = null
        try {
            val memberId = AuthState.user?.MemberId
                ?: throw IllegalStateException("Cannot read properties of null (reading 'MemberId')")
            val response = try {
                Api.get(
                    "Portfolio/Get",
                    query = mapOf("portfolioId" to id, "currentMemberId" to memberId)
                )
            } catch (e: ApiNetworkException) {
                throw IllegalStateException(e.message ?: "Network request failed", e)
            }
            if (!response.ok) throw IllegalStateException("HTTP error! status: ${response.status}")
            val result = response.json ?: throw IllegalStateException("HTTP error! status: ${response.status}")
            val parsed = parsePortfolioDetail(result)
            portfolio = parsed
            likeCount = parsed.likeCount
            isLiked = parsed.isMemberLiked
            errorImages = emptySet()
            ratingOptions = transformContentReview(parsed.contentReviewItems)
            userRatings = parsed.userDetailedRatings
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message
            portfolio = null
        } finally {
            loading = false
        }
    }

    fun requestId(): Int = if (jsTruthy(routePortfolioId)) routePortfolioId ?: 1 else 1

    useFocusEffect(routePortfolioId) {
        loading = true
        portfolio = null
        userRatings = emptyMap()
        ratingOptions = emptyList()
        likeCount = 0
        isLiked = false
        errorImages = emptySet()
        fetchPortfolio(requestId())
    }

    androidx.compose.runtime.LaunchedEffect(error) {
        if (error != null) showToast("خطا در دریافت اطلاعات نمونه کار. لطفاً دوباره تلاش کنید.", "error")
    }

    suspend fun openAction() {
        actionSlide.snapTo(0f)
        actionBackdrop.snapTo(0f)
        showActionModal = true
        coroutineScope {
            launch { actionBackdrop.animateTo(1f, tween(300)) }
            launch { actionSlide.animateTo(1f, tween(300)) }
        }
    }

    suspend fun closeAction() {
        coroutineScope {
            launch { actionBackdrop.animateTo(0f, tween(250)) }
            launch { actionSlide.animateTo(0f, tween(250)) }
        }
        showActionModal = false
    }

    suspend fun openDelete() {
        deleteSlide.snapTo(0f)
        deleteBackdrop.snapTo(0f)
        showDeleteModal = true
        coroutineScope {
            launch { deleteBackdrop.animateTo(1f, tween(300)) }
            launch { deleteSlide.animateTo(1f, tween(300)) }
        }
    }

    suspend fun closeDelete() {
        coroutineScope {
            launch { deleteBackdrop.animateTo(0f, tween(250)) }
            launch { deleteSlide.animateTo(0f, tween(250)) }
        }
        showDeleteModal = false
    }

    fun handleLike() {
        val current = portfolio ?: return
        val memberId = user?.MemberId ?: return
        if (isLiking) return
        isLiking = true
        val prevLiked = isLiked
        val prevCount = likeCount
        val nextLiked = !isLiked
        isLiked = nextLiked
        likeCount = prevCount + if (nextLiked) 1 else -1
        scope.launch {
            heart.animateTo(0.3f, tween(100))
            heart.animateTo(1f, tween(300))
        }
        if (nextLiked) {
            scope.launch {
                delay(200)
                heart.animateTo(0.8f, tween(200))
                heart.animateTo(0f, tween(600))
            }
        } else {
            scope.launch { heart.animateTo(0f, tween(100)) }
        }
        scope.launch {
            try {
                val likeId = when {
                    jsTruthy(routePortfolioId) -> routePortfolioId ?: 1
                    current.portfolioId != 0 -> current.portfolioId
                    else -> 1
                }
                val response = try {
                    Api.post("Portfolio/Like?id=$likeId&memberId=$memberId")
                } catch (e: ApiNetworkException) {
                    throw IllegalStateException(e.message ?: "Network request failed", e)
                }
                if (!response.ok) throw IllegalStateException("HTTP error! status: ${response.status}")
            } catch (e: CancellationException) {
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
        scope.launch {
            closeDelete()
            if (!jsTruthy(routePortfolioId)) {
                showToast("شناسه نمونه کار معتبر نیست", "error")
                return@launch
            }
            isDeleting = true
            try {
                val response = try {
                    Api.delete("Portfolio/Delete", query = mapOf("portfolioId" to routePortfolioId))
                } catch (e: ApiNetworkException) {
                    throw Exception(e.message ?: "خطا در حذف نمونه کار", e)
                }
                if (response.ok) {
                    if (response.text.isNotBlank() && response.json == null) throw Exception(response.text)
                    val message = response.json.str("Message")?.takeIf { it.isNotEmpty() }
                        ?: "نمونه کار با موفقیت حذف شد"
                    showToast(message, "success")
                    navigation.navigate("PortfolioList")
                } else {
                    val message = response.json.str("Message")?.takeIf { it.isNotEmpty() }
                        ?: "خطای سرور (${response.status})"
                    throw Exception(message)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(e.message?.takeIf { it.isNotEmpty() } ?: "خطا در حذف نمونه کار", "error")
            } finally {
                isDeleting = false
            }
        }
    }

    val current = portfolio
    val isOwn = current != null && current.memberId != null && current.memberId == user?.MemberId
    val workingImages = current?.images?.filterIndexed { index, _ -> index !in errorImages }.orEmpty()
    val showDefaultGallery = current != null && current.images.isNotEmpty() && workingImages.isEmpty()

    Box(Modifier.fillMaxSize().background(PortfolioPageBg)) {
        MainBackground()
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = statusTop + 38.dp)
                    .absolutePadding(left = 20.dp, right = 20.dp)
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                AppText(
                    text = "نمونه کار",
                    modifier = Modifier.fillMaxWidth().absolutePadding(left = 56.dp, right = 56.dp),
                    fontSize = 22.sp,
                    fontFamily = RNFonts.Yekan_Bakh_ExtraBold,
                    color = DetailDark,
                    textAlign = TextAlign.Center
                ,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
            val pullState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = {
                    scope.launch {
                        refreshing = true
                        fetchPortfolio(requestId())
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
                        color = DetailPrimary
                    )
                }
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 20.dp)
                ) {
                    when {
                        loading -> PortfolioDetailSkeleton()
                        error != null -> PortfolioDetailError { scope.launch { fetchPortfolio(requestId()) } }
                        current == null -> PortfolioDetailEmpty()
                        else -> {
                            val shown = current!!
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .absolutePadding(left = 20.dp, right = 20.dp)
                                    .padding(bottom = 60.dp)
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 20.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.White.copy(alpha = 0.8f))
                                ) {
                                    PortfolioGallery(
                                        images = shown.images,
                                        errorIndexes = errorImages,
                                        liked = isLiked,
                                        likeCount = likeCount,
                                        liking = isLiking,
                                        onLike = { handleLike() },
                                        onImageError = { index -> errorImages = errorImages + index },
                                        onImageOpen = { index ->
                                            selectedImageIndex = index
                                            showImageModal = true
                                        }
                                    )
                                }
                                PortfolioDesignerCard(shown.memberName)
                                PortfolioMainCard(shown.title, shown.description, shown.shamsiInsertDate)
                                PortfolioRatingBlock(
                                    portfolio = shown,
                                    options = ratingOptions,
                                    userRatings = userRatings,
                                    onSubmitted = { result ->
                                        val ratings = result.ratings
                                        if (ratings != null) {
                                            val average = result.averageRating ?: 0.0
                                            userRatings = ratings
                                            portfolio = shown.withRating(average, ratings)
                                            showToast("امتیاز شما در ${ratings.size} بخش با موفقیت ثبت شد", "success")
                                        } else {
                                            val rating = result.rating ?: 0.0
                                            portfolio = shown.copy(userRating = rating)
                                            showToast(
                                                "امتیاز ${toPersianDigits(jsNumberToString(rating))} ستاره ثبت شد",
                                                "success"
                                            )
                                        }
                                    },
                                    onError = { message ->
                                        showToast(message.ifEmpty { "خطا در ثبت امتیاز" }, "error")
                                    },
                                    onChange = { _, detailed ->
                                        if (detailed != null) userRatings = detailed
                                    }
                                )
                                PortfolioReviewBlock(shown.contentReviewItems)
                                Box(Modifier.height(40.dp))
                            }
                        }
                    }
                }
            }
        }

        val heartV = heart.value
        Box(
            Modifier
                .zIndex(1000f)
                .align(AbsoluteAlignment.TopLeft)
                .absoluteOffset(x = screenW * 0.5f - 30.dp, y = screenH * 0.4f)
                .graphicsLayer {
                    alpha = heartV
                    translationY = with(density) { (heartV * -200f).dp.toPx() }
                    val scale = heartScale(heartV)
                    scaleX = scale
                    scaleY = scale
                    rotationZ = heartV * 25f
                }
        ) {
            LinearGradient(
                colors = listOf(DetailPink, Color(0xFFE91E63)),
                modifier = Modifier.size(60.dp).rnShadow(10.dp, CircleShape, DetailPink),
                shape = CircleShape
            ) {
                MaterialIcons("favorite", size = 40.dp, color = Color.White, modifier = Modifier.align(Alignment.Center))
            }
        }

        PortfolioCircleButton(
            modifier = Modifier
                .zIndex(1000f)
                .align(AbsoluteAlignment.TopRight)
                .absoluteOffset(x = (-20).dp, y = statusTop + 38.dp),
            size = 48.dp,
            elevation = 8.dp,
            onClick = { navigation.goBack() }
        ) {
            LinearGradient(
                colors = listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.9f)),
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape
            ) {
                MaterialIcons("arrow-forward", size = 24.dp, color = PortfolioIndigo, modifier = Modifier.align(Alignment.Center))
            }
        }

        if (isOwn && !loading) {
            PortfolioCircleButton(
                modifier = Modifier
                    .zIndex(1000f)
                    .align(AbsoluteAlignment.TopLeft)
                    .absoluteOffset(x = 20.dp, y = statusTop + 38.dp),
                size = 48.dp,
                elevation = 8.dp,
                onClick = { if (!isDeleting) scope.launch { openAction() } }
            ) {
                LinearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.9f)),
                    modifier = Modifier.fillMaxSize(),
                    shape = CircleShape
                ) {
                    MaterialIcons("more-vert", size = 24.dp, color = PortfolioIndigo, modifier = Modifier.align(Alignment.Center))
                }
            }
        }

        PortfolioActionSheet(
            visible = showActionModal,
            slide = actionSlide.value,
            backdrop = actionBackdrop.value,
            deleting = isDeleting,
            onClose = { scope.launch { closeAction() } },
            onEdit = {
                val data = portfolio ?: return@PortfolioActionSheet
                navigation.navigate(
                    "AddPortfolio",
                    mapOf("isEdit" to true, "portfolioData" to data.toRouteJson())
                )
            },
            onDelete = {
                scope.launch {
                    closeAction()
                    delay(300)
                    openDelete()
                }
            }
        )
        PortfolioDeleteSheet(
            visible = showDeleteModal,
            slide = deleteSlide.value,
            backdrop = deleteBackdrop.value,
            deleting = isDeleting,
            onClose = { scope.launch { closeDelete() } },
            onConfirm = { confirmDelete() }
        )
        PortfolioImageDialog(
            visible = showImageModal,
            images = workingImages,
            showDefault = showDefaultGallery || workingImages.isEmpty(),
            initialIndex = selectedImageIndex,
            onClose = { showImageModal = false }
        )

        Toast(toast.visible, toast.message, toast.type) { toast = toast.copy(visible = false) }
    }
}

private fun heartScale(v: Float): Float = when {
    v <= 0f -> 0.5f
    v < 0.3f -> lerp(0.5f, 2f, v / 0.3f)
    v < 0.7f -> lerp(2f, 1.8f, (v - 0.3f) / 0.4f)
    v < 1f -> lerp(1.8f, 0.3f, (v - 0.7f) / 0.3f)
    else -> 0.3f
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
