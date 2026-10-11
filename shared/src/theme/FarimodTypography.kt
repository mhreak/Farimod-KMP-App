package ir.farimod.app.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import ir.farimod.app.resources.*
import org.jetbrains.compose.resources.Font

val YekanBakhFontFamily: FontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.yekan_bakh_regular, weight = FontWeight.Normal),
        Font(Res.font.yekan_bakh_bold, weight = FontWeight.Bold),
        Font(Res.font.yekan_bakh_extrabold, weight = FontWeight.ExtraBold)
    )

val IranSansFontFamily: FontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.iransans_medium, weight = FontWeight.Normal),
        Font(Res.font.iransans_bold, weight = FontWeight.Bold),
        Font(Res.font.iransans_black, weight = FontWeight.Black)
    )

@Composable
fun getFarimodTypography(): Typography {
    val defaultFont = YekanBakhFontFamily

    return Typography(
        displayLarge = TextStyle(
            fontFamily = defaultFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            textAlign = TextAlign.Right
        ),
        displayMedium = TextStyle(
            fontFamily = defaultFont,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            textAlign = TextAlign.Right
        ),
        displaySmall = TextStyle(
            fontFamily = defaultFont,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            textAlign = TextAlign.Right
        ),
        headlineLarge = TextStyle(
            fontFamily = defaultFont,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            textAlign = TextAlign.Right
        ),
        headlineMedium = TextStyle(
            fontFamily = defaultFont,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            textAlign = TextAlign.Right
        ),
        headlineSmall = TextStyle(
            fontFamily = defaultFont,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Right
        ),
        titleLarge = TextStyle(
            fontFamily = defaultFont,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Right
        ),
        titleMedium = TextStyle(
            fontFamily = defaultFont,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Right
        ),
        titleSmall = TextStyle(
            fontFamily = defaultFont,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Right
        ),
        bodyLarge = TextStyle(
            fontFamily = defaultFont,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Right
        ),
        bodyMedium = TextStyle(
            fontFamily = defaultFont,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Right
        ),
        bodySmall = TextStyle(
            fontFamily = defaultFont,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            textAlign = TextAlign.Right
        ),
        labelLarge = TextStyle(
            fontFamily = defaultFont,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Right
        ),
        labelMedium = TextStyle(
            fontFamily = defaultFont,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Right
        ),
        labelSmall = TextStyle(
            fontFamily = defaultFont,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            textAlign = TextAlign.Right
        )
    )
}
