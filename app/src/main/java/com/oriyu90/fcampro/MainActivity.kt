package com.oriyu90.fcampro

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.oriyu90.fcampro.ui.CameraScreen
import com.oriyu90.fcampro.ui.CameraViewModel
import com.oriyu90.fcampro.ui.ExternalCaptureSpec
import com.oriyu90.fcampro.ui.SettingsScreen
import com.oriyu90.fcampro.ui.theme.FcamProTheme

class MainActivity : AppCompatActivity() {

    private val viewModel: CameraViewModel by viewModels()
    private val externalSpec = mutableStateOf<ExternalCaptureSpec?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        externalSpec.value = parseExternalCapture(intent)

        setContent {
            FcamProTheme {
                val external by externalSpec
                CameraGate(
                    external = external,
                    onExternalResult = { ok, data ->
                        setResult(if (ok) Activity.RESULT_OK else Activity.RESULT_CANCELED, data)
                        finish()
                    },
                    viewModel = viewModel,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val spec = parseExternalCapture(intent)
        if (spec != null) externalSpec.value = spec
    }

    private fun parseExternalCapture(intent: Intent?): ExternalCaptureSpec? {
        val action = intent?.action ?: return null
        val isVideo = action == MediaStore.ACTION_VIDEO_CAPTURE
        val isImage =
            action == MediaStore.ACTION_IMAGE_CAPTURE ||
                action == "android.media.action.IMAGE_CAPTURE_SECURE"
        if (!isVideo && !isImage) return null
        @Suppress("DEPRECATION")
        val output: Uri? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT, Uri::class.java)
            else intent.getParcelableExtra(MediaStore.EXTRA_OUTPUT)
        return ExternalCaptureSpec(isVideo = isVideo, outputUri = output)
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun CameraGate(
    external: ExternalCaptureSpec?,
    onExternalResult: (Boolean, Intent?) -> Unit,
    viewModel: CameraViewModel,
) {
    val context = LocalContext.current
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) cameraPermission.launchPermissionRequest()
    }

    if (cameraPermission.status.isGranted) {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "camera") {
            composable("camera") {
                CameraScreen(
                    viewModel = viewModel,
                    external = external,
                    onExternalResult = onExternalResult,
                    onOpenSettings = { navController.navigate("settings") },
                )
            }
            composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize().safeContentPadding().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.perm_required_title),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.perm_camera_rationale),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                if (cameraPermission.status.shouldShowRationale) {
                    Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                        Text(stringResource(R.string.perm_grant))
                    }
                } else {
                    TextButton(
                        onClick = {
                            context.startActivity(
                                Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null),
                                    )
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    ) {
                        Text(stringResource(R.string.perm_open_settings))
                    }
                }
            }
        }
    }
}
