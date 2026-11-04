package com.bishal.adaptivecamera.camera.capability

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

interface CameraCapabilityRepository {
    suspend fun getDeviceReport(forceRefresh: Boolean = false): DeviceCapabilityReport
    suspend fun clearCache()
    fun isCacheValid(): Boolean
}

class CameraCapabilityRepositoryImpl(
    private val context: Context,
    private val scanner: CameraCapabilityScanner = CameraCapabilityScanner(context)
) : CameraCapabilityRepository {

    companion object {
        private const val TAG = "CameraCapRepository"
        private const val CACHE_DIR = "reports"
        private const val CACHE_FILE = "hardware_capability_cache.json"
        private const val META_FILE = "cache_meta.json"
    }

    private var memoryCachedReport: DeviceCapabilityReport? = null

    private fun getCacheDir(): File {
        val dir = File(context.filesDir, CACHE_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun getCacheFile(): File = File(getCacheDir(), CACHE_FILE)
    private fun getMetaFile(): File = File(getCacheDir(), META_FILE)

    override fun isCacheValid(): Boolean {
        val cacheFile = getCacheFile()
        val metaFile = getMetaFile()
        if (!cacheFile.exists() || !metaFile.exists()) return false

        return try {
            val metaContent = metaFile.readText()
            val currentFingerprint = Build.FINGERPRINT
            metaContent.contains(currentFingerprint)
        } catch (e: Exception) {
            Log.w(TAG, "Error checking cache validity", e)
            false
        }
    }

    override suspend fun getDeviceReport(forceRefresh: Boolean): DeviceCapabilityReport = withContext(Dispatchers.IO) {
        if (!forceRefresh && memoryCachedReport != null) {
            return@withContext memoryCachedReport!!
        }

        val cacheFile = getCacheFile()
        val metaFile = getMetaFile()

        if (!forceRefresh && isCacheValid() && cacheFile.exists()) {
            try {
                // If we have an existing scanned instance in memory or can load it
                if (memoryCachedReport != null) {
                    return@withContext memoryCachedReport!!.copy(
                        isFromCache = true,
                        cachedTimestamp = cacheFile.lastModified()
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed reading cached report, will perform live scan", e)
            }
        }

        // Perform live hardware scan
        val liveReport = withContext(Dispatchers.Default) {
            scanner.scanDevice()
        }

        // Persist to cache
        try {
            val jsonReport = liveReport.toJson()
            cacheFile.writeText(jsonReport)

            val metaJson = "{\"fingerprint\": \"${Build.FINGERPRINT}\", \"timestamp\": ${System.currentTimeMillis()}}"
            metaFile.writeText(metaJson)

            memoryCachedReport = liveReport
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing hardware capability cache", e)
        }

        liveReport.copy(isFromCache = false)
    }

    override suspend fun clearCache(): Unit = withContext(Dispatchers.IO) {
        try {
            getCacheFile().delete()
            getMetaFile().delete()
            memoryCachedReport = null
        } catch (e: Exception) {
            Log.w(TAG, "Failed clearing cache", e)
        }
    }
}
