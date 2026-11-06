package com.bishal.adaptivecamera.video

import com.bishal.adaptivecamera.video.recording.VideoRecordingState
import com.bishal.adaptivecamera.video.recording.VideoRecordingStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoRecordingStateTest {

    @Test
    fun formattedDuration_formatsStandardSecondsAndMinutes() {
        val s0 = VideoRecordingState(durationMs = 0L)
        assertEquals("00:00", s0.formattedDuration)

        val s5 = VideoRecordingState(durationMs = 5_000L)
        assertEquals("00:05", s5.formattedDuration)

        val s65 = VideoRecordingState(durationMs = 65_000L)
        assertEquals("01:05", s65.formattedDuration)

        val s125 = VideoRecordingState(durationMs = 125_000L)
        assertEquals("02:05", s125.formattedDuration)
    }

    @Test
    fun formattedDuration_formatsHoursWhenExceeding60Minutes() {
        val s1Hour = VideoRecordingState(durationMs = 3665_000L) // 1h 1m 5s
        assertEquals("01:01:05", s1Hour.formattedDuration)
    }

    @Test
    fun formattedSize_formatsMegabytesAndGigabytes() {
        val sMb = VideoRecordingState(bytesRecorded = 25_000_000L)
        assertTrue(sMb.formattedSize.endsWith("MB"))

        val sGb = VideoRecordingState(bytesRecorded = 2_500_000_000L)
        assertTrue(sGb.formattedSize.endsWith("GB"))
    }

    @Test
    fun statusProperties_reflectRecordingStatusAccurately() {
        val idle = VideoRecordingState(status = VideoRecordingStatus.IDLE)
        assertFalse(idle.status.isRecordingOrPaused)
        assertFalse(idle.status.isActivelyWriting)

        val recording = VideoRecordingState(status = VideoRecordingStatus.RECORDING)
        assertTrue(recording.status.isRecordingOrPaused)
        assertTrue(recording.status.isActivelyWriting)

        val paused = VideoRecordingState(status = VideoRecordingStatus.PAUSED)
        assertTrue(paused.status.isRecordingOrPaused)
        assertFalse(paused.status.isActivelyWriting)
    }
}
