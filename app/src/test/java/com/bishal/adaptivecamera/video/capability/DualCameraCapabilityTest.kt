package com.bishal.adaptivecamera.video.capability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DualCameraCapabilityTest {

    @Test
    fun testDualCameraCapabilityUnsupported() {
        val capability = DualCameraCapability(
            isSupported = false,
            supportedCameraCombinations = emptyList(),
            diagnosticReason = "Hardware Incompatible: Device ISP HAL does not expose concurrent front and rear camera sessions.",
            hardwareConcurrencyLevel = "SINGLE_STREAM_ONLY"
        )
        assertFalse(capability.isSupported)
        assertTrue(capability.supportedCameraCombinations.isEmpty())
        assertEquals("SINGLE_STREAM_ONLY", capability.hardwareConcurrencyLevel)
        assertTrue(capability.diagnosticReason.contains("Hardware Incompatible"))
    }

    @Test
    fun testDualCameraCapabilitySupported() {
        val pairs = listOf(listOf("0", "1"))
        val capability = DualCameraCapability(
            isSupported = true,
            supportedCameraCombinations = pairs,
            diagnosticReason = "Hardware ISP supports 1 concurrent streaming camera combination(s).",
            hardwareConcurrencyLevel = "CONCURRENT_ISP_SUPPORTED"
        )
        assertTrue(capability.isSupported)
        assertEquals(1, capability.supportedCameraCombinations.size)
        assertEquals("0", capability.supportedCameraCombinations[0][0])
        assertEquals("1", capability.supportedCameraCombinations[0][1])
        assertEquals("CONCURRENT_ISP_SUPPORTED", capability.hardwareConcurrencyLevel)
    }
}
