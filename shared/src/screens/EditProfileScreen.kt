package ir.farimod.app.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.navigation.Tabs
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.network.int
import ir.farimod.app.network.jsonOf
import ir.farimod.app.network.str
import ir.farimod.app.resources.Res
import ir.farimod.app.resources.background_1
import ir.farimod.app.services.ProfileService
import ir.farimod.app.state.AuthState
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.AlertButton
import ir.farimod.app.ui.AppButton
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.AppTextInput
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.RNAlert
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RNTimingEasing
import ir.farimod.app.ui.Screen
import ir.farimod.app.ui.rgba
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.rnTextStyle
import ir.farimod.app.ui.rnTiming
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.utils.PersianNumberUtils
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import org.jetbrains.compose.resources.imageResource

/** RN `useState` form data of EditProfileScreen. */
private data class EditProfileFormData(
    val cityId: Int = 0,
    val provinceId: Int = 0,
    val cityName: String = "",
    val provinceName: String = "",
    val mobile: String = "",
    val phone1: String = "",
    val phone2: String = "",
    val email: String = "",
    val websiteAddress: String = "",
    val telegramAccountId: String = "",
    val instagramAccountId: String = "",
    val whatsappAccountMobileNumber: String = "",
    val address: String = ""
)

/** JS `data.Key || ""` for string-ish API values. */
private fun JsonElement?.jsStr(key: String): String = this.str(key).takeUnless { it.isNullOrEmpty() } ?: ""

private fun JsonElement?.orZero(key: String): Int = this.int(key)?.takeUnless { it == 0 } ?: 0

@Composable
fun EditProfileScreen() {
    val navigation = useNavigation()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density

    // State برای نگه داشتن مقادیر فرم
    var formData by remember { mutableStateOf(EditProfileFormData()) }

    // شماره موبایل اولیه برای چک کردن تغییرات
    var initialMobile by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingData by remember { mutableStateOf(true) }

    // بارگذاری اطلاعات کاربر از API
    LaunchedEffect(Unit) {
        val user = AuthState.user
        if (user == null || (user.MemberId ?: 0) == 0) {
            isLoadingData = false
            return@LaunchedEffect
        }
        try {
            isLoadingData = true
            // ✅ ارسال user به service برای fallback
            val result = ProfileService.getProfileInfoToEdit(user.MemberId!!, user)
            val data = result.data
            if (result.success && data != null) {
                // ✅ استفاده از چند fallback
                val mobile = data.jsStr("Mobile").ifEmpty { user.Mobile ?: "" }

                // ✅ اگر هنوز mobile خالی است، به کاربر اطلاع بده
                if (mobile.isEmpty()) {
                    RNAlert.alert(
                        "هشدار",
                        "شماره موبایل یافت نشد. لطفاً شماره موبایل خود را وارد کنید.",
                        listOf(AlertButton("باشه"))
                    )
                }

                val profileData = EditProfileFormData(
                    cityId = data.orZero("CityId"),
                    provinceId = data.orZero("ProvinceId"),
                    cityName = data.jsStr("CityName"),
                    provinceName = data.jsStr("ProvinceName"),
                    mobile = mobile,
                    phone1 = data.jsStr("Phone1"),
                    phone2 = data.jsStr("Phone2"),
                    email = data.jsStr("Email"),
                    websiteAddress = data.jsStr("WebsiteAddress"),
                    telegramAccountId = data.jsStr("TelegramAccountId"),
                    instagramAccountId = data.jsStr("InstagramAccountId"),
                    whatsappAccountMobileNumber = data.jsStr("WhatsappAccountMobileNumber"),
                    address = data.jsStr("Address")
                )
                formData = profileData
                initialMobile = mobile
            } else {
                RNAlert.alert("خطا", "خطا در بارگذاری اطلاعات پروفایل")
            }
        } catch (e: Exception) {
            RNAlert.alert("خطا", "خطا در بارگذاری اطلاعات")
        } finally {
            isLoadingData = false
        }
    }

    // تابع ذخیره اطلاعات (performUpdate) — throws on API failure like RN
    suspend fun performUpdate() {
        val user = AuthState.user
        if (user == null || (user.MemberId ?: 0) == 0) {
            RNAlert.alert("خطا", "اطلاعات کاربر موجود نیست")
            return
        }

        // ✅ استفاده از mobile فرم یا در صورت خالی بودن از initialMobile
        val mobileToSend = formData.mobile.ifEmpty { initialMobile }.ifEmpty { user.Mobile ?: "" }

        if (mobileToSend.isEmpty()) {
            RNAlert.alert("خطا", "شماره موبایل الزامی است")
            return
        }

        val updatedData = jsonOf(
            "MemberId" to user.MemberId,
            "AboutMe" to "", // درباره من در این صفحه ویرایش نمی‌شود
            "CityId" to formData.cityId,
            "ProvinceId" to formData.provinceId,
            "Mobile" to mobileToSend, // ✅ مطمئن می‌شویم که Mobile پر است
            "Phone1" to formData.phone1,
            "Phone2" to formData.phone2,
            "Email" to formData.email,
            "WebsiteAddress" to formData.websiteAddress,
            "TelegramAccountId" to formData.telegramAccountId,
            "WhatsappAccountMobileNumber" to formData.whatsappAccountMobileNumber,
            "InstagramAccountId" to formData.instagramAccountId,
            "Address" to formData.address,
            "MemberGroupIdList" to emptyList<Int>() // لیست گروه‌ها فعلاً خالی است
        )

        val result = ProfileService.updateProfile(updatedData)
        if (!result.success) {
            throw Exception(result.message)
        }

        // بروزرسانی user در context (فقط برای نمایش)
        // NOTE: UserData only stores Mobile (other RN-only display fields have no counterpart).
        AuthState.updateUser { it.copy(Mobile = mobileToSend) }
    }

    val goHome: () -> Unit = {
        navigation.goBack()
    }

    val handleSave: () -> Unit = {
        scope.launch {
            try {
                isLoading = true

                // ✅ چک کردن تغییر شماره موبایل با مقدار اولیه
                val mobileChanged = formData.mobile != initialMobile

                if (mobileChanged) {
                    RNAlert.alert(
                        "تغییر شماره موبایل",
                        "با تغییر شماره موبایل، باید مجدداً وارد شوید. آیا ادامه می‌دهید؟",
                        listOf(
                            AlertButton("لغو", style = "cancel", onPress = { isLoading = false }),
                            AlertButton("ادامه", onPress = {
                                scope.launch {
                                    try {
                                        performUpdate()
                                        // خروج از حساب کاربری
                                        AuthState.logout()
                                        RNAlert.alert(
                                            "موفق",
                                            "لطفاً مجدداً وارد شوید",
                                            listOf(AlertButton("باشه", onPress = { navigation.navigate("Login") }))
                                        )
                                    } catch (_: Exception) {
                                        // RN: unhandled promise rejection
                                    }
                                }
                            })
                        )
                    )
                } else {
                    performUpdate()
                    RNAlert.alert("موفق", "اطلاعات با موفقیت ذخیره شد")
                    goHome()
                }
            } catch (e: Exception) {
                RNAlert.alert("خطا", "خطا در ذخیره اطلاعات")
            } finally {
                isLoading = false
            }
        }
    }

    // Animation values
    val fadeBack = remember { Animatable(0f) } // backButtonAnim
    val iconFade = remember { Animatable(0f) }
    val iconSlide = remember { Animatable(-50f) }
    val formFade = remember { Animatable(0f) }
    val formSlide = remember { Animatable(40f) }

    LaunchedEffect(Unit) {
        // Sequential animations for better effect
        // Back button appears first
        fadeBack.animateTo(1f, rnTiming(400))
        // Icon appears with slide from top
        coroutineScope {
            launch { iconFade.animateTo(1f, rnTiming(800)) }
            launch { iconSlide.animateTo(0f, rnTiming(800)) }
        }
        // Form appears with slide from bottom
        coroutineScope {
            launch { formFade.animateTo(1f, rnTiming(900)) }
            launch { formSlide.animateTo(0f, rnTiming(900)) }
        }
    }

    // Continuous pulse animation for icon / rotation for decorative ring
    val infinite = rememberInfiniteTransition(label = "editProfile")
    val pulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(2500, easing = RNTimingEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val spin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(10000, easing = RNTimingEasing), RepeatMode.Restart),
        label = "spin"
    )

    // Tiled background (resizeMode: 'repeat')
    val bgBitmap = imageResource(Res.drawable.background_1)
    val bgBrush = remember(bgBitmap) { ShaderBrush(ImageShader(bgBitmap, TileMode.Repeated, TileMode.Repeated)) }

    Box(Modifier.fillMaxSize()) { // backgroundContainer
        Box(Modifier.fillMaxSize().drawBehind { drawRect(bgBrush) }) // backgroundWrapper + backgroundImage

        LinearGradient(
            colors = listOf(
                rgba(255, 255, 255, 0.1f),
                rgba(255, 255, 255, 0.6f),
                rgba(255, 255, 255, 0.8f),
                rgba(255, 255, 255, 1f)
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Screen(Modifier.padding(10.dp)) { // container: padding 10, transparent
                // Absolute children are positioned relative to the Screen's inner view (padding 10 is ignored by RN).
                Box(Modifier.fillMaxSize()) {
                    // backButton: top 15, right 15, zIndex 10
                    Box(
                        Modifier
                            .align(AbsoluteAlignment.TopRight)
                            .absoluteOffset(x = (-5).dp, y = 5.dp)
                            .zIndex(10f)
                            .graphicsLayer {
                                alpha = fadeBack.value
                                scaleX = fadeBack.value
                                scaleY = fadeBack.value
                            }
                    ) {
                        Box(
                            Modifier
                                .touchableOpacity { goHome() }
                                .background(Color(0xFF9E22AD), RoundedCornerShape(25.dp))
                                .border(1.dp, rgba(255, 206, 232, 0.5f), RoundedCornerShape(25.dp))
                                .padding(10.dp)
                        ) {
                            MaterialIcons("arrow-forward", size = 24.dp, color = Color.White)
                        }
                    }

                    // iconContainer: absolute top 60, left 0, right 0, zIndex 1000
                    Box(
                        Modifier
                            .align(Alignment.TopCenter)
                            .absoluteOffset(y = 50.dp)
                            .zIndex(1000f)
                            .graphicsLayer {
                                alpha = iconFade.value
                                translationY = iconSlide.value * density
                                scaleX = pulse
                                scaleY = pulse
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // iconCircle (LinearGradient primary → primaryDark), shadow elevation 8
                        LinearGradient(
                            colors = listOf(FarimodColors.Primary, FarimodColors.PrimaryDark),
                            modifier = Modifier
                                .size(120.dp)
                                .rnShadow(8.dp, CircleShape, rgba(255, 206, 232, 0.2f)),
                            shape = CircleShape
                        ) {
                            Box(
                                Modifier
                                    .align(Alignment.Center)
                                    .zIndex(99f)
                                    .size(100.dp)
                                    .background(rgba(255, 206, 232, 0.15f), CircleShape)
                                    .border(2.dp, rgba(255, 206, 232, 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                MaterialIcons("person", size = 65.dp, color = FarimodColors.White)
                            }
                            // Decorative ring with rotation
                            Box(
                                Modifier
                                    .align(Alignment.Center)
                                    .requiredSize(140.dp)
                                    .graphicsLayer { rotationZ = spin }
                                    .drawBehind {
                                        val stroke = 1.5.dp.toPx()
                                        drawCircle(
                                            color = rgba(255, 206, 232, 0.3f),
                                            radius = size.minDimension / 2f - stroke / 2f,
                                            style = Stroke(
                                                width = stroke,
                                                pathEffect = PathEffect.dashPathEffect(
                                                    floatArrayOf(stroke * 3f, stroke * 3f)
                                                )
                                            )
                                        )
                                    }
                            )
                        }
                    }

                    // ScrollView (paddingTop 80, paddingBottom 30)
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(top = 80.dp, bottom = 30.dp)
                    ) {
                        // centerContainer
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            // editBox
                            val boxShape = RoundedCornerShape(25.dp)
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        alpha = formFade.value
                                        translationY = formSlide.value * density
                                    }
                                    .clip(boxShape)
                            ) {
                                // Glassmorphism overlay
                                Box(
                                    Modifier
                                        .matchParentSize()
                                        .background(rgba(255, 206, 232, 0.7f), boxShape)
                                        .border(1.5.dp, rgba(255, 206, 232, 1f), boxShape)
                                )

                                // Content
                                Column(Modifier.fillMaxWidth().padding(25.dp)) {
                                    val titleStyle = rnTextStyle(
                                        fontSize = 22.sp,
                                        color = FarimodColors.Primary,
                                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                                        textAlign = TextAlign.Center
                                    ).copy(
                                        shadow = Shadow(
                                            color = rgba(255, 206, 232, 0.1f),
                                            offset = Offset(0f, 1f * density),
                                            blurRadius = 2f * density
                                        )
                                    )
                                    Text(
                                        text = "ویرایش پروفایل",
                                        modifier = Modifier.fillMaxWidth().padding(top = 50.dp, bottom = 30.dp),
                                        style = titleStyle
                                    )

                                    if (isLoadingData) {
                                        AppText(
                                            text = "در حال بارگذاری...",
                                            modifier = Modifier.fillMaxWidth().absolutePadding(top = 20.dp, bottom = 20.dp),
                                            fontSize = 16.sp,
                                            textAlign = TextAlign.Center,
                                            color = FarimodColors.Primary,
                                            fontFamily = RNFonts.Yekan_Bakh_Regular
                                        )
                                    } else {
                                        AppTextInput(
                                            value = formData.mobile,
                                            onChangeText = { formData = formData.copy(mobile = it) },
                                            icon = "phone-android",
                                            keyboardType = KeyboardType.Phone,
                                            maxLength = 11,
                                            placeholder = "شماره موبایل"
                                        )

                                        AppTextInput(
                                            value = formData.provinceName,
                                            onChangeText = { formData = formData.copy(provinceName = it) },
                                            icon = "map",
                                            keyboardType = KeyboardType.Text,
                                            placeholder = "استان",
                                            editable = false
                                        )

                                        AppTextInput(
                                            value = formData.cityName,
                                            onChangeText = { formData = formData.copy(cityName = it) },
                                            icon = "location-city",
                                            keyboardType = KeyboardType.Text,
                                            placeholder = "شهر",
                                            editable = false
                                        )

                                        AppTextInput(
                                            value = formData.phone1,
                                            onChangeText = { formData = formData.copy(phone1 = it) },
                                            icon = "phone",
                                            keyboardType = KeyboardType.Phone,
                                            placeholder = "تلفن ثابت 1"
                                        )

                                        AppTextInput(
                                            value = formData.phone2,
                                            onChangeText = { formData = formData.copy(phone2 = it) },
                                            icon = "phone",
                                            keyboardType = KeyboardType.Phone,
                                            placeholder = "تلفن ثابت 2"
                                        )

                                        AppTextInput(
                                            value = formData.email,
                                            onChangeText = { formData = formData.copy(email = it) },
                                            icon = "email",
                                            keyboardType = KeyboardType.Email,
                                            placeholder = "ایمیل"
                                        )

                                        AppTextInput(
                                            value = formData.websiteAddress,
                                            onChangeText = { formData = formData.copy(websiteAddress = it) },
                                            icon = "web",
                                            keyboardType = KeyboardType.Uri,
                                            placeholder = "آدرس وب‌سایت"
                                        )

                                        AppTextInput(
                                            value = formData.telegramAccountId,
                                            onChangeText = { formData = formData.copy(telegramAccountId = it) },
                                            icon = "telegram",
                                            keyboardType = KeyboardType.Text,
                                            placeholder = "آیدی تلگرام"
                                        )

                                        AppTextInput(
                                            value = formData.instagramAccountId,
                                            onChangeText = { formData = formData.copy(instagramAccountId = it) },
                                            icon = "instagram",
                                            keyboardType = KeyboardType.Text,
                                            placeholder = "آیدی اینستاگرام"
                                        )

                                        AppTextInput(
                                            value = formData.whatsappAccountMobileNumber,
                                            onChangeText = { formData = formData.copy(whatsappAccountMobileNumber = it) },
                                            icon = "whatsapp",
                                            keyboardType = KeyboardType.Phone,
                                            placeholder = "شماره واتساپ"
                                        )

                                        EditProfileMultilineInput(
                                            value = formData.address,
                                            onChangeText = { formData = formData.copy(address = it) },
                                            icon = "location-on",
                                            placeholder = "آدرس",
                                            numberOfLines = 3
                                        )

                                        AppButton(
                                            title = if (isLoading) "در حال ذخیره..." else "ذخیره",
                                            onPress = handleSave,
                                            color = FarimodColors.Success,
                                            disabled = isLoading || isLoadingData
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
}

private object EditProfilePersianTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText = TransformedText(
        AnnotatedString(PersianNumberUtils.toPersianDigits(text.text), text.spanStyles, text.paragraphStyles),
        OffsetMapping.Identity
    )
}

/**
 * `components/TextInput.tsx` with `multiline numberOfLines={3}` (but without `isLargeInput`):
 * the container keeps `alignItems: center` (icon vertically centered), the text input grows with its lines.
 */
@Composable
private fun EditProfileMultilineInput(
    value: String,
    onChangeText: (String) -> Unit,
    icon: String,
    placeholder: String,
    numberOfLines: Int
) {
    var fieldValue by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    if (fieldValue.text != value) {
        fieldValue = TextFieldValue(value, TextRange(value.length))
    }
    val shape = RoundedCornerShape(16.dp)
    val textStyle = TextStyle(
        fontFamily = RNFonts.Yekan_Bakh_Regular,
        fontSize = 15.sp,
        color = FarimodColors.Dark,
        textAlign = TextAlign.Right,
        textDirection = TextDirection.Rtl
    )
    val displayPlaceholder = if (placeholder.isNotEmpty()) PersianNumberUtils.toPersianDigits(placeholder) else ""

    Column(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        RowReverse(
            modifier = Modifier
                .fillMaxWidth()
                .background(FarimodColors.White, shape)
                .border(1.dp, FarimodColors.Gray, shape),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.absolutePadding(right = 10.dp)) {
                MaterialIcons(name = icon, size = 20.dp, color = FarimodColors.Medium)
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                BasicTextField(
                    value = fieldValue,
                    onValueChange = { new ->
                        val english = PersianNumberUtils.toEnglishDigits(new.text)
                        val next = if (english == new.text) new else TextFieldValue(
                            english,
                            TextRange(
                                new.selection.start.coerceAtMost(english.length),
                                new.selection.end.coerceAtMost(english.length)
                            )
                        )
                        val changed = english != fieldValue.text
                        fieldValue = next
                        if (changed) onChangeText(english)
                    },
                    modifier = Modifier.fillMaxWidth().padding(15.dp),
                    textStyle = textStyle,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = KeyboardType.Text
                    ),
                    minLines = numberOfLines,
                    visualTransformation = EditProfilePersianTransformation,
                    cursorBrush = SolidColor(FarimodColors.Dark),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.TopStart) {
                            if (fieldValue.text.isEmpty() && displayPlaceholder.isNotEmpty()) {
                                Text(
                                    text = displayPlaceholder,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = textStyle.copy(color = FarimodColors.DarkGray)
                                )
                            }
                            inner()
                        }
                    }
                )
            }
        }
    }
}
