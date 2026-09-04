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
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    panelCollapsed: Boolean,
    panelGravity: Int,
    onSetPanelCollapsed: (Boolean) -> Unit,
    onSetPanelGravity: (Int) -> Unit,
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
    val cfg = LocalConfiguration.current
    val landscape = cfg.orientation == Configuration.ORIENTATION_LANDSCAPE
    val compact = cfg.smallestScreenWidthDp < 600

    // An OS capture-and-return request uses a minimal fixed bar.
    if (external != null) {
        ExternalCaptureBar(
            settings = settings,
            isRecording = isRecording,
            isCapturing = isCapturing,
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
            onToggleGrid = onToggleGrid,
            onOpenGallery = onOpenGallery,
            onCapturePhoto = onCapturePhoto,
            onToggleRecording = onToggleRecording,
            onToggleTimelapse = onToggleTimelapse,
            onSlowMo = onSlowMo,
            onPanorama = onPanorama,
            onToggleBackground = onToggleBackground,
            onOpenSettings = onOpenSettings,
        )

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
private class SharedActions(
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
    val onToggleGrid: () -> Unit,
    val onOpenGallery: () -> Unit,
    val onCapturePhoto: () -> Unit,
    val onToggleRecording: () -> Unit,
    val onToggleTimelapse: () -> Unit,
    val onSlowMo: () -> Unit,
    val onPanorama: () -> Unit,
    val onToggleBackground: () -> Unit,
    val onOpenSettings: () -> Unit,
)

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
                    .background(Color.Black.copy(alpha = 0.5f))
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(12.dp)
        ) {
            PanelBody(s = s, verticalTabs = false, othersTwoPerRow = false)
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
                .semantics { contentDescription = "controls" },
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
                isVideo = s.settings.cameraMode == CameraMode.VIDEO,
                isRecording = s.isRecording,
                isCapturing = s.isCapturing,
                onClick = {
                    if (s.settings.cameraMode == CameraMode.VIDEO) s.onToggleRecording()
                    else s.onCapturePhoto()
                },
            )
        }
        BatteryPill(s.batteryPct)
    }
}

// ============================ PANEL BODY (shared) ============================

@Composable
private fun PanelBody(s: SharedActions, verticalTabs: Boolean, othersTwoPerRow: Boolean) {
    val photoOrVideo =
        s.settings.cameraMode == CameraMode.PHOTO || s.settings.cameraMode == CameraMode.VIDEO

    if (s.settings.isManualMode && photoOrVideo) {
        ManualPanel(s.viewModel, s.settings, s.profiles)
    }

    val lenses = s.availableLenses.filter { it.isFront == s.settings.isFrontCamera }
    if (lenses.size > 1) {
        LensRow(
            lenses = lenses,
            current = s.settings.currentLens,
            enabled = !s.isRecording,
            onSelect = { s.viewModel.setLens(it) },
        )
    }

    Box(Modifier.fillMaxWidth().heightIn(min = 96.dp), contentAlignment = Alignment.Center) {
        if (s.settings.cameraMode == CameraMode.OTHERS) {
            OthersMenu(s = s, twoPerRow = othersTwoPerRow)
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                FrontBackButton(s)
                ShutterButton(
                    isVideo = s.settings.cameraMode == CameraMode.VIDEO,
                    isRecording = s.isRecording,
                    isCapturing = s.isCapturing,
                    onClick = {
                        if (s.settings.cameraMode == CameraMode.VIDEO) s.onToggleRecording()
                        else s.onCapturePhoto()
                    },
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
private fun ControlIcons(s: SharedActions, columns: Int, modifier: Modifier = Modifier) {
    val photoOrVideo =
        s.settings.cameraMode == CameraMode.PHOTO || s.settings.cameraMode == CameraMode.VIDEO
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
private fun FrontBackButton(s: SharedActions) {
    IconButton(onClick = { s.viewModel.toggleFrontCamera() }) {
        Icon(
            Icons.Default.Cameraswitch,
            contentDescription = stringResource(R.string.cd_switch_camera),
            tint = Color.White,
        )
    }
}

@Composable
private fun BatteryPill(pct: Int?) {
    if (pct == null) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics { contentDescription = "battery" },
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

@Composable
private fun MediaThumbButton(thumb: ImageBitmap?, hasMedia: Boolean, onClick: () -> Unit) {
    val cd = stringResource(R.string.cd_latest_capture)
    Box(
        modifier =
            Modifier.size(56.dp)
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

private val MODES =
    listOf(
        CameraMode.PHOTO to R.string.tab_photo,
        CameraMode.VIDEO to R.string.tab_video,
        CameraMode.OTHERS to R.string.tab_others,
    )

@Composable
private fun ModeTabsRow(s: SharedActions) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        MODES.forEach { (mode, res) -> ModeTab(s, mode, res) }
    }
}

@Composable
private fun ModeTabsColumn(s: SharedActions) {
    Column(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        MODES.forEach { (mode, res) -> ModeTab(s, mode, res) }
    }
}

@Composable
private fun ModeTab(s: SharedActions, mode: CameraMode, res: Int) {
    val selected = s.settings.cameraMode == mode
    TextButton(onClick = { s.viewModel.setMode(mode) }) {
        Text(
            stringResource(res),
            color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

// ============================ OTHERS MENU ============================

@Composable
private fun OthersMenu(s: SharedActions, twoPerRow: Boolean) {
    val items = buildList<@Composable () -> Unit> {
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
                Icons.Default.SlowMotionVideo,
                stringResource(R.string.others_slowmo),
                s.onSlowMo,
            )
        }
        add {
            OthersMenuItem(
                Icons.Default.PanoramaHorizontal,
                stringResource(R.string.others_panorama),
                s.onPanorama,
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

// ============================ EXTERNAL CAPTURE BAR ============================

@Composable
private fun ExternalCaptureBar(
    settings: CameraSettings,
    isRecording: Boolean,
    isCapturing: Boolean,
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
                onClick = {
                    if (settings.cameraMode == CameraMode.VIDEO) onToggleRecording()
                    else onCapturePhoto()
                },
            )
            Spacer(Modifier.size(48.dp))
        }
    }
}

// ============================ MANUAL PANEL ============================

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
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
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
                    settings.whiteBalanceMode?.toString() ?: stringResource(R.string.value_auto),
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
                                profile.name.ifBlank { stringResource(R.string.profile_unnamed) },
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
                                    contentDescription = stringResource(R.string.action_delete),
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
                Button(onClick = { viewModel.saveProfile(name); showSave = false }) {
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
                Button(onClick = { viewModel.updateProfileName(p, name); editProfile = null }) {
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
                Text(stringResource(R.string.manual_set_auto), style = MaterialTheme.typography.labelSmall)
            }
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
