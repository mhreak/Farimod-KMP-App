package ir.farimod.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.network.bool
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.str
import ir.farimod.app.theme.FarimodColors
import kotlinx.serialization.json.JsonElement

/**
 * Port of `components/ChipsUI.tsx`.
 * [groups] are the dynamic API objects of `MemberGroup` (keys `MemberGroupId`, `GroupName`, `Active`, ...),
 * [sortType] is one of "length-asc" | "length-desc" | "alphabetical" | "none".
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ChipsUI(
    groups: List<JsonElement>,
    selectedGroups: List<Int>,
    onToggleGroup: (Int) -> Unit,
    maxSelectable: Int? = null,
    allowMultipleSelection: Boolean = true,
    sortType: String = "length-asc",
    fullWidth: Boolean = true,
    modifier: Modifier = Modifier
) {
    val sortedGroups = remember(groups, sortType) {
        val activeGroups = groups.filter { it["Active"].bool }
        when (sortType) {
            "length-asc" -> activeGroups.sortedBy { (it["GroupName"].str ?: "").length }
            "length-desc" -> activeGroups.sortedByDescending { (it["GroupName"].str ?: "").length }
            "alphabetical" -> activeGroups.sortedWith { a, b ->
                (a["GroupName"].str ?: "").compareTo(b["GroupName"].str ?: "")
            }
            else -> activeGroups
        }
    }

    fun getFontSize(groupName: String): Int {
        val textLength = groupName.length
        if (textLength < 5) return 15
        if (textLength < 10) return 14
        if (textLength < 15) return 13
        return 12
    }

    Column(modifier = modifier.padding(bottom = 20.dp)) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val scrollMod = if (constraints.hasBoundedHeight) Modifier.verticalScroll(rememberScrollState()) else Modifier

            if (fullWidth) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(scrollMod),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (group in sortedGroups) {
                        val id = group.int("MemberGroupId") ?: continue
                        val name = group.str("GroupName") ?: ""
                        val isSelected = selectedGroups.contains(id)
                        val canSelect = if (allowMultipleSelection)
                            (maxSelectable == null || maxSelectable == 0 || selectedGroups.size < maxSelectable || isSelected)
                        else
                            (selectedGroups.isEmpty() || isSelected)
                        val dynamicFontSize = getFontSize(name)
                        val shape = RoundedCornerShape(16.dp)

                        var bg = FarimodColors.Light
                        var borderColor = FarimodColors.Medium
                        var elevation = 1.dp
                        var shadowColor = Color.Black
                        var chipAlpha = 1f
                        if (isSelected) {
                            bg = FarimodColors.Success
                            borderColor = FarimodColors.Success
                            shadowColor = FarimodColors.Success
                            elevation = 3.dp
                        }
                        if (!canSelect) {
                            bg = Color(0xFFF5F5F5)
                            borderColor = Color(0xFFE0E0E0)
                            chipAlpha = 0.6f
                        }
                        val textColor = when {
                            !canSelect -> Color(0xFFBDBDBD)
                            isSelected -> FarimodColors.White
                            else -> FarimodColors.Dark
                        }

                        RowReverse(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(chipAlpha)
                                .touchableOpacity(enabled = canSelect, activeOpacity = 0.7f) {
                                    if (canSelect) onToggleGroup(id)
                                }
                                .rnShadow(elevation, shape, shadowColor)
                                .background(bg, shape)
                                .border(1.dp, borderColor, shape)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppText(
                                text = name,
                                fontSize = dynamicFontSize.sp,
                                color = textColor,
                                fontFamily = if (isSelected) RNFonts.Yekan_Bakh_Bold else RNFonts.Yekan_Bakh_Regular,
                                textAlign = TextAlign.Start,
                                maxLines = 1,
                                softWrap = false
                            )

                            if (isSelected) {
                                MaterialIcons(
                                    name = "check-circle",
                                    size = 20.dp,
                                    color = FarimodColors.White
                                )
                            } else {
                                MaterialIcons(
                                    name = "add-circle-outline",
                                    size = 20.dp,
                                    color = if (canSelect) FarimodColors.Medium else Color(0xFFBDBDBD)
                                )
                            }
                        }
                    }
                }
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().then(scrollMod),
                    horizontalArrangement = Arrangement.Start,
                    verticalArrangement = Arrangement.Top
                ) {
                    for (group in sortedGroups) {
                        val id = group.int("MemberGroupId") ?: continue
                        val name = group.str("GroupName") ?: ""
                        val isSelected = selectedGroups.contains(id)
                        val canSelect = if (allowMultipleSelection)
                            (maxSelectable == null || maxSelectable == 0 || selectedGroups.size < maxSelectable || isSelected)
                        else
                            (selectedGroups.isEmpty() || isSelected)
                        val dynamicFontSize = getFontSize(name)
                        val shape = RoundedCornerShape(20.dp)

                        var bg = FarimodColors.Light
                        var borderColor = FarimodColors.Medium
                        var elevation = 1.dp
                        var shadowColor = Color.Black
                        var chipAlpha = 1f
                        if (isSelected) {
                            bg = FarimodColors.Success
                            borderColor = FarimodColors.Success
                            shadowColor = FarimodColors.Success
                            elevation = 3.dp
                        }
                        if (!canSelect) {
                            bg = Color(0xFFF5F5F5)
                            borderColor = Color(0xFFE0E0E0)
                            chipAlpha = 0.6f
                        }
                        val textColor = when {
                            !canSelect -> Color(0xFFBDBDBD)
                            isSelected -> FarimodColors.White
                            else -> FarimodColors.Dark
                        }

                        Column(modifier = Modifier.absolutePadding(right = 8.dp, bottom = 10.dp)) {
                            Row(
                                modifier = Modifier
                                    .alpha(chipAlpha)
                                    .touchableOpacity(enabled = canSelect, activeOpacity = 0.7f) {
                                        if (canSelect) onToggleGroup(id)
                                    }
                                    .rnShadow(elevation, shape, shadowColor)
                                    .background(bg, shape)
                                    .border(1.dp, borderColor, shape)
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (isSelected) {
                                    MaterialIcons(
                                        name = "check-circle",
                                        size = 16.dp,
                                        color = FarimodColors.White,
                                        modifier = Modifier.absolutePadding(right = 6.dp)
                                    )
                                }
                                AppText(
                                    text = name,
                                    fontSize = dynamicFontSize.sp,
                                    color = textColor,
                                    fontFamily = if (isSelected) RNFonts.Yekan_Bakh_Bold else RNFonts.Yekan_Bakh_Regular,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                if (!isSelected) {
                                    MaterialIcons(
                                        name = "add-circle-outline",
                                        size = 16.dp,
                                        color = if (canSelect) FarimodColors.Medium else FarimodColors.Light,
                                        modifier = Modifier.absolutePadding(left = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (maxSelectable != null && maxSelectable != 0) {
            AppText(
                text = "${selectedGroups.size}/$maxSelectable گروه انتخاب شده",
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                fontSize = 12.sp,
                color = FarimodColors.Medium,
                fontFamily = RNFonts.Yekan_Bakh_Regular,
                textAlign = TextAlign.Center
            )
        }
    }
}
