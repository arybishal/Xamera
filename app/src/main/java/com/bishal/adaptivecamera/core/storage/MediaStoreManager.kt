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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MediaStoreManager(private val context: Context) {

    companion object {
        const val ALBUM_DIRECTORY = "DCIM/AdaptiveCamera"
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
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%$ALBUM_DIRECTORY%")
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
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%$ALBUM_DIRECTORY%")
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
}
