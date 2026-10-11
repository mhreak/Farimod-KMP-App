package ir.farimod.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ir.farimod.app.components.icons.MaterialCommunityIcons
import ir.farimod.app.components.icons.MaterialIcons
import ir.farimod.app.platform.rememberMediaPicker
import ir.farimod.app.theme.FarimodColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Port of `app/components/ImageInput.tsx`.
 *
 * Shows a 120x120 tile: camera icon when [imageUri] is null (tap → image picker), otherwise the image
 * (tap → [onImagePress]). The "actions" and "delete" bottom-sheet modals exist in RN but are never opened
 * (nothing sets `modalVisible` / calls `handleDeleteImage`), they are ported 1:1 anyway.
 */
@Composable
fun ImageInput(
    imageUri: String? = null,
    onChangeImage: (String?) -> Unit,
    onImagePress: ((String) -> Unit)? = null,
    disableImageActions: Boolean = false
) {
    var modalVisible by remember { mutableStateOf(false) }
    var deleteModalVisible by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val insets = rememberSafeAreaInsets()
    val picker = rememberMediaPicker()

    val slideAnim = remember { Animatable(300f) }
    val opacityAnim = remember { Animatable(0f) }
    val deleteSlideAnim = remember { Animatable(300f) }
    val deleteOpacityAnim = remember { Animatable(0f) }

    // useEffect(() => { ... }, [modalVisible])
    LaunchedEffect(modalVisible) {
        if (modalVisible) {
            launch { slideAnim.animateTo(0f, rnTiming(300)) }
            launch { opacityAnim.animateTo(1f, rnTiming(300)) }
        } else {
            launch { slideAnim.animateTo(300f, rnTiming(250)) }
            launch { opacityAnim.animateTo(0f, rnTiming(250)) }
        }
    }

    // useEffect(() => { ... }, [deleteModalVisible])
    LaunchedEffect(deleteModalVisible) {
        if (deleteModalVisible) {
            // Animated.spring tension 100, friction 8
            launch { deleteSlideAnim.animateTo(0f, rnSpring(100f, 8f)) }
            launch { deleteOpacityAnim.animateTo(1f, rnTiming(200)) }
        } else {
            launch { deleteSlideAnim.animateTo(300f, rnTiming(200)) }
            launch { deleteOpacityAnim.animateTo(0f, rnTiming(200)) }
        }
    }

    // const selectImage = async () => { ImagePicker.launchImageLibraryAsync(...) }
    fun selectImage() {
        try {
            picker.pickImages(
                allowsMultipleSelection = false,
                selectionLimit = 1,
                onPermissionDenied = { RNAlert.alert(it) },
                onResult = { assets ->
                    if (assets.isNotEmpty()) onChangeImage(assets[0].uri)
                }
            )
        } catch (e: Exception) {
            println("Error reading an image $e")
        }
    }

    fun handlePress() {
        if (imageUri == null) {
            selectImage()
        } else if (onImagePress != null) {
            onImagePress(imageUri)
        }
    }

    @Suppress("UNUSED_VARIABLE", "unused")
    val handleDeleteImage = {
        modalVisible = false
        scope.launch {
            delay(300)
            deleteModalVisible = true
        }
        Unit
    }

    fun confirmDelete() {
        deleteModalVisible = false
        onChangeImage(null)
    }

    fun cancelDelete() {
        deleteModalVisible = false
    }

    fun handleViewImage() {
        modalVisible = false
        scope.launch {
            delay(300)
            if (onImagePress != null) {
                onImagePress(imageUri!!)
            }
        }
    }

    @Suppress("UNUSED_VARIABLE", "unused")
    val handleEditImage = {
        modalVisible = false
        scope.launch {
            delay(300)
            selectImage()
        }
        Unit
    }

    fun closeModal() {
        modalVisible = false
    }

    // styles.container: marginVertical 10, 120x120, radius 15, bg gray, overflow hidden, centered
    Box(
        Modifier
            .padding(vertical = 10.dp)
            .wrapContentSize(Alignment.TopStart, unbounded = true)
            .size(120.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(FarimodColors.Gray)
            .pressable { handlePress() },
        contentAlignment = Alignment.Center
    ) {
        if (imageUri == null) {
            MaterialCommunityIcons(name = "camera", size = 40.dp, color = FarimodColors.Medium)
        }
        if (imageUri != null) {
            NetworkImage(imageUri, Modifier.fillMaxSize(), resizeMode = ResizeMode.cover)
        }
    }

    // ── Actions Modal ──────────────────────────────────────────────────────────
    if (modalVisible || slideAnim.value < 300f) {
        val density = LocalDensity.current
        Dialog(
            onDismissRequest = { closeModal() },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0f, 0f, 0f, 0.6f))
                    .pressable { closeModal() },
                contentAlignment = Alignment.BottomCenter
            ) {
                val sheetShape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            translationY = slideAnim.value * density.density
                            alpha = opacityAnim.value
                        }
                        .rnShadow(12.dp, sheetShape)
                        .background(Color(0xFFFFFFFF), sheetShape)
                        .padding(top = 12.dp)
                        .padding(horizontal = 20.dp)
                        .padding(bottom = maxOf(insets.bottom, 20.dp) + 20.dp)
                ) {
                    // modalHeader
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .size(width = 40.dp, height = 4.dp)
                                .background(Color(0xFFD1D5DB), RoundedCornerShape(2.dp))
                        )
                    }

                    // actionsContainer
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(
                            Modifier
                                .touchableOpacity { handleViewImage() }
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                Modifier
                                    .padding(bottom = 8.dp)
                                    .rnShadow(3.dp, CircleShape)
                                    .size(60.dp)
                                    .background(Color(0xFF3B82F6), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                MaterialIcons("visibility", size = 24.dp, color = Color.White)
                            }
                            AppText(
                                "مشاهده",
                                fontSize = 14.sp,
                                color = Color(0xFF374151),
                                fontFamily = RNFonts.Yekan_Bakh_Regular,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // cancelButton
                    val cancelShape = RoundedCornerShape(15.dp)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 15.dp)
                            .padding(horizontal = 5.dp)
                            .touchableOpacity { closeModal() }
                            .background(Color(0xFFFEE2E2), cancelShape)
                            .border(1.dp, Color(0xFFFECACA), cancelShape)
                            .padding(vertical = 16.dp)
                    ) {
                        AppText(
                            "لغو",
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 16.sp,
                            color = Color(0xFFDC2626),
                            fontFamily = RNFonts.Yekan_Bakh_Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // ── Delete Modal ───────────────────────────────────────────────────────────
    if (deleteModalVisible || deleteSlideAnim.value < 300f) {
        val density = LocalDensity.current
        Dialog(
            onDismissRequest = { cancelDelete() },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0f, 0f, 0f, 0.7f))
                    .pressable { cancelDelete() }
                    .padding(horizontal = 30.dp),
                contentAlignment = Alignment.Center
            ) {
                val cardShape = RoundedCornerShape(20.dp)
                Column(
                    Modifier
                        .padding(bottom = maxOf(insets.bottom, 20.dp))
                        .graphicsLayer {
                            translationY = deleteSlideAnim.value * density.density
                            alpha = deleteOpacityAnim.value
                        }
                        .fillMaxWidth()
                        .widthIn(max = 350.dp)
                        .rnShadow(15.dp, cardShape)
                        .background(Color(0xFFFFFFFF), cardShape)
                        .padding(30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // deleteIconContainer
                    Box(
                        Modifier
                            .padding(bottom = 20.dp)
                            .size(80.dp)
                            .background(Color(0xFFFEE2E2), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        MaterialIcons("warning", size = 48.dp, color = Color(0xFFEF4444))
                    }

                    AppText(
                        "حذف عکس",
                        modifier = Modifier.padding(bottom = 12.dp),
                        fontSize = 20.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Bold,
                        color = Color(0xFF1F2937),
                        textAlign = TextAlign.Center
                    )

                    AppText(
                        "آیا مطمئن هستید که می‌خواهید این عکس را حذف کنید؟",
                        modifier = Modifier.padding(bottom = 30.dp),
                        fontSize = 16.sp,
                        fontFamily = RNFonts.Yekan_Bakh_Regular,
                        color = Color(0xFF6B7280),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )

                    // deleteButtonsContainer (flexDirection: row, gap 12)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val btnShape = RoundedCornerShape(12.dp)
                        Box(
                            Modifier
                                .weight(1f)
                                .touchableOpacity { cancelDelete() }
                                .background(Color(0xFFF3F4F6), btnShape)
                                .border(1.dp, Color(0xFFD1D5DB), btnShape)
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AppText(
                                "خیر",
                                fontSize = 16.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Regular,
                                color = Color(0xFF374151)
                            )
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .touchableOpacity { confirmDelete() }
                                .background(Color(0xFFEF4444), btnShape)
                                .padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AppText(
                                "بله، حذف کن",
                                fontSize = 16.sp,
                                fontFamily = RNFonts.Yekan_Bakh_Regular,
                                color = Color(0xFFFFFFFF)
                            )
                        }
                    }
                }
            }
        }
    }
}
