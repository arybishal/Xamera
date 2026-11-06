package com.bishal.adaptivecamera.photography.pro

import android.hardware.camera2.params.RggbChannelVector
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

data class RgbGains(
    val r: Float,
    val gEven: Float,
    val gOdd: Float,
    val b: Float
) {
    fun toCamera2Gains(): RggbChannelVector {
        return RggbChannelVector(r, gEven, gOdd, b)
    }
}

/**
 * High-precision Kelvin color temperature and green/magenta tint converter for Camera2.
 * Uses the Planckian locus approximation to compute white balance chromaticity gains.
 */
object KelvinConverter {

    const val MIN_KELVIN = 2000
    const val MAX_KELVIN = 10000
    const val DEFAULT_KELVIN = 5500

    const val MIN_TINT = -50 // Green bias
    const val MAX_TINT = 50  // Magenta bias
    const val DEFAULT_TINT = 0

    /**
     * Converts a correlated color temperature (CCT in Kelvin) and tint offset (-50 to +50)
     * into Camera2 RGB color gains.
     */
    fun kelvinToRgbGains(kelvin: Int, tint: Int = 0): RgbGains {
        val k = kelvin.coerceIn(MIN_KELVIN, MAX_KELVIN)
        val temp = k / 100.0

        // 1. Calculate Red
        val red: Double = if (temp <= 66.0) {
            255.0
        } else {
            val r = temp - 60.0
            329.698727446 * (r.pow(-0.1332047592))
        }

        // 2. Calculate Green
        val green: Double = if (temp <= 66.0) {
            99.4708025861 * ln(temp) - 161.1195681661
        } else {
            val g = temp - 60.0
            288.1221695283 * (g.pow(-0.0755148492))
        }

        // 3. Calculate Blue
        val blue: Double = if (temp >= 66.0) {
            255.0
        } else if (temp <= 19.0) {
            0.0
        } else {
            val b = temp - 10.0
            138.5177312231 * ln(b) - 305.0447927307
        }

        val clampedR = red.coerceIn(1.0, 255.0)
        val clampedG = green.coerceIn(1.0, 255.0)
        val clampedB = blue.coerceIn(1.0, 255.0)

        // Normalize relative to Green
        var rNorm = clampedG / clampedR
        var bNorm = clampedG / clampedB

        // Apply Green/Magenta tint offset:
        // Negative tint shifts towards green (reduces R/B gains)
        // Positive tint shifts towards magenta (boosts R/B gains relative to G)
        val tintFactor = 1.0 + (tint.coerceIn(MIN_TINT, MAX_TINT) / 100.0) * 0.35
        rNorm *= tintFactor
        bNorm *= (2.0 - tintFactor)

        val finalR = rNorm.toFloat().coerceIn(0.5f, 4.0f)
        val finalB = bNorm.toFloat().coerceIn(0.5f, 4.0f)

        return RgbGains(
            r = finalR,
            gEven = 1.0f,
            gOdd = 1.0f,
            b = finalB
        )
    }

    /**
     * Approximates Kelvin color temperature from Camera2 red and blue gains.
     */
    fun rgbGainsToKelvin(rGain: Float, bGain: Float): Int {
        if (rGain <= 0f || bGain <= 0f) return DEFAULT_KELVIN
        val ratio = bGain / rGain
        // Empirical logarithmic mapping between blue/red gain ratio and CCT
        val approx = (2000.0 + (ratio * 3200.0)).toInt()
        return approx.coerceIn(MIN_KELVIN, MAX_KELVIN)
    }
}
