package ir.farimod.app.services

import ir.farimod.app.network.Api
import ir.farimod.app.network.ApiNetworkException
import ir.farimod.app.network.bool
import ir.farimod.app.network.double
import ir.farimod.app.network.str
import ir.farimod.app.platform.AppBuildConfig
import ir.farimod.app.platform.getAppVersionName
import kotlinx.coroutines.CancellationException

/** Port of `app/config/appInfo.js`. */
object AppInfo {
    const val name = "farimod"
    const val slug = "farimod-2"
    val version: String get() = getAppVersionName()
    val versionNumber: Double
        get() {
            val ver = getAppVersionName()
            val twoParts = ver.split('.').take(2).joinToString(".")
            return twoParts.toDoubleOrNull() ?: 1.0
        }
    const val packageName = "ir.farimod.app"
    const val owner = "yeganeh0"

    object Stores {
        object CafeBazar {
            const val app = "bazaar://details?id=ir.farimod.app"
            const val web = "https://cafebazaar.ir/app/ir.farimod.app"
        }

        object GooglePlay {
            const val app = "market://details?id=ir.farimod.app"
            const val web = "https://play.google.com/store/apps/details?id=ir.farimod.app"
        }

        object Myket {
            const val app = "myket://details?id=ir.farimod.app"
            const val web = "https://myket.ir/app/ir.farimod.app"
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
    fun getCurrentAppVersion(): Double = AppInfo.versionNumber

    /** `apiService.get("AppRun/NotifyAppRun", { params: { memberId, mobileAppVersion } })` (no auth header, 30s timeout). */
    suspend fun checkForUpdate(memberId: Int): AppUpdateCheckResult {
        val currentVersion = getCurrentAppVersion()
        return try {
            val response = Api.get(
                AppInfo.ApiConfig.updateCheck,
                query = mapOf(
                    "memberId" to memberId,
                    "mobileAppVersion" to currentVersion,
                    "downloadSource" to AppBuildConfig.downloadSource
                ),
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
