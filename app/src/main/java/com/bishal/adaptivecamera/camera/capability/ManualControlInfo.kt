package com.bishal.adaptivecamera.camera.capability

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata

data class ManualControlInfo(
    val supportsManualSensor: Boolean,
    val manualSensorStatus: CapabilityStatus,
    val isoRange: RangeVal<Int>? = null,
    val exposureTimeRangeNs: RangeVal<Long>? = null,
    val exposureCompensationRange: RangeVal<Int>? = null,
    val exposureCompensationStep: Float? = null,
    val supportsAeLock: Boolean = false,
    val aeLockStatus: CapabilityStatus = CapabilityStatus.UNSUPPORTED,
    val afAvailableModes: List<Int> = emptyList(),
    val supportsManualFocus: Boolean = false,
    val manualFocusStatus: CapabilityStatus = CapabilityStatus.UNSUPPORTED,
    val minimumFocusDistance: Float? = null,
    val awbAvailableModes: List<Int> = emptyList(),
    val supportsAwbLock: Boolean = false,
    val awbLockStatus: CapabilityStatus = CapabilityStatus.UNSUPPORTED,
    val supportsManualWhiteBalance: Boolean = false,
    val mwbStatus: CapabilityStatus = CapabilityStatus.UNSUPPORTED
)

object ManualControlAnalyzer {

    fun analyze(characteristics: CameraCharacteristics): ManualControlInfo {
        val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.toList() ?: emptyList()
        val supportsManualSensorCap = capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)

        // ISO & Shutter
        val isoRangeRaw = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val isoRange = isoRangeRaw?.let { RangeVal(it.lower, it.upper) }

        val expTimeRangeRaw = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val expTimeRange = expTimeRangeRaw?.let { RangeVal(it.lower, it.upper) }

        val manualSensorStatus = if (supportsManualSensorCap && isoRange != null && expTimeRange != null) {
            CapabilityStatus.SUPPORTED
        } else {
            CapabilityStatus.UNSUPPORTED
        }

        // Exposure Compensation & AE Lock
        val expCompRangeRaw = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE)
        val expCompRange = expCompRangeRaw?.let { RangeVal(it.lower, it.upper) }

        val expCompStepRaw = characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
        val expCompStep = expCompStepRaw?.let { it.numerator.toFloat() / it.denominator.toFloat() }

        val aeLock = characteristics.get(CameraCharacteristics.CONTROL_AE_LOCK_AVAILABLE) ?: false
        val aeLockStatus = if (aeLock) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED

        // Focus
        val afModes = characteristics.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES)?.toList() ?: emptyList()
        val minFocusDist = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
        val hasMf = minFocusDist != null && minFocusDist > 0f && afModes.contains(CameraMetadata.CONTROL_AF_MODE_OFF)
        val mfStatus = if (hasMf) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED

        // White Balance
        val awbModes = characteristics.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES)?.toList() ?: emptyList()
        val awbLock = characteristics.get(CameraCharacteristics.CONTROL_AWB_LOCK_AVAILABLE) ?: false
        val awbLockStatus = if (awbLock) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED

        val hasMwb = awbModes.contains(CameraMetadata.CONTROL_AWB_MODE_OFF) && supportsManualSensorCap
        val mwbStatus = if (hasMwb) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED

        return ManualControlInfo(
            supportsManualSensor = manualSensorStatus.isUsable,
            manualSensorStatus = manualSensorStatus,
            isoRange = isoRange,
            exposureTimeRangeNs = expTimeRange,
            exposureCompensationRange = expCompRange,
            exposureCompensationStep = expCompStep,
            supportsAeLock = aeLock,
            aeLockStatus = aeLockStatus,
            afAvailableModes = afModes,
            supportsManualFocus = hasMf,
            manualFocusStatus = mfStatus,
            minimumFocusDistance = minFocusDist,
            awbAvailableModes = awbModes,
            supportsAwbLock = awbLock,
            awbLockStatus = awbLockStatus,
            supportsManualWhiteBalance = hasMwb,
            mwbStatus = mwbStatus
        )
    }
}
