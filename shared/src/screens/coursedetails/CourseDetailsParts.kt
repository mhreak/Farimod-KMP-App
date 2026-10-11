package ir.farimod.app.screens.coursedetails

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.farimod.app.network.bool
import ir.farimod.app.network.get
import ir.farimod.app.network.str
import ir.farimod.app.ui.RNTimingEasing
import ir.farimod.app.ui.RatingOption
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.utils.jsToNumber
import ir.farimod.app.utils.jsToString
import ir.farimod.app.utils.jsTruthy
import ir.farimod.app.utils.toPersianDigits
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

internal object CoursePalette {
    val primary = Color(0xFF667EEA)
    val secondary = Color(0xFFFF6B6B)
    val tertiary = Color(0xFF4ECDC4)
    val surface = Color.White
    val success = Color(0xFF2ECC71)
    val warning = Color(0xFFF39C12)
    val error = Color(0xFFE74C3C)
    val info = Color(0xFF3498DB)
    val desc = Color(0xFFF39C12)
    val money = Color(0xFFE74C3C)
    val place = Color(0xFF9B59B6)
    val phone = Color(0xFF3498DB)
    val group = Color(0xFFE67E22)
    val category = Color(0xFF8E44AD)
    val gold = Color(0xFFFFD700)
    val like = Color(0xFFE91E63)
}

internal fun courseIconColor(icon: String): Color = when (icon) {
    "school" -> CoursePalette.success
    "description" -> CoursePalette.desc
    "attach-money" -> CoursePalette.money
    "place" -> CoursePalette.place
    "phone" -> CoursePalette.phone
    "group" -> CoursePalette.group
    "category" -> CoursePalette.category
    "schedule" -> CoursePalette.info
    "calendar" -> CoursePalette.warning
    else -> CoursePalette.primary
}

internal fun formatCourseType(courseType: Int?): String = when (courseType) {
    1 -> "حضوری"
    2 -> "مجازی"
    3 -> "حضوری و مجازی"
    else -> "نامشخص"
}

internal fun courseScheduleText(course: JsonElement?): String {
    val days = listOf("Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
    val persian = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه")
    val lines = ArrayList<String>()
    days.forEachIndexed { index, day ->
        val has = course.bool("Has${day}Session")
        val start = course.str("${day}_StartTime")
        val finish = course.str("${day}_FinishTime")
        if (has && !start.isNullOrEmpty() && !finish.isNullOrEmpty()) {
            lines += "${persian[index]}: ${toPersianDigits(finish)} - ${toPersianDigits(start)}"
        }
    }
    return if (lines.isEmpty()) "برنامه زمانی مشخص نشده" else lines.joinToString("\n")
}

/** CourseDetailsScreen's own mapper (defaults differ from the product-detail helper). */
internal fun courseRatingOptions(contentReviewList: JsonElement?): List<RatingOption> {
    val items = (contentReviewList as? JsonArray)?.toList().orEmpty()
    if (items.isEmpty()) {
        return listOf(
            RatingOption("quality", "کیفیت آموزش", "کیفیت کلی و دقت در ارائه مطالب", "quality"),
            RatingOption("content", "محتوای دوره", "جامعیت و کاربردی بودن مطالب", "content"),
            RatingOption("instructor", "مربی", "تسلط و نحوه تدریس مربی", "instructor"),
            RatingOption("organization", "تشکیلات", "زمان‌بندی و سازماندهی", "organization")
        )
    }
    return items
        .filter { it["Active"].bool }
        .sortedBy { jsToNumber(it["ShowOrder"]) }
        .map { item ->
            val rawId = item["ContentReviewItemId"]
            val average = item["CalculatedAverageRating"]
            RatingOption(
                id = "review_" + jsToString(rawId),
                title = item.str("Text") ?: "",
                subtitle = "",
                contentReviewItemId = rawId,
                showOrder = item["ShowOrder"]?.let { jsToNumber(it) },
                averageRating = if (jsTruthy(average)) jsToNumber(average) else 0.0
            )
        }
}

@Composable
internal fun coursePulseColor(): Color {
    val transition = rememberInfiniteTransition(label = "courseSkel")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = RNTimingEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coursePulse"
    )
    return lerp(Color(0xFFE0E0E0), Color(0xFFF0F0F0), t)
}

@Composable
internal fun CoursePulse(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp,
    fraction: Float? = null,
    radius: Dp = 8.dp
) {
    val sizeMod = when {
        fraction != null -> Modifier.fillMaxWidth(fraction)
        width != null -> Modifier.width(width)
        else -> Modifier.fillMaxWidth()
    }
    Box(modifier.then(sizeMod).height(height).background(coursePulseColor(), RoundedCornerShape(radius)))
}

@Composable
internal fun CourseDetailsSkeleton() {
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
            CoursePulse(width = 200.dp, height = 26.dp, radius = 13.dp)
        }
        Box(Modifier.fillMaxWidth().height(480.dp).absolutePadding(left = 20.dp, right = 20.dp, top = 20.dp, bottom = 20.dp)) {
            CoursePulse(modifier = Modifier.fillMaxWidth(), height = 440.dp, radius = 30.dp)
        }
        RowReverse(
            Modifier.fillMaxWidth().absolutePadding(left = 20.dp, right = 20.dp).padding(top = 10.dp, bottom = 30.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoursePulse(width = 50.dp, height = 50.dp, radius = 25.dp, modifier = Modifier.absolutePadding(left = 15.dp))
            CoursePulse(width = 150.dp, height = 24.dp, radius = 12.dp)
        }
        Column(Modifier.absolutePadding(left = 20.dp, right = 20.dp)) {
            repeat(6) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .absolutePadding(left = 5.dp, right = 5.dp)
                        .padding(bottom = 14.dp)
                        .background(Color(0xFFF8FAFC).copy(alpha = 0.3f), RoundedCornerShape(22.dp))
                        .padding(18.dp)
                ) {
                    RowReverse(Modifier.padding(bottom = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                        CoursePulse(width = 44.dp, height = 44.dp, radius = 22.dp, modifier = Modifier.absolutePadding(left = 12.dp))
                        CoursePulse(height = 17.dp, fraction = 0.7f, radius = 8.dp)
                    }
                    Column(Modifier.absolutePadding(left = 15.dp, right = 15.dp)) {
                        CoursePulse(height = 16.dp, fraction = 0.9f, radius = 8.dp, modifier = Modifier.padding(bottom = 8.dp))
                        CoursePulse(height = 16.dp, fraction = 0.6f, radius = 8.dp)
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().absolutePadding(left = 20.dp, right = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CoursePulse(height = 56.dp, fraction = 0.92f, radius = 30.dp, modifier = Modifier.padding(bottom = 18.dp))
            CoursePulse(height = 48.dp, fraction = 0.7f, radius = 25.dp)
        }
    }
}
