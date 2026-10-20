package ir.farimod.app.screens

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.navigation.Tabs
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.screens.subscription.ActiveSub
import ir.farimod.app.screens.subscription.ActiveSubscriptionBanner
import ir.farimod.app.screens.subscription.SubPlan
import ir.farimod.app.screens.subscription.SubscriptionConfirmDialog
import ir.farimod.app.screens.subscription.SubscriptionPlanCard
import ir.farimod.app.screens.subscription.SubscriptionSuccessDialog
import ir.farimod.app.screens.subscription.parseActiveSub
import ir.farimod.app.screens.subscription.transformPlans
import ir.farimod.app.services.SubscriptionService
import ir.farimod.app.state.AuthState
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.MainBackground
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.ToastData
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ScreenBg = Color(0xFFF8FAFC)
private val SubPrimary = Color(0xFF667EEA)
private val SubMedium = Color(0xFF2D3748)

private suspend fun fetchPlans(): List<SubPlan> {
    val result = SubscriptionService.getAllActivePlans()
    return if (result.success) transformPlans(result.data) else emptyList()
}

private suspend fun fetchActive(memberId: Int?): ActiveSub? {
    if (memberId == null || memberId == 0) return null
    val result = SubscriptionService.getActiveSubscriptionPlan(memberId)
    return if (result.success) parseActiveSub(result.data) else null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen() {
    val navigation = useNavigation()
    val scope = rememberCoroutineScope()
    var plans by remember { mutableStateOf<List<SubPlan>>(emptyList()) }
    var plansLoading by remember { mutableStateOf(true) }
    var activeSub by remember { mutableStateOf<ActiveSub?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var successVisible by remember { mutableStateOf(false) }
    var selectedPlan by remember { mutableStateOf<SubPlan?>(null) }
    var modalType by remember { mutableStateOf("confirm") }
    var toast by remember { mutableStateOf(ToastData()) }

    fun showToast(message: String, type: String = "info") {
        toast = ToastData(visible = true, message = message, type = type)
    }

    suspend fun loadBoth() {
        coroutineScope {
            val plansJob = async { fetchPlans() }
            val activeJob = async { fetchActive(AuthState.user?.MemberId) }
            launch {
                plans = plansJob.await()
                plansLoading = false
            }
            launch { activeSub = activeJob.await() }
        }
    }

    LaunchedEffect(Unit) { loadBoth() }

    val shown = if (activeSub == null) {
        plans
    } else {
        val currentId = activeSub?.subscriptionPlanId
        plans.map { it.copy(isActive = it.id == currentId) }
    }
    val topInset = statusBarHeight()
    val pullState = rememberPullToRefreshState()
    val scrollState = rememberScrollState()
    val initialLoading = plansLoading && plans.isEmpty()

    if (initialLoading) {
        Box(Modifier.fillMaxSize().background(ScreenBg)) {
            Box(
                Modifier
                    .align(AbsoluteAlignment.TopRight)
                    .zIndex(1000f)
                    .absoluteOffset(x = (-20).dp, y = topInset + 33.dp)
                    .size(44.dp)
                    .touchableOpacity { navigation.goBack() }
                    .rnShadow(5.dp, CircleShape)
                    .background(Color.White.copy(alpha = 0.9f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                MaterialIcons(name = "arrow-forward", size = 24.dp, color = Color(0xFF6366F1))
            }
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(36.dp), color = SubPrimary, strokeWidth = 4.dp)
                AppText(
                    text = "در حال بارگذاری...",
                    fontSize = 16.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    color = SubMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
        return
    }

    Box(Modifier.fillMaxSize().background(ScreenBg)) {
        MainBackground()

        Box(
            Modifier
                .align(AbsoluteAlignment.TopRight)
                .zIndex(1000f)
                .absoluteOffset(x = (-20).dp, y = topInset + 33.dp)
                .size(44.dp)
                .touchableOpacity { navigation.goBack() }
                .rnShadow(5.dp, CircleShape)
                .background(Color.White.copy(alpha = 0.9f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MaterialIcons(name = "arrow-forward", size = 24.dp, color = Color(0xFF6366F1))
        }

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                scope.launch {
                    refreshing = true
                    coroutineScope {
                        val plansJob = async { fetchPlans() }
                        val activeJob = async { fetchActive(AuthState.user?.MemberId) }
                        plans = plansJob.await()
                        activeSub = activeJob.await()
                    }
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
                    color = SubPrimary
                )
            }
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Box(Modifier.fillMaxWidth().padding(bottom = 30.dp)) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = topInset + 35.dp, start = 20.dp, end = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AppText(
                            text = "اشتراک ها",
                            fontSize = 22.sp,
                            fontFamily = RNFonts.Yekan_Bakh_ExtraBold,
                            color = Color(0xFF2C3E50),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().absolutePadding(left = 56.dp, right = 56.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val current = activeSub
                if (current != null) {
                    ActiveSubscriptionBanner(current)
                }

                Column(
                    Modifier
                        .fillMaxWidth()
                        .absolutePadding(left = 20.dp, right = 20.dp)
                        .padding(bottom = 30.dp)
                ) {
                    if (shown.isEmpty()) {
                        Column(
                            Modifier.fillMaxWidth().absolutePadding(top = 60.dp, bottom = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            MaterialIcons(name = "info-outline", size = 60.dp, color = SubMedium)
                            AppText(
                                text = "هیچ پلن اشتراکی موجود نیست",
                                fontSize = 16.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Regular,
                                color = SubMedium,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    } else {
                        shown.forEachIndexed { index, plan ->
                            if (index > 0) Box(Modifier.height(24.dp))
                            SubscriptionPlanCard(
                                item = plan,
                                onPress = { picked ->
                                    selectedPlan = picked
                                    modalType = "confirm"
                                    confirmVisible = true
                                },
                                showToast = { message, type -> showToast(message, type) }
                            )
                        }
                    }
                }
                Box(Modifier.height(40.dp))
            }
        }

        SubscriptionConfirmDialog(
            visible = confirmVisible,
            type = modalType,
            plan = selectedPlan,
            onClose = {
                confirmVisible = false
                selectedPlan = null
            },
            onConfirm = {
                val plan = selectedPlan
                val kind = modalType
                confirmVisible = false
                scope.launch {
                    delay(300)
                    if (kind == "confirm" && plan != null) {
                        navigation.navigate(
                            "SubscriptionPurchase",
                            mapOf(
                                "plan" to plan.toJson(),
                                "selectedOption" to plan.selectedOption?.toJson()
                            )
                        )
                    } else {
                        successVisible = true
                    }
                }
            }
        )
        SubscriptionSuccessDialog(
            visible = successVisible,
            type = modalType,
            plan = selectedPlan,
            onClose = {
                successVisible = false
                selectedPlan = null
            }
        )
        Toast(toast.visible, toast.message, toast.type) { toast = toast.copy(visible = false) }
    }
}
