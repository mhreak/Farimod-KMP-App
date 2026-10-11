package ir.farimod.app.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import kotlin.math.sqrt

/**
 * `Animated.timing` default easing in React Native = `Easing.inOut(Easing.ease)`
 * where `Easing.ease = bezier(0.42, 0, 1, 1)`.
 */
internal val RNTimingEasing: Easing = run {
    val ease = CubicBezierEasing(0.42f, 0f, 1f, 1f)
    Easing { t ->
        if (t < 0.5f) ease.transform(t * 2f) / 2f
        else 1f - ease.transform((1f - t) * 2f) / 2f
    }
}

/** `Animated.timing(value, { toValue, duration })` */
internal fun <T> rnTiming(durationMillis: Int): FiniteAnimationSpec<T> =
    tween(durationMillis = durationMillis, easing = RNTimingEasing)

/**
 * `Animated.spring(value, { tension, friction })` (Origami conversion used by React Native):
 * stiffness = (tension - 30) * 3.62 + 194,  damping = (friction - 8) * 3 + 25, mass = 1.
 */
internal fun rnSpring(tension: Float, friction: Float): FiniteAnimationSpec<Float> {
    val stiffness = (tension - 30f) * 3.62f + 194f
    val damping = (friction - 8f) * 3f + 25f
    val ratio = damping / (2f * sqrt(stiffness))
    return spring(dampingRatio = ratio, stiffness = stiffness)
}
