package ir.farimod.app.screens

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.navigation.useRoute
import ir.farimod.app.network.Api
import ir.farimod.app.network.ApiFilePart
import ir.farimod.app.network.bool
import ir.farimod.app.network.get
import ir.farimod.app.network.list
import ir.farimod.app.network.long
import ir.farimod.app.network.str
import ir.farimod.app.resources.Res
import ir.farimod.app.resources.background_1
import ir.farimod.app.screens.addnewpost.submitBlogPostMultipart
import ir.farimod.app.state.AuthState
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.AppButton
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.AppTextInput
import ir.farimod.app.ui.ImageItem
import ir.farimod.app.ui.ImageUpload
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.Picker
import ir.farimod.app.ui.PickerOption
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RNTimingEasing
import ir.farimod.app.ui.Screen
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.ToastData
import ir.farimod.app.ui.Tooltip
import ir.farimod.app.ui.pickerValueEquals
import ir.farimod.app.ui.rgba
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.utils.currentTimeMillis
import ir.farimod.app.utils.jsToString
import ir.farimod.app.utils.jsTruthy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import org.jetbrains.compose.resources.imageResource

private data class PostForm(
    val title: String = "",
    val content: String = "",
    val categoryIds: List<Long> = emptyList(),
    val commentEnabled: Boolean = true,
    val active: Boolean = true,
    val images: List<ImageItem> = emptyList()
)

private data class PostFieldErrors(
    val title: String? = null,
    val content: String? = null,
    val categories: String? = null
)

private fun Modifier.pullUp(amount: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val shift = amount.roundToPx()
    layout(placeable.width, (placeable.height - shift).coerceAtLeast(0)) {
        placeable.place(0, -shift)
    }
}

private fun isoNowUtc(): String {
    val ms = currentTimeMillis()
    var z = ms / 86_400_000L
    val tod = ((ms % 86_400_000L) + 86_400_000L) % 86_400_000L
    z += 719468
    val era = if (z >= 0) z / 146097 else (z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    var y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = if (mp < 10) mp + 3 else mp - 9
    if (m <= 2) y += 1
    val hh = tod / 3_600_000
    val mm = (tod % 3_600_000) / 60_000
    val ss = (tod % 60_000) / 1000
    val millis = tod % 1000
    fun p(n: Long, w: Int = 2) = n.toString().padStart(w, '0')
    return "$y-${p(m)}-${p(d)}T${p(hh)}:${p(mm)}:${p(ss)}.${p(millis, 3)}Z"
}

private fun jsonFlag(value: JsonElement?, default: Boolean): Boolean {
    if (value == null) return default
    return value.bool
}

@Composable
fun AddNewPostScreen() {
    val navigation = useNavigation()
    val route = useRoute()
    val user = AuthState.user
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val isEditMode = route.bool("isEdit") == true
    val editPostData = route.json("blogData")

    var toast by remember { mutableStateOf(ToastData()) }
    var isSubmitting by remember { mutableStateOf(false) }
    var categories by remember { mutableStateOf<List<PickerOption>>(emptyList()) }
    var loadingCategories by remember { mutableStateOf(true) }
    var postImages by remember { mutableStateOf<List<ImageItem>>(emptyList()) }
    var categoriesApplied by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf(PostFieldErrors()) }

    var form by remember(isEditMode, editPostData) {
        mutableStateOf(
            PostForm(
                title = if (isEditMode) editPostData.str("Title") ?: "" else "",
                content = if (isEditMode) editPostData.str("Content") ?: "" else "",
                commentEnabled = if (isEditMode) jsonFlag(editPostData?.get("CommentEnabled"), true) else true,
                active = if (isEditMode) jsonFlag(editPostData?.get("Active"), true) else true
            )
        )
    }

    fun showToast(message: String, type: String = "info") {
        toast = ToastData(visible = true, message = message, type = type)
    }

    val backAnim = remember { Animatable(0f) }
    val iconFade = remember { Animatable(0f) }
    val iconSlide = remember { Animatable(-30f) }
    val formFade = remember { Animatable(0f) }
    val formSlide = remember { Animatable(30f) }
    val pulse = rememberInfiniteTransition(label = "addPostPulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = RNTimingEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LaunchedEffect(editPostData) {
        val url = editPostData.str("FeaturedImageURL")
        if (isEditMode && !url.isNullOrEmpty()) {
            postImages = listOf(
                ImageItem(
                    id = editPostData.str("FeaturedImageFileName") ?: "featured",
                    uri = url,
                    name = editPostData.str("FeaturedImageFileName")
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        launch {
            try {
                loadingCategories = true
                val response = Api.get("BlogPostCategory?filterActive=true&currentPage=1&pageSize=1000")
                if (response.ok) {
                    val data = response.json.list("Data")
                    categories = data
                        .filter { it["Active"].bool }
                        .map { category ->
                            val id = category.long("BlogPostCategoryId") ?: 0L
                            PickerOption(
                                value = id,
                                label = category.str("Name")?.takeIf { it.isNotEmpty() } ?: "دسته $id"
                            )
                        }
                } else {
                    categories = emptyList()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                categories = emptyList()
            } finally {
                loadingCategories = false
            }
        }
        backAnim.animateTo(1f, tween(300, easing = RNTimingEasing))
        coroutineScope {
            launch { iconFade.animateTo(1f, tween(600, easing = RNTimingEasing)) }
            launch { iconSlide.animateTo(0f, tween(600, easing = RNTimingEasing)) }
        }
        coroutineScope {
            launch { formFade.animateTo(1f, tween(700, easing = RNTimingEasing)) }
            launch { formSlide.animateTo(0f, tween(700, easing = RNTimingEasing)) }
        }
    }

    LaunchedEffect(categories, editPostData) {
        if (!isEditMode || categoriesApplied || categories.isEmpty()) return@LaunchedEffect
        val raw = editPostData.str("BlogPostCategoriesStr") ?: ""
        val ids = raw.split("،").mapNotNull { name ->
            val trimmed = name.trim()
            if (trimmed.isEmpty()) null
            else categories.find { it.label == trimmed }?.value as? Long
        }
        if (form.categoryIds.isEmpty()) {
            form = form.copy(categoryIds = ids)
        }
        categoriesApplied = true
    }

    fun goMyPosts() {
        navigation.goBack()
    }

    fun submit() {
        if (isSubmitting) return
        val validation = linkedMapOf<String, String>()
        if (form.title.trim().isEmpty()) validation["title"] = "عنوان پست الزامی است"
        if (form.content.trim().isEmpty()) validation["content"] = "محتوای پست الزامی است"
        if (form.categoryIds.isEmpty()) validation["categories"] = "حداقل یک دسته‌بندی الزامی است"
        if (validation.isNotEmpty()) {
            showToast(validation.values.first(), "error")
            errors = PostFieldErrors(
                title = validation["title"],
                content = validation["content"],
                categories = validation["categories"]
            )
            return
        }
        errors = PostFieldErrors()
        val memberId = user?.MemberId
        if (memberId == null) {
            showToast(if (isEditMode) "خطا در ویرایش پست" else "خطا در ثبت پست", "error")
            return
        }
        scope.launch {
            isSubmitting = true
            try {
                val imagesToUpload = if (form.images.isNotEmpty()) form.images else postImages
                val first = imagesToUpload.firstOrNull()
                val hasNewImage = first != null &&
                    first.uri.isNotEmpty() &&
                    !first.uri.startsWith("http")

                val fields = mutableListOf<Pair<String, String>>()
                val blogId = if (isEditMode) (editPostData.long("BlogPostId")?.toString() ?: editPostData.str("BlogPostId") ?: "0") else "0"
                val likeCount = if (isEditMode) {
                    val liked = editPostData?.get("LikeCount")
                    if (liked == null || !jsTruthy(liked)) "0" else jsToString(liked)
                } else "0"
                fields += "BlogPostId" to blogId
                fields += "MemberId" to memberId.toString()
                fields += "Title" to form.title.trim()
                fields += "Content" to form.content.trim()
                fields += "CommentEnabled" to form.commentEnabled.toString()
                fields += "Active" to form.active.toString()
                fields += "LikeCount" to likeCount
                fields += "Rating" to "0"
                form.categoryIds.forEach { id ->
                    fields += "cateogoryIdList" to id.toString()
                }
                val categoryNames = form.categoryIds.map { id ->
                    categories.find { pickerValueEquals(it.value, id) }?.label ?: "دسته $id"
                }
                fields += "BlogPostCategoriesStr" to categoryNames.joinToString("،")
                fields += "InsertDate" to isoNowUtc()
                fields += "ShamsiInsertDate" to ""

                var file: ApiFilePart? = null
                if (hasNewImage && first != null) {
                    val bytes = first.bytesOrNull()
                        ?: throw Exception(if (isEditMode) "خطا در ویرایش پست" else "خطا در ثبت پست")
                    var fileType = first.type ?: "image/jpeg"
                    if (!fileType.startsWith("image/")) {
                        val ext = first.uri.substringAfterLast('.', "").lowercase()
                        if (ext == "png") fileType = "image/png"
                        else if (ext == "jpg" || ext == "jpeg") fileType = "image/jpeg"
                    }
                    val fileName = first.name ?: "featured-image-${currentTimeMillis()}.jpg"
                    file = ApiFilePart("featuredImageFile", fileName, fileType, bytes)
                    fields += "FeaturedImageFileName" to fileName
                    fields += "FeaturedImageURL" to ""
                } else {
                    fields += "FeaturedImageFileName" to ""
                    fields += "FeaturedImageURL" to ""
                    val keep = imagesToUpload.firstOrNull()?.uri?.isNotEmpty() == true
                    fields += "DeleteFeaturedImage" to if (keep) "false" else "true"
                }

                val response = submitBlogPostMultipart(isEditMode, fields, file)
                if (response.ok) {
                    showToast(
                        if (isEditMode) "پست با موفقیت ویرایش شد" else "پست با موفقیت ثبت شد",
                        "success"
                    )
                    if (!isEditMode) {
                        form = PostForm()
                        postImages = emptyList()
                    }
                    goMyPosts()
                } else {
                    val message = response.json.str("Message")?.takeIf { it.isNotEmpty() }
                        ?: response.text.takeIf { it.isNotEmpty() }
                        ?: if (isEditMode) "خطا در ویرایش پست" else "خطا در ثبت پست"
                    throw Exception(message)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(
                    e.message?.takeIf { it.isNotEmpty() }
                        ?: if (isEditMode) "خطا در ویرایش پست" else "خطا در ثبت پست",
                    "error"
                )
            } finally {
                isSubmitting = false
            }
        }
    }

    val bgBitmap = imageResource(Res.drawable.background_1)
    val bgBrush = remember(bgBitmap) { ShaderBrush(ImageShader(bgBitmap, TileMode.Repeated, TileMode.Repeated)) }
    val glass = RoundedCornerShape(25.dp)
    val uploadShape = RoundedCornerShape(15.dp)
    val selectedCategories = form.categoryIds.mapNotNull { id ->
        categories.find { pickerValueEquals(it.value, id) }
    }
    val categoryPlaceholder = when {
        loadingCategories -> "در حال بارگذاری..."
        form.categoryIds.isEmpty() -> "انتخاب دسته‌بندی پست"
        else -> "${form.categoryIds.size} دسته انتخاب شده"
    }

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
            Screen {
                Box(Modifier.fillMaxSize()) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .imePadding()
                            .padding(10.dp)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 50.dp)
                                .zIndex(1000f)
                                .graphicsLayer {
                                    alpha = iconFade.value
                                    translationY = iconSlide.value * density.density
                                    scaleX = pulseScale
                                    scaleY = pulseScale
                                },
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
                                        .graphicsLayer { }
                                        .clip(CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(Modifier.fillMaxSize().drawBehind {
                                        drawCircle(rgba(255, 206, 232, 0.15f))
                                    })
                                    MaterialIcons(
                                        name = if (isEditMode) "edit" else "article",
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
                                .padding(bottom = 80.dp)
                                .graphicsLayer {
                                    alpha = formFade.value
                                    translationY = formSlide.value * density.density
                                }
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
                                AppText(
                                    text = if (isEditMode) "ویرایش پست" else "افزودن پست جدید",
                                    modifier = Modifier.fillMaxWidth().absolutePadding(left = 56.dp, right = 56.dp).padding(top = 35.dp, bottom = 30.dp),
                                    fontSize = 22.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    color = FarimodColors.Primary,
                                    textAlign = TextAlign.Center
                                ,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)

                                AppTextInput(
                                    value = form.title,
                                    onChangeText = { form = form.copy(title = it) },
                                    icon = "title",
                                    label = "عنوان پست",
                                    placeholder = "عنوان پست",
                                    error = errors.title
                                )
                                if (errors.title != null) {
                                    FieldError(errors.title)
                                }

                                AppText(
                                    text = "دسته بندی",
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    fontSize = 15.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    color = FarimodColors.Dark,
                                    textAlign = TextAlign.Right
                                )
                                Picker(
                                    items = categories,
                                    onSelectItem = { _ -> },
                                    placeholder = categoryPlaceholder,
                                    icon = "category",
                                    selectedItems = selectedCategories,
                                    disabled = loadingCategories,
                                    multiSelect = true,
                                    error = errors.categories,
                                    onMultiSelectChange = { selected ->
                                        form = form.copy(
                                            categoryIds = selected.mapNotNull { (it.value as? Number)?.toLong() }
                                        )
                                    }
                                )
                                if (errors.categories != null) {
                                    FieldError(errors.categories)
                                }

                                AppTextInput(
                                    value = form.content,
                                    onChangeText = { form = form.copy(content = it) },
                                    modifier = Modifier.padding(top = 26.dp),
                                    icon = "description",
                                    label = "محتوای پست",
                                    placeholder = "محتوای پست",
                                    isLargeInput = true,
                                    height = 140.dp,
                                    error = errors.content,
                                    marginBottom = 16.dp
                                )
                                if (errors.content != null) {
                                    FieldError(errors.content)
                                }

                                AppText(
                                    text = "وضعیت انتشار",
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    fontSize = 15.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    color = FarimodColors.Dark,
                                    textAlign = TextAlign.Right
                                )
                                Picker(
                                    items = listOf(
                                        PickerOption("منتشر شده", true),
                                        PickerOption("پیش\u200cنویس", false)
                                    ),
                                    onSelectItem = { item ->
                                        form = form.copy(active = item.value == true)
                                    },
                                    placeholder = "وضعیت انتشار",
                                    icon = "visibility",
                                    selectedItem = PickerOption(
                                        label = if (form.active) "منتشر شده" else "پیش\u200cنویس",
                                        value = form.active
                                    )
                                )

                                ImageUpload(
                                    modifier = Modifier
                                        .background(rgba(255, 255, 255, 0.3f), uploadShape)
                                        .border(1.dp, rgba(255, 206, 232, 0.2f), uploadShape)
                                        .padding(15.dp),
                                    onImageChange = { change ->
                                        val imageArray = change.list.filter { it.uri.isNotEmpty() }
                                        postImages = imageArray
                                        form = form.copy(images = imageArray)
                                    },
                                    isMultiple = false,
                                    imageQuality = 1f,
                                    allowCamera = true,
                                    allowGallery = true,
                                    maxImages = 1,
                                    placeholder = "انتخاب عکس شاخص(تصویر افقی)",
                                    initialImage = postImages.firstOrNull(),
                                    onShowToast = { message, type -> showToast(message, type) }
                                )

                                Spacer(Modifier.height(20.dp))
                                AppButton(
                                    title = when {
                                        isSubmitting && isEditMode -> "در حال ویرایش..."
                                        isSubmitting -> "در حال ثبت..."
                                        isEditMode -> "ویرایش پست"
                                        else -> "ثبت پست"
                                    },
                                    onPress = { submit() },
                                    color = if (isEditMode) FarimodColors.Info else FarimodColors.Success,
                                    disabled = isSubmitting
                                )
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
                        Box(Modifier.offset(y = 5.dp).graphicsLayer { alpha = backAnim.value }) {
                            Tooltip(
                                content = "پست یا مقاله جدید بنویسید. عنوان، محتوای کامل مقاله، تصویر شاخص و دسته‌بندی را مشخص کنید."
                            )
                        }
                        Box(
                            Modifier
                                .graphicsLayer { alpha = backAnim.value }
                                .touchableOpacity { goMyPosts() }
                                .background(Color(0xFF9E22AD), RoundedCornerShape(25.dp))
                                .border(1.dp, rgba(255, 206, 232, 0.5f), RoundedCornerShape(25.dp))
                                .padding(10.dp)
                        ) {
                            MaterialIcons("arrow-forward", size = 24.dp, color = Color.White)
                        }
                    }
                }
            }
        }
        Toast(toast.visible, toast.message, toast.type) { toast = toast.copy(visible = false) }
    }
}

@Composable
private fun FieldError(text: String?) {
    if (text.isNullOrEmpty()) return
    AppText(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 10.dp),
        fontSize = 12.sp,
        color = Color(0xFFE74C3C),
        fontFamily = RNFonts.Yekan_Bakh_Regular,
        textAlign = TextAlign.Right
    )
}
