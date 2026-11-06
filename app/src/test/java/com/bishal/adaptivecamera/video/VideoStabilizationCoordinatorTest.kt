package com.bishal.adaptivecamera.video

import android.hardware.camera2.CameraMetadata
import com.bishal.adaptivecamera.video.config.VideoStabilizationMode
import com.bishal.adaptivecamera.video.stabilization.VideoStabilizationCoordinator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoStabilizationCoordinatorTest {

    @Test
    fun resolveStabilizationIntent_offMode_disablesOisAndEis() {
        val intent = VideoStabilizationCoordinator.resolveStabilizationIntent(
            VideoStabilizationMode.OFF,
            hasOis = true,
            hasEis = true
        )
        assertEquals(CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF, intent.oisMode)
        assertEquals(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF, intent.eisMode)
        assertFalse(intent.isOpticalActive)
        assertFalse(intent.isElectronicActive)
    }

    @Test
    fun resolveStabilizationIntent_oisMode_enablesOisAndDisablesEis() {
        val intent = VideoStabilizationCoordinator.resolveStabilizationIntent(
            VideoStabilizationMode.OIS,
            hasOis = true,
            hasEis = true
        )
        assertEquals(CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON, intent.oisMode)
        assertEquals(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF, intent.eisMode)
        assertTrue(intent.isOpticalActive)
        assertFalse(intent.isElectronicActive)
    }

    @Test
    fun resolveStabilizationIntent_eisMode_enablesEisAndDisablesOis() {
        val intent = VideoStabilizationCoordinator.resolveStabilizationIntent(
            VideoStabilizationMode.EIS,
            hasOis = true,
            hasEis = true
        )
        assertEquals(CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF, intent.oisMode)
        assertEquals(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON, intent.eisMode)
        assertFalse(intent.isOpticalActive)
        assertTrue(intent.isElectronicActive)
    }

    @Test
    fun resolveStabilizationIntent_combinedMode_enablesBothWhenHardwareSupported() {
        val intent = VideoStabilizationCoordinator.resolveStabilizationIntent(
            VideoStabilizationMode.COMBINED,
            hasOis = true,
            hasEis = true
        )
        assertEquals(CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON, intent.oisMode)
        assertEquals(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON, intent.eisMode)
        assertTrue(intent.isOpticalActive)
        assertTrue(intent.isElectronicActive)
    }

    @Test
    fun resolveStabilizationIntent_combinedMode_fallsBackGracefullyWhenOisMissing() {
        val intent = VideoStabilizationCoordinator.resolveStabilizationIntent(
            VideoStabilizationMode.COMBINED,
            hasOis = false, // Device has no hardware OIS
            hasEis = true
        )
        assertEquals(CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF, intent.oisMode)
        assertEquals(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON, intent.eisMode)
        assertFalse(intent.isOpticalActive)
        assertTrue(intent.isElectronicActive)
    }
}
