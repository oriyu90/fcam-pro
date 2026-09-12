package com.oriyu90.fcampro.ui

import android.hardware.camera2.CameraMetadata
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.graphics.Color
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

/** Pro settings item selected from the quick-control deck. */
enum class ProItem { ISO, SHUTTER, FOCUS, WB, EV, FORMAT, MIC, PROFILES }

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
        ProItem.PROFILES -> R.string.profiles_title
    }

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

private fun proItemsFor(s: SharedActions): List<ProItem> =
    listOf(
        ProItem.ISO,
        ProItem.SHUTTER,
        ProItem.FOCUS,
        ProItem.WB,
        ProItem.EV,
        ProItem.PROFILES,
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
        ProItem.PROFILES -> s.profiles.size.toString()
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
            ProItem.PROFILES -> ProProfilesBlock(s)
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

@Composable
private fun ProProfilesBlock(s: SharedActions) {
    var showSave by remember { mutableStateOf(false) }
    var editProfile by remember { mutableStateOf<CameraProfile?>(null) }
    var deleteProfile by remember { mutableStateOf<CameraProfile?>(null) }
    var saved by remember { mutableStateOf(false) }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(if (s.profiles.isEmpty()) stringResource(R.string.profiles_empty) else stringResource(R.string.profile_count, s.profiles.size), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
        Button(onClick = { showSave = true; saved = false }) { Text(stringResource(R.string.save_profile)) }
    }
    if (saved) Text(stringResource(R.string.profile_saved), color = ProAccent, style = MaterialTheme.typography.labelMedium)
    s.profiles.forEach { profile ->
        val matches = profile.matches(s.settings)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                .background(if (matches) ProSelectedSurface else ProSurface, RoundedCornerShape(8.dp))
                .border(1.dp, if (matches) ProAccent else ProBoundary, RoundedCornerShape(8.dp))
                .clickable { s.viewModel.loadProfile(profile) }
                .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (matches) Icon(Icons.Default.Check, stringResource(R.string.profile_applied), tint = ProAccent, modifier = Modifier.size(18.dp))
            Text(profile.name, modifier = Modifier.weight(1f).padding(start = if (matches) 8.dp else 0.dp), color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = { editProfile = profile }) { Icon(Icons.Default.Edit, stringResource(R.string.action_edit), modifier = Modifier.size(18.dp)) }
            IconButton(onClick = { deleteProfile = profile }) { Icon(Icons.Default.Close, stringResource(R.string.action_delete), modifier = Modifier.size(18.dp)) }
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
                    supportingText = if (name.isBlank()) ({ Text(stringResource(R.string.profile_name_required)) }) else null,
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(enabled = name.isNotBlank(), onClick = { s.viewModel.saveProfile(name); saved = true; showSave = false }) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = { TextButton(onClick = { showSave = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
    editProfile?.let { profile ->
        var name by remember(profile.id) { mutableStateOf(profile.name) }
        AlertDialog(
            onDismissRequest = { editProfile = null },
            title = { Text(stringResource(R.string.edit_profile_title)) },
            text = { TextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = {
                Button(enabled = name.isNotBlank(), onClick = { s.viewModel.updateProfileName(profile, name); editProfile = null }) {
                    Text(stringResource(R.string.action_update))
                }
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
