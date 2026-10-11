package ir.farimod.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.platform.isIOS
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

// ──────────────────────────────────────────────────────────────────────────────
// Port of app/components/FilterModal.tsx
// ──────────────────────────────────────────────────────────────────────────────

/**
 * RN `customFilterOptions.sections[i]` — only `key` and `options` are read by FilterModal
 * (`"categoryId"` / `"memberGroupId"`); `title`, `type`, `icon`, `placeholder` are carried for parity with the RN objects.
 */
data class FilterSection(
    val key: String,
    val options: List<PickerOption> = emptyList(),
    val title: String? = null,
    val type: String? = null,
    val icon: String? = null,
    val placeholder: String? = null
)

/** RN `customFilterOptions` object: `{ title?, icon?, sections? }`. */
data class FilterOptions(
    val title: String? = null,
    val icon: String? = null,
    val sections: List<FilterSection> = emptyList()
)

private val fmPrimary = Color(0xFF667EEA)
private val fmPrimaryDark = Color(0xFF764BA2)
private val fmMedium = Color(0xFF34495E)

/** JS truthiness (`null`, `""`, `0`, `false` are falsy). */
private fun jsTruthy(v: Any?): Boolean = when (v) {
    null -> false
    is String -> v.isNotEmpty()
    is Boolean -> v
    is Number -> v.toDouble() != 0.0 && !v.toDouble().isNaN()
    else -> true
}

/** JS `a || b`. */
private fun jsOr(a: Any?, b: Any?): Any? = if (jsTruthy(a)) a else b

private fun jsStr(v: Any?): String = when (v) {
    is Number -> {
        val d = v.toDouble()
        if (d == d.toLong().toDouble()) d.toLong().toString() else v.toString()
    }
    else -> v.toString()
}

private fun FilterOptions?.optionsOf(key: String): List<PickerOption> =
    this?.sections?.firstOrNull { it.key == key }?.options ?: emptyList()

private fun FilterOptions?.findOption(key: String, value: Any?): PickerOption? =
    this?.sections?.firstOrNull { it.key == key }?.options?.firstOrNull { jsStr(it.value) == jsStr(value) }

/**
 * RN `FilterModal` — bottom-sheet with search field(s) and pickers.
 *
 * @param visible        RN `visible`
 * @param onClose        called after the closing animation (also for انصراف / back button)
 * @param onApplyFilters called with the RN `finalFilters` object, e.g. `{filterProductName, filterProductCategoryId}`,
 *                       `{filterCategoryId, filterTitle}` (blog), `{filterTitle}` (portfolio), `{filterMemberGroupId, filterName}` (members).
 *                       Only non-empty / non-`"all"` keys are present (same as RN), insertion order as in RN.
 * @param filterType     `"products"` (default) | `"blog"` | `"portfolio"` | `"members"`
 * @param initialFilters previously applied filters (`filterProductName`, `filterProductCategoryId`, `filterCategoryId`,
 *                       `filterTitle`, `filterMemberGroupId`, `filterName`)
 * @param customFilterOptions RN `customFilterOptions` ([FilterOptions]; sections `categoryId` / `memberGroupId` hold [PickerOption]s)
 *
 * The RN `Modal` (`transparent`, `animationType="none"`) is a full-screen [Dialog]; the sheet slides 300→0 / fades 0→1 over
 * 300 ms on open and 0→300 / 1→0 over 250 ms on close.
 */
@Composable
fun FilterModal(
    visible: Boolean,
    onClose: () -> Unit,
    onApplyFilters: (Map<String, Any?>) -> Unit,
    filterType: String = "products",
    initialFilters: Map<String, Any?> = emptyMap(),
    customFilterOptions: FilterOptions? = null
) {
    var filters by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }
    var localSearchText by remember {
        mutableStateOf((jsOr(initialFilters["filterName"], jsOr(initialFilters["filterTitle"], "")) as? String) ?: "")
    }
    var selectedCategory by remember { mutableStateOf<PickerOption?>(null) }
    var selectedMemberGroup by remember { mutableStateOf<PickerOption?>(null) }

    val scope = rememberCoroutineScope()
    val modalSlideAnim = remember { Animatable(300f) }
    val modalOpacityAnim = remember { Animatable(0f) }
    val screenHeight = windowSize().second
    val safeAreaBottom = if (isIOS) (if (screenHeight > 736.dp) 34.dp else 0.dp) else 0.dp

    LaunchedEffect(visible, filterType, initialFilters, customFilterOptions) {
        if (visible) {
            when (filterType) {
                "products" -> {
                    filters = mapOf(
                        "categoryId" to jsOr(initialFilters["filterProductCategoryId"], "all"),
                        "filterProductName" to jsOr(initialFilters["filterProductName"], "")
                    )
                    localSearchText = (jsOr(initialFilters["filterProductName"], "") as? String) ?: ""
                    selectedCategory =
                        if (jsTruthy(initialFilters["filterProductCategoryId"]) && customFilterOptions?.sections != null)
                            customFilterOptions.findOption("categoryId", initialFilters["filterProductCategoryId"])
                        else null
                }
                "blog" -> {
                    filters = mapOf(
                        "categoryId" to jsOr(initialFilters["filterCategoryId"], "all"),
                        "filterTitle" to jsOr(initialFilters["filterTitle"], "")
                    )
                    localSearchText = (jsOr(initialFilters["filterTitle"], "") as? String) ?: ""
                    selectedCategory =
                        if (jsTruthy(initialFilters["filterCategoryId"]) && customFilterOptions?.sections != null)
                            customFilterOptions.findOption("categoryId", initialFilters["filterCategoryId"])
                        else null
                }
                "portfolio" -> {
                    filters = mapOf("filterTitle" to jsOr(initialFilters["filterTitle"], ""))
                    localSearchText = (jsOr(initialFilters["filterTitle"], "") as? String) ?: ""
                }
                "members" -> {
                    filters = mapOf(
                        "filterName" to jsOr(initialFilters["filterName"], ""),
                        "memberGroupId" to jsOr(initialFilters["filterMemberGroupId"], "all")
                    )
                    localSearchText = (jsOr(initialFilters["filterName"], "") as? String) ?: ""
                    selectedMemberGroup =
                        if (jsTruthy(initialFilters["filterMemberGroupId"]) && customFilterOptions?.sections != null)
                            customFilterOptions.findOption("memberGroupId", initialFilters["filterMemberGroupId"])
                        else null
                }
            }
            coroutineScope {
                launch { modalSlideAnim.animateTo(0f, tween(300, easing = ModalTimingEasing)) }
                launch { modalOpacityAnim.animateTo(1f, tween(300, easing = ModalTimingEasing)) }
            }
        }
    }

    fun closeModal() {
        scope.launch {
            coroutineScope {
                launch { modalSlideAnim.animateTo(300f, tween(250, easing = ModalTimingEasing)) }
                launch { modalOpacityAnim.animateTo(0f, tween(250, easing = ModalTimingEasing)) }
            }
            onClose()
        }
    }

    fun handleCategorySelect(category: PickerOption) {
        selectedCategory = category
        filters = filters + ("categoryId" to category.value)
    }

    fun handleMemberGroupSelect(group: PickerOption) {
        selectedMemberGroup = group
        filters = filters + ("memberGroupId" to group.value)
    }

    fun handleSearchTextChange(text: String) {
        localSearchText = text
        filters = filters + mapOf("filterTitle" to text, "filterName" to text)
    }

    fun handleApplyFilters() {
        val finalFilters = LinkedHashMap<String, Any?>()
        val trimmed = localSearchText.trim()
        when (filterType) {
            "products" -> {
                if (trimmed.isNotEmpty()) finalFilters["filterProductName"] = trimmed
                if (jsTruthy(filters["categoryId"]) && filters["categoryId"] != "all") {
                    finalFilters["filterProductCategoryId"] = filters["categoryId"]
                }
            }
            "blog" -> {
                if (jsTruthy(filters["categoryId"]) && filters["categoryId"] != "all") {
                    finalFilters["filterCategoryId"] = filters["categoryId"]
                }
                if (trimmed.isNotEmpty()) finalFilters["filterTitle"] = trimmed
            }
            "portfolio" -> {
                if (trimmed.isNotEmpty()) finalFilters["filterTitle"] = trimmed
            }
            "members" -> {
                if (jsTruthy(filters["memberGroupId"]) && filters["memberGroupId"] != "all") {
                    finalFilters["filterMemberGroupId"] = filters["memberGroupId"]
                }
                if (trimmed.isNotEmpty()) finalFilters["filterName"] = trimmed
            }
        }
        onApplyFilters(finalFilters)
        closeModal()
    }

    fun resetFilters() {
        when (filterType) {
            "blog" -> {
                localSearchText = ""
                selectedCategory = null
                filters = mapOf("categoryId" to "all", "filterTitle" to "")
            }
            "portfolio" -> {
                localSearchText = ""
                filters = mapOf("filterTitle" to "")
            }
            "members" -> {
                localSearchText = ""
                selectedMemberGroup = null
                filters = mapOf("filterName" to "", "memberGroupId" to "all")
            }
            else -> {
                localSearchText = ""
                selectedCategory = null
                filters = mapOf("categoryId" to "all", "filterProductName" to "")
            }
        }
    }

    val modalTitle = customFilterOptions?.title?.takeIf { it.isNotEmpty() } ?: when (filterType) {
        "blog" -> "فیلتر مقالات"
        "portfolio" -> "فیلتر نمونه کارها"
        "members" -> "جستجوی اعضا"
        else -> "فیلتر محصولات"
    }
    val modalIcon = customFilterOptions?.icon?.takeIf { it.isNotEmpty() } ?: when (filterType) {
        "blog" -> "article"
        "portfolio" -> "brush"
        "members" -> "people"
        else -> "filter-list"
    }

    if (!visible) return

    val density = LocalDensity.current
    Dialog(
        onDismissRequest = { closeModal() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        // modalOverlay
        Box(
            Modifier.fillMaxSize().background(Color(0f, 0f, 0f, 0.6f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            val sheetShape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .graphicsLayer {
                        translationY = modalSlideAnim.value * density.density
                        alpha = modalOpacityAnim.value
                    }
                    .background(Color(0xFFFFFFFF), sheetShape)
                    .clip(sheetShape)
            ) {
                // Header
                Column(
                    Modifier
                        .fillMaxWidth()
                        .modalBorderBottom(1.dp, Color(0xFFF0F0F0))
                        .padding(top = 12.dp, bottom = 20.dp)
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .padding(bottom = 15.dp)
                            .size(width = 40.dp, height = 4.dp)
                            .background(Color(0xFFD1D5DB), RoundedCornerShape(2.dp))
                    )
                    RowReverse(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RowReverse(verticalAlignment = Alignment.CenterVertically) {
                            MaterialIcons(modalIcon, size = 24.dp, color = fmPrimary)
                            AppText(
                                modalTitle,
                                modifier = Modifier.absolutePadding(right = 8.dp),
                                fontSize = 20.sp,
                                color = Color(0xFF1F2937),
                                fontFamily = RNFonts.Yekan_Bakh_ExtraBold
                            )
                        }
                        Box(
                            Modifier
                                .touchableOpacity(activeOpacity = 0.7f) { resetFilters() }
                                .size(36.dp)
                                .background(Color(0xFFF8FAFC), CircleShape)
                                .border(1.dp, Color(0xFFE2E8F0), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            MaterialIcons("refresh", size = 20.dp, color = fmMedium)
                        }
                    }
                }

                // Filters
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    @Composable
                    fun SectionHeader(icon: String, title: String) {
                        RowReverse(
                            Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MaterialIcons(icon, size = 20.dp, color = fmPrimary)
                            AppText(
                                title,
                                modifier = Modifier.absolutePadding(right = 8.dp),
                                fontSize = 16.sp,
                                color = Color(0xFF2C3E50),
                                fontFamily = RNFonts.Yekan_Bakh_Bold
                            )
                        }
                    }

                    @Composable
                    fun SearchSection(title: String, placeholder: String) {
                        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                            SectionHeader("search", title)
                            AppTextInput(
                                value = localSearchText,
                                onChangeText = { handleSearchTextChange(it) },
                                icon = "search",
                                placeholder = placeholder
                            )
                        }
                    }

                    @Composable
                    fun CategorySection() {
                        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                            SectionHeader("category", "دسته‌بندی")
                            Picker(
                                icon = "category",
                                placeholder = "انتخاب دسته‌بندی",
                                items = customFilterOptions.optionsOf("categoryId"),
                                selectedItem = selectedCategory,
                                onSelectItem = { handleCategorySelect(it) },
                                width = "100%"
                            )
                        }
                    }

                    when (filterType) {
                        "blog" -> if (customFilterOptions != null) {
                            SearchSection("جستجو در مقالات", "عنوان مقاله را وارد کنید...")
                            CategorySection()
                        }
                        "portfolio" -> {
                            SearchSection("جستجو در نمونه کارها", "عنوان نمونه کار را وارد کنید...")
                        }
                        "members" -> {
                            SearchSection("جستجوی نام عضو", "نام عضو را وارد کنید...")
                            val memberGroupOptions = customFilterOptions.optionsOf("memberGroupId")
                            if (memberGroupOptions.isNotEmpty()) {
                                Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                                    SectionHeader("group", "گروه عضویت")
                                    Picker(
                                        icon = "group",
                                        placeholder = "انتخاب گروه",
                                        items = memberGroupOptions,
                                        selectedItem = selectedMemberGroup,
                                        onSelectItem = { handleMemberGroupSelect(it) },
                                        width = "100%"
                                    )
                                }
                            }
                        }
                        else -> { // "products" and default
                            SearchSection("جستجو در محصولات", "نام محصول را وارد کنید...")
                            if (customFilterOptions.optionsOf("categoryId").isNotEmpty()) {
                                CategorySection()
                            }
                        }
                    }
                }

                // Action Buttons
                val btnShape = RoundedCornerShape(12.dp)
                RowReverse(
                    Modifier
                        .fillMaxWidth()
                        .modalBorderTop(1.dp, Color(0xFFF0F0F0))
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RowReverse(
                        Modifier
                            .weight(1f)
                            .touchableOpacity(activeOpacity = 0.8f) { closeModal() }
                            .background(Color(0xFFF8FAFC), btnShape)
                            .border(1.dp, Color(0xFFE2E8F0), btnShape)
                            .padding(vertical = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MaterialIcons("close", size = 20.dp, color = fmMedium)
                        AppText(
                            "انصراف",
                            modifier = Modifier.absolutePadding(right = 6.dp),
                            fontSize = 16.sp,
                            color = fmMedium,
                            fontFamily = RNFonts.Yekan_Bakh_Bold
                        )
                    }
                    Box(
                        Modifier
                            .weight(2f)
                            .touchableOpacity(activeOpacity = 0.8f) { handleApplyFilters() }
                    ) {
                        LinearGradient(
                            colors = listOf(fmPrimary, fmPrimaryDark),
                            modifier = Modifier.fillMaxWidth(),
                            start = Offset(0f, 0f),
                            end = Offset(1f, 1f),
                            shape = btnShape
                        ) {
                            RowReverse(
                                Modifier.fillMaxWidth().padding(vertical = 14.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MaterialIcons("check", size = 20.dp, color = Color(0xFFFFFFFF))
                                AppText(
                                    "اعمال فیلترها",
                                    modifier = Modifier.absolutePadding(right = 6.dp),
                                    fontSize = 16.sp,
                                    color = Color(0xFFFFFFFF),
                                    fontFamily = RNFonts.Yekan_Bakh_Bold
                                )
                            }
                        }
                    }
                }

                Box(Modifier.fillMaxWidth().height(safeAreaBottom).background(Color(0xFFFFFFFF)))
            }
        }
    }
}
