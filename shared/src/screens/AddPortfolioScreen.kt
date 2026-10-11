package ir.farimod.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.hooks.useToast
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.navigation.useRoute
import ir.farimod.app.network.Api
import ir.farimod.app.network.ApiNetworkException
import ir.farimod.app.network.bool
import ir.farimod.app.network.int
import ir.farimod.app.network.jsonOf
import ir.farimod.app.network.list
import ir.farimod.app.network.str
import ir.farimod.app.network.without
import ir.farimod.app.resources.Res
import ir.farimod.app.resources.background_1
import ir.farimod.app.screens.addportfolio.computePortfolioSlots
import ir.farimod.app.screens.addportfolio.portfolioIsoNow
import ir.farimod.app.screens.addportfolio.uploadPortfolioImages
import ir.farimod.app.state.AuthState
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.AppButton
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.AppTextInput
import ir.farimod.app.ui.ImageItem
import ir.farimod.app.ui.ImageUpload
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.Screen
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.Tooltip
import ir.farimod.app.ui.rgba
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.rnTextStyle
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.utils.currentTimeMillis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import org.jetbrains.compose.resources.imageResource

private const val SUBMIT_COOLDOWN_MS = 3000L
private val slotIdRegex = Regex("portfolio-initial-index-(\\d+)$")

private fun Modifier.pullUp(amount: androidx.compose.ui.unit.Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val shift = amount.roundToPx()
    layout(placeable.width, (placeable.height - shift).coerceAtLeast(0)) {
        placeable.place(0, -shift)
    }
}

private fun serverImages(edit: JsonObject?): List<String> =
    edit.list("Images").mapNotNull { it.str }.filter { it.isNotBlank() }

private fun initialFeatured(urls: List<String>): List<ImageItem> {
    val first = urls.firstOrNull() ?: return emptyList()
    return listOf(
        ImageItem(
            id = "portfolio-initial-index-0",
            uri = first,
            name = "featured.jpg",
            type = "image/jpeg"
        )
    )
}

private fun initialGallery(urls: List<String>): List<ImageItem> {
    if (urls.size <= 1) return emptyList()
    return urls.drop(1).mapIndexed { index, url ->
        val serverIndex = index + 1
        ImageItem(
            id = "portfolio-initial-index-$serverIndex",
            uri = url,
            name = "portfolio_$serverIndex.jpg",
            type = "image/jpeg"
        )
    }
}

@Composable
fun AddPortfolioScreen() {
    val navigation = useNavigation()
    val route = useRoute()
    val toast = useToast()
    val scope = rememberCoroutineScope()
    val user = AuthState.user
    val edit = route.json("portfolioData") as? JsonObject
    val isEdit = route.bool("isEdit") == true
    val editId = edit?.int("PortfolioId")
    val urls = remember(editId, isEdit) { if (isEdit) serverImages(edit) else emptyList() }

    var title by remember(isEdit, editId) { mutableStateOf(if (isEdit) edit.str("Title") ?: "" else "") }
    var description by remember(isEdit, editId) { mutableStateOf(if (isEdit) edit.str("Description") ?: "" else "") }
    var featuredImage by remember(isEdit, editId) { mutableStateOf(initialFeatured(urls)) }
    var portfolioImages by remember(isEdit, editId) { mutableStateOf(initialGallery(urls)) }
    var isSubmitting by remember { mutableStateOf(false) }
    var uploadingImages by remember { mutableStateOf(false) }
    var lastSubmitTime by remember { mutableLongStateOf(0L) }

    fun showToast(message: String, type: String = "info") {
        toast.showToast(message, type)
    }

    fun deleteImage(type: Int) {
        val portfolioId = edit.int("PortfolioId") ?: return
        scope.launch {
            try {
                Api.post("Portfolio/DeletePortfolioImage?portfolioId=$portfolioId&type=$type")
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    fun onFeaturedChange(images: List<ImageItem>) {
        val imageArray = images.filter { it.uri.isNotEmpty() }
        val prev = featuredImage
        if (isEdit && edit.int("PortfolioId") != null && prev.isNotEmpty()) {
            val prevInitial = prev[0]
            val wasServer = prevInitial.id.startsWith("portfolio-initial-index-")
            val still = imageArray.any { it.id == prevInitial.id }
            if (wasServer && !still) deleteImage(0)
        }
        featuredImage = imageArray
    }

    fun onGalleryChange(images: List<ImageItem>) {
        val imageArray = images.filter { it.uri.isNotEmpty() }
        val prev = portfolioImages
        if (isEdit && edit.int("PortfolioId") != null && prev.isNotEmpty()) {
            val removed = prev.filter { item ->
                item.id.startsWith("portfolio-initial-index-") && imageArray.none { it.id == item.id }
            }
            removed.forEach { item ->
                val match = slotIdRegex.find(item.id)
                val type = match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
                deleteImage(type)
            }
        }
        portfolioImages = imageArray
    }

    fun submit() {
        val now = currentTimeMillis()
        if (now - lastSubmitTime < SUBMIT_COOLDOWN_MS) {
            showToast("لطفاً کمی صبر کنید...", "warning")
            return
        }
        if (isSubmitting || uploadingImages) return
        lastSubmitTime = now
        isSubmitting = true
        scope.launch {
            try {
                val titleError = if (title.trim().isEmpty()) "عنوان نمونه کار الزامی است" else null
                val descriptionError = if (description.trim().isEmpty()) "توضیحات نمونه کار الزامی است" else null
                val first = titleError ?: descriptionError
                if (first != null) {
                    showToast(first, "error")
                    return@launch
                }
                val slots = computePortfolioSlots(portfolioImages, edit, isEdit)
                val featured = featuredImage.firstOrNull()
                val memberId = user?.MemberId
                val activeValue: Any? = if (!isEdit || edit == null) {
                    true
                } else {
                    val raw = edit.get("Active")
                    when {
                        raw == null -> true
                        raw is JsonNull -> null
                        else -> raw.bool
                    }
                }
                var body = jsonOf(
                    "PortfolioId" to (if (isEdit) edit.int("PortfolioId") ?: 0 else 0),
                    "MemberId" to memberId,
                    "Title" to title.trim(),
                    "Description" to description.trim(),
                    "FeaturedImageURL" to (featured?.uri ?: ""),
                    "FeaturedImageFileName" to (featured?.name ?: ""),
                    "FirstImageURL" to slots[0].uri,
                    "FirstImageFileName" to slots[0].name,
                    "SecondImageURL" to slots[1].uri,
                    "SecondImageFileName" to slots[1].name,
                    "ThirdImageURL" to slots[2].uri,
                    "ThirdImageFileName" to slots[2].name,
                    "FourthImageURL" to slots[3].uri,
                    "FourthImageFileName" to slots[3].name,
                    "FifthImageURL" to slots[4].uri,
                    "FifthImageFileName" to slots[4].name,
                    "Active" to activeValue,
                    "InsertDate" to portfolioIsoNow(),
                    "Rating" to 0,
                    "LikeCount" to 0,
                    "PortfolioItemViewModelList" to emptyList<String>()
                )
                if (memberId == null) body = body.without("MemberId")
                val response = try {
                    if (isEdit) Api.put("Portfolio/Edit", body) else Api.post("Portfolio/Add", body)
                } catch (e: ApiNetworkException) {
                    throw Exception(e.message ?: "Network request failed", e)
                }
                if (!response.ok) {
                    val message = response.json.str("Message")?.takeIf { it.isNotEmpty() }
                        ?: "خطا در ${if (isEdit) "ویرایش" else "ثبت"} نمونه کار"
                    throw Exception(message)
                }
                val result = response.json ?: throw Exception("Portfolio ID not received from server")
                val sentId = if (isEdit) edit.int("PortfolioId") ?: 0 else 0
                val portfolioId = result.int("PortfolioId") ?: result.int("portfolioId") ?: sentId
                if (portfolioId == 0) throw Exception("Portfolio ID not received from server")
                showToast(
                    if (isEdit) "نمونه کار با موفقیت ویرایش شد" else "نمونه کار با موفقیت ثبت شد",
                    "success"
                )
                val hasFeaturedChange = featuredImage.isNotEmpty() &&
                    !featuredImage[0].id.startsWith("portfolio-initial-index-")
                val hasSlotChanges = slots.any { it.isNew || it.isRemovedOld }
                if (hasFeaturedChange || hasSlotChanges) {
                    uploadingImages = true
                    showToast("شروع آپلود عکس‌ها...", "info")
                    delay(2000)
                    try {
                        uploadPortfolioImages(portfolioId, featuredImage, slots)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        showToast("خطا در آپلود برخی عکس‌ها: " + (e.message ?: ""), "error")
                    } finally {
                        uploadingImages = false
                    }
                }
                if (!isEdit) {
                    title = ""
                    description = ""
                    featuredImage = emptyList()
                    portfolioImages = emptyList()
                }
                scope.launch {
                    try {
                        val listed = Api.get(
                            "Portfolio/GetAll",
                            query = mapOf(
                                "page" to 1,
                                "pageSize" to 10,
                                "memberId" to (user?.MemberId ?: "undefined")
                            )
                        )
                        if (!listed.ok) showToast("خطا در دریافت پورتفولیوها", "error")
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        showToast("خطا در دریافت پورتفولیوها", "error")
                    }
                }
                navigation.navigate("PortfolioList")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(
                    e.message?.takeIf { it.isNotEmpty() }
                        ?: "خطا در ${if (isEdit) "ویرایش" else "ثبت"} نمونه کار",
                    "error"
                )
            } finally {
                isSubmitting = false
                uploadingImages = false
            }
        }
    }

    val bgBitmap = imageResource(Res.drawable.background_1)
    val bgBrush = remember(bgBitmap) { ShaderBrush(ImageShader(bgBitmap, TileMode.Repeated, TileMode.Repeated)) }
    val glass = RoundedCornerShape(25.dp)
    val uploadShape = RoundedCornerShape(15.dp)
    val busy = isSubmitting || uploadingImages

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().drawBehind { drawRect(bgBrush) })
        LinearGradient(
            colors = listOf(
                rgba(255, 255, 255, 0.1f),
                rgba(255, 255, 255, 0.6f),
                rgba(255, 255, 255, 0.8f),
                rgba(255, 255, 255, 1f)
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            Screen(Modifier.padding(10.dp)) {
                Box(Modifier.fillMaxSize()) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .imePadding()
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 50.dp)
                                .zIndex(1000f),
                            contentAlignment = Alignment.Center
                        ) {
                            LinearGradient(
                                colors = listOf(FarimodColors.Primary, FarimodColors.PrimaryDark),
                                modifier = Modifier
                                    .size(100.dp)
                                    .rnShadow(8.dp, CircleShape, rgba(255, 206, 232, 0.2f)),
                                shape = CircleShape
                            ) {
                                Box(
                                    Modifier
                                        .align(Alignment.Center)
                                        .zIndex(99f)
                                        .size(80.dp)
                                        .border(2.dp, rgba(255, 206, 232, 0.4f), CircleShape)
                                        .clip(CircleShape)
                                        .background(rgba(255, 206, 232, 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MaterialIcons(
                                        name = if (isEdit) "edit" else "work",
                                        size = 50.dp,
                                        color = FarimodColors.White
                                    )
                                }
                                Box(
                                    Modifier
                                        .align(Alignment.Center)
                                        .requiredSize(120.dp)
                                        .drawBehind {
                                            val stroke = 1.5.dp.toPx()
                                            drawCircle(
                                                color = rgba(255, 206, 232, 0.3f),
                                                radius = size.minDimension / 2f - stroke / 2f,
                                                style = Stroke(
                                                    width = stroke,
                                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke * 3f, stroke * 3f))
                                                )
                                            )
                                        }
                                )
                            }
                        }

                        Box(
                            Modifier
                                .pullUp(45.dp)
                                .absolutePadding(left = 5.dp, right = 5.dp)
                                .padding(bottom = 20.dp)
                                .rnShadow(6.dp, glass, rgba(255, 255, 255, 0.15f))
                                .clip(glass)
                        ) {
                            Box(
                                Modifier
                                    .matchParentSize()
                                    .border(1.5.dp, rgba(255, 206, 232, 1f), glass)
                                    .drawBehind { drawRect(rgba(255, 206, 232, 0.7f)) }
                            )
                            Column(Modifier.fillMaxWidth().padding(25.dp).zIndex(1f)) {
                                Text(
                                    text = if (isEdit) "ویرایش نمونه کار" else "افزودن نمونه کار جدید",
                                    modifier = Modifier.fillMaxWidth().padding(top = 35.dp, bottom = 30.dp),
                                    style = rnTextStyle(
                                        fontSize = 22.sp,
                                        color = FarimodColors.Primary,
                                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                                        textAlign = TextAlign.Center
                                    ).copy(
                                        shadow = Shadow(
                                            color = rgba(255, 206, 232, 0.1f),
                                            offset = Offset(0f, 1f),
                                            blurRadius = 2f
                                        )
                                    )
                                )
                                AppTextInput(
                                    value = title,
                                    onChangeText = { title = it },
                                    icon = "title",
                                    label = "عنوان نمونه کار",
                                    placeholder = "عنوان نمونه کار"
                                )
                                AppTextInput(
                                    value = description,
                                    onChangeText = { description = it },
                                    icon = "description",
                                    label = "توضیحات نمونه کار",
                                    placeholder = "توضیحات نمونه کار",
                                    isLargeInput = true,
                                    height = 100.dp
                                )
                                Box(Modifier.padding(top = 25.dp, bottom = 15.dp)) {
                                    ImageUpload(
                                        modifier = Modifier
                                            .background(rgba(255, 255, 255, 0.3f), uploadShape)
                                            .border(1.dp, rgba(255, 206, 232, 0.2f), uploadShape)
                                            .padding(15.dp),
                                        onImageChange = { change -> onFeaturedChange(change.list) },
                                        isMultiple = false,
                                        maxImages = 1,
                                        imageQuality = 0.8f,
                                        allowCamera = true,
                                        allowGallery = true,
                                        initialImage = featuredImage.firstOrNull(),
                                        placeholder = "انتخاب تصویر شاخص نمونه کار",
                                        aspectRatio = 1 to 1,
                                        onShowToast = { message, type -> showToast(message, type) }
                                    )
                                }
                                Column(Modifier.padding(top = 25.dp, bottom = 15.dp)) {
                                    RowReverse(
                                        Modifier.fillMaxWidth().padding(bottom = 15.dp).absolutePadding(left = 5.dp, right = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        MaterialIcons("photo-library", size = 20.dp, color = FarimodColors.Primary)
                                        AppText(
                                            text = "تصاویر پورتفولیو",
                                            fontSize = 16.sp,
                                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                                            color = FarimodColors.Primary,
                                            modifier = Modifier.absolutePadding(right = 8.dp)
                                        )
                                    }
                                    ImageUpload(
                                        modifier = Modifier
                                            .background(rgba(255, 255, 255, 0.3f), uploadShape)
                                            .border(1.dp, rgba(255, 206, 232, 0.2f), uploadShape)
                                            .padding(15.dp),
                                        onImagesChange = { images -> onGalleryChange(images) },
                                        isMultiple = true,
                                        maxImages = 5,
                                        imageQuality = 0.8f,
                                        allowCamera = true,
                                        allowGallery = true,
                                        allowVideos = true,
                                        initialImages = portfolioImages,
                                        placeholder = "انتخاب تصاویر پورتفولیو",
                                        aspectRatio = 1 to 1,
                                        onShowToast = { message, type -> showToast(message, type) }
                                    )
                                }
                                Box(Modifier.padding(top = 20.dp).alpha(if (busy) 0.6f else 1f)) {
                                    AppButton(
                                        title = when {
                                            isSubmitting && isEdit -> "در حال ویرایش..."
                                            isSubmitting -> "در حال ثبت..."
                                            uploadingImages -> "در حال آپلود عکس‌ها..."
                                            isEdit -> "ویرایش نمونه کار"
                                            else -> "ثبت نمونه کار"
                                        },
                                        onPress = { if (!busy) submit() },
                                        color = if (isEdit) FarimodColors.Info else FarimodColors.Success,
                                        disabled = busy
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .zIndex(10f)
                            .absolutePadding(left = 15.dp, right = 15.dp, top = 15.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Tooltip(
                            content = "نمونه‌کار جدید اضافه کنید. اطلاعات پروژه شامل عنوان، توضیحات کامل، برچسب‌ها، مهارت‌های استفاده شده و دسته‌بندی را وارد کنید. تصاویر با کیفیت از پروژه خود آپلود کنید تا کارفرمایان و مشتریان بالقوه آن را مشاهده کنند."
                        )
                        Box(
                            Modifier
                                .border(1.dp, rgba(255, 206, 232, 0.5f), RoundedCornerShape(25.dp))
                                .background(Color(0xFF9E22AD), RoundedCornerShape(25.dp))
                                .touchableOpacity { navigation.navigate("PortfolioList") }
                                .padding(10.dp)
                        ) {
                            MaterialIcons("arrow-forward", size = 24.dp, color = Color.White)
                        }
                    }

                    Toast(toast.toastVisible, toast.toastMessage, toast.toastType) {
                        toast.setToastVisible(false)
                    }
                }
            }
        }
    }
}
