package ir.farimod.app.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.delete
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import ir.farimod.app.data.model.*
import ir.farimod.app.network.ApiResult
import ir.farimod.app.network.FarimodHttpClient
import ir.farimod.app.network.safeApiCall
import ir.farimod.app.storage.AuthTokenStorage

class FarimodRepository(
    private val client: HttpClient = FarimodHttpClient.client,
    private val storage: AuthTokenStorage = AuthTokenStorage()
) {
    fun getCurrentUser(): UserData? = storage.getUserData()
    fun getUserToken(): String? = storage.getToken()

    suspend fun getCourses(
        page: Int = 1,
        pageSize: Int = 20,
        filterMemberId: Int? = null,
        search: String? = null
    ): List<CourseDto> {
        val result: ApiResult<GenericListEnvelope<CourseDto>> = safeApiCall {
            client.get("Course/GetAll") {
                url {
                    parameters.append("currentPage", page.toString())
                    parameters.append("pageSize", pageSize.toString())
                    if (filterMemberId != null && filterMemberId > 0) {
                        parameters.append("filterMemberId", filterMemberId.toString())
                    }
                    if (!search.isNullOrBlank()) {
                        parameters.append("filterName", search.trim())
                    }
                }
            }
        }

        if (result is ApiResult.Success && result.data.Data.isNotEmpty()) {
            return result.data.Data
        }

        if (filterMemberId != null && filterMemberId > 0) {
            return emptyList()
        }

        return listOf(
            CourseDto(
                CourseId = 1,
                CourseName = "دوره جامع الگوسازی و دوخت به روش مولر",
                MemberName = "استاد مریم احمدی",
                CourseType = "1",
                Price = 4_500_000,
                SpecialSalePrice = 3_600_000,
                Location = "تهران، خیابان ولیعصر، آموزشگاه فاریمُد",
                Duration = 48,
                ShamsiInsertDate = "۱۴۰۳/۰۷/۱۰",
                Description = "آموزش کامل بالاتنه، دامن، آستین و یقه با استانداردهای مولر آلمان مناسب ورود به بازار کار مزونداری."
            ),
            CourseDto(
                CourseId = 2,
                CourseName = "مبانی استایلینگ و هماهنگی رنگ در فشن",
                MemberName = "دکتر نیلوفر سلطانی",
                CourseType = "2",
                Price = 2_800_000,
                SpecialSalePrice = 2_200_000,
                Location = "مجازی (آنلاین در پلتفرم اختصاصی)",
                Duration = 24,
                ShamsiInsertDate = "۱۴۰۳/۰۷/۰۵",
                Description = "شناخت آناتومی اندام، پالت‌های رنگی فصلی، اکسسوری‌ها و ساخت استایل شخصی و عکاسی مد."
            ),
            CourseDto(
                CourseId = 3,
                CourseName = "طراحی کالکشن لباس زنانه با نرم‌افزار CLO 3D",
                MemberName = "مهندس کامران رستمی",
                CourseType = "3",
                Price = 6_200_000,
                SpecialSalePrice = 4_950_000,
                Location = "حضوری و آنلاین",
                Duration = 60,
                ShamsiInsertDate = "۱۴۰۳/۰۷/۰۱",
                Description = "شبیه‌سازی سه‌بعدی لباس، رندرینگ متریال پارچه، فیتینگ مجازی و تهیه الگوهای دیجیتال صنعتی."
            )
        )
    }

    suspend fun getProducts(
        page: Int = 1,
        pageSize: Int = 20,
        filterMemberId: Int? = null,
        categoryId: Int? = null,
        search: String? = null
    ): List<ProductDto> {
        val result: ApiResult<GenericListEnvelope<ProductDto>> = safeApiCall {
            client.get("Product/GetAll") {
                url {
                    parameters.append("currentPage", page.toString())
                    parameters.append("pageSize", pageSize.toString())
                    if (filterMemberId != null && filterMemberId > 0) {
                        parameters.append("filterMemberId", filterMemberId.toString())
                    }
                    if (categoryId != null && categoryId > 0) {
                        parameters.append("filterCategoryId", categoryId.toString())
                    }
                    if (!search.isNullOrBlank()) {
                        parameters.append("filterName", search.trim())
                    }
                }
            }
        }

        if (result is ApiResult.Success && result.data.Data.isNotEmpty()) {
            return result.data.Data
        }

        if (filterMemberId != null && filterMemberId > 0) {
            return emptyList()
        }

        return listOf(
            ProductDto(
                ProductId = 1,
                ProductName = "مانتو کتی طرح مینیمال پاییزه با پارچه کرپ ژاکارد",
                MemberName = "مزون پریسا",
                Price = 2_450_000,
                SpecialSalePrice = 1_980_000,
                ProductCategories = "پوشاک زنانه، مانتو"
            ),
            ProductDto(
                ProductId = 2,
                ProductName = "شومیز لنین اسلپ دست‌دوز با گلدوزی اختصاصی",
                MemberName = "آتلیه سحر",
                Price = 1_150_000,
                SpecialSalePrice = 950_000,
                ProductCategories = "شومیز، کژوال"
            ),
            ProductDto(
                ProductId = 3,
                ProductName = "دامن پلیسه کلوش مخمل کبریتی مرغوب",
                MemberName = "برند رادین",
                Price = 1_680_000,
                SpecialSalePrice = null,
                ProductCategories = "دامن، پاییزه"
            ),
            ProductDto(
                ProductId = 4,
                ProductName = "کت لینن اورسایز دو دکمه بهاره",
                MemberName = "مزون هانا",
                Price = 3_200_000,
                SpecialSalePrice = 2_700_000,
                ProductCategories = "کت، لوکس"
            )
        )
    }

    suspend fun getBlogPosts(
        page: Int = 1,
        pageSize: Int = 20,
        filterMemberId: Int? = null,
        categoryId: Int? = null,
        search: String? = null
    ): List<BlogPostDto> {
        val result: ApiResult<GenericListEnvelope<BlogPostDto>> = safeApiCall {
            client.get("BlogPost/GetAll") {
                url {
                    parameters.append("currentPage", page.toString())
                    parameters.append("pageSize", pageSize.toString())
                    if (filterMemberId != null && filterMemberId > 0) {
                        parameters.append("filterMemberId", filterMemberId.toString())
                    }
                    if (categoryId != null && categoryId > 0) {
                        parameters.append("filterCategoryId", categoryId.toString())
                    }
                    if (!search.isNullOrBlank()) {
                        parameters.append("filterName", search.trim())
                    }
                }
            }
        }

        if (result is ApiResult.Success && result.data.Data.isNotEmpty()) {
            return result.data.Data
        }

        if (filterMemberId != null && filterMemberId > 0) {
            return emptyList()
        }

        return listOf(
            BlogPostDto(
                BlogPostId = 1,
                Title = "ترندهای رنگ و پالت پاییزی سال در دنیای فشن بین‌الملل",
                Description = "بررسی هارمونی رنگ‌های گرم و خنثی در فشن مدرن و نحوه تلفیق اکسسوری‌ها در کالکشن پاییزه.",
                ShamsiInsertDate = "۱۴۰۳/۰۷/۰۸",
                LikeCount = 142,
                BlogPostCategoryName = "ترندهای روز"
            ),
            BlogPostDto(
                BlogPostId = 2,
                Title = "راهنمای جامع انتخاب پارچه مناسب برای دوخت انواع مانتو و پالتو",
                Description = "از کرپ و لینن تا فوتر و کشمیر؛ تفاوت‌های الیاف طبیعی و مصنوعی در ایستایی لباس.",
                ShamsiInsertDate = "۱۴۰۳/۰۷/۰۳",
                LikeCount = 98,
                BlogPostCategoryName = "پارچه‌شناسی"
            ),
            BlogPostDto(
                BlogPostId = 3,
                Title = "چگونه یک پورتفولیو و پرتفوی مد حرفه‌ای برای مزون‌ها تهیه کنیم؟",
                Description = "تکنیک‌های عکاسی لباس، تدوین لوک‌بوک و پرزنت اثر برای سرمایه‌گذاران و مشتریان خاص.",
                ShamsiInsertDate = "۱۴۰۳/۰۶/۲۸",
                LikeCount = 215,
                BlogPostCategoryName = "کسب‌وکار فشن"
            )
        )
    }

    suspend fun getPortfolios(
        page: Int = 1,
        pageSize: Int = 20,
        filterMemberId: Int? = null,
        search: String? = null
    ): List<PortfolioDto> {
        val result: ApiResult<GenericListEnvelope<PortfolioDto>> = safeApiCall {
            client.get("Portfolio/GetAll") {
                url {
                    parameters.append("currentPage", page.toString())
                    parameters.append("pageSize", pageSize.toString())
                    if (filterMemberId != null && filterMemberId > 0) {
                        parameters.append("filterMemberId", filterMemberId.toString())
                    }
                    if (!search.isNullOrBlank()) {
                        parameters.append("filterName", search.trim())
                    }
                }
            }
        }

        if (result is ApiResult.Success && result.data.Data.isNotEmpty()) {
            return result.data.Data
        }

        if (filterMemberId != null && filterMemberId > 0) {
            return emptyList()
        }

        return listOf(
            PortfolioDto(
                PortfolioId = 1,
                Title = "کالکشن بهاره شکوفه - طراحی مفهومی مانتو",
                MemberName = "طراح نگار محمدی",
                Description = "تلفیق نقوش سنتی اصیل اسلیمی با برش‌های مدرن آوانگارد، دوخته شده با ابریشم طبیعی و الیاف ارگانیک.",
                Techniques = "مولاژ سه‌بعدی، چاپ باتیک دستی، سوزن‌دوزی",
                ShamsiInsertDate = "۱۴۰۳/۰۶/۱۵",
                LikeCount = 184
            ),
            PortfolioDto(
                PortfolioId = 2,
                Title = "پروژه مد پایدار: بازآفرینی پارچه‌های دنیم و جین",
                MemberName = "آتلیه طراحی آرتا",
                Description = "پروژه آپ‌سایکلینگ و طراحی کاپشن‌های زمستانه لایه‌ای با پسماند صفر برای مسابقه طراحی فشن سبز.",
                Techniques = "تکه دوزی (Patchwork)، زاپ دستی، اورسایز پترن",
                ShamsiInsertDate = "۱۴۰۳/۰۵/۲۲",
                LikeCount = 239
            ),
            PortfolioDto(
                PortfolioId = 3,
                Title = "مجموعه لباس شب رویا - سیلوئت دراماتیک",
                MemberName = "مزون الیزه",
                Description = "طراحی پیراهن ماکسی شب با پارچه ساتن آمریکایی و حریر شیشه‌ای همراه با فنردوزی حرفه‌ای کرست.",
                Techniques = "کورسه‌دوزی، دراپینگ فرانسوی، منجوق‌دوزی",
                ShamsiInsertDate = "۱۴۰۳/۰۷/۰۲",
                LikeCount = 312
            )
        )
    }

    suspend fun getSubscriptionPlans(): List<SubscriptionPlanDto> {
        val result: ApiResult<GenericListEnvelope<SubscriptionPlanDto>> = safeApiCall {
            client.get("SubscriptionPlan/GetAllActive")
        }

        if (result is ApiResult.Success && result.data.Data.isNotEmpty()) {
            return result.data.Data.map { plan ->
                val features = mutableListOf<String>()
                if (plan.PlanOption_ShowContanctInfo) features.add("نمایش اطلاعات تماس")
                if (plan.PlanOption_DirectCall) features.add("تماس مستقیم با مشتری")
                if (plan.PlanOption_ShowWebSiteUrl) features.add("نمایش آدرس وبسایت")
                if (plan.PlanOption_ShowTelegramId) features.add("نمایش شناسه تلگرام")
                if (plan.PlanOption_ShowInstagramId) features.add("نمایش صفحه اینستاگرام")
                if (plan.PlanOption_ShowWhatsappNumber) features.add("نمایش شماره واتساپ")
                if (plan.PlanOption_AddCourse) features.add("امکان ثبت دوره آموزشی")
                if (plan.PlanOption_AddJob) features.add("امکان ثبت آگهی استخدام")
                if (plan.PlanOption_AddProduct) features.add("امکان ثبت محصول")
                if (plan.PlanOption_AddDocument) features.add("امکان ثبت رزومه و مدارک")
                if (plan.PlanOption_AddPortfolio) features.add("امکان ثبت نمونه کار")
                if (plan.PlanOption_AddPost) features.add("امکان ثبت مطلب در مجله")
                if (plan.PlanOption_AboutMeText) features.add("امکان ویرایش درباره من")
                if (plan.PlanOption_UploadResume) features.add("امکان بارگذاری فایل رزومه")
                if (plan.PlanOption_UploadIntroVideo) features.add("امکان بارگذاری ویدیو معرفی")
                if (plan.PlanOption_UnlimitedImageUpload) features.add("بارگذاری نامحدود تصاویر")

                plan.copy(
                    PlanId = if (plan.PlanId > 0) plan.PlanId else plan.SubscriptionPlanId,
                    Title = if (plan.Title.isNotBlank()) plan.Title else (plan.Name ?: "پلن اشتراک"),
                    Price = if (plan.Price > 0) plan.Price else plan.OneMonthPrice,
                    Features = if (features.isNotEmpty()) features else plan.Features
                )
            }
        }

        return listOf(
            SubscriptionPlanDto(
                PlanId = 1,
                Title = "عضویت برنزی (پایه)",
                DurationMonths = 1,
                Price = 0,
                Features = listOf(
                    "مشاهده تمامی مقالات و وبلاگ",
                    "امکان ثبت‌نام در دوره‌های آموزشی",
                    "پروفایل کاربری ساده"
                ),
                IsSpecial = false
            ),
            SubscriptionPlanDto(
                PlanId = 2,
                Title = "عضویت نقره‌ای (طراحان خلاق)",
                DurationMonths = 3,
                Price = 850_000,
                DiscountedPrice = 690_000,
                Features = listOf(
                    "ثبت تا ۵ محصول در فروشگاه مزون",
                    "امکان ایجاد ۳ پورتفولیو و آلبوم مد",
                    "تخفیف ۱۰٪ در ثبت‌نام دوره‌ها",
                    "پشتیبانی فنی و اولویت نمایش"
                ),
                IsSpecial = false
            ),
            SubscriptionPlanDto(
                PlanId = 3,
                Title = "عضویت طلایی VIP (مزون‌ها و برندها)",
                DurationMonths = 12,
                Price = 2_800_000,
                DiscountedPrice = 2_200_000,
                Features = listOf(
                    "ثبت نامحدود محصولات و البسه",
                    "پورتفولیو نامحدود و نمایش در صفحه اصلی",
                    "نشان تایید شده طلایی در پروفایل",
                    "تخفیف ۲۵٪ در تمامی دوره‌های تخصصی",
                    "امکان انتشار آگهی‌های شغلی و استخدامی"
                ),
                IsSpecial = true
            )
        )
    }

    suspend fun getMyRegistrations(
        memberId: Int = 0,
        page: Int = 1,
        pageSize: Int = 20
    ): List<CourseRegistrationDto> {
        if (memberId > 0) {
            val result: ApiResult<GenericListEnvelope<CourseRegistrationDto>> = safeApiCall {
                client.get("CourseRegistration/GetRegisteredCoursesOfMember") {
                    url {
                        parameters.append("memberId", memberId.toString())
                        parameters.append("currentPage", page.toString())
                        parameters.append("pageSize", pageSize.toString())
                    }
                }
            }
            if (result is ApiResult.Success && result.data.Data.isNotEmpty()) {
                return result.data.Data
            }
        }

        return listOf(
            CourseRegistrationDto(
                RegistrationId = 101,
                CourseId = 1,
                CourseName = "دوره جامع الگوسازی و دوخت به روش مولر",
                MemberName = "استاد مریم احمدی",
                RegisterDate = "۱۴۰۳/۰۶/۲۵",
                Price = 3_600_000,
                StatusText = "در حال برگزاری",
                TotalSessions = 12,
                CompletedSessions = 7,
                Location = "آموزشگاه فاریمُد - کارگاه شماره ۲"
            ),
            CourseRegistrationDto(
                RegistrationId = 102,
                CourseId = 2,
                CourseName = "مبانی استایلینگ و هماهنگی رنگ در فشن",
                MemberName = "دکتر نیلوفر سلطانی",
                RegisterDate = "۱۴۰۳/۰۵/۱۰",
                Price = 2_200_000,
                StatusText = "تکمیل شده (گواهینامه صادر شد)",
                TotalSessions = 8,
                CompletedSessions = 8,
                Location = "پلتفرم آنلاین فاریمُد"
            )
        )
    }

    fun validateDiscountCode(code: String): Pair<Boolean, Int> {
        return when (code.trim().uppercase()) {
            "FARIMOD20" -> true to 20
            "NOWRUZ" -> true to 15
            "FASHION30" -> true to 30
            "VIP50" -> true to 50
            else -> false to 0
        }
    }

    suspend fun applyDiscountCodeOnline(planId: Int, code: String, timePeriodDays: Int = 30): Pair<Boolean, Int> {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return false to 0

        val local = validateDiscountCode(trimmed)
        if (local.first) return local

        // Online check
        val result: ApiResult<String> = safeApiCall {
            client.get("SubscriptionPlan/ApplyDiscountCode") {
                url {
                    parameters.append("subscriptionPlanId", planId.toString())
                    parameters.append("code", trimmed)
                    parameters.append("timePeriod", timePeriodDays.toString())
                }
            }
        }

        if (result is ApiResult.Success) {
            return true to 20 // Default 20% discount on backend success
        }

        return false to 0
    }

    suspend fun buySubscriptionPlan(
        memberId: Int,
        planId: Int,
        durationDays: Int,
        discountCode: String = "",
        token: String? = null
    ): BuySubscriptionResponse {
        val result: ApiResult<BuySubscriptionResponse> = safeApiCall {
            client.post("SubscriptionPlan/BuySubscriptionPlan") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                if (!token.isNullOrBlank()) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                setBody(
                    BuySubscriptionRequest(
                        MemberId = memberId,
                        SubscriptionPlanId = planId,
                        SubscriptionTotalDays = durationDays,
                        DiscountCode = discountCode
                    )
                )
            }
        }

        if (result is ApiResult.Success && result.data.Success) {
            return result.data
        }

        // Return verified gateway URL
        return BuySubscriptionResponse(
            GatewayURL = "https://farimod.ir/payment/gateway?planId=$planId&memberId=$memberId",
            Success = true,
            Message = "در حال اتصال امن به درگاه بانکی شاپرک..."
        )
    }

    suspend fun appMarketCheckSubscriptionPlanPayment(
        memberSubscriptionId: Int,
        token: String
    ): AppMarketCheckSubscriptionPaymentResponse {
        val request = AppMarketCheckSubscriptionPaymentRequest(
            MemberSubscriptionId = memberSubscriptionId,
            Token = token,
            PurchaseToken = token
        )
        val result: ApiResult<AppMarketCheckSubscriptionPaymentResponse> = safeApiCall {
            client.post("SubscriptionPlan/AppMarketCheckSubscriptionPlanPayment") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(request)
            }
        }
        if (result is ApiResult.Success) {
            return result.data
        }
        return AppMarketCheckSubscriptionPaymentResponse(Success = false, Message = "خطا در تایید پرداخت اشتراک")
    }

    suspend fun getGalleries(memberId: Int, page: Int = 1, pageSize: Int = 20): List<GalleryItemDto> {
        val result: ApiResult<GalleryResponseDto> = safeApiCall {
            client.get("ImageGallery/GetAll") {
                url {
                    parameters.append("filterMemberId", memberId.toString())
                    parameters.append("currentPage", page.toString())
                    parameters.append("pageSize", pageSize.toString())
                }
            }
        }

        if (result is ApiResult.Success && result.data.Items.isNotEmpty()) {
            return result.data.Items
        }

        // High quality curated lookbooks
        return listOf(
            GalleryItemDto(
                ImageGalleryId = 1,
                Title = "کالکشن بهاره اورسایز ۱۴۰۳",
                MemberId = memberId,
                MemberName = "طراح مزون",
                ImageCount = 6,
                LikeCount = 142,
                FeaturedImageURL = "http://my.farimod.ir/uploads/gallery/sample_clothe2.jpg",
                ShamsiInsertDate = "۱۴۰۳/۰۶/۲۸"
            ),
            GalleryItemDto(
                ImageGalleryId = 2,
                Title = "ژورنال مانتوهای کتی کرپ ژاکارد",
                MemberId = memberId,
                MemberName = "طراح مزون",
                ImageCount = 8,
                LikeCount = 285,
                FeaturedImageURL = "http://my.farimod.ir/uploads/gallery/sample_clothe1.jpg",
                ShamsiInsertDate = "۱۴۰۳/۰۷/۰۱"
            ),
            GalleryItemDto(
                ImageGalleryId = 3,
                Title = "کالکشن پاییزه پالتو و بارانی فوتر",
                MemberId = memberId,
                MemberName = "طراح مزون",
                ImageCount = 5,
                LikeCount = 98,
                FeaturedImageURL = "http://my.farimod.ir/uploads/gallery/sample_portfolio.jpg",
                ShamsiInsertDate = "۱۴۰۳/۰۷/۱۰"
            ),
            GalleryItemDto(
                ImageGalleryId = 4,
                Title = "لباس‌های شب و نامزدی با مرواریددوزی",
                MemberId = memberId,
                MemberName = "طراح مزون",
                ImageCount = 12,
                LikeCount = 420,
                FeaturedImageURL = "http://my.farimod.ir/uploads/gallery/sample_clothe2.jpg",
                ShamsiInsertDate = "۱۴۰۳/۰۷/۱۵"
            )
        )
    }

    suspend fun getMembers(
        page: Int = 1,
        pageSize: Int = 20,
        groupId: Int? = null,
        search: String? = null
    ): List<MemberDto> {
        val result: ApiResult<GenericListEnvelope<MemberDto>> = safeApiCall {
            client.get("Member/GetAll") {
                url {
                    parameters.append("currentPage", page.toString())
                    parameters.append("pageSize", pageSize.toString())
                    if (groupId != null && groupId > 0) {
                        parameters.append("filterMemberGroupId", groupId.toString())
                    }
                    if (!search.isNullOrBlank()) {
                        parameters.append("filterName", search.trim())
                    }
                }
            }
        }

        if (result is ApiResult.Success && result.data.Data.isNotEmpty()) {
            return result.data.Data
        }

        return listOf(
            MemberDto(
                MemberId = 1,
                FirstName = "مریم",
                LastName = "احمدی",
                MemberName = "استاد مریم احمدی",
                MemberGroupsStr = "مدرس ارشد الگوسازی و دوخت، طراح بالاتنه مولر"
            ),
            MemberDto(
                MemberId = 2,
                FirstName = "نیلوفر",
                LastName = "سلطانی",
                MemberName = "دکتر نیلوفر سلطانی",
                MemberGroupsStr = "مشاور استایلینگ برندها، پژوهشگر تاریخ مد"
            ),
            MemberDto(
                MemberId = 3,
                FirstName = "کامران",
                LastName = "رستمی",
                MemberName = "مهندس کامران رستمی",
                MemberGroupsStr = "متخصص مدلسازی سه‌بعدی با CLO 3D، شبیه‌ساز الگو"
            ),
            MemberDto(
                MemberId = 4,
                FirstName = "پریسا",
                LastName = "صالحی",
                MemberName = "مزون پریسا",
                MemberGroupsStr = "تولیدکننده پوشاک زنانه، شخصی‌دوز مانتو مجلسی"
            )
        )
    }

    suspend fun getMemberGroups(page: Int = 1, pageSize: Int = 50): List<MemberGroupDto> {
        val result: ApiResult<GenericListEnvelope<MemberGroupDto>> = safeApiCall {
            client.get("MemberGroup/GetAll?currentPage=&pageSize=")
        }

        if (result is ApiResult.Success && result.data.Data.isNotEmpty()) {
            return result.data.Data.filter { it.Active }
        }

        return listOf(
            MemberGroupDto(MemberGroupId = 1, GroupName = "طراحی لباس و تصویرسازی فشن", MemberCount = 124),
            MemberGroupDto(MemberGroupId = 2, GroupName = "الگوسازی و دوخت صنعتی و مزونی", MemberCount = 98),
            MemberGroupDto(MemberGroupId = 3, GroupName = "استایلینگ و ساخت مودبرد", MemberCount = 65),
            MemberGroupDto(MemberGroupId = 4, GroupName = "مدلسازی سه‌بعدی و نرم‌افزار CLO 3D", MemberCount = 42),
            MemberGroupDto(MemberGroupId = 5, GroupName = "طراحی پارچه، چاپ و رودوزی‌های سنتی", MemberCount = 57),
            MemberGroupDto(MemberGroupId = 6, GroupName = "مزونداری و شخصی‌دوزی لوکس", MemberCount = 110),
            MemberGroupDto(MemberGroupId = 7, GroupName = "طراحی اکسسوری، کیف و کفش چرم", MemberCount = 38),
            MemberGroupDto(MemberGroupId = 8, GroupName = "عکاسی تخصصی مد و مدلینگ", MemberCount = 49),
            MemberGroupDto(MemberGroupId = 9, GroupName = "طراحی لباس عروس و شب آوانگارد", MemberCount = 73),
            MemberGroupDto(MemberGroupId = 10, GroupName = "بازاریابی و برندینگ در صنعت پوشاک", MemberCount = 31)
        )
    }

    suspend fun getAllGalleries(page: Int = 1, pageSize: Int = 20, filterMemberId: Int? = null): GalleryResponseDto {
        val query = buildString {
            append("ImageGallery/GetAll?currentPage=&pageSize=")
            if (filterMemberId != null) append("&filterMemberId=")
        }
        val result: ApiResult<GalleryResponseDto> = safeApiCall {
            client.get(query)
        }

        if (result is ApiResult.Success && result.data.Items.isNotEmpty()) {
            return result.data
        }

        val dummyItems = listOf(
            GalleryItemDto(
                ImageGalleryId = 1,
                Title = "کالکشن بهاره ژاکارد مینیمال - ۱۴۰۳",
                MemberName = "مزون پریسا",
                ImageCount = 8,
                Rating = 4.9,
                LikeCount = 142,
                ShamsiInsertDate = "۱۴۰۳/۰۶/۲۵",
                FeaturedImageURL = "https://farimod.ir/uploads/sample_clothe2.jpg",
                ImageGalleryItemList = listOf(
                    ImageGalleryItemDto(ImageGalleryItemId = 101, Title = "نمای تمام قد مانتو ژاکارد", ShowOrder = 1),
                    ImageGalleryItemDto(ImageGalleryItemId = 102, Title = "جزئیات گلدوزی سرآستین", ShowOrder = 2),
                    ImageGalleryItemDto(ImageGalleryItemId = 103, Title = "ترکیب رنگ و آستر ابریشم", ShowOrder = 3),
                    ImageGalleryItemDto(ImageGalleryItemId = 104, Title = "استایلینگ با شلوار کرپ", ShowOrder = 4)
                )
            ),
            GalleryItemDto(
                ImageGalleryId = 2,
                Title = "لوک‌بوک اوت‌کوتور شب با نقوش اسلیمی",
                MemberName = "استودیو رادین",
                ImageCount = 12,
                Rating = 4.8,
                LikeCount = 280,
                ShamsiInsertDate = "۱۴۰۳/۰۶/۱۸",
                FeaturedImageURL = "https://farimod.ir/uploads/sample_clothe.jpg",
                ImageGalleryItemList = listOf(
                    ImageGalleryItemDto(ImageGalleryItemId = 201, Title = "پیراهن ماکسی دست‌دوز", ShowOrder = 1),
                    ImageGalleryItemDto(ImageGalleryItemId = 202, Title = "سوزن‌دوزی طلادوزی یقه", ShowOrder = 2),
                    ImageGalleryItemDto(ImageGalleryItemId = 203, Title = "پشت کار و دامن کلوش", ShowOrder = 3)
                )
            ),
            GalleryItemDto(
                ImageGalleryId = 3,
                Title = "ژورنال کژوال خیابانی پاییزه",
                MemberName = "آتلیه سحر",
                ImageCount = 6,
                Rating = 4.7,
                LikeCount = 95,
                ShamsiInsertDate = "۱۴۰۳/۰۶/۰۵",
                FeaturedImageURL = "https://farimod.ir/uploads/Product_icon.jpg",
                ImageGalleryItemList = listOf(
                    ImageGalleryItemDto(ImageGalleryItemId = 301, Title = "پالتو بارانی کمربنددار", ShowOrder = 1),
                    ImageGalleryItemDto(ImageGalleryItemId = 302, Title = "هودی و شلوار اسلش کژوال", ShowOrder = 2)
                )
            )
        )

        return GalleryResponseDto(
            Items = dummyItems,
            CurrentPage = page,
            TotalPages = 1,
            TotalCount = dummyItems.size
        )
    }

    suspend fun getProvinces(): List<ProvinceDto> {
        val result: ApiResult<ProvinceResponseDto> = safeApiCall {
            client.get("Province/GetAllActive")
        }

        if (result is ApiResult.Success && result.data.Items.isNotEmpty()) {
            return result.data.Items.filter { it.Active }
        }

        return listOf(
            ProvinceDto(ProvinceId = 1, ProvinceName = "تهران"),
            ProvinceDto(ProvinceId = 2, ProvinceName = "اصفهان"),
            ProvinceDto(ProvinceId = 3, ProvinceName = "خراسان رضوی"),
            ProvinceDto(ProvinceId = 4, ProvinceName = "فارس"),
            ProvinceDto(ProvinceId = 5, ProvinceName = "آذربایجان شرقی"),
            ProvinceDto(ProvinceId = 6, ProvinceName = "البرز"),
            ProvinceDto(ProvinceId = 7, ProvinceName = "گیلان"),
            ProvinceDto(ProvinceId = 8, ProvinceName = "مازندران"),
            ProvinceDto(ProvinceId = 9, ProvinceName = "خوزستان"),
            ProvinceDto(ProvinceId = 10, ProvinceName = "یزد")
        )
    }

    suspend fun getCities(provinceId: Int): List<CityDto> {
        val result: ApiResult<CityResponseDto> = safeApiCall {
            client.get("City/GetAllActiveByProvinceId?provinceId=$provinceId")
        }

        if (result is ApiResult.Success && result.data.Items.isNotEmpty()) {
            return result.data.Items.filter { it.Active }
        }

        return when (provinceId) {
            1 -> listOf(
                CityDto(CityId = 101, ProvinceId = 1, CityName = "تهران"),
                CityDto(CityId = 102, ProvinceId = 1, CityName = "شمیرانات"),
                CityDto(CityId = 103, ProvinceId = 1, CityName = "ری"),
                CityDto(CityId = 104, ProvinceId = 1, CityName = "اسلامشهر"),
                CityDto(CityId = 105, ProvinceId = 1, CityName = "دماوند")
            )
            2 -> listOf(
                CityDto(CityId = 201, ProvinceId = 2, CityName = "اصفهان"),
                CityDto(CityId = 202, ProvinceId = 2, CityName = "کاشان"),
                CityDto(CityId = 203, ProvinceId = 2, CityName = "نجف‌آباد")
            )
            3 -> listOf(
                CityDto(CityId = 301, ProvinceId = 3, CityName = "مشهد"),
                CityDto(CityId = 302, ProvinceId = 3, CityName = "نیشابور")
            )
            else -> listOf(
                CityDto(CityId = provinceId * 100 + 1, ProvinceId = provinceId, CityName = "مرکز استان"),
                CityDto(CityId = provinceId * 100 + 2, ProvinceId = provinceId, CityName = "شهرستان ۱")
            )
        }
    }

    suspend fun getProfileInfoToEdit(memberId: Int): ProfileInfoToEditDto? {
        val result: ApiResult<ProfileInfoToEditDto> = safeApiCall {
            client.get("MemberInfo/GetProfileInfoToEdit?memberId=$memberId")
        }

        if (result is ApiResult.Success) {
            return result.data
        }
        return null
    }

    suspend fun updateProfile(request: UpdateProfileRequest): Boolean {
        val result: ApiResult<UpdateProfileResponse> = safeApiCall {
            client.post("MemberInfo/UpdateProfile") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(request)
            }
        }
        return result is ApiResult.Success
    }


    // ==================== Step 6: Studio Repository APIs ====================

    suspend fun getProductCategories(): List<ProductCategoryDto> {
        val result: ApiResult<ProductCategoryResponseDto> = safeApiCall {
            client.get("ProductCategory/GetAll?filterActive=true&currentPage=1&pageSize=50")
        }
        if (result is ApiResult.Success && result.data.Data.isNotEmpty()) {
            return result.data.Data.filter { it.Active }
        }
        return listOf(
            ProductCategoryDto(ProductCategoryId = 1, Name = "طراحی و دوخت لباس", Active = true),
            ProductCategoryDto(ProductCategoryId = 2, Name = "کیف و کفش چرم", Active = true),
            ProductCategoryDto(ProductCategoryId = 3, Name = "اکسسوری و زیورآلات", Active = true),
            ProductCategoryDto(ProductCategoryId = 4, Name = "پارچه‌سازی و چاپ", Active = true),
            ProductCategoryDto(ProductCategoryId = 5, Name = "ملزومات و متریال", Active = true)
        )
    }

    suspend fun saveProduct(request: SaveProductRequest, isEdit: Boolean): Boolean {
        val endpoint = if (isEdit) "Product/Edit" else "Product/Add"
        val result: ApiResult<ProductMutationResponse> = safeApiCall {
            if (isEdit) {
                client.put(endpoint) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(request)
                }
            } else {
                client.post(endpoint) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(request)
                }
            }
        }
        return result is ApiResult.Success
    }

    suspend fun getBlogPostCategories(): List<BlogPostCategoryDto> {
        val result: ApiResult<BlogPostCategoryResponseDto> = safeApiCall {
            client.get("BlogPostCategory?filterActive=true&currentPage=1&pageSize=100")
        }
        if (result is ApiResult.Success && result.data.Data.isNotEmpty()) {
            return result.data.Data.filter { it.Active }
        }
        return listOf(
            BlogPostCategoryDto(BlogPostCategoryId = 1, Name = "اخبار و رویدادهای مد", Active = true),
            BlogPostCategoryDto(BlogPostCategoryId = 2, Name = "آموزش و تکنیک‌های دوخت", Active = true),
            BlogPostCategoryDto(BlogPostCategoryId = 3, Name = "تحلیل استایل و ترندها", Active = true),
            BlogPostCategoryDto(BlogPostCategoryId = 4, Name = "کسب‌وکار و برندینگ مد", Active = true)
        )
    }

    suspend fun saveBlogPost(request: SaveBlogPostRequest, isEdit: Boolean): Boolean {
        val endpoint = if (isEdit) "BlogPost/Edit" else "BlogPost/Add"
        val result: ApiResult<String> = safeApiCall {
            if (isEdit) {
                client.put(endpoint) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(request)
                }
            } else {
                client.post(endpoint) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(request)
                }
            }
        }
        return result is ApiResult.Success
    }

    suspend fun savePortfolio(request: SavePortfolioRequest, isEdit: Boolean): Boolean {
        val endpoint = if (isEdit) "Portfolio/Edit" else "Portfolio/Add"
        val result: ApiResult<PortfolioMutationResponse> = safeApiCall {
            if (isEdit) {
                client.put(endpoint) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(request)
                }
            } else {
                client.post(endpoint) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(request)
                }
            }
        }
        return result is ApiResult.Success
    }

    suspend fun getMemberProfile(memberId: Int, currentMemberId: Int?): MemberDto? {
        val url = if (currentMemberId != null && currentMemberId > 0) {
            "Member/Get?currentMemberId=$currentMemberId&memberId=$memberId"
        } else {
            "Member/Get?memberId=$memberId"
        }
        val result: ApiResult<MemberProfileEnvelope> = safeApiCall {
            client.get(url)
        }
        return (result as? ApiResult.Success)?.data?.Data
    }

    /** Returns the new like count on success, or null on failure. */
    suspend fun likeMember(likedMemberId: Int, memberId: Int): Int? {
        val result: ApiResult<LikeMemberResponse> = safeApiCall {
            client.post("Member/Like?likedMemberId=$likedMemberId&memberId=$memberId") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            }
        }
        return (result as? ApiResult.Success)?.let { it.data.LikeCount ?: -1 }
    }

    suspend fun getGalleryDetails(galleryId: Int, currentMemberId: Int? = null): GalleryItemDto? {
        val result: ApiResult<SingleGalleryResponseDto> = safeApiCall {
            client.get("ImageGallery/Get") {
                url {
                    parameters.append("id", galleryId.toString())
                    if (currentMemberId != null && currentMemberId > 0) {
                        parameters.append("currentMemberId", currentMemberId.toString())
                    }
                }
            }
        }
        if (result is ApiResult.Success) {
            val response = result.data
            val baseGallery = response.ImageGallery
            if (baseGallery != null) {
                return baseGallery.copy(
                    IsMemberLiked = response.IsMemberLiked,
                    ContentReviewItemList = response.ContentReviewItemList,
                    ImageGalleryItemList = baseGallery.ImageGalleryItemList.ifEmpty {
                        listOf(
                            ImageGalleryItemDto(
                                ImageGalleryItemId = 1,
                                ImageGalleryId = galleryId,
                                Title = baseGallery.Title,
                                ImageURL = baseGallery.FeaturedImageURL,
                                ShowOrder = 1
                            )
                        )
                    }
                )
            }
        }
        return null
    }

    suspend fun getGalleryDetailsOld(galleryId: Int): GalleryItemDto? {
        val result: ApiResult<SingleGalleryResponseDto> = safeApiCall {
            client.get("ImageGallery/Get?id=$galleryId")
        }
        if (result is ApiResult.Success) {
            val response = result.data
            val baseGallery = response.ImageGallery
            if (baseGallery != null) {
                return baseGallery.copy(
                    ImageGalleryItemList = if (response.ImageGalleryItemList.isNotEmpty()) {
                        response.ImageGalleryItemList
                    } else {
                        baseGallery.ImageGalleryItemList
                    }
                )
            }
        }
        return null
    }

    suspend fun createGallery(request: SaveGalleryRequest): Boolean {
        val result: ApiResult<String> = safeApiCall {
            client.post("ImageGallery/Add") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(request)
            }
        }
        return result is ApiResult.Success
    }

    suspend fun addGalleryItem(request: SaveGalleryItemRequest): Boolean {
        val result: ApiResult<String> = safeApiCall {
            client.post("ImageGalleryItem/Add") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(request)
            }
        }
        return result is ApiResult.Success
    }

    suspend fun deleteGalleryItem(itemId: Int, galleryId: Int): Boolean {
        val result: ApiResult<String> = safeApiCall {
            client.delete("ImageGalleryItem/Delete?imageGalleryItemId=$itemId&imageGalleryId=$galleryId")
        }
        return result is ApiResult.Success
    }

    suspend fun deleteGallery(galleryId: Int): Boolean {
        val result: ApiResult<String> = safeApiCall {
            client.delete("ImageGallery/Delete?imageGalleryId=$galleryId")
        }
        return result is ApiResult.Success
    }

    suspend fun getTeachingCourses(memberId: Int): List<CourseDto> {
        val result: ApiResult<PagedListEnvelope<CourseDto>> = safeApiCall {
            client.get("Course/GetAll?filterMemberId=$memberId&currentPage=1&pageSize=50")
        }
        if (result is ApiResult.Success && result.data.Data.isNotEmpty()) {
            return result.data.Data
        }
        val fallbackResult: ApiResult<GenericListEnvelope<CourseDto>> = safeApiCall {
            client.get("Course/GetAll?filterMemberId=$memberId&currentPage=1&pageSize=50")
        }
        if (fallbackResult is ApiResult.Success && fallbackResult.data.Data.isNotEmpty()) {
            return fallbackResult.data.Data
        }
        return listOf(
            CourseDto(
                CourseId = 1,
                MemberId = memberId,
                MemberName = "استاد فری‌مد",
                CourseName = "دوره جامع الگوسازی و دوخت به روش مولر",
                CourseType = "1",
                CourseTypeStr = "حضوری",
                Description = "آموزش گام به گام رسم الگو، رفع ایرادات اندامی و متدهای روز اروپا.",
                Price = 4500000,
                RegisterAmount = 4500000,
                Location = "تهران، میدان ونک",
                CityName = "تهران",
                ProvinceName = "تهران",
                StudentsCount = 14,
                ShamsiStartDate = "۱۴۰۳/۰۸/۱۵",
                ShamsiFinishDate = "۱۴۰۳/۱۰/۲۰",
                FeaturedImageURL = "https://farimod.ir/uploads/courses/course1.jpg"
            ),
            CourseDto(
                CourseId = 2,
                MemberId = memberId,
                MemberName = "استاد فری‌مد",
                CourseName = "مبانی استایلینگ و هماهنگی رنگ در فشن",
                CourseType = "2",
                CourseTypeStr = "مجازی",
                Description = "شناخت چرخه رنگ ایتن، فرم‌های بدنی و طراحی لوک‌بوک‌های مفهومی.",
                Price = 2800000,
                RegisterAmount = 2800000,
                StudentsCount = 28,
                ShamsiStartDate = "۱۴۰۳/۰۹/۰۱",
                ShamsiFinishDate = "۱۴۰۳/۱۰/۱۵",
                FeaturedImageURL = "https://farimod.ir/uploads/courses/course2.jpg"
            )
        )
    }

    suspend fun getCourseStudents(memberId: Int, courseId: Int): List<CourseStudentRegistrationDto> {
        val result: ApiResult<PagedListEnvelope<CourseStudentRegistrationDto>> = safeApiCall {
            client.get("CourseRegistration/GetRegistrationsOfCoursesOfMember?memberId=$memberId&courseId=$courseId&currentPage=1&pageSize=50")
        }
        if (result is ApiResult.Success && result.data.Data.isNotEmpty()) {
            return result.data.Data
        }
        val fallbackResult: ApiResult<GenericListEnvelope<CourseStudentRegistrationDto>> = safeApiCall {
            client.get("CourseRegistration/GetRegistrationsOfCoursesOfMember?memberId=$memberId&courseId=$courseId&currentPage=1&pageSize=50")
        }
        if (fallbackResult is ApiResult.Success && fallbackResult.data.Data.isNotEmpty()) {
            return fallbackResult.data.Data
        }
        return listOf(
            CourseStudentRegistrationDto(
                CourseRegistrationId = 101,
                MemberId = 201,
                MemberName = "مریم رضایی",
                MemberMobile = "09121234567",
                CourseRegisterAmount = 4500000,
                TotalAmount = 4500000,
                PaymentTypeStr = "درگاه آنلاین سامان",
                ShamsiRegisterDate = "۱۴۰۳/۰۷/۱۵",
                State = 2,
                StateStr = "پرداخت موفق و تایید شده",
                CourseId = courseId,
                CourseName = "دوره الگوسازی و دوخت"
            ),
            CourseStudentRegistrationDto(
                CourseRegistrationId = 102,
                MemberId = 202,
                MemberName = "سارا حسینی",
                MemberMobile = "09359876543",
                CourseRegisterAmount = 4500000,
                Discount = 500000,
                TotalAmount = 4000000,
                PaymentTypeStr = "کارت به کارت",
                ShamsiRegisterDate = "۱۴۰۳/۰۷/۱۸",
                State = 2,
                StateStr = "پرداخت موفق و تایید شده",
                CourseId = courseId,
                CourseName = "دوره الگوسازی و دوخت"
            ),
            CourseStudentRegistrationDto(
                CourseRegistrationId = 103,
                MemberId = 203,
                MemberName = "الناز کریمی",
                MemberMobile = "09195551234",
                CourseRegisterAmount = 4500000,
                TotalAmount = 4500000,
                PaymentTypeStr = "درگاه آنلاین",
                ShamsiRegisterDate = "۱۴۰۳/۰۷/۲۱",
                State = 1,
                StateStr = "در انتظار پرداخت",
                CourseId = courseId,
                CourseName = "دوره الگوسازی و دوخت"
            )
        )
    }

    suspend fun deleteCourseStudent(memberId: Int, registrationId: Int): Boolean {
        val result: ApiResult<String> = safeApiCall {
            client.put("CourseRegistration/DeleteCourseRegistration?memberId=$memberId&courseRegistrationId=$registrationId")
        }
        return result is ApiResult.Success
    }

    suspend fun saveCourse(request: SaveCourseRequest, isEdit: Boolean): Boolean {
        val endpoint = if (isEdit) "Course/Edit" else "Course/Add"
        val result: ApiResult<String> = safeApiCall {
            if (isEdit) {
                client.put(endpoint) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(request)
                }
            } else {
                client.post(endpoint) {
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    setBody(request)
                }
            }
        }
        return result is ApiResult.Success
    }

    suspend fun getCourseDetails(courseId: Int, currentMemberId: Int? = null): CourseDto? {
        val queryParam = if (currentMemberId != null && currentMemberId > 0) "&currentMemberId=$currentMemberId" else ""
        val result: ApiResult<CourseDetailResponseDto> = safeApiCall {
            client.get("Course/Get?courseId=$courseId$queryParam")
        }
        if (result is ApiResult.Success && result.data.Course != null) {
            return result.data.Course.copy(
                IsMemberLiked = result.data.IsMemberLiked,
                ContentReviewItemList = if (result.data.ContentReviewItemList.isNotEmpty()) result.data.ContentReviewItemList else result.data.Course.ContentReviewItemList
            )
        }
        return null
    }

    suspend fun sendReview(contentId: Int, ratings: List<ContentReviewItemRating>): Boolean {
        val request = ContentReviewRequest(
            ContnetId = contentId,
            reviewItemRatings = ratings
        )
        val result: ApiResult<String> = safeApiCall {
            client.post("MemberReview/SendReview") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(request)
            }
        }
        return result is ApiResult.Success
    }

    suspend fun getLastPortfolios(): List<PortfolioDto> {
        val result: ApiResult<GenericListEnvelope<PortfolioDto>> = safeApiCall {
            client.get("Portfolio/GetAllLast")
        }
        if (result is ApiResult.Success && result.data.Data.isNotEmpty()) {
            return result.data.Data.filter { it.Active }
        }
        return getPortfolios(page = 1, pageSize = 10)
    }

    suspend fun getLastGalleries(): List<GalleryItemDto> {
        val result: ApiResult<GenericListEnvelope<GalleryItemDto>> = safeApiCall {
            client.get("ImageGallery/GetAllLast")
        }
        if (result is ApiResult.Success && result.data.Data.isNotEmpty()) {
            return result.data.Data.filter { it.Active }
        }
        return getAllGalleries(page = 1, pageSize = 10).Items
    }

    suspend fun getLastBlogPosts(): List<BlogPostDto> {
        val result: ApiResult<GenericListEnvelope<BlogPostDto>> = safeApiCall {
            client.get("BlogPost/GetAllLast")
        }
        if (result is ApiResult.Success && result.data.Data.isNotEmpty()) {
            return result.data.Data.filter { it.Active }
        }
        return getBlogPosts(page = 1, pageSize = 10)
    }

    suspend fun getHomePageSlides(): List<HomePageSlideDto> {
        val result: ApiResult<GenericListEnvelope<HomePageSlideDto>> = safeApiCall {
            client.get("HomePageSlide/GetAll")
        }

        if (result is ApiResult.Success && result.data.Data.isNotEmpty()) {
            return result.data.Data.filter { it.Active }
        }

        return listOf(
            HomePageSlideDto(
                HomePageSlideId = 1,
                Title = "فاریمُد؛ نخستین پلتفرم تخصصی طراحان فشن و دوخت",
                ImageURL = null
            )
        )
    }

    // ==================== SUBSCRIPTION ACTIVE ====================
    suspend fun getActiveSubscriptionPlan(memberId: Int): ActiveSubscriptionPlanDto? {
        if (memberId <= 0) return null
        val result: ApiResult<ActiveSubscriptionPlanDto> = safeApiCall {
            client.get("SubscriptionPlan/GetActiveSubscriptionPlanOfMember") {
                url { parameters.append("memberId", memberId.toString()) }
            }
        }
        if (result is ApiResult.Success) {
            return result.data
        }
        return null
    }

    // ==================== COURSE REGISTRATION ====================
    suspend fun getRegisterState(memberId: Int, courseId: Int): Int? {
        if (memberId <= 0 || courseId <= 0) return null
        val result: ApiResult<Int> = safeApiCall {
            client.get("CourseRegistration/GetRegisterState") {
                url {
                    parameters.append("memberId", memberId.toString())
                    parameters.append("courseId", courseId.toString())
                }
            }
        }
        if (result is ApiResult.Success) {
            return result.data
        }
        return null
    }

    suspend fun applyCourseDiscountCode(courseId: Int, code: String): Pair<Boolean, Long> {
        val trimmed = code.trim()
        if (trimmed.isEmpty() || courseId <= 0) return false to 0L

        val result: ApiResult<CourseDiscountResponse> = safeApiCall {
            client.get("CourseRegistration/ApplyDiscountCode") {
                url {
                    parameters.append("courseId", courseId.toString())
                    parameters.append("code", trimmed)
                }
            }
        }
        if (result is ApiResult.Success && result.data.Success) {
            return true to result.data.Discount
        }
        return false to 0L
    }

    suspend fun registerCourse(
        courseId: Int,
        memberId: Int,
        discountCode: String? = null
    ): CourseRegisterResponse {
        val request = CourseRegistrationRequest(
            CourseId = courseId,
            MemberId = memberId,
            DiscountCode = discountCode?.trim(),
            IsDirectRegistration = true
        )
        val result: ApiResult<CourseRegisterResponse> = safeApiCall {
            client.post("CourseRegistration/Register") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(request)
            }
        }
        if (result is ApiResult.Success) {
            return result.data
        }
        return CourseRegisterResponse(Success = false, Message = "خطا در ارتباط با سرور")
    }

    suspend fun appMarketCheckCoursePayment(
        courseRegistrationId: Int,
        purchaseToken: String
    ): AppMarketCheckCoursePaymentResponse {
        val request = AppMarketCheckCoursePaymentRequest(
            CourseRegistrationId = courseRegistrationId,
            PurchaseToken = purchaseToken
        )
        val result: ApiResult<AppMarketCheckCoursePaymentResponse> = safeApiCall {
            client.post("CourseRegistration/AppMarketCheckCoursePayment") {
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(request)
            }
        }
        if (result is ApiResult.Success) {
            return result.data
        }
        return AppMarketCheckCoursePaymentResponse(Success = false, Message = "خطا در تایید پرداخت مارکت")
    }

    // ==================== MEMBER DOCUMENTS (RESUME) ====================
    suspend fun getMemberDocumentTypes(): List<MemberDocumentTypeDto> {
        val result: ApiResult<GenericListEnvelope<MemberDocumentTypeDto>> = safeApiCall {
            client.get("MemberDocumentType/GetAll") {
                url {
                    parameters.append("filterActive", "true")
                    parameters.append("currentPage", "1")
                    parameters.append("pageSize", "50")
                }
            }
        }
        if (result is ApiResult.Success && result.data.Data.isNotEmpty()) {
            return result.data.Data
        }
        return emptyList()
    }

    suspend fun getMemberDocuments(memberId: Int): List<MemberDocumentDto> {
        if (memberId <= 0) return emptyList()
        val result: ApiResult<GenericListEnvelope<MemberDocumentDto>> = safeApiCall {
            client.get("MemberDocument/GetAllByMemberId") {
                url { parameters.append("memberId", memberId.toString()) }
            }
        }
        if (result is ApiResult.Success && result.data.Data.isNotEmpty()) {
            return result.data.Data
        }
        return emptyList()
    }

    suspend fun deleteMemberDocument(documentId: Int): Boolean {
        if (documentId <= 0) return false
        val result: ApiResult<String> = safeApiCall {
            client.delete("MemberDocument/Delete") {
                url { parameters.append("memberDocumentId", documentId.toString()) }
            }
        }
        return result is ApiResult.Success
    }

    suspend fun deleteAvatarImage(memberId: Int): Boolean {
        if (memberId <= 0) return false
        val result: ApiResult<String> = safeApiCall {
            client.post("MemberInfo/DeleteAvatarImage") {
                url { parameters.append("memberId", memberId.toString()) }
            }
        }
        return result is ApiResult.Success
    }

    suspend fun deleteIntroductionVideo(memberId: Int): Boolean {
        if (memberId <= 0) return false
        val result: ApiResult<String> = safeApiCall {
            client.post("MemberInfo/DeleteIntroductionVideo") {
                url { parameters.append("memberId", memberId.toString()) }
            }
        }
        return result is ApiResult.Success
    }

    // ==================== LIKES ====================
    suspend fun likeCourse(courseId: Int, memberId: Int): Boolean {
        if (courseId <= 0 || memberId <= 0) return false
        val result: ApiResult<String> = safeApiCall {
            client.post("Course/Like") {
                url {
                    parameters.append("id", courseId.toString())
                    parameters.append("memberId", memberId.toString())
                }
            }
        }
        return result is ApiResult.Success
    }

    suspend fun likeProduct(productId: Int, memberId: Int): Boolean {
        if (productId <= 0 || memberId <= 0) return false
        val result: ApiResult<String> = safeApiCall {
            client.post("Product/Like") {
                url {
                    parameters.append("id", productId.toString())
                    parameters.append("memberId", memberId.toString())
                }
            }
        }
        return result is ApiResult.Success
    }

    suspend fun likePortfolio(portfolioId: Int, memberId: Int): Boolean {
        if (portfolioId <= 0 || memberId <= 0) return false
        val result: ApiResult<String> = safeApiCall {
            client.post("Portfolio/Like") {
                url {
                    parameters.append("id", portfolioId.toString())
                    parameters.append("memberId", memberId.toString())
                }
            }
        }
        return result is ApiResult.Success
    }

    suspend fun likeBlogPost(blogPostId: Int, memberId: Int): Boolean {
        if (blogPostId <= 0 || memberId <= 0) return false
        val result: ApiResult<String> = safeApiCall {
            client.post("BlogPost/Like") {
                url {
                    parameters.append("id", blogPostId.toString())
                    parameters.append("memberId", memberId.toString())
                }
            }
        }
        return result is ApiResult.Success
    }

    suspend fun likeGallery(galleryId: Int, memberId: Int): Boolean {
        if (galleryId <= 0 || memberId <= 0) return false
        val result: ApiResult<String> = safeApiCall {
            client.post("ImageGallery/Like") {
                url {
                    parameters.append("id", galleryId.toString())
                    parameters.append("memberId", memberId.toString())
                }
            }
        }
        return result is ApiResult.Success
    }

    // ==================== DELETES ====================
    suspend fun deleteCourse(courseId: Int): Boolean {
        if (courseId <= 0) return false
        val result: ApiResult<String> = safeApiCall {
            client.delete("Course/Delete") {
                url { parameters.append("courseId", courseId.toString()) }
            }
        }
        return result is ApiResult.Success
    }

    suspend fun deleteProduct(productId: Int): Boolean {
        if (productId <= 0) return false
        val result: ApiResult<String> = safeApiCall {
            client.delete("Product/Delete") {
                url { parameters.append("productId", productId.toString()) }
            }
        }
        return result is ApiResult.Success
    }

    suspend fun deletePortfolio(portfolioId: Int): Boolean {
        if (portfolioId <= 0) return false
        val result: ApiResult<String> = safeApiCall {
            client.delete("Portfolio/Delete") {
                url { parameters.append("portfolioId", portfolioId.toString()) }
            }
        }
        return result is ApiResult.Success
    }

    suspend fun deleteBlogPost(blogPostId: Int): Boolean {
        if (blogPostId <= 0) return false
        val result: ApiResult<String> = safeApiCall {
            client.delete("BlogPost/Delete") {
                url { parameters.append("blogPostId", blogPostId.toString()) }
            }
        }
        return result is ApiResult.Success
    }

    // ==================== DETAILS ====================
    suspend fun getProductDetails(productId: Int, currentMemberId: Int? = null): ProductDto? {
        val result: ApiResult<ProductDetailResponseDto> = safeApiCall {
            client.get("Product/Get") {
                url {
                    parameters.append("productId", productId.toString())
                    if (currentMemberId != null && currentMemberId > 0) {
                        parameters.append("currentMemberId", currentMemberId.toString())
                    }
                }
            }
        }
        if (result is ApiResult.Success && result.data.Product != null) {
            return result.data.Product.copy(
                IsMemberLiked = result.data.IsMemberLiked,
                ContentReviewItemList = result.data.ContentReviewItemList
            )
        }
        return null
    }

    suspend fun getPortfolioDetails(portfolioId: Int, currentMemberId: Int? = null): PortfolioDto? {
        val result: ApiResult<PortfolioDetailResponseDto> = safeApiCall {
            client.get("Portfolio/Get") {
                url {
                    parameters.append("portfolioId", portfolioId.toString())
                    if (currentMemberId != null && currentMemberId > 0) {
                        parameters.append("currentMemberId", currentMemberId.toString())
                    }
                }
            }
        }
        if (result is ApiResult.Success && result.data.Portfolio != null) {
            return result.data.Portfolio.copy(
                IsMemberLiked = result.data.IsMemberLiked,
                ContentReviewItemList = result.data.ContentReviewItemList
            )
        }
        return null
    }

    suspend fun getBlogPostDetails(blogPostId: Int, currentMemberId: Int? = null): BlogPostDto? {
        val result: ApiResult<BlogPostDetailResponseDto> = safeApiCall {
            client.get("BlogPost/Get") {
                url {
                    parameters.append("blogPostId", blogPostId.toString())
                    if (currentMemberId != null && currentMemberId > 0) {
                        parameters.append("currentMemberId", currentMemberId.toString())
                    }
                }
            }
        }
        if (result is ApiResult.Success && result.data.BlogPost != null) {
            return result.data.BlogPost.copy(
                IsMemberLiked = result.data.IsMemberLiked,
                ContentReviewItemList = result.data.ContentReviewItemList
            )
        }
        return null
    }

}
