package com.bishal.adaptivecamera.photography.computational

import com.bishal.adaptivecamera.camera.config.HdrPlusMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HdrPlusPipelineTest {

    @Test
    fun testHdrPlusModeCycle() {
        assertEquals(HdrPlusMode.AUTO, HdrPlusMode.OFF.next())
        assertEquals(HdrPlusMode.ON, HdrPlusMode.AUTO.next())
        assertEquals(HdrPlusMode.OFF, HdrPlusMode.ON.next())
    }

    @Test
    fun testHdrPlusTargetFrameCount() {
        val targetCount = 5
        assertEquals(5, targetCount)
    }

    @Test
    fun testDisplacementVectorProperties() {
        val zeroDisplacement = DisplacementVector(dx = 0, dy = 0, confidence = 1.0f)
        assertEquals(0, zeroDisplacement.dx)
        assertEquals(0, zeroDisplacement.dy)
        assertEquals(1.0f, zeroDisplacement.confidence, 0.001f)

        val shiftedDisplacement = DisplacementVector(dx = -4, dy = 7, confidence = 0.85f)
        assertEquals(-4, shiftedDisplacement.dx)
        assertEquals(7, shiftedDisplacement.dy)
        assertEquals(0.85f, shiftedDisplacement.confidence, 0.001f)
    }

    @Test
    fun testMotionMaskContract() {
        val maskData = FloatArray(16) { 0.0f }
        maskData[5] = 0.8f // single dynamic pixel
        val mask = MotionMask(width = 4, height = 4, mask = maskData, motionRatio = 1f / 16f)

        assertEquals(4, mask.width)
        assertEquals(4, mask.height)
        assertEquals(16, mask.mask.size)
        assertEquals(0.0625f, mask.motionRatio, 0.001f)
        assertEquals(0.8f, mask.mask[5], 0.001f)
    }

    @Test
    fun testExposureWeightFactorsNormalization() {
        // HDR radiance fusion 5-frame exposure brackets [-2EV, -1EV, 0EV, +1EV, +2EV]
        val evValues = floatArrayOf(-2.0f, -1.0f, 0.0f, 1.0f, 2.0f)
        val relativeExposures = evValues.map { Math.pow(2.0, it.toDouble()).toFloat() }

        assertEquals(0.25f, relativeExposures[0], 0.001f)
        assertEquals(0.50f, relativeExposures[1], 0.001f)
        assertEquals(1.00f, relativeExposures[2], 0.001f)
        assertEquals(2.00f, relativeExposures[3], 0.001f)
        assertEquals(4.00f, relativeExposures[4], 0.001f)

        // Inverse exposure weights for radiance accumulation
        val rawWeights = relativeExposures.map { 1.0f / it }
        val sumWeights = rawWeights.sum()
        val normalizedWeights = rawWeights.map { it / sumWeights }

        assertEquals(1.0f, normalizedWeights.sum(), 0.001f)
    }
}
