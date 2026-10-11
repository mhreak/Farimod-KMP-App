package ir.farimod.app.platform

import androidx.compose.runtime.Composable

/** True when running on iOS (React Native: `Platform.OS === "ios"`). */
expect val isIOS: Boolean

/** React Native: `Platform.OS === "android"`. */
val isAndroid: Boolean get() = !isIOS

/** React Native: `Linking.openURL(url)`. Returns false when the URL could not be opened. */
expect fun openUrl(url: String): Boolean

/** React Native: `Linking.canOpenURL(url)`. */
expect fun canOpenUrl(url: String): Boolean

/** React Native: `BackHandler.addEventListener("hardwareBackPress", ...)`. No-op on iOS. */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)
