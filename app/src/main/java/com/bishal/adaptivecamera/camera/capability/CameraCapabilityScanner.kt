package com.bishal.adaptivecamera.camera.capability

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.DynamicRangeProfiles
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import android.util.Range
import android.util.Size
import com.bishal.adaptivecamera.core.diagnostics.DeviceInfo

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
                warnings.add("CameraManager reported 0 accessible cameras.")
            }

            for (id in cameraIds) {
                try {
                    val characteristics = cameraManager.getCameraCharacteristics(id)
                    val profile = scanCamera(id, characteristics)
                    profiles.add(profile)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to inspect camera ID $id", e)
                    warnings.add("Could not query CameraCharacteristics for ID $id: ${e.message}")
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

        if (deviceInfo.isPrimaryTargetAce5) {
            warnings.add("OnePlus Ace 5 detected. Applying customized hardware validation pipeline.")
        }

        return DeviceCapabilityReport(
            deviceInfo = deviceInfo,
            cameraProfiles = profiles,
            diagnosticWarnings = warnings
        )
    }

    fun scanCamera(cameraId: String, characteristics: CameraCharacteristics): CameraCapabilityProfile {
        val facing = characteristics.get(CameraCharacteristics.LENS_FACING) ?: -1
        val rawHwLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
        val hardwareLevel = CameraHardwareLevel.fromInt(rawHwLevel)

        // Capabilities list
        val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: intArrayOf()
        val isLogical = capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA)
        val supportsManualSensor = capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)
        val supportsManualPostProcessing = capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING)
        val supportsRaw = capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW)

        // Physical camera IDs (if logical)
        val physicalIds = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            characteristics.physicalCameraIds.toList()
        } else {
            emptyList()
        }

        // Focal lengths
        val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList() ?: emptyList()

        // Sensor
        val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
        val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        val activeArrayStr = activeArray?.let { "${it.width()}x${it.height()}" } ?: ""

        // Stream configurations
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

        // FPS Ranges
        val fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            ?.map { FpsRange(it.lower, it.upper) } ?: emptyList()

        // Focus
        val afModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES) ?: intArrayOf()
        val supportsAf = afModes.any { it != CameraMetadata.CONTROL_AF_MODE_OFF }
        val minFocusDistance = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
        val supportsMf = minFocusDistance != null && minFocusDistance > 0f

        // Manual Exposure & ISO
        val isoRangeRaw = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val isoRange = isoRangeRaw?.let { RangeVal(it.lower, it.upper) }

        val expTimeRangeRaw = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val expTimeRange = expTimeRangeRaw?.let { RangeVal(it.lower, it.upper) }

        val expCompRangeRaw = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
        val expCompRange = expCompRangeRaw?.let { RangeVal(it.lower, it.upper) }

        val expCompStepRaw = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
        val expCompStep = expCompStepRaw?.let { it.numerator.toFloat() / it.denominator.toFloat() }

        // Flash
        val supportsFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

        // RAW Formats
        val rawFormats = supportedFormats.filter {
            it == ImageFormat.RAW_SENSOR || it == ImageFormat.RAW10 || it == ImageFormat.RAW12
        }

        // HDR & Dynamic Range Profiles
        val dynamicRangeProfiles = mutableListOf<String>()
        var supportsHdr = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val drProfiles = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES)
            if (drProfiles != null) {
                val supported = drProfiles.supportedProfiles
                if (supported.contains(DynamicRangeProfiles.HLG10)) dynamicRangeProfiles.add("HLG10")
                if (supported.contains(DynamicRangeProfiles.HDR10)) dynamicRangeProfiles.add("HDR10")
                if (supported.contains(DynamicRangeProfiles.HDR10_PLUS)) dynamicRangeProfiles.add("HDR10_PLUS")
                if (supported.contains(DynamicRangeProfiles.DOLBY_VISION_10B_HDR_OEM)) dynamicRangeProfiles.add("DOLBY_VISION_OEM")
                supportsHdr = dynamicRangeProfiles.isNotEmpty()
            }
        }

        // Stabilization
        val oisModes = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)?.toList() ?: emptyList()
        val eisModes = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)?.toList() ?: emptyList()
        val noiseReductionModes = characteristics.get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES)?.toList() ?: emptyList()

        // Zoom range
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
            facing = facing,
            hardwareLevel = hardwareLevel,
            isLogicalCamera = isLogical,
            physicalCameraIds = physicalIds,
            focalLengths = focalLengths,
            sensorOrientation = sensorOrientation,
            sensorActiveArraySize = activeArrayStr,
            supportedFormats = supportedFormats,
            supportedPhotoSizes = photoSizes,
            supportedVideoSizes = videoSizes,
            supportedFpsRanges = fpsRanges,
            supportsAutoFocus = supportsAf,
            supportsManualFocus = supportsMf,
            minimumFocusDistance = minFocusDistance,
            supportsManualSensor = supportsManualSensor,
            isoRange = isoRange,
            exposureTimeRangeNs = expTimeRange,
            exposureCompensationRange = expCompRange,
            exposureCompensationStep = expCompStep,
            supportsRaw = supportsRaw || rawFormats.isNotEmpty(),
            rawFormats = rawFormats,
            supportsFlash = supportsFlash,
            supportsHdr = supportsHdr,
            dynamicRangeProfilesSupported = dynamicRangeProfiles,
            opticalStabilizationModes = oisModes,
            videoStabilizationModes = eisModes,
            noiseReductionModes = noiseReductionModes,
            maxZoomRatio = maxZoom,
            minZoomRatio = minZoom,
            verificationStatus = CapabilityStatus.SUPPORTED
        )
    }
}
