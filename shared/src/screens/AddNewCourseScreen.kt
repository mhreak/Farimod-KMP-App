package ir.farimod.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.material3.Text
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.navigation.useRoute
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.resources.Res
import ir.farimod.app.resources.background_1
import ir.farimod.app.screens.addnewcourse.CourseDaySession
import ir.farimod.app.screens.addnewcourse.CourseDraft
import ir.farimod.app.screens.addnewcourse.CourseWeeklySchedule
import ir.farimod.app.screens.addnewcourse.courseAmountDisplay
import ir.farimod.app.screens.addnewcourse.fetchCourseCities
import ir.farimod.app.screens.addnewcourse.fetchCourseForEdit
import ir.farimod.app.screens.addnewcourse.fetchCourseMembers
import ir.farimod.app.screens.addnewcourse.fetchCourseProvinces
import ir.farimod.app.screens.addnewcourse.submitCourseForm
import ir.farimod.app.screens.addnewcourse.validateCourseDraft
import ir.farimod.app.state.AuthState
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.AppButton
import ir.farimod.app.ui.AppDatePicker
import ir.farimod.app.ui.AppTextInput
import ir.farimod.app.ui.ImageItem
import ir.farimod.app.ui.ImageUpload
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.Picker
import ir.farimod.app.ui.PickerOption
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.Screen
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.ToastData
import ir.farimod.app.ui.Tooltip
import ir.farimod.app.ui.pickerValueEquals
import ir.farimod.app.ui.rgba
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.rnTextStyle
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.utils.jsNow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import org.jetbrains.compose.resources.imageResource

private val courseTypeOptions = listOf(
    PickerOption("حضوری", 1),
    PickerOption("مجازی", 2),
    PickerOption("حضوری و مجازی", 3)
)
private val yesNoOptions = listOf(
    PickerOption("فعال", true),
    PickerOption("غیرفعال", false)
)
private val discountOptions = listOf(
    PickerOption("دارد", true),
    PickerOption("ندارد", false)
)

private fun estimateCourseInputHeight(text: String): Int {
    val breaks = text.count { it == '\n' }
    val wraps = text.length / 28
    return maxOf(120, (maxOf(breaks, wraps) + 1) * 22 + 20)
}

@Composable
fun AddNewCourseScreen() {
    val navigation = useNavigation()
    val route = useRoute()
    val scope = rememberCoroutineScope()
    val rawId = route.int("courseId")
    val fromData = route.json("courseData")?.get("CourseId")?.int
    val courseId = when {
        rawId != null && rawId != 0 -> rawId
        fromData != null && fromData != 0 -> fromData
        else -> null
    }

    var draft by remember { mutableStateOf(CourseDraft()) }
    var inputHeight by remember { mutableIntStateOf(120) }
    var isSubmitting by remember { mutableStateOf(false) }
    var provinces by remember { mutableStateOf<List<PickerOption>>(emptyList()) }
    var cities by remember { mutableStateOf<List<PickerOption>>(emptyList()) }
    var members by remember { mutableStateOf<List<PickerOption>>(emptyList()) }
    var loadingProvinces by remember { mutableStateOf(false) }
    var loadingCities by remember { mutableStateOf(false) }
    var loadingMembers by remember { mutableStateOf(false) }
    var loadingCourse by remember { mutableStateOf(false) }
    var featuredImage by remember { mutableStateOf<ImageItem?>(null) }
    var instructors by remember { mutableStateOf<List<PickerOption>>(emptyList()) }
    var courseJson by remember { mutableStateOf<JsonElement?>(null) }
    var searchMember by remember { mutableStateOf("") }
    var toast by remember { mutableStateOf(ToastData()) }

    fun showToast(message: String, type: String = "info") {
        toast = ToastData(visible = true, message = message, type = type)
    }

    fun goTeaching() {
        navigation.navigate("MyTeachingCourses")
    }

    LaunchedEffect(Unit) {
        loadingProvinces = true
        val (list, err) = fetchCourseProvinces()
        provinces = list
        loadingProvinces = false
        if (err != null) showToast(err, "error")
    }

    LaunchedEffect(searchMember) {
        loadingMembers = true
        val (list, err) = fetchCourseMembers(searchMember, AuthState.user?.MemberId)
        members = list
        loadingMembers = false
        if (err != null) showToast(err, "error")
    }

    LaunchedEffect(courseId) {
        val id = courseId ?: return@LaunchedEffect
        loadingCourse = true
        val loaded = fetchCourseForEdit(id, AuthState.user?.MemberId)
        if (loaded.error != null) {
            showToast(loaded.error, "error")
        } else {
            draft = loaded.draft
            featuredImage = loaded.image
            instructors = loaded.instructors
            courseJson = loaded.raw
            inputHeight = estimateCourseInputHeight(loaded.draft.description.orEmpty())
            val pid = loaded.draft.provinceId
            if (pid != null) {
                loadingCities = true
                val (list, err) = fetchCourseCities(pid)
                cities = list
                loadingCities = false
                if (err != null) showToast(err, "error")
            }
        }
        loadingCourse = false
    }

    fun submit() {
        if (isSubmitting) return
        isSubmitting = true
        scope.launch {
            try {
                val error = validateCourseDraft(draft)
                if (error != null) {
                    showToast(error, "error")
                    return@launch
                }
                val outcome = submitCourseForm(
                    draft = draft,
                    courseId = courseId,
                    courseJson = courseJson,
                    user = AuthState.user,
                    image = featuredImage,
                    instructors = instructors
                )
                showToast(outcome.message, if (outcome.ok) "success" else "error")
                if (outcome.ok) goTeaching()
            } finally {
                isSubmitting = false
            }
        }
    }

    val bgBitmap = imageResource(Res.drawable.background_1)
    val bgBrush = remember(bgBitmap) { ShaderBrush(ImageShader(bgBitmap, TileMode.Repeated, TileMode.Repeated)) }
    val density = LocalDensity.current.density
    val titleStyle = rnTextStyle(
        fontSize = 22.sp,
        color = FarimodColors.Primary,
        fontFamily = RNFonts.Yekan_Bakh_Bold,
        textAlign = TextAlign.Center
    ).copy(
        shadow = Shadow(
            color = rgba(255, 206, 232, 0.1f),
            offset = Offset(0f, density),
            blurRadius = 2f * density
        )
    )
    val boxShape = RoundedCornerShape(25.dp)

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
                            .imePadding()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Box(
                            Modifier
                                .padding(top = 50.dp)
                                .size(100.dp)
                                .align(Alignment.CenterHorizontally)
                                .zIndex(1000f),
                            contentAlignment = Alignment.Center
                        ) {
                            LinearGradient(
                                colors = listOf(FarimodColors.Primary, FarimodColors.PrimaryDark),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .rnShadow(8.dp, CircleShape, rgba(255, 206, 232, 0.2f)),
                                shape = CircleShape
                            ) {
                                Box(
                                    Modifier
                                        .align(Alignment.Center)
                                        .zIndex(99f)
                                        .size(80.dp)
                                        .background(rgba(255, 206, 232, 0.15f), CircleShape)
                                        .border(2.dp, rgba(255, 206, 232, 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MaterialIcons(name = "school", size = 50.dp, color = FarimodColors.White)
                                }
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

                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 5.dp, end = 5.dp, bottom = 80.dp)
                                .offset(y = (-45).dp)
                                .clip(boxShape)
                        ) {
                            Box(
                                Modifier
                                    .matchParentSize()
                                    .background(rgba(255, 206, 232, 0.7f), boxShape)
                                    .border(1.5.dp, rgba(255, 206, 232, 1f), boxShape)
                            )
                            Column(Modifier.fillMaxWidth().padding(25.dp)) {
                                Text(
                                    text = if (courseId != null) "ویرایش دوره" else "افزودن دوره جدید",
                                    modifier = Modifier.fillMaxWidth().padding(top = 35.dp, bottom = 30.dp),
                                    style = titleStyle
                                )
                                AppTextInput(
                                    value = draft.courseName,
                                    onChangeText = { draft = draft.copy(courseName = it) },
                                    icon = "menu-book",
                                    label = "نام دوره",
                                    placeholder = "نام دوره"
                                )
                                AppTextInput(
                                    value = draft.description.orEmpty(),
                                    onChangeText = {
                                        draft = draft.copy(description = it)
                                        inputHeight = estimateCourseInputHeight(it)
                                    },
                                    icon = "description",
                                    label = "توضیحات دوره",
                                    placeholder = "توضیحات دوره",
                                    isLargeInput = true,
                                    height = inputHeight.dp
                                )
                                CoursePickerBlock("نوع دوره") {
                                    Picker(
                                        items = courseTypeOptions,
                                        onSelectItem = { item ->
                                            draft = draft.copy(courseType = (item.value as? Number)?.toInt())
                                        },
                                        placeholder = "نوع دوره",
                                        icon = "computer",
                                        selectedItem = courseTypeOptions.find { pickerValueEquals(it.value, draft.courseType) }
                                    )
                                }
                                if (draft.courseType != 2) {
                                    CoursePickerBlock(" استان") {
                                        Picker(
                                            items = provinces,
                                            onSelectItem = { item ->
                                                val id = (item.value as? Number)?.toInt()
                                                draft = draft.copy(
                                                    provinceId = id,
                                                    provinceName = item.label,
                                                    cityId = null,
                                                    cityName = ""
                                                )
                                                cities = emptyList()
                                                if (id != null && id != 0) {
                                                    loadingCities = true
                                                    scope.launch {
                                                        val (list, err) = fetchCourseCities(id)
                                                        cities = list
                                                        loadingCities = false
                                                        if (err != null) showToast(err, "error")
                                                    }
                                                }
                                            },
                                            placeholder = if (loadingProvinces) "در حال بارگذاری..." else "استان",
                                            icon = "map",
                                            selectedItem = provinces.find { pickerValueEquals(it.value, draft.provinceId) }
                                        )
                                    }
                                    val cityBlocked = draft.provinceId == null || loadingCities || cities.isEmpty()
                                    CoursePickerBlock("شهر") {
                                        Picker(
                                            items = cities,
                                            onSelectItem = { item ->
                                                draft = draft.copy(
                                                    cityId = (item.value as? Number)?.toInt(),
                                                    cityName = item.label
                                                )
                                            },
                                            placeholder = when {
                                                loadingCities -> "در حال بارگذاری شهرها..."
                                                draft.provinceId != null -> "شهر"
                                                else -> "ابتدا استان را انتخاب کنید"
                                            },
                                            icon = "location-city",
                                            selectedItem = cities.find { pickerValueEquals(it.value, draft.cityId) },
                                            onPress = if (cityBlocked) {
                                                {
                                                    when {
                                                        draft.provinceId == null -> showToast("ابتدا استان را انتخاب کنید", "error")
                                                        loadingCities -> showToast("در حال بارگذاری...", "info")
                                                        cities.isEmpty() -> showToast("شهری یافت نشد", "error")
                                                    }
                                                }
                                            } else {
                                                null
                                            }
                                        )
                                    }
                                }
                                CoursePickerBlock("سایر مربیان") {
                                    Picker(
                                        items = members,
                                        onSelectItem = {},
                                        placeholder = if (loadingMembers) "در حال بارگذاری مربیان..." else "سایر مربیان (اختیاری)",
                                        icon = "group",
                                        multiSelect = true,
                                        selectedItems = instructors,
                                        onMultiSelectChange = { instructors = it },
                                        hasSearch = true,
                                        searchText = searchMember,
                                        onSearch = { searchMember = it },
                                        inputPlaceHolder = "جست و جوی مربیان"
                                    )
                                }
                                AppTextInput(
                                    value = draft.courseAddress,
                                    onChangeText = { draft = draft.copy(courseAddress = it) },
                                    icon = "location-on",
                                    label = "آدرس دوره",
                                    placeholder = "آدرس دوره",
                                    isLargeInput = true,
                                    height = 96.dp
                                )
                                CourseWeeklySchedule(
                                    sessions = draft.sessions,
                                    onToggle = { key, enabled ->
                                        val current = draft.sessions[key] ?: CourseDaySession()
                                        draft = draft.copy(
                                            sessions = draft.sessions + (
                                                key to current.copy(
                                                    enabled = enabled,
                                                    start = if (enabled) "" else current.start,
                                                    finish = if (enabled) "" else current.finish
                                                )
                                            )
                                        )
                                    },
                                    onStart = { key, time ->
                                        val current = draft.sessions[key] ?: CourseDaySession()
                                        draft = draft.copy(sessions = draft.sessions + (key to current.copy(start = time)))
                                    },
                                    onFinish = { key, time ->
                                        val current = draft.sessions[key] ?: CourseDaySession()
                                        draft = draft.copy(sessions = draft.sessions + (key to current.copy(finish = time)))
                                    }
                                )
                                AppTextInput(
                                    value = courseAmountDisplay(draft.registerAmount),
                                    onChangeText = { text ->
                                        draft = draft.copy(registerAmount = text.filter { it.isDigit() })
                                    },
                                    icon = "attach-money",
                                    label = "مبلغ ثبت نام (تومان)",
                                    placeholder = "مبلغ ثبت نام (تومان)",
                                    keyboardType = KeyboardType.Number
                                )
                                CoursePickerBlock("وضعیت ثبت نام") {
                                    Picker(
                                        items = yesNoOptions,
                                        onSelectItem = { item ->
                                            draft = draft.copy(registerActive = item.value as? Boolean)
                                        },
                                        placeholder = "وضعیت ثبت نام",
                                        icon = "check-circle",
                                        selectedItem = draft.registerActive?.let {
                                            PickerOption(if (it) "فعال" else "غیرفعال", it)
                                        }
                                    )
                                }
                                CoursePickerBlock("وضعیت دوره") {
                                    Picker(
                                        items = yesNoOptions,
                                        onSelectItem = { item ->
                                            draft = draft.copy(active = item.value as? Boolean)
                                        },
                                        placeholder = "وضعیت دوره",
                                        icon = "power-settings-new",
                                        selectedItem = draft.active?.let {
                                            PickerOption(if (it) "فعال" else "غیرفعال", it)
                                        }
                                    )
                                }
                                CoursePickerBlock("کد تخفیف") {
                                    Picker(
                                        items = discountOptions,
                                        onSelectItem = { item ->
                                            draft = draft.copy(allowDiscountCode = item.value as? Boolean)
                                        },
                                        placeholder = "کد تخفیف",
                                        icon = "local-offer",
                                        selectedItem = draft.allowDiscountCode?.let {
                                            PickerOption(if (it) "مجاز" else "غیرمجاز", it)
                                        }
                                    )
                                }
                                Column {
                                    CourseFieldLabel("تاریخ شروع دوره")
                                    AppDatePicker(
                                        icon = "event",
                                        placeholder = "تاریخ شروع دوره",
                                        value = draft.startDate,
                                        onDateChange = { draft = draft.copy(startDate = it) },
                                        mode = "date",
                                        minimumDate = jsNow()
                                    )
                                }
                                Column {
                                    CourseFieldLabel("تاریخ پایان دوره")
                                    AppDatePicker(
                                        icon = "event-available",
                                        placeholder = "تاریخ پایان دوره",
                                        value = draft.finishDate,
                                        onDateChange = { draft = draft.copy(finishDate = it) },
                                        mode = "date",
                                        minimumDate = draft.startDate ?: jsNow()
                                    )
                                }
                                Column {
                                    CourseFieldLabel("تاریخ شروع ثبت نام")
                                    AppDatePicker(
                                        icon = "how-to-reg",
                                        placeholder = "تاریخ شروع ثبت نام",
                                        value = draft.registerStartDate,
                                        onDateChange = { draft = draft.copy(registerStartDate = it) },
                                        mode = "date",
                                        minimumDate = jsNow()
                                    )
                                }
                                Column {
                                    CourseFieldLabel("تاریخ پایان ثبت نام")
                                    AppDatePicker(
                                        icon = "assignment-turned-in",
                                        placeholder = "تاریخ پایان ثبت نام",
                                        value = draft.registerFinishDate,
                                        onDateChange = { draft = draft.copy(registerFinishDate = it) },
                                        mode = "date",
                                        minimumDate = draft.registerStartDate ?: jsNow(),
                                        maximumDate = draft.startDate
                                    )
                                }
                                ImageUpload(
                                    onImageChange = { featuredImage = it.single },
                                    initialImage = featuredImage,
                                    isMultiple = false,
                                    placeholder = "پوستر دوره",
                                    allowFreeAspectRatio = true,
                                    allowEditing = false,
                                    allowVideos = false,
                                    allowImages = true,
                                    onShowToast = { message, type -> showToast(message, type) },
                                    loading = isSubmitting
                                )
                                AppButton(
                                    title = if (isSubmitting) "در حال ذخیره..." else if (courseId != null) "ذخیره تغییرات" else "ثبت دوره",
                                    onPress = { submit() },
                                    color = FarimodColors.Success,
                                    disabled = isSubmitting || loadingCourse
                                )
                            }
                        }
                    }

                    Box(
                        Modifier
                            .align(AbsoluteAlignment.TopLeft)
                            .absoluteOffset(x = 5.dp, y = 8.dp)
                            .zIndex(10f)
                    ) {
                        Tooltip(
                            content = "دوره آموزشی جدید خود را ایجاد کنید. اطلاعات کامل دوره شامل عنوان، توضیحات، قیمت، مدت زمان، برنامه زمانی، آدرس محل برگزاری و مربیان را وارد کنید. می‌توانید تصویر شاخص برای دوره آپلود کنید."
                        )
                    }
                    Box(
                        Modifier
                            .align(AbsoluteAlignment.TopRight)
                            .absoluteOffset(x = (-5).dp, y = 5.dp)
                            .zIndex(10f)
                            .touchableOpacity { goTeaching() }
                            .background(Color(0xFF9E22AD), RoundedCornerShape(25.dp))
                            .border(1.dp, rgba(255, 206, 232, 0.5f), RoundedCornerShape(25.dp))
                            .padding(10.dp)
                    ) {
                        MaterialIcons(name = "arrow-forward", size = 24.dp, color = FarimodColors.White)
                    }
                }
            }
        }
        Toast(toast.visible, toast.message, toast.type) { toast = toast.copy(visible = false) }
    }
}

@Composable
private fun CourseFieldLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        style = rnTextStyle(
            fontSize = 15.sp,
            color = FarimodColors.Dark,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            textAlign = TextAlign.Right
        )
    )
}

@Composable
private fun CoursePickerBlock(label: String, picker: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 15.dp)) {
        CourseFieldLabel(label)
        picker()
    }
}
