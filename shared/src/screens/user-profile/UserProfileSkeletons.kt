package ir.farimod.app.screens.userprofile

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.farimod.app.ui.RowReverse
import kotlin.random.Random

@Composable
internal fun UpSkeletonPulse(content: @Composable () -> Unit) {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 800, easing = UpTimingEasing),
            RepeatMode.Reverse
        ),
        label = "upSkeleton"
    )
    CompositionLocalProvider(LocalUpSkeletonAlpha provides alpha, content = content)
}

@Composable
internal fun UpSkeleton(modifier: Modifier = Modifier, radius: Dp = 8.dp) {
    val alpha = LocalUpSkeletonAlpha.current
    Box(
        modifier
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(radius))
            .background(Color(0xFFE0E0E0))
    )
}

@Composable
internal fun UpProfileSkeleton() {
    Column(
        Modifier
            .upChild(marginTop = UpConst.PROFILE_TOP.dp, marginBottom = 40.dp, zIndex = 2f)
            .fillMaxWidth()
            .heightIn(min = 300.dp)
            .padding(25.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        UpSkeleton(
            Modifier.padding(bottom = 25.dp).size(UpConst.AVATAR_OUTER.dp),
            radius = (UpConst.AVATAR_OUTER / 2).dp
        )
        UpSkeleton(Modifier.padding(bottom = 8.dp).width(180.dp).height(24.dp), radius = 12.dp)
        UpSkeleton(Modifier.padding(bottom = 15.dp).width(120.dp).height(16.dp), radius = 20.dp)
        UpSkeleton(Modifier.width(80.dp).height(35.dp), radius = 20.dp)
    }
}

@Composable
internal fun UpInfoSectionSkeleton() {
    Column(
        Modifier
            .upChild(marginBottom = 20.dp, zIndex = 1f)
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFFF8FAFC).copy(alpha = 0.5f))
            .border(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.4f), RoundedCornerShape(22.dp))
            .padding(20.dp)
    ) {
        RowReverse(Modifier.fillMaxWidth().padding(bottom = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            UpSkeleton(Modifier.size(44.dp).absolutePadding(left = 12.dp), radius = 22.dp)
            UpSkeleton(Modifier.width(100.dp).height(18.dp), radius = 9.dp)
        }
        UpSkeleton(Modifier.padding(bottom = 8.dp).fillMaxWidth().height(16.dp))
        UpSkeleton(Modifier.padding(bottom = 8.dp).fillMaxWidth(0.9f).height(16.dp))
        UpSkeleton(Modifier.fillMaxWidth(0.7f).height(16.dp))
    }
}

@Composable
internal fun UpContactSkeleton() {
    Column(
        Modifier
            .upChild(marginBottom = 20.dp, zIndex = 1f)
            .fillMaxWidth()
            .heightIn(min = 80.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFFF8FAFC).copy(alpha = 0.5f))
            .border(1.dp, Color(0xFFCBD5E1).copy(alpha = 0.4f), RoundedCornerShape(22.dp))
            .padding(20.dp)
    ) {
        RowReverse(Modifier.fillMaxWidth().padding(bottom = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            UpSkeleton(Modifier.size(44.dp).absolutePadding(left = 12.dp), radius = 22.dp)
            UpSkeleton(Modifier.width(120.dp).height(18.dp), radius = 9.dp)
        }
        Column {
            repeat(3) {
                RowReverse(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .heightIn(min = 70.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.8f))
                        .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                        .absolutePadding(left = 18.dp, right = 18.dp, top = 20.dp, bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UpSkeleton(Modifier.size(44.dp).absolutePadding(left = 15.dp), radius = 22.dp)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                        UpSkeleton(Modifier.width(60.dp).height(12.dp).padding(bottom = 4.dp))
                        UpSkeleton(Modifier.width(120.dp).height(15.dp))
                    }
                    UpSkeleton(Modifier.size(20.dp).absolutePadding(right = 10.dp), radius = 10.dp)
                }
            }
        }
    }
}

@Composable
internal fun UpQuickAccessSkeleton() {
    val count = remember { Random.nextInt(2, 5) }
    RowReverse(
        Modifier
            .upChild(marginTop = 15.dp, marginBottom = 30.dp, zIndex = 1f)
            .fillMaxWidth()
            .absolutePadding(top = 15.dp, bottom = 15.dp)
            .absolutePadding(left = 5.dp, right = 5.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) {
            Column(
                Modifier
                    .weight(1f)
                    .absolutePadding(left = 4.dp, right = 4.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFFE0E0E0).copy(alpha = 0.15f))
                    .border(1.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                UpSkeleton(Modifier.size(24.dp).padding(bottom = 6.dp), radius = 12.dp)
                UpSkeleton(Modifier.width(60.dp).height(11.dp), radius = 6.dp)
            }
        }
    }
}

@Composable
internal fun UpSectionSkeleton(cardType: String) {
    Column(Modifier.upChild(marginBottom = 25.dp).fillMaxWidth()) {
        RowReverse(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .padding(bottom = 20.dp, top = 10.dp)
                .absolutePadding(left = 5.dp, right = 5.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RowReverse(verticalAlignment = Alignment.CenterVertically) {
                UpSkeleton(Modifier.size(20.dp).absolutePadding(right = 8.dp), radius = 10.dp)
                UpSkeleton(Modifier.width(100.dp).height(18.dp), radius = 9.dp)
            }
            UpSkeleton(Modifier.width(80.dp).height(14.dp), radius = 7.dp)
        }
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .absolutePadding(left = 5.dp, right = 5.dp)
        ) {
            repeat(3) {
                Box(Modifier.absolutePadding(right = 15.dp)) {
                    when (cardType) {
                        "course" -> UpCourseCardSkeleton()
                        "product" -> UpProductCardSkeleton()
                        "gallery" -> UpGalleryCardSkeleton()
                        "blog" -> UpBlogCardSkeleton()
                        else -> UpPortfolioCardSkeleton()
                    }
                }
            }
        }
    }
}

@Composable
private fun UpPortfolioCardSkeleton() {
    Box(
        Modifier
            .width(280.dp)
            .height(200.dp)
            .padding(bottom = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
    ) {
        UpSkeleton(Modifier.fillMaxSize(), radius = 0.dp)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)).padding(15.dp), contentAlignment = Alignment.BottomStart) {
            Column {
                UpSkeleton(Modifier.fillMaxWidth(0.8f).height(16.dp).padding(bottom = 5.dp))
                UpSkeleton(Modifier.fillMaxWidth(0.6f).height(13.dp))
            }
        }
    }
}

@Composable
private fun UpCourseCardSkeleton() {
    Column(
        Modifier
            .width(300.dp)
            .padding(bottom = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
    ) {
        UpSkeleton(Modifier.fillMaxWidth().height(140.dp), radius = 0.dp)
        Column(Modifier.padding(15.dp)) {
            UpSkeleton(Modifier.fillMaxWidth(0.9f).height(16.dp).padding(bottom = 10.dp))
            RowReverse(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                RowReverse(verticalAlignment = Alignment.CenterVertically) {
                    UpSkeleton(Modifier.size(16.dp).absolutePadding(right = 5.dp), radius = 8.dp)
                    UpSkeleton(Modifier.width(80.dp).height(14.dp))
                }
                RowReverse(verticalAlignment = Alignment.CenterVertically) {
                    UpSkeleton(Modifier.size(14.dp).absolutePadding(right = 5.dp), radius = 7.dp)
                    UpSkeleton(Modifier.width(60.dp).height(12.dp))
                }
            }
            UpSkeleton(Modifier.width(80.dp).height(24.dp), radius = 12.dp)
        }
    }
}

@Composable
private fun UpProductCardSkeleton() {
    Column(
        Modifier
            .width(180.dp)
            .padding(bottom = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
    ) {
        Box(Modifier.fillMaxWidth().height(180.dp).background(Color(0xFFF5F5F5))) {
            UpSkeleton(Modifier.fillMaxSize(), radius = 0.dp)
        }
        Column(Modifier.padding(12.dp).heightIn(min = 85.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
            UpSkeleton(Modifier.fillMaxWidth(0.8f).height(16.dp).align(Alignment.CenterHorizontally).padding(bottom = 8.dp))
            UpSkeleton(Modifier.fillMaxWidth(0.6f).height(14.dp).align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun UpGalleryCardSkeleton() {
    Box(
        Modifier
            .size(120.dp)
            .padding(bottom = 20.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White)
    ) {
        UpSkeleton(Modifier.fillMaxSize(), radius = 0.dp)
        RowReverse(
            Modifier.align(AbsoluteAlignment.TopRight).absolutePadding(top = 8.dp, right = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UpSkeleton(Modifier.size(16.dp).absolutePadding(right = 4.dp), radius = 8.dp)
            UpSkeleton(Modifier.width(20.dp).height(11.dp))
        }
    }
}

@Composable
private fun UpBlogCardSkeleton() {
    Column(
        Modifier
            .width(280.dp)
            .padding(bottom = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
    ) {
        Box(Modifier.fillMaxWidth().height(160.dp)) {
            UpSkeleton(Modifier.fillMaxSize(), radius = 0.dp)
        }
        Column(Modifier.padding(16.dp)) {
            UpSkeleton(Modifier.fillMaxWidth(0.9f).height(16.dp).padding(bottom = 12.dp))
            RowReverse(
                Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RowReverse(verticalAlignment = Alignment.CenterVertically) {
                    UpSkeleton(Modifier.size(16.dp).absolutePadding(left = 6.dp), radius = 8.dp)
                    UpSkeleton(Modifier.width(80.dp).height(14.dp))
                }
                RowReverse(verticalAlignment = Alignment.CenterVertically) {
                    UpSkeleton(Modifier.size(16.dp).absolutePadding(right = 6.dp), radius = 8.dp)
                    UpSkeleton(Modifier.width(30.dp).height(14.dp))
                }
            }
        }
    }
}
