package ir.farimod.app.services

import ir.farimod.app.network.Api
import ir.farimod.app.network.ApiNetworkException
import ir.farimod.app.network.bool
import ir.farimod.app.network.double
import ir.farimod.app.network.str
import kotlinx.coroutines.CancellationException

/** Port of `app/config/appInfo.js`. */
object AppInfo {
    const val name = "farimod"
    const val slug = "farimod-2"
    const val version = "1.46.0"
    const val versionNumber = 1.46
    const val packageName = "com.yeganeh0.farimod2"
    const val owner = "yeganeh0"

    object Stores {
        object CafeBazar {
            const val app = "bazaar://details?id=com.yeganeh0.farimod2"
            const val web = "https://cafebazaar.ir/app/com.yeganeh0.farimod2"
        }

        object GooglePlay {
            const val app = "market://details?id=com.yeganeh0.farimod2"
            const val web = "https://play.google.com/store/apps/details?id=com.yeganeh0.farimod2"
        }
    }

    object ApiConfig {
        const val baseUrl = "http://my.farimod.ir/api/MobileApp/"
        const val updateCheck = "AppRun/NotifyAppRun"
    }
}

/** `result.data` of `AppUpdateService.checkForUpdate`. */
data class AppUpdateData(
    val currentVersion: Double,
    val latestVersion: Double?,
    val hasUpdate: Boolean,
    val isUpdateRequired: Boolean,
    val downloadLink: String?,
    val cafeBazarLink: String?
)

/** `{ success, data?, error? }` returned by `AppUpdateService.checkForUpdate`. */
data class AppUpdateCheckResult(
    val success: Boolean,
    val data: AppUpdateData? = null,
    val error: String? = null
)

/** `{ title, message, type }` returned by `AppUpdateService.getUpdateMessage`. */
data class AppUpdateMessage(val title: String, val message: String, val type: String)

/** Port of `app/services/AppUpdateService.js`. */
object AppUpdateService {
    private const val APP_VERSION = 1.46

    fun getCurrentAppVersion(): Double = APP_VERSION

    /** `apiService.get("AppRun/NotifyAppRun", { params: { memberId, mobileAppVersion } })` (no auth header, 30s timeout). */
    suspend fun checkForUpdate(memberId: Int): AppUpdateCheckResult {
        return try {
            val response = Api.get(
                AppInfo.ApiConfig.updateCheck,
                query = mapOf("memberId" to memberId, "mobileAppVersion" to APP_VERSION),
                timeoutMs = 30_000
            )

            // axios rejects on non-2xx → ApiService.handleError
            if (!response.ok) {
                val json = response.json
                val message = json.str("message")?.takeIf { it.isNotEmpty() }
                    ?: json.str("error")?.takeIf { it.isNotEmpty() }
                    ?: "خطای سرور: ${response.status}"
                return AppUpdateCheckResult(success = false, error = message)
            }

            // `if (response.data)` → empty body is falsy
            if (response.text.isBlank()) {
                return AppUpdateCheckResult(success = false, error = "Invalid response format")
            }

            val data = response.json
            AppUpdateCheckResult(
                success = true,
                data = AppUpdateData(
                    currentVersion = AppInfo.versionNumber,
                    latestVersion = data.double("MobileAppLastVersion"),
                    hasUpdate = data.bool("MobileAppHasUpdate"),
                    isUpdateRequired = data.bool("IsMobileAppUpdateRequired"),
                    downloadLink = data.str("MobileAppDownloadLink"),
                    cafeBazarLink = data.str("MobileAppCafeBazarPageLink")
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiNetworkException) {
            AppUpdateCheckResult(
                success = false,
                error = "خطا در ارتباط با سرور. لطفاً اتصال اینترنت خود را بررسی کنید."
            )
        } catch (e: Exception) {
            AppUpdateCheckResult(success = false, error = e.message?.takeIf { it.isNotEmpty() } ?: "خطا در بررسی بروزرسانی")
        }
    }

    fun shouldShowUpdateModal(result: AppUpdateCheckResult?): Boolean {
        if (result == null || !result.success) return false
        val data = result.data ?: return false
        if (data.isUpdateRequired) return true
        if (data.hasUpdate && !data.isUpdateRequired) return true
        return false
    }

    fun getUpdateMessage(result: AppUpdateCheckResult?): AppUpdateMessage? {
        if (result == null || !result.success) return null
        val data = result.data ?: return null

        return if (data.isUpdateRequired) {
            AppUpdateMessage(
                title = "بروزرسانی اجباری",
                message = "نسخه جدیدی از فریمد منتشر شده است. برای ادامه استفاده از برنامه، باید آن را بروزرسانی کنید.",
                type = "required"
            )
        } else {
            AppUpdateMessage(
                title = "نیازمند بروزرسانی",
                message = "نسخه جدیدی از فریمد منتشر شده است. آیا می‌خواهید اکنون بروزرسانی کنید؟",
                type = "optional"
            )
        }
    }
}
