package com.oriyu90.fcampro.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.oriyu90.fcampro.BuildConfig
import com.oriyu90.fcampro.R
import com.oriyu90.fcampro.core.AppSettings
import com.oriyu90.fcampro.core.LocaleController

private const val URL_DEVELOPER = "https://studio-rizi.pages.dev/"
private const val URL_PROJECT = "https://studio-rizi.pages.dev/projects/fcam-pro/"
private const val URL_SOURCE = "https://github.com/oriyu90/fcam-pro"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { AppSettings.get(context) }
    val snapshot by settings.state.collectAsState()

    fun openUrl(url: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
        ) {
            // --- Language ---
            SectionHeader(stringResource(R.string.settings_section_language))
            Column(Modifier.selectableGroup()) {
                LanguageRow(
                    label = stringResource(R.string.lang_system),
                    selected = snapshot.languageTag == LocaleController.SYSTEM,
                ) {
                    settings.languageTag = LocaleController.SYSTEM
                    LocaleController.apply(LocaleController.SYSTEM)
                }
                LanguageRow(
                    label = stringResource(R.string.lang_ja),
                    selected = snapshot.languageTag == LocaleController.JAPANESE,
                ) {
                    settings.languageTag = LocaleController.JAPANESE
                    LocaleController.apply(LocaleController.JAPANESE)
                }
                LanguageRow(
                    label = stringResource(R.string.lang_en),
                    selected = snapshot.languageTag == LocaleController.ENGLISH,
                ) {
                    settings.languageTag = LocaleController.ENGLISH
                    LocaleController.apply(LocaleController.ENGLISH)
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // --- Capture defaults ---
            SectionHeader(stringResource(R.string.settings_section_capture))
            ChoiceRow(
                title = stringResource(R.string.settings_default_aspect),
                options =
                    listOf(
                        false to stringResource(R.string.aspect_4_3),
                        true to stringResource(R.string.aspect_16_9),
                    ),
                selected = snapshot.defaultAspect16by9,
                onSelect = { settings.defaultAspect16by9 = it },
            )
            ChoiceRow(
                title = stringResource(R.string.settings_default_timer),
                options =
                    AppSettings.TIMER_OPTIONS.map { s ->
                        s to
                            if (s == 0) stringResource(R.string.timer_off)
                            else stringResource(R.string.timer_seconds, s)
                    },
                selected = snapshot.defaultTimerSeconds,
                onSelect = { settings.defaultTimerSeconds = it },
            )
            ChoiceRow(
                title = stringResource(R.string.settings_timelapse_interval),
                options =
                    AppSettings.TIMELAPSE_INTERVALS.map { s ->
                        s to stringResource(R.string.timer_seconds, s)
                    },
                selected = snapshot.timelapseIntervalSeconds,
                onSelect = { settings.timelapseIntervalSeconds = it },
            )
            ToggleRow(
                title = stringResource(R.string.settings_shutter_sound),
                checked = snapshot.shutterSound,
                onCheckedChange = { settings.shutterSound = it },
            )
            ToggleRow(
                title = stringResource(R.string.settings_bg_audio),
                checked = snapshot.backgroundAudio,
                onCheckedChange = { settings.backgroundAudio = it },
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            // --- About ---
            SectionHeader(stringResource(R.string.settings_section_about))
            InfoRow(
                title = stringResource(R.string.settings_version),
                value = BuildConfig.VERSION_NAME,
            )
            LinkRow(stringResource(R.string.settings_developer_site)) { openUrl(URL_DEVELOPER) }
            LinkRow(stringResource(R.string.settings_project_home)) { openUrl(URL_PROJECT) }
            LinkRow(stringResource(R.string.settings_source_code)) { openUrl(URL_SOURCE) }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun LanguageRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(role = Role.RadioButton, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun <T> ChoiceRow(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { (value, label) ->
                FilterChip(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    label = { Text(label) },
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LinkRow(title: String, onClick: () -> Unit) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Icon(
            Icons.AutoMirrored.Filled.OpenInNew,
            contentDescription = stringResource(R.string.settings_open_external_hint),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
