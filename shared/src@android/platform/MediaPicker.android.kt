package ir.farimod.app.platform

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------------------------
// Messages (verbatim from RN `ImageUpload.tsx` -> requestPermissions / launchCamera error handling)
// ---------------------------------------------------------------------------------------------
private const val MSG_CAMERA_DENIED = "برای ضبط ویدئو، مجوز دسترسی به دوربین لازم است"
private const val MSG_CAMERA_UNAVAILABLE = "دوربین در دسترس نیست"
private const val MSG_PERMISSION_ERROR = "مشکل در درخواست مجوزها"

/** Mutable bag shared between the Activity-result callbacks and the controller. */
private class PickerState {
    var mediaHandler: ((List<Uri>) -> Unit)? = null
    var cameraFile: File? = null
    var cameraHandler: ((File?) -> Unit)? = null
    var permissionHandler: ((Boolean) -> Unit)? = null
}

@Composable
actual fun rememberMediaPicker(): MediaPickerController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { PickerState() }

    // Photo picker (no storage permission required) - single
    val singleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val handler = state.mediaHandler
        state.mediaHandler = null
        handler?.invoke(listOfNotNull(uri))
    }
    // Photo picker - multiple (limit enforced by truncating the result to `selectionLimit`)
    val multiLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        val handler = state.mediaHandler
        state.mediaHandler = null
        handler?.invoke(uris)
    }
    // Documents (expo-document-picker)
    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val handler = state.mediaHandler
        state.mediaHandler = null
        handler?.invoke(listOfNotNull(uri))
    }
    // Camera
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = state.cameraFile
        val handler = state.cameraHandler
        state.cameraFile = null
        state.cameraHandler = null
        handler?.invoke(if (success && file != null && file.exists() && file.length() > 0L) file else null)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val handler = state.permissionHandler
        state.permissionHandler = null
        handler?.invoke(granted)
    }

    return remember(context, scope) {
        AndroidMediaPicker(
            context = context,
            scope = scope,
            state = state,
            launchSingle = { request -> singleLauncher.launch(request) },
            launchMulti = { request -> multiLauncher.launch(request) },
            launchDocument = { types -> documentLauncher.launch(types) },
            launchCamera = { uri -> cameraLauncher.launch(uri) },
            launchPermission = { permission -> permissionLauncher.launch(permission) }
        )
    }
}

private class AndroidMediaPicker(
    private val context: Context,
    private val scope: CoroutineScope,
    private val state: PickerState,
    private val launchSingle: (PickVisualMediaRequest) -> Unit,
    private val launchMulti: (PickVisualMediaRequest) -> Unit,
    private val launchDocument: (Array<String>) -> Unit,
    private val launchCamera: (Uri) -> Unit,
    private val launchPermission: (String) -> Unit
) : MediaPickerController {

    /** Converts picked [Uri]s into [PickedMedia] off the main thread and delivers them on the main thread. */
    private fun handler(limit: Int, onResult: (List<PickedMedia>) -> Unit): (List<Uri>) -> Unit = { uris ->
        scope.launch {
            val items = withContext(Dispatchers.IO) {
                uris.take(limit).mapNotNull { uri ->
                    try {
                        buildPickedMedia(context, uri)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
            onResult(items)
        }
    }

    override fun pickImages(
        allowsMultipleSelection: Boolean,
        selectionLimit: Int,
        onPermissionDenied: (String) -> Unit,
        onResult: (List<PickedMedia>) -> Unit
    ) {
        val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        try {
            if (allowsMultipleSelection && selectionLimit > 1) {
                state.mediaHandler = handler(selectionLimit, onResult)
                launchMulti(request)
            } else {
                state.mediaHandler = handler(1, onResult)
                launchSingle(request)
            }
        } catch (e: Exception) {
            state.mediaHandler = null
            onResult(emptyList())
        }
    }

    override fun pickVideo(onPermissionDenied: (String) -> Unit, onResult: (List<PickedMedia>) -> Unit) {
        try {
            state.mediaHandler = handler(1, onResult)
            launchSingle(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
        } catch (e: Exception) {
            state.mediaHandler = null
            onResult(emptyList())
        }
    }

    override fun takePhoto(onPermissionDenied: (String) -> Unit, onResult: (List<PickedMedia>) -> Unit) {
        val hasCamera = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        if (!hasCamera) {
            onPermissionDenied(MSG_CAMERA_UNAVAILABLE)
            return
        }
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            startCamera(onPermissionDenied, onResult)
        } else {
            state.permissionHandler = { ok ->
                if (ok) startCamera(onPermissionDenied, onResult) else onPermissionDenied(MSG_CAMERA_DENIED)
            }
            try {
                launchPermission(Manifest.permission.CAMERA)
            } catch (e: Exception) {
                state.permissionHandler = null
                onPermissionDenied(MSG_PERMISSION_ERROR)
            }
        }
    }

    private fun startCamera(onPermissionDenied: (String) -> Unit, onResult: (List<PickedMedia>) -> Unit) {
        try {
            val dir = File(context.cacheDir, "images").apply { mkdirs() }
            val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
            state.cameraFile = file
            state.cameraHandler = { captured ->
                if (captured == null) {
                    onResult(emptyList())
                } else {
                    scope.launch {
                        val media = withContext(Dispatchers.IO) {
                            try {
                                buildPickedMedia(context, Uri.fromFile(captured), forcedMime = "image/jpeg")
                            } catch (e: Exception) {
                                null
                            }
                        }
                        onResult(listOfNotNull(media))
                    }
                }
            }
            launchCamera(uri)
        } catch (e: Exception) {
            state.cameraFile = null
            state.cameraHandler = null
            onPermissionDenied(MSG_CAMERA_UNAVAILABLE)
        }
    }

    override fun pickDocument(mimeTypes: List<String>, onResult: (List<PickedMedia>) -> Unit) {
        val types = if (mimeTypes.isEmpty()) arrayOf("*/*") else mimeTypes.toTypedArray()
        try {
            state.mediaHandler = handler(1, onResult)
            launchDocument(types)
        } catch (e: Exception) {
            state.mediaHandler = null
            onResult(emptyList())
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Uri -> PickedMedia
// ---------------------------------------------------------------------------------------------

private fun buildPickedMedia(context: Context, uri: Uri, forcedMime: String? = null): PickedMedia {
    val resolver = context.contentResolver

    var name: String? = null
    var size: Long? = null
    if (uri.scheme == "file") {
        val f = File(uri.path ?: "")
        name = f.name
        if (f.exists()) size = f.length()
    } else {
        try {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val ni = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (ni >= 0 && !c.isNull(ni)) name = c.getString(ni)
                    val si = c.getColumnIndex(OpenableColumns.SIZE)
                    if (si >= 0 && !c.isNull(si)) size = c.getLong(si)
                }
            }
        } catch (e: Exception) {
            // ignore: some providers do not support OpenableColumns
        }
    }

    val extensionFromName = name?.substringAfterLast('.', "")?.lowercase().orEmpty()
    var mime: String? = forcedMime ?: (if (uri.scheme == "file") null else resolver.getType(uri))
    if (mime.isNullOrEmpty() || mime == "application/octet-stream") {
        val byExt = if (extensionFromName.isNotEmpty()) MimeTypeMap.getSingleton().getMimeTypeFromExtension(extensionFromName) else null
        mime = byExt ?: mime
    }
    val finalMime = mime?.takeIf { it.isNotEmpty() } ?: "application/octet-stream"

    val fileName = name?.takeIf { it.isNotBlank() } ?: run {
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(finalMime)
        "file_${System.currentTimeMillis()}" + if (ext != null) ".$ext" else ""
    }

    var width: Int? = null
    var height: Int? = null
    var durationMs: Long? = null
    if (finalMime.startsWith("image/")) {
        try {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            var w = opts.outWidth
            var h = opts.outHeight
            if (w > 0 && h > 0) {
                if (Build.VERSION.SDK_INT >= 24) {
                    try {
                        val orientation = resolver.openInputStream(uri)?.use {
                            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                        }
                        if (orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                            orientation == ExifInterface.ORIENTATION_ROTATE_270 ||
                            orientation == ExifInterface.ORIENTATION_TRANSPOSE ||
                            orientation == ExifInterface.ORIENTATION_TRANSVERSE
                        ) {
                            val t = w; w = h; h = t
                        }
                    } catch (e: Exception) {
                        // ignore exif problems
                    }
                }
                width = w
                height = h
            }
        } catch (e: Exception) {
            // ignore: dimensions are optional
        }
    } else if (finalMime.startsWith("video/")) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            var w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            var h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            if (rotation == 90 || rotation == 270) {
                val t = w; w = h; h = t
            }
            width = w
            height = h
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        } catch (e: Exception) {
            // ignore: metadata is optional
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
            }
        }
    }

    return PickedMedia(
        uri = uri.toString(),
        fileName = fileName,
        mimeType = finalMime,
        width = width,
        height = height,
        fileSize = size,
        durationMs = durationMs,
        readBytes = {
            withContext(Dispatchers.IO) {
                resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IOException("Cannot open $uri")
            }
        }
    )
}

// ---------------------------------------------------------------------------------------------
// Video (expo-video VideoView) -> android.widget.VideoView
// ---------------------------------------------------------------------------------------------

private fun Context.findLifecycleOwner(): LifecycleOwner? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is LifecycleOwner) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * FrameLayout hosting a [VideoView]. In "cover" mode the video child is laid out larger than the container
 * (center-cropped, like `contentFit="cover"`); otherwise the VideoView letterboxes itself ("contain").
 */
private class VideoHostView(context: Context) : FrameLayout(context) {
    private val video = VideoView(context)
    private var mediaPlayer: MediaPlayer? = null
    private var mediaController: MediaController? = null
    private var lifecycleOwner: LifecycleOwner? = null
    private val lifecycleObserver = object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_PAUSE) {
                try {
                    if (video.isPlaying) video.pause()
                } catch (e: Exception) {
                }
            }
        }
    }

    var currentUrl: String? = null
        private set
    private var videoWidth = 0
    private var videoHeight = 0
    private var cover = false
    private var loop = false
    private var muted = false

    init {
        clipChildren = true
        addView(video, LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER))
        video.setOnPreparedListener { mp ->
            mediaPlayer = mp
            videoWidth = mp.videoWidth
            videoHeight = mp.videoHeight
            mp.setOnVideoSizeChangedListener { _, w, h ->
                videoWidth = w
                videoHeight = h
                requestLayout()
            }
            applyPlayerSettings()
            requestLayout()
        }
        // Returning true suppresses the "Can't play this video" system dialog.
        video.setOnErrorListener { _, _, _ -> true }
    }

    fun load(url: String, autoPlay: Boolean) {
        currentUrl = url
        videoWidth = 0
        videoHeight = 0
        mediaPlayer = null
        val uri = if (url.startsWith("/")) Uri.fromFile(File(url)) else Uri.parse(url)
        video.setVideoURI(uri)
        if (autoPlay) {
            video.start()
        } else {
            // show the first frame instead of a black surface
            video.seekTo(1)
        }
    }

    fun configure(cover: Boolean, showControls: Boolean, loop: Boolean, muted: Boolean) {
        if (this.cover != cover) {
            this.cover = cover
            requestLayout()
        }
        this.loop = loop
        this.muted = muted
        applyPlayerSettings()
        if (showControls) {
            if (mediaController == null) {
                mediaController = MediaController(context)
                video.setMediaController(mediaController)
            }
        } else if (mediaController != null) {
            mediaController?.hide()
            video.setMediaController(null)
            mediaController = null
        }
    }

    private fun applyPlayerSettings() {
        val mp = mediaPlayer ?: return
        try {
            mp.isLooping = loop
            val volume = if (muted) 0f else 1f
            mp.setVolume(volume, volume)
        } catch (e: IllegalStateException) {
            // player already released
        }
    }

    fun release() {
        try {
            mediaController?.hide()
            video.setMediaController(null)
            video.stopPlayback()
        } catch (e: Exception) {
        }
        mediaController = null
        mediaPlayer = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycleOwner = context.findLifecycleOwner()
        lifecycleOwner?.lifecycle?.addObserver(lifecycleObserver)
    }

    override fun onDetachedFromWindow() {
        lifecycleOwner?.lifecycle?.removeObserver(lifecycleObserver)
        lifecycleOwner = null
        super.onDetachedFromWindow()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val cw = right - left
        val ch = bottom - top
        if (cover && videoWidth > 0 && videoHeight > 0 && cw > 0 && ch > 0) {
            val scale = max(cw.toFloat() / videoWidth, ch.toFloat() / videoHeight)
            val w = (videoWidth * scale).roundToInt()
            val h = (videoHeight * scale).roundToInt()
            video.measure(
                View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)
            )
            val l = (cw - w) / 2
            val t = (ch - h) / 2
            video.layout(l, t, l + w, t + h)
        } else {
            super.onLayout(changed, left, top, right, bottom)
        }
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
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            VideoHostView(ctx).also { host ->
                host.configure(cover, showControls, loop, muted)
                if (url.isNotEmpty()) host.load(url, autoPlay)
            }
        },
        update = { host ->
            if (url.isNotEmpty() && host.currentUrl != url) host.load(url, autoPlay)
            host.configure(cover, showControls, loop, muted)
        },
        onRelease = { host -> host.release() }
    )
}
