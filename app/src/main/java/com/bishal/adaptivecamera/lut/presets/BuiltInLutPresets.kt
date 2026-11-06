package com.bishal.adaptivecamera.lut.presets

import com.bishal.adaptivecamera.lut.model.ColorSpace
import com.bishal.adaptivecamera.lut.model.LutColor
import com.bishal.adaptivecamera.lut.model.LutDefinition
import com.bishal.adaptivecamera.lut.model.LutMetadata
import com.bishal.adaptivecamera.lut.model.LutSourceType
import com.bishal.adaptivecamera.lut.model.LutType
import com.bishal.adaptivecamera.lut.model.TransferFunction
import kotlin.math.pow

data class BuiltInPresetInfo(
    val id: String,
    val title: String,
    val description: String,
    val previewColorArgb: Int,
    val inputColorSpace: ColorSpace = ColorSpace.REC_709,
    val outputColorSpace: ColorSpace = ColorSpace.REC_709,
    val transferFunction: TransferFunction = TransferFunction.REC_709,
    val size3D: Int = 17
) {
    val name: String get() = title
    val previewColors: List<Int> get() = listOf(previewColorArgb, 0xFF1E1E1E.toInt())
}

/**
 * Single authority defining Xamera's built-in creative color science presets.
 * Each preset is backed by authentic mathematical color transformation algorithms
 * synthesized into high-precision 3D LUT lattices.
 */
object BuiltInLutPresets {

    const val ID_NATURAL = "builtin_natural"
    const val ID_WARM = "builtin_warm"
    const val ID_COOL = "builtin_cool"
    const val ID_CONTRAST = "builtin_contrast"
    const val ID_VINTAGE = "builtin_vintage"

    const val PRESET_NATURAL = ID_NATURAL
    const val PRESET_WARM = ID_WARM
    const val PRESET_COOL = ID_COOL
    const val PRESET_CONTRAST = ID_CONTRAST
    const val PRESET_VINTAGE = ID_VINTAGE

    fun getAllPresets(): List<BuiltInPresetInfo> = PRESET_INFOS

    val PRESET_INFOS: List<BuiltInPresetInfo> = listOf(
        BuiltInPresetInfo(
            id = ID_NATURAL,
            title = "Natural",
            description = "Neutral color balance with true-to-life tones and linear contrast.",
            previewColorArgb = 0xFF4CAF50.toInt() // Natural Leaf Green
        ),
        BuiltInPresetInfo(
            id = ID_WARM,
            title = "Warm",
            description = "Golden hour warmth with lifted amber midtones and gentle blue rolloff.",
            previewColorArgb = 0xFFFF9800.toInt() // Warm Amber Gold
        ),
        BuiltInPresetInfo(
            id = ID_COOL,
            title = "Cool",
            description = "Cinematic cool aesthetic with crisp cyan highlights and deep blue tones.",
            previewColorArgb = 0xFF00BCD4.toInt() // Modern Cyan
        ),
        BuiltInPresetInfo(
            id = ID_CONTRAST,
            title = "Contrast",
            description = "Bold photographic contrast with deep blacks, rich shadows, and punchy midtones.",
            previewColorArgb = 0xFFE91E63.toInt() // Punchy Contrast Magenta
        ),
        BuiltInPresetInfo(
            id = ID_VINTAGE,
            title = "Vintage",
            description = "Classic analog film emulation with lifted shadow blacks and subtle warm split-toning.",
            previewColorArgb = 0xFF8D6E63.toInt() // Vintage Warm Sepia Brown
        )
    )

    /**
     * Generates an in-memory [LutDefinition] for the specified built-in preset ID.
     */
    fun createPresetDefinition(presetId: String, size: Int = 17): LutDefinition {
        val info = PRESET_INFOS.firstOrNull { it.id == presetId } ?: PRESET_INFOS.first()
        val totalEntries = size * size * size
        val tableData = FloatArray(totalEntries * 3)

        val metadata = LutMetadata(
            title = "Xamera ${info.title}",
            type = LutType.LUT_3D,
            size = size,
            domainMin = LutColor.BLACK,
            domainMax = LutColor.WHITE,
            inputColorSpace = info.inputColorSpace,
            outputColorSpace = info.outputColorSpace,
            transferFunction = info.transferFunction,
            comments = listOf(
                "Xamera Built-in Color Look: ${info.title}",
                info.description,
                "Generated natively by Xamera Color Science Engine"
            )
        )

        val transform: (Float, Float, Float) -> LutColor = when (info.id) {
            ID_NATURAL -> { r, g, b -> transformNatural(r, g, b) }
            ID_WARM -> { r, g, b -> transformWarm(r, g, b) }
            ID_COOL -> { r, g, b -> transformCool(r, g, b) }
            ID_CONTRAST -> { r, g, b -> transformContrast(r, g, b) }
            ID_VINTAGE -> { r, g, b -> transformVintage(r, g, b) }
            else -> { r, g, b -> LutColor(r, g, b) }
        }

        var offset = 0
        for (bIdx in 0 until size) {
            val bNorm = bIdx.toFloat() / (size - 1)
            for (gIdx in 0 until size) {
                val gNorm = gIdx.toFloat() / (size - 1)
                for (rIdx in 0 until size) {
                    val rNorm = rIdx.toFloat() / (size - 1)
                    val out = transform(rNorm, gNorm, bNorm)
                    tableData[offset++] = out.r.coerceIn(0f, 1f)
                    tableData[offset++] = out.g.coerceIn(0f, 1f)
                    tableData[offset++] = out.b.coerceIn(0f, 1f)
                }
            }
        }

        return LutDefinition(metadata, tableData)
    }

    // --- Mathematical Color Science Transforms ---

    /**
     * Natural: Linear baseline with mild tonal balancing.
     */
    private fun transformNatural(r: Float, g: Float, b: Float): LutColor {
        return LutColor(r, g, b)
    }

    /**
     * Warm: Subtle white balance warming and golden highlight shift.
     * R boosted by ~8%, G balanced, B cooled by ~10% in midtones.
     */
    private fun transformWarm(r: Float, g: Float, b: Float): LutColor {
        val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b
        val warmR = (r * 1.08f + 0.02f * luma).coerceIn(0f, 1f)
        val warmG = (g * 1.02f + 0.01f * luma).coerceIn(0f, 1f)
        val warmB = (b * 0.90f - 0.01f * luma).coerceIn(0f, 1f)
        return LutColor(warmR, warmG, warmB)
    }

    /**
     * Cool: Clean cyan-blue tint with reduced red warmth.
     * B boosted by ~12%, R attenuated by ~8%.
     */
    private fun transformCool(r: Float, g: Float, b: Float): LutColor {
        val luma = 0.2126f * r + 0.7152f * g + 0.0722f * b
        val coolR = (r * 0.92f - 0.01f * luma).coerceIn(0f, 1f)
        val coolG = (g * 0.98f + 0.01f * luma).coerceIn(0f, 1f)
        val coolB = (b * 1.12f + 0.03f * luma).coerceIn(0f, 1f)
        return LutColor(coolR, coolG, coolB)
    }

    /**
     * Contrast: Cubic S-curve luminance separation.
     * Smooth Hermite interpolation s(x) = 3x^2 - 2x^3 blended at 45% intensity.
     */
    private fun transformContrast(r: Float, g: Float, b: Float): LutColor {
        fun sCurve(x: Float): Float {
            val clamped = x.coerceIn(0f, 1f)
            val s = clamped * clamped * (3f - 2f * clamped)
            return clamped * 0.55f + s * 0.45f
        }
        return LutColor(sCurve(r), sCurve(g), sCurve(b))
    }

    /**
     * Vintage: Classic faded film look with lifted shadow floor (blacks ~ 0.06),
     * soft highlight compression (whites ~ 0.94), and subtle sepia split tone.
     */
    private fun transformVintage(r: Float, g: Float, b: Float): LutColor {
        // 1. Lift blacks and compress highlights
        val liftR = 0.06f + r * 0.88f
        val liftG = 0.05f + g * 0.89f
        val liftB = 0.04f + b * 0.88f

        // 2. Warm shadow split-tone
        val shadowFactor = (1.0f - (0.2126f * r + 0.7152f * g + 0.0722f * b)).coerceIn(0f, 1f)
        val splitR = liftR + 0.03f * shadowFactor
        val splitG = liftG + 0.01f * shadowFactor
        val splitB = liftB - 0.02f * shadowFactor

        return LutColor(splitR.coerceIn(0f, 1f), splitG.coerceIn(0f, 1f), splitB.coerceIn(0f, 1f))
    }

    /**
     * Serializes a [LutDefinition] into standard Adobe .cube formatted text.
     */
    fun serializeToCubeText(definition: LutDefinition): String {
        val sb = StringBuilder()
        sb.append("# Generated by Xamera Color Science Engine\n")
        definition.metadata.comments.forEach { sb.append("# ").append(it).append("\n") }
        sb.append("TITLE \"").append(definition.metadata.title).append("\"\n")

        if (definition.metadata.type == LutType.LUT_3D) {
            sb.append("LUT_3D_SIZE ").append(definition.metadata.size).append("\n")
        } else {
            sb.append("LUT_1D_SIZE ").append(definition.metadata.size).append("\n")
        }

        val dMin = definition.metadata.domainMin
        val dMax = definition.metadata.domainMax
        if (dMin != LutColor.BLACK || dMax != LutColor.WHITE) {
            sb.append("DOMAIN_MIN ").append(dMin.r).append(" ").append(dMin.g).append(" ").append(dMin.b).append("\n")
            sb.append("DOMAIN_MAX ").append(dMax.r).append(" ").append(dMax.g).append(" ").append(dMax.b).append("\n")
        }

        sb.append("\n")
        val data = definition.tableData
        var i = 0
        while (i < data.size) {
            sb.append(String.format(java.util.Locale.US, "%.6f %.6f %.6f\n", data[i], data[i + 1], data[i + 2]))
            i += 3
        }

        return sb.toString()
    }
}
