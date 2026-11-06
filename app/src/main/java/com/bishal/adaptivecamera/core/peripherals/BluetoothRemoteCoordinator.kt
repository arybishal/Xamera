package com.bishal.adaptivecamera.core.peripherals

import android.view.KeyEvent

/**
 * Configuration for volume key behavior when hardware buttons or Bluetooth remotes
 * send volume keycodes.
 */
enum class HardwareKeyBehavior {
    SHUTTER,
    ZOOM,
    BURST,
    OFF
}

/**
 * Handles Bluetooth HID remote controls, selfie stick triggers, headset buttons,
 * and hardware volume keys (FEAT-INT-06).
 */
class BluetoothRemoteCoordinator(
    var keyBehavior: HardwareKeyBehavior = HardwareKeyBehavior.SHUTTER,
    private val onTriggerShutter: () -> Unit,
    private val onZoomIn: () -> Unit = {},
    private val onZoomOut: () -> Unit = {}
) {

    /**
     * Inspects incoming key events and returns true if consumed.
     */
    fun onKeyEvent(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) {
            // Consume key up for handled keys to prevent system volume overlay or default beep
            return isMonitoredKey(keyCode)
        }

        return when (keyCode) {
            KeyEvent.KEYCODE_CAMERA -> {
                onTriggerShutter()
                true
            }
            KeyEvent.KEYCODE_HEADSETHOOK -> {
                onTriggerShutter()
                true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                when (keyBehavior) {
                    HardwareKeyBehavior.SHUTTER, HardwareKeyBehavior.BURST -> {
                        onTriggerShutter()
                        true
                    }
                    HardwareKeyBehavior.ZOOM -> {
                        onZoomIn()
                        true
                    }
                    HardwareKeyBehavior.OFF -> false
                }
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                when (keyBehavior) {
                    HardwareKeyBehavior.SHUTTER, HardwareKeyBehavior.BURST -> {
                        onTriggerShutter()
                        true
                    }
                    HardwareKeyBehavior.ZOOM -> {
                        onZoomOut()
                        true
                    }
                    HardwareKeyBehavior.OFF -> false
                }
            }
            else -> false
        }
    }

    private fun isMonitoredKey(keyCode: Int): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_CAMERA,
            KeyEvent.KEYCODE_HEADSETHOOK,
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN -> keyBehavior != HardwareKeyBehavior.OFF
            else -> false
        }
    }
}
