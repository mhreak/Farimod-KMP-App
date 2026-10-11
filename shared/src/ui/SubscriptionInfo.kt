package ir.farimod.app.ui

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import ir.farimod.app.components.icons.MaterialCommunityIcons
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.navigation.Tabs
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.network.Api
import ir.farimod.app.network.double
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.list
import ir.farimod.app.network.str
import ir.farimod.app.platform.isIOS
import ir.farimod.app.state.AuthState
import ir.farimod.app.utils.PersianNumberUtils
import kotlin.math.abs
import kotlin.math.round
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement

// ──────────────────────────────────────────────────────────────────────────────
// Port of `app/components/SubscriptionInfo.tsx`.
//
// NOTE: despite its name, the RN file is an (older) copy of CourseStudentsScreen: a 2-column list of
// the students registered in a course, with a flip-card details drawer and a delete confirmation modal.
// `ProfileScreeen.tsx` only imports it (it is never rendered). Props = `route.params` { courseId, courseName }.
// ──────────────────────────────────────────────────────────────────────────────

// const modernColors = {...colors, primary: "#667eea", ...}
private object SubInfoColors {
    val primary = Color(0xFF667EEA)
    val primaryLight = Color(0xFFF0F4FF)
    val dark = Color(0xFF2C3E50)
    val medium = Color(0xFF34495E)
    val light = Color(0xFFECF0F1)
    val success = Color(0xFF2ECC71)
    val warning = Color(0xFFF39C12)
    val error = Color(0xFFE74C3C)
    val gradientStart = Color(0xFF667EEA)
    val gradientEnd = Color(0xFF764BA2)
}

private const val SUB_INFO_ITEMS_PER_PAGE = 20

// toPersianDigits() of RN: empty / null → "۰"
private fun subInfoDigits(input: String?): String =
    if (input.isNullOrEmpty()) "۰" else PersianNumberUtils.toPersianDigits(input)

// Number.prototype.toLocaleString() (en-US grouping, max 3 fraction digits)
private fun subInfoLocaleString(v: Double): String {
    val neg = v < 0
    val scaled = round(abs(v) * 1000).toLong()
    val intPart = scaled / 1000
    val frac = scaled % 1000
    val grouped = intPart.toString().reversed().chunked(3).joinToString(",").reversed()
    val fracStr = if (frac == 0L) "" else "." + frac.toString().padStart(3, '0').trimEnd('0')
    return (if (neg) "-" else "") + grouped + fracStr
}

// formatPrice(price, currency = "تومان") of utils/converters.ts
private fun subInfoFormatPrice(price: JsonElement?, currency: String = "تومان"): String {
    val d = price.double ?: 0.0
    val safePrice = if (d.isNaN()) 0.0 else d
    return "${subInfoDigits(subInfoLocaleString(safePrice))} $currency"
}

private fun subInfoStateColor(state: Int?): Color = when (state) {
    1 -> SubInfoColors.warning
    2 -> SubInfoColors.success
    else -> SubInfoColors.medium
}

private fun subInfoStateIcon(state: Int?): String = when (state) {
    1 -> "pending"
    2 -> "check-circle"
    else -> "info"
}

// ── useCourseStudents(courseId) ────────────────────────────────────────────────
private class SubInfoStudents(
    private val scope: CoroutineScope,
    private val courseId: Int?
) {
    var data by mutableStateOf<List<JsonElement>>(emptyList())
    var page by mutableStateOf(1)
    var hasMore by mutableStateOf(true)
    var total by mutableStateOf(0)
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    suspend fun fetchStudents(
        newPage: Int = 1,
        pageSize: Int = SUB_INFO_ITEMS_PER_PAGE,
        isLoadMore: Boolean = false
    ) {
        val memberId = AuthState.user?.MemberId
        if (memberId == null || memberId == 0 || courseId == null || courseId == 0) {
            error = "اطلاعات کاربر یا دوره موجود نیست"
            return
        }

        try {
            loading = true
            error = null

            val response = Api.get(
                "CourseRegistration/GetRegistrationsOfCoursesOfMember",
                query = mapOf(
                    "memberId" to memberId,
                    "courseId" to courseId,
                    "currentPage" to newPage,
                    "pageSize" to pageSize
                )
            )

            if (!response.ok) {
                throw Exception("HTTP error! status: ${response.status}")
            }

            val result = response.json ?: throw Exception("JSON Parse error: Unexpected end of input")
            val items = result["Data"].list

            if (isLoadMore) {
                data = data + items
            } else {
                data = items
            }

            total = result["Total"].int ?: 0
            page = newPage
            hasMore = items.size == pageSize && items.isNotEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (err: Exception) {
            error = err.message
            if (!isLoadMore) {
                data = emptyList()
                total = 0
            }
        } finally {
            loading = false
        }
    }

    fun loadMore() {
        if (!loading && hasMore) {
            scope.launch { fetchStudents(page + 1, SUB_INFO_ITEMS_PER_PAGE, true) }
        }
    }

    fun refresh() {
        page = 1
        hasMore = true
        scope.launch { fetchStudents(1, SUB_INFO_ITEMS_PER_PAGE, false) }
    }

    suspend fun deleteStudent(courseRegistrationId: Int?): Boolean {
        val memberId = AuthState.user?.MemberId
        if (memberId == null || memberId == 0) return false

        val response = Api.put(
            "CourseRegistration/DeleteCourseRegistration",
            query = mapOf(
                "memberId" to memberId,
                "courseRegistrationId" to courseRegistrationId
            )
        )

        if (!response.ok) {
            throw Exception("خطا در حذف دانشجو")
        }

        refresh()
        return true
    }
}

// ── SkeletonLoader ─────────────────────────────────────────────────────────────
@Composable
private fun SubInfoSkeletonLoader(modifier: Modifier, height: Dp, borderRadius: Dp = 8.dp) {
    val transition = rememberInfiniteTransition()
    // 0 → 1 in 1000ms, 1 → 0 in 1000ms, looped; interpolate #e0e0e0 → #f0f0f0
    val backgroundColor by transition.animateColor(
        initialValue = Color(0xFFE0E0E0),
        targetValue = Color(0xFFF0F0F0),
        animationSpec = infiniteRepeatable(tween(1000, easing = RNTimingEasing), RepeatMode.Reverse)
    )
    Box(
        modifier
            .height(height)
            .drawBehind {
                drawRoundRect(backgroundColor, cornerRadius = CornerRadius(borderRadius.toPx(), borderRadius.toPx()))
            }
    )
}

@Composable
private fun SubInfoStudentCardSkeleton() {
    val shape = RoundedCornerShape(20.dp)
    // verticalCardSkeleton
    Column(
        Modifier
            .fillMaxWidth()
            .rnShadow(4.dp, shape)
            .background(Color(0xFFFFFFFF), shape)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SubInfoSkeletonLoader(Modifier.width(70.dp).padding(bottom = 12.dp), 70.dp, 35.dp)
        SubInfoSkeletonLoader(Modifier.fillMaxWidth(0.8f).padding(bottom = 8.dp), 16.dp, 8.dp)
        SubInfoSkeletonLoader(Modifier.fillMaxWidth(0.6f).padding(bottom = 12.dp), 14.dp, 7.dp)
        SubInfoSkeletonLoader(Modifier.fillMaxWidth().padding(bottom = 12.dp), 1.dp)
        SubInfoSkeletonLoader(Modifier.fillMaxWidth(0.9f).padding(bottom = 6.dp), 14.dp, 7.dp)
        SubInfoSkeletonLoader(Modifier.fillMaxWidth(0.7f), 14.dp, 7.dp)
    }
}

// ── Avatar ─────────────────────────────────────────────────────────────────────
private val subInfoGradientColors = listOf(
    listOf(Color(0xFFFA709A), Color(0xFFFEE140)),
    listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
    listOf(Color(0xFFF093FB), Color(0xFFF5576C)),
    listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)),
    listOf(Color(0xFF43E97B), Color(0xFF38F9D7)),
    listOf(Color(0xFFFF9A56), Color(0xFFFFAD56)),
    listOf(Color(0xFFA8EDEA), Color(0xFFFED6E3)),
    listOf(Color(0xFFFBC2EB), Color(0xFFA6C1EE))
)

@Composable
private fun SubInfoAvatar(member: JsonElement?, size: Dp = 60.dp) {
    val name = member.str("MemberName")
    val index = if (!name.isNullOrEmpty()) name.length % subInfoGradientColors.size else 0
    val selectedGradient = subInfoGradientColors[index]

    Box(
        Modifier
            .rnShadow(8.dp, CircleShape, selectedGradient[0])
            .size(size)
            .linearGradient(
                selectedGradient,
                start = Offset(0f, 0f),
                end = Offset(1f, 1f),
                shape = CircleShape
            )
            .border(3.dp, Color(0xFFFFFFFF), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        MaterialCommunityIcons("account", size = size * 0.6f, color = Color.White)
    }
}

// ── Student Drawer (with flip animation) ───────────────────────────────────────
@Composable
private fun SubInfoStudentDrawer(
    visible: Boolean,
    student: JsonElement?,
    onClose: () -> Unit,
    onDelete: (JsonElement) -> Unit
) {
    val height = windowSize().second
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val slideAnim = remember { Animatable(height.value) }
    val flipAnim = remember { Animatable(0f) }
    var showFront by remember { mutableStateOf(true) }

    LaunchedEffect(visible) {
        if (visible) {
            showFront = true
            flipAnim.snapTo(0f)
            slideAnim.animateTo(0f, rnSpring(50f, 9f))
        } else {
            slideAnim.animateTo(height.value, rnTiming(300))
        }
    }

    fun handleFlip() {
        scope.launch {
            flipAnim.animateTo(if (showFront) 180f else 0f, rnTiming(600))
            showFront = !showFront
        }
    }

    if (student == null) return
    // RN <Modal visible={visible} animationType="none"> : content is removed immediately when hidden
    if (!visible) return

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        // drawerContainer: flex 1, justifyContent flex-end
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            // drawerBackdrop
            Box(
                Modifier
                    .fillMaxSize()
                    .touchableOpacity(activeOpacity = 1f) { onClose() }
                    .background(Color(0f, 0f, 0f, 0.5f))
            )

            val sheetShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
            Column(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationY = slideAnim.value * density.density }
                    .heightIn(max = height * 0.85f)
                    .rnShadow(15.dp, sheetShape)
                    .background(Color(0xFFFFFFFF), sheetShape)
                    .padding(top = 15.dp, bottom = 35.dp)
                    .padding(horizontal = 25.dp)
            ) {
                // drawerHandle
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = 20.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .background(Color(0xFFE0E0E0), RoundedCornerShape(2.dp))
                )

                // Header
                RowReverse(
                    Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .touchableOpacity { onClose() }
                            .size(40.dp)
                            .background(Color(0xFFF8F9FA), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        MaterialIcons("close", size = 24.dp, color = SubInfoColors.dark)
                    }
                    AppText(
                        "جزئیات دانشجو",
                        fontSize = 18.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = SubInfoColors.dark
                    )
                    Box(
                        Modifier
                            .touchableOpacity { handleFlip() }
                            .size(40.dp)
                            .background(Color(0xFFF0F4FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        MaterialIcons("flip", size = 24.dp, color = SubInfoColors.primary)
                    }
                }

                // Flip Container
                val flip = flipAnim.value
                val frontOpacity = if (flip <= 90f) 1f - flip / 90f else 0f
                val backOpacity = if (flip <= 90f) 0f else (flip - 90f) / 90f
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                        .height(380.dp)
                ) {
                    // Front Side
                    Box(
                        Modifier
                            .fillMaxSize()
                            .then(if (showFront) Modifier.zIndex(1f) else Modifier)
                            .graphicsLayer {
                                rotationY = flip
                                cameraDistance = 1000f * density.density
                                alpha = frontOpacity.coerceIn(0f, 1f)
                            }
                    ) {
                        LinearGradient(
                            colors = listOf(SubInfoColors.gradientStart, SubInfoColors.gradientEnd),
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(25.dp)
                        ) {
                            Column(
                                Modifier.fillMaxSize().padding(25.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                SubInfoAvatar(student, 90.dp)
                                AppText(
                                    student.str("MemberName") ?: "",
                                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 8.dp),
                                    fontSize = 22.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                AppText(
                                    subInfoDigits(student.str("MemberMobile") ?: ""),
                                    modifier = Modifier.padding(bottom = 20.dp),
                                    fontSize = 16.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                                    color = Color(1f, 1f, 1f, 0.9f)
                                )

                                // registerInfo
                                RowReverse(
                                    Modifier
                                        .padding(bottom = 20.dp)
                                        .background(Color(1f, 1f, 1f, 0.2f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MaterialIcons("calendar-today", size = 18.dp, color = Color(1f, 1f, 1f, 0.9f))
                                    AppText(
                                        "تاریخ ثبت‌نام: " + (student.str("ShamsiRegisterDate") ?: ""),
                                        fontSize = 14.sp,
                                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                                        color = Color(1f, 1f, 1f, 0.95f)
                                    )
                                }

                                // drawerStatusBadge
                                val stateColor = subInfoStateColor(student.int("State"))
                                RowReverse(
                                    Modifier
                                        .background(stateColor.copy(alpha = 0x30 / 255f), RoundedCornerShape(25.dp))
                                        .padding(horizontal = 20.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    MaterialIcons(
                                        subInfoStateIcon(student.int("State")),
                                        size = 20.dp,
                                        color = stateColor
                                    )
                                    AppText(
                                        student.str("StateStr") ?: "",
                                        fontSize = 15.sp,
                                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                                        color = stateColor
                                    )
                                }
                            }
                        }
                    }

                    // Back Side  (rotateY 180 → 360)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .then(if (!showFront) Modifier.zIndex(1f) else Modifier)
                            .graphicsLayer {
                                rotationY = 180f + flip
                                cameraDistance = 1000f * density.density
                                alpha = backOpacity.coerceIn(0f, 1f)
                            }
                    ) {
                        // backContent
                        Column(
                            Modifier
                                .fillMaxSize()
                                .background(Color(0xFFFFFFFF), RoundedCornerShape(25.dp))
                                .padding(25.dp)
                        ) {
                            AppText(
                                "اطلاعات پرداخت",
                                modifier = Modifier.fillMaxWidth().padding(bottom = 25.dp),
                                fontSize = 20.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Bold,
                                color = SubInfoColors.dark,
                                textAlign = TextAlign.Center
                            )

                            SubInfoDetailRow(
                                icon = "payment",
                                iconColor = SubInfoColors.primary,
                                iconBackground = Color(0xFFE8F0FE),
                                label = "نوع پرداخت",
                                value = student.str("PaymentTypeStr") ?: ""
                            )

                            SubInfoDetailRow(
                                icon = "attach-money",
                                iconColor = SubInfoColors.success,
                                iconBackground = Color(0xFFE8F0FE),
                                label = "مبلغ دوره",
                                value = "${subInfoDigits(subInfoFormatPrice(student["CourseRegisterAmount"]))} تومان"
                            )

                            if ((student.double("Discount") ?: 0.0) > 0) {
                                SubInfoDetailRow(
                                    icon = "local-offer",
                                    iconColor = SubInfoColors.success,
                                    iconBackground = Color(0xFFE8F5E9),
                                    label = "تخفیف",
                                    value = "${subInfoDigits(subInfoFormatPrice(student["Discount"]))} تومان",
                                    valueColor = SubInfoColors.success
                                )
                            }

                            // totalContainer
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 15.dp)
                                    .background(SubInfoColors.primaryLight, RoundedCornerShape(15.dp))
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AppText(
                                    "مبلغ نهایی",
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    fontSize = 14.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                                    color = SubInfoColors.primary
                                )
                                AppText(
                                    "${subInfoDigits(subInfoFormatPrice(student["TotalAmount"]))} تومان",
                                    fontSize = 24.sp,
                                    // fontFamily "Yekan_Bakh_Heavy" is not a loaded font in RN → system default font
                                    fontFamily = FontFamily.Default,
                                    color = SubInfoColors.primary
                                )
                            }
                        }
                    }
                }

                // Delete Button
                val deleteShape = RoundedCornerShape(15.dp)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .rnShadow(8.dp, deleteShape, SubInfoColors.error)
                        .clip(deleteShape)
                        .touchableOpacity(activeOpacity = 0.8f) { onDelete(student) }
                ) {
                    LinearGradient(
                        colors = listOf(SubInfoColors.error, Color(0xFFC0392B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RowReverse(
                            Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MaterialIcons("delete", size = 20.dp, color = Color.White)
                            AppText(
                                "حذف دانشجو",
                                fontSize = 16.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Bold,
                                color = Color(0xFFFFFFFF)
                            )
                        }
                    }
                }
            }
        }
    }
}

// detailRow
@Composable
private fun SubInfoDetailRow(
    icon: String,
    iconColor: Color,
    iconBackground: Color,
    label: String,
    value: String,
    valueColor: Color = SubInfoColors.dark
) {
    RowReverse(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(Color(0xFFF8F9FA), RoundedCornerShape(15.dp))
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // detailIcon (marginLeft: 12)
        Box(
            Modifier
                .absolutePadding(left = 12.dp)
                .size(40.dp)
                .background(iconBackground, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MaterialIcons(icon, size = 20.dp, color = iconColor)
        }
        // detailContent (flex: 1)
        Column(Modifier.weight(1f)) {
            AppText(
                label,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                fontSize = 12.sp,
                fontFamily = RNFonts.Yekan_Bakh_Regular,
                color = SubInfoColors.medium
            )
            AppText(
                value,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 15.sp,
                fontFamily = RNFonts.Yekan_Bakh_Bold,
                color = valueColor
            )
        }
    }
}

// ── renderStudentCard ──────────────────────────────────────────────────────────
@Composable
private fun SubInfoStudentCard(item: JsonElement, index: Int, onPress: () -> Unit) {
    // Animated.spring tension 50, friction 7, delay index * 80
    val cardAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 80L)
        cardAnim.animateTo(1f, rnSpring(50f, 7f))
    }

    val shape = RoundedCornerShape(20.dp)
    val density = LocalDensity.current
    val stateColor = subInfoStateColor(item.int("State"))

    Box(Modifier.touchableOpacity(activeOpacity = 0.9f) { onPress() }) {
        Box(
            Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val v = cardAnim.value
                    alpha = v.coerceIn(0f, 1f)
                    val s = 0.8f + 0.2f * v
                    scaleX = s
                    scaleY = s
                    translationY = (50f * (1f - v)) * density.density
                }
                .rnShadow(6.dp, shape)
                .clip(shape)
        ) {
            LinearGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFFF8F9FA)),
                modifier = Modifier.fillMaxWidth()
            ) {
                // cardGradient: padding 16, alignItems center
                Column(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar
                    SubInfoAvatar(item, 70.dp)

                    // Name
                    AppText(
                        item.str("MemberName") ?: "",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 4.dp)
                            .heightIn(min = 40.dp),
                        fontSize = 15.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = SubInfoColors.dark,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Mobile
                    AppText(
                        subInfoDigits(item.str("MemberMobile") ?: ""),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        fontSize = 12.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        color = SubInfoColors.medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // cardDivider
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .height(1.dp)
                            .background(Color(0xFFE9ECEF))
                    )

                    // Date
                    RowReverse(
                        Modifier.padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MaterialIcons("calendar-today", size = 14.dp, color = SubInfoColors.medium)
                        AppText(
                            item.str("ShamsiRegisterDate") ?: "",
                            fontSize = 11.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                            color = SubInfoColors.medium
                        )
                    }

                    // Amount (alignItems: baseline)
                    RowReverse(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        AppText(
                            subInfoDigits(subInfoFormatPrice(item["TotalAmount"])),
                            modifier = Modifier.alignByBaseline(),
                            fontSize = 16.sp,
                            // "Yekan_Bakh_Heavy" is not a loaded font in RN → system default font
                            fontFamily = FontFamily.Default,
                            color = SubInfoColors.primary
                        )
                        AppText(
                            "تومان",
                            modifier = Modifier.alignByBaseline(),
                            fontSize = 11.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Regular,
                            color = SubInfoColors.medium
                        )
                    }
                }

                // Status Badge (absolute top 12, left 12)
                Box(
                    Modifier
                        .align(AbsoluteAlignment.TopLeft)
                        .absoluteOffset(x = 12.dp, y = 12.dp)
                        .rnShadow(4.dp, CircleShape)
                        .size(32.dp)
                        .background(stateColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    MaterialIcons(subInfoStateIcon(item.int("State")), size = 14.dp, color = Color.White)
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Default export: CourseStudentsScreen({ route }) → route.params { courseId, courseName }
// ──────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SubscriptionInfo(courseId: Int? = null, courseName: String? = null) {
    val navigation = useNavigation()
    val scope = rememberCoroutineScope()
    val students = remember(courseId) { SubInfoStudents(scope, courseId) }

    var refreshing by remember { mutableStateOf(false) }
    var selectedStudent by remember { mutableStateOf<JsonElement?>(null) }
    var drawerVisible by remember { mutableStateOf(false) }
    var deleteModalVisible by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    // Toast states
    var toastVisible by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }
    var toastType by remember { mutableStateOf("info") }

    // Animation
    val fadeAnim = remember { Animatable(0f) }
    val slideAnim = remember { Animatable(50f) }

    LaunchedEffect(Unit) {
        launch { students.fetchStudents() }
        launch { fadeAnim.animateTo(1f, rnTiming(600)) }
        launch { slideAnim.animateTo(0f, rnTiming(600)) }
    }

    fun showToast(message: String, type: String = "info") {
        toastMessage = message
        toastType = type
        toastVisible = true
    }

    fun onRefresh() {
        refreshing = true
        students.refresh()
        refreshing = false
    }

    fun handleCardPress(item: JsonElement) {
        selectedStudent = item
        drawerVisible = true
    }

    fun handleDeletePress(item: JsonElement) {
        drawerVisible = false
        scope.launch {
            delay(300)
            selectedStudent = item
            deleteModalVisible = true
        }
    }

    fun handleConfirmDelete() {
        val selected = selectedStudent ?: return
        scope.launch {
            try {
                isDeleting = true
                students.deleteStudent(selected.int("CourseRegistrationId"))
                deleteModalVisible = false
                showToast("دانشجو با موفقیت حذف شد", "success")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                showToast("خطا در حذف دانشجو", "error")
            } finally {
                isDeleting = false
                selectedStudent = null
            }
        }
    }

    // StatusBar.currentHeight ? StatusBar.currentHeight + 20 : 45
    val sbh = statusBarHeight()
    val topOffset: Dp = if (!isIOS && sbh > 0.dp) sbh + 20.dp else 45.dp
    val density = LocalDensity.current

    Box(Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        MainBackground()

        Column(Modifier.fillMaxSize()) {
            // Header (Animated.View: fade + slide)
            Box(
                Modifier
                    .absolutePadding(left = 20.dp, right = 20.dp, top = topOffset, bottom = 20.dp)
                    .graphicsLayer {
                        alpha = fadeAnim.value
                        translationY = slideAnim.value * density.density
                    }
                    .rnShadow(8.dp, RoundedCornerShape(20.dp), SubInfoColors.primary)
            ) {
                LinearGradient(
                    colors = listOf(SubInfoColors.gradientStart, SubInfoColors.gradientEnd),
                    modifier = Modifier.fillMaxWidth(),
                    start = Offset(0f, 0f),
                    end = Offset(1f, 1f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    RowReverse(
                        Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // headerTextContainer (flex: 1)
                        Column(Modifier.weight(1f)) {
                            AppText(
                                "دانشجویان دوره",
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                fontSize = 22.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Bold,
                                color = Color.White
                            )
                            AppText(
                                courseName ?: "",
                                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                                fontSize = 15.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Regular,
                                color = Color(1f, 1f, 1f, 0.95f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (students.total > 0) {
                                AppText(
                                    "${subInfoDigits(students.total.toString())} دانشجو",
                                    modifier = Modifier.fillMaxWidth(),
                                    fontSize = 14.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                                    color = Color(1f, 1f, 1f, 0.9f)
                                )
                            }
                        }
                        // headerIconContainer
                        Box(
                            Modifier
                                .size(56.dp)
                                .background(Color(1f, 1f, 1f, 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            MaterialIcons("people", size = 32.dp, color = Color.White)
                        }
                    }
                }
            }

            val listPadding = PaddingValues(start = 20.dp, top = 0.dp, end = 20.dp, bottom = 36.dp)

            if (students.loading && students.data.isEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = listPadding,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    itemsIndexed(listOf(1, 2, 3, 4), key = { _, it -> it }) { _, _ ->
                        SubInfoStudentCardSkeleton()
                    }
                }
            } else {
                val gridState = rememberLazyGridState()
                val pullState = rememberPullToRefreshState()

                // onEndReached (onEndReachedThreshold 0.5)
                val reachedEnd by remember {
                    derivedStateOf {
                        val info = gridState.layoutInfo
                        val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
                        info.totalItemsCount > 0 && last >= info.totalItemsCount - 1 - 3
                    }
                }
                LaunchedEffect(reachedEnd, students.data.size) {
                    if (reachedEnd) students.loadMore()
                }

                PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = { onRefresh() },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = pullState,
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullState,
                            isRefreshing = refreshing,
                            modifier = Modifier.align(Alignment.TopCenter),
                            containerColor = Color.White,
                            color = SubInfoColors.primary
                        )
                    }
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        state = gridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = listPadding,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        itemsIndexed(
                            students.data,
                            key = { i, it -> "${it.int("CourseRegistrationId")}-$i" }
                        ) { index, item ->
                            SubInfoStudentCard(item, index) { handleCardPress(item) }
                        }

                        // ListEmptyComponent
                        if (students.data.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp)
                                        .padding(top = 100.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    MaterialIcons("people-outline", size = 80.dp, color = SubInfoColors.light)
                                    AppText(
                                        "هنوز هیچ دانشجویی ثبت‌نام نکرده",
                                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp),
                                        fontSize = 18.sp,
                                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                                        color = SubInfoColors.dark,
                                        textAlign = TextAlign.Center
                                    )
                                    AppText(
                                        "وقتی دانشجویان در این دوره ثبت‌نام کنند، اینجا نمایش داده می‌شوند",
                                        modifier = Modifier.fillMaxWidth(),
                                        fontSize = 15.sp,
                                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                                        color = SubInfoColors.medium,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 24.sp
                                    )
                                }
                            }
                        }

                        // ListFooterComponent
                        if (students.loading && students.hasMore) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = SubInfoColors.primary,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Back button (position absolute, top, right: 20, zIndex 10)
        Box(
            Modifier
                .align(AbsoluteAlignment.TopRight)
                .absolutePadding(right = 20.dp, top = topOffset)
                .zIndex(10f)
                .touchableOpacity(activeOpacity = 0.8f) {
                    navigation.navigate("MainTabs", mapOf("screen" to Tabs.Home))
                }
        ) {
            Box(
                Modifier
                    .rnShadow(8.dp, CircleShape, SubInfoColors.primary)
                    .size(44.dp)
                    .background(Color(1f, 1f, 1f, 0.95f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                MaterialIcons("arrow-forward", size = 24.dp, color = SubInfoColors.primary)
            }
        }

        // Student Drawer
        SubInfoStudentDrawer(
            visible = drawerVisible,
            student = selectedStudent,
            onClose = { drawerVisible = false },
            onDelete = { handleDeletePress(it) }
        )

        // Delete Confirmation Modal
        if (deleteModalVisible) {
            Dialog(
                onDismissRequest = { if (!isDeleting) deleteModalVisible = false },
                properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
            ) {
                SubInfoDeleteModalContent(
                    isDeleting = isDeleting,
                    onClose = { if (!isDeleting) deleteModalVisible = false },
                    onConfirm = { handleConfirmDelete() }
                )
            }
        }

        Toast(toastVisible, toastMessage, toastType) { toastVisible = false }
    }
}

@Composable
private fun SubInfoDeleteModalContent(
    isDeleting: Boolean,
    onClose: () -> Unit,
    onConfirm: () -> Unit
) {
    // modalContainer: flex 1, flex-end
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        // modalBackdrop
        Box(
            Modifier
                .fillMaxSize()
                .touchableOpacity(activeOpacity = 1f) { onClose() }
                .background(Color(0f, 0f, 0f, 0.5f))
        )

        val sheetShape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)
        Column(
            Modifier
                .fillMaxWidth()
                .rnShadow(10.dp, sheetShape)
                .background(Color(0xFFFFFFFF), sheetShape)
                .padding(top = 15.dp, bottom = 35.dp)
                .padding(horizontal = 25.dp)
        ) {
            // modalHandle
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 20.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(Color(0xFFE0E0E0), RoundedCornerShape(2.dp))
            )

            // deleteModalHeader
            Column(
                Modifier.fillMaxWidth().padding(bottom = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .padding(bottom = 20.dp)
                        .rnShadow(8.dp, CircleShape, SubInfoColors.error)
                        .size(70.dp)
                        .background(SubInfoColors.error, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    MaterialIcons("warning", size = 36.dp, color = Color.White)
                }
                AppText(
                    "حذف دانشجو",
                    modifier = Modifier.padding(bottom = 15.dp),
                    fontSize = 20.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                    color = Color(0xFF2C3E50)
                )
                AppText(
                    "آیا از حذف این دانشجو از دوره اطمینان دارید؟ این عملیات قابل بازگشت نیست.",
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 16.sp,
                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                    color = Color(0xFF6C757D),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }

            // deleteModalActions (marginTop 10) → deleteButtonsRow (row-reverse, gap 15)
            RowReverse(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                val btnShape = RoundedCornerShape(15.dp)
                // confirmDeleteButton
                Box(
                    Modifier
                        .weight(1f)
                        .rnShadow(8.dp, btnShape, SubInfoColors.error)
                        .clip(btnShape)
                        .touchableOpacity(enabled = !isDeleting, activeOpacity = 0.8f) { onConfirm() }
                ) {
                    LinearGradient(
                        colors = listOf(SubInfoColors.error, Color(0xFFC0392B)),
                        modifier = Modifier.fillMaxWidth(),
                        start = Offset(0f, 0f),
                        end = Offset(1f, 1f)
                    ) {
                        RowReverse(
                            Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isDeleting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                MaterialIcons("delete", size = 20.dp, color = Color.White)
                                AppText(
                                    "بله، حذف شود",
                                    fontSize = 16.sp,
                                    fontFamily = RNFonts.Yekan_Bakh_Bold,
                                    color = Color(0xFFFFFFFF)
                                )
                            }
                        }
                    }
                }

                // deleteModalCancelButton
                Box(
                    Modifier
                        .weight(1f)
                        .touchableOpacity(enabled = !isDeleting, activeOpacity = 0.8f) { onClose() }
                        .background(Color(0xFFF8F9FA), btnShape)
                        .border(1.dp, Color(0xFFE9ECEF), btnShape)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AppText(
                        "انصراف",
                        fontSize = 16.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color(0xFF6C757D)
                    )
                }
            }
        }
    }
}
