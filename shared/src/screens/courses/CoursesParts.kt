package ir.farimod.app.screens.courses

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.rnTiming
import ir.farimod.app.ui.touchableOpacity
import kotlinx.coroutines.launch

internal val CoursePrimary = Color(0xFF667EEA)
internal val CoursePrimaryDark = Color(0xFF764BA2)

@Composable
internal fun CoursePulse(modifier: Modifier, radius: Dp = 8.dp) {
    val transition = rememberInfiniteTransition(label = "courseSkeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "courseSkeletonAlpha"
    )
    Box(
        modifier
            .clip(RoundedCornerShape(radius))
            .background(Color(0xFFE0E0E0))
            .alpha(pulse)
    )
}

@Composable
internal fun CourseCardSkeleton() {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp, max = 350.dp)
            .rnShadow(4.dp, shape, Color(0xFF797979))
            .clip(shape)
            .background(FarimodColors.White)
    ) {
        CoursePulse(Modifier.fillMaxWidth().height(180.dp), 0.dp)
        Column(Modifier.padding(12.dp)) {
            RowReverse(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.Top) {
                CoursePulse(Modifier.absolutePadding(left = 8.dp).size(32.dp), 8.dp)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    CoursePulse(Modifier.padding(bottom = 4.dp).fillMaxWidth(0.9f).height(13.dp))
                    CoursePulse(Modifier.padding(bottom = 4.dp).fillMaxWidth(0.7f).height(13.dp))
                    CoursePulse(Modifier.fillMaxWidth(0.4f).height(12.dp))
                }
            }
            Column(Modifier.fillMaxWidth().padding(top = 5.dp)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFF0F0F0)))
                RowReverse(
                    Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CoursePulse(Modifier.absolutePadding(left = 4.dp).size(14.dp), 7.dp)
                    CoursePulse(Modifier.width(60.dp).height(11.dp))
                }
                CoursePulse(Modifier.padding(bottom = 2.dp).align(Alignment.Start).fillMaxWidth(0.85f).height(11.dp))
                CoursePulse(Modifier.align(Alignment.Start).fillMaxWidth(0.6f).height(11.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CoursesPagination(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (totalPages <= 1) return
    val maxVisible = 5
    val startBase = maxOf(1, currentPage - maxVisible / 2)
    val end = minOf(totalPages, startBase + maxVisible - 1)
    val start = if (end - startBase + 1 < maxVisible) maxOf(1, end - maxVisible + 1) else startBase

    Column(
        modifier
            .fillMaxWidth()
            .absolutePadding(left = 20.dp, right = 20.dp, top = 20.dp, bottom = 20.dp)
            .padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            FlowRow(
                modifier = Modifier
                    .rnShadow(1.dp, RoundedCornerShape(25.dp))
                    .clip(RoundedCornerShape(25.dp))
                    .background(Color.White.copy(alpha = 0.95f))
                    .absolutePadding(left = 16.dp, right = 16.dp, top = 12.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.Center
            ) {
                if (currentPage > 1) {
                    PageNav { onPageChange(currentPage - 1) }
                }
                if (start > 1) {
                    PageButton(1, currentPage == 1, onPageChange)
                    if (start > 2) Ellipsis()
                }
                for (page in start..end) {
                    PageButton(page, page == currentPage, onPageChange)
                }
                if (end < totalPages) {
                    if (end < totalPages - 1) Ellipsis()
                    PageButton(totalPages, currentPage == totalPages, onPageChange)
                }
                if (currentPage < totalPages) {
                    PageNav(next = true) { onPageChange(currentPage + 1) }
                }
            }
        }
    }
}

@Composable
private fun PageNav(next: Boolean = false, onClick: () -> Unit) {
    val shape = CircleShape
    Box(Modifier.absolutePadding(left = 6.dp, right = 6.dp, top = 4.dp, bottom = 4.dp).touchableOpacity(activeOpacity = 0.7f, onClick = onClick)) {
        LinearGradient(
            colors = listOf(CoursePrimary, CoursePrimaryDark),
            modifier = Modifier
                .size(40.dp)
                .rnShadow(6.dp, shape, CoursePrimary),
            start = Offset(0f, 0f),
            end = Offset(1f, 1f),
            shape = shape
        ) {
            MaterialIcons(
                if (next) "keyboard-arrow-left" else "keyboard-arrow-right",
                size = 20.dp,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun PageButton(page: Int, active: Boolean, onPress: (Int) -> Unit) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val shape = CircleShape
    Box(
        Modifier
            .absolutePadding(left = 4.dp, right = 4.dp, top = 4.dp, bottom = 4.dp)
            .touchableOpacity(activeOpacity = 0.7f) {
                if (active) return@touchableOpacity
                scope.launch {
                    scale.animateTo(0.8f, rnTiming(100))
                    scale.animateTo(1f, rnTiming(150))
                    onPress(page)
                }
            }
    ) {
        Box(
            Modifier
                .size(40.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .then(
                    if (active) Modifier.rnShadow(6.dp, shape, CoursePrimary).background(CoursePrimary, shape)
                    else Modifier.border(1.dp, Color(0xFFE0E0E0), shape)
                ),
            contentAlignment = Alignment.Center
        ) {
            AppText(
                text = page.toString(),
                fontSize = 16.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = if (active) Color.White else Color(0xFF666666),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun Ellipsis() {
    Box(Modifier.size(40.dp).absolutePadding(left = 4.dp, right = 4.dp), contentAlignment = Alignment.Center) {
        AppText(
            text = "...",
            fontSize = 18.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = Color(0xFF999999),
            textAlign = TextAlign.Center
        )
    }
}
