package ir.farimod.app.screens.membergroup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import ir.farimod.app.components.icons.MaterialCommunityIcons
import ir.farimod.app.network.ApiConfig
import ir.farimod.app.network.int
import ir.farimod.app.network.long
import ir.farimod.app.network.str
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.linearGradient
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.utils.safeString
import kotlinx.serialization.json.JsonElement

internal val GroupPrimary = Color(0xFF667EEA)
internal val GroupPrimaryDark = Color(0xFF764BA2)

private val GroupGradients = listOf(
    listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
    listOf(Color(0xFFF093FB), Color(0xFFF5576C)),
    listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)),
    listOf(Color(0xFF43E97B), Color(0xFF38F9D7)),
    listOf(Color(0xFFFA709A), Color(0xFFFEE140)),
    listOf(Color(0xFF30CFD0), Color(0xFF330867)),
    listOf(Color(0xFFA8EDEA), Color(0xFFFED6E3)),
    listOf(Color(0xFFFF9A9E), Color(0xFFFECFEF)),
    listOf(Color(0xFFFFECD2), Color(0xFFFCB69F)),
    listOf(Color(0xFFFF6E7F), Color(0xFFBFE9FF)),
    listOf(Color(0xFF8EC5FC), Color(0xFFE0C3FC)),
    listOf(Color(0xFFFBC2EB), Color(0xFFA6C1EE)),
    listOf(Color(0xFFFDCBF1), Color(0xFFE6DEE9)),
    listOf(Color(0xFFA1C4FD), Color(0xFFC2E9FB)),
    listOf(Color(0xFFD299C2), Color(0xFFFEF9D7)),
    listOf(Color(0xFFFEE140), Color(0xFFFA709A)),
    listOf(Color(0xFFFDBB2D), Color(0xFF22C1C3)),
    listOf(Color(0xFFEE9CA7), Color(0xFFFFDDE1)),
    listOf(Color(0xFF89F7FE), Color(0xFF66A6FF)),
    listOf(Color(0xFFCD9CF2), Color(0xFFF6F3FF))
)

private val GroupIcons = listOf(
    "tshirt-crew",
    "hanger",
    "ruler",
    "draw",
    "palette",
    "scissors-cutting",
    "content-cut",
    "tag",
    "shopping",
    "badge-account",
    "brush",
    "dots-grid"
)

internal fun groupIdOf(item: JsonElement): Int =
    item.int("MemberGroupId") ?: item.long("MemberGroupId")?.toInt() ?: 0

@Composable
internal fun MemberGroupTile(
    item: JsonElement,
    modifier: Modifier = Modifier,
    onPress: (JsonElement) -> Unit
) {
    val id = groupIdOf(item)
    val gradient = GroupGradients[id.mod(GroupGradients.size)]
    val icon = GroupIcons[id.mod(GroupIcons.size)]
    val shape = RoundedCornerShape(20.dp)
    val rawUrl = item.str("ImageURL")
        ?: item.str("FeaturedImageURL")
        ?: item.str("GroupImageURL")
        ?: item.str("IconURL")
        ?: item.str("PictureURL")
        ?: item.str("PhotoURL")
        ?: item.str("Image")
        ?: item.str("FeaturedImageFileName")
        ?: item.str("ImageFileName")
    val imageUrl = ApiConfig.resolveMediaUrl(rawUrl)
    var imageError by remember(imageUrl) { mutableStateOf(false) }
    val showImage = !imageUrl.isNullOrBlank() && !imageError

    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .touchableOpacity(activeOpacity = 0.85f) { onPress(item) }
            .rnShadow(6.dp, shape)
            .clip(shape)
    ) {
        if (showImage) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = { imageError = true }
            )
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .linearGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
            )
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            ) {
                AppText(
                    text = safeString(item.str("GroupName"), "گروه بدون نام"),
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 11.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            LinearGradient(
                colors = gradient,
                modifier = Modifier
                    .fillMaxSize()
                    .borderWhite(shape),
                start = Offset(0f, 0f),
                end = Offset(1f, 1f)
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        Modifier.padding(bottom = 6.dp)
                    ) {
                        MaterialCommunityIcons(
                            name = icon,
                            size = 28.dp,
                            color = Color.Black.copy(alpha = 0.95f)
                        )
                    }
                    AppText(
                        text = safeString(item.str("GroupName"), "گروه بدون نام"),
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 11.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
internal fun MemberGroupSkeleton(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .rnShadow(3.dp, shape)
            .clip(shape)
            .background(Color(0xFFE0E0E0))
    )
}

private fun Modifier.borderWhite(shape: RoundedCornerShape): Modifier =
    border(3.dp, Color.White, shape)
