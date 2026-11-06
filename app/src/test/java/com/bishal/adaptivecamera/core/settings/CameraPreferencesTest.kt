package com.bishal.adaptivecamera.core.settings

import com.bishal.adaptivecamera.camera.config.AspectRatioConfig
import com.bishal.adaptivecamera.camera.config.CompositionGridType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraPreferencesTest {

    @Test
    fun testDefaultCameraPreferences() {
        val prefs = CameraPreferences()

        assertEquals(CompositionGridType.OFF, prefs.gridType)
        assertTrue(prefs.isVirtualHorizonEnabled)
        assertEquals(AspectRatioConfig.RATIO_4_3, prefs.aspectRatio)
        assertTrue(prefs.isHapticsEnabled)
        assertFalse(prefs.isFloatingShutterEnabled)
        assertEquals(0.82f, prefs.floatingShutterX, 0.001f)
        assertEquals(0.65f, prefs.floatingShutterY, 0.001f)
        assertTrue(prefs.isFrontScreenFlashEnabled)
        assertTrue(prefs.isZslEnabled)
        assertEquals(3, prefs.aebBracketCount)
        assertEquals(1.0f, prefs.aebEvStep, 0.001f)
        assertEquals("JPEG", prefs.captureFormat)
        assertFalse(prefs.isWatermarkEnabled)
        assertFalse(prefs.isExifPrivacyEnabled)
        assertTrue(prefs.stripGpsLocation)
        assertTrue(prefs.stripDeviceModel)
    }

    @Test
    fun testCustomCameraPreferencesCopy() {
        val prefs = CameraPreferences().copy(
            gridType = CompositionGridType.GOLDEN_RATIO,
            isVirtualHorizonEnabled = false,
            aspectRatio = AspectRatioConfig.RATIO_16_9,
            isHapticsEnabled = false,
            isFloatingShutterEnabled = true,
            floatingShutterX = 0.5f,
            floatingShutterY = 0.5f,
            isFrontScreenFlashEnabled = false
        )

        assertEquals(CompositionGridType.GOLDEN_RATIO, prefs.gridType)
        assertFalse(prefs.isVirtualHorizonEnabled)
        assertEquals(AspectRatioConfig.RATIO_16_9, prefs.aspectRatio)
        assertFalse(prefs.isHapticsEnabled)
        assertTrue(prefs.isFloatingShutterEnabled)
        assertEquals(0.5f, prefs.floatingShutterX, 0.001f)
        assertEquals(0.5f, prefs.floatingShutterY, 0.001f)
        assertFalse(prefs.isFrontScreenFlashEnabled)
    }
}
