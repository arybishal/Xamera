package com.bishal.adaptivecamera.camera.capability

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.bishal.adaptivecamera.core.diagnostics.DeviceInfo
import com.bishal.adaptivecamera.core.diagnostics.PhysicalIdentityConfidence

class CameraCapabilityScanner(
    private val context: Context
) {
    companion object {
        private const val TAG = "CameraCapabilityScanner"
    }

    private val cameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    fun scanDevice(): DeviceCapabilityReport {
        val deviceInfo = DeviceInfo.current()
        val profiles = mutableListOf<CameraCapabilityProfile>()
        val warnings = mutableListOf<String>()

        try {
            val cameraIds = cameraManager.cameraIdList.toList()
            if (cameraIds.isEmpty()) {
                warnings.add("CameraManager reported 0 accessible camera IDs.")
            }

            // Relationship analysis across all cameras
            val relationships = CameraRelationshipAnalyzer.analyze(cameraIds) { id ->
                try {
                    cameraManager.getCameraCharacteristics(id)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load characteristics for relationship analysis of ID $id", e)
                    null
                }
            }

            for (id in cameraIds) {
                try {
                    val characteristics = cameraManager.getCameraCharacteristics(id)
                    val relInfo = relationships[id] ?: CameraRelationshipInfo(
                        relationship = CameraRelationship.INDEPENDENT,
                        description = "Standalone camera unit"
                    )
                    val profile = scanCamera(id, characteristics, relInfo)
                    profiles.add(profile)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to query characteristics for camera ID $id", e)
                    warnings.add("Exception reading CameraCharacteristics for ID $id: ${e.message}")
                }
            }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Camera access error during discovery", e)
            warnings.add("CameraAccessException during discovery: ${e.message}")
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission error querying cameras", e)
            warnings.add("SecurityException: Camera permission is required for full hardware inspection.")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during discovery", e)
            warnings.add("Unexpected error during discovery: ${e.message}")
        }

        // Add identity & firmware warnings if confidence is unconfirmed or conflicting
        if (deviceInfo.physicalIdentityConfidence == PhysicalIdentityConfidence.UNCONFIRMED) {
            warnings.add("Physical Hardware Identity is UNCONFIRMED: Firmware identity '${deviceInfo.firmwareIdentity}' indicates regional build, while hardware platform '${deviceInfo.probableHardwareFamily}' is shared between OnePlus Ace 5 and 13R.")
        }

        return DeviceCapabilityReport(
            deviceInfo = deviceInfo,
            cameraProfiles = profiles,
            diagnosticWarnings = warnings
        )
    }

    fun scanCamera(
        cameraId: String,
        characteristics: CameraCharacteristics,
        relationshipInfo: CameraRelationshipInfo = CameraRelationshipInfo(CameraRelationship.INDEPENDENT)
    ): CameraCapabilityProfile {
        val notes = mutableListOf<String>()

        // 1. LENS_FACING
        val rawFacing = characteristics.get(CameraCharacteristics.LENS_FACING) ?: -1
        val facingLabel = when (rawFacing) {
            CameraMetadata.LENS_FACING_BACK -> "BACK"
            CameraMetadata.LENS_FACING_FRONT -> "FRONT"
            CameraMetadata.LENS_FACING_EXTERNAL -> "EXTERNAL"
            else -> "UNKNOWN ($rawFacing)"
        }
        notes.add("Raw LENS_FACING: $rawFacing ($facingLabel)")

        // 2. Hardware Level
        val rawHwLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        val hardwareLevel = CameraHardwareLevel.fromInt(rawHwLevel)
        notes.add("Hardware Level: ${hardwareLevel.displayName} (raw: $rawHwLevel)")

        // 3. Capabilities Array
        val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.toList() ?: emptyList()
        val isLogical = capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)

        // 4. Physical Camera Mapping
        val physicalIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && isLogical) {
            try {
                characteristics.physicalCameraIds.toList()
            } catch (e: Exception) {
                notes.add("Failed to query physical camera IDs: ${e.message}")
                emptyList()
            }
        } else {
            emptyList()
        }

        if (isLogical) {
            notes.add("Logical multi-camera: Composing sub-camera IDs [${physicalIds.joinToString(", ")}]")
        } else if (relationshipInfo.logicalParentIds.isNotEmpty()) {
            notes.add("Physical sub-sensor referenced by: [${relationshipInfo.logicalParentIds.joinToString(", ")}]")
        }

        // 5. Sensor and Lens Intelligence
        val (sensorInfo, lensInfo) = SensorLensAnalyzer.analyze(characteristics)

        // 6. Stream Configuration Intelligence
        val (streamConfigs, streamCombinations) = StreamConfigurationAnalyzer.analyze(
            characteristics,
            hardwareLevel,
            isLogical
        )

        val supportedFormats = streamConfigs.map { it.format }
        val photoSizes = streamConfigs.firstOrNull { it.format == ImageFormat.JPEG }?.sizes?.map {
            CameraResolution(it.width, it.height)
        } ?: emptyList()

        val videoSizes = streamConfigs.firstOrNull { it.format == ImageFormat.PRIVATE }?.sizes?.map {
            CameraResolution(it.width, it.height)
        } ?: emptyList()

        // 7. Dynamic Range Intelligence
        val dynamicRangeInfo = DynamicRangeAnalyzer.analyze(characteristics)
        val dynamicRangeProfileNames = dynamicRangeInfo.supportedProfiles.map { it.profileName }

        // 8. Video Capability Intelligence
        val videoCapabilityInfo = VideoCapabilityAnalyzer.analyze(characteristics)

        // 9. Manual Control Intelligence
        val manualControlInfo = ManualControlAnalyzer.analyze(characteristics)

        // 10. Zoom Capability Intelligence
        val zoomCapabilityInfo = ZoomCapabilityAnalyzer.analyze(characteristics)

        // 11. RAW Capability Intelligence
        val rawCapabilityInfo = RawCapabilityAnalyzer.analyze(characteristics)

        // 12. Stabilization Modes
        val oisModes = lensInfo.opticalStabilizationModes
        val eisModes = videoCapabilityInfo.videoStabilizationModes
        val noiseReductionModes = characteristics.get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES)?.toList() ?: emptyList()

        // 13. Flash Evaluation
        val supportsFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
        val flashStatus = if (supportsFlash) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED

        // 14. Hardware Validation Summary
        val advertisedFeatures = mutableMapOf<String, CapabilityStatus>()
        advertisedFeatures["Autofocus"] = if (manualControlInfo.afAvailableModes.any { it != CameraMetadata.CONTROL_AF_MODE_OFF }) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED
        advertisedFeatures["Manual Sensor (ISO/Shutter)"] = manualControlInfo.manualSensorStatus
        advertisedFeatures["Manual Focus"] = manualControlInfo.manualFocusStatus
        advertisedFeatures["Manual White Balance"] = manualControlInfo.mwbStatus
        advertisedFeatures["AE Lock"] = manualControlInfo.aeLockStatus
        advertisedFeatures["AWB Lock"] = manualControlInfo.awbLockStatus
        advertisedFeatures["Flash Unit"] = flashStatus
        advertisedFeatures["RAW Sensor Stream"] = rawCapabilityInfo.status
        advertisedFeatures["10-bit HDR Dynamic Range"] = dynamicRangeInfo.status
        advertisedFeatures["Optical Stabilization (OIS)"] = if (lensInfo.opticalStabilizationSupported) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED
        advertisedFeatures["Electronic Stabilization (EIS)"] = if (videoCapabilityInfo.supportsVideoStabilization) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED
        advertisedFeatures["Preview Stabilization"] = if (videoCapabilityInfo.supportsPreviewStabilization) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED

        val apiAccessibleFeatures = mutableMapOf<String, CapabilityStatus>()
        apiAccessibleFeatures["CameraCharacteristics Query"] = CapabilityStatus.SUPPORTED
        apiAccessibleFeatures["StreamConfigurationMap Query"] = if (streamConfigs.isNotEmpty()) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED
        apiAccessibleFeatures["Physical Camera Query"] = if (relationshipInfo.isPhysicalStreamQuerySupported) CapabilityStatus.SUPPORTED else CapabilityStatus.UNAVAILABLE

        val validatedConfigs = mutableMapOf<String, CapabilityStatus>()
        streamCombinations.forEach { combo ->
            validatedConfigs[combo.name] = combo.status
        }

        val validationSummary = HardwareValidationSummary(
            advertisedFeatures = advertisedFeatures,
            apiAccessibleFeatures = apiAccessibleFeatures,
            validatedConfigurations = validatedConfigs,
            captureVerifiedFeatures = emptyMap()
        )

        return CameraCapabilityProfile(
            cameraId = cameraId,
            rawFacing = rawFacing,
            hardwareLevel = hardwareLevel,
            rawHardwareLevel = rawHwLevel,
            isLogicalCamera = isLogical,
            physicalCameraIds = physicalIds,
            focalLengths = lensInfo.focalLengths,
            sensorOrientation = sensorInfo.sensorOrientation,
            sensorActiveArraySize = sensorInfo.activeArraySize,
            supportedFormats = supportedFormats,
            supportedPhotoSizes = photoSizes,
            supportedVideoSizes = videoSizes,
            supportedFpsRanges = videoCapabilityInfo.supportedFpsRanges,
            autoFocusStatus = advertisedFeatures["Autofocus"] ?: CapabilityStatus.NOT_VERIFIED,
            manualFocusStatus = manualControlInfo.manualFocusStatus,
            minimumFocusDistance = manualControlInfo.minimumFocusDistance,
            manualSensorStatus = manualControlInfo.manualSensorStatus,
            isoRange = manualControlInfo.isoRange,
            exposureTimeRangeNs = manualControlInfo.exposureTimeRangeNs,
            exposureCompensationRange = manualControlInfo.exposureCompensationRange,
            exposureCompensationStep = manualControlInfo.exposureCompensationStep,
            rawStatus = rawCapabilityInfo.status,
            rawFormats = rawCapabilityInfo.supportedRawFormats,
            flashStatus = flashStatus,
            hdrStatus = dynamicRangeInfo.status,
            dynamicRangeProfilesSupported = dynamicRangeProfileNames,
            opticalStabilizationModes = oisModes,
            videoStabilizationModes = eisModes,
            noiseReductionModes = noiseReductionModes,
            maxZoomRatio = zoomCapabilityInfo.maxZoomRatio,
            minZoomRatio = zoomCapabilityInfo.minZoomRatio,
            zoomStatus = zoomCapabilityInfo.status,
            verificationStatus = CapabilityStatus.SUPPORTED,
            rawCapabilities = capabilities,
            diagnosticNotes = notes,
            scannedAtTimestamp = System.currentTimeMillis(),
            relationshipInfo = relationshipInfo,
            sensorInfo = sensorInfo,
            lensInfo = lensInfo,
            streamConfigurations = streamConfigs,
            streamCombinations = streamCombinations,
            videoCapabilityInfo = videoCapabilityInfo,
            dynamicRangeInfo = dynamicRangeInfo,
            manualControlInfo = manualControlInfo,
            zoomCapabilityInfo = zoomCapabilityInfo,
            rawCapabilityInfo = rawCapabilityInfo,
            validationSummary = validationSummary
        )
    }
}
