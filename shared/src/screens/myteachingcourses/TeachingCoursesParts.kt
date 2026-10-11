package ir.farimod.app.screens.myteachingcourses

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ir.farimod.app.network.Api
import ir.farimod.app.network.ApiNetworkException
import ir.farimod.app.network.int
import ir.farimod.app.network.list
import ir.farimod.app.ui.RNTimingEasing
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.rnShadow
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject

internal const val TEACHING_PAGE_SIZE = 20

internal class TeachingCoursesPager {
    var items by androidx.compose.runtime.mutableStateOf<List<JsonObject>>(emptyList())
    var page by androidx.compose.runtime.mutableStateOf(1)
    var hasMore by androidx.compose.runtime.mutableStateOf(true)
    var total by androidx.compose.runtime.mutableStateOf(0)
    var loading by androidx.compose.runtime.mutableStateOf(false)
    var error by androidx.compose.runtime.mutableStateOf<String?>(null)

    suspend fun fetch(memberId: String, newPage: Int, pageSize: Int = TEACHING_PAGE_SIZE) {
        try {
            loading = true
            error = null
            val response = Api.get(
                "Course/GetAll?filterMemberId=$memberId&currentPage=$newPage&pageSize=$pageSize"
            )
            if (!response.ok) throw Exception("HTTP error! status: ${response.status}")
            val result = response.json
            val data = result.list("Data").mapNotNull { it as? JsonObject }
            items = if (newPage == 1) data else items + data
            total = result.int("Total") ?: 0
            page = newPage
            hasMore = data.size == pageSize && data.isNotEmpty()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: if (e is ApiNetworkException) e.message else "error"
            if (newPage == 1) items = emptyList()
            total = 0
        } finally {
            loading = false
        }
    }
}

@Composable
internal fun teachingPulseColor(): Color {
    val transition = rememberInfiniteTransition(label = "teachingSkel")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = RNTimingEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "teachingPulse"
    )
    return lerp(Color(0xFFE0E0E0), Color(0xFFF0F0F0), t)
}

@Composable
internal fun TeachingPulse(
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp,
    fraction: Float? = null,
    radius: Dp = 8.dp
) {
    val color = teachingPulseColor()
    val shape = RoundedCornerShape(radius)
    val sizeMod = when {
        width != null -> Modifier.width(width)
        fraction != null -> Modifier.fillMaxWidth(fraction)
        else -> Modifier.fillMaxWidth()
    }
    Box(
        modifier
            .then(sizeMod)
            .height(height)
            .background(color, shape)
    )
}

/** Port of `CourseCardSkeleton` in MyTeachingCoursesScreen.tsx. */
@Composable
internal fun TeachingCourseCardSkeleton() {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp, max = 350.dp)
            .rnShadow(4.dp, shape, Color(0xFF797979))
            .clip(shape)
            .background(Color.White)
    ) {
        Box(Modifier.fillMaxWidth().height(180.dp)) {
            TeachingPulse(modifier = Modifier.fillMaxSize(), height = 180.dp, radius = 0.dp)
        }
        Column(Modifier.weight(1f).padding(12.dp)) {
            RowReverse(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                TeachingPulse(
                    width = 32.dp,
                    height = 32.dp,
                    radius = 8.dp,
                    modifier = Modifier.absolutePadding(left = 8.dp)
                )
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    TeachingPulse(height = 13.dp, fraction = 0.9f, radius = 4.dp, modifier = Modifier.padding(bottom = 4.dp))
                    TeachingPulse(height = 13.dp, fraction = 0.7f, radius = 4.dp, modifier = Modifier.padding(bottom = 4.dp))
                    TeachingPulse(height = 12.dp, fraction = 0.4f, radius = 4.dp, modifier = Modifier.padding(bottom = 3.dp))
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 5.dp)
                    .drawBehind {
                        drawRect(Color(0xFFF0F0F0), size = Size(size.width, 1.dp.toPx()))
                    }
                    .padding(top = 5.dp)
            ) {
                RowReverse(
                    Modifier.fillMaxWidth().padding(bottom = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TeachingPulse(
                        width = 14.dp,
                        height = 14.dp,
                        radius = 7.dp,
                        modifier = Modifier.absolutePadding(left = 4.dp)
                    )
                    TeachingPulse(width = 60.dp, height = 11.dp, radius = 4.dp)
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TeachingPulse(height = 11.dp, fraction = 0.85f, radius = 4.dp, modifier = Modifier.padding(bottom = 2.dp))
                }
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TeachingPulse(height = 11.dp, fraction = 0.6f, radius = 4.dp)
                }
            }
        }
    }
}
