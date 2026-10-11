package ir.farimod.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.platform.NativeVideoPlayer
import ir.farimod.app.theme.FarimodColors

// ──────────────────────────────────────────────────────────────────────────────
// Internal building blocks of `ImageUpload.kt` (port of the sub-views / styles of app/components/ImageUpload.tsx)
// ──────────────────────────────────────────────────────────────────────────────

internal val IUPrimaryAlpha10 = rgba(158, 34, 173, 0.1f)
internal val IUPrimaryAlpha05 = rgba(158, 34, 173, 0.05f)
internal val IUPrimaryAlpha20 = rgba(158, 34, 173, 0.2f)

/** RN: an overlay `View` that is not touchable still owns the touches that land on it (they don't reach siblings below). */
internal fun Modifier.iuConsumeTaps(): Modifier = pointerInput(Unit) { detectTapGestures { } }

/** RN `borderStyle: 'dashed'` (Android: dash = gap = 3 × borderWidth). Draws the stroke inside the box bounds. */
internal fun Modifier.iuDashedBorder(width: Dp, color: Color, radius: Dp, outset: Dp = 0.dp): Modifier = drawBehind {
    val w = width.toPx()
    val r = radius.toPx()
    val o = outset.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(-o + w / 2f, -o + w / 2f),
        size = Size(size.width + 2f * o - w, size.height + 2f * o - w),
        cornerRadius = CornerRadius(maxOf(0f, r - w / 2f)),
        style = Stroke(width = w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 3f, w * 3f), 0f))
    )
}

private val videoExtensions = listOf(".mp4", ".mov", ".avi", ".mkv", ".m4v")
private val supportedVideoFormats = listOf(".mp4", ".mov", ".m4v")

/** RN `isVideo(uri)` (extended with the item's mime type, since content:// URIs often have no extension). */
internal fun iuIsVideo(item: ImageItem?): Boolean {
    if (item == null || item.uri.isEmpty()) return false
    val uri = item.uri.lowercase()
    return videoExtensions.any { uri.contains(it) } || item.type?.startsWith("video") == true
}

/** RN `isValidVideoFormat(uri)` (extended with the mime type). */
internal fun iuIsValidVideoFormat(item: ImageItem?): Boolean {
    if (item == null) return false
    val uri = item.uri.lowercase()
    return supportedVideoFormats.any { uri.contains(it) } || item.type?.startsWith("video") == true
}

// ── dialogs ───────────────────────────────────────────────────────────────────

/** RN `<Modal transparent animationType="none" onRequestClose>` */
@Composable
internal fun IUModalRoot(onRequestClose: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Dialog(
        onDismissRequest = onRequestClose,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(Modifier.fillMaxSize(), content = content)
        }
    }
}

// ── small views ───────────────────────────────────────────────────────────────

/** 24×24 round gradient button (`removeButton` / `editButton`: TouchableOpacity activeOpacity .7, radius 12, overflow hidden). */
@Composable
internal fun IUCornerButton(
    colors: List<Color>,
    iconName: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .touchableOpacity(activeOpacity = 0.7f, onClick = onClick)
            .size(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .linearGradient(colors),
        contentAlignment = Alignment.Center
    ) {
        MaterialIcons(iconName, size = 16.dp, color = Color.White)
    }
}

/** `videoContainer` + `VideoThumbnail` + `videoPlayIcon` + `videoBadge`. */
@Composable
internal fun IUVideoThumb(item: ImageItem, onClick: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        // VideoThumbnail: loop=false, paused, contentFit="cover", nativeControls={false}
        NativeVideoPlayer(
            url = item.uri,
            modifier = Modifier.fillMaxSize(),
            showControls = false,
            autoPlay = false,
            loop = false,
            muted = false,
            cover = true
        )
        // native video surfaces swallow touches → transparent overlay carrying the item's onPress
        Box(Modifier.fillMaxSize().pressable(onClick = onClick))
        // videoPlayIcon (absolute 50%/50% translate -12 → centered 24×24)
        Box(
            Modifier
                .align(Alignment.Center)
                .size(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(rgba(0, 0, 0, 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            MaterialIcons("play-arrow", size = 24.dp, color = Color.White)
        }
        // videoBadge (absolute top 8, left 40)
        Box(
            Modifier
                .align(AbsoluteAlignment.TopLeft)
                .absoluteOffset(x = 40.dp, y = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(rgba(220, 38, 127, 0.9f))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            RowReverse(verticalAlignment = Alignment.CenterVertically) {
                MaterialIcons("videocam", size = 12.dp, color = Color.White)
                AppText(
                    "فیلم",
                    modifier = Modifier.absolutePadding(right = 2.dp),
                    fontSize = 9.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color.White
                )
            }
        }
    }
}

/** `errorImageContainer` */
@Composable
internal fun IUErrorImage() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        MaterialIcons("broken-image", size = 40.dp, color = FarimodColors.Medium)
        AppText(
            "خطا در بارگذاری",
            modifier = Modifier.padding(top = 5.dp),
            fontSize = 10.sp,
            fontFamily = RNFonts.Yekan_Bakh_Regular,
            color = FarimodColors.Medium,
            textAlign = TextAlign.Center
        )
    }
}

/** `imageInfo` bar (absolute bottom/left/right 0, rgba(0,0,0,.7), padding 8): name + size. */
@Composable
internal fun BoxScope.IUImageInfo(name: String, sizeText: String?) {
    Column(
        Modifier
            .align(AbsoluteAlignment.BottomLeft)
            .fillMaxWidth()
            .background(rgba(0, 0, 0, 0.7f))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppText(
            name,
            modifier = Modifier.padding(bottom = 2.dp),
            fontSize = 10.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (sizeText != null) {
            AppText(
                sizeText,
                fontSize = 8.sp,
                fontFamily = RNFonts.Yekan_Bakh_Regular,
                color = rgba(255, 255, 255, 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * One picker option of the source sheet (`pickerOption`): flex 1, icon circle 70, label, description.
 * Must be used inside the `imagePickerContainer` Row.
 */
@Composable
internal fun RowScope.IUPickerOption(
    iconBackground: Color,
    iconName: String,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .touchableOpacity(activeOpacity = 0.8f, onClick = onClick)
            .background(Color(0xFFF9FAFB), shape)
            .border(1.dp, Color(0xFFE5E7EB), shape)
            .padding(vertical = 25.dp, horizontal = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .padding(bottom = 12.dp)
                .size(70.dp)
                .rnShadow(4.dp, CircleShape, Color.Black)
                .background(iconBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MaterialIcons(iconName, size = 32.dp, color = Color.White)
        }
        AppText(
            label,
            modifier = Modifier.padding(bottom = 4.dp),
            fontSize = 16.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = Color(0xFF1F2937)
        )
        AppText(
            description,
            fontSize = 12.sp,
            fontFamily = RNFonts.Yekan_Bakh_Regular,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

// ── modals ────────────────────────────────────────────────────────────────────

/** "Select image source" bottom sheet (first `<Modal>` of ImageUpload.tsx). */
@Composable
internal fun IUSourceModal(
    title: String,
    subtitle: String?,
    showGallery: Boolean,
    galleryDescription: String,
    showCamera: Boolean,
    cameraDescription: String,
    slideY: Float,
    opacity: Float,
    bottomInset: Dp,
    onClose: () -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit
) {
    val density = LocalDensity.current.density
    IUModalRoot(onRequestClose = onClose) {
        // modalOverlay (Pressable): flex 1, rgba(0,0,0,.6), justify flex-end
        Box(
            Modifier
                .fillMaxSize()
                .background(rgba(0, 0, 0, 0.6f))
                .pressable(onClick = onClose),
            contentAlignment = Alignment.BottomCenter
        ) {
            // modalContent
            Column(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = slideY * density
                        alpha = opacity
                    }
                    .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                    .background(Color.White)
                    .padding(top = 12.dp, bottom = 20.dp)
                    .padding(horizontal = 20.dp)
            ) {
                // modalHeader
                Column(
                    Modifier.fillMaxWidth().padding(bottom = 25.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .padding(bottom = 15.dp)
                            .size(width = 40.dp, height = 4.dp)
                            .background(Color(0xFFD1D5DB), RoundedCornerShape(2.dp))
                    )
                    AppText(
                        title,
                        modifier = Modifier.padding(bottom = 8.dp),
                        fontSize = 20.sp,
                        fontFamily = RNFonts.Yekan_Bakh_ExtraBold,
                        color = Color(0xFF1F2937)
                    )
                    if (subtitle != null) {
                        AppText(
                            subtitle,
                            fontSize = 14.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                            color = Color(0xFF6B7280),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // imagePickerContainer: row, space-between, paddingHorizontal 10, marginBottom 25, gap 15
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 25.dp)
                        .padding(horizontal = 10.dp)
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    if (showGallery) {
                        IUPickerOption(
                            iconBackground = FarimodColors.Primary,
                            iconName = "photo-library",
                            label = "گالری",
                            description = galleryDescription,
                            onClick = onGallery
                        )
                    }
                    if (showCamera) {
                        IUPickerOption(
                            iconBackground = Color(0xFF10B981),
                            iconName = "camera-alt",
                            label = "دوربین",
                            description = cameraDescription,
                            onClick = onCamera
                        )
                    }
                }

                // cancelButton (TouchableOpacity default opacity)
                val cancelShape = RoundedCornerShape(15.dp)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .padding(horizontal = 5.dp)
                        .touchableOpacity(onClick = onClose)
                        .background(Color(0xFFFEE2E2), cancelShape)
                        .border(1.dp, Color(0xFFFECACA), cancelShape)
                        .padding(vertical = 16.dp)
                ) {
                    AppText(
                        "لغو",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 16.sp,
                        color = Color(0xFFDC2626),
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        textAlign = TextAlign.Center
                    )
                }

                // modalSafeArea
                Spacer(Modifier.fillMaxWidth().height(bottomInset))
            }
        }
    }
}

/** "Delete confirmation" bottom sheet (second `<Modal>`). [slide] and [backdropAlpha] are the 0..1 Animated values. */
@Composable
internal fun IUDeleteModal(
    kindLabel: String,
    slide: Float,
    backdropAlpha: Float,
    bottomInset: Dp,
    onClose: () -> Unit,
    onConfirm: () -> Unit
) {
    val density = LocalDensity.current.density
    IUModalRoot(onRequestClose = onClose) {
        // deleteModalBackdrop (opacity animated) + deleteBackdropTouchable
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = backdropAlpha }
                .background(rgba(0, 0, 0, 0.5f))
                .pressable(onClick = onClose)
        )

        // deleteModalContent
        val sheetShape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer { translationY = (300f * (1f - slide)) * density }
                .rnShadow(10.dp, sheetShape, Color.Black)
                .clip(sheetShape)
                .background(Color(0xFFFFFFFF))
                .iuConsumeTaps()
                .padding(top = 15.dp, bottom = 20.dp)
                .padding(horizontal = 25.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // deleteModalHandle
            Box(
                Modifier
                    .padding(bottom = 20.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(Color(0xFFE0E0E0), RoundedCornerShape(2.dp))
            )

            // deleteModalHeader
            Column(
                Modifier.fillMaxWidth().padding(bottom = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .padding(bottom = 20.dp)
                        .size(70.dp)
                        .rnShadow(8.dp, CircleShape, Color(0xFFE74C3C))
                        .background(Color(0xFFE74C3C), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    MaterialIcons("delete", size = 32.dp, color = Color(0xFFFFFFFF))
                }
                AppText(
                    "حذف $kindLabel",
                    modifier = Modifier.padding(bottom = 10.dp),
                    fontSize = 18.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color(0xFF2C3E50),
                    textAlign = TextAlign.Center
                )
                AppText(
                    "آیا از حذف این $kindLabel اطمینان دارید؟\nاین عمل قابل بازگشت نیست.",
                    fontSize = 14.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    color = Color(0xFF7F8C8D),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }

            // deleteModalActions: paddingTop 10 / deleteButtonsRow: row-reverse, gap 15
            RowReverse(
                Modifier.fillMaxWidth().padding(top = 10.dp).height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                // deleteModalCancelButton
                val cancelShape = RoundedCornerShape(15.dp)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .touchableOpacity(onClick = onClose)
                        .background(Color(0xFFF8F9FA), cancelShape)
                        .border(1.dp, Color(0xFFE9ECEF), cancelShape)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AppText(
                        "لغو",
                        fontSize = 16.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color(0xFF6C757D)
                    )
                }

                // confirmDeleteButton
                val confirmShape = RoundedCornerShape(15.dp)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .touchableOpacity(onClick = onConfirm)
                        .rnShadow(6.dp, confirmShape, Color(0xFFE74C3C))
                        .clip(confirmShape)
                        .linearGradient(
                            listOf(Color(0xFFE74C3C), Color(0xFFC0392B)),
                            start = Offset(0f, 0f),
                            end = Offset(1f, 1f)
                        )
                        .padding(vertical = 16.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    RowReverse(
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MaterialIcons("delete-forever", size = 20.dp, color = Color(0xFFFFFFFF))
                        AppText(
                            "بله، حذف کن",
                            fontSize = 16.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            color = Color(0xFFFFFFFF)
                        )
                    }
                }
            }

            // deleteModalSafeArea
            Spacer(Modifier.fillMaxWidth().height(bottomInset))
        }
    }
}

/** Full-screen image / video viewer (third `<Modal statusBarTranslucent>`). */
@Composable
internal fun IUViewerModal(
    item: ImageItem?,
    scale: Float,
    opacity: Float,
    onClose: () -> Unit,
    onDelete: (ImageItem) -> Unit,
    onEdit: () -> Unit
) {
    IUModalRoot(onRequestClose = onClose) {
        // imageViewerBackdrop (opacity animated) + imageViewerBackdropTouchable
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = opacity }
                .background(rgba(0, 0, 0, 0.95f))
                .pressable(onClick = onClose)
        )

        // imageViewerContent (opacity + scale animated)
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = opacity
                    scaleX = scale
                    scaleY = scale
                }
                .iuConsumeTaps()
        ) {
            if (item != null) {
                val isVid = iuIsVideo(item)
                // imageContainer (inside imageViewerContent padding: top 60, bottom 40, horizontal 20)
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(top = 60.dp, bottom = 40.dp)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.uri.isNotEmpty() && isVid) {
                        // VideoPlayerView: loop=false, play(), contentFit="contain", nativeControls
                        NativeVideoPlayer(
                            url = item.uri,
                            modifier = Modifier.fillMaxSize(),
                            showControls = true,
                            autoPlay = true,
                            loop = false,
                            muted = false,
                            cover = false
                        )
                    } else if (item.uri.isNotEmpty()) {
                        NetworkImage(item.uri, Modifier.fillMaxSize(), resizeMode = ResizeMode.contain)
                    }
                }

                // imageViewerHeader: absolute top 60, left/right 20, row, space-between, center, zIndex 10
                Row(
                    Modifier
                        .align(AbsoluteAlignment.TopLeft)
                        .zIndex(10f)
                        .fillMaxWidth()
                        .padding(top = 60.dp)
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // closeButton (padding 8) > closeButtonContainer 40
                    Box(
                        Modifier
                            .touchableOpacity(activeOpacity = 0.7f, onClick = onClose)
                            .padding(8.dp)
                            .size(40.dp)
                            .background(rgba(0, 0, 0, 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        MaterialIcons("close", size = 24.dp, color = Color(0xFFFFFFFF))
                    }
                    // imageViewerInfo: flex 1, alignItems flex-end, marginRight 15
                    Column(
                        Modifier.weight(1f).absolutePadding(right = 15.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        AppText(
                            if (!item.name.isNullOrEmpty()) item.name else if (isVid) "فیلم" else "تصویر",
                            modifier = Modifier.padding(bottom = 4.dp),
                            fontSize = 16.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            color = Color(0xFFFFFFFF),
                            textAlign = TextAlign.Right,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (item.size != null && item.size != 0L) {
                            AppText(
                                iuFormatFileSize(item.size),
                                fontSize = 12.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Regular,
                                color = rgba(255, 255, 255, 0.7f),
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                }

                // imageViewerFooter: absolute bottom 40, left/right 20, zIndex 10
                Box(
                    Modifier
                        .align(AbsoluteAlignment.BottomLeft)
                        .zIndex(10f)
                        .fillMaxWidth()
                        .padding(bottom = 40.dp)
                        .padding(horizontal = 20.dp)
                ) {
                    // imageActions: row-reverse, center, gap 30
                    RowReverse(
                        Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(30.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IUViewerAction(
                            colors = listOf(Color(0xFFFF4757), Color(0xFFFF3742)),
                            iconName = "delete",
                            label = "حذف",
                            onClick = { onDelete(item) }
                        )
                        IUViewerAction(
                            colors = listOf(FarimodColors.Primary, FarimodColors.PrimaryDark),
                            iconName = "edit",
                            label = "ویرایش",
                            onClick = onEdit
                        )
                    }
                }
            }
        }
    }
}

/** `actionButton` of the viewer footer: 50×50 gradient circle (shadow 8) + label. */
@Composable
private fun IUViewerAction(colors: List<Color>, iconName: String, label: String, onClick: () -> Unit) {
    Column(
        Modifier.touchableOpacity(activeOpacity = 0.7f, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .padding(bottom = 8.dp)
                .size(50.dp)
                .rnShadow(8.dp, CircleShape, Color.Black)
                .clip(CircleShape)
                .linearGradient(colors),
            contentAlignment = Alignment.Center
        ) {
            MaterialIcons(iconName, size = 20.dp, color = Color.White)
        }
        AppText(
            label,
            fontSize = 12.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = Color(0xFFFFFFFF),
            textAlign = TextAlign.Center
        )
    }
}

// ── formatting ────────────────────────────────────────────────────────────────

/** JS `Number.prototype.toFixed(1)` (English digits). */
internal fun iuToFixed1(value: Double): String {
    val r = kotlin.math.round(value * 10.0).toLong()
    val neg = r < 0
    val a = kotlin.math.abs(r)
    return (if (neg) "-" else "") + "${a / 10}.${a % 10}"
}

/** RN `formatFileSize(bytes)` */
internal fun iuFormatFileSize(bytes: Long?): String {
    if (bytes == null || bytes == 0L) return ""
    val kb = bytes / 1024.0
    if (kb < 1024) {
        return "${iuToFixed1(kb)} کیلوبایت"
    }
    val mb = kb / 1024.0
    return "${iuToFixed1(mb)} مگابایت"
}
