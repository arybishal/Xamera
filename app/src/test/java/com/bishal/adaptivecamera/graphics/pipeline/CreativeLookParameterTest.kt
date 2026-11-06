package com.bishal.adaptivecamera.graphics.pipeline

import com.bishal.adaptivecamera.lut.presets.BuiltInLutPresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreativeLookParameterTest {

    @Test
    fun testAllFiveBuiltInLooksPresent() {
        val presets = BuiltInLutPresets.getAllPresets()
        assertEquals(5, presets.size)

        val ids = presets.map { it.id }.toSet()
        assertTrue(ids.contains(BuiltInLutPresets.ID_NATURAL))
        assertTrue(ids.contains(BuiltInLutPresets.ID_WARM))
        assertTrue(ids.contains(BuiltInLutPresets.ID_COOL))
        assertTrue(ids.contains(BuiltInLutPresets.ID_CONTRAST))
        assertTrue(ids.contains(BuiltInLutPresets.ID_VINTAGE))
    }

    @Test
    fun testPresetDefinitionLatticeDimension() {
        for (info in BuiltInLutPresets.getAllPresets()) {
            val def = BuiltInLutPresets.createPresetDefinition(info.id, size = 17)
            assertNotNull(def)
            assertEquals(17, def.metadata.size)
            assertEquals(17 * 17 * 17 * 3, def.tableData.size)
            assertTrue(def.metadata.isValidDomain)
        }
    }

    @Test
    fun testGpuColorConfigClamping() {
        val overclamped = GpuColorConfig(
            lutIntensity = 1.5f,
            grainIntensity = -0.5f,
            grainSize = 10.0f,
            halationIntensity = 2.0f,
            halationThreshold = 1.2f,
            halationRadius = 0.1f
        ).clamped()

        assertEquals(1.0f, overclamped.lutIntensity, 0.001f)
        assertEquals(0.0f, overclamped.grainIntensity, 0.001f)
        assertEquals(5.0f, overclamped.grainSize, 0.001f)
        assertEquals(1.0f, overclamped.halationIntensity, 0.001f)
        assertEquals(0.99f, overclamped.halationThreshold, 0.001f)
        assertEquals(1.0f, overclamped.halationRadius, 0.001f)
    }

    @Test
    fun testNeutralResetBehavior() {
        val default = GpuColorConfig.DEFAULT
        assertFalse(default.lutEnabled)
        assertFalse(default.grainEnabled)
        assertFalse(default.halationEnabled)
        assertEquals(0.0f, default.grainIntensity, 0.001f)
        assertEquals(0.0f, default.halationIntensity, 0.001f)
    }
}
