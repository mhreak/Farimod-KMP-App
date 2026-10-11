package ir.farimod.app.screens.introflow

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.farimod.app.ui.rnTextStyle

/** RN `textShadowColor / textShadowOffset / textShadowRadius` (dp values) -> Compose [Shadow] (px). */
@Composable
internal fun introTextShadow(color: Color, dx: Float, dy: Float, radius: Float): Shadow {
    val d = LocalDensity.current.density
    return Shadow(color = color, offset = Offset(dx * d, dy * d), blurRadius = radius * d)
}

/**
 * RN `<Text style={{ fontSize, fontFamily, color, textAlign, lineHeight, textShadow* }}>`.
 * [fontSize] / [lineHeight] are RN logical sizes (dp); when [allowFontScaling] is false the system font scale is ignored.
 */
@Composable
internal fun IntroText(
    text: String,
    fontSize: Float,
    color: Color,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Right,
    lineHeight: Float? = null,
    shadow: Shadow? = null,
    allowFontScaling: Boolean = true
) {
    val density = LocalDensity.current
    val fs: TextUnit = if (allowFontScaling) fontSize.sp else with(density) { fontSize.dp.toSp() }
    val lh: TextUnit = when {
        lineHeight == null -> TextUnit.Unspecified
        allowFontScaling -> lineHeight.sp
        else -> with(density) { lineHeight.dp.toSp() }
    }
    val base = rnTextStyle(
        fontSize = fs,
        color = color,
        fontFamily = fontFamily,
        textAlign = textAlign,
        lineHeight = lh
    )
    Text(
        text = text,
        modifier = modifier,
        style = if (shadow != null) base.copy(shadow = shadow) else base
    )
}

/**
 * One RN `transform` stage on an `Animated.View`. Values are read lazily inside the draw phase.
 * RN applies the transform list left-to-right (first = outermost), so chain several `introLayer`
 * calls in the same order when rotate/scale must come before translate. Translations are in dp.
 */
internal fun Modifier.introLayer(
    alpha: () -> Float = { 1f },
    scale: () -> Float = { 1f },
    rotation: () -> Float = { 0f },
    translationX: () -> Float = { 0f },
    translationY: () -> Float = { 0f }
): Modifier = this.graphicsLayer {
    this.alpha = alpha()
    val s = scale()
    this.scaleX = s
    this.scaleY = s
    this.rotationZ = rotation()
    this.translationX = translationX() * density
    this.translationY = translationY() * density
}
