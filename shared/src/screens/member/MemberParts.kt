package ir.farimod.app.screens.member

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import ir.farimod.app.components.icons.MaterialCommunityIcons
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.network.ApiConfig
import ir.farimod.app.network.bool
import ir.farimod.app.network.get
import ir.farimod.app.network.str
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.ResizeMode
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.utils.safeString
import kotlinx.serialization.json.JsonElement

internal val MemberPrimary = Color(0xFF667EEA)

private val AvatarGradients = listOf(
    listOf(Color(0xFFFA709A), Color(0xFFFEE140)),
    listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
    listOf(Color(0xFFF093FB), Color(0xFFF5576C)),
    listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)),
    listOf(Color(0xFF43E97B), Color(0xFF38F9D7)),
    listOf(Color(0xFFFF9A56), Color(0xFFFFAD56)),
    listOf(Color(0xFFA8EDEA), Color(0xFFFED6E3)),
    listOf(Color(0xFFFBC2EB), Color(0xFFA6C1EE))
)

@Composable
internal fun MemberPulse(modifier: Modifier, radius: Dp = 8.dp) {
    val transition = rememberInfiniteTransition(label = "memberSkeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "memberSkeletonAlpha"
    )
    Box(
        modifier
            .clip(RoundedCornerShape(radius))
            .background(Color(0xFFE0E0E0))
            .alpha(pulse)
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MemberAvatar(
    member: JsonElement,
    size: Dp = 80.dp,
    onPress: (() -> Unit)? = null
) {
    val name = member.str("Name")
    val gradient = AvatarGradients[(name?.length ?: 0) % AvatarGradients.size]
    val rawUrl = member.str("AvatarImageURL")
    val hasUrl = !rawUrl.isNullOrBlank()
    var imageLoading by remember(rawUrl) { mutableStateOf(hasUrl) }
    var imageError by remember(rawUrl) { mutableStateOf(false) }
    val showImage = hasUrl && !imageError && !imageLoading
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onPress != null) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 230f),
        label = "avatarScale"
    )
    val tick = size * 0.28f
    val inset = size * 0.05f

    Box(
        Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                enabled = onPress != null,
                onClick = { onPress?.invoke() }
            )
    ) {
        LinearGradient(
            colors = gradient,
            modifier = Modifier
                .fillMaxSize()
                .border(3.dp, Color.White, CircleShape)
                .clip(CircleShape),
            start = Offset(0f, 0f),
            end = Offset(1f, 1f)
        ) {
            MaterialCommunityIcons(
                name = if (member["Gender"].bool) "face-man" else "face-woman",
                size = size * 0.6f,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        if (hasUrl) {
            AsyncImage(
                model = ApiConfig.resolveMediaUrl(rawUrl),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(if (showImage) 1f else 0f)
                    .border(3.dp, Color.White, CircleShape)
                    .clip(CircleShape),
                contentScale = ResizeMode.cover,
                onSuccess = {
                    imageLoading = false
                    imageError = false
                },
                onError = {
                    imageLoading = false
                    imageError = true
                }
            )
        }
        if (member["ShowBlueTick"].bool) {
            Box(
                Modifier
                    .align(AbsoluteAlignment.BottomRight)
                    .absoluteOffset(x = -inset, y = -inset)
                    .size(tick)
                    .rnShadow(3.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                MaterialIcons("verified", size = size * 0.22f, color = Color(0xFF1DA1F2))
            }
        }
    }
}

@Composable
internal fun MemberPersonCard(member: JsonElement, onPress: (JsonElement) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .touchableOpacity(activeOpacity = 0.8f) { onPress(member) }
            .clip(shape)
            .background(Color.White.copy(alpha = 0.7f))
            .border(1.dp, Color.White, shape)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        MemberAvatar(member, 80.dp) { onPress(member) }
        Column(
            Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            AppText(
                text = safeString(member.str("Name"), "نام کاربر"),
                modifier = Modifier.fillMaxWidth().heightIn(min = 20.dp).padding(bottom = 8.dp),
                fontSize = 18.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = Color(0xFF253A4E),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            MemberGroupsLine(member.str("MemberGroupsStr"))
        }
    }
}

@Composable
private fun MemberGroupsLine(groups: String?) {
    Box(
        Modifier.fillMaxWidth().height(44.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!groups.isNullOrEmpty()) {
            RowReverse(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
                    .padding(bottom = 25.dp)
                    .absolutePadding(top = 2.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppText(
                    text = groups,
                    modifier = Modifier.fillMaxWidth(0.8f).absolutePadding(right = 6.dp),
                    fontSize = 11.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4C5896),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            RowReverse(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp)
                    .padding(bottom = 25.dp)
                    .absolutePadding(top = 2.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MaterialIcons("group", size = 16.dp, color = Color(0xFFCCCCCC))
                AppText(
                    text = "گروه تعریف نشده",
                    modifier = Modifier.absolutePadding(right = 6.dp),
                    fontSize = 12.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    fontStyle = FontStyle.Italic,
                    color = Color(0xFFCCCCCC),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
internal fun MemberPersonSkeleton() {
    Column(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.7f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        MemberPulse(Modifier.padding(bottom = 12.dp).size(80.dp), 40.dp)
        MemberPulse(Modifier.padding(bottom = 8.dp).fillMaxWidth(0.7f).height(16.dp))
        MemberPulse(Modifier.fillMaxWidth(0.8f).height(14.dp))
    }
}
