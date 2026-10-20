package ir.farimod.app.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import ir.farimod.app.components.icons.MaterialCommunityIcons
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.network.ApiConfig
import ir.farimod.app.network.bool
import ir.farimod.app.network.get
import ir.farimod.app.network.str
import ir.farimod.app.resources.Res
import ir.farimod.app.resources.blog_post_icon
import ir.farimod.app.resources.portfolio_icon
import ir.farimod.app.resources.product_icon
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.LocalImage
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.ResizeMode
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.linearGradient
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.rnTextStyle
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.utils.formatPrice
import ir.farimod.app.utils.jsToFixed
import ir.farimod.app.utils.safeNumber
import ir.farimod.app.utils.safeString
import ir.farimod.app.utils.toPersianDigits
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlin.math.round

private val avatarGradients = listOf(
    listOf(Color(0xFFFA709A), Color(0xFFFEE140)),
    listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
    listOf(Color(0xFFF093FB), Color(0xFFF5576C)),
    listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)),
    listOf(Color(0xFF43E97B), Color(0xFF38F9D7))
)

private val groupGradients = listOf(
    listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
    listOf(Color(0xFFF093FB), Color(0xFFF5576C)),
    listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)),
    listOf(Color(0xFF43E97B), Color(0xFF38F9D7)),
    listOf(Color(0xFFFA709A), Color(0xFFFEE140)),
    listOf(Color(0xFF30CFD0), Color(0xFF330867)),
    listOf(Color(0xFFA8EDEA), Color(0xFFFED6E3)),
    listOf(Color(0xFFFF9A9E), Color(0xFFFECFEF)),
    listOf(Color(0xFFFFECD2), Color(0xFFFCB69F)),
    listOf(Color(0xFFFF6E7F), Color(0xFFBFE9FF)),
    listOf(Color(0xFF8EC5FC), Color(0xFFE0C3FC)),
    listOf(Color(0xFFFBC2EB), Color(0xFFA6C1EE)),
    listOf(Color(0xFFFDCBF1), Color(0xFFE6DEE9)),
    listOf(Color(0xFFA1C4FD), Color(0xFFC2E9FB)),
    listOf(Color(0xFFD299C2), Color(0xFFFEF9D7)),
    listOf(Color(0xFFFEE140), Color(0xFFFA709A)),
    listOf(Color(0xFFFDBB2D), Color(0xFF22C1C3)),
    listOf(Color(0xFFEE9CA7), Color(0xFFFFDDE1)),
    listOf(Color(0xFF89F7FE), Color(0xFF66A6FF)),
    listOf(Color(0xFFCD9CF2), Color(0xFFF6F3FF))
)

private fun homeMod(value: Int, size: Int): Int {
    if (size <= 0) return 0
    val m = value % size
    return if (m < 0) m + size else m
}

private val fashionIcons = listOf(
    "tshirt-crew",
    "hanger",
    "ruler",
    "draw",
    "palette",
    "scissors-cutting",
    "content-cut",
    "tag",
    "shopping",
    "badge-account",
    "brush",
    "dots-grid"
)

@Composable
internal fun HomeAvatar(
    member: JsonElement,
    size: Dp = 100.dp,
    onPress: (() -> Unit)? = null
) {
    val name = member.str("Name").orEmpty()
    val avatarUrl = member.str("AvatarImageURL")?.trim().orEmpty()
    var imageError by remember(avatarUrl) { mutableStateOf(false) }
    val hasImage = avatarUrl.isNotEmpty() && !imageError
    val gradient = avatarGradients[if (name.isEmpty()) 0 else name.length % avatarGradients.size]
    val tick = size * 0.28f

    Column(
        modifier = Modifier
            .absolutePadding(left = 10.dp, right = 10.dp, top = 8.dp, bottom = 8.dp)
            .then(
                if (onPress != null) Modifier.homeSpringPress(activeOpacity = 0.8f) { onPress() }
                else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(size)) {
            if (hasImage) {
                val resolved = ApiConfig.resolveMediaUrl(avatarUrl)
                Box(
                    Modifier
                        .size(size)
                        .rnShadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                ) {
                    if (resolved != null) {
                        AsyncImage(
                            model = resolved,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onError = { _ -> imageError = true }
                        )
                    }
                }
            } else {
                LinearGradient(
                    colors = gradient,
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape),
                    start = Offset(0f, 0f),
                    end = Offset(1f, 1f),
                    shape = CircleShape
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        MaterialCommunityIcons(
                            name = if (member["Gender"].bool) "face-man" else "face-woman",
                            size = size * 0.7f,
                            color = Color.White
                        )
                    }
                }
            }
            if (member["ShowBlueTick"].bool) {
                Box(
                    Modifier
                        .align(AbsoluteAlignment.BottomRight)
                        .absoluteOffset(x = -(size * 0.05f), y = -(size * 0.2f) - 15.dp)
                        .size(tick)
                        .rnShadow(3.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    MaterialIcons(name = "verified", size = size * 0.22f, color = Color(0xFF1DA1F2))
                }
            }
        }
        if (name.isNotEmpty()) {
            AppText(
                text = safeString(name, "کاربر"),
                modifier = Modifier.padding(top = 8.dp).widthIn(max = 120.dp),
                fontSize = (size.value * 0.13f).sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                fontWeight = FontWeight.W600,
                color = Color(0xFF333333),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun HomeMemberGroupCard(item: JsonElement, onPress: (JsonElement) -> Unit) {
    val id = item.homeInt("MemberGroupId")
    val gradient = groupGradients[homeMod(id, groupGradients.size)]
    val icon = fashionIcons[homeMod(id, fashionIcons.size)]
    val shape = RoundedCornerShape(20.dp)

    val rawUrl = item.str("ImageURL")
        ?: item.str("FeaturedImageURL")
        ?: item.str("GroupImageURL")
        ?: item.str("IconURL")
        ?: item.str("PictureURL")
        ?: item.str("PhotoURL")
        ?: item.str("Image")
        ?: item.str("FeaturedImageFileName")
        ?: item.str("ImageFileName")
    val imageUrl = ApiConfig.resolveMediaUrl(rawUrl)
    var imageError by remember(imageUrl) { mutableStateOf(false) }
    val showRemote = !imageUrl.isNullOrBlank() && !imageError

    Box(
        Modifier
            .absolutePadding(left = 6.dp, right = 6.dp, top = 6.dp, bottom = 6.dp)
            .homeSpringPress(activeOpacity = 0.9f) { onPress(item) }
    ) {
        Box(
            Modifier
                .size(105.dp)
                .rnShadow(6.dp, shape)
                .clip(shape)
        ) {
            if (showRemote) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = { imageError = true }
                )
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(0.7f)
                        .linearGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                )
                val density = LocalDensity.current
                val shadow = with(density) {
                    Shadow(
                        color = Color.Black.copy(alpha = 0.8f),
                        offset = Offset(0f, 1.dp.toPx()),
                        blurRadius = 3.dp.toPx()
                    )
                }
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = safeString(item["GroupName"], "گروه بدون نام"),
                        style = rnTextStyle(
                            fontSize = 11.sp,
                            color = Color.White,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        ).copy(shadow = shadow),
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                LinearGradient(
                    colors = gradient,
                    modifier = Modifier
                        .fillMaxSize()
                        .border(2.dp, Color.White.copy(alpha = 0.6f), shape),
                    start = Offset(0f, 0f),
                    end = Offset(1f, 1f)
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            Modifier.padding(bottom = 6.dp)
                        ) {
                            MaterialCommunityIcons(
                                name = icon,
                                size = 30.dp,
                                color = Color.Black.copy(alpha = 0.95f)
                            )
                        }
                        AppText(
                            text = safeString(item["GroupName"], "گروه بدون نام"),
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 11.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun HomeProductCard(item: JsonElement, onPress: (JsonElement) -> Unit) {
    val price = safeNumber(item["Price"])
    val special = safeNumber(item["SpecialSalePrice"])
    val discount = if (price > 0.0 && special > 0.0) round(((price - special) / price) * 100.0).toInt() else 0
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .touchableOpacity(activeOpacity = 0.8f) { onPress(item) }
            .rnShadow(2.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(Color(0xFFF5F5F5))
        ) {
            HomeProductImage(item.str("FeaturedImageURL"))
            if (discount > 0) {
                Box(
                    Modifier
                        .align(AbsoluteAlignment.TopRight)
                        .absolutePadding(top = 8.dp, right = 8.dp)
                        .zIndex(2f)
                        .background(Color(0xFFFF6B6B), RoundedCornerShape(8.dp))
                        .absolutePadding(left = 6.dp, right = 6.dp, top = 3.dp, bottom = 3.dp)
                ) {
                    AppText(
                        text = "${toPersianDigits(discount.toString())}% تخفیف",
                        fontSize = 10.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color.White
                    )
                }
            }
            if (!item["Active"].bool) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .zIndex(3f)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    AppText(
                        text = "غیرفعال",
                        fontSize = 14.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 85.dp)
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppText(
                text = safeString(item["ProductName"], "نام محصول"),
                modifier = Modifier.padding(bottom = 4.dp),
                fontSize = 13.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = Color(0xFF333333),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 4.dp)) {
                if (discount > 0) {
                    AppText(
                        text = formatPrice(price),
                        fontSize = 11.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        color = Color(0xFF999999),
                        textAlign = TextAlign.Center,
                        textDecoration = TextDecoration.LineThrough
                    )
                    AppText(
                        text = formatPrice(special),
                        fontSize = 14.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color(0xFFFF6B6B),
                        textAlign = TextAlign.Center
                    )
                } else {
                    AppText(
                        text = formatPrice(price),
                        fontSize = 14.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color(0xFF667EEA),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeProductImage(url: String?) {
    var imageError by remember(url) { mutableStateOf(false) }
    var imageLoaded by remember(url) { mutableStateOf(false) }
    var imageKey by remember(url) { mutableIntStateOf(0) }
    val retryCount = remember(url) { intArrayOf(0) }
    val retrying = remember(url) { booleanArrayOf(false) }
    val scope = rememberCoroutineScope()
    val job = remember(url) { mutableStateOf<Job?>(null) }
    DisposableEffect(url) {
        onDispose { job.value?.cancel() }
    }
    Box(Modifier.fillMaxSize()) {
        if (!imageLoaded) {
            LocalImage(Res.drawable.product_icon, Modifier.fillMaxSize(), ResizeMode.cover)
        }
        if (!url.isNullOrBlank() && !imageError) {
            val model = ApiConfig.resolveMediaUrl("$url?retry=$imageKey")
            if (model != null) {
                AsyncImage(
                    model = model,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().alpha(if (imageLoaded) 1f else 0f),
                    contentScale = ContentScale.Crop,
                    onSuccess = { _ -> imageLoaded = true },
                    onError = { _ ->
                        if (retryCount[0] < 3) {
                            if (!retrying[0]) {
                                retrying[0] = true
                                val attempt = retryCount[0]
                                job.value?.cancel()
                                job.value = scope.launch {
                                    delay(1000L * (attempt + 1))
                                    retryCount[0] = attempt + 1
                                    imageKey += 1
                                    retrying[0] = false
                                }
                            }
                        } else {
                            imageError = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
internal fun HomeBlogCard(item: JsonElement, onPress: (JsonElement) -> Unit) {
    val url = item.str("FeaturedImageURL")
    var imageError by remember(url) { mutableStateOf(false) }
    val showRemote = !url.isNullOrBlank() && !imageError
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .touchableOpacity(activeOpacity = 0.8f) { onPress(item) }
            .rnShadow(1.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
    ) {
        Box(Modifier.fillMaxWidth().height(200.dp)) {
            if (showRemote) {
                val resolved = ApiConfig.resolveMediaUrl(url)
                if (resolved != null) {
                    AsyncImage(
                        model = resolved,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onError = { _ -> imageError = true }
                    )
                }
            } else {
                LocalImage(Res.drawable.blog_post_icon, Modifier.fillMaxSize(), ResizeMode.cover)
            }
        }
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            AppText(
                text = safeString(item["Title"], "مقاله بدون نام"),
                modifier = Modifier.padding(bottom = 12.dp),
                fontSize = 18.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = Color(0xFF2C3E50),
                textAlign = TextAlign.Right,
                lineHeight = 26.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            RowReverse(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RowReverse(verticalAlignment = Alignment.CenterVertically) {
                    MaterialIcons(name = "calendar-month", size = 16.dp, color = Color(0xFF666666))
                    AppText(
                        text = toPersianDigits(item["ShamsiInsertDate"]),
                        modifier = Modifier.absolutePadding(right = 6.dp),
                        fontSize = 14.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        color = Color(0xFF666666)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MaterialIcons(name = "favorite", size = 16.dp, color = Color(0xFFFF6B6B))
                    AppText(
                        text = toPersianDigits((item.homeInt("LikeCount")).toString()),
                        modifier = Modifier.absolutePadding(left = 6.dp),
                        fontSize = 14.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        color = Color(0xFF666666)
                    )
                }
            }
        }
    }
}

@Composable
internal fun HomeGalleryCard(item: JsonElement, onPress: (JsonElement) -> Unit) {
    val url = item.str("FeaturedImageURL")
    var imageError by remember(url) { mutableStateOf(false) }
    val hasImage = !url.isNullOrBlank() && !imageError
    Box(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .touchableOpacity(activeOpacity = 0.8f) { onPress(item) }
            .rnShadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF5F5F5))
        ) {
            if (hasImage) {
                val resolved = ApiConfig.resolveMediaUrl(url)
                if (resolved != null) {
                    AsyncImage(
                        model = resolved,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onError = { _ -> imageError = true }
                    )
                }
            } else {
                Box(Modifier.fillMaxSize().background(Color(0xFFE0E0E0)), contentAlignment = Alignment.Center) {
                    MaterialIcons(name = "photo-library", size = 48.dp, color = Color(0xFF9E9E9E))
                }
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .linearGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
            )
            RowReverse(
                modifier = Modifier
                    .align(AbsoluteAlignment.TopLeft)
                    .absolutePadding(left = 8.dp, top = 8.dp)
                    .rnShadow(3.dp, RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                    .absolutePadding(left = 6.dp, right = 6.dp, top = 3.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MaterialIcons(name = "favorite", size = 12.dp, color = Color(0xFFFF6B6B))
                AppText(
                    text = toPersianDigits(item.homeInt("LikeCount").toString()),
                    modifier = Modifier.absolutePadding(right = 3.dp),
                    fontSize = 10.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color(0xFF333333)
                )
            }
            val density = LocalDensity.current
            val shadow = with(density) {
                Shadow(
                    color = Color.Black.copy(alpha = 0.8f),
                    offset = Offset(0f, 1.dp.toPx()),
                    blurRadius = 3.dp.toPx()
                )
            }
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(15.dp)
                    .padding(bottom = 5.dp)
            ) {
                Text(
                    text = safeString(item["Title"], "گالری بدون نام"),
                    style = rnTextStyle(
                        fontSize = 16.sp,
                        color = Color.White,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.5.sp
                    ).copy(shadow = shadow),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun HomePortfolioCard(item: JsonElement, onPress: (JsonElement) -> Unit) {
    val url = item.str("FeaturedImageURL")
    var imageError by remember(url) { mutableStateOf(false) }
    val showRemote = !url.isNullOrBlank() && !imageError
    val rating = item["Rating"]
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .height(270.dp)
            .touchableOpacity(activeOpacity = 0.8f) { onPress(item) }
            .rnShadow(3.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
    ) {
        Box(Modifier.fillMaxWidth().height(140.dp)) {
            if (showRemote) {
                val resolved = ApiConfig.resolveMediaUrl(url)
                if (resolved != null) {
                    AsyncImage(
                        model = resolved,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        onError = { _ -> imageError = true }
                    )
                }
            } else {
                LocalImage(Res.drawable.portfolio_icon, Modifier.fillMaxSize(), ResizeMode.cover)
            }
            RowReverse(
                modifier = Modifier
                    .align(AbsoluteAlignment.TopLeft)
                    .absolutePadding(left = 8.dp, top = 8.dp)
                    .background(Color(0xFFE91E63).copy(alpha = 0.8f), RoundedCornerShape(15.dp))
                    .absolutePadding(left = 6.dp, right = 6.dp, top = 3.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MaterialIcons(name = "favorite", size = 14.dp, color = Color.White)
                AppText(
                    text = toPersianDigits(item.homeInt("LikeCount").toString()),
                    modifier = Modifier.absolutePadding(right = 3.dp),
                    fontSize = 11.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color.White
                )
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                AppText(
                    text = safeString(item["Title"], "نمونه کار بدون نام"),
                    modifier = Modifier.padding(bottom = 6.dp),
                    fontSize = 15.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color(0xFF2C3E50),
                    textAlign = TextAlign.Right,
                    lineHeight = 22.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                AppText(
                    text = safeString(item["Description"], "بدون توضیحات"),
                    modifier = Modifier.padding(bottom = 10.dp),
                    fontSize = 12.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    color = Color(0xFF666666),
                    textAlign = TextAlign.Right,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            RowReverse(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RowReverse(verticalAlignment = Alignment.CenterVertically) {
                    MaterialIcons(name = "calendar-month", size = 14.dp, color = Color(0xFF666666))
                    AppText(
                        text = toPersianDigits(item["ShamsiInsertDate"]),
                        modifier = Modifier.absolutePadding(right = 4.dp),
                        fontSize = 11.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        color = Color(0xFF666666)
                    )
                }
                if (rating.bool) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MaterialIcons(name = "star", size = 14.dp, color = Color(0xFFFFD700))
                        AppText(
                            text = toPersianDigits(jsToFixed(safeNumber(rating), 1)),
                            modifier = Modifier.absolutePadding(left = 3.dp),
                            fontSize = 11.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }
        }
    }
}
