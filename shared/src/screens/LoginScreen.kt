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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.screens.auth.AuthBackdrop
import ir.farimod.app.screens.auth.AuthGlassCard
import ir.farimod.app.screens.auth.AuthHeroIcon
import ir.farimod.app.screens.auth.AuthRnTimingEasing
import ir.farimod.app.screens.auth.AuthShadowTitle
import ir.farimod.app.screens.auth.authMobileError
import ir.farimod.app.screens.auth.authRnTiming
import ir.farimod.app.services.AuthService
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.AppButton
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.AppTextInput
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.Screen
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.ToastData
import ir.farimod.app.ui.touchableOpacity
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Port of `screens/LoginScreen.tsx`. */
@Composable
fun LoginScreen() {
    val navigation = useNavigation()
    val scope = rememberCoroutineScope()
    var mobileNumber by remember { mutableStateOf("") }
    var mobileError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf(ToastData()) }

    val iconFade = remember { Animatable(0f) }
    val iconSlide = remember { Animatable(-50f) }
    val formFade = remember { Animatable(0f) }
    val formSlide = remember { Animatable(40f) }

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

    val pulseTransition = rememberInfiniteTransition(label = "loginPulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(2500, easing = AuthRnTimingEasing), RepeatMode.Reverse),
        label = "loginPulse"
    )
    val spinTransition = rememberInfiniteTransition(label = "loginSpin")
    val spin by spinTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(10000, easing = AuthRnTimingEasing), RepeatMode.Restart),
        label = "loginSpin"
    )

    fun showToast(message: String, type: String = "info") {
        toast = ToastData(visible = true, message = message, type = type)
    }

    fun hideToast() {
        toast = ToastData(visible = false, message = "", type = "info")
    }

    fun publishMobileError() {
        mobileError = authMobileError(mobileNumber)
    }

    fun handleSendOTP() {
        val err = authMobileError(mobileNumber)
        mobileError = err
        if (err != null || isLoading) return
        isLoading = true
        val number = mobileNumber
        scope.launch {
            try {
                val result = AuthService.sendOTP(number)
                if (result.success) {
                    showToast(result.message ?: "", "success")
                    // RN setTimeout is not cleared on unmount.
                    scope.launch {
                        delay(1000)
                        navigation.navigate("OTP", mapOf("mobileNumber" to number))
                    }
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
            // container: padding 10, justifyContent center. fontFamily on the View is a no-op.
            Screen(Modifier.padding(10.dp)) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // iconCircle is position absolute top -80 inside a collapsed iconContainer
                    // whose marginBottom -20 pulls the card up, so the icon top is 60dp above the card.
                    Box(Modifier.fillMaxWidth()) {
                        AuthGlassCard(
                            alpha = { formFade.value },
                            translationY = { formSlide.value },
                            modifier = Modifier.fillMaxWidth(),
                            shadowElevation = 6.dp,
                            shadowColor = Color.White
                        ) {
                            AuthShadowTitle(text = "ورود به حساب کاربری", top = 50.dp, bottom = 20.dp)
                            AppText(
                                text = "شماره موبایل خود را وارد کنید تا کد تایید برای شما ارسال شود",
                                modifier = Modifier.fillMaxWidth().padding(bottom = 30.dp),
                                fontSize = 16.sp,
                                color = FarimodColors.Medium,
                                fontFamily = RNFonts.Yekan_Bakh_Regular,
                                textAlign = TextAlign.Center,
                                lineHeight = 24.sp
                            )
                            AppTextInput(
                                value = mobileNumber,
                                onChangeText = {
                                    mobileNumber = it
                                    mobileError = authMobileError(it)
                                },
                                icon = "phone-android",
                                placeholder = "شماره موبایل",
                                keyboardType = KeyboardType.Phone,
                                maxLength = 11,
                                onBlur = { publishMobileError() }
                            )
                            if (mobileError != null) {
                                AppText(
                                    text = mobileError ?: "",
                                    modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                                    fontSize = 14.sp,
                                    color = FarimodColors.Danger,
                                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                                    textAlign = TextAlign.Center
                                )
                            }
                            // loginButton marginTop 10 replaces the button's marginVertical top (8).
                            AppButton(
                                title = if (isLoading) "در حال ارسال..." else "ارسال کد تایید",
                                onPress = { handleSendOTP() },
                                modifier = Modifier.padding(top = 2.dp),
                                disabled = isLoading
                            )
                            RowReverse(
                                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppText(
                                    text = "حساب کاربری ندارید؟ ",
                                    fontSize = 16.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_Regular
                                )
                                AppText(
                                    text = "ثبت نام کنید",
                                    modifier = Modifier.touchableOpacity { navigation.navigate("Signup") },
                                    fontSize = 16.sp,
                                    color = FarimodColors.Primary,
//                                    textDecoration = TextDecoration.Underline
                                )
                            }
                        }
                        AuthHeroIcon(
                            iconName = "phone-android",
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
