package ir.farimod.app.screens.profile

import androidx.compose.ui.graphics.Color

/** Port of `screens/profile/contants/ProfileItems.tsx`. */
internal data class ProfileItem(
    val id: Int,
    val title: String,
    val icon: String,
    val screenName: String,
    val color: Color,
    val permission: String?
)

internal val profileItems: List<ProfileItem> = listOf(
    ProfileItem(
        id = 1,
        title = "ویرایش پروفایل",
        icon = "info",
        screenName = "AboutMe",
        color = Color(0xFF8B5CF6),
        permission = "allowAboutMeText"
    ),
    ProfileItem(
        id = 2,
        title = " فایل ها و مدارک من",
        icon = "my-library-books",
        screenName = "MyResume",
        color = Color(0xFF10B981),
        permission = "allowAddDocument"
    ),
    ProfileItem(
        id = 3,
        title = "گالری من",
        icon = "image",
        screenName = "MyGallery",
        color = Color(0xFFF59E0B),
        permission = "allowAddImageGallery"
    ),
    ProfileItem(
        id = 4,
        title = "وبلاگ من",
        icon = "article",
        screenName = "MyPosts",
        color = Color(0xFF06B6D4),
        permission = "allowAddBlogPost"
    ),
    ProfileItem(
        id = 5,
        title = "دوره های ثبت نام شده",
        icon = "fact-check",
        screenName = "MyRegistrations",
        color = Color(0xFFEF4444),
        permission = null // همیشه در دسترس
    ),
    ProfileItem(
        id = 9,
        title = "محصولات من",
        icon = "sell",
        screenName = "MyProduct",
        color = Color(0xFF81CD4E),
        permission = "allowAddProduct"
    ),
    ProfileItem(
        id = 6,
        title = "برگزاری دوره",
        icon = "laptop-chromebook",
        screenName = "MyTeachingCourses",
        color = Color(0xFFE067C2),
        permission = "allowAddCourse"
    ),
    ProfileItem(
        id = 8,
        title = "نمونه کار های من",
        icon = "collections-bookmark",
        screenName = "PortfolioList",
        color = Color(0xFF6596FF),
        permission = "allowAddPortfolio"
    ),
    ProfileItem(
        id = 7,
        title = "اشتراک ها",
        icon = "star",
        screenName = "Subscription",
        color = Color(0xFFFFD700),
        permission = null // همیشه در دسترس
    )
)

/** `modernColors` from `screens/profile/styles/styles.tsx` (only the entries used by the profile screen). */
internal object ProfileModernColors {
    val primary = Color(0xFF6366F1)
}
