package ir.farimod.app.screens.userprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.network.bool
import ir.farimod.app.network.double
import ir.farimod.app.network.get
import ir.farimod.app.network.str
import ir.farimod.app.resources.Res
import ir.farimod.app.resources.blog_post_icon
import ir.farimod.app.resources.main_icon
import ir.farimod.app.resources.new_course
import ir.farimod.app.resources.portfolio_icon
import ir.farimod.app.resources.product_icon
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.LocalImage
import ir.farimod.app.ui.NetworkImage
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.ResizeMode
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.utils.formatPrice
import ir.farimod.app.utils.safeNumber
import ir.farimod.app.utils.safeString
import ir.farimod.app.utils.toPersianDigits
import kotlinx.serialization.json.JsonElement
import kotlin.math.roundToInt

private val CardShape20 = RoundedCornerShape(20.dp)

@Composable
private fun UpRemoteOrLocal(
    url: String?,
    fallback: org.jetbrains.compose.resources.DrawableResource,
    modifier: Modifier
) {
    if (url != null) {
        NetworkImage(url, modifier, resizeMode = ResizeMode.cover)
    } else {
        LocalImage(fallback, modifier, resizeMode = ResizeMode.cover)
    }
}

private fun imageUrl(item: JsonElement, fileKey: String, endpoint: String): String? {
    val file = item[fileKey].str
    if (!file.isNullOrEmpty()) return upApi(endpoint + file)
    val featured = item["FeaturedImageURL"].str
    if (!featured.isNullOrEmpty()) return featured
    return null
}

@Composable
internal fun UpCourseCard(item: JsonElement, onPress: (JsonElement) -> Unit) {
    Column(
        Modifier
            .width(300.dp)
            .padding(bottom = 20.dp)
            .rnShadow(2.dp, CardShape20)
            .clip(CardShape20)
            .background(Color.White)
            .touchableOpacity(activeOpacity = 0.8f) { onPress(item) }
    ) {
        UpRemoteOrLocal(
            imageUrl(item, "CourseImageFileName", "Course/GetCourseImage/"),
            Res.drawable.new_course,
            Modifier.fillMaxWidth().height(140.dp)
        )
        Column(Modifier.padding(15.dp)) {
            AppText(
                text = safeString(item["CourseName"].str, "نام دوره"),
                fontSize = 16.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = UpDark,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            RowReverse(
                Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText(
                    text = coursePrice(item),
                    fontSize = 14.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = UpSuccess,
                    modifier = Modifier.absolutePadding(right = 5.dp)
                )
            }
        }
    }
}

private fun coursePrice(item: JsonElement): String {
    val special = item["SpecialSalePrice"].double ?: 0.0
    val price = item["Price"].double ?: 0.0
    val register = item["RegisterAmount"].double ?: 0.0
    return when {
        special > 0.0 -> formatPrice(special)
        price > 0.0 -> formatPrice(price)
        register > 0.0 -> formatPrice(register)
        else -> "رایگان"
    }
}

@Composable
internal fun UpProductCard(item: JsonElement, onPress: (JsonElement) -> Unit) {
    val price = safeNumber(item["Price"].double)
    val special = safeNumber(item["SpecialSalePrice"].double)
    val discount = if (special > 0.0 && price > 0.0) {
        (((price - special) / price) * 100.0).roundToInt()
    } else 0
    Column(
        Modifier
            .width(180.dp)
            .padding(bottom = 20.dp)
            .rnShadow(2.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .touchableOpacity(activeOpacity = 0.8f) { onPress(item) }
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFFF5F5F5))) {
            UpRemoteOrLocal(
                imageUrl(item, "ProductImageFileName", "Product/GetProductImage/"),
                Res.drawable.product_icon,
                Modifier.fillMaxSize()
            )
            if (discount > 0) {
                Box(
                    Modifier
                        .align(AbsoluteAlignment.TopRight)
                        .absolutePadding(top = 8.dp, right = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFF6B6B))
                        .absolutePadding(left = 6.dp, right = 6.dp, top = 3.dp, bottom = 3.dp)
                ) {
                    AppText(
                        text = "${toPersianDigits(discount.toString())}% تخفیف",
                        fontSize = 10.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
            if (!item["Active"].bool) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                    AppText(
                        text = "ناموجود",
                        fontSize = 14.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Column(Modifier.padding(12.dp).heightIn(min = 85.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            AppText(
                text = safeString(item["ProductName"].str, "نام محصول"),
                fontSize = 13.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = Color(0xFF333333),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            )
            Column(Modifier.fillMaxWidth().padding(bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
internal fun UpGalleryCard(item: JsonElement, onPress: (JsonElement) -> Unit) {
    Box(
        Modifier
            .padding(bottom = 20.dp)
            .size(120.dp)
            .rnShadow(2.dp, RoundedCornerShape(15.dp))
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White)
            .touchableOpacity(activeOpacity = 0.8f) { onPress(item) }
    ) {
        UpRemoteOrLocal(
            imageUrl(item, "ImageFileName", "ImageGallery/GetImage/"),
            Res.drawable.main_icon,
            Modifier.fillMaxSize()
        )
        RowReverse(
            Modifier
                .align(AbsoluteAlignment.TopRight)
                .absolutePadding(top = 8.dp, right = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .absolutePadding(left = 8.dp, right = 8.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MaterialIcons(name = "favorite", size = 16.dp, color = Color(0xFFFF4757))
            AppText(
                text = toPersianDigits(item["Likes"].str ?: item["Likes"].double?.let { jsCount(it) } ?: "0"),
                fontSize = 11.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = Color.White,
                modifier = Modifier.absolutePadding(right = 4.dp)
            )
        }
    }
}

private fun jsCount(value: Double): String {
    val asLong = value.toLong()
    return if (value == asLong.toDouble()) asLong.toString() else value.toString()
}

@Composable
internal fun UpBlogCard(item: JsonElement, onPress: (JsonElement) -> Unit) {
    Column(
        Modifier
            .width(280.dp)
            .padding(bottom = 20.dp)
            .rnShadow(1.dp, CardShape20)
            .clip(CardShape20)
            .background(Color.White)
            .touchableOpacity(activeOpacity = 0.8f) { onPress(item) }
    ) {
        UpBlogImage(item)
        Column(Modifier.padding(16.dp)) {
            AppText(
                text = safeString(item["Title"].str, "عنوان مقاله"),
                fontSize = 16.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = Color(0xFF2C3E50),
                textAlign = TextAlign.Right,
                lineHeight = 24.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
            RowReverse(
                Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RowReverse(verticalAlignment = Alignment.CenterVertically) {
                    MaterialIcons(name = "calendar-month", size = 16.dp, color = Color(0xFF666666))
                    AppText(
                        text = toPersianDigits(item["ShamsiInsertDate"].str ?: ""),
                        fontSize = 12.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        color = Color(0xFF666666),
                        modifier = Modifier.absolutePadding(right = 6.dp)
                    )
                }
                RowReverse(verticalAlignment = Alignment.CenterVertically) {
                    MaterialIcons(name = "favorite", size = 16.dp, color = Color(0xFFFF6B6B))
                    AppText(
                        text = toPersianDigits(jsCount(item["LikeCount"].double ?: 0.0)),
                        fontSize = 12.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        color = Color(0xFF666666),
                        modifier = Modifier.absolutePadding(right = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun UpBlogImage(item: JsonElement) {
    val file = item["FeaturedImageFileName"].str
    val url = item["FeaturedImageURL"].str
    val valid = !file.isNullOrEmpty() && !url.isNullOrEmpty() && !url.endsWith("/")
    var imageError by remember(url) { mutableStateOf(false) }
    var imageLoading by remember(url) { mutableStateOf(true) }
    if (!valid || imageError) {
        Box(
            Modifier.fillMaxWidth().height(160.dp).background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            LocalImage(Res.drawable.blog_post_icon, Modifier.fillMaxSize(), resizeMode = ResizeMode.cover)
        }
        return
    }
    Box(Modifier.fillMaxWidth().height(160.dp)) {
        if (imageLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .zIndex(1f)
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                MaterialIcons(name = "article", size = 40.dp, color = Color(0xFFCCCCCC))
            }
        }
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onSuccess = { _ ->
                imageLoading = false
                imageError = false
            },
            onError = { _ ->
                imageError = true
                imageLoading = false
            }
        )
    }
}

@Composable
internal fun UpPortfolioCard(item: JsonElement, onPress: (JsonElement) -> Unit) {
    Box(
        Modifier
            .padding(bottom = 20.dp)
            .width(280.dp)
            .height(200.dp)
            .rnShadow(5.dp, CardShape20)
            .clip(CardShape20)
            .background(Color.White)
            .touchableOpacity(activeOpacity = 0.8f) { onPress(item) }
    ) {
        UpRemoteOrLocal(
            imageUrl(item, "ImageFileName", "Portfolio/GetImage/"),
            Res.drawable.portfolio_icon,
            Modifier.fillMaxSize()
        )
        LinearGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxSize()
        ) {
            Column(Modifier.fillMaxSize().padding(15.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.Bottom) {
                AppText(
                    text = safeString(item["Title"].str, "عنوان پروژه"),
                    fontSize = 16.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 5.dp)
                )
            }
        }
    }
}
