package ir.farimod.app.screens

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.navigation.Tabs
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.network.Api
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.list
import ir.farimod.app.network.long
import ir.farimod.app.network.str
import ir.farimod.app.screens.products.PRODUCTS_PAGE_SIZE
import ir.farimod.app.screens.products.ProductsCardSkeleton
import ir.farimod.app.screens.products.ProductsCircleButton
import ir.farimod.app.screens.products.ProductsEmptyState
import ir.farimod.app.screens.products.ProductsErrorState
import ir.farimod.app.screens.products.ProductsPagination
import ir.farimod.app.screens.products.ProductsPrimary
import ir.farimod.app.screens.products.ProductsProductCard
import ir.farimod.app.services.AuthService
import ir.farimod.app.state.AuthState
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.LinearGradient
import ir.farimod.app.ui.MainBackground
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.ToastData
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.ui.windowSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProductScreen() {
    val navigation = useNavigation()
    val scope = rememberCoroutineScope()
    val pullState = rememberPullToRefreshState()

    var products by remember { mutableStateOf<List<JsonElement>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var productsLoading by remember { mutableStateOf(false) }
    var productsError by remember { mutableStateOf<String?>(null) }
    var requestGen by remember { mutableStateOf(0) }
    var currentPage by remember { mutableStateOf(1) }
    val totalPages = if (total <= 0) 0 else ceil(total / PRODUCTS_PAGE_SIZE.toDouble()).toInt()
    var toast by remember { mutableStateOf(ToastData()) }
    var refreshing by remember { mutableStateOf(false) }

    fun showToast(message: String, type: String = "info") {
        toast = ToastData(true, message, type)
    }

    fun memberIdOrThrow(): Int {
        val userData = AuthState.user ?: AuthService.getUserData()
            ?: throw Exception("کاربر وارد نشده است")
        return userData.MemberId?.takeIf { it != 0 } ?: throw Exception("شناسه عضو یافت نشد")
    }

    suspend fun fetchProducts(page: Int, pageSize: Int) {
        val gen = ++requestGen
        productsLoading = true
        productsError = null
        try {
            val memberId = memberIdOrThrow()
            val response = Api.get(
                "Product/GetAll",
                query = mapOf(
                    "filterMemberId" to memberId,
                    "currentPage" to page,
                    "pageSize" to pageSize
                )
            )
            if (gen != requestGen) return
            if (!response.ok) throw Exception("HTTP error! status: ${response.status}")
            val result = response.json ?: throw Exception("HTTP error! status: ${response.status}")
            products = result["Data"].list
            total = result.int("Total") ?: 0
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (gen != requestGen) return
            productsError = e.message?.takeIf { it.isNotEmpty() } ?: "خطایی رخ داده است"
            products = emptyList()
            total = 0
        } finally {
            if (gen == requestGen) productsLoading = false
        }
    }

    LaunchedEffect(currentPage) {
        fetchProducts(currentPage, PRODUCTS_PAGE_SIZE)
    }

    LaunchedEffect(productsError) {
        if (productsError != null) {
            showToast("خطا در دریافت اطلاعات محصولات من. لطفاً دوباره تلاش کنید.", "error")
        }
    }

    val screenWidth = windowSize().first
    val itemWidth = (screenWidth - 50.dp) / 2
    val showSkeletons = productsLoading && products.isEmpty()
    val rows: List<List<JsonElement?>> = if (showSkeletons) {
        List(PRODUCTS_PAGE_SIZE) { null }.chunked(2)
    } else {
        products.chunked(2)
    }
    val statusTop = statusBarHeight()

    Box(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        MainBackground()
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = statusTop + 35.dp)
                    .absolutePadding(left = 20.dp, right = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                AppText(
                    modifier = Modifier.fillMaxWidth().absolutePadding(left = 56.dp, right = 56.dp),
                    text = "محصولات من",
                    fontSize = 22.sp,
                    fontFamily = RNFonts.Yekan_Bakh_ExtraBold,
                    color = Color(0xFF2C3E50),
                    textAlign = TextAlign.Center
                ,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (productsError != null) {
                    ProductsErrorState(onRetry = {
                        scope.launch { fetchProducts(currentPage, PRODUCTS_PAGE_SIZE) }
                    })
                } else {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        PullToRefreshBox(
                            isRefreshing = refreshing,
                            onRefresh = {
                                scope.launch {
                                    refreshing = true
                                    fetchProducts(currentPage, PRODUCTS_PAGE_SIZE)
                                    refreshing = false
                                }
                            },
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            state = pullState,
                            indicator = {
                                PullToRefreshDefaults.Indicator(
                                    state = pullState,
                                    isRefreshing = refreshing,
                                    modifier = Modifier.align(Alignment.TopCenter),
                                    containerColor = Color.White,
                                    color = ProductsPrimary
                                )
                            }
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 8.dp, top = 10.dp, end = 8.dp, bottom = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (!showSkeletons && products.isEmpty() && !productsLoading) {
                                    item(key = "empty") {
                                        Box(Modifier.fillMaxHeight().fillMaxWidth()) {
                                            ProductsEmptyState(
                                                title = "هیچ محصولی فعال نیست",
                                                subtitle = "در حال حاضر محصولی برای نمایش وجود ندارد"
                                            )
                                        }
                                    }
                                } else {
                                    itemsIndexed(rows, key = { index, row ->
                                        if (showSkeletons) "skeleton-$index"
                                        else "row-$index-" + (row.firstOrNull()?.str("ProductId")
                                            ?: row.firstOrNull()?.long("ProductId")?.toString()
                                            ?: "x")
                                    }) { index, row ->
                                        Column(Modifier.fillMaxWidth()) {
                                            Row(
                                                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                row.forEach { cell ->
                                                    Box(
                                                        Modifier
                                                            .absolutePadding(left = 6.dp, right = 6.dp)
                                                            .padding(bottom = 10.dp)
                                                            .width(itemWidth)
                                                    ) {
                                                        if (cell == null) ProductsCardSkeleton()
                                                        else ProductsProductCard(item = cell, onPress = { product ->
                                                            navigation.navigate(
                                                                "ProductDetails",
                                                                mapOf("productData" to product)
                                                            )
                                                        })
                                                    }
                                                }
                                            }
                                            if (index < rows.lastIndex) Spacer(Modifier.height(4.dp))
                                        }
                                    }
                                }
                            }
                        }
                        if (!productsLoading && productsError == null && totalPages > 1) {
                            ProductsPagination(
                                currentPage = currentPage,
                                totalPages = totalPages,
                                onPageChange = { currentPage = it },
                                destinationScale = true
                            )
                        }
                    }
                }
            }
        }

        ProductsCircleButton(
            modifier = Modifier
                .align(AbsoluteAlignment.TopRight)
                .zIndex(1000f)
                .absoluteOffset(x = (-20).dp, y = statusTop + 33.dp),
            onClick = { navigation.goBack() }
        ) {
            MaterialIcons("arrow-forward", size = 24.dp, color = Color(0xFF6366F1))
        }

        Box(
            Modifier
                .align(AbsoluteAlignment.TopLeft)
                .zIndex(1000f)
                .absoluteOffset(x = 20.dp, y = statusTop + 33.dp)
                .size(44.dp)
                .touchableOpacity { navigation.navigate("AddProduct") }
        ) {
            LinearGradient(
                colors = listOf(Color(0xFF2ECC71), Color(0xFF27AE60)),
                modifier = Modifier
                    .fillMaxSize()
                    .rnShadow(8.dp, CircleShape, Color(0xFF2ECC71)),
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
