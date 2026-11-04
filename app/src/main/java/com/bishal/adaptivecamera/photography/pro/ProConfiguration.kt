package com.bishal.adaptivecamera.photography.pro

data class ProConfiguration(
    val isManualIso: Boolean = false,
    val requestedIso: Int = 100,
    val isManualShutter: Boolean = false,
    val requestedShutterSpeedNs: Long = 16_666_666L, // 1/60s
    val isManualFocus: Boolean = false,
    val requestedFocusDistanceDiopters: Float = 0.0f, // 0.0 = infinity
    val whiteBalance: ProWhiteBalance = ProWhiteBalance.AUTO,
    val exposureCompensationIndex: Int = 0,
    val isRawCaptureEnabled: Boolean = false,
    val isHistogramVisible: Boolean = true,
    val isHistogramExpanded: Boolean = false,
    val isFocusPeakingEnabled: Boolean = false,
    val focusPeakingSensitivity: Float = 0.5f
)

data class ProAppliedState(
    val appliedIso: Int? = null,
    val appliedShutterSpeedNs: Long? = null,
    val appliedFocusDistanceDiopters: Float? = null,
    val appliedAwbMode: Int? = null,
    val appliedExposureCompensation: Int? = null,
    val aperture: Float? = null,
    val focalLength: Float? = null,
    val isManualSensorActive: Boolean = false,
    val isManualFocusActive: Boolean = false
) {
    val appliedShutterDisplay: String
        get() = appliedShutterSpeedNs?.let { ShutterSpeedHelper.formatNanoseconds(it) } ?: "Auto"

    val appliedIsoDisplay: String
        get() = appliedIso?.let { "ISO $it" } ?: "Auto"

    val appliedFocusDistanceDisplay: String
        get() = appliedFocusDistanceDiopters?.let {
            if (it <= 0.01f) "∞ (Infinity)"
            else {
                val distanceCm = (100.0f / it).toInt()
                "${distanceCm}cm (${String.format("%.1f", it)} D)"
            }
        } ?: "Auto Focus"
}
