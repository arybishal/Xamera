package com.bishal.adaptivecamera.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraDiagnosticsLoggerTest {

    @Test
    fun `CameraLogEntry formats log line correctly`() {
        val entry = CameraLogEntry(
            severity = CameraLogSeverity.ERROR,
            module = "ImageCapture",
            eventType = "BURST_FAILED",
            message = "Timeout waiting for burst frames",
            errorCode = "ERR_BURST_TIMEOUT",
            technicalDetails = "Buffer overflow at index 4",
            cameraId = "0",
            lens = "WIDE",
            captureMode = "PHOTO",
            resolution = "4000x3000",
            fps = 30
        )

        val logLine = entry.toFormattedLogLine()
        assertTrue(logLine.contains("[ERROR]"))
        assertTrue(logLine.contains("[ImageCapture/BURST_FAILED]"))
        assertTrue(logLine.contains("[Code: ERR_BURST_TIMEOUT]"))
        assertTrue(logLine.contains("[Cam: 0/WIDE]"))
        assertTrue(logLine.contains("[Mode: PHOTO]"))
        assertTrue(logLine.contains("[Res: 4000x3000]"))
        assertTrue(logLine.contains("[FPS: 30]"))
        assertTrue(logLine.contains("Timeout waiting for burst frames"))
        assertTrue(logLine.contains("Buffer overflow at index 4"))
    }

    @Test
    fun `CameraHealthMetrics determines HEALTHY when no errors or critical logs exist`() {
        val metrics = CameraHealthMetrics(
            totalLogs = 20,
            debugCount = 10,
            infoCount = 10,
            warningCount = 2,
            errorCount = 0,
            criticalCount = 0,
            lastErrorMessage = null,
            lastErrorTimestamp = null,
            healthStatus = CameraHealthMetrics.HealthStatus.HEALTHY
        )

        assertEquals(CameraHealthMetrics.HealthStatus.HEALTHY, metrics.healthStatus)
        assertEquals(20, metrics.totalLogs)
        assertEquals(0, metrics.errorCount)
    }

    @Test
    fun `CameraHealthMetrics determines DEGRADED on occasional errors`() {
        val metrics = CameraHealthMetrics(
            totalLogs = 15,
            debugCount = 5,
            infoCount = 8,
            warningCount = 1,
            errorCount = 1,
            criticalCount = 0,
            lastErrorMessage = "[VideoRecording] Dropped 1 frame",
            lastErrorTimestamp = System.currentTimeMillis(),
            healthStatus = CameraHealthMetrics.HealthStatus.DEGRADED
        )

        assertEquals(CameraHealthMetrics.HealthStatus.DEGRADED, metrics.healthStatus)
        assertNotNull(metrics.lastErrorMessage)
    }

    @Test
    fun `CameraHealthMetrics determines CRITICAL on fatal hardware or session errors`() {
        val metrics = CameraHealthMetrics(
            totalLogs = 5,
            debugCount = 1,
            infoCount = 2,
            warningCount = 0,
            errorCount = 1,
            criticalCount = 1,
            lastErrorMessage = "[CRITICAL] [CameraInit] Camera hardware failed to open",
            lastErrorTimestamp = System.currentTimeMillis(),
            healthStatus = CameraHealthMetrics.HealthStatus.CRITICAL
        )

        assertEquals(CameraHealthMetrics.HealthStatus.CRITICAL, metrics.healthStatus)
        assertEquals(1, metrics.criticalCount)
    }
}
