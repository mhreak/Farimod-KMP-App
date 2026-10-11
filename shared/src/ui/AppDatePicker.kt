package ir.farimod.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.platform.isIOS
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.utils.rnGregorianToPersian
import ir.farimod.app.utils.rnPersianToGregorian
import ir.farimod.app.utils.rnTodayPersian
import ir.farimod.app.utils.toPersianDigits
import ir.farimod.app.utils.utcDateTimeToEpoch
import ir.farimod.app.utils.utcYmdOf
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ──────────────────────────────────────────────────────────────────────────────
// Port of app/components/AppDatePicker.tsx + app/components/SimpleDatePicker.js
//
// Dates are JavaScript `Date` objects in RN → here epoch milliseconds ([Long]).
// The Jalali conversions are the exact jdf-style algorithms embedded in the RN files (see utils/Jalali.kt).
// ──────────────────────────────────────────────────────────────────────────────

private fun toFarsiDigits(value: Int): String = value.toString().toPersianDigits()

private val persianMonths = listOf(
    "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
    "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
)

private fun formatPersianDate(persianDate: List<Int>?): String {
    if (persianDate == null || persianDate.size != 3) return ""
    val (year, month, day) = persianDate
    val formattedDay = if (day < 10) "0$day" else "$day"
    val formattedMonth = if (month < 10) "0$month" else "$month"
    return "${toFarsiDigits(year)}/${formattedMonth.toPersianDigits()}/${formattedDay.toPersianDigits()}"
}

/** RN: `borderBottomWidth: 1` (drawn inside the box edge, outside of the padding). */
internal fun Modifier.pickerBorderBottom(color: Color, width: Dp = 1.dp): Modifier = this.drawBehind {
    val w = width.toPx()
    drawRect(color, topLeft = Offset(0f, size.height - w), size = androidx.compose.ui.geometry.Size(size.width, w))
}

/** RN: `borderTopWidth: 1`. */
internal fun Modifier.pickerBorderTop(color: Color, width: Dp = 1.dp): Modifier = this.drawBehind {
    val w = width.toPx()
    drawRect(color, topLeft = Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(size.width, w))
}

// ══════════════════════════════════════════════════════════════════════════════
// AppDatePicker.tsx
// ══════════════════════════════════════════════════════════════════════════════

/**
 * `<AppDatePicker icon placeholder value onDateChange mode minimumDate maximumDate disabled error />`
 *
 * [value] / [onDateChange] use epoch milliseconds (RN: `Date`; the picked date is UTC-noon of the Gregorian day,
 * exactly like `persianToGregorian` in RN). [mode], [minimumDate] and [maximumDate] are accepted but unused by RN too.
 * [modifier] is applied to the touchable wrapper (RN has `marginBottom: 16` built in).
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun AppDatePicker(
    icon: String?,
    placeholder: String,
    value: Long?,
    onDateChange: (Long) -> Unit,
    mode: String = "date",
    minimumDate: Long? = null,
    maximumDate: Long? = null,
    disabled: Boolean = false,
    error: String? = null,
    modifier: Modifier = Modifier
) {
    var isPickerVisible by remember { mutableStateOf(false) }
    var selectedPersianDate by remember { mutableStateOf<List<Int>?>(null) }
    // RN: `value._fromPersianPicker` — marks Date objects that were created by this picker
    var pickedEpoch by remember { mutableStateOf<Long?>(null) }

    val persianDate: List<Int>? = if (value != null) {
        if (pickedEpoch == value && selectedPersianDate != null) selectedPersianDate
        else {
            val (y, m, d) = utcYmdOf(value)
            rnGregorianToPersian(y, m, d)
        }
    } else null

    val displayText = if (persianDate != null) formatPersianDate(persianDate) else ""

    val handleConfirm: (List<Int>) -> Unit = { newPersianDate ->
        selectedPersianDate = newPersianDate
        val g = rnPersianToGregorian(newPersianDate[0], newPersianDate[1], newPersianDate[2])
        val epoch = utcDateTimeToEpoch(g.gy, g.gm, g.gd, 12, 0, 0, 0)
        pickedEpoch = epoch
        onDateChange(epoch)
        isPickerVisible = false
    }

    val iconColor = if (disabled) FarimodColors.Light else Color(0xFF666666)
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .absolutePadding(bottom = 16.dp) // touchableWrapper.marginBottom
            .touchableOpacity(enabled = !disabled, activeOpacity = 0.7f) {
                if (!disabled) isPickerVisible = true
            }
    ) {
        RowReverse(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (disabled) Modifier.alpha(0.6f) else Modifier)
                .rnShadow(3.dp, shape, Color(16 / 255f, 185 / 255f, 129 / 255f, 0.3f))
                .background(if (disabled) Color(0xFFF5F5F5) else FarimodColors.White, shape)
                .border(1.dp, Color(0xFFCCCCCC), shape)
                .padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!icon.isNullOrEmpty()) {
                // styles.icon = { marginLeft: 10, marginRight: -3 }  (negative margin → custom layout)
                MaterialIcons(
                    name = icon,
                    size = 20.dp,
                    color = iconColor,
                    modifier = Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val boxWidth = placeable.width + 10.dp.roundToPx() - 3.dp.roundToPx()
                        layout(boxWidth, placeable.height) {
                            placeable.place(10.dp.roundToPx(), 0)
                        }
                    }
                )
            }
            if (displayText.isNotEmpty()) {
                AppText(
                    text = displayText,
                    modifier = Modifier.weight(1f),
                    fontSize = 15.sp,
                    color = if (disabled) FarimodColors.Light else FarimodColors.Dark,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    textAlign = TextAlign.Right
                )
            } else {
                AppText(
                    text = placeholder,
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp,
                    color = if (disabled) FarimodColors.Light else Color(0xFF666666),
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    textAlign = TextAlign.Right
                )
            }
            MaterialIcons(name = "calendar-today", size = 20.dp, color = iconColor)
        }
        if (!error.isNullOrEmpty()) {
            AppText(
                text = error,
                modifier = Modifier.fillMaxWidth().absolutePadding(top = 5.dp, right = 5.dp),
                fontSize = 12.sp,
                color = FarimodColors.Danger,
                textAlign = TextAlign.Right
            )
        }
    }

    SimpleDatePicker(
        isVisible = isPickerVisible,
        onClose = { isPickerVisible = false },
        onConfirm = handleConfirm,
        initialDate = persianDate
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// SimpleDatePicker.js
// ══════════════════════════════════════════════════════════════════════════════

private const val ITEM_HEIGHT = 50 // dp
private val PURPLE = Color(0xFF8B5CF6)

/** `getDaysInMonth` of SimpleDatePicker (note: uses its own simplified leap rule). */
private fun getDaysInMonth(month: Int, year: Int): Int {
    if (month <= 6) return 31
    if (month <= 11) return 30
    val isLeap = ((year - 1) % 33) % 4 == 0
    return if (isLeap) 30 else 29
}

/**
 * `<SimpleDatePicker isVisible onClose onConfirm initialDate />` — bottom sheet with day / month / year wheels.
 * [initialDate] = `[jy, jm, jd]` (only read once, exactly like the RN `useState` initialisers).
 */
@Composable
fun SimpleDatePicker(
    isVisible: Boolean,
    onClose: () -> Unit,
    onConfirm: (List<Int>) -> Unit,
    initialDate: List<Int>?
) {
    val today = rnTodayPersian()

    var selectedYear by remember { mutableStateOf(initialDate?.get(0) ?: today[0]) }
    var selectedMonth by remember { mutableStateOf(initialDate?.get(1) ?: today[1]) }
    var selectedDay by remember { mutableStateOf(initialDate?.get(2) ?: today[2]) }

    var tempSelectedYear by remember { mutableStateOf(selectedYear) }
    var tempSelectedMonth by remember { mutableStateOf(selectedMonth) }
    var tempSelectedDay by remember { mutableStateOf(selectedDay) }

    // useEffect(() => { if (isVisible) { setTemp*(selected*) ... } }, [isVisible])
    LaunchedEffect(isVisible) {
        if (isVisible) {
            tempSelectedYear = selectedYear
            tempSelectedMonth = selectedMonth
            tempSelectedDay = selectedDay
        }
    }

    // useEffect(() => { clamp day }, [tempSelectedMonth, tempSelectedYear])
    LaunchedEffect(tempSelectedMonth, tempSelectedYear) {
        val maxDays = getDaysInMonth(tempSelectedMonth, tempSelectedYear)
        if (tempSelectedDay > maxDays) tempSelectedDay = maxDays
    }

    if (!isVisible) return

    val (_, windowHeight) = windowSize()
    val currentYear = today[0]
    val years = List(21) { currentYear - 10 + it }
    val days = List(getDaysInMonth(tempSelectedMonth, tempSelectedYear)) { it + 1 }

    val handleYearChange: (Int) -> Unit = { year ->
        tempSelectedYear = year
        val maxDays = getDaysInMonth(tempSelectedMonth, year)
        if (tempSelectedDay > maxDays) tempSelectedDay = maxDays
    }
    val handleMonthChange: (Int) -> Unit = { month ->
        tempSelectedMonth = month
        val maxDays = getDaysInMonth(month, tempSelectedYear)
        if (tempSelectedDay > maxDays) tempSelectedDay = maxDays
    }
    val handleConfirm: () -> Unit = {
        selectedYear = tempSelectedYear
        selectedMonth = tempSelectedMonth
        selectedDay = tempSelectedDay
        onConfirm(listOf(tempSelectedYear, tempSelectedMonth, tempSelectedDay))
        onClose()
    }

    val safeAreaBottom: Dp = if (isIOS && windowHeight.value > 736f) 34.dp else 0.dp

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val density = LocalDensity.current
        val itemPx = with(density) { ITEM_HEIGHT.dp.toPx() }

        // Animated.parallel([ spring(slide → 0, tension 65, friction 11), timing(opacity → 1, 300) ])
        val slideAnim = remember { Animatable(windowHeight.value) }
        val opacityAnim = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            coroutineScope {
                launch { slideAnim.animateTo(0f, rnSpring(65f, 11f)) }
                launch { opacityAnim.animateTo(1f, rnTiming(300)) }
            }
        }

        val dayScroll = rememberScrollState()
        val monthScroll = rememberScrollState()
        val yearScroll = rememberScrollState()
        var isLayoutReady by remember { mutableStateOf(false) }

        // performInitialScroll (re-created on every state change in RN → effect re-runs after 100ms)
        LaunchedEffect(isLayoutReady, tempSelectedYear, tempSelectedMonth, tempSelectedDay) {
            if (isLayoutReady) {
                delay(100)
                coroutineScope {
                    val yearIndex = years.indexOf(tempSelectedYear)
                    if (yearIndex != -1) launch { yearScroll.animateScrollTo((yearIndex * itemPx).roundToInt()) }
                    val monthIndex = tempSelectedMonth - 1
                    launch { monthScroll.animateScrollTo((monthIndex * itemPx).roundToInt()) }
                    val dayIndex = tempSelectedDay - 1
                    launch { dayScroll.animateScrollTo((dayIndex * itemPx).roundToInt()) }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0f, 0f, 0f, 0.6f))
                .pressable { onClose() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = slideAnim.value.dp.toPx()
                        alpha = opacityAnim.value
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = windowHeight * 0.75f)
                        .background(Color.White, RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp))
                        .pressable { /* e.stopPropagation() */ }
                ) {
                    // Header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pickerBorderBottom(Color(0xFFF0F0F0))
                            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier
                                .absolutePadding(bottom = 15.dp)
                                .width(40.dp)
                                .height(4.dp)
                                .background(Color(0xFFD1D5DB), RoundedCornerShape(2.dp))
                        )
                        RowReverse(
                            modifier = Modifier.fillMaxWidth().absolutePadding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RowReverse(verticalAlignment = Alignment.CenterVertically) {
                                MaterialIcons(name = "calendar-today", size = 24.dp, color = PURPLE)
                                AppText(
                                    text = "انتخاب تاریخ",
                                    modifier = Modifier.absolutePadding(right = 8.dp),
                                    fontSize = 20.sp,
                                    color = Color(0xFF1F2937),
                                    fontFamily = RNFonts.Yekan_Bakh_ExtraBold
                                )
                            }
                        }
                        AppText(
                            text = "امروز: ${toFarsiDigits(today[2])} ${persianMonths[today[1] - 1]} ${toFarsiDigits(today[0])}",
                            fontSize = 14.sp,
                            color = Color(0xFF6B7280),
                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Date Selectors
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp)
                    ) {
                        RowReverse(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .onGloballyPositioned { if (!isLayoutReady) isLayoutReady = true },
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // روز
                            DatePickerColumn(
                                label = "روز",
                                count = days.size,
                                scrollState = dayScroll,
                                itemPx = itemPx,
                                isSelected = { tempSelectedDay == days[it] },
                                itemText = { toFarsiDigits(days[it]) },
                                onItemPress = { tempSelectedDay = days[it] },
                                onMomentumEnd = { index ->
                                    val day = index + 1
                                    if (day != tempSelectedDay && day <= days.size) tempSelectedDay = day
                                },
                                modifier = Modifier.weight(1f)
                            )
                            // ماه
                            DatePickerColumn(
                                label = "ماه",
                                count = 12,
                                scrollState = monthScroll,
                                itemPx = itemPx,
                                isSelected = { tempSelectedMonth == it + 1 },
                                itemText = { persianMonths[it] },
                                onItemPress = { handleMonthChange(it + 1) },
                                onMomentumEnd = { index ->
                                    val month = index + 1
                                    if (month != tempSelectedMonth) handleMonthChange(month)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            // سال
                            DatePickerColumn(
                                label = "سال",
                                count = years.size,
                                scrollState = yearScroll,
                                itemPx = itemPx,
                                isSelected = { tempSelectedYear == years[it] },
                                itemText = { toFarsiDigits(years[it]) },
                                onItemPress = { handleYearChange(years[it]) },
                                onMomentumEnd = { index ->
                                    val year = years.getOrNull(index)
                                    if (year != null && year != tempSelectedYear) handleYearChange(year)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Action Buttons
                    RowReverse(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .pickerBorderTop(Color(0xFFF0F0F0))
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RowReverse(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .touchableOpacity(activeOpacity = 0.8f) { onClose() }
                                .padding(vertical = 14.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MaterialIcons(name = "close", size = 20.dp, color = Color(0xFF6B7280))
                            AppText(
                                text = "انصراف",
                                modifier = Modifier.absolutePadding(right = 6.dp),
                                fontSize = 16.sp,
                                color = Color(0xFF6B7280),
                                fontFamily = RNFonts.Yekan_Bakh_Bold
                            )
                        }
                        Box(
                            Modifier
                                .weight(2f)
                                .touchableOpacity(activeOpacity = 0.8f) { handleConfirm() }
                        ) {
                            LinearGradient(
                                colors = listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
                                modifier = Modifier.fillMaxWidth(),
                                start = Offset(0f, 0f),
                                end = Offset(1f, 1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                RowReverse(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MaterialIcons(name = "check", size = 20.dp, color = Color(0xFFFFFFFF))
                                    AppText(
                                        text = "تأیید انتخاب",
                                        modifier = Modifier.absolutePadding(right = 6.dp),
                                        fontSize = 16.sp,
                                        color = Color(0xFFFFFFFF),
                                        fontFamily = RNFonts.Yekan_Bakh_Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.fillMaxWidth().height(safeAreaBottom).background(Color.White))
                }
            }
        }
    }
}

/**
 * One `pickerColumn` (label + snapping ScrollView). Items are 50dp high, the content has 75dp padding top/bottom,
 * `snapToInterval = 50`; `onMomentumScrollEnd` → `onMomentumEnd(round(offset / 50))`.
 */
@Composable
private fun DatePickerColumn(
    label: String,
    count: Int,
    scrollState: ScrollState,
    itemPx: Float,
    isSelected: (Int) -> Boolean,
    itemText: (Int) -> String,
    onItemPress: (Int) -> Unit,
    onMomentumEnd: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnMomentumEnd by rememberUpdatedState(onMomentumEnd)
    val currentCount by rememberUpdatedState(count)

    // snapToInterval + onMomentumScrollEnd
    LaunchedEffect(scrollState) {
        var wasScrolling = false
        snapshotFlow { scrollState.isScrollInProgress }.collect { scrolling ->
            if (scrolling) {
                wasScrolling = true
            } else if (wasScrolling) {
                wasScrolling = false
                val target = (((scrollState.value / itemPx).roundToInt()) * itemPx).roundToInt()
                    .coerceIn(0, scrollState.maxValue)
                if (target != scrollState.value) scrollState.animateScrollTo(target)
                val index = (scrollState.value / itemPx).roundToInt().coerceAtMost(currentCount - 1)
                currentOnMomentumEnd(index)
            }
        }
    }

    Column(
        modifier = modifier.absolutePadding(left = 5.dp, right = 5.dp), // pickerColumn.marginHorizontal: 5
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppText(
            text = label,
            modifier = Modifier.absolutePadding(bottom = 10.dp),
            fontSize = 14.sp,
            color = PURPLE,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            textAlign = TextAlign.Center
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            Spacer(Modifier.height(75.dp))
            for (i in 0 until count) {
                val selected = isSelected(i)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ITEM_HEIGHT.dp)
                        .then(
                            if (selected) Modifier.background(Color(139, 92, 246, (0.1f * 255).roundToInt()), RoundedCornerShape(12.dp))
                            else Modifier
                        )
                        .touchableOpacity(activeOpacity = 0.7f) { onItemPress(i) }
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AppText(
                        text = itemText(i),
                        fontSize = if (selected) 16.sp else 15.sp,
                        color = if (selected) PURPLE else Color(0xFF9CA3AF),
                        fontFamily = if (selected) RNFonts.Yekan_Bakh_ExtraBold else RNFonts.Yekan_Bakh_Regular,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(75.dp))
        }
    }
}
