package ir.farimod.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Port of `components/Screen.tsx`:
 * `<SafeAreaView style={[screen, style]}><View style={[view, style]}>{children}</View></SafeAreaView>`
 * screen = { paddingTop: Constants.statusBarHeight, flex: 1, width: "100%" }, view = { flex: 1 }.
 * (like RN, [modifier] is applied to both the outer and the inner view).
 */
@Composable
fun Screen(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = statusBarHeight())
            .then(modifier)
    ) {
        Column(modifier = Modifier.fillMaxSize().then(modifier)) {
            content()
        }
    }
}
