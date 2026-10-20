package ir.farimod.app.screens

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import ir.farimod.app.navigation.Tabs
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.network.Api
import ir.farimod.app.network.ApiFilePart
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.jsonOf
import ir.farimod.app.network.list
import ir.farimod.app.network.str
import ir.farimod.app.screens.aboutme.AboutMeBackButton
import ir.farimod.app.screens.aboutme.AboutMeEditableText
import ir.farimod.app.screens.aboutme.AboutMeGroup
import ir.farimod.app.screens.aboutme.AboutMeInfoCard
import ir.farimod.app.screens.aboutme.AboutMeProfile
import ir.farimod.app.screens.aboutme.AboutMeUploadingOverlay
import ir.farimod.app.screens.aboutme.AboutMeValueText
import ir.farimod.app.screens.aboutme.AboutMeVideoDialog
import ir.farimod.app.screens.aboutme.aboutMeUploadMime
import ir.farimod.app.screens.aboutme.toRouteJson
import ir.farimod.app.state.AuthState
import ir.farimod.app.ui.AlertButton
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.ImageItem
import ir.farimod.app.ui.ImageUpload
import ir.farimod.app.ui.MainBackground
import ir.farimod.app.ui.RNAlert
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.ToastData
import ir.farimod.app.ui.Tooltip
import ir.farimod.app.ui.rgba
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.utils.currentTimeMillis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun AboutMeScreen() {
    val navigation = useNavigation()
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    var profile by remember { mutableStateOf(AboutMeProfile()) }
    var isEditing by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var uploadingAvatar by remember { mutableStateOf(false) }
    var uploadingVideo by remember { mutableStateOf(false) }
    var videoModalVisible by remember { mutableStateOf(false) }
    val hasLoaded = remember { mutableStateOf(false) }

    var toast by remember { mutableStateOf(ToastData()) }
    var toastToken by remember { mutableStateOf(0) }
    fun showToast(message: String, type: String = "error") {
        toastToken += 1
        toast = ToastData(visible = true, message = message, type = type)
    }

    LaunchedEffect(error) {
        val message = error
        if (!message.isNullOrEmpty()) showToast(message, "error")
    }

    LaunchedEffect(Unit) {
        val current = AuthState.user ?: return@LaunchedEffect
        val memberId = current.MemberId
        if (memberId == null || memberId == 0 || hasLoaded.value) return@LaunchedEffect
        try {
            error = null
            val response = Api.get(
                "MemberInfo/GetProfileInfoToEdit",
                query = mapOf("memberId" to memberId)
            )
            if (!response.ok) throw Exception("خطای سرور: ${response.status}")
            val result = response.json ?: throw Exception("خطای سرور: ${response.status}")
            val mobile = result.str("Mobile")?.takeIf { it.isNotEmpty() }
                ?: current.Mobile?.takeIf { it.isNotEmpty() }
                ?: ""
            val first = current.FirstName
            val last = current.LastName
            profile = AboutMeProfile(
                aboutMe = result.str("AboutMeText")?.takeIf { it.isNotEmpty() } ?: "",
                avatarUrl = result.str("AvatarImageURL")?.takeIf { it.isNotEmpty() },
                introVideo = result.str("IntroductionVideoURL")?.takeIf { it.isNotEmpty() },
                name = if (!first.isNullOrEmpty() && !last.isNullOrEmpty()) "$first $last" else "کاربر",
                title = "طراح",
                cityId = result.int("CityId"),
                cityName = result.str("CityName")?.takeIf { it.isNotEmpty() } ?: "",
                provinceId = result.int("ProvinceId"),
                provinceName = result.str("ProvinceName")?.takeIf { it.isNotEmpty() } ?: "",
                phone1 = result.str("Phone1")?.takeIf { it.isNotEmpty() } ?: "",
                phone2 = result.str("Phone2")?.takeIf { it.isNotEmpty() } ?: "",
                email = result.str("Email")?.takeIf { it.isNotEmpty() } ?: "",
                websiteAddress = result.str("WebsiteAddress")?.takeIf { it.isNotEmpty() } ?: "",
                telegramAccountId = result.str("TelegramAccountId")?.takeIf { it.isNotEmpty() } ?: "",
                instagramAccountId = result.str("InstagramAccountId")?.takeIf { it.isNotEmpty() } ?: "",
                whatsappAccountMobileNumber = result.str("WhatsappAccountMobileNumber")?.takeIf { it.isNotEmpty() } ?: "",
                address = result.str("Address")?.takeIf { it.isNotEmpty() } ?: "",
                mobile = mobile,
                memberGroupList = result["MemberGroupList"].list.mapNotNull { group ->
                    val id = group.int("MemberGroupId") ?: return@mapNotNull null
                    AboutMeGroup(id, group.str("MemberGroupName") ?: "")
                }
            )
            hasLoaded.value = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message
        }
    }

    suspend fun updateAboutMe(newText: String): Boolean {
        try {
            error = null
            if (profile.mobile.isEmpty()) {
                showToast(
                    "شماره موبایل شما یافت نشد. لطفاً ابتدا از بخش \"ویرایش پروفایل\" شماره موبایل خود را وارد کنید.",
                    "error"
                )
                return false
            }
            val memberGroupIds = profile.memberGroupList.map { it.id }.filter { it != 0 }
            val payload = jsonOf(
                "MemberId" to AuthState.user?.MemberId,
                "AboutMe" to newText.trim(),
                "CityId" to (profile.cityId ?: 0),
                "ProvinceId" to (profile.provinceId ?: 0),
                "Mobile" to profile.mobile,
                "Phone1" to profile.phone1,
                "Phone2" to profile.phone2,
                "Email" to profile.email,
                "WebsiteAddress" to profile.websiteAddress,
                "TelegramAccountId" to profile.telegramAccountId,
                "WhatsappAccountMobileNumber" to profile.whatsappAccountMobileNumber,
                "InstagramAccountId" to profile.instagramAccountId,
                "Address" to profile.address,
                "MemberGroupIdList" to memberGroupIds
            )
            val response = Api.post("MemberInfo/UpdateProfile", body = payload)
            if (!response.ok) {
                throw Exception("خطای سرور در بروزرسانی: ${response.status} - ${response.text}")
            }
            profile = profile.copy(aboutMe = newText.trim())
            return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message
            return false
        }
    }

    suspend fun updateAvatar(image: ImageItem): Boolean {
        try {
            uploadingAvatar = true
            loading = true
            error = null
            showToast("در حال آپلود تصویر...", "info")
            if (image.uri.isEmpty()) throw Exception("فایل تصویر یافت نشد")
            val bytes = image.bytesOrNull() ?: throw Exception("فایل تصویر یافت نشد")
            val fileName = image.name?.takeIf { it.isNotEmpty() } ?: "avatar_${currentTimeMillis()}.jpg"
            val mime = aboutMeUploadMime(image.uri, image.type, image = true)
            val memberId = AuthState.user?.MemberId
            val response = Api.upload(
                path = "MemberInfo/UploadAvatarImage",
                files = listOf(ApiFilePart("avatarImage", fileName, mime, bytes)),
                query = mapOf("memberId" to memberId)
            )
            if (!response.ok) throw Exception("خطای سرور: ${response.status} - ${response.text}")
            val result = response.json ?: throw Exception("خطای سرور: ${response.status} - ${response.text}")
            profile = profile.copy(avatarUrl = result.str("AvatarImageURL")?.takeIf { it.isNotEmpty() } ?: image.uri)
            showToast("تصویر با موفقیت آپلود شد", "success")
            return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showToast("خطا در آپلود تصویر: " + (e.message ?: ""), "error")
            error = e.message
            return false
        } finally {
            uploadingAvatar = false
            loading = false
        }
    }

    suspend fun deleteAvatar(): Boolean {
        try {
            loading = true
            error = null
            val response = Api.post(
                "MemberInfo/DeleteAvatarImage",
                query = mapOf("memberId" to AuthState.user?.MemberId)
            )
            if (!response.ok) throw Exception("خطای سرور: ${response.status}")
            profile = profile.copy(avatarUrl = null)
            return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message
            return false
        } finally {
            loading = false
        }
    }

    suspend fun updateIntroVideo(video: ImageItem): Boolean {
        try {
            uploadingVideo = true
            loading = true
            error = null
            showToast("در حال آپلود ویدیو...", "info")
            if (video.uri.isEmpty()) throw Exception("فایل ویدیو یافت نشد")
            val bytes = video.bytesOrNull() ?: throw Exception("فایل ویدیو یافت نشد")
            val fileName = video.name?.takeIf { it.isNotEmpty() } ?: "intro_${currentTimeMillis()}.mp4"
            val mime = aboutMeUploadMime(video.uri, video.type, image = false)
            val response = Api.upload(
                path = "MemberInfo/UploadIntroductionVideo",
                files = listOf(ApiFilePart("videoFile", fileName, mime, bytes)),
                query = mapOf("memberId" to AuthState.user?.MemberId)
            )
            if (!response.ok) {
                if (response.status == 413) throw Exception("حجم ویدیو بیش از حد مجاز است (۶۰ مگابایت)")
                if (response.status == 415) throw Exception("فرمت ویدیو پشتیبانی نمی‌شود")
                throw Exception("خطای سرور: ${response.status} - ${response.text}")
            }
            val result = response.json ?: throw Exception("خطای سرور: ${response.status} - ${response.text}")
            profile = profile.copy(
                introVideo = result.str("IntroductionVideoURL")?.takeIf { it.isNotEmpty() } ?: video.uri
            )
            showToast("ویدیو با موفقیت آپلود شد", "success")
            return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            showToast("خطا در آپلود ویدیو: " + (e.message ?: ""), "error")
            error = e.message
            return false
        } finally {
            uploadingVideo = false
            loading = false
        }
    }

    suspend fun deleteIntroVideo(): Boolean {
        try {
            loading = true
            error = null
            val response = Api.post(
                "MemberInfo/DeleteIntroductionVideo",
                query = mapOf("memberId" to AuthState.user?.MemberId)
            )
            if (!response.ok) throw Exception("خطای سرور: ${response.status}")
            profile = profile.copy(introVideo = null)
            return true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message
            return false
        } finally {
            loading = false
        }
    }

    fun handleSaveEdit(newValue: String) {
        if (newValue.trim().isEmpty()) {
            RNAlert.alert("خطا", "لطفا متن را وارد کنید")
            return
        }
        scope.launch {
            val success = updateAboutMe(newValue.trim())
            if (success) {
                isEditing = false
                keyboard?.hide()
                showToast("اطلاعات با موفقیت به‌روزرسانی شد", "success")
            } else {
                showToast("خطا در به‌روزرسانی اطلاعات", "error")
            }
        }
    }

    fun handleCancelEdit() {
        isEditing = false
        keyboard?.hide()
    }

    fun handleAvatarChange(image: ImageItem?) {
        scope.launch {
            if (image != null) {
                updateAvatar(image)
            } else {
                val success = deleteAvatar()
                if (success) showToast("تصویر پروفایل حذف شد", "success")
                else showToast("خطا در حذف تصویر", "error")
            }
        }
    }

    fun handleVideoChange(video: ImageItem?) {
        scope.launch {
            if (video != null) {
                updateIntroVideo(video)
            } else {
                val success = deleteIntroVideo()
                if (success) showToast("ویدئوی معرفی حذف شد", "success")
                else showToast("خطا در حذف ویدئو", "error")
            }
        }
    }

    fun openContactEdit() {
        navigation.navigate("EditContactInfo", mapOf("contactData" to profile.toRouteJson()))
    }

    val sbh = statusBarHeight()
    val headerTop = sbh + 35.dp

    Box(Modifier.fillMaxSize().background(Color(0xFFF8FAFC))) {
        MainBackground()
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            Box(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = headerTop, bottom = 30.dp)
                            .absolutePadding(left = 20.dp, right = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AppText(
                            text = "پروفایل من",
                            modifier = Modifier.fillMaxWidth().absolutePadding(left = 56.dp, right = 56.dp),
                            fontSize = 22.sp,
                            fontFamily = RNFonts.Yekan_Bakh_ExtraBold,
                            color = Color(0xFF2C3E50),
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Column(Modifier.fillMaxWidth().absolutePadding(left = 20.dp, right = 20.dp)) {
                        val location = when {
                            profile.provinceName.isNotEmpty() && profile.cityName.isNotEmpty() ->
                                "${profile.provinceName}، ${profile.cityName}"
                            profile.provinceName.isNotEmpty() -> profile.provinceName
                            profile.cityName.isNotEmpty() -> profile.cityName
                            else -> "ثبت موقعیت مکانی و اطلاعات تماس"
                        }
                        AboutMeInfoCard(
                            iconName = "location-on",
                            iconType = "location",
                            label = "ویرایش اطلاعات",
                            showEdit = true,
                            editEnabled = !loading,
                            onEdit = { openContactEdit() }
                        ) {
                            AboutMeValueText(location)
                        }
                        if (profile.address.isNotEmpty()) {
                            AboutMeInfoCard("home", "address", "آدرس", true, !loading, { openContactEdit() }) {
                                AboutMeValueText(profile.address)
                            }
                        }
                        if (profile.phone1.isNotEmpty()) {
                            AboutMeInfoCard("phone", "phone", "موبایل", true, !loading, { openContactEdit() }) {
                                AboutMeValueText(profile.phone1)
                            }
                        }
                        if (profile.phone2.isNotEmpty()) {
                            AboutMeInfoCard("phone", "phone", "تلفن", true, !loading, { openContactEdit() }) {
                                AboutMeValueText(profile.phone2)
                            }
                        }
                        if (profile.email.isNotEmpty()) {
                            AboutMeInfoCard("email", "email", "ایمیل", true, !loading, { openContactEdit() }) {
                                AboutMeValueText(profile.email)
                            }
                        }
                        if (profile.websiteAddress.isNotEmpty()) {
                            AboutMeInfoCard("language", "website", "وبسایت", true, !loading, { openContactEdit() }) {
                                AboutMeValueText(profile.websiteAddress)
                            }
                        }
                        if (profile.telegramAccountId.isNotEmpty()) {
                            AboutMeInfoCard("send", "telegram", "تلگرام", true, !loading, { openContactEdit() }) {
                                AboutMeValueText(profile.telegramAccountId)
                            }
                        }
                        if (profile.instagramAccountId.isNotEmpty()) {
                            AboutMeInfoCard("camera-alt", "instagram", "اینستاگرام", true, !loading, { openContactEdit() }) {
                                AboutMeValueText(profile.instagramAccountId)
                            }
                        }
                        if (profile.whatsappAccountMobileNumber.isNotEmpty()) {
                            AboutMeInfoCard("chat", "whatsapp", "واتساپ", true, !loading, { openContactEdit() }) {
                                AboutMeValueText(profile.whatsappAccountMobileNumber)
                            }
                        }
                        if (profile.memberGroupList.isNotEmpty()) {
                            AboutMeInfoCard("groups", "group", "گروه‌های عضویت", true, !loading, { openContactEdit() }) {
                                profile.memberGroupList.forEachIndexed { index, group ->
                                    key("${group.id}-$index") {
                                        Box(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                            Box(
                                                Modifier
                                                    .align(AbsoluteAlignment.CenterRight)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(rgba(99, 102, 241, 0.1f))
                                                    .border(1.dp, rgba(99, 102, 241, 0.2f), RoundedCornerShape(12.dp))
                                                    .absolutePadding(left = 12.dp, right = 12.dp, top = 8.dp, bottom = 8.dp)
                                            ) {
                                                AppText(
                                                    text = group.name,
                                                    fontSize = 14.sp,
                                                    fontFamily = RNFonts.Yekan_Bakh_Regular,
                                                    color = Color(0xFF6366F1)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        AboutMeInfoCard(
                            iconName = "person",
                            iconType = "person",
                            label = "درباره من",
                            showEdit = !isEditing,
                            editEnabled = !loading,
                            onEdit = { isEditing = true }
                        ) {
                            if (isEditing) {
                                AboutMeEditableText(
                                    value = profile.aboutMe,
                                    onSave = { handleSaveEdit(it) },
                                    onCancel = { handleCancelEdit() },
                                    loading = loading
                                )
                            } else {
                                AboutMeValueText(profile.aboutMe.ifEmpty { "متنی وارد نشده است" })
                            }
                        }
                        AboutMeInfoCard(
                            iconName = "videocam",
                            iconType = "videocam",
                            label = "ویدئوی معرفی",
                            showEdit = false,
                            editEnabled = true,
                            onEdit = {}
                        ) {
                            Box(Modifier.fillMaxWidth()) {
                                key("video-${profile.introVideo ?: "no-video"}") {
                                    ImageUpload(
                                        isMultiple = true,
                                        maxImages = 1,
                                        allowVideos = true,
                                        allowImages = false,
                                        allowCamera = true,
                                        allowGallery = true,
                                        allowEditing = false,
                                        aspectRatio = 16 to 9,
                                        initialImages = profile.introVideo?.let { listOf(ImageItem(id = "1", uri = it)) } ?: emptyList(),
                                        onImagesChange = { videos ->
                                            if (videos.isNotEmpty()) handleVideoChange(videos[0]) else handleVideoChange(null)
                                        },
                                        placeholder = "ویدئوی معرفی",
                                        onShowToast = { message, type -> showToast(message, type) },
                                        marginTop = 5.dp,
                                        marginBottom = 15.dp,
                                        loading = loading,
                                        onPress = {
                                            if (!profile.introVideo.isNullOrEmpty()) videoModalVisible = true
                                        }
                                    )
                                }
                                if (uploadingVideo) {
                                    AboutMeUploadingOverlay("در حال آپلود ویدیو...", Color(0xFF06B6D4))
                                }
                            }
                        }
                        AboutMeInfoCard(
                            iconName = "camera-alt",
                            iconType = "camera",
                            label = "تصویر پروفایل",
                            showEdit = false,
                            editEnabled = true,
                            onEdit = {}
                        ) {
                            Box(Modifier.fillMaxWidth()) {
                                key("avatar-${profile.avatarUrl ?: "no-avatar"}") {
                                    ImageUpload(
                                        isMultiple = false,
                                        allowVideos = false,
                                        allowImages = true,
                                        allowCamera = true,
                                        allowGallery = true,
                                        allowEditing = true,
                                        aspectRatio = 1 to 1,
                                        initialImage = profile.avatarUrl?.let { ImageItem(id = "1", uri = it) },
                                        onImageChange = { change -> handleAvatarChange(change.single) },
                                        placeholder = "تصویر پروفایل",
                                        onShowToast = { message, type -> showToast(message, type) },
                                        marginTop = 5.dp,
                                        marginBottom = 15.dp,
                                        loading = loading
                                    )
                                }
                                if (uploadingAvatar) {
                                    AboutMeUploadingOverlay("در حال آپلود تصویر...", Color(0xFF10B981))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(50.dp))
                }
            }
        }
        AboutMeBackButton(
            onClick = { navigation.goBack() },
            modifier = Modifier
                .align(AbsoluteAlignment.TopRight)
                .absoluteOffset(x = (-20).dp, y = sbh + 33.dp)
                .zIndex(1000f)
        )
        Box(
            Modifier
                .align(AbsoluteAlignment.TopLeft)
                .absoluteOffset(x = 20.dp, y = sbh + 33.dp)
                .zIndex(1000f)
        ) {
            Tooltip("در این بخش می‌توانید تمام اطلاعات پروفایل خود شامل متن درباره من، تصویر، ویدیو و اطلاعات تماس را مشاهده و مدیریت کنید.")
        }
        key(toastToken) {
            Toast(toast.visible, toast.message, toast.type) { toast = toast.copy(visible = false) }
        }
        val intro = profile.introVideo
        if (videoModalVisible && !intro.isNullOrEmpty()) {
            AboutMeVideoDialog(url = intro, onClose = { videoModalVisible = false })
        }
    }
}
