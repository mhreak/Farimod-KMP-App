package ir.farimod.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.navigation.Tabs
import ir.farimod.app.navigation.useFocusEffect
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.network.bool
import ir.farimod.app.network.long
import ir.farimod.app.network.str
import ir.farimod.app.screens.myposts.BlogPostCardSkeleton
import ir.farimod.app.screens.myposts.BlogPostImage
import ir.farimod.app.screens.myposts.MyPostsPager
import ir.farimod.app.screens.myposts.POSTS_PAGE_SIZE
import ir.farimod.app.state.AuthState
import ir.farimod.app.theme.FarimodColors
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.MainBackground
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.ToastData
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.ui.windowSize
import ir.farimod.app.utils.toPersianDigits
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject

private val PostsPrimary = Color(0xFF667EEA)
private val PostsSuccess = Color(0xFF2ECC71)
private val PostsWarning = Color(0xFFF39C12)
private val CardShape = RoundedCornerShape(20.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPostsScreen() {
    val navigation = useNavigation()
    val pager = remember { MyPostsPager() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val pullState = rememberPullToRefreshState()
    val top = statusBarHeight()
    val (screenW, _) = windowSize()
    val cardWidth = screenW - 40.dp

    var toast by remember { mutableStateOf(ToastData()) }
    var refreshing by remember { mutableStateOf(false) }

    val memberId = AuthState.user?.MemberId?.toString() ?: "undefined"

    fun showToast(message: String, type: String = "info") {
        toast = ToastData(visible = true, message = message, type = type)
    }

    useFocusEffect(memberId) {
        pager.fetch(memberId, 1, POSTS_PAGE_SIZE)
    }

    LaunchedEffect(pager.error) {
        if (pager.error != null) {
            showToast("خطا در دریافت اطلاعات پست‌ها. لطفاً دوباره تلاش کنید.", "error")
        }
    }

    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val visible = info.visibleItemsInfo
            if (visible.isEmpty() || info.totalItemsCount == 0) false
            else {
                val last = visible.last().index
                val threshold = (visible.size * 0.3f).toInt().coerceAtLeast(1)
                last >= info.totalItemsCount - 1 - threshold
            }
        }
    }
    LaunchedEffect(nearEnd, pager.loading, pager.hasMore, pager.page, pager.items.size) {
        if (nearEnd && !pager.loading && pager.hasMore && pager.items.isNotEmpty()) {
            pager.fetch(memberId, pager.page + 1, POSTS_PAGE_SIZE)
        }
    }

    val showSkeletons = pager.loading && pager.items.isEmpty() && pager.error == null

    Box(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        MainBackground()

        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = top + 35.dp)
                    .absolutePadding(left = 20.dp, right = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppText(
                    text = "وبلاگ من",
                    modifier = Modifier.fillMaxWidth().absolutePadding(left = 56.dp, right = 56.dp),
                    fontSize = 22.sp,
                    fontFamily = RNFonts.Yekan_Bakh_ExtraBold,
                    color = Color(0xFF2C3E50),
                    textAlign = TextAlign.Center
                ,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                if (pager.error != null) {
                    PostsErrorState(onRetry = {
                        scope.launch { pager.fetch(memberId, 1, POSTS_PAGE_SIZE) }
                    })
                } else {
                    PullToRefreshBox(
                        isRefreshing = refreshing,
                        onRefresh = {
                            scope.launch {
                                refreshing = true
                                pager.fetch(memberId, 1, POSTS_PAGE_SIZE)
                                refreshing = false
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        state = pullState,
                        indicator = {
                            PullToRefreshDefaults.Indicator(
                                state = pullState,
                                isRefreshing = refreshing,
                                modifier = Modifier.align(Alignment.TopCenter),
                                containerColor = Color.White,
                                color = PostsPrimary
                            )
                        }
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (!showSkeletons && pager.items.isEmpty() && !pager.loading) {
                                item(key = "empty") {
                                    Box(Modifier.fillMaxHeight().width(cardWidth)) {
                                        PostsEmptyState(onCreate = { navigation.navigate("AddNewPost") })
                                    }
                                }
                            } else if (showSkeletons) {
                                items(POSTS_PAGE_SIZE, key = { "skel-$it" }) {
                                    Box(Modifier.width(cardWidth).padding(bottom = 20.dp)) {
                                        BlogPostCardSkeleton()
                                    }
                                }
                            } else {
                                itemsIndexed(pager.items, key = { index, item ->
                                    item.str("BlogPostId") ?: "post-$index"
                                }) { _, item ->
                                    Box(Modifier.width(cardWidth).padding(bottom = 20.dp)) {
                                        PostCard(item) {
                                            val blogId = item["BlogPostId"]
                                            navigation.navigate(
                                                "MagDetailes",
                                                mapOf(
                                                    "title" to item.str("Title"),
                                                    "blogId" to (blogId?.long ?: blogId?.str)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            if (pager.loading) {
                                item(key = "footer") {
                                    Row(
                                        Modifier.fillMaxWidth().padding(20.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            color = PostsPrimary,
                                            strokeWidth = 2.dp
                                        )
                                        AppText(
                                            text = "در حال بارگذاری...",
                                            modifier = Modifier.absolutePadding(left = 10.dp),
                                            fontSize = 14.sp,
                                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                                            color = Color(0xFF666666),
                                            textAlign = TextAlign.Right
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Box(
            Modifier
                .align(AbsoluteAlignment.TopRight)
                .zIndex(1000f)
                .absoluteOffset(x = (-20).dp, y = top + 33.dp)
                .size(44.dp)
                .rnShadow(5.dp, CircleShape, Color.Black)
                .background(Color.White.copy(alpha = 0.9f), CircleShape)
                .touchableOpacity { navigation.goBack() },
            contentAlignment = Alignment.Center
        ) {
            MaterialIcons("arrow-forward", size = 24.dp, color = Color(0xFF6366F1))
        }

        Box(
            Modifier
                .align(AbsoluteAlignment.TopLeft)
                .zIndex(1000f)
                .absoluteOffset(x = 20.dp, y = top + 33.dp)
                .size(44.dp)
                .rnShadow(8.dp, CircleShape, PostsSuccess)
                .clip(CircleShape)
                .touchableOpacity { navigation.navigate("AddNewPost") }
        ) {
            LinearGradient(
                colors = listOf(PostsSuccess, Color(0xFF27AE60)),
                modifier = Modifier.fillMaxSize(),
                start = Offset(0f, 0f),
                end = Offset(1f, 1f),
                shape = CircleShape
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MaterialIcons("add", size = 24.dp, color = Color.White)
                }
            }
        }

        Toast(toast.visible, toast.message, toast.type) { toast = toast.copy(visible = false) }
    }
}

@Composable
private fun PostCard(item: JsonObject, onPress: () -> Unit) {
    val active = item["Active"].bool
    Column(
        Modifier
            .fillMaxWidth()
            .rnShadow(1.dp, CardShape, Color.Black)
            .clip(CardShape)
            .background(Color.White)
            .touchableOpacity(activeOpacity = 0.8f, onClick = onPress)
    ) {
        Box(Modifier.fillMaxWidth().height(200.dp)) {
            BlogPostImage(item)
            Box(
                Modifier
                    .align(AbsoluteAlignment.TopRight)
                    .absolutePadding(top = 12.dp, right = 12.dp)
                    .background(
                        if (active) PostsSuccess else PostsWarning,
                        RoundedCornerShape(12.dp)
                    )
                    .absolutePadding(left = 8.dp, right = 8.dp, top = 4.dp, bottom = 4.dp)
            ) {
                AppText(
                    text = if (active) "منتشر شده" else "پیش نویس",
                    fontSize = 12.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
        Column(Modifier.padding(16.dp)) {
            AppText(
                text = toPersianDigits(item.str("Title") ?: ""),
                fontSize = 18.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = Color(0xFF2C3E50),
                textAlign = TextAlign.Right,
                lineHeight = 26.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            RowReverse(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                RowReverse(verticalAlignment = Alignment.CenterVertically) {
                    MaterialIcons("calendar-month", size = 16.dp, color = Color(0xFF666666))
                    AppText(
                        text = toPersianDigits(item.str("ShamsiInsertDate") ?: ""),
                        modifier = Modifier.absolutePadding(right = 6.dp),
                        fontSize = 14.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Right
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MaterialIcons("favorite", size = 16.dp, color = Color(0xFFFF6B6B))
                    AppText(
                        text = toPersianDigits(item["LikeCount"] ?: 0),
                        modifier = Modifier.absolutePadding(left = 6.dp),
                        fontSize = 14.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Right
                    )
                }
            }
        }
    }
}

@Composable
private fun PostsEmptyState(onCreate: () -> Unit) {
    Column(
        Modifier.fillMaxSize().absolutePadding(left = 40.dp, right = 40.dp, top = 60.dp, bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MaterialIcons("article", size = 80.dp, color = Color(0xFF9E9E9E))
        AppText(
            text = "هیچ پست وبلاگی موجود نیست",
            modifier = Modifier.padding(top = 20.dp),
            fontSize = 20.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center
        )
        AppText(
            text = "شما هنوز هیچ پست وبلاگی منتشر نکرده‌اید",
            modifier = Modifier.padding(top = 12.dp),
            fontSize = 16.sp,
            fontFamily = RNFonts.Yekan_Bakh_Regular,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        RowReverse(
            Modifier
                .padding(top = 24.dp)
                .rnShadow(8.dp, RoundedCornerShape(25.dp), PostsSuccess)
                .background(PostsSuccess, RoundedCornerShape(25.dp))
                .touchableOpacity(onClick = onCreate)
                .absolutePadding(left = 20.dp, right = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MaterialIcons("add", size = 20.dp, color = FarimodColors.White)
            AppText(
                text = "ایجاد اولین پست",
                modifier = Modifier.absolutePadding(right = 8.dp),
                fontSize = 16.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = FarimodColors.White
            )
        }
    }
}

@Composable
private fun PostsErrorState(onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().absolutePadding(left = 40.dp, right = 40.dp, top = 60.dp, bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        MaterialIcons("error", size = 80.dp, color = Color(0xFF9E9E9E))
        AppText(
            text = "خطا در دریافت اطلاعات",
            modifier = Modifier.padding(top = 20.dp),
            fontSize = 20.sp,
            fontFamily = RNFonts.Yekan_Bakh_Bold,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center
        )
        AppText(
            text = "لطفاً اتصال اینترنت خود را بررسی کنید",
            modifier = Modifier.padding(top = 12.dp),
            fontSize = 16.sp,
            fontFamily = RNFonts.Yekan_Bakh_Regular,
            color = Color(0xFF9E9E9E),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        RowReverse(
            Modifier
                .padding(top = 24.dp)
                .rnShadow(8.dp, RoundedCornerShape(25.dp), PostsPrimary)
                .background(PostsPrimary, RoundedCornerShape(25.dp))
                .touchableOpacity(onClick = onRetry)
                .absolutePadding(left = 20.dp, right = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MaterialIcons("refresh", size = 20.dp, color = FarimodColors.White)
            AppText(
                text = "تلاش مجدد",
                modifier = Modifier.absolutePadding(right = 8.dp),
                fontSize = 16.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = FarimodColors.White
            )
        }
    }
}
