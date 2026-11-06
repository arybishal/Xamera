package com.bishal.adaptivecamera.video.export

import com.bishal.adaptivecamera.video.config.VideoBitrateProfile
import com.bishal.adaptivecamera.video.config.VideoCodec
import com.bishal.adaptivecamera.video.config.VideoFrameRate
import com.bishal.adaptivecamera.video.config.VideoResolution

/**
 * Predefined export presets tailored for common social media platforms and standard workflows.
 */
enum class SocialExportPreset(
    val displayName: String,
    val description: String,
    val targetResolution: VideoResolution,
    val targetFrameRate: VideoFrameRate,
    val bitrateProfile: VideoBitrateProfile,
    val preferredCodec: VideoCodec,
    val maxDurationSec: Int? = null,
    val isVertical: Boolean = false
) {
    ORIGINAL(
        displayName = "Original / Lossless",
        description = "Preserve original resolution, framerate, and quality profile",
        targetResolution = VideoResolution.FHD_1080P,
        targetFrameRate = VideoFrameRate.FPS_30,
        bitrateProfile = VideoBitrateProfile.STANDARD,
        preferredCodec = VideoCodec.H264_AVC
    ),

    INSTAGRAM_REELS(
        displayName = "Instagram Reels",
        description = "Optimized for Instagram Reels (1080p, 30fps, 9:16 vertical)",
        targetResolution = VideoResolution.FHD_1080P,
        targetFrameRate = VideoFrameRate.FPS_30,
        bitrateProfile = VideoBitrateProfile.HIGH,
        preferredCodec = VideoCodec.H264_AVC,
        maxDurationSec = 90,
        isVertical = true
    ),

    INSTAGRAM_STORY(
        displayName = "Instagram Story",
        description = "Optimized for Instagram Stories (1080p, 30fps, 60s limit)",
        targetResolution = VideoResolution.FHD_1080P,
        targetFrameRate = VideoFrameRate.FPS_30,
        bitrateProfile = VideoBitrateProfile.STANDARD,
        preferredCodec = VideoCodec.H264_AVC,
        maxDurationSec = 60,
        isVertical = true
    ),

    YOUTUBE(
        displayName = "YouTube Landscape",
        description = "High-bitrate landscape video for standard YouTube streaming (4K / 1080p)",
        targetResolution = VideoResolution.UHD_4K,
        targetFrameRate = VideoFrameRate.FPS_60,
        bitrateProfile = VideoBitrateProfile.HIGH,
        preferredCodec = VideoCodec.H265_HEVC
    ),

    YOUTUBE_SHORTS(
        displayName = "YouTube Shorts",
        description = "Vertical high-quality short video for YouTube Shorts (1080p, 60s max)",
        targetResolution = VideoResolution.FHD_1080P,
        targetFrameRate = VideoFrameRate.FPS_60,
        bitrateProfile = VideoBitrateProfile.HIGH,
        preferredCodec = VideoCodec.H264_AVC,
        maxDurationSec = 60,
        isVertical = true
    ),

    TIKTOK(
        displayName = "TikTok",
        description = "Standard vertical profile for TikTok upload (1080p, 30fps)",
        targetResolution = VideoResolution.FHD_1080P,
        targetFrameRate = VideoFrameRate.FPS_30,
        bitrateProfile = VideoBitrateProfile.HIGH,
        preferredCodec = VideoCodec.H264_AVC,
        isVertical = true
    ),

    WHATSAPP(
        displayName = "WhatsApp / Messaging",
        description = "Compressed for rapid messaging and quick sharing (720p, smaller file size)",
        targetResolution = VideoResolution.HD_720P,
        targetFrameRate = VideoFrameRate.FPS_30,
        bitrateProfile = VideoBitrateProfile.STANDARD,
        preferredCodec = VideoCodec.H264_AVC
    ),

    CUSTOM(
        displayName = "Custom Settings",
        description = "Fully user-configurable resolution, framerate, codec, and bitrate",
        targetResolution = VideoResolution.FHD_1080P,
        targetFrameRate = VideoFrameRate.FPS_30,
        bitrateProfile = VideoBitrateProfile.STANDARD,
        preferredCodec = VideoCodec.H264_AVC
    );

    val isDurationConstrained: Boolean
        get() = maxDurationSec != null
}
