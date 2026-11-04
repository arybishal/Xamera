package com.bishal.adaptivecamera.camera.capability

import android.hardware.camera2.CameraMetadata
import com.bishal.adaptivecamera.core.diagnostics.DeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraCapabilityProfileTest {

    @Test
    fun testLensFacingMappingFollowsAndroidCameraMetadataConstants() {
        // CameraMetadata.LENS_FACING_BACK == 1
        val backCamera = CameraCapabilityProfile(
            cameraId = "0",
            rawFacing = CameraMetadata.LENS_FACING_BACK,
            hardwareLevel = CameraHardwareLevel.LEVEL_3,
            isLogicalCamera = true,
            physicalCameraIds = listOf("2", "3")
        )
        assertEquals("BACK", backCamera.facingLabel)
        assertTrue(backCamera.isFacingBack)
        assertFalse(backCamera.isFacingFront)

        // CameraMetadata.LENS_FACING_FRONT == 0
        val frontCamera = CameraCapabilityProfile(
            cameraId = "1",
            rawFacing = CameraMetadata.LENS_FACING_FRONT,
            hardwareLevel = CameraHardwareLevel.FULL,
            isLogicalCamera = false
        )
        assertEquals("FRONT", frontCamera.facingLabel)
        assertTrue(frontCamera.isFacingFront)
        assertFalse(frontCamera.isFacingBack)

        // CameraMetadata.LENS_FACING_EXTERNAL == 2
        val externalCamera = CameraCapabilityProfile(
            cameraId = "4",
            rawFacing = CameraMetadata.LENS_FACING_EXTERNAL,
            hardwareLevel = CameraHardwareLevel.EXTERNAL,
            isLogicalCamera = false
        )
        assertEquals("EXTERNAL", externalCamera.facingLabel)
        assertFalse(externalCamera.isFacingBack)
        assertFalse(externalCamera.isFacingFront)
    }

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
            rawFacing = CameraMetadata.LENS_FACING_BACK,
            hardwareLevel = CameraHardwareLevel.FULL,
            isLogicalCamera = false,
            exposureTimeRangeNs = RangeVal(100_000L, 30_000_000_000L) // 1/10000s to 30s
        )
        val display = profileFast.exposureTimeRangeDisplay
        assertTrue(display.contains("1/10000s"))
        assertTrue(display.contains("30.00s"))
    }

    @Test
    fun testDeviceCapabilityReportSerializationWithAccurateFacing() {
        val dummyDevice = DeviceInfo.analyze(
            manufacturer = "OnePlus",
            model = "CPH2691",
            device = "OP5D3BL1",
            product = "CPH2691IN",
            brand = "OnePlus",
            hardware = "qcom",
            fingerprint = "OnePlus/CPH2691IN/OP5D3BL1:17/...",
            display = "CPH2691_17.0.0.105(EX01)",
            androidVersion = "17",
            apiLevel = 37
        )

        val cam0BackLogical = CameraCapabilityProfile(
            cameraId = "0",
            rawFacing = CameraMetadata.LENS_FACING_BACK, // 1
            hardwareLevel = CameraHardwareLevel.LEVEL_3,
            isLogicalCamera = true,
            physicalCameraIds = listOf("2", "3"),
            focalLengths = listOf(5.59f),
            supportedPhotoSizes = listOf(CameraResolution(4096, 3072)),
            supportedVideoSizes = listOf(CameraResolution(3840, 2160)),
            autoFocusStatus = CapabilityStatus.SUPPORTED,
            manualFocusStatus = CapabilityStatus.SUPPORTED,
            manualSensorStatus = CapabilityStatus.SUPPORTED,
            rawStatus = CapabilityStatus.SUPPORTED,
            flashStatus = CapabilityStatus.SUPPORTED,
            hdrStatus = CapabilityStatus.SUPPORTED
        )

        val cam1Front = CameraCapabilityProfile(
            cameraId = "1",
            rawFacing = CameraMetadata.LENS_FACING_FRONT, // 0
            hardwareLevel = CameraHardwareLevel.FULL,
            isLogicalCamera = false,
            focalLengths = listOf(3.24f),
            autoFocusStatus = CapabilityStatus.UNSUPPORTED,
            manualFocusStatus = CapabilityStatus.UNSUPPORTED,
            manualSensorStatus = CapabilityStatus.SUPPORTED,
            rawStatus = CapabilityStatus.UNSUPPORTED,
            flashStatus = CapabilityStatus.UNSUPPORTED
        )

        val report = DeviceCapabilityReport(
            deviceInfo = dummyDevice,
            cameraProfiles = listOf(cam0BackLogical, cam1Front),
            diagnosticWarnings = listOf("Physical Hardware Identity is UNCONFIRMED")
        )

        val textReport = report.toFormattedText()
        assertTrue(textReport.contains("OnePlus 13R (India / Regional CPH2691IN)"))
        assertTrue(textReport.contains("Back-Facing Cameras: 1 | Front-Facing Cameras: 1"))
        assertTrue(textReport.contains("CAMERA #1 [ID: 0 | BACK (raw: 1)]"))
        assertTrue(textReport.contains("CAMERA #2 [ID: 1 | FRONT (raw: 0)]"))
        assertTrue(textReport.contains("Physical Camera IDs: 2, 3"))

        val jsonReport = report.toJson()
        assertTrue(jsonReport.contains("\"reportedModel\": \"CPH2691\""))
        assertTrue(jsonReport.contains("\"physicalIdentityConfidence\": \"UNCONFIRMED\""))
        assertTrue(jsonReport.contains("\"rawFacing\": 1"))
        assertTrue(jsonReport.contains("\"facingLabel\": \"BACK\""))
    }
}
