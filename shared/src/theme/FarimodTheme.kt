package ir.farimod.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val FarimodColorScheme = lightColorScheme(
    primary = FarimodColors.Primary,
    onPrimary = FarimodColors.White,
    primaryContainer = FarimodColors.PrimaryLight,
    onPrimaryContainer = FarimodColors.PrimaryDark,
    secondary = FarimodColors.Secondary,
    onSecondary = FarimodColors.White,
    background = FarimodColors.Light,
    onBackground = FarimodColors.Dark,
    surface = FarimodColors.White,
    onSurface = FarimodColors.Dark,
    error = FarimodColors.Danger,
    onError = FarimodColors.White
)

@Composable
fun FarimodTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isRtl: Boolean = true,
    content: @Composable () -> Unit
) {
    val typography = getFarimodTypography()
    val layoutDirection = if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection
    ) {
        MaterialTheme(
            colorScheme = FarimodColorScheme,
            typography = typography,
            shapes = FarimodShapes,
            content = content
        )
    }
}
