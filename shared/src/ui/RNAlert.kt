package ir.farimod.app.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import ir.farimod.app.theme.FarimodColors

/** RN `Alert.alert` button: `{ text, style: "default" | "cancel" | "destructive", onPress }` */
data class AlertButton(
    val text: String,
    val style: String = "default",
    val onPress: (() -> Unit)? = null
)

private class AlertRequest(
    val title: String,
    val message: String?,
    val buttons: List<AlertButton>,
    val cancelable: Boolean
)

/**
 * React Native `Alert.alert(title, message, buttons, { cancelable })`.
 * `AlertHost()` is mounted once in App.kt.
 *   RNAlert.alert("خطا", "پیام", listOf(AlertButton("باشه")))
 */
object RNAlert {
    internal var current by mutableStateOf<Any?>(null)

    fun alert(
        title: String,
        message: String? = null,
        buttons: List<AlertButton> = listOf(AlertButton("OK")),
        cancelable: Boolean = true
    ) {
        current = AlertRequest(title, message, buttons.ifEmpty { listOf(AlertButton("OK")) }, cancelable)
    }
}

@Composable
fun AlertHost() {
    val request = RNAlert.current as? AlertRequest ?: return
    fun dismiss() { RNAlert.current = null }
    AlertDialog(
        onDismissRequest = {
            if (request.cancelable) {
                dismiss()
                request.buttons.firstOrNull { it.style == "cancel" }?.onPress?.invoke()
            }
        },
        containerColor = Color.White,
        title = {
            AppText(request.title, fontSize = 18.sp, fontFamily = RNFonts.Yekan_Bakh_Bold, textAlign = TextAlign.Right)
        },
        text = request.message?.let { msg ->
            { AppText(msg, fontSize = 15.sp, fontFamily = RNFonts.Yekan_Bakh_Regular, color = FarimodColors.Medium, textAlign = TextAlign.Right) }
        },
        confirmButton = {
            androidx.compose.foundation.layout.Row {
                for (b in request.buttons.reversed()) {
                    TextButton(onClick = { dismiss(); b.onPress?.invoke() }) {
                        Text(
                            text = b.text,
                            color = if (b.style == "destructive") FarimodColors.Danger else FarimodColors.Primary,
                            fontFamily = RNFonts.Yekan_Bakh_Bold
                        )
                    }
                }
            }
        }
    )
}
