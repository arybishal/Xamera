package com.bishal.adaptivecamera.core.display

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayRefreshRateTest {

    @Test
    fun testDisplayTelemetryDefaultValues() {
        val telemetry = DisplayTelemetry()
        assertEquals(0, telemetry.currentModeId)
        assertEquals(0, telemetry.width)
        assertEquals(0, telemetry.height)
        assertEquals(60f, telemetry.refreshRateHz, 0.001f)
        assertTrue(telemetry.supportedRefreshRates.isEmpty())
        assertFalse(telemetry.isHighRefreshRateActive)
    }

    @Test
    fun testDisplayTelemetryCustomValues() {
        val telemetry = DisplayTelemetry(
            currentModeId = 1,
            width = 1264,
            height = 2780,
            refreshRateHz = 120.0f,
            supportedRefreshRates = listOf(60f, 90f, 120f),
            isHighRefreshRateActive = true
        )
        assertEquals(1, telemetry.currentModeId)
        assertEquals(1264, telemetry.width)
        assertEquals(2780, telemetry.height)
        assertEquals(120.0f, telemetry.refreshRateHz, 0.001f)
        assertEquals(3, telemetry.supportedRefreshRates.size)
        assertTrue(telemetry.isHighRefreshRateActive)
    }
}
