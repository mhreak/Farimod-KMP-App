package ir.farimod.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.farimod.app.components.icons.MaterialCommunityIcons
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.platform.getAppVersionName
import ir.farimod.app.platform.openUrl
import ir.farimod.app.utils.toPersianDigits
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.withFrameNanos

// ──────────────────────────────────────────────────────────────────────────────
// Port of app/components/MenuModal.tsx
//
// NOTE: the RN MenuModal is NOT a `Modal`: it is an absolutely positioned overlay (`StyleSheet.absoluteFill`)
// rendered as the last child of HomeScreen's root View. It is ported the same way (no Dialog): place it as the
// LAST child of the HomeScreen root `Box`. It draws nothing while closed.
// ──────────────────────────────────────────────────────────────────────────────

private class MenuItemData(
    val id: String,
    val title: String,
    val icon: String,
    val colors: List<Color>,
    val screen: String
)

private val menuItems = listOf(
    MenuItemData("courses", " دوره‌ها", "school", listOf(Color(0xFF002FFF), Color(0xFFC993FF)), "AllCourses"),
    MenuItemData("members", " اعضا", "people", listOf(Color(0xFF5EFF00), Color(0xFF9FB9FF)), "AllMembers"),
    MenuItemData("products", " محصولات", "shopping-bag", listOf(Color(0xFF0088FF), Color(0xFF98FF98)), "AllProducts"),
    MenuItemData("articles", " مقالات", "article", listOf(Color(0xFF2CFF72), Color(0xFFC900A7)), "وبلاگ"),
    MenuItemData("portfolios", " نمونه کارها", "brush", listOf(Color(0xFFFF457D), Color(0xFFFFE550)), "AllPortfolio"),
    MenuItemData("galleries", " گالری‌ها", "photo-library", listOf(Color(0xFF90EBE6), Color(0xFF4456FF)), "AllGalleries"),
    MenuItemData("profile", "پروفایل من", "account-circle", listOf(Color(0xFF8AAFFF), Color(0xFFFF7345)), "پروفایل"),
    MenuItemData("instagram", "اینستاگرام فریمد", "camera", listOf(Color(0xFF6F0EF7), Color(0xFFF70060)), "INSTAGRAM"),
    MenuItemData("support-guide", "راهنمای تماس با پشتیبانی", "help", listOf(Color(0xFF258067), Color(0xFF4DBCA0)), "SUPPORT_GUIDE"),
    MenuItemData(
        "farimod-app", "استفاده از اپلیکیشن فریمد", "devices",
        listOf(Color(0xFF667EEA), Color(0xFF764BA2), Color(0xFFFA709A)), "farimod_APP"
    ),
    MenuItemData("logout", "خروج از حساب کاربری", "logout", listOf(Color(0xFFBD001F), Color(0xFFFF5050)), "LOGOUT")
)

private val menuTitleColor = Color(0xFF2C3E50)
private val menuCardBg = Color(0xFFF8F9FA)
private val menuCardBorder = Color(0xFFE9ECEF)
private val menuGreen = Color(0xFF10B981)

/**
 * RN `MenuModal({ visible, onClose, onNavigate, showToast })`.
 *
 *  - [onNavigate] receives the menu item's `screen` string exactly as RN: a route name (`"AllCourses"`, `"AllMembers"`,
 *    `"AllProducts"`, `"وبلاگ"`, `"AllPortfolio"`, `"AllGalleries"`, `"پروفایل"`) or `"LOGOUT"` (after the confirmation
 *    dialog) — the caller (HomeScreen `handleMenuNavigation`) does `navigation.navigate(screen)` / logout.
 *  - [showToast] is `showToast(message, type)`.
 *  - Instagram / app-use / Bale support / phone / mail links are opened here with `openUrl` (same URLs as RN).
 *  - Fontisto: the RN file only imports it and never renders it (items have no `iconFamily`), so nothing is substituted.
 *
 * Animations: sheet spring (stiffness 180, damping 20, mass 0.8) in / 220 ms timing out, backdrop 150 ms / 160 ms,
 * logout card spring (stiffness 200, damping 22, mass 0.8) + 140 ms fade.
 */
@Composable
fun MenuModal(
    visible: Boolean,
    onClose: () -> Unit,
    onNavigate: (String) -> Unit,
    showToast: (String, String) -> Unit
) {
    val (windowWidth, height) = windowSize()
    val insets = rememberSafeAreaInsets()
    val density = LocalDensity.current

    val logoutSlideAnim = remember { Animatable(300f) }
    val logoutOpacityAnim = remember { Animatable(0f) }
    val modalSlideAnim = remember { Animatable(height.value) }
    val modalBackdropAnim = remember { Animatable(0f) }

    var showLogoutModal by remember { mutableStateOf(false) }
    var showSupportTooltip by remember { mutableStateOf(false) }
    var isRendered by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            // InteractionManager.runAfterInteractions → mount first, then animate
            if (!isRendered) modalSlideAnim.snapTo(height.value)
            isRendered = true
            withFrameNanos { }
            coroutineScope {
                launch {
                    // RN spring: stiffness 180, damping 20, mass 0.8 → Compose (mass = 1): stiffness k/m, ratio c / (2·√(k·m))
                    modalSlideAnim.animateTo(0f, spring(dampingRatio = 0.8333f, stiffness = 225f))
                }
                launch { modalBackdropAnim.animateTo(1f, tween(150, easing = ModalTimingEasing)) }
            }
        } else {
            coroutineScope {
                launch { modalSlideAnim.animateTo(height.value, tween(220, easing = ModalTimingEasing)) }
                launch { modalBackdropAnim.animateTo(0f, tween(160, easing = ModalTimingEasing)) }
            }
            isRendered = false
            showLogoutModal = false
            showSupportTooltip = false
        }
    }

    LaunchedEffect(showLogoutModal) {
        coroutineScope {
            launch {
                // stiffness 200, damping 22, mass 0.8
                logoutSlideAnim.animateTo(
                    if (showLogoutModal) 0f else 300f,
                    spring(dampingRatio = 0.8696f, stiffness = 250f)
                )
            }
            launch {
                logoutOpacityAnim.animateTo(
                    if (showLogoutModal) 1f else 0f,
                    tween(140, easing = ModalTimingEasing)
                )
            }
        }
    }

    fun handleClose() {
        onClose()
    }

    fun handleCallSupport() {
        val baleUrl = "https://web.bale.ai/chat?uid=550124215"
        val ok = try { openUrl(baleUrl) } catch (_: Throwable) { false }
        if (!ok) showToast("خطا در باز کردن پیام‌رسان بله", "error")
    }

    fun handleMenuItemPress(screen: String) {
        when (screen) {
            "LOGOUT" -> showLogoutModal = true
            "SUPPORT_GUIDE" -> showSupportTooltip = true
            "farimod_APP" -> {
                val ok = try { openUrl("https://farimod.ir/app-use") } catch (_: Throwable) { false }
                if (!ok) showToast("خطا در باز کردن لینک", "error")
                handleClose()
            }
            "INSTAGRAM" -> {
                val ok = try { openUrl("https://www.instagram.com/farimod.comm") } catch (_: Throwable) { false }
                if (!ok) showToast("خطا در باز کردن اینستاگرام", "error")
                handleClose()
            }
            else -> {
                onNavigate(screen)
                handleClose()
            }
        }
    }

    fun handleLogoutConfirm() {
        onNavigate("LOGOUT")
        showLogoutModal = false
        handleClose()
    }

    if (!isRendered) return

    Box(Modifier.fillMaxSize()) {
        // modalBackdrop
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = modalBackdropAnim.value }
                .background(rgba(0, 0, 0, 0.5f))
                .pressable(enabled = visible) { handleClose() }
        )

        // modalContent
        val sheetShape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = height * 0.75f)
                .graphicsLayer { translationY = modalSlideAnim.value * density.density }
                .rnShadow(10.dp, sheetShape)
                .background(Color(0xFFFFFFFF), sheetShape)
                .pressable(enabled = visible) { /* swallow taps so the backdrop does not close the menu */ }
                .padding(top = 15.dp)
                .padding(horizontal = 20.dp)
                .padding(bottom = insets.bottom)
        ) {
            // modalHandle
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 20.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(Color(0xFFE0E0E0), RoundedCornerShape(2.dp))
            )

            // modalHeader
            Column(
                Modifier
                    .padding(bottom = 25.dp)
                    .fillMaxWidth()
                    .modalBorderBottom(1.dp, Color(0xFFF0F0F0))
                    .padding(bottom = 15.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // headerTitleRow
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Box(
                        Modifier
                            .align(AbsoluteAlignment.TopLeft)
                            .absoluteOffset(y = 5.dp)
                            .touchableOpacity(activeOpacity = 0.8f) { handleClose() }
                            .size(40.dp)
                            .border(3.dp, Color(0xFFDC2626), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        MaterialIcons("close", size = 30.dp, color = Color(0xFFDC2626))
                    }
                    AppText(
                        "منوی اصلی",
                        modifier = Modifier.padding(bottom = 5.dp),
                        fontSize = 20.sp,
                        color = menuTitleColor,
                        fontFamily = RNFonts.Yekan_Bakh_Bold
                    )
                }
                AppText(
                    "دسترسی سریع به تمام بخش‌ها",
                    fontSize = 14.sp,
                    color = Color(0xFF6C757D),
                    fontFamily = RNFonts.Yekan_Bakh_Regular
                )
            }

            // scrollContent
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // menuItems
                Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                    menuItems.filter { it.id != "logout" }.forEach { item ->
                        MenuItemRow(item, diagonal = true) { handleMenuItemPress(item.screen) }
                    }

                    // Contact Section
                    val cardShape = RoundedCornerShape(18.dp)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .background(menuCardBg, cardShape)
                            .border(1.dp, menuCardBorder, cardShape)
                            .padding(horizontal = 15.dp, vertical = 16.dp)
                    ) {
                        ContactButton(
                            colors = listOf(Color(0xFF1DB954), Color(0xFF0AA86E)),
                            title = "تماس با ما",
                            subtitle = "09937722986",
                            onClick = { openUrl("tel:09937722986") }
                        ) { MaterialIcons("phone", size = 26.dp, color = Color(0xFFFFFFFF)) }

                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(1.dp)
                                .background(Color(0xFFEFEFEF))
                        )

                        ContactButton(
                            colors = listOf(Color(0xFF4285F4), Color(0xFF6C63FF)),
                            title = "ارسال ایمیل",
                            subtitle = "farzann2386@gmail.com",
                            onClick = { openUrl("mailto:info@farimod.com") }
                        ) { MaterialCommunityIcons("email-outline", size = 26.dp, color = Color(0xFFFFFFFF)) }
                    }

                    // Logout
                    menuItems.filter { it.id == "logout" }.forEach { item ->
                        MenuItemRow(item, diagonal = false) { handleMenuItemPress(item.screen) }
                    }
                }
            }

            // fixedBottomSection
            Column(
                Modifier
                    .fillMaxWidth()
                    .modalBorderTop(1.dp, menuCardBorder)
                    .padding(top = 15.dp, bottom = 10.dp)
                    .background(Color(0xFFFFFFFF)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val supportShape = RoundedCornerShape(20.dp)
                RowReverse(
                    Modifier
                        .padding(bottom = 10.dp)
                        .touchableOpacity(activeOpacity = 0.8f) { handleCallSupport() }
                        .background(menuGreen, supportShape)
                        .border(1.dp, menuGreen, supportShape)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MaterialIcons("headset-mic", size = 20.dp, color = Color(0xFFFFFFFF))
                    AppText(
                        "پشتیبانی بله",
                        fontSize = 14.sp,
                        color = Color(0xFFFFFFFF),
                        fontFamily = RNFonts.Yekan_Bakh_Regular
                    )
                }
                val appVersion = remember { getAppVersionName() }
                AppText(
                    "نسخه ${toPersianDigits(appVersion)}",
                    fontSize = 13.sp,
                    color = Color(0xFF9CA3AF),
                    fontFamily = RNFonts.Yekan_Bakh_Regular
                )
            }
        }

        // ── Logout confirmation ──────────────────────────────────────────────
        if (showLogoutModal) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = logoutOpacityAnim.value }
                    .background(rgba(0, 0, 0, 0.7f))
                    .pressable { showLogoutModal = false }
                    .padding(horizontal = 30.dp),
                contentAlignment = Alignment.Center
            ) {
                val cardShape = RoundedCornerShape(20.dp)
                Column(
                    Modifier
                        .padding(bottom = maxOf(insets.bottom, 20.dp))
                        .fillMaxWidth()
                        .widthIn(max = 350.dp)
                        .graphicsLayer { translationY = logoutSlideAnim.value * density.density }
                        .rnShadow(15.dp, cardShape)
                        .background(Color(0xFFFFFFFF), cardShape)
                        .pressable { /* card swallows taps */ }
                        .padding(30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .padding(bottom = 20.dp)
                            .size(80.dp)
                            .background(Color(0xFFFEE2E2), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        MaterialIcons("logout", size = 48.dp, color = Color(0xFFEF4444))
                    }
                    AppText(
                        "خروج از حساب کاربری",
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        fontSize = 20.sp,
                        color = Color(0xFF1F2937),
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        textAlign = TextAlign.Center
                    )
                    AppText(
                        "آیا مطمئن هستید که می‌خواهید از حساب کاربری خود خارج شوید؟",
                        modifier = Modifier.fillMaxWidth().padding(bottom = 30.dp),
                        fontSize = 16.sp,
                        color = Color(0xFF6B7280),
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val btnShape = RoundedCornerShape(12.dp)
                        Box(
                            Modifier
                                .weight(1f)
                                .touchableOpacity { showLogoutModal = false }
                                .background(Color(0xFFF3F4F6), btnShape)
                                .border(1.dp, Color(0xFFD1D5DB), btnShape)
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AppText(
                                "انصراف",
                                fontSize = 16.sp,
                                color = Color(0xFF374151),
                                fontFamily = RNFonts.Yekan_Bakh_Regular,
                                textAlign = TextAlign.Center
                            )
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .touchableOpacity { handleLogoutConfirm() }
                                .background(Color(0xFFEF4444), btnShape)
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AppText(
                                "خروج",
                                fontSize = 16.sp,
                                color = Color(0xFFFFFFFF),
                                fontFamily = RNFonts.Yekan_Bakh_Regular,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // ── Support guide tooltip ────────────────────────────────────────────
        if (showSupportTooltip) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(rgba(0, 0, 0, 0.6f))
                    .pressable { showSupportTooltip = false }
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                val tooltipShape = RoundedCornerShape(24.dp)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = windowWidth - 40.dp)
                        .rnShadow(15.dp, tooltipShape)
                        .clip(tooltipShape)
                        .background(Color(0xFFFFFFFF))
                ) {
                    // tooltipHeader
                    LinearGradient(
                        colors = listOf(menuGreen, Color(0xFF059669)),
                        modifier = Modifier.fillMaxWidth(),
                        start = Offset(0f, 0f),
                        end = Offset(1f, 0f)
                    ) {
                        RowReverse(
                            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RowReverse(
                                Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(48.dp)
                                        .background(rgba(255, 255, 255, 0.2f), CircleShape)
                                        .border(2.dp, rgba(255, 255, 255, 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MaterialIcons("help", size = 32.dp, color = Color(0xFFFFFFFF))
                                }
                                AppText(
                                    "راهنمای تماس با پشتیبانی",
                                    modifier = Modifier.weight(1f),
                                    fontSize = 18.sp,
                                    color = Color(0xFFFFFFFF),
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    textAlign = TextAlign.Right
                                )
                            }
                            Box(
                                Modifier
                                    .touchableOpacity { showSupportTooltip = false }
                                    .size(36.dp)
                                    .background(rgba(255, 255, 255, 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                MaterialIcons("close", size = 24.dp, color = Color(0xFFFFFFFF))
                            }
                        }
                    }

                    // tooltipContent
                    Column(Modifier.fillMaxWidth().padding(24.dp)) {
                        AppText(
                            "برای ارتباط با پشتیبانی می‌توانید از طریق پیام‌رسان بله با ما در ارتباط باشید. بدین منظور از دکمه پشتیبانی بله در پایین منو استفاده کنید.",
                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                            fontSize = 16.sp,
                            color = Color(0xFF374151),
                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                            lineHeight = 28.sp,
                            textAlign = TextAlign.Justify
                        )
                        val gotItShape = RoundedCornerShape(16.dp)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .touchableOpacity(activeOpacity = 0.8f) { showSupportTooltip = false }
                                .rnShadow(6.dp, gotItShape, menuGreen)
                                .clip(gotItShape)
                        ) {
                            LinearGradient(
                                colors = listOf(menuGreen, Color(0xFF059669)),
                                modifier = Modifier.fillMaxWidth(),
                                start = Offset(0f, 0f),
                                end = Offset(1f, 0f)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MaterialIcons("check", size = 20.dp, color = Color(0xFFFFFFFF))
                                    AppText(
                                        "متوجه شدم",
                                        fontSize = 16.sp,
                                        color = Color(0xFFFFFFFF),
                                        fontFamily = RNFonts.Yekan_Bakh_Bold
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

/** RN `menuItem` card: gradient icon (56x56, marginLeft 15), title, chevron-left. */
@Composable
private fun MenuItemRow(item: MenuItemData, diagonal: Boolean, onPress: () -> Unit) {
    val cardShape = RoundedCornerShape(18.dp)
    val iconShape = RoundedCornerShape(16.dp)
    RowReverse(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .touchableOpacity(activeOpacity = 0.8f) { onPress() }
            .background(menuCardBg, cardShape)
            .border(1.dp, menuCardBorder, cardShape)
            .padding(horizontal = 15.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LinearGradient(
            colors = item.colors,
            modifier = Modifier
                .absolutePadding(left = 15.dp)
                .size(56.dp)
                .rnShadow(2.dp, iconShape),
            start = if (diagonal) Offset(0f, 0f) else Offset(0.5f, 0f),
            end = if (diagonal) Offset(1f, 1f) else Offset(0.5f, 1f),
            shape = iconShape
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MaterialIcons(item.icon, size = 28.dp, color = Color(0xFFFFFFFF))
            }
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            AppText(
                item.title,
                modifier = Modifier.padding(bottom = 4.dp),
                fontSize = 18.sp,
                color = menuTitleColor,
                fontFamily = RNFonts.Yekan_Bakh_Bold
            )
        }
        MaterialIcons("chevron-left", size = 24.dp, color = Color(0xFF9E9E9E))
    }
}

/** RN `contactButton`: 54x54 gradient icon, title (15 / 700) + subtitle (13 / #888). */
@Composable
private fun ContactButton(
    colors: List<Color>,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    RowReverse(
        Modifier
            .fillMaxWidth()
            .touchableOpacity(activeOpacity = 0.85f) { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LinearGradient(
            colors = colors,
            modifier = Modifier.size(54.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { icon() }
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            AppText(
                title,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 15.sp,
                color = Color(0xFF222222),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right
            )
            AppText(
                subtitle,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                fontSize = 13.sp,
                color = Color(0xFF888888),
                textAlign = TextAlign.Right
            )
        }
    }
}
