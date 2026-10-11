package ir.farimod.app.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ir.farimod.app.network.Api
import ir.farimod.app.network.bool
import ir.farimod.app.network.get
import ir.farimod.app.network.jsonOf
import ir.farimod.app.ui.RatingOption
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.math.abs
import kotlin.math.floor

// ──────────────────────────────────────────────────────────────────────────────
// app/screens/product-detail/utils/transformContentReviewToRatingOptions.tsx
// ──────────────────────────────────────────────────────────────────────────────

/**
 * RN `transformContentReviewToRatingOptions(contentReviewList)` (product-detail version).
 * [contentReviewList] is the API `ContentReviewItemList` array (`null` / empty → the 4 default options).
 */
fun transformContentReviewToRatingOptions(contentReviewList: JsonElement?): List<RatingOption> {
    val items = (contentReviewList as? JsonArray)?.toList()
    if (items == null || items.isEmpty()) {
        return listOf(
            RatingOption(
                id = "quality",
                title = "کیفیت محصول",
                subtitle = "کیفیت کلی و ساخت",
                contentReviewItemId = "quality"
            ),
            RatingOption(
                id = "price",
                title = "ارزش خرید",
                subtitle = "تناسب قیمت و کیفیت",
                contentReviewItemId = "price"
            ),
            RatingOption(
                id = "design",
                title = "طراحی",
                subtitle = "زیبایی و جذابیت",
                contentReviewItemId = "design"
            ),
            RatingOption(
                id = "satisfaction",
                title = "رضایت کلی",
                subtitle = "رضایت از خرید",
                contentReviewItemId = "satisfaction"
            )
        )
    }

    return items
        .filter { item -> item["Active"].bool }
        .sortedWith(Comparator { a, b ->
            // a.ShowOrder - b.ShowOrder (NaN → treated as equal)
            val d = jsToNumber(a["ShowOrder"]) - jsToNumber(b["ShowOrder"])
            if (a["ShowOrder"] == null || b["ShowOrder"] == null || d.isNaN()) 0
            else if (d < 0) -1 else if (d > 0) 1 else 0
        })
        .map { item ->
            val average = item["CalculatedAverageRating"]
            val rawId = (item as? JsonObject)?.get("ContentReviewItemId")
            RatingOption(
                id = "review_" + (if (rawId == null) "undefined" else jsToString(rawId)),
                title = item["Text"]?.let { jsToString(it) } ?: "",
                subtitle = "",
                contentReviewItemId = item["ContentReviewItemId"],
                showOrder = item["ShowOrder"]?.let { jsToNumber(it) },
                averageRating = if (jsTruthy(average)) jsToNumber(average) else 0.0
            )
        }
}

// ──────────────────────────────────────────────────────────────────────────────
// app/utils/ContentReviewForm.tsx
//   (a headless component: it holds the ratings state and POSTs them; it renders nothing)
// ──────────────────────────────────────────────────────────────────────────────

/** State holder of RN `ContentReviewForm`. */
class ContentReviewFormState(
    private val contentId: Any?,
    private val onSubmit: ((Map<String, Double>) -> Unit)?
) {
    /** RN `ratings` state: `{ [itemId]: rating }`. */
    var ratings: Map<String, Double> by mutableStateOf(emptyMap())
        private set

    /** RN `handleRating(rating, itemId)`. */
    fun handleRating(rating: Double, itemId: String) {
        ratings = ratings + (itemId to rating)
    }

    /**
     * RN `handleSubmit()` — POST `MemberReview/SendReview` (no Authorization header, like the axios call).
     * Errors are swallowed (RN: `console.error`).
     */
    suspend fun handleSubmit() {
        val reviewItemRatings = ratings.entries.map { (id, rating) ->
            jsonOf(
                "ContentReviewItemId" to jsParseInt(id),
                "Rating" to (if (rating == floor(rating) && abs(rating) < 1e15) rating.toLong() else rating)
            )
        }

        if (reviewItemRatings.isEmpty()) return

        val payload = jsonOf(
            "ContnetId" to contentId,
            "reviewItemRatings" to reviewItemRatings
        )

        try {
            val response = Api.post("MemberReview/SendReview", body = payload)
            // axios rejects on non-2xx
            if (!response.ok) throw Exception("Request failed with status code ${response.status}")
            onSubmit?.invoke(ratings)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // console.error('Error submitting review:', error)
        }
    }
}

/** RN `<ContentReviewForm ContentId reviewItemList onSubmit />` state (the RN component renders no UI). */
@Composable
fun rememberContentReviewForm(
    contentId: Any?,
    onSubmit: ((Map<String, Double>) -> Unit)? = null
): ContentReviewFormState = remember(contentId) { ContentReviewFormState(contentId, onSubmit) }
