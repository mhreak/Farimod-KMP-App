package ir.farimod.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.network.Api
import ir.farimod.app.network.get
import ir.farimod.app.network.jsonOf
import ir.farimod.app.utils.jsToFixed
import ir.farimod.app.utils.jsToString
import ir.farimod.app.utils.jsTruthy
import ir.farimod.app.utils.toPersianDigits
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.math.abs
import kotlin.math.floor

// ──────────────────────────────────────────────────────────────────────────────
// Port of app/components/RatingComponent.tsx
//   default export  → MultiOptionRatingComponent(...)
//   named export    → StarDisplay(...)
// ──────────────────────────────────────────────────────────────────────────────

/** The local `colors` object of RatingComponent.tsx. */
private object RatingColors {
    val primary = Color(0xFF667EEA)
    val gold = Color(0xFFFFD700)
    val silver = Color(0xFFC0C0C0)
    val white = Color(0xFFFFFFFF)
    val dark = Color(0xFF2C3E50)
    val gray = Color(0xFF95A5A6)
    val primaryButton = Color(0xFF667EEA)
    val primaryDarkButton = Color(0xFF764BA2)
}

/**
 * One rating row (RN: `{ id, title, subtitle, contentReviewItemId, showOrder, averageRating }`).
 * [contentReviewItemId] is a JSON/number/string value (RN: number or the fallback string id).
 */
data class RatingOption(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val contentReviewItemId: Any? = null,
    val showOrder: Double? = null,
    val averageRating: Double = 0.0
)

/**
 * RN `onRatingSubmitted({ ... })` payload.
 *  - multi option: [ratings], [averageRating], [reviewItemRatings], [response]
 *  - single option: [rating], [response]   (`ratings == null`)
 */
data class RatingSubmitResult(
    val ratings: Map<String, Double>? = null,
    val averageRating: Double? = null,
    val reviewItemRatings: List<JsonObject>? = null,
    val rating: Double? = null,
    val response: JsonElement? = null
)

// Animated.timing default easing = Easing.inOut(Easing.ease)
private val RNEase = CubicBezierEasing(0.42f, 0f, 1f, 1f)
private val RNEaseInOut = Easing { t ->
    if (t < 0.5f) RNEase.transform(t * 2f) / 2f else 1f - RNEase.transform((1f - t) * 2f) / 2f
}

/** Raw RN `<Text>` (system default is overridden by the explicit Yekan fonts; default align = left). */
@Composable
private fun RText(
    text: String,
    fontSize: Int,
    bold: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Right
) {
    AppText(
        text = text,
        modifier = modifier,
        fontSize = fontSize.sp,
        color = color,
        fontFamily = if (bold) RNFonts.Yekan_Bakh_Bold else RNFonts.Yekan_Bakh_Regular,
        textAlign = textAlign
    )
}

/** RN negative `marginTop` (the following siblings move up as well). */
private fun Modifier.negativeTopMargin(amount: Dp): Modifier = layout { measurable, constraints ->
    val p = measurable.measure(constraints)
    val a = amount.roundToPx()
    layout(p.width, (p.height - a).coerceAtLeast(0)) { p.place(0, -a) }
}

// ══════════════════════════════════════════════════════════════════════════════
// StarDisplay
// ══════════════════════════════════════════════════════════════════════════════

/**
 * RN `StarDisplay`.
 * @param onPress called with the 1-based star value; null → stars are not touchable
 * @param modifier RN `style` (applied to the stars container)
 */
@Composable
fun StarDisplay(
    rating: Double = 0.0,
    maxStars: Int = 5,
    size: Dp = 20.dp,
    color: Color = RatingColors.gold,
    emptyColor: Color = RatingColors.silver,
    showHalfStars: Boolean = true,
    modifier: Modifier = Modifier,
    onPress: ((Int) -> Unit)? = null,
    animated: Boolean = false,
    rtl: Boolean = false
) {
    val animValues = remember(maxStars) { List(maxStars) { Animatable(0f) } }

    LaunchedEffect(animated, rating) {
        if (animated) {
            animValues.forEachIndexed { index, anim ->
                launch {
                    // Animated.stagger(100, ...) → start offset index * 100, plus the animation's own delay index * 100
                    delay(100L * index)
                    anim.animateTo(
                        1f,
                        tween(durationMillis = 200, delayMillis = index * 100, easing = RNEaseInOut)
                    )
                }
            }
        }
    }

    val content: @Composable RowScope.() -> Unit = {
        for (index in 0 until maxStars) {
            val starValue = index + 1
            var iconName = "star-border"
            var starColor = emptyColor

            if (rating >= starValue) {
                iconName = "star"
                starColor = color
            } else if (showHalfStars && rating >= starValue - 0.5) {
                iconName = "star-half"
                starColor = color
            }

            val starModifier = if (animated) {
                val v = animValues[index].value
                Modifier.graphicsLayer {
                    val s = 0.5f + 0.5f * v
                    scaleX = s
                    scaleY = s
                    alpha = v
                }
            } else Modifier

            Box(starModifier) {
                Box(
                    Modifier
                        .touchableOpacity(enabled = onPress != null) { onPress?.invoke(starValue) }
                        .padding(2.dp)
                ) {
                    MaterialIcons(iconName, size = size, color = starColor)
                }
            }
        }
    }

    if (rtl) {
        RowReverse(modifier, Arrangement.Center, Alignment.CenterVertically, content)
    } else {
        Row(modifier, Arrangement.Center, Alignment.CenterVertically, content = content)
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// MultiOptionRatingComponent
// ══════════════════════════════════════════════════════════════════════════════

private fun ratingJsonValue(d: Double): Any =
    if (d == floor(d) && abs(d) < 1e15) d.toLong() else d

private fun getRatingText(rating: Double): String {
    if (rating == 0.0) return ""
    if (rating <= 1) return "خیلی ضعیف"
    if (rating <= 2) return "ضعیف"
    if (rating <= 3) return "متوسط"
    if (rating <= 4) return "خوب"
    return "عالی"
}

private fun calculateAverageFromDetailedRatings(detailedRatings: Map<String, Double>): Double {
    val ratings = detailedRatings.values
    if (ratings.isEmpty()) return 0.0
    val sum = ratings.fold(0.0) { acc, rating -> acc + rating }
    return sum / ratings.size
}

/** The gradient button content (RN `LinearGradient` with `submitButtonGradient` style). */
@Composable
private fun RatingGradientButtonContent(
    enabled: Boolean,
    isSubmitting: Boolean,
    text: String,
    fillWidth: Boolean
) {
    LinearGradient(
        colors = if (enabled) listOf(RatingColors.primaryButton, RatingColors.primaryDarkButton)
        else listOf(RatingColors.gray, RatingColors.gray),
        modifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier,
        start = Offset(0f, 0f),
        end = Offset(1f, 1f)
    ) {
        RowReverse(
            modifier = (if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                .padding(vertical = 15.dp, horizontal = 30.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = RatingColors.white,
                    strokeWidth = 2.dp
                )
            } else {
                MaterialIcons("star", size = 20.dp, color = RatingColors.white)
            }
            RText(text, 16, true, RatingColors.white)
        }
    }
}

/**
 * RN `MultiOptionRatingComponent` (default export of RatingComponent.tsx).
 *
 * Props are the RN props 1:1 (`size` in dp, `style` → [modifier]).
 * `contentId` is the numeric id sent as `ContnetId` (pass Int/Long/JsonElement).
 */
@Composable
fun MultiOptionRatingComponent(
    contentId: Any?,
    initialRating: Double = 0.0,
    initialDetailedRatings: Map<String, Double> = emptyMap(),
    maxStars: Int = 5,
    size: Dp = 24.dp,
    showRatingText: Boolean = true,
    showRatingCount: Boolean = true,
    ratingCount: Int = 0,
    averageRating: Double = 0.0,
    onRatingChange: ((Double, Map<String, Double>?) -> Unit)? = null,
    onRatingSubmitted: ((RatingSubmitResult) -> Unit)? = null,
    onRatingError: ((String) -> Unit)? = null,
    readonly: Boolean = false,
    modifier: Modifier = Modifier,
    starColor: Color = RatingColors.gold,
    emptyStarColor: Color = RatingColors.silver,
    textColor: Color = RatingColors.dark,
    animated: Boolean = true,
    showModal: Boolean = true,
    modalTitle: String = "امتیاز شما",
    submitButtonText: String = "ثبت امتیاز",
    cancelButtonText: String = "لغو",
    onSubmit: ((Double, Map<String, Double>?) -> Unit)? = null,
    allowHalfStars: Boolean = false,
    rtl: Boolean = false,
    enableMultipleOptions: Boolean = false,
    ratingOptions: List<RatingOption> = emptyList(),
    detailedRatingsAverages: Map<String, Double> = emptyMap()
) {
    var currentRating by remember { mutableStateOf(initialRating) }
    var currentDetailedRatings by remember { mutableStateOf(initialDetailedRatings) }
    var showRatingModal by remember { mutableStateOf(false) }
    var tempRating by remember { mutableStateOf(0.0) }
    var tempDetailedRatings by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var isSubmitting by remember { mutableStateOf(false) }

    val modalBackdropAnim = remember { Animatable(0f) }
    val modalSlideAnim = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    fun showModalAnimation() {
        scope.launch {
            coroutineScope {
                launch { modalBackdropAnim.animateTo(1f, tween(300, easing = RNEaseInOut)) }
                launch { modalSlideAnim.animateTo(1f, tween(300, easing = RNEaseInOut)) }
            }
        }
    }

    fun hideModalAnimation() {
        scope.launch {
            coroutineScope {
                launch { modalBackdropAnim.animateTo(0f, tween(250, easing = RNEaseInOut)) }
                launch { modalSlideAnim.animateTo(0f, tween(250, easing = RNEaseInOut)) }
            }
            showRatingModal = false
            tempRating = 0.0
            tempDetailedRatings = emptyMap()
        }
    }

    fun handleStarPress() {
        if (readonly) return

        if (showModal) {
            if (enableMultipleOptions) {
                tempDetailedRatings = currentDetailedRatings
                showRatingModal = true
                showModalAnimation()
            } else {
                // RN passes the press event object here (never a number) → `tempRating > 0` stays false
                tempRating = 0.0
                showRatingModal = true
                showModalAnimation()
            }
        } else {
            currentRating = 0.0
            onRatingChange?.invoke(0.0, null)
        }
        // RN also runs a scaleAnim (1 → 1.1 → 1) that is not bound to any view.
    }

    fun buildReviewItemRatings(detailedRatings: Map<String, Double>): List<JsonObject> {
        return ratingOptions
            .filter { option -> (detailedRatings[option.id] ?: 0.0) > 0 }
            .map { option ->
                jsonOf(
                    "ContentReviewItemId" to (if (jsTruthy(option.contentReviewItemId)) option.contentReviewItemId else option.id),
                    "Rating" to ratingJsonValue(detailedRatings[option.id] ?: 0.0)
                )
            }
    }

    fun validateRatings(): Boolean {
        if (enableMultipleOptions) {
            return tempDetailedRatings.values.any { it > 0 }
        }
        return tempRating > 0
    }

    suspend fun submitRatingToAPI(): JsonElement? {
        val reviewData = if (enableMultipleOptions) {
            jsonOf(
                "ContnetId" to contentId,
                "reviewItemRatings" to buildReviewItemRatings(tempDetailedRatings)
            )
        } else {
            jsonOf(
                "ContnetId" to contentId,
                "reviewItemRatings" to listOf(
                    jsonOf(
                        "ContentReviewItemId" to 0,
                        "Rating" to ratingJsonValue(tempRating)
                    )
                )
            )
        }

        val response = Api.post("MemberReview/SendReview", body = reviewData)

        if (!response.ok) {
            val errorData = response.json
                ?: throw Exception(if (response.text.isBlank()) "JSON Parse error: Unexpected end of input" else "JSON Parse error: Unexpected character")
            val message = errorData["Message"]
            throw Exception(if (jsTruthy(message)) jsToString(message) else "HTTP error! status: ${response.status}")
        }

        return response.json
            ?: throw Exception(if (response.text.isBlank()) "JSON Parse error: Unexpected end of input" else "JSON Parse error: Unexpected character")
    }

    fun handleSubmitRating() {
        if (!validateRatings()) {
            RNAlert.alert("خطا", "لطفاً حداقل یک امتیاز انتخاب کنید")
            return
        }

        isSubmitting = true

        scope.launch {
            try {
                val apiResult = submitRatingToAPI()

                if (enableMultipleOptions) {
                    val averageFromDetailed = calculateAverageFromDetailedRatings(tempDetailedRatings)
                    val submitted = tempDetailedRatings
                    currentRating = averageFromDetailed
                    currentDetailedRatings = submitted
                    onRatingChange?.invoke(averageFromDetailed, submitted)
                    onSubmit?.invoke(averageFromDetailed, submitted)

                    onRatingSubmitted?.invoke(
                        RatingSubmitResult(
                            ratings = submitted,
                            averageRating = averageFromDetailed,
                            reviewItemRatings = buildReviewItemRatings(submitted),
                            response = apiResult
                        )
                    )
                } else {
                    val submitted = tempRating
                    currentRating = submitted
                    onRatingChange?.invoke(submitted, null)
                    onSubmit?.invoke(submitted, null)

                    onRatingSubmitted?.invoke(
                        RatingSubmitResult(
                            rating = submitted,
                            response = apiResult
                        )
                    )
                }

                hideModalAnimation()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = e.message?.takeIf { it.isNotEmpty() } ?: "خطا در ثبت امتیاز"
                onRatingError?.invoke(message)
                RNAlert.alert("خطا", message)
            } finally {
                isSubmitting = false
            }
        }
    }

    fun isSubmitEnabled(): Boolean {
        if (isSubmitting) return false
        return validateRatings()
    }

    Column(
        modifier = Modifier.fillMaxWidth().then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (averageRating > 0) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 15.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                RText(
                    "میانگین امتیازات", 14, true, textColor,
                    Modifier.padding(bottom = 8.dp), TextAlign.Center
                )
                StarDisplay(
                    rating = averageRating,
                    maxStars = maxStars,
                    size = size - 4.dp,
                    color = starColor,
                    emptyColor = emptyStarColor,
                    showHalfStars = true,
                    animated = animated
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    RText(
                        toPersianDigits(jsToFixed(averageRating, 1)), 18, true, textColor,
                        Modifier.padding(bottom = 4.dp), TextAlign.Center
                    )
                    if (showRatingCount && ratingCount > 0) {
                        RText(
                            "(${toPersianDigits(ratingCount.toString())} نظر)", 12, false, RatingColors.gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        if (!readonly) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    Modifier
                        .padding(top = 10.dp)
                        .touchableOpacity { handleStarPress() }
                        .clip(RoundedCornerShape(15.dp))
                ) {
                    RatingGradientButtonContent(
                        enabled = true,
                        isSubmitting = isSubmitting,
                        text = if (isSubmitting) "در حال ثبت..." else "امتیاز خود را ثبت کنید !",
                        fillWidth = false
                    )
                }
            }
        }
    }

    if (showRatingModal) {
        Dialog(
            onDismissRequest = { hideModalAnimation() },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val sheetMaxHeight = maxHeight * 0.85f
                val sheetShape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)

                // modalBackdrop (rgba(0,0,0,0.5)) + backdropTouchable (activeOpacity 1)
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = modalBackdropAnim.value }
                        .background(Color(0f, 0f, 0f, 0.5f))
                        .pressable { if (!isSubmitting) hideModalAnimation() }
                )

                // modalContent
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .heightIn(max = sheetMaxHeight)
                        .graphicsLayer {
                            translationY = (1f - modalSlideAnim.value) * 300f * density.density
                        }
                        .rnShadow(10.dp, sheetShape)
                        .clip(sheetShape)
                        .background(RatingColors.white)
                        .pressable { }
                        .absolutePadding(left = 25.dp, right = 25.dp, top = 15.dp, bottom = 35.dp)
                ) {
                    // modalHandle
                    Box(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 20.dp)
                            .size(width = 40.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFE0E0E0))
                    )

                    // modalHeader
                    Column(
                        Modifier.fillMaxWidth().padding(bottom = 25.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RText(modalTitle, 20, true, RatingColors.dark)
                    }

                    // modalBody (ScrollView)
                    Column(
                        Modifier
                            .weight(1f, fill = false)
                            .padding(bottom = 20.dp)
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (enableMultipleOptions) {
                            Column(Modifier.fillMaxWidth()) {
                                RText(
                                    "لطفاً به هر بخش امتیاز دهید", 14, false, RatingColors.gray,
                                    Modifier
                                        .fillMaxWidth()
                                        .negativeTopMargin(4.dp)
                                        .padding(bottom = 20.dp),
                                    TextAlign.Center
                                )

                                for (option in ratingOptions) {
                                    val optionShape = RoundedCornerShape(15.dp)
                                    Column(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 15.dp)
                                            .clip(optionShape)
                                            .background(Color(0xFFF8F9FA))
                                            .border(1.dp, Color(0xFFE9ECEF), optionShape)
                                            .padding(18.dp)
                                    ) {
                                        // optionHeader
                                        RowReverse(
                                            Modifier.fillMaxWidth().padding(bottom = 15.dp),
                                            Arrangement.SpaceBetween,
                                            Alignment.CenterVertically
                                        ) {
                                            // optionTitleSection
                                            Column(
                                                Modifier.weight(1f),
                                                horizontalAlignment = Alignment.Start
                                            ) {
                                                RText(
                                                    option.title, 16, true, RatingColors.dark,
                                                    Modifier.padding(bottom = 4.dp)
                                                )
                                                RText(option.subtitle, 13, false, RatingColors.gray)
                                            }

                                            if (option.averageRating > 0) {
                                                // optionAverageSection (marginTop: -25 inside a centered row → -12.5)
                                                RowReverse(
                                                    Modifier
                                                        .offset(y = (-12.5).dp)
                                                        .clip(RoundedCornerShape(12.dp)),
                                                    Arrangement.spacedBy(4.dp),
                                                    Alignment.CenterVertically
                                                ) {
                                                    RText(
                                                        toPersianDigits(jsToFixed(option.averageRating, 1)),
                                                        14, true, Color(0xFF868686)
                                                    )
                                                    MaterialIcons("star", size = 16.dp, color = RatingColors.gold)
                                                }
                                            }
                                        }

                                        // optionRatingContainer
                                        Column(
                                            Modifier.fillMaxWidth(),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            StarDisplay(
                                                rating = tempDetailedRatings[option.id] ?: 0.0,
                                                maxStars = maxStars,
                                                size = 32.dp,
                                                color = starColor,
                                                emptyColor = emptyStarColor,
                                                showHalfStars = allowHalfStars,
                                                modifier = Modifier.padding(bottom = 8.dp),
                                                onPress = if (!isSubmitting) { rating ->
                                                    tempDetailedRatings = tempDetailedRatings + (option.id to rating.toDouble())
                                                } else null,
                                                animated = false
                                            )

                                            val optionRating = tempDetailedRatings[option.id] ?: 0.0
                                            if (optionRating > 0) {
                                                RText(
                                                    getRatingText(optionRating), 14, true, RatingColors.primary,
                                                    Modifier.padding(bottom = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (tempDetailedRatings.isNotEmpty()) {
                                    // totalAverageContainer
                                    Column(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp, bottom = 20.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(RatingColors.primaryDarkButton)
                                            .padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        RText(
                                            "میانگین کلی:", 16, true, RatingColors.white,
                                            Modifier.padding(bottom = 12.dp)
                                        )
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            StarDisplay(
                                                rating = calculateAverageFromDetailedRatings(tempDetailedRatings),
                                                maxStars = maxStars,
                                                size = 28.dp,
                                                color = starColor,
                                                emptyColor = emptyStarColor,
                                                showHalfStars = true,
                                                animated = false
                                            )
                                            RText(
                                                toPersianDigits(
                                                    jsToFixed(calculateAverageFromDetailedRatings(tempDetailedRatings), 1)
                                                ),
                                                20, true, RatingColors.white,
                                                Modifier.padding(top = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // singleOptionContainer
                            Column(
                                Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                StarDisplay(
                                    rating = tempRating,
                                    maxStars = maxStars,
                                    size = 40.dp,
                                    color = starColor,
                                    emptyColor = emptyStarColor,
                                    showHalfStars = allowHalfStars,
                                    modifier = Modifier.padding(bottom = 15.dp),
                                    onPress = if (!isSubmitting) { rating -> tempRating = rating.toDouble() } else null,
                                    animated = false
                                )

                                if (tempRating > 0) {
                                    RText(
                                        getRatingText(tempRating), 18, true, RatingColors.dark,
                                        Modifier.padding(bottom = 10.dp)
                                    )
                                }

                                RText(
                                    "لطفاً امتیاز خود را انتخاب کنید", 14, false, RatingColors.gray,
                                    Modifier
                                        .negativeTopMargin(4.dp)
                                        .padding(bottom = 20.dp),
                                    TextAlign.Center
                                )
                            }
                        }
                    }

                    // modalActions
                    RowReverse(
                        Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        Arrangement.spacedBy(15.dp)
                    ) {
                        // cancelButton
                        val cancelShape = RoundedCornerShape(15.dp)
                        Box(
                            Modifier
                                .weight(1f)
                                .padding(top = 8.dp)
                                .fillMaxHeight()
                                .touchableOpacity(enabled = !isSubmitting) { hideModalAnimation() }
                                .clip(cancelShape)
                                .background(Color(0xFFF8F9FA))
                                .border(1.dp, Color(0xFFE9ECEF), cancelShape)
                                .padding(vertical = 15.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            RText(cancelButtonText, 16, true, RatingColors.gray)
                        }

                        // submitButton
                        val submitEnabled = isSubmitEnabled()
                        Box(
                            Modifier
                                .weight(1f)
                                .padding(top = 10.dp)
                                .touchableOpacity(enabled = submitEnabled) { handleSubmitRating() }
                                .clip(RoundedCornerShape(15.dp))
                        ) {
                            RatingGradientButtonContent(
                                enabled = submitEnabled,
                                isSubmitting = isSubmitting,
                                text = if (isSubmitting) "در حال ثبت..." else submitButtonText,
                                fillWidth = true
                            )
                        }
                    }
                }
            }
        }
    }
}
