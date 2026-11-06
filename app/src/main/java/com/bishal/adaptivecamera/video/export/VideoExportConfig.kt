package com.bishal.adaptivecamera.video.export

import android.net.Uri
import com.bishal.adaptivecamera.video.config.VideoBitrateProfile
import com.bishal.adaptivecamera.video.config.VideoCodec
import com.bishal.adaptivecamera.video.config.VideoFrameRate
import com.bishal.adaptivecamera.video.config.VideoResolution

/**
 * Complete immutable configuration for a video export or transcoding operation.
 */
data class VideoExportConfig(
    val sourceUri: Uri? = null,
    val sourcePath: String? = null,
    val sourceDurationMs: Long = 0L,
    val sourceSizeBytes: Long = 0L,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val resolution: VideoResolution = VideoResolution.FHD_1080P,
    val frameRate: VideoFrameRate = VideoFrameRate.FPS_30,
    val codec: VideoCodec = VideoCodec.H264_AVC,
    val bitrateProfile: VideoBitrateProfile = VideoBitrateProfile.STANDARD,
    val customBitrateMbps: Float? = null,
    val rotationDegrees: Int = 0,
    val audioAction: VideoExportAudioAction = VideoExportAudioAction.KEEP,
    val preset: SocialExportPreset = SocialExportPreset.CUSTOM,
    val lutId: Long? = null,
    val lutIntensity: Float = 1.0f
) {
    /**
     * Effective duration of the exported video in milliseconds.
     */
    val effectiveDurationMs: Long
        get() {
            val end = if (trimEndMs > 0L) trimEndMs else sourceDurationMs
            val dur = end - trimStartMs
            return if (dur > 0L) dur else sourceDurationMs
        }

    /**
     * Effective duration in seconds.
     */
    val effectiveDurationSec: Float
        get() = effectiveDurationMs / 1000f

    /**
     * True if the configuration performs trimming relative to the source duration.
     */
    val isTrimmed: Boolean
        get() = trimStartMs > 0L || (trimEndMs in 1 until sourceDurationMs)

    init {
        require(trimStartMs >= 0L) { "trimStartMs cannot be negative: $trimStartMs" }
        if (trimEndMs > 0L) {
            require(trimEndMs > trimStartMs) {
                "trimEndMs ($trimEndMs) must be strictly greater than trimStartMs ($trimStartMs)"
            }
        }
        require(rotationDegrees in listOf(0, 90, 180, 270)) {
            "rotationDegrees must be one of 0, 90, 180, 270, got $rotationDegrees"
        }
        customBitrateMbps?.let {
            require(it in 0.5f..150.0f) { "customBitrateMbps must be between 0.5 and 150 Mbps, got $it" }
        }
    }
}
