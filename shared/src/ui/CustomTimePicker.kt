package ir.farimod.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.utils.PersianNumberUtils
import ir.farimod.app.utils.jsNow
import ir.farimod.app.utils.localHourMinuteOf
import ir.farimod.app.utils.toPersianDigits
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ──────────────────────────────────────────────────────────────────────────────
// Port of app/components/CustomTimePicker.tsx
// ──────────────────────────────────────────────────────────────────────────────

private fun pad2Persian(n: Int): String = n.toString().padStart(2, '0').toPersianDigits()

/** JS `parseInt(str, 10)` (leading integer, null = NaN). */
private fun jsParseInt(s: String?): Int? {
    if (s == null) return null
    val t = s.trimStart()
    var i = 0
    var sign = 1
    if (i < t.length && (t[i] == '-' || t[i] == '+')) {
        if (t[i] == '-') sign = -1
        i++
    }
    val start = i
    while (i < t.length && t[i] in '0'..'9') i++
    if (i == start) return null
    val n = t.substring(start, i).toIntOrNull() ?: return null
    return sign * n
}

private fun parseTime(timeString: String?): Pair<Int, Int> {
    if (!timeString.isNullOrEmpty() && timeString.contains(':')) {
        val englishTime = PersianNumberUtils.toEnglishDigits(timeString)
        val parts = englishTime.split(':')
        val hour = jsParseInt(parts.getOrNull(0)) ?: 0
        val minute = jsParseInt(parts.getOrNull(1)) ?: 0
        return hour to minute
    }
    return localHourMinuteOf(jsNow())
}

/**
 * `<CustomTimePicker value onTimeChange placeholder icon style />`
 * [value] / [onTimeChange] are strings like "۰۸:۳۰" (Persian digits, exactly like RN). [modifier] = RN `style`.
 */
@Composable
fun CustomTimePicker(
    value: String?,
    onTimeChange: (String) -> Unit,
    placeholder: String,
    icon: String?,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val (_, screenHeight) = windowSize()

    var showPicker by remember { mutableStateOf(false) }
    val slideAnim = remember { Animatable(screenHeight.value) }
    val opacityAnim = remember { Animatable(0f) }

    val hours = remember { List(24) { it } }
    val minutes = remember { List(60) { it } }

    val initialTime = remember { parseTime(value) }
    var selectedHour by remember { mutableStateOf(initialTime.first) }
    var selectedMinute by remember { mutableStateOf(initialTime.second) }

    val showDrawer: () -> Unit = {
        val wasShowing = showPicker
        showPicker = true
        scope.launch {
            if (!wasShowing) {
                // RN: slideAnim starts at screenHeight / opacityAnim at 0 (reset by the previous hide animation)
                slideAnim.snapTo(screenHeight.value)
                opacityAnim.snapTo(0f)
            }
            coroutineScope {
                launch { slideAnim.animateTo(0f, rnTiming(300)) }
                launch { opacityAnim.animateTo(1f, rnTiming(300)) }
            }
        }
    }

    val hideDrawer: () -> Unit = {
        scope.launch {
            coroutineScope {
                launch { slideAnim.animateTo(screenHeight.value, rnTiming(250)) }
                launch { opacityAnim.animateTo(0f, rnTiming(250)) }
            }
            showPicker = false
        }
    }

    val handleConfirm: () -> Unit = {
        val timeString = "${pad2Persian(selectedHour)}:${pad2Persian(selectedMinute)}"
        onTimeChange(timeString)
        hideDrawer()
    }

    val handleCancel: () -> Unit = {
        val originalTime = parseTime(value)
        selectedHour = originalTime.first
        selectedMinute = originalTime.second
        hideDrawer()
    }

    val handlePress: () -> Unit = {
        val currentTime = parseTime(value)
        selectedHour = currentTime.first
        selectedMinute = currentTime.second
        showDrawer()
    }

    Column(modifier = modifier) {
        Box(modifier = Modifier.touchableOpacity(activeOpacity = 0.7f) { handlePress() }) {
            // <View pointerEvents="none"> <AppTextInput editable={false} .../> </View>
            AppTextInput(
                value = value ?: "",
                onChangeText = {},
                icon = icon,
                placeholder = placeholder,
                editable = false,
                textStyleOverride = {
                    it.copy(color = if (!value.isNullOrEmpty()) FarimodColors.Dark else FarimodColors.Medium)
                }
            )
            // swallow pointer events for the text field (they bubble to the touchable wrapper above)
            Box(Modifier.matchParentSize().pointerInput(Unit) {})
        }
    }

    if (showPicker) {
        Dialog(
            onDismissRequest = handleCancel,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val drawerShape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)
            Box(
                modifier = Modifier.fillMaxSize(), // modalContainer
                contentAlignment = Alignment.BottomCenter
            ) {
                // backdrop
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = opacityAnim.value }
                        .background(Color(0f, 0f, 0f, 0.5f))
                        .pressable { handleCancel() }
                )

                // drawer
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { translationY = slideAnim.value.dp.toPx() }
                        .rnShadow(10.dp, drawerShape)
                        .clip(drawerShape)
                        .heightIn(max = screenHeight * 0.65f)
                        .pressable { }
                ) {
                    LinearGradient(
                        colors = listOf(rgba(255, 255, 255, 0.95f), rgba(255, 255, 255, 1f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, end = 20.dp, bottom = 30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // handle
                            Box(
                                Modifier
                                    .absolutePadding(top = 10.dp, bottom = 15.dp)
                                    .width(40.dp)
                                    .height(4.dp)
                                    .background(FarimodColors.Medium.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                            )

                            // drawerHeader
                            Column(
                                modifier = Modifier.fillMaxWidth().absolutePadding(bottom = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                RowReverse(
                                    modifier = Modifier.absolutePadding(bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MaterialIcons(name = "access-time", size = 24.dp, color = FarimodColors.Primary)
                                    AppText(
                                        text = "انتخاب زمان",
                                        modifier = Modifier.absolutePadding(right = 8.dp),
                                        fontSize = 18.sp,
                                        color = FarimodColors.Primary,
                                        fontFamily = RNFonts.Yekan_Bakh_Bold
                                    )
                                }
                                AppText(
                                    text = "${pad2Persian(selectedHour)}:${pad2Persian(selectedMinute)}",
                                    fontSize = 24.sp,
                                    color = FarimodColors.Primary,
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    letterSpacing = 2.sp
                                )
                            }

                            // timePickerContainer
                            Box(Modifier.fillMaxWidth().absolutePadding(bottom = 30.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(200.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        AppText(
                                            text = "ساعت",
                                            modifier = Modifier.absolutePadding(bottom = 10.dp),
                                            fontSize = 14.sp,
                                            color = FarimodColors.Primary,
                                            fontFamily = RNFonts.Yekan_Bakh_Bold
                                        )
                                        TimeWheel(
                                            data = hours,
                                            selectedValue = selectedHour,
                                            onValueChange = { selectedHour = it },
                                            itemHeight = 50.dp
                                        )
                                    }

                                    Box(
                                        modifier = Modifier.width(50.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AppText(
                                            text = ":",
                                            fontSize = 28.sp,
                                            color = FarimodColors.Primary,
                                            fontFamily = RNFonts.Yekan_Bakh_Bold
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        AppText(
                                            text = "دقیقه",
                                            modifier = Modifier.absolutePadding(bottom = 10.dp),
                                            fontSize = 14.sp,
                                            color = FarimodColors.Primary,
                                            fontFamily = RNFonts.Yekan_Bakh_Bold
                                        )
                                        TimeWheel(
                                            data = minutes,
                                            selectedValue = selectedMinute,
                                            onValueChange = { selectedMinute = it },
                                            itemHeight = 50.dp
                                        )
                                    }
                                }
                            }

                            // buttonContainer
                            RowReverse(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pickerBorderTop(Color(0xFFF0F0F0))
                                    .padding(horizontal = 20.dp, vertical = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RowReverse(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                        .touchableOpacity(activeOpacity = 0.8f) { handleCancel() }
                                        .padding(vertical = 14.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MaterialIcons(name = "close", size = 20.dp, color = FarimodColors.Medium)
                                    AppText(
                                        text = "لغو",
                                        modifier = Modifier.absolutePadding(right = 6.dp),
                                        fontSize = 16.sp,
                                        color = FarimodColors.Medium,
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
                                                text = "تأیید",
                                                modifier = Modifier.absolutePadding(right = 6.dp),
                                                fontSize = 16.sp,
                                                color = Color(0xFFFFFFFF),
                                                fontFamily = RNFonts.Yekan_Bakh_Bold
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
    }
}

/**
 * `TimeWheel`: a 150dp high snapping list (item 50dp, content padding 2 × itemHeight top/bottom).
 * The centre row (y = 50..100) holds the selected item: `centerIndex = round(scrollY / itemHeight) - 1`.
 */
@Composable
private fun TimeWheel(
    data: List<Int>,
    selectedValue: Int,
    onValueChange: (Int) -> Unit,
    itemHeight: Dp = 50.dp
) {
    val density = LocalDensity.current
    val itemPx = with(density) { itemHeight.toPx() }

    val initialIndex = data.indexOf(selectedValue).let { if (it >= 0) it else 0 }
    // initial scroll so that the selected item sits in the centre row (scrollToIndex + viewPosition 0.5)
    val scrollState = rememberScrollState(initial = ((initialIndex + 1) * itemPx).roundToInt())
    val currentOnValueChange by rememberUpdatedState(onValueChange)

    LaunchedEffect(scrollState) {
        // onScroll
        launch {
            snapshotFlow { scrollState.value }.collect { scrollY ->
                val centerIndex = (scrollY / itemPx).roundToInt() - 1
                if (centerIndex >= 0 && centerIndex < data.size) currentOnValueChange(data[centerIndex])
            }
        }
        // snapToInterval + onMomentumScrollEnd
        var wasScrolling = false
        snapshotFlow { scrollState.isScrollInProgress }.collect { scrolling ->
            if (scrolling) {
                wasScrolling = true
            } else if (wasScrolling) {
                wasScrolling = false
                val target = (((scrollState.value / itemPx).roundToInt()) * itemPx).roundToInt()
                    .coerceIn(0, scrollState.maxValue)
                if (target != scrollState.value) scrollState.animateScrollTo(target)
                val centerIndex = (scrollState.value / itemPx).roundToInt() - 1
                if (centerIndex >= 0 && centerIndex < data.size) currentOnValueChange(data[centerIndex])
            }
        }
    }

    Box(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .height(150.dp)
            .clipToBounds()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Spacer(Modifier.height(itemHeight * 2))
            for (item in data) {
                val isSelected = item == selectedValue
                Box(
                    modifier = Modifier.fillMaxWidth().height(itemHeight),
                    contentAlignment = Alignment.Center
                ) {
                    AppText(
                        text = pad2Persian(item),
                        modifier = Modifier.alpha(if (isSelected) 1f else 0.6f),
                        fontSize = if (isSelected) 24.sp else 18.sp,
                        color = if (isSelected) FarimodColors.Primary else FarimodColors.Medium,
                        fontFamily = if (isSelected) RNFonts.Yekan_Bakh_Bold else RNFonts.Yekan_Bakh_Regular,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(itemHeight * 2))
        }

        // gradientTop
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(50.dp)
                .linearGradient(listOf(rgba(255, 255, 255, 0.9f), rgba(255, 255, 255, 0f)))
        )

        // centerSelection
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .absolutePadding(top = 50.dp)
                .height(50.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .pickerBorderBottom(FarimodColors.Primary, 2.dp)
            )
        }

        // gradientBottom
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(50.dp)
                .linearGradient(listOf(rgba(255, 255, 255, 0f), rgba(255, 255, 255, 0.9f)))
        )
    }
}
