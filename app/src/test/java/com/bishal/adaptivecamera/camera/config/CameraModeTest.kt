package com.bishal.adaptivecamera.camera.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraModeTest {

    @Test
    fun testCameraModeEntries() {
        val modes = CameraMode.values()
        assertEquals(7, modes.size)
        assertEquals("PORTRAIT", CameraMode.PORTRAIT.displayName)
        assertEquals("PHOTO", CameraMode.PHOTO.displayName)
        assertEquals("PRO", CameraMode.PRO.displayName)
        assertEquals("VIDEO", CameraMode.VIDEO.displayName)
        assertEquals("TIME-LAPSE", CameraMode.TIME_LAPSE.displayName)
        assertEquals("SLOW-MO", CameraMode.SLOW_MO.displayName)
        assertEquals("DUAL", CameraMode.DUAL.displayName)
    }

    @Test
    fun testIsVideoModeProperty() {
        assertFalse(CameraMode.PORTRAIT.isVideoMode)
        assertFalse(CameraMode.PHOTO.isVideoMode)
        assertFalse(CameraMode.PRO.isVideoMode)
        assertTrue(CameraMode.VIDEO.isVideoMode)
        assertTrue(CameraMode.TIME_LAPSE.isVideoMode)
        assertTrue(CameraMode.SLOW_MO.isVideoMode)
        assertTrue(CameraMode.DUAL.isVideoMode)
    }
}
