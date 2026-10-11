package ir.farimod.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline

/**
 * expo-linear-gradient equivalent.
 *   <LinearGradient colors={[a, b]} start={{x:0,y:0}} end={{x:1,y:1}} locations={[0, 1]} />
 * start / end are FRACTIONS of the box (like RN), default is top → bottom (0.5,0) → (0.5,1).
 * Use as a background modifier: `Modifier.linearGradient(listOf(a, b), start = Offset(0f, 0f), end = Offset(1f, 1f))`
 * Combine with `.clip(RoundedCornerShape(..))` for rounded corners, or pass [shape].
 */
fun Modifier.linearGradient(
    colors: List<Color>,
    start: Offset = Offset(0.5f, 0f),
    end: Offset = Offset(0.5f, 1f),
    locations: List<Float>? = null,
    shape: Shape = RectangleShape
): Modifier = this.drawBehind {
    val s = Offset(start.x * size.width, start.y * size.height)
    val e = Offset(end.x * size.width, end.y * size.height)
    val brush = if (locations != null && locations.size == colors.size) {
        Brush.linearGradient(
            colorStops = Array(colors.size) { i -> locations[i] to colors[i] },
            start = s,
            end = e
        )
    } else {
        Brush.linearGradient(colors = colors, start = s, end = e)
    }
    if (shape == RectangleShape) {
        drawRect(brush)
    } else {
        val outline = shape.createOutline(size, layoutDirection, this)
        drawOutline(outline, brush)
    }
}

/** `<LinearGradient style={...}>children</LinearGradient>` */
@Composable
fun LinearGradient(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    start: Offset = Offset(0.5f, 0f),
    end: Offset = Offset(0.5f, 1f),
    locations: List<Float>? = null,
    shape: Shape = RectangleShape,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(modifier = modifier.linearGradient(colors, start, end, locations, shape), content = content)
}

/** `rgba(r,g,b,a)` helper: rgba(255, 206, 232, 0.7f) */
fun rgba(r: Int, g: Int, b: Int, a: Float = 1f): Color = Color(r / 255f, g / 255f, b / 255f, a)
