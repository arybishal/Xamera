package com.bishal.adaptivecamera.lut.model

import com.bishal.adaptivecamera.lut.presets.BuiltInLutPresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorScienceInterpolationTest {

    @Test
    fun lutColor_toAndFromRgb_maintainsPrecision() {
        val original = LutColor(0.2f, 0.6f, 0.8f)
        val argb = original.toArgbInt()
        val reconstructed = LutColor.fromArgbInt(argb)

        // 8-bit quantization is ~1/255 = 0.00392
        assertEquals(original.r, reconstructed.r, 0.01f)
        assertEquals(original.g, reconstructed.g, 0.01f)
        assertEquals(original.b, reconstructed.b, 0.01f)
    }

    @Test
    fun lutColor_clamping_preventsOutOfGamutValues() {
        val outOfBounds = LutColor(-0.5f, 1.8f, 0.5f)
        val clamped = outOfBounds.clamped()

        assertEquals(0.0f, clamped.r, 0.0001f)
        assertEquals(1.0f, clamped.g, 0.0001f)
        assertEquals(0.5f, clamped.b, 0.0001f)
    }

    @Test
    fun identity3dLut_trilinearInterpolation_returnsInputExact() {
        // Natural preset is an exact identity 3D LUT (size 17)
        val naturalPreset = BuiltInLutPresets.createPresetDefinition(BuiltInLutPresets.PRESET_NATURAL, size = 17)
        assertEquals(LutType.LUT_3D, naturalPreset.metadata.type)

        // Test at corners
        val black = naturalPreset.evaluate(0.0f, 0.0f, 0.0f)
        assertEquals(0.0f, black.r, 0.001f)
        assertEquals(0.0f, black.g, 0.001f)
        assertEquals(0.0f, black.b, 0.001f)

        val white = naturalPreset.evaluate(1.0f, 1.0f, 1.0f)
        assertEquals(1.0f, white.r, 0.001f)
        assertEquals(1.0f, white.g, 0.001f)
        assertEquals(1.0f, white.b, 0.001f)

        // Test at midpoint (0.5, 0.5, 0.5)
        val mid = naturalPreset.evaluate(0.5f, 0.5f, 0.5f)
        assertEquals(0.5f, mid.r, 0.001f)
        assertEquals(0.5f, mid.g, 0.001f)
        assertEquals(0.5f, mid.b, 0.001f)

        // Test at fractional coordinate not on lattice vertex (e.g. 0.33, 0.67, 0.81)
        val arbitrary = naturalPreset.evaluate(0.33f, 0.67f, 0.81f)
        assertEquals(0.33f, arbitrary.r, 0.005f)
        assertEquals(0.67f, arbitrary.g, 0.005f)
        assertEquals(0.81f, arbitrary.b, 0.005f)
    }

    @Test
    fun identity1dLut_linearInterpolation_returnsInputExact() {
        // Construct a simple 1D identity LUT
        val size = 5
        val table = FloatArray(size * 3)
        for (i in 0 until size) {
            val v = i.toFloat() / (size - 1)
            table[i * 3 + 0] = v
            table[i * 3 + 1] = v
            table[i * 3 + 2] = v
        }

        val metadata = LutMetadata(
            title = "1D Identity",
            type = LutType.LUT_1D,
            size = size
        )
        val definition = LutDefinition(metadata = metadata, tableData = table)

        val sample1 = definition.evaluate(0.25f, 0.5f, 0.75f)
        assertEquals(0.25f, sample1.r, 0.001f)
        assertEquals(0.50f, sample1.g, 0.001f)
        assertEquals(0.75f, sample1.b, 0.001f)
    }

    @Test
    fun customDomain_normalizedCorrectly() {
        // Domain [-1.0, 1.0]
        val metadata = LutMetadata(
            title = "Offset Domain",
            type = LutType.LUT_1D,
            size = 3,
            domainMin = LutColor(-1.0f, -1.0f, -1.0f),
            domainMax = LutColor(1.0f, 1.0f, 1.0f)
        )
        // Table maps -1 -> 0.0, 0 -> 0.5, 1 -> 1.0
        val table = floatArrayOf(
            0.0f, 0.0f, 0.0f,
            0.5f, 0.5f, 0.5f,
            1.0f, 1.0f, 1.0f
        )
        val definition = LutDefinition(metadata = metadata, tableData = table)

        // At 0.0 (which is the midpoint of [-1, 1]), output should be 0.5
        val mid = definition.evaluate(0.0f, 0.0f, 0.0f)
        assertEquals(0.5f, mid.r, 0.001f)
        assertEquals(0.5f, mid.g, 0.001f)
        assertEquals(0.5f, mid.b, 0.001f)
    }

    @Test
    fun outOfBoundsInputs_safelyClampedWithoutException() {
        val naturalPreset = BuiltInLutPresets.createPresetDefinition(BuiltInLutPresets.PRESET_NATURAL, size = 17)

        // Below 0.0
        val underflow = naturalPreset.evaluate(-10.0f, -2.0f, -0.01f)
        assertEquals(0.0f, underflow.r, 0.001f)
        assertEquals(0.0f, underflow.g, 0.001f)
        assertEquals(0.0f, underflow.b, 0.001f)

        // Above 1.0
        val overflow = naturalPreset.evaluate(15.0f, 1.01f, 100.0f)
        assertEquals(1.0f, overflow.r, 0.001f)
        assertEquals(1.0f, overflow.g, 0.001f)
        assertEquals(1.0f, overflow.b, 0.001f)
    }
}
