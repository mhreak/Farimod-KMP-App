package ir.farimod.app.screens.coursestudents

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import ir.farimod.app.components.icons.MaterialCommunityIcons
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.network.ApiConfig
import ir.farimod.app.network.get
import ir.farimod.app.network.str
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.ResizeMode
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.rnSpring
import ir.farimod.app.ui.rnTiming
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.ui.windowSize
import ir.farimod.app.utils.formatPrice
import ir.farimod.app.utils.safeNumber
import ir.farimod.app.utils.toPersianDigits
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement

internal val StudentPrimary = Color(0xFF667EEA)
internal val StudentDark = Color(0xFF2C3E50)
internal val StudentMedium = Color(0xFF34495E)
internal val StudentSuccess = Color(0xFF2ECC71)
internal val StudentWarning = Color(0xFFF39C12)

@Composable
internal fun studentCardWidthDp() = (windowSize().first - 60.dp) / 2

internal fun stateColor(state: Double): Color = when (state) {
    1.0 -> StudentWarning
    2.0 -> StudentSuccess
    else -> StudentMedium
}

internal fun stateIcon(state: Double): String = when (state) {
    1.0 -> "pending"
    2.0 -> "check-circle"
    else -> "info"
}

private val AvatarGradients = listOf(
    listOf(Color(0xFFFA709A), Color(0xFFFEE140)),
    listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
    listOf(Color(0xFFF093FB), Color(0xFFF5576C)),
    listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)),
    listOf(Color(0xFF43E97B), Color(0xFF38F9D7)),
    listOf(Color(0xFFFF9A56), Color(0xFFFFAD56)),
    listOf(Color(0xFFA8EDEA), Color(0xFFFED6E3)),
    listOf(Color(0xFFFBC2EB), Color(0xFFA6C1EE))
)

@Composable
internal fun StudentPulse(modifier: Modifier, radius: androidx.compose.ui.unit.Dp = 8.dp) {
    val transition = rememberInfiniteTransition(label = "studentSkeleton")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
        label = "studentSkeletonColor"
    )
    Box(modifier.clip(RoundedCornerShape(radius)).background(lerp(Color(0xFFE0E0E0), Color(0xFFF0F0F0), t)))
}

@Composable
internal fun StudentCardSkeleton() {
    val width = studentCardWidthDp()
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .padding(bottom = 16.dp)
            .width(width)
            .rnShadow(4.dp, shape)
            .clip(shape)
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StudentPulse(Modifier.padding(bottom = 12.dp).size(70.dp), 35.dp)
        StudentPulse(Modifier.padding(bottom = 8.dp).fillMaxWidth(0.8f).height(16.dp))
        StudentPulse(Modifier.padding(bottom = 12.dp).fillMaxWidth(0.6f).height(14.dp), 7.dp)
        StudentPulse(Modifier.padding(bottom = 12.dp).fillMaxWidth().height(1.dp), 0.dp)
        StudentPulse(Modifier.padding(bottom = 6.dp).fillMaxWidth(0.9f).height(14.dp), 7.dp)
        StudentPulse(Modifier.fillMaxWidth(0.7f).height(14.dp), 7.dp)
    }
}

@Composable
private fun StudentAvatar(member: JsonElement, size: androidx.compose.ui.unit.Dp) {
    val name = member.str("MemberName")
    val gradient = AvatarGradients[(name?.length ?: 0) % AvatarGradients.size]
    val url = member.str("MemberAvatarImageURL")
    var failed by remember(url) { mutableStateOf(false) }
    val shape = CircleShape
    if (!url.isNullOrBlank() && !failed) {
        Box(
            Modifier
                .size(size)
                .rnShadow(8.dp, shape)
                .border(3.dp, Color.White, shape)
                .clip(shape)
        ) {
            AsyncImage(
                model = ApiConfig.resolveMediaUrl(url),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ResizeMode.cover,
                onError = { failed = true }
            )
        }
    } else {
        LinearGradient(
            colors = gradient,
            modifier = Modifier
                .size(size)
                .rnShadow(8.dp, shape, gradient[0])
                .border(3.dp, Color.White, shape)
                .clip(shape),
            start = Offset(0f, 0f),
            end = Offset(1f, 1f)
        ) {
            MaterialCommunityIcons(
                "face-man",
                size = size * 0.6f,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
internal fun StudentPersonCard(item: JsonElement, index: Int, onPress: (JsonElement) -> Unit) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 80L)
        appear.animateTo(1f, rnSpring(50f, 7f))
    }
    val width = studentCardWidthDp()
    val shape = RoundedCornerShape(20.dp)
    val state = safeNumber(item["State"])
    Box(
        Modifier
            .padding(bottom = 16.dp)
            .width(width)
            .graphicsLayer {
                alpha = appear.value
                scaleX = 0.8f + 0.2f * appear.value
                scaleY = scaleX
                translationY = (1f - appear.value) * 50.dp.toPx()
            }
            .touchableOpacity(activeOpacity = 0.9f) { onPress(item) }
            .rnShadow(6.dp, shape)
            .clip(shape)
    ) {
        LinearGradient(
            colors = listOf(Color.White, Color(0xFFF8F9FA)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                StudentAvatar(item, 70.dp)
                AppText(
                    text = item.str("MemberName").orEmpty(),
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp).height(40.dp),
                    fontSize = 15.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = StudentDark,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                AppText(
                    text = toPersianDigits(item.str("MemberMobile").orEmpty()),
                    modifier = Modifier.padding(bottom = 12.dp),
                    fontSize = 12.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    color = StudentMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE9ECEF)))
                AppText(
                    text = toPersianDigits(formatPrice(safeNumber(item["TotalAmount"]))),
                    modifier = Modifier.padding(top = 12.dp),
                    fontSize = 16.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = StudentPrimary,
                    textAlign = TextAlign.Center
                )
            }
            Box(
                Modifier
                    .align(AbsoluteAlignment.TopLeft)
                    .absoluteOffset(x = 12.dp, y = 12.dp)
                    .size(32.dp)
                    .rnShadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(stateColor(state)),
                contentAlignment = Alignment.Center
            ) {
                MaterialIcons(stateIcon(state), size = 14.dp, color = Color.White)
            }
        }
    }
}

@Composable
internal fun StudentDrawer(
    visible: Boolean,
    student: JsonElement?,
    onClose: () -> Unit,
    onDelete: (JsonElement) -> Unit
) {
    val slide = remember { Animatable(1f) }
    val flip = remember { Animatable(0f) }
    var showFront by remember { mutableStateOf(true) }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val screenHeight = windowSize().second
    val current = student
    LaunchedEffect(visible, current) {
        if (!visible || current == null) return@LaunchedEffect
        showFront = true
        flip.snapTo(0f)
        slide.snapTo(1f)
        slide.animateTo(0f, rnSpring(50f, 9f))
    }
    if (!visible || current == null) return
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).touchableOpacity { onClose() })
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(screenHeight * 0.85f)
                    .graphicsLayer { translationY = slide.value * with(density) { screenHeight.toPx() } }
                    .rnShadow(15.dp, RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                    .background(Color.White)
                    .absolutePadding(left = 25.dp, right = 25.dp)
                    .padding(top = 15.dp, bottom = 35.dp)
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 20.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFE0E0E0))
                )
                RowReverse(
                    Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF8F9FA))
                            .touchableOpacity { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        MaterialIcons("close", size = 24.dp, color = StudentDark)
                    }
                    AppText(
                        text = "جزئیات دانشجو",
                        fontSize = 18.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = StudentDark,
                        textAlign = TextAlign.Center
                    )
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF0F4FF))
                            .touchableOpacity {
                                scope.launch {
                                    flip.animateTo(if (showFront) 180f else 0f, rnTiming(600))
                                    showFront = !showFront
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        MaterialIcons("flip", size = 24.dp, color = StudentPrimary)
                    }
                }
                Box(Modifier.fillMaxWidth().height(380.dp).padding(bottom = 20.dp)) {
                    val frontAngle = flip.value
                    val backAngle = flip.value + 180f
                    val frontAlpha = if (frontAngle < 90f) 1f - frontAngle / 90f else 0f
                    val backAlpha = if (frontAngle > 90f) (frontAngle - 90f) / 90f else 0f
                    StudentFront(current, frontAngle, frontAlpha, showFront, Modifier.matchParentSize())
                    StudentBack(current, backAngle, backAlpha, !showFront, Modifier.matchParentSize())
                }
                val deleteShape = RoundedCornerShape(15.dp)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .rnShadow(10.dp, deleteShape, Color(0xFFFF6B6B))
                        .clip(deleteShape)
                        .touchableOpacity(activeOpacity = 0.8f) { onDelete(current) }
                ) {
                    LinearGradient(
                        colors = listOf(Color(0xFFFF6B6B), Color(0xFFEE5A6F)),
                        modifier = Modifier.fillMaxWidth(),
                        start = Offset(0f, 0f),
                        end = Offset(1f, 1f)
                    ) {
                        RowReverse(
                            Modifier.fillMaxWidth().absolutePadding(top = 18.dp, bottom = 18.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MaterialIcons("delete-forever", size = 24.dp, color = Color.White)
                            AppText(
                                text = "حذف دانشجو از دوره",
                                fontSize = 16.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudentFront(
    student: JsonElement,
    angle: Float,
    alpha: Float,
    active: Boolean,
    modifier: Modifier
) {
    val density = LocalDensity.current.density
    val state = safeNumber(student["State"])
    LinearGradient(
        colors = listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
        modifier = modifier
            .zIndex(if (active) 1f else 0f)
            .graphicsLayer {
                rotationY = angle
                cameraDistance = 12f * density
                this.alpha = alpha
            }
            .clip(RoundedCornerShape(25.dp)),
        start = Offset(0.5f, 0f),
        end = Offset(0.5f, 1f)
    ) {
        Column(
            Modifier.fillMaxSize().padding(25.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            StudentAvatar(student, 90.dp)
            AppText(
                text = student.str("MemberName").orEmpty(),
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
                fontSize = 22.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            AppText(
                text = toPersianDigits(student.str("MemberMobile").orEmpty()),
                modifier = Modifier.padding(bottom = 20.dp),
                fontSize = 16.sp,
                fontFamily = RNFonts.Yekan_Bakh_Regular,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            RowReverse(
                Modifier
                    .padding(bottom = 20.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .absolutePadding(left = 16.dp, right = 16.dp, top = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MaterialIcons("calendar-today", size = 18.dp, color = Color.White.copy(alpha = 0.9f))
                AppText(
                    text = "تاریخ ثبت‌نام: ${toPersianDigits(student.str("ShamsiRegisterDate").orEmpty())}",
                    fontSize = 14.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    color = Color.White.copy(alpha = 0.95f)
                )
            }
            RowReverse(
                Modifier
                    .clip(RoundedCornerShape(25.dp))
                    .background(stateColor(state).copy(alpha = 0x30 / 255f))
                    .absolutePadding(left = 20.dp, right = 20.dp, top = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MaterialIcons(stateIcon(state), size = 20.dp, color = stateColor(state))
                AppText(
                    text = student.str("StateStr").orEmpty(),
                    fontSize = 15.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = stateColor(state)
                )
            }
        }
    }
}

@Composable
private fun StudentBack(
    student: JsonElement,
    angle: Float,
    alpha: Float,
    active: Boolean,
    modifier: Modifier
) {
    val density = LocalDensity.current.density
    val discount = safeNumber(student["Discount"])
    LinearGradient(
        colors = listOf(Color(0xFF013D38), Color(0xFF38EF7D)),
        modifier = modifier
            .zIndex(if (active) 1f else 0f)
            .graphicsLayer {
                rotationY = angle
                cameraDistance = 12f * density
                this.alpha = alpha
            }
            .clip(RoundedCornerShape(25.dp)),
        start = Offset(0f, 0f),
        end = Offset(1f, 1f)
    ) {
        Column(Modifier.fillMaxSize().padding(25.dp)) {
            AppText(
                text = "اطلاعات پرداخت",
                modifier = Modifier.fillMaxWidth().padding(bottom = 25.dp),
                fontSize = 20.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            PayRow("payment", "نوع پرداخت", student.str("PaymentTypeStr").orEmpty())
            PayRow("attach-money", "مبلغ دوره", toPersianDigits(formatPrice(safeNumber(student["CourseRegisterAmount"]))))
            if (discount > 0) {
                PayRow("local-offer", "تخفیف", toPersianDigits(formatPrice(discount)))
            }
            Column(
                Modifier
                    .padding(top = 15.dp, bottom = 15.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color.White.copy(alpha = 0.3f))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppText(
                    text = "مبلغ نهایی",
                    fontSize = 14.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                AppText(
                    text = toPersianDigits(formatPrice(safeNumber(student["TotalAmount"]))),
                    fontSize = 24.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PayRow(icon: String, label: String, value: String) {
    RowReverse(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White.copy(alpha = 0.2f))
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .absolutePadding(left = 12.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            MaterialIcons(icon, size = 20.dp, color = Color.White)
        }
        Column(Modifier.weight(1f)) {
            AppText(
                text = label,
                modifier = Modifier.padding(bottom = 4.dp),
                fontSize = 12.sp,
                fontFamily = RNFonts.Yekan_Bakh_Regular,
                color = Color.White.copy(alpha = 0.85f)
            )
            AppText(
                text = value,
                fontSize = 15.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = Color.White
            )
        }
    }
}

@Composable
internal fun DeleteStudentSheet(
    visible: Boolean,
    deleting: Boolean,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!visible) return
    Dialog(
        onDismissRequest = { if (!deleting) onCancel() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .touchableOpacity(enabled = !deleting) { onCancel() }
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .rnShadow(10.dp, RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                    .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                    .background(Color.White)
                    .absolutePadding(left = 25.dp, right = 25.dp)
                    .padding(top = 15.dp, bottom = 35.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .padding(bottom = 20.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFE0E0E0))
                )
                Box(
                    Modifier
                        .padding(bottom = 20.dp)
                        .size(80.dp)
                        .rnShadow(10.dp, CircleShape, Color(0xFFFF6B6B))
                        .clip(CircleShape)
                        .background(Color(0xFFFF6B6B)),
                    contentAlignment = Alignment.Center
                ) {
                    MaterialIcons("warning", size = 40.dp, color = Color.White)
                }
                AppText(
                    text = "حذف دانشجو",
                    modifier = Modifier.padding(bottom = 15.dp),
                    fontSize = 22.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color(0xFF2C3E50),
                    textAlign = TextAlign.Center
                )
                AppText(
                    text = "آیا از حذف این دانشجو از دوره اطمینان دارید؟\nاین عملیات قابل بازگشت نیست.",
                    modifier = Modifier.padding(bottom = 30.dp),
                    fontSize = 16.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    color = Color(0xFF6C757D),
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )
                RowReverse(
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    val shape = RoundedCornerShape(15.dp)
                    Box(
                        Modifier
                            .weight(1f)
                            .rnShadow(10.dp, shape, Color(0xFFFF6B6B))
                            .clip(shape)
                            .touchableOpacity(enabled = !deleting, activeOpacity = 0.8f, onClick = onConfirm)
                    ) {
                        LinearGradient(
                            colors = listOf(Color(0xFFFF6B6B), Color(0xFFEE5A6F)),
                            modifier = Modifier.fillMaxWidth(),
                            start = Offset(0f, 0f),
                            end = Offset(1f, 1f)
                        ) {
                            if (deleting) {
                                Box(Modifier.fillMaxWidth().absolutePadding(top = 16.dp, bottom = 16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                }
                            } else {
                                RowReverse(
                                    Modifier.fillMaxWidth().absolutePadding(left = 20.dp, right = 20.dp, top = 16.dp, bottom = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MaterialIcons("delete-forever", size = 22.dp, color = Color.White)
                                    AppText(
                                        text = "بله، حذف شود",
                                        fontSize = 16.sp,
                                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(shape)
                            .background(Color(0xFFF8F9FA))
                            .border(2.dp, Color(0xFFE9ECEF), shape)
                            .touchableOpacity(enabled = !deleting, activeOpacity = 0.8f) { onCancel() }
                            .absolutePadding(top = 16.dp, bottom = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppText(
                            text = "انصراف",
                            fontSize = 16.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            color = Color(0xFF6C757D),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
