package ir.farimod.app.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ir.farimod.app.platform.openUrl
import ir.farimod.app.resources.Res
import ir.farimod.app.resources.design_learn
import ir.farimod.app.resources.resume_banner
import ir.farimod.app.resources.web_icon
import ir.farimod.app.ui.LocalImage
import ir.farimod.app.ui.ResizeMode
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.touchableOpacity
import org.jetbrains.compose.resources.DrawableResource

private class HomeJobSlide(val image: DrawableResource, val url: String)

private val homeJobSlides = listOf(
    HomeJobSlide(Res.drawable.web_icon, "https://www.farimod.ir"),
    HomeJobSlide(Res.drawable.resume_banner, "https://share.google/TZCNi0lESeBnSrJbO"),
    HomeJobSlide(Res.drawable.design_learn, "https://www.farimod.ir")
)

@Composable
fun HomeJobSlider() {
    Column(
        Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            Modifier
                .absolutePadding(left = 4.dp, right = 4.dp)
                .widthIn(max = 460.dp)
                .fillMaxWidth()
        ) {
            val cardWidth = if (homeJobSlides.size == 1) maxWidth else maxWidth * 0.86f
            HomeCardRow(
                modifier = Modifier.fillMaxWidth().height(110.dp),
                itemSpacing = 12.dp,
                contentPaddingHorizontal = 0.dp
            ) {
                itemsIndexed(homeJobSlides, key = { index, _ -> "job-$index" }) { _, slide ->
                    Box(
                        Modifier
                            .width(cardWidth)
                            .height(110.dp)
                            .rnShadow(5.dp, RoundedCornerShape(20.dp))
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFFF3F4F6))
                            .touchableOpacity(activeOpacity = 0.9f) { openJobUrl(slide.url) }
                    ) {
                        LocalImage(slide.image, Modifier.fillMaxSize(), ResizeMode.cover)
                    }
                }
            }
        }
    }
}

private fun openJobUrl(url: String) {
    if (url.isEmpty()) return
    // canOpenUrl is false on Android 11+ without a <queries> match, so the banner never opened.
    openUrl(url)
}
