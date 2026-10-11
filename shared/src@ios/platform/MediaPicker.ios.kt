@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class, ExperimentalComposeUiApi::class)

package ir.farimod.app.platform

import platform.AVFoundation.requestAccessForMediaType
import platform.AVFoundation.authorizationStatusForMediaType
import platform.Foundation.dataWithContentsOfURL
import platform.Foundation.dataWithContentsOfFile
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVAssetTrack
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVLayerVideoGravityResizeAspect
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.muted
import platform.AVFoundation.naturalSize
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.preferredTransform
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.tracksWithMediaType
import platform.AVKit.AVPlayerViewController
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSItemProvider
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.lastPathComponent
import platform.Foundation.pathExtension
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.writeToFile
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIImage
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UniformTypeIdentifiers.UTType
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy
import kotlin.math.abs

// ---------------------------------------------------------------------------------------------
// Messages (verbatim from RN `ImageUpload.tsx`)
// ---------------------------------------------------------------------------------------------
private const val MSG_CAMERA_DENIED = "برای ضبط ویدئو، مجوز دسترسی به دوربین لازم است"
private const val MSG_CAMERA_UNAVAILABLE = "دوربین در دسترس نیست"

/** RN `imageQuality = 0.8` default of `ImageUpload`. */
private const val JPEG_QUALITY = 0.8

// ---------------------------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------------------------

/** Objective-C delegates are held weakly by UIKit -> keep strong references here until the picker finishes. */
private val retainedDelegates = mutableListOf<Any>()

private fun retain(obj: Any) {
    retainedDelegates.add(obj)
}

private fun release(obj: Any) {
    retainedDelegates.remove(obj)
}

private fun onMain(block: () -> Unit) {
    dispatch_async(dispatch_get_main_queue()) { block() }
}

private fun topViewController(): UIViewController? {
    val app = UIApplication.sharedApplication
    val window = app.keyWindow ?: (app.windows.firstOrNull() as? UIWindow)
    var top = window?.rootViewController ?: return null
    while (true) {
        val presented = top.presentedViewController ?: break
        top = presented
    }
    return top
}

private fun present(controller: UIViewController): Boolean {
    val top = topViewController() ?: return false
    top.presentViewController(controller, true, null)
    return true
}

private fun tempDirectory(): String {
    val dir = NSTemporaryDirectory() + "farimod_picks/"
    NSFileManager.defaultManager.createDirectoryAtPath(dir, true, null, null)
    return dir
}

private fun newTempPath(extension: String): String {
    val ext = extension.trim().trimStart('.').ifEmpty { "bin" }
    return tempDirectory() + NSUUID().UUIDString() + "." + ext
}

private fun fileSizeOf(path: String): Long? {
    val attrs = NSFileManager.defaultManager.attributesOfItemAtPath(path, null)
    return (attrs?.get(NSFileSize) as? Number)?.toLong()
}

private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val result = ByteArray(size)
    if (size > 0) {
        result.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this.bytes, this.length)
        }
    }
    return result
}

private fun readFileBytes(path: String): ByteArray {
    val data = NSData.dataWithContentsOfFile(path) ?: throw IllegalStateException("Cannot read file: $path")
    return data.toByteArray()
}

private val knownMimeByExtension = mapOf(
    "jpg" to "image/jpeg", "jpeg" to "image/jpeg", "png" to "image/png", "gif" to "image/gif",
    "webp" to "image/webp", "heic" to "image/heic", "heif" to "image/heif", "bmp" to "image/bmp",
    "mp4" to "video/mp4", "mov" to "video/quicktime", "m4v" to "video/x-m4v", "3gp" to "video/3gpp",
    "pdf" to "application/pdf", "doc" to "application/msword",
    "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "xls" to "application/vnd.ms-excel",
    "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "ppt" to "application/vnd.ms-powerpoint",
    "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "txt" to "text/plain", "zip" to "application/zip", "mp3" to "audio/mpeg", "m4a" to "audio/mp4"
)

private fun mimeForExtension(extension: String): String {
    val ext = extension.lowercase()
    knownMimeByExtension[ext]?.let { return it }
    return UTType.typeWithFilenameExtension(ext)?.preferredMIMEType ?: "application/octet-stream"
}

private data class VideoInfo(val width: Int?, val height: Int?, val durationMs: Long?)

private fun readVideoInfo(path: String): VideoInfo {
    return try {
        val asset = AVURLAsset.URLAssetWithURL(NSURL.fileURLWithPath(path), null)
        val seconds = CMTimeGetSeconds(asset.duration)
        val durationMs = if (seconds.isNaN() || seconds.isInfinite()) null else (seconds * 1000.0).toLong()
        var width: Int? = null
        var height: Int? = null
        val track = asset.tracksWithMediaType(AVMediaTypeVideo!!).firstOrNull() as? AVAssetTrack
        if (track != null) {
            var w = 0.0
            var h = 0.0
            track.naturalSize.useContents {
                w = this.width
                h = this.height
            }
            var rotated = false
            track.preferredTransform.useContents {
                rotated = abs(this.b) > 0.5 && abs(this.c) > 0.5
            }
            if (rotated) {
                val t = w; w = h; h = t
            }
            if (w > 0.0 && h > 0.0) {
                width = w.toInt()
                height = h.toInt()
            }
        }
        VideoInfo(width, height, durationMs)
    } catch (e: Throwable) {
        VideoInfo(null, null, null)
    }
}

/** Builds a [PickedMedia] for a local file path (already inside the app sandbox). */
private fun localMedia(
    path: String,
    fileName: String,
    mimeType: String,
    width: Int? = null,
    height: Int? = null,
    durationMs: Long? = null
): PickedMedia {
    val uri = NSURL.fileURLWithPath(path).absoluteString ?: ("file://" + path)
    return PickedMedia(
        uri = uri,
        fileName = fileName,
        mimeType = mimeType,
        width = width,
        height = height,
        fileSize = fileSizeOf(path),
        durationMs = durationMs,
        readBytes = { withContext(Dispatchers.Default) { readFileBytes(path) } }
    )
}

private fun nowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()

/** Saves a [UIImage] as JPEG in the temp directory and returns the media (expo returns JPEG for camera/library photos). */
private fun saveImageAsJpeg(image: UIImage, baseName: String): PickedMedia? {
    val data = UIImageJPEGRepresentation(image, JPEG_QUALITY) ?: return null
    val path = newTempPath("jpg")
    if (!data.writeToFile(path, true)) return null
    var w = 0.0
    var h = 0.0
    image.size.useContents {
        w = this.width
        h = this.height
    }
    return localMedia(
        path = path,
        fileName = "${baseName}_${nowMillis()}.jpg",
        mimeType = "image/jpeg",
        width = if (w > 0.0) w.toInt() else null,
        height = if (h > 0.0) h.toInt() else null
    )
}

/** UTI string(s) accepted by the document picker for a MIME type like `application/pdf` or an image wildcard. */
private fun documentTypeFor(mime: String): UTType? {
    val identifier: String? = when {
        mime == "*/*" || mime.isEmpty() -> "public.item"
        mime == "image/*" -> "public.image"
        mime == "video/*" -> "public.movie"
        mime == "audio/*" -> "public.audio"
        mime == "text/*" -> "public.text"
        mime.endsWith("/*") -> "public.item"
        else -> UTType.typeWithMIMEType(mime)?.identifier
    }
    return identifier?.let { UTType.typeWithIdentifier(it) }
}

// ---------------------------------------------------------------------------------------------
// Delegates
// ---------------------------------------------------------------------------------------------

private class LibraryPickerDelegate(
    private val onFinish: (List<PHPickerResult>) -> Unit
) : NSObject(), PHPickerViewControllerDelegateProtocol {
    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, null)
        onFinish(didFinishPicking.filterIsInstance<PHPickerResult>())
    }
}

private class CameraDelegate(
    private val onFinish: (UIImage?) -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    override fun imagePickerController(picker: UIImagePickerController, didFinishPickingMediaWithInfo: Map<Any?, *>) {
        picker.dismissViewControllerAnimated(true, null)
        onFinish(didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, null)
        onFinish(null)
    }
}

private class DocumentDelegate(
    private val onFinish: (List<NSURL>) -> Unit
) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        onFinish(didPickDocumentsAtURLs.filterIsInstance<NSURL>())
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onFinish(emptyList())
    }
}

// ---------------------------------------------------------------------------------------------
// Controller
// ---------------------------------------------------------------------------------------------

private class IosMediaPicker : MediaPickerController {

    override fun pickImages(
        allowsMultipleSelection: Boolean,
        selectionLimit: Int,
        onPermissionDenied: (String) -> Unit,
        onResult: (List<PickedMedia>) -> Unit
    ) {
        val limit = if (allowsMultipleSelection) selectionLimit.coerceAtLeast(1) else 1
        presentLibrary(PHPickerFilter.imagesFilter, limit, onResult)
    }

    override fun pickVideo(onPermissionDenied: (String) -> Unit, onResult: (List<PickedMedia>) -> Unit) {
        presentLibrary(PHPickerFilter.videosFilter, 1, onResult)
    }

    private fun presentLibrary(filter: PHPickerFilter, limit: Int, onResult: (List<PickedMedia>) -> Unit) {
        val configuration = PHPickerConfiguration()
        configuration.selectionLimit = limit.toLong()
        configuration.filter = filter
        val picker = PHPickerViewController(configuration = configuration)
        lateinit var delegate: LibraryPickerDelegate
        delegate = LibraryPickerDelegate { results ->
            loadLibraryResults(results) { media ->
                onMain {
                    release(delegate)
                    onResult(media)
                }
            }
        }
        retain(delegate)
        picker.delegate = delegate
        if (!present(picker)) {
            release(delegate)
            onResult(emptyList())
        }
    }

    /** Loads the picked items one after another (callbacks arrive on background queues). */
    private fun loadLibraryResults(results: List<PHPickerResult>, onDone: (List<PickedMedia>) -> Unit) {
        val out = mutableListOf<PickedMedia>()
        fun next(index: Int) {
            if (index >= results.size) {
                onDone(out.toList())
                return
            }
            loadLibraryItem(results[index].itemProvider) { media ->
                if (media != null) out.add(media)
                next(index + 1)
            }
        }
        next(0)
    }

    private fun loadLibraryItem(provider: NSItemProvider, done: (PickedMedia?) -> Unit) {
        if (provider.hasItemConformingToTypeIdentifier("public.movie")) {
            provider.loadFileRepresentationForTypeIdentifier("public.movie") { url, _ ->
                // the file at `url` is deleted when this handler returns -> copy synchronously
                if (url == null) {
                    done(null)
                } else {
                    val ext = url.pathExtension?.takeIf { it.isNotEmpty() } ?: "mov"
                    val dest = newTempPath(ext)
                    val copied = NSFileManager.defaultManager.copyItemAtURL(url, NSURL.fileURLWithPath(dest), null)
                    if (copied) {
                        val info = readVideoInfo(dest)
                        done(
                            localMedia(
                                path = dest,
                                fileName = url.lastPathComponent ?: "video_${nowMillis()}.$ext",
                                mimeType = mimeForExtension(ext),
                                width = info.width,
                                height = info.height,
                                durationMs = info.durationMs
                            )
                        )
                    } else {
                        done(null)
                    }
                }
            }
        } else if (provider.hasItemConformingToTypeIdentifier("public.image")) {
            provider.loadFileRepresentationForTypeIdentifier("public.image") { url, _ ->
                // the file at `url` is only valid inside this handler -> decode + re-encode (JPEG) synchronously
                val data = if (url == null) null else NSData.dataWithContentsOfURL(url)
                val image = if (data == null) null else UIImage.imageWithData(data)
                done(if (image == null) null else saveImageAsJpeg(image, "image"))
            }
        } else {
            done(null)
        }
    }

    override fun takePhoto(onPermissionDenied: (String) -> Unit, onResult: (List<PickedMedia>) -> Unit) {
        val cameraType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        if (!UIImagePickerController.isSourceTypeAvailable(cameraType)) {
            onPermissionDenied(MSG_CAMERA_UNAVAILABLE)
            return
        }
        ensureCameraPermission { granted ->
            if (!granted) {
                onPermissionDenied(MSG_CAMERA_DENIED)
                return@ensureCameraPermission
            }
            val picker = UIImagePickerController()
            picker.sourceType = cameraType
            picker.allowsEditing = false
            lateinit var delegate: CameraDelegate
            delegate = CameraDelegate { image ->
                release(delegate)
                if (image == null) {
                    onResult(emptyList())
                } else {
                    onResult(listOfNotNull(saveImageAsJpeg(image, "captured_photo")))
                }
            }
            retain(delegate)
            picker.delegate = delegate
            if (!present(picker)) {
                release(delegate)
                onPermissionDenied(MSG_CAMERA_UNAVAILABLE)
            }
        }
    }

    private fun ensureCameraPermission(onResult: (Boolean) -> Unit) {
        val mediaType = AVMediaTypeVideo!!
        when (AVCaptureDevice.authorizationStatusForMediaType(mediaType)) {
            AVAuthorizationStatusAuthorized -> onResult(true)
            AVAuthorizationStatusNotDetermined ->
                AVCaptureDevice.requestAccessForMediaType(mediaType) { granted ->
                    onMain { onResult(granted) }
                }
            else -> onResult(false)
        }
    }

    override fun pickDocument(mimeTypes: List<String>, onResult: (List<PickedMedia>) -> Unit) {
        val types = mimeTypes.mapNotNull { documentTypeFor(it) }.ifEmpty {
            listOfNotNull(UTType.typeWithIdentifier("public.item"))
        }
        // asCopy = true: the system copies the chosen file into the app sandbox (no security-scoped access needed)
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = types, asCopy = true)
        picker.allowsMultipleSelection = false
        lateinit var delegate: DocumentDelegate
        delegate = DocumentDelegate { urls ->
            release(delegate)
            val media = urls.mapNotNull { url ->
                val path = url.path ?: return@mapNotNull null
                val name = url.lastPathComponent ?: "file_${nowMillis()}"
                val ext = name.substringAfterLast('.', "")
                val mime = mimeForExtension(ext)
                if (mime.startsWith("video/")) {
                    val info = readVideoInfo(path)
                    localMedia(path, name, mime, info.width, info.height, info.durationMs)
                } else {
                    localMedia(path, name, mime)
                }
            }
            onResult(media)
        }
        retain(delegate)
        picker.delegate = delegate
        if (!present(picker)) {
            release(delegate)
            onResult(emptyList())
        }
    }
}

@Composable
actual fun rememberMediaPicker(): MediaPickerController = remember { IosMediaPicker() }

// ---------------------------------------------------------------------------------------------
// Video (expo-video VideoView) -> AVPlayerViewController
// ---------------------------------------------------------------------------------------------

private fun percentEncodeNonAscii(value: String): String {
    val hex = "0123456789ABCDEF"
    val sb = StringBuilder()
    val run = StringBuilder()
    fun flush() {
        if (run.isNotEmpty()) {
            for (b in run.toString().encodeToByteArray()) {
                val v = b.toInt() and 0xFF
                sb.append('%').append(hex[v shr 4]).append(hex[v and 0x0F])
            }
            run.clear()
        }
    }
    for (c in value) {
        if (c.code in 33..126) {
            flush()
            sb.append(c)
        } else {
            run.append(c)
        }
    }
    flush()
    return sb.toString()
}

private fun makeNsUrl(url: String): NSURL? {
    if (url.startsWith("/")) return NSURL.fileURLWithPath(url)
    return NSURL.URLWithString(url) ?: NSURL.URLWithString(percentEncodeNonAscii(url))
}

private class IosVideoHolder(
    url: String,
    autoPlay: Boolean,
    private var loop: Boolean,
    muted: Boolean,
    cover: Boolean,
    showControls: Boolean
) {
    val controller = AVPlayerViewController()
    private val player: AVPlayer
    private var observer: Any? = null

    init {
        val nsUrl = makeNsUrl(url)
        val p: AVPlayer
        if (nsUrl != null) {
            val item = AVPlayerItem.playerItemWithURL(nsUrl)
            p = AVPlayer.playerWithPlayerItem(item)
            observer = NSNotificationCenter.defaultCenter.addObserverForName(
                AVPlayerItemDidPlayToEndTimeNotification,
                item,
                NSOperationQueue.mainQueue
            ) { _ ->
                if (this.loop) {
                    p.seekToTime(CMTimeMake(0, 1))
                    p.play()
                }
            }
        } else {
            p = AVPlayer()
        }
        player = p
        controller.player = p
        update(showControls, loop, muted, cover)
        if (autoPlay) p.play()
    }

    fun update(showControls: Boolean, loop: Boolean, muted: Boolean, cover: Boolean) {
        this.loop = loop
        player.muted = muted
        controller.showsPlaybackControls = showControls
        controller.videoGravity =
            if (cover) AVLayerVideoGravityResizeAspectFill!! else AVLayerVideoGravityResizeAspect!!
    }

    fun dispose() {
        observer?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        observer = null
        player.pause()
        player.replaceCurrentItemWithPlayerItem(null)
        controller.player = null
    }
}

@Composable
actual fun NativeVideoPlayer(
    url: String,
    modifier: Modifier,
    showControls: Boolean,
    autoPlay: Boolean,
    loop: Boolean,
    muted: Boolean,
    cover: Boolean
) {
    if (url.isEmpty()) {
        Box(modifier)
        return
    }
    key(url) {
        val holder = remember { IosVideoHolder(url, autoPlay, loop, muted, cover, showControls) }
        UIKitViewController(
            factory = { holder.controller },
            modifier = modifier,
            update = { holder.update(showControls, loop, muted, cover) },
            onRelease = { holder.dispose() }
        )
    }
}
