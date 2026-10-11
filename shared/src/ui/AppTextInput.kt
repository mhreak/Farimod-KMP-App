package ir.farimod.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.utils.PersianNumberUtils

/** 1:1 char mapping English → Persian digits for display only (the stored value keeps English digits). */
private object PersianDigitsTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val converted = PersianNumberUtils.toPersianDigits(text.text)
        return TransformedText(
            AnnotatedString(converted, text.spanStyles, text.paragraphStyles),
            OffsetMapping.Identity
        )
    }
}

/**
 * Port of `components/TextInput.tsx`.
 *
 * [value] / [onChangeText] use ENGLISH digits; the field renders Persian digits (typed Persian digits are
 * converted back to English before [onChangeText]). [modifier] is the RN `containerStyle` of the outer view
 * (default width 100% + marginBottom 16, see [marginBottom]); [inputContainerModifier] is `inputContainerStyle`.
 */
@Composable
fun AppTextInput(
    value: String,
    onChangeText: (String) -> Unit,
    modifier: Modifier = Modifier,
    icon: String? = null,
    label: String? = null,
    placeholder: String = "",
    isLargeInput: Boolean = false,
    height: Dp? = null,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    secureTextEntry: Boolean = false,
    maxLength: Int? = null,
    editable: Boolean = true,
    onSubmitEditing: (() -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Default,
    inputContainerModifier: Modifier = Modifier,
    textStyleOverride: ((TextStyle) -> TextStyle)? = null,
    labelColor: Color? = null,
    onFocus: (() -> Unit)? = null,
    onBlur: (() -> Unit)? = null,
    autoFocus: Boolean = false,
    // ── extras (not in the fixed signature) ──
    marginBottom: Dp = 16.dp,
    inputId: String = "",
    onChangeInput: ((String, String) -> Unit)? = null
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var hasFocus by remember { mutableStateOf(false) }

    // The stored text is always English digits (RN: lastValueRef / toEnglishDigits).
    var fieldValue by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    if (fieldValue.text != value) {
        fieldValue = TextFieldValue(value, TextRange(value.length))
    }

    LaunchedEffect(Unit) {
        if (autoFocus) runCatching { focusRequester.requestFocus() }
    }

    val doSubmit: () -> Unit = {
        onSubmitEditing?.invoke()
        // RN default `blurOnSubmit` for single-line inputs
        if (!isLargeInput) {
            keyboardController?.hide()
            focusManager.clearFocus()
        }
    }

    val shape = RoundedCornerShape(16.dp)
    val textStyle = TextStyle(
        fontFamily = RNFonts.Yekan_Bakh_Regular,
        fontSize = 15.sp,
        color = FarimodColors.Dark,
        textAlign = TextAlign.Right,
        textDirection = TextDirection.Rtl
    ).let { base -> textStyleOverride?.invoke(base) ?: base }

    val displayPlaceholder = if (placeholder.isNotEmpty()) PersianNumberUtils.toPersianDigits(placeholder) else ""

    Column(modifier = modifier.fillMaxWidth().padding(bottom = marginBottom)) {
        if (label != null && label.isNotEmpty()) {
            AppText(
                text = label,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                fontSize = 15.sp,
                color = labelColor ?: FarimodColors.Dark,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                textAlign = TextAlign.Right
            )
        }

        RowReverse(
            modifier = Modifier
                .fillMaxWidth()
                .background(FarimodColors.White, shape)
                .border(1.dp, FarimodColors.Gray, shape)
                .then(inputContainerModifier)
                .then(if (isLargeInput && height != null) Modifier.height(height) else Modifier)
                .pressable { runCatching { focusRequester.requestFocus() } },
            verticalAlignment = if (isLargeInput) Alignment.Top else Alignment.CenterVertically
        ) {
            if (icon != null && icon.isNotEmpty()) {
                Box(
                    modifier = Modifier.absolutePadding(right = 10.dp, top = if (isLargeInput) 12.dp else 0.dp)
                ) {
                    MaterialIcons(name = icon, size = 20.dp, color = FarimodColors.Medium)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(if (height != null && !isLargeInput) Modifier.height(height) else Modifier),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = fieldValue,
                    onValueChange = { new ->
                        var english = PersianNumberUtils.toEnglishDigits(new.text)
                        if (maxLength != null && english.length > maxLength) english = english.take(maxLength)
                        val next = if (english == new.text) new else TextFieldValue(
                            english,
                            TextRange(
                                new.selection.start.coerceAtMost(english.length),
                                new.selection.end.coerceAtMost(english.length)
                            )
                        )
                        val changed = english != fieldValue.text
                        fieldValue = next
                        if (changed) {
                            if (onChangeInput != null && inputId.isNotEmpty()) onChangeInput(inputId, english)
                            onChangeText(english)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(15.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            if (it.isFocused) {
                                if (!hasFocus) {
                                    hasFocus = true
                                    onFocus?.invoke()
                                }
                            } else if (hasFocus) {
                                hasFocus = false
                                onBlur?.invoke()
                            }
                        },
                    readOnly = !editable,
                    textStyle = textStyle,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        keyboardType = keyboardType,
                        imeAction = imeAction
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { doSubmit() },
                        onGo = { doSubmit() },
                        onNext = { doSubmit() },
                        onSearch = { doSubmit() },
                        onSend = { doSubmit() }
                    ),
                    singleLine = !isLargeInput,
                    visualTransformation = if (secureTextEntry) PasswordVisualTransformation() else PersianDigitsTransformation,
                    cursorBrush = SolidColor(FarimodColors.Dark),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.TopStart) {
                            if (fieldValue.text.isEmpty() && displayPlaceholder.isNotEmpty()) {
                                Text(
                                    text = displayPlaceholder,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = textStyle.copy(color = FarimodColors.DarkGray),
                                    maxLines = if (isLargeInput) Int.MAX_VALUE else 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
        }

        if (!error.isNullOrEmpty()) {
            AppText(
                text = error,
                modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                fontSize = 12.sp,
                color = FarimodColors.Danger,
                fontFamily = RNFonts.Yekan_Bakh_Regular,
                textAlign = TextAlign.Right
            )
        }
    }
}

/** Port of `components/forms/ErrorMessage.js`. */
@Composable
fun ErrorMessage(error: String?, visible: Boolean, modifier: Modifier = Modifier) {
    if (!visible || error.isNullOrEmpty()) return
    AppText(text = error, modifier = modifier, color = Color.Red)
}
