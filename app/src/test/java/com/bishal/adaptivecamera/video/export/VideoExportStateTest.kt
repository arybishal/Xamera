package com.bishal.adaptivecamera.video.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoExportStateTest {

    @Test
    fun processingState_calculatesPercentCorrectly() {
        val state1 = VideoExportState.Processing(
            progress = 0.456f,
            currentBytes = 4_500_000L,
            totalEstimatedBytes = 10_000_000L,
            elapsedMs = 4_000L,
            etaMs = 5_000L
        )

        assertEquals(45, state1.percent)

        val stateClamp = VideoExportState.Processing(
            progress = 1.2f,
            currentBytes = 12_000_000L,
            totalEstimatedBytes = 10_000_000L,
            elapsedMs = 10_000L,
            etaMs = 0L
        )
        assertEquals(100, stateClamp.percent)
    }

    @Test
    fun completedState_containsOutputInformation() {
        val state = VideoExportState.Completed(
            outputUri = null,
            outputPath = "/storage/emulated/0/Movies/AdaptiveCamera/export.mp4",
            outputSizeBytes = 12_500_000L,
            durationMs = 8_400L,
            resolution = "1920x1080",
            codec = "H.264"
        )

        assertEquals(null, state.outputUri)
        assertEquals(12_500_000L, state.outputSizeBytes)
        assertEquals(8_400L, state.durationMs)
        assertEquals("1920x1080", state.resolution)
        assertEquals("H.264", state.codec)
    }

    @Test
    fun errorState_carriesMessageAndCause() {
        val ex = IllegalStateException("Test failure")
        val state = VideoExportState.Error("Failed to transcode", ex)

        assertEquals("Failed to transcode", state.message)
        assertEquals(ex, state.cause)
    }
}
