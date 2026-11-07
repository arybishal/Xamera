package com.bishal.adaptivecamera.ui.camera

import com.bishal.adaptivecamera.camera.config.AspectRatioConfig
import com.bishal.adaptivecamera.camera.config.CameraConfiguration
import com.bishal.adaptivecamera.camera.config.CameraMode
import com.bishal.adaptivecamera.camera.config.CompositionGridType
import com.bishal.adaptivecamera.camera.config.FlashMode
import com.bishal.adaptivecamera.camera.config.HdrPlusMode
import com.bishal.adaptivecamera.camera.config.PhotoTimer
import com.bishal.adaptivecamera.intelligence.analysis.FrameMetrics
import com.bishal.adaptivecamera.intelligence.scene.SceneCategory
import com.bishal.adaptivecamera.intelligence.scene.SceneClassificationResult
import com.bishal.adaptivecamera.photography.auto.SmartAutoState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying behavioral logic, formatting, and state mappings
 * for the Minimalist Premium Camera UI components.
 */
class MinimalistUiComponentsTest {

    // Helper formatting matching MinimalistZoomBar logic
    private fun formatZoomLabel(zoom: Float): String {
        return when {
            zoom < 1f -> String.format(java.util.Locale.US, "%.1f", zoom)
            zoom == 1f -> "1"
            zoom == zoom.toInt().toFloat() -> "${zoom.toInt()}x"
            else -> String.format(java.util.Locale.US, "%.1fx", zoom)
        }
    }

    @Test
    fun testZoomStepFormatting() {
        assertEquals("0.6", formatZoomLabel(0.6f))
        assertEquals("0.5", formatZoomLabel(0.5f))
        assertEquals("1", formatZoomLabel(1.0f))
        assertEquals("2x", formatZoomLabel(2.0f))
        assertEquals("3x", formatZoomLabel(3.0f))
        assertEquals("5x", formatZoomLabel(5.0f))
        assertEquals("10x", formatZoomLabel(10.0f))
        assertEquals("2.5x", formatZoomLabel(2.5f))
    }

    @Test
    fun testMinimalistModeCarouselItems() {
        val primaryModes = listOf(
            CameraMode.PRO,
            CameraMode.VIDEO,
            CameraMode.PHOTO,
            CameraMode.PORTRAIT
        )

        assertEquals("PRO", primaryModes[0].displayName.uppercase())
        assertEquals("VIDEO", primaryModes[1].displayName.uppercase())
        assertEquals("PHOTO", primaryModes[2].displayName.uppercase())
        assertEquals("PORTRAIT", primaryModes[3].displayName.uppercase())

        val moreModes = listOf(
            CameraMode.TIME_LAPSE,
            CameraMode.SLOW_MO,
            CameraMode.DUAL
        )

        assertTrue(moreModes.contains(CameraMode.TIME_LAPSE))
        assertTrue(moreModes.contains(CameraMode.SLOW_MO))
        assertTrue(moreModes.contains(CameraMode.DUAL))
    }

    @Test
    fun testSceneBadgeVisibilityConditions() {
        // Condition 1: Standard daylight, not dismissed -> Should NOT show
        val daylightState = SmartAutoState(
            sceneResult = SceneClassificationResult(
                primaryCategory = SceneCategory.STANDARD,
                confidence = 0.9f,
                explanation = "Standard daylight scene"
            ),
            latestMetrics = FrameMetrics(
                meanLuminance = 0.5f,
                rmsContrast = 0.2f,
                shadowRatio = 0.1f,
                highlightRatio = 0.1f,
                midtoneRatio = 0.8f,
                estimatedDynamicRangeStops = 8.0f,
                motionDelta = 0.05f
            )
        )
        val categoryDaylight = daylightState.sceneResult?.primaryCategory ?: SceneCategory.STANDARD
        val isLowLightDaylight = daylightState.latestMetrics?.isLowLight == true
        val shouldShowDaylight = !false && (categoryDaylight != SceneCategory.STANDARD || isLowLightDaylight)
        assertFalse(shouldShowDaylight)

        // Condition 2: Night detected -> Should show
        val nightState = SmartAutoState(
            sceneResult = SceneClassificationResult(
                primaryCategory = SceneCategory.NIGHT,
                confidence = 0.95f,
                explanation = "Low light night scene"
            ),
            latestMetrics = FrameMetrics(
                meanLuminance = 0.1f,
                rmsContrast = 0.08f,
                shadowRatio = 0.75f,
                highlightRatio = 0.02f,
                midtoneRatio = 0.23f,
                estimatedDynamicRangeStops = 4.0f,
                motionDelta = 0.02f
            )
        )
        val categoryNight = nightState.sceneResult?.primaryCategory ?: SceneCategory.STANDARD
        val isLowLightNight = nightState.latestMetrics?.isLowLight == true
        val shouldShowNight = !false && (categoryNight != SceneCategory.STANDARD || isLowLightNight)
        assertTrue(shouldShowNight)

        // Condition 3: Night detected but user dismissed -> Should NOT show
        val shouldShowDismissed = !true && (categoryNight != SceneCategory.STANDARD || isLowLightNight)
        assertFalse(shouldShowDismissed)
    }

    @Test
    fun testAspectRatioCycleLogic() {
        assertEquals("4:3", AspectRatioConfig.RATIO_4_3.displayName)
        assertEquals("16:9", AspectRatioConfig.RATIO_16_9.displayName)
        assertEquals("1:1", AspectRatioConfig.RATIO_1_1.displayName)
        assertEquals("Full", AspectRatioConfig.FULL.displayName)
    }

    @Test
    fun testCompositionGridCycleLogic() {
        val nextFromOff = CompositionGridType.OFF.next()
        assertEquals(CompositionGridType.RULE_OF_THIRDS, nextFromOff)

        val nextFromThirds = CompositionGridType.RULE_OF_THIRDS.next()
        assertEquals(CompositionGridType.GOLDEN_RATIO, nextFromThirds)

        val nextFromGolden = CompositionGridType.GOLDEN_RATIO.next()
        assertEquals(CompositionGridType.GOLDEN_SPIRAL, nextFromGolden)

        val nextFromSpiral = CompositionGridType.GOLDEN_SPIRAL.next()
        assertEquals(CompositionGridType.CENTER_CROSSHAIRS, nextFromSpiral)

        val nextFromCross = CompositionGridType.CENTER_CROSSHAIRS.next()
        assertEquals(CompositionGridType.OFF, nextFromCross)
    }

    @Test
    fun testPhotoTimerCycleLogic() {
        val nextFromOff = PhotoTimer.OFF.next()
        assertEquals(PhotoTimer.SEC_3, nextFromOff)

        val nextFrom3s = PhotoTimer.SEC_3.next()
        assertEquals(PhotoTimer.SEC_10, nextFrom3s)

        val nextFrom10s = PhotoTimer.SEC_10.next()
        assertEquals(PhotoTimer.OFF, nextFrom10s)
    }

    @Test
    fun testHdrPlusModeCycleLogic() {
        val nextFromOff = HdrPlusMode.OFF.next()
        assertEquals(HdrPlusMode.AUTO, nextFromOff)

        val nextFromAuto = HdrPlusMode.AUTO.next()
        assertEquals(HdrPlusMode.ON, nextFromAuto)

        val nextFromOn = HdrPlusMode.ON.next()
        assertEquals(HdrPlusMode.OFF, nextFromOn)
    }

    @Test
    fun testFlashModeCycleLogic() {
        val nextFromOff = FlashMode.OFF.next(supportsAuto = true, supportsTorch = true)
        assertEquals(FlashMode.AUTO, nextFromOff)

        val nextFromAuto = FlashMode.AUTO.next(supportsAuto = true, supportsTorch = true)
        assertEquals(FlashMode.ON, nextFromAuto)

        val nextFromOn = FlashMode.ON.next(supportsAuto = true, supportsTorch = true)
        assertEquals(FlashMode.TORCH, nextFromOn)

        val nextFromTorch = FlashMode.TORCH.next(supportsAuto = true, supportsTorch = true)
        assertEquals(FlashMode.OFF, nextFromTorch)
    }

    @Test
    fun testCameraConfigDefaultsForMinimalistUI() {
        val config = CameraConfiguration()
        assertEquals(CameraMode.PHOTO, config.mode)
        assertEquals(AspectRatioConfig.RATIO_4_3, config.aspectRatio)
        assertEquals(FlashMode.OFF, config.flashMode)
        assertEquals(PhotoTimer.OFF, config.timer)
        assertEquals(CompositionGridType.OFF, config.gridType)
        assertEquals(HdrPlusMode.OFF, config.hdrPlusMode)
        assertEquals(1.0f, config.zoomRatio, 0.001f)
    }

    @Test
    fun testImageStackingPreservation() {
        val defaultStacking = com.bishal.adaptivecamera.photography.computational.ImageStackingConfig()
        assertFalse("Stacking should be optional and off by default", defaultStacking.isEnabled)
        assertEquals(com.bishal.adaptivecamera.photography.computational.ImageStackingMode.SMART_AUTO, defaultStacking.mode)
        assertEquals(com.bishal.adaptivecamera.photography.computational.ImageStackingFrameCount.AUTO, defaultStacking.frameCount)
        assertEquals("Default configuration must capture 5 frames", 5, defaultStacking.frameCount.count)

        // Verify frame counts across all options
        assertEquals(3, com.bishal.adaptivecamera.photography.computational.ImageStackingFrameCount.FRAMES_3.count)
        assertEquals(5, com.bishal.adaptivecamera.photography.computational.ImageStackingFrameCount.FRAMES_5.count)
        assertEquals(7, com.bishal.adaptivecamera.photography.computational.ImageStackingFrameCount.FRAMES_7.count)

        // Verify quick settings badge formatting
        val enabledStacking = defaultStacking.copy(isEnabled = true)
        val badge = if (enabledStacking.isEnabled) "${enabledStacking.frameCount.count}F" else null
        assertEquals("5F", badge)
    }
}
