package ir.farimod.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import ir.farimod.app.navigation.LocalNavigation
import ir.farimod.app.navigation.MainTabs
import ir.farimod.app.navigation.NavigationState
import ir.farimod.app.navigation.RenderRoute
import ir.farimod.app.navigation.Route
import ir.farimod.app.navigation.StackHost
import ir.farimod.app.platform.PlatformBackHandler
import ir.farimod.app.state.AuthState
import ir.farimod.app.theme.FarimodTheme
import ir.farimod.app.ui.AlertHost
import ir.farimod.app.ui.AppUpdateHost

/**
 * App.tsx → AuthProvider + IntroProvider + AppUpdateProvider + NavigationContainer + StackNavigator.
 *  - logged in  : root stack = MainTabs (+ every app screen)
 *  - logged out : root stack = IntroFlow (WelcomeIntro ...) then Auth (Login / OTP / Signup)
 */
@Composable
fun App() {
    FarimodTheme(isRtl = true) {
        // AuthContext: check auth status once on start (storage is synchronous so there is no loading flash)
        remember { AuthState.checkAuthStatus(); true }

        val nav = remember {
            NavigationState(
                if (AuthState.isAuthenticated) listOf(Route("MainTabs")) else listOf(Route("WelcomeIntro"))
            )
        }

        // StackNavigator: conditional groups — swap the whole root stack when auth state changes.
        var lastAuth = remember { booleanArrayOf(AuthState.isAuthenticated) }
        LaunchedEffect(AuthState.isAuthenticated) {
            if (AuthState.isAuthenticated != lastAuth[0]) {
                lastAuth[0] = AuthState.isAuthenticated
                val target = if (AuthState.isAuthenticated) "MainTabs" else "WelcomeIntro"
                // Login already calls reset("MainTabs"). A second reset creates a new route key,
                // cancels the stack animation, and leaves every card off-screen (white page).
                val already = nav.root.size == 1 && nav.root.first().name == target
                if (!already) nav.resetRoot(listOf(Route(target)))
            }
        }

        PlatformBackHandler(enabled = true) { nav.handleBack() }

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            CompositionLocalProvider(LocalNavigation provides nav.rootNavigation) {
                StackHost(
                    stack = nav.root,
                    onBack = { nav.rootNavigation.goBack() }
                ) { route ->
                    if (route.name == "MainTabs") MainTabs(nav) else RenderRoute(route)
                }
            }
            AppUpdateHost()
            AlertHost()
        }
    }
}
