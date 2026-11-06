package com.bishal.adaptivecamera.photography.raw

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RawVaultManagerTest {

    @Test
    fun testRawVaultItemSizeAndResolutionFormatting() {
        val item = RawVaultItem(
            id = 101L,
            filename = "RAW_20261106_120000.dng",
            fileSizeBytes = 25_165_824L, // 24.0 MB
            dateTakenMs = 1730000000000L,
            width = 4000,
            height = 3000,
            iso = 100,
            exposureTimeNs = 8_333_333L,
            aperture = 1.8f,
            focalLengthMm = 24.0f,
            hasMatchingJpeg = true
        )

        assertEquals("24.0 MB", item.sizeMb)
        assertEquals("4000 × 3000", item.resolutionDisplay)
        assertEquals(10, item.sensorBitDepth)
        assertTrue(item.hasMatchingJpeg)
    }

    @Test
    fun testRawVaultItemZeroResolutionFallback() {
        val unparsedItem = RawVaultItem(
            id = 102L,
            filename = "RAW_corrupted.dng",
            fileSizeBytes = 10_485_760L, // 10.0 MB
            dateTakenMs = 1730000000000L,
            width = 0,
            height = 0
        )

        assertEquals("10.0 MB", unparsedItem.sizeMb)
        assertEquals("RAW DNG", unparsedItem.resolutionDisplay)
        assertFalse(unparsedItem.hasMatchingJpeg)
    }

    @Test
    fun testPairedBaseNameMatchingLogic() {
        val rawFilename = "RAW_20261106_173045.dng"
        val jpegFilename = "IMG_20261106_173045.jpg"

        val rawBase = rawFilename.removePrefix("RAW_").substringBeforeLast(".")
        val jpegBase = jpegFilename.removePrefix("IMG_").substringBeforeLast(".")

        assertEquals("20261106_173045", rawBase)
        assertEquals("20261106_173045", jpegBase)
        assertEquals("Both files must resolve to identical base capture timestamp", rawBase, jpegBase)
    }
}
