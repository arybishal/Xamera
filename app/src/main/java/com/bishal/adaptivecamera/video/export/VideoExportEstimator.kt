package com.bishal.adaptivecamera.video.export

import com.bishal.adaptivecamera.video.config.VideoBitrateProfile
import com.bishal.adaptivecamera.video.config.VideoCodec
import com.bishal.adaptivecamera.video.config.VideoFrameRate
import com.bishal.adaptivecamera.video.config.VideoResolution
import java.util.Locale

/**
 * Utility for estimating video export bitrate, file size, and duration metrics.
 */
object VideoExportEstimator {

    private const val AUDIO_BITRATE_BPS = 128_000 // 128 kbps AAC stereo

    /**
     * Resolves the target video bitrate in bits per second.
     */
    fun resolveTargetBitrateBps(
        resolution: VideoResolution,
        frameRate: VideoFrameRate,
        codec: VideoCodec,
        bitrateProfile: VideoBitrateProfile,
        customBitrateMbps: Float? = null
    ): Int {
        if (customBitrateMbps != null && customBitrateMbps > 0f) {
            return (customBitrateMbps * 1_000_000).toInt()
        }

        // Base bitrate in bps based on resolution at 30fps
        val baseBps = when (resolution) {
            VideoResolution.UHD_4K -> 45_000_000
            VideoResolution.FHD_1080P -> 16_000_000
            VideoResolution.HD_720P -> 8_000_000
            VideoResolution.SD_480P -> 3_000_000
        }

        // Profile scaling factor
        val profileScale = when (bitrateProfile) {
            VideoBitrateProfile.ALL_I -> 2.5f
            VideoBitrateProfile.HIGH -> 1.5f
            VideoBitrateProfile.STANDARD -> 1.0f
            VideoBitrateProfile.CUSTOM -> 1.2f
        }

        // Frame rate multiplier (60fps needs ~30% more bitrate than 30fps)
        val fpsScale = when (frameRate) {
            VideoFrameRate.FPS_24, VideoFrameRate.FPS_25 -> 0.9f
            VideoFrameRate.FPS_30 -> 1.0f
            VideoFrameRate.FPS_48, VideoFrameRate.FPS_50 -> 1.25f
            VideoFrameRate.FPS_60 -> 1.35f
            VideoFrameRate.FPS_120 -> 2.0f
            VideoFrameRate.FPS_240 -> 3.0f
        }

        // Codec efficiency discount (HEVC is ~35% more efficient than H.264, AV1 is ~45% more efficient)
        val codecScale = when (codec) {
            VideoCodec.H264_AVC -> 1.0f
            VideoCodec.H265_HEVC -> 0.70f
            VideoCodec.AV1 -> 0.60f
        }

        return (baseBps * profileScale * fpsScale * codecScale).toInt()
    }

    /**
     * Estimates output file size in bytes for the given export configuration.
     */
    fun estimateExportSizeBytes(config: VideoExportConfig): Long {
        if (config.audioAction == VideoExportAudioAction.EXTRACT_ONLY) {
            val audioBytes = (AUDIO_BITRATE_BPS.toDouble() * config.effectiveDurationSec / 8.0).toLong()
            return audioBytes + 16_384 // container overhead
        }

        val videoBitrateBps = resolveTargetBitrateBps(
            resolution = config.resolution,
            frameRate = config.frameRate,
            codec = config.codec,
            bitrateProfile = config.bitrateProfile,
            customBitrateMbps = config.customBitrateMbps
        )

        val audioBitrateBps = if (config.audioAction == VideoExportAudioAction.KEEP) AUDIO_BITRATE_BPS else 0
        val totalBitrateBps = videoBitrateBps + audioBitrateBps
        val rawBytes = (totalBitrateBps.toDouble() * config.effectiveDurationSec / 8.0).toLong()

        // Container metadata overhead (~32 KB)
        return (rawBytes + 32_768).coerceAtLeast(1_024L)
    }

    /**
     * Formats bytes into a human-readable size string (e.g. "14.2 MB", "1.1 GB").
     */
    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824L -> String.format(Locale.US, "%.2f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576L -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
            bytes >= 1_024L -> String.format(Locale.US, "%.0f KB", bytes / 1_024.0)
            else -> "$bytes B"
        }
    }

    /**
     * Formats duration in milliseconds to MM:SS.SS or HH:MM:SS.
     */
    fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        val millis = (ms % 1000) / 10

        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, millis)
        }
    }
}
