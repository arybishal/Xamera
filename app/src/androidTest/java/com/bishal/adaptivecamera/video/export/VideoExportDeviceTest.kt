package com.bishal.adaptivecamera.video.export

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bishal.adaptivecamera.core.storage.MediaStoreManager
import com.bishal.adaptivecamera.video.config.VideoBitrateProfile
import com.bishal.adaptivecamera.video.config.VideoCodec
import com.bishal.adaptivecamera.video.config.VideoFrameRate
import com.bishal.adaptivecamera.video.config.VideoResolution
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class VideoExportDeviceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val mediaStoreManager = MediaStoreManager(context)
    private val exportEngine = VideoExportEngine(context, mediaStoreManager)
    private val codecDetector = VideoCodecCapabilityDetector()
    private lateinit var testVideoFile: File

    @Before
    fun setUp() {
        val cacheFile = File(context.cacheDir, "input_test.mp4")
        val candidate = File("/data/local/tmp/test_source.mp4")
        if (candidate.exists() && (!cacheFile.exists() || cacheFile.length() == 0L)) {
            candidate.inputStream().use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        testVideoFile = cacheFile
    }

    @Test
    fun hardwareCodecs_detectedOnSnapdragonPlatform() {
        val supported = codecDetector.getSupportedCodecs()
        assertTrue("Device must support H.264", supported.contains(VideoCodec.H264_AVC))
        assertTrue("Device must support H.265", supported.contains(VideoCodec.H265_HEVC))

        val h264Encoder = codecDetector.findEncoder(VideoCodec.H264_AVC)
        assertNotNull(h264Encoder)
        assertTrue(h264Encoder!!.isHardwareAccelerated)

        val hevcEncoder = codecDetector.findEncoder(VideoCodec.H265_HEVC)
        assertNotNull(hevcEncoder)
        assertTrue(hevcEncoder!!.isHardwareAccelerated)
    }

    @Test
    fun cancellation_stopsExportAndCleansUp() = runBlocking {
        if (!testVideoFile.exists() || testVideoFile.length() == 0L) return@runBlocking

        exportEngine.cancelExport()
        val config = VideoExportConfig(
            sourcePath = testVideoFile.absolutePath,
            sourceDurationMs = 8_400L,
            sourceSizeBytes = testVideoFile.length()
        )

        val result = exportEngine.exportVideo(config)
        assertEquals(VideoExportState.Cancelled, result)
        exportEngine.resetCancel()
    }

    @Test
    fun physicalExport_trimsVideoSuccessfully() = runBlocking {
        if (!testVideoFile.exists() || testVideoFile.length() == 0L) return@runBlocking
        exportEngine.resetCancel()

        val config = VideoExportConfig(
            sourcePath = testVideoFile.absolutePath,
            sourceDurationMs = 8_400L,
            sourceSizeBytes = testVideoFile.length(),
            trimStartMs = 1_000L,
            trimEndMs = 4_000L, // 3 second trim
            resolution = VideoResolution.FHD_1080P,
            frameRate = VideoFrameRate.FPS_30,
            codec = VideoCodec.H264_AVC,
            bitrateProfile = VideoBitrateProfile.STANDARD,
            audioAction = VideoExportAudioAction.KEEP,
            preset = SocialExportPreset.INSTAGRAM_REELS
        )

        val result = exportEngine.exportVideo(config)
        assertTrue("Export must complete successfully: $result", result is VideoExportState.Completed)

        val completed = result as VideoExportState.Completed
        assertTrue("Output size must be greater than 0", completed.outputSizeBytes > 0L)
        assertEquals(3_000L, completed.durationMs)
    }

    @Test
    fun physicalExport_extractsAudioSuccessfully() = runBlocking {
        if (!testVideoFile.exists() || testVideoFile.length() == 0L) return@runBlocking
        exportEngine.resetCancel()

        val config = VideoExportConfig(
            sourcePath = testVideoFile.absolutePath,
            sourceDurationMs = 8_400L,
            sourceSizeBytes = testVideoFile.length(),
            trimStartMs = 0L,
            trimEndMs = 3_000L,
            audioAction = VideoExportAudioAction.EXTRACT_ONLY
        )

        val result = exportEngine.exportVideo(config)
        // If test video has audio track, expect Completed; if video-only, verify clean explanatory Error
        if (result is VideoExportState.Completed) {
            assertEquals("Audio (M4A)", result.resolution)
            assertEquals("AAC", result.codec)
            assertTrue(result.outputSizeBytes > 0L)
        } else if (result is VideoExportState.Error) {
            assertEquals("Source video has no audio track to extract", result.message)
        } else {
            org.junit.Assert.fail("Unexpected state: $result")
        }
    }
}
