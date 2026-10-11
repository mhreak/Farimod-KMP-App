package ir.farimod.app.ui

import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.farimod.app.resources.Res
import ir.farimod.app.resources.iransans_black
import ir.farimod.app.resources.iransans_bold
import ir.farimod.app.resources.iransans_medium
import ir.farimod.app.resources.yekan_bakh_bold
import ir.farimod.app.resources.yekan_bakh_extrabold
import ir.farimod.app.resources.yekan_bakh_regular
import ir.farimod.app.theme.FarimodColors
import org.jetbrains.compose.resources.Font

// ──────────────────────────────────────────────────────────────────────────────
// Colors (app/config/colors.ts) — alias for readability when porting RN code
// ──────────────────────────────────────────────────────────────────────────────
typealias colors = FarimodColors

// ──────────────────────────────────────────────────────────────────────────────
// Fonts: names are the same as the `fontFamily` strings loaded in RN App.tsx
// ──────────────────────────────────────────────────────────────────────────────
object RNFonts {
    val Yekan_Bakh_Regular: FontFamily @Composable get() = FontFamily(Font(Res.font.yekan_bakh_regular))
    val Yekan_Bakh_Bold: FontFamily @Composable get() = FontFamily(Font(Res.font.yekan_bakh_bold))
    val Yekan_Bakh_ExtraBold: FontFamily @Composable get() = FontFamily(Font(Res.font.yekan_bakh_extrabold))
    val iran_sans_medium: FontFamily @Composable get() = FontFamily(Font(Res.font.iransans_medium))
    val iran_sans_bold: FontFamily @Composable get() = FontFamily(Font(Res.font.iransans_bold))
    val iran_sans_black: FontFamily @Composable get() = FontFamily(Font(Res.font.iransans_black))
}

/** Resolve a RN `fontFamily` string ("Yekan_Bakh_Bold", ...) to a Compose [FontFamily]. */
@Composable
fun rnFont(name: String?): FontFamily = when (name) {
    "Yekan_Bakh_Regular" -> RNFonts.Yekan_Bakh_Regular
    "Yekan_Bakh_Bold" -> RNFonts.Yekan_Bakh_Bold
    "Yekan_Bakh_ExtraBold" -> RNFonts.Yekan_Bakh_ExtraBold
    "iran_sans_bold" -> RNFonts.iran_sans_bold
    "iran_sans_black" -> RNFonts.iran_sans_black
    else -> RNFonts.iran_sans_medium
}

// ──────────────────────────────────────────────────────────────────────────────
// AppText  (app/components/Text.tsx + config/styles.ts text style)
// default: color dark, fontSize 18, fontFamily iran_sans_medium, textAlign right
// ──────────────────────────────────────────────────────────────────────────────
@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 18.sp,
    color: Color = FarimodColors.Dark,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    textAlign: TextAlign = TextAlign.Right,
    lineHeight: TextUnit = TextUnit.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        style = rnTextStyle(fontSize, color, fontFamily, fontWeight, fontStyle, textAlign, lineHeight, letterSpacing, textDecoration),
        maxLines = maxLines,
        overflow = overflow,
        softWrap = softWrap,
        onTextLayout = onTextLayout
    )
}

/** Rich text (RN nested `<Text>` spans) — build the [AnnotatedString] with SpanStyle(fontFamily = ..., color = ...). */
@Composable
fun AppText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 18.sp,
    color: Color = FarimodColors.Dark,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign = TextAlign.Right,
    lineHeight: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Text(
        text = text,
        modifier = modifier,
        style = rnTextStyle(fontSize, color, fontFamily, fontWeight, null, textAlign, lineHeight, TextUnit.Unspecified, textDecoration),
        maxLines = maxLines,
        overflow = overflow,
        inlineContent = inlineContent,
        onTextLayout = onTextLayout
    )
}

@Composable
fun rnTextStyle(
    fontSize: TextUnit = 18.sp,
    color: Color = FarimodColors.Dark,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    textAlign: TextAlign = TextAlign.Right,
    lineHeight: TextUnit = TextUnit.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null
): TextStyle = TextStyle(
    color = color,
    fontSize = fontSize,
    fontFamily = fontFamily ?: RNFonts.iran_sans_medium,
    fontWeight = fontWeight,
    fontStyle = fontStyle,
    textAlign = textAlign,
    textDirection = TextDirection.Rtl,
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
    textDecoration = textDecoration,
    lineHeightStyle = if (lineHeight != TextUnit.Unspecified)
        LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None) else null
)

// ──────────────────────────────────────────────────────────────────────────────
// Layout helpers
// ──────────────────────────────────────────────────────────────────────────────

/**
 * RN `flexDirection: "row-reverse"` written for the Persian layout: the first child sits on the right.
 * Children stay RTL, same as the rest of the app.
 * Do not use `padding(start=/end=)` on the modifier — use `absolutePadding(left=, right=)`.
 */
@Composable
fun RowReverse(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(
            modifier = modifier,
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
            content = content
        )
    }
}

/** RN `TouchableOpacity` (activeOpacity 0.2). */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.touchableOpacity(
    enabled: Boolean = true,
    activeOpacity: Float = 0.2f,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    this
        .graphicsLayer { alpha = if (pressed && enabled) activeOpacity else 1f }
        .combinedClickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onLongClick = onLongClick,
            onClick = onClick
        )
}

/** RN `Pressable` / `TouchableWithoutFeedback` — click without any visual feedback. */
fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        enabled = enabled,
        onClick = onClick
    )
}

/** Shadows are disabled. The Persian UI stays flat; callers can keep the modifier. */
fun Modifier.rnShadow(
    elevation: Dp,
    shape: Shape = RectangleShape,
    color: Color = Color.Black
): Modifier = this

// ──────────────────────────────────────────────────────────────────────────────
// Safe area (react-native-safe-area-context / Constants.statusBarHeight / StatusBar.currentHeight)
// ──────────────────────────────────────────────────────────────────────────────
data class SafeAreaInsets(val top: Dp, val bottom: Dp, val left: Dp, val right: Dp)

/** true while rendering inside the tab navigator content (the tab bar already covers the bottom inset). */
val LocalBottomInsetConsumed = androidx.compose.runtime.compositionLocalOf { false }

@Composable
fun rememberSafeAreaInsets(): SafeAreaInsets {
    val ld = LocalLayoutDirection.current
    val status = WindowInsets.statusBars.asPaddingValues()
    val nav = WindowInsets.navigationBars.asPaddingValues()
    val bottomConsumed = LocalBottomInsetConsumed.current
    return SafeAreaInsets(
        top = status.calculateTopPadding(),
        bottom = if (bottomConsumed) 0.dp else nav.calculateBottomPadding(),
        left = nav.calculateLeftPadding(ld),
        right = nav.calculateRightPadding(ld)
    )
}

/** `Constants.statusBarHeight` / `StatusBar.currentHeight` */
@Composable
fun statusBarHeight(): Dp {
    val density = LocalDensity.current
    return with(density) { WindowInsets.statusBars.getTop(density).toDp() }
}

/** Keyboard height (0.dp when hidden). */
@Composable
fun keyboardHeight(): Dp = WindowInsets.ime.asPaddingValues().calculateBottomPadding()

/** Screen size in dp (React Native: `Dimensions.get("window")`). */
@Composable
fun windowSize(): Pair<Dp, Dp> {
    val info = androidx.compose.ui.platform.LocalWindowInfo.current
    val density = LocalDensity.current
    return with(density) { info.containerSize.width.toDp() to info.containerSize.height.toDp() }
}
