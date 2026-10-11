package ir.farimod.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import ir.farimod.app.state.AppUpdateState
import ir.farimod.app.state.AuthState
import kotlinx.coroutines.launch

/**
 * Port of `AppUpdateProvider` + `<AppUpdateModal ... />` (App.tsx / contexts/AppUpdateContext.js).
 * Rendered once at the root of the app, on top of everything.
 */
@Composable
fun AppUpdateHost() {
    AppUpdateEffects()

    AppUpdateModal(
        visible = AppUpdateState.showUpdateModal,
        updateInfo = AppUpdateState.updateInfo,
        onDirectDownload = { AppUpdateState.handleDirectDownload() },
        onCafeBazarDownload = { AppUpdateState.handleCafeBazarDownload() },
        onLater = { AppUpdateState.handleLater() },
        onClose = { AppUpdateState.handleClose() }
    )
}

/**
 * The consolidated `useEffect(..., [isAuthenticated, user?.MemberId, isHomeScreen])` of AppUpdateProvider:
 *  - no-op unless `isAuthenticated && user?.MemberId && isHomeScreen`
 *  - startup check (when due), 30 minute interval, and a check when the app comes back to the foreground.
 */
@Composable
private fun AppUpdateEffects() {
    val isAuthenticated = AuthState.isAuthenticated
    val memberId = AuthState.user?.MemberId
    val isHomeScreen = AppUpdateState.isHomeScreen

    if (!isAuthenticated || memberId == null || memberId == 0 || !isHomeScreen) return

    LaunchedEffect(isAuthenticated, memberId, isHomeScreen) {
        AppUpdateState.runScheduledChecks()
    }

    // AppState.addEventListener("change", s => s === "active" && checkForUpdateOnAppActivation())
    val scope = rememberCoroutineScope()
    var wasInBackground by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        wasInBackground = true
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (wasInBackground) {
            wasInBackground = false
            scope.launch { AppUpdateState.checkForUpdateOnAppActivation() }
        }
    }
}
