package ir.farimod.app.ui

import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.farimod.app.theme.FarimodColors

/**
 * Port of `components/Button.tsx`.
 *
 * RN style: borderRadius 16, center/center, padding 15, marginVertical 8, text 18 uppercase Yekan_Bakh_Bold.
 * Like an RN `View` in a column parent the button stretches to the available width; pass a width modifier
 * (`Modifier.fillMaxWidth(0.48f)`, `Modifier.width(..)`) FIRST in [modifier] to override (it is applied before the stretch).
 * Extra margins from the RN `style` prop go in [modifier] (use `absolutePadding(left = ..)` for physical margins).
 */
@Composable
fun AppButton(
    title: String,
    onPress: () -> Unit = {},
    modifier: Modifier = Modifier,
    color: Color = FarimodColors.Primary,
    textColor: Color = FarimodColors.White,
    disabled: Boolean = false
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .touchableOpacity(enabled = !disabled, onClick = onPress)
            .background(color, shape)
            .padding(15.dp),
        contentAlignment = Alignment.Center
    ) {
        AppText(
            text = title.uppercase(),
            fontSize = 18.sp,
            color = textColor,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            fontWeight = null,
            textAlign = TextAlign.Right
        )
    }
}
