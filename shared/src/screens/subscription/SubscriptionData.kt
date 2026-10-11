package ir.farimod.app.screens.subscription

import androidx.compose.ui.graphics.Color
import ir.farimod.app.network.bool
import ir.farimod.app.network.double
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.jsonOf
import ir.farimod.app.network.list
import ir.farimod.app.network.str
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/** `modernColors` of SubscriptionScreen.tsx */
internal object SubColors {
    val primary = Color(0xFF667EEA)
    val primaryDark = Color(0xFF5A67D8)
    val secondary = Color(0xFF764BA2)
    val dark = Color(0xFF1A202C)
    val medium = Color(0xFF2D3748)
    val light = Color(0xFFF7FAFC)
    val success = Color(0xFF38A169)
    val warning = Color(0xFFED8936)
    val error = Color(0xFFE53E3E)
    val info = Color(0xFF3182CE)
}

/** One entry of `subscriptionOptions`. */
internal data class SubOption(
    val value: String,
    val label: String,
    val price: Double,
    val duration: String,
    val icon: String
) {
    fun toJson(): JsonObject = jsonOf(
        "value" to value,
        "label" to label,
        "price" to price,
        "duration" to duration,
        "icon" to icon
    )
}

/** RN `ISubscriptionPlan` (+ optional `selectedOption` added by `onPress({ ...item, selectedOption })`). */
internal data class SubPlan(
    val id: Int,
    val title: String,
    val color: String,
    val gradientColors: List<String>,
    val glassColors: List<String>,
    val icon: String,
    val isPopular: Boolean,
    val badgeBackgroundColor: String?,
    val badgeText: String?,
    val isActive: Boolean,
    val features: List<String>,
    val subscriptionOptions: List<SubOption>,
    val selectedOption: SubOption? = null
) {
    /** The plain JS object RN passes as `route.params.plan`. */
    fun toJson(): JsonObject = jsonOf(
        "id" to id,
        "title" to title,
        "color" to color,
        "gradientColors" to gradientColors,
        "glassColors" to glassColors,
        "icon" to icon,
        "isPopular" to isPopular,
        "badgeBackgroundColor" to badgeBackgroundColor,
        "badgeText" to badgeText,
        "isActive" to isActive,
        "features" to features,
        "subscriptionOptions" to subscriptionOptions.map { it.toJson() },
        "selectedOption" to selectedOption?.toJson()
    )
}

/** RN `IActiveSubscription` (only the fields the screen reads). */
internal data class ActiveSub(
    val subscriptionPlanId: Int?,
    val subscriptionPlanName: String,
    val shamsiFinishDate: String?,
    val remainingDays: Double,
    val totalDays: Double
)

internal fun parseActiveSub(data: JsonElement?): ActiveSub = ActiveSub(
    subscriptionPlanId = data["SubscriptionPlanId"].int,
    subscriptionPlanName = data["SubscriptionPlanName"].str ?: "",
    shamsiFinishDate = data["ShamsiFinishDate"].str,
    remainingDays = data["RemainingDaysToFinishDate"].double ?: 0.0,
    totalDays = data["SubscriptionTotalDays"].double ?: 0.0
)

/** Transform of one API plan to [SubPlan] exactly like `fetchSubscriptionPlans` in RN. */
internal fun transformPlan(plan: JsonElement): SubPlan {
    val features = mutableListOf<String>()
    if (plan["PlanOption_ShowContanctInfo"].bool) features.add("نمایش اطلاعات تماس")
    if (plan["PlanOption_ShowBlueTick"].bool) features.add("تیک آبی تایید هویت")
    if (plan["PlanOption_AllowAboutMeText"].bool) features.add("امکان افزودن درباره من")
    if (plan["PlanOption_AllowAddImageGallery"].bool) features.add("گالری تصاویر")
    if (plan["PlanOption_AllowAddPortfolio"].bool) features.add("نمونه کارها")
    if (plan["PlanOption_AllowAddProduct"].bool) features.add("افزودن محصول")
    if (plan["PlanOption_AllowAddCourse"].bool) features.add("افزودن دوره آموزشی")
    if (plan["PlanOption_AllowAddBlogPost"].bool) features.add("انتشار مقاله")
    if (plan["PlanOption_AllowAddDocument"].bool) features.add("اسناد و مدارک")
    if (features.isEmpty()) features.add("امکانات محدود")

    val options = mutableListOf<SubOption>()
    if (plan["AllowFourteenDaysSubscription"].bool && plan["FourteenDaysSubscriptionPrice"] != null) {
        options.add(SubOption("fourteen_days", "۱۴ روزه", plan["FourteenDaysSubscriptionPrice"].double ?: 0.0, "۱۴ روزه", "schedule"))
    }
    if (plan["AllowOneMonthSubscription"].bool && plan["OneMonthSubscriptionPrice"] != null) {
        options.add(SubOption("one_month", "یک ماهه", plan["OneMonthSubscriptionPrice"].double ?: 0.0, "یک ماهه", "calendar-today"))
    }
    if (plan["AllowThreeMonthsSubscription"].bool && plan["ThreeMonthsSubscriptionPrice"] != null) {
        options.add(SubOption("three_months", "سه ماهه", plan["ThreeMonthsSubscriptionPrice"].double ?: 0.0, "سه ماهه", "calendar-view-month"))
    }
    if (plan["AllowSixMonthsSubscription"].bool && plan["SixMonthsSubscriptionPrice"] != null) {
        options.add(SubOption("six_months", "شش ماهه", plan["SixMonthsSubscriptionPrice"].double ?: 0.0, "شش ماهه", "date-range"))
    }
    if (plan["AllowAnnualSubscription"].bool && plan["AnnualSubscriptionPrice"] != null) {
        options.add(SubOption("annual", "سالانه", plan["AnnualSubscriptionPrice"].double ?: 0.0, "سالانه", "event"))
    }

    val color = plan["ColorHexCode"].str ?: ""
    val iconName = plan["IconName"].str
    return SubPlan(
        id = plan["SubscriptionPlanId"].int ?: 0,
        title = plan["Name"].str ?: "",
        color = color,
        gradientColors = listOf(color, color),
        glassColors = listOf("${color}20", "${color}10"),
        icon = if (iconName.isNullOrEmpty()) "star" else iconName,
        isPopular = plan["ShowBadge"].bool,
        badgeBackgroundColor = plan["BadgeBackgroundColorHexCode"].str,
        badgeText = plan["BadgeText"].str,
        isActive = false,
        features = features,
        subscriptionOptions = options
    )
}

internal fun transformPlans(data: JsonElement?): List<SubPlan> = data.list.map { transformPlan(it) }

/**
 * CSS-like hex color as used by RN: `#RGB`, `#RRGGBB`, `#RRGGBBAA` (alpha LAST, e.g. `"#667eea" + "20"`).
 * Unparseable values render as transparent (RN renders nothing / transparent for invalid colors).
 */
internal fun subHex(value: String?): Color {
    val s = (value ?: return Color.Transparent).trim().removePrefix("#")
    return try {
        when (s.length) {
            3 -> {
                val r = s.substring(0, 1).repeat(2).toInt(16)
                val g = s.substring(1, 2).repeat(2).toInt(16)
                val b = s.substring(2, 3).repeat(2).toInt(16)
                Color(r / 255f, g / 255f, b / 255f, 1f)
            }
            6 -> Color(
                s.substring(0, 2).toInt(16) / 255f,
                s.substring(2, 4).toInt(16) / 255f,
                s.substring(4, 6).toInt(16) / 255f,
                1f
            )
            8 -> Color(
                s.substring(0, 2).toInt(16) / 255f,
                s.substring(2, 4).toInt(16) / 255f,
                s.substring(4, 6).toInt(16) / 255f,
                s.substring(6, 8).toInt(16) / 255f
            )
            else -> Color.Transparent
        }
    } catch (e: NumberFormatException) {
        Color.Transparent
    }
}
