package com.bishal.adaptivecamera.photography.exif

import android.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

data class ExifPrivacyConfig(
    val isPrivacyModeEnabled: Boolean = false,
    val stripGpsLocation: Boolean = true,
    val stripDeviceModel: Boolean = true,
    val stripDeviceManufacturer: Boolean = true,
    val stripAuthorCopyright: Boolean = true
)

/**
 * Strips identifiable location, hardware model, and personal author metadata from captured photos
 * while strictly preserving essential exposure parameters and optical orientation.
 */
object ExifPrivacyManager {

    val GPS_TAGS = arrayOf(
        ExifInterface.TAG_GPS_LATITUDE,
        ExifInterface.TAG_GPS_LONGITUDE,
        ExifInterface.TAG_GPS_ALTITUDE,
        ExifInterface.TAG_GPS_LATITUDE_REF,
        ExifInterface.TAG_GPS_LONGITUDE_REF,
        ExifInterface.TAG_GPS_ALTITUDE_REF,
        ExifInterface.TAG_GPS_TIMESTAMP,
        ExifInterface.TAG_GPS_DATESTAMP,
        ExifInterface.TAG_GPS_PROCESSING_METHOD,
        ExifInterface.TAG_GPS_SPEED,
        ExifInterface.TAG_GPS_SPEED_REF,
        ExifInterface.TAG_GPS_TRACK,
        ExifInterface.TAG_GPS_TRACK_REF,
        ExifInterface.TAG_GPS_IMG_DIRECTION,
        ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
        ExifInterface.TAG_GPS_MAP_DATUM,
        ExifInterface.TAG_GPS_AREA_INFORMATION,
        ExifInterface.TAG_GPS_DOP,
        ExifInterface.TAG_GPS_DIFFERENTIAL
    )

    val HARDWARE_TAGS = arrayOf(
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL
    )

    val AUTHOR_TAGS = arrayOf(
        ExifInterface.TAG_ARTIST,
        ExifInterface.TAG_COPYRIGHT,
        ExifInterface.TAG_SOFTWARE
    )

    /**
     * Strips configured privacy tags from [jpegFile] in place without image recompression.
     */
    fun stripExifPrivacyData(jpegFile: File, config: ExifPrivacyConfig): Boolean {
        if (!config.isPrivacyModeEnabled || !jpegFile.exists()) return false

        return try {
            val exif = ExifInterface(jpegFile.absolutePath)

            if (config.stripGpsLocation) {
                for (tag in GPS_TAGS) {
                    exif.setAttribute(tag, null)
                }
            }

            if (config.stripDeviceModel) {
                exif.setAttribute(ExifInterface.TAG_MODEL, null)
            }

            if (config.stripDeviceManufacturer) {
                exif.setAttribute(ExifInterface.TAG_MAKE, null)
            }

            if (config.stripAuthorCopyright) {
                for (tag in AUTHOR_TAGS) {
                    exif.setAttribute(tag, null)
                }
            }

            exif.saveAttributes()
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Strips configured privacy tags from in-memory [jpegBytes] via a temporary file.
     */
    fun stripExifPrivacyBytes(jpegBytes: ByteArray, config: ExifPrivacyConfig): ByteArray {
        if (!config.isPrivacyModeEnabled) return jpegBytes

        return try {
            val tempFile = File.createTempFile("exif_privacy", ".jpg").apply {
                FileOutputStream(this).use { it.write(jpegBytes) }
                deleteOnExit()
            }

            stripExifPrivacyData(tempFile, config)
            val result = tempFile.readBytes()
            tempFile.delete()
            result
        } catch (_: Exception) {
            jpegBytes
        }
    }

    /**
     * Reads back relevant EXIF metadata for testing and diagnostic verification.
     */
    fun readExifMetadata(jpegBytes: ByteArray): Map<String, String?> {
        return try {
            val tempFile = File.createTempFile("exif_read", ".jpg").apply {
                FileOutputStream(this).use { it.write(jpegBytes) }
                deleteOnExit()
            }
            val exif = ExifInterface(tempFile.absolutePath)
            val tags = arrayOf(
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_ARTIST,
                ExifInterface.TAG_COPYRIGHT,
                ExifInterface.TAG_EXPOSURE_TIME,
                ExifInterface.TAG_F_NUMBER,
                ExifInterface.TAG_ISO_SPEED_RATINGS,
                ExifInterface.TAG_FOCAL_LENGTH,
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_ORIENTATION
            )
            val map = tags.associateWith { exif.getAttribute(it) }
            tempFile.delete()
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
