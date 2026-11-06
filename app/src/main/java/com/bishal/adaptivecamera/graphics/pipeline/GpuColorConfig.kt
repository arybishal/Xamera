package com.bishal.adaptivecamera.graphics.pipeline

import com.bishal.adaptivecamera.lut.model.ColorSpace
import com.bishal.adaptivecamera.lut.model.TransferFunction

/**
 * Authoritative runtime configuration for the GPU color science pipeline.
 * Controls active 3D LUT, intensity, transfer characteristics, film grain, and halation.
 */
data class GpuColorConfig(
    val lutEnabled: Boolean = false,
    val selectedLutId: String? = null,
    val lutIntensity: Float = 1.0f,
    val inputColorSpace: ColorSpace = ColorSpace.REC_709,
    val outputColorSpace: ColorSpace = ColorSpace.REC_709,
    val transferFunction: TransferFunction = TransferFunction.REC_709,
    val grainEnabled: Boolean = false,
    val grainIntensity: Float = 0.0f,
    val grainSize: Float = 1.5f,
    val halationEnabled: Boolean = false,
    val halationIntensity: Float = 0.0f,
    val halationThreshold: Float = 0.85f,
    val halationRadius: Float = 4.0f
) {
    /**
     * Maps [TransferFunction] to integer mode index consumed by GLSL shaders.
     */
    val transferModeIndex: Int
        get() = when (transferFunction) {
            TransferFunction.REC_709, TransferFunction.SRGB -> 1
            TransferFunction.LINEAR -> 2
            TransferFunction.S_LOG3 -> 3
            TransferFunction.LOG_C -> 4
            TransferFunction.HLG -> 5
            TransferFunction.PQ_ST2084 -> 1
            TransferFunction.UNKNOWN -> 1
        }

    fun clamped(): GpuColorConfig = copy(
        lutIntensity = lutIntensity.coerceIn(0.0f, 1.0f),
        grainIntensity = grainIntensity.coerceIn(0.0f, 1.0f),
        grainSize = grainSize.coerceIn(0.5f, 5.0f),
        halationIntensity = halationIntensity.coerceIn(0.0f, 1.0f),
        halationThreshold = halationThreshold.coerceIn(0.5f, 0.99f),
        halationRadius = halationRadius.coerceIn(1.0f, 10.0f)
    )

    companion object {
        val DEFAULT = GpuColorConfig()
    }
}
