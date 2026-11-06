package com.bishal.adaptivecamera.video.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FFmpegCommandBuilderTest {

    @Test
    fun testBasicTranscodeCommand() {
        val options = FFmpegVideoOptions(
            inputPath = "/storage/test_input.mp4",
            outputPath = "/storage/test_output.mp4",
            codec = "libx265",
            crf = 20,
            fastStart = true
        )

        val cmd = FFmpegCommandBuilder.buildCommand(options)

        assertTrue(cmd.contains("-i"))
        assertTrue(cmd.contains("/storage/test_input.mp4"))
        assertTrue(cmd.contains("-c:v"))
        assertTrue(cmd.contains("libx265"))
        assertTrue(cmd.contains("hvc1")) // Apple compatibility tag
        assertTrue(cmd.contains("+faststart"))
        assertEquals("/storage/test_output.mp4", cmd.last())
    }

    @Test
    fun testAnamorphicDeSqueezeFilter() {
        val options = FFmpegVideoOptions(
            inputPath = "/storage/input.mp4",
            outputPath = "/storage/output.mp4",
            anamorphicFactor = 1.33f
        )

        val cmd = FFmpegCommandBuilder.buildCommand(options)
        assertTrue(cmd.contains("-vf"))
        val vfIndex = cmd.indexOf("-vf")
        val filterString = cmd[vfIndex + 1]

        assertTrue(filterString.contains("scale=iw*1.33:ih"))
    }

    @Test
    fun testAudioMutingOption() {
        val options = FFmpegVideoOptions(
            inputPath = "/storage/input.mp4",
            outputPath = "/storage/output.mp4",
            muteAudio = true
        )

        val cmd = FFmpegCommandBuilder.buildCommand(options)
        assertTrue(cmd.contains("-an"))
    }

    @Test
    fun testTrimmingOptions() {
        val options = FFmpegVideoOptions(
            inputPath = "/storage/input.mp4",
            outputPath = "/storage/output.mp4",
            startTrimSec = 2.5f,
            endTrimSec = 7.5f
        )

        val cmd = FFmpegCommandBuilder.buildCommand(options)
        assertTrue(cmd.contains("-ss"))
        assertTrue(cmd.contains("2.500"))
        assertTrue(cmd.contains("-t"))
        assertTrue(cmd.contains("5.000"))
    }
}
