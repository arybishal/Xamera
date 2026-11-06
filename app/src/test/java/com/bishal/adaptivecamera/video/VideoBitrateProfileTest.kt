package com.bishal.adaptivecamera.video

import com.bishal.adaptivecamera.video.config.VideoBitrateProfile
import com.bishal.adaptivecamera.video.config.VideoFrameRate
import com.bishal.adaptivecamera.video.config.VideoResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoBitrateProfileTest {

    @Test
    fun standardProfile_returnsRealisticBitrates() {
        val b4k = VideoBitrateProfile.STANDARD.getTargetBitrateBps(
            VideoResolution.UHD_4K,
            VideoFrameRate.FPS_30
        )
        assertEquals(45_000_000L, b4k)

        val b1080p = VideoBitrateProfile.STANDARD.getTargetBitrateBps(
            VideoResolution.FHD_1080P,
            VideoFrameRate.FPS_30
        )
        assertEquals(16_000_000L, b1080p)

        val b720p = VideoBitrateProfile.STANDARD.getTargetBitrateBps(
            VideoResolution.HD_720P,
            VideoFrameRate.FPS_30
        )
        assertEquals(8_000_000L, b720p)
    }

    @Test
    fun highProfile_providesProfessionalBitrates() {
        val b4kHigh = VideoBitrateProfile.HIGH.getTargetBitrateBps(
            VideoResolution.UHD_4K,
            VideoFrameRate.FPS_30
        )
        assertEquals(80_000_000L, b4kHigh)

        val b1080pHigh = VideoBitrateProfile.HIGH.getTargetBitrateBps(
            VideoResolution.FHD_1080P,
            VideoFrameRate.FPS_30
        )
        assertEquals(35_000_000L, b1080pHigh)
    }

    @Test
    fun allIProfile_providesMaximumIntraFrameBandwidth() {
        val b4kAllI = VideoBitrateProfile.ALL_I.getTargetBitrateBps(
            VideoResolution.UHD_4K,
            VideoFrameRate.FPS_30
        )
        assertEquals(120_000_000L, b4kAllI)
        assertTrue(b4kAllI > VideoBitrateProfile.HIGH.getTargetBitrateBps(VideoResolution.UHD_4K, VideoFrameRate.FPS_30))
    }

    @Test
    fun frameRateScaling_scalesBitrateProportionallyForHighFps() {
        val b1080p30 = VideoBitrateProfile.HIGH.getTargetBitrateBps(
            VideoResolution.FHD_1080P,
            VideoFrameRate.FPS_30
        )
        val b1080p60 = VideoBitrateProfile.HIGH.getTargetBitrateBps(
            VideoResolution.FHD_1080P,
            VideoFrameRate.FPS_60
        )
        assertEquals(b1080p30 * 2, b1080p60)
    }
}
