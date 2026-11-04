package com.bishal.adaptivecamera.camera.capability

import android.hardware.camera2.CameraCharacteristics
import android.os.Build

data class ZoomCapabilityInfo(
    val minZoomRatio: Float,
    val maxZoomRatio: Float,
    val maxDigitalZoom: Float,
    val hasOpticalFocalLengths: Boolean,
    val opticalFocalLengths: List<Float> = emptyList(),
    val zoomRatioRangeSupported: Boolean = false,
    val status: CapabilityStatus = CapabilityStatus.SUPPORTED
)

object ZoomCapabilityAnalyzer {

    fun analyze(characteristics: CameraCharacteristics): ZoomCapabilityInfo {
        val maxDigital = characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
        val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList() ?: emptyList()
        val hasMultiFocal = focalLengths.size > 1

        val minZoom: Float
        val maxZoom: Float
        val zoomRatioSupported: Boolean

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val zoomRange = characteristics.get(CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE)
            if (zoomRange != null) {
                minZoom = zoomRange.lower
                maxZoom = zoomRange.upper
                zoomRatioSupported = true
            } else {
                minZoom = 1.0f
                maxZoom = maxDigital
                zoomRatioSupported = false
            }
        } else {
            minZoom = 1.0f
            maxZoom = maxDigital
            zoomRatioSupported = false
        }

        return ZoomCapabilityInfo(
            minZoomRatio = minZoom,
            maxZoomRatio = maxZoom,
            maxDigitalZoom = maxDigital,
            hasOpticalFocalLengths = hasMultiFocal,
            opticalFocalLengths = focalLengths,
            zoomRatioRangeSupported = zoomRatioSupported,
            status = CapabilityStatus.SUPPORTED
        )
    }
}
