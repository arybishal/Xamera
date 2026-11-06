package com.bishal.adaptivecamera.photography.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

class PhotometryLumaZebraTest {

    @Test
    fun testZebraDetectsHighlightsAboveThreshold() {
        val width = 120
        val height = 120
        val buffer = ByteBuffer.allocate(width * height)

        val lowLuma = (0.50f * 255f).toInt().toByte()
        val highLuma = (0.95f * 255f).toInt().toByte()

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (x < 60) {
                    buffer.put(lowLuma)
                } else {
                    buffer.put(highLuma)
                }
            }
        }
        buffer.flip()

        val result = PhotometryAnalyzer.analyzeYuv(
            yBuffer = buffer,
            uBuffer = null,
            vBuffer = null,
            width = width,
            height = height,
            yRowStride = width,
            yPixelStride = 1,
            uvRowStride = 0,
            uvPixelStride = 0,
            isHistogramEnabled = true,
            isZebraEnabled = true,
            zebraThresholdIre = 80,
            isFalseColorEnabled = false
        )

        val histogram = result.histogram
        val zebra = result.zebraMap

        assertTrue("Histogram should be populated", histogram.lumaBins.isNotEmpty())
        assertTrue("Zebra should be enabled", zebra.isEnabled)
        assertEquals(80, zebra.thresholdIre)
        assertTrue("Zebra points should be detected for 95% IRE region", zebra.points.isNotEmpty())

        for (point in zebra.points) {
            assertTrue("Zebra point xNorm should be in right half (>= 0.45f)", point.xNorm >= 0.45f)
        }
    }

    @Test
    fun testZebraDisabledProducesEmptyPoints() {
        val width = 60
        val height = 60
        val buffer = ByteBuffer.allocate(width * height)
        for (i in 0 until width * height) {
            buffer.put(255.toByte())
        }
        buffer.flip()

        val result = PhotometryAnalyzer.analyzeYuv(
            yBuffer = buffer,
            uBuffer = null,
            vBuffer = null,
            width = width,
            height = height,
            yRowStride = width,
            yPixelStride = 1,
            uvRowStride = 0,
            uvPixelStride = 0,
            isHistogramEnabled = false,
            isZebraEnabled = false,
            zebraThresholdIre = 80,
            isFalseColorEnabled = false
        )

        assertFalse(result.zebraMap.isEnabled)
        assertTrue(result.zebraMap.points.isEmpty())
    }

    @Test
    fun testZebraThresholdFiltering() {
        val width = 60
        val height = 60
        val buffer = ByteBuffer.allocate(width * height)
        val luma85 = (0.85f * 255f).toInt().toByte()
        for (i in 0 until width * height) {
            buffer.put(luma85)
        }

        // Test with 90% threshold -> should NOT trigger
        buffer.position(0)
        val result90 = PhotometryAnalyzer.analyzeYuv(
            yBuffer = buffer,
            uBuffer = null,
            vBuffer = null,
            width = width,
            height = height,
            yRowStride = width,
            yPixelStride = 1,
            uvRowStride = 0,
            uvPixelStride = 0,
            isHistogramEnabled = false,
            isZebraEnabled = true,
            zebraThresholdIre = 90,
            isFalseColorEnabled = false
        )
        assertTrue("85% IRE luma should not trigger 90% threshold", result90.zebraMap.points.isEmpty())

        // Test with 80% threshold -> SHOULD trigger
        buffer.position(0)
        val result80 = PhotometryAnalyzer.analyzeYuv(
            yBuffer = buffer,
            uBuffer = null,
            vBuffer = null,
            width = width,
            height = height,
            yRowStride = width,
            yPixelStride = 1,
            uvRowStride = 0,
            uvPixelStride = 0,
            isHistogramEnabled = false,
            isZebraEnabled = true,
            zebraThresholdIre = 80,
            isFalseColorEnabled = false
        )
        assertTrue("85% IRE luma should trigger 80% threshold", result80.zebraMap.points.isNotEmpty())
    }
}
