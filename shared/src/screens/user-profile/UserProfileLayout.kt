package ir.farimod.app.screens.userprofile

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.farimod.app.network.ApiConfig
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/** `modernColors` + `PROFILE_CONSTANTS` from user-profile/styles/styles.tsx. */
internal object UpHex {
    const val primary = "#667eea"
    const val primaryDark = "#764ba2"
    const val secondary = "#ff6b6b"
    const val tertiary = "#4ecdc4"
    const val accent = "#45b7d1"
    const val surface = "#ffffff"
    const val dark = "#2c3e50"
    const val medium = "#34495e"
    const val success = "#2ecc71"
    const val warning = "#f39c12"
    const val error = "#e74c3c"
    const val info = "#3498db"
}

internal object UpConst {
    const val CIRCLE = 1800
    const val AVATAR = 120
    const val AVATAR_OUTER = 130
    const val AVATAR_MIDDLE = 125
    const val PROFILE_TOP = 840
}

internal val UpPrimary = Color(0xFF667EEA)
internal val UpPrimaryDark = Color(0xFF764BA2)
internal val UpSecondary = Color(0xFFFF6B6B)
internal val UpTertiary = Color(0xFF4ECDC4)
internal val UpAccent = Color(0xFF45B7D1)
internal val UpSurface = Color.White
internal val UpDark = Color(0xFF2C3E50)
internal val UpMedium = Color(0xFF34495E)
internal val UpSuccess = Color(0xFF2ECC71)
internal val UpWarning = Color(0xFFF39C12)
internal val UpError = Color(0xFFE74C3C)
internal val UpInfo = Color(0xFF3498DB)

/** CSS hex: `#RGB`, `#RRGGBB`, `#RRGGBBAA` (alpha last, as in `"#667eea" + "20"`). */
internal fun upHex(value: String?): Color {
    val s = (value ?: return Color.Transparent).trim().removePrefix("#")
    return try {
        when (s.length) {
            3 -> Color(
                s.substring(0, 1).repeat(2).toInt(16),
                s.substring(1, 2).repeat(2).toInt(16),
                s.substring(2, 3).repeat(2).toInt(16)
            )
            6 -> Color(
                s.substring(0, 2).toInt(16),
                s.substring(2, 4).toInt(16),
                s.substring(4, 6).toInt(16)
            )
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

internal fun upHex(base: String, alphaSuffix: String): Color = upHex(base + alphaSuffix)

/** RN `Animated.timing` default easing (`Easing.inOut(Easing.ease)`). */
internal val UpTimingEasing: Easing = run {
    val ease = CubicBezierEasing(0.42f, 0f, 1f, 1f)
    Easing { t ->
        if (t < 0.5f) ease.transform(t * 2f) / 2f
        else 1f - ease.transform((1f - t) * 2f) / 2f
    }
}

internal val LocalUpSkeletonAlpha = compositionLocalOf { 1f }

/** `ShowAboutMeText === true` (strict boolean, not truthy). */
internal fun JsonElement?.isStrictTrue(): Boolean =
    (this as? JsonPrimitive)?.booleanOrNull == true

internal fun upApi(path: String): String = ApiConfig.BASE_URL + path

/** JS `encodeURIComponent`. */
internal fun upEncodeURIComponent(value: String): String {
    val unreserved = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.!~*'()"
    return buildString {
        for (ch in value) {
            if (ch in unreserved) append(ch)
            else {
                val bytes = ch.toString().encodeToByteArray()
                for (b in bytes) {
                    val hex = (b.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')
                    append('%')
                    append(hex)
                }
            }
        }
    }
}

internal const val UP_STRETCH = 0
internal const val UP_CENTER = 1
internal const val UP_END = 2

private class UpParentData(
    val marginTop: Dp,
    val marginBottom: Dp,
    val align: Int,
    val zIndex: Float,
    val width: Dp?,
    val height: Dp?
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any = this@UpParentData
}

/**
 * Marks a direct child of [UpFlow]. Negative margins pull the next sibling up,
 * matching the profile header's Yoga margins (giant circle + back button).
 * [align]: [UP_STRETCH] fills the parent width, [UP_CENTER] / [UP_END] are cross-axis.
 */
internal fun Modifier.upChild(
    marginTop: Dp = 0.dp,
    marginBottom: Dp = 0.dp,
    align: Int = UP_STRETCH,
    zIndex: Float = 0f,
    width: Dp? = null,
    height: Dp? = null
): Modifier = this.then(UpParentData(marginTop, marginBottom, align, zIndex, width, height))

/**
 * Column that honors negative top/bottom margins (RN Yoga). Children keep their drawn
 * overflow, and the reported height is the lowest bottom edge.
 */
@Composable
internal fun UpFlow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    SubcomposeLayout(modifier) { constraints ->
        val parentW = constraints.maxWidth
        val measurables = subcompose(0, content)
        var cursor = 0
        var maxBottom = 0
        val placed = measurables.map { measurable ->
            val data = measurable.parentData as? UpParentData
                ?: UpParentData(0.dp, 0.dp, UP_STRETCH, 0f, null, null)
            val w = data.width?.roundToPx()
            val h = data.height?.roundToPx()
            val childConstraints = if (w != null && h != null) {
                Constraints.fixed(w, h)
            } else if (data.align == UP_STRETCH && w == null) {
                Constraints(
                    minWidth = parentW.coerceAtLeast(0),
                    maxWidth = if (parentW == Constraints.Infinity) Constraints.Infinity else parentW.coerceAtLeast(0),
                    minHeight = h ?: 0,
                    maxHeight = h ?: Constraints.Infinity
                )
            } else {
                Constraints(
                    minWidth = w ?: 0,
                    maxWidth = w ?: Constraints.Infinity,
                    minHeight = h ?: 0,
                    maxHeight = h ?: Constraints.Infinity
                )
            }
            val placeable = measurable.measure(childConstraints)
            val y = cursor + data.marginTop.roundToPx()
            val bottom = y + placeable.height
            cursor = bottom + data.marginBottom.roundToPx()
            if (bottom > maxBottom) maxBottom = bottom
            if (cursor > maxBottom) maxBottom = cursor
            Triple(placeable, data, y)
        }
        layout(if (parentW == Constraints.Infinity) 0 else parentW, maxBottom.coerceAtLeast(0)) {
            placed.forEach { (placeable, data, y) ->
                val x = when (data.align) {
                    UP_CENTER -> (parentW - placeable.width) / 2
                    UP_END -> parentW - placeable.width
                    else -> 0
                }
                placeable.place(x, y, zIndex = data.zIndex)
            }
        }
    }
}
