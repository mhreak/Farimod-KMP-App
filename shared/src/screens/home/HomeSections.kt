package ir.farimod.app.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ir.farimod.app.hooks.useBlogPosts
import ir.farimod.app.hooks.useFetchData
import ir.farimod.app.hooks.useImageGalleries
import ir.farimod.app.hooks.useMemberGroups
import ir.farimod.app.hooks.usePortfolios
import ir.farimod.app.navigation.useFocusEffect
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.screens.mag.MagBlogEventBus
import ir.farimod.app.network.str
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.CourseCard

@Composable
private fun useHomeMembers() = useFetchData("Member/GetAllLast", "خطا در دریافت اطلاعات")

@Composable
private fun useHomeProducts() = useFetchData("Product/GetAllLast", "خطا در دریافت اطلاعات")

@Composable
private fun useHomeCourses() = useFetchData("Course/GetAllLast", "خطا در دریافت اطلاعات")

@Composable
fun HomeMemberGroupSection(showToast: (String, String) -> Unit) {
    val navigation = useNavigation()
    val state = useMemberGroups()
    val items = state.data.asReversed()
    HomeSectionHeader(
        label = "گروه‌های اصلی",
        dotColor = Color(0xFF00BCD4),
        loading = state.loading,
        hasError = state.error != null,
        onViewAll = {
            homeTryNavigate(showToast, "خطا در باز کردن لیست گروه‌ها") {
                navigation.navigate("AllMemberGroups")
            }
        }
    )
    when {
        state.error != null -> HomeErrorBlock("خطا در دریافت اطلاعات گروه‌ها", state.refetch)
        state.loading -> HomeCardRow(modifier = Modifier.fillMaxWidth().height(140.dp), itemSpacing = 0.dp) {
            items(3) { HomeMemberGroupCardSkeleton() }
        }
        items.isEmpty() -> HomeEmptyBlock("group", "هیچ گروهی موجود نیست", 140.dp)
        else -> HomeCardRow(itemSpacing = 0.dp) {
            itemsIndexed(items, key = { index, group -> "mg-${group.homeInt("MemberGroupId")}-$index" }) { _, group ->
                HomeMemberGroupCard(group) { item ->
                    homeTryNavigate(showToast, "خطا در باز کردن گروه") {
                        navigation.navigate(
                            "AllMembers",
                            mapOf<String, Any?>(
                                "filterGroupId" to item.homeInt("MemberGroupId"),
                                "filterGroupName" to item.str("GroupName")
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeCoursesSection(showToast: (String, String) -> Unit) {
    val navigation = useNavigation()
    val state = useHomeCourses()
    val items = state.data.asReversed()
    LaunchedEffect(state.error) {
        if (state.error != null) showToast("خطا در دریافت اطلاعات دوره‌ها. لطفاً دوباره تلاش کنید.", "error")
    }
    HomeSectionHeader(
        label = "جدیدترین دوره ها",
        dotColor = Color(0xFFFFD700),
        loading = state.loading,
        hasError = state.error != null,
        onViewAll = { navigation.navigate("AllCourses") }
    )
    when {
        state.error != null -> HomeErrorBlock("خطا در دریافت اطلاعات دوره‌ها", state.refetch)
        state.loading -> HomeSizedCards(modifier = Modifier.padding(bottom = 12.dp), count = 2) { _ ->
            HomeCourseCardSkeleton()
        }
        items.isEmpty() -> HomeEmptyBlock("school", "هیچ دوره‌ای موجود نیست", 220.dp)
        else -> HomeSizedCards(modifier = Modifier.padding(bottom = 12.dp), count = items.size) { index ->
            val course = items[index]
            CourseCard(course, onPress = { data ->
                homeTryNavigate(showToast, "خطا در باز کردن جزئیات دوره") {
                    navigation.navigate("CourseDetails", mapOf<String, Any?>("courseData" to data))
                }
            })
        }
    }
}

@Composable
fun HomeProductsSection(showToast: (String, String) -> Unit) {
    val navigation = useNavigation()
    val state = useHomeProducts()
    val items = state.data.asReversed()
    LaunchedEffect(state.error) {
        if (state.error != null) showToast("خطا در دریافت اطلاعات محصولات. لطفاً دوباره تلاش کنید.", "error")
    }
    HomeSectionHeader(
        label = "جدیدترین محصولات",
        dotColor = FarimodColors.Primary,
        loading = state.loading,
        hasError = state.error != null,
        onViewAll = { navigation.navigate("AllProducts") }
    )
    when {
        state.error != null -> HomeErrorBlock("خطا در دریافت اطلاعات محصولات", state.refetch)
        state.loading -> HomeSizedCards(modifier = Modifier.padding(bottom = 12.dp), count = 2) { _ ->
            HomeProductCardSkeleton()
        }
        items.isEmpty() -> Unit
        else -> HomeSizedCards(modifier = Modifier.padding(bottom = 12.dp), count = items.size) { index ->
            val product = items[index]
            HomeProductCard(product) { item ->
                homeTryNavigate(showToast, "خطا در باز کردن جزئیات محصول") {
                    navigation.navigate(
                        "ProductDetails",
                        mapOf<String, Any?>(
                            "productData" to item,
                            "productId" to item.homeInt("ProductId")
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun HomeBlogSection(showToast: (String, String) -> Unit) {
    val navigation = useNavigation()
    val state = useBlogPosts()
    val posts = state.data.asReversed()

    useFocusEffect {
        state.refetch()
    }

    LaunchedEffect(MagBlogEventBus.lastLikedBlogPostId, MagBlogEventBus.lastLikeCount) {
        if (MagBlogEventBus.lastLikedBlogPostId != null) {
            state.refetch()
        }
    }

    LaunchedEffect(state.error) {
        if (state.error != null) showToast("خطا در دریافت اطلاعات مقالات. لطفاً دوباره تلاش کنید.", "error")
    }
    HomeSectionHeader(
        label = "جدیدترین مقالات",
        dotColor = FarimodColors.Primary,
        loading = state.loading,
        hasError = state.error != null,
        onViewAll = { navigation.navigate("وبلاگ") }
    )
    when {
        state.error != null -> HomeErrorBlock("خطا در دریافت اطلاعات مقالات", state.refetch)
        state.loading -> HomeSizedCards(
            modifier = Modifier.padding(bottom = 12.dp),
            visibleCards = 1.08f,
            count = 1
        ) { _ -> HomeBlogCardSkeleton() }
        posts.isEmpty() -> HomeEmptyBlock("article", "هیچ مقاله‌ای موجود نیست", 220.dp)
        else -> HomeSizedCards(
            modifier = Modifier.padding(bottom = 12.dp),
            visibleCards = 1.08f,
            count = posts.size
        ) { index ->
            val post = posts[index]
            HomeBlogCard(post) {
                homeTryNavigate(showToast, "خطا در باز کردن مقاله") {
                    navigation.navigate(
                        "MagDetailes",
                        mapOf<String, Any?>(
                            "title" to it.str("Title"),
                            "blogId" to it.homeInt("BlogPostId")
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun HomePortfoliosSection(showToast: (String, String) -> Unit) {
    val navigation = useNavigation()
    val state = usePortfolios()
    val items = state.data.asReversed()
    LaunchedEffect(state.error) {
        if (state.error != null) showToast("خطا در دریافت اطلاعات نمونه کارها. لطفاً دوباره تلاش کنید.", "error")
    }
    HomeSectionHeader(
        label = "جدیدترین نمونه کارها",
        dotColor = Color(0xFF9370DB),
        loading = state.loading,
        hasError = state.error != null,
        onViewAll = { navigation.navigate("AllPortfolio") }
    )
    when {
        state.error != null -> HomeErrorBlock("خطا در دریافت اطلاعات نمونه کارها", state.refetch)
        state.loading -> HomeSizedCards(modifier = Modifier.padding(bottom = 12.dp), count = 2) { _ ->
            HomePortfolioCardSkeleton()
        }
        items.isEmpty() -> HomeEmptyBlock("brush", "هیچ نمونه کاری موجود نیست", 180.dp)
        else -> HomeSizedCards(modifier = Modifier.padding(bottom = 12.dp), count = items.size) { index ->
            val portfolio = items[index]
            HomePortfolioCard(portfolio) { item ->
                val portfolioId = item.homeIdOr("PortfolioId", "PotfolioId")
                if (portfolioId == 0) {
                    showToast("خطا: شناسه نمونه کار نامعتبر است", "error")
                } else {
                    homeTryNavigate(showToast, "خطا در باز کردن نمونه کار") {
                        navigation.navigate(
                            "PortfolioDetail",
                            mapOf<String, Any?>(
                                "title" to item.str("Title"),
                                "portfolioId" to portfolioId
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeGallerySection(showToast: (String, String) -> Unit) {
    val navigation = useNavigation()
    val state = useImageGalleries()
    val items = state.data.asReversed()
    LaunchedEffect(state.error) {
        if (state.error != null) showToast("خطا در دریافت اطلاعات گالری‌ها. لطفاً دوباره تلاش کنید.", "error")
    }
    HomeSectionHeader(
        label = "جدیدترین گالری‌ها",
        dotColor = Color(0xFF9C27B0),
        loading = state.loading,
        hasError = state.error != null,
        onViewAll = { navigation.navigate("AllGalleries") }
    )
    when {
        state.error != null -> HomeErrorBlock("خطا در دریافت اطلاعات گالری‌ها", state.refetch)
        state.loading -> HomeSizedCards(modifier = Modifier.padding(bottom = 8.dp), count = 2) { _ ->
            HomeGalleryCardSkeleton()
        }
        items.isEmpty() -> HomeEmptyBlock("photo-library", "هیچ گالری‌ای موجود نیست", 180.dp)
        else -> HomeSizedCards(modifier = Modifier.padding(bottom = 8.dp), count = items.size) { index ->
            val gallery = items[index]
            HomeGalleryCard(gallery) { item ->
                homeTryNavigate(showToast, "خطا در باز کردن گالری") {
                    navigation.navigate(
                        "GalleryItem",
                        mapOf<String, Any?>(
                            "title" to item.str("Title"),
                            "galleryId" to item.homeInt("ImageGalleryId"),
                            "galleryData" to item
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun HomeMembersSection(showToast: (String, String) -> Unit) {
    val navigation = useNavigation()
    val state = useHomeMembers()
    val items = state.data
    LaunchedEffect(state.error) {
        if (state.error != null) showToast("خطا در دریافت اطلاعات اعضا. لطفاً دوباره تلاش کنید.", "error")
    }
    HomeSectionHeader(
        label = "اعضای جدید",
        dotColor = Color(0xFFFF69B4),
        loading = state.loading,
        hasError = state.error != null,
        onViewAll = { navigation.navigate("AllMembers") }
    )
    when {
        state.error != null -> HomeErrorBlock("خطا در دریافت اطلاعات گالری‌ها", state.refetch)
        state.loading -> HomeCardRow(itemSpacing = 0.dp) {
            items(3) { HomeAvatarSkeleton() }
        }
        items.isNotEmpty() -> HomeCardRow(itemSpacing = 0.dp) {
            itemsIndexed(items, key = { index, member -> "member-${member.homeInt("MemberId")}-$index" }) { _, member ->
                HomeAvatar(member, size = 100.dp) {
                    homeTryNavigate(showToast, "خطا در باز کردن پروفایل کاربر") {
                        navigation.navigate("UserProfile", mapOf<String, Any?>("userData" to member))
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSizedCards(
    modifier: Modifier = Modifier,
    visibleCards: Float = 2.05f,
    count: Int,
    card: @Composable (index: Int) -> Unit
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val cardWidth = (maxWidth - 28.dp) / visibleCards
        HomeCardRow(itemSpacing = 12.dp, contentPaddingHorizontal = 2.dp) {
            items(count) { index ->
                Box(Modifier.width(cardWidth)) {
                    card(index)
                }
            }
        }
    }
}
