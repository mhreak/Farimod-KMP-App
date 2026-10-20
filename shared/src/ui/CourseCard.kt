package ir.farimod.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import ir.farimod.app.components.icons.MaterialCommunityIcons
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.network.ApiConfig
import ir.farimod.app.network.get
import ir.farimod.app.resources.Res
import ir.farimod.app.resources.new_course
import ir.farimod.app.utils.formatPrice
import ir.farimod.app.utils.jsToString
import ir.farimod.app.utils.jsTruthy
import ir.farimod.app.utils.safeNumber
import ir.farimod.app.utils.safeString
import ir.farimod.app.utils.toPersianDigits
import kotlinx.serialization.json.JsonElement
import kotlin.math.floor

/** Colour/icon/text of the course type badge (RN `getCourseTypeInfo`). */
private class CourseTypeInfo(val text: String, val isPresential: Boolean, val color: Color, val icon: String)

private fun getCourseTypeInfo(type: Double): CourseTypeInfo = when (type) {
    1.0 -> CourseTypeInfo("حضوری", true, Color(0xFF4CAF50), "group")
    2.0 -> CourseTypeInfo("مجازی", false, Color(0xFF2196F3), "computer")
    3.0 -> CourseTypeInfo("حضوری و مجازی", true, Color(0xFFFF9800), "swap-horiz")
    else -> CourseTypeInfo("نامشخص", false, Color(0xFF9E9E9E), "help-outline")
}

/**
 * RN `<Image source={uri && !imageError ? { uri } : require("new_course.jpg")} resizeMode="cover" onError=... />`
 * Used by CourseCard and CourseRegistrationCard (fills its parent).
 */
@Composable
internal fun CourseCardCoverImage(uri: String?) {
    var imageError by remember(uri) { mutableStateOf(false) }
    val resolved = if (!uri.isNullOrEmpty() && !imageError) ApiConfig.resolveMediaUrl(uri) else null
    if (resolved != null) {
        AsyncImage(
            model = resolved,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ResizeMode.cover,
            onError = { imageError = true }
        )
    } else {
        LocalImage(Res.drawable.new_course, Modifier.fillMaxSize(), ResizeMode.cover)
    }
}

/** RN `borderTopWidth: 1` (the 1dp line is drawn inside the top 1dp of the element). */
internal fun Modifier.courseCardTopBorder(color: Color): Modifier = this.drawBehind {
    drawRect(color, size = Size(size.width, 1.dp.toPx()))
}

/**
 * Port of `app/components/CourseCard.tsx`.
 *
 * @param course the API course JSON object (RN plain `course` object); null renders nothing
 * @param onPress RN `onPress(course)`
 */
@Composable
fun CourseCard(
    course: JsonElement?,
    onPress: ((JsonElement) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (course == null) {
        return
    }

    val price = safeNumber(course["RegisterAmount"])
    val specialPrice = safeNumber(course["SpecialSalePrice"])
    val discountPercentage = if (specialPrice > 0 && price > 0)
        floor(((price - specialPrice) / price) * 100 + 0.5).toInt() // Math.round
    else 0

    val courseTypeNumber = safeNumber(course["CourseType"])
    val courseTypeInfo = getCourseTypeInfo(courseTypeNumber)
    val courseType = courseTypeInfo.text

    val featuredImage = course["FeaturedImageURL"]
    val featuredImageUrl = if (jsTruthy(featuredImage)) jsToString(featuredImage) else null

    val shape = RoundedCornerShape(16.dp)
    val density = LocalDensity.current

    // container
    Box(
        modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .touchableOpacity(activeOpacity = 0.8f) { onPress?.invoke(course) }
            .heightIn(min = 410.dp)
            .rnShadow(4.dp, shape, Color(0xFF797979))
            .clip(shape)
            .background(colors.White)
    ) {
        Column(Modifier.fillMaxWidth()) {
            // imageContainer
            Box(Modifier.fillMaxWidth().height(250.dp)) {
                CourseCardCoverImage(featuredImageUrl)

                if (discountPercentage > 0) {
                    Box(
                        Modifier
                            .align(AbsoluteAlignment.TopRight)
                            .absolutePadding(top = 8.dp, right = 8.dp)
                            .zIndex(2f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFFF6B6B))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        AppText(
                            text = "${toPersianDigits(discountPercentage.toString())}% تخفیف",
                            fontSize = 9.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            color = Color.White
                        )
                    }
                }

                // typeBadge
                RowReverse(
                    Modifier
                        .align(AbsoluteAlignment.TopLeft)
                        .absolutePadding(top = 8.dp, left = 8.dp)
                        .zIndex(2f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(courseTypeInfo.color)
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MaterialIcons(courseTypeInfo.icon, size = 12.dp, color = Color(0xFFFFFFFF))
                    AppText(
                        text = courseType,
                        modifier = Modifier.absolutePadding(right = 3.dp),
                        fontSize = 8.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color.White
                    )
                }

                if (!jsTruthy(course["Active"])) {
                    // inactiveCourseBadge
                    Column(
                        Modifier
                            .fillMaxSize()
                            .zIndex(4f)
                            .background(rgba(231, 76, 60, 0.85f)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        MaterialIcons("block", size = 16.dp, color = Color(0xFFFFFFFF))
                        Text(
                            text = "دوره غیرفعال",
                            modifier = Modifier.padding(top = 8.dp),
                            style = rnTextStyle(
                                fontSize = 14.sp,
                                color = Color(0xFFFFFFFF),
                                fontFamily = RNFonts.Yekan_Bakh_Bold
                            ).copy(
                                shadow = Shadow(
                                    color = Color(0f, 0f, 0f, 0.3f),
                                    offset = Offset(0f, with(density) { 1.dp.toPx() }),
                                    blurRadius = with(density) { 2.dp.toPx() }
                                )
                            )
                        )
                    }
                }

                if (jsTruthy(course["Active"]) && !jsTruthy(course["RegisterActive"])) {
                    // inactiveRegisterBadge
                    Column(
                        Modifier
                            .fillMaxSize()
                            .zIndex(3f)
                            .background(rgba(0, 0, 0, 0.7f)),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        MaterialIcons("close", size = 16.dp, color = Color(0xFFFFFFFF))
                        AppText(
                            text = "ثبت‌نام غیرفعال",
                            modifier = Modifier.padding(top = 8.dp),
                            fontSize = 13.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            color = Color(0xFFFFFFFF)
                        )
                    }
                }
            }

            // courseDetails
            Column(
                Modifier
                    .fillMaxWidth()
                    .absolutePadding(left = 12.dp, right = 12.dp, top = 12.dp, bottom = 10.dp)
            ) {
                // courseHeader
                RowReverse(
                    Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // categoryIcon
                    Box(
                        Modifier
                            .absolutePadding(left = 8.dp)
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.PrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        MaterialCommunityIcons("school", size = 18.dp, color = colors.Primary)
                    }
                    // headerContent
                    Column(Modifier.weight(1f)) {
                        AppText(
                            text = safeString(course["CourseName"], "نام دوره نامشخص"),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF333333),
                            textAlign = TextAlign.Right,
                            lineHeight = 18.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // priceSection
                        Column(
                            Modifier.fillMaxWidth().padding(top = 3.dp, bottom = 5.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            if (discountPercentage > 0) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    AppText(
                                        text = if (price <= 0.0) "رایگان" else formatPrice(price),
                                        modifier = Modifier.padding(bottom = 2.dp),
                                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                                        fontSize = 9.sp,
                                        color = Color(0xFF999999),
                                        textAlign = TextAlign.Right,
                                        textDecoration = TextDecoration.LineThrough
                                    )
                                    AppText(
                                        text = if (specialPrice <= 0.0) "رایگان" else formatPrice(specialPrice),
                                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFFFF6B6B),
                                        textAlign = TextAlign.Right
                                    )
                                }
                            } else {
                                AppText(
                                    text = if (price <= 0.0) "رایگان" else formatPrice(price),
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    fontSize = 12.sp,
                                    color = if (price <= 0.0) Color(0xFF10B981) else colors.Primary,
                                    textAlign = TextAlign.Right
                                )
                            }
                        }
                    }
                }

                // courseInfo
                Column(Modifier.fillMaxWidth()) {
                    // locationSection
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 3.dp)
                            .courseCardTopBorder(colors.Light)
                            .padding(top = 6.dp) // borderTopWidth 1 + paddingTop 5
                    ) {
                        // locationHeader
                        RowReverse(
                            Modifier.fillMaxWidth().padding(bottom = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MaterialIcons(
                                "place", size = 14.dp, color = colors.Primary,
                                modifier = Modifier.absolutePadding(left = 4.dp)
                            )
                            AppText(
                                text = "محل برگزاری:",
                                fontFamily = RNFonts.Yekan_Bakh_Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF333333)
                            )
                        }
                        AppText(
                            text = safeString(course["CourseAddress"], "اطلاعات محل برگزاری موجود نیست"),
                            modifier = Modifier.fillMaxWidth().absolutePadding(right = 18.dp),
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            fontSize = 11.sp,
                            color = Color(0xFFA0A0A0),
                            textAlign = TextAlign.Right,
                            lineHeight = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
