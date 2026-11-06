package com.bishal.adaptivecamera.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThermalMonitorTest {

    @Test
    fun testThermalStatusSeverityMapping() {
        assertEquals(ThermalStatus.NONE, ThermalStatus.fromSeverity(0))
        assertEquals(ThermalStatus.LIGHT, ThermalStatus.fromSeverity(1))
        assertEquals(ThermalStatus.MODERATE, ThermalStatus.fromSeverity(2))
        assertEquals(ThermalStatus.SEVERE, ThermalStatus.fromSeverity(3))
        assertEquals(ThermalStatus.CRITICAL, ThermalStatus.fromSeverity(4))
        assertEquals(ThermalStatus.EMERGENCY, ThermalStatus.fromSeverity(5))
        assertEquals(ThermalStatus.SHUTDOWN, ThermalStatus.fromSeverity(6))
        assertEquals(ThermalStatus.NONE, ThermalStatus.fromSeverity(999))
    }

    @Test
    fun testThermalStatusThrottlingFlag() {
        assertFalse(ThermalStatus.NONE.isThrottling)
        assertFalse(ThermalStatus.LIGHT.isThrottling)
        assertFalse(ThermalStatus.MODERATE.isThrottling)
        assertTrue(ThermalStatus.SEVERE.isThrottling)
        assertTrue(ThermalStatus.CRITICAL.isThrottling)
        assertTrue(ThermalStatus.EMERGENCY.isThrottling)
        assertTrue(ThermalStatus.SHUTDOWN.isThrottling)
    }

    @Test
    fun testNormalThermalPolicy() {
        val policy = ThermalMitigationPolicy(
            thermalStatus = ThermalStatus.NONE,
            isThrottlingActive = false,
            disableAnalyticalOverlays = false,
            reduceGpuEffectsOnPreview = false,
            maxRecommendedStackingFrames = 7,
            advisoryNotice = null
        )
        assertFalse(policy.isThrottlingActive)
        assertFalse(policy.disableAnalyticalOverlays)
        assertFalse(policy.reduceGpuEffectsOnPreview)
        assertEquals(7, policy.maxRecommendedStackingFrames)
        assertNull(policy.advisoryNotice)
    }

    @Test
    fun testSevereThermalPolicy() {
        val policy = ThermalMitigationPolicy(
            thermalStatus = ThermalStatus.SEVERE,
            isThrottlingActive = true,
            disableAnalyticalOverlays = true,
            reduceGpuEffectsOnPreview = false,
            maxRecommendedStackingFrames = 3,
            advisoryNotice = "Thermal Throttling Active"
        )
        assertTrue(policy.isThrottlingActive)
        assertTrue(policy.disableAnalyticalOverlays)
        assertFalse(policy.reduceGpuEffectsOnPreview)
        assertEquals(3, policy.maxRecommendedStackingFrames)
        assertNotNull(policy.advisoryNotice)
    }

    @Test
    fun testCriticalThermalPolicy() {
        val policy = ThermalMitigationPolicy(
            thermalStatus = ThermalStatus.CRITICAL,
            isThrottlingActive = true,
            disableAnalyticalOverlays = true,
            reduceGpuEffectsOnPreview = true,
            maxRecommendedStackingFrames = 1,
            advisoryNotice = "High Temperature Alert"
        )
        assertTrue(policy.isThrottlingActive)
        assertTrue(policy.disableAnalyticalOverlays)
        assertTrue(policy.reduceGpuEffectsOnPreview)
        assertEquals(1, policy.maxRecommendedStackingFrames)
    }
}
