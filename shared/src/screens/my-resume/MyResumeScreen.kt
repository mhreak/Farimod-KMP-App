package ir.farimod.app.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.navigation.Tabs
import ir.farimod.app.navigation.useNavigation
import ir.farimod.app.network.Api
import ir.farimod.app.network.ApiFilePart
import ir.farimod.app.network.get
import ir.farimod.app.network.int
import ir.farimod.app.network.list
import ir.farimod.app.network.str
import ir.farimod.app.platform.canOpenUrl
import ir.farimod.app.platform.openUrl
import ir.farimod.app.platform.rememberMediaPicker
import ir.farimod.app.screens.myresume.ResumeBackButton
import ir.farimod.app.screens.myresume.ResumeDeleteDialog
import ir.farimod.app.screens.myresume.ResumeMessageState
import ir.farimod.app.screens.myresume.ResumeSectionTitle
import ir.farimod.app.screens.myresume.ResumeSkeletonCard
import ir.farimod.app.screens.myresume.ResumeUploadCard
import ir.farimod.app.screens.myresume.ResumeUploadedFile
import ir.farimod.app.screens.myresume.resumeColorForIndex
import ir.farimod.app.screens.myresume.resumeIsoNow
import ir.farimod.app.screens.myresume.resumePullUp
import ir.farimod.app.screens.myresume.resumeShamsiToday
import ir.farimod.app.state.AuthState
import ir.farimod.app.ui.AppText
import ir.farimod.app.ui.MainBackground
import ir.farimod.app.ui.RNFonts
import ir.farimod.app.ui.RowReverse
import ir.farimod.app.ui.Toast
import ir.farimod.app.ui.ToastData
import ir.farimod.app.ui.rnShadow
import ir.farimod.app.ui.statusBarHeight
import ir.farimod.app.ui.touchableOpacity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private data class ResumeDocType(val id: Int, val name: String, val description: String)

private data class ResumePendingDelete(
    val typeId: Int,
    val index: Int,
    val file: ResumeUploadedFile,
    val typeName: String
)

private data class ResumeSnapshot(
    val types: List<ResumeDocType> = emptyList(),
    val files: Map<Int, List<ResumeUploadedFile>> = emptyMap(),
    val error: String? = null
)

private suspend fun fetchExistingResumeFiles(memberId: Int): Map<Int, List<ResumeUploadedFile>> {
    return try {
        val response = Api.get(
            "MemberDocument/GetAllByMemberId",
            query = mapOf("memberId" to memberId)
        )
        if (!response.ok) throw Exception("HTTP error! status: ${response.status}")
        val data = response.json["Data"].list
        if (data.isEmpty()) return emptyMap()
        val organized = linkedMapOf<Int, MutableList<ResumeUploadedFile>>()
        for (doc in data) {
            val typeId = doc.int("MemberDocumentTypeId") ?: continue
            val name = doc.str("MemberDocumentFileName")?.takeIf { it.isNotEmpty() } ?: "فایل بدون نام"
            val uri = doc.str("FileURL")?.takeIf { it.isNotEmpty() } ?: ""
            organized.getOrPut(typeId) { mutableListOf() }
                .add(ResumeUploadedFile(doc.int("MemberDocumentId"), name, uri))
        }
        organized
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        emptyMap()
    }
}

private suspend fun fetchResumeSnapshot(memberId: Int): ResumeSnapshot {
    return try {
        val response = Api.get(
            "MemberDocumentType/GetAll",
            query = mapOf("filterActive" to true, "currentPage" to 1, "pageSize" to 50)
        )
        if (!response.ok) throw Exception("HTTP error! status: ${response.status}")
        val data = response.json["Data"].list
        if (data.isEmpty()) return ResumeSnapshot()
        val types = data.mapNotNull { item ->
            val id = item.int("MemberDocumentTypeId") ?: return@mapNotNull null
            ResumeDocType(
                id = id,
                name = item.str("Name") ?: "",
                description = item.str("ShortDescription") ?: ""
            )
        }
        ResumeSnapshot(types = types, files = fetchExistingResumeFiles(memberId))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ResumeSnapshot(error = e.message)
    }
}

@Composable
fun MyResumeScreen() {
    val navigation = useNavigation()
    val scope = rememberCoroutineScope()
    val picker = rememberMediaPicker()
    val memberId = AuthState.user?.MemberId
    val memberName = "کاربر"

    var documentTypes by remember { mutableStateOf<List<ResumeDocType>>(emptyList()) }
    var uploadedFiles by remember { mutableStateOf<Map<Int, List<ResumeUploadedFile>>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var isUploading by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var deleteModalVisible by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<ResumePendingDelete?>(null) }
    var toast by remember { mutableStateOf(ToastData()) }
    var toastToken by remember { mutableStateOf(0) }

    fun showToast(message: String, type: String = "info") {
        toastToken += 1
        toast = ToastData(visible = true, message = message, type = type)
    }

    suspend fun applySnapshot(id: Int) {
        loading = true
        error = null
        try {
            val snap = fetchResumeSnapshot(id)
            documentTypes = snap.types
            uploadedFiles = snap.files
            error = snap.error
        } finally {
            loading = false
        }
    }

    LaunchedEffect(memberId) {
        if (memberId == null || memberId == 0) return@LaunchedEffect
        applySnapshot(memberId)
    }

    fun goHome() {
        navigation.navigate("MainTabs", mapOf("screen" to Tabs.Home))
    }

    fun goProfile() {
        navigation.navigate("MainTabs", mapOf("screen" to Tabs.Profile))
    }

    fun openFile(url: String) {
        try {
            if (canOpenUrl(url)) {
                if (!openUrl(url)) showToast("مشکلی در باز کردن فایل پیش آمد", "error")
            } else {
                showToast("امکان باز کردن این فایل وجود ندارد", "error")
            }
        } catch (_: Exception) {
            showToast("مشکلی در باز کردن فایل پیش آمد", "error")
        }
    }

    fun pickDocument(typeId: Int, typeName: String) {
        isUploading = typeId.toString()
        try {
            picker.pickDocument(listOf("*/*")) { picked ->
                scope.launch {
                    try {
                        if (picked.isEmpty()) return@launch
                        var successCount = 0
                        val ownerId = AuthState.user?.MemberId
                        for (file in picked) {
                            try {
                                val mime = file.mimeType.ifEmpty { "application/octet-stream" }
                                val response = Api.upload(
                                    path = "MemberDocument/Add",
                                    files = listOf(ApiFilePart("file", file.fileName, mime, file.bytes())),
                                    fields = mapOf(
                                        "MemberDocumentId" to "0",
                                        "MemberId" to (ownerId?.toString() ?: ""),
                                        "MemberName" to memberName,
                                        "MemberDocumentTypeId" to typeId.toString(),
                                        "MemberDocumentTypeName" to typeName,
                                        "InsertDate" to resumeIsoNow(),
                                        "ShamsiInsertDate" to resumeShamsiToday(),
                                        "MemberDocumentFileName" to "",
                                        "FileURL" to ""
                                    )
                                )
                                if (!response.ok || response.json == null) {
                                    throw Exception("Failed to upload file: ${response.status} - ${response.text}")
                                }
                                successCount++
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                showToast("مشکلی در اپلود فایل ${file.fileName} پیش آمد", "error")
                            }
                        }
                        if (successCount > 0) {
                            showToast("یک فایل به بخش $typeName اپلود شد", "success")
                            if (ownerId != null && ownerId != 0) {
                                uploadedFiles = fetchExistingResumeFiles(ownerId)
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        showToast("مشکلی در انتخاب فایل پیش آمد", "error")
                    } finally {
                        isUploading = ""
                    }
                }
            }
        } catch (_: Exception) {
            isUploading = ""
            showToast("مشکلی در انتخاب فایل پیش آمد", "error")
        }
    }

    fun confirmDelete() {
        val target = fileToDelete ?: return
        scope.launch {
            try {
                val documentId = target.file.memberDocumentId
                if (documentId != null) {
                    val response = Api.delete(
                        "MemberDocument/Delete",
                        query = mapOf("memberDocumentId" to documentId)
                    )
                    if (!response.ok || response.json == null) {
                        throw Exception("Delete failed: ${response.status}")
                    }
                }
                val current = uploadedFiles.toMutableMap()
                current[target.typeId] = (current[target.typeId] ?: emptyList())
                    .filterIndexed { index, _ -> index != target.index }
                uploadedFiles = current
                deleteModalVisible = false
                showToast("فایل با موفقیت حذف شد", "success")
                val ownerId = AuthState.user?.MemberId
                if (ownerId != null && ownerId != 0) {
                    uploadedFiles = fetchExistingResumeFiles(ownerId)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                deleteModalVisible = false
                showToast("مشکلی در حذف فایل از سرور پیش آمد", "error")
            } finally {
                fileToDelete = null
            }
        }
    }

    val sbh = statusBarHeight()
    val pageColor = Color(0xFFF8FAFC)

    if (!loading && error != null) {
        Box(Modifier.fillMaxSize().background(pageColor)) {
            MainBackground()
            ResumeBackButton(
                onClick = { goHome() },
                modifier = Modifier.align(AbsoluteAlignment.TopRight).absoluteOffset(x = (-20).dp, y = sbh + 30.dp - 32.dp)
            )
            ResumeMessageState(
                icon = "error",
                title = "خطا در دریافت اطلاعات",
                subtitle = "لطفاً اتصال اینترنت خود را بررسی کنید",
                retry = {
                    RowReverse(
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .rnShadow(8.dp, RoundedCornerShape(25.dp), Color(0xFF6366F1))
                            .clip(RoundedCornerShape(25.dp))
                            .background(Color(0xFF6366F1))
                            .touchableOpacity {
                                val id = AuthState.user?.MemberId ?: return@touchableOpacity
                                scope.launch { applySnapshot(id) }
                            }
                            .absolutePadding(left = 20.dp, right = 20.dp, top = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MaterialIcons("refresh", size = 20.dp, color = Color.White)
                        AppText(
                            text = "تلاش مجدد",
                            modifier = Modifier.absolutePadding(right = 8.dp),
                            fontSize = 16.sp,
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            )
        }
        return
    }

    if (!loading && error == null && documentTypes.isEmpty()) {
        Box(Modifier.fillMaxSize().background(pageColor)) {
            MainBackground()
            ResumeBackButton(
                onClick = { goHome() },
                modifier = Modifier.align(AbsoluteAlignment.TopRight).absoluteOffset(x = (-20).dp, y = sbh + 30.dp - 32.dp)
            )
            ResumeMessageState(
                icon = "folder",
                title = "هیچ نوع مدرکی یافت نشد",
                subtitle = "در حال حاضر هیچ دسته‌ای برای آپلود مدارک تعریف نشده است"
            )
        }
        return
    }

    if (loading) {
        Box(Modifier.fillMaxSize().background(pageColor)) {
            MainBackground()
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Box(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth()) {
                        Spacer(Modifier.fillMaxWidth().height(sbh + 80.dp + 30.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .absolutePadding(left = 20.dp, right = 20.dp)
                                .padding(bottom = 30.dp)
                                .resumePullUp(40.dp)
                        ) {
                            ResumeSectionTitle("فایل ها و مدارک")
                        }
                        Column(Modifier.absolutePadding(left = 20.dp, right = 20.dp)) {
                            repeat(3) { index ->
                                key(index) { ResumeSkeletonCard() }
                            }
                        }
                        Spacer(Modifier.height(50.dp))
                    }
                    ResumeBackButton(
                        onClick = { goHome() },
                        modifier = Modifier
                            .align(AbsoluteAlignment.TopRight)
                            .absoluteOffset(x = (-20).dp, y = sbh + 30.dp - 32.dp)
                    )
                }
            }
        }
        return
    }

    Box(Modifier.fillMaxSize().background(pageColor)) {
        MainBackground()
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.fillMaxWidth().height(sbh + 80.dp + 30.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .absolutePadding(left = 20.dp, right = 20.dp)
                    .padding(bottom = 30.dp)
                    .resumePullUp(40.dp)
            ) {
                ResumeSectionTitle("فایل ها و مدارک")
                ResumeBackButton(
                    onClick = { goProfile() },
                    modifier = Modifier
                        .align(AbsoluteAlignment.TopRight)
                        .absoluteOffset(x = (-20).dp, y = sbh + 30.dp - 32.dp)
                )
            }
            Column(Modifier.absolutePadding(left = 20.dp, right = 20.dp)) {
                documentTypes.forEachIndexed { index, type ->
                    key(type.id) {
                        ResumeUploadCard(
                            name = type.name,
                            description = type.description,
                            color = resumeColorForIndex(index),
                            files = uploadedFiles[type.id] ?: emptyList(),
                            uploading = isUploading == type.id.toString(),
                            onPick = { pickDocument(type.id, type.name) },
                            onOpen = { openFile(it) },
                            onDelete = { fileIndex ->
                                val file = uploadedFiles[type.id]?.getOrNull(fileIndex) ?: return@ResumeUploadCard
                                fileToDelete = ResumePendingDelete(type.id, fileIndex, file, type.name)
                                deleteModalVisible = true
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(50.dp))
        }
        key(toastToken) {
            Toast(toast.visible, toast.message, toast.type) { toast = toast.copy(visible = false) }
        }
        val pending = fileToDelete
        if (deleteModalVisible && pending != null) {
            ResumeDeleteDialog(
                typeName = pending.typeName,
                onCancel = {
                    deleteModalVisible = false
                    fileToDelete = null
                },
                onConfirm = { confirmDelete() }
            )
        }
    }
}
