package com.oriyu90.fcampro.camera

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.roundToInt

/** Pure panorama capture constants and yaw math (unit-testable). */
object PanoEstimate {
    const val MAX_FRAMES = 8
    const val CAPTURE_STEP_DEG = 12f
    const val MIN_CAPTURE_INTERVAL_MS = 1200L
    const val OVERLAP_FRAC = 0.30f
    const val MAX_FRAME_WIDTH = 1280
    /** Timer-based capture cadence when the device has no rotation sensor. */
    const val FALLBACK_CAPTURE_INTERVAL_MS = 2500L
    /** Tighter budgets for low-RAM devices (memoryClass <= 128MB). */
    const val LOW_RAM_FRAME_WIDTH = 960
    const val LOW_RAM_MAX_FRAMES = 6

    fun maxFrameWidth(memoryClassMb: Int): Int =
        if (memoryClassMb <= 128) LOW_RAM_FRAME_WIDTH else MAX_FRAME_WIDTH

    fun maxFrames(memoryClassMb: Int): Int =
        if (memoryClassMb <= 128) LOW_RAM_MAX_FRAMES else MAX_FRAMES

    /**
     * Wrap-aware yaw delta in degrees, range [-180, 180].
     * Positive when rotating from [fromDeg] towards [toDeg].
     */
    fun yawDelta(fromDeg: Float, toDeg: Float): Float {
        var d = (toDeg - fromDeg) % 360f
        if (d > 180f) d -= 360f
        if (d < -180f) d += 360f
        return d
    }
}

/**
 * Sweep-panorama stitcher for frames captured left-to-right with roughly
 * constant overlap ([PanoEstimate.OVERLAP_FRAC]).
 *
 * Alignment is translation-only (vertical offset estimated per seam via a
 * sum-of-absolute-differences search on a downscaled strip); seams are
 * feather-blended. This handles handheld panning; it is not a replacement
 * for feature-based stitching on scenes with strong parallax.
 *
 * Inputs are not recycled; the caller owns all bitmaps.
 */
object PanoramaStitcher {
    private const val SEARCH_WIDTH = 160
    private const val SEARCH_MAX_SHIFT = 12
    private const val BLEND_FRAC_OF_OVERLAP = 0.5f

    /** Downscale so width <= [maxWidth], preserving aspect. Returns [src] if small enough. */
    fun downscaleToMaxWidth(src: Bitmap, maxWidth: Int = PanoEstimate.MAX_FRAME_WIDTH): Bitmap {
        if (src.width <= maxWidth) return src
        val scale = maxWidth.toFloat() / src.width
        val h = (src.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, maxWidth, h, true)
    }

    /**
     * Estimated vertical offset of [next] relative to [prev], in full-resolution
     * pixels. Positive means [next] sits lower. Searches +/-[maxShiftPx].
     */
    fun estimateVerticalOffset(
        prev: Bitmap,
        next: Bitmap,
        overlapFrac: Float = PanoEstimate.OVERLAP_FRAC,
        maxShiftPx: Int = 60,
    ): Int {
        val scale = SEARCH_WIDTH.toFloat() / prev.width.coerceAtLeast(1)
        val sw = SEARCH_WIDTH
        val sh = (prev.height * scale).roundToInt().coerceAtLeast(1)
        val pSmall = Bitmap.createScaledBitmap(prev, sw, sh, true)
        val nSmall = Bitmap.createScaledBitmap(next, (next.width * scale).roundToInt().coerceAtLeast(1), (next.height * scale).roundToInt().coerceAtLeast(1), true)
        return try {
            val pGray = grayscale(pSmall)
            val nGray = grayscale(nSmall)
            val stripW = (sw * overlapFrac).roundToInt().coerceIn(4, sw / 2)
            val maxShift = (maxShiftPx * scale).roundToInt().coerceIn(1, SEARCH_MAX_SHIFT)
            var bestDy = 0
            var bestScore = Long.MAX_VALUE
            for (dy in -maxShift..maxShift) {
                var score = 0L
                var count = 0
                for (y in 0 until sh) {
                    val ny = y + dy
                    if (ny < 0 || ny >= nSmall.height) continue
                    for (x in 0 until stripW) {
                        val pv = pGray[y * sw + (sw - stripW + x)]
                        val nv = nGray[ny * nSmall.width + x]
                        score += abs(pv - nv)
                        count++
                    }
                }
                if (count > 0 && score < bestScore) {
                    bestScore = score
                    bestDy = dy
                }
            }
            (bestDy / scale).roundToInt()
        } finally {
            if (pSmall !== prev) pSmall.recycle()
            if (nSmall !== next) nSmall.recycle()
        }
    }

    /**
     * Stitch [frames] left-to-right. All frames are scaled to the minimum
     * height first. Returns null for an empty list.
     */
    fun stitch(
        frames: List<Bitmap>,
        overlapFrac: Float = PanoEstimate.OVERLAP_FRAC,
    ): Bitmap? {
        if (frames.isEmpty()) return null
        if (frames.size == 1) return frames.first()
        val targetH = frames.minOf { it.height }.coerceAtLeast(1)
        val scaled =
            frames.map { f ->
                if (f.height == targetH) f
                else Bitmap.createScaledBitmap(f, (f.width * targetH / f.height.toFloat()).roundToInt().coerceAtLeast(1), targetH, true)
            }
        try {
            val overlapPx = scaled.map { (it.width * overlapFrac).roundToInt().coerceIn(1, it.width / 2) }
            val offsets = IntArray(scaled.size)
            for (i in 1 until scaled.size) {
                // Reuse the full-res estimator on the height-normalized pair.
                offsets[i] = offsets[i - 1] + estimateVerticalOffset(scaled[i - 1], scaled[i], overlapFrac)
            }
            val minY = offsets.min()
            val maxBottom = offsets.indices.maxOf { offsets[it] + targetH }
            val outH = (maxBottom - minY).coerceAtLeast(1)
            var outW = scaled[0].width
            for (i in 1 until scaled.size) outW += scaled[i].width - overlapPx[i]
            val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
            var dx = 0
            val empty = IntArray(outW * outH)
            out.setPixels(empty, 0, outW, 0, 0, outW, outH)
            for (i in scaled.indices) {
                val bmp = scaled[i]
                val w = bmp.width
                val oy = offsets[i] - minY
                val rowBuf = IntArray(w * targetH)
                bmp.getPixels(rowBuf, 0, w, 0, 0, w, targetH)
                val ov = if (i == 0) 0 else overlapPx[i]
                val baseX = dx - ov
                for (y in 0 until targetH) {
                    for (x in 0 until w) {
                        val srcPixel = rowBuf[y * w + x]
                        val ox = baseX + x
                        if (ox < 0 || ox >= outW) continue
                        if (i > 0 && x < ov) {
                            val t = (x + 1).toFloat() / (ov + 1)
                            val dstPixel = out.getPixel(ox, oy + y)
                            // Feather only the inner part of the overlap; the outer
                            // edge is fully replaced to avoid ghosting.
                            val k = ((t - (1f - BLEND_FRAC_OF_OVERLAP)) / BLEND_FRAC_OF_OVERLAP).coerceIn(0f, 1f)
                            out.setPixel(ox, oy + y, blend(dstPixel, srcPixel, k))
                        } else {
                            out.setPixel(ox, oy + y, srcPixel)
                        }
                    }
                }
                dx = baseX + w
            }
            return out
        } finally {
            scaled.forEachIndexed { index, bmp -> if (bmp !== frames[index]) bmp.recycle() }
        }
    }

    private fun grayscale(bmp: Bitmap): IntArray {
        val w = bmp.width
        val h = bmp.height
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        for (i in px.indices) {
            val p = px[i]
            px[i] = (((p shr 16) and 0xFF) + ((p shr 8) and 0xFF) + (p and 0xFF)) / 3
        }
        return px
    }

    private fun blend(a: Int, b: Int, t: Float): Int {
        val ia = ((a shr 24) and 0xFF)
        // Output stays opaque; source frames are opaque camera stills.
        fun ch(shift: Int): Int {
            val ca = (a shr shift) and 0xFF
            val cb = (b shr shift) and 0xFF
            return (ca + ((cb - ca) * t)).roundToInt().coerceIn(0, 255)
        }
        return (ia shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }
}
