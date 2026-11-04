package com.bishal.adaptivecamera.camera.engine

import com.bishal.adaptivecamera.camera.config.CameraConfiguration
import com.bishal.adaptivecamera.camera.config.CameraFacing
import com.bishal.adaptivecamera.camera.config.CameraMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class ExposureReliabilityAndSettingsTest {

    @Test
    fun testDefaultExposureStateIsSafeAndNotReady() {
        val config = CameraConfiguration()
        assertFalse("Default state must not claim exposure is ready before camera binds", config.isExposureStateReady)
        assertFalse("Default state must not claim exposure is supported before querying HAL", config.isExposureCompensationSupported)
        assertEquals(0, config.exposureCompensationIndex)
        assertEquals(0, config.minExposureIndex)
        assertEquals(0, config.maxExposureIndex)
        assertFalse("Exposure control must not be available on uninitialized camera", config.isExposureControlAvailable)
        assertEquals("0.0 EV", config.exposureEvDisplay)
    }

    @Test
    fun testExposureControlAvailableWhenBoundAndSupported() {
        val config = CameraConfiguration(
            isExposureStateReady = true,
            isExposureCompensationSupported = true,
            minExposureIndex = -4,
            maxExposureIndex = 4,
            exposureStepEv = 0.5f,
            exposureCompensationIndex = 0
        )
        assertTrue("Control must be available when ready, supported, and valid range exists", config.isExposureControlAvailable)
        assertEquals("0.0 EV", config.exposureEvDisplay)
    }

    @Test
    fun testExposureControlUnavailableWhenHardwareUnsupported() {
        val config = CameraConfiguration(
            isExposureStateReady = true,
            isExposureCompensationSupported = false,
            minExposureIndex = 0,
            maxExposureIndex = 0
        )
        assertFalse("Control must report unavailable when sensor hardware does not support compensation", config.isExposureControlAvailable)
        assertEquals("0.0 EV", config.exposureEvDisplay)
    }

    @Test
    fun testExposureControlUnavailableWhenNotReady() {
        val config = CameraConfiguration(
            isExposureStateReady = false,
            isExposureCompensationSupported = true,
            minExposureIndex = -6,
            maxExposureIndex = 6
        )
        assertFalse("Control must report unavailable while camera session is still binding or switching", config.isExposureControlAvailable)
    }

    @Test
    fun testExposureControlUnavailableWhenRangeFlat() {
        val config = CameraConfiguration(
            isExposureStateReady = true,
            isExposureCompensationSupported = true,
            minExposureIndex = 0,
            maxExposureIndex = 0
        )
        assertFalse("Control must report unavailable if min >= max range", config.isExposureControlAvailable)
    }

    @Test
    fun testExposureEvDisplayFormatting() {
        Locale.setDefault(Locale.US)

        val configPositive = CameraConfiguration(
            isExposureStateReady = true,
            isExposureCompensationSupported = true,
            minExposureIndex = -6,
            maxExposureIndex = 6,
            exposureStepEv = 0.33333334f,
            exposureCompensationIndex = 3
        )
        assertEquals("+1.0 EV", configPositive.exposureEvDisplay)

        val configNegative = CameraConfiguration(
            isExposureStateReady = true,
            isExposureCompensationSupported = true,
            minExposureIndex = -6,
            maxExposureIndex = 6,
            exposureStepEv = 0.5f,
            exposureCompensationIndex = -3
        )
        assertEquals("-1.5 EV", configNegative.exposureEvDisplay)

        val configZero = CameraConfiguration(
            isExposureStateReady = true,
            isExposureCompensationSupported = true,
            minExposureIndex = -4,
            maxExposureIndex = 4,
            exposureStepEv = 0.5f,
            exposureCompensationIndex = 0
        )
        assertEquals("0.0 EV", configZero.exposureEvDisplay)
    }

    @Test
    fun testExposureClampingLogic() {
        val min = -6
        val max = 6

        val requestedHigh = 10
        val clampedHigh = requestedHigh.coerceIn(min, max)
        assertEquals(6, clampedHigh)

        val requestedLow = -12
        val clampedLow = requestedLow.coerceIn(min, max)
        assertEquals(-6, clampedLow)

        val requestedMid = 2
        val clampedMid = requestedMid.coerceIn(min, max)
        assertEquals(2, clampedMid)
    }

    @Test
    fun testCameraFacingResetBehavior() {
        val initialConfig = CameraConfiguration(
            facing = CameraFacing.BACK,
            exposureCompensationIndex = 3,
            isExposureStateReady = true,
            isExposureCompensationSupported = true,
            minExposureIndex = -4,
            maxExposureIndex = 4
        )

        // Simulating camera facing switch: state reset to unready & index reset to 0
        val switchedConfig = initialConfig.copy(
            facing = initialConfig.facing.toggle(),
            isExposureStateReady = false,
            exposureCompensationIndex = 0
        )

        assertEquals(CameraFacing.FRONT, switchedConfig.facing)
        assertEquals(0, switchedConfig.exposureCompensationIndex)
        assertFalse("Switched camera must wait for new session ready before enabling controls", switchedConfig.isExposureStateReady)
    }
}
