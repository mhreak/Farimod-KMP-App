package ir.farimod.app.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.hooks.useToast
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.network.Api
import ir.farimod.app.network.ApiFilePart
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.jsonOf
import ir.farimod.app.network.list
import ir.farimod.app.network.str
import ir.farimod.app.resources.Res
import ir.farimod.app.resources.background_1
import ir.farimod.app.screens.galleryitem.galleryCreatedId
import ir.farimod.app.screens.galleryitem.galleryInsertMillis
import ir.farimod.app.screens.galleryitem.galleryUtcIsoNow
import ir.farimod.app.state.AuthState
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.AppButton
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.AppTextInput
import ir.farimod.app.ui.ImageItem
import ir.farimod.app.ui.ImageUpload
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.Screen
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.Tooltip
import ir.farimod.app.ui.rgba
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.rnTextStyle
import ir.farimod.app.ui.rnTiming
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.ui.linearGradient
import ir.farimod.app.utils.currentTimeMillis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import org.jetbrains.compose.resources.imageResource

/**
 * Port of `screens/AddGalleryScreen.tsx`.
 * Creates ImageGallery/Add, then uploads each image with ImageGalleryItem/Add (multipart, field `imageFile`).
 * UserData has no MemberName, so the JSON field is sent as an empty string (RN `user?.MemberName || ''`).
 */
@Composable
fun AddGalleryScreen() {
    val navigation = useNavigation()
    val toast = useToast()
    val scope = rememberCoroutineScope()
    val user = AuthState.user

    var title by remember { mutableStateOf("") }
    var images by remember { mutableStateOf<List<ImageItem>>(emptyList()) }
    var titleError by remember { mutableStateOf<String?>(null) }
    var imagesError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val iconFade = remember { Animatable(0f) }
    val iconSlide = remember { Animatable(-30f) }
    LaunchedEffect(Unit) {
        coroutineScope {
            launch { iconFade.animateTo(1f, rnTiming(600)) }
            launch { iconSlide.animateTo(0f, rnTiming(600)) }
        }
    }

    fun goMyGallery() {
        navigation.goBack()
    }

    fun submit() {
        if (isSubmitting) return
        scope.launch {
            isSubmitting = true
            try {
                titleError = null
                imagesError = null
                val trimmed = title.trim()
                if (trimmed.isEmpty()) titleError = "عنوان گالری الزامی است"
                if (images.isEmpty()) imagesError = "حداقل یک تصویر الزامی است"
                val firstError = titleError ?: imagesError
                if (firstError != null) {
                    toast.showToast(firstError, "error")
                    isSubmitting = false
                    return@launch
                }

                val body = jsonOf(
                    "ImageGalleryId" to 0,
                    "Title" to trimmed,
                    "MemberId" to (user?.MemberId ?: 0),
                    "MemberName" to "",
                    "ImageCount" to images.size,
                    "Rating" to 0,
                    "LikeCount" to 0,
                    "FeaturedImageURL" to "",
                    "Active" to true,
                    "ActiveStr" to "فعال",
                    "InsertDate" to galleryUtcIsoNow(),
                    "ShamsiInsertDate" to "",
                    "ImageGalleryItemList" to emptyList<String>()
                )
                val galleryResponse = Api.post("ImageGallery/Add", body = body)
                if (!galleryResponse.ok) throw Exception("خطا در ایجاد گالری")
                val galleryResult = galleryResponse.json ?: throw Exception("خطا در دریافت پاسخ سرور")

                var imageGalleryId = galleryCreatedId(galleryResult)
                val message = galleryResult.str("Message") ?: ""
                if ((imageGalleryId == null || imageGalleryId == 0) && message.contains("موفقیت")) {
                    try {
                        imageGalleryId = resolveGalleryIdFromList(user?.MemberId ?: 0, trimmed)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        throw Exception("خطا در دریافت شناسه گالری")
                    }
                }
                if (imageGalleryId == null || imageGalleryId == 0) throw Exception("شناسه گالری دریافت نشد")

                var successCount = 0
                var failedCount = 0
                images.forEachIndexed { index, image ->
                    try {
                        uploadGalleryImage(
                            image = image,
                            index = index,
                            galleryId = imageGalleryId,
                            fallbackTitle = trimmed
                        )
                        successCount++
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        failedCount++
                    }
                }

                isSubmitting = false
                when {
                    successCount == images.size -> {
                        toast.showToast("گالری با موفقیت ایجاد شد", "success")
                        title = ""
                        images = emptyList()
                        titleError = null
                        imagesError = null
                        goMyGallery()
                    }
                    successCount > 0 -> {
                        toast.showToast("گالری ایجاد شد اما $failedCount تصویر آپلود نشد", "warning")
                        goMyGallery()
                    }
                    else -> throw Exception("هیچ تصویری آپلود نشد")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                isSubmitting = false
                toast.showToast(e.message?.takeIf { it.isNotEmpty() } ?: "خطا در ذخیره گالری", "error")
            }
        }
    }

    val bgBitmap = imageResource(Res.drawable.background_1)
    val bgBrush = remember(bgBitmap) { ShaderBrush(ImageShader(bgBitmap, TileMode.Repeated, TileMode.Repeated)) }
    val glass = RoundedCornerShape(25.dp)
    val uploadShape = RoundedCornerShape(15.dp)
    val scroll = rememberScrollState()

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().drawBehind { drawRect(bgBrush) })
        Screen(Modifier.padding(10.dp)) {
            Box(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll)
                        .imePadding()
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .zIndex(1000f)
                            .pullFollowingUp(110.dp)
                            .padding(top = 50.dp)
                            .graphicsLayer {
                                alpha = iconFade.value
                                translationY = iconSlide.value.dp.toPx()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        AddGalleryIcon()
                    }

                    Box(
                        Modifier
                            .padding(top = 65.dp, bottom = 80.dp)
                            .absolutePadding(left = 5.dp, right = 5.dp)
                            .clip(glass)
                    ) {
                        Box(
                            Modifier
                                .matchParentSize()
                                .rnShadow(6.dp, glass, Color.White)
                                .clip(glass)
                                .background(rgba(255, 206, 232, 0.7f))
                                .border(1.5.dp, rgba(255, 206, 232, 1f), glass)
                        )
                        Column(Modifier.padding(25.dp).zIndex(1f)) {
                            Text(
                                text = "افزودن گالری جدید",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 35.dp, bottom = 30.dp),
                                style = rnTextStyle(
                                    fontSize = 22.sp,
                                    color = FarimodColors.Primary,
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    textAlign = TextAlign.Center
                                ).copy(
                                    shadow = Shadow(
                                        color = rgba(255, 206, 232, 0.1f),
                                        offset = androidx.compose.ui.geometry.Offset(0f, 1f),
                                        blurRadius = 2f
                                    )
                                ),
                                textAlign = TextAlign.Center
                            )

                            AppTextInput(
                                value = title,
                                onChangeText = { title = it },
                                icon = "title",
                                label = "عنوان گالری",
                                placeholder = "عنوان گالری",
                                error = titleError,
                                inputContainerModifier = if (titleError != null) {
                                    Modifier.border(1.dp, Color(0xFFE74C3C), RoundedCornerShape(16.dp))
                                } else {
                                    Modifier
                                }
                            )
                            if (titleError != null) {
                                AppText(
                                    text = titleError ?: "",
                                    modifier = Modifier.padding(top = 5.dp, bottom = 10.dp),
                                    fontSize = 12.sp,
                                    color = Color(0xFFE74C3C),
                                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                                    textAlign = TextAlign.Right
                                )
                            }

                            if (images.isNotEmpty()) {
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 15.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White.copy(alpha = 0.5f))
                                        .border(1.dp, rgba(255, 206, 232, 0.3f), RoundedCornerShape(10.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MaterialIcons(name = "photo-library", size = 20.dp, color = FarimodColors.Primary)
                                    Text(
                                        text = "${images.size} تصویر انتخاب شده",
                                        modifier = Modifier.absolutePadding(left = 8.dp),
                                        style = rnTextStyle(
                                            fontSize = 14.sp,
                                            color = FarimodColors.Primary,
                                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Right
                                        )
                                    )
                                }
                            }

                            ImageUpload(
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.3f), uploadShape)
                                    .border(1.dp, rgba(255, 206, 232, 0.2f), uploadShape)
                                    .padding(15.dp),
                                marginBottom = 10.dp,
                                onImageChange = { change ->
                                    val next = change.list.filter { it.uri.isNotEmpty() }
                                    images = next
                                },
                                isMultiple = true,
                                initialImages = images,
                                imageQuality = 0.8f,
                                allowCamera = true,
                                allowGallery = true,
                                error = imagesError,
                                maxImages = 10,
                                placeholder = "انتخاب تصاویر گالری (حداکثر 10 عکس)",
                                aspectRatio = 16 to 9,
                                onShowToast = { message, type -> toast.showToast(message, type) }
                            )
                            if (imagesError != null) {
                                AppText(
                                    text = imagesError ?: "",
                                    modifier = Modifier.padding(top = 5.dp, bottom = 10.dp),
                                    fontSize = 12.sp,
                                    color = Color(0xFFE74C3C),
                                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                                    textAlign = TextAlign.Right
                                )
                            }

                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 5.dp, bottom = 15.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.3f))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MaterialIcons(name = "info-outline", size = 16.dp, color = FarimodColors.Medium)
                                Text(
                                    text = "می‌توانید چندین تصویر را همزمان انتخاب کنید",
                                    modifier = Modifier.absolutePadding(left = 6.dp),
                                    style = rnTextStyle(
                                        fontSize = 12.sp,
                                        color = FarimodColors.Medium,
                                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                                        textAlign = TextAlign.Right
                                    )
                                )
                            }

                            Box(Modifier.padding(top = 20.dp)) {
                                AppButton(
                                    title = if (isSubmitting) "در حال ثبت..." else "ثبت گالری",
                                    onPress = { submit() },
                                    color = FarimodColors.Success,
                                    disabled = isSubmitting
                                )
                            }
                        }
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .zIndex(10f)
                        .offset(y = 15.dp)
                        .absolutePadding(left = 15.dp, right = 15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Tooltip(
                        content = "گالری تصویری جدید ایجاد کنید. عنوان، توضیحات و دسته‌بندی گالری را مشخص کنید تا تصاویر خود را به صورت دسته‌بندی شده نمایش دهید."
                    )
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(25.dp))
                            .background(Color(0xFF9E22AD))
                            .border(1.dp, rgba(255, 206, 232, 0.5f), RoundedCornerShape(25.dp))
                            .touchableOpacity { goMyGallery() }
                            .padding(10.dp)
                    ) {
                        MaterialIcons(name = "arrow-forward", size = 24.dp, color = Color.White)
                    }
                }
            }
        }
        Toast(toast.toastVisible, toast.toastMessage, toast.toastType) { toast.setToastVisible(false) }
    }
}

@Composable
private fun AddGalleryIcon() {
    Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(120.dp)
                .drawBehind {
                    drawCircle(
                        color = rgba(255, 206, 232, 0.3f),
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                        )
                    )
                }
        )
        Box(
            Modifier
                .size(100.dp)
                .rnShadow(8.dp, CircleShape, rgba(255, 206, 232, 0.2f))
                .clip(CircleShape)
                .linearGradient(
                    listOf(FarimodColors.Primary, FarimodColors.PrimaryDark),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(rgba(255, 206, 232, 0.15f))
                    .border(2.dp, rgba(255, 206, 232, 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                MaterialIcons(name = "photo-library", size = 50.dp, color = FarimodColors.White)
            }
        }
    }
}

/** RN negative marginBottom: the next sibling is pulled up by [amount]. */
private fun Modifier.pullFollowingUp(amount: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val pull = amount.roundToPx()
    val height = (placeable.height - pull).coerceAtLeast(0)
    layout(placeable.width, height) { placeable.place(0, 0) }
}

private suspend fun resolveGalleryIdFromList(memberId: Int, title: String): Int {
    val response = Api.get("ImageGallery/GetByMemberId", query = mapOf("memberId" to memberId))
    if (!response.ok) throw Exception("خطا در دریافت لیست گالری‌ها")
    val body = response.json ?: throw Exception("خطا در دریافت شناسه گالری")
    val list = when {
        body is kotlinx.serialization.json.JsonArray -> body.list
        body["data"] != null -> body.list("data")
        else -> emptyList()
    }.mapNotNull { it as? JsonObject }
    val matching = list.filter { it.str("Title") == title }
    val pool = if (matching.isNotEmpty()) matching else list
    val picked = pool.maxByOrNull { galleryInsertMillis(it) } ?: throw Exception("گالری در لیست پیدا نشد")
    return picked.int("ImageGalleryId") ?: picked.int("imageGalleryId") ?: throw Exception("خطا در دریافت شناسه گالری")
}

private suspend fun uploadGalleryImage(image: ImageItem, index: Int, galleryId: Int, fallbackTitle: String) {
    val bytes = image.readBytes?.invoke() ?: throw Exception("upload")
    val fileName = image.name?.takeIf { it.isNotEmpty() } ?: "gallery-image-${currentTimeMillis()}-$index.jpg"
    var mime = image.type ?: "image/jpeg"
    if (!mime.startsWith("image/")) {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        if (ext == "png") mime = "image/png"
        else if (ext == "jpg" || ext == "jpeg") mime = "image/jpeg"
    }
    val fields = mapOf(
        "ImageGalleryItemId" to "0",
        "ImageGalleryId" to galleryId.toString(),
        "Title" to fallbackTitle,
        "ImageFileName" to "",
        "ImageURL" to "",
        "ShowOrder" to index.toString(),
        "Active" to "true",
        "ActiveStr" to "فعال",
        "InsertDate" to galleryUtcIsoNow(),
        "ShamsiInsertDate" to ""
    )
    val response = Api.upload(
        path = "ImageGalleryItem/Add",
        files = listOf(ApiFilePart("imageFile", fileName, mime, bytes)),
        fields = fields,
        timeoutMs = 60_000
    )
    if (!response.ok) {
        val msg = response.json.str("Message")?.takeIf { it.isNotEmpty() } ?: "upload"
        throw Exception(msg)
    }
}
