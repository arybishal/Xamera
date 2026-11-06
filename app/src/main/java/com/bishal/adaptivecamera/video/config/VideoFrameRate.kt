package com.bishal.adaptivecamera.video.config

/**
 * Standard professional video recording frame rates.
 */
enum class VideoFrameRate(
    val fps: Int,
    val label: String,
    val isHighSpeed: Boolean = false
) {
    FPS_24(24, "24 fps (Cinema)"),
    FPS_25(25, "25 fps (PAL)"),
    FPS_30(30, "30 fps (Standard)"),
    FPS_48(48, "48 fps (HFR)"),
    FPS_50(50, "50 fps (PAL HFR)"),
    FPS_60(60, "60 fps (Smooth)"),
    FPS_120(120, "120 fps (Slow-Mo)", isHighSpeed = true),
    FPS_240(240, "240 fps (Super Slow-Mo)", isHighSpeed = true);

    val shortLabel: String get() = "$fps"

    companion object {
        fun fromFps(targetFps: Int): VideoFrameRate {
            return entries.minByOrNull { kotlin.math.abs(it.fps - targetFps) } ?: FPS_30
        }
    }
}
