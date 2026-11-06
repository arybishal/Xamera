package com.bishal.adaptivecamera.video.export

import com.bishal.adaptivecamera.video.config.VideoBitrateProfile
import com.bishal.adaptivecamera.video.config.VideoCodec
import com.bishal.adaptivecamera.video.config.VideoFrameRate
import com.bishal.adaptivecamera.video.config.VideoResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialExportPresetTest {

    @Test
    fun instagramReelsPreset_hasCorrectConfiguration() {
        val preset = SocialExportPreset.INSTAGRAM_REELS

        assertEquals(VideoResolution.FHD_1080P, preset.targetResolution)
        assertEquals(VideoFrameRate.FPS_30, preset.targetFrameRate)
        assertEquals(VideoBitrateProfile.HIGH, preset.bitrateProfile)
        assertEquals(VideoCodec.H264_AVC, preset.preferredCodec)
        assertEquals(90, preset.maxDurationSec)
        assertTrue(preset.isVertical)
        assertTrue(preset.isDurationConstrained)
    }

    @Test
    fun youtubePreset_hasCorrectConfiguration() {
        val preset = SocialExportPreset.YOUTUBE

        assertEquals(VideoResolution.UHD_4K, preset.targetResolution)
        assertEquals(VideoFrameRate.FPS_60, preset.targetFrameRate)
        assertEquals(VideoBitrateProfile.HIGH, preset.bitrateProfile)
        assertEquals(VideoCodec.H265_HEVC, preset.preferredCodec)
        assertFalse(preset.isVertical)
        assertFalse(preset.isDurationConstrained)
    }

    @Test
    fun whatsappPreset_optimizesForSmallFileSize() {
        val preset = SocialExportPreset.WHATSAPP

        assertEquals(VideoResolution.HD_720P, preset.targetResolution)
        assertEquals(VideoFrameRate.FPS_30, preset.targetFrameRate)
        assertEquals(VideoBitrateProfile.STANDARD, preset.bitrateProfile)
        assertFalse(preset.isVertical)
    }

    @Test
    fun allPresets_haveNonEmptyDescriptions() {
        SocialExportPreset.entries.forEach { preset ->
            assertNotNull(preset.displayName)
            assertTrue(preset.displayName.isNotBlank())
            assertNotNull(preset.description)
            assertTrue(preset.description.isNotBlank())
        }
    }
}
