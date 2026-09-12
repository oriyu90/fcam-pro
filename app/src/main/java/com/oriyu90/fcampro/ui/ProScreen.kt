package com.oriyu90.fcampro.ui

import android.hardware.camera2.CameraMetadata
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.oriyu90.fcampro.R
import com.oriyu90.fcampro.data.CameraProfile
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** Pro settings item selected from the quick-control deck. */
enum class ProItem { ISO, SHUTTER, FOCUS, WB, EV, FORMAT, MIC }

private val ProAccent = Color(0xFFFF9F0A)
private val ProSurface = Color(0xFF171717)
private val ProSelectedSurface = Color(0xFF2A2117)
private val ProBoundary = Color(0xFF626262)

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
    }

private fun ProItem.isModified(s: SharedActions): Boolean =
    when (this) {
        ProItem.ISO, ProItem.SHUTTER -> s.settings.iso != null
        ProItem.FOCUS -> s.settings.focusDistance != null
        ProItem.WB -> s.settings.whiteBalanceMode != null
        ProItem.EV -> s.settings.exposureCompensation != 0
        ProItem.FORMAT -> s.settings.saveFormat != SaveFormat.JPEG
        ProItem.MIC -> s.settings.audioChannels == 2
    }

private fun proItemsFor(s: SharedActions): List<ProItem> =
    listOf(
        ProItem.ISO,
        ProItem.SHUTTER,
        ProItem.FOCUS,
        ProItem.WB,
        ProItem.EV,
        ProItem.FORMAT,
        ProItem.MIC,
    ).filter { item ->
        (item != ProItem.FORMAT || s.settings.currentLens?.capabilities?.rawCapability?.supported == true) &&
            (item != ProItem.MIC || s.settings.cameraMode == CameraMode.VIDEO)
    }

/** Toolbar pinned above all Android preview content. */
@Composable
internal fun ProToolbarBlock(s: SharedActions, modifier: Modifier = Modifier) {
    Surface(color = Color.Black, modifier = modifier.zIndex(4f)) {
        ControlIcons(s = s, columns = 1, modifier = Modifier.fillMaxWidth())
    }
}

/** Compact camera status overlay. */
@Composable
internal fun ProStatusTexts(s: SharedActions, modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val freeText = remember(s.hasMedia) { formatStorageGb(freeStorageBytes(context)) }
    Row(
        modifier =
            modifier
                .background(Color.Black.copy(alpha = 0.58f), RoundedCornerShape(8.dp))
                .padding(horizontal = 9.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        s.batteryPct?.let {
            Text("$it%", color = Color.White, style = MaterialTheme.typography.labelSmall)
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
                "%.0fmm".format(Locale.US, it.focalLength),
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

private fun freeStorageBytes(context: android.content.Context): Long =
    runCatching { android.os.StatFs(context.filesDir.path).availableBytes }.getOrDefault(-1L)

/** Four fixed exposure readouts directly below the viewfinder. */
@Composable
internal fun ProSummaryLine(s: SharedActions, modifier: Modifier = Modifier) {
    val caps = s.settings.currentLens?.capabilities
    val evStep = caps?.exposureCompStep?.takeIf { it > 0f } ?: (1f / 3f)
    val values =
        listOf(
            "SS" to (s.settings.shutterSpeedNs?.let(ProControlPresets::shutterText) ?: stringResource(R.string.value_auto)),
            "F" to (caps?.apertures?.firstOrNull()?.let { "%.1f".format(Locale.US, it) } ?: "--"),
            "EV" to ExposureComp.evText(s.settings.exposureCompensation, evStep).removeSuffix(" EV"),
            "ISO" to (s.settings.iso?.toString() ?: stringResource(R.string.value_auto)),
        )
    Row(
        modifier = modifier.fillMaxWidth().background(Color.Black).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        values.forEach { (label, value) ->
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, color = Color.White.copy(alpha = 0.55f), style = MaterialTheme.typography.labelSmall)
                Text(
                    value,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Vertical lens switcher on the viewfinder's right edge. */
@Composable
internal fun ProLensTexts(s: SharedActions, modifier: Modifier = Modifier) {
    val lenses = s.availableLenses.filter { it.isFront == s.settings.isFrontCamera }
    if (lenses.size <= 1) return
    val base = lenses.firstOrNull { it.type == CameraLensType.WIDE }?.focalLength ?: lenses.minOf { it.focalLength }
    val enabled = !s.isRecording && !s.bgRunning
    Column(
        modifier = modifier.background(Color.Black.copy(alpha = 0.48f), RoundedCornerShape(10.dp)).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        lenses.forEach { lens ->
            val selected = s.settings.currentLens?.id == lens.id
            val ratio = lens.focalLength / base.coerceAtLeast(0.1f)
            Text(
                text = "×%.1f".format(Locale.US, ratio),
                color = if (selected) ProAccent else Color.White.copy(alpha = if (enabled) 0.72f else 0.3f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.clickable(enabled = enabled) { s.viewModel.setLens(lens) }.padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}

/** Portrait strip / landscape two-column deck; every item includes its live value. */
@Composable
internal fun ProIconSelector(
    s: SharedActions,
    active: ProItem?,
    onSelect: (ProItem?) -> Unit,
    vertical: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val entries = proItemsFor(s)
    if (vertical) {
        Column(modifier = modifier.fillMaxWidth().padding(horizontal = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            entries.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (row.size == 1) {
                        val item = row.first()
                        ProValueButton(s, item, active == item, { onSelect(if (active == item) null else item) }, Modifier.fillMaxWidth())
                    } else {
                        row.forEach { item ->
                            ProValueButton(s, item, active == item, { onSelect(if (active == item) null else item) }, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    } else {
        LazyRow(
            modifier = modifier.fillMaxWidth().background(Color.Black).padding(vertical = 6.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            items(entries, key = { it.name }) { item ->
                ProValueButton(s, item, active == item, { onSelect(if (active == item) null else item) }, Modifier.width(74.dp))
            }
        }
    }
}

@Composable
private fun ProValueButton(s: SharedActions, item: ProItem, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(item.labelRes())
    val value = proItemValue(item, s)
    val modified = item.isModified(s)
    Column(
        modifier =
            modifier.height(58.dp)
                .semantics { contentDescription = "$label, $value" }
                .background(if (selected) ProSelectedSurface else ProSurface, RoundedCornerShape(8.dp))
                .border(if (selected) 2.dp else 1.dp, if (selected) ProAccent else ProBoundary, RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(item.code(), color = if (selected || modified) ProAccent else Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
private fun proItemValue(item: ProItem, s: SharedActions): String {
    val caps = s.settings.currentLens?.capabilities
    return when (item) {
        ProItem.ISO -> s.settings.iso?.toString() ?: stringResource(R.string.value_auto)
        ProItem.SHUTTER -> s.settings.shutterSpeedNs?.let(ProControlPresets::shutterText) ?: stringResource(R.string.value_auto)
        ProItem.FOCUS -> s.settings.focusDistance?.let { "MF %.1f".format(Locale.US, it) } ?: stringResource(R.string.focus_continuous)
        ProItem.WB -> wbLabel(s.settings.whiteBalanceMode)
        ProItem.EV -> ExposureComp.evText(s.settings.exposureCompensation, caps?.exposureCompStep?.takeIf { it > 0f } ?: 1f / 3f).removeSuffix(" EV")
        ProItem.FORMAT -> when (s.settings.saveFormat) { SaveFormat.JPEG -> "JPEG"; SaveFormat.JPEG_RAW -> "J+R"; SaveFormat.RAW -> "RAW" }
        ProItem.MIC -> stringResource(if (s.settings.audioChannels == 2) R.string.mic_stereo else R.string.mic_mono)
    }
}

/** Selected setting options. */
@Composable
internal fun ProControlArea(s: SharedActions, active: ProItem?, modifier: Modifier = Modifier) {
    val caps = s.settings.currentLens?.capabilities
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        if (active == null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.pro_hint_select), color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = { s.viewModel.resetManualSettings() }) { Text(stringResource(R.string.manual_all_auto), color = ProAccent) }
            }
            return@Column
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(active.labelRes()), color = Color.White, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(proItemValue(active, s), color = ProAccent, style = MaterialTheme.typography.titleSmall)
        }
        HorizontalDivider(Modifier.padding(vertical = 6.dp), color = ProBoundary)

        when (active) {
            ProItem.ISO -> {
                val range = caps?.isoRange ?: 50..3200
                ProChoices(
                    choices = listOf(null) + ProControlPresets.isoValues(range),
                    selected = s.settings.iso,
                    label = { it?.toString() ?: stringResource(R.string.value_auto) },
                    onSelect = { value -> if (value == null) s.viewModel.clearExposureManual() else s.viewModel.updateManualSettings(value, s.settings.shutterSpeedNs, s.settings.focusDistance, s.settings.whiteBalanceMode) },
                )
            }
            ProItem.SHUTTER -> {
                val range = caps?.exposureRangeNs ?: 125_000L..1_000_000_000L
                ProChoices(
                    choices = listOf(null) + ProControlPresets.shutterValues(range),
                    selected = s.settings.shutterSpeedNs,
                    label = { it?.let(ProControlPresets::shutterText) ?: stringResource(R.string.value_auto) },
                    onSelect = { value -> if (value == null) s.viewModel.clearExposureManual() else s.viewModel.updateManualSettings(s.settings.iso, value, s.settings.focusDistance, s.settings.whiteBalanceMode) },
                )
            }
            ProItem.FOCUS -> {
                val focusMax = caps?.minFocusDistance?.takeIf { it > 0f }
                if (focusMax == null) {
                    Text(stringResource(R.string.focus_fixed), color = Color.White.copy(alpha = 0.65f))
                } else {
                    FilterChip(
                        selected = s.settings.focusDistance == null,
                        onClick = { s.viewModel.updateManualSettings(s.settings.iso, s.settings.shutterSpeedNs, null, s.settings.whiteBalanceMode) },
                        label = { Text(stringResource(R.string.focus_continuous)) },
                        leadingIcon = if (s.settings.focusDistance == null) ({ Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }) else null,
                        colors = proFilterChipColors(),
                    )
                    LabeledSlider(
                        label = stringResource(R.string.focus_near_far),
                        valueText = s.settings.focusDistance?.let { "%.1f".format(Locale.US, it) } ?: stringResource(R.string.value_auto),
                        value = s.settings.focusDistance ?: 0f,
                        range = 0f..focusMax,
                        onChange = { s.viewModel.updateManualSettings(s.settings.iso, s.settings.shutterSpeedNs, it, s.settings.whiteBalanceMode) },
                    )
                }
            }
            ProItem.WB -> {
                val modes = caps?.awbModes.orEmpty().distinct()
                if (modes.isEmpty()) Text(stringResource(R.string.manual_unsupported), color = Color.White.copy(alpha = 0.65f))
                else ProChoices(
                    choices = listOf(null) + modes.filter { it != CameraMetadata.CONTROL_AWB_MODE_OFF },
                    selected = s.settings.whiteBalanceMode,
                    label = { wbLabel(it) },
                    onSelect = { s.viewModel.updateManualSettings(s.settings.iso, s.settings.shutterSpeedNs, s.settings.focusDistance, it) },
                )
            }
            ProItem.EV -> {
                if (s.settings.iso == null && s.settings.shutterSpeedNs == null && caps?.exposureCompRange != null) {
                    val step = caps.exposureCompStep.takeIf { it > 0f } ?: 1f / 3f
                    ProChoices(
                        choices = caps.exposureCompRange.toList(),
                        selected = s.settings.exposureCompensation,
                        label = { ExposureComp.evText(it, step).removeSuffix(" EV") },
                        onSelect = s.viewModel::updateExposureCompensation,
                    )
                } else Text(stringResource(R.string.pro_ev_disabled), color = Color.White.copy(alpha = 0.65f))
            }
            ProItem.FORMAT -> SaveFormatSelector(s.settings.saveFormat, caps?.rawCapability?.bitDepth, s.viewModel::setSaveFormat)
            ProItem.MIC -> ProChoices(
                choices = listOf(1, 2),
                selected = s.settings.audioChannels,
                label = { stringResource(if (it == 2) R.string.mic_stereo else R.string.mic_mono) },
                onSelect = { if (it != s.settings.audioChannels) s.viewModel.cycleAudioChannels() },
            )
        }
    }
}

@Composable
private fun <T> ProChoices(choices: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(choices) { value ->
            val isSelected = value == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(value) },
                label = { Text(label(value), maxLines = 1) },
                leadingIcon = if (isSelected) ({ Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }) else null,
                colors = proFilterChipColors(),
            )
        }
    }
}

@Composable
private fun proFilterChipColors() =
    FilterChipDefaults.filterChipColors(
        selectedContainerColor = ProSelectedSurface,
        selectedLabelColor = Color.White,
        selectedLeadingIconColor = ProAccent,
    )

@Composable
private fun wbLabel(mode: Int?): String =
    stringResource(
        when (mode) {
            CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT -> R.string.wb_incandescent
            CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT -> R.string.wb_fluorescent
            CameraMetadata.CONTROL_AWB_MODE_WARM_FLUORESCENT -> R.string.wb_warm_fluorescent
            CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT -> R.string.wb_daylight
            CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT -> R.string.wb_cloudy
            CameraMetadata.CONTROL_AWB_MODE_TWILIGHT -> R.string.wb_twilight
            CameraMetadata.CONTROL_AWB_MODE_SHADE -> R.string.wb_shade
            else -> R.string.value_auto
        }
    )

private val ProfileColors =
    listOf(0xFFFF9F0A, 0xFFE05A47, 0xFF4FA3D1, 0xFF55A56D, 0xFFC57AD8, 0xFFE0C34F)
        .map { it.toInt() }

/** Always-visible profile dock below the selected Pro setting. */
@Composable
internal fun ProProfileDock(s: SharedActions, modifier: Modifier = Modifier) {
    var chooseSave by remember { mutableStateOf(false) }
    var createNew by remember { mutableStateOf(false) }
    var chooseOverwrite by remember { mutableStateOf(false) }
    var deleteProfile by remember { mutableStateOf<CameraProfile?>(null) }
    var editProfile by remember { mutableStateOf<CameraProfile?>(null) }

    Row(
        modifier = modifier.fillMaxWidth().height(66.dp).background(Color.Black).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ProSurface, RoundedCornerShape(10.dp))
                .border(1.dp, ProBoundary, RoundedCornerShape(10.dp))
                .clickable { chooseSave = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Save, stringResource(R.string.profile_save_or_overwrite), tint = ProAccent)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(s.profiles, key = { it.id }) { profile ->
                val index = s.profiles.indexOfFirst { it.id == profile.id }
                ProfileDockButton(
                    profile = profile,
                    selected = profile.matches(s.settings),
                    index = index,
                    count = s.profiles.size,
                    onClick = { s.viewModel.loadProfile(profile) },
                    onMove = { target -> s.viewModel.moveProfile(profile.id, target) },
                    onDelete = { deleteProfile = profile },
                )
            }
        }
    }

    if (chooseSave) {
        AlertDialog(
            onDismissRequest = { chooseSave = false },
            title = { Text(stringResource(R.string.profile_save_or_overwrite)) },
            text = { Text(stringResource(R.string.profile_save_choice_hint)) },
            confirmButton = {
                Button(onClick = { chooseSave = false; createNew = true }) {
                    Text(stringResource(R.string.profile_create_new))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = s.profiles.isNotEmpty(),
                    onClick = { chooseSave = false; chooseOverwrite = true },
                ) { Text(stringResource(R.string.profile_overwrite)) }
            },
        )
    }
    if (createNew) {
        var name by remember { mutableStateOf("") }
        var color by remember { mutableStateOf(ProfileColors.first()) }
        AlertDialog(
            onDismissRequest = { createNew = false },
            title = { Text(stringResource(R.string.profile_create_new)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.profile_name)) },
                        supportingText = if (name.isBlank()) ({ Text(stringResource(R.string.profile_name_required)) }) else null,
                        singleLine = true,
                    )
                    Text(stringResource(R.string.profile_color), style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(ProfileColors.size) { index ->
                            val swatch = ProfileColors[index]
                            val colorOptionDescription = stringResource(R.string.profile_color_option, index + 1)
                            Box(
                                Modifier.size(38.dp)
                                    .background(Color(swatch), CircleShape)
                                    .border(if (swatch == color) 3.dp else 1.dp, if (swatch == color) Color.White else ProBoundary, CircleShape)
                                    .clickable { color = swatch }
                                    .semantics { contentDescription = colorOptionDescription },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (swatch == color) Icon(Icons.Default.Check, null, Modifier.size(18.dp), tint = Color.Black)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(enabled = name.isNotBlank(), onClick = {
                    s.viewModel.saveProfile(name, color)
                    createNew = false
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { createNew = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    if (chooseOverwrite) {
        AlertDialog(
            onDismissRequest = { chooseOverwrite = false },
            title = { Text(stringResource(R.string.profile_overwrite)) },
            text = {
                Column {
                    s.profiles.forEach { profile ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = { s.viewModel.overwriteProfile(profile); chooseOverwrite = false },
                                modifier = Modifier.weight(1f),
                            ) { Text(profile.name, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) }
                            IconButton(onClick = { chooseOverwrite = false; editProfile = profile }) {
                                Icon(Icons.Default.Edit, stringResource(R.string.action_edit))
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { chooseOverwrite = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    editProfile?.let { profile ->
        var name by remember(profile.id) { mutableStateOf(profile.name) }
        AlertDialog(
            onDismissRequest = { editProfile = null },
            title = { Text(stringResource(R.string.edit_profile_title)) },
            text = { TextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = {
                Button(enabled = name.isNotBlank(), onClick = {
                    s.viewModel.updateProfileName(profile, name)
                    editProfile = null
                }) { Text(stringResource(R.string.action_update)) }
            },
            dismissButton = { TextButton(onClick = { editProfile = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    deleteProfile?.let { profile ->
        AlertDialog(
            onDismissRequest = { deleteProfile = null },
            title = { Text(stringResource(R.string.delete_profile_title)) },
            text = { Text(stringResource(R.string.delete_profile_message, profile.name)) },
            confirmButton = { Button(onClick = { s.viewModel.deleteProfile(profile.id); deleteProfile = null }) { Text(stringResource(R.string.action_delete)) } },
            dismissButton = { TextButton(onClick = { deleteProfile = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun ProfileDockButton(
    profile: CameraProfile,
    selected: Boolean,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    var dragX by remember(profile.id) { mutableStateOf(0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val slot = with(density) { 62.dp.toPx() }
    val movementThreshold = with(density) { 8.dp.toPx() }
    Box(
        modifier =
            Modifier.size(54.dp)
                .graphicsLayer { translationX = dragX; alpha = if (dragX == 0f) 1f else 0.8f }
                .pointerInput(profile.id, index, count) {
                    kotlinx.coroutines.coroutineScope {
                        var deleteJob: kotlinx.coroutines.Job? = null
                        var moved = false
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                moved = false
                                deleteJob = launch {
                                    kotlinx.coroutines.delay(900)
                                    if (!moved) onDelete()
                                }
                            },
                            onDrag = { change, amount ->
                                change.consume()
                                dragX += amount.x
                                if (kotlin.math.abs(dragX) > movementThreshold) {
                                    moved = true
                                    deleteJob?.cancel()
                                }
                            },
                            onDragCancel = { deleteJob?.cancel(); dragX = 0f },
                            onDragEnd = {
                                deleteJob?.cancel()
                                if (moved && count > 1) {
                                    onMove((index + (dragX / slot).roundToInt()).coerceIn(0, count - 1))
                                }
                                dragX = 0f
                            },
                        )
                    }
                }
                .clip(RoundedCornerShape(10.dp))
                .background(Color(profile.colorArgb), RoundedCornerShape(10.dp))
                .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else ProBoundary, RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
                .semantics { contentDescription = profile.name },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            profile.name.trim().take(1).uppercase(Locale.getDefault()).ifEmpty { "P" },
            color = if (Color(profile.colorArgb).luminance() > 0.45f) Color.Black else Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

private fun CameraProfile.matches(settings: CameraSettings): Boolean =
    iso == settings.iso && shutterSpeedNs == settings.shutterSpeedNs && focusDistance == settings.focusDistance &&
        whiteBalanceMode == settings.whiteBalanceMode && exposureCompensation == settings.exposureCompensation

/** Pure preset generation shared by UI and regression tests. */
internal object ProControlPresets {
    private val commonIso = listOf(50, 64, 80, 100, 125, 160, 200, 250, 320, 400, 500, 640, 800, 1000, 1250, 1600, 2000, 2500, 3200, 4000, 5000, 6400, 8000, 10000, 12800)
    private val shutterDenominators = listOf(8000, 6400, 5000, 4000, 3200, 2500, 2000, 1600, 1250, 1000, 800, 640, 500, 400, 320, 250, 200, 160, 125, 100, 80, 60, 50, 40, 30, 25, 20, 15, 13, 10, 8, 6, 5, 4, 3, 2, 1)

    fun isoValues(range: IntRange): List<Int> =
        (listOf(range.first) + commonIso.filter { it in range } + range.last).distinct().sorted()

    fun shutterValues(range: LongRange): List<Long> {
        val fractional = shutterDenominators.map { 1_000_000_000L / it }
        val long = listOf(2_000_000_000L, 4_000_000_000L, 8_000_000_000L, 15_000_000_000L, 30_000_000_000L)
        return (listOf(range.first) + (fractional + long).filter { it in range } + range.last).distinct().sorted()
    }

    fun shutterText(ns: Long): String =
        if (ns < 1_000_000_000L) "1/${(1_000_000_000.0 / ns.coerceAtLeast(1L)).roundToInt()}"
        else if (ns % 1_000_000_000L == 0L) "${ns / 1_000_000_000L}s"
        else "%.1fs".format(Locale.US, ns / 1_000_000_000.0)
}
