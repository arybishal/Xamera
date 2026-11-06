package com.bishal.adaptivecamera.video.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FFmpegExecutionAuditTest {

    @Test
    fun testFFmpegExecutionMethodClassification() {
        val binaryMethod = FFmpegExecutionMethod.FFMPEG_CLI_BINARY
        val mediaCodecMethod = FFmpegExecutionMethod.MEDIA_CODEC_HARDWARE_FALLBACK

        assertEquals("FFmpeg CLI Native Binary", binaryMethod.displayName)
        assertEquals("Android Native MediaCodec Hardware Pipeline", mediaCodecMethod.displayName)
    }

    @Test
    fun testExecutionPlanDataContract() {
        val plan = FFmpegExecutionPlan(
            commandConstructed = true,
            commandLine = "ffmpeg -i input.mp4 output.mp4",
            binaryAvailable = false,
            executionMethod = FFmpegExecutionMethod.MEDIA_CODEC_HARDWARE_FALLBACK,
            binaryPath = null
        )

        assertTrue(plan.commandConstructed)
        assertFalse(plan.binaryAvailable)
        assertEquals(FFmpegExecutionMethod.MEDIA_CODEC_HARDWARE_FALLBACK, plan.executionMethod)
        assertEquals("ffmpeg -i input.mp4 output.mp4", plan.commandLine)
    }

    @Test
    fun testCommandBuilderConstructsValidCliArguments() {
        val tempLut = java.io.File.createTempFile("test_lut", ".cube")
        tempLut.deleteOnExit()

        val options = FFmpegVideoOptions(
            inputPath = "/sdcard/DCIM/source.mp4",
            outputPath = "/sdcard/DCIM/exported.mp4",
            codec = "libx265",
            lutFilePath = tempLut.absolutePath,
            anamorphicFactor = 1.33f,
            muteAudio = true
        )

        val commandLine = FFmpegCommandBuilder.buildCommandLineString(options)
        assertNotNull(commandLine)
        assertTrue(commandLine.contains("-i /sdcard/DCIM/source.mp4"))
        assertTrue(commandLine.contains("libx265"))
        assertTrue(commandLine.contains("scale=iw*1.33:ih"))
        assertTrue(commandLine.contains("-an"))
    }
}
