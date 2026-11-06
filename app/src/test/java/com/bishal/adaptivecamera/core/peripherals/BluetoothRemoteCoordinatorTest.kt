package com.bishal.adaptivecamera.core.peripherals

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothRemoteCoordinatorTest {

    @Test
    fun testShutterKeyCodeTriggersCallback() {
        var shutterCount = 0
        val coordinator = BluetoothRemoteCoordinator(
            keyBehavior = HardwareKeyBehavior.SHUTTER,
            onTriggerShutter = { shutterCount++ }
        )

        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_CAMERA)
        val handled = coordinator.onKeyEvent(KeyEvent.KEYCODE_CAMERA, downEvent)

        assertTrue(handled)
        assertEquals(1, shutterCount)
    }

    @Test
    fun testHeadsetHookShutterTrigger() {
        var shutterCount = 0
        val coordinator = BluetoothRemoteCoordinator(
            keyBehavior = HardwareKeyBehavior.SHUTTER,
            onTriggerShutter = { shutterCount++ }
        )

        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK)
        val handled = coordinator.onKeyEvent(KeyEvent.KEYCODE_HEADSETHOOK, downEvent)

        assertTrue(handled)
        assertEquals(1, shutterCount)
    }

    @Test
    fun testVolumeKeysInZoomMode() {
        var zoomInCount = 0
        var zoomOutCount = 0
        val coordinator = BluetoothRemoteCoordinator(
            keyBehavior = HardwareKeyBehavior.ZOOM,
            onTriggerShutter = {},
            onZoomIn = { zoomInCount++ },
            onZoomOut = { zoomOutCount++ }
        )

        val volUp = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP)
        val volDown = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_DOWN)

        assertTrue(coordinator.onKeyEvent(KeyEvent.KEYCODE_VOLUME_UP, volUp))
        assertEquals(1, zoomInCount)

        assertTrue(coordinator.onKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN, volDown))
        assertEquals(1, zoomOutCount)
    }

    @Test
    fun testHardwareKeysOffBehavior() {
        var shutterCount = 0
        val coordinator = BluetoothRemoteCoordinator(
            keyBehavior = HardwareKeyBehavior.OFF,
            onTriggerShutter = { shutterCount++ }
        )

        val volUp = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_VOLUME_UP)
        val handled = coordinator.onKeyEvent(KeyEvent.KEYCODE_VOLUME_UP, volUp)

        assertFalse(handled)
        assertEquals(0, shutterCount)
    }
}
