package com.bishal.adaptivecamera.photography.watermark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotoWatermarkCompositorTest {

    @Test
    fun testDefaultWatermarkConfiguration() {
        val config = WatermarkConfig()
        assertFalse("Watermark should be disabled by default", config.isEnabled)
        assertEquals(WatermarkType.TEXT, config.type)
        assertEquals("Xamera Professional", config.text)
        assertEquals(WatermarkPosition.BOTTOM_RIGHT, config.position)
        assertTrue("Watermark opacity must be positive and <= 1.0", config.opacity in 0.1f..1.0f)
        assertTrue("Save original alongside must be true to prevent destructive edits", config.saveOriginalAlongside)
    }

    @Test
    fun testTimestampFormattingValidity() {
        val config = WatermarkConfig(type = WatermarkType.TIMESTAMP, timestampFormat = "yyyy-MM-dd HH:mm:ss")
        val sdf = SimpleDateFormat(config.timestampFormat, Locale.US)
        val formatted = sdf.format(Date(1700000000000L))
        assertNotNull(formatted)
        assertTrue("Formatted date should start with year", formatted.startsWith("2023"))
    }

    @Test
    fun testWatermarkPositions() {
        val positions = WatermarkPosition.values()
        assertEquals(5, positions.size)
        assertTrue(positions.contains(WatermarkPosition.BOTTOM_RIGHT))
        assertTrue(positions.contains(WatermarkPosition.BOTTOM_LEFT))
        assertTrue(positions.contains(WatermarkPosition.TOP_RIGHT))
        assertTrue(positions.contains(WatermarkPosition.TOP_LEFT))
        assertTrue(positions.contains(WatermarkPosition.CENTER))
    }
}
