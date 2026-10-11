package ir.farimod.app.services

import ir.farimod.app.platform.canOpenUrl
import ir.farimod.app.platform.isIOS
import ir.farimod.app.platform.openUrl
import ir.farimod.app.ui.AlertButton
import ir.farimod.app.ui.RNAlert

/** `{ success: true }` / `{ success: false, error }` */
data class LinkResult(val success: Boolean, val error: String? = null)

/** Port of `app/services/LinkHandlerService.js` (not used by the RN app flow, kept for parity). */
object LinkHandlerService {
    // Same placeholder as RN (`com.yourcompany.yourapp`)
    const val APP_PACKAGE_NAME = "com.yourcompany.yourapp"

    /** `Linking.openURL` rejects when the URL cannot be opened. */
    private fun openOrThrow(url: String) {
        if (!openUrl(url)) throw IllegalStateException("Unable to open URL: $url")
    }

    fun openUpdateLink(cafeBazarLink: String?, downloadLink: String?): LinkResult {
        return try {
            val updateUrl: String
            if (!cafeBazarLink.isNullOrEmpty()) {
                updateUrl = cafeBazarLink
            } else if (!downloadLink.isNullOrEmpty()) {
                updateUrl = downloadLink
            } else {
                // اگر لینک مستقیم موجود نیست، به فروشگاه هدایت کن
                return openAppStore()
            }

            if (canOpenUrl(updateUrl)) {
                openOrThrow(updateUrl)
                LinkResult(success = true)
            } else {
                // اگر لینک باز نشد، به فروشگاه هدایت کن
                openAppStore()
            }
        } catch (e: Exception) {
            LinkResult(success = false, error = "خطا در باز کردن لینک بروزرسانی")
        }
    }

    fun openAppStore(): LinkResult {
        return try {
            if (!isIOS) {
                // ابتدا سعی کن CafeBazar را باز کنی
                val cafeBazarUrl = "bazaar://details?id=$APP_PACKAGE_NAME"
                if (canOpenUrl(cafeBazarUrl)) {
                    openOrThrow(cafeBazarUrl)
                    return LinkResult(success = true)
                }

                // اگر CafeBazar موجود نبود، Google Play را امتحان کن
                val playStoreUrl = "market://details?id=$APP_PACKAGE_NAME"
                if (canOpenUrl(playStoreUrl)) {
                    openOrThrow(playStoreUrl)
                    return LinkResult(success = true)
                }

                // اگر هیچ‌کدام کار نکرد، لینک وب Google Play را باز کن
                val playStoreWebUrl = "https://play.google.com/store/apps/details?id=$APP_PACKAGE_NAME"
                openOrThrow(playStoreWebUrl)
                LinkResult(success = true)
            } else {
                // برای iOS - App Store
                val appStoreUrl = "itms-apps://itunes.apple.com/app/idYOUR_APP_ID" // App ID خود را جایگزین کنید
                if (canOpenUrl(appStoreUrl)) {
                    openOrThrow(appStoreUrl)
                    return LinkResult(success = true)
                }

                // لینک وب App Store
                val appStoreWebUrl = "https://apps.apple.com/app/idYOUR_APP_ID"
                openOrThrow(appStoreWebUrl)
                LinkResult(success = true)
            }
        } catch (e: Exception) {
            LinkResult(success = false, error = "خطا در باز کردن فروشگاه اپ")
        }
    }

    fun openCustomUrl(url: String): LinkResult {
        return try {
            if (canOpenUrl(url)) {
                openOrThrow(url)
                LinkResult(success = true)
            } else {
                LinkResult(success = false, error = "امکان باز کردن لینک وجود ندارد")
            }
        } catch (e: Exception) {
            LinkResult(success = false, error = "خطا در باز کردن لینک")
        }
    }

    fun showErrorAlert(message: String) {
        RNAlert.alert("خطا", message, listOf(AlertButton("باشه", style = "default")))
    }

    fun showSuccessAlert(message: String) {
        RNAlert.alert("موفقیت", message, listOf(AlertButton("باشه", style = "default")))
    }
}
