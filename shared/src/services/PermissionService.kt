package ir.farimod.app.services

import ir.farimod.app.data.model.UserData

/** Port of `app/services/PermissionService.tsx` (permission keys are the same strings as in RN). */
object PermissionService {
    data class Permissions(
        val allowAboutMeText: Boolean = false,
        val allowAddImageGallery: Boolean = false,
        val allowAddPortfolio: Boolean = false,
        val allowAddProduct: Boolean = false,
        val allowAddCourse: Boolean = false,
        val allowAddBlogPost: Boolean = false,
        val allowAddDocument: Boolean = false,
        val showContactInfo: Boolean = false,
        val showBlueTick: Boolean = false
    ) {
        fun toMap(): Map<String, Boolean> = mapOf(
            "allowAboutMeText" to allowAboutMeText,
            "allowAddImageGallery" to allowAddImageGallery,
            "allowAddPortfolio" to allowAddPortfolio,
            "allowAddProduct" to allowAddProduct,
            "allowAddCourse" to allowAddCourse,
            "allowAddBlogPost" to allowAddBlogPost,
            "allowAddDocument" to allowAddDocument,
            "showContactInfo" to showContactInfo,
            "showBlueTick" to showBlueTick
        )
    }

    data class SubscriptionInfoData(
        val planId: Int?,
        val planName: String?,
        val startDate: String?,
        val finishDate: String?,
        val remainingDays: Int?,
        val isInfinity: Boolean,
        val totalDays: Int?,
        val subscriptionPrice: Long?,
        val finalAmount: Long?
    )

    fun getPermissions(userData: UserData?): Permissions {
        val plan = userData?.ActiveSubscriptionPlan ?: return Permissions()
        return Permissions(
            allowAboutMeText = plan.PlanOption_AllowAboutMeText,
            allowAddImageGallery = plan.PlanOption_AllowAddImageGallery,
            allowAddPortfolio = plan.PlanOption_AllowAddPortfolio,
            allowAddProduct = plan.PlanOption_AllowAddProduct,
            allowAddCourse = plan.PlanOption_AllowAddCourse,
            allowAddBlogPost = plan.PlanOption_AllowAddBlogPost,
            allowAddDocument = plan.PlanOption_AllowAddDocument,
            showContactInfo = plan.PlanOption_ShowContanctInfo,
            showBlueTick = plan.PlanOption_ShowBlueTick
        )
    }

    /** `permission == null` → always allowed (like RN). */
    fun hasPermission(userData: UserData?, permission: String?): Boolean {
        if (permission == null) return true
        return getPermissions(userData).toMap()[permission] ?: false
    }

    fun getSubscriptionInfo(userData: UserData?): SubscriptionInfoData? {
        val plan = userData?.ActiveSubscriptionPlan ?: return null
        return SubscriptionInfoData(
            planId = plan.SubscriptionPlanId,
            planName = plan.SubscriptionPlanName,
            startDate = plan.ShamsiStartDate,
            finishDate = plan.ShamsiFinishDate,
            remainingDays = plan.RemainingDaysToFinishDate,
            isInfinity = plan.IsInfinityPlan,
            totalDays = plan.SubscriptionTotalDays,
            subscriptionPrice = plan.SubscriptionPrice,
            finalAmount = plan.FinalAmount
        )
    }

    fun isSubscriptionActive(userData: UserData?): Boolean {
        val plan = userData?.ActiveSubscriptionPlan ?: return false
        if (plan.IsInfinityPlan) return true
        return (plan.RemainingDaysToFinishDate ?: 0) > 0
    }

    fun getPermissionMessage(permission: String?): String = when (permission) {
        "allowAboutMeText" -> "برای ویرایش متن درباره من، نیاز به ارتقای اشتراک دارید"
        "allowAddImageGallery" -> "برای افزودن تصاویر به گالری، نیاز به ارتقای اشتراک دارید"
        "allowAddPortfolio" -> "برای افزودن نمونه کار، نیاز به ارتقای اشتراک دارید"
        "allowAddProduct" -> "برای افزودن محصول، نیاز به ارتقای اشتراک دارید"
        "allowAddCourse" -> "برای افزودن دوره، نیاز به ارتقای اشتراک دارید"
        "allowAddBlogPost" -> "برای انتشار پست، نیاز به ارتقای اشتراک دارید"
        "allowAddDocument" -> "برای افزودن مدرک، نیاز به ارتقای اشتراک دارید"
        else -> "دسترسی محدود - برای استفاده از این بخش، نیاز به ارتقای اشتراک دارید"
    }

    fun getPermissionIcon(hasPermission: Boolean): String = if (hasPermission) "check-circle" else "lock"
    fun getPermissionIconColor(hasPermission: Boolean): String = if (hasPermission) "#4CAF50" else "#999"

    fun getSubscriptionLevel(userData: UserData?): String {
        if (userData?.ActiveSubscriptionPlan == null) return "none"
        val count = getPermissions(userData).toMap().values.count { it }
        return when {
            count == 0 -> "none"
            count <= 3 -> "basic"
            count <= 6 -> "premium"
            else -> "unlimited"
        }
    }
}
