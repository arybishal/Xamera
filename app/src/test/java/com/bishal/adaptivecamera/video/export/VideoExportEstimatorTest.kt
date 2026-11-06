package com.bishal.adaptivecamera.video.export

import com.bishal.adaptivecamera.video.config.VideoBitrateProfile
import com.bishal.adaptivecamera.video.config.VideoCodec
import com.bishal.adaptivecamera.video.config.VideoFrameRate
import com.bishal.adaptivecamera.video.config.VideoResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoExportEstimatorTest {

    @Test
    fun resolveTargetBitrate_scalesWithResolution() {
        val bitrate4k = VideoExportEstimator.resolveTargetBitrateBps(
            resolution = VideoResolution.UHD_4K,
            frameRate = VideoFrameRate.FPS_30,
            codec = VideoCodec.H264_AVC,
            bitrateProfile = VideoBitrateProfile.STANDARD
        )

        val bitrate1080p = VideoExportEstimator.resolveTargetBitrateBps(
            resolution = VideoResolution.FHD_1080P,
            frameRate = VideoFrameRate.FPS_30,
            codec = VideoCodec.H264_AVC,
            bitrateProfile = VideoBitrateProfile.STANDARD
        )

        val bitrate720p = VideoExportEstimator.resolveTargetBitrateBps(
            resolution = VideoResolution.HD_720P,
            frameRate = VideoFrameRate.FPS_30,
            codec = VideoCodec.H264_AVC,
            bitrateProfile = VideoBitrateProfile.STANDARD
        )

        assertTrue("4K bitrate must be higher than 1080p", bitrate4k > bitrate1080p)
        assertTrue("1080p bitrate must be higher than 720p", bitrate1080p > bitrate720p)
    }

    @Test
    fun resolveTargetBitrate_codecEfficiencyDiscount() {
        val bitrateH264 = VideoExportEstimator.resolveTargetBitrateBps(
            resolution = VideoResolution.FHD_1080P,
            frameRate = VideoFrameRate.FPS_30,
            codec = VideoCodec.H264_AVC,
            bitrateProfile = VideoBitrateProfile.STANDARD
        )

        val bitrateH265 = VideoExportEstimator.resolveTargetBitrateBps(
            resolution = VideoResolution.FHD_1080P,
            frameRate = VideoFrameRate.FPS_30,
            codec = VideoCodec.H265_HEVC,
            bitrateProfile = VideoBitrateProfile.STANDARD
        )

        assertTrue("HEVC must produce lower bitrate target than H.264 for equivalent quality", bitrateH265 < bitrateH264)
    }

    @Test
    fun resolveTargetBitrate_customBitrateOverridesCalculations() {
        val customBitrate = VideoExportEstimator.resolveTargetBitrateBps(
            resolution = VideoResolution.FHD_1080P,
            frameRate = VideoFrameRate.FPS_30,
            codec = VideoCodec.H264_AVC,
            bitrateProfile = VideoBitrateProfile.STANDARD,
            customBitrateMbps = 25.0f
        )

        assertEquals(25_000_000, customBitrate)
    }

    @Test
    fun estimateExportSizeBytes_scalesWithDuration() {
        val config10s = VideoExportConfig(
            sourcePath = "/storage/test.mp4",
            sourceDurationMs = 10_000L
        )

        val config20s = VideoExportConfig(
            sourcePath = "/storage/test.mp4",
            sourceDurationMs = 20_000L
        )

        val size10s = VideoExportEstimator.estimateExportSizeBytes(config10s)
        val size20s = VideoExportEstimator.estimateExportSizeBytes(config20s)

        assertTrue("20s video must be estimated larger than 10s video", size20s > size10s)
    }

    @Test
    fun formatFileSize_formatsCorrectUnits() {
        assertEquals("500 B", VideoExportEstimator.formatFileSize(500L))
        assertEquals("50 KB", VideoExportEstimator.formatFileSize(50 * 1024L))
        assertEquals("15.5 MB", VideoExportEstimator.formatFileSize((15.5 * 1024 * 1024).toLong()))
        assertEquals("1.25 GB", VideoExportEstimator.formatFileSize((1.25 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun formatDuration_formatsCorrectUnits() {
        assertEquals("00:15.50", VideoExportEstimator.formatDuration(15_500L))
        assertEquals("01:30.00", VideoExportEstimator.formatDuration(90_000L))
        assertEquals("01:05:20", VideoExportEstimator.formatDuration(3_920_000L))
    }
}
