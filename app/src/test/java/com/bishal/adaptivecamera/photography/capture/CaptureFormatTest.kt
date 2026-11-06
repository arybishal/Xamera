package com.bishal.adaptivecamera.photography.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureFormatTest {

    @Test
    fun testCaptureFormatInvariants() {
        // JPEG: processed, non-raw
        assertFalse(CaptureFormat.JPEG.isRaw)
        assertTrue(CaptureFormat.JPEG.isProcessed)
        assertEquals("JPG", CaptureFormat.JPEG.shortLabel)

        // RAW_ONLY: raw, non-processed
        assertTrue(CaptureFormat.RAW_ONLY.isRaw)
        assertFalse(CaptureFormat.RAW_ONLY.isProcessed)
        assertEquals("RAW", CaptureFormat.RAW_ONLY.shortLabel)

        // RAW_JPEG: synchronized dual stream
        assertTrue(CaptureFormat.RAW_JPEG.isRaw)
        assertTrue(CaptureFormat.RAW_JPEG.isProcessed)
        assertEquals("RAW+JPG", CaptureFormat.RAW_JPEG.shortLabel)

        // RAW_HEIC: synchronized dual stream
        assertTrue(CaptureFormat.RAW_HEIC.isRaw)
        assertTrue(CaptureFormat.RAW_HEIC.isProcessed)
        assertEquals("RAW+HEIC", CaptureFormat.RAW_HEIC.shortLabel)

        // HEIC_ONLY: processed, non-raw
        assertFalse(CaptureFormat.HEIC_ONLY.isRaw)
        assertTrue(CaptureFormat.HEIC_ONLY.isProcessed)
        assertEquals("HEIC", CaptureFormat.HEIC_ONLY.shortLabel)
    }

    @Test
    fun testFromIdParsing() {
        assertEquals(CaptureFormat.JPEG, CaptureFormat.fromId("JPEG"))
        assertEquals(CaptureFormat.JPEG, CaptureFormat.fromId("jpeg"))
        assertEquals(CaptureFormat.RAW_ONLY, CaptureFormat.fromId("RAW_ONLY"))
        assertEquals(CaptureFormat.RAW_JPEG, CaptureFormat.fromId("raw_jpeg"))
        assertEquals(CaptureFormat.RAW_HEIC, CaptureFormat.fromId("RAW_HEIC"))
        assertEquals(CaptureFormat.HEIC_ONLY, CaptureFormat.fromId("heic_only"))

        // Fallback on invalid
        assertEquals(CaptureFormat.JPEG, CaptureFormat.fromId("INVALID_FORMAT"))
    }
}
