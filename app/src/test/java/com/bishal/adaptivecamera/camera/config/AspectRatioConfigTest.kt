package com.bishal.adaptivecamera.camera.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AspectRatioConfigTest {

    @Test
    fun testTargetRatioPortrait() {
        assertNull("FULL should return null unconstrained ratio", AspectRatioConfig.FULL.getTargetRatio(isPortrait = true))
        
        val ratio43 = AspectRatioConfig.RATIO_4_3.getTargetRatio(isPortrait = true)
        assertNotNull(ratio43)
        assertEquals(0.75f, ratio43!!, 0.001f) // 3:4 portrait

        val ratio169 = AspectRatioConfig.RATIO_16_9.getTargetRatio(isPortrait = true)
        assertNotNull(ratio169)
        assertEquals(9f / 16f, ratio169!!, 0.001f)

        val ratio11 = AspectRatioConfig.RATIO_1_1.getTargetRatio(isPortrait = true)
        assertNotNull(ratio11)
        assertEquals(1.0f, ratio11!!, 0.001f)

        val ratio239 = AspectRatioConfig.RATIO_2_39_1.getTargetRatio(isPortrait = true)
        assertNotNull(ratio239)
        assertEquals(1f / 2.39f, ratio239!!, 0.001f)
    }

    @Test
    fun testTargetRatioLandscape() {
        assertNull(AspectRatioConfig.FULL.getTargetRatio(isPortrait = false))

        val ratio43 = AspectRatioConfig.RATIO_4_3.getTargetRatio(isPortrait = false)
        assertNotNull(ratio43)
        assertEquals(4f / 3f, ratio43!!, 0.001f) // 4:3 landscape

        val ratio169 = AspectRatioConfig.RATIO_16_9.getTargetRatio(isPortrait = false)
        assertNotNull(ratio169)
        assertEquals(16f / 9f, ratio169!!, 0.001f)

        val ratio11 = AspectRatioConfig.RATIO_1_1.getTargetRatio(isPortrait = false)
        assertNotNull(ratio11)
        assertEquals(1.0f, ratio11!!, 0.001f)

        val ratio239 = AspectRatioConfig.RATIO_2_39_1.getTargetRatio(isPortrait = false)
        assertNotNull(ratio239)
        assertEquals(2.39f, ratio239!!, 0.001f)
    }

    @Test
    fun testFramingBoundsFullReturnsFullContainer() {
        val bounds = AspectRatioConfig.FULL.calculateFramingBounds(1080f, 2400f, isPortrait = true)
        assertEquals(0f, bounds.left, 0.001f)
        assertEquals(0f, bounds.top, 0.001f)
        assertEquals(1080f, bounds.right, 0.001f)
        assertEquals(2400f, bounds.bottom, 0.001f)
    }

    @Test
    fun testFramingBounds43Portrait() {
        // Container 1080 x 2400, target ratio 3:4 (width = 1080, height = 1440)
        val bounds = AspectRatioConfig.RATIO_4_3.calculateFramingBounds(1080f, 2400f, isPortrait = true)
        assertEquals(0f, bounds.left, 0.001f)
        assertEquals(1080f, bounds.right, 0.001f)
        assertEquals(1440f, bounds.height, 0.001f)
        assertEquals(480f, bounds.top, 0.001f) // (2400 - 1440) / 2 = 480
        assertEquals(1920f, bounds.bottom, 0.001f)
    }

    @Test
    fun testFramingBoundsSquare11() {
        // Container 1080 x 2400, target ratio 1:1 (width = 1080, height = 1080)
        val bounds = AspectRatioConfig.RATIO_1_1.calculateFramingBounds(1080f, 2400f, isPortrait = true)
        assertEquals(0f, bounds.left, 0.001f)
        assertEquals(1080f, bounds.right, 0.001f)
        assertEquals(1080f, bounds.height, 0.001f)
        assertEquals(660f, bounds.top, 0.001f) // (2400 - 1080) / 2 = 660
        assertEquals(1740f, bounds.bottom, 0.001f)
    }

    @Test
    fun testAspectRatioCycleOrder() {
        assertEquals(AspectRatioConfig.RATIO_16_9, AspectRatioConfig.RATIO_4_3.next())
        assertEquals(AspectRatioConfig.RATIO_1_1, AspectRatioConfig.RATIO_16_9.next())
        assertEquals(AspectRatioConfig.RATIO_2_39_1, AspectRatioConfig.RATIO_1_1.next())
        assertEquals(AspectRatioConfig.FULL, AspectRatioConfig.RATIO_2_39_1.next())
        assertEquals(AspectRatioConfig.RATIO_4_3, AspectRatioConfig.FULL.next())
    }
}
