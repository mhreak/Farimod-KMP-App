package ir.farimod.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.theme.FarimodColors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** State holder used by screens: `var toast by remember { mutableStateOf(ToastData()) }` */
data class ToastData(
    val visible: Boolean = false,
    val message: String = "",
    val type: String = "info"
)

/**
 * Port of `components/Toast.tsx` — absolute overlay at the top (top 50, left/right 20).
 * Place as the LAST child of the screen's root Box. The overlay does not consume touches.
 */
@Composable
fun Toast(
    visible: Boolean,
    message: String,
    type: String = "info",
    duration: Long = 3000,
    onHide: () -> Unit
) {
    val translateY = remember { Animatable(-100f) }
    val opacity = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val currentOnHide by rememberUpdatedState(onHide)

    suspend fun hideToast() {
        coroutineScope {
            launch { translateY.animateTo(-100f, rnTiming(300)) }
            launch { opacity.animateTo(0f, rnTiming(300)) }
        }
        currentOnHide()
    }

    LaunchedEffect(visible) {
        if (visible) {
            // Show animation
            launch {
                coroutineScope {
                    launch { translateY.animateTo(0f, rnTiming(300)) }
                    launch { opacity.animateTo(1f, rnTiming(300)) }
                }
            }
            // Auto hide after duration
            delay(duration)
            hideToast()
        }
    }

    if (!visible) return

    val (backgroundColor, iconName) = when (type) {
        "success" -> FarimodColors.Success to "check-circle"
        "error" -> FarimodColors.Danger to "error"
        "warning" -> FarimodColors.Warning to "warning"
        else -> FarimodColors.Info to "info"
    }
    val contentColor = Color.White
    val density = LocalDensity.current
    val shape = RoundedCornerShape(12.dp)

    Box(modifier = Modifier.fillMaxSize().zIndex(9999f)) {
        RowReverse(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .absolutePadding(left = 20.dp, right = 20.dp, top = 50.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = translateY.value * density.density
                    alpha = opacity.value
                }
                .rnShadow(8.dp, shape, Color.Black)
                .background(backgroundColor, shape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MaterialIcons(
                name = iconName,
                size = 24.dp,
                color = contentColor,
                modifier = Modifier.absolutePadding(left = 12.dp)
            )
            AppText(
                text = message,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                color = contentColor,
                fontFamily = RNFonts.Yekan_Bakh_Regular,
                textAlign = TextAlign.Right,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .absolutePadding(right = 8.dp)
                    .touchableOpacity { scope.launch { hideToast() } }
                    .padding(4.dp)
            ) {
                MaterialIcons(name = "close", size = 20.dp, color = contentColor)
            }
        }
    }
}
