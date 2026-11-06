package com.bishal.adaptivecamera.video

import com.bishal.adaptivecamera.video.config.VideoBitrateProfile
import com.bishal.adaptivecamera.video.config.VideoCodec
import com.bishal.adaptivecamera.video.config.VideoConfig
import com.bishal.adaptivecamera.video.config.VideoConfigValidationResult
import com.bishal.adaptivecamera.video.config.VideoExposureMode
import com.bishal.adaptivecamera.video.config.VideoFrameRate
import com.bishal.adaptivecamera.video.config.VideoResolution
import com.bishal.adaptivecamera.video.config.VideoStabilizationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoConfigTest {

    @Test
    fun defaultConfiguration_isValid() {
        val config = VideoConfig()
        assertEquals(VideoResolution.FHD_1080P, config.resolution)
        assertEquals(VideoFrameRate.FPS_30, config.frameRate)
        assertEquals(VideoCodec.H264_AVC, config.videoCodec)
        assertEquals(VideoExposureMode.AUTO, config.exposureMode)
        assertEquals(180f, config.shutterAngle)
        assertTrue(config.audioEnabled)
        assertEquals(VideoConfigValidationResult.Valid, config.validate())
    }

    @Test
    fun effectiveShutterSpeed_calculatesAccuratelyFromShutterAngle() {
        // 30 FPS, 180° shutter angle -> 1/60s
        val config30Fps = VideoConfig(
            frameRate = VideoFrameRate.FPS_30,
            shutterAngle = 180f
        )
        val speed30 = config30Fps.effectiveShutterSpeedSeconds
        assertNotNull(speed30)
        assertEquals(1.0 / 60.0, speed30!!, 1e-6)
        assertEquals("1/60s", config30Fps.formattedShutterSpeed)

        // 24 FPS, 180° shutter angle -> 1/48s
        val config24Fps = VideoConfig(
            frameRate = VideoFrameRate.FPS_24,
            shutterAngle = 180f
        )
        val speed24 = config24Fps.effectiveShutterSpeedSeconds
        assertNotNull(speed24)
        assertEquals(1.0 / 48.0, speed24!!, 1e-6)
        assertEquals("1/48s", config24Fps.formattedShutterSpeed)

        // 60 FPS, 90° shutter angle -> 1/240s
        val config60Fps = VideoConfig(
            frameRate = VideoFrameRate.FPS_60,
            shutterAngle = 90f
        )
        val speed60 = config60Fps.effectiveShutterSpeedSeconds
        assertNotNull(speed60)
        assertEquals(1.0 / 240.0, speed60!!, 1e-6)
        assertEquals("1/240s", config60Fps.formattedShutterSpeed)
    }

    @Test
    fun effectiveShutterSpeed_manualSpeedOverridesShutterAngle() {
        val config = VideoConfig(
            frameRate = VideoFrameRate.FPS_30,
            shutterAngle = 180f,
            manualShutterSpeedSeconds = 1.0 / 125.0
        )
        assertEquals(1.0 / 125.0, config.effectiveShutterSpeedSeconds!!, 1e-6)
        assertEquals("1/125s", config.formattedShutterSpeed)
    }

    @Test
    fun effectiveShutterSpeed_nullWhenAutoWithoutAngle() {
        val config = VideoConfig(shutterAngle = null, manualShutterSpeedSeconds = null)
        assertNull(config.effectiveShutterSpeedSeconds)
        assertEquals("Auto", config.formattedShutterSpeed)
    }

    @Test
    fun syncBitrate_updatesTargetBitrateForResolutionAndFps() {
        val config = VideoConfig(
            resolution = VideoResolution.UHD_4K,
            frameRate = VideoFrameRate.FPS_60,
            bitrateProfile = VideoBitrateProfile.HIGH
        ).syncBitrate()

        // 4K High base is 80Mbps, scaled for 60fps (2x) = 160Mbps
        assertEquals(160_000_000L, config.targetBitrateBps)
    }

    @Test
    fun validation_detectsOutOfBoundsParameters() {
        // Bitrate too low
        val invalidBitrate = VideoConfig(targetBitrateBps = 100_000L).validate()
        assertTrue(invalidBitrate is VideoConfigValidationResult.Invalid)

        // Invalid shutter angle
        val invalidAngle = VideoConfig(shutterAngle = 400f).validate()
        assertTrue(invalidAngle is VideoConfigValidationResult.Invalid)

        // Invalid Kelvin
        val invalidKelvin = VideoConfig(manualKelvin = 12000).validate()
        assertTrue(invalidKelvin is VideoConfigValidationResult.Invalid)

        // Invalid ISO
        val invalidIso = VideoConfig(manualIso = 50000).validate()
        assertTrue(invalidIso is VideoConfigValidationResult.Invalid)
    }

    @Test
    fun badgeLabel_formatsAccurately() {
        val c1 = VideoConfig(resolution = VideoResolution.UHD_4K, frameRate = VideoFrameRate.FPS_30)
        assertEquals("4K · 30", c1.badgeLabel)

        val c2 = VideoConfig(resolution = VideoResolution.FHD_1080P, frameRate = VideoFrameRate.FPS_60)
        assertEquals("1080p · 60", c2.badgeLabel)
    }
}
