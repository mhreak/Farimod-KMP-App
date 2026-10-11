package ir.farimod.app.screens.myresume

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.linearGradient
import ir.farimod.app.ui.pressable
import ir.farimod.app.ui.rememberSafeAreaInsets
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.rnSpring
import ir.farimod.app.ui.rnTiming
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.utils.PersianNumberUtils
import ir.farimod.app.utils.currentTimeMillis
import ir.farimod.app.utils.getCurrentPersianDate
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.animation.core.Animatable

internal val ResumeDocumentColors = listOf(
    Color(0xFF6366F1),
    Color(0xFF8B5CF6),
    Color(0xFF10B981),
    Color(0xFFF59E0B),
    Color(0xFFEF4444),
    Color(0xFF06B6D4)
)

internal fun resumeColorForIndex(index: Int): Color = ResumeDocumentColors[index % ResumeDocumentColors.size]

internal fun Color.resumeAlpha(hex: String): Color = copy(alpha = hex.toInt(16) / 255f)

internal data class ResumeUploadedFile(
    val memberDocumentId: Int?,
    val name: String,
    val uri: String
)

internal fun resumeFileIcon(fileName: String): String {
    val extension = fileName.substringAfterLast('.', fileName).lowercase()
    return when (extension) {
        "pdf" -> "picture-as-pdf"
        "doc", "docx" -> "description"
        "jpg", "jpeg", "png", "gif" -> "image"
        "mp4", "avi", "mov" -> "videocam"
        else -> "insert-drive-file"
    }
}

internal fun resumeIsoNow(): String {
    val epochMs = currentTimeMillis()
    val milli = (epochMs % 1000L).toInt()
    var z = epochMs / 1000L
    val second = (z % 60L).toInt()
    z /= 60L
    val minute = (z % 60L).toInt()
    z /= 60L
    val hour = (z % 24L).toInt()
    val days = z / 24L
    val (year, month, day) = resumeCivilFromDays(days)
    return "$year-${month.pad2()}-${day.pad2()}T${hour.pad2()}:${minute.pad2()}:${second.pad2()}.${milli.pad3()}Z"
}

/** `toLocaleDateString('fa-IR')`: Jalali year/month/day, Persian digits, no zero-padding. */
internal fun resumeShamsiToday(): String {
    val date = getCurrentPersianDate() ?: return ""
    return "${PersianNumberUtils.toPersianDigits(date.year)}/${PersianNumberUtils.toPersianDigits(date.month)}/${PersianNumberUtils.toPersianDigits(date.day)}"
}

private fun Int.pad2(): String = toString().padStart(2, '0')
private fun Int.pad3(): String = toString().padStart(3, '0')

private fun resumeCivilFromDays(daysSinceEpoch: Long): Triple<Int, Int, Int> {
    val z = daysSinceEpoch + 719468L
    val era = if (z >= 0) z / 146097L else (z - 146096L) / 146097L
    val doe = (z - era * 146097L).toInt()
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    var y = yoe + era.toInt() * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = if (mp < 10) mp + 3 else mp - 9
    y += if (m <= 2) 1 else 0
    return Triple(y, m, d)
}

internal fun Modifier.resumePullUp(top: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val shift = top.roundToPx()
    val height = (placeable.height - shift).coerceAtLeast(0)
    layout(placeable.width, height) {
        placeable.place(0, -shift)
    }
}

internal fun Modifier.resumeExpandHorizontal(extra: Dp): Modifier = layout { measurable, constraints ->
    val px = extra.roundToPx()
    val bounded = constraints.maxWidth < androidx.compose.ui.unit.Constraints.Infinity
    val max = if (bounded) constraints.maxWidth + px * 2 else constraints.maxWidth
    val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = max))
    val width = if (bounded) constraints.maxWidth else placeable.width
    layout(width, placeable.height) {
        placeable.place(-px, 0)
    }
}

@Composable
internal fun ResumeBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .zIndex(1000f)
            .padding(top = 0.dp)
    ) {
        Box(
            Modifier
                .size(44.dp)
                .rnShadow(5.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.9f))
                .touchableOpacity(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            MaterialIcons("arrow-forward", size = 24.dp, color = Color(0xFF6366F1))
        }
    }
}

@Composable
internal fun ResumeSectionTitle(text: String, modifier: Modifier = Modifier) {
    AppText(
        text = text,
        modifier = modifier.fillMaxWidth(),
        fontSize = 22.sp,
        fontFamily = RNFonts.Yekan_Bakh_ExtraBold,
        color = Color(0xFF2C3E50),
        textAlign = TextAlign.Center
    ,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
}

@Composable
internal fun ResumeSkeletonCard() {
    val shape = RoundedCornerShape(22.dp)
    Column(
        Modifier
            .absolutePadding(left = 5.dp, right = 5.dp, bottom = 20.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFFF8FAFC).copy(alpha = 0.3f))
            .border(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.4f), shape)
            .padding(18.dp)
    ) {
        RowReverse(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 15.dp)) {
            Box(
                Modifier
                    .absolutePadding(left = 12.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0))
            )
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Box(
                    Modifier
                        .padding(bottom = 8.dp)
                        .fillMaxWidth(0.6f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color(0xFFE2E8F0))
                )
                Box(
                    Modifier
                        .fillMaxWidth(0.8f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color(0xFFF1F5F9))
                )
            }
        }
        Column(Modifier.absolutePadding(left = 15.dp, right = 15.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFE2E8F0))
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF1F5F9))
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ResumeUploadCard(
    name: String,
    description: String,
    color: Color,
    files: List<ResumeUploadedFile>,
    uploading: Boolean,
    onPick: () -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (Int) -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier
            .absolutePadding(left = 5.dp, right = 5.dp, bottom = 20.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFFF8FAFC).copy(alpha = 0.3f))
            .border(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.4f), shape)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            RowReverse(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .absolutePadding(left = 12.dp)
                        .size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MaterialIcons("description", size = 22.dp, color = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    AppText(
                        text = name,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 17.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color(0xFF2C3E50),
                        textAlign = TextAlign.Right
                    )
                    if (description.isNotEmpty()) {
                        AppText(
                            text = description,
                            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                            fontSize = 13.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Right
                        )
                    }
                }
            }
            Column(Modifier.fillMaxWidth().absolutePadding(left = 15.dp, right = 15.dp).padding(top = 15.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .alpha(if (uploading) 0.7f else 1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(color)
                        .touchableOpacity(enabled = !uploading, onClick = onPick)
                ) {
                    Row(
                        Modifier.fillMaxWidth().absolutePadding(left = 20.dp, right = 20.dp, top = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MaterialIcons(
                            name = if (uploading) "timer" else "add",
                            size = 24.dp,
                            color = Color.White
                        )
                        AppText(
                            text = if (uploading) "در حال اپلود..." else "انتخاب فایل",
                            modifier = Modifier.absolutePadding(left = 10.dp),
                            fontSize = 16.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                if (files.isNotEmpty()) {
                    Column(Modifier.fillMaxWidth().padding(top = 15.dp)) {
                        RowReverse(
                            modifier = Modifier.padding(bottom = 15.dp).absolutePadding(left = 4.dp, right = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MaterialIcons(
                                "check-circle",
                                size = 18.dp,
                                color = color,
                                modifier = Modifier.absolutePadding(left = 8.dp)
                            )
                            AppText(
                                text = "فایل های آپلود شده",
                                fontSize = 15.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Bold,
                                color = Color(0xFF2C3E50),
                                textAlign = TextAlign.Right
                            )
                        }
                        BoxWithConstraints(Modifier.fillMaxWidth().resumeExpandHorizontal(6.dp)) {
                            val itemMargin = maxWidth * 0.01f
                            val cardWidth = maxWidth * 0.48f
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                FlowRow(Modifier.fillMaxWidth()) {
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        files.forEachIndexed { index, file ->
                                            ResumeFileCard(
                                                file = file,
                                                color = color,
                                                modifier = Modifier.padding(itemMargin).width(cardWidth),
                                                onOpen = { onOpen(file.uri) },
                                                onDelete = { onDelete(index) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (files.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().absolutePadding(top = 40.dp, bottom = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        MaterialIcons("description", size = 48.dp, color = color.resumeAlpha("30"))
                        AppText(
                            text = "هنوز فایلی در این بخش اپلود نشده",
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            fontSize = 16.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center
                        )
                        AppText(
                            text = "فایل‌های $name را اینجا اپلود کنید",
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            fontSize = 14.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                            color = Color(0xFF9CA3AF),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .align(AbsoluteAlignment.CenterRight)
                    .fillMaxHeight()
                    .width(6.dp)
                    .rnShadow(3.dp, RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp))
                    .clip(RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp))
                    .background(color.resumeAlpha("60"))
            )
        }
    }
}

@Composable
private fun ResumeFileCard(
    file: ResumeUploadedFile,
    color: Color,
    modifier: Modifier,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .clip(shape)
            .border(1.5.dp, color.resumeAlpha("30"), shape)
            .touchableOpacity(activeOpacity = 0.7f, onClick = onOpen)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp)
                .linearGradient(listOf(color.resumeAlpha("15"), color.resumeAlpha("05")))
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .padding(bottom = 12.dp)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(color.resumeAlpha("20")),
                contentAlignment = Alignment.Center
            ) {
                MaterialIcons(resumeFileIcon(file.name), size = 28.dp, color = color)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.resumeAlpha("20"))
                        .touchableOpacity { onOpen() }
                        .absolutePadding(top = 8.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MaterialIcons("visibility", size = 16.dp, color = color)
                }
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.1f))
                        .touchableOpacity { onDelete() }
                        .absolutePadding(top = 8.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MaterialIcons("delete-outline", size = 16.dp, color = Color(0xFFEF4444))
                }
            }
        }
    }
}

@Composable
internal fun ResumeDeleteDialog(
    typeName: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val density = LocalDensity.current.density
    val insets = rememberSafeAreaInsets()
    val slide = remember { Animatable(300f) }
    val opacity = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        coroutineScope {
            launch { slide.animateTo(0f, rnSpring(tension = 100f, friction = 8f)) }
            launch { opacity.animateTo(1f, rnTiming(200)) }
        }
    }
    val bottom = if (insets.bottom > 20.dp) insets.bottom else 20.dp
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .pressable { onCancel() }
                .absolutePadding(left = 30.dp, right = 30.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 350.dp)
                    .graphicsLayer {
                        translationY = slide.value * density
                        alpha = opacity.value
                    }
                    .padding(bottom = bottom)
                    .rnShadow(15.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .pressable { }
                    .padding(30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .padding(bottom = 20.dp)
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEE2E2)),
                    contentAlignment = Alignment.Center
                ) {
                    MaterialIcons("warning", size = 48.dp, color = Color(0xFFEF4444))
                }
                AppText(
                    text = "حذف فایل",
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    fontSize = 20.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color(0xFF1F2937),
                    textAlign = TextAlign.Center
                )
                AppText(
                    text = "آیا مطمئن هستید که می‌خواهید این فایل را از بخش $typeName حذف کنید؟",
                    modifier = Modifier.fillMaxWidth().padding(bottom = 30.dp),
                    fontSize = 16.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF3F4F6))
                            .border(1.dp, Color(0xFFD1D5DB), RoundedCornerShape(10.dp))
                            .touchableOpacity(onClick = onCancel)
                            .absolutePadding(top = 8.dp, bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppText(
                            text = "خیر",
                            fontSize = 16.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                            color = Color(0xFF374151),
                            textAlign = TextAlign.Center
                        )
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEF4444))
                            .touchableOpacity { onConfirm() }
                            .absolutePadding(top = 8.dp, bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppText(
                            text = "بله، حذف کن",
                            fontSize = 16.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ResumeMessageState(
    icon: String,
    title: String,
    subtitle: String,
    retry: (@Composable () -> Unit)? = null
) {
    Column(
        Modifier
            .fillMaxSize()
            .absolutePadding(left = 40.dp, right = 40.dp, top = 60.dp, bottom = 60.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MaterialIcons(icon, size = 80.dp, color = Color(0xFF9E9E9E))
        AppText(
            text = title,
            modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            fontSize = 20.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center
        )
        AppText(
            text = subtitle,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            fontSize = 16.sp,
            fontFamily = RNFonts.Yekan_Bakh_Regular,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        retry?.invoke()
    }
}
