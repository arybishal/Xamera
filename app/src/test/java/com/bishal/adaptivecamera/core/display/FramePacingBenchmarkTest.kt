package com.bishal.adaptivecamera.core.display

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

data class FramePacingStats(
    val frameCount: Int,
    val avgFrameTimeMs: Float,
    val medianFrameTimeMs: Float,
    val p90FrameTimeMs: Float,
    val p95FrameTimeMs: Float,
    val p99FrameTimeMs: Float,
    val longStallCount: Int,
    val calculatedFps: Float
)

object FramePacingCalculator {
    fun calculateStats(frameDurationsMs: List<Float>, targetFrametimeMs: Float = 8.33f): FramePacingStats {
        if (frameDurationsMs.isEmpty()) {
            return FramePacingStats(0, 0f, 0f, 0f, 0f, 0f, 0, 0f)
        }
        val sorted = frameDurationsMs.sorted()
        val count = sorted.size
        val avg = sorted.average().toFloat()
        val median = sorted[count / 2]

        fun percentile(p: Double): Float {
            val index = ((p / 100.0) * (count - 1)).roundToInt().coerceIn(0, count - 1)
            return sorted[index]
        }

        val p90 = percentile(90.0)
        val p95 = percentile(95.0)
        val p99 = percentile(99.0)

        // Stalls defined as frames exceeding 2x target frame duration (> 16.6ms for 120Hz or > 33.3ms for 60Hz)
        val stallThreshold = targetFrametimeMs * 2.0f
        val stalls = sorted.count { it > stallThreshold }

        val fps = if (avg > 0f) 1000f / avg else 0f

        return FramePacingStats(
            frameCount = count,
            avgFrameTimeMs = avg,
            medianFrameTimeMs = median,
            p90FrameTimeMs = p90,
            p95FrameTimeMs = p95,
            p99FrameTimeMs = p99,
            longStallCount = stalls,
            calculatedFps = fps
        )
    }
}

class FramePacingBenchmarkTest {

    @Test
    fun testPacingStatsCalculationOnConsistentStream() {
        // Ideal 120Hz stream: ~8.33ms per frame
        val sampleFrames = List(120) { 8.33f }
        val stats = FramePacingCalculator.calculateStats(sampleFrames, targetFrametimeMs = 8.33f)

        assertEquals(120, stats.frameCount)
        assertEquals(8.33f, stats.avgFrameTimeMs, 0.05f)
        assertEquals(8.33f, stats.medianFrameTimeMs, 0.05f)
        assertEquals(8.33f, stats.p90FrameTimeMs, 0.05f)
        assertEquals(8.33f, stats.p95FrameTimeMs, 0.05f)
        assertEquals(8.33f, stats.p99FrameTimeMs, 0.05f)
        assertEquals(0, stats.longStallCount)
        assertEquals(120f, stats.calculatedFps, 1.0f)
    }

    @Test
    fun testPacingStatsDetectionOfStalls() {
        val mixedFrames = mutableListOf<Float>()
        repeat(97) { mixedFrames.add(8.33f) }
        repeat(2) { mixedFrames.add(12.0f) }
        repeat(1) { mixedFrames.add(25.0f) } // 1 major stall exceeding 16.6ms

        val stats = FramePacingCalculator.calculateStats(mixedFrames, targetFrametimeMs = 8.33f)

        assertEquals(100, stats.frameCount)
        assertEquals(1, stats.longStallCount)
        assertTrue(stats.p99FrameTimeMs >= 12.0f)
        assertTrue(stats.avgFrameTimeMs > 8.33f)
    }
}
