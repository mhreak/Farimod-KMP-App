package ir.farimod.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.farimod.app.components.icons.Ionicons
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.platform.isIOS
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.utils.toPersianDigits
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

// ──────────────────────────────────────────────────────────────────────────────
// Port of app/components/Picker.tsx (+ PickerItem.tsx)
// ──────────────────────────────────────────────────────────────────────────────

/** RN `Animated.timing` default easing = `Easing.inOut(Easing.ease)`. Shared by Picker / FilterModal / MenuModal. */
internal val ModalTimingEasing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

/** RN `borderBottomWidth` only (the border takes `w` of extra height below the content). */
internal fun Modifier.modalBorderBottom(w: Dp, color: Color): Modifier =
    this.drawBehind { drawRect(color, topLeft = Offset(0f, size.height - w.toPx()), size = androidx.compose.ui.geometry.Size(size.width, w.toPx())) }
        .padding(bottom = w)

/** RN `borderTopWidth` only. */
internal fun Modifier.modalBorderTop(w: Dp, color: Color): Modifier =
    this.drawBehind { drawRect(color, topLeft = Offset.Zero, size = androidx.compose.ui.geometry.Size(size.width, w.toPx())) }
        .padding(top = w)

/**
 * One entry of a [Picker] / [FilterModal] list — RN `{ value: string | number | boolean, label: string, price?: number, icon?: string }`.
 *
 * `value` is `Any?` exactly like the JS union (String, Number, Boolean ...). Two options are "the same" when their
 * `value`s are equal like JS `===` (numbers are compared numerically, so `Int 5` equals `Long 5`) — see [pickerValueEquals].
 * [icon] is a MaterialIcons name, [price] is shown as `<persian digits> تومان`.
 */
data class PickerOption(
    val label: String,
    val value: Any? = null,
    val price: Number? = null,
    val icon: String? = null
)

/** JS `a === b` for picker option values (numbers compared numerically). */
fun pickerValueEquals(a: Any?, b: Any?): Boolean {
    if (a is Number && b is Number) return a.toDouble() == b.toDouble()
    return a == b
}

private fun jsNumberToString(n: Number): String {
    val d = n.toDouble()
    return if (d == d.toLong().toDouble() && abs(d) < 1e15) d.toLong().toString() else n.toString()
}

private val modernPrimary = Color(0xFF667EEA)
private val modernPrimaryDark = Color(0xFF764BA2)
private val modernDark = Color(0xFF2C3E50)
private val modernMedium = Color(0xFF34495E)

private class PickerThemeColors(
    val primary: Color,
    val primaryDark: Color,
    val iconBackground: Color,
    val selectedIconBackground: Color
)

private fun pickerThemeColors(theme: String): PickerThemeColors =
    if (theme == "subscription") PickerThemeColors(
        primary = Color(0xFF2ECC71),
        primaryDark = Color(0xFF27AE60),
        iconBackground = rgba(46, 204, 113, 0.1f),
        selectedIconBackground = rgba(255, 255, 255, 0.2f)
    ) else PickerThemeColors(
        primary = modernPrimary,
        primaryDark = modernPrimaryDark,
        iconBackground = rgba(102, 126, 234, 0.1f),
        selectedIconBackground = rgba(255, 255, 255, 0.2f)
    )

/** RN width prop: `"100%"`, `"50%"` or a plain number string (dp). */
private fun Modifier.pickerWidth(width: String): Modifier {
    val w = width.trim()
    return if (w.endsWith("%")) {
        fillMaxWidth(((w.dropLast(1).toFloatOrNull() ?: 100f) / 100f).coerceIn(0f, 1f))
    } else {
        val n = w.toFloatOrNull()
        if (n != null) this.width(n.dp) else fillMaxWidth()
    }
}

/**
 * RN `AppPicker` (default export of Picker.tsx): a dropdown field that opens a bottom-sheet modal with the options.
 *
 * Props are the RN props 1:1 (camelCase):
 *  - [items] `{label, value, price?, icon?}` → [PickerOption]; [selectedItem] / [selectedItems] are [PickerOption]s
 *    (selection is matched by `value`, JS `===` semantics).
 *  - [onSelectItem] is called with the confirmed option (single select, only when something is selected);
 *    in [multiSelect] mode [onMultiSelectChange] receives the confirmed list instead.
 *  - [onPress]: when non-null the modal is NOT opened, it is called instead (RN behaviour).
 *  - [theme] `"default"` | `"subscription"`, [width] `"100%"`-style string, [hasSearch]/[searchText]/[onSearch]/[inputPlaceHolder]
 *    render the search box at the top of the list (the parent owns the text and filters [items]).
 *  - [numberOfColumns] and `PickerItemComponent` are declared but unused in RN as well. [style] is accepted for call-site
 *    compatibility but ignored — RN's AppPicker never applies its `style` prop. [modifier] decorates the outer wrapper.
 */
@Composable
fun Picker(
    items: List<PickerOption>,
    onSelectItem: (PickerOption) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    icon: String? = null,
    numberOfColumns: Int = 1,
    selectedItem: PickerOption? = null,
    selectedItems: List<PickerOption> = emptyList(),
    width: String = "100%",
    error: String? = null,
    disabled: Boolean = false,
    onPress: (() -> Unit)? = null,
    theme: String = "default",
    multiSelect: Boolean = false,
    onSearch: ((String) -> Unit)? = null,
    searchText: String? = null,
    hasSearch: Boolean = false,
    onMultiSelectChange: ((List<PickerOption>) -> Unit)? = null,
    inputPlaceHolder: String? = null,
    style: Modifier = Modifier
) {
    var modalVisible by remember { mutableStateOf(false) }
    var tempSelectedItem by remember { mutableStateOf(selectedItem) }
    var tempSelectedItems by remember { mutableStateOf(selectedItems) }
    val screenHeight = windowSize().second

    val scope = rememberCoroutineScope()
    val modalSlideAnim = remember { Animatable(300f) }
    val modalOpacityAnim = remember { Animatable(0f) }
    val backdropAnim = remember { Animatable(0f) }

    val modalHeight: Dp = remember(screenHeight, items.size) {
        val h = screenHeight.value
        val baseHeight = minOf(h * 0.9f, 760f)
        val contentHeight = if (items.size > 10) minOf(h * 0.96f, 820f) else baseHeight
        maxOf(320f, minOf(contentHeight, 820f)).dp
    }
    val safeAreaBottom: Dp = if (isIOS) (if (screenHeight > 736.dp) 24.dp else 8.dp) else 8.dp

    val themeColors = pickerThemeColors(theme)

    fun openModal() {
        if (disabled) return
        if (onPress != null) {
            onPress()
            return
        }
        modalVisible = true
        tempSelectedItem = selectedItem
        tempSelectedItems = selectedItems.toList()
        scope.launch {
            coroutineScope {
                launch { backdropAnim.animateTo(1f, tween(200)) }
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
            backdropAnim.animateTo(0f, tween(120))
            modalVisible = false
        }
    }

    fun handleConfirm() {
        if (multiSelect) {
            onMultiSelectChange?.invoke(tempSelectedItems)
        } else {
            tempSelectedItem?.let { onSelectItem(it) }
        }
        closeModal()
    }

    fun handleItemSelect(item: PickerOption) {
        if (multiSelect) {
            val isSelected = tempSelectedItems.any { pickerValueEquals(it.value, item.value) }
            tempSelectedItems = if (isSelected) {
                tempSelectedItems.filter { !pickerValueEquals(it.value, item.value) }
            } else {
                tempSelectedItems + item
            }
        } else {
            tempSelectedItem = item
        }
    }

    fun clearSelection() {
        if (multiSelect) tempSelectedItems = emptyList() else tempSelectedItem = null
    }

    fun isItemSelected(item: PickerOption): Boolean =
        if (multiSelect) tempSelectedItems.any { pickerValueEquals(it.value, item.value) }
        else tempSelectedItem?.let { pickerValueEquals(it.value, item.value) } == true

    fun getDisplayText(): String {
        if (multiSelect) {
            return when {
                selectedItems.isEmpty() -> placeholder
                selectedItems.size == 1 -> selectedItems[0].label
                else -> {
                    val labels = selectedItems.joinToString("، ") { it.label }
                    if (labels.length > 40) "${selectedItems.size} مورد انتخاب شده" else labels
                }
            }
        }
        return selectedItem?.label?.takeIf { it.isNotEmpty() } ?: placeholder
    }

    fun isSelected(): Boolean =
        if (multiSelect) selectedItems.isNotEmpty() else !selectedItem?.label.isNullOrEmpty()

    // ── Trigger ──────────────────────────────────────────────────────────────
    Column(modifier) {
        val shape = RoundedCornerShape(16.dp)
        RowReverse(
            Modifier
                .pickerWidth(width)
                .then(if (disabled) Modifier.alpha(0.6f) else Modifier)
                .rnShadow(3.dp, shape)
                .background(if (disabled) Color(0xFFF5F5F5) else FarimodColors.White, shape)
                .border(0.5.dp, Color(0xFF6E6E6E), shape)
                .pressable { openModal() }
                .padding(15.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (!icon.isNullOrEmpty()) {
                // icon: marginLeft 10, marginRight -3, marginVertical "auto"
                MaterialIcons(
                    icon,
                    size = 20.dp,
                    color = if (disabled) FarimodColors.Light else Color(0xFF6E6E6E),
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .absoluteOffset(x = 3.dp)
                        .absolutePadding(left = 7.dp)
                )
            }
            val selected = isSelected()
            AppText(
                getDisplayText(),
                modifier = Modifier.weight(1f),
                fontSize = if (selected) 15.sp else 16.sp,
                color = when {
                    disabled -> FarimodColors.Light
                    selected -> FarimodColors.Dark
                    else -> Color(0xFF6E6E6E)
                },
                fontFamily = if (selected) RNFonts.Yekan_Bakh_Regular else RNFonts.Yekan_Bakh_Bold,
                textAlign = TextAlign.Right
            )
            MaterialIcons(
                "arrow-drop-down",
                size = 20.dp,
                color = if (disabled) FarimodColors.Light else modernDark
            )
        }
        if (!error.isNullOrEmpty()) {
            AppText(
                error,
                modifier = Modifier.fillMaxWidth().absolutePadding(top = 5.dp, right = 5.dp),
                fontSize = 12.sp,
                color = FarimodColors.Danger,
                textAlign = TextAlign.Right
            )
        }
    }

    // ── Modal ────────────────────────────────────────────────────────────────
    if (modalVisible) {
        val density = LocalDensity.current
        Dialog(
            onDismissRequest = { closeModal() },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind { drawRect(Color(0f, 0f, 0f, 0.6f * backdropAnim.value)) }
                    .pressable { closeModal() }
                    .imePadding(),
                contentAlignment = Alignment.BottomCenter
            ) {
                val sheetShape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = screenHeight * 0.95f)
                        .height(modalHeight)
                        .graphicsLayer {
                            translationY = modalSlideAnim.value * density.density
                            alpha = modalOpacityAnim.value
                        }
                        .background(Color(0xFFFFFFFF), sheetShape)
                        .clip(sheetShape)
                        .pressable { /* swallow taps inside the sheet */ }
                ) {
                    // modalHeader
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
                            RowReverse(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                if (!icon.isNullOrEmpty()) MaterialIcons(icon, size = 24.dp, color = themeColors.primary)
                                AppText(
                                    placeholder,
                                    modifier = Modifier.absolutePadding(right = 8.dp),
                                    fontSize = 20.sp,
                                    color = Color(0xFF1F2937),
                                    fontFamily = RNFonts.Yekan_Bakh_ExtraBold
                                )
                            }
                            if (multiSelect && tempSelectedItems.isNotEmpty()) {
                                val clearShape = RoundedCornerShape(8.dp)
                                RowReverse(
                                    Modifier
                                        .touchableOpacity(activeOpacity = 0.7f) { clearSelection() }
                                        .background(Color(0xFFF8FAFC), clearShape)
                                        .border(1.dp, Color(0xFFE2E8F0), clearShape)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MaterialIcons("clear-all", size = 18.dp, color = modernMedium)
                                    AppText(
                                        "پاک کردن همه",
                                        modifier = Modifier.absolutePadding(right = 4.dp),
                                        fontSize = 12.sp,
                                        color = modernMedium,
                                        fontFamily = RNFonts.Yekan_Bakh_Bold
                                    )
                                }
                            }
                        }
                    }

                    // itemsContainer
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(top = 10.dp)
                    ) {
                        LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp)
                        ) {
                            if (hasSearch) {
                                item(key = "__search__") {
                                    val searchShape = RoundedCornerShape(12.dp)
                                    RowReverse(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp)
                                            .height(48.dp)
                                            .background(Color(0xFFF5F7FA), searchShape)
                                            .border(1.dp, Color(0xFFE4E7EC), searchShape)
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Ionicons(
                                            "search-outline",
                                            size = 20.dp,
                                            color = FarimodColors.Dark,
                                            modifier = Modifier.alpha(0.5f)
                                        )
                                        val searchValue = searchText ?: ""
                                        BasicTextField(
                                            value = searchValue,
                                            onValueChange = { onSearch?.invoke(it) },
                                            modifier = Modifier.weight(1f).absolutePadding(right = 8.dp),
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                fontSize = 14.sp,
                                                fontFamily = RNFonts.Yekan_Bakh_Regular,
                                                color = FarimodColors.Dark,
                                                textAlign = TextAlign.Right
                                            ),
                                            cursorBrush = SolidColor(FarimodColors.Dark),
                                            decorationBox = { inner ->
                                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                                    if (searchValue.isEmpty() && !inputPlaceHolder.isNullOrEmpty()) {
                                                        AppText(
                                                            inputPlaceHolder,
                                                            modifier = Modifier.fillMaxWidth(),
                                                            fontSize = 14.sp,
                                                            color = FarimodColors.Medium,
                                                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                                                            textAlign = TextAlign.Right
                                                        )
                                                    }
                                                    inner()
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            itemsIndexed(items) { _, item ->
                                val selected = isItemSelected(item)
                                val optionShape = RoundedCornerShape(16.dp)
                                RowReverse(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                        .touchableOpacity(activeOpacity = 0.7f) { handleItemSelect(item) }
                                        .background(if (selected) themeColors.primary else Color(0xFFF8FAFC), optionShape)
                                        .border(2.dp, if (selected) themeColors.primary else Color(0xFFE2E8F0), optionShape)
                                        .padding(horizontal = 16.dp, vertical = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // optionLeft
                                    RowReverse(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        if (!item.icon.isNullOrEmpty()) {
                                            Box(
                                                Modifier
                                                    .absolutePadding(left = 12.dp)
                                                    .size(40.dp)
                                                    .background(
                                                        if (selected) themeColors.selectedIconBackground else themeColors.iconBackground,
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                MaterialIcons(
                                                    item.icon,
                                                    size = 20.dp,
                                                    color = if (selected) Color(0xFFFFFFFF) else themeColors.primary
                                                )
                                            }
                                        }
                                        Column(Modifier.weight(1f)) {
                                            AppText(
                                                item.label,
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                                fontSize = 16.sp,
                                                color = if (selected) Color(0xFFFFFFFF) else modernDark,
                                                fontFamily = RNFonts.Yekan_Bakh_Bold
                                            )
                                            if (item.price != null) {
                                                AppText(
                                                    "${jsNumberToString(item.price).toPersianDigits()} تومان",
                                                    modifier = Modifier.fillMaxWidth(),
                                                    fontSize = 14.sp,
                                                    color = if (selected) rgba(255, 255, 255, 0.8f) else Color(0xFF9CA3AF),
                                                    fontFamily = RNFonts.Yekan_Bakh_Regular
                                                )
                                            }
                                        }
                                    }
                                    if (selected) {
                                        Box(
                                            Modifier
                                                .size(24.dp)
                                                .background(rgba(255, 255, 255, 0.2f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            MaterialIcons("check", size = 18.dp, color = Color(0xFFFFFFFF))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // actionButtons
                    RowReverse(
                        Modifier
                            .fillMaxWidth()
                            .modalBorderTop(1.dp, Color(0xFFF0F0F0))
                            .padding(horizontal = 20.dp)
                            .padding(top = 20.dp, bottom = if (isIOS) 18.dp + safeAreaBottom else 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val btnShape = RoundedCornerShape(12.dp)
                        // resetButton (انصراف)
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
                            MaterialIcons("close", size = 20.dp, color = modernMedium)
                            AppText(
                                "انصراف",
                                modifier = Modifier.absolutePadding(right = 6.dp),
                                fontSize = 16.sp,
                                color = modernMedium,
                                fontFamily = RNFonts.Yekan_Bakh_Bold
                            )
                        }
                        // applyButton
                        val hasSelection = if (multiSelect) tempSelectedItems.isNotEmpty() else tempSelectedItem != null
                        Box(
                            Modifier
                                .weight(2f)
                                .touchableOpacity(
                                    enabled = if (multiSelect) true else tempSelectedItem != null,
                                    activeOpacity = 0.8f
                                ) { handleConfirm() }
                        ) {
                            LinearGradient(
                                colors = if (hasSelection) listOf(themeColors.primary, themeColors.primaryDark)
                                else listOf(Color(0xFF9CA3AF), Color(0xFF6B7280)),
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
                                        if (multiSelect) "تأیید انتخاب‌ها" else "تأیید انتخاب",
                                        modifier = Modifier.absolutePadding(right = 6.dp),
                                        fontSize = 16.sp,
                                        color = Color(0xFFFFFFFF),
                                        fontFamily = RNFonts.Yekan_Bakh_Bold
                                    )
                                }
                            }
                        }
                    }

                    // modalSafeArea
                    Box(Modifier.fillMaxWidth().height(safeAreaBottom).background(Color(0xFFFFFFFF)))
                }
            }
        }
    }
}

/**
 * Port of PickerItem.tsx (not used by Picker itself in RN either).
 * Row with the label and, when [isSelected], a small primaryLight `circle` icon on the left.
 * RN draws `borderBottomWidth: 1` in `colors.primary` with `borderRadius: 16`; a straight 1dp bottom line is drawn here.
 */
@Composable
fun PickerItem(
    item: PickerOption,
    onPress: () -> Unit,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    RowReverse(
        modifier
            .padding(5.dp)
            .touchableOpacity { onPress() }
            .modalBorderBottom(1.dp, FarimodColors.Primary),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppText(item.label, modifier = Modifier.padding(10.dp))
        if (isSelected) {
            MaterialIcons(
                "circle",
                size = 18.dp,
                color = FarimodColors.PrimaryLight,
                modifier = Modifier.absolutePadding(left = 15.dp)
            )
        }
    }
}
