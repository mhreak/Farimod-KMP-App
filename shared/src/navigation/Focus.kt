package ir.farimod.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.CoroutineScope

/** True while the screen is the visible one of its navigator (and every parent navigator / tab is focused too). */
val LocalIsFocused = compositionLocalOf { true }

/** RN `useIsFocused()` */
@Composable
fun useIsFocused(): Boolean = LocalIsFocused.current

/**
 * RN `useFocusEffect(useCallback(() => { ... }, deps))`:
 * runs [effect] every time the screen gains focus (and when [keys] change while focused).
 * The coroutine is cancelled when the screen loses focus.
 */
@Composable
fun useFocusEffect(vararg keys: Any?, effect: suspend CoroutineScope.() -> Unit) {
    val focused = LocalIsFocused.current
    LaunchedEffect(focused, *keys) {
        if (focused) effect()
    }
}
