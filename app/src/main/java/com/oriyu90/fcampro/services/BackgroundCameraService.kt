package com.oriyu90.fcampro.services

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.oriyu90.fcampro.MainActivity
import com.oriyu90.fcampro.R
import com.oriyu90.fcampro.core.AppSettings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Headless foreground service that records video while the app is not in front.
 *
 * Scope for v1: recording continues while the process is alive (screen off, app in
 * background). It is stopped cleanly if the task is removed or the process dies —
 * fully detached indefinite recording is out of scope.
 */
class BackgroundCameraService : LifecycleService() {

    private var cameraProvider: ProcessCameraProvider? = null
    private var recording: Recording? = null
    private var stopping = false
    private var startedAtElapsed = 0L
    private var tickerJob: Job? = null
    private var lensFront = false
    private val main = Handler(Looper.getMainLooper())

    override fun onCreate() {
        super.onCreate()
        // A fresh service instance is not recording yet. This also clears a stale
        // "running" flag if a previous instance was killed without onDestroy.
        _running.value = false
        createChannel()
        startForegroundSafely(getString(R.string.notif_starting_text))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopEverything()
            return START_NOT_STICKY
        }

        lensFront = intent?.getBooleanExtra(EXTRA_LENS_FRONT, false) ?: false

        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "camera permission missing")
            stopEverything()
            return START_NOT_STICKY
        }

        // Publish running=true synchronously so the Activity's CameraScreen unbinds its
        // own use cases first, then bind the service camera one frame later to avoid a
        // same-process physical-camera conflict.
        if (recording == null && !stopping) {
            _running.value = true
            main.postDelayed({ if (!stopping) startRecording() }, 350L)
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener(
            {
                val provider =
                    runCatching { future.get() }.getOrNull()
                        ?: return@addListener fail("camera provider unavailable")
                cameraProvider = provider
                bindAndRecord(provider, firstAttempt = true)
            },
            ContextCompat.getMainExecutor(this),
        )
    }

    private fun bindAndRecord(provider: ProcessCameraProvider, firstAttempt: Boolean) {
        try {
            provider.unbindAll()
            val recorder =
                Recorder.Builder()
                    .setQualitySelector(
                        QualitySelector.fromOrderedList(
                            listOf(Quality.FHD, Quality.HD, Quality.SD),
                            FallbackStrategy.lowerQualityOrHigherThan(Quality.SD),
                        )
                    )
                    .build()
            val videoCapture = VideoCapture.withOutput(recorder)
            val selector =
                if (lensFront) CameraSelector.DEFAULT_FRONT_CAMERA
                else CameraSelector.DEFAULT_BACK_CAMERA
            provider.bindToLifecycle(this, selector, videoCapture)
            beginRecord(videoCapture)
        } catch (e: Exception) {
            if (firstAttempt) {
                // The Activity may not have released the camera yet; retry once.
                Log.w(TAG, "bind failed, retrying once", e)
                main.postDelayed({ if (!stopping) bindAndRecord(provider, firstAttempt = false) }, 400L)
            } else {
                Log.e(TAG, "bind failed", e)
                fail(e.message ?: "bind error")
            }
        }
    }

    private fun beginRecord(videoCapture: VideoCapture<Recorder>) {
        val name = "Fcam-bg-" + System.currentTimeMillis() + ".mp4"
        val values =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Fcam pro")
                }
            }
        val output =
            MediaStoreOutputOptions.Builder(
                    contentResolver,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                )
                .setContentValues(values)
                .build()

        val wantAudio =
            AppSettings.get(this).backgroundAudio &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED

        try {
            var pending = videoCapture.output.prepareRecording(this, output)
            if (wantAudio) pending = enableAudio(pending)
            recording =
                pending.start(ContextCompat.getMainExecutor(this)) { event ->
                    when (event) {
                        is VideoRecordEvent.Start -> {
                            startedAtElapsed = SystemClock.elapsedRealtime()
                            startTicker()
                            updateNotification(getString(R.string.notif_recording_text))
                        }
                        is VideoRecordEvent.Finalize -> {
                            if (event.hasError()) {
                                Log.e(TAG, "recording finalized with error ${event.error}")
                            }
                            if (!stopping) stopEverything()
                        }
                        else -> Unit
                    }
                }
            _running.value = true
        } catch (e: Exception) {
            Log.e(TAG, "prepareRecording failed", e)
            fail(e.message ?: "record error")
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun enableAudio(pending: PendingRecording): PendingRecording = pending.withAudioEnabled()

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob =
            lifecycleScope.launch {
                while (isActive && !stopping) {
                    updateNotification(
                        getString(
                            R.string.notif_recording_elapsed,
                            formatElapsed(SystemClock.elapsedRealtime() - startedAtElapsed),
                        )
                    )
                    delay(1000L)
                }
            }
    }

    private fun formatElapsed(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    private fun fail(reason: String) {
        Log.w(TAG, "background recording failed: $reason")
        stopEverything()
    }

    private fun stopEverything() {
        if (stopping) return
        stopping = true
        _running.value = false
        tickerJob?.cancel()
        runCatching { recording?.stop() }
        recording = null
        runCatching { cameraProvider?.unbindAll() }
        cameraProvider = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Flush the current file cleanly instead of letting the process die mid-write.
        stopEverything()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        _running.value = false
        main.removeCallbacksAndMessages(null)
        tickerJob?.cancel()
        runCatching { recording?.stop() }
        recording = null
        runCatching { cameraProvider?.unbindAll() }
        super.onDestroy()
    }

    // --- notification -------------------------------------------------------

    private fun startForegroundSafely(text: String) {
        val type =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            else 0
        try {
            ServiceCompat.startForeground(this, NOTIF_ID, buildNotification(text), type)
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
            _running.value = false
            stopSelf()
        }
    }

    private fun updateNotification(text: String) {
        if (stopping) return
        runCatching {
            getSystemService(NotificationManager::class.java)
                ?.notify(NOTIF_ID, buildNotification(text))
        }
    }

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_recording_title))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setLargeIcon(
                runCatching {
                        android.graphics.BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
                    }
                    .getOrNull()
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            )
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.notif_stop),
                PendingIntent.getService(
                    this,
                    1,
                    Intent(this, BackgroundCameraService::class.java).setAction(ACTION_STOP),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                ),
            )
            .build()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                        CHANNEL_ID,
                        getString(R.string.notif_channel_name),
                        NotificationManager.IMPORTANCE_LOW,
                    )
                    .apply { description = getString(R.string.notif_channel_desc) }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "BgCameraService"
        private const val CHANNEL_ID = "fcam_background_recording"
        private const val NOTIF_ID = 4211
        const val ACTION_STOP = "com.oriyu90.fcampro.action.STOP_BG_RECORDING"
        const val EXTRA_LENS_FRONT = "com.oriyu90.fcampro.extra.LENS_FRONT"

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running

        fun start(context: Context, lensFront: Boolean = false) {
            val intent =
                Intent(context, BackgroundCameraService::class.java)
                    .putExtra(EXTRA_LENS_FRONT, lensFront)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent =
                Intent(context, BackgroundCameraService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
