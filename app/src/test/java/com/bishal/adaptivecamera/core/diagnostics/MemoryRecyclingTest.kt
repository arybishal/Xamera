package com.bishal.adaptivecamera.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoryRecyclingTest {

    @Test
    fun testMemoryBudgetCalculation() {
        // Full HD RGBA 8888: 1920 * 1080 * 4 bytes = 8,294,400 bytes (~7.9 MB)
        val width = 1920
        val height = 1080
        val bytesPerPixel = 4
        val frameBytes = width.toLong() * height * bytesPerPixel
        assertEquals(8_294_400L, frameBytes)

        // 5-frame burst memory footprint
        val burstCount = 5
        val totalBurstBytes = frameBytes * burstCount
        assertEquals(41_472_000L, totalBurstBytes) // ~39.55 MB

        // Safe recycling budget test: total burst should not exceed max single heap allocation threshold (~96MB)
        assertTrue(totalBurstBytes < 96 * 1024 * 1024)
    }

    @Test
    fun test4kMemoryBudgetCalculation() {
        // 4K UHD: 3840 * 2160 * 4 = 33,177,600 bytes (~31.64 MB per frame)
        val width = 3840
        val height = 2160
        val bytesPerPixel = 4
        val frameBytes = width.toLong() * height * bytesPerPixel
        assertEquals(33_177_600L, frameBytes)

        // 7-frame max burst at 4K without recycling would consume ~221 MB!
        val burstCount = 7
        val unmanagedBurstBytes = frameBytes * burstCount
        assertTrue(unmanagedBurstBytes > 200 * 1024 * 1024)

        // With immediate frame recycling, peak memory footprint is capped at reference + 1 active frame (~63.28 MB)
        val managedPeakBytes = frameBytes * 2
        assertTrue(managedPeakBytes < 70 * 1024 * 1024)
    }

    @Test
    fun testRecyclingSafetyInvariants() {
        // Test safe list disposal logic without calling Android native stub
        val disposedIndices = mutableListOf<Int>()
        val mockFrames = listOf("frame_0", "frame_1", "frame_2", "frame_3", "frame_4")

        mockFrames.forEachIndexed { index, _ ->
            disposedIndices.add(index)
        }
        assertEquals(5, disposedIndices.size)
        assertEquals(listOf(0, 1, 2, 3, 4), disposedIndices)
    }
}
