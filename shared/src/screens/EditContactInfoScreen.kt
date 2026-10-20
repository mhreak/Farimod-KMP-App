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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.navigation.Tabs
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.navigation.useRoute
import ir.farimod.app.network.Api
import ir.farimod.app.network.bool
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.jsonOf
import ir.farimod.app.network.list
import ir.farimod.app.network.str
import ir.farimod.app.resources.Res
import ir.farimod.app.screens.auth.AuthBackdrop
import ir.farimod.app.state.AuthState
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.AlertButton
import ir.farimod.app.ui.AppButton
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.AppTextInput
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.Picker
import ir.farimod.app.ui.PickerOption
import ir.farimod.app.ui.RNAlert
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.Screen
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.ToastData
import ir.farimod.app.ui.Tooltip
import ir.farimod.app.ui.pickerValueEquals
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.rnTiming
import ir.farimod.app.ui.touchableOpacity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import org.jetbrains.compose.resources.imageResource

private data class ContactForm(
    val mobile: String = "",
    val provinceId: Int = 0,
    val provinceName: String = "",
    val cityId: Int = 0,
    val cityName: String = "",
    val phone1: String = "",
    val phone2: String = "",
    val email: String = "",
    val websiteAddress: String = "",
    val telegramAccountId: String = "",
    val instagramAccountId: String = "",
    val whatsappAccountMobileNumber: String = "",
    val address: String = "",
    val aboutMe: String = ""
)

private val PinkGlass = Color(0xB3FFCEE8)
private val PinkBorder = Color(0xFFFFCEE8)
private val PinkSoft = Color(0x26FFCEE8)
private val PinkRing = Color(0x4DFFCEE8)
private val PinkInner = Color(0x66FFCEE8)

private fun Modifier.collapseBottom(amount: androidx.compose.ui.unit.Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val cut = amount.roundToPx()
    layout(placeable.width, (placeable.height - cut).coerceAtLeast(0)) {
        placeable.place(0, 0)
    }
}

private fun sanitizeSocial(text: String): String {
    val persian = Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF]")
    var cleaned = text.replace(persian, "").replace(Regex("[^A-Za-z0-9._@]"), "")
    if (cleaned.isNotEmpty() && !cleaned.startsWith("@")) cleaned = "@$cleaned"
    return cleaned
}

private fun contactErrors(form: ContactForm): Map<String, String> {
    val errors = linkedMapOf<String, String>()
    val mobile = form.mobile.trim()
    if (mobile.isEmpty()) errors["mobile"] = "شماره موبایل الزامی است"
    else if (!Regex("^09[0-9]{9}$").matches(mobile)) {
        errors["mobile"] = "فرمت شماره موبایل صحیح نیست (مثال: 09123456789)"
    }
    if (form.email.isNotEmpty() && !Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(form.email)) {
        errors["email"] = "فرمت ایمیل صحیح نیست"
    }
    val phone = Regex("^[0-9+\\-() ]+$")
    if (form.phone1.isNotEmpty() && !phone.matches(form.phone1)) errors["phone1"] = "فرمت شماره تلفن صحیح نیست"
    if (form.phone2.isNotEmpty() && !phone.matches(form.phone2)) errors["phone2"] = "فرمت شماره تلفن صحیح نیست"
    if (form.whatsappAccountMobileNumber.isNotEmpty() && !phone.matches(form.whatsappAccountMobileNumber)) {
        errors["whatsappAccountMobileNumber"] = "فرمت شماره واتساپ صحیح نیست"
    }
    return errors
}

@Composable
fun EditContactInfoScreen() {
    val navigation = useNavigation()
    val route = useRoute()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val contact = route.json("contactData")
    val plan = route.json("subscriptionPlan")
    val planFlag = plan?.get("planOption_ShowContanctInfo") ?: plan?.get("PlanOption_ShowContanctInfo")
    val canShow = planFlag == null || planFlag is JsonNull || planFlag.bool
    val user = AuthState.user
    var form by remember { mutableStateOf(ContactForm()) }
    var groups by remember { mutableStateOf<List<Int>>(emptyList()) }
    var initialMobile by remember { mutableStateOf("") }
    var provinces by remember { mutableStateOf<List<PickerOption>>(emptyList()) }
    var cities by remember { mutableStateOf<List<PickerOption>>(emptyList()) }
    var memberGroups by remember { mutableStateOf<List<PickerOption>>(emptyList()) }
    var loadingProvinces by remember { mutableStateOf(false) }
    var loadingCities by remember { mutableStateOf(false) }
    var loadingGroups by remember { mutableStateOf(false) }
    var loadingMobile by remember { mutableStateOf(true) }
    var submitting by remember { mutableStateOf(false) }
    var errors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var toast by remember { mutableStateOf(ToastData()) }
    val backAnim = remember { Animatable(0f) }
    val iconFade = remember { Animatable(0f) }
    val iconSlide = remember { Animatable(-30f) }
    val formFade = remember { Animatable(0f) }
    val formSlide = remember { Animatable(30f) }

    fun showToast(message: String, type: String = "info") {
        toast = ToastData(true, message, type)
    }

    fun goHome() {
        navigation.goBack()
    }

    suspend fun fetchCities(provinceId: Int) {
        if (provinceId == 0) {
            cities = emptyList()
            return
        }
        loadingCities = true
        try {
            var currentPage = 1
            val pageSize = 20
            val allCities = mutableListOf<JsonElement>()
            while (true) {
                val response = Api.get(
                    "City/GetAllActiveByProvinceId",
                    query = mapOf(
                        "provinceId" to provinceId,
                        "currentPage" to currentPage,
                        "page" to currentPage,
                        "pageSize" to pageSize
                    )
                )
                if (!response.ok) break
                val result = response.json ?: break
                val items = when {
                    result is JsonArray -> result
                    result["Items"] is JsonArray -> result["Items"]
                    result["Data"] is JsonArray -> result["Data"]
                    else -> null
                } as? JsonArray ?: break

                val list = items.list
                allCities.addAll(list)
                val total = result["Total"].int ?: result["TotalCount"].int ?: result["TotalItems"].int ?: allCities.size
                if (list.size < pageSize || allCities.size >= total) {
                    break
                }
                currentPage++
            }
            cities = allCities.map { city ->
                PickerOption(city.str("CityName").orEmpty(), city.int("CityId") ?: 0)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            cities = emptyList()
        } finally {
            loadingCities = false
        }
    }

    LaunchedEffect(Unit) {
        launch {
            loadingProvinces = true
            try {
                var currentPage = 1
                val pageSize = 20
                val allProvinces = mutableListOf<JsonElement>()
                while (true) {
                    val response = Api.get(
                        "Province/GetAllActive",
                        query = mapOf(
                            "currentPage" to currentPage,
                            "page" to currentPage,
                            "pageSize" to pageSize
                        )
                    )
                    if (!response.ok) break
                    val result = response.json ?: break
                    val items = when {
                        result is JsonArray -> result
                        result["Items"] is JsonArray -> result["Items"]
                        result["Data"] is JsonArray -> result["Data"]
                        else -> null
                    } as? JsonArray ?: break

                    val list = items.list
                    allProvinces.addAll(list)
                    val total = result["Total"].int ?: result["TotalCount"].int ?: result["TotalItems"].int ?: allProvinces.size
                    if (list.size < pageSize || allProvinces.size >= total) {
                        break
                    }
                    currentPage++
                }
                provinces = allProvinces.map { item ->
                    PickerOption(item.str("ProvinceName").orEmpty(), item.int("ProvinceId") ?: 0)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            } finally {
                loadingProvinces = false
            }
        }
        launch {
            loadingGroups = true
            try {
                val response = Api.get(
                    "MemberGroup/GetAll",
                    query = mapOf("filterGroupName" to "", "currentPage" to 1, "pageSize" to 1000)
                )
                if (response.ok) {
                    memberGroups = response.json["Data"].list
                        .filter { it.bool("Active") }
                        .map { group ->
                            val id = group.int("MemberGroupId") ?: 0
                            PickerOption(group.str("GroupName") ?: "گروه $id", id)
                        }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                memberGroups = emptyList()
            } finally {
                loadingGroups = false
            }
        }
        launch {
            loadingMobile = true
            var mobile = ""
            var serverAboutMe: String? = null
            try {
                val response = Api.get(
                    "MemberInfo/GetProfileInfoToEdit",
                    query = mapOf("memberId" to user?.MemberId)
                )
                if (response.ok) {
                    mobile = response.json.str("Mobile") ?: user?.Mobile.orEmpty()
                    serverAboutMe = response.json.str("AboutMeText") ?: response.json.str("AboutMe")
                } else {
                    mobile = user?.Mobile.orEmpty()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mobile = user?.Mobile.orEmpty()
            }
            if (mobile.isEmpty()) {
                RNAlert.alert(
                    "هشدار",
                    "شماره موبایل شما یافت نشد. لطفاً شماره موبایل خود را وارد کنید.",
                    listOf(AlertButton("متوجه شدم"))
                )
            }
            initialMobile = mobile
            form = ContactForm(
                mobile = mobile,
                provinceId = contact.int("provinceId") ?: 0,
                provinceName = contact.str("provinceName").orEmpty(),
                cityId = contact.int("cityId") ?: 0,
                cityName = contact.str("cityName").orEmpty(),
                phone1 = contact.str("phone1").orEmpty(),
                phone2 = contact.str("phone2").orEmpty(),
                email = contact.str("email").orEmpty(),
                websiteAddress = contact.str("websiteAddress").orEmpty(),
                telegramAccountId = contact.str("telegramAccountId").orEmpty(),
                instagramAccountId = contact.str("instagramAccountId").orEmpty(),
                whatsappAccountMobileNumber = contact.str("whatsappAccountMobileNumber").orEmpty(),
                address = contact.str("address").orEmpty(),
                aboutMe = contact.str("aboutMe") ?: contact.str("AboutMe") ?: contact.str("AboutMeText") ?: serverAboutMe.orEmpty()
            )
            if (form.aboutMe.isEmpty() && !serverAboutMe.isNullOrEmpty()) {
                form = form.copy(aboutMe = serverAboutMe)
            }
            groups = contact["memberGroupList"].list.mapNotNull { it.int("MemberGroupId") }
            loadingMobile = false
            val provinceId = form.provinceId
            val cityId = form.cityId
            if (provinceId != 0 && cityId != 0) {
                fetchCities(provinceId)
                cities.find { pickerValueEquals(it.value, cityId) }?.let { found ->
                    form = form.copy(cityName = found.label)
                }
            }
        }
        backAnim.animateTo(1f, rnTiming(300))
        kotlinx.coroutines.coroutineScope {
            launch { iconFade.animateTo(1f, rnTiming(600)) }
            launch { iconSlide.animateTo(0f, rnTiming(600)) }
        }
        kotlinx.coroutines.coroutineScope {
            launch { formFade.animateTo(1f, rnTiming(700)) }
            launch { formSlide.animateTo(0f, rnTiming(700)) }
        }
    }

    LaunchedEffect(canShow) {
        if (!canShow) {
            RNAlert.alert(
                "دسترسی محدود",
                "نمایش و ویرایش اطلاعات تماس در پلن اشتراک شما فعال نیست. برای استفاده از این امکان، پلن خود را ارتقا دهید.",
                listOf(AlertButton("بازگشت", style = "cancel", onPress = { goHome() })),
                cancelable = false
            )
        }
    }

    fun submit(shouldLogout: Boolean) {
        if (form.mobile.trim().isEmpty()) {
            RNAlert.alert("خطا", "شماره موبایل الزامی است", listOf(AlertButton("باشه")))
            return
        }
        scope.launch {
            submitting = true
            try {
                val payload = jsonOf(
                    "MemberId" to (user?.MemberId ?: 0),
                    "AboutMe" to form.aboutMe.trim(),
                    "CityId" to form.cityId,
                    "ProvinceId" to form.provinceId,
                    "Mobile" to form.mobile.trim(),
                    "Phone1" to form.phone1.trim(),
                    "Phone2" to form.phone2.trim(),
                    "Email" to form.email.trim(),
                    "WebsiteAddress" to form.websiteAddress.trim(),
                    "TelegramAccountId" to form.telegramAccountId.trim(),
                    "WhatsappAccountMobileNumber" to form.whatsappAccountMobileNumber.trim(),
                    "InstagramAccountId" to form.instagramAccountId.trim(),
                    "Address" to form.address.trim(),
                    "MemberGroupIdList" to groups
                )
                val response = Api.post("MemberInfo/UpdateProfile", body = payload)
                if (!response.ok) throw IllegalStateException("خطا در به‌روزرسانی اطلاعات")
                submitting = false
                showToast("اطلاعات تماس با موفقیت به‌روزرسانی شد", "success")
                if (shouldLogout) {
                    delay(1500)
                    AuthState.logout()
                    RNAlert.alert(
                        "موفق",
                        "لطفاً مجدداً با شماره موبایل جدید وارد شوید",
                        listOf(AlertButton("باشه", onPress = { navigation.navigate("Login") }))
                    )
                } else {
                    delay(2000)
                    goHome()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                submitting = false
                showToast(e.message ?: "خطا در ذخیره اطلاعات", "error")
            }
        }
    }

    fun onSave() {
        val found = contactErrors(form)
        errors = found
        if (found.isNotEmpty()) {
            val order = listOf("mobile", "email", "phone1", "phone2", "whatsappAccountMobileNumber")
            order.firstNotNullOfOrNull { found[it] }?.let { showToast(it, "error") }
            return
        }
        if (form.mobile != initialMobile) {
            RNAlert.alert(
                "تغییر شماره موبایل",
                "با تغییر شماره موبایل، باید مجدداً وارد شوید. آیا ادامه می‌دهید؟",
                listOf(
                    AlertButton("لغو", style = "cancel"),
                    AlertButton("ادامه", onPress = { submit(true) })
                )
            )
        } else {
            submit(false)
        }
    }

    val circle = RoundedCornerShape(25.dp)
    val selectedProvince = provinces.find { pickerValueEquals(it.value, form.provinceId) }
    val selectedCity = cities.find { pickerValueEquals(it.value, form.cityId) }
    val selectedGroups = groups.mapNotNull { id -> memberGroups.find { pickerValueEquals(it.value, id) } }
    val cityPlaceholder = when {
        form.provinceId == 0 -> "ابتدا استان را انتخاب کنید"
        loadingCities -> "در حال بارگذاری..."
        cities.isEmpty() -> "شهری یافت نشد"
        else -> "شهر"
    }

    AuthBackdrop {
        if (loadingMobile) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = FarimodColors.Primary)
                AppText(
                    text = "در حال بارگذاری...",
                    modifier = Modifier.padding(top = 10.dp),
                    fontSize = 18.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Screen(Modifier.background(Color.Transparent)) {
                Box(Modifier.fillMaxSize().padding(10.dp)) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .imePadding()
                            .verticalScroll(rememberScrollState())
                            .absolutePadding(left = 0.dp, right = 0.dp)
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 50.dp)
                                .padding(bottom = 0.dp)
                                .zIndex(1000f)
                                .collapseBottom(110.dp)
                                .graphicsLayer {
                                    alpha = iconFade.value
                                    translationY = iconSlide.value * density.density
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier
                                    .size(120.dp)
                                    .drawBehind {
                                        drawCircle(
                                            color = PinkRing,
                                            style = Stroke(
                                                width = 1.5.dp.toPx(),
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                                            )
                                        )
                                    }
                            )
                            LinearGradient(
                                colors = listOf(FarimodColors.Primary, FarimodColors.PrimaryDark),
                                modifier = Modifier
                                    .size(100.dp)
                                    .rnShadow(8.dp, CircleShape, PinkSoft)
                                    .clip(CircleShape),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(0f, Float.POSITIVE_INFINITY)
                            ) {
                                Box(
                                    Modifier
                                        .align(Alignment.Center)
                                        .size(80.dp)
                                        .border(2.dp, PinkInner, CircleShape)
                                        .background(Color(0x26FFCEE8), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MaterialIcons("contacts", size = 50.dp, color = FarimodColors.White)
                                }
                            }
                        }
                        Box(
                            Modifier
                                .absolutePadding(left = 5.dp, right = 5.dp)
                                .padding(top = 0.dp)
                                .padding(bottom = 80.dp)
                                .graphicsLayer {
                                    alpha = formFade.value
                                    translationY = formSlide.value * density.density
                                }
                        ) {
                            Column(
                                Modifier
                                    .padding(top = 65.dp)
                                    .fillMaxWidth()
                                    .rnShadow(6.dp, circle, Color.White)
                                    .clip(circle)
                                    .border(1.5.dp, PinkBorder, circle)
                                    .background(PinkGlass)
                                    .padding(25.dp)
                            ) {
                                AppText(
                                    text = "ویرایش اطلاعات تماس",
                                    modifier = Modifier.fillMaxWidth().absolutePadding(left = 56.dp, right = 56.dp).padding(top = 35.dp, bottom = 30.dp),
                                    fontSize = 22.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    color = FarimodColors.Primary,
                                    textAlign = TextAlign.Center
                                ,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                                if (!canShow) {
                                    RowReverse(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 20.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, Color(0x4DF59E0B), RoundedCornerShape(12.dp))
                                            .background(Color(0x1AF59E0B))
                                            .padding(15.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        MaterialIcons("lock", size = 24.dp, color = FarimodColors.Warning)
                                        AppText(
                                            text = "ویرایش اطلاعات تماس در پلن اشتراک شما فعال نیست",
                                            modifier = Modifier.weight(1f),
                                            fontSize = 14.sp,
                                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                                            color = FarimodColors.Warning,
                                            textAlign = TextAlign.Right
                                        )
                                    }
                                }
                                if (form.mobile != initialMobile) {
                                    RowReverse(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 15.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, Color(0x66F59E0B), RoundedCornerShape(12.dp))
                                            .background(Color(0xA4F59E0B))
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        MaterialIcons("warning", size = 20.dp, color = FarimodColors.White)
                                        AppText(
                                            text = "با تغییر شماره موبایل، باید مجدداً وارد شوید",
                                            modifier = Modifier.weight(1f),
                                            fontSize = 13.sp,
                                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                                            color = FarimodColors.White,
                                            textAlign = TextAlign.Right
                                        )
                                    }
                                }
                                AppTextInput(
                                    value = form.mobile,
                                    onChangeText = {
                                        form = form.copy(mobile = it)
                                        errors = errors - "mobile"
                                    },
                                    icon = "phone-android",
                                    placeholder = "شماره موبایل *",
                                    keyboardType = KeyboardType.Phone,
                                    error = errors["mobile"],
                                    editable = canShow
                                )
                                ContactSection("location-on", "موقعیت مکانی")
                                Picker(
                                    items = provinces,
                                    onSelectItem = { item ->
                                        val id = (item.value as? Number)?.toInt() ?: 0
                                        form = form.copy(
                                            provinceId = id,
                                            provinceName = item.label,
                                            cityId = 0,
                                            cityName = ""
                                        )
                                        scope.launch { fetchCities(id) }
                                    },
                                    placeholder = if (loadingProvinces) "در حال بارگذاری..." else "استان",
                                    modifier = Modifier.padding(bottom = 15.dp),
                                    icon = "map",
                                    selectedItem = if (form.provinceId == 0) null else selectedProvince,
                                    disabled = loadingProvinces || !canShow
                                )
                                Picker(
                                    items = cities,
                                    onSelectItem = { item ->
                                        form = form.copy(
                                            cityId = (item.value as? Number)?.toInt() ?: 0,
                                            cityName = item.label
                                        )
                                    },
                                    placeholder = cityPlaceholder,
                                    modifier = Modifier.padding(bottom = 15.dp),
                                    icon = "location-city",
                                    selectedItem = if (form.cityId == 0) null else selectedCity,
                                    disabled = form.provinceId == 0 || loadingCities || cities.isEmpty() || !canShow
                                )
                                if (form.provinceId == 0) {
                                    AppText(
                                        text = "لطفاً ابتدا استان را انتخاب کنید",
                                        modifier = Modifier.absolutePadding(right = 5.dp).padding(top = 5.dp),
                                        fontSize = 12.sp,
                                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                                        color = FarimodColors.Medium.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Right
                                    )
                                }
                                AppTextInput(
                                    value = form.address,
                                    onChangeText = { form = form.copy(address = it) },
                                    icon = "home",
                                    placeholder = "آدرس کامل",
                                    isLargeInput = true,
                                    height = 96.dp,
                                    error = errors["address"],
                                    editable = canShow
                                )
                                ContactSection("phone", "اطلاعات تماس")
                                AppTextInput(
                                    value = form.phone1,
                                    onChangeText = {
                                        form = form.copy(phone1 = it)
                                        errors = errors - "phone1"
                                    },
                                    icon = "phone",
                                    placeholder = "تلفن ثابت 1",
                                    keyboardType = KeyboardType.Phone,
                                    error = errors["phone1"],
                                    editable = canShow
                                )
                                AppTextInput(
                                    value = form.phone2,
                                    onChangeText = {
                                        form = form.copy(phone2 = it)
                                        errors = errors - "phone2"
                                    },
                                    icon = "phone",
                                    placeholder = "تلفن ثابت 2 (اختیاری)",
                                    keyboardType = KeyboardType.Phone,
                                    error = errors["phone2"],
                                    editable = canShow
                                )
                                AppTextInput(
                                    value = form.email,
                                    onChangeText = {
                                        form = form.copy(email = it)
                                        errors = errors - "email"
                                    },
                                    icon = "email",
                                    placeholder = "ایمیل",
                                    keyboardType = KeyboardType.Email,
                                    error = errors["email"],
                                    editable = canShow
                                )
                                AppTextInput(
                                    value = form.websiteAddress,
                                    onChangeText = { form = form.copy(websiteAddress = it) },
                                    icon = "language",
                                    placeholder = "وبسایت (اختیاری)",
                                    keyboardType = KeyboardType.Uri,
                                    error = errors["websiteAddress"],
                                    editable = canShow
                                )
                                ContactSection("share", "شبکه‌های اجتماعی")
                                AppTextInput(
                                    value = form.telegramAccountId,
                                    onChangeText = { form = form.copy(telegramAccountId = sanitizeSocial(it)) },
                                    icon = "send",
                                    placeholder = "آیدی تلگرام (مثال: @username)",
                                    error = errors["telegramAccountId"],
                                    editable = canShow
                                )
                                AppTextInput(
                                    value = form.instagramAccountId,
                                    onChangeText = { form = form.copy(instagramAccountId = sanitizeSocial(it)) },
                                    icon = "camera-alt",
                                    placeholder = "آیدی اینستاگرام (مثال: @username)",
                                    error = errors["instagramAccountId"],
                                    editable = canShow
                                )
                                AppTextInput(
                                    value = form.whatsappAccountMobileNumber,
                                    onChangeText = {
                                        form = form.copy(whatsappAccountMobileNumber = it)
                                        errors = errors - "whatsappAccountMobileNumber"
                                    },
                                    icon = "chat",
                                    placeholder = "شماره واتساپ",
                                    keyboardType = KeyboardType.Phone,
                                    error = errors["whatsappAccountMobileNumber"],
                                    editable = canShow
                                )
                                RowReverse(
                                    Modifier.fillMaxWidth().absolutePadding(top = 20.dp, bottom = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.weight(1f).height(1.dp).background(Color(0x66CBD5E1)))
                                    AppText(
                                        text = "گروه‌های عضویت",
                                        fontSize = 18.sp,
                                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                                        color = FarimodColors.Primary,
                                        textAlign = TextAlign.Center
                                    )
                                    Box(Modifier.weight(1f).height(1.dp).background(Color(0x66CBD5E1)))
                                }
                                Picker(
                                    items = memberGroups,
                                    onSelectItem = { _ -> },
                                    placeholder = "انتخاب گروه‌های عضویت",
                                    modifier = Modifier.padding(bottom = 15.dp),
                                    icon = "groups",
                                    selectedItems = selectedGroups,
                                    disabled = !canShow || loadingGroups,
                                    multiSelect = true,
                                    onMultiSelectChange = { selected ->
                                        groups = selected.mapNotNull { (it.value as? Number)?.toInt() }
                                    }
                                )
                                ContactSection("person", "درباره من")
                                AppTextInput(
                                    value = form.aboutMe,
                                    onChangeText = { form = form.copy(aboutMe = it) },
                                    icon = "person",
                                    placeholder = "متن درباره خود را وارد کنید...",
                                    isLargeInput = true,
                                    height = 120.dp,
                                    editable = canShow
                                )
//                                RowReverse(
//                                    Modifier
//                                        .fillMaxWidth()
//                                        .padding(top = 15.dp, bottom = 15.dp)
//                                        .clip(RoundedCornerShape(8.dp))
//                                        .background(Color.White.copy(alpha = 0.3f))
//                                        .padding(10.dp),
//                                    verticalAlignment = Alignment.CenterVertically
//                                ) {
//                                    MaterialIcons("info-outline", size = 16.dp, color = FarimodColors.Medium)
//                                    AppText(
//                                        text = "شماره موبایل الزامی است. سایر فیلدها اختیاری هستند.",
//                                        modifier = Modifier.weight(1f).absolutePadding(right = 6.dp),
//                                        fontSize = 12.sp,
//                                        fontFamily = RNFonts.Yekan_Bakh_Regular,
//                                        color = FarimodColors.Medium,
//                                        textAlign = TextAlign.Right
//                                    )
//                                }
                                AppButton(
                                    title = if (submitting) "در حال ذخیره..." else "ذخیره تغییرات",
                                    onPress = { onSave() },
                                    modifier = Modifier.padding(top = 12.dp),
                                    color = FarimodColors.Success,
                                    disabled = submitting || !canShow || form.mobile.isBlank()
                                )
                            }
                        }
                    }
                    RowReverse(
                        Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .zIndex(10f)
                            .padding(15.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .graphicsLayer {
                                    alpha = backAnim.value
                                    scaleX = backAnim.value
                                    scaleY = backAnim.value
                                }
                                .touchableOpacity { goHome() }
                                .border(1.dp, Color(0x80FFCEE8), CircleShape)
                                .background(Color(0xFF9E22AD), CircleShape)
                                .padding(10.dp)
                        ) {
                            MaterialIcons("arrow-forward", size = 24.dp, color = Color.White)
                        }
                        Tooltip(
                            content = "اطلاعات تماس خود شامل تلفن، ایمیل، شبکه‌های اجتماعی و آدرس را ویرایش کنید. این اطلاعات برای سایر کاربران قابل نمایش خواهد بود."
                        )
                    }
                }
            }
        }
        Toast(toast.visible, toast.message, toast.type) { toast = toast.copy(visible = false) }
    }
}

@Composable
private fun ContactSection(icon: String, title: String) {
    RowReverse(
        Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 15.dp).absolutePadding(left = 5.dp, right = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MaterialIcons(icon, size = 20.dp, color = FarimodColors.Primary)
        AppText(
            text = title,
            modifier = Modifier.absolutePadding(right = 8.dp),
            fontSize = 18.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = FarimodColors.Primary,
            textAlign = TextAlign.Right
        )
    }
}
