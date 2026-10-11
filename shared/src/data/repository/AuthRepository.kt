package ir.farimod.app.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import ir.farimod.app.data.model.*
import ir.farimod.app.network.ApiConfig
import ir.farimod.app.network.ApiResult
import ir.farimod.app.network.FarimodHttpClient
import ir.farimod.app.network.safeApiCall
import ir.farimod.app.storage.AuthTokenStorage

class AuthRepository(
    private val client: HttpClient = FarimodHttpClient.client,
    private val storage: AuthTokenStorage = AuthTokenStorage()
) {
    suspend fun sendOtp(mobile: String): ApiResult<SendOtpResponse> {
        return safeApiCall {
            client.post(ApiConfig.Endpoints.SEND_OTP_SMS) {
                parameter("mobile", mobile)
            }
        }
    }

    suspend fun verifyOtpAndLogin(mobile: String, otp: String): ApiResult<LoginResponse> {
        val result: ApiResult<LoginResponse> = safeApiCall {
            client.post(ApiConfig.Endpoints.LOGIN) {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(Mobile = mobile, OTP = otp))
            }
        }

        if (result is ApiResult.Success) {
            val response = result.data
            if (!response.Token.isNullOrEmpty()) {
                storage.saveUserData(
                    UserData(
                        Token = response.Token,
                        MemberId = response.MemberId,
                        FirstName = response.FirstName,
                        LastName = response.LastName,
                        Mobile = response.Mobile,
                        AvatarImageURL = response.AvatarImageURL
                    )
                )
            }
        }

        return result
    }

    suspend fun signupFirstStep(
        firstName: String,
        lastName: String,
        mobile: String,
        groups: List<Int>
    ): ApiResult<SignupFirstStepResponse> {
        return safeApiCall {
            client.post(ApiConfig.Endpoints.SIGNUP_FIRST_STEP) {
                contentType(ContentType.Application.Json)
                setBody(
                    SignupFirstStepRequest(
                        FirstName = firstName,
                        LastName = lastName,
                        Mobile = mobile,
                        MemberGroupIdList = groups
                    )
                )
            }
        }
    }

    suspend fun sendSignupOtpSms(signupToken: String): ApiResult<SendOtpResponse> {
        return safeApiCall {
            client.post(ApiConfig.Endpoints.SEND_SIGNUP_OTP_SMS) {
                parameter("signupToken", signupToken)
            }
        }
    }

    suspend fun validateSignupOtp(signupToken: String, otp: String): ApiResult<SendOtpResponse> {
        return safeApiCall {
            client.post(ApiConfig.Endpoints.VALIDATE_SIGNUP_OTP) {
                contentType(ContentType.Application.Json)
                setBody(ValidateSignupOtpRequest(SignupToken = signupToken, OTP = otp))
            }
        }
    }

    suspend fun getMemberGroups(): ApiResult<List<MemberGroupDto>> {
        return safeApiCall {
            client.get(ApiConfig.Endpoints.GET_MEMBER_GROUPS)
        }
    }

    fun logout() {
        storage.clear()
    }

    fun isLoggedIn(): Boolean {
        return storage.isLoggedIn()
    }

    fun getCurrentUser(): UserData? {
        return storage.getUserData()
    }

    fun isIntroCompleted(): Boolean {
        return storage.isIntroCompleted()
    }

    fun setIntroCompleted(completed: Boolean = true) {
        storage.setIntroCompleted(completed)
    }

    fun validateMobile(mobile: String): Boolean {
        val regex = Regex("^09\\d{9}$")
        return regex.matches(mobile)
    }
}
