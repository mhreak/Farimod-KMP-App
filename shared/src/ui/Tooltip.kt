package ir.farimod.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.farimod.app.components.icons.Ionicons
import ir.farimod.app.components.icons.MaterialIcons
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private val TooltipGreen1 = Color(0xFF10B981)
private val TooltipGreen2 = Color(0xFF059669)

/**
 * Port of `components/Tooltip.tsx`: a round green "information-circle" button which opens a full-screen
 * dimmed overlay with a "راهنما" card (RN `Modal` -> compose `Dialog`).
 */
@Composable
fun Tooltip(
    content: String,
    iconSize: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }

    val backdropOpacity = remember { Animatable(0f) }
    val contentOpacity = remember { Animatable(0f) }
    val scale = remember { Animatable(0.85f) }
    val translateY = remember { Animatable(35f) }

    LaunchedEffect(visible) {
        if (visible) {
            coroutineScope {
                launch { backdropOpacity.animateTo(1f, rnTiming(220)) }
                launch { contentOpacity.animateTo(1f, rnTiming(220)) }
                launch { scale.animateTo(1f, rnSpring(tension = 70f, friction = 8f)) }
                launch { translateY.animateTo(0f, rnSpring(tension = 70f, friction = 8f)) }
            }
        } else {
            // RN animates to these values in 170ms, but the Modal is already closed (invisible) by then.
            backdropOpacity.snapTo(0f)
            contentOpacity.snapTo(0f)
            scale.snapTo(0.92f)
            translateY.snapTo(20f)
        }
    }

    // iconButton: shadow 4 (#10b981)
    Box(
        modifier = modifier
            .rnShadow(4.dp, CircleShape, TooltipGreen1)
            .touchableOpacity(activeOpacity = 0.8f) { visible = true }
    ) {
        LinearGradient(
            colors = listOf(TooltipGreen1, TooltipGreen2),
            modifier = Modifier.size(44.dp).clip(CircleShape)
        ) {
            Box(Modifier.align(Alignment.Center)) {
                Ionicons(name = "information-circle", size = iconSize, color = Color(0xFFFFFFFF))
            }
        }
    }

    if (visible) {
        val density = LocalDensity.current
        val cardShape = RoundedCornerShape(26.dp)
        Dialog(
            onDismissRequest = { visible = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            // overlay: flex 1, rgba(0,0,0,0.45), center
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = backdropOpacity.value }
                    .background(Color(0x73000000)),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.fillMaxSize().pressable { visible = false })

                // card
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .graphicsLayer {
                            alpha = contentOpacity.value
                            scaleX = scale.value
                            scaleY = scale.value
                            translationY = translateY.value * density.density
                        }
                        .rnShadow(18.dp, cardShape, Color.Black)
                        .clip(cardShape)
                        .background(Color.White)
                        .pressable { }
                ) {
                    // header
                    LinearGradient(
                        colors = listOf(TooltipGreen1, TooltipGreen2),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp, vertical = 18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Ionicons(name = "information-circle", size = 32.dp, color = Color.White)
                                AppText(
                                    text = "راهنما",
                                    modifier = Modifier.absolutePadding(left = 10.dp),
                                    fontSize = 20.sp,
                                    color = Color.White,
                                    fontFamily = FontFamily.Default,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right
                                )
                            }
                            Box(Modifier.touchableOpacity { visible = false }) {
                                MaterialIcons(name = "close", size = 24.dp, color = Color.White)
                            }
                        }
                    }

                    // body
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                        AppText(
                            text = content,
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 16.sp,
                            color = Color(0xFF555555),
                            fontFamily = FontFamily.Default,
                            textAlign = TextAlign.Right,
                            lineHeight = 28.sp
                        )

                        Box(Modifier.fillMaxWidth().touchableOpacity(activeOpacity = 0.9f) { visible = false }) {
                            LinearGradient(
                                colors = listOf(TooltipGreen1, TooltipGreen2),
                                modifier = Modifier
                                    .padding(top = 25.dp)
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.align(Alignment.Center),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MaterialIcons(name = "check", size = 20.dp, color = Color.White)
                                    AppText(
                                        text = "متوجه شدم",
                                        modifier = Modifier.absolutePadding(left = 8.dp),
                                        fontSize = 16.sp,
                                        color = Color.White,
                                        fontFamily = FontFamily.Default,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Right
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
