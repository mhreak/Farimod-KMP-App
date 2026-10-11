package ir.farimod.app.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.navigation.Tabs
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.navigation.useRoute
import ir.farimod.app.network.double
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.str
import ir.farimod.app.platform.isIOS
import ir.farimod.app.platform.openUrl
import ir.farimod.app.screens.subscription.SubOption
import ir.farimod.app.screens.subscription.SubPlan
import ir.farimod.app.services.SubscriptionService
import ir.farimod.app.state.AuthState
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.MainBackground
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.ToastData
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.rnTextStyle
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.utils.formatPrice
import ir.farimod.app.utils.toPersianDigits
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlin.math.max

private val SpPrimary = Color(0xFF667EEA)
private val SpPrimaryDark = Color(0xFF764BA2)
private val SpDark = Color(0xFF2C3E50)
private val SpMedium = Color(0xFF34495E)
private val SpPage = Color(0xFFF5F3FF)

private val SpTiming: Easing = run {
    val ease = CubicBezierEasing(0.42f, 0f, 1f, 1f)
    Easing { t ->
        if (t < 0.5f) ease.transform(t * 2f) / 2f
        else 1f - ease.transform((1f - t) * 2f) / 2f
    }
}

@Composable
fun SubscriptionPurchaseScreen() {
    val navigation = useNavigation()
    val route = useRoute()
    val plan = route.params["plan"].planJson()
    val option = route.params["selectedOption"].optionJson() ?: (plan?.get("selectedOption") as? JsonObject)
    val user = AuthState.user

    var discountCode by remember { mutableStateOf("") }
    var discountAmount by remember { mutableDoubleStateOf(0.0) }
    var isCheckingCode by remember { mutableStateOf(false) }
    var codeApplied by remember { mutableStateOf(false) }
    var isPurchasing by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf(ToastData()) }

    val fade = remember { Animatable(0f) }
    val slide = remember { Animatable(50f) }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    LaunchedEffect(Unit) {
        launch { fade.animateTo(1f, tween(600, easing = SpTiming)) }
        launch { slide.animateTo(0f, tween(600, easing = SpTiming)) }
    }

    fun showToast(message: String, type: String = "info") {
        toast = ToastData(true, message, type)
    }

    fun goHome() {
        navigation.navigate("MainTabs", mapOf("screen" to Tabs.Home))
    }

    val planColor = spHex(plan.str("color"))
    val planColorSoft = spHex((plan.str("color") ?: "") + "CC")
    val planIcon = plan.str("icon")?.takeIf { it.isNotEmpty() } ?: "star"
    val planTitle = plan.str("title") ?: ""
    val durationLabel = option.str("duration") ?: ""
    val optionValue = option.str("value") ?: ""
    val originalPrice = option["price"].double ?: 0.0
    val finalPrice = max(0.0, originalPrice - discountAmount)
    val bar = statusBarHeight()
    val backTop = if (isIOS) 40.dp else bar + 12.dp
    val headerTop = if (isIOS) 90.dp else bar + 70.dp
    val bottomPad = if (isIOS) 32.dp else 20.dp
    val motion = Modifier.graphicsLayer {
        alpha = fade.value
        translationY = slide.value * density.density
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(SpPage)
            .then(if (isIOS) Modifier.imePadding() else Modifier)
    ) {
        MainBackground()
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(bottom = 120.dp)
        ) {
            Column(
                motion
                    .fillMaxWidth()
                    .padding(top = headerTop)
                    .absolutePadding(left = 20.dp, right = 20.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.padding(bottom = 16.dp)) {
                    LinearGradient(
                        colors = listOf(planColor, planColorSoft),
                        modifier = Modifier
                            .size(72.dp)
                            .rnShadow(6.dp, CircleShape, Color(0xFF667EEA))
                            .clip(CircleShape),
                        start = Offset(0f, 0f),
                        end = Offset(1f, 1f),
                        shape = CircleShape
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            MaterialIcons(name = planIcon, size = 32.dp, color = Color.White)
                        }
                    }
                }
                AppText(
                    text = "خرید اشتراک",
                    fontSize = 22.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = SpDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                AppText(
                    text = "اطلاعات پرداخت",
                    fontSize = 13.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    color = SpMedium,
                    textAlign = TextAlign.Center
                )
            }

            Column(motion.fillMaxWidth().absolutePadding(left = 20.dp, right = 20.dp)) {
                SpPlanCard(planColor, planIcon, planTitle, durationLabel)
                SpDiscountCard(
                    discountCode = discountCode,
                    onCode = { discountCode = it },
                    isCheckingCode = isCheckingCode,
                    codeApplied = codeApplied,
                    discountAmount = discountAmount,
                    onApply = {
                        if (discountCode.trim().isEmpty()) {
                            showToast("لطفا کد تخفیف را وارد کنید", "error")
                        } else {
                        val planId = plan["id"].int ?: 0
                        scope.launch {
                            isCheckingCode = true
                            try {
                                val days = SubscriptionService.calculateDays(optionValue)
                                val result = SubscriptionService.applyDiscountCode(planId, discountCode, days)
                                if (result.success) {
                                    discountAmount = result.data["Discount"].double ?: 0.0
                                    codeApplied = true
                                    showToast("کد تخفیف با موفقیت اعمال شد", "success")
                                } else {
                                    showToast(result.message ?: "", "error")
                                    discountAmount = 0.0
                                    codeApplied = false
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                showToast("خطا در اعمال کد تخفیف", "error")
                                discountAmount = 0.0
                                codeApplied = false
                            } finally {
                                isCheckingCode = false
                            }
                        }
                        }
                    }
                )
                SpSummaryCard(originalPrice, discountAmount, finalPrice)
            }
        }

        Box(
            Modifier
                .align(AbsoluteAlignment.TopRight)
                .zIndex(10f)
                .absolutePadding(top = backTop, right = 16.dp)
                .touchableOpacity(activeOpacity = 0.7f) { goHome() }
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .rnShadow(3.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                MaterialIcons(name = "arrow-forward", size = 24.dp, color = SpDark)
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.White)
                .drawBehind {
                    drawLine(
                        color = Color(0xFFF3F4F6),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .absolutePadding(left = 20.dp, right = 20.dp)
                .padding(top = 12.dp, bottom = bottomPad)
        ) {
            val payShape = RoundedCornerShape(16.dp)
            Box(
                Modifier
                    .padding(bottom = 50.dp)
                    .then(if (isPurchasing) Modifier.alpha(0.6f) else Modifier)
                    .rnShadow(6.dp, payShape, Color(0xFF2ECC71))
                    .clip(payShape)
                    .touchableOpacity(enabled = !isPurchasing, activeOpacity = 0.8f) {
                        val memberId = user?.MemberId
                        if (memberId == null || memberId == 0) {
                            showToast("لطفا ابتدا وارد حساب کاربری خود شوید", "error")
                            return@touchableOpacity
                        }
                        val planId = plan["id"].int ?: 0
                        scope.launch {
                            isPurchasing = true
                            try {
                                val days = SubscriptionService.calculateDays(optionValue)
                                val result = SubscriptionService.buySubscriptionPlan(
                                    memberId,
                                    planId,
                                    days,
                                    if (codeApplied) discountCode else ""
                                )
                                if (result.success) {
                                    val gateway = result.data["GatewayURL"].str
                                    if (!gateway.isNullOrEmpty()) {
                                        showToast("در حال انتقال به درگاه پرداخت...", "success")
                                        launch {
                                            delay(1500)
                                            openUrl(gateway)
                                            goHome()
                                        }
                                    } else {
                                        showToast("خرید با موفقیت انجام شد", "success")
                                        launch {
                                            delay(1500)
                                            goHome()
                                        }
                                    }
                                } else {
                                    showToast(result.message ?: "", "error")
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                showToast("خطا در خرید اشتراک. لطفا دوباره تلاش کنید", "error")
                            } finally {
                                isPurchasing = false
                            }
                        }
                    }
            ) {
                LinearGradient(
                    colors = listOf(Color(0xFF2ECC71), Color(0xFF27AE60)),
                    modifier = Modifier.fillMaxWidth(),
                    start = Offset(0f, 0f),
                    end = Offset(1f, 0f)
                ) {
                    if (isPurchasing) {
                        Box(Modifier.fillMaxWidth().absolutePadding(top = 16.dp, bottom = 16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                    } else {
                        RowReverse(
                            Modifier.fillMaxWidth().absolutePadding(left = 20.dp, right = 20.dp, top = 16.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MaterialIcons(name = "credit-card", size = 22.dp, color = Color.White)
                            AppText(
                                text = "خرید اشتراک",
                                fontSize = 16.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            AppText(
                                text = toPersianDigits(formatPrice(finalPrice)),
                                fontSize = 14.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Regular,
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Toast(toast.visible, toast.message, toast.type) { toast = toast.copy(visible = false) }
    }
}

@Composable
private fun SpPlanCard(planColor: Color, planIcon: String, planTitle: String, durationLabel: String) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .rnShadow(2.dp, shape)
            .clip(shape)
            .background(Color.White)
            .padding(20.dp)
    ) {
        RowReverse(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .absolutePadding(left = 8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(planColor.copy(alpha = 0.125f)),
                contentAlignment = Alignment.Center
            ) {
                MaterialIcons(name = planIcon, size = 20.dp, color = planColor)
            }
            AppText(
                text = "پلن انتخابی",
                fontSize = 13.sp,
                fontFamily = RNFonts.Yekan_Bakh_Regular,
                color = SpMedium
            )
        }
        AppText(
            text = planTitle,
            fontSize = 18.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = SpDark,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )
        RowReverse(
            Modifier
                .align(Alignment.Start)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF9FAFB))
                .absolutePadding(left = 12.dp, right = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MaterialIcons(name = "schedule", size = 16.dp, color = SpMedium)
            AppText(
                text = "مدت اشتراک: $durationLabel",
                fontSize = 13.sp,
                fontFamily = RNFonts.Yekan_Bakh_Regular,
                color = SpMedium,
                modifier = Modifier.absolutePadding(right = 6.dp)
            )
        }
    }
}

@Composable
private fun SpDiscountCard(
    discountCode: String,
    onCode: (String) -> Unit,
    isCheckingCode: Boolean,
    codeApplied: Boolean,
    discountAmount: Double,
    onApply: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val applyDisabledLook = isCheckingCode || discountCode.trim().isEmpty()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .rnShadow(2.dp, shape)
            .clip(shape)
            .background(Color.White)
            .padding(20.dp)
    ) {
        AppText(
            text = "کد تخفیف دارید؟",
            fontSize = 15.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = SpDark,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        )
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                .widthIn(min = 80.dp)
                    .then(if (applyDisabledLook) Modifier.alpha(0.6f) else Modifier)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (applyDisabledLook) Color(0xFFD1D5DB) else Color(0xFFF59E0B))
                    .touchableOpacity(
                        enabled = !isCheckingCode && discountCode.trim().isNotEmpty() && !codeApplied,
                        activeOpacity = 0.7f,
                        onClick = onApply
                    )
                    .absolutePadding(left = 24.dp, right = 24.dp, top = 13.dp, bottom = 13.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isCheckingCode) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    AppText(
                        text = if (codeApplied) "✓" else "اعمال",
                        fontSize = 14.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
            val fieldBg = if (codeApplied) Color(0xFFF0FDF4) else Color(0xFFF9FAFB)
            val fieldBorder = if (codeApplied) Color(0xFF86EFAC) else Color(0xFFE5E7EB)
            BasicTextField(
                value = discountCode,
                onValueChange = onCode,
                enabled = !codeApplied,
                singleLine = true,
                textStyle = rnTextStyle(
                    fontSize = 14.sp,
                    color = SpDark,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    textAlign = TextAlign.Right
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                cursorBrush = SolidColor(SpDark),
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(fieldBg)
                    .border(1.dp, fieldBorder, RoundedCornerShape(12.dp))
                    .absolutePadding(left = 16.dp, right = 16.dp, top = 13.dp, bottom = 13.dp),
                decorationBox = { inner ->
                    Box {
                        if (discountCode.isEmpty()) {
                            AppText(
                                text = "کد تخفیف خود را وارد کنید",
                                fontSize = 14.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Regular,
                                color = Color(0xFF9CA3AF),
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        inner()
                    }
                }
            )
        }
        if (codeApplied && discountAmount > 0.0) {
            Row(
                Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF0FDF4))
            ) {
                Box(Modifier.width(3.dp).fillMaxHeight().background(Color(0xFF22C55E)))
                AppText(
                    text = "✓ ${toPersianDigits(formatPrice(discountAmount))} تومان تخفیف اعمال شد",
                    fontSize = 13.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    color = Color(0xFF15803D),
                    textAlign = TextAlign.Right,
                    modifier = Modifier.weight(1f).padding(12.dp)
                )
            }
        }
    }
}

@Composable
private fun SpSummaryCard(originalPrice: Double, discountAmount: Double, finalPrice: Double) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .rnShadow(2.dp, shape)
            .clip(shape)
            .background(Color.White)
    ) {
        LinearGradient(
            colors = listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
            modifier = Modifier.fillMaxWidth(),
            start = Offset(0f, 0f),
            end = Offset(1f, 0f)
        ) {
            RowReverse(
                Modifier.fillMaxWidth().absolutePadding(left = 16.dp, right = 16.dp, top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MaterialIcons(name = "receipt", size = 20.dp, color = Color.White)
                AppText(
                    text = "خلاصه پرداخت",
                    fontSize = 14.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color.White
                )
            }
        }
        Column(Modifier.padding(20.dp)) {
            SummaryLine("قیمت اشتراک:", toPersianDigits(formatPrice(originalPrice)), false)
            if (discountAmount > 0.0) {
                SummaryLine("تخفیف:", "- ${toPersianDigits(formatPrice(discountAmount))}", true)
            }
            Box(Modifier.fillMaxWidth().absolutePadding(top = 12.dp, bottom = 12.dp).height(1.dp).background(Color(0xFFF3F4F6)))
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF0F4FF))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText(
                    text = toPersianDigits(formatPrice(finalPrice)),
                    fontSize = 18.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color(0xFF667EEA)
                )
                AppText(
                    text = "مبلغ قابل پرداخت:",
                    fontSize = 14.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = SpDark
                )
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, discount: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppText(
            text = value,
            fontSize = 14.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = if (discount) Color(0xFF22C55E) else SpDark
        )
        AppText(
            text = label,
            fontSize = 14.sp,
            fontFamily = RNFonts.Yekan_Bakh_Regular,
            color = SpMedium
        )
    }
}

private fun Any?.planJson(): JsonObject? = when (this) {
    is JsonObject -> this
    is SubPlan -> toJson()
    else -> null
}

private fun Any?.optionJson(): JsonObject? = when (this) {
    is JsonObject -> this
    is SubOption -> toJson()
    else -> null
}

/** `#RGB` / `#RRGGBB` / `#RRGGBBAA` (alpha last). */
private fun spHex(value: String?): Color {
    val s = (value ?: return Color.Transparent).trim().removePrefix("#")
    return try {
        when (s.length) {
            3 -> Color(
                s.substring(0, 1).repeat(2).toInt(16),
                s.substring(1, 2).repeat(2).toInt(16),
                s.substring(2, 3).repeat(2).toInt(16)
            )
            6 -> Color(s.substring(0, 2).toInt(16), s.substring(2, 4).toInt(16), s.substring(4, 6).toInt(16))
            8 -> Color(
                s.substring(0, 2).toInt(16),
                s.substring(2, 4).toInt(16),
                s.substring(4, 6).toInt(16),
                s.substring(6, 8).toInt(16)
            )
            else -> Color.Transparent
        }
    } catch (_: NumberFormatException) {
        Color.Transparent
    }
}
