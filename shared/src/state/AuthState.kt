package ir.farimod.app.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ir.farimod.app.data.model.SubscriptionPlanSummary
import ir.farimod.app.data.model.UserData
import ir.farimod.app.network.Api
import ir.farimod.app.network.get
import ir.farimod.app.network.rnJson
import ir.farimod.app.network.str
import ir.farimod.app.services.AuthService
import ir.farimod.app.services.SubscriptionService
import ir.farimod.app.storage.AuthTokenStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Global scope for fire-and-forget work (RN: un-awaited promises). */
val AppScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

/**
 * Port of `app/contexts/AuthContext.js`. Use `AuthState.user`, `AuthState.isAuthenticated`, etc. in any screen
 * (React: `const { user, logout, updateUser, refreshSubscription } = useAuth()`).
 */
object AuthState {
    var user: UserData? by mutableStateOf(null)
    var isLoading: Boolean by mutableStateOf(true)
    var isAuthenticated: Boolean by mutableStateOf(false)

    /** Port of `checkAuthStatus` (storage is synchronous here, so the UI never blocks). */
    fun checkAuthStatus() {
        try {
            isLoading = true
            if (AuthService.isLoggedIn()) {
                val userData = AuthService.getUserData()
                if (userData != null) {
                    user = userData
                    isAuthenticated = true
                    AppScope.launch { runCatching { updateSubscriptionInfo(userData) } }
                    AppScope.launch { runCatching { fetchUserAvatar(userData) } }
                } else {
                    AuthService.logout()
                    user = null
                    isAuthenticated = false
                }
            } else {
                user = null
                isAuthenticated = false
            }
        } catch (_: Exception) {
            user = null
            isAuthenticated = false
        } finally {
            isLoading = false
        }
    }

    /** Latest avatar from `MemberInfo/GetProfileInfoToEdit` → stored in state only. Returns the URL or null. */
    suspend fun fetchUserAvatar(userData: UserData? = null): String? {
        return try {
            val current = userData ?: user
            val memberId = current?.MemberId ?: return null
            val response = Api.get("MemberInfo/GetProfileInfoToEdit", query = mapOf("memberId" to memberId))
            val url = response.json["AvatarImageURL"].str
            if (!url.isNullOrEmpty()) {
                user = (user ?: current).copy(AvatarImageURL = url)
                url
            } else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun updateSubscriptionInfo(userData: UserData? = null) {
        try {
            val current = userData ?: user
            if (current == null || current.MemberId == null || current.MemberId == 0) {
                if (current != null) {
                    user = current
                    isAuthenticated = true
                }
                return
            }
            val result = withTimeoutOrNull(10_000) { SubscriptionService.getActiveSubscriptionPlan(current.MemberId) }
                ?: throw Exception("Subscription API timeout")
            val plan: SubscriptionPlanSummary? = if (result.success && result.data != null) {
                try { rnJson.decodeFromJsonElement(SubscriptionPlanSummary.serializer(), result.data) } catch (_: Exception) { null }
            } else null
            // keep the avatar that may have been fetched meanwhile
            val avatar = user?.AvatarImageURL ?: current.AvatarImageURL
            user = current.copy(ActiveSubscriptionPlan = plan, AvatarImageURL = avatar)
            isAuthenticated = true
        } catch (_: Exception) {
            userData?.let {
                user = it.copy(ActiveSubscriptionPlan = null)
                isAuthenticated = true
            }
        }
    }

    suspend fun login(userData: UserData) {
        AuthService.saveUserData(userData)
        user = userData
        isAuthenticated = true
        AppScope.launch { runCatching { updateSubscriptionInfo(userData) } }
        AppScope.launch { runCatching { fetchUserAvatar(userData) } }
    }

    fun logout() {
        AuthService.logout()
        user = null
        isAuthenticated = false
    }

    /** RN `updateUser({FirstName, ...})` — merges into persisted + in-memory user, keeping subscription & avatar. */
    fun updateUser(transform: (UserData) -> UserData) {
        AuthService.updateUserData(transform)
        val current = user ?: return
        val updated = transform(current)
        user = updated.copy(ActiveSubscriptionPlan = current.ActiveSubscriptionPlan, AvatarImageURL = current.AvatarImageURL)
    }

    suspend fun refreshSubscription(): Boolean {
        val u = user ?: return false
        if (u.MemberId == null || u.MemberId == 0) return false
        updateSubscriptionInfo(u)
        return true
    }

    // ── Intro (IntroContext.tsx + IntroStorageService) ─────────────────────────
    private val storage by lazy { AuthTokenStorage() }
    fun isIntroCompleted(): Boolean = storage.isIntroCompleted()
    fun completeIntro() = storage.setIntroCompleted(true)
    fun resetIntro() = storage.setIntroCompleted(false)
}
