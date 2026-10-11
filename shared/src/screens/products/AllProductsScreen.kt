package ir.farimod.app.screens

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.absolutePadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
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
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.FilterModal
import ir.farimod.app.ui.FilterOptions
import ir.farimod.app.ui.FilterSection
import ir.farimod.app.ui.MainBackground
import ir.farimod.app.ui.PickerOption
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.ToastData
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import ir.farimod.app.ui.windowSize
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.navigation.useRoute
import ir.farimod.app.utils.jsToString
import ir.farimod.app.utils.jsTruthy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllProductsScreen() {
    val navigation = useNavigation()
    val route = useRoute()
    val scope = rememberCoroutineScope()
    val pullState = rememberPullToRefreshState()

    val filteredMemberId = route.params["filteredMemberId"]
    val filteredMemberName = route.params["filteredMemberName"]
    val filterType = route.str("filterType")

    var products by remember { mutableStateOf<List<JsonElement>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var productsLoading by remember { mutableStateOf(false) }
    var productsError by remember { mutableStateOf<String?>(null) }
    var requestGen by remember { mutableStateOf(0) }

    var currentPage by remember { mutableStateOf(1) }
    val totalPages = if (total <= 0) 0 else ceil(total / PRODUCTS_PAGE_SIZE.toDouble()).toInt()

    var filterModalVisible by remember { mutableStateOf(false) }
    var appliedFilters by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }
    var hasActiveFilters by remember { mutableStateOf(false) }
    var categoryOptions by remember { mutableStateOf<List<PickerOption>>(emptyList()) }
    var toast by remember { mutableStateOf(ToastData()) }
    var refreshing by remember { mutableStateOf(false) }

    fun showToast(message: String, type: String = "info") {
        toast = ToastData(true, message, type)
    }

    suspend fun fetchProducts(page: Int, pageSize: Int, filters: Map<String, Any?>) {
        val gen = ++requestGen
        productsLoading = true
        productsError = null
        try {
            val query = buildMap<String, Any?> {
                put("filterActive", true)
                put("currentPage", page)
                put("pageSize", pageSize)
                val name = filters["filterProductName"]
                if (jsTruthy(name)) put("filterProductName", jsToString(name))
                val categoryId = filters["filterProductCategoryId"]
                if (jsTruthy(categoryId)) put("filterProductCategoryId", jsToString(categoryId))
                val memberId = filters["filterMemberId"]
                if (jsTruthy(memberId)) put("filterMemberId", jsToString(memberId))
            }
            val response = Api.get("Product/GetAll", query = query)
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

    val productFilterOptions = remember(categoryOptions) {
        FilterOptions(
            title = "فیلتر محصولات",
            icon = "filter-list",
            sections = listOf(FilterSection(key = "categoryId", options = categoryOptions))
        )
    }

    LaunchedEffect(Unit) {
        try {
            val response = Api.get(
                "ProductCategory/GetAll",
                query = mapOf("filterActive" to true, "currentPage" to 1, "pageSize" to 100)
            )
            val options = response.json["Data"].list.map { cat ->
                PickerOption(
                    label = cat.str("Name").orEmpty(),
                    value = cat.long("ProductCategoryId") ?: cat.str("ProductCategoryId")
                )
            }
            categoryOptions = options
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(filteredMemberId, filterType, filteredMemberName) {
        if (jsTruthy(filteredMemberId) && filterType == "member") {
            appliedFilters = mapOf("filterMemberId" to filteredMemberId)
            hasActiveFilters = true
            showToast("نمایش محصولات ${jsToString(filteredMemberName)}", "info")
        }
    }

    LaunchedEffect(currentPage, appliedFilters) {
        fetchProducts(currentPage, PRODUCTS_PAGE_SIZE, appliedFilters)
    }

    LaunchedEffect(productsError) {
        if (productsError != null) {
            showToast("خطا در دریافت اطلاعات محصولات. لطفاً دوباره تلاش کنید.", "error")
        }
    }

    fun handleApplyFilters(filters: Map<String, Any?>) {
        appliedFilters = filters
        currentPage = 1
        val hasFilters = filters.keys.any { key ->
            val value = filters[key]
            value != false && value != "" && value != "all" && value != null
        }
        hasActiveFilters = hasFilters
        if (hasFilters) showToast("فیلترها اعمال شد", "success")
    }

    fun clearAllFilters() {
        appliedFilters = emptyMap()
        hasActiveFilters = false
        currentPage = 1
        showToast("فیلترها پاک شد", "info")
    }

    val screenWidth = windowSize().first
    val itemWidth = (screenWidth - 50.dp) / 2
    val showSkeletons = productsLoading && products.isEmpty()
    val rows: List<List<JsonElement?>> = if (showSkeletons) {
        List(PRODUCTS_PAGE_SIZE) { null }.chunked(2)
    } else {
        products.chunked(2)
    }
    val title = if (jsTruthy(filteredMemberId) && jsTruthy(filteredMemberName)) {
        "محصولات ${jsToString(filteredMemberName)}"
    } else {
        "محصولات"
    }
    val statusTop = statusBarHeight()

    Box(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        MainBackground()
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = statusTop + 35.dp)
                    .absolutePadding(left = 20.dp, right = 20.dp)
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AppText(
                    modifier = Modifier.fillMaxWidth().absolutePadding(left = 56.dp, right = 56.dp),
                        text = title,
                        fontSize = 22.sp,
                        fontFamily = RNFonts.Yekan_Bakh_ExtraBold,
                        color = Color(0xFF2C3E50),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis)
                }
                Box(
                    Modifier
                        .align(AbsoluteAlignment.CenterLeft)
                        .size(50.dp)
                        .rnShadow(5.dp, CircleShape)
                        .clip(CircleShape)
                        .background(if (hasActiveFilters) ProductsPrimary else Color(1f, 1f, 1f, 0.9f))
                        .touchableOpacity { filterModalVisible = true },
                    contentAlignment = Alignment.Center
                ) {
                    MaterialIcons(
                        "filter-list",
                        size = 24.dp,
                        color = if (hasActiveFilters) Color.White else ProductsPrimary
                    )
                    if (hasActiveFilters) {
                        Box(
                            Modifier
                                .align(AbsoluteAlignment.TopRight)
                                .absolutePadding(top = 8.dp, right = 8.dp)
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF6B6B))
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }
                }
                if (hasActiveFilters) {
                    Box(
                        Modifier
                            .align(AbsoluteAlignment.CenterRight)
                            .size(40.dp)
                            .rnShadow(5.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color(1f, 1f, 1f, 0.9f))
                            .touchableOpacity { clearAllFilters() },
                        contentAlignment = Alignment.Center
                    ) {
                        MaterialIcons("clear", size = 20.dp, color = Color(0xFFFF6B6B))
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (productsError != null) {
                    ProductsErrorState(onRetry = {
                        scope.launch { fetchProducts(currentPage, PRODUCTS_PAGE_SIZE, appliedFilters) }
                    })
                } else {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                        PullToRefreshBox(
                            isRefreshing = refreshing,
                            onRefresh = {
                                scope.launch {
                                    refreshing = true
                                    fetchProducts(currentPage, PRODUCTS_PAGE_SIZE, appliedFilters)
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
                                                title = "هیچ محصولی موجود نیست",
                                                subtitle = "در حال حاضر محصولی برای نمایش وجود ندارد"
                                            )
                                        }
                                    }
                                } else {
                                    itemsIndexed(rows, key = { index, row ->
                                        if (showSkeletons) "skeleton-$index" else row.joinToString("-") { cell ->
                                            cell?.let { it.str("ProductId") ?: it.long("ProductId")?.toString() } ?: "product-$index"
                                        }
                                    }) { index, row ->
                                        Column(Modifier.fillMaxWidth()) {
                                            Row(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 8.dp),
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                row.forEach { cell ->
                                                    Box(
                                                        Modifier
                                                            .absolutePadding(left = 6.dp, right = 6.dp)
                                                            .padding(bottom = 10.dp)
                                                            .width(itemWidth)
                                                    ) {
                                                        if (cell == null) {
                                                            ProductsCardSkeleton()
                                                        } else {
                                                            ProductsProductCard(item = cell, onPress = { product ->
                                                                navigation.navigate("ProductDetails", mapOf("productData" to product))
                                                            })
                                                        }
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
                                onPageChange = { currentPage = it }
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
                .absoluteOffset(x = (-20).dp, y = statusTop + 45.dp),
            onClick = { navigation.goBack() }
        ) {
            MaterialIcons("arrow-forward", size = 24.dp, color = Color(0xFF6366F1))
        }

        FilterModal(
            visible = filterModalVisible,
            onClose = { filterModalVisible = false },
            onApplyFilters = { handleApplyFilters(it) },
            filterType = "products",
            initialFilters = appliedFilters,
            customFilterOptions = productFilterOptions
        )
        Toast(toast.visible, toast.message, toast.type) { toast = toast.copy(visible = false) }
    }
}
