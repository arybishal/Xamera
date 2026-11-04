package com.bishal.adaptivecamera.intelligence.analysis

/**
 * Metric summary computed from an analyzed viewfinder frame.
 */
data class FrameMetrics(
    val meanLuminance: Float,           // 0.0 (pitch black) to 1.0 (pure white)
    val rmsContrast: Float,             // Standard deviation of luminance (0.0 to 0.5)
    val shadowRatio: Float,             // Proportion of pixels with luminance < 0.15
    val highlightRatio: Float,          // Proportion of pixels with luminance > 0.85
    val midtoneRatio: Float,            // Proportion of pixels in 0.15..0.85 range
    val estimatedDynamicRangeStops: Float, // Estimated dynamic range based on histogram span
    val motionDelta: Float,             // Frame-to-frame temporal difference (0.0 to 1.0)
    val histogram: IntArray = IntArray(16), // 16-bin normalized luminance histogram
    val timestampMs: Long = System.currentTimeMillis()
) {
    val isLowLight: Boolean
        get() = meanLuminance < 0.18f || (shadowRatio > 0.65f && meanLuminance < 0.30f)

    val isBrightOrHighKey: Boolean
        get() = meanLuminance > 0.70f && highlightRatio > 0.35f

    val isHighContrastOrBacklit: Boolean
        get() = shadowRatio > 0.25f && highlightRatio > 0.20f && rmsContrast > 0.28f

    val isHighMotion: Boolean
        get() = motionDelta > 0.18f

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FrameMetrics
        if (meanLuminance != other.meanLuminance) return false
        if (rmsContrast != other.rmsContrast) return false
        if (motionDelta != other.motionDelta) return false
        if (!histogram.contentEquals(other.histogram)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = meanLuminance.hashCode()
        result = 31 * result + rmsContrast.hashCode()
        result = 31 * result + motionDelta.hashCode()
        result = 31 * result + histogram.contentHashCode()
        return result
    }
}
