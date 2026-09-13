package com.oriyu90.fcampro.ui

import android.content.ContentValues
import android.content.Intent
import android.content.res.Configuration
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CaptureRequest
import android.media.MediaActionSound
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Range
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraFilter
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.movableContentOf
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.oriyu90.fcampro.R
import com.oriyu90.fcampro.camera.PanoEstimate
import com.oriyu90.fcampro.camera.PanoramaStitcher
import com.oriyu90.fcampro.camera.RawSupport
import com.oriyu90.fcampro.camera.SlowMoFactors
import com.oriyu90.fcampro.camera.SlowMoProcessor
import com.oriyu90.fcampro.camera.YuvConverter
import com.oriyu90.fcampro.core.AppSettings
import com.oriyu90.fcampro.data.CameraProfile
import com.oriyu90.fcampro.services.BackgroundCameraService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs

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
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val availableLenses by viewModel.availableLenses.collectAsStateWithLifecycle()
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    val bgRunning by BackgroundCameraService.running.collectAsStateWithLifecycle()
    val noCameraAvailable by viewModel.noCameraAvailable.collectAsStateWithLifecycle()
    val lastMedia by viewModel.lastMedia.collectAsStateWithLifecycle()
    val currentOrientation = LocalConfiguration.current.orientation

    // One PreviewView and one movable AndroidView owner survive mode and orientation
    // changes. Reusing the raw View from separate AndroidView call sites caused
    // parent races and frozen TextureViews on Sony's Android 11 compositor.
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            // The UI deliberately keeps the selected 4:3 / 16:9 viewfinder
            // landscape-shaped in both device orientations. FIT_CENTER uses the
            // rotation-aware stream ratio and pillarboxes it into a narrow,
            // portrait-shaped TextureView after a device rotation. FILL_CENTER
            // keeps the live surface attached to the immutable viewfinder frame;
            // only the capture/display rotation changes.
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var previewUseCase by remember { mutableStateOf<Preview?>(null) }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var qrAnalyzer by remember { mutableStateOf<QrCodeAnalyzer?>(null) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraReady by remember { mutableStateOf(false) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    // Upper bound for pinch zoom. Refreshed from the bound Camera after every
    // (re)bind; falls back to the selected lens capabilities until then.
    var maxZoomRatio by remember { mutableFloatStateOf(1f) }

    var detectedQr by remember { mutableStateOf<String?>(null) }
    var lastQrAt by remember { mutableStateOf(0L) }
    var timelapseActive by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    // Slow-motion post-processing (timestamp stretch) runs after recording stops.
    var slowMoProcessing by remember { mutableStateOf(false) }
    // Panorama sweep session: frames accumulate here until finish/cancel.
    var panoActive by remember { mutableStateOf(false) }
    var panoFrames by remember { mutableStateOf<List<android.graphics.Bitmap>>(emptyList()) }
    var panoFrameInFlight by remember { mutableStateOf(false) }
    var panoAzimuth by remember { mutableStateOf<Float?>(null) }
    var currentAzimuth by remember { mutableStateOf<Float?>(null) }
    var lastPanoCaptureAt by remember { mutableStateOf(0L) }
    // Pro settings item selected via the icon row/strip (null = hint).
    var proItem by remember { mutableStateOf<ProItem?>(null) }
    // Low-RAM devices stitch smaller, fewer frames to avoid OOM kills.
    val memoryClassMb =
        remember {
            context.getSystemService(android.app.ActivityManager::class.java)?.memoryClass ?: 256
        }
    val panoMaxFrames = PanoEstimate.maxFrames(memoryClassMb)

    val appSnapshot by appSettings.state.collectAsStateWithLifecycle()
    val modeBar =
        remember(appSnapshot.modeBar) {
            ModeBarOrder.sanitize(
                appSnapshot.modeBar.mapNotNull { raw ->
                    runCatching { CameraMode.valueOf(raw) }.getOrNull()
                }
            )
        }
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
                // BATTERY_CHANGED is emitted by a privileged system process. On
                // several Sony builds NOT_EXPORTED blocks subsequent updates even
                // though the initial sticky value is returned.
                androidx.core.content.ContextCompat.RECEIVER_EXPORTED,
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
        settings.saveFormat,
        bgRunning,
        previewView,
    ) {
        val provider = cameraProvider ?: return@LaunchedEffect
        val lens = settings.currentLens ?: return@LaunchedEffect
        cameraReady = false

        // The background recording service owns the physical camera while it runs.
        if (bgRunning) {
            runCatching { provider.unbindAll() }
            camera = null
            return@LaunchedEffect
        }

        runCatching { provider.unbindAll() }
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
                                    lens.logicalCameraId
                            }
                        if (match.isNotEmpty()) match else infos
                    }
                )
                .build()

        val previewBuilder = Preview.Builder().setResolutionSelector(resolutionSelector)
        // Sub-cameras hidden behind a logical multi-camera (e.g. Galaxy
        // telephoto) are routed via the physical id on every use case.
        lens.physicalCameraId?.let { pid ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Camera2Interop.Extender(previewBuilder).setPhysicalCameraId(pid)
            }
        }

        val slowMoHs =
            if (settings.cameraMode == CameraMode.SLOWMO) {
                settings.currentLens?.capabilities?.highSpeedVideo
            } else {
                null
            }

        fun bindVideoUseCases(useHighSpeed: Boolean): Camera {
            val pb = Preview.Builder().setResolutionSelector(resolutionSelector)
            val qualities =
                if (settings.cameraMode == CameraMode.SLOWMO) {
                    // High-speed sensors top out at modest resolutions; prefer HD.
                    listOf(Quality.HD, Quality.SD)
                } else {
                    listOf(Quality.FHD, Quality.HD, Quality.SD)
                }
            val recorder =
                Recorder.Builder()
                    .setQualitySelector(QualitySelector.fromOrderedList(qualities))
                    .build()
            val vb = VideoCapture.Builder(recorder)
            lens.physicalCameraId?.let { pid ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                Camera2Interop.Extender(pb).setPhysicalCameraId(pid)
                Camera2Interop.Extender(vb).setPhysicalCameraId(pid)
            }
            }
            if (useHighSpeed && slowMoHs != null) {
                // Request a fixed high frame rate on both streams. Strict HALs
                // reject the combination; callers fall back to a plain bind.
                val fpsRange = Range(slowMoHs.maxFps, slowMoHs.maxFps)
                Camera2Interop.Extender(vb)
                    .setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                        fpsRange,
                    )
                Camera2Interop.Extender(pb)
                    .setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                        fpsRange,
                    )
            }
            val pv =
                pb.build().also {
                    previewUseCase = it
                    it.targetRotation = previewView.display?.rotation ?: android.view.Surface.ROTATION_0
                    it.surfaceProvider = previewView.surfaceProvider
                }
            val vc = vb.build().also {
                it.targetRotation = previewView.display?.rotation ?: android.view.Surface.ROTATION_0
            }
            videoCapture = vc
            return provider.bindToLifecycle(lifecycleOwner, selector, pv, vc)
        }

        // Collapse rapid lens/mode taps into the final selection: without this
        // pause, rebind races the previous session teardown and setup fails.
        // LaunchedEffect cancellation keeps only the latest key set alive.
        kotlinx.coroutines.delay(250)

        var attempt = 0
        var boundOk = false
        while (!boundOk && attempt < 3) {
        try {
            camera =
                if (settings.cameraMode == CameraMode.VIDEO ||
                    settings.cameraMode == CameraMode.SLOWMO
                ) {
                    // Tear down photo-mode use cases so the ML Kit scanner and the stale
                    // ImageCapture reference are not retained while recording.
                    qrAnalyzer?.release()
                    qrAnalyzer = null
                    imageCapture = null
                    if (settings.cameraMode == CameraMode.SLOWMO && slowMoHs != null) {
                        // High-fps bind first; strict HALs that reject the fps
                        // range still get a working preview + normal recording
                        // instead of a dead camera.
                        runCatching { bindVideoUseCases(useHighSpeed = true) }
                            .getOrElse { bindVideoUseCases(useHighSpeed = false) }
                    } else {
                        bindVideoUseCases(useHighSpeed = false)
                    }
                } else {
                    videoCapture = null
                    // RAW stills ride on CameraX 1.5 DNG output, which is only
                    // valid in plain PHOTO mode: panorama uses in-memory frames,
                    // timelapse/OTHERS and external requests expect plain JPEG.
                    val stillOutputFormat =
                        if (external == null &&
                            settings.cameraMode == CameraMode.PHOTO &&
                            settings.saveFormat != SaveFormat.JPEG &&
                            lens.capabilities.rawCapability?.supported == true
                        ) {
                            when (settings.saveFormat) {
                                SaveFormat.RAW -> ImageCapture.OUTPUT_FORMAT_RAW
                                else -> ImageCapture.OUTPUT_FORMAT_RAW_JPEG
                            }
                        } else {
                            ImageCapture.OUTPUT_FORMAT_JPEG
                        }
                    fun buildPreview(): Preview =
                        previewBuilder.build().also {
                            previewUseCase = it
                            it.targetRotation = previewView.display?.rotation ?: android.view.Surface.ROTATION_0
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                    fun buildStillCapture(): ImageCapture {
                        val icBuilder =
                            ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                // Sweep frames must share exposure: a per-frame flash
                                // would poison the stitch, so panorama never flashes.
                                .setFlashMode(
                                    if (settings.cameraMode == CameraMode.PANORAMA) {
                                        ImageCapture.FLASH_MODE_OFF
                                    } else {
                                        settings.flashMode
                                    }
                                )
                                .setResolutionSelector(resolutionSelector)
                                .setOutputFormat(stillOutputFormat)
                        lens.physicalCameraId?.let { pid ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                Camera2Interop.Extender(icBuilder).setPhysicalCameraId(pid)
                            }
                        }
                        return icBuilder.build().also {
                            it.targetRotation = previewView.display?.rotation ?: android.view.Surface.ROTATION_0
                        }
                    }
                    fun bindFull(pv: Preview, ic: ImageCapture): Camera {
                        if (settings.cameraMode == CameraMode.PANORAMA) {
                            // No QR scanning while sweeping; the analyzer would only
                            // add latency between panorama frames.
                            qrAnalyzer?.release()
                            qrAnalyzer = null
                            return provider.bindToLifecycle(lifecycleOwner, selector, pv, ic)
                        }
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
                        return provider.bindToLifecycle(lifecycleOwner, selector, pv, ic, analysis)
                    }
                    // Tiered fallback: exotic HALs may reject the analyzer or even
                    // the still stream — degrade to preview-only with a working
                    // viewfinder rather than a dead camera.
                    runCatching {
                        val pv = buildPreview()
                        val ic = buildStillCapture()
                        imageCapture = ic
                        bindFull(pv, ic)
                    }.getOrElse {
                        qrAnalyzer?.release()
                        qrAnalyzer = null
                        runCatching {
                            val pv = buildPreview()
                            val ic = buildStillCapture()
                            imageCapture = ic
                            provider.bindToLifecycle(lifecycleOwner, selector, pv, ic)
                        }.getOrElse {
                            imageCapture = null
                            provider.bindToLifecycle(
                                lifecycleOwner,
                                selector,
                                buildPreview(),
                            )
                        }
                    }
                }
            boundOk = true
        } catch (e: Exception) {
            attempt++
            camera = null
            if (attempt >= 3) {
                Log.e(TAG, "bind failed", e)
                msg(R.string.snack_camera_setup_failed, e.message ?: "")
            } else {
                // Give the HAL time to finish tearing down before retrying.
                kotlinx.coroutines.delay(400)
            }
        }
        }

        // Startup / rebind contract: both the UI state and the physical camera
        // start at 1.0x. ProcessCameraProvider may hand back the same Camera
        // instance for the same selector, so an explicit setZoomRatio is required
        // — resetting only the local state would leave the lens zoomed while the
        // UI claims 1.0x.
        val bound = camera
        if (bound != null) {
            zoomRatio = 1f
            maxZoomRatio =
                (bound.cameraInfo.zoomState.value?.maxZoomRatio
                    ?: lens.capabilities.maxZoomRatio).coerceAtLeast(1f)
            runCatching { bound.cameraControl.setZoomRatio(1f) }
            // Recorder and ImageCapture surfaces become usable shortly after
            // bind returns on some legacy HALs. Blocking the shutter during
            // this short hand-off prevents zero-frame recordings.
            kotlinx.coroutines.delay(350)
            cameraReady = true
        } else {
            zoomRatio = 1f
            maxZoomRatio = lens.capabilities.maxZoomRatio.coerceAtLeast(1f)
        }
    }

    // Keep still/video output orientation correct while the Activity is not recreated
    // on rotation (android:configChanges).
    DisposableEffect(previewView) {
        val dm = context.getSystemService(android.hardware.display.DisplayManager::class.java)
        val listener =
            object : android.hardware.display.DisplayManager.DisplayListener {
                override fun onDisplayAdded(displayId: Int) {}

                override fun onDisplayRemoved(displayId: Int) {}

                override fun onDisplayChanged(displayId: Int) {
                    val rotation = previewView.display?.rotation ?: return
                    previewUseCase?.targetRotation = rotation
                    imageCapture?.targetRotation = rotation
                    videoCapture?.targetRotation = rotation
                    previewView.post {
                        previewView.requestLayout()
                        previewView.invalidate()
                    }
                }
            }
        dm?.registerDisplayListener(listener, android.os.Handler(android.os.Looper.getMainLooper()))
        onDispose { dm?.unregisterDisplayListener(listener) }
    }

    // Some Android 11 devices update Configuration before DisplayManager emits its
    // callback. Refresh transforms from both signals without tearing down the camera.
    LaunchedEffect(currentOrientation, previewView) {
        kotlinx.coroutines.delay(32)
        val rotation = previewView.display?.rotation ?: android.view.Surface.ROTATION_0
        previewUseCase?.targetRotation = rotation
        imageCapture?.targetRotation = rotation
        videoCapture?.targetRotation = rotation
        previewView.requestLayout()
        previewView.invalidate()
    }

    // --- Manual controls -> Camera2 -----------------------------------
    LaunchedEffect(
        settings.isManualMode,
        settings.aeAfLocked,
        settings.iso,
        settings.shutterSpeedNs,
        settings.focusDistance,
        settings.whiteBalanceMode,
        settings.exposureCompensation,
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

            if (!manualExposure) {
                // AE is running: apply exposure compensation (neutral 0 is a no-op).
                val evRange = settings.currentLens?.capabilities?.exposureCompRange
                if (evRange != null) {
                    b.setCaptureRequestOption(
                        android.hardware.camera2.CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
                        ExposureComp.clamp(settings.exposureCompensation, evRange),
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

    fun jpegOutputOptions(): ImageCapture.OutputFileOptions {
        val name = "Fcam-photo-${System.currentTimeMillis()}.jpg"
        return ImageCapture.OutputFileOptions.Builder(
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
    }

    fun rawOutputOptions(): ImageCapture.OutputFileOptions {
        val name = "Fcam-raw-${System.currentTimeMillis()}${RawSupport.DNG_EXTENSION}"
        return ImageCapture.OutputFileOptions.Builder(
                context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, RawSupport.DNG_MIME)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Fcam pro")
                    }
                },
            )
            .build()
    }

    /** Order-agnostic saver for single + dual (RAW_JPEG) capture callbacks. */
    fun onStillSaved(uri: Uri?) {
        val u = uri ?: return
        val mime = runCatching { context.contentResolver.getType(u) }.getOrNull()
        viewModel.setLastMedia(u, isVideo = false)
        if (mime == RawSupport.DNG_MIME) msg(R.string.snack_raw_saved)
        else msg(R.string.snack_photo_saved)
    }

    fun capturePhoto() {
        // While the background service owns the camera, this screen has no bound
        // use cases (camera == null). Guard explicitly so a tap can never start
        // a self-timer that silently does nothing.
        if (bgRunning) return
        val ic = imageCapture ?: run {
            // Preview-only fallback bind (tier 3): tell the user instead of
            // silently swallowing the tap.
            msg(R.string.snack_photo_failed, "")
            return
        }
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

            // Mirrors the bind contract: RAW output only in plain PHOTO mode
            // on a RAW-capable lens; every other path stays plain JPEG.
            val rawActive =
                settings.cameraMode == CameraMode.PHOTO &&
                    settings.saveFormat != SaveFormat.JPEG &&
                    settings.currentLens?.capabilities?.rawCapability?.supported == true
            val executor = ContextCompat.getMainExecutor(context)
            if (!rawActive) {
                ic.takePicture(
                    jpegOutputOptions(),
                    executor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(r: ImageCapture.OutputFileResults) {
                            isCapturing = false
                            onStillSaved(r.savedUri)
                        }

                        override fun onError(e: ImageCaptureException) {
                            isCapturing = false
                            msg(R.string.snack_photo_failed, e.message ?: "")
                        }
                    },
                )
                return@launch
            }
            if (settings.saveFormat == SaveFormat.RAW) {
                ic.takePicture(
                    rawOutputOptions(),
                    executor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(r: ImageCapture.OutputFileResults) {
                            isCapturing = false
                            onStillSaved(r.savedUri)
                        }

                        override fun onError(e: ImageCaptureException) {
                            isCapturing = false
                            msg(R.string.snack_photo_failed, e.message ?: "")
                        }
                    },
                )
                return@launch
            }
            // JPEG + RAW: the callback fires once per file, in either order.
            var pending = 2
            ic.takePicture(
                rawOutputOptions(),
                jpegOutputOptions(),
                executor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(r: ImageCapture.OutputFileResults) {
                        onStillSaved(r.savedUri)
                        pending--
                        if (pending <= 0) isCapturing = false
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
        if (bgRunning) return
        val vc = videoCapture ?: run {
            msg(R.string.snack_video_failed, "")
            return
        }
        val current = recording
        if (current != null) {
            current.stop()
            recording = null
            if (appSettings.shutterSound) {
                mediaActionSound.play(MediaActionSound.STOP_VIDEO_RECORDING)
            }
            return
        }
        if (settings.cameraMode == CameraMode.SLOWMO && !viewModel.isSlowMotionSupported()) {
            msg(R.string.snack_slowmo_unsupported)
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
                            } else if (!ok) {
                                msg(R.string.snack_video_failed, event.error.toString())
                            } else if (settings.cameraMode == CameraMode.SLOWMO) {
                                // Stretch the high-fps recording into slow motion.
                                slowMoProcessing = true
                                msg(R.string.snack_slowmo_processing)
                                scope.launch(Dispatchers.IO) {
                                    val fps =
                                        settings.currentLens?.capabilities?.highSpeedVideo?.maxFps
                                            ?: SlowMoFactors.BASE_FPS
                                    val out =
                                        SlowMoProcessor.convertToSlowMotion(
                                            context,
                                            uri,
                                            fps,
                                            "Fcam-slowmo-${System.currentTimeMillis()}.mp4",
                                        )
                                    slowMoProcessing = false
                                    if (out != null) {
                                        viewModel.setLastMedia(out, isVideo = true)
                                        msg(R.string.snack_video_saved)
                                    } else {
                                        msg(R.string.snack_video_failed, "")
                                    }
                                }
                            } else {
                                viewModel.setLastMedia(uri, isVideo = true)
                                msg(R.string.snack_video_saved)
                            }
                        }
                    }
                }
                .getOrElse {
                    msg(R.string.snack_video_failed, it.message ?: "")
                    null
                }
    }

    // --- Panorama sweep session --------------------------------------
    fun recyclePanoFrames() {
        panoFrames.forEach { runCatching { it.recycle() } }
        panoFrames = emptyList()
        panoAzimuth = null
    }

    fun cancelPanorama() {
        panoActive = false
        panoFrameInFlight = false
        recyclePanoFrames()
    }

    fun finishPanorama() {
        if (!panoActive) return
        panoActive = false
        val frames = panoFrames
        panoFrames = emptyList()
        panoAzimuth = null
        if (frames.size < 2) {
            if (frames.isNotEmpty()) msg(R.string.snack_panorama_need_frames)
            frames.forEach { runCatching { it.recycle() } }
            return
        }
        isCapturing = true
        scope.launch(Dispatchers.Default) {
            val stitched = runCatching { PanoramaStitcher.stitch(frames) }.getOrNull()
            frames.forEach { runCatching { it.recycle() } }
            if (stitched == null) {
                isCapturing = false
                msg(R.string.snack_panorama_failed, "")
                return@launch
            }
            val uri =
                saveBitmapToGallery(
                    context,
                    stitched,
                    "Fcam-pano-${System.currentTimeMillis()}.jpg",
                )
            runCatching { stitched.recycle() }
            isCapturing = false
            if (uri != null) {
                viewModel.setLastMedia(uri, isVideo = false)
                msg(R.string.snack_photo_saved)
            } else {
                msg(R.string.snack_panorama_failed, "")
            }
        }
    }

    fun capturePanoFrame(ic: ImageCapture) {
        if (panoFrameInFlight || panoFrames.size >= panoMaxFrames) return
        panoFrameInFlight = true
        ic.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    // Full-res YUV->JPEG conversion runs off the main thread:
                    // on high-megapixel sensors it would otherwise jank or ANR.
                    scope.launch(Dispatchers.Default) {
                        val bmp = YuvConverter.imageProxyToBitmap(image)
                        runCatching { image.close() }
                        val frame =
                            bmp?.let {
                                PanoramaStitcher.downscaleToMaxWidth(
                                    it,
                                    PanoEstimate.maxFrameWidth(memoryClassMb),
                                )
                            }
                        if (frame != null && frame !== bmp) bmp.recycle()
                        if (frame != null) {
                            panoFrames = panoFrames + frame
                            currentAzimuth?.let { panoAzimuth = it }
                            lastPanoCaptureAt = System.currentTimeMillis()
                            if (panoFrames.size >= panoMaxFrames) finishPanorama()
                        }
                        panoFrameInFlight = false
                    }
                }

                override fun onError(e: ImageCaptureException) {
                    panoFrameInFlight = false
                    msg(R.string.snack_photo_failed, e.message ?: "")
                }
            },
        )
    }

    fun startPanorama() {
        if (bgRunning || panoActive) return
        val ic = imageCapture ?: run {
            msg(R.string.snack_panorama_failed, "")
            return
        }
        playShutter()
        panoActive = true
        capturePanoFrame(ic)
    }

    fun togglePanorama() {
        if (bgRunning || isCapturing || slowMoProcessing) return
        if (panoActive) finishPanorama() else startPanorama()
    }

    // --- Panorama yaw tracking (rotation vector; no permission needed) ---
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    val hasRotationSensor =
        remember(sensorManager) {
            sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null
        }
    if (panoActive) {
        DisposableEffect(Unit) {
            val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            val listener =
                object : SensorEventListener {
                    override fun onSensorChanged(e: SensorEvent) {
                        val r = FloatArray(9)
                        val o = FloatArray(3)
                        SensorManager.getRotationMatrixFromVector(r, e.values)
                        SensorManager.getOrientation(r, o)
                        currentAzimuth = Math.toDegrees(o[0].toDouble()).toFloat()
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }
            if (sensor != null) {
                sensorManager?.registerListener(
                    listener,
                    sensor,
                    SensorManager.SENSOR_DELAY_UI,
                )
            }
            onDispose { sensorManager?.unregisterListener(listener) }
        }
    }

    LaunchedEffect(currentAzimuth) {
        val az = currentAzimuth ?: return@LaunchedEffect
        if (!panoActive || panoFrameInFlight) return@LaunchedEffect
        val lastAz = panoAzimuth
        if (lastAz == null) {
            panoAzimuth = az
            return@LaunchedEffect
        }
        val ic = imageCapture ?: return@LaunchedEffect
        if (panoFrames.size in 1 until panoMaxFrames &&
            abs(PanoEstimate.yawDelta(lastAz, az)) >= PanoEstimate.CAPTURE_STEP_DEG &&
            System.currentTimeMillis() - lastPanoCaptureAt >= PanoEstimate.MIN_CAPTURE_INTERVAL_MS
        ) {
            capturePanoFrame(ic)
        }
    }

    // Devices without a rotation-vector sensor fall back to timer captures so
    // panorama stays usable instead of stalling after the first frame.
    LaunchedEffect(panoActive, hasRotationSensor) {
        if (!panoActive || hasRotationSensor) return@LaunchedEffect
        while (isActive && panoActive) {
            kotlinx.coroutines.delay(PanoEstimate.FALLBACK_CAPTURE_INTERVAL_MS)
            if (!panoActive || panoFrameInFlight) continue
            val ic = imageCapture ?: continue
            if (panoFrames.size in 1 until panoMaxFrames &&
                System.currentTimeMillis() - lastPanoCaptureAt >=
                    PanoEstimate.FALLBACK_CAPTURE_INTERVAL_MS
            ) {
                capturePanoFrame(ic)
            }
        }
    }

    // Leaving panorama mode discards the in-progress sweep.
    LaunchedEffect(settings.cameraMode) {
        if (settings.cameraMode != CameraMode.PANORAMA && panoActive) cancelPanorama()
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

    // --- Focus lock (tap locked box to release, tap elsewhere to refocus) ---
    fun onPreviewTap(offset: androidx.compose.ui.geometry.Offset) {
        val cam = camera ?: return
        // Manual focus distance overrides tap-to-focus.
        if (settings.isManualMode && settings.focusDistance != null) return
        val halfPx = with(density) { 34.dp.toPx() }
        val w = previewSize.width.toFloat()
        val h = previewSize.height.toFloat()
        val fp = focusPoint
        if (focusLocked && fp != null &&
            ZoomRatios.isTapInFocusBox(offset.x, offset.y, fp.x, fp.y, halfPx, w, h)
        ) {
            runCatching { cam.cameraControl.cancelFocusAndMetering() }
            focusLocked = false
            focusPoint = null
            return
        }
        if (focusLocked) {
            // Locked but tapped outside the box: move the lock there instead
            // of dropping it.
            runCatching { cam.cameraControl.cancelFocusAndMetering() }
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

    // Zoom pill "tap to reset": restores 1.0x on both UI state and camera.
    fun resetZoom() {
        zoomRatio = 1f
        camera?.let { runCatching { it.cameraControl.setZoomRatio(1f) } }
    }

    // --- QR overlay side effect ---------------------------------------
    val clipboard =
        remember {
            context.getSystemService(android.content.ClipboardManager::class.java)
        }

    fun toggleTimelapse() {
        if (!timelapseActive) {
            timelapseActive = true
            msg(R.string.snack_timelapse_started, appSettings.timelapseIntervalSeconds)
        } else {
            timelapseActive = false
            msg(R.string.snack_timelapse_stopped)
        }
    }

    fun toggleBackground() {
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
            BackgroundCameraService.start(
                context,
                lensFront = settings.isFrontCamera,
                cameraId = settings.currentLens?.logicalCameraId,
                physicalCameraId = settings.currentLens?.physicalCameraId,
                targetRotation =
                    previewView.display?.rotation ?: android.view.Surface.ROTATION_0,
            )
            msg(R.string.snack_bg_record_started)
        }
    }

    // ============================ UI ============================
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val movablePreview =
            remember(previewView) {
                movableContentOf<Modifier> { hostModifier ->
                    AndroidView(factory = { previewView }, modifier = hostModifier)
                }
            }
        @Composable
        fun PreviewSurface(modifier: Modifier) {
            movablePreview(
                modifier
                        .onSizeChanged { previewSize = it }
                        .pointerInput(camera, settings.isManualMode, settings.focusDistance, focusLocked) {
                            detectTapGestures { offset -> onPreviewTap(offset) }
                        }
                        .pointerInput(camera, settings.cameraMode) {
                            detectTransformGestures { _, _, zoom, _ ->
                                // Fixed framing while sweeping a panorama; zoom would
                                // invalidate the assumed frame overlap.
                                if (zoom == 1f || settings.cameraMode == CameraMode.PANORAMA) {
                                    return@detectTransformGestures
                                }
                                val cam = camera ?: return@detectTransformGestures
                                // Refresh the upper bound when the camera reports one;
                                // otherwise keep the post-bind fallback.
                                cam.cameraInfo.zoomState.value?.maxZoomRatio?.let {
                                    maxZoomRatio = it.coerceAtLeast(1f)
                                }
                                zoomRatio =
                                    ZoomRatios.next(zoomRatio, zoom, maxZoomRatio)
                                runCatching { cam.cameraControl.setZoomRatio(zoomRatio) }
                            }
                        }
            )
        }

        @Composable
        fun PreviewDecor() {
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

            if (settings.cameraMode == CameraMode.PANORAMA) {
                PanoramaGuideFrame()
            }
        }

        // Pro mode shrinks the preview onto a black stage with edge margins
        // (Sony-style); the AndroidView node itself stays at this one call
        // site so no reparenting crash can occur — only containers change.
        val proMode =
            external == null &&
                settings.isManualMode &&
                (settings.cameraMode == CameraMode.PHOTO ||
                    settings.cameraMode == CameraMode.VIDEO)
        val cfg = LocalConfiguration.current
        val proLandscape = cfg.orientation == Configuration.ORIENTATION_LANDSCAPE
        val compact = cfg.smallestScreenWidthDp < 600
        val streamAspect =
            if (settings.aspectRatio == AspectRatio.RATIO_16_9) 16f / 9f else 4f / 3f

        val shared =
            SharedActions(
                viewModel = viewModel,
                settings = settings,
                availableLenses = availableLenses,
                profiles = profiles,
                bgRunning = bgRunning,
                timelapseActive = timelapseActive,
                isCapturing = isCapturing || slowMoProcessing || !cameraReady,
                isRecording = recording != null,
                gridOn = appSnapshot.gridLines,
                mediaThumb = thumb,
                hasMedia = lastMedia != null,
                batteryPct = batteryPct,
                zoomRatio = zoomRatio,
                maxZoom = maxZoomRatio,
                onResetZoom = ::resetZoom,
                onToggleGrid = { appSettings.gridLines = !appSettings.gridLines },
                onOpenGallery = ::openGallery,
                onCapturePhoto = ::capturePhoto,
                onToggleRecording = ::toggleRecording,
                onToggleTimelapse = ::toggleTimelapse,
                panoActive = panoActive,
                panoCount = panoFrames.size,
                panoMax = panoMaxFrames,
                onTogglePanorama = ::togglePanorama,
                onCancelPanorama = ::cancelPanorama,
                onToggleBackground = ::toggleBackground,
                onOpenSettings = onOpenSettings,
                modeBar = modeBar,
                onSetModeBar = { updated -> appSettings.modeBar = updated.map { it.name } },
            )

        @Composable
        fun ProPreviewBox(modifier: Modifier) {
            Box(modifier) {
                PreviewSurface(Modifier.fillMaxSize())
                PreviewDecor()
                ProStatusTexts(shared, Modifier.align(Alignment.TopStart))
                ProLensTexts(shared, Modifier.align(Alignment.CenterEnd))
            }
        }

        /** Fits one immutable-aspect preview frame inside both width and height limits. */
        @Composable
        fun AspectFitPreviewFrame(
            modifier: Modifier,
            alignment: Alignment = Alignment.Center,
            content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
        ) {
            BoxWithConstraints(modifier = modifier, contentAlignment = alignment) {
                val widthFromHeight = maxHeight * streamAspect
                val frameWidth = if (widthFromHeight <= maxWidth) widthFromHeight else maxWidth
                val frameHeight = frameWidth / streamAspect
                Box(Modifier.size(frameWidth, frameHeight), content = content)
            }
        }

        @Composable
        fun ProPanelColumn(modifier: Modifier, showShutter: Boolean, showToolbar: Boolean) {
            Column(
                modifier =
                    modifier.background(Color.Black)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (showToolbar) {
                    ProToolbarBlock(
                        shared,
                        Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    )
                }
                if (!proLandscape) {
                    // Keep the live-value deck directly below the viewfinder.
                    // This removes the large dead zone that previously separated
                    // the exposure summary from its controls on tall phones.
                    ProIconSelector(
                        shared,
                        proItem,
                        { proItem = it },
                        vertical = false,
                    )
                }
                Box(
                    Modifier.fillMaxWidth().weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    ProControlArea(shared, proItem)
                }
                ProProfileDock(shared)
                IphoneModeTabs(shared)
                if (showShutter) {
                    ShutterBar(shared)
                } else {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MediaThumbButton(
                            shared.mediaThumb,
                            shared.hasMedia,
                            shared.onOpenGallery,
                            size = 44.dp,
                        )
                        FrontBackButton(shared)
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black).padding(padding)) {
            if (proMode && !proLandscape && compact) {
                // Portrait: toolbar, gapped preview, summary, panel.
                Column(Modifier.fillMaxSize()) {
                    ProToolbarBlock(
                        shared,
                        Modifier.fillMaxWidth()
                            .background(Color.Black)
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                    ProPreviewBox(
                        Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .aspectRatio(streamAspect)
                            .align(Alignment.CenterHorizontally)
                    )
                    ProSummaryLine(shared, Modifier.align(Alignment.CenterHorizontally))
                    ProPanelColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        showShutter = true,
                        showToolbar = false,
                    )
                }
            } else if (proMode && proLandscape) {
                // Landscape: slim icon strip in the left gap.
                Row(Modifier.fillMaxSize()) {
                    Column(
                        // Two columns keep all eight controls reachable on short
                        // landscape displays (the former one-column rail clipped
                        // MIC and PRF on Xperia 1).
                        Modifier.width(if (compact) 196.dp else 212.dp).fillMaxHeight()
                            .background(Color.Black)
                            .windowInsetsPadding(WindowInsets.safeDrawing),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
                    ) {
                        ProIconSelector(
                            shared,
                            proItem,
                            { proItem = it },
                            vertical = true,
                        )
                    }
                    Column(
                        Modifier.weight(if (compact) 1f else 1.25f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        // Reserved summary row: the aspect box alone would take
                        // the full height and squeeze the summary to 0px.
                        AspectFitPreviewFrame(
                            modifier = Modifier.fillMaxWidth().weight(1f).padding(vertical = 12.dp),
                        ) {
                            PreviewSurface(Modifier.fillMaxSize())
                            PreviewDecor()
                            ProStatusTexts(shared, Modifier.align(Alignment.TopStart))
                            ProLensTexts(shared, Modifier.align(Alignment.CenterEnd))
                        }
                        ProSummaryLine(shared, Modifier.padding(bottom = 4.dp))
                    }
                    ProPanelColumn(
                        modifier = Modifier.fillMaxHeight().weight(1f),
                        showShutter = true,
                        showToolbar = true,
                    )
                }
            } else if (proMode) {
                // Tablet portrait: shutter just below the preview's right edge.
                Row(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxHeight().weight(1.3f)) {
                        ProToolbarBlock(
                            shared,
                            Modifier.fillMaxWidth()
                                .background(Color.Black)
                                .windowInsetsPadding(WindowInsets.safeDrawing)
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                        ProPreviewBox(
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .aspectRatio(streamAspect)
                                .align(Alignment.CenterHorizontally)
                        )
                        ProSummaryLine(shared, Modifier.align(Alignment.CenterHorizontally))
                        Row(
                            Modifier.fillMaxWidth()
                                .padding(horizontal = 32.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ShutterButton(
                                isVideo = shared.isRecordMode(),
                                isRecording = shared.isRecording,
                                isCapturing = shared.isCapturing,
                                enabled = !shared.bgRunning,
                                size = 78.dp,
                                onClick = { shared.onShutter() },
                            )
                        }
                        Spacer(Modifier.weight(1f))
                    }
                    ProPanelColumn(
                        modifier = Modifier.fillMaxHeight().weight(1f),
                        showShutter = false,
                        showToolbar = false,
                    )
                }
            }

            if (!proMode) {
            CameraOverlay(
                viewModel = viewModel,
                settings = settings,
                availableLenses = availableLenses,
                profiles = profiles,
                external = external,
                bgRunning = bgRunning,
                timelapseActive = timelapseActive,
                isCapturing = isCapturing || slowMoProcessing || !cameraReady,
                isRecording = recording != null,
                gridOn = appSnapshot.gridLines,
                mediaThumb = thumb,
                hasMedia = lastMedia != null,
                batteryPct = batteryPct,
                zoomRatio = zoomRatio,
                maxZoom = maxZoomRatio,
                onResetZoom = ::resetZoom,
                panelCollapsed = appSnapshot.panelCollapsed,
                modeBar = modeBar,
                onSetModeBar = { updated -> appSettings.modeBar = updated.map { it.name } },
                onSetPanelCollapsed = { appSettings.panelCollapsed = it },
                onToggleGrid = { appSettings.gridLines = !appSettings.gridLines },
                onOpenGallery = ::openGallery,
                onCapturePhoto = ::capturePhoto,
                onToggleRecording = ::toggleRecording,
                onToggleTimelapse = ::toggleTimelapse,
                panoActive = panoActive,
                panoCount = panoFrames.size,
                panoMax = panoMaxFrames,
                onTogglePanorama = ::togglePanorama,
                onCancelPanorama = ::cancelPanorama,
                onToggleBackground = ::toggleBackground,
                onOpenSettings = onOpenSettings,
                onCancelExternal = { onExternalResult(false, null) },
                streamAspect = streamAspect,
                previewContent = {
                    Box(Modifier.fillMaxSize()) {
                        PreviewSurface(Modifier.fillMaxSize())
                        PreviewDecor()
                    }
                },
            )
            }
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

/** Sweep guide rails + hint shown only in panorama mode. */
@Composable
private fun PanoramaGuideFrame() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Text(
            text = stringResource(R.string.pano_hint),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier =
                Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(top = 116.dp, start = 16.dp, end = 16.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
    Canvas(Modifier.fillMaxSize()) {
        val c = Color.White.copy(alpha = 0.5f)
        val stroke = 2.dp.toPx()
        for (fx in listOf(0.12f, 0.88f)) {
            val x = size.width * fx
            drawLine(
                c,
                androidx.compose.ui.geometry.Offset(x, size.height * 0.08f),
                androidx.compose.ui.geometry.Offset(x, size.height * 0.92f),
                stroke,
            )
        }
    }
}

private fun saveBitmapToGallery(
    context: android.content.Context,
    bmp: android.graphics.Bitmap,
    displayName: String,
): Uri? {
    val values =
        ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Fcam pro")
            }
        }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        ?: return null
    return try {
        resolver.openOutputStream(uri)?.use { out ->
            if (!bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 92, out)) {
                throw java.io.IOException("compress failed")
            }
        } ?: throw java.io.IOException("open failed")
        uri
    } catch (e: Exception) {
        Log.e(TAG, "save bitmap failed", e)
        runCatching { resolver.delete(uri, null, null) }
        null
    }
}

private const val TAG = "CameraScreen"
