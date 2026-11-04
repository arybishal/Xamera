package com.bishal.adaptivecamera.camera.config

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraConfigurationTest {

    @Test
    fun testCameraFacingToggle() {
        assertEquals(CameraFacing.FRONT, CameraFacing.BACK.toggle())
        assertEquals(CameraFacing.BACK, CameraFacing.FRONT.toggle())
    }

    @Test
    fun testFlashModeTransitions() {
        assertEquals(FlashMode.AUTO, FlashMode.OFF.next(supportsAuto = true, supportsTorch = true))
        assertEquals(FlashMode.ON, FlashMode.AUTO.next(supportsAuto = true, supportsTorch = true))
        assertEquals(FlashMode.TORCH, FlashMode.ON.next(supportsAuto = true, supportsTorch = true))
        assertEquals(FlashMode.OFF, FlashMode.TORCH.next(supportsAuto = true, supportsTorch = true))

        // When auto and torch are not supported
        assertEquals(FlashMode.ON, FlashMode.OFF.next(supportsAuto = false, supportsTorch = false))
        assertEquals(FlashMode.OFF, FlashMode.ON.next(supportsAuto = false, supportsTorch = false))
    }

    @Test
    fun testPhotoTimerTransitions() {
        assertEquals(PhotoTimer.SEC_3, PhotoTimer.OFF.next())
        assertEquals(PhotoTimer.SEC_10, PhotoTimer.SEC_3.next())
        assertEquals(PhotoTimer.OFF, PhotoTimer.SEC_10.next())
    }

    @Test
    fun testExposureEvDisplayFormatting() {
        val configPositive = CameraConfiguration(
            exposureCompensationIndex = 2,
            exposureStepEv = 0.5f
        )
        assertEquals("+1.0 EV", configPositive.exposureEvDisplay)

        val configNegative = CameraConfiguration(
            exposureCompensationIndex = -3,
            exposureStepEv = 0.33333334f
        )
        assertEquals("-1.0 EV", configNegative.exposureEvDisplay)

        val configZero = CameraConfiguration(
            exposureCompensationIndex = 0,
            exposureStepEv = 0.5f
        )
        assertEquals("0.0 EV", configZero.exposureEvDisplay)
    }

    @Test
    fun testZoomClampingLogic() {
        val config = CameraConfiguration(
            minZoomRatio = 1.0f,
            maxZoomRatio = 10.0f
        )
        val clampedUnder = 0.5f.coerceIn(config.minZoomRatio, config.maxZoomRatio)
        assertEquals(1.0f, clampedUnder, 0.001f)

        val clampedOver = 15.0f.coerceIn(config.minZoomRatio, config.maxZoomRatio)
        assertEquals(10.0f, clampedOver, 0.001f)

        val clampedValid = 3.5f.coerceIn(config.minZoomRatio, config.maxZoomRatio)
        assertEquals(3.5f, clampedValid, 0.001f)
    }
}
