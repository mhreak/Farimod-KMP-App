package ir.farimod.app.screens.auth

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.resources.Res
import ir.farimod.app.resources.background_1
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.rgba
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.rnTextStyle
import org.jetbrains.compose.resources.imageResource

/**
 * `Animated.timing` default easing: `Easing.inOut(Easing.ease)` with `Easing.ease = bezier(0.42, 0, 1, 1)`.
 * Duplicated here because `rnTiming` in ui is internal to that package.
 */
internal val AuthRnTimingEasing: Easing = run {
    val ease = CubicBezierEasing(0.42f, 0f, 1f, 1f)
    Easing { t ->
        if (t < 0.5f) ease.transform(t * 2f) / 2f
        else 1f - ease.transform((1f - t) * 2f) / 2f
    }
}

internal fun <T> authRnTiming(durationMillis: Int): FiniteAnimationSpec<T> =
    tween(durationMillis = durationMillis, easing = AuthRnTimingEasing)

/** Yup `mobileNumber` on Login (`matches` then `required`, abortEarly). Empty fails required; anything else fails matches. */
internal fun authMobileError(mobile: String): String? = when {
    mobile.isEmpty() -> "شماره موبایل وارد نشده است"
    !Regex("^09\\d{9}$").matches(mobile) -> "شماره موبایل معتبر نیست"
    else -> null
}

/**
 * Yup OTP on OTPScreen: `.length(5)` then `.required()`, abortEarly.
 * Empty string fails `length` (it is not absent), so the required message never wins.
 */
internal fun authOtpLengthError(otp: String): String? =
    if (otp.length == 5) null else "کد تایید باید 5 رقم باشد"

/**
 * RN marginTop / marginBottom, including a negative bottom margin.
 * Reported height is top + child + bottom so the next sibling overlaps when bottom is negative.
 */
internal fun Modifier.authVerticalMargins(top: Dp, bottom: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val topPx = top.roundToPx()
    val bottomPx = bottom.roundToPx()
    val height = (topPx + placeable.height + bottomPx).coerceAtLeast(0)
    layout(placeable.width, height) {
        placeable.place(0, topPx)
    }
}

/** Tiled `background-1.jpg` (resizeMode repeat) plus the white wash used by Login, OTP, and Signup. */
@Composable
internal fun AuthBackdrop(content: @Composable BoxScope.() -> Unit) {
    val bitmap = imageResource(Res.drawable.background_1)
    val brush = remember(bitmap) {
        ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated))
    }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().drawBehind { drawRect(brush) })
        LinearGradient(
            colors = listOf(
                rgba(255, 255, 255, 0.1f),
                rgba(255, 255, 255, 0.6f),
                rgba(255, 255, 255, 0.8f),
                rgba(255, 255, 255, 1f)
            ),
            modifier = Modifier.fillMaxSize(),
            content = content
        )
    }
}

/**
 * Pink glass card (borderRadius 25, padding 25, overflow hidden).
 * [shadowElevation] is the RN `elevation` (0 on Signup, where the shadow props are absent).
 * [alpha] / [translationY] are read in the draw phase (translationY is dp).
 */
@Composable
internal fun AuthGlassCard(
    alpha: () -> Float,
    translationY: () -> Float,
    modifier: Modifier = Modifier,
    shadowElevation: Dp = 6.dp,
    shadowColor: Color = Color.White,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(25.dp)
    Box(
        modifier
            .graphicsLayer {
                this.alpha = alpha()
                this.translationY = translationY() * density
            }
            .rnShadow(shadowElevation, shape, shadowColor)
            .clip(shape)
    ) {
        Box(
            Modifier
                .matchParentSize()
                .zIndex(1f)
                .background(rgba(255, 206, 232, 0.7f), shape)
                .border(1.5.dp, rgba(255, 206, 232, 1f), shape)
        )
        Column(
            Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .padding(25.dp),
            content = content
        )
    }
}

/**
 * Gradient circle + inner disc + dashed ring. Transforms (fade, slide in dp, pulse) wrap the icon.
 * [ringRotation] is degrees; null leaves the ring still (Signup).
 */
@Composable
internal fun AuthHeroIcon(
    iconName: String,
    modifier: Modifier = Modifier,
    iconSize: Dp = 65.dp,
    circleSize: Dp = 120.dp,
    innerSize: Dp = 100.dp,
    ringSize: Dp = 140.dp,
    elevation: Dp = 8.dp,
    shadowColor: Color = rgba(255, 206, 232, 0.2f),
    alpha: () -> Float = { 1f },
    translationY: () -> Float = { 0f },
    scale: () -> Float = { 1f },
    ringRotation: (() -> Float)? = null
) {
    val shadowMod = if (elevation.value > 0f) {
        Modifier.rnShadow(elevation, CircleShape, shadowColor)
    } else {
        Modifier
    }
    LinearGradient(
        colors = listOf(FarimodColors.Primary, FarimodColors.PrimaryDark),
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha()
                this.translationY = translationY() * density
                val s = scale()
                scaleX = s
                scaleY = s
            }
            .then(shadowMod)
            .size(circleSize),
        shape = CircleShape
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .zIndex(99f)
                .size(innerSize)
                .background(rgba(255, 206, 232, 0.15f), CircleShape)
                .border(2.dp, rgba(255, 206, 232, 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MaterialIcons(iconName, size = iconSize, color = FarimodColors.White)
        }
        Box(
            Modifier
                .align(Alignment.Center)
                .zIndex(0f)
                .requiredSize(ringSize)
                .graphicsLayer {
                    if (ringRotation != null) rotationZ = ringRotation()
                }
                .drawBehind {
                    val stroke = 1.5.dp.toPx()
                    drawCircle(
                        color = rgba(255, 206, 232, 0.3f),
                        radius = size.minDimension / 2f - stroke / 2f,
                        style = Stroke(
                            width = stroke,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke * 3f, stroke * 3f))
                        )
                    )
                }
        )
    }
}

/** Title with the RN textShadow (color rgba(255,206,232,0.1), offset 0,1, radius 2). */
@Composable
internal fun AuthShadowTitle(
    text: String,
    top: Dp,
    bottom: Dp,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current.density
    val style = rnTextStyle(
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
        text = text,
        modifier = modifier.fillMaxWidth().padding(top = top, bottom = bottom),
        style = style
    )
}
