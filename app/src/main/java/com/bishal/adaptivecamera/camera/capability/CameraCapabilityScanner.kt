package com.bishal.adaptivecamera.camera.capability

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.DynamicRangeProfiles
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
            val cameraIds = cameraManager.cameraIdList
            if (cameraIds.isEmpty()) {
                warnings.add("CameraManager reported 0 accessible camera IDs.")
            }

            for (id in cameraIds) {
                try {
                    val characteristics = cameraManager.getCameraCharacteristics(id)
                    val profile = scanCamera(id, characteristics)
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

    fun scanCamera(cameraId: String, characteristics: CameraCharacteristics): CameraCapabilityProfile {
        val notes = mutableListOf<String>()

        // 1. LENS_FACING (Accurate Android CameraMetadata mapping)
        // CameraMetadata.LENS_FACING_FRONT = 0
        // CameraMetadata.LENS_FACING_BACK = 1
        // CameraMetadata.LENS_FACING_EXTERNAL = 2
        val rawFacing = characteristics.get(CameraCharacteristics.LENS_FACING) ?: -1
        val facingLabel = when (rawFacing) {
            CameraMetadata.LENS_FACING_BACK -> "BACK"
            CameraMetadata.LENS_FACING_FRONT -> "FRONT"
            CameraMetadata.LENS_FACING_EXTERNAL -> "EXTERNAL"
            else -> "UNKNOWN ($rawFacing)"
        }
        notes.add("Raw LENS_FACING integer: $rawFacing ($facingLabel)")

        // 2. Hardware Level
        val rawHwLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        val hardwareLevel = CameraHardwareLevel.fromInt(rawHwLevel)
        notes.add("Raw INFO_SUPPORTED_HARDWARE_LEVEL: $rawHwLevel (${hardwareLevel.displayName})")

        // 3. Capabilities Array
        val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.toList() ?: emptyList()
        val isLogical = capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)
        val supportsManualSensorCap = capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)
        val supportsRawCap = capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW)

        // 4. Physical Camera Mapping (API 28+)
        val physicalIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && isLogical) {
            try {
                characteristics.physicalCameraIds.toList()
            } catch (e: Exception) {
                notes.add("Failed to retrieve physical camera IDs: ${e.message}")
                emptyList()
            }
        } else {
            emptyList()
        }

        if (isLogical) {
            notes.add("Logical multi-camera: References physical camera IDs [${physicalIds.joinToString(", ")}]")
        } else {
            notes.add("Single physical camera stream")
        }

        // 5. Optics & Sensor Geometry
        val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList() ?: emptyList()
        val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
        val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        val activeArrayStr = activeArray?.let { "${it.width()}x${it.height()}" } ?: "Unavailable"

        // 6. Stream Configuration Map
        val streamMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val supportedFormats = streamMap?.outputFormats?.toList() ?: emptyList()

        val photoSizes = mutableListOf<CameraResolution>()
        if (streamMap != null) {
            val jpegSizes = streamMap.getOutputSizes(ImageFormat.JPEG) ?: emptyArray()
            photoSizes.addAll(jpegSizes.map { CameraResolution(it.width, it.height) })
        }
        photoSizes.sortByDescending { it.width * it.height }

        val videoSizes = mutableListOf<CameraResolution>()
        if (streamMap != null) {
            val recSizes = streamMap.getOutputSizes(MediaRecorder::class.java)
                ?: streamMap.getOutputSizes(SurfaceTexture::class.java)
                ?: emptyArray()
            videoSizes.addAll(recSizes.map { CameraResolution(it.width, it.height) })
        }
        videoSizes.sortByDescending { it.width * it.height }

        val fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            ?.map { FpsRange(it.lower, it.upper) } ?: emptyList()

        // 7. Focus Evaluation
        val afModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES) ?: intArrayOf()
        val supportsAf = afModes.any { it != CameraMetadata.CONTROL_AF_MODE_OFF }
        val afStatus = if (supportsAf) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED

        val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
        val hasManualFocusSupport = minFocusDistance != null && minFocusDistance > 0f &&
                afModes.contains(CameraMetadata.CONTROL_AF_MODE_OFF)
        val mfStatus = if (hasManualFocusSupport) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED

        // 8. Manual Sensor Evaluation (ISO & Shutter)
        val isoRangeRaw = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val isoRange = isoRangeRaw?.let { RangeVal(it.lower, it.upper) }

        val expTimeRangeRaw = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val expTimeRange = expTimeRangeRaw?.let { RangeVal(it.lower, it.upper) }

        val expCompRangeRaw = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
        val expCompRange = expCompRangeRaw?.let { RangeVal(it.lower, it.upper) }

        val expCompStepRaw = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
        val expCompStep = expCompStepRaw?.let { it.numerator.toFloat() / it.denominator.toFloat() }

        val manualSensorStatus = if (supportsManualSensorCap && isoRange != null && expTimeRange != null) {
            CapabilityStatus.SUPPORTED
        } else {
            CapabilityStatus.UNSUPPORTED
        }

        // 9. Flash Evaluation
        val supportsFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
        val flashStatus = if (supportsFlash) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED

        // 10. RAW Evaluation
        val rawFormats = supportedFormats.filter {
            it == ImageFormat.RAW_SENSOR || it == ImageFormat.RAW10 || it == ImageFormat.RAW12
        }
        val rawStatus = if (supportsRawCap && rawFormats.isNotEmpty()) {
            notes.add("RAW stream supported in formats: ${rawFormats.joinToString()}")
            CapabilityStatus.SUPPORTED
        } else {
            CapabilityStatus.UNSUPPORTED
        }

        // 11. HDR & Dynamic Range Profiles
        val dynamicRangeProfiles = mutableListOf<String>()
        var hdrStatus = CapabilityStatus.UNSUPPORTED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val drProfiles = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES)
            if (drProfiles != null) {
                val supported = drProfiles.supportedProfiles
                if (supported.contains(DynamicRangeProfiles.HLG10)) dynamicRangeProfiles.add("HLG10")
                if (supported.contains(DynamicRangeProfiles.HDR10)) dynamicRangeProfiles.add("HDR10")
                if (supported.contains(DynamicRangeProfiles.HDR10_PLUS)) dynamicRangeProfiles.add("HDR10_PLUS")
                if (supported.contains(DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM)) dynamicRangeProfiles.add("DOLBY_VISION_OEM")
                if (dynamicRangeProfiles.isNotEmpty()) {
                    hdrStatus = CapabilityStatus.SUPPORTED
                }
            }
        }

        // 12. Stabilization
        val oisModes = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)?.toList() ?: emptyList()
        val eisModes = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)?.toList() ?: emptyList()
        val noiseReductionModes = characteristics.get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES)?.toList() ?: emptyList()

        // 13. Zoom range
        val minZoom: Float
        val maxZoom: Float
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val zoomRange = characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
            minZoom = zoomRange?.lower ?: 1.0f
            maxZoom = zoomRange?.upper ?: (characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f)
        } else {
            minZoom = 1.0f
            maxZoom = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
        }

        return CameraCapabilityProfile(
            cameraId = cameraId,
            rawFacing = rawFacing,
            hardwareLevel = hardwareLevel,
            rawHardwareLevel = rawHwLevel,
            isLogicalCamera = isLogical,
            physicalCameraIds = physicalIds,
            focalLengths = focalLengths,
            sensorOrientation = sensorOrientation,
            sensorActiveArraySize = activeArrayStr,
            supportedFormats = supportedFormats,
            supportedPhotoSizes = photoSizes,
            supportedVideoSizes = videoSizes,
            supportedFpsRanges = fpsRanges,
            autoFocusStatus = afStatus,
            manualFocusStatus = mfStatus,
            minimumFocusDistance = minFocusDistance,
            manualSensorStatus = manualSensorStatus,
            isoRange = isoRange,
            exposureTimeRangeNs = expTimeRange,
            exposureCompensationRange = expCompRange,
            exposureCompensationStep = expCompStep,
            rawStatus = rawStatus,
            rawFormats = rawFormats,
            flashStatus = flashStatus,
            hdrStatus = hdrStatus,
            dynamicRangeProfilesSupported = dynamicRangeProfiles,
            opticalStabilizationModes = oisModes,
            videoStabilizationModes = eisModes,
            noiseReductionModes = noiseReductionModes,
            maxZoomRatio = maxZoom,
            minZoomRatio = minZoom,
            zoomStatus = CapabilityStatus.SUPPORTED,
            verificationStatus = CapabilityStatus.SUPPORTED,
            rawCapabilities = capabilities,
            diagnosticNotes = notes
        )
    }
}
