package com.oriyu90.fcampro.camera

import android.util.Size
import androidx.camera.core.ImageCapture

/**
 * Per-lens sensor-RAW capability.
 *
 * Sensor packing (RAW10 / RAW12 / API-37 RAW14, see
 * [android.graphics.ImageFormat]) is consumed inside CameraX/DngCreator;
 * the app only needs to know whether DNG output is possible and, for the
 * UI badge, the approximate bit depth.
 */
data class RawCapability(
    val supported: Boolean,
    /** Sensor bit-depth heuristic (10 / 12 / 14) or null when unknown. */
    val bitDepth: Int?,
    /** Largest RAW_SENSOR output size, or null when unsupported. */
    val maxSize: Size?,
)

object RawSupport {
    const val DNG_MIME = "image/x-adobe-dng"
    const val DNG_EXTENSION = ".dng"

    /**
     * Sensor bit depth from SENSOR_INFO_WHITE_LEVEL: a 10-bit sensor tops
     * out at 1023, 12-bit at 4095, 14-bit at 16383. Anything else (or null)
     * means the depth cannot be told from public characteristics.
     */
    fun bitDepthFromWhiteLevel(whiteLevel: Int?): Int? =
        when (whiteLevel) {
            1023 -> 10
            4095 -> 12
            16383 -> 14
            else -> null
        }

    /**
     * CameraX output format actually used: RAW variants only when the lens
     * reports RAW support, otherwise plain JPEG. Keeps bind + capture from
     * ever requesting an unsupported format.
     */
    fun effectiveOutputFormat(requested: Int, rawSupported: Boolean): Int =
        if (rawSupported) requested else ImageCapture.OUTPUT_FORMAT_JPEG
}
