package ir.farimod.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CourseDto(
    val CourseId: Int = 0,
    val MemberId: Int = 0,
    val MemberName: String? = null,
    val CourseName: String = "",
    val CourseType: String? = "1", // 1: حضوری, 2: مجازی, 3: حضوری و مجازی
    val CourseTypeStr: String? = null,
    val Description: String? = null,
    val Price: Long = 0,
    val RegisterAmount: Long = 0,
    val SpecialSalePrice: Long? = null,
    val Location: String? = null,
    val CityId: Int? = null,
    val CityName: String? = null,
    val ProvinceId: Int? = null,
    val ProvinceName: String? = null,
    val CourseAddress: String? = null,
    val Duration: Int? = null,
    val Active: Boolean = true,
    val RegisterActive: Boolean? = true,
    val AllowDiscountCode: Boolean? = false,
    val ShamsiInsertDate: String? = null,
    val ShamsiStartDate: String? = null,
    val ShamsiFinishDate: String? = null,
    val ShamsiRegisterStartDate: String? = null,
    val ShamsiRegisterFinishDate: String? = null,
    val StartDate: String? = null,
    val FinishDate: String? = null,
    val RegisterStartDate: String? = null,
    val RegisterFinishDate: String? = null,
    val Rating: Double? = null,
    val StudentsCount: Int = 0,
    val FeaturedImageURL: String? = null,
    val FeaturedImageFileName: String? = null,
    val HasSaturdaySession: Boolean = false,
    val Saturday_StartTime: String? = null,
    val Saturday_FinishTime: String? = null,
    val HasSundaySession: Boolean = false,
    val Sunday_StartTime: String? = null,
    val Sunday_FinishTime: String? = null,
    val HasMondaySession: Boolean = false,
    val Monday_StartTime: String? = null,
    val Monday_FinishTime: String? = null,
    val HasTuesdaySession: Boolean = false,
    val Tuesday_StartTime: String? = null,
    val Tuesday_FinishTime: String? = null,
    val HasWednesdaySession: Boolean = false,
    val Wednesday_StartTime: String? = null,
    val Wednesday_FinishTime: String? = null,
    val HasThursdaySession: Boolean = false,
    val Thursday_StartTime: String? = null,
    val Thursday_FinishTime: String? = null,
    val HasFridaySession: Boolean = false,
    val Friday_StartTime: String? = null,
    val Friday_FinishTime: String? = null,
    val IsMemberLiked: Boolean = false,
    val LikeCount: Int = 0,
    val ContentReviewItemList: List<ContentReviewItemDto> = emptyList()
)

@Serializable
data class ProductDto(
    val ProductId: Int = 0,
    val MemberId: Int = 0,
    val MemberName: String? = null,
    val ProductName: String = "",
    val Description: String? = null,
    val Price: Long = 0,
    val SpecialSalePrice: Long? = null,
    val ProductCategories: String? = null,
    val ProductCategoryIdList: List<Int> = emptyList(),
    val Active: Boolean = true,
    val ProductImageFileName: String? = null,
    val FeaturedImageURL: String? = null,
    val FirstImageURL: String? = null,
    val SecondImageURL: String? = null,
    val ThirdImageURL: String? = null,
    val FourthImageURL: String? = null,
    val FifthImageURL: String? = null,
    val ImageURL: String? = null,
    val IsMemberLiked: Boolean = false,
    val LikeCount: Int = 0,
    val Rating: Double? = null,
    val RatingCount: Int = 0,
    val ContentReviewItemList: List<ContentReviewItemDto> = emptyList()
)

@Serializable
data class BlogPostDto(
    val BlogPostId: Int = 0,
    val MemberId: Int = 0,
    val Title: String = "",
    val Description: String? = null,
    val Content: String? = null,
    val Summary: String? = null,
    val ShamsiInsertDate: String? = null,
    val LikeCount: Int = 0,
    val CommentCount: Int = 0,
    val Active: Boolean = true,
    val BlogPostCategoryId: Int? = null,
    val BlogPostCategoryName: String? = null,
    val BlogPostCategoriesStr: String? = null,
    val FeaturedImageFileName: String? = null,
    val FeaturedImageURL: String? = null,
    val ImageURL: String? = null,
    val IsMemberLiked: Boolean = false,
    val Rating: Double? = null,
    val RatingCount: Int = 0,
    val ContentReviewItemList: List<ContentReviewItemDto> = emptyList()
)

@Serializable
data class MemberDto(
    val MemberId: Int = 0,
    val FirstName: String? = null,
    val LastName: String? = null,
    val MemberName: String = "",
    val Mobile: String? = null,
    val MemberGroupsStr: String? = null,
    val AvatarImageURL: String? = null,
    // Public profile fields (Member/Get)
    val Name: String? = null,
    val AboutMe: String? = null,
    val Email: String? = null,
    val Phone1: String? = null,
    val Phone2: String? = null,
    val WhatsappAccountMobileNumber: String? = null,
    val TelegramAccountId: String? = null,
    val InstagramAccountId: String? = null,
    val WebsiteAddress: String? = null,
    val CityName: String? = null,
    val ProvinceName: String? = null,
    val ShowAboutMeText: Boolean = false,
    val ShowContactInfo: Boolean = false,
    val ShowBlueTick: Boolean = false,
    val Gender: Boolean = true,
    val LikeCount: Int = 0,
    val IsCurrentMemberLikedThisMember: Boolean = false,
    val PortfolioViewModelList: List<PortfolioDto> = emptyList(),
    val ProductViewModelList: List<ProductDto> = emptyList(),
    val CourseViewModelList: List<CourseDto> = emptyList(),
    val ImageGalleryViewModelList: List<ProfileGalleryDto> = emptyList(),
    val BlogPostViewModelList: List<BlogPostDto> = emptyList()
)

@Serializable
data class ProfileGalleryDto(
    val ImageGalleryId: Int = 0,
    val Title: String = "",
    val ImageFileName: String? = null,
    val FeaturedImageURL: String? = null,
    val Likes: Int = 0,
    val LikeCount: Int = 0
)

@Serializable
data class LikeMemberResponse(
    val LikeCount: Int? = null,
    val Message: String? = null
)

@Serializable
data class MemberProfileEnvelope(
    val Data: MemberDto? = null
)

@Serializable
data class PortfolioDto(
    val PortfolioId: Int = 0,
    val MemberId: Int = 0,
    val MemberName: String? = null,
    val Title: String = "",
    val Description: String? = null,
    val Techniques: String? = null,
    val ShamsiInsertDate: String? = null,
    val LikeCount: Int = 0,
    val ViewsCount: Int = 0,
    val Rating: Double? = null,
    val Active: Boolean = true,
    val FeaturedImageFileName: String? = null,
    val FeaturedImageURL: String? = null,
    val FirstImageURL: String? = null,
    val SecondImageURL: String? = null,
    val ThirdImageURL: String? = null,
    val FourthImageURL: String? = null,
    val FifthImageURL: String? = null,
    val ImageURL: String? = null,
    val Images: List<String> = emptyList(),
    val IsMemberLiked: Boolean = false,
    val RatingCount: Int = 0,
    val ContentReviewItemList: List<ContentReviewItemDto> = emptyList()
)

@Serializable
data class SubscriptionPlanDto(
    val PlanId: Int = 0,
    val SubscriptionPlanId: Int = 0,
    val Name: String? = null,
    val Title: String = "",
    val DurationMonths: Int = 1,
    val Price: Long = 0,
    val OneMonthPrice: Long = 0,
    val ThreeMonthPrice: Long = 0,
    val SixMonthPrice: Long = 0,
    val OneYearPrice: Long = 0,
    val DiscountedPrice: Long? = null,
    val ColorHexCode: String = "#8B5CF6",
    val IconName: String? = "star",
    val Features: List<String> = emptyList(),
    val IsSpecial: Boolean = false,
    val IsPopular: Boolean = false,
    val BadgeText: String = "",
    val IsInfinityPlan: Boolean = false,
    val PlanOption_ShowContanctInfo: Boolean = false,
    val PlanOption_DirectCall: Boolean = false,
    val PlanOption_ShowWebSiteUrl: Boolean = false,
    val PlanOption_ShowTelegramId: Boolean = false,
    val PlanOption_ShowInstagramId: Boolean = false,
    val PlanOption_ShowWhatsappNumber: Boolean = false,
    val PlanOption_AddCourse: Boolean = false,
    val PlanOption_AddJob: Boolean = false,
    val PlanOption_AddProduct: Boolean = false,
    val PlanOption_AddDocument: Boolean = false,
    val PlanOption_AddPortfolio: Boolean = false,
    val PlanOption_AddPost: Boolean = false,
    val PlanOption_AboutMeText: Boolean = false,
    val PlanOption_UploadResume: Boolean = false,
    val PlanOption_UploadIntroVideo: Boolean = false,
    val PlanOption_UnlimitedImageUpload: Boolean = false
)

@Serializable
data class CourseRegistrationDto(
    val CourseRegistrationId: Int = 0,
    val RegistrationId: Int = 0,
    val CourseId: Int = 0,
    val CourseName: String = "",
    val CourseFeaturedImageURL: String? = null,
    val CourseMemberName: String? = null,
    val CourseMemberAvatarImageURL: String? = null,
    val MemberName: String? = null,
    val MemberMobile: String? = null,
    val RegisterDate: String = "",
    val ShamsiRegisterDate: String? = null,
    val Price: Long = 0,
    val CourseRegisterAmount: Long = 0,
    val Discount: Long = 0,
    val TotalAmount: Long = 0,
    val State: Int = 0,
    val StateStr: String? = null,
    val PaymentType: Int = 0,
    val PaymentTypeStr: String? = null,
    val StatusText: String = "در حال برگزاری",
    val TotalSessions: Int = 12,
    val CompletedSessions: Int = 6,
    val Location: String? = null
)

@Serializable
data class ImageGalleryItemDto(
    val ImageGalleryItemId: Int = 0,
    val ImageGalleryId: Int = 0,
    val Title: String = "",
    val ImageFileName: String? = null,
    val ImageURL: String? = null,
    val ShowOrder: Int = 0,
    val Active: Boolean = true,
    val InsertDate: String? = null,
    val ShamsiInsertDate: String? = null
)

@Serializable
data class GalleryItemDto(
    val ImageGalleryId: Int = 0,
    val Title: String = "",
    val MemberId: Int = 0,
    val MemberName: String? = null,
    val ImageCount: Int = 0,
    val Rating: Double? = null,
    val LikeCount: Int = 0,
    val FeaturedImageURL: String? = null,
    val Active: Boolean = true,
    val InsertDate: String? = null,
    val ShamsiInsertDate: String? = null,
    val ImageGalleryItemList: List<ImageGalleryItemDto> = emptyList(),
    val IsMemberLiked: Boolean = false,
    val RatingCount: Int = 0,
    val ContentReviewItemList: List<ContentReviewItemDto> = emptyList()
)

@Serializable
data class GalleryResponseDto(
    val Items: List<GalleryItemDto> = emptyList(),
    val CurrentPage: Int = 1,
    val TotalPages: Int = 1,
    val PageSize: Int = 20,
    val TotalCount: Int = 0,
    val HasPrevious: Boolean = false,
    val HasNext: Boolean = false
)

@Serializable
data class BuySubscriptionRequest(
    val MemberId: Int,
    val SubscriptionPlanId: Int,
    val SubscriptionTotalDays: Int,
    val DiscountCode: String = ""
)

@Serializable
data class BuySubscriptionResponse(
    val GatewayURL: String? = null,
    val Success: Boolean = false,
    val Message: String? = null,
    val MemberSubscriptionId: Int? = null
)

@Serializable
data class GenericListEnvelope<T>(
    val Data: List<T> = emptyList(),
    val Success: Boolean = true,
    val Message: String? = null
)

@Serializable
data class AppUpdateResponse(
    val MobileAppLastVersion: String? = null,
    val MobileAppHasUpdate: Boolean = false,
    val IsMobileAppUpdateRequired: Boolean = false,
    val MobileAppDownloadLink: String? = null,
    val MobileAppCafeBazarPageLink: String? = null
)



@Serializable
data class ProvinceDto(
    val ProvinceId: Int = 0,
    val ProvinceName: String = "",
    val Active: Boolean = true
)

@Serializable
data class ProvinceResponseDto(
    val Items: List<ProvinceDto> = emptyList(),
    val TotalCount: Int = 0
)

@Serializable
data class CityDto(
    val CityId: Int = 0,
    val ProvinceId: Int = 0,
    val CityName: String = "",
    val Active: Boolean = true
)

@Serializable
data class CityResponseDto(
    val Items: List<CityDto> = emptyList(),
    val TotalCount: Int = 0
)

@Serializable
data class ProfileInfoToEditDto(
    val MemberId: Int = 0,
    val Name: String? = null,
    val FirstName: String? = null,
    val LastName: String? = null,
    val Title: String? = null,
    val AboutMe: String? = null,
    val AvatarUrl: String? = null,
    val AvatarImageURL: String? = null,
    val IntroVideoUrl: String? = null,
    val IntroVideo: String? = null,
    val CityId: Int? = null,
    val CityName: String? = null,
    val ProvinceId: Int? = null,
    val ProvinceName: String? = null,
    val Mobile: String? = null,
    val Phone1: String? = null,
    val Phone2: String? = null,
    val Email: String? = null,
    val WebsiteAddress: String? = null,
    val TelegramAccountId: String? = null,
    val InstagramAccountId: String? = null,
    val WhatsappAccountMobileNumber: String? = null,
    val Address: String? = null,
    val PostalCode: String? = null,
    val MemberGroupIdList: List<Int> = emptyList()
)

@Serializable
data class UpdateProfileRequest(
    val MemberId: Int = 0,
    val AboutMe: String = "",
    val CityId: Int = 0,
    val ProvinceId: Int = 0,
    val Mobile: String = "",
    val Phone1: String = "",
    val Phone2: String = "",
    val Email: String = "",
    val WebsiteAddress: String = "",
    val TelegramAccountId: String = "",
    val WhatsappAccountMobileNumber: String = "",
    val InstagramAccountId: String = "",
    val Address: String = "",
    val MemberGroupIdList: List<Int> = emptyList()
)

@Serializable
data class UpdateProfileResponse(
    val Success: Boolean = false,
    val Message: String? = null
)

// ==================== Step 6 Studio Models ====================

@Serializable
data class ProductCategoryDto(
    val ProductCategoryId: Int = 0,
    val Name: String? = null,
    val Active: Boolean = true
)

@Serializable
data class ProductCategoryResponseDto(
    val Data: List<ProductCategoryDto> = emptyList(),
    val TotalCount: Int = 0
)

@Serializable
data class SaveProductRequest(
    val ProductId: Int = 0,
    val MemberId: Int = 0,
    val MemberName: String = "",
    val ProductName: String = "",
    val Description: String = "",
    val Price: Double = 0.0,
    val SpecialSalePrice: Double = 0.0,
    val ProductCategories: String = "",
    val ProductCategoryIdList: List<Int> = emptyList(),
    val FeaturedImageFileName: String = "",
    val FeaturedImageURL: String = "",
    val FirstImageFileName: String = "",
    val FirstImageURL: String = "",
    val SecondImageFileName: String = "",
    val SecondImageURL: String = "",
    val ThirdImageFileName: String = "",
    val ThirdImageURL: String = "",
    val FourthImageFileName: String = "",
    val FourthImageURL: String = "",
    val FifthImageFileName: String = "",
    val FifthImageURL: String = "",
    val LikeCount: Int = 0,
    val Rating: Double = 0.0,
    val Active: Boolean = true,
    val ActiveStr: String = "فعال",
    val InsertDate: String = ""
)

@Serializable
data class ProductMutationResponse(
    val ProductId: Int = 0,
    val Success: Boolean = true,
    val Message: String? = null
)

@Serializable
data class BlogPostCategoryDto(
    val BlogPostCategoryId: Int = 0,
    val Name: String? = null,
    val Active: Boolean = true
)

@Serializable
data class BlogPostCategoryResponseDto(
    val Data: List<BlogPostCategoryDto> = emptyList(),
    val TotalCount: Int = 0
)

@Serializable
data class SaveBlogPostRequest(
    val BlogPostId: Int = 0,
    val MemberId: Int = 0,
    val Title: String = "",
    val Content: String = "",
    val CommentEnabled: Boolean = true,
    val Active: Boolean = true,
    val LikeCount: Int = 0,
    val Rating: Double = 0.0,
    val cateogoryIdList: List<Int> = emptyList(),
    val BlogPostCategoriesStr: String = "",
    val FeaturedImageURL: String = "",
    val FeaturedImageFileName: String = "",
    val InsertDate: String = ""
)

@Serializable
data class SavePortfolioRequest(
    val PortfolioId: Int = 0,
    val MemberId: Int = 0,
    val Title: String = "",
    val Description: String = "",
    val FeaturedImageURL: String = "",
    val FeaturedImageFileName: String = "",
    val FirstImageURL: String = "",
    val FirstImageFileName: String = "",
    val SecondImageURL: String = "",
    val SecondImageFileName: String = "",
    val ThirdImageURL: String = "",
    val ThirdImageFileName: String = "",
    val FourthImageURL: String = "",
    val FourthImageFileName: String = "",
    val FifthImageURL: String = "",
    val FifthImageFileName: String = "",
    val Active: Boolean = true,
    val InsertDate: String = "",
    val Rating: Double = 0.0,
    val LikeCount: Int = 0
)

@Serializable
data class SaveGalleryRequest(
    val ImageGalleryId: Int = 0,
    val Title: String = "",
    val MemberId: Int = 0,
    val MemberName: String = "",
    val ImageCount: Int = 0,
    val Rating: Double = 0.0,
    val LikeCount: Int = 0,
    val FeaturedImageURL: String = "",
    val Active: Boolean = true,
    val ActiveStr: String = "فعال",
    val InsertDate: String = "",
    val ShamsiInsertDate: String = "",
    val ImageGalleryItemList: List<ImageGalleryItemDto> = emptyList(),
    val IsMemberLiked: Boolean = false,
    val RatingCount: Int = 0,
    val ContentReviewItemList: List<ContentReviewItemDto> = emptyList()
)

@Serializable
data class SaveGalleryItemRequest(
    val ImageGalleryItemId: Int = 0,
    val ImageGalleryId: Int = 0,
    val Title: String = "",
    val ImageFileName: String = "",
    val ImageURL: String = "",
    val ShowOrder: Int = 0,
    val Active: Boolean = true,
    val ActiveStr: String = "فعال",
    val InsertDate: String = "",
    val ShamsiInsertDate: String = ""
)

@Serializable
data class SingleGalleryResponseDto(
    val ImageGallery: GalleryItemDto? = null,
    val ImageGalleryItemList: List<ImageGalleryItemDto> = emptyList(),
    val IsMemberLiked: Boolean = false,
    val RatingCount: Int = 0,
    val ContentReviewItemList: List<ContentReviewItemDto> = emptyList()
)

@Serializable
data class PortfolioMutationResponse(
    val PortfolioId: Int = 0,
    val Success: Boolean = true,
    val Message: String? = null
)


@Serializable
data class PagedListEnvelope<T>(
    val Data: List<T> = emptyList(),
    val Total: Int = 0,
    val Success: Boolean = true,
    val Message: String? = null
)

@Serializable
data class CourseStudentRegistrationDto(
    val CourseRegistrationId: Int = 0,
    val MemberId: Int = 0,
    val MemberName: String? = null,
    val MemberMobile: String? = null,
    val MemberAvatarImageURL: String? = null,
    val CourseRegisterAmount: Long = 0,
    val Discount: Long = 0,
    val TotalAmount: Long = 0,
    val PaymentType: Int = 0,
    val PaymentTypeStr: String? = "پرداخت آنلاین",
    val ShamsiRegisterDate: String? = null,
    val State: Int = 1, // 1: در انتظار, 2: تایید شده
    val StateStr: String? = "در انتظار پرداخت",
    val CourseId: Int = 0,
    val CourseName: String? = null
)

@Serializable
data class CourseDetailResponseDto(
    val Course: CourseDto? = null,
    val IsMemberLiked: Boolean = false,
    val ContentReviewItemList: List<ContentReviewItemDto> = emptyList(),
    val Success: Boolean = true,
    val Message: String? = null
)

@Serializable
data class SaveCourseRequest(
    val CourseId: Int = 0,
    val CourseName: String = "",
    val MemberId: Int = 0,
    val MemberName: String? = null,
    val CourseType: String = "1",
    val CourseTypeStr: String = "حضوری",
    val CityId: Int? = null,
    val CityName: String? = null,
    val ProvinceId: Int? = null,
    val ProvinceName: String? = null,
    val CourseAddress: String? = null,
    val Description: String? = null,
    val RegisterAmount: Long = 0,
    val AllowDiscountCode: Boolean = false,
    val RegisterActive: Boolean = true,
    val Active: Boolean = true,
    val StartDate: String? = null,
    val ShamsiStartDate: String? = null,
    val FinishDate: String? = null,
    val ShamsiFinishDate: String? = null,
    val RegisterStartDate: String? = null,
    val ShamsiRegisterStartDate: String? = null,
    val RegisterFinishDate: String? = null,
    val ShamsiRegisterFinishDate: String? = null,
    val FeaturedImageFileName: String? = null,
    val FeaturedImageURL: String? = null,
    val HasSaturdaySession: Boolean = false,
    val Saturday_StartTime: String? = null,
    val Saturday_FinishTime: String? = null,
    val HasSundaySession: Boolean = false,
    val Sunday_StartTime: String? = null,
    val Sunday_FinishTime: String? = null,
    val HasMondaySession: Boolean = false,
    val Monday_StartTime: String? = null,
    val Monday_FinishTime: String? = null,
    val HasTuesdaySession: Boolean = false,
    val Tuesday_StartTime: String? = null,
    val Tuesday_FinishTime: String? = null,
    val HasWednesdaySession: Boolean = false,
    val Wednesday_StartTime: String? = null,
    val Wednesday_FinishTime: String? = null,
    val HasThursdaySession: Boolean = false,
    val Thursday_StartTime: String? = null,
    val Thursday_FinishTime: String? = null,
    val HasFridaySession: Boolean = false,
    val Friday_StartTime: String? = null,
    val Friday_FinishTime: String? = null,
    val IsMemberLiked: Boolean = false,
    val LikeCount: Int = 0,
    val ContentReviewItemList: List<ContentReviewItemDto> = emptyList()
)

@Serializable
data class ContentReviewItemRating(
    val ContentReviewItemId: Int = 0,
    val Rating: Int = 5
)

@Serializable
data class ContentReviewRequest(
    val ContnetId: Int = 0,
    val reviewItemRatings: List<ContentReviewItemRating> = emptyList()
)


@Serializable
data class HomePageSlideDto(
    val HomePageSlideId: Int = 0,
    val Title: String? = null,
    val ImageURL: String? = null,
    val TargetScreen: String? = null,
    val Active: Boolean = true,
    val ShowOrder: Int? = null
)


@Serializable
data class ContentReviewItemDto(
    val ContentReviewItemId: Int = 0,
    val Text: String = "",
    val ShowOrder: Int = 0,
    val Active: Boolean = true,
    val CalculatedAverageRating: Double? = 0.0
)

@Serializable
data class ActiveSubscriptionPlanDto(
    val MemberId: Int = 0,
    val MemberName: String? = null,
    val SubscriptionPlanId: Int = 0,
    val SubscriptionPlanName: String? = null,
    val StartDate: String? = null,
    val ShamsiStartDate: String? = null,
    val FinishDate: String? = null,
    val ShamsiFinishDate: String? = null,
    val SubscriptionTotalDays: Int = 0,
    val RemainingDaysToFinishDate: Int = 0,
    val IsInfinityPlan: Boolean = false
)

@Serializable
data class MemberDocumentTypeDto(
    val MemberDocumentTypeId: Int = 0,
    val Name: String = "",
    val Description: String? = null,
    val Active: Boolean = true
)

@Serializable
data class MemberDocumentDto(
    val MemberDocumentId: Int = 0,
    val MemberId: Int = 0,
    val MemberName: String? = null,
    val MemberDocumentTypeId: Int = 0,
    val MemberDocumentTypeName: String? = null,
    val MemberDocumentFileName: String? = null,
    val FileURL: String? = null,
    val InsertDate: String? = null,
    val ShamsiInsertDate: String? = null
)

@Serializable
data class CourseRegistrationRequest(
    val CourseId: Int = 0,
    val MemberId: Int = 0,
    val DiscountCode: String? = null,
    val IsDirectRegistration: Boolean = true
)

@Serializable
data class CourseRegisterResponse(
    val Success: Boolean = true,
    val Message: String? = null,
    val GatewayURL: String? = null,
    val CourseRegistrationId: Int? = null,
    val PaymentType: String? = null,
    val FreeRegistration: Boolean = false
)

@Serializable
data class AppMarketCheckCoursePaymentRequest(
    val CourseRegistrationId: Int = 0,
    val PurchaseToken: String = ""
)

@Serializable
data class AppMarketCheckCoursePaymentResponse(
    val Success: Boolean = true,
    val Message: String? = null
)

@Serializable
data class AppMarketCheckSubscriptionPaymentRequest(
    val MemberSubscriptionId: Int = 0,
    val Token: String = "",
    val PurchaseToken: String = ""
)

@Serializable
data class AppMarketCheckSubscriptionPaymentResponse(
    val Success: Boolean = true,
    val Message: String? = null
)

@Serializable
data class CourseDiscountResponse(
    val Success: Boolean = true,
    val Message: String? = null,
    val Discount: Long = 0L
)

@Serializable
data class SubscriptionDiscountResponse(
    val Success: Boolean = true,
    val Message: String? = null,
    val DiscountAmount: Long = 0L,
    val DiscountPercent: Int = 0
)

@Serializable
data class ProductDetailResponseDto(
    val Product: ProductDto? = null,
    val IsMemberLiked: Boolean = false,
    val ContentReviewItemList: List<ContentReviewItemDto> = emptyList(),
    val Success: Boolean = true,
    val Message: String? = null
)

@Serializable
data class PortfolioDetailResponseDto(
    val Portfolio: PortfolioDto? = null,
    val IsMemberLiked: Boolean = false,
    val ContentReviewItemList: List<ContentReviewItemDto> = emptyList(),
    val Success: Boolean = true,
    val Message: String? = null
)

@Serializable
data class BlogPostDetailResponseDto(
    val BlogPost: BlogPostDto? = null,
    val IsMemberLiked: Boolean = false,
    val ContentReviewItemList: List<ContentReviewItemDto> = emptyList(),
    val Success: Boolean = true,
    val Message: String? = null
)
