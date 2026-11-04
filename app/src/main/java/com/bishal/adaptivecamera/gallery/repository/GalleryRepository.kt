package com.bishal.adaptivecamera.gallery.repository

import android.content.Context
import com.bishal.adaptivecamera.core.storage.MediaItem
import com.bishal.adaptivecamera.core.storage.MediaStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GalleryRepository(
    private val context: Context,
    private val mediaStoreManager: MediaStoreManager = MediaStoreManager(context)
) {
    suspend fun getRecentMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        mediaStoreManager.queryRecentMedia(limit = 200)
    }

    suspend fun getLatestMedia(): MediaItem? = withContext(Dispatchers.IO) {
        mediaStoreManager.queryLatestMedia()
    }

    suspend fun deleteMedia(item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        mediaStoreManager.deleteMedia(item)
    }
}
