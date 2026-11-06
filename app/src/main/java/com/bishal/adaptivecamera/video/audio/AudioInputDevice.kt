package com.bishal.adaptivecamera.video.audio

import android.media.AudioDeviceInfo
import android.os.Build

data class AudioInputDevice(
    val id: Int,
    val name: String,
    val typeName: String,
    val type: Int,
    val isExternal: Boolean,
    val isSelected: Boolean = false
) {
    companion object {
        fun fromDeviceInfo(device: AudioDeviceInfo, isSelected: Boolean = false): AudioInputDevice {
            val typeName = when (device.type) {
                AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Built-in Mic"
                AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset Mic"
                AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Audio Device"
                AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset Mic"
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth Mic (SCO)"
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth Audio"
                else -> "External Device (${device.type})"
            }

            val isExt = device.type != AudioDeviceInfo.TYPE_BUILTIN_MIC

            val productName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val name = device.productName?.toString()?.trim()
                if (!name.isNullOrEmpty()) name else typeName
            } else {
                typeName
            }

            return AudioInputDevice(
                id = device.id,
                name = productName,
                typeName = typeName,
                type = device.type,
                isExternal = isExt,
                isSelected = isSelected
            )
        }
    }
}
