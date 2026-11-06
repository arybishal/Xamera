package com.bishal.adaptivecamera.photography.portrait

/**
 * Photographic aperture values for portrait bokeh simulation.
 * Note: Modern mobile sensors feature fixed physical irises; these values
 * control synthetic optical depth-of-field simulation.
 */
enum class PortraitAperture(
    val label: String,
    val fNumber: Float,
    val blurRadius: Int
) {
    F1_4("f/1.4", 1.4f, 26),
    F2_0("f/2.0", 2.0f, 20),
    F2_8("f/2.8", 2.8f, 14),
    F4_0("f/4.0", 4.0f, 8),
    F5_6("f/5.6", 5.6f, 4);

    fun next(): PortraitAperture = when (this) {
        F1_4 -> F2_0
        F2_0 -> F2_8
        F2_8 -> F4_0
        F4_0 -> F5_6
        F5_6 -> F1_4
    }
}

/**
 * Portrait lighting style presets.
 */
enum class PortraitLightingStyle(
    val label: String,
    val description: String
) {
    NATURAL(
        "Natural",
        "True-to-life skin tones and natural ambient balance"
    ),
    STUDIO(
        "Studio",
        "Facial midtone fill lighting with gentle background dimming"
    ),
    SOFT(
        "Soft",
        "Delicate highlight bloom and flattering warm skin diffusion"
    ),
    DRAMATIC(
        "Dramatic",
        "High-contrast key lighting with deep shadow falloff and vignette"
    );

    fun next(): PortraitLightingStyle = when (this) {
        NATURAL -> STUDIO
        STUDIO -> SOFT
        SOFT -> DRAMATIC
        DRAMATIC -> NATURAL
    }
}

/**
 * Configuration for the Dedicated Portrait Mode.
 */
data class PortraitConfig(
    val aperture: PortraitAperture = PortraitAperture.F2_8,
    val lightingStyle: PortraitLightingStyle = PortraitLightingStyle.NATURAL,
    val depthFalloff: Float = 0.5f,
    val boostHighlights: Boolean = true
)

/**
 * Subject segmentation result containing the subject alpha mask and confidence.
 */
data class SubjectSegmentationResult(
    val width: Int,
    val height: Int,
    val mask: FloatArray,
    val confidence: Float,
    val hasFaceDetected: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SubjectSegmentationResult
        if (width != other.width) return false
        if (height != other.height) return false
        if (confidence != other.confidence) return false
        if (hasFaceDetected != other.hasFaceDetected) return false
        return mask.contentEquals(other.mask)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + confidence.hashCode()
        result = 31 * result + hasFaceDetected.hashCode()
        result = 31 * result + mask.contentHashCode()
        return result
    }
}
