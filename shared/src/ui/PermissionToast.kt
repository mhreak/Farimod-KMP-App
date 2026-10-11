package ir.farimod.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.farimod.app.components.icons.MaterialIcons
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Port of `components/PermissionToast.tsx` (RN `Modal` -> compose `Dialog`). */
@Composable
fun PermissionToast(
    visible: Boolean,
    message: String,
    onClose: () -> Unit,
    onUpgrade: () -> Unit
) {
    val fadeAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.8f) }

    LaunchedEffect(visible) {
        if (visible) {
            coroutineScope {
                launch { fadeAnim.animateTo(1f, rnTiming(300)) }
                launch { scaleAnim.animateTo(1f, rnSpring(tension = 100f, friction = 8f)) }
            }
        } else {
            // RN animates back in 200ms while the Modal is already closed.
            fadeAnim.snapTo(0f)
            scaleAnim.snapTo(0.8f)
        }
    }

    if (!visible) return

    val containerShape = RoundedCornerShape(25.dp)
    val buttonShape = RoundedCornerShape(25.dp)

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        // overlay: flex 1, rgba(0,0,0,0.6), opacity fadeAnim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = fadeAnim.value }
                .background(Color(0x99000000))
        ) {
            // overlayTouch: flex 1, center, paddingHorizontal 20, TouchableOpacity(activeOpacity 1) -> onClose
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pressable { onClose() }
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                // container: width 100%, maxWidth 340, radius 25, overflow hidden, elevation 10, scale
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 340.dp)
                        .graphicsLayer {
                            scaleX = scaleAnim.value
                            scaleY = scaleAnim.value
                        }
                        .rnShadow(10.dp, containerShape, Color.Black)
                        .clip(containerShape)
                        .pressable { } // inner TouchableOpacity(activeOpacity 1) swallows taps
                ) {
                    LinearGradient(
                        colors = listOf(Color(0xFFFF6B6B), Color(0xFFFF5252)),
                        modifier = Modifier.fillMaxWidth(),
                        start = Offset(0f, 0f),
                        end = Offset(1f, 1f)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(25.dp)) {
                            // header
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(bottom = 12.dp)
                                        .size(60.dp)
                                        .background(Color(0x33FFFFFF), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    MaterialIcons(name = "lock", size = 32.dp, color = Color.White)
                                }
                                AppText(
                                    text = "دسترسی محدود",
                                    fontSize = 22.sp,
                                    color = Color.White,
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    textAlign = TextAlign.Center
                                )
                            }

                            AppText(
                                text = message,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 25.dp)
                                    .padding(horizontal = 5.dp),
                                fontSize = 16.sp,
                                color = Color.White,
                                fontFamily = RNFonts.Yekan_Bakh_Regular,
                                textAlign = TextAlign.Center,
                                lineHeight = 26.sp
                            )

                            // buttonContainer: row, space-between, gap 15
                            Row(
                                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                                horizontalArrangement = Arrangement.spacedBy(15.dp)
                            ) {
                                // cancelButton
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .touchableOpacity { onClose() }
                                        .background(Color(0x33FFFFFF), buttonShape)
                                        .border(1.dp, Color(0x4DFFFFFF), buttonShape)
                                        .padding(vertical = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AppText(
                                        text = "باشه",
                                        fontSize = 16.sp,
                                        color = Color.White,
                                        fontFamily = RNFonts.Yekan_Bakh_Regular
                                    )
                                }

                                // upgradeButton
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .touchableOpacity { onUpgrade() }
                                        .rnShadow(5.dp, buttonShape, Color(0xFF4CAF50))
                                        .clip(buttonShape)
                                ) {
                                    LinearGradient(
                                        colors = listOf(Color(0xFF4CAF50), Color(0xFF2E7D32)),
                                        modifier = Modifier.fillMaxSize(),
                                        start = Offset(0f, 0f),
                                        end = Offset(1f, 0f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            MaterialIcons(name = "upgrade", size = 20.dp, color = Color.White)
                                            AppText(
                                                text = "ارتقای اشتراک",
                                                modifier = Modifier.absolutePadding(left = 8.dp),
                                                fontSize = 16.sp,
                                                color = Color.White,
                                                fontFamily = RNFonts.Yekan_Bakh_Bold
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
    }
}
