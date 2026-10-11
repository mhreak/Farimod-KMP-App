package ir.farimod.app.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import ir.farimod.app.platform.isIOS
import kotlinx.coroutines.launch

/** React Navigation DefaultTheme background (card behind every screen). */
private val CardBackground = Color(0xFFF2F2F2)

private class StackEntry(val route: Route, val progress: Animatable<Float, androidx.compose.animation.core.AnimationVector1D>)

/**
 * Renders a stack of routes like `@react-navigation/stack` with the app's custom transition:
 * 180 ms timing, the new card slides in from the right (translateX = screenWidth → 0), going back reverses it.
 * Screens below the top stay composed (state is preserved, exactly as in React Navigation).
 */
@Composable
fun StackHost(
    stack: List<Route>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    gestureEnabled: (Route) -> Boolean = { it.name !in GestureDisabledRoutes },
    content: @Composable (Route) -> Unit
) {
    val displayed = remember {
        mutableStateListOf<StackEntry>().apply { stack.forEach { add(StackEntry(it, Animatable(1f))) } }
    }

    val keys = stack.map { it.key }
    // Full replacement (login / logout) must paint the new screen now. Waiting for the
    // animation coroutine loses to the second reset and every card stays off-screen.
    SideEffect {
        if (stack.isEmpty()) return@SideEffect
        val desired = stack.map { it.key }
        val shown = displayed.map { it.route.key }
        val noOverlap = shown.none { it in desired }
        if (shown != desired && (shown.isEmpty() || noOverlap)) {
            displayed.clear()
            stack.forEach { displayed.add(StackEntry(it, Animatable(1f))) }
        }
    }
    LaunchedEffect(keys) {
        val incoming = stack.filter { route -> displayed.none { it.route.key == route.key } }
        val outgoing = displayed.filter { entry -> stack.none { it.key == entry.route.key } }
        val replacingStack = incoming.isNotEmpty() && outgoing.isNotEmpty() && outgoing.size == displayed.size
        if (replacingStack) {
            displayed.clear()
            incoming.forEach { displayed.add(StackEntry(it, Animatable(1f))) }
            return@LaunchedEffect
        }
        for (route in incoming) {
            val first = displayed.isEmpty()
            val entry = StackEntry(route, Animatable(if (first) 1f else 0f))
            displayed.add(entry)
            if (!first) launch { entry.progress.animateTo(1f, tween(180)) }
        }
        for (entry in outgoing) {
            launch {
                entry.progress.animateTo(0f, tween(180))
                displayed.remove(entry)
            }
        }
    }

    val density = LocalDensity.current
    val edgePx = with(density) { 28.dp.toPx() }
    val triggerPx = with(density) { 90.dp.toPx() }
    val top = stack.lastOrNull()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val swipeEnabled = isIOS && stack.size > 1 && top != null && gestureEnabled(top)

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .then(
                if (swipeEnabled) Modifier.pointerInput(top?.key, rtl) {
                    var startedAtEdge = false
                    var total = 0f
                    val width = size.width
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            val fromBackEdge = if (rtl) offset.x >= width - edgePx else offset.x <= edgePx
                            startedAtEdge = fromBackEdge
                            total = 0f
                        },
                        onDragEnd = {
                            val passed = if (rtl) total < -triggerPx else total > triggerPx
                            if (startedAtEdge && passed) onBack()
                        },
                        onDragCancel = { startedAtEdge = false },
                        onHorizontalDrag = { _, delta -> if (startedAtEdge) total += delta }
                    )
                } else Modifier
            )
    ) {
        displayed.forEachIndexed { index, entry ->
            key(entry.route.key) {
                val covered = displayed.drop(index + 1).any { it.progress.value >= 1f }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (covered) Modifier.layout { measurable, constraints ->
                                val p = measurable.measure(constraints)
                                layout(p.width, p.height) { /* not placed: hidden below an opaque card */ }
                            } else Modifier
                        )
                        .graphicsLayer {
                            val sign = if (rtl) -1f else 1f
                            translationX = sign * (1f - entry.progress.value) * size.width
                        }
                        .background(CardBackground)
                ) {
                    val parentFocused = LocalIsFocused.current
                    CompositionLocalProvider(
                        LocalRoute provides entry.route,
                        LocalIsFocused provides (parentFocused && entry.route.key == top?.key)
                    ) {
                        content(entry.route)
                    }
                }
            }
        }
    }
}
