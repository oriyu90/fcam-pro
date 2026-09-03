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
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
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
import com.oriyu90.fcampro.MainActivity
import com.oriyu90.fcampro.R
import com.oriyu90.fcampro.core.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Headless foreground service that records video while the app is not in front.
 *
 * Scope for v1: recording continues while the process is alive (screen off, app in
 * background). It is intentionally stopped if the task is removed or the process dies —
 * fully detached indefinite recording is out of scope.
 */
class BackgroundCameraService : LifecycleService() {

    private var cameraProvider: ProcessCameraProvider? = null
    private var recording: Recording? = null
    private var stopping = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForegroundSafely(getString(R.string.notif_starting_text))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopEverything()
            return START_NOT_STICKY
        }
        if (recording == null && !stopping) {
            startRecording()
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
                try {
                    provider.unbindAll()
                    val recorder =
                        Recorder.Builder()
                            .setQualitySelector(
                                QualitySelector.fromOrderedList(
                                    listOf(Quality.FHD, Quality.HD, Quality.SD)
                                )
                            )
                            .build()
                    val videoCapture = VideoCapture.withOutput(recorder)
                    provider.bindToLifecycle(
                        this,
                        androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA,
                        videoCapture,
                    )
                    beginRecord(videoCapture)
                } catch (e: Exception) {
                    Log.e(TAG, "bind/record failed", e)
                    fail(e.message ?: "error")
                }
            },
            ContextCompat.getMainExecutor(this),
        )
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
            if (wantAudio) {
                pending = enableAudio(pending)
            }
            recording =
                pending.start(ContextCompat.getMainExecutor(this)) { event ->
                    if (event is VideoRecordEvent.Finalize) {
                        if (event.hasError()) {
                            Log.e(TAG, "recording finalized with error ${event.error}")
                        }
                        if (!stopping) stopEverything()
                    }
                }
            _running.value = true
            updateNotification(getString(R.string.notif_recording_text))
        } catch (e: Exception) {
            Log.e(TAG, "prepareRecording failed", e)
            fail(e.message ?: "error")
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun enableAudio(
        pending: androidx.camera.video.PendingRecording
    ): androidx.camera.video.PendingRecording = pending.withAudioEnabled()

    private fun fail(reason: String) {
        Log.w(TAG, "background recording failed: $reason")
        stopEverything()
    }

    private fun stopEverything() {
        if (stopping) return
        stopping = true
        _running.value = false
        runCatching { recording?.stop() }
        recording = null
        runCatching { cameraProvider?.unbindAll() }
        cameraProvider = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        _running.value = false
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
            stopSelf()
        }
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm?.notify(NOTIF_ID, buildNotification(text))
    }

    private fun buildNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_recording_title))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
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

        private val _running = MutableStateFlow(false)
        val running: StateFlow<Boolean> = _running

        fun start(context: Context) {
            val intent = Intent(context, BackgroundCameraService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent =
                Intent(context, BackgroundCameraService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
