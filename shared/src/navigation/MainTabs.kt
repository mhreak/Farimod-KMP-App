package ir.farimod.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.farimod.app.components.icons.Ionicons
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LocalBottomInsetConsumed
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.pressable
import ir.farimod.app.ui.rememberSafeAreaInsets
import ir.farimod.app.screens.HomeScreen
import ir.farimod.app.screens.MagScreen
import ir.farimod.app.screens.ProfileScreen

/**
 * `TabNavigator` of AppNavigator.tsx: tabs پروفایل | خانه | وبلاگ (left → right), lazy mounted, history back behaviour.
 */
@Composable
fun MainTabs(nav: NavigationState) {
    val visited = remember { mutableStateListOf(Tabs.Home) }
    if (nav.currentTab !in visited) visited.add(nav.currentTab)

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            CompositionLocalProvider(LocalBottomInsetConsumed provides true) {
                for (tab in Tabs.all) {
                    if (tab !in visited) continue
                    val selected = tab == nav.currentTab
                    val parentFocused = LocalIsFocused.current
                    CompositionLocalProvider(LocalIsFocused provides (parentFocused && selected)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (selected) Modifier else Modifier.layout { m, c ->
                                    val p = m.measure(c)
                                    layout(p.width, p.height) { }
                                }
                            )
                    ) {
                        when (tab) {
                            Tabs.Profile -> CompositionLocalProvider(
                                LocalNavigation provides nav.rootNavigation,
                                LocalRoute provides remember { Route(Tabs.Profile) }
                            ) { ProfileScreen() }

                            Tabs.Mag -> CompositionLocalProvider(
                                LocalNavigation provides nav.rootNavigation,
                                LocalRoute provides remember { Route(Tabs.Mag) }
                            ) { MagScreen() }

                            Tabs.Home -> CompositionLocalProvider(LocalNavigation provides nav.homeNavigation) {
                                StackHost(
                                    stack = nav.homeStack,
                                    onBack = { nav.homeNavigation.goBack() }
                                ) { route -> RenderRoute(route) }
                            }
                        }
                    }
                    }
                }
            }
        }
        TabBar(selected = nav.currentTab, onSelect = { nav.selectTab(it) })
    }
}

@Composable
private fun TabBar(selected: String, onSelect: (String) -> Unit) {
    val insets = rememberSafeAreaInsets()
    val separator = Color(0xFFD8D8D8) // React Navigation DefaultTheme border
    Column(modifier = Modifier.fillMaxWidth().background(Color.White)) {
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(separator))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp + insets.bottom)
                .padding(bottom = insets.bottom),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (tab in Tabs.all) {
                val focused = tab == selected
                val tint = if (focused) FarimodColors.Primary else Color(0xFF808080) // "gray"
                val icon = when (tab) {
                    Tabs.Home -> if (focused) "home" else "home-outline"
                    Tabs.Profile -> if (focused) "person" else "person-outline"
                    else -> if (focused) "book" else "book-outline"
                }
                Column(
                    modifier = Modifier.weight(1f).fillMaxSize().pressable { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Ionicons(icon, size = 25.dp, color = tint)
                    AppText(
                        text = tab,
                        fontSize = 13.sp,
                        color = tint,
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }
            }
        }
    }
}
