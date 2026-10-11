package ir.farimod.app.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
import ir.farimod.app.components.icons.MaterialCommunityIcons
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.RNFonts
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class CareerRow(
    val icon: String,
    val title: String,
    val description: String,
    val color: Color,
    val gradient: List<Color>,
)

private fun Modifier.enter(fade: Float, slideDp: Float, scale: Float = 1f): Modifier = graphicsLayer {
    alpha = fade
    translationY = slideDp * density
    scaleX = scale
    scaleY = scale
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CareerScreen() {
    val navigation = useNavigation()
    val insets = rememberSafeAreaInsets()
    val fade = remember { Animatable(0f) }
    val slide = remember { Animatable(50f) }
    val scale = remember { Animatable(0.9f) }

    // pulseAnim is driven in the same loop but never bound to a view. Its 1500+1500ms
    // timings still delay the scale spring (0.9 → 1).
    LaunchedEffect(Unit) {
        coroutineScope {
            launch { fade.animateTo(1f, rnTiming(1000)) }
            launch { slide.animateTo(0f, rnTiming(800)) }
            launch {
                while (true) {
                    delay(1500)
                    delay(1500)
                    scale.animateTo(1f, rnSpring(tension = 100f, friction = 8f))
                }
            }
        }
    }

    val fadeV = fade.value
    val slideV = slide.value
    val scaleV = scale.value
    val cardShape = RoundedCornerShape(35.dp)

    val rows = listOf(
        CareerRow("file-document-outline", "بارگذاری رزومه", "آپلود رزومه و مدارک شما", Color(0xFF4CAF50), listOf(Color(0xFF4CAF50), Color(0xFF2E7D32))),
        CareerRow("briefcase-outline", "پورتفولیو", "نمایش نمونه کارهای شما", Color(0xFFFF9800), listOf(Color(0xFFFF9800), Color(0xFFE65100))),
        CareerRow("account-group", "اتصال به مزون‌ها", "ارتباط مستقیم با کارفرمایان", Color(0xFF2196F3), listOf(Color(0xFF2196F3), Color(0xFF0D47A1))),
        CareerRow("factory", "کارخانجات تولیدی", "همکاری با تولیدکنندگان", Color(0xFF9C27B0), listOf(Color(0xFF9C27B0), Color(0xFF4A148C))),
        CareerRow("chart-line", "رشد حرفه‌ای", "توسعه مهارت‌ها و شبکه کاری", Color(0xFF00BCD4), listOf(Color(0xFF00BCD4), Color(0xFF006064))),
    )

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .linearGradient(
                colors = listOf(
                    FarimodColors.Primary,
                    FarimodColors.PrimaryDark,
                    Color(0xFF4A148C),
                    FarimodColors.Tertiary,
                ),
                start = Offset(0f, 0f),
                end = Offset(1f, 1f),
            )
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(bottom = insets.bottom + 20.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = statusBarHeight() + 40.dp, bottom = 40.dp)
                    .absolutePadding(left = 20.dp, right = 20.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 35.dp)
                        .enter(fadeV, slideV),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalArrangement = Arrangement.Center,
                        itemVerticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(50.dp)
                                .rnShadow(10.dp, CircleShape, Color.Black.copy(alpha = 0.3f))
                                .clip(CircleShape)
                                .linearGradient(
                                    listOf(Color(0xFFFFD700), Color(0xFFFFA500)),
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            MaterialCommunityIcons("briefcase-variant", size = 30.dp, color = Color.White)
                        }
                        CareerShadowText(
                            text = "پروژه‌گیری و کاریابی فشن",
                            fontSize = 20.sp,
                            color = Color.White,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            textAlign = TextAlign.Center,
                            shadowAlpha = 0.4f,
                            shadowOffsetY = 3.dp,
                            shadowBlur = 6.dp,
                            modifier = Modifier.absolutePadding(left = 15.dp, right = 15.dp),
                        )
                    }
                }

                Column(
                    Modifier
                        .padding(bottom = 35.dp)
                        .fillMaxWidth()
                        .enter(fadeV, slideV)
                        .clip(cardShape)
                        .border(1.dp, rgba(255, 255, 255, 0.3f), cardShape)
                        .linearGradient(
                            colors = listOf(rgba(255, 255, 255, 0.15f), rgba(255, 255, 255, 0.08f)),
                            start = Offset(0f, 0f),
                            end = Offset(1f, 1f),
                        )
                        .padding(30.dp)
                ) {
                    Box(Modifier.fillMaxWidth().padding(bottom = 25.dp)) {
                        Box(
                            Modifier
                                .align(Alignment.Center)
                                .size(90.dp)
                                .rnShadow(15.dp, CircleShape, Color.Black.copy(alpha = 0.3f))
                                .clip(CircleShape)
                                .linearGradient(
                                    listOf(Color(0xFFE91E63), Color(0xFFAD1457)),
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            MaterialCommunityIcons("handshake", size = 45.dp, color = Color.White)
                        }
                        Box(
                            Modifier
                                .align(AbsoluteAlignment.TopRight)
                                .absoluteOffset(x = 10.dp, y = (-10).dp)
                                .size(0.dp)
                        ) {
                            MaterialCommunityIcons(
                                "star-four-points",
                                size = 16.dp,
                                color = Color(0xFFFFD700),
                                modifier = Modifier.align(AbsoluteAlignment.TopRight),
                            )
                            MaterialCommunityIcons(
                                "star-four-points",
                                size = 12.dp,
                                color = Color(0xFFFF6B6B),
                                modifier = Modifier.align(AbsoluteAlignment.TopRight).absoluteOffset(x = (-25).dp, y = 20.dp),
                            )
                        }
                    }

                    CareerShadowText(
                        text = "طراحان لباس در این پلتفرم با ارائه و بارگذاری مدارک، رزومه، پورتفولیو و نمونه کار خود می‌توانند با تولیدکنندگان و مزون‌ها و کارخانجات لینک شوند و جذب پروژه و کاریابی داشته باشند.",
                        fontSize = 17.sp,
                        color = Color.White,
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp,
                        shadowAlpha = 0.3f,
                        shadowOffsetY = 1.dp,
                        shadowBlur = 3.dp,
                        modifier = Modifier.padding(bottom = 30.dp).absolutePadding(left = 5.dp, right = 5.dp),
                    )

                    Column(Modifier.fillMaxWidth().padding(top = 15.dp)) {
                        rows.forEach { CareerFeatureRow(it) }
                    }

                    val linkShape = RoundedCornerShape(22.dp)
                    RowReverse(
                        Modifier
                            .padding(top = 25.dp)
                            .fillMaxWidth()
                            .clip(linkShape)
                            .border(1.dp, rgba(255, 255, 255, 0.2f), linkShape)
                            .background(rgba(63, 81, 181, 0.2f), linkShape)
                            .padding(22.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .absolutePadding(left = 15.dp)
                                .size(45.dp)
                                .clip(CircleShape)
                                .linearGradient(
                                    listOf(Color(0xFF3F51B5), Color(0xFF1A237E)),
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            MaterialCommunityIcons("link-variant", size = 24.dp, color = Color.White)
                        }
                        CareerShadowText(
                            text = "لینک سایت‌های کاریابی مد و فشن را از ما بخواهید",
                            fontSize = 15.sp,
                            color = Color.White,
                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                            textAlign = TextAlign.Right,
                            lineHeight = 22.sp,
                            shadowAlpha = 0.3f,
                            shadowOffsetY = 1.dp,
                            shadowBlur = 2.dp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 25.dp)
                        .enter(fadeV, slideV, scaleV),
                    contentAlignment = Alignment.Center,
                ) {
                    val buttonShape = RoundedCornerShape(25.dp)
                    Box(
                        Modifier
                            .fillMaxWidth(0.7f)
                            .rnShadow(8.dp, buttonShape, FarimodColors.Primary.copy(alpha = 0.3f))
                            .clip(buttonShape)
                            .touchableOpacity(activeOpacity = 0.8f) { navigation.navigate("EventsScreen") }
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .linearGradient(
                                    colors = listOf(FarimodColors.Primary, FarimodColors.Secondary),
                                    start = Offset(0f, 0f),
                                    end = Offset(1f, 0f),
                                )
                                .absolutePadding(left = 30.dp, right = 30.dp, top = 16.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MaterialCommunityIcons("arrow-left", size = 20.dp, color = Color.White)
                            Text(
                                text = "ادامه",
                                modifier = Modifier.absolutePadding(left = 10.dp),
                                style = rnTextStyle(
                                    fontSize = 18.sp,
                                    color = Color.White,
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    textAlign = TextAlign.Right,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CareerFeatureRow(row: CareerRow) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier
            .padding(bottom = 14.dp)
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, rgba(255, 255, 255, 0.2f), shape)
            .background(rgba(255, 255, 255, 0.15f), shape)
    ) {
        RowReverse(
            Modifier.fillMaxWidth().absolutePadding(left = 22.dp, right = 22.dp, top = 18.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .absolutePadding(left = 18.dp)
                    .size(50.dp)
                    .rnShadow(5.dp, CircleShape, Color.Black.copy(alpha = 0.2f))
                    .clip(CircleShape)
                    .linearGradient(row.gradient, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                MaterialCommunityIcons(row.icon, size = 24.dp, color = Color.White)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                CareerShadowText(
                    text = row.title,
                    fontSize = 16.sp,
                    color = Color.White,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    textAlign = TextAlign.Right,
                    shadowAlpha = 0.3f,
                    shadowOffsetY = 1.dp,
                    shadowBlur = 2.dp,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                CareerShadowText(
                    text = row.description,
                    fontSize = 14.sp,
                    color = rgba(255, 255, 255, 0.9f),
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    textAlign = TextAlign.Right,
                    lineHeight = 20.sp,
                    shadowAlpha = 0.2f,
                    shadowOffsetY = 1.dp,
                    shadowBlur = 2.dp,
                )
            }
        }
        Box(Modifier.matchParentSize()) {
            Box(
                Modifier
                    .align(AbsoluteAlignment.CenterRight)
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(
                        row.color.copy(alpha = 0x20 / 255f),
                        RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp),
                    )
            )
        }
    }
}

@Composable
private fun CareerShadowText(
    text: String,
    fontSize: TextUnit,
    color: Color,
    fontFamily: FontFamily,
    textAlign: TextAlign,
    shadowAlpha: Float,
    shadowOffsetY: Dp,
    shadowBlur: Dp,
    modifier: Modifier = Modifier,
    lineHeight: TextUnit = TextUnit.Unspecified,
) {
    val localDensity = LocalDensity.current
    Text(
        text = text,
        modifier = modifier,
        style = rnTextStyle(
            fontSize = fontSize,
            color = color,
            fontFamily = fontFamily,
            textAlign = textAlign,
            lineHeight = lineHeight,
        ).copy(
            shadow = Shadow(
                color = Color.Black.copy(alpha = shadowAlpha),
                offset = Offset(0f, with(localDensity) { shadowOffsetY.toPx() }),
                blurRadius = with(localDensity) { shadowBlur.toPx() },
            )
        ),
    )
}
