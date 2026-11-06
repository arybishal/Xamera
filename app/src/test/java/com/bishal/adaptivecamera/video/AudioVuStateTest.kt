package com.bishal.adaptivecamera.video

import com.bishal.adaptivecamera.video.audio.AudioRecordingManager
import com.bishal.adaptivecamera.video.audio.AudioVuState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioVuStateTest {

    @Test
    fun amplitudeToDbfs_computesAccurateLevels() {
        // Full scale (32767) -> 0.0 dBFS
        val fullScale = AudioRecordingManager.amplitudeToDbfs(32767)
        assertEquals(0.0f, fullScale, 0.1f)

        // Half scale (~16384) -> -6.0 dBFS
        val halfScale = AudioRecordingManager.amplitudeToDbfs(16384)
        assertEquals(-6.0f, halfScale, 0.2f)

        // Quarter scale (~8192) -> -12.0 dBFS
        val quarterScale = AudioRecordingManager.amplitudeToDbfs(8192)
        assertEquals(-12.0f, quarterScale, 0.2f)

        // Zero amplitude -> -60.0 dBFS
        val zero = AudioRecordingManager.amplitudeToDbfs(0)
        assertEquals(-60.0f, zero, 0.01f)
    }

    @Test
    fun normalizedFractions_mapsDbfsToUnitInterval() {
        // -60 dBFS -> fraction 0.0
        val silence = AudioVuState(-60f, -60f)
        assertEquals(0.0f, silence.leftFraction, 0.01f)
        assertEquals(0.0f, silence.rightFraction, 0.01f)

        // 0 dBFS -> fraction 1.0
        val peak = AudioVuState(0f, 0f)
        assertEquals(1.0f, peak.leftFraction, 0.01f)
        assertEquals(1.0f, peak.rightFraction, 0.01f)

        // -30 dBFS -> fraction 0.5
        val mid = AudioVuState(-30f, -30f)
        assertEquals(0.5f, mid.leftFraction, 0.01f)
        assertEquals(0.5f, mid.rightFraction, 0.01f)
    }

    @Test
    fun clippingState_triggersAtNearZeroDbfs() {
        val nonClipping = AudioVuState(leftChannelDbfs = -3.0f, isClipping = false)
        assertFalse(nonClipping.isClipping)

        val clipping = AudioVuState(leftChannelDbfs = -0.2f, isClipping = true)
        assertTrue(clipping.isClipping)
    }

    @Test
    fun mutedState_reportsZeroFractions() {
        val muted = AudioVuState.MUTED
        assertTrue(muted.isMuted)
        assertEquals(0.0f, muted.leftFraction, 0.01f)
        assertEquals(0.0f, muted.rightFraction, 0.01f)
    }
}
