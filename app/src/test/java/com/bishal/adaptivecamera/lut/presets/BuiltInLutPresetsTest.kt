package com.bishal.adaptivecamera.lut.presets

import com.bishal.adaptivecamera.lut.model.LutType
import com.bishal.adaptivecamera.lut.model.LutValidationResult
import com.bishal.adaptivecamera.lut.parser.CubeLutParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltInLutPresetsTest {

    @Test
    fun getAllPresets_returnsAllFiveExpectedPresets() {
        val presets = BuiltInLutPresets.getAllPresets()
        assertEquals(5, presets.size)

        val ids = presets.map { it.id }.toSet()
        assertTrue(ids.contains(BuiltInLutPresets.PRESET_NATURAL))
        assertTrue(ids.contains(BuiltInLutPresets.PRESET_WARM))
        assertTrue(ids.contains(BuiltInLutPresets.PRESET_COOL))
        assertTrue(ids.contains(BuiltInLutPresets.PRESET_CONTRAST))
        assertTrue(ids.contains(BuiltInLutPresets.PRESET_VINTAGE))
    }

    @Test
    fun presets_haveStableMetadataAndNonEmptyDescriptions() {
        val presets = BuiltInLutPresets.getAllPresets()
        for (preset in presets) {
            assertTrue("ID must start with builtin_", preset.id.startsWith("builtin_"))
            assertTrue("Name must not be empty", preset.name.isNotBlank())
            assertTrue("Description must not be empty", preset.description.isNotBlank())
            assertEquals(17, preset.size3D)
            assertTrue("Preview gradient must have at least 2 stops", preset.previewColors.size >= 2)
        }
    }

    @Test
    fun createPresetDefinition_generatesCorrectLatticeAndBounds() {
        val preset = BuiltInLutPresets.createPresetDefinition(BuiltInLutPresets.PRESET_WARM, size = 17)
        assertEquals(LutType.LUT_3D, preset.metadata.type)
        assertEquals(17, preset.metadata.size)
        assertEquals(4913, preset.metadata.totalEntries) // 17^3
        assertEquals(4913 * 3, preset.tableData.size)

        // Ensure all table entries are valid numbers in [0.0, 1.0]
        for (i in preset.tableData.indices) {
            val v = preset.tableData[i]
            assertTrue("Value must not be NaN or Infinite", !v.isNaN() && !v.isInfinite())
            assertTrue("Value must be clamped in [0, 1]", v in 0.0f..1.0f)
        }
    }

    @Test
    fun roundTripSerialization_parsesCleanlyBackThroughCubeLutParser() {
        val original = BuiltInLutPresets.createPresetDefinition(BuiltInLutPresets.PRESET_VINTAGE, size = 17)
        val cubeText = BuiltInLutPresets.serializeToCubeText(original)

        assertTrue(cubeText.contains("TITLE \"Xamera Vintage\""))
        assertTrue(cubeText.contains("LUT_3D_SIZE 17"))

        val parseResult = CubeLutParser.parse(cubeText)
        assertTrue("Serialized cube must parse cleanly", parseResult is LutValidationResult.Success)

        val parsed = (parseResult as LutValidationResult.Success).definition
        assertEquals(original.metadata.size, parsed.metadata.size)
        assertEquals(original.metadata.totalEntries, parsed.metadata.totalEntries)
        assertEquals(original.tableData.size, parsed.tableData.size)

        // Verify values match within precision tolerance
        for (i in 0 until 100) {
            assertEquals(original.tableData[i], parsed.tableData[i], 0.0001f)
        }
    }

    @Test
    fun mathematicalCharacteristics_warmPresetLiftsRedGoldChannel() {
        val warm = BuiltInLutPresets.createPresetDefinition(BuiltInLutPresets.PRESET_WARM, size = 17)
        val natural = BuiltInLutPresets.createPresetDefinition(BuiltInLutPresets.PRESET_NATURAL, size = 17)

        // Evaluate at mid-gray (0.5, 0.5, 0.5)
        val warmMid = warm.evaluate(0.5f, 0.5f, 0.5f)
        val naturalMid = natural.evaluate(0.5f, 0.5f, 0.5f)

        // Warm preset should have higher red and lower blue than neutral
        assertTrue("Warm red should be higher than neutral", warmMid.r >= naturalMid.r)
        assertTrue("Warm blue should be lower than neutral", warmMid.b <= naturalMid.b)
    }

    @Test
    fun mathematicalCharacteristics_coolPresetLiftsCyanBlueChannel() {
        val cool = BuiltInLutPresets.createPresetDefinition(BuiltInLutPresets.PRESET_COOL, size = 17)
        val natural = BuiltInLutPresets.createPresetDefinition(BuiltInLutPresets.PRESET_NATURAL, size = 17)

        val coolMid = cool.evaluate(0.5f, 0.5f, 0.5f)
        val naturalMid = natural.evaluate(0.5f, 0.5f, 0.5f)

        // Cool preset should have higher blue and lower red than neutral
        assertTrue("Cool blue should be higher than neutral", coolMid.b >= naturalMid.b)
        assertTrue("Cool red should be lower than neutral", coolMid.r <= naturalMid.r)
    }

    @Test
    fun mathematicalCharacteristics_contrastPresetEnhancesSCurve() {
        val contrast = BuiltInLutPresets.createPresetDefinition(BuiltInLutPresets.PRESET_CONTRAST, size = 17)

        // In an S-curve, shadows (< 0.5) are darkened and highlights (> 0.5) are brightened
        val shadow = contrast.evaluate(0.2f, 0.2f, 0.2f)
        val highlight = contrast.evaluate(0.8f, 0.8f, 0.8f)

        assertTrue("Shadows should be pulled deeper", shadow.r < 0.2f)
        assertTrue("Highlights should be pushed brighter", highlight.r > 0.8f)
    }
}
