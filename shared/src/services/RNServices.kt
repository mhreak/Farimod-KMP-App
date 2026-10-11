package ir.farimod.app.services

import ir.farimod.app.data.model.UserData
import ir.farimod.app.network.Api
import ir.farimod.app.network.ApiNetworkException
import ir.farimod.app.network.bool
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.jsonOf
import ir.farimod.app.network.list
import ir.farimod.app.network.long
import ir.farimod.app.network.rnJson
import ir.farimod.app.network.str
import ir.farimod.app.storage.AuthTokenStorage
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Result shape used by the RN services: `{ success, data, message }`.
 */
data class ServiceResult(
    val success: Boolean,
    val data: JsonElement? = null,
    val message: String? = null
)

/** Port of `app/services/AuthService.js` (same endpoints / messages / validation). */
object AuthService {
    private val storage by lazy { AuthTokenStorage() }

    suspend fun sendOTP(mobile: String): ServiceResult {
        return try {
            val response = Api.post("MobileAccount/SendOTPSMS", query = mapOf("mobile" to mobile))
            val data = response.json
            if (response.ok) {
                ServiceResult(true, data, data.str("Message") ?: data.str("message") ?: "کد تایید ارسال شد")
            } else {
                ServiceResult(false, null, data.str("message") ?: data.str("Message") ?: "خطا در ارسال کد تایید")
            }
        } catch (e: ApiNetworkException) {
            ServiceResult(false, null, "خطا در اتصال به سرور")
        }
    }

    suspend fun verifyOTP(mobile: String, otp: String): ServiceResult {
        return try {
            val response = Api.post("MobileAccount/Login", body = jsonOf("Mobile" to mobile, "OTP" to otp))
            val data = response.json
            if (response.ok && data != null) {
                saveUserData(data)
                ServiceResult(true, data, "ورود موفقیت‌آمیز")
            } else {
                ServiceResult(false, null, data.str("message") ?: data.str("Message") ?: "کد تایید اشتباه است")
            }
        } catch (e: ApiNetworkException) {
            ServiceResult(false, null, "خطا در اتصال به سرور")
        }
    }

    /** Saves the user (without ActiveSubscriptionPlan / AvatarImageURL, exactly like RN). */
    fun saveUserData(userData: JsonElement) {
        val user = try { rnJson.decodeFromJsonElement(UserData.serializer(), userData) } catch (_: Exception) { return }
        storage.saveUserData(user.copy(ActiveSubscriptionPlan = null, AvatarImageURL = null))
    }

    fun saveUserData(user: UserData) {
        storage.saveUserData(user.copy(ActiveSubscriptionPlan = null, AvatarImageURL = null))
    }

    /** Merge-update of the persisted user (RN `updateUserData`). */
    fun updateUserData(transform: (UserData) -> UserData) {
        val current = storage.getUserData() ?: return
        val updated = transform(current).copy(ActiveSubscriptionPlan = null, AvatarImageURL = null)
        storage.saveUserData(updated)
    }

    fun getUserData(): UserData? = storage.getUserData()
    fun getUserToken(): String? = storage.getToken()
    fun isLoggedIn(): Boolean = storage.isLoggedIn()
    fun logout() = storage.clear()

    fun validateMobileNumber(mobile: String): Boolean = Regex("^09\\d{9}$").matches(mobile)
    fun validateOTP(otp: String): Boolean = otp.length == 5 && otp.all { it.isDigit() }
}

/** Port of `app/services/SubscriptionService.tsx`. */
object SubscriptionService {
    private const val TIMEOUT = 10_000L

    private fun errMessage(e: ApiNetworkException, fallback: String) =
        if (e.message == "Request timeout") "زمان درخواست به پایان رسید" else fallback

    suspend fun getAllActivePlans(): ServiceResult {
        return try {
            val response = Api.get("SubscriptionPlan/GetAllActive", timeoutMs = TIMEOUT)
            val data = response.json
            if (response.ok) ServiceResult(true, data["Data"] ?: kotlinx.serialization.json.JsonArray(emptyList()), "لیست اشتراک‌ها دریافت شد")
            else ServiceResult(false, null, data.str("Message") ?: "خطا در دریافت اشتراک‌ها")
        } catch (e: ApiNetworkException) {
            ServiceResult(false, null, errMessage(e, "خطا در اتصال به سرور"))
        }
    }

    suspend fun getActiveSubscriptionPlan(memberId: Int): ServiceResult {
        if (memberId == 0) return ServiceResult(false, null, "شناسه کاربر موجود نیست")
        return try {
            val response = Api.get(
                "SubscriptionPlan/GetActiveSubscriptionPlanOfMember",
                query = mapOf("memberId" to memberId),
                auth = true,
                timeoutMs = TIMEOUT
            )
            if (response.text.isBlank()) return ServiceResult(false, null, "اشتراک فعالی یافت نشد")
            val data = response.json
            if (response.ok && data != null) ServiceResult(true, data, "اطلاعات اشتراک دریافت شد")
            else ServiceResult(false, null, data.str("Message") ?: "اشتراک فعالی یافت نشد")
        } catch (e: ApiNetworkException) {
            ServiceResult(false, null, errMessage(e, "خطا در اتصال به سرور"))
        }
    }

    suspend fun applyDiscountCode(subscriptionPlanId: Int, code: String, timePeriod: Int): ServiceResult {
        return try {
            val response = Api.get(
                "SubscriptionPlan/ApplyDiscountCode",
                query = mapOf("subscriptionPlanId" to subscriptionPlanId, "code" to code, "timePeriod" to timePeriod),
                timeoutMs = TIMEOUT
            )
            val data = response.json
            if (response.ok) ServiceResult(true, data, "کد تخفیف اعمال شد")
            else ServiceResult(false, null, data.str("Message") ?: "کد تخفیف معتبر نیست")
        } catch (e: ApiNetworkException) {
            ServiceResult(false, null, errMessage(e, "خطا در اعمال کد تخفیف"))
        }
    }

    suspend fun buySubscriptionPlan(
        memberId: Int,
        subscriptionPlanId: Int,
        subscriptionTotalDays: Int,
        discountCode: String = ""
    ): ServiceResult {
        val token = AuthService.getUserToken()
        if (token.isNullOrEmpty()) return ServiceResult(false, null, "کاربر وارد نشده است")
        return try {
            val response = Api.post(
                "SubscriptionPlan/BuySubscriptionPlan",
                body = jsonOf(
                    "MemberId" to memberId,
                    "SubscriptionPlanId" to subscriptionPlanId,
                    "SubscriptionTotalDays" to subscriptionTotalDays,
                    "DiscountCode" to discountCode
                ),
                auth = true,
                timeoutMs = TIMEOUT
            )
            val data = response.json
            if (response.ok) ServiceResult(true, data, "اشتراک با موفقیت ثبت شد")
            else ServiceResult(false, null, data.str("Message") ?: "خطا در ثبت اشتراک")
        } catch (e: ApiNetworkException) {
            ServiceResult(false, null, errMessage(e, "خطا در اتصال به سرور"))
        }
    }

    fun calculateDays(subscriptionType: String): Int = when (subscriptionType) {
        "fourteen_days" -> 14
        "one_month" -> 30
        "three_months" -> 90
        "six_months" -> 180
        "annual" -> 365
        else -> 30
    }

    fun getPrice(plan: JsonElement?, subscriptionType: String): Long {
        val key = when (subscriptionType) {
            "fourteen_days" -> "FourteenDaysSubscriptionPrice"
            "one_month" -> "OneMonthSubscriptionPrice"
            "three_months" -> "ThreeMonthsSubscriptionPrice"
            "six_months" -> "SixMonthsSubscriptionPrice"
            "annual" -> "AnnualSubscriptionPrice"
            else -> return 0
        }
        return plan[key].long ?: 0L
    }
}

/**
 * Port of `app/services/ProfileService.js`.
 * NOTE: RN builds `/MobileApp/MemberInfo/...` on top of a base URL that already ends in `/api/MobileApp/`;
 * we call the real endpoint (`MemberInfo/...`) which is what works against the server.
 */
object ProfileService {
    suspend fun getProfileInfoToEdit(memberId: Int, userContext: UserData? = null): ServiceResult {
        return try {
            val response = Api.get("MemberInfo/GetProfileInfoToEdit", query = mapOf("memberId" to memberId), timeoutMs = 30_000)
            if (!response.ok) return ServiceResult(false, null, "خطای سرور: ${response.status}")
            var data = response.json
            if (data is JsonObject && data["Mobile"].str.isNullOrEmpty() && !userContext?.Mobile.isNullOrEmpty()) {
                data = JsonObject(data + (jsonOf("Mobile" to userContext?.Mobile)))
            }
            ServiceResult(true, data)
        } catch (e: ApiNetworkException) {
            ServiceResult(false, null, "خطا در ارتباط با سرور. لطفاً اتصال اینترنت خود را بررسی کنید.")
        }
    }

    /** Returns `Member/Get` JSON in [ServiceResult.data]; avatar URL is `data["AvatarImageURL"]`. */
    suspend fun getProfileAvatar(currentMemberId: Int, memberId: Int? = null): ServiceResult {
        val target = memberId ?: currentMemberId
        return try {
            val response = Api.get("Member/Get", query = mapOf("currentMemberId" to currentMemberId, "memberId" to target), timeoutMs = 30_000)
            val data = response.json
            if (response.ok && !data["AvatarImageURL"].str.isNullOrEmpty()) ServiceResult(true, data)
            else ServiceResult(false, data, "هیچ عکس پروفایلی یافت نشد")
        } catch (e: ApiNetworkException) {
            ServiceResult(false, null, "خطا در دریافت عکس پروفایل")
        }
    }

    suspend fun updateProfile(profileData: JsonObject): ServiceResult {
        if (profileData["Mobile"].str.isNullOrEmpty()) return ServiceResult(false, null, "شماره موبایل الزامی است")
        return try {
            val response = Api.post("MemberInfo/UpdateProfile", body = profileData, timeoutMs = 30_000)
            if (response.ok) ServiceResult(true, response.json)
            else ServiceResult(false, null, response.json.str("message") ?: response.json.str("error") ?: "خطای سرور: ${response.status}")
        } catch (e: ApiNetworkException) {
            ServiceResult(false, null, "خطا در ارتباط با سرور. لطفاً اتصال اینترنت خود را بررسی کنید.")
        }
    }
}
