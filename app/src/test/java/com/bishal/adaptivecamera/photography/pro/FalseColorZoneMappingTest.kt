package com.bishal.adaptivecamera.photography.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer

class FalseColorZoneMappingTest {

    @Test
    fun testAllBroadcastZonesCoverContinuousIreRange() {
        for (ire in 0..100) {
            val zone = FalseColorZone.fromIre(ire)
            assertTrue(
                "IRE $ire must be within zone bounds [${zone.ireMin}, ${zone.ireMax}]",
                ire in zone.ireMin..zone.ireMax
            )
        }
    }

    @Test
    fun testKeyReferenceZones() {
        // 0-9 IRE: Crushed black
        assertEquals(FalseColorZone.CRUSHED_BLACK, FalseColorZone.fromIre(0))
        assertEquals(FalseColorZone.CRUSHED_BLACK, FalseColorZone.fromIre(5))
        assertEquals(FalseColorZone.CRUSHED_BLACK, FalseColorZone.fromIre(9))

        // 38-44 IRE: 18% Middle Gray
        assertEquals(FalseColorZone.MIDDLE_GRAY_18, FalseColorZone.fromIre(38))
        assertEquals(FalseColorZone.MIDDLE_GRAY_18, FalseColorZone.fromIre(41))
        assertEquals(FalseColorZone.MIDDLE_GRAY_18, FalseColorZone.fromIre(44))

        // 55-69 IRE: Optimal Skin Tones
        assertEquals(FalseColorZone.SKIN_TONES, FalseColorZone.fromIre(55))
        assertEquals(FalseColorZone.SKIN_TONES, FalseColorZone.fromIre(62))
        assertEquals(FalseColorZone.SKIN_TONES, FalseColorZone.fromIre(69))

        // 98-100 IRE: Clipped Highlight
        assertEquals(FalseColorZone.CLIPPED_HIGHLIGHT, FalseColorZone.fromIre(98))
        assertEquals(FalseColorZone.CLIPPED_HIGHLIGHT, FalseColorZone.fromIre(100))
    }

    @Test
    fun testFalseColorSampling() {
        val width = 60
        val height = 60
        val buffer = ByteBuffer.allocate(width * height)

        // Fill with skin tone luma (~60% IRE -> ~153)
        val skinLuma = (0.60f * 255f).toInt().toByte()
        for (i in 0 until width * height) {
            buffer.put(skinLuma)
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
            zebraThresholdIre = 90,
            isFalseColorEnabled = true
        )

        val falseColorMap = result.falseColorMap
        assertTrue(falseColorMap.isEnabled)
        assertTrue(falseColorMap.samples.isNotEmpty())

        val firstSample = falseColorMap.samples.first()
        assertEquals(FalseColorZone.SKIN_TONES.colorArgb, firstSample.colorArgb)
        assertTrue(firstSample.ire in 55..65)
    }

    @Test
    fun testFalseColorDisabled() {
        val width = 60
        val height = 60
        val buffer = ByteBuffer.allocate(width * height)
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
            zebraThresholdIre = 90,
            isFalseColorEnabled = false
        )

        val falseColorMap = result.falseColorMap
        assertFalse(falseColorMap.isEnabled)
        assertTrue(falseColorMap.samples.isEmpty())
    }
}
