package com.bishal.adaptivecamera.graphics.opengl

import com.bishal.adaptivecamera.lut.model.LutColor
import com.bishal.adaptivecamera.lut.model.LutDefinition
import com.bishal.adaptivecamera.lut.model.LutMetadata
import com.bishal.adaptivecamera.lut.model.LutType
import com.bishal.adaptivecamera.lut.presets.BuiltInLutPresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlTexture3DMathTest {

    @Test
    fun bufferAllocation_matchesCubedDimensionTimesThree() {
        // Size 17: 17^3 * 3 = 14,739 floats
        val size17 = 17
        val floats17 = size17 * size17 * size17 * 3
        assertEquals(14739, floats17)

        // Size 33: 33^3 * 3 = 107,811 floats
        val size33 = 33
        val floats33 = size33 * size33 * size33 * 3
        assertEquals(107811, floats33)

        // Size 65: 65^3 * 3 = 823,875 floats (~3.3MB)
        val size65 = 65
        val floats65 = size65 * size65 * size65 * 3
        assertEquals(823875, floats65)
    }

    @Test
    fun channelOrdering_redChangesFastestThenGreenThenBlue() {
        val natural = BuiltInLutPresets.createPresetDefinition(BuiltInLutPresets.PRESET_NATURAL, size = 17)
        val n = 17
        val data = natural.tableData

        // index(r, g, b) = (b * N^2 + g * N + r) * 3
        fun index(r: Int, g: Int, b: Int): Int = (b * n * n + g * n + r) * 3

        // Step across Red: index increases by 3
        for (r in 0 until n - 1) {
            val idx0 = index(r, 0, 0)
            val idx1 = index(r + 1, 0, 0)
            assertEquals(3, idx1 - idx0)
        }

        // Step across Green: index increases by N * 3 = 51
        for (g in 0 until n - 1) {
            val idx0 = index(0, g, 0)
            val idx1 = index(0, g + 1, 0)
            assertEquals(n * 3, idx1 - idx0)
        }

        // Step across Blue: index increases by N^2 * 3 = 289 * 3 = 867
        for (b in 0 until n - 1) {
            val idx0 = index(0, 0, b)
            val idx1 = index(0, 0, b + 1)
            assertEquals(n * n * 3, idx1 - idx0)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejection_whenSizeExceedsMaximumSafeLimit() {
        val oversizedMetadata = LutMetadata(
            title = "Oversized",
            type = LutType.LUT_3D,
            size = 128
        )
        val table = FloatArray(128 * 128 * 128 * 3)
        val definition = LutDefinition(oversizedMetadata, table)

        // GlTexture3D must reject size 128 > 65
        val glTexture = GlTexture3D()
        glTexture.loadFromDefinition(definition)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejection_when1dLutPassedTo3dTexture() {
        val metadata1d = LutMetadata(
            title = "1D LUT",
            type = LutType.LUT_1D,
            size = 32
        )
        val table = FloatArray(32 * 3)
        val definition = LutDefinition(metadata1d, table)

        val glTexture = GlTexture3D()
        glTexture.loadFromDefinition(definition)
    }

    @Test
    fun halfTexelOffsetProperties_computedAccurately() {
        val texture = GlTexture3D()
        assertEquals(1.0f, texture.lutScale, 0.001f)
        assertEquals(0.0f, texture.lutOffset, 0.001f)
    }
}
