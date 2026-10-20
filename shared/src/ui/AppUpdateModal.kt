package ir.farimod.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.platform.AppBuildConfig
import ir.farimod.app.platform.AppDistribution
import ir.farimod.app.platform.PlatformBackHandler
import ir.farimod.app.state.AppUpdateInfo
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Port of `app/components/AppUpdateModal.tsx`.
 *
 * RN renders a transparent `<Modal>`; here it is a full-size overlay (the host is mounted last in the root `Box` of
 * `App.kt`, so it covers the whole app). Optional updates can be dismissed by tapping anywhere on the overlay
 * (RN `Pressable` parent receives taps of non-touchable children as well) or with the back button;
 * required updates (`type == "required"`) swallow the back button and cannot be dismissed.
 */
@Composable
fun AppUpdateModal(
    visible: Boolean,
    updateInfo: AppUpdateInfo?,
    onDirectDownload: (() -> Unit)? = null,
    onCafeBazarDownload: (() -> Unit)? = null,
    onLater: (() -> Unit)? = null,
    onClose: (() -> Unit)? = null
) {
    if (!visible || updateInfo == null) return

    val isRequired = updateInfo.type == "required"

    // required: BackHandler returns true (consumes); optional: onRequestClose = onClose
    PlatformBackHandler(enabled = true) {
        if (!isRequired) onClose?.invoke()
    }

    // Animated.parallel([ spring(slide 300 → 0, tension 100, friction 8), timing(opacity 0 → 1, 200) ])
    val slideAnim = remember { Animatable(300f) }
    val opacityAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        coroutineScope {
            launch { slideAnim.animateTo(0f, rnSpring(100f, 8f)) }
            launch { opacityAnim.animateTo(1f, rnTiming(200)) }
        }
    }

    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isRequired) Color(0f, 0f, 0f, 0.95f) else Color(0f, 0f, 0f, 0.7f))
            .pressable {
                // handleBackdropPress
                if (!isRequired) onClose?.invoke()
            }
            .padding(horizontal = 30.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = slideAnim.value.dp.toPx()
                    alpha = opacityAnim.value
                }
                .rnShadow(if (isRequired) 25.dp else 15.dp, shape)
                .background(Color(0xFFFFFFFF), shape)
                .then(if (isRequired) Modifier.border(3.dp, Color(0xFFEF4444), shape) else Modifier)
                .padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // iconContainer
            Box(
                modifier = Modifier
                    .absolutePadding(bottom = 20.dp)
                    .size(80.dp)
                    .background(if (isRequired) Color(0xFFFEE2E2) else Color(0xFFE0F7FA), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                MaterialIcons(
                    name = if (isRequired) "warning" else "system-update",
                    size = 48.dp,
                    color = if (isRequired) Color(0xFFEF4444) else Color(0xFF0891B2)
                )
            }

            // title
            AppText(
                text = updateInfo.title,
                modifier = Modifier.absolutePadding(bottom = 12.dp),
                fontSize = if (isRequired) 22.sp else 20.sp,
                color = if (isRequired) Color(0xFFEF4444) else Color(0xFF1F2937),
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                textAlign = TextAlign.Center
            )

            // message
            AppText(
                text = updateInfo.message,
                modifier = Modifier.absolutePadding(bottom = 20.dp),
                fontSize = if (isRequired) 17.sp else 16.sp,
                color = if (isRequired) Color(0xFF374151) else Color(0xFF6B7280),
                fontFamily = if (isRequired) RNFonts.Yekan_Bakh_Bold else RNFonts.Yekan_Bakh_Regular,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )

            if (isRequired) {
                RowReverse(
                    modifier = Modifier
                        .absolutePadding(bottom = 20.dp)
                        .background(Color(0xFFFEF2F2), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(10.dp))
                        .padding(horizontal = 15.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MaterialIcons(name = "error", size = 16.dp, color = Color(0xFFEF4444))
                    AppText(
                        text = "این بروزرسانی اجباری است و باید انجام شود",
                        modifier = Modifier.weight(1f).absolutePadding(right = 8.dp),
                        fontSize = 13.sp,
                        color = Color(0xFFEF4444),
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // buttonsContainer
            Column(
                modifier = Modifier.fillMaxWidth().absolutePadding(top = 20.dp)
            ) {
                val dist = AppBuildConfig.activeDistribution
                if (dist.isMyket) {
                    UpdateButton(
                        icon = "shop",
                        iconColor = Color(0xFFFFFFFF),
                        text = "بروزرسانی از مایکت",
                        textColor = Color(0xFFFFFFFF),
                        background = Color(0xFF2563EB),
                        onPress = { onCafeBazarDownload?.invoke() }
                    )
                } else if (dist.isBazaar) {
                    UpdateButton(
                        icon = "shop",
                        iconColor = Color(0xFFFFFFFF),
                        text = "بروزرسانی از کافه بازار",
                        textColor = Color(0xFFFFFFFF),
                        background = Color(0xFF8FB937),
                        onPress = { onCafeBazarDownload?.invoke() }
                    )
                }

                UpdateButton(
                    icon = "download",
                    iconColor = Color(0xFFFFFFFF),
                    text = "دانلود با لینک مستقیم",
                    textColor = Color(0xFFFFFFFF),
                    background = Color(0xFF1E40AF),
                    onPress = { onDirectDownload?.invoke() }
                )
                if (!isRequired) {
                    UpdateButton(
                        icon = "schedule",
                        iconColor = Color(0xFF6B7280),
                        text = "بعداً بروزرسانی می‌کنم",
                        textColor = Color(0xFF6B7280),
                        background = Color(0xFFF3F4F6),
                        borderColor = Color(0xFFD1D5DB),
                        onPress = { onLater?.invoke() }
                    )
                }
            }

            if (isRequired) {
                // forceUpdateWarning (flexDirection: "row")
                Row(
                    modifier = Modifier
                        .absolutePadding(top = 15.dp)
                        .background(Color(0xFFFEF2F2), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(10.dp))
                        .padding(horizontal = 15.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MaterialIcons(name = "block", size = 18.dp, color = Color(0xFFEF4444))
                    AppText(
                        text = "تا زمان بروزرسانی، امکان استفاده از اپ وجود ندارد",
                        modifier = Modifier.weight(1f).absolutePadding(right = 8.dp),
                        fontSize = 13.sp,
                        color = Color(0xFFEF4444),
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/** `styles.button` (+ bazar/download/later variants): full width, paddingVertical 15, radius 12, minHeight 50, mb 12. */
@Composable
private fun UpdateButton(
    icon: String,
    iconColor: Color,
    text: String,
    textColor: Color,
    background: Color,
    onPress: () -> Unit,
    borderColor: Color? = null
) {
    val shape = RoundedCornerShape(12.dp)
    RowReverse(
        modifier = Modifier
            .fillMaxWidth()
            .absolutePadding(bottom = 12.dp)
            .touchableOpacity { onPress() }
            .background(background, shape)
            .then(if (borderColor != null) Modifier.border(1.dp, borderColor, shape) else Modifier)
            .heightIn(min = 50.dp)
            .padding(vertical = 15.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MaterialIcons(name = icon, size = 20.dp, color = iconColor)
        AppText(
            text = text,
            modifier = Modifier.absolutePadding(right = 8.dp),
            fontSize = 15.sp,
            color = textColor,
            fontFamily = RNFonts.Yekan_Bakh_Bold
        )
    }
}
