package ir.farimod.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A file chosen by the user (expo-image-picker / expo-document-picker asset).
 *  - [uri]  : displayable URI (file:// or content:// or ph://) usable with Coil / [ir.farimod.app.ui.NetworkImage]
 *  - [bytes]: reads the whole file (use for multipart upload with [ir.farimod.app.network.ApiFilePart])
 */
class PickedMedia(
    val uri: String,
    val fileName: String,
    val mimeType: String,
    val width: Int? = null,
    val height: Int? = null,
    val fileSize: Long? = null,
    val durationMs: Long? = null,
    val readBytes: suspend () -> ByteArray
) {
    val isImage: Boolean get() = mimeType.startsWith("image/")
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    suspend fun bytes(): ByteArray = readBytes()
}

/**
 * expo-image-picker + expo-document-picker.
 * Every `launch*` call is asynchronous; [onResult] is invoked with an empty list when the user cancels.
 * Permission prompts are handled internally; [onPermissionDenied] receives a short Persian message to show in a toast/alert.
 */
interface MediaPickerController {
    /** `ImagePicker.launchImageLibraryAsync({ mediaTypes: images })` */
    fun pickImages(
        allowsMultipleSelection: Boolean = false,
        selectionLimit: Int = 1,
        onPermissionDenied: (String) -> Unit = {},
        onResult: (List<PickedMedia>) -> Unit
    )

    /** `ImagePicker.launchImageLibraryAsync({ mediaTypes: videos })` */
    fun pickVideo(
        onPermissionDenied: (String) -> Unit = {},
        onResult: (List<PickedMedia>) -> Unit
    )

    /** `ImagePicker.launchCameraAsync()` (photo) */
    fun takePhoto(
        onPermissionDenied: (String) -> Unit = {},
        onResult: (List<PickedMedia>) -> Unit
    )

    /** `DocumentPicker.getDocumentAsync({ type })` — [mimeTypes] e.g. application/pdf, image wildcard */
    fun pickDocument(
        mimeTypes: List<String> = listOf("*/*"),
        onResult: (List<PickedMedia>) -> Unit
    )
}

@Composable
expect fun rememberMediaPicker(): MediaPickerController

/**
 * expo-video `<VideoView player={player} nativeControls />`.
 * [url] can be a remote URL or a local picked-file URI.
 */
@Composable
expect fun NativeVideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    showControls: Boolean = true,
    autoPlay: Boolean = false,
    loop: Boolean = false,
    muted: Boolean = false,
    /** true = scale to fill (RN contentFit="cover"), false = fit inside (contentFit="contain") */
    cover: Boolean = false
)
