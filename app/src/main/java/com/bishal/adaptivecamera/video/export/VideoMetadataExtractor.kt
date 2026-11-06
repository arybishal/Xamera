package com.bishal.adaptivecamera.video.export

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.bishal.adaptivecamera.video.config.VideoCodec
import com.bishal.adaptivecamera.video.config.VideoFrameRate
import com.bishal.adaptivecamera.video.config.VideoResolution

/**
 * Metadata extracted from a source video file.
 */
data class SourceVideoMetadata(
    val uri: Uri,
    val path: String?,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val rotationDegrees: Int,
    val bitrateBps: Long,
    val frameRate: Float,
    val mimeType: String?,
    val hasAudio: Boolean,
    val audioMimeType: String?,
    val audioChannels: Int,
    val audioSampleRateHz: Int,
    val fileSizeBytes: Long
) {
    val durationSec: Float
        get() = durationMs / 1000f

    val isPortrait: Boolean
        get() = (rotationDegrees == 90 || rotationDegrees == 270) || (width < height)

    val closestResolution: VideoResolution
        get() {
            val maxDim = maxOf(width, height)
            return when {
                maxDim >= 3800 -> VideoResolution.UHD_4K
                maxDim >= 1800 -> VideoResolution.FHD_1080P
                maxDim >= 1200 -> VideoResolution.HD_720P
                else -> VideoResolution.SD_480P
            }
        }

    val closestFrameRate: VideoFrameRate
        get() {
            return when {
                frameRate >= 100f -> VideoFrameRate.FPS_120
                frameRate >= 50f -> VideoFrameRate.FPS_60
                frameRate >= 27f -> VideoFrameRate.FPS_30
                else -> VideoFrameRate.FPS_24
            }
        }

    val detectedCodec: VideoCodec
        get() {
            return when {
                mimeType?.contains("hevc", ignoreCase = true) == true -> VideoCodec.H265_HEVC
                mimeType?.contains("av01", ignoreCase = true) == true -> VideoCodec.AV1
                else -> VideoCodec.H264_AVC
            }
        }
}

/**
 * Utility for extracting metadata from a media source.
 */
object VideoMetadataExtractor {

    fun extract(context: Context, uri: Uri, fileSizeBytes: Long = 0L): SourceVideoMetadata {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)

            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 1920
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 1080
            val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull() ?: 0L
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            val hasAudioStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
            val hasAudio = hasAudioStr.equals("yes", ignoreCase = true)

            // Extract frame rate (available on API 23+)
            val fpsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)
            val frameRate = fpsStr?.toFloatOrNull() ?: 30.0f

            return SourceVideoMetadata(
                uri = uri,
                path = uri.path,
                durationMs = durationMs,
                width = width,
                height = height,
                rotationDegrees = rotation,
                bitrateBps = bitrate,
                frameRate = frameRate,
                mimeType = mimeType,
                hasAudio = hasAudio,
                audioMimeType = if (hasAudio) "audio/mp4a-latm" else null,
                audioChannels = if (hasAudio) 2 else 0,
                audioSampleRateHz = if (hasAudio) 48000 else 0,
                fileSizeBytes = fileSizeBytes
            )
        } catch (e: Exception) {
            // Safe fallback defaults
            return SourceVideoMetadata(
                uri = uri,
                path = uri.path,
                durationMs = 0L,
                width = 1920,
                height = 1080,
                rotationDegrees = 0,
                bitrateBps = 16_000_000L,
                frameRate = 30.0f,
                mimeType = "video/avc",
                hasAudio = true,
                audioMimeType = "audio/mp4a-latm",
                audioChannels = 2,
                audioSampleRateHz = 48000,
                fileSizeBytes = fileSizeBytes
            )
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }
}
