package ir.farimod.app.network

object ApiConfig {
    /**
     * Primary backend API Base URL.
     * Uses HTTPS directly because the Farimod IIS server issues HTTP 307 Temporary Redirects
     * for all cleartext HTTP calls.
     */
    const val BASE_URL = "https://my.farimod.ir/api/MobileApp/"
    const val HTTP_BASE_URL = "http://my.farimod.ir/api/MobileApp/"
    const val MEDIA_BASE_URL = "https://my.farimod.ir/"
    const val TIMEOUT_MILLIS = 30_000L

    fun resolveMediaUrl(pathOrUrl: String?): String? {
        if (pathOrUrl.isNullOrBlank()) return null
        val clean = pathOrUrl.trim()
        if (clean.endsWith("/")) return null
        if (clean.startsWith("https://")) return clean
        if (clean.startsWith("http://")) {
            // Upgrade farimod.ir domains to https to avoid 307 redirects for media
            if (clean.contains("farimod.ir")) {
                return clean.replace("http://", "https://")
            }
            return clean
        }
        val relative = clean.removePrefix("/")
        return "$MEDIA_BASE_URL$relative"
    }

    object Endpoints {
        const val SEND_OTP_SMS = "MobileAccount/SendOTPSMS"
        const val LOGIN = "MobileAccount/Login"
        const val SIGNUP_FIRST_STEP = "MobileAccount/SignupFirstStep"
        const val SEND_SIGNUP_OTP_SMS = "MobileAccount/SendSignupOTPSMS"
        const val VALIDATE_SIGNUP_OTP = "MobileAccount/ValidateSignupOTP"
        const val GET_PROFILE_INFO = "MemberInfo/GetProfileInfoToEdit"
        const val GET_MEMBER_GROUPS = "MemberGroup/GetMemberGroups"
        const val NOTIFY_APP_RUN = "AppRun/NotifyAppRun"
        const val GET_SUBSCRIPTION_PLANS = "SubscriptionPlan/GetAllActive"
        const val APPLY_DISCOUNT = "SubscriptionPlan/ApplyDiscountCode"
        const val BUY_SUBSCRIPTION = "SubscriptionPlan/BuySubscriptionPlan"
        const val GET_IMAGE_GALLERIES = "ImageGallery/GetAll"
    }
}
