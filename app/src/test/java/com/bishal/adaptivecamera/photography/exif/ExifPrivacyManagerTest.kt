package com.bishal.adaptivecamera.photography.exif

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExifPrivacyManagerTest {

    @Test
    fun testDefaultExifPrivacyConfiguration() {
        val config = ExifPrivacyConfig()
        assertFalse(config.isPrivacyModeEnabled)
        assertTrue(config.stripGpsLocation)
        assertTrue(config.stripDeviceModel)
        assertTrue(config.stripDeviceManufacturer)
        assertTrue(config.stripAuthorCopyright)
    }

    @Test
    fun testGpsTagsCompleteness() {
        val tags = ExifPrivacyManager.GPS_TAGS
        assertEquals(19, tags.size)
        assertTrue(tags.contains("GPSLatitude"))
        assertTrue(tags.contains("GPSLongitude"))
        assertTrue(tags.contains("GPSAltitude"))
        assertTrue(tags.contains("GPSTimeStamp"))
        assertTrue(tags.contains("GPSDateStamp"))
    }

    @Test
    fun testHardwareAndAuthorTagsDefinitions() {
        val hwTags = ExifPrivacyManager.HARDWARE_TAGS
        assertTrue(hwTags.contains("Make"))
        assertTrue(hwTags.contains("Model"))

        val authorTags = ExifPrivacyManager.AUTHOR_TAGS
        assertTrue(authorTags.contains("Artist"))
        assertTrue(authorTags.contains("Copyright"))
        assertTrue(authorTags.contains("Software"))
    }

    @Test
    fun testDisabledPrivacyModeReturnsFalseWithoutTouchingFile() {
        val dummyFile = File("non_existent_file.jpg")
        val config = ExifPrivacyConfig(isPrivacyModeEnabled = false)
        val result = ExifPrivacyManager.stripExifPrivacyData(dummyFile, config)
        assertFalse("Disabled privacy mode should return false immediately", result)
    }
}
