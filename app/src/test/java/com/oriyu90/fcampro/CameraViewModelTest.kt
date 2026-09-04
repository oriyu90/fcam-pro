package com.oriyu90.fcampro

import android.app.Application
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageCapture
import androidx.test.core.app.ApplicationProvider
import com.oriyu90.fcampro.ui.CameraLensInfo
import com.oriyu90.fcampro.ui.CameraLensType
import com.oriyu90.fcampro.ui.CameraViewModel
import com.oriyu90.fcampro.ui.LensCapabilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CameraViewModelTest {

    private fun vm() =
        CameraViewModel(ApplicationProvider.getApplicationContext<Application>())

    private fun lensWith(caps: LensCapabilities) =
        CameraLensInfo(
            id = "test-0",
            type = CameraLensType.WIDE,
            focalLength = 4.5f,
            isFront = false,
            capabilities = caps,
        )

    @Test
    fun flashModeCyclesAutoOnOffAuto() {
        val vm = vm()
        assertEquals(ImageCapture.FLASH_MODE_AUTO, vm.settings.value.flashMode)
        vm.cycleFlashMode()
        assertEquals(ImageCapture.FLASH_MODE_ON, vm.settings.value.flashMode)
        vm.cycleFlashMode()
        assertEquals(ImageCapture.FLASH_MODE_OFF, vm.settings.value.flashMode)
        vm.cycleFlashMode()
        assertEquals(ImageCapture.FLASH_MODE_AUTO, vm.settings.value.flashMode)
    }

    @Test
    fun timerCyclesThroughOptions() {
        val vm = vm()
        vm.cycleTimer()
        assertEquals(3, vm.settings.value.timerSeconds)
        vm.cycleTimer()
        assertEquals(10, vm.settings.value.timerSeconds)
        vm.cycleTimer()
        assertEquals(0, vm.settings.value.timerSeconds)
    }

    @Test
    fun aspectRatioToggles() {
        val vm = vm()
        val first = vm.settings.value.aspectRatio
        vm.cycleAspectRatio()
        assertTrue(vm.settings.value.aspectRatio != first)
        vm.cycleAspectRatio()
        assertEquals(first, vm.settings.value.aspectRatio)
    }

    @Test
    fun manualSettingsAreClampedToLensRanges() {
        val vm = vm()
        vm.setLens(
            lensWith(
                LensCapabilities(
                    supportsManualSensor = true,
                    isoRange = 100..800,
                    exposureRangeNs = 1_000_000L..100_000_000L,
                    minFocusDistance = 5f,
                    awbModes = listOf(1, 2, 3),
                    hasFlash = true,
                    maxZoomRatio = 4f,
                )
            )
        )
        vm.updateManualSettings(iso = 5000, shutterNs = 999_999_999L, focus = 42f, wb = 2)
        val s = vm.settings.value
        assertEquals(800, s.iso)
        assertEquals(100_000_000L, s.shutterSpeedNs)
        assertEquals(5f, s.focusDistance)
    }

    @Test
    fun manualSettingsCanBeResetToAuto() {
        val vm = vm()
        vm.setLens(
            lensWith(
                LensCapabilities(
                    supportsManualSensor = true,
                    isoRange = 100..800,
                    exposureRangeNs = 1_000_000L..100_000_000L,
                    minFocusDistance = 5f,
                    awbModes = listOf(1, 2),
                    hasFlash = false,
                    maxZoomRatio = 2f,
                )
            )
        )
        vm.updateManualSettings(iso = 400, shutterNs = 50_000_000L, focus = 2f, wb = 2)
        vm.updateManualSettings(iso = null, shutterNs = 50_000_000L, focus = 2f, wb = 2)
        assertEquals(null, vm.settings.value.iso)
        assertEquals(50_000_000L, vm.settings.value.shutterSpeedNs)
        vm.updateManualSettings(iso = null, shutterNs = null, focus = null, wb = null)
        assertEquals(null, vm.settings.value.shutterSpeedNs)
        assertEquals(null, vm.settings.value.focusDistance)
        assertEquals(null, vm.settings.value.whiteBalanceMode)
    }

    @Test
    fun shutterVolumeIsClampedToUnitRange() {
        val vm = vm()
        vm.setShutterVolume(9f)
        assertEquals(1f, vm.settings.value.shutterVolume)
        vm.setShutterVolume(-3f)
        assertEquals(0f, vm.settings.value.shutterVolume)
    }

    @Test
    fun lastMediaStartsNullAndUpdates() {
        val vm = vm()
        assertEquals(null, vm.lastMedia.value)
        val uri = android.net.Uri.parse("content://media/external/images/media/42")
        vm.setLastMedia(uri, isVideo = false)
        assertEquals(uri, vm.lastMedia.value?.uri)
        assertEquals(false, vm.lastMedia.value?.isVideo)
        vm.setLastMedia(uri, isVideo = true)
        assertEquals(true, vm.lastMedia.value?.isVideo)
    }
}
