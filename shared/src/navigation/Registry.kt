package ir.farimod.app.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import ir.farimod.app.screens.*

/**
 * Maps RN route names (AppNavigator.tsx / AuthAndIntroNavigator.tsx) to their screen composables.
 * Every screen reads its params through `useRoute()` and navigates with `useNavigation()`.
 */
@Composable
fun RenderRoute(route: Route) {
    when (route.name) {
        // Intro + Auth stacks
        "WelcomeIntro" -> WelcomeIntroScreen()
        "WhyFrimod" -> WhyFrimodScreen()
        "EventsScreen" -> EventsScreen()
        "CareerScreen" -> CareerScreen()
        "Login" -> LoginScreen()
        "OTP" -> OTPScreen()
        "Signup" -> SignupScreen()

        // Tabs / home stack
        "HomeScreen" -> HomeScreen()
        "MagScreen" -> MagScreen()

        // App stack
        "AddPortfolio" -> AddPortfolioScreen()
        "AddGallery" -> AddGalleryScreen()
        "AllMemberGroups" -> AllMemberGroupsScreen()
        "SubscriptionPurchase" -> SubscriptionPurchaseScreen()
        "AddProduct" -> AddProductScreen()
        "CourseStudents" -> CourseStudentsScreen()
        "CourseRegistration" -> CourseRegistrationScreen()
        "MyProduct" -> MyProductScreen()
        "AllPortfolio" -> AllPortfolioScreen()
        "EditProfile" -> EditProfileScreen()
        "AboutMe" -> AboutMeScreen()
        "EditContactInfo" -> EditContactInfoScreen()
        "ManageGalleryItems" -> ManageGalleryItemsScreen()
        "MyResume" -> MyResumeScreen()
        "MyGallery" -> MyGalleryScreen()
        "AllGalleries" -> AllGalleriesScreen()
        "MyPosts" -> MyPostsScreen()
        "AddNewPost" -> AddNewPostScreen()
        "PortfolioDetail" -> PortfolioDetailScreen()
        "PortfolioList" -> PortfolioListScreen()
        "MyCourses" -> MyCoursesScreen()
        "AddNewCourse" -> AddNewCourseScreen()
        "AllCourses" -> AllCoursesScreen()
        "CourseDetails" -> CourseDetailsScreen()
        "MyRegistrations" -> MyRegistrationsScreen()
        "MyTeachingCourses" -> MyTeachingCoursesScreen()
        "ProductDetails" -> ProductDetailsScreen()
        "AllProducts" -> AllProductsScreen()
        "AllMembers" -> AllMembersScreen()
        "Subscription" -> SubscriptionScreen()
        "UserProfile" -> UserProfileScreen()
        "MagDetailes" -> MagDetailesScreen()
        "GalleryItem" -> GalleryItemScreen()
        else -> Text("Unknown route: ${route.name}")
    }
}
