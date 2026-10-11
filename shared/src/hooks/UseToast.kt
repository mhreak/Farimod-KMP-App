package ir.farimod.app.hooks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Port of `app/hooks/useToast.tsx` → `{ toastVisible, setToastVisible, toastMessage, toastType, showToast }`.
 *
 *   val toast = useToast()
 *   toast.showToast("پیام", "success")
 *   Toast(toast.toastVisible, toast.toastMessage, toast.toastType) { toast.setToastVisible(false) }
 */
class ToastState {
    private var visible by mutableStateOf(false)
    private var message by mutableStateOf("")
    private var type by mutableStateOf("error")

    val toastVisible: Boolean get() = visible
    val toastMessage: String get() = message
    /** "success" | "error" | "warning" | "info" (RN default: "error"). */
    val toastType: String get() = type

    fun setToastVisible(value: Boolean) {
        visible = value
    }

    fun showToast(message: String, type: String = "error") {
        this.message = message
        this.type = type
        this.visible = true
    }
}

@Composable
fun useToast(): ToastState = remember { ToastState() }
