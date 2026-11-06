package com.bishal.adaptivecamera.core.peripherals

/**
 * Standardized gimbal actions parsed from DJI / Zhiyun Bluetooth LE protocols.
 */
sealed class GimbalAction {
    object None : GimbalAction()
    object TriggerShutter : GimbalAction()
    object StartRecording : GimbalAction()
    object StopRecording : GimbalAction()
    object ToggleMode : GimbalAction()
    data class ZoomDelta(val deltaRatio: Float) : GimbalAction()
    data class JoystickMove(val panDelta: Float, val tiltDelta: Float) : GimbalAction()
}

enum class GimbalBrand {
    DJI_OSMO_MOBILE,
    ZHIYUN_SMOOTH,
    GENERIC_BLE
}

/**
 * Handles Bluetooth LE GATT payload parsing for gimbal peripheral hardware (FEAT-INT-06).
 * Decodes shutter, zoom wheel, joystick, and status packets from DJI Osmo Mobile
 * and Zhiyun Smooth series gimbals.
 */
class GimbalProtocolHandler(
    var activeBrand: GimbalBrand = GimbalBrand.DJI_OSMO_MOBILE
) {

    /**
     * Parses an incoming raw byte packet received over Bluetooth LE GATT.
     */
    fun parsePacket(bytes: ByteArray): GimbalAction {
        if (bytes.isEmpty()) return GimbalAction.None

        return when (activeBrand) {
            GimbalBrand.DJI_OSMO_MOBILE -> parseDjiPacket(bytes)
            GimbalBrand.ZHIYUN_SMOOTH -> parseZhiyunPacket(bytes)
            GimbalBrand.GENERIC_BLE -> parseGenericPacket(bytes)
        }
    }

    /**
     * DJI Osmo Mobile packet decoder:
     * Header: 0x55 (SOM), Length, CmdSet, CmdID, Data, CRC16.
     * Common Command IDs:
     * - 0x01: Shutter / Photo Capture
     * - 0x02: Start / Stop Recording
     * - 0x03: Zoom Slider (Data byte indicates zoom speed / direction)
     */
    private fun parseDjiPacket(bytes: ByteArray): GimbalAction {
        if (bytes.size < 4) return GimbalAction.None
        val header = bytes[0].toInt() and 0xFF
        if (header != 0x55) return GimbalAction.None

        val cmdId = bytes[3].toInt() and 0xFF
        return when (cmdId) {
            0x01 -> GimbalAction.TriggerShutter
            0x02 -> {
                val state = if (bytes.size > 4) bytes[4].toInt() and 0xFF else 0
                if (state == 1) GimbalAction.StartRecording else GimbalAction.StopRecording
            }
            0x03 -> {
                // Zoom byte: signed byte indicating zoom speed (-128 to 127)
                val rawZoom = if (bytes.size > 4) bytes[4].toInt() else 0
                val delta = (rawZoom.toFloat() / 100.0f).coerceIn(-0.5f, 0.5f)
                GimbalAction.ZoomDelta(delta)
            }
            0x04 -> GimbalAction.ToggleMode
            else -> GimbalAction.None
        }
    }

    /**
     * Zhiyun Smooth packet decoder:
     * Header: 0x24 (Header byte), Len, KeyCode, Param, Checksum.
     * Keycodes:
     * - 0x10: Shutter trigger
     * - 0x11: Record toggle
     * - 0x20: Zoom In (+)
     * - 0x21: Zoom Out (-)
     * - 0x30: Mode switch
     */
    private fun parseZhiyunPacket(bytes: ByteArray): GimbalAction {
        if (bytes.size < 3) return GimbalAction.None
        val header = bytes[0].toInt() and 0xFF
        if (header != 0x24) return GimbalAction.None

        val keyCode = bytes[2].toInt() and 0xFF
        return when (keyCode) {
            0x10 -> GimbalAction.TriggerShutter
            0x11 -> GimbalAction.StartRecording
            0x20 -> GimbalAction.ZoomDelta(+0.1f)
            0x21 -> GimbalAction.ZoomDelta(-0.1f)
            0x30 -> GimbalAction.ToggleMode
            else -> GimbalAction.None
        }
    }

    /**
     * Generic BLE HID/GATT packet decoder:
     * Fallback for generic Bluetooth camera remotes and sliders.
     */
    private fun parseGenericPacket(bytes: ByteArray): GimbalAction {
        if (bytes.isEmpty()) return GimbalAction.None
        return when (bytes[0].toInt() and 0xFF) {
            0x01 -> GimbalAction.TriggerShutter
            0x02 -> GimbalAction.ZoomDelta(+0.1f)
            0x03 -> GimbalAction.ZoomDelta(-0.1f)
            else -> GimbalAction.None
        }
    }

    companion object {
        // Known GATT Service / Characteristic UUIDs for DJI and Zhiyun
        const val DJI_SERVICE_UUID = "0000FFF0-0000-1000-8000-00805F9B34FB"
        const val DJI_CHAR_NOTIFY_UUID = "0000FFF1-0000-1000-8000-00805F9B34FB"
        const val ZHIYUN_SERVICE_UUID = "0000FFE0-0000-1000-8000-00805F9B34FB"
        const val ZHIYUN_CHAR_NOTIFY_UUID = "0000FFE1-0000-1000-8000-00805F9B34FB"
    }
}
