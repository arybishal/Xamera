package com.bishal.adaptivecamera.camera.capability

import android.hardware.camera2.CameraMetadata

data class CameraResolution(
    val width: Int,
    val height: Int
) {
    val megapixels: Double
        get() = (width.toLong() * height.toLong()) / 1_000_000.0

    val aspectRatio: String
        get() {
            fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
            val d = gcd(width, height)
            val w = width / d
            val h = height / d
            return if ((w == 16 && h == 9) || (w == 4 && h == 3) || (w == 1 && h == 1)) {
                "$w:$h"
            } else {
                String.format("%.2f:1", width.toDouble() / height)
            }
        }

    override fun toString(): String = "${width}x${height} (${String.format("%.1f", megapixels)}MP)"
}

data class FpsRange(
    val lower: Int,
    val upper: Int
) {
    override fun toString(): String = "$lower-$upper fps"
}

data class RangeVal<T : Comparable<T>>(
    val lower: T,
    val upper: T
) {
    override fun toString(): String = "[$lower, $upper]"
}

data class CameraCapabilityProfile(
    val cameraId: String,
    val rawFacing: Int,
    val hardwareLevel: CameraHardwareLevel,
    val rawHardwareLevel: Int? = null,
    val isLogicalCamera: Boolean = false,
    val physicalCameraIds: List<String> = emptyList(),
    val focalLengths: List<Float> = emptyList(),
    val sensorOrientation: Int = 90,
    val sensorActiveArraySize: String = "",
    val supportedFormats: List<Int> = emptyList(),
    val supportedPhotoSizes: List<CameraResolution> = emptyList(),
    val supportedVideoSizes: List<CameraResolution> = emptyList(),
    val supportedFpsRanges: List<FpsRange> = emptyList(),

    // Detailed Capability Statuses (Separating Advertised, API-Accessible, Validated)
    val autoFocusStatus: CapabilityStatus = CapabilityStatus.NOT_VERIFIED,
    val manualFocusStatus: CapabilityStatus = CapabilityStatus.NOT_VERIFIED,
    val minimumFocusDistance: Float? = null, // diopters

    val manualSensorStatus: CapabilityStatus = CapabilityStatus.NOT_VERIFIED,
    val isoRange: RangeVal<Int>? = null,
    val exposureTimeRangeNs: RangeVal<Long>? = null,
    val exposureCompensationRange: RangeVal<Int>? = null,
    val exposureCompensationStep: Float? = null,

    val rawStatus: CapabilityStatus = CapabilityStatus.NOT_VERIFIED,
    val rawFormats: List<Int> = emptyList(),

    val flashStatus: CapabilityStatus = CapabilityStatus.NOT_VERIFIED,
    val hdrStatus: CapabilityStatus = CapabilityStatus.NOT_VERIFIED,
    val dynamicRangeProfilesSupported: List<String> = emptyList(),

    val opticalStabilizationModes: List<Int> = emptyList(),
    val videoStabilizationModes: List<Int> = emptyList(),
    val noiseReductionModes: List<Int> = emptyList(),

    val maxZoomRatio: Float = 1.0f,
    val minZoomRatio: Float = 1.0f,
    val zoomStatus: CapabilityStatus = CapabilityStatus.NOT_VERIFIED,

    // Verification and Raw Diagnostics
    val verificationStatus: CapabilityStatus = CapabilityStatus.NOT_VERIFIED,
    val rawCapabilities: List<Int> = emptyList(),
    val diagnosticNotes: List<String> = emptyList(),
    val scannedAtTimestamp: Long = System.currentTimeMillis(),

    // --- PHASE 2 HARDWARE INTELLIGENCE EXTENSIONS ---
    val relationshipInfo: CameraRelationshipInfo = CameraRelationshipInfo(
        relationship = if (isLogicalCamera) CameraRelationship.LOGICAL else CameraRelationship.INDEPENDENT,
        physicalSubCameraIds = physicalCameraIds
    ),
    val sensorInfo: SensorInfo = SensorInfo(
        activeArraySize = sensorActiveArraySize,
        sensitivityRange = isoRange,
        exposureTimeRangeNs = exposureTimeRangeNs,
        sensorOrientation = sensorOrientation
    ),
    val lensInfo: LensInfo = LensInfo(
        facing = rawFacing,
        facingLabel = when (rawFacing) {
            CameraMetadata.LENS_FACING_BACK -> "BACK"
            CameraMetadata.LENS_FACING_FRONT -> "FRONT"
            CameraMetadata.LENS_FACING_EXTERNAL -> "EXTERNAL"
            else -> "UNKNOWN ($rawFacing)"
        },
        focalLengths = focalLengths,
        minimumFocusDistance = minimumFocusDistance,
        opticalStabilizationModes = opticalStabilizationModes,
        opticalStabilizationSupported = opticalStabilizationModes.any { it != CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF }
    ),
    val streamConfigurations: List<FormatStreamConfiguration> = emptyList(),
    val streamCombinations: List<StreamCombinationCapability> = emptyList(),
    val videoCapabilityInfo: VideoCapabilityInfo = VideoCapabilityInfo(
        supportedFpsRanges = supportedFpsRanges,
        videoStabilizationModes = videoStabilizationModes
    ),
    val dynamicRangeInfo: DynamicRangeInfo = DynamicRangeInfo(
        status = hdrStatus
    ),
    val manualControlInfo: ManualControlInfo = ManualControlInfo(
        supportsManualSensor = manualSensorStatus.isUsable,
        manualSensorStatus = manualSensorStatus,
        isoRange = isoRange,
        exposureTimeRangeNs = exposureTimeRangeNs,
        exposureCompensationRange = exposureCompensationRange,
        exposureCompensationStep = exposureCompensationStep,
        supportsManualFocus = manualFocusStatus.isUsable,
        manualFocusStatus = manualFocusStatus,
        minimumFocusDistance = minimumFocusDistance
    ),
    val zoomCapabilityInfo: ZoomCapabilityInfo = ZoomCapabilityInfo(
        minZoomRatio = minZoomRatio,
        maxZoomRatio = maxZoomRatio,
        maxDigitalZoom = maxZoomRatio,
        hasOpticalFocalLengths = focalLengths.size > 1,
        opticalFocalLengths = focalLengths
    ),
    val rawCapabilityInfo: RawCapabilityInfo = RawCapabilityInfo(
        status = rawStatus,
        supportsRawSensor = rawStatus.isUsable,
        supportedRawFormats = rawFormats
    ),
    val validationSummary: HardwareValidationSummary = HardwareValidationSummary()
) {
    // Correct Android CameraMetadata mapping
    // CameraMetadata.LENS_FACING_FRONT == 0
    // CameraMetadata.LENS_FACING_BACK == 1
    // CameraMetadata.LENS_FACING_EXTERNAL == 2
    val facing: Int
        get() = rawFacing

    val isFacingBack: Boolean
        get() = rawFacing == CameraMetadata.LENS_FACING_BACK

    val isFacingFront: Boolean
        get() = rawFacing == CameraMetadata.LENS_FACING_FRONT

    val facingLabel: String
        get() = when (rawFacing) {
            CameraMetadata.LENS_FACING_BACK -> "BACK"
            CameraMetadata.LENS_FACING_FRONT -> "FRONT"
            CameraMetadata.LENS_FACING_EXTERNAL -> "EXTERNAL"
            else -> "UNKNOWN ($rawFacing)"
        }

    val supportsAutoFocus: Boolean
        get() = autoFocusStatus.isUsable

    val supportsManualFocus: Boolean
        get() = manualFocusStatus.isUsable

    val supportsManualSensor: Boolean
        get() = manualSensorStatus.isUsable

    val supportsRaw: Boolean
        get() = rawStatus.isUsable

    val supportsFlash: Boolean
        get() = flashStatus.isUsable

    val supportsHdr: Boolean
        get() = hdrStatus.isUsable

    val maxPhotoResolution: CameraResolution?
        get() = supportedPhotoSizes.maxByOrNull { it.width * it.height }

    val maxVideoResolution: CameraResolution?
        get() = supportedVideoSizes.maxByOrNull { it.width * it.height }

    val exposureTimeRangeDisplay: String
        get() {
            val range = exposureTimeRangeNs ?: return "Unavailable"
            val minSec = range.lower.toDouble() / 1_000_000_000.0
            val maxSec = range.upper.toDouble() / 1_000_000_000.0
            val minStr = if (minSec < 1.0) "1/${(1.0 / minSec).toInt()}s" else "${String.format("%.2f", minSec)}s"
            val maxStr = if (maxSec < 1.0) "1/${(1.0 / maxSec).toInt()}s" else "${String.format("%.2f", maxSec)}s"
            return "$minStr to $maxStr"
        }
}
