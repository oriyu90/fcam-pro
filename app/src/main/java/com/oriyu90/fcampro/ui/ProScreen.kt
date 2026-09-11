package com.oriyu90.fcampro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oriyu90.fcampro.R
import com.oriyu90.fcampro.data.CameraProfile
import kotlin.math.roundToInt

/** Pro settings item selected via the icon row/strip. */
enum class ProItem { ISO, SHUTTER, FOCUS, WB, EV, FORMAT, MIC, PROFILES }

internal fun modeTabRes(mode: CameraMode): Int =
    when (mode) {
        CameraMode.PHOTO -> R.string.tab_photo
        CameraMode.VIDEO -> R.string.tab_video
        CameraMode.SLOWMO -> R.string.tab_slowmo
        CameraMode.PANORAMA -> R.string.tab_panorama
        CameraMode.OTHERS -> R.string.tab_others
    }

private fun ProItem.code(): String =
    when (this) {
        ProItem.ISO -> "ISO"
        ProItem.SHUTTER -> "SS"
        ProItem.FOCUS -> "AF"
        ProItem.WB -> "WB"
        ProItem.EV -> "EV"
        ProItem.FORMAT -> "FMT"
        ProItem.MIC -> "MIC"
        ProItem.PROFILES -> "PRF"
    }

private fun ProItem.labelRes(): Int =
    when (this) {
        ProItem.ISO -> R.string.label_iso
        ProItem.SHUTTER -> R.string.label_shutter
        ProItem.FOCUS -> R.string.label_focus
        ProItem.WB -> R.string.label_wb
        ProItem.EV -> R.string.label_ev
        ProItem.FORMAT -> R.string.label_save_format
        ProItem.MIC -> R.string.label_mic
        ProItem.PROFILES -> R.string.save_profile
    }

/** Dots under icons with non-default values. */
private fun ProItem.isModified(s: SharedActions): Boolean =
    when (this) {
        ProItem.ISO, ProItem.SHUTTER -> s.settings.iso != null
        ProItem.FOCUS -> s.settings.focusDistance != null
        ProItem.WB -> s.settings.whiteBalanceMode != null
        ProItem.EV -> s.settings.exposureCompensation != 0
        ProItem.FORMAT -> s.settings.saveFormat != SaveFormat.JPEG
        ProItem.MIC -> s.settings.audioChannels == 2
        ProItem.PROFILES -> false
    }

private fun proItemsFor(caps: LensCapabilities?): List<ProItem> =
    ProItem.entries.filter { item ->
        item != ProItem.FORMAT || caps?.rawCapability?.supported == true
    }

/** Toolbar pinned to the top (portrait) / panel top (landscape). */
@Composable
internal fun ProToolbarBlock(s: SharedActions, modifier: Modifier = Modifier) {
    ControlIcons(s = s, columns = 1, modifier = modifier)
}

/** Plain status texts at the preview's top corner (no popups). */
@Composable
internal fun ProStatusTexts(s: SharedActions, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val freeText = remember(s.hasMedia) { formatStorageGb(freeStorageBytes(context)) }
    Row(
        modifier = modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        s.batteryPct?.let {
            Text(
                "$it%",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
        Text(
            stringResource(modeTabRes(s.settings.cameraMode)),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Text(freeText, color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        s.settings.currentLens?.let {
            Text(
                "%.0fmm".format(java.util.Locale.US, it.focalLength),
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

private fun freeStorageBytes(context: android.content.Context): Long =
    runCatching { android.os.StatFs(context.filesDir.path).availableBytes }.getOrDefault(-1L)

/** Small current-settings summary directly under the preview. */
@Composable
internal fun ProSummaryLine(s: SharedActions, modifier: Modifier = Modifier) {
    val caps = s.settings.currentLens?.capabilities
    val evStep = caps?.exposureCompStep?.takeIf { it > 0f } ?: (1f / 3f)
    val ss =
        s.settings.shutterSpeedNs?.let { "1/${(1_000_000_000L / it).coerceAtLeast(1)}" }
            ?: stringResource(R.string.value_auto)
    val f = caps?.apertures?.firstOrNull()?.let { "F%.1f".format(java.util.Locale.US, it) } ?: "--"
    val ev = ExposureComp.evText(s.settings.exposureCompensation, evStep)
    val iso = s.settings.iso?.toString() ?: stringResource(R.string.value_auto)
    Text(
        text = "SS $ss · $f · EV $ev · ISO $iso",
        color = Color.White.copy(alpha = 0.85f),
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(vertical = 4.dp),
    )
}

/** Vertical plain-text lens switcher on the preview's right side. */
@Composable
internal fun ProLensTexts(s: SharedActions, modifier: Modifier = Modifier) {
    val lenses = s.availableLenses.filter { it.isFront == s.settings.isFrontCamera }
    if (lenses.size <= 1) return
    val base =
        lenses.firstOrNull { it.type == CameraLensType.WIDE }?.focalLength
            ?: lenses.minOf { it.focalLength }
    val enabled = !s.isRecording && !s.bgRunning
    Column(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        lenses.forEach { lens ->
            val selected = s.settings.currentLens?.id == lens.id
            val ratio = lens.focalLength / base.coerceAtLeast(0.1f)
            Text(
                text = "×%.1f".format(java.util.Locale.US, ratio),
                color =
                    when {
                        selected -> Color.White
                        !enabled -> Color.White.copy(alpha = 0.3f)
                        else -> Color.White.copy(alpha = 0.6f)
                    },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                modifier =
                    Modifier.clickable(enabled = enabled) { s.viewModel.setLens(lens) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}

/** Settings icon row (portrait) or vertical strip (landscape gap). */
@Composable
internal fun ProIconSelector(
    s: SharedActions,
    active: ProItem?,
    onSelect: (ProItem?) -> Unit,
    vertical: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val items = proItemsFor(s.settings.currentLens?.capabilities)
    if (vertical) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items.forEach { item -> ProIconButton(s, item, active == item, { onSelect(if (active == item) null else item) }) }
        }
    } else {
        // Two rows of four: all eight icons visible without scrolling.
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items.chunked(4).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    row.forEach { item ->
                        ProIconButton(s, item, active == item, { onSelect(if (active == item) null else item) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ProIconButton(
    s: SharedActions,
    item: ProItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val cd = stringResource(item.labelRes())
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier =
                Modifier.size(52.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) MaterialTheme.colorScheme.primary
                        else Color.White.copy(alpha = 0.14f)
                    )
                    .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                item.code(),
                color =
                    if (selected) MaterialTheme.colorScheme.onPrimary
                    else Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(3.dp))
        Box(
            modifier =
                Modifier.size(5.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.isModified(s)) MaterialTheme.colorScheme.primary
                        else Color.Transparent
                    )
        )
    }
}

/** The selected item's control UI (or a hint when nothing is selected). */
@Composable
internal fun ProControlArea(
    s: SharedActions,
    active: ProItem?,
    modifier: Modifier = Modifier,
) {
    val caps = s.settings.currentLens?.capabilities
    Column(modifier = modifier.fillMaxWidth()) {
        when (active) {
            null ->
                Text(
                    stringResource(R.string.pro_hint_select),
                    color = Color.White.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            ProItem.ISO -> {
                val isoRange = caps?.isoRange ?: 50..3200
                LabeledSlider(
                    label = stringResource(R.string.label_iso),
                    valueText = s.settings.iso?.toString() ?: stringResource(R.string.value_auto),
                    value = s.settings.iso?.toFloat() ?: isoRange.first.toFloat(),
                    range = isoRange.first.toFloat()..isoRange.last.toFloat(),
                    isAuto = s.settings.iso == null,
                    onAuto = { s.viewModel.clearExposureManual() },
                    onChange = {
                        s.viewModel.updateManualSettings(
                            it.toInt(),
                            s.settings.shutterSpeedNs,
                            s.settings.focusDistance,
                            s.settings.whiteBalanceMode,
                        )
                    },
                )
            }
            ProItem.SHUTTER -> {
                val expRange = caps?.exposureRangeNs ?: 125_000L..1_000_000_000L
                val text =
                    s.settings.shutterSpeedNs?.let { "1/${(1_000_000_000L / it).coerceAtLeast(1)}s" }
                        ?: stringResource(R.string.value_auto)
                LabeledSlider(
                    label = stringResource(R.string.label_shutter),
                    valueText = text,
                    value = s.settings.shutterSpeedNs?.toFloat() ?: expRange.first.toFloat(),
                    range = expRange.first.toFloat()..expRange.last.toFloat(),
                    isAuto = s.settings.shutterSpeedNs == null,
                    onAuto = { s.viewModel.clearExposureManual() },
                    onChange = {
                        s.viewModel.updateManualSettings(
                            s.settings.iso,
                            it.toLong(),
                            s.settings.focusDistance,
                            s.settings.whiteBalanceMode,
                        )
                    },
                )
            }
            ProItem.FOCUS -> {
                val focusMax = caps?.minFocusDistance?.takeIf { it > 0f } ?: 10f
                LabeledSlider(
                    label = stringResource(R.string.label_focus),
                    valueText =
                        s.settings.focusDistance?.let { "%.1f".format(it) }
                            ?: stringResource(R.string.value_auto),
                    value = s.settings.focusDistance ?: 0f,
                    range = 0f..focusMax,
                    isAuto = s.settings.focusDistance == null,
                    onAuto = {
                        s.viewModel.updateManualSettings(
                            s.settings.iso,
                            s.settings.shutterSpeedNs,
                            null,
                            s.settings.whiteBalanceMode,
                        )
                    },
                    onChange = {
                        s.viewModel.updateManualSettings(
                            s.settings.iso,
                            s.settings.shutterSpeedNs,
                            it,
                            s.settings.whiteBalanceMode,
                        )
                    },
                )
            }
            ProItem.WB -> {
                if (caps != null && caps.awbModes.size > 1) {
                    LabeledSlider(
                        label = stringResource(R.string.label_wb),
                        valueText =
                            s.settings.whiteBalanceMode?.toString()
                                ?: stringResource(R.string.value_auto),
                        value = s.settings.whiteBalanceMode?.toFloat() ?: 1f,
                        range = 1f..8f,
                        steps = 6,
                        isAuto = s.settings.whiteBalanceMode == null,
                        onAuto = {
                            s.viewModel.updateManualSettings(
                                s.settings.iso,
                                s.settings.shutterSpeedNs,
                                s.settings.focusDistance,
                                null,
                            )
                        },
                        onChange = {
                            s.viewModel.updateManualSettings(
                                s.settings.iso,
                                s.settings.shutterSpeedNs,
                                s.settings.focusDistance,
                                it.toInt(),
                            )
                        },
                    )
                } else {
                    Text(
                        stringResource(R.string.manual_unsupported),
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }
            ProItem.EV -> {
                if (s.settings.iso == null && s.settings.shutterSpeedNs == null &&
                    caps?.exposureCompRange != null
                ) {
                    val evRange = caps.exposureCompRange
                    val evStep = caps.exposureCompStep.takeIf { it > 0f } ?: (1f / 3f)
                    LabeledSlider(
                        label = stringResource(R.string.label_ev),
                        valueText = ExposureComp.evText(s.settings.exposureCompensation, evStep),
                        value = s.settings.exposureCompensation.toFloat(),
                        range = evRange.first.toFloat()..evRange.last.toFloat(),
                        steps = (evRange.last - evRange.first - 1).coerceAtLeast(0),
                        onChange = { s.viewModel.updateExposureCompensation(it.roundToInt()) },
                    )
                } else {
                    Text(
                        stringResource(R.string.pro_ev_disabled),
                        color = Color.White.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }
            ProItem.FORMAT -> {
                SaveFormatSelector(
                    selected = s.settings.saveFormat,
                    bitDepth = caps?.rawCapability?.bitDepth,
                    onSelect = { s.viewModel.setSaveFormat(it) },
                )
            }
            ProItem.MIC -> {
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
                                    if (s.settings.audioChannels == 2) R.string.mic_stereo
                                    else R.string.mic_mono
                                ),
                        color = Color.White,
                        modifier = Modifier.width(120.dp),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    androidx.compose.material3.Switch(
                        checked = s.settings.audioChannels == 2,
                        onCheckedChange = { s.viewModel.cycleAudioChannels() },
                    )
                }
            }
            ProItem.PROFILES -> ProProfilesBlock(s)
        }
    }
}

@Composable
private fun ProProfilesBlock(s: SharedActions) {
    var showSave by remember { mutableStateOf(false) }
    var editProfile by remember { mutableStateOf<CameraProfile?>(null) }
    Column(Modifier.fillMaxWidth()) {
        Button(
            onClick = { showSave = true },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(stringResource(R.string.save_profile))
        }
        if (s.profiles.isNotEmpty()) {
            LazyRow(modifier = Modifier.padding(vertical = 8.dp)) {
                items(s.profiles, key = { it.id }) { profile ->
                    ElevatedFilterChip(
                        selected = false,
                        onClick = { s.viewModel.loadProfile(profile) },
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
                                onClick = { s.viewModel.deleteProfile(profile.id) },
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
                Button(onClick = { s.viewModel.saveProfile(name); showSave = false }) {
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
                Button(onClick = { s.viewModel.updateProfileName(p, name); editProfile = null }) {
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
