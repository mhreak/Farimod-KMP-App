package ir.farimod.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialCommunityIcons
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.network.get
import ir.farimod.app.utils.formatPrice
import ir.farimod.app.utils.jsToString
import ir.farimod.app.utils.jsTruthy
import ir.farimod.app.utils.safeNumber
import ir.farimod.app.utils.safeString
import ir.farimod.app.utils.toPersianDigits
import kotlinx.serialization.json.JsonElement

private class RegistrationBadgeInfo(val text: String, val color: Color, val icon: String)

/** RN `getStatusInfo(statusCode)`. */
private fun getStatusInfo(statusCode: Double): RegistrationBadgeInfo = when (statusCode) {
    1.0 -> RegistrationBadgeInfo("در انتظار پرداخت", Color(0xFFF39C12), "schedule")
    2.0 -> RegistrationBadgeInfo("تسویه شده", Color(0xFF2ECC71), "check-circle")
    3.0 -> RegistrationBadgeInfo("لغو شده", Color(0xFFE74C3C), "cancel")
    4.0 -> RegistrationBadgeInfo("رد شده", Color(0xFF95A5A6), "block")
    else -> RegistrationBadgeInfo("نامشخص", Color(0xFF95A5A6), "help-outline")
}

/** RN `getPaymentTypeInfo(type)`. */
private fun getPaymentTypeInfo(type: Double, paymentTypeStr: String): RegistrationBadgeInfo = when (type) {
    1.0 -> RegistrationBadgeInfo("پرداخت آنلاین", Color(0xFF3498DB), "credit-card")
    2.0 -> RegistrationBadgeInfo("پرداخت نقدی", Color(0xFF27AE60), "money")
    3.0 -> RegistrationBadgeInfo("پرداخت چکی", Color(0xFF8E44AD), "receipt")
    else -> RegistrationBadgeInfo(paymentTypeStr.ifEmpty { "نامشخص" }, Color(0xFF95A5A6), "payment")
}

/**
 * Port of `app/components/CourseRegistrationCard.tsx`.
 *
 * @param registration the API registration JSON object (RN plain `registration` object); null renders nothing
 * @param onPress RN `onPress(registration)`
 */
@Composable
fun CourseRegistrationCard(
    registration: JsonElement?,
    onPress: ((JsonElement) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (registration == null) {
        return
    }

    val courseName = safeString(registration["CourseName"], "نام دوره نامشخص")
    val courseImageRaw = registration["CourseFeaturedImageURL"]
    val courseImage = if (jsTruthy(courseImageRaw)) jsToString(courseImageRaw) else null
    val instructorName = safeString(registration["CourseMemberName"], "مربی نامشخص")
    val registrationDate = registration["ShamsiRegisterDate"]
    val state = safeNumber(registration["State"])
    val totalAmount = safeNumber(registration["TotalAmount"])
    val courseRegisterAmount = safeNumber(registration["CourseRegisterAmount"])
    val discount = safeNumber(registration["Discount"])
    val paymentType = safeNumber(registration["PaymentType"])
    val paymentTypeStr = safeString(registration["PaymentTypeStr"], "")

    val statusInfo = getStatusInfo(state)
    val paymentTypeInfo = getPaymentTypeInfo(paymentType, paymentTypeStr)

    val shape = RoundedCornerShape(16.dp)

    // container
    Box(
        modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .touchableOpacity(activeOpacity = 0.8f) { onPress?.invoke(registration) }
            .heightIn(min = 340.dp)
            .rnShadow(4.dp, shape, Color(0xFF797979))
            .clip(shape)
            .background(colors.White)
    ) {
        Column(Modifier.fillMaxWidth()) {
            // imageContainer
            Box(Modifier.fillMaxWidth().height(160.dp)) {
                CourseCardCoverImage(courseImage)

                // Status Badge
                RowReverse(
                    Modifier
                        .align(AbsoluteAlignment.TopRight)
                        .absolutePadding(top = 8.dp, right = 8.dp)
                        .zIndex(2f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(statusInfo.color)
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MaterialIcons(statusInfo.icon, size = 13.dp, color = Color(0xFFFFFFFF))
                    AppText(
                        text = statusInfo.text,
                        modifier = Modifier.absolutePadding(right = 3.dp),
                        fontSize = 10.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color.White
                    )
                }

                // Payment Type Badge
                RowReverse(
                    Modifier
                        .align(AbsoluteAlignment.TopLeft)
                        .absolutePadding(top = 8.dp, left = 8.dp)
                        .zIndex(2f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(paymentTypeInfo.color)
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MaterialIcons(paymentTypeInfo.icon, size = 12.dp, color = Color(0xFFFFFFFF))
                    AppText(
                        text = paymentTypeInfo.text,
                        modifier = Modifier.absolutePadding(right = 3.dp),
                        fontSize = 9.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color.White
                    )
                }
            }

            // courseDetails
            Column(
                Modifier
                    .fillMaxWidth()
                    .absolutePadding(left = 10.dp, right = 10.dp, top = 10.dp, bottom = 8.dp)
            ) {
                // courseHeader
                RowReverse(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // categoryIcon
                    Box(
                        Modifier
                            .absolutePadding(left = 7.dp)
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.PrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        MaterialCommunityIcons("school", size = 18.dp, color = colors.Primary)
                    }
                    // headerContent
                    Column(Modifier.weight(1f)) {
                        AppText(
                            text = courseName,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF2C3E50),
                            textAlign = TextAlign.Right,
                            lineHeight = 19.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (jsTruthy(registrationDate)) {
                            // dateContainer
                            RowReverse(
                                Modifier.fillMaxWidth().padding(top = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MaterialIcons(
                                    "event", size = 14.dp, color = Color(0xFF4D4D4D),
                                    modifier = Modifier.absolutePadding(left = 4.dp)
                                )
                                AppText(
                                    text = toPersianDigits(registrationDate),
                                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                                    fontSize = 11.sp,
                                    color = Color(0xFF747474),
                                    textAlign = TextAlign.Right
                                )
                            }
                        }
                    }
                }

                // courseInfo
                Column(Modifier.fillMaxWidth()) {
                    // Instructor Section
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                            .courseCardTopBorder(colors.Light)
                            .padding(top = 7.dp) // borderTopWidth 1 + paddingTop 6
                    ) {
                        RowReverse(
                            Modifier.fillMaxWidth().padding(bottom = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MaterialIcons(
                                "person", size = 14.dp, color = colors.Primary,
                                modifier = Modifier.absolutePadding(left = 4.dp)
                            )
                            AppText(
                                text = "مربی:",
                                fontFamily = RNFonts.Yekan_Bakh_Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF34495E)
                            )
                        }
                        AppText(
                            text = instructorName,
                            modifier = Modifier.fillMaxWidth().absolutePadding(right = 17.dp),
                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                            fontSize = 11.sp,
                            color = Color(0xFF7F8C8D),
                            textAlign = TextAlign.Right,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Payment Section
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .courseCardTopBorder(colors.Light)
                            .padding(top = 7.dp) // borderTopWidth 1 + paddingTop 6
                    ) {
                        RowReverse(
                            Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            Arrangement.SpaceBetween,
                            Alignment.CenterVertically
                        ) {
                            RowReverse(verticalAlignment = Alignment.CenterVertically) {
                                MaterialIcons(
                                    "attach-money", size = 14.dp, color = colors.Primary,
                                    modifier = Modifier.absolutePadding(left = 4.dp)
                                )
                                AppText(
                                    text = "مبلغ دوره:",
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFF34495E)
                                )
                            }
                            AppText(
                                text = formatPrice(courseRegisterAmount),
                                fontFamily = RNFonts.Yekan_Bakh_Bold,
                                fontSize = 11.sp,
                                color = colors.Primary
                            )
                        }

                        if (discount > 0) {
                            RowReverse(
                                Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                Arrangement.SpaceBetween,
                                Alignment.CenterVertically
                            ) {
                                RowReverse(verticalAlignment = Alignment.CenterVertically) {
                                    MaterialIcons(
                                        "local-offer", size = 14.dp, color = Color(0xFFE74C3C),
                                        modifier = Modifier.absolutePadding(left = 4.dp)
                                    )
                                    AppText(
                                        text = "تخفیف:",
                                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF34495E)
                                    )
                                }
                                AppText(
                                    text = formatPrice(discount),
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFFE74C3C)
                                )
                            }
                        }

                        RowReverse(
                            Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            Arrangement.SpaceBetween,
                            Alignment.CenterVertically
                        ) {
                            RowReverse(verticalAlignment = Alignment.CenterVertically) {
                                MaterialIcons(
                                    "payment", size = 14.dp, color = Color(0xFF2ECC71),
                                    modifier = Modifier.absolutePadding(left = 4.dp)
                                )
                                AppText(
                                    text = "مبلغ پرداخت شده:",
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFF34495E)
                                )
                            }
                            AppText(
                                text = formatPrice(totalAmount),
                                fontFamily = RNFonts.Yekan_Bakh_Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF2ECC71)
                            )
                        }
                    }
                }
            }
        }
    }
}
