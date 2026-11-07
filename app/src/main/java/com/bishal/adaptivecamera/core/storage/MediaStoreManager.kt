package com.bishal.adaptivecamera.core.storage

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.video.MediaStoreOutputOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaStoreManager(private val context: Context) {

    companion object {
        const val ALBUM_DIRECTORY = "DCIM/Xamera"
        const val LEGACY_ALBUM_DIRECTORY = "DCIM/AdaptiveCamera"
        private const val PHOTO_PREFIX = "IMG"
        private const val VIDEO_PREFIX = "VID"
        private const val PHOTO_MIME_TYPE = "image/jpeg"
        private const val VIDEO_MIME_TYPE = "video/mp4"

        fun generateMediaName(prefix: String, extension: String, date: Date = Date()): String {
            val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            return "${prefix}_${formatter.format(date)}.$extension"
        }
    }

    private val contentResolver: ContentResolver
        get() = context.contentResolver

    fun createPhotoOutputFileOptions(): ImageCapture.OutputFileOptions {
        val fileName = generateMediaName(PHOTO_PREFIX, "jpg")
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, PHOTO_MIME_TYPE)
            put(MediaStore.MediaColumns.RELATIVE_PATH, ALBUM_DIRECTORY)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        return ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()
    }

    fun markPhotoPendingComplete(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            try {
                contentResolver.update(uri, values, null, null)
            } catch (e: Exception) {
                // Ignore or log
            }
        }
    }

    /**
     * Center-crops a saved photo Uri to match the configured aspect ratio.
     */
    fun cropPhotoUri(uri: Uri, aspectRatio: com.bishal.adaptivecamera.camera.config.AspectRatioConfig) {
        if (aspectRatio == com.bishal.adaptivecamera.camera.config.AspectRatioConfig.FULL) return
        try {
            val bitmap = contentResolver.openInputStream(uri)?.use { stream ->
                android.graphics.BitmapFactory.decodeStream(stream)
            } ?: return

            val cropRect = aspectRatio.calculateCropRect(bitmap.width, bitmap.height)
            if (cropRect.width() == bitmap.width && cropRect.height() == bitmap.height) {
                bitmap.recycle()
                return
            }

            val cropped = android.graphics.Bitmap.createBitmap(
                bitmap,
                cropRect.left,
                cropRect.top,
                cropRect.width(),
                cropRect.height()
            )

            contentResolver.openOutputStream(uri, "wt")?.use { out ->
                cropped.compress(android.graphics.Bitmap.CompressFormat.JPEG, 98, out)
                out.flush()
            }

            if (cropped != bitmap) cropped.recycle()
            bitmap.recycle()
        } catch (e: Exception) {
            android.util.Log.w("MediaStoreManager", "Failed to crop photo for aspect ratio", e)
        }
    }

    /**
     * Center-crops raw JPEG bytes to match the configured aspect ratio.
     */
    fun cropJpegBytes(bytes: ByteArray, aspectRatio: com.bishal.adaptivecamera.camera.config.AspectRatioConfig): ByteArray {
        if (aspectRatio == com.bishal.adaptivecamera.camera.config.AspectRatioConfig.FULL) return bytes
        return try {
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return bytes
            val cropRect = aspectRatio.calculateCropRect(bitmap.width, bitmap.height)
            if (cropRect.width() == bitmap.width && cropRect.height() == bitmap.height) {
                bitmap.recycle()
                return bytes
            }

            val cropped = android.graphics.Bitmap.createBitmap(
                bitmap,
                cropRect.left,
                cropRect.top,
                cropRect.width(),
                cropRect.height()
            )

            val stream = java.io.ByteArrayOutputStream()
            cropped.compress(android.graphics.Bitmap.CompressFormat.JPEG, 98, stream)
            val outBytes = stream.toByteArray()

            if (cropped != bitmap) cropped.recycle()
            bitmap.recycle()
            outBytes
        } catch (e: Exception) {
            bytes
        }
    }

    fun createVideoOutputOptions(): MediaStoreOutputOptions {
        val fileName = generateMediaName(VIDEO_PREFIX, "mp4")
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, VIDEO_MIME_TYPE)
            put(MediaStore.MediaColumns.RELATIVE_PATH, ALBUM_DIRECTORY)
            put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        return MediaStoreOutputOptions.Builder(
            contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()
    }

    /**
     * Publishes a completed video recording file into the Android MediaStore under DCIM/AdaptiveCamera.
     */
    fun insertVideoFile(file: File, mimeType: String = VIDEO_MIME_TYPE): MediaItem? {
        val fileName = file.name
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, ALBUM_DIRECTORY)
            put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        contentResolver.openOutputStream(uri)?.use { out ->
            file.inputStream().use { input ->
                input.copyTo(out)
            }
            out.flush()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            contentResolver.update(uri, values, null, null)
        }
        val id = try {
            ContentUris.parseId(uri)
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
        return MediaItem(
            id = id,
            uri = uri,
            displayName = fileName,
            mimeType = mimeType,
            dateTakenMs = System.currentTimeMillis(),
            sizeBytes = file.length(),
            mediaType = MediaType.VIDEO
        )
    }

    fun queryLatestMedia(): MediaItem? {
        val photos = queryRecentMedia(limit = 1, type = MediaType.PHOTO)
        val videos = queryRecentMedia(limit = 1, type = MediaType.VIDEO)

        return (photos + videos).maxByOrNull { it.dateTakenMs }
    }

    fun queryRecentMedia(limit: Int = 100, type: MediaType? = null): List<MediaItem> {
        val mediaList = mutableListOf<MediaItem>()

        if (type == null || type == MediaType.PHOTO) {
            queryPhotos(limit, mediaList)
        }
        if (type == null || type == MediaType.VIDEO) {
            queryVideos(limit, mediaList)
        }

        return mediaList.sortedByDescending { it.dateTakenMs }.take(limit)
    }

    private fun queryPhotos(limit: Int, outList: MutableList<MediaItem>) {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )
        val selection = "(${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? OR ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?)"
        val selectionArgs = arrayOf("%$ALBUM_DIRECTORY%", "%$LEGACY_ALBUM_DIRECTORY%")
        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        try {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    val name = cursor.getString(nameCol) ?: "IMG_$id.jpg"
                    val mime = cursor.getString(mimeCol) ?: PHOTO_MIME_TYPE
                    val size = cursor.getLong(sizeCol)
                    val date = cursor.getLong(dateCol)
                    val width = cursor.getInt(widthCol)
                    val height = cursor.getInt(heightCol)

                    outList.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            displayName = name,
                            mimeType = mime,
                            sizeBytes = size,
                            dateTakenMs = date,
                            mediaType = MediaType.PHOTO,
                            width = width,
                            height = height
                        )
                    )
                    count++
                }
            }
        } catch (e: Exception) {
            // Scoped storage permission or missing table
        }
    }

    private fun queryVideos(limit: Int, outList: MutableList<MediaItem>) {
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.DURATION
        )
        val selection = "(${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? OR ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?)"
        val selectionArgs = arrayOf("%$ALBUM_DIRECTORY%", "%$LEGACY_ALBUM_DIRECTORY%")
        val sortOrder = "${MediaStore.Video.Media.DATE_TAKEN} DESC"

        try {
            contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)

                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val name = cursor.getString(nameCol) ?: "VID_$id.mp4"
                    val mime = cursor.getString(mimeCol) ?: VIDEO_MIME_TYPE
                    val size = cursor.getLong(sizeCol)
                    val date = cursor.getLong(dateCol)
                    val width = cursor.getInt(widthCol)
                    val height = cursor.getInt(heightCol)
                    val duration = cursor.getLong(durationCol)

                    outList.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            displayName = name,
                            mimeType = mime,
                            sizeBytes = size,
                            dateTakenMs = date,
                            mediaType = MediaType.VIDEO,
                            width = width,
                            height = height,
                            durationMs = duration
                        )
                    )
                    count++
                }
            }
        } catch (e: Exception) {
            // Scoped storage permission or missing table
        }
    }

    fun deleteMedia(item: MediaItem): Boolean {
        val uri = item.uri ?: return false
        return try {
            val rows = contentResolver.delete(uri, null, null)
            rows > 0
        } catch (e: Exception) {
            false
        }
    }

    fun savePhoto(file: File, displayName: String): MediaItem {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, PHOTO_MIME_TYPE)
            put(MediaStore.MediaColumns.RELATIVE_PATH, ALBUM_DIRECTORY)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("Failed to insert MediaStore record for $displayName")

        contentResolver.openOutputStream(uri)?.use { out ->
            file.inputStream().use { input ->
                input.copyTo(out)
            }
        } ?: throw IllegalStateException("Failed to open output stream for $uri")

        markPhotoPendingComplete(uri)

        val id = try { ContentUris.parseId(uri) } catch (_: Exception) { System.currentTimeMillis() }
        return MediaItem(
            id = id,
            uri = uri,
            displayName = displayName,
            mimeType = PHOTO_MIME_TYPE,
            sizeBytes = file.length(),
            dateTakenMs = System.currentTimeMillis(),
            mediaType = MediaType.PHOTO
        )
    }

    fun saveImageBytes(bytes: ByteArray, displayName: String, mimeType: String = PHOTO_MIME_TYPE): MediaItem {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, ALBUM_DIRECTORY)
            put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("Failed to insert MediaStore record for $displayName")

        contentResolver.openOutputStream(uri)?.use { out ->
            out.write(bytes)
            out.flush()
        } ?: throw IllegalStateException("Failed to open output stream for $uri")

        markPhotoPendingComplete(uri)

        val id = try { ContentUris.parseId(uri) } catch (_: Exception) { System.currentTimeMillis() }
        return MediaItem(
            id = id,
            uri = uri,
            displayName = displayName,
            mimeType = mimeType,
            sizeBytes = bytes.size.toLong(),
            dateTakenMs = System.currentTimeMillis(),
            mediaType = MediaType.PHOTO
        )
    }
}
