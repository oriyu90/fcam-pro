package com.oriyu90.fcampro.ui

import android.content.res.Configuration
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageCapture
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop169
import androidx.compose.material.icons.filled.Crop54
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
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
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.oriyu90.fcampro.R
import com.oriyu90.fcampro.data.CameraProfile
import kotlin.math.roundToInt

private enum class Layout { PHONE_PORTRAIT, LARGE_PORTRAIT, SIDE }

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
    mediaThumb: ImageBitmap?,
    hasMedia: Boolean,
    batteryPct: Int?,
    zoomRatio: Float,
    maxZoom: Float,
    onResetZoom: () -> Unit,
    panelCollapsed: Boolean,
    panelGravity: Int,
    modeBar: List<CameraMode>,
    onSetModeBar: (List<CameraMode>) -> Unit,
    onSetPanelCollapsed: (Boolean) -> Unit,
    onSetPanelGravity: (Int) -> Unit,
    onToggleGrid: () -> Unit,
    onOpenGallery: () -> Unit,
    onCapturePhoto: () -> Unit,
    onToggleRecording: () -> Unit,
    onToggleTimelapse: () -> Unit,
    panoActive: Boolean,
    panoCount: Int,
    panoMax: Int,
    onTogglePanorama: () -> Unit,
    onCancelPanorama: () -> Unit,
    onToggleBackground: () -> Unit,
    onOpenSettings: () -> Unit,
    onCancelExternal: () -> Unit,
) {
    val cfg = LocalConfiguration.current
    val landscape = cfg.orientation == Configuration.ORIENTATION_LANDSCAPE
    val compact = cfg.smallestScreenWidthDp < 600

    // An OS capture-and-return request uses a minimal fixed bar.
    if (external != null) {
        ExternalCaptureBar(
            settings = settings,
            isRecording = isRecording,
            isCapturing = isCapturing,
            shutterEnabled = !bgRunning,
            onCapturePhoto = onCapturePhoto,
            onToggleRecording = onToggleRecording,
            onCancel = onCancelExternal,
        )
        return
    }

    val layout =
        when {
            compact && !landscape -> Layout.PHONE_PORTRAIT
            !compact && !landscape -> Layout.LARGE_PORTRAIT
            else -> Layout.SIDE
        }

    val shared =
        SharedActions(
            viewModel = viewModel,
            settings = settings,
            availableLenses = availableLenses,
            profiles = profiles,
            bgRunning = bgRunning,
            timelapseActive = timelapseActive,
            isCapturing = isCapturing,
            isRecording = isRecording,
            gridOn = gridOn,
            mediaThumb = mediaThumb,
            hasMedia = hasMedia,
            batteryPct = batteryPct,
            zoomRatio = zoomRatio,
            maxZoom = maxZoom,
            onResetZoom = onResetZoom,
            onToggleGrid = onToggleGrid,
            onOpenGallery = onOpenGallery,
            onCapturePhoto = onCapturePhoto,
            onToggleRecording = onToggleRecording,
            onToggleTimelapse = onToggleTimelapse,
            panoActive = panoActive,
            panoCount = panoCount,
            panoMax = panoMax,
            onTogglePanorama = onTogglePanorama,
            onCancelPanorama = onCancelPanorama,
            onToggleBackground = onToggleBackground,
            onOpenSettings = onOpenSettings,
            modeBar = modeBar,
            onSetModeBar = onSetModeBar,
        )

    // Pro (manual) mode is composed by CameraScreen (ProScreen layouts);
    // this overlay only serves the normal phone/tablet panels.
    when (layout) {
        Layout.PHONE_PORTRAIT -> PhonePortrait(shared)
        Layout.LARGE_PORTRAIT ->
            SidePanelLayout(
                shared = shared,
                onLeft = true,
                twoColumnIcons = true,
                verticalTabs = true,
                allowGravity = true,
                gravity = panelGravity,
                collapsed = panelCollapsed,
                onSetCollapsed = onSetPanelCollapsed,
                onSetGravity = onSetPanelGravity,
            )
        Layout.SIDE ->
            SidePanelLayout(
                shared = shared,
                onLeft = false,
                twoColumnIcons = false,
                verticalTabs = false,
                allowGravity = false,
                gravity = 1,
                collapsed = panelCollapsed,
                onSetCollapsed = onSetPanelCollapsed,
                onSetGravity = onSetPanelGravity,
            )
    }
}

/** Bundle of state + callbacks passed to the per-layout composables. */
internal class SharedActions(
    val viewModel: CameraViewModel,
    val settings: CameraSettings,
    val availableLenses: List<CameraLensInfo>,
    val profiles: List<CameraProfile>,
    val bgRunning: Boolean,
    val timelapseActive: Boolean,
    val isCapturing: Boolean,
    val isRecording: Boolean,
    val gridOn: Boolean,
    val mediaThumb: ImageBitmap?,
    val hasMedia: Boolean,
    val batteryPct: Int?,
    val zoomRatio: Float,
    val maxZoom: Float,
    val onResetZoom: () -> Unit,
    val onToggleGrid: () -> Unit,
    val onOpenGallery: () -> Unit,
    val onCapturePhoto: () -> Unit,
    val onToggleRecording: () -> Unit,
    val onToggleTimelapse: () -> Unit,
    val panoActive: Boolean,
    val panoCount: Int,
    val panoMax: Int,
    val onTogglePanorama: () -> Unit,
    val onCancelPanorama: () -> Unit,
    val onToggleBackground: () -> Unit,
    val onOpenSettings: () -> Unit,
    val modeBar: List<CameraMode>,
    val onSetModeBar: (List<CameraMode>) -> Unit,
) {
    /** Central shutter dispatch shared by every layout. */
    fun onShutter() {
        when (settings.cameraMode) {
            CameraMode.VIDEO, CameraMode.SLOWMO -> onToggleRecording()
            CameraMode.PANORAMA -> onTogglePanorama()
            else -> onCapturePhoto()
        }
    }

    fun isRecordMode(): Boolean =
        settings.cameraMode == CameraMode.VIDEO || settings.cameraMode == CameraMode.SLOWMO
}

// ============================ PHONE PORTRAIT ============================

@Composable
private fun PhonePortrait(s: SharedActions) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
        ControlIcons(
            s = s,
            columns = 1,
            modifier =
                Modifier.fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(4.dp),
        )
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.72f))
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (s.settings.cameraMode == CameraMode.PANORAMA) {
                PanoProgressRow(s)
            } else if (s.settings.cameraMode == CameraMode.OTHERS) {
                OthersMenu(s = s, twoPerRow = false)
            } else {
                // Pinch-zoom pill only once zoomed, so the idle view stays clean.
                if (s.zoomRatio > 1.01f) {
                    ZoomPill(
                        zoomRatio = s.zoomRatio,
                        maxZoom = s.maxZoom,
                        enabled = !s.bgRunning,
                        onReset = s.onResetZoom,
                    )
                }
                LensZoomPills(
                    lenses = s.availableLenses.filter { it.isFront == s.settings.isFrontCamera },
                    current = s.settings.currentLens,
                    enabled = !s.isRecording && !s.bgRunning,
                    onSelect = { s.viewModel.setLens(it) },
                )
            }
            IphoneModeTabs(s)
            ShutterBar(s)
        }
    }
}

/** PHOTO / VIDEO VAL still-photo modes share the manual panel. */
private fun isStillMode(mode: CameraMode): Boolean =
    mode == CameraMode.PHOTO || mode == CameraMode.VIDEO

/** Bottom shutter bar: thumbnail | large shutter | front-back switch. */
@Composable
internal fun ShutterBar(s: SharedActions) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        MediaThumbButton(s.mediaThumb, s.hasMedia, s.onOpenGallery, size = 48.dp)
        ShutterButton(
            isVideo = s.isRecordMode(),
            isRecording = s.isRecording,
            isCapturing = s.isCapturing,
            enabled = !s.bgRunning,
            size = 78.dp,
            onClick = { s.onShutter() },
        )
        FrontBackButton(s)
    }
}

/** Per-lens zoom pills ("×0.5" / "×1" / "×2") relative to the wide lens. */
@Composable
private fun LensZoomPills(
    lenses: List<CameraLensInfo>,
    current: CameraLensInfo?,
    enabled: Boolean,
    onSelect: (CameraLensInfo) -> Unit,
) {
    if (lenses.size <= 1) return
    val base =
        lenses.firstOrNull { it.type == CameraLensType.WIDE }?.focalLength
            ?: lenses.minOf { it.focalLength }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        lenses.forEach { lens ->
            val selected = current?.id == lens.id
            val ratio = lens.focalLength / base.coerceAtLeast(0.1f)
            val label =
                if (kotlin.math.abs(ratio - 1f) < 0.05f) "×1"
                else "×%.1f".format(ratio)
            Box(
                modifier =
                    Modifier.size(44.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color.White else Color.White.copy(alpha = 0.16f))
                        .clickable(enabled = enabled) { onSelect(lens) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color =
                        if (selected) Color.Black
                        else Color.White.copy(alpha = if (enabled) 1f else 0.4f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}

/** iPhone-style centered mode selector. */
@Composable
internal fun IphoneModeTabs(s: SharedActions) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(s.modeBar, key = { it.name }) { mode ->
            DraggableModeTab(s, mode, vertical = false)
        }
    }
}

/** Panorama sweep progress + finish/cancel, shown above the mode tabs. */
@Composable
private fun PanoProgressRow(s: SharedActions) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (s.panoActive) {
            Text(
                stringResource(R.string.pano_progress, s.panoCount, s.panoMax),
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.width(12.dp))
            TextButton(onClick = s.onTogglePanorama) {
                Text(
                    stringResource(R.string.pano_stop),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            TextButton(onClick = s.onCancelPanorama) {
                Text(
                    stringResource(R.string.action_cancel),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        } else {
            Text(
                stringResource(R.string.pano_hint),
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

// ============================ SIDE / LARGE PORTRAIT ============================

@Composable
private fun SidePanelLayout(
    shared: SharedActions,
    onLeft: Boolean,
    twoColumnIcons: Boolean,
    verticalTabs: Boolean,
    allowGravity: Boolean,
    gravity: Int,
    collapsed: Boolean,
    onSetCollapsed: (Boolean) -> Unit,
    onSetGravity: (Int) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        if (collapsed) {
            CollapsedCluster(
                s = shared,
                anchor =
                    if (onLeft) Alignment.CenterStart else Alignment.CenterEnd,
                onExpand = { onSetCollapsed(false) },
            )
        } else {
            val panelAlign =
                when {
                    !allowGravity -> if (onLeft) Alignment.CenterStart else Alignment.CenterEnd
                    gravity == 0 -> Alignment.TopStart
                    gravity == 1 -> Alignment.CenterStart
                    else -> Alignment.BottomStart
                }
            val side = !allowGravity
            Column(
                modifier =
                    Modifier.align(panelAlign)
                        .widthIn(max = 380.dp)
                        .then(if (side) Modifier.fillMaxHeight() else Modifier.wrapContentHeight())
                        .background(Color.Black.copy(alpha = 0.6f))
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ControlIcons(s = shared, columns = if (twoColumnIcons) 2 else 1)
                PanelBody(s = shared, verticalTabs = verticalTabs, othersTwoPerRow = true)

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (allowGravity) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            GravityButton(R.string.panel_pos_top, gravity == 0) { onSetGravity(0) }
                            GravityButton(R.string.panel_pos_center, gravity == 1) { onSetGravity(1) }
                            GravityButton(R.string.panel_pos_bottom, gravity == 2) { onSetGravity(2) }
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    IconButton(onClick = { onSetCollapsed(true) }) {
                        Icon(
                            if (onLeft) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
                            contentDescription = stringResource(R.string.cd_panel_close),
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GravityButton(labelRes: Int, selected: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick) {
        Text(
            stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.CollapsedCluster(
    s: SharedActions,
    anchor: Alignment,
    onExpand: () -> Unit,
) {
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val cfg = LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val cdControls = stringResource(R.string.cd_controls)
    // Keep the draggable cluster from leaving the screen.
    val maxX = with(density) { (cfg.screenWidthDp.dp.toPx() * 0.42f) }
    val maxY = with(density) { (cfg.screenHeightDp.dp.toPx() * 0.42f) }
    Column(
        modifier =
            Modifier.align(anchor)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(12.dp)
                .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                .pointerInput(maxX, maxY) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        offset =
                            androidx.compose.ui.geometry.Offset(
                                (offset.x + drag.x).coerceIn(-maxX, maxX),
                                (offset.y + drag.y).coerceIn(-maxY, maxY),
                            )
                    }
                }
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(8.dp)
                .semantics { contentDescription = cdControls },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        IconButton(onClick = onExpand) {
            Icon(
                Icons.Default.UnfoldMore,
                contentDescription = stringResource(R.string.cd_panel_open),
                tint = Color.White,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FrontBackButton(s)
            ShutterButton(
                isVideo = s.isRecordMode(),
                isRecording = s.isRecording,
                isCapturing = s.isCapturing,
                enabled = !s.bgRunning,
                onClick = { s.onShutter() },
            )
        }
        BatteryPill(s.batteryPct)
    }
}

// ============================ PANEL BODY (shared) ============================

@Composable
private fun PanelBody(s: SharedActions, verticalTabs: Boolean, othersTwoPerRow: Boolean) {
    // Manual mode is rendered by the dedicated pro screen (ProScreen.kt);
    // this shared body only serves non-manual phone/tablet panels.
    val lenses = s.availableLenses.filter { it.isFront == s.settings.isFrontCamera }
    // Pinch-zoom state is always visible here when the lens supports zoom, so the
    // current ratio is discoverable and one tap restores the 1.0x startup state.
    // While the background service owns the camera the pill is shown disabled.
    ZoomPill(
        zoomRatio = s.zoomRatio,
        maxZoom = s.maxZoom,
        enabled = !s.bgRunning,
        onReset = s.onResetZoom,
    )
    if (lenses.size > 1) {
        LensRow(
            lenses = lenses,
            current = s.settings.currentLens,
            enabled = !s.isRecording && !s.bgRunning,
            onSelect = { s.viewModel.setLens(it) },
        )
    }

    Box(Modifier.fillMaxWidth().heightIn(min = 96.dp), contentAlignment = Alignment.Center) {
        if (s.settings.cameraMode == CameraMode.OTHERS) {
            OthersMenu(s = s, twoPerRow = othersTwoPerRow)
        } else if (s.settings.cameraMode == CameraMode.PANORAMA) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PanoProgressRow(s)
                ShutterButton(
                    isVideo = false,
                    isRecording = false,
                    isCapturing = s.isCapturing,
                    enabled = !s.bgRunning,
                    onClick = { s.onShutter() },
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                FrontBackButton(s)
                ShutterButton(
                    isVideo = s.isRecordMode(),
                    isRecording = s.isRecording,
                    isCapturing = s.isCapturing,
                    enabled = !s.bgRunning,
                    onClick = { s.onShutter() },
                )
                BatteryPill(s.batteryPct)
            }
        }
    }

    if (verticalTabs) ModeTabsColumn(s) else ModeTabsRow(s)

    // Latest capture — bottom of the panel.
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MediaThumbButton(s.mediaThumb, s.hasMedia, s.onOpenGallery)
    }
}

// ============================ ICONS ============================

@Composable
internal fun ControlIcons(s: SharedActions, columns: Int, modifier: Modifier = Modifier) {
    val photoOrVideo = isStillMode(s.settings.cameraMode)
    val caps = s.settings.currentLens?.capabilities

    val icons = buildList<@Composable () -> Unit> {
        if (photoOrVideo) {
            add {
                IconButton(
                    onClick = { s.viewModel.toggleManualMode() },
                    enabled = caps?.supportsManualSensor == true,
                ) {
                    Icon(
                        if (s.settings.isManualMode) Icons.Default.Tune else Icons.Default.AutoMode,
                        contentDescription = stringResource(R.string.cd_manual_mode),
                        tint =
                            if (s.settings.isManualMode) MaterialTheme.colorScheme.primary
                            else Color.White,
                    )
                }
            }
        }
        add {
            IconButton(onClick = { s.viewModel.cycleFlashMode() }) {
                Icon(
                    when (s.settings.flashMode) {
                        ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                        ImageCapture.FLASH_MODE_OFF -> Icons.Default.FlashOff
                        else -> Icons.Default.FlashAuto
                    },
                    contentDescription = stringResource(R.string.cd_flash_mode),
                    tint = Color.White,
                )
            }
        }
        add {
            IconButton(onClick = { s.viewModel.cycleTimer() }) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = stringResource(R.string.cd_timer),
                        tint =
                            if (s.settings.timerSeconds > 0) MaterialTheme.colorScheme.primary
                            else Color.White,
                    )
                    if (s.settings.timerSeconds > 0) {
                        Text(
                            s.settings.timerSeconds.toString(),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
        add {
            IconButton(onClick = { s.viewModel.cycleAspectRatio() }) {
                Icon(
                    if (s.settings.aspectRatio == AspectRatio.RATIO_16_9) Icons.Default.Crop169
                    else Icons.Default.Crop54,
                    contentDescription = stringResource(R.string.cd_aspect_ratio),
                    tint = Color.White,
                )
            }
        }
        add {
            IconButton(onClick = s.onToggleGrid) {
                Icon(
                    if (s.gridOn) Icons.Default.GridOn else Icons.Default.GridOff,
                    contentDescription = stringResource(R.string.cd_grid),
                    tint = if (s.gridOn) MaterialTheme.colorScheme.primary else Color.White,
                )
            }
        }
        add {
            IconButton(onClick = { s.viewModel.toggleAeAfLock() }) {
                Icon(
                    if (s.settings.aeAfLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = stringResource(R.string.cd_ae_af_lock),
                    tint =
                        if (s.settings.aeAfLocked) MaterialTheme.colorScheme.primary else Color.White,
                )
            }
        }
        add {
            IconButton(onClick = s.onOpenSettings) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = stringResource(R.string.cd_settings),
                    tint = Color.White,
                )
            }
        }
    }

    if (columns <= 1) {
        Row(
            modifier =
                if (modifier == Modifier) Modifier.fillMaxWidth() else modifier,
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icons.forEach { it() }
        }
    } else {
        Column(
            modifier = if (modifier == Modifier) Modifier.fillMaxWidth() else modifier,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            icons.chunked(columns).forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    row.forEach { it() }
                    if (row.size < columns) repeat(columns - row.size) { Spacer(Modifier.size(48.dp)) }
                }
            }
        }
    }
}

@Composable
internal fun FrontBackButton(s: SharedActions) {
    IconButton(onClick = { s.viewModel.toggleFrontCamera() }) {
        Icon(
            Icons.Default.Cameraswitch,
            contentDescription = stringResource(R.string.cd_switch_camera),
            tint = Color.White,
        )
    }
}

@Composable
internal fun BatteryPill(pct: Int?) {
    if (pct == null) return
    val cdBattery = stringResource(R.string.cd_battery)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics { contentDescription = cdBattery },
    ) {
        Icon(
            Icons.Default.BatteryStd,
            contentDescription = stringResource(R.string.cd_battery),
            tint = if (pct <= 15) MaterialTheme.colorScheme.error else Color.White,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(2.dp))
        Text(
            "$pct%",
            color = if (pct <= 15) MaterialTheme.colorScheme.error else Color.White,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

/** Current pinch-zoom ratio. Hidden when the lens reports no zoom range. */
@Composable
private fun ZoomPill(zoomRatio: Float, maxZoom: Float, enabled: Boolean, onReset: () -> Unit) {
    if (maxZoom <= 1.01f) return
    val zoomed = zoomRatio > 1.01f
    val cdZoom = stringResource(R.string.cd_zoom)
    val cdReset = stringResource(R.string.cd_zoom_reset)
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .semantics { contentDescription = cdZoom }
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier.clip(RoundedCornerShape(16.dp))
                    .background(
                        if (zoomed) MaterialTheme.colorScheme.primary
                        else Color.White.copy(alpha = 0.12f)
                    )
                    .clickable(enabled = enabled && zoomed, onClick = onReset)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription =
                            if (zoomed) cdReset else cdZoom
                    },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.zoom_ratio, zoomRatio),
                color =
                    if (zoomed) MaterialTheme.colorScheme.onPrimary
                    else Color.White.copy(alpha = if (enabled) 1f else 0.4f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (zoomed) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun MediaThumbButton(
    thumb: ImageBitmap?,
    hasMedia: Boolean,
    onClick: () -> Unit,
    size: Dp = 56.dp,
) {
    val cd = stringResource(R.string.cd_latest_capture)
    Box(
        modifier =
            Modifier.size(size)
                .semantics { contentDescription = cd }
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = if (hasMedia) 0.18f else 0.06f))
                .border(
                    1.dp,
                    Color.White.copy(alpha = if (hasMedia) 0.6f else 0.2f),
                    RoundedCornerShape(10.dp),
                )
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (thumb != null) {
            Image(
                bitmap = thumb,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
            )
        } else {
            Icon(
                Icons.Default.PhotoLibrary,
                contentDescription = null,
                tint = Color.White.copy(alpha = if (hasMedia) 1f else 0.4f),
            )
        }
    }
}

// ============================ LENS ROW ============================

@Composable
private fun LensRow(
    lenses: List<CameraLensInfo>,
    current: CameraLensInfo?,
    enabled: Boolean,
    onSelect: (CameraLensInfo) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        items(lenses, key = { it.id }) { lens ->
            val selected = current?.id == lens.id
            Box(
                modifier =
                    Modifier.size(46.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary else Color.DarkGray
                        )
                        .clickable(enabled = enabled) { onSelect(lens) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    lensLabel(lens.type),
                    color =
                        if (selected) MaterialTheme.colorScheme.onPrimary else Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                )
            }
        }
    }
}

// ============================ MODE TABS ============================

internal object ModeBarOrder {
    val default = listOf(CameraMode.PHOTO, CameraMode.VIDEO, CameraMode.OTHERS)

    fun sanitize(raw: List<CameraMode>): List<CameraMode> =
        if (raw.isEmpty()) default
        else raw.filter { it != CameraMode.OTHERS }.distinct() + CameraMode.OTHERS

    fun add(current: List<CameraMode>, mode: CameraMode): List<CameraMode> =
        sanitize(current.filter { it != CameraMode.OTHERS } + mode + CameraMode.OTHERS)

    fun remove(current: List<CameraMode>, mode: CameraMode): List<CameraMode> =
        if (mode == CameraMode.OTHERS) sanitize(current)
        else sanitize(current.filter { it != mode })

    fun move(current: List<CameraMode>, mode: CameraMode, target: Int): List<CameraMode> {
        if (mode == CameraMode.OTHERS) return sanitize(current)
        val movable = sanitize(current).filter { it != CameraMode.OTHERS }.toMutableList()
        val from = movable.indexOf(mode)
        if (from < 0) return sanitize(current)
        movable.add(target.coerceIn(0, movable.lastIndex), movable.removeAt(from))
        return movable + CameraMode.OTHERS
    }
}

@Composable
private fun ModeTabsRow(s: SharedActions) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        s.modeBar.forEach { mode -> DraggableModeTab(s, mode, vertical = false) }
    }
}

@Composable
private fun ModeTabsColumn(s: SharedActions) {
    Column(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        s.modeBar.forEach { mode -> DraggableModeTab(s, mode, vertical = true) }
    }
}

@Composable
private fun DraggableModeTab(s: SharedActions, mode: CameraMode, vertical: Boolean) {
    var dragX by remember(mode) { mutableStateOf(0f) }
    var dragY by remember(mode) { mutableStateOf(0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val removeThreshold = with(density) { 38.dp.toPx() }
    val slot = with(density) { if (vertical) 52.dp.toPx() else 92.dp.toPx() }
    val selected = s.settings.cameraMode == mode
    Column(
        modifier =
            Modifier.graphicsLayer {
                    translationX = dragX
                    translationY = dragY
                    alpha = if (dragX != 0f || dragY != 0f) 0.78f else 1f
                }
                .pointerInput(mode, s.modeBar) {
                    detectDragGestures(
                        onDrag = { change, amount ->
                            change.consume()
                            dragX += amount.x
                            dragY += amount.y
                        },
                        onDragCancel = { dragX = 0f; dragY = 0f },
                        onDragEnd = {
                            if (mode != CameraMode.OTHERS && dragY < -removeThreshold) {
                                s.onSetModeBar(ModeBarOrder.remove(s.modeBar, mode))
                            } else {
                                val axis = if (vertical) dragY else dragX
                                val from = s.modeBar.indexOf(mode)
                                if (from >= 0 && kotlin.math.abs(axis) > slot / 2f) {
                                    s.onSetModeBar(
                                        ModeBarOrder.move(
                                            s.modeBar,
                                            mode,
                                            from + (axis / slot).roundToInt(),
                                        )
                                    )
                                }
                            }
                            dragX = 0f
                            dragY = 0f
                        },
                    )
                }
                .heightIn(min = 48.dp)
                .clickable { s.viewModel.setMode(mode) }
                .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = modeIcon(mode),
            contentDescription = stringResource(modeTabRes(mode)),
            tint = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp),
        )
        Text(
            stringResource(modeTabRes(mode)),
            color = if (selected) Color.White else Color.White.copy(alpha = 0.55f),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

private fun modeIcon(mode: CameraMode): ImageVector =
    when (mode) {
        CameraMode.PHOTO -> Icons.Default.PhotoLibrary
        CameraMode.VIDEO -> Icons.Default.Videocam
        CameraMode.SLOWMO -> Icons.Default.SlowMotionVideo
        CameraMode.PANORAMA -> Icons.Default.PanoramaHorizontal
        CameraMode.OTHERS -> Icons.Default.UnfoldMore
    }

// ============================ OTHERS MENU ============================

@Composable
private fun OthersMenu(s: SharedActions, twoPerRow: Boolean) {
    val items = buildList<@Composable () -> Unit> {
        CameraMode.entries
            .filter { it != CameraMode.OTHERS && it !in s.modeBar }
            .forEach { mode ->
                add {
                    DraggableOtherMode(
                        mode = mode,
                        onClick = { s.viewModel.setMode(mode) },
                        onAdd = { s.onSetModeBar(ModeBarOrder.add(s.modeBar, mode)) },
                    )
                }
            }
        add {
            OthersMenuItem(
                Icons.Default.Timelapse,
                stringResource(
                    if (s.timelapseActive) R.string.others_timelapse_stop
                    else R.string.others_timelapse
                ),
                s.onToggleTimelapse,
            )
        }
        add {
            OthersMenuItem(
                Icons.Default.Security,
                stringResource(
                    if (s.bgRunning) R.string.others_bg_record_stop else R.string.others_bg_record
                ),
                s.onToggleBackground,
            )
        }
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.mode_drag_hint),
            color = Color.White.copy(alpha = 0.68f),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
        if (!twoPerRow) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                items.forEach { item { it() } }
            }
        } else {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items.chunked(2).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        row.forEach { it() }
                        if (row.size < 2) Spacer(Modifier.size(56.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DraggableOtherMode(mode: CameraMode, onClick: () -> Unit, onAdd: () -> Unit) {
    var dragY by remember(mode) { mutableStateOf(0f) }
    val threshold = with(androidx.compose.ui.platform.LocalDensity.current) { 38.dp.toPx() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier.graphicsLayer { translationY = dragY }
                .pointerInput(mode) {
                    detectDragGestures(
                        onDrag = { change, amount -> change.consume(); dragY += amount.y },
                        onDragCancel = { dragY = 0f },
                        onDragEnd = { if (dragY > threshold) onAdd(); dragY = 0f },
                    )
                }
                .heightIn(min = 58.dp)
                .clickable(onClick = onClick)
                .padding(8.dp),
    ) {
        Box(
            modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.DarkGray),
            contentAlignment = Alignment.Center,
        ) {
            Icon(modeIcon(mode), contentDescription = stringResource(modeTabRes(mode)), tint = Color.White)
        }
        Spacer(Modifier.height(4.dp))
        Text(stringResource(modeTabRes(mode)), color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun OthersMenuItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp),
    ) {
        Box(
            modifier = Modifier.size(52.dp).clip(CircleShape).background(Color.DarkGray),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White)
        }
        Spacer(Modifier.height(4.dp))
        Text(text = text, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}

// ============================ SHUTTER ============================

@Composable
internal fun ShutterButton(
    isVideo: Boolean,
    isRecording: Boolean,
    isCapturing: Boolean,
    enabled: Boolean = true,
    size: Dp = 72.dp,
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
            Modifier.size(size)
                .semantics { contentDescription = cd }
                .clip(CircleShape)
                .background(if (isCapturing || !enabled) Color.Gray else Color.White)
                .clickable(enabled = enabled && !isCapturing, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (isVideo) {
            Box(
                Modifier.size(size * 0.36f)
                    .clip(if (isRecording) RoundedCornerShape(4.dp) else CircleShape)
                    .background(Color.Red)
            )
        } else {
            Box(Modifier.size(size * 0.86f).clip(CircleShape).background(Color.LightGray))
        }
    }
}

// ============================ EXTERNAL CAPTURE BAR ============================

@Composable
private fun ExternalCaptureBar(
    settings: CameraSettings,
    isRecording: Boolean,
    isCapturing: Boolean,
    shutterEnabled: Boolean,
    onCapturePhoto: () -> Unit,
    onToggleRecording: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel) {
                Text(stringResource(R.string.action_cancel), color = Color.White)
            }
            ShutterButton(
                isVideo = settings.cameraMode == CameraMode.VIDEO,
                isRecording = isRecording,
                isCapturing = isCapturing,
                enabled = shutterEnabled,
                onClick = {
                    if (settings.cameraMode == CameraMode.VIDEO) onToggleRecording()
                    else onCapturePhoto()
                },
            )
            Spacer(Modifier.size(48.dp))
        }
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

internal enum class ProArrangement {
    PHONE_PORTRAIT,
    PHONE_LANDSCAPE,
    TABLET_PORTRAIT,
    TABLET_LANDSCAPE,
}

internal fun proArrangement(compact: Boolean, landscape: Boolean): ProArrangement =
    when {
        compact && !landscape -> ProArrangement.PHONE_PORTRAIT
        compact && landscape -> ProArrangement.PHONE_LANDSCAPE
        !compact && !landscape -> ProArrangement.TABLET_PORTRAIT
        else -> ProArrangement.TABLET_LANDSCAPE
    }

fun formatStorageGb(bytes: Long): String =
    if (bytes < 0) "--" else "%.1f GB".format(java.util.Locale.US, bytes / 1e9)

@Composable
internal fun SaveFormatSelector(
    selected: SaveFormat,
    bitDepth: Int?,
    onSelect: (SaveFormat) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options =
        listOf(
            SaveFormat.JPEG to R.string.format_jpeg,
            SaveFormat.JPEG_RAW to R.string.format_jpeg_raw,
            SaveFormat.RAW to R.string.format_raw,
        )
    Column(modifier = modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.label_save_format),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text =
                    if (bitDepth == null) stringResource(R.string.format_raw)
                    else stringResource(R.string.raw_bit_depth, bitDepth),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (format, res) ->
                FilterChip(
                    selected = selected == format,
                    onClick = { onSelect(format) },
                    label = { Text(stringResource(res)) },
                )
            }
        }
    }
}

@Composable
internal fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    isAuto: Boolean = false,
    onAuto: (() -> Unit)? = null,
    onChange: (Float) -> Unit,
) {
    // Degenerate HAL ranges (empty / inverted / single-point) would crash
    // coerceIn or the Slider itself — render a static row instead.
    val lo = range.start.coerceAtMost(range.endInclusive)
    val hi = range.start.coerceAtLeast(range.endInclusive)
    if (hi <= lo || !lo.isFinite() || !hi.isFinite()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
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
        }
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
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
            value = value.coerceIn(lo, hi),
            onValueChange = onChange,
            valueRange = lo..hi,
            steps = steps,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        if (onAuto != null) {
            TextButton(
                onClick = onAuto,
                enabled = !isAuto,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
            ) {
                Text(stringResource(R.string.manual_set_auto), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
