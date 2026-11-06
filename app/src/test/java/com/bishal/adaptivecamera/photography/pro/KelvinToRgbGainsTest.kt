package com.bishal.adaptivecamera.photography.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KelvinToRgbGainsTest {

    @Test
    fun testWarmColorTemperatureHasHighRedLowBlue() {
        val warmGains = KelvinConverter.kelvinToRgbGains(2500, tint = 0)

        // Warm light requires higher blue boost or inverted depending on normalization,
        // let's check:
        // In KelvinConverter:
        // At 2500K: clampedR ~ 255, clampedB is low (~30-60).
        // rNorm = clampedG / clampedR, bNorm = clampedG / clampedB.
        // Therefore bNorm is high and rNorm is low (compensating for warm scene illuminant).
        assertTrue("R gain should be within reasonable bounds [0.5, 4.0]", warmGains.r in 0.5f..4.0f)
        assertTrue("B gain should be within reasonable bounds [0.5, 4.0]", warmGains.b in 0.5f..4.0f)
        assertTrue("Green gains must be 1.0f neutral", warmGains.gEven == 1.0f && warmGains.gOdd == 1.0f)
        assertTrue("Low Kelvin (warm illumination) requires higher Blue gain than Red gain to neutralize",
            warmGains.b > warmGains.r
        )
    }

    @Test
    fun testCoolColorTemperatureHasHighRedLowBlue() {
        val coolGains = KelvinConverter.kelvinToRgbGains(9000, tint = 0)

        assertTrue("R gain within bounds", coolGains.r in 0.5f..4.0f)
        assertTrue("B gain within bounds", coolGains.b in 0.5f..4.0f)
        assertTrue("High Kelvin (cool illumination) requires higher Red gain than Blue gain to neutralize",
            coolGains.r > coolGains.b
        )
    }

    @Test
    fun testTintOffsetModulatesGains() {
        val neutral = KelvinConverter.kelvinToRgbGains(5500, tint = 0)
        val magenta = KelvinConverter.kelvinToRgbGains(5500, tint = 50)
        val green = KelvinConverter.kelvinToRgbGains(5500, tint = -50)

        // Magenta tint increases rNorm relative to bNorm
        assertTrue("Magenta tint should increase Red gain", magenta.r >= neutral.r)
        assertTrue("Green tint should decrease Red gain", green.r <= neutral.r)
    }

    @Test
    fun testBoundaryClamping() {
        val belowMin = KelvinConverter.kelvinToRgbGains(1000)
        val atMin = KelvinConverter.kelvinToRgbGains(2000)
        assertEquals(atMin.r, belowMin.r, 0.001f)
        assertEquals(atMin.b, belowMin.b, 0.001f)

        val aboveMax = KelvinConverter.kelvinToRgbGains(15000)
        val atMax = KelvinConverter.kelvinToRgbGains(10000)
        assertEquals(atMax.r, aboveMax.r, 0.001f)
        assertEquals(atMax.b, aboveMax.b, 0.001f)
    }

    @Test
    fun testInverseRgbGainsToKelvin() {
        val kelvinLow = KelvinConverter.rgbGainsToKelvin(rGain = 1.0f, bGain = 0.5f)
        val kelvinHigh = KelvinConverter.rgbGainsToKelvin(rGain = 0.5f, bGain = 2.0f)

        assertTrue("Lower B/R ratio should yield lower CCT", kelvinLow < kelvinHigh)
        assertTrue("CCT should be clamped within [2000, 10000]", kelvinLow in 2000..10000)
        assertTrue("CCT should be clamped within [2000, 10000]", kelvinHigh in 2000..10000)
    }
}
