package com.bishal.adaptivecamera.video.modes

/**
 * Capture frame rates supported for Slow-Motion recording.
 */
enum class SlowMotionCaptureFps(val fps: Int, val displayName: String, val speedFactor: Float) {
    FPS_120(120, "120 fps (4x)", 4.0f),
    FPS_240(240, "240 fps (8x)", 8.0f);

    /**
     * Calculates the expanded playback duration.
     * Example: 5 seconds recorded at 120 fps plays for 20 seconds at 30 fps (4x slowdown).
     */
    fun calculateOutputDuration(capturedDurationSec: Float, playbackFps: Int = 30): Float {
        return capturedDurationSec * (fps.toFloat() / playbackFps.toFloat())
    }
}

/**
 * Slow-motion recording configuration.
 */
data class SlowMotionConfig(
    val captureFps: SlowMotionCaptureFps = SlowMotionCaptureFps.FPS_120,
    val playbackFps: Int = 30
)
