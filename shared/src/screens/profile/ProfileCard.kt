package ir.farimod.app.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.data.model.UserData
import ir.farimod.app.services.PermissionService
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.touchableOpacity

/**
 * Port of `screens/profile/ui/ProfileCard.tsx`.
 * [cardWidth] = `width * 0.75` (styles.profileCard.width).
 */
@Composable
internal fun ProfileCard(
    item: ProfileItem,
    cardWidth: Dp,
    onPress: (String) -> Unit,
    user: UserData?,
    onShowPermissionModal: (String?, String) -> Unit
) {
    val hasPermission = if (item.permission != null) PermissionService.hasPermission(user, item.permission) else true

    val handlePress = {
        if (!hasPermission) {
            onShowPermissionModal(item.permission, item.screenName)
        } else {
            onPress(item.screenName)
        }
    }

    val cardShape = RoundedCornerShape(25.dp)
    val borderColor = if (hasPermission) item.color else Color(0xFFDDDDDD)

    Box(
        modifier = Modifier
            .width(cardWidth)
            .touchableOpacity(activeOpacity = 0.8f, onClick = handlePress)
            .clip(cardShape)
            .border(2.dp, borderColor, cardShape)
    ) {
        // glassCard
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp)
                .background(if (hasPermission) item.color.copy(alpha = 0x15 / 255f) else Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            // cardContent
            Column(
                modifier = Modifier.fillMaxWidth().padding(25.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // iconContainer
                Box(
                    modifier = Modifier
                        .absolutePadding(bottom = 15.dp)
                        .size(80.dp)
                        .rnShadow(8.dp, CircleShape)
                        .background(Color(0xF2FFFFFF), CircleShape)
                        .border(2.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    MaterialIcons(
                        name = item.icon,
                        size = 36.dp,
                        color = if (hasPermission) item.color else Color(0xFF999999)
                    )
                    if (!hasPermission) {
                        // lockOverlay: top -5, right -5 (relative to the padding box, inside the 2dp border)
                        Box(
                            modifier = Modifier
                                .align(AbsoluteAlignment.TopRight)
                                .absoluteOffset(x = 5.dp, y = (-5).dp)
                                .size(24.dp)
                                .background(Color(0xFFFF4444), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            MaterialIcons(name = "lock", size = 16.dp, color = Color(0xFFFFFFFF))
                        }
                    }
                }
                AppText(
                    text = item.title,
                    fontSize = 17.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp,
                    color = if (hasPermission) Color(0xFF2C3E50) else Color(0xFF999999)
                )
            }

            if (!hasPermission) {
                // upgradeTextVertical
                Box(
                    modifier = Modifier
                        .align(AbsoluteAlignment.CenterLeft)
                        .absolutePadding(left = 8.dp)
                        .width(20.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    AppText(
                        text = "نیاز به ارتقای اشتراک",
                        fontSize = 11.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        color = Color(0xFF999999),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .requiredWidth(100.dp)
                            .graphicsLayer { rotationZ = 270f }
                    )
                }
            }
        }
    }
}
