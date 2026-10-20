package ir.farimod.app.screens.subscription

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.rememberSafeAreaInsets
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.rnSpring
import ir.farimod.app.ui.rnTiming
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.ui.windowSize
import ir.farimod.app.utils.jsToLocaleString
import ir.farimod.app.utils.jsToString
import ir.farimod.app.utils.toPersianDigits
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SheetWhite = Color(0xFFFFFFFF)
private val Green1 = Color(0xFF10B981)
private val Green2 = Color(0xFF059669)
private val Purple1 = Color(0xFF667EEA)
private val Purple2 = Color(0xFF764BA2)
private val TitleDark = Color(0xFF2C3E50)
private val Muted = Color(0xFF64748B)
private val FeatureText = Color(0xFF475569)

internal fun subGroupedPrice(price: Double): String {
    return if (price <= 0.0) "رایگان" else "${toPersianDigits(jsToLocaleString(price))} تومان"
}

@Composable
internal fun ActiveSubscriptionBanner(active: ActiveSub) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .absolutePadding(left = 20.dp, right = 20.dp)
            .padding(bottom = 30.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Color(1f, 1f, 1f, 0.25f), shape)
                .border(1.dp, Color(56 / 255f, 161 / 255f, 105 / 255f, 0.3f), shape)
                .padding(18.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MaterialIcons(name = "verified", size = 24.dp, color = Color(0xFF38A169))
                Column(Modifier.weight(1f).absolutePadding(right = 12.dp)) {
                    AppText(
                        text = "پلن فعال: ${active.subscriptionPlanName}",
                        fontSize = 16.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = TitleDark,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    AppText(
                        text = "اعتبار تا: ${toPersianDigits(active.shamsiFinishDate)} (${toPersianDigits(jsToString(active.remainingDays))} روز باقی مانده)",
                        fontSize = 14.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        color = Muted
                    )
                }
            }
            SubscriptionProgressBar(active)
        }
    }
}

@Composable
private fun SubscriptionProgressBar(active: ActiveSub) {
    val raw = if (active.totalDays == 0.0) {
        Double.POSITIVE_INFINITY
    } else {
        (active.remainingDays / active.totalDays) * 100.0
    }
    val colors = when {
        raw <= 20.0 -> listOf(Color(0xFFEF4444), Color(0xFFDC2626))
        raw <= 40.0 -> listOf(Color(0xFFF59E0B), Color(0xFFD97706))
        else -> listOf(Green1, Green2)
    }
    val target = when {
        raw.isNaN() || raw < 0.0 -> 0f
        raw >= 100.0 -> 1f
        else -> (raw / 100.0).toFloat()
    }
    val anim = remember { Animatable(0f) }
    LaunchedEffect(active.subscriptionPlanId, active.remainingDays, active.totalDays) {
        anim.animateTo(target, rnTiming(1500))
    }
    val track = RoundedCornerShape(4.dp)
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .height(8.dp)
                .clip(track)
                .background(Color(1f, 1f, 1f, 0.3f))
                .border(1.dp, Color(1f, 1f, 1f, 0.2f), track)
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(anim.value.coerceIn(0f, 1f))
                    .align(AbsoluteAlignment.CenterRight)
                    .clip(track)
            ) {
                LinearGradient(
                    colors = colors,
                    modifier = Modifier.fillMaxSize(),
                    start = Offset(0f, 0f),
                    end = Offset(1f, 0f)
                )
            }
        }
    }
}

@Composable
internal fun SubscriptionPlanCard(
    item: SubPlan,
    onPress: (SubPlan) -> Unit,
    showToast: (String, String) -> Unit
) {
    var selectedOption by remember { mutableStateOf<SubOption?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var tempSelected by remember { mutableStateOf<SubOption?>(null) }
    val scope = rememberCoroutineScope()
    val slide = remember { Animatable(300f) }
    val fade = remember { Animatable(0f) }
    val shape = RoundedCornerShape(24.dp)
    val accent = subHex(item.color)
    val accentSoft = subHex("${item.color}20")

    fun openPicker() {
        val options = item.subscriptionOptions
        if (options.isEmpty()) {
            showToast("هیچ گزینه اشتراکی برای این پلن موجود نیست", "error")
            return
        }
        tempSelected = selectedOption
        showPicker = true
        scope.launch {
            coroutineScope {
                launch { slide.animateTo(0f, rnTiming(300)) }
                launch { fade.animateTo(1f, rnTiming(300)) }
            }
        }
    }

    fun closePicker() {
        scope.launch {
            coroutineScope {
                launch { slide.animateTo(300f, rnTiming(250)) }
                launch { fade.animateTo(0f, rnTiming(250)) }
            }
            showPicker = false
            tempSelected = null
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .clip(shape)
    ) {
        Box(Modifier.matchParentSize().background(Color(1f, 1f, 1f, 0.01f), shape))
        Box(
            Modifier
                .matchParentSize()
                .background(Color(1f, 1f, 1f, 0.05f), shape)
                .border(1.5.dp, accent, shape)
        )
        if (item.isPopular) {
            Box(
                Modifier
                    .align(AbsoluteAlignment.TopLeft)
                    .padding(start = 16.dp, top = 16.dp)
                    .zIndex(10f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(subHex(item.badgeBackgroundColor))
                    .absolutePadding(left = 12.dp, right = 12.dp, top = 6.dp, bottom = 6.dp)
            ) {
                AppText(
                    text = item.badgeText ?: "",
                    fontSize = 12.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color.White,
                    modifier = Modifier.absolutePadding(right = 6.dp)
                )
            }
        }
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            RowReverse(
                Modifier.fillMaxWidth().padding(bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .absolutePadding(left = 16.dp)
                        .size(56.dp)
                        .background(accentSoft, CircleShape)
                        .border(2.dp, accent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    MaterialIcons(name = item.icon, size = 32.dp, color = accent)
                }
                Column(Modifier.weight(1f)) {
                    AppText(
                        text = item.title,
                        fontSize = 22.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = TitleDark,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    if (item.isActive) {
                        RowReverse(
                            Modifier
                                .align(Alignment.Start)
                                .background(Color(56 / 255f, 161 / 255f, 105 / 255f, 0.1f), RoundedCornerShape(12.dp))
                                .absolutePadding(left = 8.dp, right = 8.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MaterialIcons(name = "check-circle", size = 14.dp, color = Color(0xFF38A169))
                            AppText(
                                text = "فعال",
                                fontSize = 11.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Regular,
                                color = Color(0xFF38A169),
                                modifier = Modifier.absolutePadding(right = 4.dp)
                            )
                        }
                    }
                }
            }

            Column(Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 28.dp)) {
                item.features.forEach { feature ->
                    RowReverse(
                        Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .absolutePadding(left = 12.dp)
                                .size(24.dp)
                                .background(subHex("#38a16920"), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            MaterialIcons(name = "check", size = 14.dp, color = Color(0xFF38A169))
                        }
                        AppText(
                            text = feature,
                            modifier = Modifier.weight(1f),
                            fontSize = 15.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                            color = FeatureText
                        )
                    }
                }
            }

            if (!item.isActive) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .touchableOpacity { openPicker() }
                ) {
                    LinearGradient(
                        colors = listOf(Green1, Green2),
                        modifier = Modifier.fillMaxWidth(),
                        start = Offset(0f, 0f),
                        end = Offset(1f, 1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        RowReverse(
                            Modifier.fillMaxWidth().absolutePadding(left = 24.dp, right = 24.dp, top = 16.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MaterialIcons(name = "rocket-launch", size = 18.dp, color = Color.White)
                            AppText(
                                text = "انتخاب پلن",
                                fontSize = 16.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Bold,
                                color = Color.White,
                                modifier = Modifier.absolutePadding(right = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPicker) {
        val density = LocalDensity.current.density
        val sheetHeight = windowSize().second * 0.85f
        Dialog(
            onDismissRequest = { closePicker() },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0f, 0f, 0f, 0.6f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .height(sheetHeight)
                        .graphicsLayer {
                            translationY = slide.value * density
                            alpha = fade.value
                        }
                        .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                        .background(SheetWhite)
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                val h = 1.dp.toPx()
                                drawRect(
                                    Color(0xFFF0F0F0),
                                    topLeft = Offset(0f, size.height - h),
                                    size = androidx.compose.ui.geometry.Size(size.width, h)
                                )
                            }
                            .padding(top = 12.dp, bottom = 20.dp)
                            .absolutePadding(left = 20.dp, right = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier
                                .padding(bottom = 15.dp)
                                .size(40.dp, 4.dp)
                                .background(Color(0xFFD1D5DB), RoundedCornerShape(2.dp))
                        )
                        RowReverse(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RowReverse(verticalAlignment = Alignment.CenterVertically) {
                                MaterialIcons(name = "auto-awesome-mosaic", size = 24.dp, color = Purple1)
                                AppText(
                                    text = "انتخاب مدت اشتراک",
                                    fontSize = 20.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_ExtraBold,
                                    color = Color(0xFF1F2937),
                                    modifier = Modifier.absolutePadding(right = 8.dp)
                                )
                            }
                        }
                    }

                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .absolutePadding(left = 20.dp, right = 20.dp)
                            .padding(top = 8.dp, bottom = 20.dp)
                    ) {
                        item.subscriptionOptions.forEach { option ->
                            val picked = tempSelected?.value == option.value
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .absolutePadding(top = 3.dp, bottom = 3.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .touchableOpacity(activeOpacity = 0.7f) { tempSelected = option }
                            ) {
                                LinearGradient(
                                    colors = if (picked) listOf(Purple1, Purple2) else listOf(Color(0xFFF8FAFC), Color(0xFFF8FAFC)),
                                    modifier = Modifier.fillMaxWidth(),
                                    start = Offset(0f, 0f),
                                    end = Offset(1f, 1f),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    RowReverse(
                                        Modifier.fillMaxWidth().absolutePadding(left = 16.dp, right = 16.dp, top = 16.dp, bottom = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RowReverse(
                                            Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                Modifier
                                                    .absolutePadding(left = 12.dp)
                                                    .size(40.dp)
                                                    .background(
                                                        if (picked) Color(1f, 1f, 1f, 0.2f) else Color(102 / 255f, 126 / 255f, 234 / 255f, 0.1f),
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                MaterialIcons(
                                                    name = option.icon,
                                                    size = 20.dp,
                                                    color = if (picked) Color.White else Purple1
                                                )
                                            }
                                            Column(Modifier.weight(1f)) {
                                                AppText(
                                                    text = option.label,
                                                    fontSize = 16.sp,
                                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                                    color = if (picked) Color.White else TitleDark,
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                )
                                                AppText(
                                                    text = subGroupedPrice(option.price),
                                                    fontSize = 14.sp,
                                                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                                                    color = if (picked) Color(1f, 1f, 1f, 0.8f) else Color(0xFF9CA3AF)
                                                )
                                            }
                                        }
                                        if (picked) {
                                            MaterialIcons(name = "check", size = 18.dp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        val chosen = tempSelected
                        if (chosen != null) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp, bottom = 20.dp)
                                    .drawBehind {
                                        val h = 1.dp.toPx()
                                        drawRect(Color(0xFFF0F0F0), size = androidx.compose.ui.geometry.Size(size.width, h))
                                    }
                                    .absolutePadding(top = 20.dp, bottom = 20.dp)
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                        .padding(16.dp)
                                ) {
                                    SummaryLine("پلن انتخابی:", item.title)
                                    SummaryLine("مدت اشتراک:", chosen.duration)
                                    SummaryLine("قیمت:", subGroupedPrice(chosen.price))
                                }
                            }
                        }
                    }

                    RowReverse(
                        Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                val h = 1.dp.toPx()
                                drawRect(Color(0xFFF0F0F0), size = androidx.compose.ui.geometry.Size(size.width, h))
                            }
                            .absolutePadding(left = 20.dp, right = 20.dp, top = 20.dp, bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RowReverse(
                            Modifier
                                .weight(1f)
                                .touchableOpacity(activeOpacity = 0.8f) { closePicker() }
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .absolutePadding(top = 14.dp, bottom = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MaterialIcons(name = "close", size = 20.dp, color = Color(0xFF34495E))
                            AppText(
                                text = "انصراف",
                                fontSize = 16.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Bold,
                                color = Color(0xFF34495E),
                                modifier = Modifier.absolutePadding(right = 6.dp)
                            )
                        }
                        Box(
                            Modifier
                                .weight(2f)
                                .clip(RoundedCornerShape(12.dp))
                                .touchableOpacity(enabled = chosenEnabled(tempSelected), activeOpacity = 0.8f) {
                                    val picked = tempSelected
                                    if (picked != null) {
                                        selectedOption = picked
                                        onPress(item.copy(selectedOption = picked))
                                    }
                                    closePicker()
                                }
                        ) {
                            LinearGradient(
                                colors = if (tempSelected != null) listOf(Green1, Green2) else listOf(Color(0xFF9CA3AF), Color(0xFF6B7280)),
                                modifier = Modifier.fillMaxWidth(),
                                start = Offset(0f, 0f),
                                end = Offset(1f, 1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                RowReverse(
                                    Modifier.fillMaxWidth().absolutePadding(top = 14.dp, bottom = 14.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MaterialIcons(name = "check", size = 20.dp, color = Color.White)
                                    AppText(
                                        text = "تأیید انتخاب",
                                        fontSize = 16.sp,
                                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                                        color = Color.White,
                                        modifier = Modifier.absolutePadding(right = 6.dp)
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

private fun chosenEnabled(option: SubOption?): Boolean = option != null

@Composable
private fun SummaryLine(label: String, value: String) {
    RowReverse(
        Modifier.fillMaxWidth().padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppText(text = label, fontSize = 14.sp, fontFamily = RNFonts.Yekan_Bakh_Regular, color = Muted)
        AppText(text = value, fontSize = 14.sp, fontFamily = RNFonts.Yekan_Bakh_Bold, color = TitleDark)
    }
}

@Composable
internal fun SubscriptionConfirmDialog(
    visible: Boolean,
    type: String,
    plan: SubPlan?,
    onClose: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!visible) return
    val isCancel = type == "cancel"
    val iconName = if (isCancel) "warning" else "help-outline"
    val iconColor = if (isCancel) Color(0xFFEF4444) else Color(0xFF3B82F6)
    val iconBg = if (isCancel) Color(0xFFFEE2E2) else Color(0xFFEBF8FF)
    val confirmColor = if (isCancel) Color(0xFFEF4444) else Green1
    val title = if (isCancel) "لغو اشتراک" else "تایید خرید اشتراک"
    val message = if (isCancel) {
        "آیا می‌خواهید اشتراک ${plan?.title ?: ""} را لغو کنید؟"
    } else {
        "آیا می‌خواهید به صفحه پرداخت بروید؟"
    }
    val confirmText = if (isCancel) "بله، لغو کن" else "ادامه خرید"
    val option = plan?.selectedOption
    val insets = rememberSafeAreaInsets()
    val bottom = if (insets.bottom > 20.dp) insets.bottom else 20.dp
    val density = LocalDensity.current.density
    val slide = remember { Animatable(300f) }
    val fade = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        coroutineScope {
            launch { slide.animateTo(0f, rnSpring(100f, 8f)) }
            launch { fade.animateTo(1f, rnTiming(200)) }
        }
    }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0f, 0f, 0f, 0.7f))
                .touchableOpacity(activeOpacity = 1f) { onClose() }
                .absolutePadding(left = 30.dp, right = 30.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 350.dp)
                    .graphicsLayer {
                        translationY = slide.value * density
                        alpha = fade.value
                    }
                    .padding(bottom = bottom)
                    .rnShadow(15.dp, RoundedCornerShape(20.dp), Color(0f, 0f, 0f, 0.25f))
                    .background(SheetWhite, RoundedCornerShape(20.dp))
                    .padding(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .padding(bottom = 20.dp)
                        .size(80.dp)
                        .background(iconBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    MaterialIcons(name = iconName, size = 48.dp, color = iconColor)
                }
                AppText(
                    text = title,
                    fontSize = 20.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color(0xFF1F2937),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )
                AppText(
                    text = message,
                    fontSize = 16.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                )
                if (!isCancel && plan != null && option != null) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                            .background(Color(0xFFF9FAFB), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(12.dp))
                            .padding(16.dp)
                    ) {
                        PlanDetailRow("پلن:", plan.title, price = false)
                        PlanDetailRow("مدت:", option.duration, price = false)
                        PlanDetailRow("قیمت:", subGroupedPrice(option.price), price = true)
                    }
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .touchableOpacity { onClose() }
                            .background(Color(0xFFF3F4F6), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(12.dp))
                            .absolutePadding(top = 14.dp, bottom = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppText(
                            text = "انصراف",
                            fontSize = 16.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                            color = Color(0xFF374151),
                            textAlign = TextAlign.Center
                        )
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .touchableOpacity { onConfirm() }
                            .background(confirmColor, RoundedCornerShape(12.dp))
                            .absolutePadding(top = 14.dp, bottom = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppText(
                            text = confirmText,
                            fontSize = 16.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanDetailRow(label: String, value: String, price: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppText(
            text = value,
            fontSize = if (price) 16.sp else 14.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = if (price) Green1 else Color(0xFF1F2937)
        )
        AppText(
            text = label,
            fontSize = 14.sp,
            fontFamily = RNFonts.Yekan_Bakh_Regular,
            color = Color(0xFF6B7280)
        )
    }
}

@Composable
internal fun SubscriptionSuccessDialog(
    visible: Boolean,
    type: String,
    plan: SubPlan?,
    onClose: () -> Unit
) {
    if (!visible) return
    val isCancel = type == "cancel"
    val title = if (isCancel) "لغو شد" else "موفق"
    val message = if (isCancel) {
        "اشتراک ${plan?.title ?: ""} لغو شد."
    } else {
        "پلن ${plan?.title ?: ""} با موفقیت انتخاب شد."
    }
    val insets = rememberSafeAreaInsets()
    val bottom = if (insets.bottom > 20.dp) insets.bottom else 20.dp
    val density = LocalDensity.current.density
    val slide = remember { Animatable(300f) }
    val fade = remember { Animatable(0f) }
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        coroutineScope {
            launch { slide.animateTo(0f, rnSpring(100f, 8f)) }
            launch { fade.animateTo(1f, rnTiming(200)) }
            launch {
                delay(100)
                scale.animateTo(1f, rnSpring(100f, 8f))
            }
        }
    }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0f, 0f, 0f, 0.7f))
                .touchableOpacity(activeOpacity = 1f) { onClose() }
                .absolutePadding(left = 30.dp, right = 30.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 350.dp)
                    .graphicsLayer {
                        translationY = slide.value * density
                        alpha = fade.value
                    }
                    .padding(bottom = bottom)
                    .rnShadow(15.dp, RoundedCornerShape(20.dp), Color(0f, 0f, 0f, 0.25f))
                    .background(SheetWhite, RoundedCornerShape(20.dp))
                    .padding(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .padding(bottom = 20.dp)
                        .graphicsLayer {
                            scaleX = scale.value
                            scaleY = scale.value
                        }
                        .size(80.dp)
                        .background(subHex("#38a16920"), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    MaterialIcons(name = "check-circle", size = 48.dp, color = Color(0xFF38A169))
                }
                AppText(
                    text = title,
                    fontSize = 20.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color(0xFF1F2937),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )
                AppText(
                    text = message,
                    fontSize = 16.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .touchableOpacity { onClose() }
                        .background(Green1, RoundedCornerShape(12.dp))
                        .absolutePadding(left = 20.dp, right = 20.dp, top = 16.dp, bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AppText(
                        text = "باشه",
                        fontSize = 18.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
