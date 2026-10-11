package ir.farimod.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.farimod.app.platform.isIOS
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.utils.PersianNumberUtils
import kotlin.math.floor
import kotlin.math.max

private data class OTPDimensions(val inputSize: Int, val gap: Int, val fontSize: Int, val containerPadding: Int)

private fun getResponsiveDimensions(screenWidth: Float, length: Int): OTPDimensions {
    val containerPadding = 20
    val safeMargin = 30

    val availableWidth = screenWidth - (2 * containerPadding) - safeMargin

    var baseInputSize: Int
    var gap: Int

    if (screenWidth < 350) {
        baseInputSize = 40
        gap = 3
    } else if (screenWidth < 400) {
        baseInputSize = 45
        gap = 4
    } else if (screenWidth < 450) {
        baseInputSize = 50
        gap = 5
    } else {
        baseInputSize = 55
        gap = 6
    }

    val totalGapWidth = (length - 1) * gap
    val totalInputWidth = length * baseInputSize
    val totalNeededWidth = totalInputWidth + totalGapWidth

    if (totalNeededWidth > availableWidth) {
        baseInputSize = floor((availableWidth - totalGapWidth) / length).toInt()
        if (baseInputSize < 30) {
            gap = max(1, floor((availableWidth - (length * 30)) / (length - 1)).toInt())
            baseInputSize = floor((availableWidth - ((length - 1) * gap)) / length).toInt()
        }
    }

    return OTPDimensions(
        inputSize = max(baseInputSize, 30),
        gap = max(gap, 1),
        fontSize = floor(baseInputSize * 0.35f).toInt(),
        containerPadding = containerPadding
    )
}

private fun toEnglishDigits(str: String): String = PersianNumberUtils.toEnglishDigits(str)
private fun toPersianDigits(str: String): String = PersianNumberUtils.toPersianDigits(str)

/** JS `code.split('')` padded with '' up to [length] (JS `while (newCode.length < length) newCode.push('')`). */
private fun codeToList(code: String, length: Int, padTo: Boolean): MutableList<String> {
    val list = code.map { it.toString() }.toMutableList()
    if (padTo) while (list.size < length) list.add("")
    return list
}

/**
 * Port of `components/OTPInput.tsx` (digits are displayed Persian, [code] / [onCodeChange] use English digits).
 * The first cell is styled as focused initially (RN `useState(0)`), without actually taking focus.
 */
@Composable
fun OTPInput(
    onCodeChange: (String) -> Unit,
    code: String,
    length: Int = 5,
    modifier: Modifier = Modifier
) {
    var focusedIndex by remember { mutableStateOf(0) }
    val requesters = remember(length) { List(length) { FocusRequester() } }

    val screenWidth = windowSize().first.value
    val dims = getResponsiveDimensions(screenWidth, length)

    fun handleTextChange(text: String, index: Int) {
        val englishText = toEnglishDigits(text)
        val sanitizedText = englishText.filter { it in '0'..'9' }

        if (sanitizedText.length <= 1) {
            val newCode = codeToList(code, length, padTo = true)
            newCode[index] = sanitizedText
            val updatedCode = newCode.joinToString("")
            onCodeChange(updatedCode)

            if (sanitizedText.isNotEmpty() && index < length - 1) {
                runCatching { requesters[index + 1].requestFocus() }
                focusedIndex = index + 1
            }
        }
    }

    fun handleBackspace(index: Int): Boolean {
        val hasChar = code.getOrNull(index) != null
        if (!hasChar && index > 0) {
            val newCode = codeToList(code, length, padTo = false)
            while (newCode.size <= index - 1) newCode.add("")
            newCode[index - 1] = ""
            onCodeChange(newCode.joinToString(""))
            runCatching { requesters[index - 1].requestFocus() }
            focusedIndex = index - 1
            return true
        } else {
            val newCode = codeToList(code, length, padTo = false)
            while (newCode.size <= index) newCode.add("")
            newCode[index] = ""
            onCodeChange(newCode.joinToString(""))
            return false
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dims.containerPadding.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (index in 0 until length) {
                key(index) {
                    OTPCell(
                        index = index,
                        length = length,
                        codeChar = code.getOrNull(index)?.toString(),
                        focused = focusedIndex == index,
                        inputSize = dims.inputSize.dp,
                        gap = dims.gap.dp,
                        fontSizeValue = dims.fontSize.toFloat(),
                        requester = requesters[index],
                        onTextChange = { text -> handleTextChange(text, index) },
                        onBackspace = { handleBackspace(index) },
                        onFocused = { focusedIndex = index },
                        onBlurred = { if (focusedIndex == index) focusedIndex = -1 }
                    )
                }
            }
        }
    }
}

@Composable
private fun OTPCell(
    index: Int,
    length: Int,
    codeChar: String?,
    focused: Boolean,
    inputSize: Dp,
    gap: Dp,
    fontSizeValue: Float,
    requester: FocusRequester,
    onTextChange: (String) -> Unit,
    onBackspace: () -> Boolean,
    onFocused: () -> Unit,
    onBlurred: () -> Unit
) {
    val filled = codeChar != null
    val expectedText = if (codeChar != null) toPersianDigits(codeChar) else ""

    var fieldValue by remember { mutableStateOf(TextFieldValue(expectedText, TextRange(expectedText.length))) }
    if (fieldValue.text != expectedText) {
        fieldValue = TextFieldValue(expectedText, TextRange(expectedText.length))
    }

    val shape = RoundedCornerShape(15.dp)

    // styles.input -> + focusedInput -> + filledInput (later wins)
    var borderColor = rgba(255, 206, 232, 0.5f)
    var borderWidth = 2.dp
    var background = rgba(255, 255, 255, 0.9f)
    var elevation = 3.dp
    var shadowOpacity = 0.4f
    if (focused) {
        borderColor = FarimodColors.Primary
        borderWidth = 3.dp
        background = rgba(255, 255, 255, 1f)
        elevation = 5.dp
        shadowOpacity = 0.6f
    }
    if (filled) {
        background = rgba(255, 206, 232, 0.25f)
        borderColor = FarimodColors.Primary
        borderWidth = 2.dp
    }
    val shadowColor = rgba(255, 206, 232, if (isIOS) shadowOpacity else 0.4f)

    val fontSize = fontSizeValue.sp
    val lineHeight = (if (!isIOS) fontSizeValue * 1.2f else fontSizeValue).sp
    val textStyle = TextStyle(
        fontFamily = RNFonts.Yekan_Bakh_Bold,
        fontSize = fontSize,
        lineHeight = lineHeight,
        color = FarimodColors.Primary,
        textAlign = TextAlign.Center,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both)
    )

    val a11y = "فیلد کد تأیید ${toPersianDigits((index + 1).toString())}"

    Box(
        modifier = Modifier
            .absolutePadding(right = if (index < length - 1) gap else 0.dp)
            .graphicsLayer {
                val s = if (focused) 1.05f else 1f
                scaleX = s
                scaleY = s
            }
            .size(inputSize)
            .rnShadow(elevation, shape, shadowColor)
            .background(background, shape)
            .border(borderWidth, borderColor, shape),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = fieldValue,
            onValueChange = { new ->
                // maxLength = 1 (native length filter)
                var raw = new.text
                if (raw.length > 1) {
                    if (fieldValue.text.isEmpty() || !fieldValue.selection.collapsed) raw = raw.take(1)
                    else return@BasicTextField
                }
                onTextChange(raw)
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = a11y }
                .focusRequester(requester)
                .onFocusChanged {
                    if (it.isFocused) {
                        // selectTextOnFocus
                        fieldValue = fieldValue.copy(selection = TextRange(0, fieldValue.text.length))
                        onFocused()
                    } else {
                        onBlurred()
                    }
                }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Backspace) onBackspace()
                    else false
                },
            textStyle = textStyle,
            singleLine = true,
            cursorBrush = SolidColor(FarimodColors.Primary),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Number,
                imeAction = if (index == length - 1) ImeAction.Done else ImeAction.Next
            ),
            // blurOnSubmit={false}: submitting neither blurs nor moves focus
            keyboardActions = KeyboardActions(onDone = {}, onNext = {})
        )
    }
}
