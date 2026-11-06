package com.bishal.adaptivecamera.video.modes

/**
 * Capture intervals for Time-Lapse recording.
 */
enum class TimelapseInterval(val seconds: Float, val displayName: String, val recommendedScene: String) {
    HALF_SEC(0.5f, "0.5s", "Fast Traffic & Pedestrians"),
    ONE_SEC(1.0f, "1s", "Moving Clouds & Busy Streets"),
    TWO_SEC(2.0f, "2s", "Sunset & Sunrise"),
    FIVE_SEC(5.0f, "5s", "Slow Clouds & Shadows"),
    TEN_SEC(10.0f, "10s", "Plant Growth & Stars"),
    THIRTY_SEC(30.0f, "30s", "Long Term Construction & Tides"),
    SIXTY_SEC(60.0f, "60s", "Extended Solar & Lunar Transitions");

    /**
     * Calculates video speedup multiplier at specified playback framerate.
     * Example: 1 frame every 1s at 30fps playback = 30x real-world speedup.
     */
    fun calculateSpeedup(playbackFps: Int = 30): Float = seconds * playbackFps.toFloat()
}

/**
 * Real-time telemetry emitted during Time-Lapse recording.
 */
data class TimelapseTelemetry(
    val interval: TimelapseInterval = TimelapseInterval.ONE_SEC,
    val capturedFramesCount: Long = 0L,
    val realElapsedSeconds: Long = 0L,
    val outputVideoSeconds: Float = 0f,
    val speedupFactor: Float = 30f
)
