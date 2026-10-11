package ir.farimod.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.platform.PickedMedia
import ir.farimod.app.platform.rememberMediaPicker
import ir.farimod.app.theme.FarimodColors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * RN `ImageItem` (`{ id, uri, name?, type?, size? }`) of `app/components/ImageUpload.tsx`.
 *
 *  - [uri]  displayable URI: remote URL, or a local picked file (`file://`, `content://`, ...).
 *  - [type] mime type (`image/jpeg`, `video/mp4` ...). RN stored expo's `asset.type`; here it is the real mime type,
 *    so screens can use it directly as the multipart content-type (RN screens do `image.type || 'image/jpeg'`).
 *  - [size] bytes.
 *  - [readBytes] NEW (KMP only): for items PICKED by the user, reads the file content so the screen can upload it with
 *    `Api.upload(.., files = listOf(ApiFilePart(field, item.name, item.type, item.readBytes!!())))`.
 *    It is `null` for items that came from the server (those only have a remote [uri]; in RN, `fetch` of the local uri
 *    was done only for freshly picked files — test [isLocal] / `readBytes != null`).
 */
data class ImageItem(
    val id: String,
    val uri: String,
    val name: String? = null,
    val type: String? = null,
    val size: Long? = null,
    val readBytes: (suspend () -> ByteArray)? = null
) {
    /** true when this item was picked on this device (RN: uri that is not http/https). */
    val isLocal: Boolean get() = !(uri.startsWith("http://") || uri.startsWith("https://"))

    /** Bytes of a picked item (null for remote items). */
    suspend fun bytesOrNull(): ByteArray? = readBytes?.invoke()
}

/**
 * Value passed to `onImageChange` — in RN this is `ImageItem[]` for `isMultiple` and `ImageItem | null` otherwise.
 *  - [list]   : RN's `imageArray` normalisation that the Add* screens do (`Array.isArray(images) ? images.filter(uri) : images?.uri ? [images] : []`).
 *  - [single] : the RN single value (`ImageItem | null`) → first item or null (null = the image was removed).
 *  - [isArray]: true when RN would have passed an array (`isMultiple = true`).
 */
class ImageChange internal constructor(val isArray: Boolean, private val items: List<ImageItem>) {
    val list: List<ImageItem> get() = items
    val single: ImageItem? get() = items.firstOrNull()
}

private object ImageUploadIds {
    private var counter = 0L
    fun next(): String { counter += 1; return "$counter" }
}

/**
 * Port of `app/components/ImageUpload.tsx` (a picker + preview component; in RN it performs NO network request and
 * exposes NO imperative ref methods — the screens upload/delete themselves in `onImageChange` / on submit, so no
 * controller object is needed).
 *
 * Props mirror RN 1:1 (unused-by-RN props such as `multiple`, `imageResizeMode` are accepted/ignored):
 *  - [onImagesChange] `(ImageItem[]) -> void`, called only when `isMultiple`.
 *  - [onImageChange]  `(ImageItem[] | ImageItem | null) -> void` → [ImageChange] (`.list` / `.single`).
 *  - [maxImages]=5, [imageQuality]=0.8 (picker side, not applied here), [allowCamera]=true, [allowGallery]=true,
 *    [allowImages]=true, [isMultiple]=true, [initialImages]/[initialImage] (re-synced when changed, like RN's `useEffect`),
 *    [placeholder], [aspectRatio]=16:9, [allowEditing]=true, [allowVideos]=false, [allowFreeAspectRatio]=false,
 *    [loading]=false, [error], [onShowToast] `(message, type)` with type `"success" | "error" | "warning"`, [onPress]
 *    (called instead of opening the viewer when a VIDEO thumbnail is tapped).
 *  - RN `style`: [modifier] = the style's background / border / borderRadius / padding / width (e.g. the screens'
 *    `imageUploadContainer`: `Modifier.background(rgba(255,255,255,0.3f), shape).border(1.dp, rgba(255,206,232,0.2f), shape).padding(15.dp)`),
 *    applied AFTER the container margins. The container's own `marginVertical: 15` is split into [marginTop] / [marginBottom]
 *    (defaults 15.dp) so a style override like `marginTop: 5` / `marginBottom: 10` becomes `marginTop = 5.dp` / `marginBottom = 10.dp`.
 *  - To re-create the component (RN `key={...}`) wrap the call in `key(value) { ImageUpload(..) }`.
 *
 * Platform limitations (the `MediaPickerController` interface has no equivalent): cropping (`allowsEditing` / `aspect`) is not
 * performed; "images + videos" gallery mode uses `pickDocument` with image and video wildcards; camera capture is photo-only.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImageUpload(
    modifier: Modifier = Modifier,
    onImagesChange: ((List<ImageItem>) -> Unit)? = null,
    onImageChange: ((ImageChange) -> Unit)? = null,
    maxImages: Int = 5,
    imageQuality: Float = 0.8f,
    allowCamera: Boolean = true,
    allowGallery: Boolean = true,
    allowImages: Boolean = true,
    marginTop: Dp = 15.dp,
    marginBottom: Dp = 15.dp,
    error: String? = null,
    isMultiple: Boolean = true,
    initialImages: List<ImageItem> = emptyList(),
    initialImage: ImageItem? = null,
    placeholder: String? = null,
    aspectRatio: Pair<Int, Int> = 16 to 9,
    allowEditing: Boolean = true,
    allowVideos: Boolean = false,
    onShowToast: ((message: String, type: String) -> Unit)? = null,
    loading: Boolean = false,
    imageResizeMode: String = "contain",
    allowFreeAspectRatio: Boolean = false,
    onPress: (() -> Unit)? = null
) {
    val picker = rememberMediaPicker()
    val scope = rememberCoroutineScope()
    val insets = rememberSafeAreaInsets()

    val onImagesChangeState by rememberUpdatedState(onImagesChange)
    val onImageChangeState by rememberUpdatedState(onImageChange)
    val onShowToastState by rememberUpdatedState(onShowToast)
    val onPressState by rememberUpdatedState(onPress)

    var images by remember { mutableStateOf<List<ImageItem>>(if (isMultiple) initialImages else emptyList()) }
    var singleImage by remember { mutableStateOf<ImageItem?>(if (isMultiple) null else initialImage) }
    var isUploading by remember { mutableStateOf(false) }
    var modalVisible by remember { mutableStateOf(false) }
    var deleteModalVisible by remember { mutableStateOf(false) }
    var imageToDelete by remember { mutableStateOf<String?>(null) }
    var imageViewerVisible by remember { mutableStateOf(false) }
    var selectedImageForView by remember { mutableStateOf<ImageItem?>(null) }

    // Animation values (RN Animated.Value refs)
    val modalSlideAnim = remember { Animatable(300f) }
    val modalOpacityAnim = remember { Animatable(0f) }
    val deleteModalSlideAnim = remember { Animatable(0f) }
    val deleteModalBackdropAnim = remember { Animatable(0f) }
    val imageViewerScaleAnim = remember { Animatable(0f) }
    val imageViewerOpacityAnim = remember { Animatable(0f) }

    fun computeCurrentImages(): List<ImageItem> =
        if (isMultiple) images.filter { it.uri.isNotEmpty() }
        else singleImage?.takeIf { it.uri.isNotEmpty() }?.let { listOf(it) } ?: emptyList()

    val currentImages = computeCurrentImages()
    val canAddMore = if (isMultiple) currentImages.size < maxImages else singleImage == null
    val busy = isUploading || loading

    fun showToastMessage(message: String, type: String = "error") {
        onShowToastState?.invoke(message, type)
    }

    // ── Animations ────────────────────────────────────────────────────────────
    LaunchedEffect(modalVisible) {
        coroutineScope {
            if (modalVisible) {
                launch { modalSlideAnim.animateTo(0f, rnTiming(300)) }
                launch { modalOpacityAnim.animateTo(1f, rnTiming(300)) }
            } else {
                launch { modalSlideAnim.animateTo(300f, rnTiming(250)) }
                launch { modalOpacityAnim.animateTo(0f, rnTiming(250)) }
            }
        }
    }
    LaunchedEffect(deleteModalVisible) {
        coroutineScope {
            if (deleteModalVisible) {
                launch { deleteModalSlideAnim.animateTo(1f, rnTiming(300)) }
                launch { deleteModalBackdropAnim.animateTo(1f, rnTiming(300)) }
            } else {
                launch { deleteModalSlideAnim.animateTo(0f, rnTiming(250)) }
                launch { deleteModalBackdropAnim.animateTo(0f, rnTiming(250)) }
            }
        }
    }
    LaunchedEffect(imageViewerVisible) {
        coroutineScope {
            if (imageViewerVisible) {
                launch { imageViewerScaleAnim.animateTo(1f, rnSpring(tension = 100f, friction = 8f)) }
                launch { imageViewerOpacityAnim.animateTo(1f, rnTiming(300)) }
            } else {
                launch { imageViewerScaleAnim.animateTo(0.7f, rnTiming(200)) }
                launch { imageViewerOpacityAnim.animateTo(0f, rnTiming(200)) }
            }
        }
    }

    // ── Helpers (normalize / signature) ───────────────────────────────────────
    fun normalizeImageItem(image: ImageItem?, fallbackId: String?): ImageItem? {
        if (image == null || image.uri.isEmpty()) return null
        val uri = image.uri
        val isMp4 = uri.lowercase().contains(".mp4")
        return image.copy(
            id = image.id.ifEmpty { fallbackId ?: "image_$uri" },
            name = image.name?.takeIf { it.isNotEmpty() } ?: (if (isMp4) "ویدئو" else "تصویر"),
            type = image.type?.takeIf { it.isNotEmpty() } ?: (if (isMp4) "video/mp4" else "image/jpeg")
        )
    }

    fun getImagesSignature(items: List<ImageItem>): String =
        items.map { it.uri }.filter { it.isNotEmpty() }.joinToString("|")

    // Sync with initialImages / initialImage (RN useEffect on [initialImage, initialImages, isMultiple])
    LaunchedEffect(initialImage, initialImages, isMultiple) {
        if (isMultiple) {
            val normalizedInitialImages = initialImages
                .mapIndexedNotNull { index, image -> normalizeImageItem(image, "initial-image-$index") }
            if (getImagesSignature(images) != getImagesSignature(normalizedInitialImages)) {
                images = normalizedInitialImages
            }
            return@LaunchedEffect
        }
        val normalizedInitialImage = normalizeImageItem(initialImage, "initial-single-image")
        if (singleImage?.uri != normalizedInitialImage?.uri) {
            singleImage = normalizedInitialImage
        }
    }

    fun updateImages(newImages: List<ImageItem>) {
        val validImages = newImages.filter { it.uri.isNotEmpty() }
        if (isMultiple) {
            images = validImages
            onImagesChangeState?.invoke(validImages)
            onImageChangeState?.invoke(ImageChange(true, validImages))
        } else {
            val newImage = validImages.firstOrNull()
            singleImage = newImage
            onImageChangeState?.invoke(ImageChange(false, listOfNotNull(newImage)))
        }
    }

    fun validateFileSize(assets: List<PickedMedia>): List<PickedMedia> = assets.filter { asset ->
        val fileSize = asset.fileSize
        if (fileSize != null && fileSize != 0L) {
            val video = asset.isVideo
            val maxSize = if (video) 60L * 1024 * 1024 else 5L * 1024 * 1024
            if (fileSize > maxSize) {
                val fileSizeInMB = iuToFixed1(fileSize / (1024.0 * 1024.0))
                val maxSizeInMB = if (video) "۶۰" else "۵"
                showToastMessage(
                    "حجم ${if (video) "ویدئو" else "تصویر"} ($fileSizeInMB مگابایت) نباید بیشتر از $maxSizeInMB مگابایت باشد"
                )
                false
            } else true
        } else true
    }

    fun isAssetVideo(asset: PickedMedia) = asset.isVideo

    fun showImageSourceOptions() {
        modalVisible = true
    }

    // ── pickers ───────────────────────────────────────────────────────────────
    fun handleGalleryResult(assets: List<PickedMedia>, enableMultipleSelection: Boolean) {
        try {
            if (assets.isNotEmpty()) {
                val validAssets = validateFileSize(assets)
                if (validAssets.isEmpty()) {
                    isUploading = false
                    return
                }
                val stamp = ImageUploadIds.next()
                val newImages = validAssets.mapIndexedNotNull { index, asset ->
                    if (asset.uri.isEmpty()) null
                    else {
                        val video = isAssetVideo(asset)
                        ImageItem(
                            id = stamp + index,
                            uri = asset.uri,
                            name = asset.fileName.ifEmpty {
                                "${if (video) "video" else "image"}_$stamp.${if (video) "mp4" else "jpg"}"
                            },
                            type = asset.mimeType.ifEmpty {
                                if (allowVideos && !allowImages) "video/mp4" else "image/jpeg"
                            },
                            size = asset.fileSize,
                            readBytes = { asset.bytes() }
                        )
                    }
                }

                if (isMultiple && enableMultipleSelection) {
                    val updatedImages = (computeCurrentImages() + newImages).take(maxImages)
                    updateImages(updatedImages)
                } else {
                    updateImages(newImages)
                }
                modalVisible = false

                if (validAssets.size < assets.size) {
                    showToastMessage("برخی فایل‌ها به دلیل حجم زیاد نادیده گرفته شدند", "warning")
                }
            }
        } catch (e: Exception) {
            showToastMessage("مشکلی در انتخاب فایل پیش آمد")
        } finally {
            isUploading = false
        }
    }

    fun pickImageFromGallery() {
        isUploading = true
        try {
            val shouldEnableEditing = !isMultiple && (allowEditing || allowFreeAspectRatio) && !allowVideos
            val enableMultipleSelection = isMultiple && maxImages > 1 && !shouldEnableEditing
            val selectionCount =
                if (isMultiple) maxOf(1, minOf(maxImages - computeCurrentImages().size, maxImages)) else 1
            val onDenied: (String) -> Unit = {
                showToastMessage("برای انتخاب فایل، مجوز دسترسی به گالری لازم است")
                isUploading = false
            }
            val onResult: (List<PickedMedia>) -> Unit = { assets ->
                handleGalleryResult(assets, enableMultipleSelection)
            }
            if (allowVideos && allowImages) {
                // mediaTypes: 'All'
                picker.pickDocument(listOf("image/*", "video/*"), onResult)
            } else if (allowVideos && !allowImages) {
                // mediaTypes: 'Videos'
                picker.pickVideo(onDenied, onResult)
            } else {
                // mediaTypes: 'Images'
                picker.pickImages(enableMultipleSelection, selectionCount, onDenied, onResult)
            }
        } catch (e: Exception) {
            showToastMessage("مشکلی در انتخاب فایل پیش آمد")
            isUploading = false
        }
    }

    fun handleCameraResult(assets: List<PickedMedia>) {
        try {
            if (assets.isNotEmpty()) {
                val asset = assets[0]
                if (asset.uri.isEmpty()) {
                    showToastMessage("خطا در دریافت فایل از دوربین")
                    isUploading = false
                    return
                }

                val validAssets = validateFileSize(assets)
                if (validAssets.isEmpty()) {
                    isUploading = false
                    return
                }

                val validAsset = validAssets[0]
                val video = isAssetVideo(validAsset)
                val stamp = ImageUploadIds.next()
                val newImage = ImageItem(
                    id = stamp,
                    uri = validAsset.uri,
                    name = validAsset.fileName.ifEmpty {
                        "${if (video) "captured_video" else "captured_photo"}_$stamp.${if (video) "mp4" else "jpg"}"
                    },
                    type = validAsset.mimeType.ifEmpty {
                        if (allowVideos && !allowImages) "video/mp4" else "image/jpeg"
                    },
                    size = validAsset.fileSize,
                    readBytes = { validAsset.bytes() }
                )

                if (isMultiple) {
                    val updatedImages = (computeCurrentImages() + newImage).take(maxImages)
                    updateImages(updatedImages)
                } else {
                    updateImages(listOf(newImage))
                }
                modalVisible = false
                showToastMessage("${if (video) "ویدئو" else "عکس"} با موفقیت ضبط شد", "success")
            }
        } catch (e: Exception) {
            val m = e.message
            if (m != null && (m.contains("User cancelled") || m.contains("cancelled"))) {
                // cancelled: nothing
            } else if (m != null && m.contains("Camera permission")) {
                showToastMessage("مجوز دسترسی به دوربین لازم است")
            } else if (m != null && m.contains("not available")) {
                showToastMessage("دوربین در دسترس نیست")
            } else {
                showToastMessage("مشکلی در ضبط ویدئو پیش آمد: " + (if (m.isNullOrEmpty()) "خطای نامشخص" else m))
            }
        } finally {
            isUploading = false
        }
    }

    fun takePhoto() {
        isUploading = true
        try {
            picker.takePhoto(
                onPermissionDenied = {
                    showToastMessage("برای ضبط ویدئو، مجوز دسترسی به دوربین لازم است")
                    isUploading = false
                },
                onResult = { assets -> handleCameraResult(assets) }
            )
        } catch (e: Exception) {
            showToastMessage("مشکلی در ضبط ویدئو پیش آمد: " + (e.message ?: "خطای نامشخص"))
            isUploading = false
        }
    }

    // ── delete / viewer handlers ─────────────────────────────────────────────
    fun removeImage(imageId: String) {
        imageToDelete = imageId
        deleteModalVisible = true
    }

    fun handleCloseDeleteModal() {
        deleteModalVisible = false
        imageToDelete = null
    }

    fun handleImagePress(image: ImageItem) {
        selectedImageForView = image
        imageViewerVisible = true
    }

    fun handleCloseImageViewer() {
        imageViewerVisible = false
        scope.launch {
            delay(300)
            selectedImageForView = null
        }
    }

    fun confirmRemoveImage() {
        val toDelete = imageToDelete
        if (!toDelete.isNullOrEmpty()) {
            if (isMultiple) {
                val updatedImages = computeCurrentImages().filter { it.id != toDelete }
                updateImages(updatedImages)
            } else {
                updateImages(emptyList())
            }
        }
        handleCloseDeleteModal()
    }

    // ── texts ─────────────────────────────────────────────────────────────────
    fun getDefaultPlaceholder(): String {
        if (!placeholder.isNullOrEmpty()) return placeholder
        return if (allowVideos && !allowImages) {
            if (isMultiple) "ویدئوهای پست" else "ویدئو"
        } else if (!allowVideos && allowImages) {
            if (isMultiple) "تصاویر پست" else "تصویر"
        } else {
            if (isMultiple) "فایل‌های پست" else "فایل"
        }
    }

    fun getUploadText(): String {
        val fileType = if (allowVideos && !allowImages) "ویدئو"
        else if (!allowVideos && allowImages) "تصویر"
        else "فایل"
        if (isMultiple) {
            return if (canAddMore) "برای افزودن $fileType کلیک کنید" else "حداکثر تعداد $fileType انتخاب شده"
        }
        return if (singleImage != null) "برای تغییر $fileType کلیک کنید" else "برای انتخاب $fileType کلیک کنید"
    }

    fun getAspectRatioText(): String {
        if (allowFreeAspectRatio) return "آزاد"
        return "${aspectRatio.first}:${aspectRatio.second}"
    }

    fun getUploadTypeText(): String {
        if (allowVideos && allowImages) {
            return if (isMultiple) "تکی و چندتایی" else "تکی"
        } else if (allowVideos && !allowImages) {
            return if (isMultiple) "ویدئو (تکی و چندتایی)" else "ویدئو (تکی)"
        } else if (!allowVideos && allowImages) {
            return if (isMultiple) "تصویر (تکی و چندتایی)" else "تصویر (تکی)"
        }
        return if (isMultiple) "تکی و چندتایی" else "تکی"
    }

    // ──────────────────────────────────────────────────────────────────────────
    // UI
    // ──────────────────────────────────────────────────────────────────────────
    // container: marginVertical 15 (+ style)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = marginTop, bottom = marginBottom)
            .then(modifier)
    ) {
        // header
        RowReverse(
            Modifier.fillMaxWidth().padding(bottom = 15.dp).padding(horizontal = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // headerLeft: row-reverse, center, flex 1, wrap
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                FlowRow(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Start,
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        MaterialIcons(
                            if (allowVideos) "perm-media" else "photo-camera",
                            size = 24.dp,
                            color = FarimodColors.Primary
                        )
                        AppText(
                            getDefaultPlaceholder(),
                            modifier = Modifier.absolutePadding(right = 8.dp),
                            fontSize = 18.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            color = FarimodColors.Primary
                        )
                        if (!isMultiple && (allowEditing || allowFreeAspectRatio) && !allowVideos) {
                            // aspectRatioBadge
                            Box(
                                Modifier
                                    .absolutePadding(right = 8.dp)
                                    .background(IUPrimaryAlpha10, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                AppText(
                                    getAspectRatioText(),
                                    fontSize = 12.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    color = FarimodColors.Primary
                                )
                            }
                        }
                        // uploadTypeBadge
                        Box(
                            Modifier
                                .absolutePadding(right = 8.dp)
                                .background(rgba(34, 139, 173, 0.1f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            AppText(
                                getUploadTypeText(),
                                fontSize = 10.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Regular,
                                color = Color(0xFF228BAD)
                            )
                        }
                    }
                }
            }
            if (isMultiple) {
                AppText(
                    "${currentImages.size}/$maxImages",
                    modifier = Modifier
                        .background(IUPrimaryAlpha10, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    fontSize = 14.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    color = FarimodColors.Medium
                )
            }
        }

        // upload area
        if (canAddMore) {
            val areaShape = RoundedCornerShape(20.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .then(if (busy) Modifier.alpha(0.7f) else Modifier)
                    .touchableOpacity(enabled = !busy, activeOpacity = 0.7f, onClick = { showImageSourceOptions() })
                    .clip(areaShape)
                    .iuDashedBorder(2.dp, IUPrimaryAlpha20, 20.dp)
                    .padding(2.dp)
            ) {
                // uploadGradient: padding 30
                Box(
                    Modifier
                        .fillMaxWidth()
                        .linearGradient(listOf(IUPrimaryAlpha10, IUPrimaryAlpha05))
                        .padding(30.dp)
                ) {
                    // uploadContent
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (busy) {
                            val transition = rememberInfiniteTransition()
                            val rotation by transition.animateFloat(
                                initialValue = 0f,
                                targetValue = 360f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = RNTimingEasing),
                                    repeatMode = RepeatMode.Restart
                                )
                            )
                            Box(
                                Modifier
                                    .padding(bottom = 15.dp)
                                    .graphicsLayer { rotationZ = rotation }
                            ) {
                                MaterialIcons("refresh", size = 32.dp, color = FarimodColors.Primary)
                            }
                        } else {
                            // uploadIconContainer (+ uploadIconRing: absolute -10 on every side, radius 30, dashed 2)
                            Box(
                                Modifier
                                    .padding(bottom = 15.dp)
                                    .iuDashedBorder(2.dp, IUPrimaryAlpha20, 30.dp, outset = 10.dp)
                            ) {
                                MaterialIcons(
                                    if (allowVideos) "perm-media" else (if (isMultiple) "add-photo-alternate" else "add-a-photo"),
                                    size = 40.dp,
                                    color = FarimodColors.Primary
                                )
                                if (allowEditing && !isMultiple) {
                                    // cropIcon: absolute bottom -5, right -5
                                    Box(
                                        Modifier
                                            .align(AbsoluteAlignment.BottomRight)
                                            .absoluteOffset(x = 5.dp, y = 5.dp)
                                            .background(rgba(255, 255, 255, 0.9f), RoundedCornerShape(10.dp))
                                            .padding(2.dp)
                                    ) {
                                        MaterialIcons("edit", size = 16.dp, color = FarimodColors.Primary)
                                    }
                                }
                            }
                            AppText(
                                getUploadText(),
                                modifier = Modifier.padding(bottom = 5.dp),
                                fontSize = 16.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Bold,
                                color = FarimodColors.Primary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // images
        if (currentImages.isNotEmpty()) {
            Column(
                Modifier.fillMaxWidth().padding(bottom = 15.dp),
                horizontalAlignment = if (!isMultiple && singleImage != null) Alignment.CenterHorizontally else Alignment.Start
            ) {
                if (isMultiple) {
                    // ScrollView horizontal, height 140, contentContainer: paddingHorizontal 5, paddingVertical 10, gap 15
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 5.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        currentImages.forEachIndexed { index, item ->
                            key(item.id) {
                                val video = iuIsVideo(item) && iuIsValidVideoFormat(item)
                                IUImageCard(
                                    item = item,
                                    width = 120.dp,
                                    height = 120.dp,
                                    radius = 15.dp,
                                    showAsVideo = video,
                                    nameFallback = if (iuIsVideo(item)) "فیلم" else "تصویر",
                                    onPress = {
                                        if (item.uri.isNotEmpty() && iuIsVideo(item) && onPressState != null) {
                                            onPressState?.invoke()
                                        } else {
                                            handleImagePress(item)
                                        }
                                    },
                                    onRemove = { removeImage(item.id) },
                                    indexBadge = "${index + 1}",
                                    onEdit = null,
                                    cropBadge = null
                                )
                            }
                        }
                    }
                } else {
                    val single = singleImage
                    // getImageItemStyle: width 250, height = 250 * (h / w)
                    val ratio = aspectRatio.second.toFloat() / aspectRatio.first.toFloat()
                    val imageWidth = 250.dp
                    val imageHeight = imageWidth * ratio
                    IUImageCard(
                        item = single,
                        width = imageWidth,
                        height = imageHeight,
                        radius = 20.dp,
                        showAsVideo = single != null && single.uri.isNotEmpty() && iuIsVideo(single),
                        nameFallback = if (single != null && single.uri.isNotEmpty() && iuIsVideo(single)) "فیلم" else "تصویر",
                        onPress = {
                            if (single != null && iuIsVideo(single) && onPressState != null) {
                                onPressState?.invoke()
                            } else if (single != null) {
                                handleImagePress(single)
                            }
                        },
                        onRemove = { single?.let { removeImage(it.id) } },
                        indexBadge = null,
                        onEdit = { showImageSourceOptions() },
                        cropBadge = if (allowEditing && !isMultiple) getAspectRatioText() else null
                    )
                }
            }
        }

        // error
        if (!error.isNullOrEmpty()) {
            RowReverse(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .background(rgba(231, 76, 60, 0.1f), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MaterialIcons("error-outline", size = 16.dp, color = FarimodColors.Danger)
                AppText(
                    error,
                    modifier = Modifier.weight(1f).absolutePadding(right = 5.dp),
                    fontSize = 12.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    color = FarimodColors.Danger
                )
            }
        }

        // guidelines  (JSX whitespace between the {} expressions collapses to nothing — kept verbatim)
        val guidelineShape = RoundedCornerShape(10.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .background(IUPrimaryAlpha05, guidelineShape)
                .border(1.dp, IUPrimaryAlpha10, guidelineShape)
                .padding(12.dp)
        ) {
            val guidelinesText = buildString {
                append("• ")
                append(if (isMultiple) "حداکثر $maxImages فایل" else "یک فایل")
                if (!isMultiple && allowEditing && !allowVideos) append(" • نسبت ابعاد: ${getAspectRatioText()}")
                append("• فرمت‌های مجاز: ")
                append(
                    if (allowImages && allowVideos) "JPG, PNG, MP4, MOV"
                    else if (allowVideos && !allowImages) "MP4, MOV"
                    else "JPG, PNG"
                )
                append("• حداکثر حجم: ")
                append(
                    if (allowVideos && !allowImages) "۶۰ مگابایت"
                    else if (!allowVideos && allowImages) "۵ مگابایت"
                    else "تصویر: ۵ مگابایت، ویدئو: ۶۰ مگابایت"
                )
                if (allowVideos) append(" • بدون محدودیت زمان")
            }
            AppText(
                guidelinesText,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 11.sp,
                fontFamily = RNFonts.Yekan_Bakh_Regular,
                color = FarimodColors.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }

    // Modal for selecting image source
    if (modalVisible) {
        val editingSingle = !isMultiple && allowEditing && !allowVideos
        IUSourceModal(
            title = if (isMultiple) "انتخاب روش آپلود"
            else if (singleImage != null) "تغییر ${if (allowVideos) "فایل" else "تصویر"}"
            else "انتخاب ${if (allowVideos) "فایل" else "تصویر"}",
            subtitle = if (editingSingle) "تصویر با نسبت ابعاد ${getAspectRatioText()} برش داده می‌شود" else null,
            showGallery = allowGallery,
            galleryDescription = if (editingSingle) "انتخاب و ویرایش تصویر"
            else "انتخاب از ${if (allowVideos) "فایل‌های" else "تصاویر"} موجود",
            showCamera = allowCamera,
            cameraDescription = if (editingSingle) "عکس گرفتن و ویرایش"
            else "گرفتن ${if (allowVideos) "عکس/فیلم" else "عکس"} جدید",
            slideY = modalSlideAnim.value,
            opacity = modalOpacityAnim.value,
            bottomInset = insets.bottom,
            onClose = { modalVisible = false },
            onGallery = { pickImageFromGallery() },
            onCamera = { takePhoto() }
        )
    }

    // Delete confirmation modal
    if (deleteModalVisible) {
        val sel = selectedImageForView
        val kind = if (sel != null && sel.uri.isNotEmpty() && iuIsVideo(sel)) "فیلم" else "تصویر"
        IUDeleteModal(
            kindLabel = kind,
            slide = deleteModalSlideAnim.value,
            backdropAlpha = deleteModalBackdropAnim.value,
            bottomInset = insets.bottom,
            onClose = { handleCloseDeleteModal() },
            onConfirm = { confirmRemoveImage() }
        )
    }

    // Image viewer modal
    if (imageViewerVisible) {
        IUViewerModal(
            item = selectedImageForView,
            scale = imageViewerScaleAnim.value,
            opacity = imageViewerOpacityAnim.value,
            onClose = { handleCloseImageViewer() },
            onDelete = { sel ->
                handleCloseImageViewer()
                scope.launch {
                    delay(300)
                    removeImage(sel.id)
                }
            },
            onEdit = {
                handleCloseImageViewer()
                scope.launch {
                    delay(300)
                    showImageSourceOptions()
                }
            }
        )
    }
}

/**
 * One preview card (`imageItem` → TouchableOpacity(.8) → `imageWrapper`):
 * media + removeButton (+ editButton, cropBadge for single) + imageInfo (+ imageBadge for multiple).
 */
@Composable
private fun IUImageCard(
    item: ImageItem?,
    width: Dp,
    height: Dp,
    radius: Dp,
    showAsVideo: Boolean,
    nameFallback: String,
    onPress: () -> Unit,
    onRemove: () -> Unit,
    indexBadge: String?,
    onEdit: (() -> Unit)?,
    cropBadge: String?
) {
    // shadow (elevation 3) of `imageWrapper` is invisible in RN (transparent background + overflow hidden) → omitted
    Box(
        Modifier
            .touchableOpacity(activeOpacity = 0.8f, onClick = onPress)
            .size(width, height)
            .clip(RoundedCornerShape(radius))
    ) {
        if (showAsVideo && item != null) {
            IUVideoThumb(item, onPress)
        } else if (item != null && item.uri.isNotEmpty()) {
            // styles.image: 100% × 100%, resizeMode contain
            NetworkImage(item.uri, Modifier.fillMaxSize(), resizeMode = ResizeMode.contain)
        } else {
            IUErrorImage()
        }

        // removeButton: absolute top 8, left 8
        IUCornerButton(
            colors = listOf(Color(0xFFFF4757), Color(0xFFFF3742)),
            iconName = "close",
            modifier = Modifier.align(AbsoluteAlignment.TopLeft).absoluteOffset(x = 8.dp, y = 8.dp),
            onClick = onRemove
        )

        // editButton: absolute top 8, right 8
        if (onEdit != null) {
            IUCornerButton(
                colors = listOf(FarimodColors.Primary, FarimodColors.PrimaryDark),
                iconName = "edit",
                modifier = Modifier.align(AbsoluteAlignment.TopRight).absoluteOffset(x = (-8).dp, y = 8.dp),
                onClick = onEdit
            )
        }

        // cropBadge: absolute bottom 8, right 8
        if (cropBadge != null) {
            Box(
                Modifier
                    .align(AbsoluteAlignment.BottomRight)
                    .absoluteOffset(x = (-8).dp, y = (-8).dp)
                    .background(rgba(158, 34, 173, 0.9f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                AppText(
                    cropBadge,
                    fontSize = 10.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color.White
                )
            }
        }

        // imageInfo
        IUImageInfo(
            name = item?.name?.takeIf { it.isNotEmpty() } ?: nameFallback,
            sizeText = item?.size?.takeIf { it != 0L }?.let { iuFormatFileSize(it) }
        )

        // imageBadge: absolute top 8, right 8, 20×20 primary circle-ish (radius 10)
        if (indexBadge != null) {
            Box(
                Modifier
                    .align(AbsoluteAlignment.TopRight)
                    .absoluteOffset(x = (-8).dp, y = 8.dp)
                    .size(20.dp)
                    .background(FarimodColors.Primary, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                AppText(
                    indexBadge,
                    fontSize = 10.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color.White
                )
            }
        }
    }
}
