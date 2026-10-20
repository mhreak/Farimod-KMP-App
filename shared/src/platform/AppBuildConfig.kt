package ir.farimod.app.platform

/**
 * Distribution targets for Farimod builds:
 *
 * 1 = WEBSITE (خروجی سایت - درگاه مستقیم اختصاصی زرین‌پال / بدون وابستگی به مایکت یا بازار)
 * 2 = CAFE_BAZAAR (خروجی کافه بازار - آماده برای درگاه پرداخت بازار در آینده)
 * 3 = MYKET (خروجی مایکت - پرداخت درون‌برنامه‌ای مایکت)
 */
enum class AppDistribution(
    val downloadSource: Int,
    val titleFa: String,
    val storeNameFa: String,
    val storeSchemeUrl: String,
    val storeWebUrl: String
) {
    WEBSITE(
        downloadSource = 1,
        titleFa = "نسخه وب‌سایت (درگاه اختصاصی مستقیم)",
        storeNameFa = "سایت فریمد",
        storeSchemeUrl = "https://farimod.ir",
        storeWebUrl = "https://farimod.ir"
    ),
    CAFE_BAZAAR(
        downloadSource = 2,
        titleFa = "نسخه کافه بازار",
        storeNameFa = "کافه بازار",
        storeSchemeUrl = "bazaar://details?id=ir.farimod.app",
        storeWebUrl = "https://cafebazaar.ir/app/ir.farimod.app"
    ),
    MYKET(
        downloadSource = 3,
        titleFa = "نسخه مایکت",
        storeNameFa = "مایکت",
        storeSchemeUrl = "myket://details?id=ir.farimod.app",
        storeWebUrl = "https://myket.ir/app/ir.farimod.app"
    );

    val isWebsite: Boolean get() = this == WEBSITE
    val isBazaar: Boolean get() = this == CAFE_BAZAAR
    val isMyket: Boolean get() = this == MYKET
}

/**
 * Global build and distribution configuration.
 *
 * برای تغییر نوع بیلد، مقدار [activeDistribution] را روی یکی از گزینه‌های زیر قرار دهید:
 * - [AppDistribution.WEBSITE]     -> downloadSource = 1 (درگاه مستقیم وب / زرین‌پال)
 * - [AppDistribution.CAFE_BAZAAR] -> downloadSource = 2 (کافه بازار)
 * - [AppDistribution.MYKET]       -> downloadSource = 3 (مایکت / پرداخت درون‌برنامه‌ای مایکت)
 */
object AppBuildConfig {
    /** The active distribution flavor for this build */
    var activeDistribution: AppDistribution = AppDistribution.MYKET

    /** downloadSource code for AppRun/NotifyAppRun (1: Web, 2: Bazaar, 3: Myket) */
    val downloadSource: Int get() = activeDistribution.downloadSource
}
