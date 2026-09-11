package com.oriyu90.fcampro.camera

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Pure slow-motion math (unit-testable). */
object SlowMoFactors {
    const val BASE_FPS = 30

    /**
     * Playback time-stretch factor for footage recorded at [recordFps].
     * 120fps -> 4x slow (plays back at 30fps), 240fps -> 8x, 60fps -> 2x.
     */
    fun playbackFactor(recordFps: Int, baseFps: Int = BASE_FPS): Float =
        (recordFps.coerceAtLeast(1).toFloat() / baseFps.coerceAtLeast(1)).coerceAtLeast(1f)
}

/**
 * Turns a high-frame-rate recording into a slow-motion file by stretching the
 * video track timestamps (no re-encode, so it is fast and lossless).
 *
 * Audio is dropped: slowing audio requires re-encoding, and silent slow-mo
 * matches the platform convention.
 */
object SlowMoProcessor {
    private const val TAG = "SlowMoProcessor"

    /**
     * @return the new MediaStore URI, or null on failure (the input is kept
     *   on failure and deleted on success).
     */
    // Sample flags straight from MediaExtractor are valid BufferInfo flags by
    // API contract; lint cannot infer that, hence the targeted suppression.
    @android.annotation.SuppressLint("WrongConstant")
    suspend fun convertToSlowMotion(
        context: Context,
        inputUri: Uri,
        recordFps: Int,
        displayName: String,
    ): Uri? =
        withContext(Dispatchers.IO) {
            val factor = SlowMoFactors.playbackFactor(recordFps)
            val resolver = context.contentResolver
            // MediaMuxer(FileDescriptor, …) needs API 26; mux to a temp file
            // first (path ctor is API 18) so minSdk 24 keeps working.
            val tmp =
                runCatching { File.createTempFile("slowmo", ".mp4", context.cacheDir) }
                    .getOrNull() ?: return@withContext null
            var muxer: MediaMuxer? = null
            val extractor = MediaExtractor()
            var outputUri: Uri? = null
            try {
                extractor.setDataSource(context, inputUri, null)
                var videoTrack = -1
                var rotation = 0
                for (i in 0 until extractor.trackCount) {
                    val format = extractor.getTrackFormat(i)
                    val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                    if (mime.startsWith("video/") && videoTrack < 0) {
                        videoTrack = i
                        if (format.containsKey(MediaFormat.KEY_ROTATION)) {
                            rotation = format.getInteger(MediaFormat.KEY_ROTATION)
                        }
                    }
                }
                if (videoTrack < 0) return@withContext null

                muxer = MediaMuxer(tmp.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val out = muxer ?: return@withContext null
                out.setOrientationHint(rotation)
                extractor.selectTrack(videoTrack)
                out.addTrack(extractor.getTrackFormat(videoTrack))
                out.start()
                val buffer = ByteBuffer.allocate(2 * 1024 * 1024)
                val info = MediaCodec.BufferInfo()
                while (true) {
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) break
                    info.set(
                        0,
                        size,
                        (extractor.sampleTime * factor).toLong(),
                        extractor.sampleFlags,
                    )
                    out.writeSampleData(0, buffer, info)
                    extractor.advance()
                }
                out.stop()

                val values =
                    ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Fcam pro")
                        }
                    }
                outputUri =
                    resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                        ?: return@withContext null
                resolver.openOutputStream(outputUri!!)?.use { o ->
                    tmp.inputStream().use { it.copyTo(o) }
                } ?: return@withContext null
                runCatching { resolver.delete(inputUri, null, null) }
                outputUri
            } catch (e: Exception) {
                Log.e(TAG, "slow-mo convert failed", e)
                outputUri?.let { runCatching { resolver.delete(it, null, null) } }
                null
            } finally {
                runCatching { muxer?.release() }
                runCatching { extractor.release() }
                runCatching { tmp.delete() }
            }
        }
}
