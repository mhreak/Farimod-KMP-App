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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.hooks.useToast
import ir.farimod.app.navigation.useIsFocused
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.navigation.useRoute
import ir.farimod.app.network.Api
import ir.farimod.app.network.bool
import ir.farimod.app.network.get
import ir.farimod.app.network.jsonOf
import ir.farimod.app.network.list
import ir.farimod.app.network.long
import ir.farimod.app.network.str
import ir.farimod.app.network.without
import ir.farimod.app.resources.Res
import ir.farimod.app.resources.background_1
import ir.farimod.app.screens.addproduct.PRODUCT_IMAGE_INITIAL_PREFIX
import ir.farimod.app.screens.addproduct.assignProductSlots
import ir.farimod.app.screens.addproduct.deleteProductImage
import ir.farimod.app.screens.addproduct.groupThousands
import ir.farimod.app.screens.addproduct.isServerProductImage
import ir.farimod.app.screens.addproduct.productIsoNow
import ir.farimod.app.screens.addproduct.removedServerSlots
import ir.farimod.app.screens.addproduct.uploadProductImages
import ir.farimod.app.services.AuthService
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
import ir.farimod.app.ui.Screen
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.Tooltip
import ir.farimod.app.ui.pickerValueEquals
import ir.farimod.app.ui.rgba
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.utils.jsNow
import ir.farimod.app.utils.jsToString
import ir.farimod.app.utils.jsTruthy
import ir.farimod.app.utils.safeNumber
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import org.jetbrains.compose.resources.imageResource

private data class ProductForm(
    val productName: String = "",
    val description: String = "",
    val price: String = "",
    val specialPrice: String = "",
    val categoryIds: List<Any?> = emptyList(),
    val active: Boolean = true
)

private data class ProductErrors(
    val productName: String? = null,
    val specialPrice: String? = null
)

private val fallbackCategories = listOf(
    PickerOption("مواد اولیه", 1),
    PickerOption("لباس زنانه", 2),
    PickerOption("تست۲", 3)
)

private val imageFields = listOf(
    "FirstImageURL",
    "SecondImageURL",
    "ThirdImageURL",
    "FourthImageURL",
    "FifthImageURL"
)

@Composable
fun AddProductScreen() {
    val navigation = useNavigation()
    val route = useRoute()
    val toast = useToast()
    val scope = rememberCoroutineScope()
    val focused = useIsFocused()
    val focusedNow = rememberUpdatedState(focused)
    val density = LocalDensity.current
    val isEditMode = route.bool("isEdit") == true
    val editProductData = route.json("productData")

    var form by remember(isEditMode, editProductData) { mutableStateOf(initialProductForm(isEditMode, editProductData)) }
    var errors by remember { mutableStateOf(ProductErrors()) }
    var isSubmitting by remember { mutableStateOf(false) }
    var uploadingImages by remember { mutableStateOf(false) }
    var featuredImage by remember { mutableStateOf<List<ImageItem>>(emptyList()) }
    var productImages by remember { mutableStateOf<List<ImageItem>>(emptyList()) }
    var originalFeaturedImageId by remember { mutableStateOf<String?>(null) }
    var categories by remember { mutableStateOf<List<PickerOption>>(emptyList()) }
    var loadingCategories by remember { mutableStateOf(true) }
    var lastSubmitTime by remember { mutableStateOf(0L) }
    var categoriesApplied by remember { mutableStateOf(false) }

    val iconFade = remember { Animatable(0f) }
    val iconSlide = remember { Animatable(-30f) }
    val pulse by rememberInfiniteTransition().animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse)
    )

    fun showToast(message: String, type: String = "error") {
        toast.showToast(message, type)
    }

    fun goMyProducts() {
        navigation.navigate("MyProduct")
    }

    LaunchedEffect(isEditMode, editProductData) {
        if (!isEditMode || editProductData == null) return@LaunchedEffect
        val featured = mutableListOf<ImageItem>()
        val featuredUrl = editProductData.str("FeaturedImageURL")
        if (jsTruthy(featuredUrl)) {
            featured += ImageItem(
                id = "${PRODUCT_IMAGE_INITIAL_PREFIX}0",
                uri = featuredUrl!!,
                name = "featured.jpg"
            )
        }
        val extras = imageFields.mapIndexedNotNull { index, field ->
            val url = editProductData.str(field)
            if (!jsTruthy(url)) null
            else ImageItem(
                id = "$PRODUCT_IMAGE_INITIAL_PREFIX${index + 1}",
                uri = url!!,
                name = "product_${index + 1}.jpg"
            )
        }
        featuredImage = featured
        productImages = extras
        originalFeaturedImageId = featured.firstOrNull()?.id
    }

    LaunchedEffect(categories, isEditMode, editProductData) {
        if (!isEditMode || editProductData == null || categories.isEmpty() || categoriesApplied) return@LaunchedEffect
        val names = editProductData.str("ProductCategories").orEmpty()
        if (names.isNotEmpty()) {
            form = form.copy(
                categoryIds = names.split("،").map { it.trim() }.mapNotNull { name ->
                    categories.find { it.label == name }?.value
                }
            )
        }
        categoriesApplied = true
    }

    LaunchedEffect(Unit) {
        loadingCategories = true
        try {
            val response = Api.get(
                "ProductCategory/GetAll",
                query = mapOf("filterActive" to true, "currentPage" to 1, "pageSize" to 20)
            )
            if (!response.ok) {
                categories = fallbackCategories
            } else {
                val data = response.json["Data"].list
                categories = if (data.isEmpty()) {
                    fallbackCategories
                } else {
                    data.filter { it.bool("Active") }.map { category ->
                        val id = category.long("ProductCategoryId") ?: category.str("ProductCategoryId")
                        PickerOption(
                            label = category.str("Name")?.takeIf { it.isNotEmpty() } ?: "دسته $id",
                            value = id
                        )
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            categories = fallbackCategories
        } finally {
            loadingCategories = false
        }
        coroutineScope {
            launch { iconFade.animateTo(1f, tween(600)) }
            launch { iconSlide.animateTo(0f, tween(600)) }
        }
    }

    fun submit() {
        val now = jsNow()
        if (now - lastSubmitTime < 3000L) {
            showToast("لطفاً کمی صبر کنید...", "warning")
            return
        }
        if (isSubmitting || uploadingImages) return
        val validation = ProductErrors(
            productName = if (form.productName.trim().isEmpty()) "نام محصول الزامی است" else null,
            specialPrice = run {
                val priceN = form.price.toDoubleOrNull()
                val specialN = form.specialPrice.toDoubleOrNull()
                if (priceN != null && priceN != 0.0 && specialN != null && specialN != 0.0 && specialN >= priceN) {
                    "قیمت ویژه باید کمتر از قیمت اصلی باشد"
                } else null
            }
        )
        var first: String? = validation.productName
        if (first == null && validation.specialPrice != null) first = validation.specialPrice
        if (form.categoryIds.isEmpty()) {
            if (first == null) first = "حداقل یک دسته‌بندی الزامی است"
        }
        if (first != null) {
            errors = validation
            showToast(first, "error")
            return
        }
        errors = ProductErrors()
        lastSubmitTime = now
        isSubmitting = true
        val snapshot = form
        val featuredSnapshot = featuredImage
        val imagesSnapshot = productImages
        scope.launch {
            try {
                val userData = AuthState.user ?: AuthService.getUserData()
                val memberId = userData?.MemberId?.takeIf { it != 0 }
                val categoryIdList = snapshot.categoryIds.map { id ->
                    val n = safeNumber(id, Double.NaN)
                    if (!n.isNaN() && n == n.toLong().toDouble()) n.toLong() else id
                }
                val active = snapshot.active
                var body = jsonOf(
                    "ProductId" to if (isEditMode) (editProductData?.long("ProductId") ?: 0) else 0,
                    "MemberId" to memberId,
                    "MemberName" to "",
                    "ProductName" to snapshot.productName.trim(),
                    "Description" to snapshot.description.trim(),
                    "Price" to moneyValue(snapshot.price),
                    "SpecialSalePrice" to moneyValue(snapshot.specialPrice),
                    "ProductCategories" to "",
                    "ProductCategoryIdList" to categoryIdList,
                    "FeaturedImageFileName" to "",
                    "FeaturedImageURL" to "",
                    "FirstImageFileName" to "",
                    "FirstImageURL" to "",
                    "SecondImageFileName" to "",
                    "SecondImageURL" to "",
                    "ThirdImageFileName" to "",
                    "ThirdImageURL" to "",
                    "FourthImageFileName" to "",
                    "FourthImageURL" to "",
                    "FifthImageFileName" to "",
                    "FifthImageURL" to "",
                    "LikeCount" to 0,
                    "Rating" to 0,
                    "Active" to active,
                    "ActiveStr" to if (active) "فعال" else "غیرفعال",
                    "InsertDate" to productIsoNow()
                )
                if (memberId == null) body = body.without("MemberId")
                val response = if (isEditMode) {
                    Api.put("Product/Edit", body = body)
                } else {
                    Api.post("Product/Add", body = body)
                }
                if (!response.ok) {
                    val message = response.json.str("Message")?.takeIf { jsTruthy(it) }
                        ?: "خطا در ${if (isEditMode) "ویرایش" else "ثبت"} محصول"
                    throw Exception(message)
                }
                val result = response.json
                val createdId = listOf(
                    result["ProductId"],
                    result["productId"],
                    result["Data"]["ProductId"],
                    if (isEditMode) editProductData?.get("ProductId") else null
                ).firstOrNull { jsTruthy(it) } ?: throw Exception("Product ID not received from server")
                showToast(if (isEditMode) "محصول با موفقیت ویرایش شد" else "محصول با موفقیت ثبت شد", "success")
                val idText = jsToString(createdId)
                val featuredNeeds = featuredSnapshot.isNotEmpty() && !isServerProductImage(featuredSnapshot[0])
                val slots = assignProductSlots(imagesSnapshot, editProductData, isEditMode)
                val imagesNeed = slots.any { it.isNew }
                if (featuredNeeds || imagesNeed) {
                    uploadingImages = true
                    showToast("شروع آپلود عکس‌ها...", "info")
                    delay(1000)
                    try {
                        uploadProductImages(idText, featuredSnapshot, slots) { message, type ->
                            showToast(message, type)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        showToast("خطا در آپلود برخی عکس‌ها: " + (e.message ?: ""), "error")
                    } finally {
                        uploadingImages = false
                    }
                }
                if (!isEditMode) {
                    form = ProductForm()
                    featuredImage = emptyList()
                    productImages = emptyList()
                }
                if (focusedNow.value) goMyProducts()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast(
                    e.message?.takeIf { it.isNotEmpty() }
                        ?: "خطا در ${if (isEditMode) "ویرایش" else "ثبت"} محصول",
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
    val selectedCategories = form.categoryIds.mapNotNull { id ->
        categories.find { pickerValueEquals(it.value, id) }
    }.filter { option ->
        val value = option.value
        value != null && value != 0 && !pickerValueEquals(value, 0)
    }
    val categoryPlaceholder = when {
        loadingCategories -> "در حال بارگذاری..."
        form.categoryIds.isEmpty() -> "انتخاب دسته‌بندی محصول"
        else -> "${form.categoryIds.size} دسته انتخاب شده"
    }
    val buttonTitle = when {
        isSubmitting && isEditMode -> "در حال ویرایش..."
        isSubmitting -> "در حال ثبت..."
        uploadingImages -> "در حال آپلود عکس‌ها..."
        isEditMode -> "ویرایش محصول"
        else -> "ثبت محصول"
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
                Box(Modifier.fillMaxSize().padding(10.dp)) {
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
                                .zIndex(1000f)
                                .graphicsLayer {
                                    alpha = iconFade.value
                                    translationY = iconSlide.value * density.density
                                    scaleX = pulse
                                    scaleY = pulse
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
                                        .clip(CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(Modifier.fillMaxSize().drawBehind { drawCircle(rgba(255, 206, 232, 0.15f)) })
                                    MaterialIcons(
                                        name = if (isEditMode) "edit" else "shopping-bag",
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
                                AppText(
                                    text = if (isEditMode) "ویرایش محصول" else "افزودن محصول جدید",
                                    modifier = Modifier.fillMaxWidth().absolutePadding(left = 56.dp, right = 56.dp).padding(top = 35.dp, bottom = 30.dp),
                                    fontSize = 22.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    color = FarimodColors.Primary,
                                    textAlign = TextAlign.Center
                                ,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                                AppTextInput(
                                    value = form.productName,
                                    onChangeText = { form = form.copy(productName = it) },
                                    icon = "shopping-bag",
                                    label = "نام محصول",
                                    placeholder = "نام محصول",
                                    error = errors.productName,
                                    inputContainerModifier = if (errors.productName != null) {
                                        Modifier.border(1.dp, Color(0xFFE74C3C), RoundedCornerShape(16.dp))
                                    } else Modifier
                                )
                                AppTextInput(
                                    value = form.description,
                                    onChangeText = { form = form.copy(description = it) },
                                    icon = "description",
                                    label = "توضیحات محصول",
                                    placeholder = "توضیحات محصول",
                                    isLargeInput = true,
                                    height = 100.dp
                                )
                                AppTextInput(
                                    value = groupThousands(form.price),
                                    onChangeText = { form = form.copy(price = it.filter { ch -> ch.isDigit() }) },
                                    icon = "attach-money",
                                    label = "قیمت (تومان)",
                                    placeholder = "قیمت (تومان)",
                                    keyboardType = KeyboardType.Number,
                                    error = null,
                                    inputContainerModifier = Modifier
                                )
                                AppTextInput(
                                    value = groupThousands(form.specialPrice),
                                    onChangeText = { form = form.copy(specialPrice = it.filter { ch -> ch.isDigit() }) },
                                    icon = "local-offer",
                                    label = "قیمت ویژه (تومان)",
                                    placeholder = "قیمت ویژه (تومان)",
                                    keyboardType = KeyboardType.Number,
                                    error = errors.specialPrice,
                                    inputContainerModifier = if (errors.specialPrice != null) {
                                        Modifier.border(1.dp, Color(0xFFE74C3C), RoundedCornerShape(16.dp))
                                    } else Modifier
                                )
                                AppText(
                                    text = "دسته بندی",
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    fontSize = 15.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                    color = FarimodColors.Dark,
                                    textAlign = TextAlign.Right
                                )
                                Picker(
                                    items = categories,
                                    onSelectItem = {},
                                    placeholder = categoryPlaceholder,
                                    icon = "category",
                                    selectedItems = selectedCategories,
                                    disabled = loadingCategories,
                                    multiSelect = true,
                                    onMultiSelectChange = { selected ->
                                        form = form.copy(categoryIds = selected.map { it.value })
                                    }
                                )
                                Spacer(Modifier.height(20.dp))
                                AppText(
                                    text = "وضعیت محصول",
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    fontSize = 15.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                    color = FarimodColors.Dark,
                                    textAlign = TextAlign.Right
                                )
                                Picker(
                                    items = listOf(
                                        PickerOption("فعال", true),
                                        PickerOption("غیرفعال", false)
                                    ),
                                    onSelectItem = { item ->
                                        form = form.copy(active = item.value == true)
                                    },
                                    placeholder = "وضعیت محصول",
                                    icon = "inventory",
                                    selectedItem = PickerOption(
                                        label = if (form.active) "فعال" else "غیرفعال",
                                        value = form.active
                                    )
                                )
                                Column(Modifier.padding(top = 25.dp, bottom = 15.dp)) {
                                    ImageUpload(
                                        modifier = Modifier
                                            .background(rgba(255, 255, 255, 0.3f), uploadShape)
                                            .border(1.dp, rgba(255, 206, 232, 0.2f), uploadShape)
                                            .padding(15.dp),
                                        onImageChange = { change ->
                                            val imageArray = change.list.filter { it.uri.isNotEmpty() }
                                            val featuredId = originalFeaturedImageId
                                            val editId = editProductData?.get("ProductId")
                                            if (isEditMode && jsTruthy(editId) && featuredId != null &&
                                                imageArray.none { it.id == featuredId }
                                            ) {
                                                scope.launch { deleteProductImage(jsToString(editId), 0) }
                                            }
                                            featuredImage = imageArray
                                        },
                                        initialImage = featuredImage.firstOrNull(),
                                        isMultiple = false,
                                        maxImages = 1,
                                        imageQuality = 0.8f,
                                        allowCamera = true,
                                        allowGallery = true,
                                        placeholder = "انتخاب تصویر شاخص محصول",
                                        aspectRatio = 1 to 1,
                                        onShowToast = { message, type -> showToast(message, type) }
                                    )
                                }
                                Column(Modifier.padding(top = 25.dp, bottom = 15.dp)) {
                                    ImageUpload(
                                        modifier = Modifier
                                            .background(rgba(255, 255, 255, 0.3f), uploadShape)
                                            .border(1.dp, rgba(255, 206, 232, 0.2f), uploadShape)
                                            .padding(15.dp),
                                        onImageChange = { change ->
                                            val imageArray = change.list.filter { it.uri.isNotEmpty() }
                                            val editId = editProductData?.get("ProductId")
                                            if (isEditMode && jsTruthy(editId) && productImages.isNotEmpty()) {
                                                removedServerSlots(productImages, imageArray).forEach { type ->
                                                    scope.launch { deleteProductImage(jsToString(editId), type) }
                                                }
                                            }
                                            productImages = imageArray
                                        },
                                        initialImages = productImages,
                                        isMultiple = true,
                                        maxImages = 5,
                                        imageQuality = 0.8f,
                                        allowCamera = true,
                                        allowGallery = true,
                                        placeholder = "انتخاب تصاویر محصول",
                                        aspectRatio = 1 to 1,
                                        onShowToast = { message, type -> showToast(message, type) }
                                    )
                                }
                                Box(Modifier.padding(top = 20.dp).alpha(if (isSubmitting || uploadingImages) 0.6f else 1f)) {
                                    AppButton(
                                        title = buttonTitle,
                                        onPress = { submit() },
                                        color = if (isEditMode) FarimodColors.Info else FarimodColors.Success,
                                        disabled = isSubmitting || uploadingImages
                                    )
                                }
                            }
                        }
                    }

                    Box(Modifier.fillMaxWidth().zIndex(10f)) {
                        Box(Modifier.absolutePadding(left = 15.dp, top = 15.dp).offset(y = 15.dp)) {
                            Tooltip(
                                content = "محصول جدید برای فروش اضافه کنید. اطلاعات کامل محصول شامل عنوان، توضیحات، قیمت، موجودی، دسته‌بندی و ویژگی‌ها را وارد کنید. می‌توانید چندین تصویر از محصول آپلود کنید تا خریداران بهتر آن را بشناسند."
                            )
                        }
                        Box(
                            Modifier
                                .align(AbsoluteAlignment.TopRight)
                                .absolutePadding(top = 15.dp, right = 15.dp)
                                .touchableOpacity { goMyProducts() }
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
        Toast(toast.toastVisible, toast.toastMessage, toast.toastType) { toast.setToastVisible(false) }
    }
}

private fun initialProductForm(isEdit: Boolean, edit: JsonElement?): ProductForm {
    if (!isEdit || edit == null) return ProductForm()
    val activeRaw = edit["Active"]
    return ProductForm(
        productName = edit.str("ProductName").orEmpty(),
        description = edit.str("Description").orEmpty(),
        price = if (edit["Price"] == null) "" else jsToString(edit["Price"]),
        specialPrice = if (edit["SpecialSalePrice"] == null) "" else jsToString(edit["SpecialSalePrice"]),
        active = if (activeRaw == null) true else activeRaw.bool
    )
}

private fun moneyValue(raw: String): Any {
    val n = raw.toDoubleOrNull() ?: 0.0
    return if (n == n.toLong().toDouble()) n.toLong() else n
}

private fun Modifier.pullUp(amount: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val shift = amount.roundToPx()
    layout(placeable.width, (placeable.height - shift).coerceAtLeast(0)) {
        placeable.place(0, -shift)
    }
}
