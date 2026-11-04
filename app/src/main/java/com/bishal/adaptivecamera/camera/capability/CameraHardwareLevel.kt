package com.bishal.adaptivecamera.camera.capability

import android.hardware.camera2.CameraCharacteristics

enum class CameraHardwareLevel(val rawLevel: Int, val displayName: String) {
    LIMITED(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED, "LIMITED"),
    FULL(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL, "FULL"),
    LEGACY(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY, "LEGACY"),
    LEVEL_3(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3, "LEVEL_3"),
    EXTERNAL(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL, "EXTERNAL"),
    UNKNOWN(-1, "UNKNOWN");

    companion object {
        fun fromInt(value: Int?): CameraHardwareLevel {
            return entries.firstOrNull { it.rawLevel == value } ?: UNKNOWN
        }
    }
}
