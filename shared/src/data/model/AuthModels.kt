package ir.farimod.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    val Token: String? = null,
    val MemberId: Int? = null,
    val FirstName: String? = null,
    val LastName: String? = null,
    val Mobile: String? = null,
    val AvatarImageURL: String? = null,
    val ActiveSubscriptionPlan: SubscriptionPlanSummary? = null
)

@Serializable
data class SubscriptionPlanSummary(
    val SubscriptionPlanId: Int? = null,
    val SubscriptionPlanName: String? = null,
    val ShamsiStartDate: String? = null,
    val ShamsiFinishDate: String? = null,
    val RemainingDaysToFinishDate: Int? = null,
    val IsInfinityPlan: Boolean = false,
    val SubscriptionTotalDays: Int? = null,
    val SubscriptionPrice: Long? = null,
    val FinalAmount: Long? = null,
    val PlanOption_AllowAboutMeText: Boolean = false,
    val PlanOption_AllowAddImageGallery: Boolean = false,
    val PlanOption_AllowAddPortfolio: Boolean = false,
    val PlanOption_AllowAddProduct: Boolean = false,
    val PlanOption_AllowAddCourse: Boolean = false,
    val PlanOption_AllowAddBlogPost: Boolean = false,
    val PlanOption_AllowAddDocument: Boolean = false,
    val PlanOption_ShowContanctInfo: Boolean = false,
    val PlanOption_ShowBlueTick: Boolean = false,
    val ExpireDate: String? = null
)

@Serializable
data class LoginRequest(
    val Mobile: String,
    val OTP: String
)

@Serializable
data class LoginResponse(
    val Token: String? = null,
    val MemberId: Int? = null,
    val FirstName: String? = null,
    val LastName: String? = null,
    val Mobile: String? = null,
    val AvatarImageURL: String? = null,
    @SerialName("Message")
    val message: String? = null
)

@Serializable
data class SendOtpResponse(
    @SerialName("Message")
    val message: String? = null,
    val Success: Boolean? = null
)

@Serializable
data class SignupFirstStepRequest(
    val FirstName: String,
    val LastName: String,
    val Mobile: String,
    val MemberGroupIdList: List<Int>
)

@Serializable
data class SignupFirstStepResponse(
    val SignupToken: String? = null,
    @SerialName("Message")
    val message: String? = null,
    val RedirectToLogin: Boolean? = null
)

@Serializable
data class ValidateSignupOtpRequest(
    val SignupToken: String,
    val OTP: String
)

@Serializable
data class MemberGroupDto(
    val MemberGroupId: Int = 0,
    val GroupName: String = "",
    val MemberCount: Int = 0,
    val Active: Boolean = true,
    val ActiveStr: String? = null,
    val InsertDate: String? = null,
    val ShamsiInsertDate: String? = null
)

@Serializable
data class ProfileInfoResponse(
    val MemberId: Int? = null,
    val FirstName: String? = null,
    val LastName: String? = null,
    val AvatarImageURL: String? = null
)
