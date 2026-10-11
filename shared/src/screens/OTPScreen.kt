package ir.farimod.app.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.data.model.UserData
import ir.farimod.app.navigation.Tabs
import ir.farimod.app.navigation.useIsFocused
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.navigation.useRoute
import ir.farimod.app.network.rnJson
import ir.farimod.app.platform.PlatformBackHandler
import ir.farimod.app.screens.auth.AuthBackdrop
import ir.farimod.app.screens.auth.AuthGlassCard
import ir.farimod.app.screens.auth.AuthHeroIcon
import ir.farimod.app.screens.auth.AuthRnTimingEasing
import ir.farimod.app.screens.auth.AuthShadowTitle
import ir.farimod.app.screens.auth.authOtpLengthError
import ir.farimod.app.screens.auth.authRnTiming
import ir.farimod.app.services.AuthService
import ir.farimod.app.state.AuthState
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.AppButton
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.OTPInput
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.Screen
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.ToastData
import ir.farimod.app.ui.touchableOpacity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Port of `screens/OTPScreen.tsx`. */
@Composable
fun OTPScreen() {
    val navigation = useNavigation()
    val route = useRoute()
    val mobileNumber = route.str("mobileNumber") ?: ""
    val scope = rememberCoroutineScope()
    val focused = useIsFocused()

    var otp by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf(ToastData()) }
    var countdown by remember { mutableStateOf(120) }

    val iconFade = remember { Animatable(0f) }
    val iconSlide = remember { Animatable(-50f) }
    val formFade = remember { Animatable(0f) }
    val formSlide = remember { Animatable(40f) }

    // RN: navigate("App", { screen: "MainTabs", params: { screen: "خانه" } }).
    // KMP has no "App" route; other screens flatten this to MainTabs + tab name.
    fun goAppHome() {
        navigation.navigate("MainTabs", mapOf("screen" to Tabs.Home))
    }

    PlatformBackHandler(enabled = focused) { goAppHome() }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { iconFade.animateTo(1f, authRnTiming(800)) }
            launch { iconSlide.animateTo(0f, authRnTiming(800)) }
        }
        coroutineScope {
            launch { formFade.animateTo(1f, authRnTiming(900)) }
            launch { formSlide.animateTo(0f, authRnTiming(900)) }
        }
    }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown -= 1
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "otpPulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(2500, easing = AuthRnTimingEasing), RepeatMode.Reverse),
        label = "otpPulse"
    )
    val spinTransition = rememberInfiniteTransition(label = "otpSpin")
    val spin by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(10000, easing = AuthRnTimingEasing), RepeatMode.Restart),
        label = "otpSpin"
    )

    fun showToast(message: String, type: String = "info") {
        toast = ToastData(visible = true, message = message, type = type)
    }

    fun hideToast() {
        toast = ToastData(visible = false, message = "", type = "info")
    }

    fun handleVerifyOTP() {
        val err = authOtpLengthError(otp)
        otpError = err
        if (err != null || isLoading) return
        isLoading = true
        val code = otp
        scope.launch {
            try {
                val result = AuthService.verifyOTP(mobileNumber, code)
                if (result.success) {
                    val data = result.data
                    val user = if (data != null) {
                        runCatching { rnJson.decodeFromJsonElement(UserData.serializer(), data) }.getOrNull()
                    } else {
                        null
                    }
                    if (user == null) {
                        showToast("خطا در اتصال به سرور", "error")
                    } else {
                        AuthState.login(user)
                        showToast(result.message ?: "ورود موفقیت‌آمیز", "success")
                        navigation.reset("MainTabs")
                    }
                } else {
                    showToast(result.message ?: "کد تایید اشتباه است", "error")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                showToast("خطا در اتصال به سرور", "error")
            } finally {
                isLoading = false
            }
        }
    }

    fun handleResendOTP() {
        if (countdown != 0 || isLoading) return
        isLoading = true
        scope.launch {
            try {
                val result = AuthService.sendOTP(mobileNumber)
                if (result.success) {
                    countdown = 120
                    showToast(result.message ?: "", "success")
                } else {
                    showToast(result.message ?: "", "error")
                }
            } finally {
                isLoading = false
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AuthBackdrop {
            Screen(Modifier.padding(10.dp)) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.fillMaxWidth()) {
                        AuthGlassCard(
                            alpha = { formFade.value },
                            translationY = { formSlide.value },
                            modifier = Modifier.fillMaxWidth(),
                            shadowElevation = 6.dp,
                            shadowColor = Color.White
                        ) {
                            AuthShadowTitle(text = "کد تائید یکبار مصرف", top = 50.dp, bottom = 20.dp)
                            AppText(
                                text = "کد تایید ۵ رقمی به شماره $mobileNumber ارسال شد",
                                modifier = Modifier.fillMaxWidth().padding(bottom = 30.dp),
                                fontSize = 16.sp,
                                color = FarimodColors.Medium,
                                fontFamily = RNFonts.Yekan_Bakh_Regular,
                                textAlign = TextAlign.Center,
                                lineHeight = 24.sp
                            )
                            Column(
                                Modifier.fillMaxWidth().padding(bottom = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                OTPInput(
                                    onCodeChange = { code ->
                                        otp = code
                                        otpError = authOtpLengthError(code)
                                    },
                                    code = otp
                                )
                                if (otpError != null) {
                                    AppText(
                                        text = otpError ?: "",
                                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                                        fontSize = 14.sp,
                                        color = FarimodColors.Danger,
                                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            // loginButton marginTop 20 replaces the button's marginVertical top (8).
                            AppButton(
                                title = if (isLoading) "در حال بررسی..." else "تایید",
                                onPress = { handleVerifyOTP() },
                                modifier = Modifier.padding(top = 12.dp),
                                disabled = isLoading || otp.length != 5
                            )
                            Box(
                                Modifier.fillMaxWidth().padding(top = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (countdown > 0) {
                                    AppText(
                                        text = "ارسال مجدد کد در ${formatOtpCountdown(countdown)}",
                                        fontSize = 14.sp,
                                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                                        color = FarimodColors.Medium,
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    AppText(
                                        text = "ارسال مجدد کد تایید",
                                        modifier = Modifier.touchableOpacity(enabled = !isLoading) { handleResendOTP() },
                                        fontSize = 16.sp,
                                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                                        color = if (isLoading) FarimodColors.Medium else FarimodColors.Primary,
                                        textDecoration = if (isLoading) null else TextDecoration.Underline,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            RowReverse(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 30.dp)
                                    .touchableOpacity { goAppHome() },
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MaterialIcons("arrow-forward", size = 20.dp, color = FarimodColors.Primary)
                                AppText(
                                    text = "تغییر شماره موبایل",
                                    modifier = Modifier.absolutePadding(right = 8.dp),
                                    fontSize = 16.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                                    color = FarimodColors.Primary
                                )
                            }
                        }
                        AuthHeroIcon(
                            iconName = "security",
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .absoluteOffset(y = (-60).dp)
                                .zIndex(1000f),
                            alpha = { iconFade.value },
                            translationY = { iconSlide.value },
                            scale = { pulse },
                            ringRotation = { spin }
                        )
                    }
                }
            }
        }
        Toast(toast.visible, toast.message, toast.type, onHide = { hideToast() })
    }
}

/** RN `formatTime`: minutes unpadded, seconds padded, English digits. */
private fun formatOtpCountdown(seconds: Int): String {
    val mins = seconds / 60
    val secs = (seconds % 60).toString().padStart(2, '0')
    return "$mins:$secs"
}
