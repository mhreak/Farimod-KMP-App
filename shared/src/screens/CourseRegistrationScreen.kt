package ir.farimod.app.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.ktor.client.request.put
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.navigation.Tabs
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.navigation.useRoute
import ir.farimod.app.network.Api
import ir.farimod.app.network.FarimodHttpClient
import ir.farimod.app.network.bool
import ir.farimod.app.network.get
import ir.farimod.app.network.jsonOf
import ir.farimod.app.network.str
import ir.farimod.app.platform.isIOS
import ir.farimod.app.state.AuthState
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.MainBackground
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.ToastData
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.rnTiming
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.utils.formatPrice
import ir.farimod.app.utils.safeNumber
import ir.farimod.app.utils.toPersianDigits
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlin.math.max

private val RegPrimary = Color(0xFF667EEA)
private val RegDark = Color(0xFF2C3E50)
private val RegMedium = Color(0xFF34495E)
private const val MYKET_TOKEN =
    "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQChXXM0/itjd4zMeRiTBoASWlgVRRSX4of23aNP7mXLHwzn5P7nOIcD9ObrE0fIludBGRffeSgkAmMDeqC6SY6fou5WJMVbOvZi7bbG2s9dbIeVy6TseHf0u/Y05e6RxCpUqDZ6UNJfwN9gyLcTnJkim9U9be7ibkS421RPcz8PZwIDAQAB"

@Composable
fun CourseRegistrationScreen() {
    val navigation = useNavigation()
    val route = useRoute()
    val scope = rememberCoroutineScope()
    val courseId = route.long("courseId")?.takeIf { it != 0L }
    val user = AuthState.user
    var course by remember { mutableStateOf<JsonElement?>(null) }
    var loading by remember { mutableStateOf(true) }
    var discountCode by remember { mutableStateOf("") }
    var discountAmount by remember { mutableStateOf(0.0) }
    var checkingCode by remember { mutableStateOf(false) }
    var codeApplied by remember { mutableStateOf(false) }
    var registering by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf(ToastData()) }
    val fade = remember { Animatable(0f) }
    val slide = remember { Animatable(50f) }
    val density = LocalDensity.current
    val top = statusBarHeight()
    val backTop = if (top > 0.dp) top + 12.dp else 40.dp

    fun showToast(message: String, type: String = "info") {
        toast = ToastData(true, message, type)
    }

    fun goHome() {
        navigation.navigate("MainTabs", mapOf("screen" to Tabs.Home))
    }

    suspend fun fetchCourse() {
        val id = courseId ?: return
        loading = true
        try {
            val response = Api.get("Course/Get", query = mapOf("courseId" to id))
            if (!response.ok) throw IllegalStateException("HTTP error! status: ${response.status}")
            val body = response.json["Course"]
            if (body == null) throw IllegalStateException("Invalid data structure")
            course = body
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (_: Exception) {
            showToast("خطا در دریافت اطلاعات دوره", "error")
            course = null
        } finally {
            loading = false
        }
    }

    LaunchedEffect(courseId) {
        if (courseId != null) fetchCourse()
    }

    LaunchedEffect(course != null) {
        if (course != null) {
            coroutineScope {
                launch { fade.animateTo(1f, rnTiming(600)) }
                launch { slide.animateTo(0f, rnTiming(600)) }
            }
        }
    }

    suspend fun applyCode() {
        val id = courseId ?: return
        if (discountCode.isBlank()) {
            showToast("لطفا کد تخفیف را وارد کنید", "error")
            return
        }
        if (!course["AllowDiscountCode"].bool) {
            showToast("این دوره امکان استفاده از کد تخفیف را ندارد", "error")
            return
        }
        checkingCode = true
        try {
            val response = Api.get(
                "CourseRegistration/ApplyDiscountCode",
                query = mapOf("courseId" to id, "code" to discountCode)
            )
            if (!response.ok) {
                showToast(response.json.str("Message") ?: "کد تخفیف معتبر نیست", "error")
                discountAmount = 0.0
                codeApplied = false
                return
            }
            discountAmount = safeNumber(response.json["Discount"])
            codeApplied = true
            showToast("کد تخفیف با موفقیت اعمال شد", "success")
        } catch (_: Exception) {
            showToast("خطا در اعمال کد تخفیف", "error")
            discountAmount = 0.0
            codeApplied = false
        } finally {
            checkingCode = false
        }
    }

    suspend fun register() {
        val id = courseId ?: return
        val memberId = user?.MemberId
        if (memberId == null) {
            showToast("لطفا ابتدا وارد حساب کاربری خود شوید", "error")
            return
        }
        if (!course["RegisterActive"].bool) {
            showToast("ثبت‌نام در این دوره غیرفعال است", "error")
            return
        }
        registering = true
        try {
            val response = Api.post(
                "CourseRegistration/Register",
                body = jsonOf(
                    "CourseId" to id,
                    "MemberId" to memberId,
                    "DiscountCode" to if (codeApplied) discountCode else ""
                )
            )
            if (!response.ok) {
                showToast(response.json.str("Message") ?: "خطا در ثبت‌نام", "error")
                return
            }
            FarimodHttpClient.client.put(
                "https://developer.myket.ir/api/partners/applications/ir.farimod.app/purchases/products/$id/tokens/$MYKET_TOKEN/consume"
            )
        } catch (_: Exception) {
            showToast("خطا در ثبت‌نام. لطفا دوباره تلاش کنید", "error")
        } finally {
            registering = false
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFFF5F3FF)).imePadding()) {
        MainBackground()
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = RegPrimary)
                    AppText(
                        text = "در حال بارگذاری...",
                        modifier = Modifier.padding(top = 16.dp),
                        fontSize = 16.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        color = RegMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (course == null) {
            Column(
                Modifier.fillMaxSize().absolutePadding(left = 40.dp, right = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                MaterialIcons("error-outline", size = 80.dp, color = RegMedium)
                AppText(
                    text = "دوره یافت نشد",
                    modifier = Modifier.padding(top = 20.dp),
                    fontSize = 20.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = RegDark,
                    textAlign = TextAlign.Center
                )
                AppText(
                    text = "اطلاعات دوره موجود نیست",
                    modifier = Modifier.padding(top = 12.dp),
                    fontSize = 16.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    color = RegMedium,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
                Box(
                    Modifier
                        .padding(top = 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(RegPrimary)
                        .touchableOpacity { scope.launch { fetchCourse() } }
                        .absolutePadding(left = 32.dp, right = 32.dp, top = 12.dp, bottom = 12.dp)
                ) {
                    AppText(
                        text = "تلاش مجدد",
                        fontSize = 15.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val data = course!!
            val registerAmount = safeNumber(data["RegisterAmount"])
            val finalPrice = max(0.0, registerAmount - discountAmount)
            val active = data["RegisterActive"].bool
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 120.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = if (top > 0.dp) top + 70.dp else 90.dp)
                        .absolutePadding(left = 20.dp, right = 20.dp)
                        .padding(bottom = 32.dp)
                        .graphicsLayer {
                            alpha = fade.value
                            translationY = slide.value * density.density
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearGradient(
                        colors = listOf(Color(0xFF7C91EE), Color(0xFF5A67D8)),
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .size(72.dp)
                            .rnShadow(6.dp, CircleShape, RegPrimary),
                        start = Offset(0f, 0f),
                        end = Offset(1f, 1f),
                        shape = CircleShape
                    ) {
                        MaterialIcons("book", size = 32.dp, color = Color.White, modifier = Modifier.align(Alignment.Center))
                    }
                    AppText(
                        text = "ثبت‌نام در دوره",
                        modifier = Modifier.padding(bottom = 6.dp),
                        fontSize = 22.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = RegDark,
                        textAlign = TextAlign.Center
                    )
                    AppText(
                        text = "اطلاعات پرداخت",
                        fontSize = 13.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        color = RegMedium,
                        textAlign = TextAlign.Center
                    )
                }

                Column(
                    Modifier
                        .absolutePadding(left = 20.dp, right = 20.dp)
                        .graphicsLayer {
                            alpha = fade.value
                            translationY = slide.value * density.density
                        }
                ) {
                    val cardShape = RoundedCornerShape(16.dp)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .rnShadow(2.dp, cardShape)
                            .clip(cardShape)
                            .background(Color.White)
                            .padding(20.dp)
                    ) {
                        RowReverse(
                            Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .absolutePadding(left = 8.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE0F2FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                MaterialIcons("menu-book", size = 20.dp, color = Color(0xFF3498DB))
                            }
                            AppText(
                                text = "نام دوره",
                                fontSize = 13.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Regular,
                                color = RegMedium
                            )
                        }
                        AppText(
                            text = data.str("CourseName")?.takeIf { it.isNotEmpty() } ?: "نام دوره مشخص نشده",
                            fontSize = 16.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            color = RegDark,
                            textAlign = TextAlign.Right,
                            lineHeight = 26.sp
                        )
                    }

                    if (data["AllowDiscountCode"].bool) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .rnShadow(2.dp, cardShape)
                                .clip(cardShape)
                                .background(Color.White)
                                .padding(20.dp)
                        ) {
                            AppText(
                                text = "کد تخفیف دارید؟ ",
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                fontSize = 15.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Bold,
                                color = RegDark,
                                textAlign = TextAlign.Right
                            )
                            val applyEnabled = !checkingCode && discountCode.isNotBlank() && !codeApplied
                            val applyDim = checkingCode || discountCode.isBlank()
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    Modifier
                                        .defaultMinSize(minWidth = 80.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (applyDim) Color(0xFFD1D5DB) else Color(0xFFF59E0B))
                                        .alpha(if (applyDim) 0.6f else 1f)
                                        .touchableOpacity(enabled = applyEnabled) { scope.launch { applyCode() } }
                                        .absolutePadding(left = 24.dp, right = 24.dp, top = 13.dp, bottom = 13.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (checkingCode) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
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
                                val fieldShape = RoundedCornerShape(12.dp)
                                BasicTextField(
                                    value = discountCode,
                                    onValueChange = { discountCode = it.uppercase() },
                                    enabled = !codeApplied,
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                                        fontSize = 14.sp,
                                        color = RegDark,
                                        textAlign = TextAlign.Right
                                    ),
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                    cursorBrush = SolidColor(RegDark),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { inner ->
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .clip(fieldShape)
                                                .background(if (codeApplied) Color(0xFFF0FDF4) else Color(0xFFF9FAFB))
                                                .border(
                                                    1.dp,
                                                    if (codeApplied) Color(0xFF86EFAC) else Color(0xFFE5E7EB),
                                                    fieldShape
                                                )
                                                .absolutePadding(left = 16.dp, right = 16.dp, top = 13.dp, bottom = 13.dp)
                                        ) {
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
                            if (codeApplied && discountAmount > 0) {
                                Box(
                                    Modifier
                                        .padding(top = 12.dp)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFF0FDF4))
                                        .drawLeftBar()
                                        .padding(12.dp)
                                ) {
                                    AppText(
                                        text = "✓ ${toPersianDigits(formatPrice(discountAmount))} تومان تخفیف اعمال شد",
                                        modifier = Modifier.fillMaxWidth(),
                                        fontSize = 13.sp,
                                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                                        color = Color(0xFF15803D),
                                        textAlign = TextAlign.Right
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .rnShadow(2.dp, cardShape)
                            .clip(cardShape)
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
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MaterialIcons("receipt", size = 20.dp, color = Color.White)
                                AppText(
                                    text = "خلاصه پرداخت",
                                    modifier = Modifier.padding(start = 8.dp),
                                    fontSize = 14.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Column(Modifier.padding(20.dp)) {
                            PriceLine("قیمت دوره:", toPersianDigits(formatPrice(registerAmount)))
                            if (discountAmount > 0) {
                                PriceLine("تخفیف:", "- ${toPersianDigits(formatPrice(discountAmount))}", Color(0xFF22C55E))
                            }
                            Box(
                                Modifier
                                    .absolutePadding(top = 12.dp, bottom = 12.dp)
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0xFFF3F4F6))
                            )
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
                                    color = RegDark
                                )
                            }
                        }
                    }
                }
            }

            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.White)
                    .drawBehind {
                        drawRect(Color(0xFFF3F4F6), size = Size(size.width, 1.dp.toPx()))
                    }
                    .absolutePadding(left = 20.dp, right = 20.dp)
                    .padding(top = 12.dp, bottom = if (isIOS) 32.dp else 20.dp)
            ) {
                val payShape = RoundedCornerShape(16.dp)
                Box(
                    Modifier
                        .padding(bottom = 50.dp)
                        .alpha(if (!active || registering) 0.6f else 1f)
                        .rnShadow(6.dp, payShape, Color(0xFF2ECC71))
                        .clip(payShape)
                        .touchableOpacity(enabled = active && !registering, activeOpacity = 0.8f) {
                            scope.launch { register() }
                        }
                ) {
                    LinearGradient(
                        colors = if (active) listOf(Color(0xFF2ECC71), Color(0xFF27AE60))
                        else listOf(Color(0xFF95A5A6), Color(0xFF7F8C8D)),
                        modifier = Modifier.fillMaxWidth(),
                        start = Offset(0f, 0f),
                        end = Offset(1f, 0f)
                    ) {
                        if (registering) {
                            Box(Modifier.fillMaxWidth().absolutePadding(top = 16.dp, bottom = 16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            }
                        } else {
                            RowReverse(
                                Modifier.fillMaxWidth().absolutePadding(left = 20.dp, right = 20.dp, top = 16.dp, bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MaterialIcons("credit-card", size = 22.dp, color = Color.White)
                                AppText(
                                    text = if (active) "ثبت‌نام و پرداخت" else "ثبت‌نام غیرفعال",
                                    fontSize = 16.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                if (active) {
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
            }
        }

        Box(
            Modifier
                .align(AbsoluteAlignment.TopRight)
                .zIndex(10f)
                .absoluteOffset(x = (-16).dp, y = backTop)
                .size(40.dp)
                .touchableOpacity(activeOpacity = 0.7f) { goHome() }
                .rnShadow(3.dp, CircleShape)
                .background(Color.White.copy(alpha = 0.95f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MaterialIcons("arrow-forward", size = 24.dp, color = RegDark)
        }

        Toast(toast.visible, toast.message, toast.type) { toast = toast.copy(visible = false) }
    }
}

@Composable
private fun PriceLine(label: String, value: String, valueColor: Color = RegDark) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppText(text = value, fontSize = 14.sp, fontFamily = RNFonts.Yekan_Bakh_Bold, color = valueColor)
        AppText(text = label, fontSize = 14.sp, fontFamily = RNFonts.Yekan_Bakh_Regular, color = RegMedium)
    }
}

private fun Modifier.drawLeftBar(): Modifier = this.drawBehind {
    drawRect(Color(0xFF22C55E), size = Size(3.dp.toPx(), size.height))
}
