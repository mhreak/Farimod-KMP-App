package ir.farimod.app.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.network.Api
import ir.farimod.app.network.ApiNetworkException
import ir.farimod.app.network.get
import ir.farimod.app.network.jsonOf
import ir.farimod.app.network.str
import ir.farimod.app.screens.auth.AuthBackdrop
import ir.farimod.app.screens.auth.AuthGlassCard
import ir.farimod.app.screens.auth.AuthHeroIcon
import ir.farimod.app.screens.auth.AuthRnTimingEasing
import ir.farimod.app.screens.auth.AuthShadowTitle
import ir.farimod.app.screens.auth.authRnTiming
import ir.farimod.app.screens.auth.authVerticalMargins
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.AppButton
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.AppTextInput
import ir.farimod.app.ui.ChipsUI
import ir.farimod.app.ui.OTPInput
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.Screen
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.ToastData
import ir.farimod.app.ui.rnTextStyle
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.ui.windowSize
import ir.farimod.app.utils.toPersianDigits
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import androidx.compose.runtime.CompositionLocalProvider

/** Snapshot sent to SignupFirstStep and shown on the OTP / success steps. */
private data class SignupDraft(
    val firstName: String,
    val lastName: String,
    val mobileNumber: String,
    val selectedGroups: List<Int>
)

private class SignupResendJob {
    var job: Job? = null
}

/** Port of `screens/SignupScreen.tsx`. */
@Composable
fun SignupScreen() {
    val navigation = useNavigation()
    val scope = rememberCoroutineScope()

    var currentStep by remember { mutableStateOf(1) }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var selectedGroups by remember { mutableStateOf(listOf<Int>()) }
    var formData by remember { mutableStateOf(SignupDraft("", "", "", emptyList())) }
    var otp by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var signupToken by remember { mutableStateOf("") }
    var resendTimer by remember { mutableStateOf(0) }
    var canResend by remember { mutableStateOf(false) }
    val resendJob = remember { SignupResendJob() }
    var toast by remember { mutableStateOf(ToastData()) }

    var groups by remember { mutableStateOf(listOf<JsonElement>()) }
    var groupsLoading by remember { mutableStateOf(true) }
    var groupsError by remember { mutableStateOf<String?>(null) }

    val iconFade = remember { Animatable(0f) }
    val iconSlide = remember { Animatable(-30f) }
    val formFade = remember { Animatable(0f) }
    val formSlide = remember { Animatable(30f) }

    // backButtonAnim (300ms) is started in RN but never attached to a view. The delay is kept
    // so the icon and form still wait for it.
    LaunchedEffect(Unit) {
        delay(300)
        coroutineScope {
            launch { iconFade.animateTo(1f, authRnTiming(600)) }
            launch { iconSlide.animateTo(0f, authRnTiming(600)) }
        }
        coroutineScope {
            launch { formFade.animateTo(1f, authRnTiming(700)) }
            launch { formSlide.animateTo(0f, authRnTiming(700)) }
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "signupPulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000, easing = AuthRnTimingEasing), RepeatMode.Reverse),
        label = "signupPulse"
    )

    fun showToast(message: String, type: String = "info") {
        toast = ToastData(visible = true, message = message, type = type)
    }

    fun hideToast() {
        toast = ToastData(visible = false, message = "", type = "info")
    }

    fun startResendTimer(duration: Int = 120) {
        resendTimer = duration
        canResend = false
        resendJob.job?.cancel()
        resendJob.job = scope.launch {
            var left = duration
            while (true) {
                delay(1000)
                if (left <= 1) {
                    resendTimer = 0
                    canResend = true
                    return@launch
                }
                left -= 1
                resendTimer = left
            }
        }
    }

    suspend fun loadGroups() {
        try {
            groupsLoading = true
            groupsError = null
            val response = Api.get("MemberGroup/GetAll", query = mapOf("filterActive" to true))
            if (!response.ok) throw Exception("HTTP error! status: ${response.status}")
            val result = response.json ?: throw Exception("Invalid data format received from API")
            val data = result["Data"]
            if (data is JsonArray) {
                groups = data.toList()
            } else {
                throw Exception("Invalid data format received from API")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            groupsError = e.message ?: "خطا در دریافت اطلاعات"
        } finally {
            groupsLoading = false
        }
    }

    suspend fun sendSignupOTPSMS(token: String) {
        try {
            val response = Api.post(
                "MobileAccount/SendSignupOTPSMS",
                query = mapOf("signupToken" to token)
            )
            val result = response.json
            if (result == null) {
                showToast("خطا در ارسال پیامک", "error")
                return
            }
            if (response.ok) {
                val msg = result.str("Message")
                showToast(if (msg.isNullOrEmpty()) "کد پیامکی با موفقیت ارسال شد" else msg, "success")
                scope.launch {
                    delay(1000)
                    currentStep = 3
                }
            } else {
                val msg = result.str("Message")
                showToast(if (msg.isNullOrEmpty()) "خطا در ارسال پیامک" else msg, "error")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: ApiNetworkException) {
            showToast("خطا در ارسال پیامک", "error")
        } catch (_: Exception) {
            showToast("خطا در ارسال پیامک", "error")
        }
    }

    suspend fun sendSignupFirstStep(userData: SignupDraft) {
        try {
            isSubmitting = true
            val response = Api.post(
                "MobileAccount/SignupFirstStep",
                body = jsonOf(
                    "FirstName" to userData.firstName,
                    "LastName" to userData.lastName,
                    "Mobile" to userData.mobileNumber,
                    "MemberGroupIdList" to userData.selectedGroups
                )
            )
            val result = response.json
            if (result == null) {
                showToast("خطا در اتصال به سرور", "error")
                return
            }
            val token = result.str("SignupToken")
            if (response.ok && !token.isNullOrEmpty()) {
                signupToken = token
                formData = userData
                sendSignupOTPSMS(token)
            } else {
                showToast(signupFirstStepError(result), "error")
                if (jsonStrictTrue(result["RedirectToLogin"])) {
                    scope.launch {
                        delay(1000)
                        navigation.navigate("Login")
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            showToast("خطا در اتصال به سرور", "error")
        } finally {
            isSubmitting = false
        }
    }

    suspend fun validateSignupOTP(otpCode: String) {
        try {
            isSubmitting = true
            val response = Api.post(
                "MobileAccount/ValidateSignupOTP",
                body = jsonOf("SignupToken" to signupToken, "OTP" to otpCode)
            )
            val result = response.json
            if (result == null) {
                showToast("خطا در تایید کد", "error")
                return
            }
            if (response.ok) {
                val msg = result.str("Message")
                showToast(if (msg.isNullOrEmpty()) "کد با موفقیت تایید شد" else msg, "success")
                resendJob.job?.cancel()
                resendJob.job = null
                scope.launch {
                    delay(1000)
                    currentStep = 4
                }
            } else {
                val msg = result.str("Message")
                showToast(if (msg.isNullOrEmpty()) "کد وارد شده اشتباه است" else msg, "error")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            showToast("خطا در تایید کد", "error")
        } finally {
            isSubmitting = false
        }
    }

    LaunchedEffect(Unit) { loadGroups() }

    LaunchedEffect(groupsError) {
        val err = groupsError ?: return@LaunchedEffect
        showToast("خطا در بارگذاری گروه‌ها: $err", "error")
    }

    LaunchedEffect(currentStep) {
        if (currentStep == 3) {
            otp = ""
            startResendTimer()
        }
    }

    DisposableEffect(Unit) {
        onDispose { resendJob.job?.cancel() }
    }

    fun submitStep1() {
        if (firstName.isEmpty()) {
            showToast("نام وارد نشده است", "error")
            return
        }
        if (lastName.isEmpty()) {
            showToast("نام خانوادگی وارد نشده است", "error")
            return
        }
        if (mobileNumber.isEmpty()) {
            showToast("شماره موبایل وارد نشده است", "error")
            return
        }
        if (!Regex("^09\\d{9}$").matches(mobileNumber)) {
            showToast("شماره موبایل باید با 09 شروع شده و 11 رقم باشد", "error")
            return
        }
        currentStep = 2
    }

    fun submitStep2() {
        if (selectedGroups.isEmpty()) {
            showToast("لطفاً حداقل یک گروه انتخاب کنید", "error")
            return
        }
        val draft = SignupDraft(firstName, lastName, mobileNumber, selectedGroups)
        scope.launch { sendSignupFirstStep(draft) }
    }

    fun submitOtp() {
        if (otp.length != 5) {
            showToast("لطفاً کد ۵ رقمی را کامل وارد کنید", "error")
            return
        }
        val code = otp
        scope.launch { validateSignupOTP(code) }
    }

    fun resendOtp() {
        if (signupToken.isNotEmpty() && canResend) {
            canResend = false
            val token = signupToken
            scope.launch {
                sendSignupOTPSMS(token)
                startResendTimer(120)
            }
        }
    }

    fun toggleGroup(groupId: Int) {
        selectedGroups = if (groupId in selectedGroups) {
            selectedGroups.filter { it != groupId }
        } else {
            selectedGroups + groupId
        }
    }

    Box(Modifier.fillMaxSize()) {
        AuthBackdrop {
            Screen(Modifier.padding(10.dp)) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .zIndex(1000f)
                            .authVerticalMargins(top = 50.dp, bottom = (-110).dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        AuthHeroIcon(
                            iconName = "person-add",
                            iconSize = 50.dp,
                            circleSize = 100.dp,
                            innerSize = 80.dp,
                            ringSize = 120.dp,
                            elevation = 0.dp,
                            alpha = { iconFade.value },
                            translationY = { iconSlide.value },
                            scale = { pulse }
                        )
                    }

                    AuthGlassCard(
                        alpha = { formFade.value },
                        translationY = { formSlide.value },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 5.dp, end = 5.dp, top = 65.dp, bottom = 80.dp)
                            .zIndex(1f),
                        shadowElevation = 0.dp
                    ) {
                        AuthShadowTitle(text = "ثبت نام", top = 35.dp, bottom = 30.dp)
                        SignupStepIndicator(currentStep)

                        if (currentStep == 1) {
                            AppTextInput(
                                value = firstName,
                                onChangeText = { firstName = it },
                                icon = "person",
                                placeholder = "نام"
                            )
                            AppTextInput(
                                value = lastName,
                                onChangeText = { lastName = it },
                                icon = "person",
                                placeholder = "نام خانوادگی"
                            )
                            AppTextInput(
                                value = mobileNumber,
                                onChangeText = { mobileNumber = it },
                                icon = "phone-android",
                                placeholder = "شماره موبایل",
                                keyboardType = KeyboardType.Phone,
                                maxLength = 11
                            )
                            // nextButton marginTop 25 replaces the button's marginVertical top (8).
                            AppButton(
                                title = "مرحله بعد",
                                onPress = { submitStep1() },
                                modifier = Modifier.padding(top = 17.dp)
                            )
                            RowReverse(
                                modifier = Modifier.fillMaxWidth().padding(top = 25.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppText(
                                    text = "حساب کاربری دارید؟ ",
                                    fontSize = 16.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                                    color = FarimodColors.Medium
                                )
                                AppText(
                                    text = "وارد شوید",
                                    modifier = Modifier.touchableOpacity { navigation.navigate("Login") },
                                    fontSize = 16.sp,
                                    color = FarimodColors.Primary,
                                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                                    textDecoration = TextDecoration.Underline
                                )
                            }
                        }

                        if (currentStep == 2) {
                            SignupSectionTitle("انتخاب گروه‌ها")
                            SignupSectionSubtitle("جزء کدام یک از گروه‌های زیر هستید؟ می‌توانید چند گزینه انتخاب کنید")
                            when {
                                groupsLoading -> {
                                    Column(
                                        Modifier.fillMaxWidth().absolutePadding(top = 40.dp, bottom = 40.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.height(36.dp).width(36.dp),
                                            color = FarimodColors.Primary
                                        )
                                        AppText(
                                            text = "در حال بارگذاری گروه‌ها...",
                                            modifier = Modifier.padding(top = 15.dp),
                                            fontSize = 14.sp,
                                            color = FarimodColors.Medium,
                                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                groupsError != null -> {
                                    Column(
                                        Modifier.fillMaxWidth().absolutePadding(top = 40.dp, bottom = 40.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        MaterialIcons("error-outline", size = 48.dp, color = Color(0xFF9E9E9E))
                                        AppText(
                                            text = "خطا در بارگذاری گروه‌ها",
                                            modifier = Modifier.padding(top = 15.dp),
                                            fontSize = 14.sp,
                                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                                            color = Color(0xFF9E9E9E),
                                            textAlign = TextAlign.Center
                                        )
                                        RowReverse(
                                            modifier = Modifier
                                                .padding(top = 15.dp)
                                                .touchableOpacity { scope.launch { loadGroups() } }
                                                .background(FarimodColors.Primary, RoundedCornerShape(10.dp))
                                                .absolutePadding(left = 20.dp, right = 20.dp, top = 10.dp, bottom = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            MaterialIcons("refresh", size = 20.dp, color = FarimodColors.White)
                                            AppText(
                                                text = "تلاش مجدد",
                                                modifier = Modifier.absolutePadding(right = 8.dp),
                                                fontSize = 12.sp,
                                                fontFamily = RNFonts.Yekan_Bakh_Bold,
                                                color = FarimodColors.White
                                            )
                                        }
                                    }
                                }
                                else -> {
                                    val screenHeight = windowSize().second
                                    Box(Modifier.padding(bottom = 25.dp)) {
                                        ChipsUI(
                                            groups = groups,
                                            selectedGroups = selectedGroups,
                                            onToggleGroup = { toggleGroup(it) },
                                            allowMultipleSelection = true,
                                            sortType = "length-asc",
                                            modifier = Modifier.heightIn(max = screenHeight)
                                        )
                                    }
                                }
                            }
                            SignupStepButtons(
                                rightTitle = "مرحله قبل",
                                onRight = { currentStep = 1 },
                                leftTitle = if (isSubmitting) "در حال ارسال..." else "ادامه",
                                onLeft = { submitStep2() },
                                leftDisabled = isSubmitting
                            )
                        }

                        if (currentStep == 3) {
                            SignupSectionTitle("کد تأیید را وارد کنید")
                            SignupSectionSubtitle("کد ۵ رقمی ارسال شده به شماره ${formData.mobileNumber} را وارد کنید")
                            Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 25.dp)) {
                                OTPInput(
                                    onCodeChange = { otp = it },
                                    code = otp,
                                    length = 5
                                )
                            }
                            Box(
                                Modifier.fillMaxWidth().padding(bottom = 25.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (canResend) {
                                    AppText(
                                        text = "ارسال مجدد کد",
                                        modifier = Modifier.touchableOpacity { resendOtp() },
                                        fontSize = 14.sp,
                                        color = FarimodColors.Primary,
                                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                                        textDecoration = TextDecoration.Underline,
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    AppText(
                                        text = "ارسال مجدد کد تا ${formatSignupResend(resendTimer)}",
                                        fontSize = 14.sp,
                                        color = FarimodColors.Medium,
                                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            SignupStepButtons(
                                rightTitle = "تغییر شماره",
                                onRight = { currentStep = 1 },
                                leftTitle = if (isSubmitting) "در حال تایید..." else "تایید کد",
                                onLeft = { submitOtp() },
                                leftDisabled = isSubmitting || otp.length != 5
                            )
                        }

                        if (currentStep == 4) {
                            SignupSectionTitle("ثبت نام با موفقیت انجام شد!")
                            SignupSectionSubtitle("اکنون می‌توانید از حساب کاربری خود استفاده کنید")
                            Column(
                                Modifier.fillMaxWidth().absolutePadding(top = 40.dp, bottom = 40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                MaterialIcons("check-circle", size = 80.dp, color = FarimodColors.Success)
                                AppText(
                                    text = "خوش آمدید ${formData.firstName} ${formData.lastName}",
                                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                                    fontSize = 18.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    color = FarimodColors.Dark,
                                    textAlign = TextAlign.Center
                                )
                                AppText(
                                    text = "شماره موبایل شما با موفقیت تایید شد",
                                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                    fontSize = 14.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                                    color = FarimodColors.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }
                            // finalButton marginTop 30 replaces the button's marginVertical top (8).
                            AppButton(
                                title = "ورود به برنامه",
                                onPress = { navigation.navigate("Login") },
                                modifier = Modifier.padding(top = 22.dp)
                            )
                        }
                    }
                }
            }
        }
        Toast(
            visible = toast.visible,
            message = toast.message,
            type = toast.type,
            duration = 3000,
            onHide = { hideToast() }
        )
    }
}

@Composable
private fun SignupSectionTitle(text: String) {
    AppText(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        fontSize = 18.sp,
        color = FarimodColors.Dark,
        fontFamily = RNFonts.Yekan_Bakh_Bold,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SignupSectionSubtitle(text: String) {
    AppText(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(bottom = 25.dp),
        fontSize = 14.sp,
        fontFamily = RNFonts.Yekan_Bakh_Regular,
        color = Color(0xFF7E7E7E),
        textAlign = TextAlign.Center,
        lineHeight = 22.sp
    )
}

@Composable
private fun SignupStepButtons(
    rightTitle: String,
    onRight: () -> Unit,
    leftTitle: String,
    onLeft: () -> Unit,
    leftDisabled: Boolean
) {
    RowReverse(
        modifier = Modifier.fillMaxWidth().padding(top = 25.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppButton(
            title = rightTitle,
            onPress = onRight,
            modifier = Modifier.weight(1f).absolutePadding(left = 10.dp),
            color = FarimodColors.Medium
        )
        AppButton(
            title = leftTitle,
            onPress = onLeft,
            modifier = Modifier.weight(1f),
            disabled = leftDisabled
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SignupStepIndicator(currentStep: Int) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(bottom = 30.dp),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.Top
        ) {
            SignupStepNode(number = "۱", label = "اطلاعات شخصی", active = currentStep >= 1, rawText = false)
            SignupStepLine(active = currentStep >= 2)
            SignupStepNode(number = "۲", label = "انتخاب گروه", active = currentStep >= 2, rawText = true)
            SignupStepLine(active = currentStep >= 3)
            SignupStepNode(number = "۳", label = "تأیید تلفن همراه", active = currentStep >= 3, rawText = true)
        }
    }
}

@Composable
private fun SignupStepNode(number: String, label: String, active: Boolean, rawText: Boolean) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            Modifier.absolutePadding(left = 5.dp, right = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val borderColor = if (active) FarimodColors.Primary else FarimodColors.Medium
            val fill = if (active) FarimodColors.Primary else FarimodColors.Light
            val numberColor = if (active) FarimodColors.White else FarimodColors.Medium
            Box(
                Modifier
                    .width(35.dp)
                    .height(35.dp)
                    .background(fill, CircleShape)
                    .border(2.dp, borderColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (rawText) {
                    Text(
                        text = number,
                        style = rnTextStyle(
                            fontSize = 14.sp,
                            color = numberColor,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            textAlign = TextAlign.Right
                        )
                    )
                } else {
                    AppText(
                        text = number,
                        fontSize = 14.sp,
                        color = numberColor,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        textAlign = TextAlign.Right
                    )
                }
            }
            Text(
                text = label,
                modifier = Modifier.padding(top = 8.dp).widthIn(max = 70.dp),
                style = rnTextStyle(
                    fontSize = 10.sp,
                    color = FarimodColors.Medium,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
private fun SignupStepLine(active: Boolean) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        // RN: height 2, width 30, marginHorizontal 5, alignItems center, marginTop -20,
        // which sits the line on the 35dp circle. FlowRow top-aligns, so paddingTop 16
        // (circle center) is the same visual.
        Box(
            Modifier
                .absolutePadding(left = 5.dp, right = 5.dp)
                .padding(top = 16.dp)
                .width(30.dp)
                .height(2.dp)
                .background(if (active) FarimodColors.Primary else FarimodColors.Light)
        )
    }
}

/** Both parts padded, then Persian digits. RN `formatTime` on Signup. */
private fun formatSignupResend(seconds: Int): String {
    val minutes = (seconds / 60).toString().padStart(2, '0')
    val remaining = (seconds % 60).toString().padStart(2, '0')
    return "${toPersianDigits(minutes)}:${toPersianDigits(remaining)}"
}

private fun signupFirstStepError(result: JsonElement): String {
    val message = result.str("Message")
    if (!message.isNullOrEmpty()) return message
    val mobileError = result["errors"]["Mobile"][0].str
    if (!mobileError.isNullOrEmpty()) return mobileError
    val title = result.str("title")
    if (!title.isNullOrEmpty()) return title
    return "خطا در ارسال اطلاعات"
}

/** RN `result.RedirectToLogin === true` (strict), not truthy strings. */
private fun jsonStrictTrue(element: JsonElement?): Boolean {
    val primitive = element as? JsonPrimitive ?: return false
    return primitive.booleanOrNull == true
}
