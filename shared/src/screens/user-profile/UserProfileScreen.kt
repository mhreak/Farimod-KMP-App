package ir.farimod.app.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.Ionicons
import ir.farimod.app.components.icons.MaterialCommunityIcons
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.navigation.Tabs
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.navigation.useRoute
import ir.farimod.app.network.Api
import ir.farimod.app.network.bool
import ir.farimod.app.network.double
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.jsonOf
import ir.farimod.app.network.list
import ir.farimod.app.network.str
import ir.farimod.app.platform.canOpenUrl
import ir.farimod.app.platform.isIOS
import ir.farimod.app.platform.openUrl
import ir.farimod.app.screens.userprofile.UP_CENTER
import ir.farimod.app.screens.userprofile.UP_END
import ir.farimod.app.screens.userprofile.UpAccent
import ir.farimod.app.screens.userprofile.UpBlogCard
import ir.farimod.app.screens.userprofile.UpConst
import ir.farimod.app.screens.userprofile.UpContactItem
import ir.farimod.app.screens.userprofile.UpContactSkeleton
import ir.farimod.app.screens.userprofile.UpCourseCard
import ir.farimod.app.screens.userprofile.UpDark
import ir.farimod.app.screens.userprofile.UpError
import ir.farimod.app.screens.userprofile.UpErrorState
import ir.farimod.app.screens.userprofile.UpExpandableText
import ir.farimod.app.screens.userprofile.UpFlow
import ir.farimod.app.screens.userprofile.UpGalleryCard
import ir.farimod.app.screens.userprofile.UpHex
import ir.farimod.app.screens.userprofile.UpInfo
import ir.farimod.app.screens.userprofile.UpInfoSection
import ir.farimod.app.screens.userprofile.UpInfoSectionSkeleton
import ir.farimod.app.screens.userprofile.UpLikeButton
import ir.farimod.app.screens.userprofile.UpMedium
import ir.farimod.app.screens.userprofile.UpPortfolioCard
import ir.farimod.app.screens.userprofile.UpPrimary
import ir.farimod.app.screens.userprofile.UpPrimaryDark
import ir.farimod.app.screens.userprofile.UpProductCard
import ir.farimod.app.screens.userprofile.UpProfileSkeleton
import ir.farimod.app.screens.userprofile.UpQuickAccessSkeleton
import ir.farimod.app.screens.userprofile.UpSectionHeader
import ir.farimod.app.screens.userprofile.UpSectionSkeleton
import ir.farimod.app.screens.userprofile.UpSkeleton
import ir.farimod.app.screens.userprofile.UpSkeletonPulse
import ir.farimod.app.screens.userprofile.UpSurface
import ir.farimod.app.screens.userprofile.UpTertiary
import ir.farimod.app.screens.userprofile.UpVideoSection
import ir.farimod.app.screens.userprofile.UpWarning
import ir.farimod.app.screens.userprofile.isStrictTrue
import ir.farimod.app.screens.userprofile.upChild
import ir.farimod.app.screens.userprofile.upEncodeURIComponent
import ir.farimod.app.screens.userprofile.upHex
import ir.farimod.app.state.AuthState
import ir.farimod.app.ui.AlertButton
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.MainBackground
import ir.farimod.app.ui.NetworkImage
import ir.farimod.app.ui.RNAlert
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.ResizeMode
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.ToastData
import ir.farimod.app.ui.linearGradient
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.ui.windowSize
import ir.farimod.app.utils.safeString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import kotlin.math.roundToInt

private val ProfileFallback = jsonOf(
    "MemberId" to 0,
    "Name" to "کاربر ناشناس",
    "MemberGroupsStr" to "تعریف نشده",
    "AboutMe" to "اطلاعات بیوگرافی موجود نیست",
    "Email" to "example@email.com",
    "Mobile" to "09123456789",
    "CityName" to "تهران",
    "ProvinceName" to "ایران",
    "ShowAboutMeText" to true,
    "ShowContactInfo" to true
)

@Composable
fun UserProfileScreen() {
    val navigation = useNavigation()
    val route = useRoute()
    val userData = route.json("userData")
    val memberId = userData["MemberId"].int
    val currentMemberId = AuthState.user?.MemberId
    val shouldFetch = memberId != null && memberId != 0

    var profile by remember { mutableStateOf<JsonElement?>(null) }
    var loading by remember(memberId) { mutableStateOf(shouldFetch) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf(ToastData()) }

    LaunchedEffect(memberId, currentMemberId, reload) {
        if (!shouldFetch || memberId == null) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        error = null
        try {
            profile = fetchMemberProfile(memberId, currentMemberId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "خطا در دریافت اطلاعات پروفایل"
        } finally {
            loading = false
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        MainBackground()
        when {
            loading -> UpProfileLoading()
            error != null -> UpErrorState(error ?: "", onRetry = { reload++ })
            else -> UpProfileContent(
                source = profile ?: userData ?: ProfileFallback,
                routeUser = userData,
                onLikeError = { msg -> toast = ToastData(true, msg, "error") },
                onContact = { type, value -> openContact(type, value) },
                onSeeAll = { type, id, name ->
                    openFiltered(navigation, type, id, name)
                },
                onPortfolio = { item ->
                    val id = item["PortfolioId"].int ?: item["PotfolioId"].int
                    if (id != null && id != 0) {
                        navigation.navigate("PortfolioDetail", mapOf("title" to item["Title"].str, "portfolioId" to id))
                    }
                },
                onCourse = { item ->
                    navigation.navigate("CourseDetails", mapOf("courseData" to item, "courseId" to item["CourseId"].int))
                },
                onProduct = { item ->
                    navigation.navigate("ProductDetails", mapOf("productData" to item, "productId" to item["ProductId"].int))
                },
                onGallery = { item ->
                    navigation.navigate("GalleryItem", mapOf("title" to item["Title"].str, "galleryId" to item["ImageGalleryId"].int))
                },
                onBlog = { item ->
                    navigation.navigate("MagDetailes", mapOf("title" to item["Title"].str, "blogId" to item["BlogPostId"].int))
                }
            )
        }

        Box(
            Modifier
                .align(AbsoluteAlignment.TopRight)
                .zIndex(1000f)
                .absoluteOffset(x = (-20).dp, y = statusBarHeight() + 33.dp)
                .size(44.dp)
                .touchableOpacity { navigation.goBack() }
                .rnShadow(5.dp, CircleShape)
                .background(Color.White.copy(alpha = 0.9f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MaterialIcons("arrow-forward", size = 24.dp, color = Color(0xFF6366F1))
        }

        Toast(toast.visible, toast.message, toast.type) { toast = toast.copy(visible = false) }
    }
}

private suspend fun fetchMemberProfile(memberId: Int, currentMemberId: Int?): JsonElement {
    val query = mutableMapOf<String, Any?>("memberId" to memberId)
    if (currentMemberId != null && currentMemberId != 0) query["currentMemberId"] = currentMemberId
    val response = Api.get("Member/Get", query = query)
    if (!response.ok) throw IllegalStateException("HTTP error! status: ${response.status}")
    return response.json["Data"] ?: throw IllegalStateException("Invalid data format received from API")
}

private fun openContact(type: String, value: String) {
    try {
        when (type) {
            "phone" -> openUrl("tel:${value.replace(Regex("\\s+"), "")}")
            "email" -> {
                val mailto = "mailto:$value"
                if (canOpenUrl(mailto)) openUrl(mailto)
                else RNAlert.alert("ایمیل", "آدرس ایمیل: $value", listOf(AlertButton("باشه")))
            }
            "whatsapp" -> {
                val digits = value.filter { it in '0'..'9' }
                val full = if (digits.startsWith("98")) digits else "98$digits"
                val url = "whatsapp://send?phone=$full"
                if (canOpenUrl(url)) openUrl(url) else RNAlert.alert("خطا", "واتساپ نصب نیست")
            }
            "telegram" -> {
                val id = value.replace("@", "").trim()
                val appUrl = "tg://resolve?domain=$id"
                try {
                    if (canOpenUrl(appUrl)) openUrl(appUrl) else openUrl("https://t.me/$id")
                } catch (_: Exception) {
                    openUrl("https://t.me/$id")
                }
            }
            "instagram" -> {
                val id = value.replace("@", "").trim()
                val appUrl = "instagram://user?username=$id"
                try {
                    if (canOpenUrl(appUrl)) openUrl(appUrl) else openUrl("https://instagram.com/$id")
                } catch (_: Exception) {
                    openUrl("https://instagram.com/$id")
                }
            }
            "website" -> {
                val trimmed = value.trim()
                val url = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
                openUrl(url)
            }
            "location" -> {
                val q = upEncodeURIComponent(value)
                val primary = if (isIOS) "maps://app?q=$q" else "geo:0,0?q=$q"
                if (canOpenUrl(primary)) openUrl(primary)
                else openUrl("https://www.google.com/maps/search/?api=1&query=$q")
            }
        }
    } catch (_: Exception) {
        RNAlert.alert("خطا", "خطا در باز کردن لینک")
    }
}

private fun openFiltered(
    navigation: ir.farimod.app.navigation.Navigation,
    type: String,
    memberId: Int,
    memberName: String?
) {
    if (memberId == 0) return
    val name = if (memberName.isNullOrEmpty()) "کاربر" else memberName
    val params = mapOf(
        "filteredMemberId" to memberId,
        "filteredMemberName" to name,
        "filterType" to "member"
    )
    if (type == "blog") {
        navigation.navigate("MainTabs", mapOf("screen" to Tabs.Mag, "params" to params))
    } else {
        val screen = when (type) {
            "portfolio" -> "AllPortfolio"
            "products" -> "AllProducts"
            "courses" -> "AllCourses"
            "gallery" -> "AllGalleries"
            else -> return
        }
        navigation.navigate(screen, params)
    }
}

@Composable
private fun UpHalfCircle() {
    Box(
        Modifier
            .upChild(
                marginTop = (-1600).dp,
                marginBottom = (-1080).dp,
                align = UP_CENTER,
                zIndex = 1f,
                width = UpConst.CIRCLE.dp,
                height = UpConst.CIRCLE.dp
            )
            .clip(CircleShape)
            .linearGradient(
                listOf(UpPrimary, UpPrimaryDark, UpAccent),
                start = Offset(0f, 0f),
                end = Offset(1f, 1f),
                shape = CircleShape
            )
    )
}

@Composable
private fun UpProfileLoading() {
    val scroll = rememberScrollState()
    UpSkeletonPulse {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .absolutePadding(left = 20.dp, right = 20.dp)
        ) {
            UpFlow(Modifier.fillMaxWidth()) {
                UpHalfCircle()
                UpProfileSkeleton()
                UpSkeleton(
                    Modifier
                        .upChild(marginTop = 20.dp, marginBottom = 60.dp, zIndex = 1f)
                        .fillMaxWidth()
                        .height(200.dp),
                    radius = 20.dp
                )
                UpInfoSectionSkeleton()
                UpContactSkeleton()
                UpQuickAccessSkeleton()
                UpSectionSkeleton("portfolio")
                UpSectionSkeleton("product")
                UpSectionSkeleton("course")
                UpSectionSkeleton("gallery")
                UpSectionSkeleton("blog")
                Box(Modifier.upChild(height = 50.dp).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun UpProfileContent(
    source: JsonElement,
    routeUser: JsonElement?,
    onLikeError: (String) -> Unit,
    onContact: (String, String) -> Unit,
    onSeeAll: (String, Int, String?) -> Unit,
    onPortfolio: (JsonElement) -> Unit,
    onCourse: (JsonElement) -> Unit,
    onProduct: (JsonElement) -> Unit,
    onGallery: (JsonElement) -> Unit,
    onBlog: (JsonElement) -> Unit
) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var scrollOrigin by remember { mutableFloatStateOf(0f) }
    val sectionY = remember { mutableMapOf<String, Float>() }
    fun Modifier.anchor(key: String): Modifier = onGloballyPositioned { coords ->
        sectionY[key] = coords.positionInRoot().y - scrollOrigin + scroll.value.toFloat()
    }
    fun scrollTo(key: String) {
        val y = sectionY[key] ?: return
        val delta = with(density) { 100.dp.toPx() }
        scope.launch { scroll.animateScrollTo((y - delta).roundToInt().coerceAtLeast(0)) }
    }

    val portfolio = source["PortfolioViewModelList"].list
    val products = source["ProductViewModelList"].list
    val courses = source["CourseViewModelList"].list
    val gallery = source["ImageGalleryViewModelList"].list
    val blogs = source["BlogPostViewModelList"].list
    val sectionCount = listOf(portfolio, products, courses, gallery, blogs).count { it.isNotEmpty() }
    val showQuick = sectionCount >= 2
    val memberId = source["MemberId"].int ?: 0
    val memberName = source["Name"].str
    val routeHasVideo = !routeUser["IntroductionVideoFileName"].str.isNullOrEmpty() ||
        !routeUser["IntroductionVideoURL"].str.isNullOrEmpty()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .onGloballyPositioned { scrollOrigin = it.positionInRoot().y }
            .padding(top = statusBarHeight())
            .absolutePadding(left = 20.dp, right = 20.dp)
    ) {
        UpFlow(Modifier.fillMaxWidth()) {
            UpHalfCircle()
            UpProfileBlock(source, onLikeError)
            if (routeHasVideo) UpVideoSection(source)
            if (!showQuick) Box(Modifier.upChild(height = 50.dp).fillMaxWidth())
            if (source["ShowAboutMeText"].isStrictTrue()) {
                UpInfoSection(icon = "person", title = "درباره من", iconColor = UpInfo, iconHex = UpHex.info) {
                    UpExpandableText(safeString(source["AboutMe"].str, "اطلاعات بیوگرافی موجود نیست"), maxLines = 3)
                }
            }
            if (source["ShowContactInfo"].isStrictTrue()) {
                UpInfoSection(icon = "contact-phone", title = "اطلاعات تماس", iconColor = UpTertiary, iconHex = UpHex.tertiary) {
                    UpContactBlock(source, onContact)
                }
            }
            if (showQuick) {
                UpQuickAccess(
                    hasPortfolio = portfolio.isNotEmpty(),
                    hasProducts = products.isNotEmpty(),
                    hasCourses = courses.isNotEmpty(),
                    hasGallery = gallery.isNotEmpty(),
                    onPortfolio = { scrollTo("portfolio") },
                    onProducts = { scrollTo("products") },
                    onCourses = { scrollTo("courses") },
                    onGallery = { scrollTo("gallery") }
                )
            }
            if (blogs.isNotEmpty()) {
                Column(Modifier.upChild(marginBottom = 25.dp).anchor("blog").fillMaxWidth()) {
                    UpSectionHeader("مقالات", "article", UpInfo, true) { onSeeAll("blog", memberId, memberName) }
                    UpCardRow(blogs, "BlogPostId", 15.dp, snap = false) { UpBlogCard(it, onBlog) }
                }
            }
            if (portfolio.isNotEmpty()) {
                Column(Modifier.upChild(marginBottom = 25.dp).anchor("portfolio").fillMaxWidth()) {
                    UpSectionHeader("نمونه کارها", "work", ir.farimod.app.screens.userprofile.UpSecondary, true) {
                        onSeeAll("portfolio", memberId, memberName)
                    }
                    UpCardRow(portfolio, "PortfolioId", 15.dp, snap = false) { UpPortfolioCard(it, onPortfolio) }
                }
            }
            if (products.isNotEmpty()) {
                Column(Modifier.upChild(marginBottom = 25.dp).anchor("products").fillMaxWidth()) {
                    UpSectionHeader("محصولات", "shopping-bag", UpAccent, true) { onSeeAll("products", memberId, memberName) }
                    UpCardRow(products, "ProductId", 15.dp, snap = false) { UpProductCard(it, onProduct) }
                }
            }
            if (courses.isNotEmpty()) {
                Column(Modifier.upChild(marginBottom = 25.dp).anchor("courses").fillMaxWidth()) {
                    UpSectionHeader("دوره‌ها", "school", UpTertiary, true) { onSeeAll("courses", memberId, memberName) }
                    UpCardRow(courses, "CourseId", 15.dp, snap = true, contentPadding = 10.dp) { UpCourseCard(it, onCourse) }
                }
            }
            if (gallery.isNotEmpty()) {
                Column(Modifier.upChild(marginBottom = 25.dp).anchor("gallery").fillMaxWidth()) {
                    UpSectionHeader("گالری تصاویر", "photo-library", UpError, true) { onSeeAll("gallery", memberId, memberName) }
                    UpCardRow(gallery, "ImageGalleryId", 10.dp, snap = false) { UpGalleryCard(it, onGallery) }
                }
            }
            Box(Modifier.upChild(height = 50.dp).fillMaxWidth())
        }
    }
}

@Composable
private fun UpProfileBlock(source: JsonElement, onLikeError: (String) -> Unit) {
    val avatar = source["AvatarImageURL"].str?.trim().orEmpty()
    Column(
        Modifier
            .upChild(marginTop = UpConst.PROFILE_TOP.dp, marginBottom = 40.dp, zIndex = 2f)
            .fillMaxWidth()
            .heightIn(min = 300.dp)
            .padding(25.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.padding(bottom = 25.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(UpConst.AVATAR_OUTER.dp)
                    .rnShadow(0.dp, CircleShape, UpPrimary)
                    .clip(CircleShape)
                    .linearGradient(
                        listOf(UpPrimary, UpAccent, ir.farimod.app.screens.userprofile.UpSecondary, UpTertiary),
                        start = Offset(0f, 0f),
                        end = Offset(1f, 1f),
                        shape = CircleShape
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(UpConst.AVATAR_MIDDLE.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(UpConst.AVATAR.dp).clip(CircleShape)) {
                        if (avatar.isNotEmpty()) {
                            NetworkImage(avatar, Modifier.fillMaxSize(), resizeMode = ResizeMode.cover)
                        } else {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .linearGradient(
                                        listOf(UpPrimary, UpPrimaryDark),
                                        start = Offset(0f, 0f),
                                        end = Offset(1f, 1f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                MaterialCommunityIcons(
                                    name = if (source["Gender"].bool) "face-man" else "face-woman",
                                    size = 60.dp,
                                    color = Color.White
                                )
                                MaterialIcons(
                                    name = "star",
                                    size = 12.dp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.align(AbsoluteAlignment.TopRight).absoluteOffset(x = (-15).dp, y = 12.dp)
                                )
                                MaterialIcons(
                                    name = "auto-awesome",
                                    size = 10.dp,
                                    color = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.align(AbsoluteAlignment.BottomLeft).absoluteOffset(x = 12.dp, y = (-15).dp)
                                )
                            }
                        }
                    }
                }
            }
            if (source["ShowBlueTick"].bool) {
                Box(
                    Modifier
                        .align(AbsoluteAlignment.BottomRight)
                        .absoluteOffset(x = (-4).dp, y = (-4).dp)
                        .size(30.dp)
                        .rnShadow(5.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    MaterialIcons(name = "verified", size = 26.dp, color = Color(0xFF1DA1F2))
                }
            }
        }
        AppText(
            text = safeString(source["Name"].str, "کاربر ناشناس"),
            fontSize = 24.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = UpDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp).zIndex(10f)
        )
        AppText(
            text = safeString(source["MemberGroupsStr"].str, "تعریف نشده"),
            fontSize = 14.sp,
            fontFamily = RNFonts.Yekan_Bakh_Regular,
            color = UpMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(bottom = 20.dp)
                .zIndex(9f)
                .rnShadow(3.dp, RoundedCornerShape(50.dp))
                .clip(RoundedCornerShape(50.dp))
                .background(Color.White.copy(alpha = 0.8f))
                .absolutePadding(left = 16.dp, right = 16.dp, top = 8.dp, bottom = 8.dp)
        )
        UpLikeButton(
            memberId = source["MemberId"].int,
            initialLikeCount = source["LikeCount"].int ?: 0,
            initialIsLiked = source["IsCurrentMemberLikedThisMember"].bool,
            onError = onLikeError
        )
    }
}

@Composable
private fun UpContactBlock(source: JsonElement, onContact: (String, String) -> Unit) {
    val city = safeString(source["CityName"].str, "")
    val province = safeString(source["ProvinceName"].str, "")
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        source["Email"].str?.takeIf { it.isNotEmpty() }?.let {
            UpContactItem("email", it, "email") { onContact("email", it) }
        }
        source["Mobile"].str?.takeIf { it.isNotEmpty() }?.let {
            UpContactItem("phone", it, "phone") { onContact("phone", it) }
        }
        source["Phone1"].str?.takeIf { it.isNotEmpty() }?.let {
            UpContactItem("phone", it, "phone") { onContact("phone", it) }
        }
        source["Phone2"].str?.takeIf { it.isNotEmpty() }?.let {
            UpContactItem("phone", it, "phone") { onContact("phone", it) }
        }
        source["WhatsappAccountMobileNumber"].str?.takeIf { it.isNotEmpty() }?.let {
            UpContactItem("chat", it, "whatsapp") { onContact("whatsapp", it) }
        }
        source["TelegramAccountId"].str?.takeIf { it.isNotEmpty() }?.let {
            UpContactItem("send", it, "telegram") { onContact("telegram", it) }
        }
        source["InstagramAccountId"].str?.takeIf { it.isNotEmpty() }?.let {
            UpContactItem("photo-camera", it, "instagram") { onContact("instagram", it) }
        }
        source["WebsiteAddress"].str?.takeIf { it.isNotEmpty() }?.let {
            UpContactItem("language", it, "website") { onContact("website", it) }
        }
        if (city.isNotEmpty() || province.isNotEmpty()) {
            val label = city + (if (city.isNotEmpty() && province.isNotEmpty()) "، " else "") + province
            val query = "${source["CityName"].str.orEmpty()}, ${source["ProvinceName"].str.orEmpty()}"
            UpContactItem("location-on", label, "location") { onContact("location", query) }
        }
    }
}

@Composable
private fun UpQuickAccess(
    hasPortfolio: Boolean,
    hasProducts: Boolean,
    hasCourses: Boolean,
    hasGallery: Boolean,
    onPortfolio: () -> Unit,
    onProducts: () -> Unit,
    onCourses: () -> Unit,
    onGallery: () -> Unit
) {
    val maxW = (windowSize().first - 60.dp) / 4
    RowReverse(
        Modifier
            .upChild(marginTop = 15.dp, marginBottom = 30.dp, zIndex = 1f)
            .fillMaxWidth()
            .absolutePadding(top = 15.dp, bottom = 15.dp)
            .absolutePadding(left = 5.dp, right = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasPortfolio) UpQuickItem("work", "نمونه کار", UpHex.warning, UpWarning, maxW, onPortfolio)
        if (hasProducts) UpQuickItem("shopping-bag", "محصولات", UpHex.accent, UpAccent, maxW, onProducts)
        if (hasCourses) UpQuickItem("school", "دوره‌ها", UpHex.tertiary, UpTertiary, maxW, onCourses)
        if (hasGallery) UpQuickItem("photo-library", "گالری", UpHex.error, UpError, maxW, onGallery)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.UpQuickItem(
    icon: String,
    label: String,
    hex: String,
    tint: Color,
    maxW: Dp,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .weight(1f)
            .widthIn(max = maxW)
            .absolutePadding(left = 4.dp, right = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(upHex(hex, "15"))
            .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .touchableOpacity(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MaterialIcons(name = icon, size = 24.dp, color = tint)
        AppText(
            text = label,
            fontSize = 11.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = UpDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UpCardRow(
    data: List<JsonElement>,
    idKey: String,
    spaced: Dp,
    snap: Boolean,
    contentPadding: Dp = 5.dp,
    item: @Composable (JsonElement) -> Unit
) {
    val state = rememberLazyListState()
    LazyRow(
        state = state,
        modifier = Modifier.fillMaxWidth(),
        reverseLayout = true,
        contentPadding = PaddingValues(horizontal = contentPadding),
        horizontalArrangement = Arrangement.spacedBy(spaced),
        flingBehavior = if (snap) {
            rememberSnapFlingBehavior(state, SnapPosition.Start)
        } else {
            androidx.compose.foundation.gestures.ScrollableDefaults.flingBehavior()
        }
    ) {
        itemsIndexed(data, key = { index, el -> "${el[idKey].int ?: "x"}-$index" }) { _, el ->
            item(el)
        }
    }
}
