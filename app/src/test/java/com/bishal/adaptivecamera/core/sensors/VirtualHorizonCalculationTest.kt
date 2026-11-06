package com.bishal.adaptivecamera.core.sensors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VirtualHorizonCalculationTest {

    @Test
    fun testNormalizeRollPortrait() {
        // Roll within -45 to 45 should remain unchanged
        assertEquals(0f, VirtualHorizonCalculator.normalizeRoll(0f), 0.001f)
        assertEquals(15f, VirtualHorizonCalculator.normalizeRoll(15f), 0.001f)
        assertEquals(-20f, VirtualHorizonCalculator.normalizeRoll(-20f), 0.001f)
        assertEquals(45f, VirtualHorizonCalculator.normalizeRoll(45f), 0.001f)
        assertEquals(-45f, VirtualHorizonCalculator.normalizeRoll(-45f), 0.001f)
    }

    @Test
    fun testNormalizeRollLandscapeRight() {
        // Roll in (45, 135] normalized by subtracting 90°
        assertEquals(0f, VirtualHorizonCalculator.normalizeRoll(90f), 0.001f)
        assertEquals(5f, VirtualHorizonCalculator.normalizeRoll(95f), 0.001f)
        assertEquals(-10f, VirtualHorizonCalculator.normalizeRoll(80f), 0.001f)
    }

    @Test
    fun testNormalizeRollLandscapeLeft() {
        // Roll in [-135, -45) normalized by adding 90°
        assertEquals(0f, VirtualHorizonCalculator.normalizeRoll(-90f), 0.001f)
        assertEquals(5f, VirtualHorizonCalculator.normalizeRoll(-85f), 0.001f)
        assertEquals(-10f, VirtualHorizonCalculator.normalizeRoll(-100f), 0.001f)
    }

    @Test
    fun testNormalizeRollUpsideDown() {
        // Roll > 135 or < -135
        assertEquals(0f, VirtualHorizonCalculator.normalizeRoll(180f), 0.001f)
        assertEquals(0f, VirtualHorizonCalculator.normalizeRoll(-180f), 0.001f)
        assertEquals(-5f, VirtualHorizonCalculator.normalizeRoll(175f), 0.001f)
        assertEquals(5f, VirtualHorizonCalculator.normalizeRoll(-175f), 0.001f)
    }

    @Test
    fun testSmoothingFilter() {
        val current = 0f
        val target = 10f
        val smoothed = VirtualHorizonCalculator.applySmoothing(current, target, alpha = 0.22f)
        assertEquals(2.2f, smoothed, 0.001f)

        // Multiple steps converge towards target
        var value = 0f
        repeat(20) {
            value = VirtualHorizonCalculator.applySmoothing(value, target, alpha = 0.22f)
        }
        assertTrue("After 20 iterations value should be close to target", value > 9.8f)
    }

    @Test
    fun testIsLevel() {
        assertTrue("0.0° is level", VirtualHorizonCalculator.isLevel(0f))
        assertTrue("0.5° is within 0.75° threshold", VirtualHorizonCalculator.isLevel(0.5f))
        assertTrue("-0.5° is within 0.75° threshold", VirtualHorizonCalculator.isLevel(-0.5f))
        assertTrue("0.75° exact boundary is level", VirtualHorizonCalculator.isLevel(0.75f))
        assertTrue("-0.75° exact boundary is level", VirtualHorizonCalculator.isLevel(-0.75f))

        assertFalse("0.8° exceeds 0.75° threshold", VirtualHorizonCalculator.isLevel(0.8f))
        assertFalse("-1.2° exceeds 0.75° threshold", VirtualHorizonCalculator.isLevel(-1.2f))
    }
}
