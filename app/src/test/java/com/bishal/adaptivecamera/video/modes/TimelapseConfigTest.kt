package com.bishal.adaptivecamera.video.modes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TimelapseConfigTest {

    @Test
    fun testTimelapseIntervalSpeedupCalculation() {
        // 1 frame every 1s at 30fps playback = 30x real-world speedup
        assertEquals(30f, TimelapseInterval.ONE_SEC.calculateSpeedup(30), 0.001f)

        // 1 frame every 0.5s at 30fps playback = 15x real-world speedup
        assertEquals(15f, TimelapseInterval.HALF_SEC.calculateSpeedup(30), 0.001f)

        // 1 frame every 2s at 30fps playback = 60x real-world speedup
        assertEquals(60f, TimelapseInterval.TWO_SEC.calculateSpeedup(30), 0.001f)

        // 1 frame every 5s at 30fps playback = 150x real-world speedup
        assertEquals(150f, TimelapseInterval.FIVE_SEC.calculateSpeedup(30), 0.001f)

        // 1 frame every 10s at 30fps playback = 300x real-world speedup
        assertEquals(300f, TimelapseInterval.TEN_SEC.calculateSpeedup(30), 0.001f)
    }

    @Test
    fun testTimelapseTelemetryDefaults() {
        val telemetry = TimelapseTelemetry()
        assertEquals(TimelapseInterval.ONE_SEC, telemetry.interval)
        assertEquals(0L, telemetry.capturedFramesCount)
        assertEquals(0L, telemetry.realElapsedSeconds)
        assertEquals(0f, telemetry.outputVideoSeconds, 0.001f)
        assertEquals(30f, telemetry.speedupFactor, 0.001f)
    }

    @Test
    fun testTimelapseIntervalRecommendedScenes() {
        TimelapseInterval.values().forEach { interval ->
            assertNotNull(interval.displayName)
            assertNotNull(interval.recommendedScene)
        }
    }
}
