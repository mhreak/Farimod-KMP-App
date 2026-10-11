package ir.farimod.app.screens.portfoliodetail

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.network.ApiConfig
import ir.farimod.app.network.bool
import ir.farimod.app.network.double
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.jsonOf
import ir.farimod.app.network.list
import ir.farimod.app.network.long
import ir.farimod.app.network.str
import ir.farimod.app.resources.Res
import ir.farimod.app.resources.portfolio_icon
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.LocalImage
import ir.farimod.app.ui.MultiOptionRatingComponent
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RatingOption
import ir.farimod.app.ui.RatingSubmitResult
import ir.farimod.app.ui.ResizeMode
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.StarDisplay
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.rnTextStyle
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.ui.windowSize
import ir.farimod.app.utils.jsNumberToString
import ir.farimod.app.utils.jsToFixed
import ir.farimod.app.utils.jsToString
import ir.farimod.app.utils.jsTruthy
import ir.farimod.app.utils.toPersianDigits
import kotlinx.coroutines.flow.drop
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal val DetailPrimary = Color(0xFF667EEA)
internal val DetailPrimaryDark = Color(0xFF764BA2)
internal val DetailInfo = Color(0xFF3498DB)
internal val DetailError = Color(0xFFE74C3C)
internal val DetailSuccess = Color(0xFF2ECC71)
internal val DetailPink = Color(0xFFFF69B4)
internal val DetailDark = Color(0xFF2C3E50)
internal val DetailStar = Color(0xFFFFB300)
internal val DetailLike = Color(0xFFE91E63)

internal data class PortfolioDetailModel(
    val portfolioId: Int,
    val title: String,
    val description: String,
    val memberName: String,
    val shamsiInsertDate: String,
    val likeCount: Int,
    val images: List<String>,
    val isAvailable: Boolean,
    val memberId: Int?,
    val status: String,
    val averageRating: Double,
    val ratingCount: Int,
    val userRating: Double,
    val userDetailedRatings: Map<String, Double>,
    val isMemberLiked: Boolean,
    val contentReviewItems: List<JsonObject>
) {
    fun withRating(userRating: Double, ratings: Map<String, Double>): PortfolioDetailModel =
        copy(userRating = userRating, userDetailedRatings = ratings)

    fun toRouteJson(): JsonObject {
        val ratings = buildJsonObject {
            userDetailedRatings.forEach { (k, v) ->
                if (v == v.toLong().toDouble()) put(k, v.toLong()) else put(k, v)
            }
        }
        return jsonOf(
            "PortfolioId" to portfolioId,
            "Title" to title,
            "Description" to description,
            "Category" to "نمونه کار",
            "MemberName" to memberName,
            "ShamsiInsertDate" to shamsiInsertDate,
            "ViewCount" to 0,
            "LikeCount" to likeCount,
            "Images" to images,
            "IsAvailable" to isAvailable,
            "MemberId" to memberId,
            "Status" to status,
            "AverageRating" to averageRating,
            "RatingCount" to ratingCount,
            "UserRating" to userRating,
            "UserDetailedRatings" to ratings,
            "DetailedRatingsAverages" to jsonOf(),
            "IsMemberLiked" to isMemberLiked,
            "ContentReviewItemList" to contentReviewItems
        )
    }
}

internal fun parsePortfolioDetail(result: kotlinx.serialization.json.JsonElement): PortfolioDetailModel {
    val portfolio = result["Portfolio"] ?: throw IllegalStateException("HTTP error! status: 200")
    val images = listOf(
        "FeaturedImageURL", "FirstImageURL", "SecondImageURL",
        "ThirdImageURL", "FourthImageURL", "FifthImageURL"
    ).mapNotNull { key -> portfolio.str(key)?.trim()?.takeIf { it.isNotEmpty() } }
    val active = jsTruthy(portfolio["Active"])
    val ratingsEl = result["UserDetailedRatings"]
    val ratings = if (ratingsEl is JsonObject) {
        ratingsEl.entries.mapNotNull { (k, v) -> v.double?.let { k to it } }.toMap()
    } else {
        emptyMap()
    }
    val reviews = if (jsTruthy(result["ContentReviewItemList"])) {
        result.list("ContentReviewItemList").mapNotNull { it as? JsonObject }
    } else {
        emptyList()
    }
    val dateRaw = portfolio["ShamsiInsertDate"]
    return PortfolioDetailModel(
        portfolioId = portfolio.int("PortfolioId") ?: 0,
        title = portfolio.str("Title")?.takeIf { it.isNotEmpty() } ?: "نام نامشخص",
        description = portfolio.str("Description")?.takeIf { it.isNotEmpty() } ?: "توضیحات موجود نیست",
        memberName = portfolio.str("MemberName")?.takeIf { it.isNotEmpty() } ?: "نام طراح نامشخص",
        shamsiInsertDate = if (jsTruthy(dateRaw)) jsToString(dateRaw) else "تاریخ نامشخص",
        likeCount = if (jsTruthy(portfolio["LikeCount"])) portfolio["LikeCount"].int ?: portfolio["LikeCount"].double?.toInt() ?: 0 else 0,
        images = images,
        isAvailable = active,
        memberId = portfolio.int("MemberId"),
        status = if (active) "منتشر شده" else "غیرفعال",
        averageRating = if (jsTruthy(portfolio["Rating"])) portfolio["Rating"].double ?: 0.0 else 0.0,
        ratingCount = if (jsTruthy(portfolio["RatingCount"])) portfolio["RatingCount"].int ?: 0 else 0,
        userRating = if (jsTruthy(result["UserRating"])) result["UserRating"].double ?: 0.0 else 0.0,
        userDetailedRatings = ratings,
        isMemberLiked = jsTruthy(result["IsMemberLiked"]),
        contentReviewItems = reviews
    )
}

internal fun defaultPortfolioRatingOptions(): List<RatingOption> = listOf(
    RatingOption("quality", "کیفیت کار", "کیفیت کلی و دقت در جزئیات"),
    RatingOption("creativity", "خلاقیت", "نوآوری و ایده‌های جدید"),
    RatingOption("materials", "مواد و پارچه", "انتخاب و کیفیت مواد اولیه"),
    RatingOption("style", "سبک و طراحی", "زیبایی و هماهنگی کلی")
)

internal fun transformContentReview(list: List<JsonObject>): List<RatingOption> {
    if (list.isEmpty()) return defaultPortfolioRatingOptions()
    return list
        .filter { jsTruthy(it["Active"]) }
        .sortedBy { it["ShowOrder"].double ?: 0.0 }
        .map { item ->
            val rawId = item["ContentReviewItemId"]
            val idText = rawId.long?.let { jsNumberToString(it.toDouble()) } ?: rawId.str ?: ""
            val show = item["ShowOrder"]
            RatingOption(
                id = "review_$idText",
                title = item.str("Text")?.takeIf { it.isNotEmpty() } ?: "مورد ${jsToString(show)}",
                subtitle = "",
                contentReviewItemId = rawId.long ?: rawId.str,
                showOrder = show.double,
                averageRating = item["CalculatedAverageRating"].double ?: 0.0
            )
        }
}

@Composable
internal fun PortfolioDetailSkeleton() {
    val transition = rememberInfiniteTransition()
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse)
    )
    Column(Modifier.fillMaxWidth().absolutePadding(left = 20.dp, right = 20.dp).padding(bottom = 40.dp)) {
        repeat(3) { card ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .rnShadow(6.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .padding(20.dp)
            ) {
                if (card == 0) {
                    DetailBone(Modifier.fillMaxWidth().height(300.dp).padding(bottom = 15.dp), 12.dp, pulse)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        repeat(3) {
                            DetailBone(Modifier.width(20.dp).height(8.dp), 4.dp, pulse)
                        }
                    }
                } else if (card == 1) {
                    DetailBone(Modifier.fillMaxWidth(0.8f).align(Alignment.Start).padding(bottom = 15.dp).height(28.dp), 8.dp, pulse)
                    DetailBone(Modifier.fillMaxWidth().align(Alignment.Start).padding(bottom = 8.dp).height(18.dp), 8.dp, pulse)
                    DetailBone(Modifier.fillMaxWidth(0.95f).align(Alignment.Start).padding(bottom = 8.dp).height(18.dp), 8.dp, pulse)
                    DetailBone(Modifier.fillMaxWidth(0.9f).align(Alignment.Start).height(18.dp), 8.dp, pulse)
                } else {
                    DetailBone(Modifier.fillMaxWidth(0.4f).align(Alignment.Start).padding(bottom = 15.dp).height(22.dp), 8.dp, pulse)
                    DetailBone(Modifier.width(150.dp).align(Alignment.Start).height(30.dp), 15.dp, pulse)
                }
            }
        }
    }
}

@Composable
private fun DetailBone(modifier: Modifier, radius: Dp, pulse: Float) {
    Box(modifier.alpha(pulse).clip(RoundedCornerShape(radius)).background(Color(0xFFE0E0E0)))
}

@Composable
internal fun PortfolioDetailRemoteImage(
    url: String,
    modifier: Modifier = Modifier,
    onLoad: () -> Unit = {},
    onError: () -> Unit = {}
) {
    var failed by remember(url) { mutableStateOf(false) }
    var loading by remember(url) { mutableStateOf(true) }
    val resolved = ApiConfig.resolveMediaUrl(url)
    if (failed || resolved == null) {
        LaunchedEffect(url) { onError() }
        return
    }
    Box(modifier) {
        AsyncImage(
            model = resolved,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(50.dp)),
            contentScale = ContentScale.Fit,
            onSuccess = {
                loading = false
                onLoad()
            },
            onError = {
                loading = false
                failed = true
            }
        )
        if (loading && !failed) {
            Box(
                Modifier.fillMaxSize().background(Color(0xFFF5F5F5), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(Modifier.size(18.dp), color = Color(0xFFCCCCCC), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
internal fun PortfolioDefaultImage(modifier: Modifier = Modifier) {
    LocalImage(Res.drawable.portfolio_icon, modifier, ResizeMode.contain)
}

@Composable
internal fun PortfolioLikeBadge(
    liked: Boolean,
    count: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (liked) argb(233, 30, 99, 204) else Color.White.copy(alpha = 0.8f)
    val fg = if (liked) Color.White else DetailLike
    Box(
        modifier
            .rnShadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .touchableOpacity(enabled = enabled, activeOpacity = 0.7f, onClick = onClick)
            .absolutePadding(left = 12.dp, right = 12.dp, top = 8.dp, bottom = 8.dp)
    ) {
        RowReverse(verticalAlignment = Alignment.CenterVertically) {
            MaterialIcons(if (liked) "favorite" else "favorite-border", size = 18.dp, color = fg)
            AppText(
                text = toPersianDigits(count.toString()),
                fontSize = 15.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = fg,
                modifier = Modifier.absolutePadding(right = 6.dp)
            )
        }
    }
}

@Composable
internal fun PortfolioGallery(
    images: List<String>,
    errorIndexes: Set<Int>,
    liked: Boolean,
    likeCount: Int,
    liking: Boolean,
    onLike: () -> Unit,
    onImageError: (Int) -> Unit,
    onImageOpen: (Int) -> Unit
) {
    val screenW = windowSize().first
    val side = screenW - 40.dp
    val pages = images.mapIndexedNotNull { index, url -> if (index in errorIndexes) null else index to url }
    val isDefault = images.isNotEmpty() && pages.isEmpty()
    Box(Modifier.fillMaxWidth().height(side)) {
        when {
            images.isEmpty() || isDefault -> PortfolioDefaultImage(Modifier.fillMaxSize())
            else -> {
                val pagerState = rememberPagerState { pages.size.coerceAtLeast(1) }
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    val (original, url) = pages[page]
                    Box(
                        Modifier
                            .fillMaxSize()
                            .touchableOpacity(activeOpacity = 0.9f) { onImageOpen(original) }
                    ) {
                        PortfolioDetailRemoteImage(
                            url = url,
                            modifier = Modifier.fillMaxSize(),
                            onError = { onImageError(original) }
                        )
                    }
                }
                if (pages.size > 1) {
                    Row(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 15.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        pages.forEachIndexed { index, _ ->
                            val active = index == pagerState.currentPage
                            Box(
                                Modifier
                                    .height(8.dp)
                                    .width(if (active) 20.dp else 8.dp)
                                    .background(
                                        if (active) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.5f),
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
        PortfolioLikeBadge(
            liked = liked,
            count = likeCount,
            enabled = !liking,
            onClick = onLike,
            modifier = Modifier.align(AbsoluteAlignment.TopLeft).absolutePadding(left = 15.dp, top = 15.dp)
        )
    }
}

@Composable
internal fun PortfolioDesignerCard(name: String) {
    DetailCard(bottom = 15.dp) {
        RowReverse(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .absolutePadding(left = 12.dp)
                    .size(40.dp)
                    .border(1.dp, argb(119, 178, 210, 51), CircleShape)
                    .background(argb(119, 178, 210, 26), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                MaterialIcons("person", size = 24.dp, color = Color(0xFF77B2D2))
            }
            AppText(
                text = name,
                fontSize = 18.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = DetailDark
            )
        }
    }
}

@Composable
internal fun PortfolioMainCard(title: String, description: String, date: String) {
    DetailCard(bottom = 15.dp) {
        AppText(
            text = toPersianDigits(title),
            fontSize = 22.sp,
            fontFamily = RNFonts.Yekan_Bakh_ExtraBold,
            color = DetailDark,
            textAlign = TextAlign.Right,
            lineHeight = 32.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )
        Text(
            text = toPersianDigits(description),
            modifier = Modifier.fillMaxWidth().padding(bottom = 15.dp),
            style = rnTextStyle(
                fontSize = 16.sp,
                color = Color(0xFF6C757D),
                fontFamily = RNFonts.Yekan_Bakh_Regular,
                textAlign = TextAlign.Justify,
                lineHeight = 26.sp
            ).copy(textDirection = TextDirection.Rtl)
        )
        Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF0F0F0)))
            RowReverse(
                Modifier.fillMaxWidth().padding(top = 15.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MaterialIcons("calendar-today", size = 16.dp, color = Color(0xFF9E9E9E))
                AppText(
                    text = toPersianDigits(date),
                    fontSize = 14.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.absolutePadding(right = 6.dp)
                )
            }
        }
    }
}

@Composable
internal fun PortfolioRatingBlock(
    portfolio: PortfolioDetailModel,
    options: List<RatingOption>,
    userRatings: Map<String, Double>,
    onSubmitted: (RatingSubmitResult) -> Unit,
    onError: (String) -> Unit,
    onChange: (Double, Map<String, Double>?) -> Unit
) {
    val displayOptions = if (options.isNotEmpty()) options else defaultPortfolioRatingOptions()
    DetailCard(bottom = 15.dp) {
        AppText(
            text = "امتیازدهی",
            fontSize = 18.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = DetailDark,
            textAlign = TextAlign.Right,
            modifier = Modifier.fillMaxWidth().padding(bottom = 15.dp)
        )
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            MultiOptionRatingComponent(
                contentId = portfolio.portfolioId,
                averageRating = portfolio.averageRating,
                ratingCount = portfolio.ratingCount,
                initialRating = portfolio.userRating,
                initialDetailedRatings = userRatings,
                maxStars = 5,
                size = 24.dp,
                starColor = DetailStar,
                enableMultipleOptions = true,
                ratingOptions = displayOptions,
                modalTitle = "امتیازدهی نمونه کار",
                submitButtonText = "ثبت امتیاز",
                cancelButtonText = "لغو",
                showRatingCount = true,
                showRatingText = true,
                animated = true,
                allowHalfStars = false,
                onRatingSubmitted = onSubmitted,
                onRatingError = onError,
                onRatingChange = onChange
            )
            if (options.isNotEmpty() && userRatings.isNotEmpty()) {
                val shown = options.filter { (userRatings[it.id] ?: 0.0) != 0.0 }
                if (shown.isNotEmpty()) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp)
                            .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(15.dp))
                            .background(Color(0xFFE8F5E8), RoundedCornerShape(15.dp))
                            .padding(15.dp)
                    ) {
                        AppText(
                            text = "امتیازات شما:",
                            fontSize = 16.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            color = DetailDark,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 15.dp)
                        )
                        shown.forEach { option ->
                            val score = userRatings[option.id] ?: 0.0
                            RowReverse(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                                    .border(1.dp, Color(0xFFE8F5E8), RoundedCornerShape(12.dp))
                                    .background(Color.White, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                    AppText(
                                        text = option.title,
                                        fontSize = 14.sp,
                                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                                        color = DetailDark,
                                        textAlign = TextAlign.Right
                                    )
                                }
                                RowReverse(
                                    Modifier.absolutePadding(left = 15.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StarDisplay(
                                        rating = score,
                                        maxStars = 5,
                                        size = 16.dp,
                                        color = DetailStar,
                                        emptyColor = Color(0xFFE0E0E0),
                                        showHalfStars = false,
                                        animated = false
                                    )
                                    AppText(
                                        text = toPersianDigits(jsNumberToString(score)),
                                        fontSize = 14.sp,
                                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                                        color = DetailSuccess
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PortfolioReviewBlock(items: List<JsonObject>) {
    val rated = items
        .filter {
            jsTruthy(it["Active"]) && (it["CalculatedAverageRating"].double ?: 0.0) > 0.0
        }
        .sortedBy { it["ShowOrder"].double ?: 0.0 }
    if (rated.isEmpty()) return
    DetailCard(bottom = 15.dp) {
        AppText(
            text = "میانگین امتیاز در هر بخش",
            fontSize = 18.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = DetailDark,
            textAlign = TextAlign.Right,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 15.dp)
                .drawBehind {
                    val y = size.height
                    drawLine(Color(0xFFF0F0F0), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
        )
        Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
            rated.forEach { item ->
                val score = item["CalculatedAverageRating"].double ?: 0.0
                val title = item.str("Text")?.takeIf { it.isNotEmpty() } ?: "مورد ${jsToString(item["ShowOrder"])}"
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 15.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8F9FA))
                ) {
                    Row(
                        Modifier.weight(1f).padding(15.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RowReverse(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StarDisplay(
                                rating = score,
                                size = 16.dp,
                                color = DetailStar,
                                showHalfStars = true
                            )
                            AppText(
                                text = toPersianDigits(jsToFixed(score, 1)),
                                fontSize = 14.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Bold,
                                color = DetailStar
                            )
                        }
                        AppText(
                            text = title,
                            fontSize = 16.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            color = DetailDark,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.weight(1f).absolutePadding(left = 8.dp)
                        )
                    }
                    Box(Modifier.width(4.dp).height(56.dp).background(DetailPink))
                }
            }
        }
    }
}

@Composable
internal fun DetailCard(bottom: Dp, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = bottom)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.8f))
            .padding(20.dp)
    ) {
        content()
    }
}

@Composable
internal fun PortfolioDetailError(onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().absolutePadding(left = 40.dp, right = 40.dp, top = 60.dp, bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearGradient(
            colors = listOf(Color(0xFFFF6B6B), Color(0xFFEE5A52)),
            modifier = Modifier
                .padding(bottom = 25.dp)
                .size(120.dp)
                .rnShadow(12.dp, CircleShape, Color(0xFFFF6B6B)),
            shape = CircleShape
        ) {
            MaterialIcons("error-outline", size = 60.dp, color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
        AppText(
            text = "خطا در دریافت اطلاعات",
            fontSize = 22.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = DetailDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        AppText(
            text = "اتصال اینترنت خود را بررسی کنید",
            fontSize = 16.sp,
            fontFamily = RNFonts.Yekan_Bakh_Regular,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(bottom = 30.dp)
        )
        Box(
            Modifier
                .rnShadow(10.dp, RoundedCornerShape(25.dp), DetailPrimary)
                .clip(RoundedCornerShape(25.dp))
                .touchableOpacity(onClick = onRetry)
        ) {
            LinearGradient(
                colors = listOf(DetailPrimary, DetailPrimaryDark),
                modifier = Modifier,
                start = Offset(0f, 0f),
                end = Offset(1f, 1f),
                shape = RoundedCornerShape(25.dp)
            ) {
                RowReverse(
                    Modifier.absolutePadding(left = 25.dp, right = 25.dp, top = 15.dp, bottom = 15.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MaterialIcons("refresh", size = 22.dp, color = FarimodColors.White)
                    AppText(
                        text = "تلاش مجدد",
                        fontSize = 16.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = FarimodColors.White
                    )
                }
            }
        }
    }
}

@Composable
internal fun PortfolioDetailEmpty() {
    Column(
        Modifier.fillMaxWidth().absolutePadding(left = 40.dp, right = 40.dp, top = 60.dp, bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearGradient(
            colors = listOf(Color(0xFF9E9E9E), Color(0xFF757575)),
            modifier = Modifier
                .padding(bottom = 25.dp)
                .size(120.dp)
                .rnShadow(8.dp, CircleShape, Color(0xFF9E9E9E)),
            shape = CircleShape
        ) {
            MaterialIcons("brush", size = 60.dp, color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
        AppText(
            text = "نمونه کار یافت نشد",
            fontSize = 22.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = DetailDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        AppText(
            text = "نمونه کار مورد نظر موجود نیست",
            fontSize = 16.sp,
            fontFamily = RNFonts.Yekan_Bakh_Regular,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

@Composable
internal fun PortfolioActionSheet(
    visible: Boolean,
    slide: Float,
    backdrop: Float,
    deleting: Boolean,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    if (!visible && backdrop == 0f && slide == 0f) return
    val density = androidx.compose.ui.platform.LocalDensity.current
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .alpha(backdrop)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .touchableOpacity(activeOpacity = 1f, onClick = onClose)
            )
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .graphicsLayer { translationY = with(density) { ((1f - slide) * 300).dp.toPx() } }
                    .rnShadow(10.dp, RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                    .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                    .background(Color.White)
                    .padding(top = 15.dp, bottom = 35.dp)
                    .absolutePadding(left = 20.dp, right = 20.dp)
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 20.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color(0xFFE0E0E0), RoundedCornerShape(2.dp))
                )
                AppText(
                    text = "عملیات نمونه کار",
                    fontSize = 18.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = DetailDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 25.dp)
                )
                ActionRow(DetailInfo, "edit", "ویرایش نمونه کار", "ویرایش اطلاعات و تصاویر") {
                    onClose()
                    onEdit()
                }
                ActionRow(DetailError, "delete", "حذف نمونه کار", "حذف کامل از سیستم", enabled = !deleting, onClick = onDelete)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .border(1.dp, FarimodColors.Danger, RoundedCornerShape(15.dp))
                        .background(Color(0xFFFFCDCD), RoundedCornerShape(15.dp))
                        .touchableOpacity(onClick = onClose)
                        .absolutePadding(top = 15.dp, bottom = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AppText(
                        text = "لغو",
                        fontSize = 16.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = FarimodColors.Danger
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionRow(
    color: Color,
    icon: String,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    RowReverse(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .border(1.dp, Color(0xFFE9ECEF), RoundedCornerShape(15.dp))
            .background(Color(0xFFF8F9FA), RoundedCornerShape(15.dp))
            .touchableOpacity(enabled = enabled, onClick = onClick)
            .absolutePadding(left = 10.dp, right = 10.dp, top = 15.dp, bottom = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .absolutePadding(left = 15.dp)
                .size(45.dp)
                .background(color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MaterialIcons(icon, size = 22.dp, color = Color.White)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
            AppText(title, fontSize = 16.sp, fontFamily = RNFonts.Yekan_Bakh_Bold, color = DetailDark)
            AppText(subtitle, fontSize = 13.sp, fontFamily = RNFonts.Yekan_Bakh_Regular, color = Color(0xFF6C757D))
        }
    }
}

@Composable
internal fun PortfolioDeleteSheet(
    visible: Boolean,
    slide: Float,
    backdrop: Float,
    deleting: Boolean,
    onClose: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!visible && backdrop == 0f && slide == 0f) return
    val density = androidx.compose.ui.platform.LocalDensity.current
    Dialog(
        onDismissRequest = { if (!deleting) onClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .alpha(backdrop)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .touchableOpacity(activeOpacity = 1f, enabled = !deleting, onClick = onClose)
            )
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .graphicsLayer { translationY = with(density) { ((1f - slide) * 300).dp.toPx() } }
                    .rnShadow(10.dp, RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                    .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                    .background(Color.White)
                    .padding(top = 15.dp, bottom = 35.dp)
                    .absolutePadding(left = 25.dp, right = 25.dp)
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 20.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color(0xFFE0E0E0), RoundedCornerShape(2.dp))
                )
                Column(Modifier.fillMaxWidth().padding(bottom = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .padding(bottom = 20.dp)
                            .size(70.dp)
                            .rnShadow(8.dp, CircleShape, DetailError)
                            .background(DetailError, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        MaterialIcons("warning", size = 32.dp, color = Color.White)
                    }
                    AppText(
                        text = "تأیید حذف",
                        fontSize = 20.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = DetailDark,
                        modifier = Modifier.padding(bottom = 15.dp)
                    )
                    AppText(
                        text = "آیا از حذف این نمونه کار اطمینان دارید؟\nاین عمل قابل بازگشت نیست.",
                        fontSize = 16.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        color = Color(0xFF6C757D),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                }
                RowReverse(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .border(1.dp, Color(0xFFE9ECEF), RoundedCornerShape(15.dp))
                            .background(Color(0xFFF8F9FA), RoundedCornerShape(15.dp))
                            .touchableOpacity(enabled = !deleting, onClick = onClose)
                            .absolutePadding(top = 16.dp, bottom = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppText(
                            text = "لغو",
                            fontSize = 16.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            color = Color(0xFF6C757D)
                        )
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .rnShadow(8.dp, RoundedCornerShape(15.dp), DetailError)
                            .clip(RoundedCornerShape(15.dp))
                            .touchableOpacity(enabled = !deleting, onClick = onConfirm)
                    ) {
                        LinearGradient(
                            colors = listOf(DetailError, Color(0xFFC0392B)),
                            modifier = Modifier.fillMaxWidth(),
                            start = Offset(0f, 0f),
                            end = Offset(1f, 1f)
                        ) {
                            RowReverse(
                                Modifier.fillMaxWidth().absolutePadding(left = 20.dp, right = 20.dp, top = 16.dp, bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MaterialIcons(
                                    if (deleting) "hourglass-empty" else "delete-forever",
                                    size = 20.dp,
                                    color = Color.White
                                )
                                AppText(
                                    text = if (deleting) "در حال حذف..." else "بله، حذف کن",
                                    fontSize = 16.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PortfolioImageDialog(
    visible: Boolean,
    images: List<String>,
    showDefault: Boolean,
    initialIndex: Int,
    onClose: () -> Unit
) {
    if (!visible) return
    val screenW = windowSize().first
    val statusTop = statusBarHeight()
    var indicator by remember(initialIndex) { mutableStateOf(initialIndex) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f))) {
            Box(Modifier.fillMaxSize().touchableOpacity(activeOpacity = 1f, onClick = onClose))
            Column(Modifier.fillMaxSize().padding(top = statusTop + 20.dp)) {
                RowReverse(
                    Modifier.fillMaxWidth().absolutePadding(left = 20.dp, right = 20.dp, top = 15.dp, bottom = 15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppText(
                        text = "گالری تصاویر",
                        fontSize = 20.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color.White
                    )
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .touchableOpacity(onClick = onClose),
                        contentAlignment = Alignment.Center
                    ) {
                        MaterialIcons("close", size = 24.dp, color = Color.White)
                    }
                }
                if (images.isNotEmpty() && !showDefault) {
                    val pagerState = rememberPagerState { images.size.coerceAtLeast(1) }
                    LaunchedEffect(pagerState) {
                        snapshotFlow { pagerState.currentPage }.drop(1).collect { indicator = it }
                    }
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().weight(1f)) { page ->
                        Box(Modifier.fillMaxSize().absolutePadding(left = 20.dp, right = 20.dp), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(Color.White.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                PortfolioDetailRemoteImage(images[page], Modifier.fillMaxSize())
                            }
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().absolutePadding(top = 20.dp, bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        images.forEachIndexed { index, _ ->
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .background(
                                        if (index == indicator) Color.White else Color.White.copy(alpha = 0.3f),
                                        CircleShape
                                    )
                            )
                        }
                    }
                } else {
                    Box(Modifier.fillMaxWidth().weight(1f).absolutePadding(left = 20.dp, right = 20.dp), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            PortfolioDefaultImage(Modifier.fillMaxSize())
                        }
                    }
                }
            }
        }
    }
}

private fun argb(r: Int, g: Int, b: Int, a: Int): Color = Color((a shl 24) or (r shl 16) or (g shl 8) or b)
