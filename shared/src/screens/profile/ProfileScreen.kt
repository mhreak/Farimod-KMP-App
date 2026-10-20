package ir.farimod.app.screens

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.absoluteOffset
import ir.farimod.app.components.icons.MaterialCommunityIcons
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.navigation.useFocusEffect
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.network.Api
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.str
import ir.farimod.app.screens.profile.ProfileCard
import ir.farimod.app.screens.profile.ProfileModernColors
import ir.farimod.app.screens.profile.profileItems
import ir.farimod.app.services.AuthService
import ir.farimod.app.state.AuthState
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.MainBackground
import ir.farimod.app.ui.NetworkImage
import ir.farimod.app.ui.ResizeMode
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.ToastData
import ir.farimod.app.ui.Tooltip
import ir.farimod.app.ui.pressable
import ir.farimod.app.ui.rememberSafeAreaInsets
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.rnSpring
import ir.farimod.app.ui.rnTiming
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.ui.windowSize
import ir.farimod.app.utils.currentTimeMillis
import ir.farimod.app.utils.toPersianDigits
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

/**
 * In RN the fetched profile (`GetProfileInfoToEdit`) is merged into the AuthContext user (so `user.MemberName` and
 * `user.Gender` exist there). The KMP `UserData` has no such fields, so they are kept here for the app session.
 */
private object ProfileSessionCache {
    var memberName: String? by mutableStateOf<String?>(null)
    var gender: Int? by mutableStateOf<Int?>(null)
}

private data class PermissionModalState(
    val visible: Boolean = false,
    val permission: String? = null,
    val targetScreen: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val navigation = useNavigation()
    val scope = rememberCoroutineScope()
    var toast by remember { mutableStateOf(ToastData()) }

    var permissionModal by remember { mutableStateOf(PermissionModalState()) }
    var logoutModal by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }

    val insets = rememberSafeAreaInsets()
    val user = AuthState.user
    val (screenWidth, _) = windowSize()
    val topInset = statusBarHeight()

    val filteredProfileItems = remember(user?.ActiveSubscriptionPlan?.IsInfinityPlan) {
        if (user?.ActiveSubscriptionPlan?.IsInfinityPlan == true) {
            profileItems.filter { it.id != 7 }
        } else {
            profileItems
        }
    }

    suspend fun fetchProfile() {
        val memberId = AuthState.user?.MemberId ?: AuthService.getUserData()?.MemberId
        if (memberId == null || memberId == 0) return
        try {
            var response = Api.get("MemberInfo/GetProfileInfoToEdit", query = mapOf("memberId" to memberId))
            var json = response.json
            var data = (json["Data"] as? JsonObject) ?: (json as? JsonObject)

            var fn = data?.str("FirstName")
            var ln = data?.str("LastName")
            var name = data?.str("MemberName")
                ?: data?.str("Name")
                ?: listOfNotNull(fn, ln).filter { !it.isNullOrBlank() }.joinToString(" ").trim()

            if (name.isBlank()) {
                val memberResponse = Api.get("Member/Get", query = mapOf("memberId" to memberId, "currentMemberId" to memberId))
                val mJson = memberResponse.json
                val mData = (mJson["Data"] as? JsonObject) ?: (mJson as? JsonObject)
                if (mData != null) {
                    fn = mData.str("FirstName") ?: fn
                    ln = mData.str("LastName") ?: ln
                    name = mData.str("MemberName")
                        ?: mData.str("Name")
                        ?: listOfNotNull(fn, ln).filter { !it.isNullOrBlank() }.joinToString(" ").trim()
                    if (data == null) data = mData
                }
            }

            if (name.isNotBlank()) {
                ProfileSessionCache.memberName = name
            }

            val genderVal = data?.int("Gender")
            if (genderVal != null) {
                ProfileSessionCache.gender = genderVal
            }

            val avatar = data?.str("AvatarImageURL") ?: data?.str("AvatarUrl")
            val current = AuthState.user ?: AuthService.getUserData()
            if (current != null) {
                val avatarUrl = if (!avatar.isNullOrEmpty()) avatar + "?t=" + currentTimeMillis() else current.AvatarImageURL
                val newFirstName = fn.takeIf { !it.isNullOrBlank() } ?: current.FirstName
                val newLastName = ln.takeIf { !it.isNullOrBlank() } ?: current.LastName

                AuthState.updateUser { cur ->
                    cur.copy(
                        MemberId = data?.int("MemberId") ?: cur.MemberId,
                        FirstName = newFirstName,
                        LastName = newLastName,
                        Mobile = data?.str("Mobile") ?: cur.Mobile,
                        AvatarImageURL = avatarUrl
                    )
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            println("Error fetching profile: $e")
        }
    }

    useFocusEffect(user?.MemberId) {
        fetchProfile()
    }

    LaunchedEffect(user?.MemberId) {
        fetchProfile()
    }

    fun showToast(message: String, type: String = "info") {
        toast = ToastData(visible = true, message = message, type = type)
    }

    fun hideToast() {
        toast = ToastData(visible = false, message = "", type = "info")
    }

    val showPermissionModal: (String?, String) -> Unit = { permission, targetScreen ->
        permissionModal = PermissionModalState(true, permission, targetScreen)
    }

    fun hidePermissionModal() {
        permissionModal = PermissionModalState(false, null, null)
    }

    fun handleUpgradeFromModal() {
        hidePermissionModal()
        navigation.navigate("Subscription")
    }

    fun onRefresh() {
        scope.launch {
            refreshing = true
            try {
                val result = AuthState.refreshSubscription()
                if (result) {
                    // showToast('اطلاعات اشتراک بروزرسانی شد', 'success');
                } else {
                    showToast("خطا در بروزرسانی اطلاعات", "error")
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast("خطا در بروزرسانی اطلاعات", "error")
            }
            refreshing = false
        }
    }

    fun handleLogout() {
        logoutModal = true
    }

    fun confirmLogout() {
        logoutModal = false
        try {
            AuthState.logout()
            showToast("با موفقیت خارج شدید", "success")
            navigation.reset("Login")
        } catch (e: Exception) {
            println("Logout error: $e")
            showToast("خطا در خروج از حساب کاربری", "error")
        }
    }

    fun cancelLogout() {
        logoutModal = false
    }

    val handleCardPress: (String) -> Unit = { screenName ->
        navigation.navigate(screenName)
    }

    // تعیین آیکون پروفایل بر اساس جنسیت
    fun getGenderIcon(): String {
        return when (ProfileSessionCache.gender) {
            1 -> "face-man" // مرد
            2 -> "face-woman" // زن
            else -> "account-circle" // جنسیت مشخص نشده
        }
    }

    val modalBottomMargin = if (insets.bottom > 20.dp) insets.bottom else 20.dp

    Box(Modifier.fillMaxSize()) {
        // Permission Upgrade Modal
        if (permissionModal.visible) {
            Dialog(
                onDismissRequest = { hidePermissionModal() },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                ProfileModalOverlay(
                    onDismiss = { hidePermissionModal() },
                    bottomMargin = modalBottomMargin
                ) {
                    // Icon
                    ProfileModalIconContainer(Color(0xFFFFF3CD)) {
                        MaterialIcons(name = "star", size = 48.dp, color = Color(0xFFFFD700))
                    }

                    // Title
                    AppText(
                        text = "ارتقای اشتراک",
                        fontSize = 20.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color(0xFF1F2937),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.absolutePadding(bottom = 12.dp)
                    )

                    // Message
                    AppText(
                        text = "برای استفاده از این بخش نیاز به ارتقای اشتراک دارید. آیا می‌خواهید به صفحه اشتراک‌ها بروید؟",
                        fontSize = 16.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp,
                        modifier = Modifier.absolutePadding(bottom = 30.dp)
                    )

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProfileModalButton(
                            text = "خیر",
                            textColor = Color(0xFF374151),
                            background = Color(0xFFF3F4F6),
                            bordered = true,
                            modifier = Modifier.weight(1f),
                            onClick = { hidePermissionModal() }
                        )
                        ProfileModalButton(
                            text = "ارتقای اشتراک",
                            textColor = Color(0xFFFFFFFF),
                            background = Color(0xFFFFD700),
                            bordered = false,
                            modifier = Modifier.weight(1f),
                            onClick = { handleUpgradeFromModal() }
                        )
                    }
                }
            }
        }

        // Logout Confirmation Modal
        if (logoutModal) {
            Dialog(
                onDismissRequest = { cancelLogout() },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                ProfileModalOverlay(
                    onDismiss = { cancelLogout() },
                    bottomMargin = modalBottomMargin
                ) {
                    // Icon
                    ProfileModalIconContainer(Color(0xFFFEE2E2)) {
                        MaterialIcons(name = "logout", size = 48.dp, color = Color(0xFFEF4444))
                    }

                    // Title
                    AppText(
                        text = "خروج از حساب کاربری",
                        fontSize = 20.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color(0xFF1F2937),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.absolutePadding(bottom = 12.dp)
                    )

                    // Message
                    AppText(
                        text = "آیا مطمئن هستید که می‌خواهید از حساب کاربری خود خارج شوید؟",
                        fontSize = 16.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp,
                        modifier = Modifier.absolutePadding(bottom = 30.dp)
                    )

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ProfileModalButton(
                            text = "انصراف",
                            textColor = Color(0xFF374151),
                            background = Color(0xFFF3F4F6),
                            bordered = true,
                            modifier = Modifier.weight(1f),
                            onClick = { cancelLogout() }
                        )
                        ProfileModalButton(
                            text = "خروج",
                            textColor = Color(0xFFFFFFFF),
                            background = Color(0xFFEF4444),
                            bordered = false,
                            modifier = Modifier.weight(1f),
                            onClick = { confirmLogout() }
                        )
                    }
                }
            }
        }

        // container
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFC))
        ) {
            MainBackground()

            val pullState = rememberPullToRefreshState()
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = { onRefresh() },
                modifier = Modifier.fillMaxSize(),
                state = pullState,
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullState,
                        isRefreshing = refreshing,
                        modifier = Modifier.align(Alignment.TopCenter),
                        containerColor = Color.White,
                        color = FarimodColors.Primary
                    )
                }
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        // headerContainer
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = topInset + 35.dp, bottom = 20.dp)
                                .absolutePadding(left = 20.dp, right = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // titleWrapper
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppText(
                                    text = "پروفایل",
                                    fontSize = 22.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_ExtraBold,
                                    color = Color(0xFF2C3E50),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().absolutePadding(left = 56.dp, right = 56.dp)
                                ,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                            }
                        }

                        // Profile Header
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .absolutePadding(left = 20.dp, right = 20.dp)
                                .padding(bottom = 30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // profileImageContainer
                            Box(
                                modifier = Modifier
                                    .absolutePadding(bottom = 20.dp)
                                    .size(140.dp)
                                    .border(4.dp, Color(0xFFFFFFFF), CircleShape)
                                    .padding(4.dp)
                                    .clip(CircleShape)
                            ) {
                                val avatarUrl = user?.AvatarImageURL
                                if (!avatarUrl.isNullOrEmpty()) {
                                    androidx.compose.runtime.key(avatarUrl) {
                                        NetworkImage(
                                            uri = avatarUrl,
                                            modifier = Modifier.fillMaxSize(),
                                            resizeMode = ResizeMode.cover
                                        )
                                    }
                                } else {
                                    LinearGradient(
                                        colors = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1), Color(0xFF06B6D4)),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            MaterialCommunityIcons(
                                                name = getGenderIcon(),
                                                size = 85.dp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                            // profileInfo
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val memberName = ProfileSessionCache.memberName
                                    ?: listOfNotNull(user?.FirstName, user?.LastName).filter { it.isNotBlank() }.joinToString(" ").takeIf { it.isNotBlank() }
                                AppText(
                                    text = if (!memberName.isNullOrEmpty()) memberName else "نام و نام خانوادگی",
                                    fontSize = 22.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    color = Color(0xFF2C3E50),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().absolutePadding(left = 56.dp, right = 56.dp).absolutePadding(bottom = 12.dp)
                                ,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                                // mobileChip
                                val chipShape = RoundedCornerShape(20.dp)
                                Row(
                                    modifier = Modifier
                                        .absolutePadding(bottom = 8.dp)
                                        .rnShadow(3.dp, chipShape)
                                        .background(Color(0xE6FFFFFF), chipShape)
                                        .border(1.dp, Color(0x336366F1), chipShape)
                                        .absolutePadding(left = 16.dp, right = 16.dp, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MaterialIcons(name = "phone", size = 16.dp, color = ProfileModernColors.primary)
                                    val mobile = user?.Mobile
                                    AppText(
                                        text = toPersianDigits(if (!mobile.isNullOrEmpty()) mobile else "09131234567"),
                                        fontSize = 16.sp,
                                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                                        color = ProfileModernColors.primary,
                                        modifier = Modifier.absolutePadding(left = 8.dp)
                                    )
                                }
                            }
                        }

                        // Profile Options - Horizontal FlatList with RTL
                        val cardWidth: Dp = screenWidth * 0.75f
                        val listState = rememberLazyListState()
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp, bottom = 45.dp)
                        ) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                LazyRow(
                                    state = listState,
                                    reverseLayout = false,
                                    contentPadding = PaddingValues(horizontal = 15.dp),
                                    horizontalArrangement = Arrangement.spacedBy(15.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Center),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(filteredProfileItems, key = { it.id }) { item ->
                                        ProfileCard(
                                            item = item,
                                            cardWidth = cardWidth,
                                            onPress = handleCardPress,
                                            user = user,
                                            onShowPermissionModal = showPermissionModal
                                        )
                                    }
                                }
                            }
                        }

                        // Logout Button at Bottom
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .absolutePadding(left = 20.dp, right = 20.dp)
                                .padding(top = 20.dp, bottom = 20.dp)
                        ) {
                            val buttonShape = RoundedCornerShape(30.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .rnShadow(20.dp, buttonShape, Color(0xFFE91E63))
                                    .touchableOpacity(activeOpacity = 0.8f, onClick = { handleLogout() })
                                    .clip(buttonShape)
                            ) {
                                LinearGradient(
                                    colors = listOf(Color(0xFFE91E63), Color(0xFFAD1457)),
                                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                    end = androidx.compose.ui.geometry.Offset(1f, 0f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RowReverse(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .absolutePadding(left = 35.dp, right = 35.dp, top = 18.dp, bottom = 18.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        MaterialIcons(name = "logout", size = 24.dp, color = Color.White)
                                        AppText(
                                            text = "خروج از حساب کاربری",
                                            fontSize = 19.sp,
                                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                                            color = Color.White,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.absolutePadding(right = 12.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Spacer
                        Box(Modifier.height(50.dp))
                    }

                    // headerActions (absolute, top = StatusBar.currentHeight + 45, left/right 0, zIndex 1000)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .absoluteOffset(y = topInset + 45.dp)
                            .zIndex(1000f)
                            .absolutePadding(left = 20.dp, right = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // headerLeft (marginTop -12)
                        Box(Modifier.absoluteOffset(y = (-12).dp)) {
                            Tooltip(content = "در این بخش می‌توانید اطلاعات پروفایل، دوره‌ها، نمونه‌کارها و محصولات خود را مدیریت کنید.")
                        }
                    }
                }
            }
        }

        Toast(toast.visible, toast.message, toast.type) { hideToast() }
    }
}

/** `modalOverlay` + animated `modalContent` (translateY spring 300 → 0, opacity 0 → 1 in 200ms). */
@Composable
private fun ProfileModalOverlay(
    onDismiss: () -> Unit,
    bottomMargin: Dp,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val slide = remember { Animatable(300f) }
    val opacity = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        coroutineScope {
            launch { slide.animateTo(0f, rnSpring(tension = 100f, friction = 8f)) }
            launch { opacity.animateTo(1f, rnTiming(200)) }
        }
    }
    val contentShape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3000000))
            .pressable { onDismiss() }
            .absolutePadding(left = 30.dp, right = 30.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .absolutePadding(bottom = bottomMargin)
                .widthIn(max = 350.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = slide.value * density
                    alpha = opacity.value
                }
                .rnShadow(15.dp, contentShape)
                .background(Color(0xFFFFFFFF), contentShape)
                .padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
private fun ProfileModalIconContainer(background: Color, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .absolutePadding(bottom = 20.dp)
            .size(80.dp)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun ProfileModalButton(
    text: String,
    textColor: Color,
    background: Color,
    bordered: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .touchableOpacity(onClick = onClick)
            .background(background, shape)
            .then(if (bordered) Modifier.border(1.dp, Color(0xFFD1D5DB), shape) else Modifier)
            .absolutePadding(top = 14.dp, bottom = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        AppText(
            text = text,
            fontSize = 16.sp,
            fontFamily = RNFonts.Yekan_Bakh_Regular,
            color = textColor
        )
    }
}
