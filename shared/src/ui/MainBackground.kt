package ir.farimod.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import ir.farimod.app.resources.Res
import ir.farimod.app.resources.background_1
import org.jetbrains.compose.resources.imageResource

/**
 * Port of `components/MainBackground.tsx` (absolute, full-size, no touches).
 *  - `background-1.jpg`, resizeMode "repeat" (tiled at the intrinsic pixel size), opacity 0.06,
 *    width 100%, height 250% of the container, positioned top.
 *  - LinearGradient ["#e2bcef", "transparent"] over the top 70%.
 */
@Composable
fun MainBackground() {
    val bitmap = imageResource(Res.drawable.background_1)
    val brush = remember(bitmap) {
        ShaderBrush(ImageShader(bitmap, TileMode.Repeated, TileMode.Repeated))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // ImageBackground (height 250%): tiles are drawn at intrinsic px size, anchored at the top-left corner.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        brush = brush,
                        size = Size(size.width, size.height * 2.5f),
                        alpha = 0.06f
                    )
                }
        )
        // LinearGradient colors={["#e2bcef", "transparent"]} height: 70%
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .linearGradient(listOf(Color(0xFFE2BCEF), Color.Transparent))
        )
    }
}
