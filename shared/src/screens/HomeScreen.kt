package ir.farimod.app.screens

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.resources.Res
import ir.farimod.app.resources.main_icon
import ir.farimod.app.screens.home.HomeBlogSection
import ir.farimod.app.screens.home.HomeCoursesSection
import ir.farimod.app.screens.home.HomeGallerySection
import ir.farimod.app.screens.home.HomeJobSlider
import ir.farimod.app.screens.home.HomeMemberGroupSection
import ir.farimod.app.screens.home.HomeMembersSection
import ir.farimod.app.screens.home.HomePortfoliosSection
import ir.farimod.app.screens.home.HomeProductsSection
import ir.farimod.app.screens.home.HomeSlides
import ir.farimod.app.state.AppUpdateState
import ir.farimod.app.state.AuthState
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LocalImage
import ir.farimod.app.ui.MainBackground
import ir.farimod.app.ui.MenuModal
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.ResizeMode
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.ToastData
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import kotlinx.coroutines.delay

private data class HomeSectionVisibility(
    val memberGroup: Boolean = false,
    val courses: Boolean = false,
    val products: Boolean = false,
    val blog: Boolean = false,
    val portfolios: Boolean = false,
    val gallery: Boolean = false,
    val members: Boolean = false
)

@Composable
fun HomeScreen() {
    val navigation = useNavigation()
    val toastHolder = remember { mutableStateOf(ToastData()) }
    var toast by toastHolder
    val showToast = remember<(String, String) -> Unit> {
        { message, type -> toastHolder.value = ToastData(visible = true, message = message, type = type) }
    }
    var showMenu by remember { mutableStateOf(false) }
    var sections by remember { mutableStateOf(HomeSectionVisibility()) }
    val statusTop = statusBarHeight()

    DisposableEffect(Unit) {
        AppUpdateState.setHomeScreenStatus(true)
        onDispose { AppUpdateState.setHomeScreenStatus(false) }
    }

    LaunchedEffect(Unit) {
        delay(300)
        sections = sections.copy(memberGroup = true)
        delay(300)
        sections = sections.copy(courses = true)
        delay(300)
        sections = sections.copy(products = true)
        delay(300)
        sections = sections.copy(blog = true)
        delay(300)
        sections = sections.copy(portfolios = true)
        delay(300)
        sections = sections.copy(gallery = true)
        delay(300)
        sections = sections.copy(members = true)
    }

    Box(Modifier.fillMaxSize()) {
        MainBackground()

        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = statusTop + 8.dp, bottom = 18.dp)
                    .absolutePadding(left = 64.dp, right = 64.dp),
                contentAlignment = Alignment.Center
            ) {
                AppText(
                    modifier = Modifier.fillMaxWidth().absolutePadding(left = 56.dp, right = 56.dp),
                    text = "فریمد",
                    fontSize = 22.sp,
                    fontFamily = RNFonts.Yekan_Bakh_ExtraBold,
                    color = Color(0xFF2C3E50),
                    textAlign = TextAlign.Center
                ,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                HomeSlides(showToast)
                if (sections.memberGroup) HomeMemberGroupSection(showToast)
                if (sections.courses) HomeCoursesSection(showToast)
                if (sections.products) HomeProductsSection(showToast)
                if (sections.blog) HomeBlogSection(showToast)
                if (sections.portfolios) HomePortfoliosSection(showToast)
                if (sections.gallery) HomeGallerySection(showToast)
                if (sections.members) HomeMembersSection(showToast)
                HomeJobSlider()
            }
        }

        Toast(
            visible = toast.visible,
            message = toast.message,
            type = toast.type,
            onHide = { toast = toast.copy(visible = false) }
        )

        Box(Modifier.fillMaxSize().zIndex(1000f)) {
            Box(
                Modifier
                    .align(AbsoluteAlignment.TopRight)
                    .absoluteOffset(x = (-16).dp, y = statusTop + 4.dp)
                    .size(44.dp)
                    .touchableOpacity { }
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                LocalImage(
                    Res.drawable.main_icon,
                    Modifier.fillMaxSize().padding(10.dp),
                    ResizeMode.cover
                )
            }
            Box(
                Modifier
                    .align(AbsoluteAlignment.TopLeft)
                    .absoluteOffset(x = 16.dp, y = statusTop + 4.dp)
                    .size(44.dp)
                    .touchableOpacity { showMenu = true }
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                MaterialIcons(name = "menu", size = 24.dp, color = Color(0xFF6366F1))
            }
        }

        MenuModal(
            visible = showMenu,
            onClose = { showMenu = false },
            onNavigate = { screen ->
                if (screen == "LOGOUT") {
                    try {
                        AuthState.logout()
                        showToast("با موفقیت خارج شدید", "success")
                        navigation.reset("Login")
                    } catch (_: Exception) {
                        showToast("خطا در خروج از حساب کاربری", "error")
                    }
                } else {
                    navigation.navigate(screen)
                }
            },
            showToast = showToast
        )
    }
}
