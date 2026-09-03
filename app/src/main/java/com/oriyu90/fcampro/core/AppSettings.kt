package com.oriyu90.fcampro.core

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Small, synchronous settings store backed by [SharedPreferences]. The data set is a
 * handful of primitives, so a full DataStore dependency is not warranted. All values
 * have safe defaults and reads never throw.
 */
class AppSettings private constructor(private val prefs: SharedPreferences) {

    data class Snapshot(
        val languageTag: String, // "" = follow system
        val defaultAspect16by9: Boolean,
        val defaultTimerSeconds: Int,
        val timelapseIntervalSeconds: Int,
        val shutterSound: Boolean,
        val backgroundAudio: Boolean,
    )

    private val _state = MutableStateFlow(readSnapshot())
    val state: StateFlow<Snapshot> = _state.asStateFlow()

    private fun readSnapshot() =
        Snapshot(
            languageTag = prefs.getString(KEY_LANG, "") ?: "",
            defaultAspect16by9 = prefs.getBoolean(KEY_ASPECT_169, false),
            defaultTimerSeconds = prefs.getInt(KEY_TIMER, 0).coerceIn(0, 10),
            timelapseIntervalSeconds =
                prefs.getInt(KEY_TL_INTERVAL, 3).let { if (it in TIMELAPSE_INTERVALS) it else 3 },
            shutterSound = prefs.getBoolean(KEY_SHUTTER_SOUND, true),
            backgroundAudio = prefs.getBoolean(KEY_BG_AUDIO, true),
        )

    private fun mutate(block: SharedPreferences.Editor.() -> Unit) {
        prefs.edit(commit = false, action = block)
        _state.value = readSnapshot()
    }

    /** BCP-47 language tag, or "" to follow the system locale. */
    var languageTag: String
        get() = _state.value.languageTag
        set(value) = mutate { putString(KEY_LANG, value) }

    var defaultAspect16by9: Boolean
        get() = _state.value.defaultAspect16by9
        set(value) = mutate { putBoolean(KEY_ASPECT_169, value) }

    var defaultTimerSeconds: Int
        get() = _state.value.defaultTimerSeconds
        set(value) = mutate { putInt(KEY_TIMER, value.coerceIn(0, 10)) }

    var timelapseIntervalSeconds: Int
        get() = _state.value.timelapseIntervalSeconds
        set(value) = mutate { putInt(KEY_TL_INTERVAL, if (value in TIMELAPSE_INTERVALS) value else 3) }

    var shutterSound: Boolean
        get() = _state.value.shutterSound
        set(value) = mutate { putBoolean(KEY_SHUTTER_SOUND, value) }

    var backgroundAudio: Boolean
        get() = _state.value.backgroundAudio
        set(value) = mutate { putBoolean(KEY_BG_AUDIO, value) }

    companion object {
        val TIMELAPSE_INTERVALS = listOf(1, 3, 5, 10)
        val TIMER_OPTIONS = listOf(0, 3, 10)

        private const val PREFS = "fcam_settings"
        private const val KEY_LANG = "language_tag"
        private const val KEY_ASPECT_169 = "default_aspect_16_9"
        private const val KEY_TIMER = "default_timer_seconds"
        private const val KEY_TL_INTERVAL = "timelapse_interval_seconds"
        private const val KEY_SHUTTER_SOUND = "shutter_sound"
        private const val KEY_BG_AUDIO = "background_audio"

        @Volatile private var instance: AppSettings? = null

        fun get(context: Context): AppSettings =
            instance
                ?: synchronized(this) {
                    instance
                        ?: AppSettings(
                                context.applicationContext
                                    .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                            )
                            .also { instance = it }
                }
    }
}
