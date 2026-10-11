package ir.farimod.app.screens.mag

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import ir.farimod.app.ui.RNTimingEasing
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.rnShadow

/** Port of `screens/mag/ui/SkeletonLoader.tsx` — opacity 0.3 → 0.7 over 1500ms, inOut ease, loop. */
@Composable
internal fun MagSkeletonBar(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(0.dp),
    color: Color = Color(0xFFE1E5E9)
) {
    val transition = rememberInfiniteTransition()
    val opacity by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = RNTimingEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    Box(
        modifier
            .clip(shape)
            .background(color.copy(alpha = color.alpha * opacity))
    )
}

/** Port of `screens/mag/ui/BlogPostCardSkeleton.tsx`. */
@Composable
internal fun MagBlogPostCardSkeleton() {
    val cardShape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .rnShadow(1.dp, cardShape)
            .clip(cardShape)
            .background(Color.White)
    ) {
        MagSkeletonBar(
            Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            MagSkeletonBar(
                Modifier
                    .fillMaxWidth(0.9f)
                    .absolutePadding(bottom = 12.dp)
                    .height(18.dp),
                shape = RoundedCornerShape(4.dp)
            )
            MagSkeletonBar(
                Modifier
                    .fillMaxWidth(0.7f)
                    .absolutePadding(bottom = 8.dp)
                    .height(16.dp),
                shape = RoundedCornerShape(4.dp)
            )
            RowReverse(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RowReverse(verticalAlignment = Alignment.CenterVertically) {
                    MagSkeletonBar(
                        Modifier
                            .size(16.dp)
                            .absolutePadding(left = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                    MagSkeletonBar(
                        Modifier
                            .width(80.dp)
                            .height(14.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MagSkeletonBar(
                        Modifier
                            .size(16.dp)
                            .absolutePadding(right = 6.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                    MagSkeletonBar(
                        Modifier
                            .width(30.dp)
                            .height(14.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                }
            }
        }
    }
}
