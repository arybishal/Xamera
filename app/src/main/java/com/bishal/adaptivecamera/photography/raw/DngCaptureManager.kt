package com.bishal.adaptivecamera.photography.raw

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.DngCreator
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.bishal.adaptivecamera.camera.capability.CameraResolution
import com.bishal.adaptivecamera.core.storage.MediaItem
import com.bishal.adaptivecamera.core.storage.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RawEligibility(
    val isEligible: Boolean,
    val rawFormat: Int? = null,
    val maxRawResolution: CameraResolution? = null,
    val failureReason: String? = null
)

data class DngValidationResult(
    val isValid: Boolean,
    val fileSizeBytes: Long,
    val mimeType: String,
    val hasTiffHeader: Boolean,
    val details: String
)

class DngCaptureManager(private val context: Context) {

    companion object {
        private const val TAG = "DngCaptureManager"
        const val DNG_MIME_TYPE = "image/x-adobe-dng"
        private const val DIRECTORY_NAME = "AdaptiveCamera"

        /**
         * Inspects a DNG file by validating existence, non-zero file size, and standard TIFF/DNG magic header bytes.
         */
        fun validateDngFile(file: File): DngValidationResult {
            if (!file.exists()) {
                return DngValidationResult(
                    isValid = false,
                    fileSizeBytes = 0,
                    mimeType = DNG_MIME_TYPE,
                    hasTiffHeader = false,
                    details = "File does not exist: ${file.absolutePath}"
                )
            }

            val size = file.length()
            if (size <= 0) {
                return DngValidationResult(
                    isValid = false,
                    fileSizeBytes = size,
                    mimeType = DNG_MIME_TYPE,
                    hasTiffHeader = false,
                    details = "File is empty (0 bytes)"
                )
            }

            return try {
                FileInputStream(file).use { fis ->
                    val header = ByteArray(4)
                    val read = fis.read(header)
                    if (read < 4) {
                        return@use DngValidationResult(
                            isValid = false,
                            fileSizeBytes = size,
                            mimeType = DNG_MIME_TYPE,
                            hasTiffHeader = false,
                            details = "Could not read 4-byte header"
                        )
                    }

                    // Check Little-Endian (0x49 0x49 0x2A 0x00 -> "II*") or Big-Endian (0x4D 0x4D 0x00 0x2A -> "MM*")
                    val isLittleEndianTiff = header[0] == 0x49.toByte() && header[1] == 0x49.toByte() && header[2] == 0x2A.toByte() && header[3] == 0x00.toByte()
                    val isBigEndianTiff = header[0] == 0x4D.toByte() && header[1] == 0x4D.toByte() && header[2] == 0x00.toByte() && header[3] == 0x2A.toByte()

                    val hasTiff = isLittleEndianTiff || isBigEndianTiff

                    DngValidationResult(
                        isValid = hasTiff,
                        fileSizeBytes = size,
                        mimeType = DNG_MIME_TYPE,
                        hasTiffHeader = hasTiff,
                        details = if (hasTiff) "Valid TIFF/DNG header confirmed (size: $size bytes)" else "Invalid header bytes: [${header.joinToString { it.toString(16) }}]"
                    )
                }
            } catch (e: Exception) {
                DngValidationResult(
                    isValid = false,
                    fileSizeBytes = size,
                    mimeType = DNG_MIME_TYPE,
                    hasTiffHeader = false,
                    details = "Exception reading header: ${e.message}"
                )
            }
        }
    }

    /**
     * Verifies if the target camera device supports capturing and outputting RAW_SENSOR streams.
     */
    fun checkRawEligibility(characteristics: CameraCharacteristics): RawEligibility {
        val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
        val hasRawCap = capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW)

        if (!hasRawCap) {
            return RawEligibility(
                isEligible = false,
                failureReason = "Hardware does not advertise REQUEST_AVAILABLE_CAPABILITIES_RAW"
            )
        }

        val streamMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        if (streamMap == null) {
            return RawEligibility(
                isEligible = false,
                failureReason = "StreamConfigurationMap unavailable"
            )
        }

        val rawSizes = streamMap.getOutputSizes(ImageFormat.RAW_SENSOR)
        if (rawSizes.isNullOrEmpty()) {
            return RawEligibility(
                isEligible = false,
                failureReason = "No output sizes advertised for ImageFormat.RAW_SENSOR"
            )
        }

        val maxRawSize = rawSizes.maxByOrNull { it.width * it.height }!!
        val resolution = CameraResolution(maxRawSize.width, maxRawSize.height)

        return RawEligibility(
            isEligible = true,
            rawFormat = ImageFormat.RAW_SENSOR,
            maxRawResolution = resolution
        )
    }

    /**
     * Writes raw Image data into MediaStore as a valid Adobe DNG file using DngCreator.
     */
    suspend fun saveDngImage(
        rawImage: Image,
        characteristics: CameraCharacteristics,
        captureResult: TotalCaptureResult,
        orientationDegrees: Int = 0
    ): Result<MediaItem> = withContext(Dispatchers.IO) {
        var dngCreator: DngCreator? = null
        var outputUri: Uri? = null
        var outputStream: OutputStream? = null

        try {
            dngCreator = DngCreator(characteristics, captureResult)
            dngCreator.setOrientation(orientationDegrees)

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "RAW_${timeStamp}.dng"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, DNG_MIME_TYPE)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/$DIRECTORY_NAME")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            }

            outputUri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return@withContext Result.failure(IllegalStateException("Failed to create MediaStore entry for DNG"))

            outputStream = context.contentResolver.openOutputStream(outputUri)
                ?: return@withContext Result.failure(IllegalStateException("Failed to open output stream for DNG URI: $outputUri"))

            dngCreator.writeImage(outputStream, rawImage)
            outputStream.flush()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(outputUri, contentValues, null, null)
            }

            val mediaId = try { ContentUris.parseId(outputUri) } catch (_: Exception) { System.currentTimeMillis() }
            val savedMediaItem = MediaItem(
                id = mediaId,
                uri = outputUri,
                displayName = filename,
                mimeType = DNG_MIME_TYPE,
                sizeBytes = 0L,
                dateTakenMs = System.currentTimeMillis(),
                mediaType = MediaType.PHOTO,
                width = rawImage.width,
                height = rawImage.height
            )

            Result.success(savedMediaItem)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing DNG capture", e)
            outputUri?.let { uri ->
                try {
                    context.contentResolver.delete(uri, null, null)
                } catch (_: Exception) {}
            }
            Result.failure(e)
        } finally {
            try {
                outputStream?.close()
            } catch (_: Exception) {}
            try {
                dngCreator?.close()
            } catch (_: Exception) {}
            try {
                rawImage.close()
            } catch (_: Exception) {}
        }
    }
}
