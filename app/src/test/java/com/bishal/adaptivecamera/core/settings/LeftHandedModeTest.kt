package com.bishal.adaptivecamera.core.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeftHandedModeTest {

    @Test
    fun testDefaultLeftHandedModeDisabled() {
        val prefs = CameraPreferences()
        assertFalse("Left-handed mode should be disabled by default", prefs.isLeftHandedModeEnabled)
    }

    @Test
    fun testEnableLeftHandedModeInPreferences() {
        val prefs = CameraPreferences()
        val updated = prefs.copy(isLeftHandedModeEnabled = true)
        assertTrue(updated.isLeftHandedModeEnabled)
    }
}
