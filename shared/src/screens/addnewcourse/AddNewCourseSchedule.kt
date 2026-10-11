package ir.farimod.app.screens.addnewcourse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.CustomTimePicker
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.rgba
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.touchableOpacity

@Composable
internal fun CourseWeeklySchedule(
    sessions: Map<String, CourseDaySession>,
    onToggle: (String, Boolean) -> Unit,
    onStart: (String, String) -> Unit,
    onFinish: (String, String) -> Unit
) {
    val cardShape = RoundedCornerShape(12.dp)
    val hairline = rgba(255, 206, 232, 0.1f)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 25.dp, bottom = 25.dp)
    ) {
        RowReverse(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .absolutePadding(left = 5.dp, right = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MaterialIcons(name = "schedule", size = 24.dp, color = FarimodColors.Primary)
            AppText(
                text = "برنامه هفتگی کلاس‌ها",
                fontSize = 18.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = FarimodColors.Primary,
                modifier = Modifier.absolutePadding(right = 8.dp)
            )
        }
        AppText(
            text = "روزهای برگزاری کلاس و ساعات مربوطه را انتخاب کنید",
            fontSize = 13.sp,
            color = FarimodColors.Medium,
            textAlign = TextAlign.Right,
            lineHeight = 20.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .absolutePadding(left = 5.dp, right = 5.dp)
        )

        courseWeekDays.forEach { day ->
            val session = sessions[day.key] ?: CourseDaySession()
            val on = session.enabled
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .rnShadow(2.dp, cardShape, rgba(0, 0, 0, 0.1f))
                    .clip(cardShape)
                    .background(rgba(255, 255, 255, 0.5f))
                    .border(1.dp, rgba(255, 206, 232, 0.2f), cardShape)
            ) {
                Column(Modifier.fillMaxWidth()) {
                RowReverse(
                    Modifier
                        .fillMaxWidth()
                        .touchableOpacity { onToggle(day.key, !on) }
                        .background(if (on) FarimodColors.Primary else rgba(255, 255, 255, 0.3f))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RowReverse(
                        Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MaterialIcons(
                            name = day.icon,
                            size = 20.dp,
                            color = if (on) FarimodColors.White else FarimodColors.Medium,
                            modifier = Modifier.absolutePadding(left = 8.dp)
                        )
                        AppText(
                            text = day.label,
                            fontSize = 16.sp,
                            fontFamily = if (on) RNFonts.Yekan_Bakh_Bold else RNFonts.Yekan_Bakh_Regular,
                            color = if (on) FarimodColors.White else FarimodColors.Dark
                        )
                    }
                    Box(
                        Modifier
                            .size(24.dp)
                            .border(
                                2.dp,
                                if (on) FarimodColors.White else FarimodColors.Medium,
                                RoundedCornerShape(6.dp)
                            )
                            .background(
                                if (on) rgba(255, 255, 255, 0.2f) else Color.Transparent,
                                RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (on) {
                            MaterialIcons(name = "check", size = 16.dp, color = FarimodColors.White)
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(hairline))
                }

                if (on) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(rgba(255, 255, 255, 0.4f))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TimeSlot(
                            label = "پایان",
                            value = session.finish,
                            onChange = { onFinish(day.key, it) },
                            modifier = Modifier.weight(1f)
                        )
                        Box(Modifier.absolutePadding(left = 15.dp, right = 15.dp, top = 10.dp, bottom = 10.dp)) {
                            MaterialIcons(name = "arrow-back", size = 20.dp, color = FarimodColors.Medium)
                        }
                        TimeSlot(
                            label = "شروع",
                            value = session.start,
                            onChange = { onStart(day.key, it) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeSlot(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        AppText(
            text = label,
            fontSize = 12.sp,
            color = FarimodColors.Medium,
            fontFamily = RNFonts.Yekan_Bakh_Regular,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        CustomTimePicker(
            value = value,
            onTimeChange = onChange,
            placeholder = "00:00",
            icon = "schedule",
            modifier = Modifier.fillMaxWidth()
        )
    }
}
