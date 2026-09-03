package com.oriyu90.fcampro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FcamColorScheme =
    darkColorScheme(
        primary = FcamAccent,
        onPrimary = FcamOnAccent,
        secondary = FcamAccent,
        onSecondary = FcamOnAccent,
        background = FcamSurface,
        onBackground = FcamOnSurface,
        surface = FcamSurface,
        onSurface = FcamOnSurface,
        surfaceVariant = FcamSurfaceVariant,
        onSurfaceVariant = FcamOnSurfaceVariant,
        outline = FcamOutline,
        error = FcamError,
    )

/**
 * App theme. Intentionally single (dark) scheme; dynamic color is not used so the
 * viewfinder chrome is predictable on every device.
 */
@Composable
fun FcamProTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FcamColorScheme,
        typography = Typography,
        content = content,
    )
}
