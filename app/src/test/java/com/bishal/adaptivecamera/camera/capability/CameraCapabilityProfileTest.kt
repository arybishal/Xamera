package com.bishal.adaptivecamera.camera.capability

import com.bishal.adaptivecamera.core.diagnostics.DeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraCapabilityProfileTest {

    @Test
    fun testCameraResolutionCalculations() {
        val res4k = CameraResolution(3840, 2160)
        assertEquals("16:9", res4k.aspectRatio)
        assertTrue(res4k.megapixels > 8.2 && res4k.megapixels < 8.4)

        val resPhoto = CameraResolution(4000, 3000)
        assertEquals("4:3", resPhoto.aspectRatio)
        assertEquals(12.0, resPhoto.megapixels, 0.01)

        val resSquare = CameraResolution(1080, 1080)
        assertEquals("1:1", resSquare.aspectRatio)
    }

    @Test
    fun testExposureTimeFormatting() {
        val profileFast = CameraCapabilityProfile(
            cameraId = "0",
            facing = 0,
            hardwareLevel = CameraHardwareLevel.FULL,
            isLogicalCamera = false,
            exposureTimeRangeNs = RangeVal(100_000L, 30_000_000_000L) // 1/10000s to 30s
        )
        val display = profileFast.exposureTimeRangeDisplay
        assertTrue(display.contains("1/10000s"))
        assertTrue(display.contains("30.00s"))
    }

    @Test
    fun testDeviceCapabilityReportSerialization() {
        val dummyDevice = DeviceInfo(
            manufacturer = "OnePlus",
            model = "PKG110",
            device = "PKG110",
            product = "PKG110",
            brand = "OnePlus",
            hardware = "qcom",
            androidVersion = "15",
            apiLevel = 35,
            buildFingerprint = "OnePlus/PKG110/PKG110:15/...",
            displayBuildId = "PKG110_15.0.0.100",
            isPrimaryTargetAce5 = true
        )

        val dummyCamera = CameraCapabilityProfile(
            cameraId = "0",
            facing = 0,
            hardwareLevel = CameraHardwareLevel.LEVEL_3,
            isLogicalCamera = true,
            physicalCameraIds = listOf("2", "3"),
            focalLengths = listOf(5.59f),
            supportedPhotoSizes = listOf(
                CameraResolution(4096, 3072),
                CameraResolution(1920, 1080)
            ),
            supportedVideoSizes = listOf(
                CameraResolution(3840, 2160),
                CameraResolution(1920, 1080)
            ),
            supportedFpsRanges = listOf(
                FpsRange(15, 30),
                FpsRange(30, 30),
                FpsRange(60, 60)
            ),
            supportsAutoFocus = true,
            supportsManualFocus = true,
            supportsManualSensor = true,
            supportsRaw = true,
            supportsFlash = true,
            supportsHdr = true
        )

        val report = DeviceCapabilityReport(
            deviceInfo = dummyDevice,
            cameraProfiles = listOf(dummyCamera),
            diagnosticWarnings = listOf("Ace 5 China hardware baseline verified")
        )

        val textReport = report.toFormattedText()
        assertTrue(textReport.contains("OnePlus"))
        assertTrue(textReport.contains("PKG110"))
        assertTrue(textReport.contains("LEVEL_3"))
        assertTrue(textReport.contains("CAMERA #1 [ID: 0 | BACK]"))

        val jsonReport = report.toJson()
        assertTrue(jsonReport.contains("\"manufacturer\": \"OnePlus\""))
        assertTrue(jsonReport.contains("\"isPrimaryTargetAce5\": true"))
        assertTrue(jsonReport.contains("\"cameraId\": \"0\""))
        assertTrue(jsonReport.contains("\"hardwareLevel\": \"LEVEL_3\""))
    }

    @Test
    fun testCapabilityStatusUsability() {
        assertTrue(CapabilityStatus.SUPPORTED.isUsable)
        assertFalse(CapabilityStatus.UNSUPPORTED.isUsable)
        assertFalse(CapabilityStatus.UNAVAILABLE.isUsable)
        assertFalse(CapabilityStatus.NOT_VERIFIED.isUsable)
        assertFalse(CapabilityStatus.TEMPORARILY_UNAVAILABLE.isUsable)
    }

    @Test
    fun testHardwareLevelMapping() {
        assertEquals(CameraHardwareLevel.FULL, CameraHardwareLevel.fromInt(1))
        assertEquals(CameraHardwareLevel.LIMITED, CameraHardwareLevel.fromInt(0))
        assertEquals(CameraHardwareLevel.LEGACY, CameraHardwareLevel.fromInt(2))
        assertEquals(CameraHardwareLevel.LEVEL_3, CameraHardwareLevel.fromInt(3))
        assertEquals(CameraHardwareLevel.EXTERNAL, CameraHardwareLevel.fromInt(4))
        assertEquals(CameraHardwareLevel.UNKNOWN, CameraHardwareLevel.fromInt(99))
    }
}
