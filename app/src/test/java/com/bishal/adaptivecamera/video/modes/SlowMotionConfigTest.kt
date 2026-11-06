package com.bishal.adaptivecamera.video.modes

import org.junit.Assert.assertEquals
import org.junit.Test

class SlowMotionConfigTest {

    @Test
    fun testSlowMotionDurationExpansion() {
        // 5 seconds at 120 fps plays for 20 seconds at 30 fps (4x slow motion)
        val duration120 = SlowMotionCaptureFps.FPS_120.calculateOutputDuration(5.0f, 30)
        assertEquals(20.0f, duration120, 0.001f)

        // 5 seconds at 240 fps plays for 40 seconds at 30 fps (8x slow motion)
        val duration240 = SlowMotionCaptureFps.FPS_240.calculateOutputDuration(5.0f, 30)
        assertEquals(40.0f, duration240, 0.001f)
    }

    @Test
    fun testSlowMotionSpeedFactors() {
        assertEquals(4.0f, SlowMotionCaptureFps.FPS_120.speedFactor, 0.001f)
        assertEquals(8.0f, SlowMotionCaptureFps.FPS_240.speedFactor, 0.001f)
    }

    @Test
    fun testSlowMotionConfigDefaults() {
        val config = SlowMotionConfig()
        assertEquals(SlowMotionCaptureFps.FPS_120, config.captureFps)
        assertEquals(30, config.playbackFps)
    }
}
