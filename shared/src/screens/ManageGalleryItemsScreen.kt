package ir.farimod.app.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.hooks.useToast
import ir.farimod.app.navigation.useFocusEffect
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.navigation.useRoute
import ir.farimod.app.network.Api
import ir.farimod.app.network.ApiFilePart
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.list
import ir.farimod.app.network.str
import ir.farimod.app.platform.PickedMedia
import ir.farimod.app.platform.PlatformBackHandler
import ir.farimod.app.platform.rememberMediaPicker
import ir.farimod.app.screens.galleryitem.galleryUtcIsoNow
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.MainBackground
import ir.farimod.app.ui.NetworkImage
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.ResizeMode
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.rememberSafeAreaInsets
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.rnTiming
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.utils.currentTimeMillis
import ir.farimod.app.utils.toPersianDigits
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

private val ManagePrimary = Color(0xFF667EEA)
private val ManageError = Color(0xFFE74C3C)
private val ManageGreen = Color(0xFF4CAF50)

/**
 * Port of `screens/ManageGalleryItemsScreen.tsx`.
 * Params: galleryId, galleryTitle.
 * Upload: ImageGalleryItem/Add multipart (`imageFile`). Delete item and delete gallery use the RN query names.
 * RN has no drag-reorder; ShowOrder is the current item count at upload time.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManageGalleryItemsScreen() {
    val navigation = useNavigation()
    val route = useRoute()
    val toast = useToast()
    val scope = rememberCoroutineScope()
    val picker = rememberMediaPicker()
    val insets = rememberSafeAreaInsets()
    val statusTop = statusBarHeight()

    val galleryId = route.int("galleryId")
    val galleryTitle = route.str("galleryTitle")

    var galleryItems by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var deletingItemId by remember { mutableStateOf<Int?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    var modalVisible by remember { mutableStateOf(false) }
    var fullScreenUri by remember { mutableStateOf<String?>(null) }
    var showAction by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<JsonObject?>(null) }
    var showDeleteGallery by remember { mutableStateOf(false) }

    val pickerSlide = remember { Animatable(300f) }
    val pickerOpacity = remember { Animatable(0f) }
    val actionSlide = remember { Animatable(0f) }
    val actionBackdrop = remember { Animatable(0f) }
    val deleteSlide = remember { Animatable(0f) }
    val deleteBackdrop = remember { Animatable(0f) }

    suspend fun fetchGalleryItems() {
        if (galleryId == null) return
        try {
            loading = true
            val response = Api.get("ImageGallery/Get", query = mapOf("id" to galleryId))
            if (!response.ok) throw Exception("خطا در دریافت اطلاعات گالری")
            val result = response.json ?: throw Exception("JSON")
            val raw = when {
                result["ImageGallery"]["ImageGalleryItemList"] != null ->
                    result["ImageGallery"].list("ImageGalleryItemList")
                result["ImageGalleryItemList"] != null -> result.list("ImageGalleryItemList")
                result is JsonArray -> result.list
                else -> emptyList()
            }
            galleryItems = raw.mapNotNull { it as? JsonObject }
                .sortedBy { it.int("ShowOrder") ?: 0 }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            toast.showToast("خطا در دریافت تصاویر", "error")
            galleryItems = emptyList()
        } finally {
            loading = false
        }
    }

    useFocusEffect(galleryId) {
        fetchGalleryItems()
    }

    LaunchedEffect(modalVisible) {
        if (modalVisible) {
            coroutineScope {
                launch { pickerSlide.animateTo(0f, rnTiming(300)) }
                launch { pickerOpacity.animateTo(1f, rnTiming(300)) }
            }
        } else {
            pickerSlide.snapTo(300f)
            pickerOpacity.snapTo(0f)
        }
    }

    LaunchedEffect(showAction) {
        if (showAction) {
            coroutineScope {
                launch { actionBackdrop.animateTo(1f, rnTiming(300)) }
                launch { actionSlide.animateTo(1f, rnTiming(300)) }
            }
        }
    }

    LaunchedEffect(showDeleteGallery) {
        if (showDeleteGallery) {
            coroutineScope {
                launch { deleteBackdrop.animateTo(1f, rnTiming(300)) }
                launch { deleteSlide.animateTo(1f, rnTiming(300)) }
            }
        }
    }

    fun closeAction() {
        scope.launch {
            coroutineScope {
                launch { actionBackdrop.animateTo(0f, rnTiming(250)) }
                launch { actionSlide.animateTo(0f, rnTiming(250)) }
            }
            showAction = false
            selectedItem = null
        }
    }

    fun closeDeleteGallery() {
        scope.launch {
            coroutineScope {
                launch { deleteBackdrop.animateTo(0f, rnTiming(250)) }
                launch { deleteSlide.animateTo(0f, rnTiming(250)) }
            }
            showDeleteGallery = false
        }
    }

    fun uploadNewImage(media: PickedMedia) {
        scope.launch {
            try {
                uploading = true
                val bytes = media.bytes()
                val fileName = media.fileName.ifBlank { "gallery-item-${currentTimeMillis()}.jpg" }
                var mime = media.mimeType.ifBlank { "image/jpeg" }
                if (!mime.startsWith("image/")) {
                    val ext = fileName.substringAfterLast('.', "").lowercase()
                    mime = if (ext == "png") "image/png" else "image/jpeg"
                }
                val response = Api.upload(
                    path = "ImageGalleryItem/Add",
                    files = listOf(ApiFilePart("imageFile", fileName, mime, bytes)),
                    fields = mapOf(
                        "ImageGalleryItemId" to "0",
                        "ImageGalleryId" to (galleryId?.toString() ?: "0"),
                        "Title" to (galleryTitle?.takeIf { it.isNotEmpty() } ?: "تصویر جدید"),
                        "ImageFileName" to "",
                        "ImageURL" to "",
                        "ShowOrder" to galleryItems.size.toString(),
                        "Active" to "true",
                        "ActiveStr" to "فعال",
                        "InsertDate" to galleryUtcIsoNow(),
                        "ShamsiInsertDate" to ""
                    ),
                    timeoutMs = 60_000
                )
                if (!response.ok) {
                    throw Exception(response.json.str("Message")?.takeIf { it.isNotEmpty() } ?: "خطا در آپلود تصویر")
                }
                toast.showToast("تصویر با موفقیت اضافه شد", "success")
                fetchGalleryItems()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toast.showToast(e.message?.takeIf { it.isNotEmpty() } ?: "خطا در آپلود تصویر", "error")
            } finally {
                uploading = false
            }
        }
    }

    fun confirmDeleteItem() {
        val item = selectedItem ?: return
        val itemId = item.int("ImageGalleryItemId") ?: return
        val gid = galleryId ?: return
        closeAction()
        scope.launch {
            try {
                deletingItemId = itemId
                val response = Api.delete(
                    "ImageGalleryItem/Delete",
                    query = mapOf("imageGalleryItemId" to itemId, "imageGalleryId" to gid)
                )
                if (response.ok) {
                    toast.showToast("تصویر با موفقیت حذف شد", "success")
                    fetchGalleryItems()
                } else {
                    throw Exception(response.json.str("Message")?.takeIf { it.isNotEmpty() } ?: "خطا در حذف تصویر")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toast.showToast(e.message?.takeIf { it.isNotEmpty() } ?: "خطا در حذف تصویر", "error")
            } finally {
                deletingItemId = null
                selectedItem = null
            }
        }
    }

    fun confirmDeleteGallery() {
        val gid = galleryId ?: return
        closeDeleteGallery()
        scope.launch {
            try {
                isDeleting = true
                val response = Api.delete("ImageGallery/Delete", query = mapOf("imageGalleryId" to gid))
                if (response.ok) {
                    toast.showToast("گالری با موفقیت حذف شد", "success")
                    navigation.navigate("MyGallery")
                } else {
                    throw Exception(response.json.str("Message")?.takeIf { it.isNotEmpty() } ?: "خطا در حذف گالری")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                toast.showToast(e.message?.takeIf { it.isNotEmpty() } ?: "خطا در حذف گالری", "error")
            } finally {
                isDeleting = false
            }
        }
    }

    val scroll = rememberScrollState()
    val pullState = rememberPullToRefreshState()

    PlatformBackHandler(fullScreenUri != null || showDeleteGallery || showAction || modalVisible) {
        when {
            fullScreenUri != null -> fullScreenUri = null
            showDeleteGallery -> closeDeleteGallery()
            showAction -> closeAction()
            modalVisible -> modalVisible = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        MainBackground()
        Column(Modifier.fillMaxSize()) {
            RowReverse(
                Modifier
                    .fillMaxWidth()
                    .padding(top = statusTop + 30.dp)
                    .absolutePadding(left = 20.dp, right = 20.dp, bottom = 15.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(50.dp)
                        .rnShadow(5.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f))
                        .touchableOpacity { navigation.goBack() },
                    contentAlignment = Alignment.Center
                ) {
                    MaterialIcons(name = "arrow-forward", size = 26.dp, color = Color(0xFF6366F1))
                }
                Column(
                    Modifier.weight(1f).absolutePadding(left = 10.dp, right = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AppText(
                        text = galleryTitle?.takeIf { it.isNotEmpty() } ?: "مدیریت گالری",
                        fontSize = 20.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color(0xFF2C3E50),
                        textAlign = TextAlign.Center
                    )
                    AppText(
                        text = "${toPersianDigits(galleryItems.size.toString())} تصویر",
                        modifier = Modifier.padding(top = 2.dp),
                        fontSize = 14.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        color = Color(0xFF7F8C8D),
                        textAlign = TextAlign.Center
                    )
                }
                Box(
                    Modifier
                        .size(50.dp)
                        .rnShadow(5.dp, CircleShape, ManageGreen)
                        .clip(CircleShape)
                        .background(ManageGreen)
                        .touchableOpacity(enabled = !uploading) { modalVisible = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (uploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        MaterialIcons(name = "add-a-photo", size = 26.dp, color = Color.White)
                    }
                }
            }

            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = {
                    scope.launch {
                        refreshing = true
                        fetchGalleryItems()
                        refreshing = false
                    }
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = pullState,
                indicator = {
                    PullToRefreshDefaults.Indicator(
                        state = pullState,
                        isRefreshing = refreshing,
                        modifier = Modifier.align(Alignment.TopCenter),
                        containerColor = Color.White,
                        color = ManagePrimary
                    )
                }
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll)
                        .padding(15.dp)
                ) {
                    when {
                        loading -> Column(
                            Modifier.fillMaxWidth().padding(top = 100.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = ManagePrimary)
                            AppText(
                                text = "در حال بارگذاری...",
                                modifier = Modifier.padding(top = 15.dp),
                                fontSize = 16.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Regular,
                                color = Color(0xFF7F8C8D),
                                textAlign = TextAlign.Center
                            )
                        }
                        galleryItems.isEmpty() -> Column(
                            Modifier.fillMaxWidth().padding(top = 100.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            MaterialIcons(name = "photo-library", size = 80.dp, color = Color(0xFFCCCCCC))
                            AppText(
                                text = "هنوز تصویری اضافه نشده",
                                modifier = Modifier.padding(top = 20.dp),
                                fontSize = 18.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Bold,
                                color = Color(0xFF7F8C8D),
                                textAlign = TextAlign.Center
                            )
                            AppText(
                                text = "از دکمه بالا تصویر جدید اضافه کنید",
                                modifier = Modifier.padding(top = 10.dp),
                                fontSize = 14.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Regular,
                                color = Color(0xFF95A5A6),
                                textAlign = TextAlign.Center
                            )
                        }
                        else -> {
                            FlowRow(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                maxItemsInEachRow = 2
                            ) {
                                galleryItems.forEach { item ->
                                    ManageImageCard(
                                        item = item,
                                        deleting = deletingItemId != null && deletingItemId == item.int("ImageGalleryItemId"),
                                        onOpen = { fullScreenUri = item.str("ImageURL") },
                                        onDelete = {
                                            selectedItem = item
                                            showAction = true
                                        }
                                    )
                                }
                            }
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 30.dp, bottom = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .rnShadow(8.dp, RoundedCornerShape(15.dp), ManageError)
                                        .clip(RoundedCornerShape(15.dp))
                                        .touchableOpacity(enabled = !isDeleting) { showDeleteGallery = true }
                                ) {
                                    LinearGradient(
                                        colors = listOf(ManageError, Color(0xFFC0392B)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        RowReverse(
                                            Modifier.fillMaxWidth().absolutePadding(left = 20.dp, right = 20.dp, top = 16.dp, bottom = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            MaterialIcons(name = "delete-forever", size = 24.dp, color = Color.White)
                                            AppText(
                                                text = if (isDeleting) "در حال حذف..." else "حذف کل گالری",
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
            }
        }

        if (modalVisible) {
            ManagePickerSheet(
                slide = pickerSlide.value,
                opacity = pickerOpacity.value,
                bottomInset = insets.bottom,
                onDismiss = { modalVisible = false },
                onGallery = {
                    picker.pickImages(
                        onPermissionDenied = { toast.showToast("نیاز به دسترسی گالری دارید", "error") },
                        onResult = { picked ->
                            val media = picked.firstOrNull() ?: return@pickImages
                            modalVisible = false
                            uploadNewImage(media)
                        }
                    )
                },
                onCamera = {
                    picker.takePhoto(
                        onPermissionDenied = { toast.showToast("نیاز به دسترسی دوربین دارید", "error") },
                        onResult = { picked ->
                            val media = picked.firstOrNull() ?: return@takePhoto
                            modalVisible = false
                            uploadNewImage(media)
                        }
                    )
                }
            )
        }

        if (showAction) {
            ManageConfirmSheet(
                backdrop = actionBackdrop.value,
                slide = actionSlide.value,
                title = "حذف تصویر",
                message = "آیا از حذف این تصویر اطمینان دارید؟",
                confirm = "حذف",
                busy = false,
                icon = "delete",
                onClose = { closeAction() },
                onConfirm = { confirmDeleteItem() }
            )
        }

        if (showDeleteGallery) {
            ManageConfirmSheet(
                backdrop = deleteBackdrop.value,
                slide = deleteSlide.value,
                title = "حذف کل گالری",
                message = "آیا از حذف کل گالری اطمینان دارید؟\nتمام تصاویر این گالری حذف خواهند شد.\nاین عمل قابل بازگشت نیست!",
                confirm = if (isDeleting) "در حال حذف..." else "بله، حذف کن",
                busy = isDeleting,
                icon = "delete-forever",
                onClose = { closeDeleteGallery() },
                onConfirm = { confirmDeleteGallery() }
            )
        }

        if (fullScreenUri != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .zIndex(30f)
                    .background(Color.Black.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                NetworkImage(fullScreenUri, Modifier.fillMaxSize(), resizeMode = ResizeMode.contain)
                Box(
                    Modifier
                        .align(AbsoluteAlignment.TopRight)
                        .zIndex(1f)
                        .offset(x = (-20).dp, y = 50.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .touchableOpacity { fullScreenUri = null }
                        .padding(10.dp)
                ) {
                    MaterialIcons(name = "close", size = 30.dp, color = Color.White)
                }
            }
        }

        Toast(toast.toastVisible, toast.toastMessage, toast.toastType) { toast.setToastVisible(false) }
    }
}

@Composable
private fun ManageImageCard(
    item: JsonObject,
    deleting: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val shape = RoundedCornerShape(15.dp)
    Box(
        Modifier
            .fillMaxWidth(0.48f)
            .padding(bottom = 15.dp)
            .rnShadow(3.dp, shape)
            .clip(shape)
            .background(Color.White)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .touchableOpacity(activeOpacity = 0.8f, onClick = onOpen)
        ) {
            NetworkImage(item.str("ImageURL"), Modifier.fillMaxSize())
            if (deleting) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }
        Box(
            Modifier
                .align(AbsoluteAlignment.TopLeft)
                .offset(8.dp, 8.dp)
                .size(36.dp)
                .rnShadow(5.dp, CircleShape)
                .clip(CircleShape)
                .touchableOpacity(enabled = !deleting, onClick = onDelete)
        ) {
            LinearGradient(
                colors = listOf(Color(0xFFE74C3C), Color(0xFFC0392B)),
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MaterialIcons(name = "delete", size = 20.dp, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ManagePickerSheet(
    slide: Float,
    opacity: Float,
    bottomInset: androidx.compose.ui.unit.Dp,
    onDismiss: () -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .zIndex(20f)
            .background(Color.Black.copy(alpha = 0.6f))
            .touchableOpacity(activeOpacity = 1f, onClick = onDismiss)
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = slide.dp.toPx()
                    alpha = opacity
                }
                .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                .background(Color.White)
                .padding(top = 12.dp)
                .absolutePadding(left = 20.dp, right = 20.dp)
                .padding(bottom = 20.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(bottom = 25.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .padding(bottom = 15.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFD1D5DB))
                )
                AppText(
                    text = "انتخاب روش",
                    fontSize = 20.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color(0xFF1F2937),
                    textAlign = TextAlign.Center
                )
            }
            Row(
                Modifier.fillMaxWidth().absolutePadding(left = 10.dp, right = 10.dp).padding(bottom = 25.dp),
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                ManagePickerOption(Modifier.weight(1f), "photo-library", Color(0xFF6366F1), "گالری", "انتخاب از تصاویر موجود", onGallery)
                ManagePickerOption(Modifier.weight(1f), "camera-alt", Color(0xFF10B981), "دوربین", "گرفتن عکس جدید", onCamera)
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .absolutePadding(left = 5.dp, right = 5.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color(0xFFFEE2E2))
                    .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(15.dp))
                    .touchableOpacity(onClick = onDismiss)
                    .absolutePadding(top = 16.dp, bottom = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                AppText(
                    text = "لغو",
                    fontSize = 16.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color(0xFFDC2626),
                    textAlign = TextAlign.Center
                )
            }
            Box(Modifier.fillMaxWidth().height(bottomInset).background(Color.White))
        }
    }
}

@Composable
private fun ManagePickerOption(
    modifier: Modifier,
    icon: String,
    iconBg: Color,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .clip(shape)
            .background(Color(0xFFF9FAFB))
            .border(1.dp, Color(0xFFE5E7EB), shape)
            .touchableOpacity(activeOpacity = 0.8f, onClick = onClick)
            .absolutePadding(left = 15.dp, right = 15.dp, top = 25.dp, bottom = 25.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(70.dp).rnShadow(4.dp, CircleShape).clip(CircleShape).background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            MaterialIcons(name = icon, size = 32.dp, color = Color.White)
        }
        AppText(
            text = label,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            fontSize = 16.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = Color(0xFF1F2937),
            textAlign = TextAlign.Center
        )
        AppText(
            text = description,
            fontSize = 12.sp,
            fontFamily = RNFonts.Yekan_Bakh_Regular,
            color = Color(0xFF6B7280),
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun ManageConfirmSheet(
    backdrop: Float,
    slide: Float,
    title: String,
    message: String,
    confirm: String,
    busy: Boolean,
    icon: String,
    onClose: () -> Unit,
    onConfirm: () -> Unit
) {
    Box(Modifier.fillMaxSize().zIndex(22f)) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = backdrop }
                .background(Color.Black.copy(alpha = 0.5f))
                .touchableOpacity(activeOpacity = 1f, onClick = onClose)
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .graphicsLayer { translationY = (1f - slide) * 300.dp.toPx() }
                .rnShadow(10.dp, RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                .clip(RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                .background(Color.White)
                .padding(top = 15.dp, bottom = 35.dp)
                .absolutePadding(left = 25.dp, right = 25.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .padding(bottom = 15.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFD1D5DB))
            )
            Box(
                Modifier
                    .padding(bottom = 20.dp)
                    .size(70.dp)
                    .rnShadow(8.dp, CircleShape, ManageError)
                    .clip(CircleShape)
                    .background(ManageError),
                contentAlignment = Alignment.Center
            ) {
                MaterialIcons(name = "warning", size = 32.dp, color = Color.White)
            }
            AppText(
                text = title,
                modifier = Modifier.padding(bottom = 15.dp),
                fontSize = 20.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = Color(0xFF2C3E50),
                textAlign = TextAlign.Center
            )
            AppText(
                text = message,
                modifier = Modifier.padding(bottom = 25.dp),
                fontSize = 16.sp,
                fontFamily = RNFonts.Yekan_Bakh_Regular,
                color = Color(0xFF6C757D),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            RowReverse(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color(0xFFF8F9FA))
                        .border(1.dp, Color(0xFFE9ECEF), RoundedCornerShape(15.dp))
                        .touchableOpacity(enabled = !busy, onClick = onClose)
                        .absolutePadding(top = 16.dp, bottom = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AppText(
                        text = "لغو",
                        fontSize = 16.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color(0xFF6C757D),
                        textAlign = TextAlign.Center
                    )
                }
                Box(
                    Modifier
                        .weight(1f)
                        .rnShadow(8.dp, RoundedCornerShape(15.dp), ManageError)
                        .clip(RoundedCornerShape(15.dp))
                        .touchableOpacity(enabled = !busy, onClick = onConfirm)
                ) {
                    LinearGradient(
                        colors = listOf(ManageError, Color(0xFFC0392B)),
                        modifier = Modifier.fillMaxWidth(),
                        start = Offset(0f, 0f),
                        end = Offset(1f, 1f)
                    ) {
                        RowReverse(
                            Modifier.fillMaxWidth().absolutePadding(left = 12.dp, right = 12.dp, top = 16.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (busy && icon == "delete-forever") {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                MaterialIcons(name = icon, size = 20.dp, color = Color.White)
                            }
                            AppText(
                                text = confirm,
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
