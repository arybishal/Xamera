package com.bishal.adaptivecamera.core.storage

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class MediaStoreManagerTest {

    @Test
    fun testGenerateMediaFilename() {
        val testDate = Date(1730745600000L) // Fixed timestamp
        val photoName = MediaStoreManager.generateMediaName("IMG", "jpg", testDate)
        assertTrue(photoName.startsWith("IMG_"))
        assertTrue(photoName.endsWith(".jpg"))

        val videoName = MediaStoreManager.generateMediaName("VID", "mp4", testDate)
        assertTrue(videoName.startsWith("VID_"))
        assertTrue(videoName.endsWith(".mp4"))
    }

    @Test
    fun testMediaItemFormatting() {
        val photoItem = MediaItem(
            id = 1L,
            uri = null,
            displayName = "IMG_20261104_120000.jpg",
            mimeType = "image/jpeg",
            sizeBytes = 3_500_000L, // ~3.3 MB
            dateTakenMs = 1730745600000L,
            mediaType = MediaType.PHOTO
        )
        assertEquals("3.3 MB", photoItem.sizeFormatted)
        assertEquals("", photoItem.durationFormatted)

        val videoItem = MediaItem(
            id = 2L,
            uri = null,
            displayName = "VID_20261104_120000.mp4",
            mimeType = "video/mp4",
            sizeBytes = 15_200_000L,
            dateTakenMs = 1730745600000L,
            mediaType = MediaType.VIDEO,
            durationMs = 85_000L // 1 minute 25 seconds
        )
        assertEquals("14.5 MB", videoItem.sizeFormatted)
        assertEquals("01:25", videoItem.durationFormatted)
    }
}
