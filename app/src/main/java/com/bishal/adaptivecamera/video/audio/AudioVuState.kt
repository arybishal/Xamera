package com.bishal.adaptivecamera.video.audio

/**
 * Real-time audio VU meter telemetry data in dBFS (-60 dBFS to 0 dBFS).
 */
data class AudioVuState(
    val leftChannelDbfs: Float = -60.0f,
    val rightChannelDbfs: Float = -60.0f,
    val peakDbfs: Float = -60.0f,
    val isClipping: Boolean = false,
    val isMuted: Boolean = false
) {
    /**
     * Normalized 0.0 to 1.0 fraction for UI visual meter rendering.
     * Maps [-60 dBFS, 0 dBFS] linearly into [0.0, 1.0].
     */
    val leftFraction: Float
        get() = ((leftChannelDbfs + 60f) / 60f).coerceIn(0f, 1f)

    val rightFraction: Float
        get() = ((rightChannelDbfs + 60f) / 60f).coerceIn(0f, 1f)

    companion object {
        val SILENCE = AudioVuState(-60f, -60f, -60f, isClipping = false, isMuted = false)
        val MUTED = AudioVuState(-60f, -60f, -60f, isClipping = false, isMuted = true)
    }
}
