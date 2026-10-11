package ir.farimod.app.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.alpha
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
import ir.farimod.app.platform.canOpenUrl
import ir.farimod.app.platform.openUrl
import ir.farimod.app.state.AuthState
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.RNAlert
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
import kotlinx.coroutines.launch

private data class EventRow(
    val icon: String,
    val title: String,
    val description: String,
    val color: Color,
)

private fun Modifier.enter(fade: Float, slideDp: Float, scale: Float = 1f): Modifier = graphicsLayer {
    alpha = fade
    translationY = slideDp * density
    scaleX = scale
    scaleY = scale
}

@Composable
fun EventsScreen() {
    val navigation = useNavigation()
    val insets = rememberSafeAreaInsets()
    val fade = remember { Animatable(0f) }
    val slide = remember { Animatable(50f) }
    val scale = remember { Animatable(0.9f) }
    val pulse = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        coroutineScope {
            launch { fade.animateTo(1f, rnTiming(1200)) }
            launch { slide.animateTo(0f, rnTiming(1000)) }
            launch { scale.animateTo(1f, rnSpring(tension = 100f, friction = 8f)) }
            launch {
                while (true) {
                    pulse.animateTo(1.05f, rnTiming(1500))
                    pulse.animateTo(1f, rnTiming(1500))
                }
            }
        }
    }

    val fadeV = fade.value
    val slideV = slide.value
    val scaleV = scale.value
    val pulseV = pulse.value

    val events = listOf(
        EventRow("calendar-clock", "ایونت‌ها و رویدادها", "آخرین اخبار فشن شوها و نمایشگاه‌ها", FarimodColors.Primary),
        EventRow("school", "دوره‌ها و ورکشاپ‌ها", "آموزش‌های تخصصی طراحی و دوخت", FarimodColors.Secondary),
        EventRow("palette", "طراحی پارچه", "دوره‌های طراحی و چاپ پارچه", FarimodColors.Danger),
        EventRow("book-open-variant", "کتاب‌های مد و فشن", "جدیدترین منابع علمی و آموزشی", FarimodColors.PrimaryDark),
        EventRow("trending-up", "ترندهای مد", "آخرین ترندهای جهانی مد و لباس", FarimodColors.Info),
        EventRow("certificate", "گواهینامه‌ها", "مدارک معتبر بین‌المللی", FarimodColors.Success),
    )

    fun openTutorial() {
        val tutorialUrl = "https://farimod.ir/app-use"
        try {
            if (canOpenUrl(tutorialUrl)) {
                val opened = openUrl(tutorialUrl)
                if (!opened) RNAlert.alert("خطا", "مشکلی در باز کردن لینک پیش آمد")
            } else {
                RNAlert.alert("خطا", "امکان باز کردن لینک وجود ندارد")
            }
        } catch (_: Exception) {
            RNAlert.alert("خطا", "مشکلی در باز کردن لینک پیش آمد")
        }
    }

    // RN resets the root to the Auth navigator. The KMP stack is flat: Auth's initial
    // route is Login, and params.screen = Signup is the Signup route.
    fun finishIntro(signup: Boolean) {
        runCatching { AuthState.completeIntro() }
        try {
            navigation.reset(if (signup) "Signup" else "Login")
        } catch (_: Exception) {
        }
    }

    val cardShape = RoundedCornerShape(30.dp)

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .linearGradient(
                colors = listOf(
                    Color(0xFFFF6B9D),
                    Color(0xFFFFB5D1),
                    Color(0xFFFFF8FC),
                    Color(0xFFFFE0EC),
                    Color(0xFFFF6B9D),
                ),
                locations = listOf(0f, 0.3f, 0.6f, 0.8f, 1f),
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
                    .padding(top = statusBarHeight() + 50.dp)
                    .absolutePadding(left = 20.dp, right = 20.dp)
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 35.dp)
                        .enter(fadeV, slideV, scaleV),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        Modifier.padding(bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MaterialCommunityIcons("star-circle", size = 32.dp, color = Color.White)
                        EventsShadowText(
                            text = "رویدادها و دوره‌ها",
                            fontSize = 22.sp,
                            color = Color.White,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            textAlign = TextAlign.Right,
                            shadowAlpha = 0.3f,
                            shadowOffsetY = 2.dp,
                            shadowBlur = 6.dp,
                            modifier = Modifier.absolutePadding(left = 15.dp, right = 15.dp),
                        )
                        MaterialCommunityIcons("star-circle", size = 32.dp, color = Color.White)
                    }
                    EventsShadowText(
                        text = "دنیای مد و فشن را کشف کنید",
                        fontSize = 16.sp,
                        color = rgba(255, 255, 255, 0.9f),
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        textAlign = TextAlign.Center,
                        shadowAlpha = 0.2f,
                        shadowOffsetY = 1.dp,
                        shadowBlur = 3.dp,
                    )
                }

                Box(
                    Modifier
                        .absolutePadding(left = 5.dp, right = 5.dp)
                        .padding(bottom = 35.dp)
                        .fillMaxWidth()
                        .enter(fadeV, slideV, scaleV)
                        .rnShadow(25.dp, cardShape, FarimodColors.Primary.copy(alpha = 0.25f))
                        .clip(cardShape)
                ) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .linearGradient(
                                listOf(rgba(255, 255, 255, 0.98f), rgba(255, 255, 255, 0.9f))
                            )
                    )
                    Column(Modifier.fillMaxWidth().padding(30.dp)) {
                        Box(
                            Modifier.fillMaxWidth().padding(bottom = 25.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(Modifier.size(90.dp)) {
                                Box(
                                    Modifier
                                        .align(Alignment.TopCenter)
                                        .absoluteOffset(y = (-15).dp)
                                        .size(120.dp)
                                        .alpha(0.1f)
                                        .background(FarimodColors.Primary, CircleShape)
                                )
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .rnShadow(15.dp, CircleShape, FarimodColors.Primary.copy(alpha = 0.3f))
                                        .clip(CircleShape)
                                        .linearGradient(
                                            listOf(FarimodColors.Primary, FarimodColors.Secondary),
                                            shape = CircleShape,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    MaterialCommunityIcons("calendar-heart", size = 42.dp, color = Color.White)
                                }
                            }
                        }
                        Text(
                            text = "اینجا از جدیدترین ایونت‌ها، رویدادها، دوره‌ها و ورکشاپ‌های مد و لباس، طراحی پارچه و کتاب‌های مفید فشن مطلع می‌شوید",
                            modifier = Modifier.padding(bottom = 30.dp).absolutePadding(left = 10.dp, right = 10.dp),
                            style = rnTextStyle(
                                fontSize = 17.sp,
                                color = Color(0xFF2C2C2C),
                                fontFamily = RNFonts.Yekan_Bakh_Regular,
                                textAlign = TextAlign.Center,
                                lineHeight = 28.sp,
                            ),
                        )
                        Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                            events.forEach { row -> EventItem(row) }
                        }
                    }
                    Box(Modifier.matchParentSize()) {
                        MaterialCommunityIcons(
                            "star-four-points",
                            size = 16.dp,
                            color = FarimodColors.Primary,
                            modifier = Modifier.align(AbsoluteAlignment.TopRight).absoluteOffset(x = (-80).dp, y = 30.dp),
                        )
                        MaterialCommunityIcons(
                            "star",
                            size = 14.dp,
                            color = FarimodColors.Secondary,
                            modifier = Modifier.align(AbsoluteAlignment.TopLeft).absoluteOffset(x = 30.dp, y = 120.dp),
                        )
                        MaterialCommunityIcons(
                            "heart",
                            size = 12.dp,
                            color = FarimodColors.Danger,
                            modifier = Modifier.align(AbsoluteAlignment.TopRight).absoluteOffset(x = (-50).dp, y = 90.dp),
                        )
                    }
                }

                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 25.dp)
                        .enter(fadeV, slideV, pulseV),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val primaryShape = RoundedCornerShape(30.dp)
                    Box(
                        Modifier
                            .fillMaxWidth(0.92f)
                            .rnShadow(20.dp, primaryShape, Color(0xFFE91E63).copy(alpha = 0.4f))
                            .clip(primaryShape)
                            .touchableOpacity(activeOpacity = 0.8f) { finishIntro(signup = true) }
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .linearGradient(
                                    colors = listOf(Color(0xFFE91E63), Color(0xFFAD1457)),
                                    start = Offset(0f, 0f),
                                    end = Offset(1f, 0f),
                                )
                                .absolutePadding(left = 35.dp, right = 35.dp, top = 18.dp, bottom = 18.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MaterialCommunityIcons("rocket-launch", size = 22.dp, color = Color.White)
                            Text(
                                text = "ثبت نام در فریمد",
                                modifier = Modifier.absolutePadding(left = 12.dp),
                                style = rnTextStyle(
                                    fontSize = 19.sp,
                                    color = Color.White,
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    textAlign = TextAlign.Right,
                                ),
                            )
                        }
                    }

                    val secondaryShape = RoundedCornerShape(25.dp)
                    Box(
                        Modifier
                            .padding(top = 18.dp)
                            .fillMaxWidth(0.7f)
                            .clip(secondaryShape)
                            .border(2.dp, rgba(255, 255, 255, 0.6f), secondaryShape)
                            .touchableOpacity { finishIntro(signup = false) }
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .linearGradient(
                                    listOf(rgba(255, 255, 255, 0.2f), rgba(255, 255, 255, 0.1f))
                                )
                                .absolutePadding(left = 25.dp, right = 25.dp, top = 12.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MaterialCommunityIcons(
                                "login",
                                size = 20.dp,
                                color = FarimodColors.Primary,
                                modifier = Modifier.absolutePadding(right = 8.dp),
                            )
                            Text(
                                text = "ورود به حساب کاربری",
                                style = rnTextStyle(
                                    fontSize = 15.sp,
                                    color = FarimodColors.Primary,
                                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                                    textAlign = TextAlign.Center,
                                ),
                            )
                        }
                    }

                    Column(
                        Modifier
                            .padding(top = 20.dp, bottom = 25.dp)
                            .enter(fadeV, slideV),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val tutorialShape = RoundedCornerShape(25.dp)
                        RowReverse(
                            Modifier
                                .clip(tutorialShape)
                                .border(1.dp, rgba(255, 255, 255, 0.5f), tutorialShape)
                                .background(rgba(255, 255, 255, 0.3f), tutorialShape)
                                .touchableOpacity(activeOpacity = 0.7f) { openTutorial() }
                                .absolutePadding(left = 20.dp, right = 20.dp, top = 12.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MaterialCommunityIcons("play-circle-outline", size = 18.dp, color = FarimodColors.Primary)
                            Text(
                                text = "آموزش استفاده از فریمد",
                                style = rnTextStyle(
                                    fontSize = 14.sp,
                                    color = FarimodColors.Primary,
                                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                                    textAlign = TextAlign.Center,
                                ),
                            )
                            MaterialCommunityIcons("chevron-left", size = 16.dp, color = FarimodColors.Primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventItem(row: EventRow) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier
            .padding(bottom = 15.dp)
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, rgba(255, 255, 255, 0.5f), shape)
            .background(Color(0xFFF9EDF5), shape)
            .absolutePadding(left = 22.dp, right = 22.dp, top = 18.dp, bottom = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            Text(
                text = row.title,
                modifier = Modifier.padding(bottom = 6.dp),
                style = rnTextStyle(
                    fontSize = 17.sp,
                    color = Color(0xFF1A1A1A),
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    textAlign = TextAlign.Right,
                ),
            )
            Text(
                text = row.description,
                style = rnTextStyle(
                    fontSize = 14.sp,
                    color = Color(0xFF555555),
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    textAlign = TextAlign.Right,
                    lineHeight = 22.sp,
                ),
            )
        }
        Box(
            Modifier
                .absolutePadding(left = 18.dp)
                .size(55.dp)
                .background(row.color.copy(alpha = 0x15 / 255f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            MaterialCommunityIcons(row.icon, size = 26.dp, color = row.color)
        }
    }
}

@Composable
private fun EventsShadowText(
    text: String,
    fontSize: TextUnit,
    color: Color,
    fontFamily: FontFamily,
    textAlign: TextAlign,
    shadowAlpha: Float,
    shadowOffsetY: Dp,
    shadowBlur: Dp,
    modifier: Modifier = Modifier,
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
        ).copy(
            shadow = Shadow(
                color = Color.Black.copy(alpha = shadowAlpha),
                offset = Offset(0f, with(localDensity) { shadowOffsetY.toPx() }),
                blurRadius = with(localDensity) { shadowBlur.toPx() },
            )
        ),
    )
}
