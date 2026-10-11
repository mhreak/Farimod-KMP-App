package ir.farimod.app.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.russhwolf.settings.Settings
import ir.farimod.app.platform.canOpenUrl
import ir.farimod.app.platform.openUrl
import ir.farimod.app.platform.rnNowMillis
import ir.farimod.app.services.AppInfo
import ir.farimod.app.services.AppUpdateCheckResult
import ir.farimod.app.services.AppUpdateService
import ir.farimod.app.ui.AlertButton
import ir.farimod.app.ui.RNAlert
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

/** `updateInfo` of AppUpdateContext: `{ ...result.data, ...message }`. */
data class AppUpdateInfo(
    val currentVersion: Double,
    val latestVersion: Double?,
    val hasUpdate: Boolean,
    val isUpdateRequired: Boolean,
    val downloadLink: String?,
    val cafeBazarLink: String?,
    val title: String,
    val message: String,
    /** "required" | "optional" */
    val type: String
)

/**
 * Port of `app/contexts/AppUpdateContext.js` (`useAppUpdate()`).
 *
 * The periodic / foreground checks are driven by `AppUpdateHost` (ui/AppUpdateHost.kt) which runs
 * [runScheduledChecks] only while `isAuthenticated && user.MemberId && isHomeScreen` — exactly the RN effect.
 * `HomeScreen` must call `AppUpdateState.setHomeScreenStatus(true)` when it is shown and `(false)` when it is
 * disposed (RN: `useEffect(() => { setHomeScreenStatus(true); return () => setHomeScreenStatus(false); }, [])`).
 */
object AppUpdateState {
    private const val LAST_UPDATE_CHECK_KEY = "lastUpdateCheck"
    private const val DISMISSED_VERSION_KEY = "dismissedUpdateVersion"
    private const val DISMISSED_VERSION_TIMESTAMP_KEY = "dismissedUpdateVersionTimestamp"
    private const val CHECK_INTERVAL = 30L * 60 * 1000

    private val settings: Settings by lazy { Settings() }

    var updateInfo: AppUpdateInfo? by mutableStateOf(null)
    var showUpdateModal: Boolean by mutableStateOf(false)
    var isCheckingUpdate: Boolean by mutableStateOf(false)
    var isHomeScreen: Boolean by mutableStateOf(false)

    fun setHomeScreenStatus(status: Boolean) {
        isHomeScreen = status
    }

    // ── scheduling (the `useEffect` body of AppUpdateProvider) ─────────────────────────────────────

    /**
     * RN effect body: initial check (if due), then `setInterval(checkForUpdate, 30 min)`.
     * Runs until cancelled (effect cleanup).
     */
    suspend fun runScheduledChecks() {
        // performInitialCheck
        try {
            val shouldCheck = shouldCheckForUpdate()
            if (shouldCheck) checkForUpdate()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }

        // setInterval(() => checkForUpdate(), CHECK_INTERVAL)
        while (true) {
            delay(CHECK_INTERVAL)
            checkForUpdate()
        }
    }

    /** AppState "active" handler. */
    suspend fun checkForUpdateOnAppActivation() {
        try {
            val shouldCheck = shouldCheckForUpdate()
            if (shouldCheck) checkForUpdate()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    suspend fun checkForUpdateOnStartup() {
        try {
            val shouldCheck = shouldCheckForUpdate()
            if (shouldCheck) checkForUpdate()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    private fun shouldCheckForUpdate(): Boolean {
        return try {
            val lastCheckTime = settings.getStringOrNull(LAST_UPDATE_CHECK_KEY)

            if (lastCheckTime.isNullOrEmpty()) {
                return true // اولین بار که اپ اجرا می‌شود
            }

            // `Date.now() - parseInt(lastCheckTime) > CHECK_INTERVAL` (NaN → false)
            val last = lastCheckTime.trim().toLongOrNull() ?: return false
            val timeSinceLastCheck = rnNowMillis() - last
            timeSinceLastCheck > CHECK_INTERVAL
        } catch (_: Exception) {
            true // در صورت خطا، چک کن
        }
    }

    // ── checking ──────────────────────────────────────────────────────────────────────────────────

    /** `checkForUpdate` (also exposed as `manualCheckForUpdate` / `checkForUpdate` in the context value). */
    suspend fun checkForUpdate() {
        val memberId = AuthState.user?.MemberId
        if (memberId == null || memberId == 0 || isCheckingUpdate) {
            return
        }

        try {
            isCheckingUpdate = true

            val result = AppUpdateService.checkForUpdate(memberId)

            if (result.success) {
                val shouldShow = shouldShowUpdateModal(result)

                if (shouldShow) {
                    val data = result.data
                    val message = AppUpdateService.getUpdateMessage(result)
                    if (data != null && message != null) {
                        updateInfo = AppUpdateInfo(
                            currentVersion = data.currentVersion,
                            latestVersion = data.latestVersion,
                            hasUpdate = data.hasUpdate,
                            isUpdateRequired = data.isUpdateRequired,
                            downloadLink = data.downloadLink,
                            cafeBazarLink = data.cafeBazarLink,
                            title = message.title,
                            message = message.message,
                            type = message.type
                        )
                        showUpdateModal = true
                    }
                }
            }

            // ذخیره زمان آخرین چک
            settings.putString(LAST_UPDATE_CHECK_KEY, rnNowMillis().toString())
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        } finally {
            isCheckingUpdate = false
        }
    }

    private fun shouldShowUpdateModal(updateResult: AppUpdateCheckResult): Boolean {
        val data = updateResult.data
        if (!updateResult.success || data == null || !data.hasUpdate) {
            return false
        }

        val isUpdateRequired = data.isUpdateRequired
        val latestVersion = data.latestVersion

        // اگر بروزرسانی اجباری است، همیشه نشان بده
        if (isUpdateRequired) {
            return true
        }

        // اگر بروزرسانی اختیاری است، چک کن که آیا کاربر قبلاً آن را رد کرده
        try {
            val dismissedVersion = settings.getStringOrNull(DISMISSED_VERSION_KEY)
            val dismissedTimestamp = settings.getStringOrNull(DISMISSED_VERSION_TIMESTAMP_KEY)

            // `dismissedVersion && parseFloat(dismissedVersion) >= latestVersion`
            val dismissed = if (!dismissedVersion.isNullOrEmpty()) dismissedVersion.trim().toDoubleOrNull() else null
            if (dismissed != null && latestVersion != null && dismissed >= latestVersion) {
                if (!dismissedTimestamp.isNullOrEmpty()) {
                    val ts = dismissedTimestamp.trim().toLongOrNull()
                    if (ts != null) {
                        val timeSinceDismissed = rnNowMillis() - ts
                        if (timeSinceDismissed < CHECK_INTERVAL) {
                            return false
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }

        return true
    }

    // ── handlers passed to <AppUpdateModal> ────────────────────────────────────────────────────

    fun handleDirectDownload() {
        val link = updateInfo?.downloadLink
        if (link.isNullOrEmpty()) {
            errorAlert("لینک دانلود مستقیم موجود نیست")
            return
        }

        try {
            val canOpen = canOpenUrl(link)
            if (canOpen) {
                // `Linking.openURL` rejects when it fails → catch → alert
                if (!openUrl(link)) throw IllegalStateException("openURL failed")
                showUpdateModal = false
            } else {
                errorAlert("امکان باز کردن لینک دانلود وجود ندارد")
            }
        } catch (_: Exception) {
            errorAlert("خطا در باز کردن لینک دانلود")
        }
    }

    fun handleCafeBazarDownload() {
        try {
            // اگر لینک کافه بازار موجود نیست، از package name استفاده کن
            val bazarUrl: String = updateInfo?.cafeBazarLink?.takeIf { it.isNotEmpty() }
                ?: "bazaar://details?id=${AppInfo.packageName}"

            val canOpen = canOpenUrl(bazarUrl)
            if (canOpen) {
                if (!openUrl(bazarUrl)) throw IllegalStateException("openURL failed")
                showUpdateModal = false
            } else {
                // اگر کافه بازار نصب نیست، لینک وب را باز کن
                val webUrl = "https://cafebazaar.ir/app/${AppInfo.packageName}"
                if (!openUrl(webUrl)) throw IllegalStateException("openURL failed")
                showUpdateModal = false
            }
        } catch (_: Exception) {
            errorAlert("خطا در باز کردن کافه بازار")
        }
    }

    fun handleLater() {
        try {
            // ذخیره نسخه‌ای که کاربر رد کرده تا دوباره نشان داده نشود
            val latest = updateInfo?.latestVersion
            if (latest != null && latest != 0.0) {
                settings.putString(DISMISSED_VERSION_KEY, jsNumberToString(latest))
                // ذخیره زمان رد کردن
                settings.putString(DISMISSED_VERSION_TIMESTAMP_KEY, rnNowMillis().toString())
            }

            showUpdateModal = false
            updateInfo = null
        } catch (_: Exception) {
            showUpdateModal = false
            updateInfo = null
        }
    }

    fun handleClose() {
        // حالا همیشه می‌توان مدال را بست
        showUpdateModal = false
        updateInfo = null
    }

    private fun errorAlert(message: String) {
        RNAlert.alert("خطا", message, listOf(AlertButton("باشه", style = "default")))
    }

    /** JS `Number#toString()` for typical version numbers (1.47 → "1.47", 2.0 → "2"). */
    private fun jsNumberToString(d: Double): String {
        return if (d == d.toLong().toDouble() && kotlin.math.abs(d) < 1e15) d.toLong().toString() else d.toString()
    }
}
