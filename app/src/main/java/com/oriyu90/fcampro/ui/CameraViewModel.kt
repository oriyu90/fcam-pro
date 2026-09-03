package com.oriyu90.fcampro.ui

import android.app.Application
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.util.Log
import android.util.Range
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageCapture
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oriyu90.fcampro.core.AppSettings
import com.oriyu90.fcampro.data.AppDatabase
import com.oriyu90.fcampro.data.CameraProfile
import com.oriyu90.fcampro.data.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CameraLensType { ULTRAWIDE, WIDE, TELEPHOTO, MACRO, FRONT }

enum class CameraMode { PHOTO, VIDEO, OTHERS }

/** Manual-control ranges reported by a specific physical camera. */
data class LensCapabilities(
    val supportsManualSensor: Boolean,
    val isoRange: IntRange?,
    val exposureRangeNs: LongRange?,
    val minFocusDistance: Float, // 0f => fixed focus / not reported
    val awbModes: List<Int>,
    val hasFlash: Boolean,
    val maxZoomRatio: Float,
)

data class CameraLensInfo(
    val id: String,
    val type: CameraLensType,
    val focalLength: Float,
    val isFront: Boolean,
    val capabilities: LensCapabilities,
)

data class CameraSettings(
    val cameraMode: CameraMode = CameraMode.PHOTO,
    val isManualMode: Boolean = false,
    val shutterVolume: Float = 1.0f,
    val currentLens: CameraLensInfo? = null,
    val iso: Int? = null,
    val shutterSpeedNs: Long? = null,
    val focusDistance: Float? = null,
    val whiteBalanceMode: Int? = null,
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

    /** True when the device exposed no usable camera. UI shows a terminal message. */
    var noCameraAvailable: Boolean = false
        private set

    init {
        detectLenses(application)
        applyDefaultsFromSettings()
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
            noCameraAvailable = true
            return
        }
        val lenses = mutableListOf<CameraLensInfo>()
        try {
            for (id in manager.cameraIdList) {
                val chars =
                    runCatching { manager.getCameraCharacteristics(id) }.getOrNull() ?: continue

                val caps = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
                val backwardCompatible =
                    caps?.contains(
                        CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
                    ) ?: true
                // Depth-only / IR-only / measurement cameras cannot back a preview or
                // capture use case; binding them throws. Skip them.
                if (!backwardCompatible) continue

                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                val isFront = facing == CameraCharacteristics.LENS_FACING_FRONT
                val focalLengths =
                    chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                val focalLength = focalLengths?.firstOrNull() ?: 4.5f
                val minFocus =
                    chars.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f

                val type =
                    when {
                        isFront -> CameraLensType.FRONT
                        minFocus >= 10f -> CameraLensType.MACRO
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
                val flash =
                    chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                val maxZoom =
                    chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1f

                lenses.add(
                    CameraLensInfo(
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
                                hasFlash = flash,
                                maxZoomRatio = maxZoom.coerceAtLeast(1f),
                            ),
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting lenses", e)
        }

        val sorted =
            lenses.sortedWith(compareBy({ it.isFront }, { it.type.ordinal }, { it.focalLength }))
        _availableLenses.value = sorted
        noCameraAvailable = sorted.isEmpty()

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

    // --- Mode / simple toggles -------------------------------------------------

    fun setMode(mode: CameraMode) {
        _settings.value = _settings.value.copy(cameraMode = mode)
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
        _settings.value =
            _settings.value.copy(
                iso = clampedIso,
                shutterSpeedNs = clampedShutter,
                focusDistance = clampedFocus,
                whiteBalanceMode = wb,
            )
    }

    // --- Profiles ------------------------------------------------------------

    fun saveProfile(name: String) {
        val trimmed = name.trim()
        viewModelScope.launch {
            val s = _settings.value
            repository.insert(
                CameraProfile(
                    name = trimmed.ifEmpty { "" },
                    iso = s.iso,
                    shutterSpeedNs = s.shutterSpeedNs,
                    focusDistance = s.focusDistance,
                    whiteBalanceMode = s.whiteBalanceMode,
                )
            )
        }
    }

    fun deleteProfile(id: Int) {
        viewModelScope.launch { repository.deleteById(id) }
    }

    fun updateProfileName(profile: CameraProfile, newName: String) {
        viewModelScope.launch { repository.update(profile.copy(name = newName.trim())) }
    }

    fun loadProfile(profile: CameraProfile) {
        _settings.value = _settings.value.copy(isManualMode = true)
        updateManualSettings(
            profile.iso,
            profile.shutterSpeedNs,
            profile.focusDistance,
            profile.whiteBalanceMode,
        )
    }

    private companion object {
        const val TAG = "CameraViewModel"
    }
}
