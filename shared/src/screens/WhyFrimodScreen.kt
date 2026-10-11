package ir.farimod.app.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.farimod.app.components.icons.MaterialCommunityIcons
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.linearGradient
import ir.farimod.app.ui.rememberSafeAreaInsets
import ir.farimod.app.ui.rgba
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.rnTextStyle
import ir.farimod.app.ui.rnTiming
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private data class WhyFeature(val icon: String, val title: String, val description: String)

private val whyFeatures = listOf(
    WhyFeature("account-tie", "طراحان حرفه‌ای", "معرفی بهترین طراحان پارچه و لباس"),
    WhyFeature("school", "اساتید و مدرسان", "دسترسی به اساتید برتر حوزه فشن"),
    WhyFeature("domain", "آکادمی مد و لباس", "آموزشگاه‌های تخصصی طراحی و دوخت"),
    WhyFeature("store", "برندها و مزون‌ها", "شبکه‌ای از معتبرترین برندها"),
    WhyFeature("camera-enhance", "اژانس‌های مدلینگ", "ارتباط با اژانس‌های حرفه‌ای"),
    WhyFeature("shopping", "فروشگاه‌ها", "دسترسی به فروشگاه‌های تخصصی"),
    WhyFeature("brush", "ابزارها و تجهیزات", "فروشندگان ابزارهای طراحی"),
)

private fun Modifier.enter(fade: Float, slideDp: Float): Modifier = graphicsLayer {
    alpha = fade
    translationY = slideDp * density
}

@Composable
fun WhyFrimodScreen() {
    val navigation = useNavigation()
    val insets = rememberSafeAreaInsets()
    val fade = remember { Animatable(0f) }
    val slide = remember { Animatable(50f) }
    LaunchedEffect(Unit) {
        coroutineScope {
            launch { fade.animateTo(1f, rnTiming(1000)) }
            launch { slide.animateTo(0f, rnTiming(800)) }
        }
    }
    val fadeV = fade.value
    val slideV = slide.value
    val cardShape = RoundedCornerShape(25.dp)

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .linearGradient(
                listOf(
                    Color(0xFF667EEA),
                    Color(0xFFC58BFF),
                    Color(0xFFC3B3E7),
                    Color(0xFFB2AFD8),
                )
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
                        .padding(bottom = 30.dp)
                        .enter(fadeV, slideV),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MaterialCommunityIcons("star-four-points", size = 30.dp, color = Color.White)
                        WhyTitle(
                            "چرا فریمد؟",
                            Modifier.absolutePadding(left = 15.dp, right = 15.dp),
                        )
                        MaterialCommunityIcons("star-four-points", size = 30.dp, color = Color.White)
                    }
                }

                Column(
                    Modifier
                        .padding(bottom = 30.dp)
                        .fillMaxWidth()
                        .enter(fadeV, slideV)
                        .rnShadow(20.dp, cardShape, Color.Black.copy(alpha = 0.3f))
                        .clip(cardShape)
                        .linearGradient(
                            listOf(rgba(255, 255, 255, 0.95f), rgba(255, 255, 255, 0.85f))
                        )
                        .padding(25.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .padding(bottom = 20.dp)
                            .size(80.dp)
                            .rnShadow(10.dp, CircleShape, Color.Black.copy(alpha = 0.2f))
                            .clip(CircleShape)
                            .linearGradient(
                                listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
                                shape = CircleShape,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        MaterialCommunityIcons("account-group", size = 40.dp, color = Color.White)
                    }
                    Text(
                        text = "فریمد اولین پلتفرم معرفی طراحان پارچه و لباس کشور، اساتید و مدرسان حوزه فشن، موسسات و آکادمی‌های آموزشی طراحی لباس و دوخت، برندها و مزون‌ها و اژانس‌های مدلینگ، فروشگاه‌های پارچه و لباس‌فروشی‌ها و خرازی‌ها و فروشندگان ابزارهای طراحی لباس است.",
                        modifier = Modifier.width(300.dp).padding(bottom = 25.dp),
                        style = rnTextStyle(
                            fontSize = 16.sp,
                            color = Color(0xFF333333),
                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                            textAlign = TextAlign.Center,
                            lineHeight = 26.sp,
                        ),
                    )
                    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                        whyFeatures.forEach { feature ->
                            WhyFeatureRow(feature)
                        }
                    }
                }

                Box(
                    Modifier.fillMaxWidth().enter(fadeV, slideV),
                    contentAlignment = Alignment.Center,
                ) {
                    val buttonShape = RoundedCornerShape(25.dp)
                    Box(
                        Modifier
                            .fillMaxWidth(0.8f)
                            .rnShadow(8.dp, buttonShape, Color(0xFF917BE0).copy(alpha = 0.3f))
                            .clip(buttonShape)
                            .touchableOpacity { navigation.navigate("CareerScreen") }
                    ) {
                        RowReverse(
                            Modifier
                                .fillMaxWidth()
                                .linearGradient(
                                    colors = listOf(Color(0xFF917BE0), Color(0xFF8F88E7)),
                                    start = Offset(1f, 1f),
                                    end = Offset(1f, 0f),
                                    locations = listOf(0f, 1f),
                                )
                                .absolutePadding(left = 30.dp, right = 30.dp, top = 16.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
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
                            MaterialCommunityIcons("arrow-left", size = 20.dp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WhyTitle(text: String, modifier: Modifier = Modifier) {
    val localDensity = LocalDensity.current
    Text(
        text = text,
        modifier = modifier,
        style = rnTextStyle(
            fontSize = 32.sp,
            color = Color.White,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            textAlign = TextAlign.Right,
        ).copy(
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.3f),
                offset = Offset(0f, with(localDensity) { 2.dp.toPx() }),
                blurRadius = with(localDensity) { 4.dp.toPx() },
            )
        ),
    )
}

@Composable
private fun WhyFeatureRow(feature: WhyFeature) {
    val shape = RoundedCornerShape(15.dp)
    RowReverse(
        Modifier
            .padding(bottom = 12.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(rgba(102, 126, 234, 0.1f), shape)
            .absolutePadding(left = 13.dp, right = 13.dp, top = 15.dp, bottom = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .absolutePadding(left = 13.dp)
                .size(50.dp)
                .background(rgba(102, 126, 234, 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            MaterialCommunityIcons(feature.icon, size = 24.dp, color = Color(0xFF667EEA))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text(
                text = feature.title,
                modifier = Modifier.padding(bottom = 4.dp),
                style = rnTextStyle(
                    fontSize = 16.sp,
                    color = Color(0xFF333333),
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    textAlign = TextAlign.Right,
                ),
            )
            Text(
                text = feature.description,
                style = rnTextStyle(
                    fontSize = 14.sp,
                    color = Color(0xFF666666),
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    textAlign = TextAlign.Right,
                    lineHeight = 20.sp,
                ),
            )
        }
    }
}
