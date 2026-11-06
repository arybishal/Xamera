package com.bishal.adaptivecamera.graphics.pipeline

import com.bishal.adaptivecamera.lut.model.TransferFunction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GpuColorConfigTest {

    @Test
    fun defaultInvariants_allEffectsDisabled() {
        val config = GpuColorConfig.DEFAULT
        assertFalse("LUT must be disabled by default", config.lutEnabled)
        assertFalse("Film grain must be disabled by default", config.grainEnabled)
        assertFalse("Halation must be disabled by default", config.halationEnabled)
        assertEquals(1.0f, config.lutIntensity, 0.001f)
        assertEquals(0.0f, config.grainIntensity, 0.001f)
        assertEquals(0.0f, config.halationIntensity, 0.001f)
        assertEquals(0.85f, config.halationThreshold, 0.001f)
    }

    @Test
    fun clamping_constrainsOutOfRangeValues() {
        val outOfBounds = GpuColorConfig(
            lutIntensity = -5.0f,
            grainIntensity = 10.0f,
            grainSize = 100.0f,
            halationIntensity = -2.0f,
            halationThreshold = 0.1f,
            halationRadius = 50.0f
        )
        val clamped = outOfBounds.clamped()

        assertEquals(0.0f, clamped.lutIntensity, 0.001f)
        assertEquals(1.0f, clamped.grainIntensity, 0.001f)
        assertEquals(5.0f, clamped.grainSize, 0.001f)
        assertEquals(0.0f, clamped.halationIntensity, 0.001f)
        assertEquals(0.5f, clamped.halationThreshold, 0.001f)
        assertEquals(10.0f, clamped.halationRadius, 0.001f)
    }

    @Test
    fun clamping_preservesValidValues() {
        val valid = GpuColorConfig(
            lutEnabled = true,
            selectedLutId = "builtin_warm",
            lutIntensity = 0.75f,
            grainEnabled = true,
            grainIntensity = 0.35f,
            grainSize = 1.8f,
            halationEnabled = true,
            halationIntensity = 0.25f,
            halationThreshold = 0.88f,
            halationRadius = 5.0f
        )
        val clamped = valid.clamped()

        assertTrue(clamped.lutEnabled)
        assertEquals("builtin_warm", clamped.selectedLutId)
        assertEquals(0.75f, clamped.lutIntensity, 0.001f)
        assertTrue(clamped.grainEnabled)
        assertEquals(0.35f, clamped.grainIntensity, 0.001f)
        assertEquals(1.8f, clamped.grainSize, 0.001f)
        assertTrue(clamped.halationEnabled)
        assertEquals(0.25f, clamped.halationIntensity, 0.001f)
        assertEquals(0.88f, clamped.halationThreshold, 0.001f)
        assertEquals(5.0f, clamped.halationRadius, 0.001f)
    }

    @Test
    fun transferModeIndex_correctForLogTransferFunctions() {
        val slog = GpuColorConfig(transferFunction = TransferFunction.S_LOG3)
        assertEquals(3, slog.transferModeIndex)

        val logC = GpuColorConfig(transferFunction = TransferFunction.LOG_C)
        assertEquals(4, logC.transferModeIndex)

        val hlg = GpuColorConfig(transferFunction = TransferFunction.HLG)
        assertEquals(5, hlg.transferModeIndex)
    }
}
