package ir.farimod.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Port of `app/components/forms/ErrorMessage.js` : `<Text style={{ color: "red" }}>` (AppText defaults) shown only
 * when `visible && error`.
 */
@Composable
private fun FormErrorMessage(error: String?, visible: Boolean, modifier: Modifier = Modifier) {
    if (!visible || error.isNullOrEmpty()) return
    AppText(error, modifier = modifier, color = Color.Red)
}

/**
 * Port of `app/components/forms/FormImagePicker.js` (Formik `useFormikContext` replaced by explicit state):
 *  - `values[name]`            → [imageUris]
 *  - `setFieldValue(name, v)`  → [onChange]
 *  - `touched[name]` / `errors[name]` → [touched] / [error]
 */
@Composable
fun FormImagePicker(
    imageUris: List<String>,
    onChange: (List<String>) -> Unit,
    error: String? = null,
    touched: Boolean = false
) {
    val handleAdd: (String?) -> Unit = { uri ->
        if (uri != null) onChange(imageUris + uri)
    }

    val handleRemove: (String?) -> Unit = { uri ->
        onChange(imageUris.filter { imageUri -> imageUri != uri })
    }

    ImageInputList(
        imageUris = imageUris,
        onAddImage = handleAdd,
        onRemoveImage = handleRemove
    )
    FormErrorMessage(error = error, visible = touched)
}
