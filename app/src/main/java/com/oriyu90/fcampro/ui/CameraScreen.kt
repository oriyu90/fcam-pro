package com.oriyu90.fcampro.ui

import android.content.ContentValues
import android.content.Intent
import android.media.MediaActionSound
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.Camera
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop169
import androidx.compose.material.icons.filled.Crop54
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PanoramaHorizontal
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.oriyu90.fcampro.R
import com.oriyu90.fcampro.core.AppSettings
import com.oriyu90.fcampro.data.CameraProfile
import com.oriyu90.fcampro.services.BackgroundCameraService
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** Describes an OS "capture and return" request (ACTION_IMAGE_CAPTURE / ACTION_VIDEO_CAPTURE). */
data class ExternalCaptureSpec(val isVideo: Boolean, val outputUri: Uri?)

@OptIn(ExperimentalMaterial3Api::class)
@androidx.annotation.OptIn(markerClass = [ExperimentalCamera2Interop::class])
@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    external: ExternalCaptureSpec? = null,
    onExternalResult: (Boolean, Intent?) -> Unit = { _, _ -> },
    onOpenSettings: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val appSettings = remember { AppSettings.get(context) }
    val settings by viewModel.settings.collectAsState()
    val availableLenses by viewModel.availableLenses.collectAsState()
    val profiles by viewModel.profiles.collectAsState()
    val bgRunning by BackgroundCameraService.running.collectAsState()
    val noCameraAvailable by viewModel.noCameraAvailable.collectAsState()
    val lastMedia by viewModel.lastMedia.collectAsState()

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var qrAnalyzer by remember { mutableStateOf<QrCodeAnalyzer?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }

    var detectedQr by remember { mutableStateOf<String?>(null) }
    var lastQrAt by remember { mutableStateOf(0L) }
    var timelapseActive by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }

    val appSnapshot by appSettings.state.collectAsState()
    var batteryPct by remember { mutableStateOf<Int?>(null) }
    var thumb by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var focusPoint by remember { mutableStateOf<androidx.compose.ui.geometry.Offset?>(null) }
    var focusLocked by remember { mutableStateOf(false) }
    var previewSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val density = androidx.compose.ui.platform.LocalDensity.current

    val mediaActionSound = remember { MediaActionSound() }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val analysisExecutor = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }
    var timerJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    var showFlash by remember { mutableStateOf(false) }
    val flashAlpha by
        animateFloatAsState(
            targetValue = if (showFlash) 1f else 0f,
            animationSpec = tween(durationMillis = if (showFlash) 50 else 300),
            finishedListener = { if (it == 1f) showFlash = false },
            label = "flash",
        )

    // Force the mode when serving an external capture request.
    LaunchedEffect(external) {
        if (external != null) {
            viewModel.setMode(if (external.isVideo) CameraMode.VIDEO else CameraMode.PHOTO)
        }
    }

    fun msg(resId: Int, vararg args: Any) {
        scope.launch { snackbar.showSnackbar(context.getString(resId, *args)) }
    }

    // --- Sound lifecycle ---------------------------------------------------
    DisposableEffect(Unit) {
        mediaActionSound.load(MediaActionSound.SHUTTER_CLICK)
        mediaActionSound.load(MediaActionSound.START_VIDEO_RECORDING)
        mediaActionSound.load(MediaActionSound.STOP_VIDEO_RECORDING)
        onDispose {
            timelapseActive = false
            timerJob?.cancel()
            runCatching { recording?.stop() }
            qrAnalyzer?.release()
            mediaActionSound.release()
            runCatching { analysisExecutor.shutdown() }
        }
    }

    LaunchedEffect(noCameraAvailable) {
        if (noCameraAvailable) msg(R.string.snack_camera_unavailable)
    }

    // --- Battery level (for the collapsed control cluster) --------------
    DisposableEffect(Unit) {
        fun read(i: Intent?) {
            val lvl = i?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = i?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
            batteryPct = if (lvl >= 0 && scale > 0) (lvl * 100 / scale) else null
        }
        val receiver =
            object : android.content.BroadcastReceiver() {
                override fun onReceive(c: android.content.Context?, i: Intent?) = read(i)
            }
        val sticky =
            androidx.core.content.ContextCompat.registerReceiver(
                context,
                receiver,
                android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED),
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        read(sticky)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    // --- Latest-capture thumbnail -------------------------------------
    LaunchedEffect(lastMedia) {
        val lm = lastMedia
        if (lm == null) {
            thumb = null
            return@LaunchedEffect
        }
        thumb =
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching {
                        val bmp =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                context.contentResolver.loadThumbnail(
                                    lm.uri,
                                    android.util.Size(160, 160),
                                    null,
                                )
                            } else {
                                context.contentResolver.openInputStream(lm.uri)?.use { ins ->
                                    android.graphics.BitmapFactory.decodeStream(
                                        ins,
                                        null,
                                        android.graphics.BitmapFactory.Options().apply {
                                            inSampleSize = 8
                                        },
                                    )
                                }
                            }
                        bmp?.asImageBitmap()
                    }
                    .getOrNull()
            }
    }

    // --- Acquire provider ------------------------------------------------
    LaunchedEffect(Unit) {
        cameraProvider =
            runCatching {
                    suspendCoroutine<ProcessCameraProvider> { cont ->
                        val f = ProcessCameraProvider.getInstance(context)
                        f.addListener(
                            { cont.resume(f.get()) },
                            ContextCompat.getMainExecutor(context),
                        )
                    }
                }
                .getOrNull()
        if (cameraProvider == null) msg(R.string.snack_camera_unavailable)
    }

    // --- Bind use cases -------------------------------------------------
    LaunchedEffect(
        cameraProvider,
        settings.currentLens?.id,
        settings.cameraMode,
        settings.flashMode,
        settings.aspectRatio,
        bgRunning,
    ) {
        val provider = cameraProvider ?: return@LaunchedEffect
        val lens = settings.currentLens ?: return@LaunchedEffect

        // The background recording service owns the physical camera while it runs.
        if (bgRunning) {
            runCatching { provider.unbindAll() }
            camera = null
            return@LaunchedEffect
        }

        runCatching { provider.unbindAll() }
        zoomRatio = 1f
        focusLocked = false
        focusPoint = null

        val resolutionSelector =
            ResolutionSelector.Builder()
                .setAspectRatioStrategy(
                    if (settings.aspectRatio == androidx.camera.core.AspectRatio.RATIO_16_9)
                        AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
                    else AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
                )
                .build()

        val selector =
            CameraSelector.Builder()
                .requireLensFacing(
                    if (lens.isFront) CameraSelector.LENS_FACING_FRONT
                    else CameraSelector.LENS_FACING_BACK
                )
                .addCameraFilter(
                    CameraFilter { infos ->
                        val match =
                            infos.filter {
                                runCatching { Camera2CameraInfo.from(it).cameraId }.getOrNull() ==
                                    lens.id
                            }
                        if (match.isNotEmpty()) match else infos
                    }
                )
                .build()

        val preview =
            Preview.Builder().setResolutionSelector(resolutionSelector).build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

        try {
            camera =
                if (settings.cameraMode == CameraMode.VIDEO) {
                    // Tear down photo-mode use cases so the ML Kit scanner and the stale
                    // ImageCapture reference are not retained while recording.
                    qrAnalyzer?.release()
                    qrAnalyzer = null
                    imageCapture = null
                    val recorder =
                        Recorder.Builder()
                            .setQualitySelector(
                                QualitySelector.fromOrderedList(
                                    listOf(Quality.FHD, Quality.HD, Quality.SD)
                                )
                            )
                            .build()
                    val vc = VideoCapture.withOutput(recorder)
                    videoCapture = vc
                    provider.bindToLifecycle(lifecycleOwner, selector, preview, vc)
                } else {
                    videoCapture = null
                    val ic =
                        ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .setFlashMode(settings.flashMode)
                            .setResolutionSelector(resolutionSelector)
                            .build()
                    imageCapture = ic
                    val analyzer = QrCodeAnalyzer { value ->
                        val now = System.currentTimeMillis()
                        if (now - lastQrAt > 1500) {
                            lastQrAt = now
                            detectedQr = value
                        }
                    }
                    qrAnalyzer?.release()
                    qrAnalyzer = analyzer
                    val analysis =
                        ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { it.setAnalyzer(analysisExecutor, analyzer) }
                    provider.bindToLifecycle(lifecycleOwner, selector, preview, ic, analysis)
                }
        } catch (e: Exception) {
            Log.e(TAG, "bind failed", e)
            camera = null
            msg(R.string.snack_camera_setup_failed, e.message ?: "")
        }
    }

    // Keep still/video output orientation correct while the Activity is not recreated
    // on rotation (android:configChanges).
    DisposableEffect(Unit) {
        val dm = context.getSystemService(android.hardware.display.DisplayManager::class.java)
        val listener =
            object : android.hardware.display.DisplayManager.DisplayListener {
                override fun onDisplayAdded(displayId: Int) {}

                override fun onDisplayRemoved(displayId: Int) {}

                override fun onDisplayChanged(displayId: Int) {
                    val rotation = previewView.display?.rotation ?: return
                    imageCapture?.targetRotation = rotation
                    videoCapture?.targetRotation = rotation
                }
            }
        dm?.registerDisplayListener(listener, android.os.Handler(android.os.Looper.getMainLooper()))
        onDispose { dm?.unregisterDisplayListener(listener) }
    }

    // --- Manual controls -> Camera2 -----------------------------------
    LaunchedEffect(
        settings.isManualMode,
        settings.aeAfLocked,
        settings.iso,
        settings.shutterSpeedNs,
        settings.focusDistance,
        settings.whiteBalanceMode,
        camera,
    ) {
        val cam = camera ?: return@LaunchedEffect
        runCatching {
            val control = Camera2CameraControl.from(cam.cameraControl)
            val b = CaptureRequestOptions.Builder()

            val manualExposure =
                settings.isManualMode &&
                    (settings.iso != null || settings.shutterSpeedNs != null)

            when {
                manualExposure -> {
                    // Full manual exposure: AE must be off for ISO / exposure time to apply.
                    b.setCaptureRequestOption(
                        android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE,
                        android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE_OFF,
                    )
                    settings.iso?.let {
                        b.setCaptureRequestOption(
                            android.hardware.camera2.CaptureRequest.SENSOR_SENSITIVITY,
                            it,
                        )
                    }
                    settings.shutterSpeedNs?.let {
                        b.setCaptureRequestOption(
                            android.hardware.camera2.CaptureRequest.SENSOR_EXPOSURE_TIME,
                            it,
                        )
                    }
                }
                settings.aeAfLocked -> {
                    // Keep auto-exposure metering but freeze the result.
                    b.setCaptureRequestOption(
                        android.hardware.camera2.CaptureRequest.CONTROL_AE_LOCK,
                        true,
                    )
                }
            }

            if (settings.isManualMode) {
                settings.focusDistance?.let {
                    b.setCaptureRequestOption(
                        android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE,
                        android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE_OFF,
                    )
                    b.setCaptureRequestOption(
                        android.hardware.camera2.CaptureRequest.LENS_FOCUS_DISTANCE,
                        it,
                    )
                }
                settings.whiteBalanceMode?.let {
                    b.setCaptureRequestOption(
                        android.hardware.camera2.CaptureRequest.CONTROL_AWB_MODE,
                        it,
                    )
                }
            } else if (settings.aeAfLocked) {
                b.setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.CONTROL_AF_TRIGGER,
                    android.hardware.camera2.CaptureRequest.CONTROL_AF_TRIGGER_IDLE,
                )
            }

            control.captureRequestOptions = b.build()
        }
    }

    // --- Time-lapse loop --------------------------------------------------
    LaunchedEffect(timelapseActive) {
        if (!timelapseActive) return@LaunchedEffect
        val intervalMs = appSettings.timelapseIntervalSeconds * 1000L
        var consecutiveErrors = 0
        while (isActive && timelapseActive) {
            kotlinx.coroutines.delay(intervalMs)
            if (!timelapseActive) break
            val ic = imageCapture ?: continue
            val name = "Fcam-timelapse-${System.currentTimeMillis()}.jpg"
            val opts =
                ImageCapture.OutputFileOptions.Builder(
                        context.contentResolver,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        },
                    )
                    .build()
            showFlash = true
            ic.takePicture(
                opts,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(r: ImageCapture.OutputFileResults) {
                        consecutiveErrors = 0
                        r.savedUri?.let { viewModel.setLastMedia(it, isVideo = false) }
                    }

                    override fun onError(e: ImageCaptureException) {
                        consecutiveErrors++
                        if (consecutiveErrors >= 3) {
                            timelapseActive = false
                            msg(R.string.snack_timelapse_autostopped)
                        }
                    }
                },
            )
        }
    }

    // --- Capture actions ------------------------------------------------
    fun playShutter() {
        if (appSettings.shutterSound && settings.shutterVolume > 0f) {
            mediaActionSound.play(MediaActionSound.SHUTTER_CLICK)
        }
    }

    fun capturePhoto() {
        val ic = imageCapture ?: return
        // Second tap during the self-timer countdown cancels it.
        if (isCapturing) {
            if (timerJob?.isActive == true) {
                timerJob?.cancel()
                timerJob = null
                isCapturing = false
            }
            return
        }
        isCapturing = true
        timerJob =
            scope.launch {
            if (settings.timerSeconds > 0 && external == null) {
                kotlinx.coroutines.delay(settings.timerSeconds * 1000L)
            }
            timerJob = null
            playShutter()
            showFlash = true

            if (external != null) {
                captureForExternal(context, ic, external, onExternalResult) { isCapturing = false }
                return@launch
            }

            val name = "Fcam-photo-${System.currentTimeMillis()}.jpg"
            val opts =
                ImageCapture.OutputFileOptions.Builder(
                        context.contentResolver,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Fcam pro")
                            }
                        },
                    )
                    .build()
            ic.takePicture(
                opts,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(r: ImageCapture.OutputFileResults) {
                        isCapturing = false
                        r.savedUri?.let { viewModel.setLastMedia(it, isVideo = false) }
                        msg(R.string.snack_photo_saved)
                    }

                    override fun onError(e: ImageCaptureException) {
                        isCapturing = false
                        msg(R.string.snack_photo_failed, e.message ?: "")
                    }
                },
            )
        }
    }

    fun toggleRecording() {
        val vc = videoCapture ?: return
        val current = recording
        if (current != null) {
            current.stop()
            recording = null
            if (appSettings.shutterSound) {
                mediaActionSound.play(MediaActionSound.STOP_VIDEO_RECORDING)
            }
            return
        }
        if (appSettings.shutterSound) mediaActionSound.play(MediaActionSound.START_VIDEO_RECORDING)

        // Always record to the shared MediaStore. For an external VIDEO_CAPTURE request
        // the resulting content URI is handed back to the caller as the result data,
        // which every well-behaved caller accepts (EXTRA_OUTPUT is advisory only).
        val name = "Fcam-video-${System.currentTimeMillis()}.mp4"
        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Fcam pro")
                }
            }
        val pending =
            vc.output.prepareRecording(
                context,
                MediaStoreOutputOptions.Builder(
                        context.contentResolver,
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    )
                    .setContentValues(values)
                    .build(),
            )

        val audioGranted =
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO,
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val withAudio = if (audioGranted) pending.withAudioEnabled() else pending
        if (!audioGranted) msg(R.string.perm_audio_denied_muted)

        recording =
            runCatching {
                    withAudio.start(ContextCompat.getMainExecutor(context)) { event ->
                        if (event is VideoRecordEvent.Finalize) {
                            recording = null
                            val ok = !event.hasError()
                            val uri = event.outputResults.outputUri
                            if (external != null) {
                                onExternalResult(ok, Intent().setData(uri))
                            } else if (ok) {
                                if (uri != null) viewModel.setLastMedia(uri, isVideo = true)
                                msg(R.string.snack_video_saved)
                            } else {
                                msg(R.string.snack_video_failed, event.error.toString())
                            }
                        }
                    }
                }
                .getOrElse {
                    msg(R.string.snack_video_failed, it.message ?: "")
                    null
                }
    }

    fun openGallery() {
        val lm = lastMedia
        val uri = lm?.uri
        val viewIntent =
            if (lm != null) {
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(lm.uri, if (lm.isVideo) "video/*" else "image/*")
                    .addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                    )
            } else {
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        "image/*",
                    )
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        val ok = runCatching { context.startActivity(viewIntent); true }.getOrDefault(false)
        if (!ok) {
            val fallback =
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_APP_GALLERY)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val ok2 =
                runCatching { context.startActivity(fallback); true }.getOrDefault(false)
            if (!ok2) msg(if (uri == null) R.string.snack_no_media else R.string.snack_open_failed)
        }
    }

    // --- Focus lock (tap to lock, tap again to release) ---------------
    fun onPreviewTap(offset: androidx.compose.ui.geometry.Offset) {
        val cam = camera ?: return
        // Manual focus distance overrides tap-to-focus.
        if (settings.isManualMode && settings.focusDistance != null) return
        if (focusLocked) {
            runCatching { cam.cameraControl.cancelFocusAndMetering() }
            focusLocked = false
            focusPoint = null
            return
        }
        val point = previewView.meteringPointFactory.createPoint(offset.x, offset.y)
        val action =
            FocusMeteringAction.Builder(
                    point,
                    FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE,
                )
                .disableAutoCancel()
                .build()
        runCatching {
            cam.cameraControl.startFocusAndMetering(action)
            focusPoint = offset
            focusLocked = true
        }
    }

    // --- QR overlay side effect ---------------------------------------
    val clipboard =
        remember {
            context.getSystemService(android.content.ClipboardManager::class.java)
        }

    // ============================ UI ============================
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { previewView },
                modifier =
                    Modifier.fillMaxSize()
                        .onSizeChanged { previewSize = it }
                        .pointerInput(camera, settings.isManualMode, settings.focusDistance, focusLocked) {
                            detectTapGestures { offset -> onPreviewTap(offset) }
                        }
                        .pointerInput(camera) {
                            detectTransformGestures { _, _, zoom, _ ->
                                if (zoom == 1f) return@detectTransformGestures
                                val cam = camera ?: return@detectTransformGestures
                                val max =
                                    cam.cameraInfo.zoomState.value?.maxZoomRatio
                                        ?: settings.currentLens?.capabilities?.maxZoomRatio
                                        ?: 1f
                                zoomRatio = (zoomRatio * zoom).coerceIn(1f, maxOf(1f, max))
                                runCatching { cam.cameraControl.setZoomRatio(zoomRatio) }
                            }
                        },
            )

            if (flashAlpha > 0f) {
                Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = flashAlpha)))
            }

            if (appSnapshot.gridLines) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 1.dp.toPx()
                    val c = Color.White.copy(alpha = 0.32f)
                    for (i in 1..2) {
                        val x = size.width * i / 3f
                        val y = size.height * i / 3f
                        drawLine(c, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), stroke)
                        drawLine(c, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), stroke)
                    }
                }
            }

            focusPoint?.let { fp ->
                val halfPx = with(density) { 34.dp.toPx() }
                val w = previewSize.width.toFloat().coerceAtLeast(1f)
                val h = previewSize.height.toFloat().coerceAtLeast(1f)
                val cx = fp.x.coerceIn(halfPx, (w - halfPx).coerceAtLeast(halfPx))
                val cy = fp.y.coerceIn(halfPx, (h - halfPx).coerceAtLeast(halfPx))
                val leftDp = with(density) { (cx - halfPx).toDp() }
                val topDp = with(density) { (cy - halfPx).toDp() }
                Box(
                    modifier =
                        Modifier.offset(x = leftDp, y = topDp)
                            .size(68.dp)
                            .border(
                                1.5.dp,
                                if (focusLocked) MaterialTheme.colorScheme.primary else Color.White,
                                RoundedCornerShape(4.dp),
                            )
                )
                if (focusLocked) {
                    Text(
                        text = stringResource(R.string.af_locked),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.offset(x = leftDp, y = topDp + 70.dp),
                    )
                }
            }

            if (detectedQr != null) {
                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(top = 72.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val qr = detectedQr!!
                    ElevatedAssistChip(
                        onClick = {
                            val isUrl = qr.startsWith("http://") || qr.startsWith("https://")
                            if (isUrl) {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(qr))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            } else {
                                clipboard?.setPrimaryClip(
                                    android.content.ClipData.newPlainText("QR", qr)
                                )
                                msg(R.string.snack_qr_copied)
                            }
                            detectedQr = null
                        },
                        label = { Text(qr.take(28)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.QrCode,
                                contentDescription =
                                    stringResource(R.string.cd_qr_detected),
                            )
                        },
                    )
                }
            }

            CameraOverlay(
                viewModel = viewModel,
                settings = settings,
                availableLenses = availableLenses,
                profiles = profiles,
                external = external,
                bgRunning = bgRunning,
                timelapseActive = timelapseActive,
                isCapturing = isCapturing,
                isRecording = recording != null,
                gridOn = appSnapshot.gridLines,
                mediaThumb = thumb,
                hasMedia = lastMedia != null,
                batteryPct = batteryPct,
                panelCollapsed = appSnapshot.panelCollapsed,
                panelGravity = appSnapshot.panelGravity,
                onSetPanelCollapsed = { appSettings.panelCollapsed = it },
                onSetPanelGravity = { appSettings.panelGravity = it },
                onToggleGrid = { appSettings.gridLines = !appSettings.gridLines },
                onOpenGallery = ::openGallery,
                onCapturePhoto = ::capturePhoto,
                onToggleRecording = ::toggleRecording,
                onToggleTimelapse = {
                    if (!timelapseActive) {
                        timelapseActive = true
                        msg(
                            R.string.snack_timelapse_started,
                            appSettings.timelapseIntervalSeconds,
                        )
                    } else {
                        timelapseActive = false
                        msg(R.string.snack_timelapse_stopped)
                    }
                },
                onSlowMo = { msg(R.string.snack_slowmo_unsupported) },
                onPanorama = { msg(R.string.snack_panorama_unsupported) },
                onToggleBackground = {
                    if (bgRunning) {
                        BackgroundCameraService.stop(context)
                        msg(R.string.snack_bg_record_stopped)
                    } else if (noCameraAvailable) {
                        msg(R.string.snack_bg_record_unsupported)
                    } else if (
                        ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.CAMERA,
                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        msg(R.string.snack_bg_record_no_permission)
                    } else {
                        BackgroundCameraService.start(context, lensFront = settings.isFrontCamera)
                        msg(R.string.snack_bg_record_started)
                    }
                },
                onOpenSettings = onOpenSettings,
                onCancelExternal = { onExternalResult(false, null) },
            )
        }
    }
}

private fun captureForExternal(
    context: android.content.Context,
    ic: ImageCapture,
    external: ExternalCaptureSpec,
    onResult: (Boolean, Intent?) -> Unit,
    onDone: () -> Unit,
) {
    val target = external.outputUri
    if (target != null) {
        val stream = runCatching { context.contentResolver.openOutputStream(target) }.getOrNull()
        if (stream == null) {
            onDone()
            onResult(false, null)
            return
        }
        val opts = ImageCapture.OutputFileOptions.Builder(stream).build()
        ic.takePicture(
            opts,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(r: ImageCapture.OutputFileResults) {
                    runCatching { stream.close() }
                    onDone()
                    onResult(true, null)
                }

                override fun onError(e: ImageCaptureException) {
                    runCatching { stream.close() }
                    onDone()
                    onResult(false, null)
                }
            },
        )
    } else {
        ic.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: androidx.camera.core.ImageProxy) {
                    val bmp =
                        runCatching {
                                val buffer = image.planes[0].buffer
                                val bytes = ByteArray(buffer.remaining())
                                buffer.get(bytes)
                                // ACTION_IMAGE_CAPTURE without EXTRA_OUTPUT expects a small
                                // thumbnail in the result; a full-size bitmap overflows the
                                // Binder transaction limit and fails the caller.
                                val opt =
                                    android.graphics.BitmapFactory.Options().apply {
                                        inJustDecodeBounds = true
                                    }
                                android.graphics.BitmapFactory.decodeByteArray(
                                    bytes, 0, bytes.size, opt
                                )
                                var sample = 1
                                while (
                                    opt.outWidth / sample > 1024 || opt.outHeight / sample > 1024
                                ) sample *= 2
                                android.graphics.BitmapFactory.decodeByteArray(
                                    bytes,
                                    0,
                                    bytes.size,
                                    android.graphics.BitmapFactory.Options().apply {
                                        inSampleSize = sample
                                    },
                                )
                            }
                            .getOrNull()
                    image.close()
                    onDone()
                    if (bmp != null) {
                        onResult(true, Intent().putExtra("data", bmp))
                    } else {
                        onResult(false, null)
                    }
                }

                override fun onError(e: ImageCaptureException) {
                    onDone()
                    onResult(false, null)
                }
            },
        )
    }
}

private const val TAG = "CameraScreen"
