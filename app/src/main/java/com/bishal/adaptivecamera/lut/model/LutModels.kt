package com.bishal.adaptivecamera.lut.model

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Dimensionality of the Look-Up Table.
 */
enum class LutType(val dimensions: Int, val label: String) {
    LUT_1D(1, "1D Curve"),
    LUT_3D(3, "3D Lattice")
}

/**
 * Provenance of the LUT file in Xamera.
 */
enum class LutSourceType(val label: String) {
    BUILT_IN("Built-in Preset"),
    IMPORTED("User Imported")
}

/**
 * Standard color spaces supported for color science transform declarations.
 */
enum class ColorSpace(val id: String, val displayName: String, val description: String) {
    REC_709("REC_709", "Rec.709", "Standard HD broadcast color gamut (BT.709)"),
    SRGB("SRGB", "sRGB", "Standard display & web photographic color space"),
    DCI_P3("DCI_P3", "Display P3", "Wide color gamut standard for digital cinema & modern OLEDs"),
    REC_2020("REC_2020", "Rec.2020", "Ultra-wide gamut for UHD HDR displays (BT.2020)"),
    UNKNOWN("UNKNOWN", "Unspecified", "Custom or generic color space");

    companion object {
        fun fromId(id: String?): ColorSpace = values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: REC_709
    }
}

/**
 * Electro-Optical Transfer Functions (EOTF) and camera logarithmic profiles.
 */
enum class TransferFunction(
    val id: String,
    val displayName: String,
    val isLog: Boolean,
    val isHdr: Boolean,
    val gammaApproximation: Float?
) {
    LINEAR("LINEAR", "Linear", false, false, 1.0f),
    REC_709("REC_709", "Rec.709 (Gamma 2.4)", false, false, 2.4f),
    SRGB("SRGB", "sRGB (Gamma 2.2)", false, false, 2.2f),
    HLG("HLG", "Hybrid Log-Gamma (HLG)", false, true, null),
    PQ_ST2084("PQ_ST2084", "SMPTE ST 2084 (PQ)", false, true, null),
    S_LOG3("S_LOG3", "Sony S-Log3", true, false, null),
    LOG_C("LOG_C", "ARRI Log-C v3", true, false, null),
    UNKNOWN("UNKNOWN", "Standard Gamma", false, false, 2.2f);

    companion object {
        fun fromId(id: String?): TransferFunction = values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: REC_709
    }
}

/**
 * Mathematical interpolation strategy for 3D LUT sampling.
 */
enum class LutInterpolation(val displayName: String) {
    NEAREST("Nearest Neighbor"),
    TRILINEAR("Trilinear Interpolation"),
    TETRAHEDRAL("Tetrahedral Interpolation")
}

/**
 * Normalized 32-bit floating point RGB color vector [0.0f .. 1.0f].
 */
data class LutColor(
    val r: Float,
    val g: Float,
    val b: Float
) {
    fun clamped(): LutColor = LutColor(
        r = r.coerceIn(0.0f, 1.0f),
        g = g.coerceIn(0.0f, 1.0f),
        b = b.coerceIn(0.0f, 1.0f)
    )

    fun toArgbInt(): Int {
        val ir = (r.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
        val ig = (g.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
        val ib = (b.coerceIn(0f, 1f) * 255f + 0.5f).toInt()
        return (0xFF shl 24) or (ir shl 16) or (ig shl 8) or ib
    }

    companion object {
        val BLACK = LutColor(0f, 0f, 0f)
        val WHITE = LutColor(1f, 1f, 1f)

        fun fromArgbInt(argb: Int): LutColor {
            val r = ((argb shr 16) and 0xFF) / 255.0f
            val g = ((argb shr 8) and 0xFF) / 255.0f
            val b = (argb and 0xFF) / 255.0f
            return LutColor(r, g, b)
        }
    }
}

/**
 * Comprehensive metadata describing a 1D or 3D LUT file.
 */
data class LutMetadata(
    val title: String,
    val type: LutType,
    val size: Int, // Size N (e.g. 17, 33, 64)
    val domainMin: LutColor = LutColor(0.0f, 0.0f, 0.0f),
    val domainMax: LutColor = LutColor(1.0f, 1.0f, 1.0f),
    val inputColorSpace: ColorSpace = ColorSpace.REC_709,
    val outputColorSpace: ColorSpace = ColorSpace.REC_709,
    val transferFunction: TransferFunction = TransferFunction.REC_709,
    val comments: List<String> = emptyList()
) {
    val totalEntries: Int
        get() = if (type == LutType.LUT_1D) size else size * size * size

    val isValidDomain: Boolean
        get() = domainMax.r > domainMin.r && domainMax.g > domainMin.g && domainMax.b > domainMin.b
}

/**
 * Result of .cube LUT file validation and parsing.
 */
sealed class LutValidationResult {
    data class Success(val definition: LutDefinition) : LutValidationResult()
    data class Error(val message: String, val line: Int? = null) : LutValidationResult()
}

/**
 * In-memory representation of a validated Look-Up Table with CPU color evaluation.
 * Data layout in [tableData]: Flat FloatArray of length (totalEntries * 3),
 * ordered with Red changing fastest, then Green, then Blue (standard Adobe .cube format).
 */
class LutDefinition(
    val metadata: LutMetadata,
    val tableData: FloatArray
) {
    init {
        val expectedFloats = metadata.totalEntries * 3
        require(tableData.size == expectedFloats) {
            "LUT tableData size (${tableData.size}) does not match expected entry count ($expectedFloats)"
        }
    }

    /**
     * Samples the LUT at normalized input [color] using Trilinear Interpolation on 3D LUTs
     * or Linear Interpolation on 1D LUTs.
     */
    fun evaluate(color: LutColor): LutColor {
        // 1. Normalize input color against [domainMin, domainMax]
        val dMin = metadata.domainMin
        val dMax = metadata.domainMax

        val normR = if (dMax.r > dMin.r) ((color.r - dMin.r) / (dMax.r - dMin.r)).coerceIn(0f, 1f) else 0f
        val normG = if (dMax.g > dMin.g) ((color.g - dMin.g) / (dMax.g - dMin.g)).coerceIn(0f, 1f) else 0f
        val normB = if (dMax.b > dMin.b) ((color.b - dMin.b) / (dMax.b - dMin.b)).coerceIn(0f, 1f) else 0f

        return if (metadata.type == LutType.LUT_3D) {
            evaluate3dTrilinear(normR, normG, normB)
        } else {
            evaluate1dLinear(normR, normG, normB)
        }
    }

    /**
     * Convenience evaluator for raw (r, g, b) floating point components.
     */
    fun evaluate(r: Float, g: Float, b: Float): LutColor = evaluate(LutColor(r, g, b))

    private fun evaluate1dLinear(u: Float, v: Float, w: Float): LutColor {
        val n = metadata.size
        if (n <= 1) return LutColor(tableData[0], tableData[1], tableData[2])

        fun sampleChannel(normVal: Float, channelOffset: Int): Float {
            val coord = normVal * (n - 1)
            val idx0 = floor(coord).toInt().coerceIn(0, n - 1)
            val idx1 = min(idx0 + 1, n - 1)
            val frac = coord - idx0
            val val0 = tableData[idx0 * 3 + channelOffset]
            val val1 = tableData[idx1 * 3 + channelOffset]
            return val0 + (val1 - val0) * frac
        }

        return LutColor(
            r = sampleChannel(u, 0),
            g = sampleChannel(v, 1),
            b = sampleChannel(w, 2)
        )
    }

    private fun evaluate3dTrilinear(u: Float, v: Float, w: Float): LutColor {
        val n = metadata.size
        if (n <= 1) return LutColor(tableData[0], tableData[1], tableData[2])

        // Scale to grid coordinates [0 .. N-1]
        val rx = u * (n - 1)
        val gy = v * (n - 1)
        val bz = w * (n - 1)

        val r0 = floor(rx).toInt().coerceIn(0, n - 1)
        val r1 = min(r0 + 1, n - 1)
        val g0 = floor(gy).toInt().coerceIn(0, n - 1)
        val g1 = min(g0 + 1, n - 1)
        val b0 = floor(bz).toInt().coerceIn(0, n - 1)
        val b1 = min(b0 + 1, n - 1)

        val dr = rx - r0
        val dg = gy - g0
        val db = bz - b0

        // In .cube format: index = (b * N * N + g * N + r) * 3
        fun getEntry(rIdx: Int, gIdx: Int, bIdx: Int, ch: Int): Float {
            val entryIndex = (bIdx * n * n + gIdx * n + rIdx) * 3 + ch
            return tableData[entryIndex]
        }

        // Interpolate each channel (0=R, 1=G, 2=B)
        fun trilinearChannel(ch: Int): Float {
            val c000 = getEntry(r0, g0, b0, ch)
            val c100 = getEntry(r1, g0, b0, ch)
            val c010 = getEntry(r0, g1, b0, ch)
            val c110 = getEntry(r1, g1, b0, ch)
            val c001 = getEntry(r0, g0, b1, ch)
            val c101 = getEntry(r1, g0, b1, ch)
            val c011 = getEntry(r0, g1, b1, ch)
            val c111 = getEntry(r1, g1, b1, ch)

            val c00 = c000 * (1f - dr) + c100 * dr
            val c10 = c010 * (1f - dr) + c110 * dr
            val c01 = c001 * (1f - dr) + c101 * dr
            val c11 = c011 * (1f - dr) + c111 * dr

            val c0 = c00 * (1f - dg) + c10 * dg
            val c1 = c01 * (1f - dg) + c11 * dg

            return c0 * (1f - db) + c1 * db
        }

        return LutColor(
            r = trilinearChannel(0),
            g = trilinearChannel(1),
            b = trilinearChannel(2)
        )
    }
}
