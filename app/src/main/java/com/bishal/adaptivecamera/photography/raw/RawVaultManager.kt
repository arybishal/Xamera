package com.bishal.adaptivecamera.photography.raw

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

data class RawVaultItem(
    val id: Long,
    val uri: Uri? = null,
    val filename: String,
    val fileSizeBytes: Long,
    val dateTakenMs: Long,
    val width: Int,
    val height: Int,
    val iso: Int? = null,
    val exposureTimeNs: Long? = null,
    val aperture: Float? = null,
    val focalLengthMm: Float? = null,
    val hasMatchingJpeg: Boolean = false,
    val pairedJpegUri: Uri? = null,
    val sensorBitDepth: Int = 10
) {
    val sizeMb: String
        get() = String.format(java.util.Locale.US, "%.1f MB", fileSizeBytes.toDouble() / (1024.0 * 1024.0))

    val resolutionDisplay: String
        get() = if (width > 0 && height > 0) "${width} × ${height}" else "RAW DNG"
}

/**
 * Manages discovery, metadata extraction, pairing reconciliation, and export
 * for uncompressed Adobe DNG files stored in the Xamera RAW Vault.
 */
object RawVaultManager {

    /**
     * Scans MediaStore for all DNG files created by Xamera and reconciles paired JPEGs.
     */
    suspend fun queryRawVaultItems(context: Context): List<RawVaultItem> = withContext(Dispatchers.IO) {
        val vaultItems = mutableListOf<RawVaultItem>()
        val jpegMap = mutableMapOf<String, Uri>() // base name -> URI

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        try {
            // 1. First gather all JPEGs to identify pairings
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Images.Media.MIME_TYPE} = ? OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?",
                arrayOf("image/jpeg", "%.jpg"),
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: continue
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    val baseName = name.removePrefix("IMG_").substringBeforeLast(".")
                    jpegMap[baseName] = uri
                }
            }

            // 2. Query all DNG files
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                "${MediaStore.Images.Media.MIME_TYPE} = ? OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?",
                arrayOf("image/x-adobe-dng", "%.dng"),
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val filename = cursor.getString(nameCol) ?: "RAW.dng"
                    val size = cursor.getLong(sizeCol)
                    val dateTaken = cursor.getLong(dateCol)
                    val width = cursor.getInt(widthCol)
                    val height = cursor.getInt(heightCol)

                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    // Reconcile paired JPEG by comparing base timestamp
                    val baseName = filename.removePrefix("RAW_").substringBeforeLast(".")
                    val pairedUri = jpegMap[baseName]
                    val hasPair = pairedUri != null

                    // Extract EXIF tags
                    var iso: Int? = null
                    var exposureNs: Long? = null
                    var aperture: Float? = null
                    var focalLen: Float? = null

                    try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            val exif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                ExifInterface(stream)
                            } else null

                            exif?.let {
                                iso = it.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)?.toIntOrNull()
                                val expSec = it.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.toDoubleOrNull()
                                exposureNs = expSec?.let { s -> (s * 1_000_000_000.0).toLong() }
                                aperture = it.getAttribute(ExifInterface.TAG_F_NUMBER)?.toFloatOrNull()
                                focalLen = it.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.toFloatOrNull()
                            }
                        }
                    } catch (_: Exception) {}

                    vaultItems.add(
                        RawVaultItem(
                            id = id,
                            uri = uri,
                            filename = filename,
                            fileSizeBytes = size,
                            dateTakenMs = dateTaken,
                            width = width,
                            height = height,
                            iso = iso,
                            exposureTimeNs = exposureNs,
                            aperture = aperture,
                            focalLengthMm = focalLen,
                            hasMatchingJpeg = hasPair,
                            pairedJpegUri = pairedUri,
                            sensorBitDepth = 10 // Standard Sony/OmniVision mobile sensor bit depth
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        vaultItems
    }

    /**
     * Deletes an item from the RAW Vault.
     */
    suspend fun deleteRawItem(context: Context, item: RawVaultItem): Boolean = withContext(Dispatchers.IO) {
        val uri = item.uri ?: return@withContext false
        try {
            val rows = context.contentResolver.delete(uri, null, null)
            rows > 0
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Builds a standard Android share intent for the Adobe DNG file.
     */
    fun createShareIntent(item: RawVaultItem): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/x-adobe-dng"
            item.uri?.let { putExtra(Intent.EXTRA_STREAM, it) }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Exports a RAW DNG file to an external destination URI selected via Storage Access Framework (SAF).
     */
    suspend fun exportToCustomUri(
        context: Context,
        item: RawVaultItem,
        destinationUri: Uri
    ): Boolean = withContext(Dispatchers.IO) {
        val srcUri = item.uri ?: return@withContext false
        try {
            var input: InputStream? = null
            var output: OutputStream? = null
            try {
                input = context.contentResolver.openInputStream(srcUri) ?: return@withContext false
                output = context.contentResolver.openOutputStream(destinationUri) ?: return@withContext false
                input.copyTo(output)
                output.flush()
                true
            } finally {
                input?.close()
                output?.close()
            }
        } catch (_: Exception) {
            false
        }
    }
}
