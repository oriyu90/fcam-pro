package com.oriyu90.fcampro.ui

import android.app.Application
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Range
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageCapture
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oriyu90.fcampro.camera.RawCapability
import com.oriyu90.fcampro.camera.RawSupport
import com.oriyu90.fcampro.core.AppSettings
import com.oriyu90.fcampro.data.AppDatabase
import com.oriyu90.fcampro.data.CameraProfile
import com.oriyu90.fcampro.data.ProfileRepository
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class CameraLensType { ULTRAWIDE, WIDE, TELEPHOTO, MACRO, FRONT }

enum class CameraMode { PHOTO, VIDEO, SLOWMO, PANORAMA, OTHERS }

/** Still-image save format (pro panel). RAW variants need lens support. */
enum class SaveFormat { JPEG, JPEG_RAW, RAW }

/** Pure zoom-ratio helpers (unit-testable; no Android dependencies). */
object ZoomRatios {
    const val MIN = 1f

    /** Next zoom ratio for a pinch factor, clamped to [1, max]. */
    fun next(current: Float, factor: Float, max: Float): Float {
        val hi = max.coerceAtLeast(MIN)
        if (!factor.isFinite() || factor <= 0f) return current.coerceIn(MIN, hi)
        return (current * factor).coerceIn(MIN, hi)
    }

    /**
     * True when a tap lands inside the displayed AF box. The box is centered
     * on the focus point and clamped into the preview, mirroring the display
     * math: inside = unlock, outside = refocus there.
     */
    fun isTapInFocusBox(
        tapX: Float,
        tapY: Float,
        fpX: Float,
        fpY: Float,
        halfPx: Float,
        viewW: Float,
        viewH: Float,
    ): Boolean {
        if (halfPx <= 0f || viewW <= 0f || viewH <= 0f) return false
        val cx = fpX.coerceIn(halfPx, (viewW - halfPx).coerceAtLeast(halfPx))
        val cy = fpY.coerceIn(halfPx, (viewH - halfPx).coerceAtLeast(halfPx))
        return tapX >= cx - halfPx && tapX <= cx + halfPx &&
            tapY >= cy - halfPx && tapY <= cy + halfPx
    }
}

/** Pure exposure-compensation helpers (unit-testable). */
object ExposureComp {
    /** Clamp an EV index to the device range; 0 when the lens reports none. */
    fun clamp(value: Int, range: IntRange?): Int =
        if (range == null) 0 else value.coerceIn(range.first, range.last)

    /** Display text, e.g. "+1.0 EV". */
    fun evText(value: Int, step: Float): String =
        "%+.1f EV".format(Locale.US, value * step)
}

/**
 * Atomic manual-exposure helpers (unit-testable).
 *
 * Camera2 AE_MODE_OFF requires BOTH SENSOR_SENSITIVITY and SENSOR_EXPOSURE_TIME;
 * sending only one side is HAL-undefined (ignored / black frames on strict HALs
 * such as Samsung's). Exposure manual state is therefore atomic: setting either
 * side fills the other with a clamped default, and clearing either side clears
 * both back to full auto.
 */
object ManualExposure {
    const val DEFAULT_ISO = 100
    const val DEFAULT_SHUTTER_NS = 16_666_667L // 1/60 s

    fun defaultIso(isoRange: IntRange?): Int {
        val d = DEFAULT_ISO
        return isoRange?.let { d.coerceIn(it.first, it.last) } ?: d
    }

    fun defaultShutterNs(expRange: LongRange?): Long {
        val d = DEFAULT_SHUTTER_NS
        return expRange?.let { d.coerceIn(it.first, it.last) } ?: d
    }

    /**
     * Complete a possibly half-manual pair. Returns (null, null) when both are
     * auto, otherwise both sides filled (missing side = clamped default).
     */
    fun complete(
        iso: Int?,
        shutterNs: Long?,
        isoRange: IntRange?,
        expRange: LongRange?,
    ): Pair<Int?, Long?> {
        if (iso == null && shutterNs == null) return null to null
        return (iso ?: defaultIso(isoRange)) to (shutterNs ?: defaultShutterNs(expRange))
    }
}

/** Constrained high-speed video configuration of one physical camera. */
@Immutable
data class HighSpeedVideo(
    val width: Int,
    val height: Int,
    val minFps: Int,
    val maxFps: Int,
)

/** Manual-control ranges reported by a specific physical camera. */
@Immutable
data class LensCapabilities(
    val supportsManualSensor: Boolean,
    val isoRange: IntRange?,
    val exposureRangeNs: LongRange?,
    val minFocusDistance: Float, // 0f => fixed focus / not reported
    val awbModes: List<Int>,
    val hasFlash: Boolean,
    val maxZoomRatio: Float,
    /** AE exposure-compensation index range (null => unsupported). */
    val exposureCompRange: IntRange?,
    /** EV step per compensation index (e.g. 1/3 EV). */
    val exposureCompStep: Float,
    /** Best high-speed video config (null => slow-motion unsupported). */
    val highSpeedVideo: HighSpeedVideo?,
    /** Sensor-RAW (DNG) capability (null => not probed / unsupported). */
    val rawCapability: RawCapability?,
    /** Available apertures (f-numbers); empty when unreported (fixed aperture). */
    val apertures: List<Float>,
)

@Immutable
data class CameraLensInfo(
    val id: String,
    val type: CameraLensType,
    val focalLength: Float,
    val isFront: Boolean,
    val capabilities: LensCapabilities,
    /** CameraX selector match id (logical camera). */
    val logicalCameraId: String,
    /**
     * Physical camera id behind a logical multi-camera (e.g. Galaxy telephoto),
     * or null when [id] is directly bindable. Bound via setPhysicalCameraId.
     */
    val physicalCameraId: String?,
) {
    /** True for sub-cameras hidden behind a logical multi-camera. */
    val isPhysical: Boolean get() = physicalCameraId != null
}

@Immutable
data class CameraSettings(
    val cameraMode: CameraMode = CameraMode.PHOTO,
    val isManualMode: Boolean = false,
    val shutterVolume: Float = 1.0f,
    val currentLens: CameraLensInfo? = null,
    val iso: Int? = null,
    val shutterSpeedNs: Long? = null,
    val focusDistance: Float? = null,
    val whiteBalanceMode: Int? = null,
    /** AE exposure-compensation index; only meaningful when ISO/shutter are auto. */
    val exposureCompensation: Int = 0,
    /** Still save format; RAW variants are coerced to JPEG when unsupported. */
    val saveFormat: SaveFormat = SaveFormat.JPEG,
    val flashMode: Int = ImageCapture.FLASH_MODE_AUTO,
    val timerSeconds: Int = 0,
    val isFrontCamera: Boolean = false,
    val aspectRatio: Int = AspectRatio.RATIO_4_3,
    val aeAfLocked: Boolean = false,
    val audioChannels: Int = 1, // 1 = mono, 2 = stereo
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProfileRepository(AppDatabase.get(application).cameraProfileDao())
    private val appSettings = AppSettings.get(application)

    val profiles: StateFlow<List<CameraProfile>> =
        repository.allProfiles.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    private val _settings = MutableStateFlow(CameraSettings())
    val settings: StateFlow<CameraSettings> = _settings.asStateFlow()

    private val _availableLenses = MutableStateFlow<List<CameraLensInfo>>(emptyList())
    val availableLenses: StateFlow<List<CameraLensInfo>> = _availableLenses.asStateFlow()

    /** True once lens detection has finished and found no usable camera. */
    private val _noCameraAvailable = MutableStateFlow(false)
    val noCameraAvailable: StateFlow<Boolean> = _noCameraAvailable.asStateFlow()

    /** Most recent saved capture, kept here so the "open gallery" button survives
     *  navigation to Settings and back. */
    data class LastMedia(val uri: Uri, val isVideo: Boolean)

    private val _lastMedia = MutableStateFlow<LastMedia?>(null)
    val lastMedia: StateFlow<LastMedia?> = _lastMedia.asStateFlow()

    init {
        applyDefaultsFromSettings()
        // Enumerating cameras can touch the camera HAL on first use; keep it off the
        // main thread so ViewModel creation never stalls Activity start.
        viewModelScope.launch(Dispatchers.Default) { detectLenses(application) }
    }

    fun setLastMedia(uri: Uri, isVideo: Boolean) {
        _lastMedia.value = LastMedia(uri, isVideo)
    }

    private fun applyDefaultsFromSettings() {
        val s = appSettings.state.value
        _settings.value =
            _settings.value.copy(
                aspectRatio =
                    if (s.defaultAspect16by9) AspectRatio.RATIO_16_9 else AspectRatio.RATIO_4_3,
                timerSeconds = s.defaultTimerSeconds,
            )
    }

    private fun detectLenses(context: Context) {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
        if (manager == null) {
            _noCameraAvailable.value = true
            return
        }
        val lenses = mutableListOf<CameraLensInfo>()
        val seenPhysicalIds = mutableSetOf<String>()
        try {
            for (id in manager.cameraIdList) {
                val chars =
                    runCatching { manager.getCameraCharacteristics(id) }.getOrNull() ?: continue

                val caps = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                val backwardCompatible =
                    caps?.contains(
                        CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
                    ) ?: true
                // Depth-only cameras cannot back a preview or capture use case;
                // binding them throws. Anything else (including non-backward-
                // compatible auxiliaries such as standalone telephoto logical
                // cameras on Galaxy devices) is kept — the bind path degrades
                // gracefully if a use case turns out unsupported.
                val depthOnly =
                    caps?.contains(
                        CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT
                    ) == true
                if (!backwardCompatible && depthOnly) continue

                // The logical camera itself is always a candidate (direct bind).
                addLensCandidate(
                    id = id,
                    logicalId = id,
                    physicalId = null,
                    chars = chars,
                    logicalFlash =
                        chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false,
                )?.let { lenses.add(it) }

                // Sub-cameras hidden behind a logical multi-camera (e.g. Galaxy
                // telephoto / ultra-wide): not in cameraIdList, reachable only
                // via setPhysicalCameraId on a logical bind.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val physicalIds =
                        runCatching { chars.getPhysicalCameraIds().toList() }.getOrNull()
                            ?: emptyList()
                    for (pid in physicalIds) {
                        if (!seenPhysicalIds.add(pid)) continue
                        val pchars =
                            runCatching { manager.getCameraCharacteristics(pid) }.getOrNull()
                                ?: continue
                        addLensCandidate(
                            id = pid,
                            logicalId = id,
                            physicalId = pid,
                            chars = pchars,
                            logicalFlash =
                                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false,
                        )?.let { lenses.add(it) }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting lenses", e)
        }

        // Drop logical/physical same-focal twins (e.g. Galaxy main twice).
        val deduped = dedupeLenses(lenses)
        for (lens in deduped) {
            Log.i(
                TAG,
                "lens id=${lens.id} logical=${lens.logicalCameraId} " +
                    "physical=${lens.physicalCameraId} type=${lens.type} " +
                    "focal=${lens.focalLength} minFocus=${lens.capabilities.minFocusDistance} " +
                    "front=${lens.isFront} manual=${lens.capabilities.supportsManualSensor} " +
                    "raw=${lens.capabilities.rawCapability?.supported} " +
                    "hs=${lens.capabilities.highSpeedVideo?.maxFps}",
            )
        }
        val sorted =
            deduped.sortedWith(
                compareBy(
                    { it.isFront },
                    { it.type.ordinal },
                    { it.focalLength },
                    // Directly bindable cameras first within the same bucket.
                    { it.physicalCameraId != null },
                    { it.id },
                )
            )
        _availableLenses.value = sorted
        _noCameraAvailable.value = sorted.isEmpty()

        val defaultBack =
            sorted.firstOrNull { !it.isFront && it.type == CameraLensType.WIDE }
                ?: sorted.firstOrNull { !it.isFront }
        val initial = defaultBack ?: sorted.firstOrNull()
        if (initial != null) {
            _settings.value =
                _settings.value.copy(currentLens = initial, isFrontCamera = initial.isFront)
        }
    }

    fun lensesForCurrentFacing(): List<CameraLensInfo> =
        _availableLenses.value.filter { it.isFront == _settings.value.isFrontCamera }

    fun currentCapabilities(): LensCapabilities? = _settings.value.currentLens?.capabilities

    /** True when the active lens can record constrained high-speed video. */
    fun isSlowMotionSupported(): Boolean =
        currentCapabilities()?.highSpeedVideo?.let { it.maxFps >= MIN_SLOWMO_FPS } ?: false

    /** True when the active lens can produce sensor-RAW (DNG) output. */
    fun isRawSupported(): Boolean =
        currentCapabilities()?.rawCapability?.supported == true

    /** Save format, coerced to JPEG when the lens has no RAW support. */
    fun setSaveFormat(format: SaveFormat) {
        _settings.value =
            _settings.value.copy(
                saveFormat = if (isRawSupported()) format else SaveFormat.JPEG
            )
    }

    // --- Mode / simple toggles -------------------------------------------------

    fun setMode(mode: CameraMode) {
        _settings.value =
            _settings.value.copy(
                cameraMode = mode,
                // Manual controls only make sense in PHOTO / VIDEO; collapse the panel
                // when leaving them so other modes never show a stale manual panel.
                isManualMode =
                    if (mode == CameraMode.PHOTO || mode == CameraMode.VIDEO) {
                        _settings.value.isManualMode
                    } else {
                        false
                    },
            )
    }

    fun toggleFrontCamera() {
        val target = !_settings.value.isFrontCamera
        val lens =
            _availableLenses.value.firstOrNull {
                it.isFront == target && it.type == CameraLensType.WIDE
            }
                ?: _availableLenses.value.firstOrNull { it.isFront == target }
        if (lens != null) {
            _settings.value =
                _settings.value.copy(
                    isFrontCamera = target,
                    currentLens = lens,
                    // Manual sensor support differs per camera; re-clamp.
                    iso = null,
                    shutterSpeedNs = null,
                    focusDistance = null,
                    whiteBalanceMode = null,
                    exposureCompensation = 0,
                    saveFormat = coerceSaveFormat(_settings.value.saveFormat, lens),
                )
        }
    }

    fun cycleFlashMode() {
        val next =
            when (_settings.value.flashMode) {
                ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
                else -> ImageCapture.FLASH_MODE_AUTO
            }
        _settings.value = _settings.value.copy(flashMode = next)
    }

    fun cycleTimer() {
        val opts = AppSettings.TIMER_OPTIONS
        val idx = opts.indexOf(_settings.value.timerSeconds).let { if (it < 0) 0 else it }
        _settings.value = _settings.value.copy(timerSeconds = opts[(idx + 1) % opts.size])
    }

    fun cycleAspectRatio() {
        val next =
            if (_settings.value.aspectRatio == AspectRatio.RATIO_4_3) AspectRatio.RATIO_16_9
            else AspectRatio.RATIO_4_3
        _settings.value = _settings.value.copy(aspectRatio = next)
    }

    fun toggleAeAfLock() {
        _settings.value = _settings.value.copy(aeAfLocked = !_settings.value.aeAfLocked)
    }

    fun cycleAudioChannels() {
        _settings.value =
            _settings.value.copy(audioChannels = if (_settings.value.audioChannels == 1) 2 else 1)
    }

    fun toggleManualMode() {
        _settings.value = _settings.value.copy(isManualMode = !_settings.value.isManualMode)
    }

    fun setLens(lens: CameraLensInfo) {
        _settings.value =
            _settings.value.copy(
                currentLens = lens,
                isFrontCamera = lens.isFront,
                iso = null,
                shutterSpeedNs = null,
                focusDistance = null,
                whiteBalanceMode = null,
                exposureCompensation = 0,
                saveFormat = coerceSaveFormat(_settings.value.saveFormat, lens),
            )
    }

    fun setShutterVolume(volume: Float) {
        _settings.value = _settings.value.copy(shutterVolume = volume.coerceIn(0f, 1f))
    }

    // --- Manual settings (clamped to the active lens capabilities) ------------

    fun updateManualSettings(iso: Int?, shutterNs: Long?, focus: Float?, wb: Int?) {
        val caps = currentCapabilities()
        val clampedIso = iso?.let { v -> caps?.isoRange?.let { v.coerceIn(it.first, it.last) } ?: v }
        val clampedShutter =
            shutterNs?.let { v ->
                caps?.exposureRangeNs?.let { v.coerceIn(it.first, it.last) } ?: v
            }
        val clampedFocus =
            focus?.let { v ->
                val max = caps?.minFocusDistance?.takeIf { it > 0f } ?: 10f
                v.coerceIn(0f, max)
            }
        // Exposure is atomic (see ManualExposure): a half-manual pair is
        // HAL-undefined, so the missing side is filled with a clamped default
        // instead of sending AE_MODE_OFF with a single parameter.
        val (finalIso, finalShutter) =
            ManualExposure.complete(clampedIso, clampedShutter, caps?.isoRange, caps?.exposureRangeNs)
        _settings.value =
            _settings.value.copy(
                iso = finalIso,
                shutterSpeedNs = finalShutter,
                focusDistance = clampedFocus,
                whiteBalanceMode = wb,
            )
    }

    /** Back to full-auto exposure (clears both ISO and shutter speed). */
    fun clearExposureManual() {
        _settings.value = _settings.value.copy(iso = null, shutterSpeedNs = null)
    }

    /** AE exposure-compensation index, clamped to the active lens range. */
    fun updateExposureCompensation(ev: Int) {
        _settings.value =
            _settings.value.copy(
                exposureCompensation = ExposureComp.clamp(ev, currentCapabilities()?.exposureCompRange)
            )
    }

    fun resetManualSettings() {
        _settings.value =
            _settings.value.copy(
                iso = null,
                shutterSpeedNs = null,
                focusDistance = null,
                whiteBalanceMode = null,
                exposureCompensation = 0,
            )
    }

    // --- Profiles ------------------------------------------------------------

    fun saveProfile(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val s = _settings.value
            repository.insert(
                CameraProfile(
                    name = trimmed,
                    iso = s.iso,
                    shutterSpeedNs = s.shutterSpeedNs,
                    focusDistance = s.focusDistance,
                    whiteBalanceMode = s.whiteBalanceMode,
                    exposureCompensation = s.exposureCompensation,
                )
            )
        }
    }

    fun deleteProfile(id: Int) {
        viewModelScope.launch { repository.deleteById(id) }
    }

    fun updateProfileName(profile: CameraProfile, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.update(profile.copy(name = trimmed)) }
    }

    fun loadProfile(profile: CameraProfile) {
        _settings.value = _settings.value.copy(isManualMode = true)
        updateManualSettings(
            profile.iso,
            profile.shutterSpeedNs,
            profile.focusDistance,
            profile.whiteBalanceMode,
        )
        updateExposureCompensation(profile.exposureCompensation)
    }

    private companion object {
        const val TAG = "CameraViewModel"

        /** Minimum high-speed fps that counts as slow-motion capable (2x of 30fps). */
        const val MIN_SLOWMO_FPS = 60

        fun coerceSaveFormat(format: SaveFormat, lens: CameraLensInfo): SaveFormat =
            if (lens.capabilities.rawCapability?.supported == true) format
            else SaveFormat.JPEG

        /**
         * One bindable lens entry, or null when it cannot be classified.
         * [chars] are the characteristics to probe (physical characteristics
         * for sub-cameras); flash availability is always inherited from the
         * logical camera because the flash unit is shared and physical
         * cameras report none.
         */
        fun addLensCandidate(
            id: String,
            logicalId: String,
            physicalId: String?,
            chars: CameraCharacteristics,
            logicalFlash: Boolean,
        ): CameraLensInfo? {
            val caps = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            val facing = chars.get(CameraCharacteristics.LENS_FACING)
            val isFront = facing == CameraCharacteristics.LENS_FACING_FRONT
            val focalLengths =
                chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
            // Logical cameras keep the legacy 4.5mm fallback so a missing table
            // can never yield zero lenses; physicals without one are skipped.
            val focalLength = focalLengths?.firstOrNull() ?: if (physicalId == null) 4.5f else return null
            val minFocus =
                chars.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f

            val type =
                when {
                    isFront -> CameraLensType.FRONT
                    // Dedicated macro lenses report very close focus (20+ diopters).
                    // Main cameras with close focus (e.g. Galaxy S24 main = 10.0)
                    // must stay WIDE, so the bar is 15, not 10.
                    minFocus >= 15f -> CameraLensType.MACRO
                    focalLength < 3.5f -> CameraLensType.ULTRAWIDE
                    focalLength > 6.5f -> CameraLensType.TELEPHOTO
                    else -> CameraLensType.WIDE
                }

            val manualSensor =
                caps?.contains(
                    CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR
                ) ?: false
            val isoR: Range<Int>? =
                chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
            val expR: Range<Long>? =
                chars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
            val awb =
                chars.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)?.toList()
                    ?: emptyList()
            val maxZoom =
                chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1f
            val evRange: Range<Int>? =
                chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
            val evStep =
                chars.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
                    ?.toDouble()?.toFloat() ?: 0f
            val apertures =
                chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.toList()
                    ?: emptyList()

            return CameraLensInfo(
                id = id,
                type = type,
                focalLength = focalLength,
                isFront = isFront,
                capabilities =
                    LensCapabilities(
                        supportsManualSensor = manualSensor,
                        isoRange = isoR?.let { it.lower..it.upper },
                        exposureRangeNs = expR?.let { it.lower..it.upper },
                        minFocusDistance = minFocus,
                        awbModes = awb,
                        hasFlash = logicalFlash,
                        maxZoomRatio = maxZoom.coerceAtLeast(1f),
                        exposureCompRange = evRange?.let { it.lower..it.upper },
                        exposureCompStep = evStep,
                        highSpeedVideo = bestHighSpeedVideo(chars),
                        rawCapability = probeRawCapability(chars),
                        apertures = apertures,
                    ),
                logicalCameraId = logicalId,
                physicalCameraId = physicalId,
            )
        }

        /**
         * Sensor-RAW probe: REQUEST_AVAILABLE_CAPABILITIES_RAW plus at least
         * one RAW_SENSOR stream size. Bit depth is a white-level heuristic
         * (RAW10/RAW12/API-37 RAW14 scale) for the UI badge only.
         */
        fun probeRawCapability(chars: CameraCharacteristics): RawCapability {
            val caps = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
            val hasRaw =
                caps?.contains(
                    CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW
                ) ?: false
            val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val rawSizes =
                map?.getOutputSizes(android.graphics.ImageFormat.RAW_SENSOR)
            val supported = hasRaw && !rawSizes.isNullOrEmpty()
            return RawCapability(
                supported = supported,
                bitDepth =
                    RawSupport.bitDepthFromWhiteLevel(
                        chars.get(CameraCharacteristics.SENSOR_INFO_WHITE_LEVEL)
                    ),
                maxSize = rawSizes?.maxByOrNull { it.width * it.height },
            )
        }

        /**
         * Best constrained high-speed video config, or null when the camera has none.
         * Prefers the highest frame rate, then the largest frame area.
         */
        fun bestHighSpeedVideo(chars: CameraCharacteristics): HighSpeedVideo? {
            val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                ?: return null
            var best: HighSpeedVideo? = null
            for (size in map.highSpeedVideoSizes ?: return null) {
                val ranges = map.getHighSpeedVideoFpsRangesFor(size) ?: continue
                for (r in ranges) {
                    val candidate =
                        HighSpeedVideo(size.width, size.height, r.lower, r.upper)
                    val current = best
                    if (current == null ||
                        candidate.maxFps > current.maxFps ||
                        (candidate.maxFps == current.maxFps &&
                            candidate.width * candidate.height > current.width * current.height)
                    ) {
                        best = candidate
                    }
                }
            }
            return best
        }
    }
}

/**
 * Drop same-facing / same-type / same-focal (0.5mm buckets) duplicates from a
 * detected lens list, keeping the first entry (directly bindable cameras sort
 * first). Pure and unit-testable.
 */
internal fun dedupeLenses(input: List<CameraLensInfo>): List<CameraLensInfo> {
    val seen = mutableSetOf<Triple<Boolean, CameraLensType, Int>>()
    return input.filter { lens ->
        seen.add(
            Triple(lens.isFront, lens.type, (lens.focalLength * 2).roundToInt())
        )
    }
}
