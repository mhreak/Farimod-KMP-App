package ir.farimod.app.screens.portfolio

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.network.Api
import ir.farimod.app.network.ApiNetworkException
import ir.farimod.app.network.double
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.list
import ir.farimod.app.network.long
import ir.farimod.app.network.str
import ir.farimod.app.resources.Res
import ir.farimod.app.resources.portfolio_icon
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LocalImage
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.ResizeMode
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.rgba
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.utils.jsToFixed
import ir.farimod.app.utils.jsToString
import ir.farimod.app.utils.jsTruthy
import ir.farimod.app.utils.toPersianDigits
import kotlinx.coroutines.CancellationException
import ir.farimod.app.network.ApiConfig
import kotlinx.serialization.json.JsonObject

internal val PortfolioPrimary = Color(0xFF667EEA)
internal val PortfolioSuccess = Color(0xFF2ECC71)
internal val PortfolioWarning = Color(0xFFF39C12)
internal val PortfolioGold = Color(0xFFFFD700)
internal val PortfolioDark = Color(0xFF2C3E50)
internal val PortfolioMuted = Color(0xFF666666)
internal val PortfolioGreyIcon = Color(0xFF9E9E9E)
internal val PortfolioPageBg = Color(0xFFF8FAFC)
internal val PortfolioIndigo = Color(0xFF6366F1)

internal const val PORTFOLIO_PAGE_SIZE = 20

/**
 * Shared infinite list for AllPortfolio and PortfolioList.
 * Mirrors `usePortfoliosWithInfiniteLoading` (page append, hasMore, page-1 wipe on error).
 */
internal class PortfolioFeed {
    var items by mutableStateOf<List<JsonObject>>(emptyList())
    var page by mutableStateOf(1)
    var hasMore by mutableStateOf(true)
    var total by mutableStateOf(0)
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var filters by mutableStateOf<Map<String, Any?>>(emptyMap())

    suspend fun fetch(newPage: Int, pageSize: Int, query: Map<String, Any?>, nextFilters: Map<String, Any?> = filters) {
        loading = true
        error = null
        try {
            val response = try {
                Api.get("Portfolio/GetAll", query = query)
            } catch (e: CancellationException) {
                throw e
            } catch (e: ApiNetworkException) {
                throw IllegalStateException(e.message ?: "Network request failed", e)
            }
            if (!response.ok) throw IllegalStateException("HTTP error! status: ${response.status}")
            val result = response.json ?: throw IllegalStateException("HTTP error! status: ${response.status}")
            val chunk = result.list("Data").mapNotNull { it as? JsonObject }
            items = if (newPage == 1) chunk else items + chunk
            total = result["Total"].int ?: result["Total"].double?.toInt() ?: 0
            page = newPage
            filters = nextFilters
            hasMore = chunk.size == pageSize && chunk.isNotEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message
            if (newPage == 1) items = emptyList()
            total = 0
        } finally {
            loading = false
        }
    }
}

internal fun allPortfolioQuery(page: Int, pageSize: Int, filters: Map<String, Any?>): Map<String, Any?> {
    val query = LinkedHashMap<String, Any?>()
    val title = filters["filterTitle"]
    if (jsTruthy(title)) query["filterTitle"] = jsToString(title)
    val member = filters["filterMemberId"]
    if (jsTruthy(member)) query["filterMemberId"] = jsToString(member)
    query["currentPage"] = page
    query["pageSize"] = pageSize
    return query
}

internal fun myPortfolioQuery(page: Int, pageSize: Int, memberId: Any?): Map<String, Any?> {
    val query = LinkedHashMap<String, Any?>()
    if (jsTruthy(memberId)) query["filterMemberId"] = jsToString(memberId)
    query["currentPage"] = page
    query["pageSize"] = pageSize
    return query
}

/** `item.PotfolioId || item.PortfolioId || item.index || 1` */
internal fun portfolioPressId(item: JsonObject): Any {
    val pot = item["PotfolioId"]
    if (jsTruthy(pot)) return jsonId(pot)
    val port = item["PortfolioId"]
    if (jsTruthy(port)) return jsonId(port)
    val index = item["index"]
    if (jsTruthy(index)) return jsonId(index)
    return 1
}

private fun jsonId(el: kotlinx.serialization.json.JsonElement?): Any {
    val n = el.long ?: return el.str ?: 1
    return if (n in Int.MIN_VALUE..Int.MAX_VALUE) n.toInt() else n
}

internal fun portfolioItemKey(item: JsonObject, index: Int): String {
    val pot = item["PotfolioId"]
    if (jsTruthy(pot)) return jsToString(pot)
    val port = item["PortfolioId"]
    if (jsTruthy(port)) return jsToString(port)
    return "portfolio-$index"
}

@Composable
internal fun PortfolioCardImage(item: JsonObject) {
    val url = item.str("FeaturedImageURL")
    var imageError by remember(url) { mutableStateOf(false) }
    val hasValid = !url.isNullOrBlank() && !url.trim().endsWith("/") && url.trim().isNotEmpty()
    Box(Modifier.fillMaxWidth().height(140.dp)) {
        if (!hasValid || imageError) {
            Box(
                Modifier.fillMaxSize().background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                LocalImage(Res.drawable.portfolio_icon, Modifier.fillMaxSize(), ResizeMode.cover)
            }
        } else {
            val resolved = ApiConfig.resolveMediaUrl(url)
            if (resolved == null) {
                Box(Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
                    LocalImage(Res.drawable.portfolio_icon, Modifier.fillMaxSize(), ResizeMode.cover)
                }
            } else {
                AsyncImage(
                    model = resolved,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = { imageError = true }
                )
            }
        }
    }
}

@Composable
internal fun PortfolioGridCard(
    item: JsonObject,
    showStatus: Boolean,
    onPress: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(270.dp)
            .rnShadow(3.dp, shape)
            .clip(shape)
            .background(Color.White)
            .touchableOpacity(activeOpacity = 0.8f, onClick = onPress)
    ) {
        Column(Modifier.fillMaxSize()) {
            PortfolioCardImage(item)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    AppText(
                        text = toPersianDigits(item["Title"]),
                        fontSize = 15.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = PortfolioDark,
                        textAlign = TextAlign.Right,
                        lineHeight = 22.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().absolutePadding(bottom = 6.dp)
                    )
                    val description = item.str("Description")?.takeIf { it.isNotEmpty() } ?: "بدون توضیحات"
                    AppText(
                        text = toPersianDigits(description),
                        fontSize = 12.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        color = PortfolioMuted,
                        textAlign = TextAlign.Right,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().absolutePadding(bottom = 10.dp)
                    )
                }
                RowReverse(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RowReverse(verticalAlignment = Alignment.CenterVertically) {
                        MaterialIcons("calendar-month", size = 14.dp, color = PortfolioMuted)
                        AppText(
                            text = toPersianDigits(item.str("ShamsiInsertDate") ?: ""),
                            fontSize = 11.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                            color = PortfolioMuted,
                            modifier = Modifier.absolutePadding(right = 4.dp)
                        )
                    }
                    val ratingEl = item["Rating"]
                    if (jsTruthy(ratingEl)) {
                        val ratingValue = ratingEl.double ?: 0.0
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            MaterialIcons("star", size = 14.dp, color = PortfolioGold)
                            AppText(
                                text = toPersianDigits(jsToFixed(ratingValue, 1)),
                                fontSize = 11.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Bold,
                                color = PortfolioMuted,
                                modifier = Modifier.absolutePadding(left = 3.dp)
                            )
                        }
                    }
                }
            }
        }
        val likes = item["LikeCount"].int ?: item["LikeCount"].double?.toInt() ?: 0
        Row(
            Modifier
                .align(AbsoluteAlignment.TopLeft)
                .absolutePadding(left = 8.dp, top = 8.dp)
                .background(rgba(233, 30, 99, 0.8f), RoundedCornerShape(15.dp))
                .absolutePadding(left = 6.dp, right = 6.dp, top = 3.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            MaterialIcons("favorite", size = 14.dp, color = Color.White)
            AppText(
                text = toPersianDigits(likes.toString()),
                fontSize = 11.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = Color.White
            )
        }
        if (showStatus) {
            val active = jsTruthy(item["Active"])
            Box(
                Modifier
                    .align(AbsoluteAlignment.TopRight)
                    .absolutePadding(top = 8.dp, right = 8.dp)
                    .background(if (active) PortfolioSuccess else PortfolioWarning, RoundedCornerShape(10.dp))
                    .absolutePadding(left = 6.dp, right = 6.dp, top = 3.dp, bottom = 3.dp)
            ) {
                AppText(
                    text = if (active) "منتشر شده" else "پیش نویس",
                    fontSize = 10.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
internal fun PortfolioGridSkeleton(height: Dp, pulseOpacity: Boolean) {
    val transition = rememberInfiniteTransition()
    val anim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse)
    )
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .height(height)
            .rnShadow(3.dp, shape)
            .clip(shape)
            .background(Color.White)
    ) {
        Box(Modifier.fillMaxWidth().height(140.dp).padding(0.dp)) {
            PortfolioBone(
                Modifier.fillMaxSize(),
                radius = 12.dp,
                pulse = anim,
                pulseOpacity = pulseOpacity
            )
        }
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            PortfolioBone(
                Modifier.fillMaxWidth(0.9f).align(Alignment.Start).padding(bottom = 8.dp).height(16.dp),
                pulse = anim,
                pulseOpacity = pulseOpacity
            )
            PortfolioBone(
                Modifier.fillMaxWidth(0.7f).align(Alignment.Start).padding(bottom = 8.dp).height(14.dp),
                pulse = anim,
                pulseOpacity = pulseOpacity
            )
            RowReverse(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RowReverse(verticalAlignment = Alignment.CenterVertically) {
                    PortfolioBone(Modifier.size(14.dp).absolutePadding(left = 4.dp), radius = 7.dp, pulse = anim, pulseOpacity = pulseOpacity)
                    PortfolioBone(Modifier.width(60.dp).height(12.dp), pulse = anim, pulseOpacity = pulseOpacity)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PortfolioBone(Modifier.size(14.dp).absolutePadding(right = 4.dp), radius = 7.dp, pulse = anim, pulseOpacity = pulseOpacity)
                    PortfolioBone(Modifier.width(25.dp).height(12.dp), pulse = anim, pulseOpacity = pulseOpacity)
                }
            }
        }
    }
}

@Composable
private fun PortfolioBone(
    modifier: Modifier,
    radius: Dp = 8.dp,
    pulse: Float,
    pulseOpacity: Boolean
) {
    val color = if (pulseOpacity) Color(0xFFE0E0E0) else lerp(Color(0xFFE0E0E0), Color(0xFFF0F0F0), pulse)
    Box(
        modifier
            .alpha(if (pulseOpacity) 0.4f + 0.6f * pulse else 1f)
            .clip(RoundedCornerShape(radius))
            .background(color)
    )
}

@Composable
internal fun PortfolioLoadingFooter() {
    Row(
        Modifier.fillMaxWidth().padding(20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = PortfolioPrimary,
            strokeWidth = 2.dp
        )
        AppText(
            text = "در حال بارگذاری...",
            fontSize = 14.sp,
            fontFamily = RNFonts.Yekan_Bakh_Regular,
            color = PortfolioMuted,
            modifier = Modifier.absolutePadding(left = 10.dp)
        )
    }
}

@Composable
internal fun PortfolioEmptyState() {
    Column(
        Modifier.fillMaxWidth().absolutePadding(left = 40.dp, right = 40.dp, top = 60.dp, bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MaterialIcons("brush", size = 80.dp, color = PortfolioGreyIcon)
        AppText(
            text = "هیچ نمونه کاری موجود نیست",
            fontSize = 20.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = PortfolioDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp)
        )
        AppText(
            text = "در حال حاضر نمونه کاری برای نمایش وجود ندارد",
            fontSize = 16.sp,
            fontFamily = RNFonts.Yekan_Bakh_Regular,
            color = PortfolioGreyIcon,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
internal fun PortfolioErrorState(onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().absolutePadding(left = 40.dp, right = 40.dp, top = 60.dp, bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MaterialIcons("error", size = 80.dp, color = PortfolioGreyIcon)
        AppText(
            text = "خطا در دریافت اطلاعات",
            fontSize = 20.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = PortfolioDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp)
        )
        AppText(
            text = "لطفاً اتصال اینترنت خود را بررسی کنید",
            fontSize = 16.sp,
            fontFamily = RNFonts.Yekan_Bakh_Regular,
            color = PortfolioGreyIcon,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.padding(top = 12.dp)
        )
        RowReverse(
            Modifier
                .padding(top = 24.dp)
                .rnShadow(8.dp, RoundedCornerShape(25.dp), PortfolioPrimary)
                .clip(RoundedCornerShape(25.dp))
                .background(PortfolioPrimary)
                .touchableOpacity(onClick = onRetry)
                .absolutePadding(left = 20.dp, right = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MaterialIcons("refresh", size = 20.dp, color = FarimodColors.White)
            AppText(
                text = "تلاش مجدد",
                fontSize = 16.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = FarimodColors.White,
                modifier = Modifier.absolutePadding(right = 8.dp)
            )
        }
    }
}

@Composable
internal fun PortfolioCircleButton(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    background: Color = Color.White.copy(alpha = 0.9f),
    elevation: Dp = 5.dp,
    shadowColor: Color = Color.Black,
    onClick: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    Box(
        modifier
            .size(size)
            .rnShadow(elevation, CircleShape, shadowColor)
            .clip(CircleShape)
            .background(background)
            .touchableOpacity(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
