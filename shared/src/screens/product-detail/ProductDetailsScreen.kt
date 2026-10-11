package ir.farimod.app.screens

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
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
import ir.farimod.app.network.bool
import ir.farimod.app.network.double
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.list
import ir.farimod.app.network.str
import ir.farimod.app.network.with
import ir.farimod.app.screens.productdetail.PdDark
import ir.farimod.app.screens.productdetail.PdError
import ir.farimod.app.screens.productdetail.PdGold
import ir.farimod.app.screens.productdetail.PdInfo
import ir.farimod.app.screens.productdetail.PdLike
import ir.farimod.app.screens.productdetail.PdPrimary
import ir.farimod.app.screens.productdetail.PdSuccess
import ir.farimod.app.screens.productdetail.PdSurface
import ir.farimod.app.screens.productdetail.PdWarning
import ir.farimod.app.screens.productdetail.ProductActionSheet
import ir.farimod.app.screens.productdetail.ProductDeleteSheet
import ir.farimod.app.screens.productdetail.ProductDetailBackButton
import ir.farimod.app.screens.productdetail.ProductDetailImage
import ir.farimod.app.screens.productdetail.ProductDetailMenuButton
import ir.farimod.app.screens.productdetail.ProductDetailsSkeleton
import ir.farimod.app.screens.productdetail.ProductGalleryHeader
import ir.farimod.app.screens.productdetail.ProductSmartDetail
import ir.farimod.app.screens.productdetail.ProductThumbnailRow
import ir.farimod.app.screens.productdetail.productGalleryUrls
import ir.farimod.app.screens.productdetail.transformContentReviewToRatingOptions
import ir.farimod.app.state.AuthState
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.MainBackground
import ir.farimod.app.ui.MultiOptionRatingComponent
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.ResizeMode
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.StarDisplay
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.ui.windowSize
import ir.farimod.app.utils.formatPrice
import ir.farimod.app.utils.jsToFixed
import ir.farimod.app.utils.jsToString
import ir.farimod.app.utils.jsTruthy
import ir.farimod.app.utils.safeNumber
import ir.farimod.app.utils.safeString
import ir.farimod.app.utils.toPersianDigits
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProductDetailsScreen() {
    val navigation = useNavigation()
    val route = useRoute()
    val scope = rememberCoroutineScope()
    val toast = useToast()
    val user = AuthState.user
    val memberId = user?.MemberId?.takeIf { it != 0 }
    val productId = resolveProductId(route)
    val productIdKey = productId?.let { jsToString(it) } ?: ""

    var productData by remember { mutableStateOf<JsonObject?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var currentImageIndex by remember { mutableIntStateOf(0) }
    var likeCount by remember { mutableIntStateOf(0) }
    var isLiked by remember { mutableStateOf(false) }
    var isLiking by remember { mutableStateOf(false) }
    var userDetailedRatings by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var isDeleting by remember { mutableStateOf(false) }
    var showActionModal by remember { mutableStateOf(false) }
    var showDeleteModal by remember { mutableStateOf(false) }
    var imageModalVisible by remember { mutableStateOf(false) }
    var modalImageIndex by remember { mutableIntStateOf(0) }
    val heart = remember { Animatable(0f) }
    val pullState = rememberPullToRefreshState()

    suspend fun fetchProductDetails(id: Any?) {
        if (id == null) return
        loading = true
        error = null
        try {
            val response = Api.get(
                "Product/Get",
                query = mapOf(
                    "productId" to jsToString(id),
                    "currentMemberId" to (memberId?.toString() ?: "null")
                )
            )
            if (!response.ok) throw Exception("HTTP error! status: ${response.status}")
            val result = response.json ?: throw Exception("HTTP error! status: ${response.status}")
            val product = (result["Product"] as? JsonObject) ?: JsonObject(emptyMap())
            productData = product.with(
                "IsMemberLiked" to result.bool("IsMemberLiked"),
                "ContentReviewItemList" to (result["ContentReviewItemList"] ?: JsonArray(emptyList())),
                "LikeCount" to (product.int("LikeCount") ?: 0),
                "AverageRating" to (if (jsTruthy(product["Rating"])) safeNumber(product["Rating"]) else 0.0),
                "RatingCount" to 0,
                "UserRating" to 0.0
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message
            productData = null
        } finally {
            loading = false
        }
    }

    useFocusEffect(productIdKey) {
        if (productId != null) fetchProductDetails(productId)
    }

    LaunchedEffect(productData) {
        val data = productData ?: return@LaunchedEffect
        likeCount = data.int("LikeCount") ?: 0
        isLiked = data.bool("IsMemberLiked")
        currentImageIndex = 0
    }

    LaunchedEffect(error) {
        if (error != null) toast.showToast("خطا در دریافت اطلاعات محصول. لطفاً دوباره تلاش کنید.", "error")
    }

    if (loading || (productData == null && productId != null && error == null)) {
        ProductDetailsSkeleton(onBack = { navigation.goBack() })
        return
    }
    if (error != null && productData == null) {
        ProductMessageState(
            icon = "error",
            title = "خطا در دریافت اطلاعات",
            subtitle = "لطفاً اتصال اینترنت خود را بررسی کنید",
            onBack = { navigation.goBack() },
            onRetry = { scope.launch { fetchProductDetails(productId) } }
        )
        return
    }
    if (productData == null) {
        ProductMessageState(
            icon = "info",
            title = "محصول یافت نشد",
            subtitle = "اطلاعات محصول مورد نظر موجود نیست",
            onBack = { navigation.goBack() }
        )
        return
    }

    val product = productData!!
    val ratingOptions = transformContentReviewToRatingOptions(product.list("ContentReviewItemList"))
    val price = safeNumber(product["Price"])
    val special = safeNumber(product["SpecialSalePrice"])
    val discount = if (special > 0 && price > 0) ((price - special) / price * 100.0).roundToInt() else 0
    val urls = productGalleryUrls(product)
    val own = memberId != null && product.double("MemberId") == memberId.toDouble()
    val active = product.bool("Active")
    val statusTop = statusBarHeight()
    val (screenW, screenH) = windowSize()

    fun playLikeAnim(nowLiked: Boolean) {
        scope.launch {
            heart.animateTo(0.3f, tween(100))
            heart.animateTo(1f, tween(300))
        }
        if (nowLiked) {
            scope.launch {
                delay(200)
                heart.animateTo(0.8f, tween(200))
                heart.animateTo(0f, tween(600))
            }
        } else {
            scope.launch { heart.animateTo(0f, tween(100)) }
        }
    }

    fun handleLike() {
        if (isLiking || memberId == null) return
        val previousLiked = isLiked
        val previousCount = likeCount
        val nextLiked = !isLiked
        isLiking = true
        isLiked = nextLiked
        likeCount = previousCount + if (nextLiked) 1 else -1
        playLikeAnim(nextLiked)
        scope.launch {
            try {
                val id = productId ?: product["ProductId"]
                val response = Api.post(
                    "Product/Like",
                    query = mapOf("id" to jsToString(id), "memberId" to memberId)
                )
                if (!response.ok) toast.showToast("خطا در لایک محصول")
                if (response.json == null) {
                    throw Exception(if (response.text.isBlank()) "JSON Parse error: Unexpected end of input" else "JSON Parse error: Unexpected character")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                isLiked = previousLiked
                likeCount = previousCount
            } finally {
                isLiking = false
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        MainBackground()
        Box(
            Modifier
                .zIndex(1000f)
                .absoluteOffset(x = screenW / 2 - 25.dp, y = screenH * 0.4f)
                .graphicsLayer {
                    val t = heart.value
                    alpha = t
                    translationY = t * (-150).dp.toPx()
                    val s = heartScale(t)
                    scaleX = s
                    scaleY = s
                    rotationZ = t * 15f
                }
        ) {
            MaterialIcons("favorite", size = 50.dp, color = PdLike)
        }

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                scope.launch {
                    refreshing = true
                    fetchProductDetails(productId)
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
                    color = PdPrimary
                )
            }
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Box(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = statusTop + 35.dp)
                                .absolutePadding(left = 20.dp, right = 20.dp)
                                .padding(bottom = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AppText(
                                text = "جزئیات محصول",
                                modifier = Modifier.fillMaxWidth().absolutePadding(left = 56.dp, right = 56.dp),
                                fontSize = 22.sp,
                                fontFamily = RNFonts.Yekan_Bakh_ExtraBold,
                                color = PdDark,
                                textAlign = TextAlign.Center
                            ,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                        }
                        ProductGalleryHeader(
                            urls = urls,
                            productName = toPersianDigits(safeString(product["ProductName"], "نام محصول مشخص نشده")),
                            active = active,
                            currentIndex = currentImageIndex,
                            onIndexChange = { currentImageIndex = it },
                            onImagePress = {
                                modalImageIndex = it
                                imageModalVisible = true
                            },
                            overlay = {
                                if (discount > 0) {
                                    Box(
                                        Modifier
                                            .align(AbsoluteAlignment.TopRight)
                                            .zIndex(10f)
                                            .absolutePadding(top = 15.dp, right = 15.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFFFF6B6B))
                                            .absolutePadding(left = 12.dp, right = 12.dp, top = 6.dp, bottom = 6.dp)
                                    ) {
                                        AppText(
                                            text = "${toPersianDigits(discount.toString())}% تخفیف",
                                            fontSize = 12.sp,
                                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                                if (own) {
                                    Box(
                                        Modifier
                                            .align(AbsoluteAlignment.TopRight)
                                            .zIndex(10f)
                                            .absolutePadding(top = if (discount > 0) 60.dp else 15.dp, right = 15.dp)
                                            .rnShadow(4.dp, RoundedCornerShape(15.dp))
                                            .clip(RoundedCornerShape(15.dp))
                                            .background(if (active) PdSuccess else PdWarning)
                                            .absolutePadding(left = 12.dp, right = 12.dp, top = 6.dp, bottom = 6.dp)
                                    ) {
                                        AppText(
                                            text = if (active) "منتشر شده" else "پیش‌نویس",
                                            fontSize = 12.sp,
                                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                                Box(
                                    Modifier
                                        .align(AbsoluteAlignment.TopLeft)
                                        .zIndex(10f)
                                        .absolutePadding(top = 15.dp, left = 15.dp)
                                        .rnShadow(4.dp, RoundedCornerShape(20.dp))
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isLiked) Color(0.914f, 0.118f, 0.388f, 0.8f) else Color(1f, 1f, 1f, 0.8f))
                                        .touchableOpacity(enabled = !isLiking, activeOpacity = 0.7f) { handleLike() }
                                        .absolutePadding(left = 12.dp, right = 12.dp, top = 8.dp, bottom = 8.dp)
                                ) {
                                    RowReverse(verticalAlignment = Alignment.CenterVertically) {
                                        MaterialIcons(
                                            if (isLiked) "favorite" else "favorite-border",
                                            size = 18.dp,
                                            color = if (isLiked) Color.White else Color(0xFFE91E63)
                                        )
                                        AppText(
                                            text = toPersianDigits(likeCount.toString()),
                                            modifier = Modifier.absolutePadding(right = 6.dp),
                                            fontSize = 15.sp,
                                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                                            color = if (isLiked) Color.White else Color(0xFFE91E63)
                                        )
                                    }
                                }
                            }
                        )
                        if (urls.size > 1) {
                            ProductThumbnailRow(urls, currentImageIndex) { currentImageIndex = it }
                        }
                        Row(
                            Modifier
                                .absolutePadding(left = 20.dp, right = 20.dp)
                                .padding(bottom = 20.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(1f, 1f, 1f, 0.9f))
                                .border(1.dp, Color(0.796f, 0.835f, 0.882f, 0.3f), RoundedCornerShape(20.dp))
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                if (discount > 0) {
                                    Column(
                                        Modifier.padding(top = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        AppText(
                                            text = formatPrice(safeNumber(product["Price"])),
                                            modifier = Modifier.padding(bottom = 4.dp),
                                            fontSize = 16.sp,
                                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                                            color = Color(0xFFA1A1A1),
                                            textAlign = TextAlign.Center,
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                        )
                                        AppText(
                                            text = formatPrice(safeNumber(product["SpecialSalePrice"])),
                                            fontSize = 20.sp,
                                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                                            color = Color(0xFFFF6B6B),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                } else {
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(15.dp))
                                            .background(Color(1f, 1f, 1f, 0.1f))
                                            .border(1.dp, Color(1f, 1f, 1f, 0.2f), RoundedCornerShape(15.dp))
                                            .absolutePadding(left = 15.dp, right = 15.dp, top = 8.dp, bottom = 8.dp)
                                    ) {
                                        AppText(
                                            text = formatPrice(safeNumber(product["Price"])),
                                            fontSize = 22.sp,
                                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                                            color = Color(0xFF2ECC71),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
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
                                modifier = Modifier
                                    .absolutePadding(left = 15.dp)
                                    .size(50.dp)
                                    .rnShadow(15.dp, CircleShape),
                                shape = CircleShape
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    MaterialIcons("info", size = 26.dp, color = PdSurface)
                                }
                            }
                            AppText(
                                text = "مشخصات محصول",
                                fontSize = 24.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Bold,
                                color = PdDark
                            )
                        }

                        Column(Modifier.absolutePadding(left = 20.dp, right = 20.dp)) {
                            ProductSmartDetail(
                                label = "درباره این محصول",
                                value = safeString(product["Description"]),
                                icon = "description",
                                maxLength = 50
                            )
                            ProductSmartDetail(
                                label = "فروشنده",
                                value = safeString(product["MemberName"]),
                                icon = "person"
                            )
                            ProductSmartDetail(
                                label = "دسته‌بندی",
                                value = safeString(product["ProductCategories"]).ifEmpty { "عمومی" },
                                icon = "category"
                            )
                            ProductSmartDetail(
                                label = "وضعیت",
                                value = if (active) "فعال" else "غیرفعال",
                                icon = if (active) "check-circle" else "cancel"
                            )
                            ProductRatingBlock(
                                product = product,
                                options = ratingOptions,
                                userRatings = userDetailedRatings,
                                onUserRatings = { userDetailedRatings = it },
                                onProduct = { productData = it },
                                onSuccess = { toast.showToast(it, "success") },
                                onError = { toast.showToast(it.ifEmpty { "خطا در ثبت امتیاز" }, "error") }
                            )
                        }
                        Spacer(Modifier.height(30.dp))
                    }
                    ProductDetailBackButton(
                        modifier = Modifier
                            .align(AbsoluteAlignment.TopRight)
                            .zIndex(1000f)
                            .absoluteOffset(x = (-20).dp, y = statusTop + 45.dp),
                        onClick = { navigation.goBack() }
                    )
                    if (own) {
                        ProductDetailMenuButton(
                            modifier = Modifier
                                .align(AbsoluteAlignment.TopLeft)
                                .zIndex(1000f)
                                .absoluteOffset(x = 20.dp, y = statusTop + 45.dp)
                                .offset(y = (-12).dp),
                            enabled = !isDeleting,
                            onClick = { showActionModal = true }
                        )
                    }
                }
            }
        }

        if (showActionModal) {
            ProductActionSheet(
                isDeleting = isDeleting,
                onDismiss = { showActionModal = false },
                onEdit = {
                    scope.launch {
                        delay(300)
                        navigation.navigate(
                            "AddProduct",
                            mapOf("isEdit" to true, "productData" to product)
                        )
                    }
                },
                onDelete = {
                    scope.launch {
                        delay(300)
                        showDeleteModal = true
                    }
                }
            )
        }
        if (showDeleteModal) {
            ProductDeleteSheet(
                isDeleting = isDeleting,
                onDismiss = { showDeleteModal = false },
                onConfirm = {
                    showDeleteModal = false
                    scope.launch {
                        isDeleting = true
                        try {
                            val response = Api.delete(
                                "Product/Delete",
                                query = mapOf("productId" to jsToString(product["ProductId"]))
                            )
                            if (response.ok) {
                                toast.showToast("محصول با موفقیت حذف شد", "success")
                                navigation.navigate("MyProduct")
                            } else {
                                val message = response.json.str("Message")?.takeIf { jsTruthy(it) }
                                    ?: "خطا در حذف محصول"
                                throw Exception(message)
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            toast.showToast(e.message?.takeIf { it.isNotEmpty() } ?: "خطا در حذف محصول", "error")
                        } finally {
                            isDeleting = false
                        }
                    }
                }
            )
        }
        if (imageModalVisible) {
            ProductImageDialog(
                urls = urls,
                startIndex = modalImageIndex,
                onClose = { imageModalVisible = false }
            )
        }
        Toast(toast.toastVisible, toast.toastMessage, toast.toastType) { toast.setToastVisible(false) }
    }
}

private fun resolveProductId(route: ir.farimod.app.navigation.Route): Any? {
    val direct = route.params["productId"]
    if (jsTruthy(direct)) return direct
    val fromData = (route.params["productData"] as? kotlinx.serialization.json.JsonElement)?.get("ProductId")
    if (jsTruthy(fromData)) return fromData
    val fromProduct = (route.params["product"] as? kotlinx.serialization.json.JsonElement)?.get("ProductId")
    if (jsTruthy(fromProduct)) return fromProduct
    val id = route.params["id"]
    if (jsTruthy(id)) return id
    return null
}

private fun heartScale(t: Float): Float = when {
    t <= 0.3f -> lerp(0.5f, 1.8f, if (t <= 0f) 0f else t / 0.3f)
    t <= 0.7f -> lerp(1.8f, 1.5f, (t - 0.3f) / 0.4f)
    else -> lerp(1.5f, 0.3f, ((t - 0.7f) / 0.3f).coerceIn(0f, 1f))
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

@Composable
private fun ProductMessageState(
    icon: String,
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    val statusTop = statusBarHeight()
    Box(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        MainBackground()
        Column(
            Modifier
                .fillMaxSize()
                .absolutePadding(left = 40.dp, right = 40.dp, top = 60.dp, bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            MaterialIcons(icon, size = 80.dp, color = Color(0xFF9E9E9E))
            AppText(
                text = title,
                modifier = Modifier.padding(top = 20.dp),
                fontSize = 20.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = PdDark,
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
            if (onRetry != null) {
                RowReverse(
                    Modifier
                        .padding(top = 24.dp)
                        .rnShadow(8.dp, RoundedCornerShape(25.dp), PdPrimary)
                        .clip(RoundedCornerShape(25.dp))
                        .background(PdPrimary)
                        .touchableOpacity { onRetry() }
                        .absolutePadding(left = 20.dp, right = 20.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MaterialIcons("refresh", size = 20.dp, color = Color.White)
                    AppText(
                        text = "تلاش مجدد",
                        modifier = Modifier.absolutePadding(right = 8.dp),
                        fontSize = 16.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color.White
                    )
                }
            }
        }
        ProductDetailBackButton(
            modifier = Modifier
                .align(AbsoluteAlignment.TopRight)
                .zIndex(1000f)
                .absoluteOffset(x = (-20).dp, y = statusTop + 45.dp),
            onClick = onBack
        )
    }
}

@Composable
private fun ProductRatingBlock(
    product: JsonObject,
    options: List<ir.farimod.app.ui.RatingOption>,
    userRatings: Map<String, Double>,
    onUserRatings: (Map<String, Double>) -> Unit,
    onProduct: (JsonObject) -> Unit,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    Box(
        Modifier
            .absolutePadding(left = 5.dp, right = 5.dp)
            .padding(bottom = 14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0.973f, 0.980f, 0.988f, 0.3f))
    ) {
        Column(Modifier.padding(18.dp)) {
            RowReverse(verticalAlignment = Alignment.CenterVertically) {
                LinearGradient(
                    colors = listOf(PdGold, PdGold.copy(alpha = 0xCC / 255f)),
                    modifier = Modifier.absolutePadding(left = 12.dp).size(44.dp),
                    shape = CircleShape
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        MaterialIcons("star", size = 22.dp, color = PdSurface)
                    }
                }
                AppText(
                    text = "امتیاز محصول",
                    fontSize = 17.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = PdDark
                )
            }
            Column(
                Modifier
                    .padding(top = 15.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color(1f, 1f, 1f, 0.5f))
                    .padding(15.dp)
            ) {
                MultiOptionRatingComponent(
                    contentId = product["ProductId"],
                    averageRating = product.double("AverageRating") ?: 0.0,
                    ratingCount = product.int("RatingCount") ?: 0,
                    initialRating = product.double("UserRating") ?: 0.0,
                    initialDetailedRatings = userRatings,
                    maxStars = 5,
                    size = 24.dp,
                    starColor = PdGold,
                    enableMultipleOptions = true,
                    ratingOptions = options,
                    modalTitle = "امتیازدهی محصول",
                    submitButtonText = "ثبت امتیاز",
                    cancelButtonText = "لغو",
                    showRatingCount = true,
                    showRatingText = true,
                    animated = true,
                    allowHalfStars = false,
                    onRatingSubmitted = { result ->
                        if (result.ratings != null) {
                            onUserRatings(result.ratings)
                            val average = result.averageRating ?: 0.0
                            onProduct(product.with("UserRating" to average))
                            onSuccess("امتیاز شما در ${result.ratings.size} بخش با موفقیت ثبت شد")
                        } else {
                            val rating = result.rating ?: 0.0
                            onProduct(product.with("UserRating" to rating))
                            onSuccess("امتیاز ${toPersianDigits(jsToString(rating))} ستاره ثبت شد")
                        }
                    },
                    onRatingError = { message -> onError(message) },
                    onRatingChange = { newRating, detailed ->
                        onProduct(product.with("UserRating" to newRating))
                        if (detailed != null) onUserRatings(detailed)
                    }
                )
                val picked = options.filter { (userRatings[it.id] ?: 0.0) > 0.0 }
                if (options.isNotEmpty() && picked.isNotEmpty()) {
                    Column(
                        Modifier
                            .padding(top = 20.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color(0xFFE8F5E8))
                            .padding(15.dp)
                    ) {
                        AppText(
                            text = "امتیازات شما:",
                            modifier = Modifier.padding(bottom = 15.dp),
                            fontSize = 16.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            color = PdDark,
                            textAlign = TextAlign.Right
                        )
                        picked.forEach { option ->
                            val score = userRatings[option.id] ?: 0.0
                            Box(
                                Modifier
                                    .padding(bottom = 10.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White)
                                    .padding(12.dp)
                            ) {
                                RowReverse(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AppText(
                                        text = option.title,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 14.sp,
                                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                                        color = PdDark,
                                        textAlign = TextAlign.Right
                                    )
                                    RowReverse(
                                        Modifier.absolutePadding(left = 15.dp),
                                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StarDisplay(
                                            rating = score,
                                            maxStars = 5,
                                            size = 16.dp,
                                            color = PdGold,
                                            emptyColor = Color(0xFFE0E0E0),
                                            showHalfStars = false,
                                            animated = false
                                        )
                                        AppText(
                                            text = toPersianDigits(jsToString(score)),
                                            fontSize = 14.sp,
                                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                                            color = PdSuccess
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                val averaged = options.filter { it.averageRating > 0.0 }
                if (options.isNotEmpty() && averaged.isNotEmpty()) {
                    Column(
                        Modifier
                            .padding(top = 15.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(1f, 0.973f, 0.882f, 0.5f))
                            .padding(12.dp)
                    ) {
                        AppText(
                            text = "میانگین امتیاز در هر بخش:",
                            modifier = Modifier.padding(bottom = 10.dp),
                            fontSize = 14.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            color = PdDark,
                            textAlign = TextAlign.Right
                        )
                        averaged.forEach { option ->
                            Box(
                                Modifier
                                    .padding(bottom = 8.dp)
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(1f, 1f, 1f, 0.7f))
                                    .padding(8.dp)
                            ) {
                                RowReverse(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AppText(
                                        text = option.title,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 13.sp,
                                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                                        color = PdDark,
                                        textAlign = TextAlign.Right
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        StarDisplay(
                                            rating = option.averageRating,
                                            maxStars = 5,
                                            size = 16.dp,
                                            color = PdGold,
                                            emptyColor = Color(0xFFE0E0E0),
                                            showHalfStars = true,
                                            animated = false
                                        )
                                        AppText(
                                            text = toPersianDigits(jsToFixed(option.averageRating, 1)),
                                            fontSize = 12.sp,
                                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                                            color = Color(0xFF868686)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
            Box(Modifier.fillMaxHeight().width(6.dp).background(PdGold.copy(alpha = 0x60 / 255f)))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProductImageDialog(urls: List<String?>, startIndex: Int, onClose: () -> Unit) {
    val width = windowSize().first
    val statusTop = statusBarHeight()
    val pager = rememberPagerState(initialPage = startIndex.coerceIn(0, (urls.size - 1).coerceAtLeast(0))) {
        urls.size.coerceAtLeast(1)
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color(0f, 0f, 0f, 0.95f))) {
            Box(
                Modifier
                    .align(AbsoluteAlignment.TopRight)
                    .zIndex(10f)
                    .absoluteOffset(x = (-20).dp, y = statusTop + 20.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(1f, 1f, 1f, 0.2f))
                    .touchableOpacity { onClose() },
                contentAlignment = Alignment.Center
            ) {
                MaterialIcons("close", size = 24.dp, color = Color.White)
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().height(width)) { page ->
                    Box(Modifier.fillMaxWidth().height(width), contentAlignment = Alignment.Center) {
                        ProductDetailImage(
                            urls.getOrNull(page),
                            Modifier.size(width),
                            ResizeMode.contain
                        )
                    }
                }
            }
        }
    }
}
