package ir.farimod.app.storage

import com.russhwolf.settings.Settings
import ir.farimod.app.data.model.UserData
import ir.farimod.app.network.FarimodHttpClient

class AuthTokenStorage(
    private val settings: Settings = Settings()
) {
    companion object {
        private const val KEY_TOKEN = "userToken"
        private const val KEY_USER_DATA = "userData"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_INTRO_COMPLETED = "introCompleted"
    }

    fun saveToken(token: String) {
        settings.putString(KEY_TOKEN, token)
        settings.putBoolean(KEY_IS_LOGGED_IN, true)
    }

    fun getToken(): String? {
        return settings.getStringOrNull(KEY_TOKEN)
    }

    fun saveUserData(user: UserData) {
        if (!user.Token.isNullOrEmpty()) {
            saveToken(user.Token)
        }
        val json = FarimodHttpClient.jsonInstance.encodeToString(user)
        settings.putString(KEY_USER_DATA, json)
        settings.putBoolean(KEY_IS_LOGGED_IN, true)
    }

    fun getUserData(): UserData? {
        val json = settings.getStringOrNull(KEY_USER_DATA) ?: return null
        return try {
            FarimodHttpClient.jsonInstance.decodeFromString<UserData>(json)
        } catch (_: Exception) {
            null
        }
    }

    fun isLoggedIn(): Boolean {
        val loggedIn = settings.getBoolean(KEY_IS_LOGGED_IN, false)
        val token = getToken()
        return loggedIn && !token.isNullOrEmpty()
    }

    fun isIntroCompleted(): Boolean {
        return settings.getBoolean(KEY_INTRO_COMPLETED, false)
    }

    fun setIntroCompleted(completed: Boolean = true) {
        settings.putBoolean(KEY_INTRO_COMPLETED, completed)
    }

    fun clear() {
        settings.remove(KEY_TOKEN)
        settings.remove(KEY_USER_DATA)
        settings.putBoolean(KEY_IS_LOGGED_IN, false)
    }
}
