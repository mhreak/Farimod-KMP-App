package ir.farimod.app.screens.userprofile

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.network.Api
import ir.farimod.app.network.ApiNetworkException
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.str
import ir.farimod.app.platform.NativeVideoPlayer
import ir.farimod.app.state.AuthState
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.rnTextStyle
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.utils.toPersianDigits
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlin.math.ceil

internal fun upContactColor(type: String): Color = when (type) {
    "email" -> UpInfo
    "phone" -> UpSuccess
    "whatsapp" -> Color(0xFF25D366)
    "instagram" -> Color(0xFFE4405F)
    "location" -> UpWarning
    else -> UpMedium
}

internal fun upContactLabel(type: String): String = when (type) {
    "email" -> "ایمیل"
    "phone" -> "تلفن"
    "whatsapp" -> "واتساپ"
    "telegram" -> "تلگرام"
    "mobile" -> "موبایل"
    "instagram" -> "اینستاگرام"
    "location" -> "آدرس"
    "website" -> "وبسایت"
    else -> ""
}

@Composable
internal fun UpContactItem(icon: String, text: String, type: String, onPress: () -> Unit) {
    val iconColor = upContactColor(type)
    RowReverse(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .heightIn(min = 70.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.8f))
            .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .touchableOpacity(activeOpacity = 0.7f, onClick = onPress)
            .absolutePadding(left = 18.dp, right = 18.dp, top = 20.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .absolutePadding(left = 15.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(upHex(upContactColorHex(type) + "20"))
                .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MaterialIcons(name = icon, size = 20.dp, color = iconColor)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            AppText(
                text = upContactLabel(type),
                fontSize = 12.sp,
                fontFamily = RNFonts.Yekan_Bakh_Regular,
                color = UpMedium,
                modifier = Modifier.padding(bottom = 2.dp)
            )
            AppText(
                text = text,
                fontSize = 15.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = UpDark,
                textAlign = TextAlign.Right
            )
        }
        Box(Modifier.absolutePadding(right = 10.dp).alpha(0.6f)) {
            MaterialIcons(name = "chevron-left", size = 20.dp, color = UpMedium)
        }
    }
}

private fun upContactColorHex(type: String): String = when (type) {
    "email" -> UpHex.info
    "phone" -> UpHex.success
    "whatsapp" -> "#25D366"
    "instagram" -> "#E4405F"
    "location" -> UpHex.warning
    else -> UpHex.medium
}

@Composable
internal fun UpSectionHeader(title: String, icon: String, color: Color, hasData: Boolean, onSeeAll: () -> Unit) {
    RowReverse(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .padding(top = 10.dp, bottom = 20.dp)
            .absolutePadding(left = 5.dp, right = 5.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowReverse(verticalAlignment = Alignment.CenterVertically) {
            MaterialIcons(name = icon, size = 20.dp, color = color)
            AppText(
                text = title,
                fontSize = 18.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = UpDark,
                modifier = Modifier.absolutePadding(right = 8.dp)
            )
        }
        if (hasData) {
            Row(
                Modifier.touchableOpacity { onSeeAll() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText(
                    text = "نمایش بیشتر",
                    fontSize = 14.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    color = UpPrimary,
                    textAlign = TextAlign.Right
                )
                MaterialIcons(
                    name = "chevron-left",
                    size = 20.dp,
                    color = UpPrimary,
                    modifier = Modifier.absolutePadding(right = 2.dp)
                )
            }
        }
    }
}

@Composable
internal fun UpExpandableText(text: String, maxLines: Int = 3) {
    var expanded by remember(text) { mutableStateOf(false) }
    val charsPerLine = 40
    val showMore = ceil(text.length / charsPerLine.toFloat()) > maxLines
    val shown = if (!expanded && showMore) truncateAtWord(text, charsPerLine * maxLines) else text
    val bioStyle = rnTextStyle(
        fontSize = 16.sp,
        color = UpDark,
        fontFamily = RNFonts.Yekan_Bakh_Regular,
        textAlign = TextAlign.Justify,
        lineHeight = 28.sp,
        letterSpacing = 0.2.sp
    ).copy(textDirection = TextDirection.Rtl)
    Column(Modifier.fillMaxWidth().zIndex(1f)) {
        val body = if (!expanded && showMore) {
            buildAnnotatedString {
                append(shown)
                withStyle(SpanStyle(color = UpMedium, fontFamily = RNFonts.Yekan_Bakh_Regular, fontSize = 16.sp)) {
                    append("...")
                }
            }
        } else null
        if (body != null) {
            androidx.compose.material3.Text(
                text = body,
                modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 15.dp),
                style = bioStyle
            )
        } else {
            androidx.compose.material3.Text(
                text = shown,
                modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 15.dp),
                style = bioStyle
            )
        }
        if (showMore) {
            val label = if (expanded) "نمایش کمتر" else "نمایش بیشتر"
            val icon = if (expanded) "keyboard-arrow-up" else "keyboard-arrow-down"
            RowReverse(
                Modifier
                    .align(Alignment.Start)
                    .padding(top = 10.dp, bottom = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF667EEA).copy(alpha = 0.1f))
                    .touchableOpacity(activeOpacity = 0.7f) { expanded = !expanded }
                    .absolutePadding(left = 10.dp, right = 10.dp, top = 6.dp, bottom = 6.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText(
                    text = label,
                    fontSize = 14.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = UpPrimary,
                    modifier = Modifier.absolutePadding(left = 4.dp)
                )
                MaterialIcons(name = icon, size = 16.dp, color = UpPrimary)
            }
        }
    }
}

private fun truncateAtWord(text: String, maxChars: Int): String {
    if (text.length <= maxChars) return text
    var cut = maxChars
    while (cut > 0 && text[cut] != ' ' && text[cut] != '\n') cut--
    if (cut <= 0) cut = maxChars
    return text.substring(0, cut).trim()
}

@Composable
internal fun UpInfoSection(icon: String, title: String, iconColor: Color, iconHex: String, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier
            .upChild(marginTop = 10.dp, marginBottom = 10.dp, zIndex = 1f)
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .clip(shape)
            .background(Color(0xFFF8FAFC).copy(alpha = 0.5f))
            .border(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.4f), shape)
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            RowReverse(
                Modifier.fillMaxWidth().padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .absolutePadding(left = 12.dp)
                        .size(44.dp)
                        .rnShadow(8.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    LinearGradient(
                        colors = listOf(iconColor, upHex(iconHex, "CC")),
                        modifier = Modifier.fillMaxSize(),
                        start = Offset(0f, 0f),
                        end = Offset(1f, 1f),
                        shape = CircleShape
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            MaterialIcons(name = icon, size = 22.dp, color = UpSurface)
                        }
                    }
                }
                AppText(
                    text = title,
                    fontSize = 18.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = UpDark
                )
            }
            Box(Modifier.fillMaxWidth().zIndex(1f)) { content() }
        }
        Box(Modifier.fillMaxSize()) {
            val accent = RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp)
            Box(
                Modifier
                    .align(AbsoluteAlignment.CenterRight)
                    .fillMaxHeight()
                    .width(6.dp)
                    .rnShadow(3.dp, accent)
                    .background(upHex(iconHex, "40"), accent)
            )
        }
    }
}

@Composable
internal fun UpErrorState(error: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().absolutePadding(left = 40.dp, right = 40.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MaterialIcons(name = "error-outline", size = 48.dp, color = UpError)
        AppText(
            text = error,
            fontSize = 16.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = UpError,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.absolutePadding(top = 20.dp, bottom = 20.dp)
        )
        RowReverse(
            Modifier
                .rnShadow(3.dp, RoundedCornerShape(25.dp))
                .clip(RoundedCornerShape(25.dp))
                .background(UpPrimary)
                .touchableOpacity(onClick = onRetry)
                .absolutePadding(left = 20.dp, right = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MaterialIcons(name = "refresh", size = 20.dp, color = UpSurface)
            AppText(
                text = "تلاش مجدد",
                fontSize = 14.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = UpSurface,
                modifier = Modifier.absolutePadding(right = 8.dp)
            )
        }
    }
}

@Composable
internal fun UpVideoSection(user: JsonElement) {
    val file = user["IntroductionVideoFileName"].str
    val introUrl = user["IntroductionVideoURL"].str
    val hasVideo = !file.isNullOrEmpty() || !introUrl.isNullOrEmpty()
    if (!hasVideo) return
    val url = if (!introUrl.isNullOrEmpty()) introUrl else user["VideoURL"].str
    if (url.isNullOrEmpty()) {
        Column(
            Modifier
                .upChild(marginTop = 20.dp, marginBottom = 60.dp, zIndex = 1f)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF5F5F5))
                .drawBehind {
                    val stroke = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                    )
                    drawRoundRect(
                        color = Color(0xFFE0E0E0),
                        style = stroke,
                        cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            MaterialIcons(name = "play-circle-outline", size = 60.dp, color = UpMedium)
            AppText(
                text = "ویدیو موجود نیست",
                fontSize = 14.sp,
                fontFamily = RNFonts.Yekan_Bakh_Regular,
                color = UpMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
        return
    }
    Box(
        Modifier
            .upChild(marginTop = 20.dp, marginBottom = 60.dp, zIndex = 1f)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Transparent)
    ) {
        LinearGradient(
            colors = listOf(upHex(UpHex.primary, "10"), upHex(UpHex.accent, "10")),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).padding(4.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
            ) {
                NativeVideoPlayer(
                    url = url,
                    modifier = Modifier.fillMaxSize(),
                    showControls = true,
                    autoPlay = false,
                    loop = false,
                    cover = true
                )
            }
        }
    }
}

@Composable
internal fun UpLikeButton(memberId: Int?, initialLikeCount: Int, initialIsLiked: Boolean, onError: (String) -> Unit) {
    var likeCount by remember { mutableIntStateOf(initialLikeCount) }
    var isLiked by remember { mutableStateOf(initialIsLiked) }
    var isLiking by remember { mutableStateOf(false) }
    val heart = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    LaunchedEffect(initialLikeCount, initialIsLiked) {
        likeCount = initialLikeCount
        isLiked = initialIsLiked
    }
    Box(Modifier.fillMaxWidth().padding(top = 15.dp, bottom = 0.dp).absolutePadding(top = 10.dp, bottom = 10.dp).zIndex(8f), contentAlignment = Alignment.TopCenter) {
        Box(
            Modifier
                .clip(RoundedCornerShape(25.dp))
                .touchableOpacity(enabled = !isLiking, activeOpacity = 0.7f) {
                    val userId = AuthState.user?.MemberId
                    if (isLiking || memberId == null || memberId == 0 || userId == null || userId == 0) return@touchableOpacity
                    val prevLiked = isLiked
                    val prevCount = likeCount
                    val nextLiked = !prevLiked
                    isLiking = true
                    isLiked = nextLiked
                    likeCount = prevCount + if (nextLiked) 1 else -1
                    scope.launch {
                        heart.snapTo(0f)
                        heart.animateTo(0.3f, tween(100, easing = UpTimingEasing))
                        heart.animateTo(1f, tween(300, easing = UpTimingEasing))
                    }
                    if (nextLiked) {
                        scope.launch {
                            delay(200)
                            heart.animateTo(0.8f, tween(200, easing = UpTimingEasing))
                            heart.animateTo(0f, tween(600, easing = UpTimingEasing))
                        }
                    } else {
                        scope.launch {
                            heart.animateTo(0f, tween(100, easing = UpTimingEasing))
                        }
                    }
                    scope.launch {
                        try {
                            val response = Api.post(
                                "Member/Like",
                                query = mapOf("likedMemberId" to memberId, "memberId" to userId)
                            )
                            if (!response.ok) {
                                val message = response.json.str("Message") ?: "خطا در ثبت لایک"
                                throw ApiNetworkException(message)
                            }
                            response.json["LikeCount"].int?.let { likeCount = it }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            isLiked = prevLiked
                            likeCount = prevCount
                            onError(e.message ?: "خطا در ثبت لایک")
                        } finally {
                            isLiking = false
                        }
                    }
                }
        ) {
            RowReverse(
                Modifier
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(25.dp))
                    .background(Color.White.copy(alpha = 0.25f))
                    .absolutePadding(left = 18.dp, right = 18.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MaterialIcons(
                    name = if (isLiked) "favorite" else "favorite-border",
                    size = 20.dp,
                    color = if (isLiked) UpSecondary else UpMedium
                )
                AppText(
                    text = toPersianDigits(likeCount.toString()),
                    fontSize = 12.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    color = if (isLiked) UpSecondary else UpMedium,
                    modifier = Modifier.absolutePadding(right = 8.dp)
                )
            }
        }
        val scale = heartScale(heart.value)
        MaterialIcons(
            name = "favorite",
            size = 30.dp,
            color = UpSecondary,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .absoluteOffset(y = (-10).dp)
                .graphicsLayer {
                    alpha = heart.value
                    translationY = with(density) { (heart.value * -100f).dp.toPx() }
                    scaleX = scale
                    scaleY = scale
                }
        )
    }
}

private fun heartScale(v: Float): Float = when {
    v <= 0.3f -> lerp(0.5f, 1.5f, if (v <= 0f) 0f else v / 0.3f)
    v <= 0.7f -> lerp(1.2f, 1.5f, (0.7f - v) / 0.4f)
    else -> lerp(0.3f, 1.2f, (1f - v) / 0.3f)
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)
