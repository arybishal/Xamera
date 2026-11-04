package com.bishal.adaptivecamera.camera.capability

import android.graphics.ImageFormat
import android.hardware.camera2.CameraMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HardwareIntelligencePhase2Test {

    @Test
    fun testCameraRelationshipInfoProperties() {
        val logicalInfo = CameraRelationshipInfo(
            relationship = CameraRelationship.LOGICAL,
            physicalSubCameraIds = listOf("2", "3"),
            isPhysicalStreamQuerySupported = true,
            description = "Logical multi-camera"
        )
        assertEquals(CameraRelationship.LOGICAL, logicalInfo.relationship)
        assertEquals(2, logicalInfo.physicalSubCameraIds.size)
        assertTrue(logicalInfo.isPhysicalStreamQuerySupported)

        val physicalRef = CameraRelationshipInfo(
            relationship = CameraRelationship.PHYSICAL_REFERENCE,
            logicalParentIds = listOf("0"),
            description = "Sub-sensor"
        )
        assertEquals(CameraRelationship.PHYSICAL_REFERENCE, physicalRef.relationship)
        assertEquals("0", physicalRef.logicalParentIds.first())
    }

    @Test
    fun testDynamicRangeInfoEvaluation() {
        val hlgProfile = DynamicRangeProfileDetail(
            profileName = "HLG10",
            bitDepth = 10,
            isHdr = true,
            description = "Hybrid Log-Gamma"
        )
        val sdrProfile = DynamicRangeProfileDetail(
            profileName = "SDR",
            bitDepth = 8,
            isHdr = false,
            description = "Standard 8-bit"
        )

        val info = DynamicRangeInfo(
            status = CapabilityStatus.SUPPORTED,
            supportedProfiles = listOf(sdrProfile, hlgProfile),
            recommendedHdrProfile = "HLG10"
        )

        assertEquals(CapabilityStatus.SUPPORTED, info.status)
        assertEquals(2, info.supportedProfiles.size)
        assertTrue(info.supportedProfiles.any { it.isHdr && it.bitDepth == 10 })
        assertEquals("HLG10", info.recommendedHdrProfile)
    }

    @Test
    fun testStreamConfigurationModels() {
        val streamSize = StreamSizeInfo(
            width = 3840,
            height = 2160,
            minFrameDurationNs = 33_333_333L,
            stallDurationNs = 0L,
            maxFps = 30.0
        )
        assertEquals(3840, streamSize.width)
        assertEquals(2160, streamSize.height)
        assertNotNull(streamSize.maxFps)
        assertEquals(30.0, streamSize.maxFps!!, 0.5)

        val formatConfig = FormatStreamConfiguration(
            format = ImageFormat.JPEG,
            formatName = "JPEG",
            isRaw = false,
            sizes = listOf(streamSize)
        )
        assertEquals("JPEG", formatConfig.formatName)
        assertFalse(formatConfig.isRaw)
        assertEquals(1, formatConfig.sizes.size)

        val combo = StreamCombinationCapability(
            name = "Preview + Capture",
            description = "YUV_420_888 (Preview) + JPEG (Full)",
            status = CapabilityStatus.CONFIGURATION_VALIDATED,
            guaranteedByHardwareLevel = true
        )
        assertTrue(combo.status.isUsable)
        assertTrue(combo.guaranteedByHardwareLevel)
    }

    @Test
    fun testVideoCapabilityInfoProperties() {
        val qual4k = VideoQualityProfile(
            qualityName = "4K UHD",
            resolution = CameraResolution(3840, 2160),
            isSupported = true,
            maxFps = 60.0
        )
        val qual1080p = VideoQualityProfile(
            qualityName = "1080p FHD",
            resolution = CameraResolution(1920, 1080),
            isSupported = true,
            maxFps = 60.0
        )

        val videoInfo = VideoCapabilityInfo(
            supportedQualities = listOf(qual4k, qual1080p),
            maxVideoResolution = CameraResolution(3840, 2160),
            supportedFpsRanges = listOf(FpsRange(30, 30), FpsRange(60, 60)),
            supportsVideoStabilization = true,
            supportsPreviewStabilization = true,
            status = CapabilityStatus.SUPPORTED
        )

        assertTrue(videoInfo.supportsVideoStabilization)
        assertTrue(videoInfo.supportsPreviewStabilization)
        assertEquals(2, videoInfo.supportedQualities.size)
        assertEquals(3840, videoInfo.maxVideoResolution?.width)
    }

    @Test
    fun testManualControlInfoAndClamping() {
        val manualInfo = ManualControlInfo(
            supportsManualSensor = true,
            manualSensorStatus = CapabilityStatus.SUPPORTED,
            isoRange = RangeVal(50, 6400),
            exposureTimeRangeNs = RangeVal(100_000L, 30_000_000_000L),
            exposureCompensationRange = RangeVal(-12, 12),
            exposureCompensationStep = 0.333f,
            supportsManualFocus = true,
            manualFocusStatus = CapabilityStatus.SUPPORTED,
            minimumFocusDistance = 10.0f,
            supportsAeLock = true,
            supportsAwbLock = true
        )

        assertTrue(manualInfo.supportsManualSensor)
        assertTrue(manualInfo.supportsManualFocus)
        assertEquals(50, manualInfo.isoRange?.lower)
        assertEquals(6400, manualInfo.isoRange?.upper)
        assertTrue(manualInfo.supportsAeLock)
        assertTrue(manualInfo.supportsAwbLock)
    }

    @Test
    fun testZoomCapabilityInfo() {
        val zoomInfo = ZoomCapabilityInfo(
            minZoomRatio = 0.6f,
            maxZoomRatio = 20.0f,
            maxDigitalZoom = 10.0f,
            zoomRatioRangeSupported = true,
            hasOpticalFocalLengths = true,
            opticalFocalLengths = listOf(2.1f, 5.59f),
            status = CapabilityStatus.SUPPORTED
        )

        assertTrue(zoomInfo.zoomRatioRangeSupported)
        assertTrue(zoomInfo.hasOpticalFocalLengths)
        assertEquals(0.6f, zoomInfo.minZoomRatio, 0.01f)
        assertEquals(20.0f, zoomInfo.maxZoomRatio, 0.01f)
    }

    @Test
    fun testRawCapabilityInfo() {
        val rawInfo = RawCapabilityInfo(
            status = CapabilityStatus.SUPPORTED,
            supportsRawSensor = true,
            supportedRawFormats = listOf(ImageFormat.RAW_SENSOR),
            maxRawResolution = CameraResolution(4096, 3072)
        )

        assertTrue(rawInfo.supportsRawSensor)
        assertEquals(1, rawInfo.supportedRawFormats.size)
        assertEquals(4096, rawInfo.maxRawResolution?.width)
    }

    @Test
    fun testHardwareValidationSummaryCounters() {
        val summary = HardwareValidationSummary(
            advertisedFeatures = mapOf(
                "Autofocus" to CapabilityStatus.SUPPORTED,
                "Manual Sensor" to CapabilityStatus.SUPPORTED,
                "RAW Sensor" to CapabilityStatus.UNSUPPORTED
            ),
            apiAccessibleFeatures = mapOf(
                "Characteristics" to CapabilityStatus.SUPPORTED
            ),
            validatedConfigurations = mapOf(
                "StreamCombo1" to CapabilityStatus.CONFIGURATION_VALIDATED,
                "StreamCombo2" to CapabilityStatus.UNSUPPORTED
            ),
            captureVerifiedFeatures = mapOf(
                "JPEG Capture" to CapabilityStatus.CAPTURE_VERIFIED
            )
        )

        assertEquals(3, summary.totalAdvertised)
        assertEquals(2, summary.usableAdvertisedCount)
        assertEquals(1, summary.validatedCount)
        assertEquals(1, summary.verifiedCount)
    }

    @Test
    fun testSensorAndLensInfoParsing() {
        val sensor = SensorInfo(
            activeArraySize = "4096x3072 (0,0)",
            pixelArraySize = "4128x3096",
            physicalSizeMm = "6.40 x 4.80 mm",
            sensitivityRange = RangeVal(100, 3200),
            exposureTimeRangeNs = RangeVal(200_000L, 10_000_000_000L),
            timestampSource = "REALTIME (CLOCK_BOOTTIME)",
            whiteLevel = 1023,
            blackLevelPattern = "[64, 64, 64, 64]",
            colorFilterArrangement = "RGGB",
            sensorOrientation = 90
        )

        val lens = LensInfo(
            facing = CameraMetadata.LENS_FACING_BACK,
            facingLabel = "BACK",
            focalLengths = listOf(5.59f),
            availableApertures = listOf(1.8f),
            minimumFocusDistance = 10.0f,
            opticalStabilizationModes = listOf(CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON),
            opticalStabilizationSupported = true
        )

        assertEquals("RGGB", sensor.colorFilterArrangement)
        assertEquals(1023, sensor.whiteLevel)
        assertTrue(lens.opticalStabilizationSupported)
        assertEquals(1, lens.focalLengths.size)
        assertEquals(5.59f, lens.focalLengths.first(), 0.01f)
    }
}
