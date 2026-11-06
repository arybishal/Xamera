package com.bishal.adaptivecamera.video.exposure

/**
 * Calculates cinematic shutter speed from shutter angle (in degrees)
 * and frame rate (FPS), and vice versa.
 *
 * Formula:
 *   Exposure Time (seconds) = 1.0 / (FPS * (shutterAngle / 360.0))
 *   Exposure Time (nanoseconds) = (360.0 / (FPS * shutterAngle)) * 10^9
 */
object ShutterAngleCalculator {

    /** Common photographic and cinematic shutter angles */
    val PRESET_SHUTTER_ANGLES = listOf(
        360f to "360° (Full Exposure)",
        270f to "270° (Motion Blur)",
        180f to "180° (Cinema Standard)",
        144f to "144° (50Hz Flicker Reduction)",
        90f to "90° (Action Crisp)",
        45f to "45° (Extreme Sharpness)"
    )

    /**
     * Calculates exposure time in seconds for a given FPS and shutter angle.
     * Formula: Exposure Time (seconds) = (shutterAngle / 360.0) / FPS
     */
    fun calculateExposureSeconds(fps: Int, shutterAngle: Float): Double {
        require(fps > 0) { "FPS must be greater than 0" }
        require(shutterAngle > 0f) { "Shutter angle must be greater than 0°" }
        val clampedAngle = shutterAngle.coerceIn(1.0f, 360.0f)
        return (clampedAngle.toDouble() / 360.0) / fps.toDouble()
    }

    /**
     * Calculates exposure time in nanoseconds for Camera2 CaptureRequest.SENSOR_EXPOSURE_TIME.
     * Optionally clamps to physical sensor hardware bounds.
     */
    fun calculateExposureNanoseconds(
        fps: Int,
        shutterAngle: Float,
        minExposureNanos: Long = 10_000L,       // 1/100,000s default lower bound
        maxExposureNanos: Long = 1_000_000_000L // 1s default upper bound
    ): Long {
        val seconds = calculateExposureSeconds(fps, shutterAngle)
        val rawNanos = (seconds * 1_000_000_000.0).toLong()
        return rawNanos.coerceIn(minExposureNanos, maxExposureNanos)
    }

    /**
     * Calculates equivalent shutter angle from an explicit exposure time in seconds.
     */
    fun calculateEquivalentShutterAngle(fps: Int, exposureSeconds: Double): Float {
        require(fps > 0) { "FPS must be greater than 0" }
        require(exposureSeconds > 0.0) { "Exposure seconds must be greater than 0" }
        val rawAngle = (exposureSeconds * fps.toDouble() * 360.0).toFloat()
        return rawAngle.coerceIn(1.0f, 360.0f)
    }
}
