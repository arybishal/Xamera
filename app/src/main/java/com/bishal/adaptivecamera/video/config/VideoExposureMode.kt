package com.bishal.adaptivecamera.video.config

/**
 * Operating exposure mode during video recording.
 */
enum class VideoExposureMode(val label: String) {
    AUTO("Auto Exposure"),
    SHUTTER_PRIORITY("Shutter Priority (Angle)"),
    MANUAL("Manual (ISO + Shutter)")
}
