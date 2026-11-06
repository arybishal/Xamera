package com.bishal.adaptivecamera.video

import com.bishal.adaptivecamera.video.exposure.ShutterAngleCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShutterAngleCalculatorTest {

    @Test
    fun calculateExposureSeconds_matchesPhotographicStandards() {
        // Standard 180° shutter at 24 fps -> 1/48s
        val s24_180 = ShutterAngleCalculator.calculateExposureSeconds(24, 180f)
        assertEquals(1.0 / 48.0, s24_180, 1e-6)

        // 180° shutter at 30 fps -> 1/60s
        val s30_180 = ShutterAngleCalculator.calculateExposureSeconds(30, 180f)
        assertEquals(1.0 / 60.0, s30_180, 1e-6)

        // 180° shutter at 60 fps -> 1/120s
        val s60_180 = ShutterAngleCalculator.calculateExposureSeconds(60, 180f)
        assertEquals(1.0 / 120.0, s60_180, 1e-6)

        // 360° shutter at 30 fps -> 1/30s
        val s30_360 = ShutterAngleCalculator.calculateExposureSeconds(30, 360f)
        assertEquals(1.0 / 30.0, s30_360, 1e-6)

        // 90° shutter at 30 fps -> 1/120s
        val s30_90 = ShutterAngleCalculator.calculateExposureSeconds(30, 90f)
        assertEquals(1.0 / 120.0, s30_90, 1e-6)

        // 45° shutter at 24 fps -> 1/192s
        val s24_45 = ShutterAngleCalculator.calculateExposureSeconds(24, 45f)
        assertEquals(1.0 / 192.0, s24_45, 1e-6)
    }

    @Test
    fun calculateExposureNanoseconds_clampsToSensorBounds() {
        val minNanos = 20_000L      // 1/50,000s
        val maxNanos = 500_000_000L // 0.5s

        // Normal calculation: 30fps at 180° = 1/60s = 16,666,666 ns
        val nanos = ShutterAngleCalculator.calculateExposureNanoseconds(
            fps = 30,
            shutterAngle = 180f,
            minExposureNanos = minNanos,
            maxExposureNanos = maxNanos
        )
        assertEquals(16_666_666L, nanos)

        // Clamping check: very high fps or small angle clamped to min
        val clampedMin = ShutterAngleCalculator.calculateExposureNanoseconds(
            fps = 240,
            shutterAngle = 1f,
            minExposureNanos = 50_000L,
            maxExposureNanos = maxNanos
        )
        assertEquals(50_000L, clampedMin)
    }

    @Test
    fun calculateEquivalentShutterAngle_computesInverseAccurately() {
        // 1/60s at 30 fps -> 180°
        val angle30 = ShutterAngleCalculator.calculateEquivalentShutterAngle(30, 1.0 / 60.0)
        assertEquals(180f, angle30, 1e-2f)

        // 1/48s at 24 fps -> 180°
        val angle24 = ShutterAngleCalculator.calculateEquivalentShutterAngle(24, 1.0 / 48.0)
        assertEquals(180f, angle24, 1e-2f)

        // 1/120s at 30 fps -> 90°
        val angle90 = ShutterAngleCalculator.calculateEquivalentShutterAngle(30, 1.0 / 120.0)
        assertEquals(90f, angle90, 1e-2f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun calculateExposureSeconds_rejectsZeroFps() {
        ShutterAngleCalculator.calculateExposureSeconds(0, 180f)
    }

    @Test
    fun presetShutterAngles_containsStandardCinemaValues() {
        val angles = ShutterAngleCalculator.PRESET_SHUTTER_ANGLES.map { it.first }
        assertTrue(angles.contains(180f))
        assertTrue(angles.contains(90f))
        assertTrue(angles.contains(45f))
        assertTrue(angles.contains(360f))
    }
}
