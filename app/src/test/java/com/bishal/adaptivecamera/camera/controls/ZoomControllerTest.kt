package com.bishal.adaptivecamera.camera.controls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomControllerTest {

    @Test
    fun testInitialValues() {
        val controller = ZoomControllerImpl(
            initialZoom = 1.0f,
            initialMin = 0.6f,
            initialMax = 10.0f
        ) {}

        assertEquals(1.0f, controller.zoomRatio.value, 0.001f)
        assertEquals(0.6f, controller.minZoomRatio.value, 0.001f)
        assertEquals(10.0f, controller.maxZoomRatio.value, 0.001f)
    }

    @Test
    fun testSetZoomClampedWithinBounds() {
        var appliedZoom = 0f
        val controller = ZoomControllerImpl(
            initialZoom = 1.0f,
            initialMin = 1.0f,
            initialMax = 8.0f
        ) { appliedZoom = it }

        // Normal within bounds
        val result1 = controller.setZoomRatio(2.5f)
        assertEquals(2.5f, result1, 0.001f)
        assertEquals(2.5f, controller.zoomRatio.value, 0.001f)
        assertEquals(2.5f, appliedZoom, 0.001f)

        // Below min: clamped to 1.0
        val result2 = controller.setZoomRatio(0.4f)
        assertEquals(1.0f, result2, 0.001f)
        assertEquals(1.0f, controller.zoomRatio.value, 0.001f)
        assertEquals(1.0f, appliedZoom, 0.001f)

        // Above max: clamped to 8.0
        val result3 = controller.setZoomRatio(15.0f)
        assertEquals(8.0f, result3, 0.001f)
        assertEquals(8.0f, controller.zoomRatio.value, 0.001f)
        assertEquals(8.0f, appliedZoom, 0.001f)
    }

    @Test
    fun testApplyScaleFactor() {
        val controller = ZoomControllerImpl(
            initialZoom = 1.0f,
            initialMin = 1.0f,
            initialMax = 10.0f
        ) {}

        // Scale by 2.0x
        controller.applyScaleFactor(2.0f)
        assertEquals(2.0f, controller.zoomRatio.value, 0.001f)

        // Scale by 1.5x -> 3.0x
        controller.applyScaleFactor(1.5f)
        assertEquals(3.0f, controller.zoomRatio.value, 0.001f)

        // Negative or zero scale factor ignored
        controller.applyScaleFactor(0f)
        assertEquals(3.0f, controller.zoomRatio.value, 0.001f)
        controller.applyScaleFactor(-1.5f)
        assertEquals(3.0f, controller.zoomRatio.value, 0.001f)
    }

    @Test
    fun testUpdateRangeClampsCurrentZoom() {
        var callbackTriggered = false
        val controller = ZoomControllerImpl(
            initialZoom = 5.0f,
            initialMin = 1.0f,
            initialMax = 10.0f
        ) { callbackTriggered = true }

        // Restrict range to max 3.0f
        controller.updateRange(minRatio = 1.0f, maxRatio = 3.0f)
        assertEquals(3.0f, controller.zoomRatio.value, 0.001f)
        assertEquals(3.0f, controller.maxZoomRatio.value, 0.001f)
        assertTrue(callbackTriggered)
    }

    @Test
    fun testGetAvailableSteps() {
        val controller = ZoomControllerImpl(
            initialZoom = 1.0f,
            initialMin = 0.6f,
            initialMax = 10.0f
        ) {}

        // For hardware with 0.6x minimum (e.g. OnePlus 13R / Ace 5)
        val steps = controller.getAvailableSteps()
        assertEquals(listOf(0.6f, 1.0f, 2.0f, 3.0f, 5.0f, 10.0f), steps)

        // For hardware supporting 0.5x minimum
        controller.updateRange(0.5f, 10.0f)
        assertEquals(listOf(0.5f, 1.0f, 2.0f, 3.0f, 5.0f, 10.0f), controller.getAvailableSteps())

        // Limited sensor range (e.g. 1.0x to 3.0x)
        controller.updateRange(1.0f, 3.0f)
        assertEquals(listOf(1.0f, 2.0f, 3.0f), controller.getAvailableSteps())
    }

    @Test
    fun testIsNearestStep() {
        val controller = ZoomControllerImpl(
            initialZoom = 1.02f,
            initialMin = 0.6f,
            initialMax = 10.0f
        ) {}

        assertTrue("1.02 is within 0.08 of 1.0", controller.isNearestStep(1.0f))
        assertFalse("1.02 is not near 2.0", controller.isNearestStep(2.0f))

        controller.setZoomRatio(1.95f)
        assertTrue("1.95 is within 0.08 of 2.0", controller.isNearestStep(2.0f))
    }
}
