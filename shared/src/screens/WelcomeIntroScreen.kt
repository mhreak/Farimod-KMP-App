package ir.farimod.app.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.Ionicons
import ir.farimod.app.components.icons.MaterialCommunityIcons
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.resources.Res
import ir.farimod.app.resources.main_icon
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.LocalImage
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.ResizeMode
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.linearGradient
import ir.farimod.app.ui.rememberSafeAreaInsets
import ir.farimod.app.ui.rgba
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.rnSpring
import ir.farimod.app.ui.rnTextStyle
import ir.farimod.app.ui.rnTiming
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.ui.windowSize
import kotlin.math.floor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * RN `responsiveFontSize`: scale by window width / 400, clamp to 0.8x–1.2x,
 * otherwise `PixelRatio.roundToNearestPixel` (JS `Math.round`).
 */
private fun welcomeLayoutSize(size: Float, widthDp: Float, pixelRatio: Float): Float {
    val newSize = size * (widthDp / 400f)
    return when {
        newSize < size * 0.8f -> size * 0.8f
        newSize > size * 1.2f -> size * 1.2f
        else -> floor(newSize * pixelRatio + 0.5f) / pixelRatio
    }
}

@Composable
private fun rsDp(size: Float, widthDp: Float): Dp {
    val ratio = LocalDensity.current.density
    return welcomeLayoutSize(size, widthDp, ratio).dp
}

/** Icon / layout dp. `allowFontScaling={false}` texts use [rsSpFixed] instead. */
@Composable
private fun rsSpFixed(size: Float, widthDp: Float): TextUnit {
    val d = LocalDensity.current
    val layout = welcomeLayoutSize(size, widthDp, d.density)
    return (layout / d.fontScale).sp
}

@Composable
private fun rsSp(size: Float, widthDp: Float): TextUnit {
    val d = LocalDensity.current
    return welcomeLayoutSize(size, widthDp, d.density).sp
}

private fun Modifier.enter(fade: Float, slideDp: Float, scale: Float = 1f): Modifier = graphicsLayer {
    alpha = fade
    translationY = slideDp * density
    scaleX = scale
    scaleY = scale
}

@Composable
fun WelcomeIntroScreen() {
    val navigation = useNavigation()
    val insets = rememberSafeAreaInsets()
    val (width, _) = windowSize()
    val widthDp = width.value

    val fade = remember { Animatable(0f) }
    val slide = remember { Animatable(100f) }
    val scale = remember { Animatable(0.5f) }
    val sparkle = remember { Animatable(0f) }
    val floatA = remember { Animatable(0f) }

    // Entrance, then the sparkle loop. Float starts immediately (separate Animated.loop).
    LaunchedEffect(Unit) {
        coroutineScope {
            launch {
                coroutineScope {
                    launch { fade.animateTo(1f, rnTiming(1500)) }
                    launch { slide.animateTo(0f, rnSpring(tension = 50f, friction = 8f)) }
                    launch { scale.animateTo(1f, rnSpring(tension = 60f, friction = 10f)) }
                }
                while (true) {
                    sparkle.animateTo(1f, rnTiming(2000))
                    sparkle.animateTo(0f, rnTiming(2000))
                }
            }
            launch {
                while (true) {
                    floatA.animateTo(1f, rnTiming(4000))
                    floatA.animateTo(0f, rnTiming(4000))
                }
            }
        }
    }

    val fadeV = fade.value
    val slideV = slide.value
    val scaleV = scale.value
    val sparkleV = sparkle.value
    val floatV = floatA.value
    val logoSize = (widthDp * 0.4f).coerceIn(120f, 150f).dp

    Box(
        Modifier
            .fillMaxSize()
            .linearGradient(
                colors = listOf(FarimodColors.Primary, FarimodColors.PrimaryLight, FarimodColors.Tertiary),
                locations = listOf(0f, 0.5f, 1f),
            )
    ) {
        WelcomeDecorations(widthDp, sparkleV, floatV)
        Column(
            Modifier
                .fillMaxSize()
                .zIndex(1f)
                .absolutePadding(left = 30.dp, right = 30.dp)
                .padding(top = statusBarHeight() + 60.dp, bottom = insets.bottom + 60.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.enter(fadeV, slideV, scaleV),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(logoSize), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .size(logoSize)
                            .rnShadow(15.dp, CircleShape, FarimodColors.Primary.copy(alpha = 0.3f))
                            .clip(CircleShape)
                            .border(2.dp, FarimodColors.White, CircleShape)
                            .linearGradient(
                                colors = listOf(FarimodColors.White, FarimodColors.PrimaryLight),
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        LocalImage(
                            Res.drawable.main_icon,
                            Modifier.size(logoSize * 0.65f).clip(RoundedCornerShape(100.dp)),
                            ResizeMode.contain,
                        )
                    }
                    WelcomeSparkle(Alignment.TopEnd, 10.dp, (-10).dp, sparkleV, rsSp(16f, widthDp))
                    WelcomeSparkle(Alignment.TopStart, (-15).dp, (-5).dp, sparkleV, rsSp(16f, widthDp))
                    WelcomeSparkle(Alignment.BottomEnd, 5.dp, 10.dp, sparkleV, rsSp(16f, widthDp))
                    WelcomeSparkle(Alignment.BottomStart, (-10).dp, 5.dp, sparkleV, rsSp(16f, widthDp))
                }
                WelcomeLabel(
                    text = "فریمد | Farimod",
                    fontSize = rsSpFixed(42f, widthDp),
                    color = FarimodColors.Primary,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    textAlign = TextAlign.Center,
                    shadowOffsetY = 2.dp,
                    shadowRadius = 4.dp,
                    shadowAlpha = 0.1f,
                    modifier = Modifier.padding(top = 25.dp),
                )
            }

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .absolutePadding(left = 10.dp, right = 10.dp)
                    .enter(fadeV, slideV),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                WelcomeLabel(
                    text = "به اپلیکیشن معرفی طراحان پارچه و لباس ایران",
                    fontSize = rsSpFixed(18f, widthDp),
                    color = FarimodColors.Dark,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    textAlign = TextAlign.Center,
                    lineHeight = rsSpFixed(28f, widthDp),
                )
                Row(
                    Modifier.absolutePadding(top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WelcomeLabel(
                        text = "فریمد",
                        fontSize = rsSpFixed(28f, widthDp),
                        color = FarimodColors.Primary,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        textAlign = TextAlign.Center,
                        shadowOffsetY = 1.dp,
                        shadowRadius = 2.dp,
                        shadowAlpha = 0.1f,
                    )
                    MaterialCommunityIcons(
                        name = "heart",
                        size = rsDp(20f, widthDp),
                        color = FarimodColors.Primary,
                        modifier = Modifier.absolutePadding(left = 8.dp, top = 2.dp),
                    )
                }
                WelcomeLabel(
                    text = "خوش آمدید!",
                    fontSize = rsSpFixed(18f, widthDp),
                    color = FarimodColors.Dark,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    textAlign = TextAlign.Center,
                    lineHeight = rsSpFixed(28f, widthDp),
                )
                Row(
                    Modifier.absolutePadding(top = 25.dp, bottom = 25.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .absolutePadding(left = 10.dp, right = 10.dp)
                            .width(40.dp)
                            .height(1.dp)
                            .alpha(0.3f)
                            .background(FarimodColors.Primary)
                    )
                    Ionicons(
                        name = "sparkles",
                        size = rsDp(30f, widthDp),
                        color = FarimodColors.Primary,
                    )
                    Box(
                        Modifier
                            .absolutePadding(left = 10.dp, right = 10.dp)
                            .width(40.dp)
                            .height(1.dp)
                            .alpha(0.3f)
                            .background(FarimodColors.Primary)
                    )
                }
                WelcomeLabel(
                    text = "به خانواده بزرگ طراحان پارچه و لباس ایران بپیوندید.",
                    fontSize = rsSpFixed(16f, widthDp),
                    color = FarimodColors.Medium,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    textAlign = TextAlign.Center,
                    lineHeight = rsSpFixed(24f, widthDp),
                )
                Row(
                    Modifier.padding(top = 20.dp).width(120.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {}
            }

            Box(
                Modifier.fillMaxWidth().enter(fadeV, slideV),
                contentAlignment = Alignment.Center,
            ) {
                val shape = RoundedCornerShape(25.dp)
                Box(
                    Modifier
                        .fillMaxWidth(0.8f)
                        .clip(shape)
                        .touchableOpacity { navigation.navigate("WhyFrimod") }
                ) {
                    RowReverse(
                        Modifier
                            .fillMaxWidth()
                            .linearGradient(
                                colors = listOf(FarimodColors.Primary, FarimodColors.PrimaryDark),
                                start = androidx.compose.ui.geometry.Offset(1f, 1f),
                                end = androidx.compose.ui.geometry.Offset(1f, 0f),
                                locations = listOf(0f, 1f),
                            )
                            .absolutePadding(left = 30.dp, right = 30.dp, top = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        WelcomeLabel(
                            text = "ادامه",
                            fontSize = rsSpFixed(18f, widthDp),
                            color = Color.White,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.absolutePadding(left = 10.dp),
                        )
                        MaterialCommunityIcons(
                            name = "arrow-left",
                            size = rsDp(20f, widthDp),
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.WelcomeDecorations(widthDp: Float, sparkle: Float, floatV: Float) {
    Box(Modifier.fillMaxSize().zIndex(0f)) {
        FabricMark("brush", rsDp(30f, widthDp), rgba(255, 255, 255, 0.7f), Alignment.TopEnd, (-30).dp, 120.dp, sparkle, sparkle * 360f, 1f, 0f, floatV * -10f)
        FabricMark("palette", rsDp(25f, widthDp), rgba(255, 255, 255, 0.6f), Alignment.TopStart, 40.dp, 200.dp, sparkle, 0f, sparkle, floatV * 15f, 0f)
        FabricMark("tshirt-crew", rsDp(35f, widthDp), rgba(255, 255, 255, 0.5f), Alignment.BottomEnd, (-50).dp, (-150).dp, sparkle, (1f - sparkle) * 360f, 1f, 0f, floatV * 12f)
        FabricMark("scissors-cutting", rsDp(28f, widthDp), rgba(255, 255, 255, 0.65f), Alignment.TopStart, 20.dp, 160.dp, sparkle, sparkle * 180f, 1f, floatV * -8f, 0f)
        FabricMark("tshirt-v", rsDp(32f, widthDp), rgba(255, 255, 255, 0.55f), Alignment.BottomStart, 30.dp, (-250).dp, sparkle, 0f, sparkle, 0f, floatV * -15f)
        FabricMark("pin", rsDp(26f, widthDp), rgba(255, 255, 255, 0.7f), Alignment.TopEnd, (-25).dp, 280.dp, sparkle, 180f + sparkle * 180f, 1f, floatV * 10f, 0f)
        FabricMark("hanger", rsDp(30f, widthDp), rgba(255, 255, 255, 0.6f), Alignment.BottomEnd, (-35).dp, (-320).dp, sparkle, 0f, sparkle, 0f, floatV * 8f)
        FabricMark("tape-measure", rsDp(27f, widthDp), rgba(255, 255, 255, 0.5f), Alignment.BottomStart, 55.dp, (-180).dp, sparkle, 0f, sparkle, 0f, floatV * -6f)
    }
}

@Composable
private fun BoxScope.FabricMark(
    name: String,
    size: Dp,
    color: Color,
    align: Alignment,
    x: Dp,
    y: Dp,
    sparkle: Float,
    rotation: Float,
    scale: Float,
    dx: Float,
    dy: Float,
) {
    MaterialCommunityIcons(
        name = name,
        size = size,
        color = color,
        modifier = Modifier
            .align(align)
            .absoluteOffset(x = x, y = y)
            .graphicsLayer {
                alpha = sparkle
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
                translationX = dx * density
                translationY = dy * density
            },
    )
}

@Composable
private fun BoxScope.WelcomeSparkle(align: Alignment, x: Dp, y: Dp, sparkle: Float, fontSize: TextUnit) {
    Text(
        text = "✨",
        modifier = Modifier
            .align(align)
            .absoluteOffset(x = x, y = y)
            .graphicsLayer {
                alpha = sparkle
                scaleX = sparkle
                scaleY = sparkle
            },
        style = androidx.compose.ui.text.TextStyle(
            color = Color.Black,
            fontSize = fontSize,
            fontFamily = FontFamily.Default,
            textAlign = TextAlign.Right,
        ),
    )
}

@Composable
private fun WelcomeLabel(
    text: String,
    fontSize: TextUnit,
    color: Color,
    fontFamily: FontFamily,
    textAlign: TextAlign,
    modifier: Modifier = Modifier,
    lineHeight: TextUnit = TextUnit.Unspecified,
    shadowOffsetY: Dp = 0.dp,
    shadowRadius: Dp = 0.dp,
    shadowAlpha: Float = 0f,
) {
    val localDensity = LocalDensity.current
    val shadow = if (shadowAlpha > 0f) {
        Shadow(
            color = Color.Black.copy(alpha = shadowAlpha),
            offset = androidx.compose.ui.geometry.Offset(0f, with(localDensity) { shadowOffsetY.toPx() }),
            blurRadius = with(localDensity) { shadowRadius.toPx() },
        )
    } else {
        null
    }
    Text(
        text = text,
        modifier = modifier,
        style = rnTextStyle(
            fontSize = fontSize,
            color = color,
            fontFamily = fontFamily,
            textAlign = textAlign,
            lineHeight = lineHeight,
        ).copy(shadow = shadow),
    )
}
