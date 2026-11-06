package com.bishal.adaptivecamera.video.export

import com.bishal.adaptivecamera.video.config.VideoBitrateProfile
import com.bishal.adaptivecamera.video.config.VideoCodec
import com.bishal.adaptivecamera.video.config.VideoFrameRate
import com.bishal.adaptivecamera.video.config.VideoResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoExportConfigTest {

    @Test
    fun defaultConfiguration_hasExpectedValues() {
        val config = VideoExportConfig(sourcePath = "/storage/test.mp4")

        assertEquals(0L, config.trimStartMs)
        assertEquals(0L, config.trimEndMs)
        assertEquals(VideoResolution.FHD_1080P, config.resolution)
        assertEquals(VideoFrameRate.FPS_30, config.frameRate)
        assertEquals(VideoCodec.H264_AVC, config.codec)
        assertEquals(VideoBitrateProfile.STANDARD, config.bitrateProfile)
        assertEquals(VideoExportAudioAction.KEEP, config.audioAction)
        assertEquals(SocialExportPreset.CUSTOM, config.preset)
        assertEquals(0, config.rotationDegrees)
        assertFalse(config.isTrimmed)
    }

    @Test
    fun effectiveDuration_calculatesCorrectly() {
        val untrimmed = VideoExportConfig(
            sourcePath = "/storage/test.mp4",
            sourceDurationMs = 15_000L
        )
        assertEquals(15_000L, untrimmed.effectiveDurationMs)
        assertEquals(15.0f, untrimmed.effectiveDurationSec, 0.001f)
        assertFalse(untrimmed.isTrimmed)

        val trimmed = VideoExportConfig(
            sourcePath = "/storage/test.mp4",
            sourceDurationMs = 15_000L,
            trimStartMs = 2_000L,
            trimEndMs = 8_000L
        )
        assertEquals(6_000L, trimmed.effectiveDurationMs)
        assertEquals(6.0f, trimmed.effectiveDurationSec, 0.001f)
        assertTrue(trimmed.isTrimmed)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidTrim_throwsExceptionWhenEndLessThanStart() {
        VideoExportConfig(
            sourcePath = "/storage/test.mp4",
            sourceDurationMs = 10_000L,
            trimStartMs = 5_000L,
            trimEndMs = 3_000L
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidRotation_throwsException() {
        VideoExportConfig(
            sourcePath = "/storage/test.mp4",
            rotationDegrees = 45
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidCustomBitrate_throwsExceptionWhenOutOfBounds() {
        VideoExportConfig(
            sourcePath = "/storage/test.mp4",
            customBitrateMbps = 250.0f
        )
    }
}
