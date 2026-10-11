package ir.farimod.app.hooks

import androidx.compose.runtime.Composable
import ir.farimod.app.network.bool
import ir.farimod.app.network.get
import ir.farimod.app.utils.jsToNumber
import kotlinx.serialization.json.JsonElement

/**
 * Port of `app/hooks/home/useHomeData.ts`. Every hook returns the same `{ data, loading, error, refetch }`
 * holder as [useFetchData]; items are the API JSON objects.
 */

/** JS `a - b` used inside `sort` comparators (NaN is treated as 0 like `Array.prototype.sort`). */
private fun jsDiff(a: JsonElement?, b: JsonElement?): Int {
    if (a == null || b == null) return 0
    val d = jsToNumber(a) - jsToNumber(b)
    return if (d.isNaN()) 0 else if (d < 0) -1 else if (d > 0) 1 else 0
}

private val slidesTransform: (List<JsonElement>) -> List<JsonElement> = { items ->
    items
        .filter { slide -> slide["Active"].bool }
        .sortedWith(Comparator { a, b ->
            val aOrder = a["ShowOrder"].bool
            val bOrder = b["ShowOrder"].bool
            if (aOrder && !bOrder) -1
            else if (!aOrder && bOrder) 1
            else jsDiff(a["HomePageSlideId"], b["HomePageSlideId"])
        })
}

private val activeOnlyTransform: (List<JsonElement>) -> List<JsonElement> = { items ->
    items.filter { it["Active"].bool }
}

private val memberGroupsTransform: (List<JsonElement>) -> List<JsonElement> = { items ->
    items.filter { group ->
        group["Active"].bool && (group["MemberCount"]?.let { jsToNumber(it) > 0 } ?: false)
    }
}

@Composable
fun useHomePageSlides(): FetchDataState = useFetchData(
    url = "HomePageSlide/GetAll",
    errorMessage = "خطا در دریافت اطلاعات اسلایدر صفحه اصلی",
    filterOrTransform = slidesTransform
)

@Composable
fun usePortfolios(): FetchDataState = useFetchData(
    url = "Portfolio/GetAllLast",
    errorMessage = "خطا در دریافت اطلاعات نمونه کارها",
    filterOrTransform = activeOnlyTransform
)

@Composable
fun useImageGalleries(): FetchDataState = useFetchData(
    url = "ImageGallery/GetAllLast",
    errorMessage = "خطا در دریافت اطلاعات گالری‌ها",
    filterOrTransform = activeOnlyTransform
)

@Composable
fun useBlogPosts(): FetchDataState = useFetchData(
    url = "BlogPost/GetAllLast",
    errorMessage = "خطا در دریافت اطلاعات مقالات",
    filterOrTransform = activeOnlyTransform
)

@Composable
fun useMemberGroups(): FetchDataState = useFetchData(
    url = "MemberGroup/GetAll?currentPage=1&pageSize=100",
    errorMessage = "خطا در دریافت اطلاعات گروه‌های اصلی",
    filterOrTransform = memberGroupsTransform
)
