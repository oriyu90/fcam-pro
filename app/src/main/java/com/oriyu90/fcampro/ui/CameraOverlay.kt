package com.oriyu90.fcampro.ui

import android.content.res.Configuration
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop169
import androidx.compose.material.icons.filled.Crop54
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PanoramaHorizontal
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oriyu90.fcampro.R
import com.oriyu90.fcampro.data.CameraProfile

@Composable
fun CameraOverlay(
    viewModel: CameraViewModel,
    settings: CameraSettings,
    availableLenses: List<CameraLensInfo>,
    profiles: List<CameraProfile>,
    external: ExternalCaptureSpec?,
    bgRunning: Boolean,
    timelapseActive: Boolean,
    isCapturing: Boolean,
    isRecording: Boolean,
    gridOn: Boolean,
    hasMedia: Boolean,
    onToggleGrid: () -> Unit,
    onOpenGallery: () -> Unit,
    onCapturePhoto: () -> Unit,
    onToggleRecording: () -> Unit,
    onToggleTimelapse: () -> Unit,
    onSlowMo: () -> Unit,
    onPanorama: () -> Unit,
    onToggleBackground: () -> Unit,
    onOpenSettings: () -> Unit,
    onCancelExternal: () -> Unit,
) {
    val config = LocalConfiguration.current
    val wide =
        config.orientation == Configuration.ORIENTATION_LANDSCAPE || config.screenWidthDp >= 600

    val top: @Composable (Modifier) -> Unit = { m ->
        TopControls(viewModel, settings, gridOn, onToggleGrid, onOpenSettings, m)
    }
    val bottom: @Composable (Modifier) -> Unit = { m ->
        BottomControls(
            viewModel = viewModel,
            settings = settings,
            availableLenses = availableLenses,
            profiles = profiles,
            external = external,
            bgRunning = bgRunning,
            timelapseActive = timelapseActive,
            isCapturing = isCapturing,
            isRecording = isRecording,
            hasMedia = hasMedia,
            onOpenGallery = onOpenGallery,
            onCapturePhoto = onCapturePhoto,
            onToggleRecording = onToggleRecording,
            onToggleTimelapse = onToggleTimelapse,
            onSlowMo = onSlowMo,
            onPanorama = onPanorama,
            onToggleBackground = onToggleBackground,
            onCancelExternal = onCancelExternal,
            modifier = m,
        )
    }

    if (wide) {
        Row(Modifier.fillMaxSize()) {
            Spacer(Modifier.weight(1f))
            Column(
                modifier =
                    Modifier.widthIn(max = 360.dp)
                        .fillMaxHeight()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                top(Modifier.fillMaxWidth())
                bottom(Modifier.fillMaxWidth())
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            top(
                Modifier.fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
            bottom(
                Modifier.fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(12.dp)
            )
        }
    }
}

@Composable
private fun TopControls(
    viewModel: CameraViewModel,
    settings: CameraSettings,
    gridOn: Boolean,
    onToggleGrid: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val caps = settings.currentLens?.capabilities
        IconButton(
            onClick = { viewModel.toggleManualMode() },
            enabled = caps?.supportsManualSensor == true,
        ) {
            Icon(
                imageVector = if (settings.isManualMode) Icons.Default.Tune else Icons.Default.AutoMode,
                contentDescription = stringResource(R.string.cd_manual_mode),
                tint = if (settings.isManualMode) MaterialTheme.colorScheme.primary else Color.White,
            )
        }
        IconButton(onClick = { viewModel.cycleFlashMode() }) {
            Icon(
                imageVector =
                    when (settings.flashMode) {
                        ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                        ImageCapture.FLASH_MODE_OFF -> Icons.Default.FlashOff
                        else -> Icons.Default.FlashAuto
                    },
                contentDescription = stringResource(R.string.cd_flash_mode),
                tint = Color.White,
            )
        }
        IconButton(onClick = { viewModel.cycleTimer() }) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = stringResource(R.string.cd_timer),
                    tint =
                        if (settings.timerSeconds > 0) MaterialTheme.colorScheme.primary
                        else Color.White,
                )
                if (settings.timerSeconds > 0) {
                    Text(
                        settings.timerSeconds.toString(),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        IconButton(onClick = { viewModel.cycleAspectRatio() }) {
            Icon(
                imageVector =
                    if (settings.aspectRatio == AspectRatio.RATIO_16_9) Icons.Default.Crop169
                    else Icons.Default.Crop54,
                contentDescription = stringResource(R.string.cd_aspect_ratio),
                tint = Color.White,
            )
        }
        IconButton(onClick = onToggleGrid) {
            Icon(
                imageVector = if (gridOn) Icons.Default.GridOn else Icons.Default.GridOff,
                contentDescription = stringResource(R.string.cd_grid),
                tint = if (gridOn) MaterialTheme.colorScheme.primary else Color.White,
            )
        }
        IconButton(onClick = { viewModel.toggleAeAfLock() }) {
            Icon(
                imageVector = if (settings.aeAfLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = stringResource(R.string.cd_ae_af_lock),
                tint = if (settings.aeAfLocked) MaterialTheme.colorScheme.primary else Color.White,
            )
        }
        IconButton(onClick = { viewModel.toggleFrontCamera() }) {
            Icon(
                Icons.Default.FlipCameraAndroid,
                contentDescription = stringResource(R.string.cd_switch_camera),
                tint = Color.White,
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(
                Icons.Default.Settings,
                contentDescription = stringResource(R.string.cd_settings),
                tint = Color.White,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomControls(
    viewModel: CameraViewModel,
    settings: CameraSettings,
    availableLenses: List<CameraLensInfo>,
    profiles: List<CameraProfile>,
    external: ExternalCaptureSpec?,
    bgRunning: Boolean,
    timelapseActive: Boolean,
    isCapturing: Boolean,
    isRecording: Boolean,
    hasMedia: Boolean,
    onOpenGallery: () -> Unit,
    onCapturePhoto: () -> Unit,
    onToggleRecording: () -> Unit,
    onToggleTimelapse: () -> Unit,
    onSlowMo: () -> Unit,
    onPanorama: () -> Unit,
    onToggleBackground: () -> Unit,
    onCancelExternal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        if (settings.isManualMode) {
            ManualPanel(viewModel, settings, profiles)
        }

        // Compact lens picker (small pop button) — available in PHOTO / VIDEO.
        val lenses = availableLenses.filter { it.isFront == settings.isFrontCamera }
        if (lenses.size > 1 && settings.cameraMode != CameraMode.OTHERS && external == null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                LensMenuButton(
                    lenses = lenses,
                    current = settings.currentLens,
                    enabled = !isRecording,
                    onSelect = { viewModel.setLens(it) },
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (settings.cameraMode == CameraMode.OTHERS && external == null) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    item {
                        OthersMenuItem(
                            Icons.Default.Timelapse,
                            stringResource(
                                if (timelapseActive) R.string.others_timelapse_stop
                                else R.string.others_timelapse
                            ),
                            onToggleTimelapse,
                        )
                    }
                    item {
                        OthersMenuItem(
                            Icons.Default.SlowMotionVideo,
                            stringResource(R.string.others_slowmo),
                            onSlowMo,
                        )
                    }
                    item {
                        OthersMenuItem(
                            Icons.Default.PanoramaHorizontal,
                            stringResource(R.string.others_panorama),
                            onPanorama,
                        )
                    }
                    item {
                        OthersMenuItem(
                            Icons.Default.Security,
                            stringResource(
                                if (bgRunning) R.string.others_bg_record_stop
                                else R.string.others_bg_record
                            ),
                            onToggleBackground,
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    if (external != null) {
                        TextButton(onClick = onCancelExternal) {
                            Text(stringResource(R.string.action_cancel), color = Color.White)
                        }
                    } else {
                        GalleryButton(enabled = hasMedia, onClick = onOpenGallery)
                    }
                    ShutterButton(
                        isVideo = settings.cameraMode == CameraMode.VIDEO,
                        isRecording = isRecording,
                        isCapturing = isCapturing,
                        onClick = {
                            if (settings.cameraMode == CameraMode.VIDEO) onToggleRecording()
                            else onCapturePhoto()
                        },
                    )
                    if (external == null) {
                        Spacer(Modifier.size(48.dp)) // visual balance opposite the gallery button
                    }
                }
            }
        }

        if (external == null) {
            TabRow(
                selectedTabIndex = settings.cameraMode.ordinal,
                containerColor = Color.Transparent,
                contentColor = Color.White,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Tab(
                    selected = settings.cameraMode == CameraMode.PHOTO,
                    onClick = { viewModel.setMode(CameraMode.PHOTO) },
                    text = { Text(stringResource(R.string.tab_photo)) },
                )
                Tab(
                    selected = settings.cameraMode == CameraMode.VIDEO,
                    onClick = { viewModel.setMode(CameraMode.VIDEO) },
                    text = { Text(stringResource(R.string.tab_video)) },
                )
                Tab(
                    selected = settings.cameraMode == CameraMode.OTHERS,
                    onClick = { viewModel.setMode(CameraMode.OTHERS) },
                    text = { Text(stringResource(R.string.tab_others)) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LensMenuButton(
    lenses: List<CameraLensInfo>,
    current: CameraLensInfo?,
    enabled: Boolean,
    onSelect: (CameraLensInfo) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        AssistChip(
            onClick = { if (enabled) open = true },
            enabled = enabled,
            label = { Text(current?.let { lensLabel(it.type) } ?: stringResource(R.string.cd_lens)) },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            },
            modifier = Modifier.semantics { contentDescription = "lens" },
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            lenses.forEach { lens ->
                DropdownMenuItem(
                    text = {
                        Text(
                            lensLabel(lens.type) +
                                "  ·  ${"%.1f".format(lens.focalLength)}mm"
                        )
                    },
                    onClick = {
                        onSelect(lens)
                        open = false
                    },
                    leadingIcon = {
                        if (current?.id == lens.id) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun GalleryButton(enabled: Boolean, onClick: () -> Unit) {
    val cd = stringResource(R.string.cd_gallery)
    Box(
        modifier =
            Modifier.size(48.dp)
                .semantics { contentDescription = cd }
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = if (enabled) 0.18f else 0.06f))
                .border(
                    1.dp,
                    Color.White.copy(alpha = if (enabled) 0.6f else 0.2f),
                    RoundedCornerShape(10.dp),
                )
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.PhotoLibrary,
            contentDescription = null,
            tint = Color.White.copy(alpha = if (enabled) 1f else 0.4f),
        )
    }
}

@Composable
private fun ShutterButton(
    isVideo: Boolean,
    isRecording: Boolean,
    isCapturing: Boolean,
    onClick: () -> Unit,
) {
    val cd =
        stringResource(
            when {
                isVideo && isRecording -> R.string.cd_stop_recording
                isVideo -> R.string.cd_start_recording
                else -> R.string.cd_capture_photo
            }
        )
    Box(
        modifier =
            Modifier.size(72.dp)
                .semantics { contentDescription = cd }
                .clip(CircleShape)
                .background(if (isCapturing) Color.Gray else Color.White)
                .clickable(enabled = !isCapturing, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isVideo) {
            Box(
                Modifier.size(26.dp)
                    .clip(if (isRecording) RoundedCornerShape(4.dp) else CircleShape)
                    .background(Color.Red)
            )
        } else {
            Box(Modifier.size(62.dp).clip(CircleShape).background(Color.LightGray))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualPanel(
    viewModel: CameraViewModel,
    settings: CameraSettings,
    profiles: List<CameraProfile>,
) {
    val caps = settings.currentLens?.capabilities ?: return
    var showSave by remember { mutableStateOf(false) }
    var editProfile by remember { mutableStateOf<CameraProfile?>(null) }

    fun update(iso: Int?, shutter: Long?, focus: Float?, wb: Int?) =
        viewModel.updateManualSettings(iso, shutter, focus, wb)

    Column(
        modifier =
            Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(
                onClick = { update(null, null, null, null) },
                enabled =
                    settings.iso != null ||
                        settings.shutterSpeedNs != null ||
                        settings.focusDistance != null ||
                        settings.whiteBalanceMode != null,
            ) {
                Text(stringResource(R.string.manual_all_auto))
            }
        }

        val isoRange = caps.isoRange ?: 50..3200
        LabeledSlider(
            label = stringResource(R.string.label_iso),
            valueText = settings.iso?.toString() ?: stringResource(R.string.value_auto),
            value = settings.iso?.toFloat() ?: isoRange.first.toFloat(),
            range = isoRange.first.toFloat()..isoRange.last.toFloat(),
            isAuto = settings.iso == null,
            onAuto = { update(null, settings.shutterSpeedNs, settings.focusDistance, settings.whiteBalanceMode) },
            onChange = {
                update(it.toInt(), settings.shutterSpeedNs, settings.focusDistance, settings.whiteBalanceMode)
            },
        )

        val expRange = caps.exposureRangeNs ?: 125_000L..1_000_000_000L
        val shutterText =
            settings.shutterSpeedNs?.let { "1/${(1_000_000_000L / it).coerceAtLeast(1)}s" }
                ?: stringResource(R.string.value_auto)
        LabeledSlider(
            label = stringResource(R.string.label_shutter),
            valueText = shutterText,
            value = settings.shutterSpeedNs?.toFloat() ?: expRange.first.toFloat(),
            range = expRange.first.toFloat()..expRange.last.toFloat(),
            isAuto = settings.shutterSpeedNs == null,
            onAuto = { update(settings.iso, null, settings.focusDistance, settings.whiteBalanceMode) },
            onChange = {
                update(settings.iso, it.toLong(), settings.focusDistance, settings.whiteBalanceMode)
            },
        )

        val focusMax = caps.minFocusDistance.takeIf { it > 0f } ?: 10f
        LabeledSlider(
            label = stringResource(R.string.label_focus),
            valueText =
                settings.focusDistance?.let { "%.1f".format(it) }
                    ?: stringResource(R.string.value_auto),
            value = settings.focusDistance ?: 0f,
            range = 0f..focusMax,
            isAuto = settings.focusDistance == null,
            onAuto = { update(settings.iso, settings.shutterSpeedNs, null, settings.whiteBalanceMode) },
            onChange = {
                update(settings.iso, settings.shutterSpeedNs, it, settings.whiteBalanceMode)
            },
        )

        if (caps.awbModes.size > 1) {
            LabeledSlider(
                label = stringResource(R.string.label_wb),
                valueText =
                    settings.whiteBalanceMode?.toString()
                        ?: stringResource(R.string.value_auto),
                value = settings.whiteBalanceMode?.toFloat() ?: 1f,
                range = 1f..8f,
                steps = 6,
                isAuto = settings.whiteBalanceMode == null,
                onAuto = { update(settings.iso, settings.shutterSpeedNs, settings.focusDistance, null) },
                onChange = {
                    update(settings.iso, settings.shutterSpeedNs, settings.focusDistance, it.toInt())
                },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.label_shutter_volume),
                color = Color.White,
                modifier = Modifier.width(96.dp),
                style = MaterialTheme.typography.labelSmall,
            )
            Slider(
                value = settings.shutterVolume,
                onValueChange = { viewModel.setShutterVolume(it) },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text =
                    stringResource(R.string.label_mic) +
                        ": " +
                        stringResource(
                            if (settings.audioChannels == 2) R.string.mic_stereo
                            else R.string.mic_mono
                        ),
                color = Color.White,
                modifier = Modifier.width(120.dp),
                style = MaterialTheme.typography.labelSmall,
            )
            Switch(
                checked = settings.audioChannels == 2,
                onCheckedChange = { viewModel.cycleAudioChannels() },
            )
        }

        Button(onClick = { showSave = true }, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.save_profile))
        }

        if (profiles.isNotEmpty()) {
            LazyRow(modifier = Modifier.padding(vertical = 8.dp)) {
                items(profiles, key = { it.id }) { profile ->
                    ElevatedFilterChip(
                        selected = false,
                        onClick = { viewModel.loadProfile(profile) },
                        label = {
                            Text(
                                profile.name.ifBlank {
                                    stringResource(R.string.profile_unnamed)
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        leadingIcon = {
                            IconButton(
                                onClick = { editProfile = profile },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.action_edit),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { viewModel.deleteProfile(profile.id) },
                                modifier = Modifier.size(24.dp),
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription =
                                        stringResource(R.string.action_delete),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    if (showSave) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSave = false },
            title = { Text(stringResource(R.string.save_profile)) },
            text = {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.profile_name)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveProfile(name)
                        showSave = false
                    }
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSave = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    editProfile?.let { p ->
        var name by remember(p.id) { mutableStateOf(p.name) }
        AlertDialog(
            onDismissRequest = { editProfile = null },
            title = { Text(stringResource(R.string.edit_profile_title)) },
            text = {
                TextField(value = name, onValueChange = { name = it }, singleLine = true)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfileName(p, name)
                        editProfile = null
                    }
                ) {
                    Text(stringResource(R.string.action_update))
                }
            },
            dismissButton = {
                TextButton(onClick = { editProfile = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    isAuto: Boolean = false,
    onAuto: (() -> Unit)? = null,
    onChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$label: $valueText",
            color = Color.White,
            modifier = Modifier.width(108.dp),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        if (onAuto != null) {
            TextButton(
                onClick = onAuto,
                enabled = !isAuto,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
            ) {
                Text(
                    stringResource(R.string.manual_set_auto),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun OthersMenuItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp),
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.DarkGray),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White)
        }
        Spacer(Modifier.height(4.dp))
        Text(text = text, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun lensLabel(type: CameraLensType): String =
    stringResource(
        when (type) {
            CameraLensType.ULTRAWIDE -> R.string.lens_ultrawide
            CameraLensType.WIDE -> R.string.lens_wide
            CameraLensType.TELEPHOTO -> R.string.lens_telephoto
            CameraLensType.MACRO -> R.string.lens_macro
            CameraLensType.FRONT -> R.string.lens_front
        }
    )
