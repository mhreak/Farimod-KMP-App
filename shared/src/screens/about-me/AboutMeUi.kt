package ir.farimod.app.screens.aboutme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.network.jsonOf
import ir.farimod.app.platform.NativeVideoPlayer
import ir.farimod.app.platform.isIOS
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.linearGradient
import ir.farimod.app.ui.pressable
import ir.farimod.app.ui.rgba
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.ui.windowSize
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonObject

internal object AboutMeColors {
    val primary = Color(0xFF6366F1)
    val tertiary = Color(0xFF06B6D4)
    val accent = Color(0xFF10B981)
    val surface = Color(0xFFFFFFFF)
    val success = Color(0xFF10B981)
    val error = Color(0xFFEF4444)
    val fashionIcon = Color(0xFF8B5CF6)
    val phoneIcon = Color(0xFF10B981)
    val emailIcon = Color(0xFFEF4444)
    val websiteIcon = Color(0xFF06B6D4)
    val telegramIcon = Color(0xFF0088CC)
    val instagramIcon = Color(0xFFE1306C)
    val whatsappIcon = Color(0xFF25D366)
    val addressIcon = Color(0xFFF59E0B)
    val locationIcon = Color(0xFF8B5CF6)
    val groupIcon = Color(0xFF6366F1)

    fun icon(type: String): Color = when (type) {
        "person" -> fashionIcon
        "videocam" -> tertiary
        "camera" -> accent
        "phone" -> phoneIcon
        "email" -> emailIcon
        "website" -> websiteIcon
        "telegram" -> telegramIcon
        "instagram" -> instagramIcon
        "whatsapp" -> whatsappIcon
        "address" -> addressIcon
        "location" -> locationIcon
        "group" -> groupIcon
        else -> primary
    }
}

internal fun Color.alphaHex(hex: String): Color = copy(alpha = hex.toInt(16) / 255f)

internal data class AboutMeGroup(val id: Int, val name: String)

internal data class AboutMeProfile(
    val aboutMe: String = "",
    val avatarUrl: String? = null,
    val introVideo: String? = null,
    val name: String = "",
    val title: String = "",
    val cityId: Int? = null,
    val cityName: String = "",
    val provinceId: Int? = null,
    val provinceName: String = "",
    val phone1: String = "",
    val phone2: String = "",
    val email: String = "",
    val websiteAddress: String = "",
    val telegramAccountId: String = "",
    val instagramAccountId: String = "",
    val whatsappAccountMobileNumber: String = "",
    val address: String = "",
    val mobile: String = "",
    val memberGroupList: List<AboutMeGroup> = emptyList()
)

internal fun AboutMeProfile.toRouteJson(): JsonObject = jsonOf(
    "aboutMe" to aboutMe,
    "avatarUrl" to avatarUrl,
    "introVideo" to introVideo,
    "name" to name,
    "title" to title,
    "cityId" to cityId,
    "cityName" to cityName,
    "provinceId" to provinceId,
    "provinceName" to provinceName,
    "phone1" to phone1,
    "phone2" to phone2,
    "email" to email,
    "websiteAddress" to websiteAddress,
    "telegramAccountId" to telegramAccountId,
    "instagramAccountId" to instagramAccountId,
    "whatsappAccountMobileNumber" to whatsappAccountMobileNumber,
    "address" to address,
    "mobile" to mobile,
    "memberGroupList" to memberGroupList.map {
        jsonOf("MemberGroupId" to it.id, "MemberGroupName" to it.name)
    }
)

internal fun aboutMeUploadMime(uri: String, type: String?, image: Boolean): String {
    val ext = uri.substringAfterLast('.', "").substringBefore('?').substringBefore('#').lowercase()
    if (image) {
        when (ext) {
            "png" -> return "image/png"
            "jpg", "jpeg" -> return "image/jpeg"
        }
        return type?.takeIf { it.startsWith("image/") } ?: "image/jpeg"
    }
    when (ext) {
        "mp4" -> return "video/mp4"
        "mov" -> return "video/quicktime"
        "m4v" -> return "video/x-m4v"
    }
    return type?.takeIf { it.startsWith("video/") } ?: "video/mp4"
}

@Composable
internal fun AboutMeBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(44.dp)
            .zIndex(1000f)
            .rnShadow(5.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.9f))
            .touchableOpacity(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        MaterialIcons("arrow-forward", size = 24.dp, color = Color(0xFF6366F1))
    }
}

@Composable
internal fun AboutMeValueText(text: String, modifier: Modifier = Modifier) {
    AppText(
        text = text,
        modifier = modifier.fillMaxWidth(),
        fontSize = 16.sp,
        color = Color(0xFF374151),
        textAlign = TextAlign.Right,
        lineHeight = 26.sp,
        fontFamily = RNFonts.Yekan_Bakh_Regular
    )
}

@Composable
internal fun AboutMeInfoCard(
    iconName: String,
    iconType: String,
    label: String,
    showEdit: Boolean,
    editEnabled: Boolean,
    onEdit: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val color = AboutMeColors.icon(iconType)
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier
            .absolutePadding(left = 5.dp, right = 5.dp, bottom = 14.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(rgba(255, 255, 255, 0.4f))
            .border(1.dp, rgba(203, 213, 225, 0.4f), shape)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            RowReverse(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RowReverse(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .absolutePadding(left = 12.dp)
                            .size(44.dp)
                            .clip(CircleShape)
                            .linearGradient(
                                colors = listOf(color, color.alphaHex("CC")),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        MaterialIcons(iconName, size = 22.dp, color = AboutMeColors.surface)
                    }
                    AppText(
                        text = label,
                        fontSize = 17.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color(0xFF2C3E50)
                    )
                }
                if (showEdit) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AboutMeColors.primary)
                            .touchableOpacity(enabled = editEnabled, onClick = onEdit),
                        contentAlignment = Alignment.Center
                    ) {
                        MaterialIcons("edit", size = 18.dp, color = AboutMeColors.surface)
                    }
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .absolutePadding(left = 15.dp, right = 15.dp)
                    .padding(top = 15.dp),
                content = content
            )
        }
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .align(AbsoluteAlignment.CenterRight)
                    .fillMaxHeight()
                    .width(6.dp)
                    .clip(RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp))
                    .background(color.alphaHex("60"))
            )
        }
    }
}

@Composable
internal fun BoxScope.AboutMeUploadingOverlay(text: String, color: Color) {
    Box(
        Modifier
            .matchParentSize()
            .zIndex(1000f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(36.dp), color = color, strokeWidth = 3.dp)
            AppText(
                text = text,
                modifier = Modifier.padding(top = 12.dp),
                fontSize = 16.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = Color(0xFF2C3E50),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun AboutMeEditableText(
    value: String,
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
    loading: Boolean,
    placeholder: String = "متن درباره خود را وارد کنید..."
) {
    val focusRequester = remember { FocusRequester() }
    var localValue by remember { mutableStateOf(value) }
    LaunchedEffect(value) { localValue = value }
    LaunchedEffect(Unit) {
        delay(100)
        runCatching { focusRequester.requestFocus() }
    }
    val textStyle = TextStyle(
        fontFamily = RNFonts.Yekan_Bakh_Regular,
        fontSize = 16.sp,
        color = Color(0xFF374151),
        textAlign = TextAlign.Right,
        lineHeight = 26.sp,
        textDirection = TextDirection.Rtl
    )
    BasicTextField(
        value = localValue,
        onValueChange = { localValue = it },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(2.dp, AboutMeColors.primary, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .focusRequester(focusRequester),
        textStyle = textStyle,
        readOnly = loading,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Text,
            imeAction = if (isIOS) ImeAction.Done else ImeAction.Default
        ),
        cursorBrush = SolidColor(Color(0xFF374151)),
        decorationBox = { inner ->
            Box {
                if (localValue.isEmpty()) {
                    Text(text = placeholder, style = textStyle.copy(color = Color.Black.copy(alpha = 0.5f)))
                }
                inner()
            }
        }
    )
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AboutMeActionButton(
            icon = "check",
            color = AboutMeColors.success,
            enabled = !loading,
            onClick = { onSave(localValue.trim()) }
        )
        AboutMeActionButton(
            icon = "close",
            color = AboutMeColors.error,
            enabled = !loading,
            onClick = {
                localValue = value
                onCancel()
            }
        )
    }
}

@Composable
private fun AboutMeActionButton(icon: String, color: Color, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(32.dp)
            .alpha(if (enabled) 1f else 0.6f)
            .clip(CircleShape)
            .background(color)
            .touchableOpacity(enabled = enabled, activeOpacity = 0.8f, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        MaterialIcons(icon, size = 18.dp, color = AboutMeColors.surface)
    }
}

@Composable
internal fun AboutMeVideoDialog(url: String, onClose: () -> Unit) {
    val (width, _) = windowSize()
    val modalWidth = width * 0.9f
    val videoHeight = modalWidth * (9f / 16f)
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(rgba(0, 0, 0, 0.95f))
                .pressable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .width(modalWidth)
                    .height(50.dp + videoHeight)
                    .pressable { }
            ) {
                NativeVideoPlayer(
                    url = url,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(videoHeight)
                        .clip(RoundedCornerShape(12.dp)),
                    showControls = true,
                    autoPlay = true,
                    loop = false,
                    cover = false
                )
                Box(
                    Modifier
                        .align(AbsoluteAlignment.TopRight)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(rgba(0, 0, 0, 0.5f))
                        .touchableOpacity(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    MaterialIcons("close", size = 28.dp, color = Color.White)
                }
            }
        }
    }
}
