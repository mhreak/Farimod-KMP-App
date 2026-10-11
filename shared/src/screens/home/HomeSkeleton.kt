package ir.farimod.app.screens.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.rnShadow
import kotlinx.coroutines.delay

@Composable
internal fun HomeSkeleton(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp
) {
    val pulse = remember { Animatable(0.4f) }
    val shimmer = remember { Animatable(-300f) }
    LaunchedEffect(Unit) {
        while (true) {
            pulse.animateTo(1f, tween(1200))
            pulse.animateTo(0.4f, tween(1200))
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            shimmer.snapTo(-300f)
            shimmer.animateTo(300f, tween(1500))
            delay(300)
        }
    }
    Box(
        modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xFFE0E0E0))
    ) {
        Box(Modifier.matchParentSize().background(Color(0xFFF5F5F5)).alpha(pulse.value))
        Box(
            Modifier
                .fillMaxHeight()
                .width(100.dp)
                .offset(x = shimmer.value.dp)
                .background(Color.White.copy(alpha = 0.3f))
        )
    }
}

@Composable
internal fun HomeHeaderSliderSkeleton() {
    Box(
        Modifier
            .padding(bottom = 20.dp)
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(20.dp))
    ) {
        HomeSkeleton(Modifier.fillMaxSize(), cornerRadius = 20.dp)
    }
}

@Composable
internal fun HomeAvatarSkeleton(size: Dp = 100.dp) {
    Column(
        modifier = Modifier.absolutePadding(left = 10.dp, right = 10.dp, top = 8.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HomeSkeleton(Modifier.padding(bottom = 8.dp).size(size), cornerRadius = size / 2)
        HomeSkeleton(Modifier.size(width = 80.dp, height = 12.dp))
    }
}

@Composable
internal fun HomeMemberGroupCardSkeleton() {
    Box(Modifier.absolutePadding(left = 8.dp, right = 8.dp, top = 10.dp, bottom = 10.dp)) {
        Box(
            Modifier
                .size(100.dp)
                .rnShadow(10.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
        ) {
            HomeSkeleton(Modifier.fillMaxSize(), cornerRadius = 20.dp)
        }
    }
}

@Composable
internal fun HomeProductCardSkeleton() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .rnShadow(2.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectOne()
                .background(Color(0xFFF5F5F5))
        ) {
            HomeSkeleton(Modifier.fillMaxSize(), cornerRadius = 0.dp)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 85.dp)
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HomeSkeleton(
                Modifier.fillMaxWidth(0.8f).height(16.dp).padding(bottom = 8.dp),
                cornerRadius = 8.dp
            )
            HomeSkeleton(Modifier.fillMaxWidth(0.6f).height(14.dp))
        }
    }
}

@Composable
internal fun HomeBlogCardSkeleton() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .rnShadow(1.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
    ) {
        Box(Modifier.fillMaxWidth().height(200.dp)) {
            HomeSkeleton(Modifier.fillMaxSize(), cornerRadius = 0.dp)
        }
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            HomeSkeleton(
                Modifier.fillMaxWidth(0.9f).height(18.dp).align(Alignment.Start).padding(bottom = 12.dp)
            )
            HomeSkeleton(
                Modifier.fillMaxWidth(0.7f).height(16.dp).align(Alignment.Start).padding(bottom = 8.dp)
            )
            RowReverse(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RowReverse(verticalAlignment = Alignment.CenterVertically) {
                    HomeSkeleton(
                        Modifier.absolutePadding(left = 6.dp).size(16.dp),
                        cornerRadius = 8.dp
                    )
                    HomeSkeleton(Modifier.size(width = 80.dp, height = 14.dp))
                }
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    HomeSkeleton(
                        Modifier.absolutePadding(right = 6.dp).size(16.dp),
                        cornerRadius = 8.dp
                    )
                    HomeSkeleton(Modifier.size(width = 30.dp, height = 14.dp))
                }
            }
        }
    }
}

@Composable
internal fun HomeGalleryCardSkeleton() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .rnShadow(8.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectOne()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF5F5F5))
        ) {
            HomeSkeleton(Modifier.fillMaxSize(), cornerRadius = 16.dp)
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(15.dp)
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HomeSkeleton(
                    Modifier.fillMaxWidth(0.8f).height(18.dp).padding(bottom = 8.dp),
                    cornerRadius = 9.dp
                )
                HomeSkeleton(Modifier.fillMaxWidth(0.6f).height(16.dp), cornerRadius = 8.dp)
            }
        }
    }
}

@Composable
internal fun HomePortfolioCardSkeleton() {
    Column(
        Modifier
            .padding(bottom = 10.dp)
            .fillMaxWidth()
            .height(270.dp)
            .rnShadow(3.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
    ) {
        Box(Modifier.fillMaxWidth().height(140.dp)) {
            HomeSkeleton(Modifier.fillMaxSize(), cornerRadius = 0.dp)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                HomeSkeleton(
                    Modifier.fillMaxWidth(0.9f).height(16.dp).align(Alignment.Start).padding(bottom = 8.dp)
                )
                HomeSkeleton(
                    Modifier.fillMaxWidth(0.7f).height(14.dp).align(Alignment.Start).padding(bottom = 8.dp)
                )
            }
            RowReverse(
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RowReverse(verticalAlignment = Alignment.CenterVertically) {
                    HomeSkeleton(
                        Modifier.absolutePadding(left = 4.dp).size(14.dp),
                        cornerRadius = 7.dp
                    )
                    HomeSkeleton(Modifier.size(width = 60.dp, height = 12.dp))
                }
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    HomeSkeleton(
                        Modifier.absolutePadding(right = 4.dp).size(14.dp),
                        cornerRadius = 7.dp
                    )
                    HomeSkeleton(Modifier.size(width = 25.dp, height = 12.dp))
                }
            }
        }
    }
}

@Composable
internal fun HomeCourseCardSkeleton() {
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 390.dp)
            .padding(bottom = 8.dp)
            .rnShadow(4.dp, RoundedCornerShape(16.dp), Color(0xFF797979))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
    ) {
        Box(Modifier.fillMaxWidth().height(250.dp).padding(bottom = 8.dp)) {
            HomeSkeleton(Modifier.fillMaxSize(), cornerRadius = 0.dp)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .absolutePadding(left = 12.dp, right = 12.dp)
                .padding(top = 12.dp, bottom = 10.dp)
        ) {
            RowReverse(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                verticalAlignment = Alignment.Top
            ) {
                HomeSkeleton(
                    Modifier.absolutePadding(left = 12.dp).size(44.dp),
                    cornerRadius = 12.dp
                )
                Column(Modifier.weight(1f)) {
                    HomeSkeleton(Modifier.fillMaxWidth(0.9f).height(16.dp).align(Alignment.Start).padding(bottom = 8.dp))
                    HomeSkeleton(Modifier.fillMaxWidth(0.7f).height(16.dp).align(Alignment.Start).padding(bottom = 8.dp))
                    HomeSkeleton(Modifier.fillMaxWidth(0.4f).height(14.dp).align(Alignment.Start).padding(bottom = 6.dp))
                    HomeSkeleton(Modifier.fillMaxWidth(0.5f).height(12.dp).align(Alignment.Start))
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp)
                    .homeTopBorder()
            ) {
                RowReverse(
                    modifier = Modifier.padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HomeSkeleton(
                        Modifier.absolutePadding(left = 8.dp).size(20.dp),
                        cornerRadius = 10.dp
                    )
                    HomeSkeleton(Modifier.size(width = 80.dp, height = 13.dp))
                }
            }
        }
    }
}

private fun Modifier.aspectOne(): Modifier = layout { measurable, constraints ->
    val side = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.maxHeight
    val placeable = measurable.measure(androidx.compose.ui.unit.Constraints.fixed(side, side))
    layout(side, side) { placeable.place(0, 0) }
}

private fun Modifier.homeTopBorder(): Modifier = drawBehind {
    drawRect(Color(0xFFF0F0F0), size = Size(size.width, 1.dp.toPx()))
}
