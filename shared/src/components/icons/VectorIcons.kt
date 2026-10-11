package ir.farimod.app.components.icons

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.farimod.app.resources.Res
import ir.farimod.app.resources.ionicons
import ir.farimod.app.resources.material_community_icons
import ir.farimod.app.resources.material_icons
import org.jetbrains.compose.resources.Font

/**
 * Drop-in replacement for `@expo/vector-icons` (MaterialIcons / Ionicons / MaterialCommunityIcons).
 * The very same icon fonts used by the React Native app are bundled, so every glyph is pixel-identical.
 *
 * Usage mirrors React Native:
 *   MaterialIcons("phone-android", size = 24.dp, color = Color.White)
 */

private fun parseGlyphs(chunks: List<String>): Map<String, Int> {
    val map = HashMap<String, Int>(chunks.size * 400)
    for (chunk in chunks) {
        for (entry in chunk.split(';')) {
            val i = entry.lastIndexOf('=')
            if (i <= 0) continue
            map[entry.substring(0, i)] = entry.substring(i + 1).toInt(16)
        }
    }
    return map
}

private val materialMap: Map<String, Int> by lazy { parseGlyphs(materialGlyphsChunks) }
private val ioniconsMap: Map<String, Int> by lazy { parseGlyphs(ioniconsGlyphsChunks) }
private val mciMap: Map<String, Int> by lazy { parseGlyphs(mciGlyphsChunks) }

private fun glyphString(code: Int?): String =
    if (code == null) "?" else {
        // code points above 0xFFFF need surrogate pairs
        if (code <= 0xFFFF) code.toChar().toString()
        else {
            val c = code - 0x10000
            val hi = (0xD800 + (c shr 10)).toChar()
            val lo = (0xDC00 + (c and 0x3FF)).toChar()
            "$hi$lo"
        }
    }

@Composable
private fun GlyphIcon(
    glyph: String,
    fontFamily: FontFamily,
    size: Dp,
    color: Color,
    modifier: Modifier
) {
    val fontSizeSp = with(LocalDensity.current) { size.toSp() }
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Text(
            text = glyph,
            style = TextStyle(
                fontFamily = fontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = fontSizeSp,
                lineHeight = fontSizeSp,
                color = color,
                textAlign = TextAlign.Center,
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both
                )
            ),
            softWrap = false,
            maxLines = 1
        )
    }
}

@Composable
fun MaterialIcons(
    name: String,
    size: Dp = 24.dp,
    color: Color = Color.Black,
    modifier: Modifier = Modifier
) {
    val family = FontFamily(Font(Res.font.material_icons))
    val glyph = remember(name) { glyphString(materialMap[name]) }
    GlyphIcon(glyph, family, size, color, modifier)
}

@Composable
fun Ionicons(
    name: String,
    size: Dp = 24.dp,
    color: Color = Color.Black,
    modifier: Modifier = Modifier
) {
    val family = FontFamily(Font(Res.font.ionicons))
    val glyph = remember(name) { glyphString(ioniconsMap[name]) }
    GlyphIcon(glyph, family, size, color, modifier)
}

@Composable
fun MaterialCommunityIcons(
    name: String,
    size: Dp = 24.dp,
    color: Color = Color.Black,
    modifier: Modifier = Modifier
) {
    val family = FontFamily(Font(Res.font.material_community_icons))
    val glyph = remember(name) { glyphString(mciMap[name]) }
    GlyphIcon(glyph, family, size, color, modifier)
}
