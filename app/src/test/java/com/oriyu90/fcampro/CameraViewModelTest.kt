package com.oriyu90.fcampro

import android.app.Application
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageCapture
import androidx.test.core.app.ApplicationProvider
import com.oriyu90.fcampro.camera.PanoEstimate
import com.oriyu90.fcampro.camera.PanoramaStitcher
import com.oriyu90.fcampro.camera.RawCapability
import com.oriyu90.fcampro.camera.RawSupport
import com.oriyu90.fcampro.camera.SlowMoFactors
import com.oriyu90.fcampro.ui.CameraLensInfo
import com.oriyu90.fcampro.ui.CameraLensType
import com.oriyu90.fcampro.ui.CameraViewModel
import com.oriyu90.fcampro.ui.ExposureComp
import com.oriyu90.fcampro.ui.LensCapabilities
import com.oriyu90.fcampro.ui.ManualExposure
import com.oriyu90.fcampro.ui.SaveFormat
import com.oriyu90.fcampro.ui.dedupeLenses
import com.oriyu90.fcampro.ui.formatStorageGb
import com.oriyu90.fcampro.ui.ZoomRatios
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
            logicalCameraId = "test-0",
            physicalCameraId = null,
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
                    exposureCompRange = -6..6,
                    exposureCompStep = 1f / 3f,
                    highSpeedVideo = null,
                    rawCapability = null,
                    apertures = emptyList(),
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
                    exposureCompRange = -6..6,
                    exposureCompStep = 1f / 3f,
                    highSpeedVideo = null,
                    rawCapability = null,
                    apertures = emptyList(),
                )
            )
        )
        vm.updateManualSettings(iso = 400, shutterNs = 50_000_000L, focus = 2f, wb = 2)
        // Exposure is atomic: clearing one side refills it with the default.
        vm.updateManualSettings(iso = null, shutterNs = 50_000_000L, focus = 2f, wb = 2)
        assertEquals(100, vm.settings.value.iso)
        assertEquals(50_000_000L, vm.settings.value.shutterSpeedNs)
        vm.clearExposureManual()
        assertEquals(null, vm.settings.value.iso)
        assertEquals(null, vm.settings.value.shutterSpeedNs)
        vm.updateManualSettings(iso = null, shutterNs = null, focus = null, wb = null)
        assertEquals(null, vm.settings.value.focusDistance)
        assertEquals(null, vm.settings.value.whiteBalanceMode)
    }

    @Test
    fun exposureIsAtomicWhenSetOneSided() {
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
                    exposureCompRange = -6..6,
                    exposureCompStep = 0.5f,
                    highSpeedVideo = null,
                    rawCapability = null,
                    apertures = emptyList(),
                )
            )
        )
        // ISO only: shutter fills with the 1/60 default (inside the range).
        vm.updateManualSettings(iso = 400, shutterNs = null, focus = null, wb = null)
        assertEquals(400, vm.settings.value.iso)
        assertEquals(16_666_667L, vm.settings.value.shutterSpeedNs)
        // Shutter only: ISO fills with the 100 default.
        vm.clearExposureManual()
        vm.updateManualSettings(iso = null, shutterNs = 50_000_000L, focus = null, wb = null)
        assertEquals(100, vm.settings.value.iso)
        assertEquals(50_000_000L, vm.settings.value.shutterSpeedNs)
        // Pure helper: both-auto stays both-auto.
        assertEquals(
            null to null,
            ManualExposure.complete(null, null, 100..800, 1L..100L),
        )
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

    @Test
    fun zoomNextClampsToUnitAndMax() {
        assertEquals(1f, ZoomRatios.next(1f, 0.5f, 8f))
        assertEquals(2f, ZoomRatios.next(1f, 2f, 8f))
        assertEquals(8f, ZoomRatios.next(4f, 3f, 8f))
        assertEquals(1f, ZoomRatios.next(1f, 2f, 1f))
        // Non-finite / non-positive factors never change the ratio.
        assertEquals(2f, ZoomRatios.next(2f, Float.NaN, 8f))
        assertEquals(2f, ZoomRatios.next(2f, 0f, 8f))
    }

    @Test
    fun exposureCompClampsToRange() {
        assertEquals(0, ExposureComp.clamp(5, null))
        assertEquals(6, ExposureComp.clamp(99, -6..6))
        assertEquals(-6, ExposureComp.clamp(-99, -6..6))
        assertEquals(2, ExposureComp.clamp(2, -6..6))
    }

    @Test
    fun evTextFormatsSignedValue() {
        assertEquals("+1.5 EV", ExposureComp.evText(3, 0.5f))
        assertEquals("-1.0 EV", ExposureComp.evText(-2, 0.5f))
        assertEquals("+0.0 EV", ExposureComp.evText(0, 0.5f))
    }

    @Test
    fun exposureCompensationUpdatesAndResets() {
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
                    exposureCompRange = -6..6,
                    exposureCompStep = 0.5f,
                    highSpeedVideo = null,
                    rawCapability = null,
                    apertures = emptyList(),
                )
            )
        )
        vm.updateExposureCompensation(99)
        assertEquals(6, vm.settings.value.exposureCompensation)
        vm.updateExposureCompensation(-99)
        assertEquals(-6, vm.settings.value.exposureCompensation)
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
                    exposureCompRange = -6..6,
                    exposureCompStep = 0.5f,
                    highSpeedVideo = null,
                    rawCapability = null,
                    apertures = emptyList(),
                )
            )
        )
        assertEquals(0, vm.settings.value.exposureCompensation)
    }

    @Test
    fun slowMoFactorScalesPlayback() {
        assertEquals(4f, SlowMoFactors.playbackFactor(120))
        assertEquals(8f, SlowMoFactors.playbackFactor(240))
        assertEquals(2f, SlowMoFactors.playbackFactor(60))
        assertEquals(1f, SlowMoFactors.playbackFactor(30))
        assertEquals(1f, SlowMoFactors.playbackFactor(0))
    }

    @Test
    fun yawDeltaWrapsAroundNorth() {
        assertEquals(10f, PanoEstimate.yawDelta(350f, 0f))
        assertEquals(-10f, PanoEstimate.yawDelta(0f, 350f))
        assertEquals(0f, PanoEstimate.yawDelta(45f, 45f))
        assertEquals(180f, PanoEstimate.yawDelta(0f, 180f))
    }

    @Test
    fun panoBudgetsShrinkOnLowRam() {
        assertEquals(PanoEstimate.MAX_FRAMES, PanoEstimate.maxFrames(256))
        assertEquals(PanoEstimate.MAX_FRAME_WIDTH, PanoEstimate.maxFrameWidth(256))
        assertEquals(PanoEstimate.LOW_RAM_MAX_FRAMES, PanoEstimate.maxFrames(128))
        assertEquals(PanoEstimate.LOW_RAM_FRAME_WIDTH, PanoEstimate.maxFrameWidth(96))
        assertTrue(PanoEstimate.LOW_RAM_MAX_FRAMES >= 2)
    }

    @Test
    fun stitcherFindsVerticalOffset() {
        // Horizontal stripes (period 20px); next is shifted down by 6px.
        val w = 200
        val h = 100
        val prev = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        val next = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
        for (y in 0 until h) {
            val cPrev = if ((y / 10) % 2 == 0) android.graphics.Color.WHITE else android.graphics.Color.BLACK
            val srcY = y - 6
            val cNext =
                if (srcY < 0) android.graphics.Color.GRAY
                else if ((srcY / 10) % 2 == 0) android.graphics.Color.WHITE else android.graphics.Color.BLACK
            for (x in 0 until w) {
                prev.setPixel(x, y, cPrev)
                next.setPixel(x, y, cNext)
            }
        }
        val dy = PanoramaStitcher.estimateVerticalOffset(prev, next)
        assertTrue("expected ~6, got $dy", kotlin.math.abs(dy - 6) <= 2)
        prev.recycle()
        next.recycle()
    }

    @Test
    fun stitchJoinsFrameWidths() {
        val a = android.graphics.Bitmap.createBitmap(100, 50, android.graphics.Bitmap.Config.ARGB_8888)
        val b = android.graphics.Bitmap.createBitmap(120, 50, android.graphics.Bitmap.Config.ARGB_8888)
        a.eraseColor(android.graphics.Color.RED)
        b.eraseColor(android.graphics.Color.BLUE)
        val out = PanoramaStitcher.stitch(listOf(a, b))
        // 100 + (120 - 30% overlap of 120).
        assertEquals(100 + (120 - 36), out?.width)
        assertTrue((out?.height ?: 0) >= 50)
        out?.recycle()
        a.recycle()
        b.recycle()
    }

    @Test
    fun formatStorageGbShowsOneDecimal() {
        assertEquals("--", formatStorageGb(-1L))
        assertEquals("0.0 GB", formatStorageGb(0L))
        assertEquals("12.4 GB", formatStorageGb(12_400_000_000L))
    }

    @Test
    fun tapInFocusBoxUnlocksOnlyInside() {
        // Box centered at (500, 500), half 68, view 1000x1000.
        assertEquals(true, ZoomRatios.isTapInFocusBox(500f, 500f, 500f, 500f, 68f, 1000f, 1000f))
        assertEquals(true, ZoomRatios.isTapInFocusBox(560f, 440f, 500f, 500f, 68f, 1000f, 1000f))
        assertEquals(false, ZoomRatios.isTapInFocusBox(100f, 100f, 500f, 500f, 68f, 1000f, 1000f))
        // Degenerate geometry never matches.
        assertEquals(false, ZoomRatios.isTapInFocusBox(500f, 500f, 500f, 500f, 0f, 1000f, 1000f))
        assertEquals(false, ZoomRatios.isTapInFocusBox(500f, 500f, 500f, 500f, 68f, 0f, 1000f))
        // Clamped box near the edge still hit-tests.
        assertEquals(true, ZoomRatios.isTapInFocusBox(30f, 30f, 10f, 10f, 68f, 1000f, 1000f))
    }

    @Test
    fun dedupeLensesDropsSameFocalTwins() {
        fun lens(id: String, type: com.oriyu90.fcampro.ui.CameraLensType, focal: Float) =
            com.oriyu90.fcampro.ui.CameraLensInfo(
                id = id,
                type = type,
                focalLength = focal,
                isFront = false,
                capabilities = LensCapabilities(
                    supportsManualSensor = true,
                    isoRange = 100..800,
                    exposureRangeNs = 1L..100L,
                    minFocusDistance = 0f,
                    awbModes = listOf(1),
                    hasFlash = false,
                    maxZoomRatio = 4f,
                    exposureCompRange = -6..6,
                    exposureCompStep = 0.5f,
                    highSpeedVideo = null,
                    rawCapability = null,
                    apertures = emptyList(),
                ),
                logicalCameraId = "0",
                physicalCameraId = if (id == "0") null else id,
            )
        val out =
            dedupeLenses(
                listOf(
                    lens("0", com.oriyu90.fcampro.ui.CameraLensType.WIDE, 5.4f),
                    // Same-focal physical twin: dropped.
                    lens("p1", com.oriyu90.fcampro.ui.CameraLensType.WIDE, 5.4f),
                    lens("p2", com.oriyu90.fcampro.ui.CameraLensType.TELEPHOTO, 8.0f),
                    // Distinct focal: kept.
                    lens("p3", com.oriyu90.fcampro.ui.CameraLensType.TELEPHOTO, 9.0f),
                )
            )
        assertEquals(listOf("0", "p2", "p3"), out.map { it.id })
    }

    @Test
    fun rawBitDepthFollowsWhiteLevel() {
        assertEquals(10, RawSupport.bitDepthFromWhiteLevel(1023))
        assertEquals(12, RawSupport.bitDepthFromWhiteLevel(4095))
        assertEquals(14, RawSupport.bitDepthFromWhiteLevel(16383))
        assertEquals(null, RawSupport.bitDepthFromWhiteLevel(255))
        assertEquals(null, RawSupport.bitDepthFromWhiteLevel(null))
    }

    @Test
    fun effectiveOutputFormatFallsBackToJpeg() {
        assertEquals(
            androidx.camera.core.ImageCapture.OUTPUT_FORMAT_RAW_JPEG,
            RawSupport.effectiveOutputFormat(
                androidx.camera.core.ImageCapture.OUTPUT_FORMAT_RAW_JPEG,
                true,
            ),
        )
        assertEquals(
            androidx.camera.core.ImageCapture.OUTPUT_FORMAT_JPEG,
            RawSupport.effectiveOutputFormat(
                androidx.camera.core.ImageCapture.OUTPUT_FORMAT_RAW,
                false,
            ),
        )
    }

    @Test
    fun saveFormatCoercesWithoutRawSupport() {
        val vm = vm()
        val rawLens =
            lensWith(
                LensCapabilities(
                    supportsManualSensor = true,
                    isoRange = 100..800,
                    exposureRangeNs = 1_000_000L..100_000_000L,
                    minFocusDistance = 5f,
                    awbModes = listOf(1, 2),
                    hasFlash = false,
                    maxZoomRatio = 2f,
                    exposureCompRange = -6..6,
                    exposureCompStep = 0.5f,
                    highSpeedVideo = null,
                    rawCapability = RawCapability(supported = true, bitDepth = 12, maxSize = null),
                    apertures = emptyList(),
                )
            )
        val jpegLens =
            lensWith(
                LensCapabilities(
                    supportsManualSensor = true,
                    isoRange = 100..800,
                    exposureRangeNs = 1_000_000L..100_000_000L,
                    minFocusDistance = 5f,
                    awbModes = listOf(1, 2),
                    hasFlash = false,
                    maxZoomRatio = 2f,
                    exposureCompRange = -6..6,
                    exposureCompStep = 0.5f,
                    highSpeedVideo = null,
                    rawCapability = RawCapability(supported = false, bitDepth = null, maxSize = null),
                    apertures = emptyList(),
                )
            )
        vm.setLens(rawLens)
        vm.setSaveFormat(SaveFormat.RAW)
        assertEquals(SaveFormat.RAW, vm.settings.value.saveFormat)
        // Selecting RAW on a JPEG-only lens falls back to JPEG.
        vm.setLens(jpegLens)
        assertEquals(SaveFormat.JPEG, vm.settings.value.saveFormat)
        vm.setSaveFormat(SaveFormat.JPEG_RAW)
        assertEquals(SaveFormat.JPEG, vm.settings.value.saveFormat)
    }
}
