package ir.farimod.app.billing.myket

/**
 * Configuration and constants for Myket In-App Billing.
 * Documented at: https://myket.ir/kb/pages/java/
 */
object MyketConfig {
    /**
     * User's RSA Public Key provided from Myket Developer Console.
     */
    const val PUBLIC_KEY: String =
        "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQChXXM0/itjd4zMeRiTBoASWlgVRRSX4of23aNP7mXLHwzn5P7nOIcD9ObrE0fIludBGRffeSgkAmMDeqC6SY6fou5WJMVbOvZi7bbG2s9dbIeVy6TseHf0u/Y05e6RxCpUqDZ6UNJfwN9gyLcTnJkim9U9be7ibkS421RPcz8PZwIDAQAB"

    /**
     * Myket market application ID and package name.
     */
    const val MARKET_PACKAGE: String = "ir.mservices.market"

    /**
     * In-App Billing Service bind action for Myket.
     */
    const val BIND_ACTION: String = "ir.mservices.market.InAppBillingService.BIND"

    /**
     * In-App Billing permission required by Myket.
     */
    const val BILLING_PERMISSION: String = "ir.mservices.market.BILLING"

    /**
     * In-App Billing API version (v3 is standard across Myket and Play Store).
     */
    const val API_VERSION: Int = 3

    /**
     * Product types.
     */
    const val ITEM_TYPE_INAPP: String = "inapp"
    const val ITEM_TYPE_SUBS: String = "subs"

    // Standard IAB response codes
    const val BILLING_RESPONSE_RESULT_OK: Int = 0
    const val BILLING_RESPONSE_RESULT_USER_CANCELED: Int = 1
    const val BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE: Int = 3
    const val BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE: Int = 4
    const val BILLING_RESPONSE_RESULT_DEVELOPER_ERROR: Int = 5
    const val BILLING_RESPONSE_RESULT_ERROR: Int = 6
    const val BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED: Int = 7
    const val BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED: Int = 8

    // Helper error codes (negative codes for local errors)
    const val IABHELPER_ERROR_BASE: Int = -1000
    const val IABHELPER_REMOTE_EXCEPTION: Int = -1001
    const val IABHELPER_BAD_RESPONSE: Int = -1002
    const val IABHELPER_VERIFICATION_FAILED: Int = -1003
    const val IABHELPER_SEND_INTENT_FAILED: Int = -1004
    const val IABHELPER_USER_CANCELLED: Int = -1005
    const val IABHELPER_UNKNOWN_PURCHASE_RESPONSE: Int = -1006
    const val IABHELPER_MISSING_TOKEN: Int = -1007
    const val IABHELPER_UNKNOWN_ERROR: Int = -1008
    const val IABHELPER_MYKET_NOT_INSTALLED: Int = -1009

    // Bundle keys
    const val RESPONSE_CODE: String = "RESPONSE_CODE"
    const val DETAILS_LIST: String = "DETAILS_LIST"
    const val BUY_INTENT: String = "BUY_INTENT"
    const val INAPP_PURCHASE_DATA: String = "INAPP_PURCHASE_DATA"
    const val INAPP_DATA_SIGNATURE: String = "INAPP_DATA_SIGNATURE"
    const val INAPP_PURCHASE_ITEM_LIST: String = "INAPP_PURCHASE_ITEM_LIST"
    const val INAPP_PURCHASE_DATA_LIST: String = "INAPP_PURCHASE_DATA_LIST"
    const val INAPP_DATA_SIGNATURE_LIST: String = "INAPP_DATA_SIGNATURE_LIST"
    const val INAPP_CONTINUATION_TOKEN: String = "INAPP_CONTINUATION_TOKEN"
    const val ITEM_ID_LIST: String = "ITEM_ID_LIST"

    /**
     * Human-readable Persian & technical message for response codes.
     */
    fun getResponseDescription(code: Int): String = when (code) {
        BILLING_RESPONSE_RESULT_OK -> "عملیات با موفقیت انجام شد (OK)"
        BILLING_RESPONSE_RESULT_USER_CANCELED -> "پرداخت توسط کاربر لغو شد"
        BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE -> "سرویس پرداخت مایکت در دسترس نیست. لطفا مایکت را باز کرده و ورود به حساب را بررسی کنید"
        BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE -> "این محصول برای خرید در دسترس نیست (شناسه محصول نامعتبر است)"
        BILLING_RESPONSE_RESULT_DEVELOPER_ERROR -> "خطای تنظیمات در درخواست خرید (Developer Error)"
        BILLING_RESPONSE_RESULT_ERROR -> "خطا در برقراری ارتباط با سرویس پرداخت مایکت"
        BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED -> "این محصول قبلاً خریداری شده است و هم‌اکنون فعال است"
        BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED -> "این محصول در لیست خریدهای شما وجود ندارد یا قبلاً مصرف شده است"
        IABHELPER_MYKET_NOT_INSTALLED -> "برنامه مایکت روی گوشی شما نصب نیست"
        IABHELPER_VERIFICATION_FAILED -> "تایید امضای دیجیتالی خرید ناموفق بود (اعتبارسنجی امنیتی نامعتبر)"
        IABHELPER_BAD_RESPONSE -> "پاسخ نامعتبر از سرویس مایکت دریافت شد"
        IABHELPER_REMOTE_EXCEPTION -> "ارتباط با سرویس پرداخت مایکت قطع شد"
        else -> "کد خطا: $code"
    }
}
