package com.oriyu90.fcampro.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/**
 * Converts an [ImageProxy] to a [Bitmap], honoring rotation.
 * Used for panorama frame capture where full stills go through ImageCapture
 * but stitching needs in-memory bitmaps.
 *
 * Both YUV_420_888 (video-frame style analysis images) and JPEG (still
 * captures from a JPEG-bound ImageCapture) are accepted; anything else
 * returns null so the caller can skip the frame instead of crashing.
 */
object YuvConverter {
    fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        if (image.format == ImageFormat.JPEG) return jpegToBitmap(image)
        if (image.format != ImageFormat.YUV_420_888) return null
        val w = image.width
        val h = image.height
        if (w <= 0 || h <= 0 || image.planes.size < 3) return null
        return runCatching {
            val nv21 = yuv420ToNv21(image, w, h) ?: return@runCatching null
            val out = ByteArrayOutputStream(w * h)
            YuvImage(nv21, ImageFormat.NV21, w, h, null)
                .compressToJpeg(Rect(0, 0, w, h), 95, out)
            val bytes = out.toByteArray()
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return@runCatching null
            applyRotation(bmp, image.imageInfo.rotationDegrees)
        }.getOrNull()
    }

    private fun jpegToBitmap(image: ImageProxy): Bitmap? {
        if (image.planes.isEmpty()) return null
        return runCatching {
            val buf = image.planes[0].buffer
            val bytes = ByteArray(buf.remaining())
            buf.get(bytes)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return@runCatching null
            applyRotation(bmp, image.imageInfo.rotationDegrees)
        }.getOrNull()
    }

    private fun applyRotation(bmp: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bmp
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        if (rotated !== bmp) bmp.recycle()
        return rotated
    }

    private fun yuv420ToNv21(image: ImageProxy, w: Int, h: Int): ByteArray? {
        return runCatching {
            val yPlane = image.planes[0]
            val uPlane = image.planes[1]
            val vPlane = image.planes[2]
            val nv21 = ByteArray(w * h * 3 / 2)
            // Y channel, row by row (rowStride may exceed width).
            val yRowStride = yPlane.rowStride
            val yBuf = yPlane.buffer
            var dst = 0
            val yRow = ByteArray(yRowStride)
            for (row in 0 until h) {
                yBuf.get(yRow, 0, yRowStride)
                yRow.copyInto(nv21, dst, 0, w)
                dst += w
            }
            // Interleaved VU from the U/V planes (U and V share geometry).
            val uvRowStride = uPlane.rowStride
            val uvPixelStride = uPlane.pixelStride
            val vBuf = vPlane.buffer
            val uBuf = uPlane.buffer
            val uvH = h / 2
            val vRow = ByteArray(uvRowStride)
            val uRow = ByteArray(uvRowStride)
            for (row in 0 until uvH) {
                vBuf.get(vRow, 0, uvRowStride)
                uBuf.get(uRow, 0, uvRowStride)
                var col = 0
                while (col < w / 2) {
                    nv21[dst++] = vRow[col * uvPixelStride]
                    nv21[dst++] = uRow[col * uvPixelStride]
                    col++
                }
            }
            nv21
        }.getOrNull()
    }
}
