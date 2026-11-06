package com.bishal.adaptivecamera.photography.computational

/**
 * Computational photography image stacking mode.
 */
enum class ImageStackingMode(val label: String, val description: String) {
    SMART_AUTO(
        "Smart Auto",
        "Dynamically analyzes scene illumination and contrast to select optimal multi-frame fusion"
    ),
    LOW_LIGHT(
        "Low-Light NR",
        "Multi-frame temporal denoising reducing shadow noise up to 10 dB without motion smear"
    ),
    HDR(
        "HDR Fusion",
        "Exposure radiance blending preserving highlight cloud detail and shadow depth"
    ),
    DETAIL_ENHANCEMENT(
        "Detail Enhancement",
        "Sub-pixel alignment and unsharp edge reinforcement for fine textures"
    );

    fun next(): ImageStackingMode = when (this) {
        SMART_AUTO -> LOW_LIGHT
        LOW_LIGHT -> HDR
        HDR -> DETAIL_ENHANCEMENT
        DETAIL_ENHANCEMENT -> SMART_AUTO
    }
}

/**
 * Number of burst frames captured for multi-frame stacking.
 */
enum class ImageStackingFrameCount(val count: Int, val label: String) {
    AUTO(5, "Auto (5f)"),
    FRAMES_3(3, "3 frames"),
    FRAMES_5(5, "5 frames"),
    FRAMES_7(7, "7 frames");

    fun next(): ImageStackingFrameCount = when (this) {
        AUTO -> FRAMES_3
        FRAMES_3 -> FRAMES_5
        FRAMES_5 -> FRAMES_7
        FRAMES_7 -> AUTO
    }
}

/**
 * Configuration for the Image Stacking & Computational Photography subsystem.
 */
data class ImageStackingConfig(
    val isEnabled: Boolean = false,
    val mode: ImageStackingMode = ImageStackingMode.SMART_AUTO,
    val frameCount: ImageStackingFrameCount = ImageStackingFrameCount.AUTO,
    val retainOriginalFrames: Boolean = false,
    val motionRejectionThreshold: Float = 0.08f,
    val alignmentMaxShiftPx: Int = 32
)

/**
 * 2D displacement offset computed by the frame alignment engine.
 */
data class DisplacementVector(
    val dx: Int,
    val dy: Int,
    val confidence: Float = 1.0f
)

/**
 * Motion estimation mask identifying dynamic regions between frames.
 * Values in [mask] range from 0.0f (completely static) to 1.0f (significant motion).
 */
data class MotionMask(
    val width: Int,
    val height: Int,
    val mask: FloatArray,
    val motionRatio: Float
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as MotionMask
        if (width != other.width) return false
        if (height != other.height) return false
        if (motionRatio != other.motionRatio) return false
        return mask.contentEquals(other.mask)
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + motionRatio.hashCode()
        result = 31 * result + mask.contentHashCode()
        return result
    }
}
