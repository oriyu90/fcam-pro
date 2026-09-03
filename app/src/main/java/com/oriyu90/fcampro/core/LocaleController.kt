package com.oriyu90.fcampro.core

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Thin wrapper over AppCompat per-app locales.
 *
 * Supported UI languages are English (default) and Japanese. When the selected tag is
 * empty the app follows the system locale; AppCompat resolves any unsupported system
 * locale to the default resources (English) automatically.
 */
object LocaleController {

    const val SYSTEM = ""
    const val JAPANESE = "ja"
    const val ENGLISH = "en"

    val supportedTags = listOf(SYSTEM, JAPANESE, ENGLISH)

    /** Apply [tag] ("" = system). Safe to call from Application.onCreate and at runtime. */
    fun apply(tag: String) {
        val locales =
            if (tag.isBlank()) LocaleListCompat.getEmptyLocaleList()
            else LocaleListCompat.forLanguageTags(tag)
        AppCompatDelegate.setApplicationLocales(locales)
    }

    /** The tag currently in effect from AppCompat, or "" when following the system. */
    fun currentTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) SYSTEM else locales[0]?.language ?: SYSTEM
    }
}
