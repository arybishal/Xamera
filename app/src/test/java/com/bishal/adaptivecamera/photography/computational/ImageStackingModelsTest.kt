package com.bishal.adaptivecamera.photography.computational

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageStackingModelsTest {

    @Test
    fun testImageStackingModeValues() {
        val modes = ImageStackingMode.values()
        assertEquals(4, modes.size)
        assertEquals("Smart Auto", ImageStackingMode.SMART_AUTO.label)
        assertEquals("Low-Light NR", ImageStackingMode.LOW_LIGHT.label)
        assertEquals("HDR Fusion", ImageStackingMode.HDR.label)
        assertEquals("Detail Enhancement", ImageStackingMode.DETAIL_ENHANCEMENT.label)
    }

    @Test
    fun testImageStackingModeCycling() {
        assertEquals(ImageStackingMode.LOW_LIGHT, ImageStackingMode.SMART_AUTO.next())
        assertEquals(ImageStackingMode.HDR, ImageStackingMode.LOW_LIGHT.next())
        assertEquals(ImageStackingMode.DETAIL_ENHANCEMENT, ImageStackingMode.HDR.next())
        assertEquals(ImageStackingMode.SMART_AUTO, ImageStackingMode.DETAIL_ENHANCEMENT.next())
    }

    @Test
    fun testImageStackingFrameCount() {
        val counts = ImageStackingFrameCount.values()
        assertEquals(4, counts.size)
        assertEquals(5, ImageStackingFrameCount.AUTO.count)
        assertEquals(3, ImageStackingFrameCount.FRAMES_3.count)
        assertEquals(5, ImageStackingFrameCount.FRAMES_5.count)
        assertEquals(7, ImageStackingFrameCount.FRAMES_7.count)

        assertEquals(ImageStackingFrameCount.FRAMES_3, ImageStackingFrameCount.AUTO.next())
        assertEquals(ImageStackingFrameCount.FRAMES_5, ImageStackingFrameCount.FRAMES_3.next())
        assertEquals(ImageStackingFrameCount.FRAMES_7, ImageStackingFrameCount.FRAMES_5.next())
        assertEquals(ImageStackingFrameCount.AUTO, ImageStackingFrameCount.FRAMES_7.next())
    }

    @Test
    fun testImageStackingConfigDefaults() {
        val config = ImageStackingConfig()
        assertFalse(config.isEnabled)
        assertEquals(ImageStackingMode.SMART_AUTO, config.mode)
        assertEquals(ImageStackingFrameCount.AUTO, config.frameCount)
        assertFalse(config.retainOriginalFrames)
        assertEquals(0.08f, config.motionRejectionThreshold, 0.001f)
        assertEquals(32, config.alignmentMaxShiftPx)
    }

    @Test
    fun testDisplacementVector() {
        val disp = DisplacementVector(dx = 4, dy = -2, confidence = 0.95f)
        assertEquals(4, disp.dx)
        assertEquals(-2, disp.dy)
        assertEquals(0.95f, disp.confidence, 0.001f)
    }

    @Test
    fun testMotionMaskEqualityAndHash() {
        val mask1 = MotionMask(
            width = 4,
            height = 4,
            mask = FloatArray(16) { 0.5f },
            motionRatio = 0.25f
        )
        val mask2 = MotionMask(
            width = 4,
            height = 4,
            mask = FloatArray(16) { 0.5f },
            motionRatio = 0.25f
        )
        val mask3 = MotionMask(
            width = 4,
            height = 4,
            mask = FloatArray(16) { 1.0f },
            motionRatio = 0.75f
        )

        assertEquals(mask1, mask2)
        assertEquals(mask1.hashCode(), mask2.hashCode())
        assertNotEquals(mask1, mask3)
    }
}
