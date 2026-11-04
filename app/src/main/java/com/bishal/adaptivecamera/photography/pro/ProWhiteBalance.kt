package com.bishal.adaptivecamera.photography.pro

import android.hardware.camera2.CameraMetadata

enum class ProWhiteBalance(
    val displayName: String,
    val camera2Mode: Int,
    val kelvinApprox: Int? = null
) {
    AUTO("Auto AWB", CameraMetadata.CONTROL_AWB_MODE_AUTO, null),
    DAYLIGHT("Daylight", CameraMetadata.CONTROL_AWB_MODE_DAYLIGHT, 5500),
    CLOUDY("Cloudy", CameraMetadata.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT, 6500),
    SHADE("Shade", CameraMetadata.CONTROL_AWB_MODE_SHADE, 7500),
    FLUORESCENT("Fluorescent", CameraMetadata.CONTROL_AWB_MODE_FLUORESCENT, 4000),
    INCANDESCENT("Incandescent", CameraMetadata.CONTROL_AWB_MODE_INCANDESCENT, 2800),
    TWILIGHT("Twilight", CameraMetadata.CONTROL_AWB_MODE_TWILIGHT, null)
}
