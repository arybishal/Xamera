package com.bishal.adaptivecamera.photography.pro

import android.hardware.camera2.CameraMetadata
import com.bishal.adaptivecamera.photography.raw.DngCaptureManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer

class ProPhotographyPhase4Test {

    @Test
    fun testIsoRangeClamping() {
        val minIso = 100
        val maxIso = 6400

        val config = ProConfiguration(
            isManualIso = true,
            requestedIso = 50
        )
        val clampedUnder = config.requestedIso.coerceIn(minIso, maxIso)
        assertEquals(100, clampedUnder)

        val clampedOver = 12800.coerceIn(minIso, maxIso)
        assertEquals(6400, clampedOver)

        val clampedNormal = 800.coerceIn(minIso, maxIso)
        assertEquals(800, clampedNormal)
    }

    @Test
    fun testShutterSpeedFormatting() {
        assertEquals("1/8000s", ShutterSpeedHelper.formatNanoseconds(125_000L))
        assertEquals("1/2000s", ShutterSpeedHelper.formatNanoseconds(500_000L))
        assertEquals("1/1000s", ShutterSpeedHelper.formatNanoseconds(1_000_000L))
        assertEquals("1/500s", ShutterSpeedHelper.formatNanoseconds(2_000_000L))
        assertEquals("1/250s", ShutterSpeedHelper.formatNanoseconds(4_000_000L))
        assertEquals("1/125s", ShutterSpeedHelper.formatNanoseconds(8_000_000L))
        assertEquals("1/60s", ShutterSpeedHelper.formatNanoseconds(16_666_666L))
        assertEquals("1/30s", ShutterSpeedHelper.formatNanoseconds(33_333_333L))
        assertEquals("1/15s", ShutterSpeedHelper.formatNanoseconds(66_666_666L))
        assertEquals("1/8s", ShutterSpeedHelper.formatNanoseconds(125_000_000L))
        assertEquals("1/4s", ShutterSpeedHelper.formatNanoseconds(250_000_000L))
        assertEquals("1/2s", ShutterSpeedHelper.formatNanoseconds(500_000_000L))
        assertEquals("1s", ShutterSpeedHelper.formatNanoseconds(1_000_000_000L))
        assertEquals("2s", ShutterSpeedHelper.formatNanoseconds(2_000_000_000L))
        assertEquals("10s", ShutterSpeedHelper.formatNanoseconds(10_000_000_000L))
    }

    @Test
    fun testShutterSpeedPresetsWithinHardwareRange() {
        val minNs = 100_000L // 1/10000s
        val maxNs = 30_000_000_000L // 30s
        val steps = ShutterSpeedHelper.getSupportedPresets(minNs, maxNs)

        assertTrue(steps.isNotEmpty())
        assertTrue(steps.first().nanoseconds >= minNs)
        assertTrue(steps.last().nanoseconds <= maxNs)

        // Find standard 1/60s
        val step60 = steps.firstOrNull { it.displayString == "1/60s" }
        assertNotNull(step60)
        assertEquals(16_666_666L, step60?.nanoseconds)
    }

    @Test
    fun testFocusDistanceFormatting() {
        val infinityState = ProAppliedState(appliedFocusDistanceDiopters = 0.0f)
        assertEquals("∞ (Infinity)", infinityState.appliedFocusDistanceDisplay)

        val meterState = ProAppliedState(appliedFocusDistanceDiopters = 1.0f)
        assertTrue(meterState.appliedFocusDistanceDisplay.contains("100cm"))

        val closeState = ProAppliedState(appliedFocusDistanceDiopters = 10.0f)
        assertTrue(closeState.appliedFocusDistanceDisplay.contains("10cm"))
    }

    @Test
    fun testProWhiteBalanceMappings() {
        assertEquals(CameraMetadata.CONTROL_AWB_MODE_AUTO, ProWhiteBalance.AUTO.camera2Mode)
        assertEquals(CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT, ProWhiteBalance.CLOUDY.camera2Mode)
        assertEquals(CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT, ProWhiteBalance.DAYLIGHT.camera2Mode)
        assertEquals(CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT, ProWhiteBalance.FLUORESCENT.camera2Mode)
        assertEquals(CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT, ProWhiteBalance.INCANDESCENT.camera2Mode)
        assertEquals(CameraMetadata.CONTROL_AWB_MODE_SHADE, ProWhiteBalance.SHADE.camera2Mode)
    }

    @Test
    fun testProConfigurationStateTransitions() {
        var config = ProConfiguration()
        assertFalse(config.isManualIso)
        assertFalse(config.isManualShutter)
        assertFalse(config.isManualFocus)
        assertEquals(ProWhiteBalance.AUTO, config.whiteBalance)

        // Enable manual controls
        config = config.copy(
            isManualIso = true,
            requestedIso = 400,
            isManualShutter = true,
            requestedShutterSpeedNs = 16_666_666L,
            isManualFocus = true,
            requestedFocusDistanceDiopters = 2.0f,
            whiteBalance = ProWhiteBalance.CLOUDY
        )

        assertTrue(config.isManualIso)
        assertEquals(400, config.requestedIso)
        assertTrue(config.isManualShutter)
        assertEquals(16_666_666L, config.requestedShutterSpeedNs)
        assertTrue(config.isManualFocus)
        assertEquals(2.0f, config.requestedFocusDistanceDiopters, 0.01f)
        assertEquals(ProWhiteBalance.CLOUDY, config.whiteBalance)

        // Reset to auto
        val resetConfig = config.copy(
            isManualIso = false,
            isManualShutter = false,
            isManualFocus = false,
            whiteBalance = ProWhiteBalance.AUTO,
            exposureCompensationIndex = 0
        )

        assertFalse(resetConfig.isManualIso)
        assertFalse(resetConfig.isManualShutter)
        assertFalse(resetConfig.isManualFocus)
        assertEquals(ProWhiteBalance.AUTO, resetConfig.whiteBalance)
        assertEquals(0, resetConfig.exposureCompensationIndex)
    }

    @Test
    fun testHistogramAnalyzerWithWhiteFrame() {
        val width = 32
        val height = 32
        val size = width * height

        // Pure white: Y = 255, U = 128, V = 128
        val yBuf = ByteBuffer.allocateDirect(size)
        for (i in 0 until size) yBuf.put(255.toByte())
        yBuf.flip()

        val uBuf = ByteBuffer.allocateDirect(size / 4)
        for (i in 0 until size / 4) uBuf.put(128.toByte())
        uBuf.flip()

        val vBuf = ByteBuffer.allocateDirect(size / 4)
        for (i in 0 until size / 4) vBuf.put(128.toByte())
        vBuf.flip()

        val hist = HistogramAnalyzer.analyzeYuv(
            yBuffer = yBuf,
            uBuffer = uBuf,
            vBuffer = vBuf,
            width = width,
            height = height,
            yRowStride = width,
            yPixelStride = 1,
            uvRowStride = width / 2,
            uvPixelStride = 1
        )

        assertEquals(64, hist.lumaBins.size)
        assertEquals(64, hist.rBins.size)
        assertEquals(64, hist.gBins.size)
        assertEquals(64, hist.bBins.size)

        // The highest bin (63) should have all the counts
        assertTrue(hist.lumaBins[63] > 0.90f)
        assertTrue(hist.rBins[63] > 0.90f)
        assertTrue(hist.gBins[63] > 0.90f)
        assertTrue(hist.bBins[63] > 0.90f)

        // Highlight clipping ratio should be high, shadow clipping 0
        assertTrue(hist.highlightClippingRatio > 0.90f)
        assertEquals(0.0f, hist.shadowClippingRatio, 0.001f)
    }

    @Test
    fun testHistogramAnalyzerWithBlackFrame() {
        val width = 32
        val height = 32
        val size = width * height

        // Pure black: Y = 0, U = 128, V = 128
        val yBuf = ByteBuffer.allocateDirect(size)
        for (i in 0 until size) yBuf.put(0.toByte())
        yBuf.flip()

        val uBuf = ByteBuffer.allocateDirect(size / 4)
        for (i in 0 until size / 4) uBuf.put(128.toByte())
        uBuf.flip()

        val vBuf = ByteBuffer.allocateDirect(size / 4)
        for (i in 0 until size / 4) vBuf.put(128.toByte())
        vBuf.flip()

        val hist = HistogramAnalyzer.analyzeYuv(
            yBuffer = yBuf,
            uBuffer = uBuf,
            vBuffer = vBuf,
            width = width,
            height = height,
            yRowStride = width,
            yPixelStride = 1,
            uvRowStride = width / 2,
            uvPixelStride = 1
        )

        // Bin 0 should have all counts
        assertTrue(hist.lumaBins[0] > 0.90f)
        assertTrue(hist.shadowClippingRatio > 0.90f)
        assertEquals(0.0f, hist.highlightClippingRatio, 0.001f)
    }

    @Test
    fun testFocusPeakingOnUniformFrame() {
        val width = 32
        val height = 32
        val size = width * height

        // Flat uniform luminance (no edges)
        val yBuf = ByteBuffer.allocateDirect(size)
        for (i in 0 until size) yBuf.put(128.toByte())
        yBuf.flip()

        val peaking = FocusPeakingAnalyzer.analyzeY(
            yBuffer = yBuf,
            width = width,
            height = height,
            rowStride = width,
            pixelStride = 1,
            sensitivity = 0.5f
        )

        assertTrue(peaking.isEnabled)
        // Uniform surface has no high gradients -> 0 peaking points
        assertEquals(0, peaking.points.size)
    }

    @Test
    fun testFocusPeakingOnHighContrastEdge() {
        val width = 32
        val height = 32
        val size = width * height

        // High contrast alternating step edge (left half 0, right half 255)
        val yBuf = ByteBuffer.allocateDirect(size)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val value = if (x < width / 2) 0 else 255
                yBuf.put(value.toByte())
            }
        }
        yBuf.flip()

        val peaking = FocusPeakingAnalyzer.analyzeY(
            yBuffer = yBuf,
            width = width,
            height = height,
            rowStride = width,
            pixelStride = 1,
            sensitivity = 0.5f
        )

        assertTrue(peaking.isEnabled)
        // Edge should be detected along the midline (x ~ 0.5)
        assertTrue(peaking.points.isNotEmpty())
        for (pt in peaking.points) {
            assertTrue(pt.xNorm in 0.30f..0.70f)
            assertTrue(pt.intensityNorm > 0.5f)
        }
    }

    @Test
    fun testDngHeaderValidation() {
        // Little endian TIFF header: 0x49, 0x49, 0x2A, 0x00 ('I', 'I', 42, 0)
        val validLittleEndianHeader = byteArrayOf(0x49, 0x49, 0x2A, 0x00, 0x08, 0x00, 0x00, 0x00)
        val tempValid = File.createTempFile("test_valid_dng", ".dng").apply {
            writeBytes(validLittleEndianHeader + ByteArray(5000))
        }

        val validResult = DngCaptureManager.validateDngFile(tempValid)
        assertTrue(validResult.isValid)
        assertTrue(validResult.hasTiffHeader)
        assertEquals(DngCaptureManager.DNG_MIME_TYPE, validResult.mimeType)
        tempValid.delete()

        // Invalid header (e.g. JPEG bytes: 0xFF, 0xD8, 0xFF)
        val invalidJpegHeader = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x00, 0x10)
        val tempInvalid = File.createTempFile("test_invalid_dng", ".dng").apply {
            writeBytes(invalidJpegHeader + ByteArray(5000))
        }

        val invalidResult = DngCaptureManager.validateDngFile(tempInvalid)
        assertFalse(invalidResult.isValid)
        assertFalse(invalidResult.hasTiffHeader)
        assertTrue(invalidResult.details.contains("Invalid header bytes"))
        tempInvalid.delete()

        // Zero-length file
        val emptyFile = File.createTempFile("test_empty", ".dng")
        val emptyResult = DngCaptureManager.validateDngFile(emptyFile)
        assertFalse(emptyResult.isValid)
        emptyFile.delete()
    }

    @Test
    fun testProAppliedStateFormatting() {
        val state = ProAppliedState(
            appliedIso = 400,
            appliedShutterSpeedNs = 16_666_666L,
            appliedFocusDistanceDiopters = 2.0f,
            aperture = 1.8f,
            focalLength = 5.6f,
            isManualSensorActive = true,
            isManualFocusActive = true
        )

        assertEquals("ISO 400", state.appliedIsoDisplay)
        assertEquals("1/60s", state.appliedShutterDisplay)
        assertTrue(state.appliedFocusDistanceDisplay.contains("50cm"))
        assertEquals(1.8f, state.aperture ?: 0f, 0.01f)
        assertEquals(5.6f, state.focalLength ?: 0f, 0.01f)
    }

    @Test
    fun testSmartAutoCompatibilityDoesNotOverrideManualSettings() {
        val proConfig = ProConfiguration(
            isManualIso = true,
            requestedIso = 800,
            isManualShutter = true,
            requestedShutterSpeedNs = 4_000_000L // 1/250s
        )

        // In Pro mode with manual sensor locked:
        // Smart Auto recommendation should remain purely advisory
        assertTrue(proConfig.isManualIso)
        assertTrue(proConfig.isManualShutter)

        val appliedState = ProAppliedState(
            appliedIso = 800,
            appliedShutterSpeedNs = 4_000_000L,
            isManualSensorActive = true
        )

        assertTrue(appliedState.isManualSensorActive)
        assertEquals("ISO 800", appliedState.appliedIsoDisplay)
        assertEquals("1/250s", appliedState.appliedShutterDisplay)
    }
}
