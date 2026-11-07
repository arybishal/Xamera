package com.bishal.adaptivecamera.photography.exif

import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build
import android.util.Log
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Standard utility for extracting, preserving, and injecting EXIF metadata
 * across computational photography, GPU color grading, and format conversions.
 */
object ExifMetadataHelper {
    private const val TAG = "ExifMetadataHelper"

    val STANDARD_EXIF_TAGS = listOf(
        ExifInterface.TAG_MAKE,
        ExifInterface.TAG_MODEL,
        ExifInterface.TAG_DATETIME,
        ExifInterface.TAG_DATETIME_ORIGINAL,
        ExifInterface.TAG_DATETIME_DIGITIZED,
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.TAG_EXPOSURE_TIME,
        ExifInterface.TAG_F_NUMBER,
        ExifInterface.TAG_ISO_SPEED_RATINGS,
        ExifInterface.TAG_FOCAL_LENGTH,
        ExifInterface.TAG_WHITE_BALANCE,
        ExifInterface.TAG_FLASH,
        ExifInterface.TAG_COLOR_SPACE,
        ExifInterface.TAG_IMAGE_WIDTH,
        ExifInterface.TAG_IMAGE_LENGTH,
        ExifInterface.TAG_SOFTWARE
    )

    /**
     * Extracts standard capture and optical metadata from [inputStream].
     */
    fun extractExifMetadata(inputStream: InputStream): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        try {
            val exif = ExifInterface(inputStream)
            for (tag in STANDARD_EXIF_TAGS) {
                val value = exif.getAttribute(tag)
                if (value != null) {
                    metadata[tag] = value
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read EXIF from inputStream: ${e.message}")
        }
        return metadata
    }

    /**
     * Extracts standard capture and optical metadata from [file].
     */
    fun extractExifMetadata(file: File): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        if (!file.exists()) return metadata
        try {
            val exif = ExifInterface(file.absolutePath)
            for (tag in STANDARD_EXIF_TAGS) {
                val value = exif.getAttribute(tag)
                if (value != null) {
                    metadata[tag] = value
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read EXIF from file: ${e.message}")
        }
        return metadata
    }

    /**
     * Calculates the rotation angle in degrees from the orientation tag.
     */
    fun getRotationDegreesFromOrientation(orientationTag: String?): Int {
        val orientation = orientationTag?.toIntOrNull() ?: ExifInterface.ORIENTATION_NORMAL
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    /**
     * Rotates [bitmap] if [degrees] is non-zero, returning a freshly oriented bitmap.
     * Recycles [bitmap] if a new rotated instance is created.
     */
    fun rotateBitmapIfRequired(bitmap: Bitmap?, degrees: Int): Bitmap? {
        if (bitmap == null) return null
        if (degrees % 360 == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) {
            bitmap.recycle()
        }
        return rotated
    }

    /**
     * Writes standard [attributes] to [targetFile] via ExifInterface.
     */
    fun writeExifMetadata(targetFile: File, attributes: Map<String, String>) {
        if (!targetFile.exists() || attributes.isEmpty()) return
        try {
            val exif = ExifInterface(targetFile.absolutePath)
            for ((tag, value) in attributes) {
                exif.setAttribute(tag, value)
            }

            // Ensure make and model fallback if missing
            if (!attributes.containsKey(ExifInterface.TAG_MAKE)) {
                exif.setAttribute(ExifInterface.TAG_MAKE, Build.MANUFACTURER)
            }
            if (!attributes.containsKey(ExifInterface.TAG_MODEL)) {
                exif.setAttribute(ExifInterface.TAG_MODEL, Build.MODEL)
            }
            if (!attributes.containsKey(ExifInterface.TAG_SOFTWARE)) {
                exif.setAttribute(ExifInterface.TAG_SOFTWARE, "Xamera")
            }
            if (!attributes.containsKey(ExifInterface.TAG_DATETIME)) {
                val formatted = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date())
                exif.setAttribute(ExifInterface.TAG_DATETIME, formatted)
            }

            exif.saveAttributes()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write EXIF attributes to target file: ${e.message}")
        }
    }

    /**
     * Injects camera device branding and timestamp to a new photo file.
     */
    fun stampDefaultCameraMetadata(targetFile: File, orientation: Int = ExifInterface.ORIENTATION_NORMAL) {
        if (!targetFile.exists()) return
        val map = mapOf(
            ExifInterface.TAG_MAKE to Build.MANUFACTURER,
            ExifInterface.TAG_MODEL to Build.MODEL,
            ExifInterface.TAG_SOFTWARE to "Xamera",
            ExifInterface.TAG_ORIENTATION to orientation.toString(),
            ExifInterface.TAG_DATETIME to SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US).format(Date())
        )
        writeExifMetadata(targetFile, map)
    }
}
