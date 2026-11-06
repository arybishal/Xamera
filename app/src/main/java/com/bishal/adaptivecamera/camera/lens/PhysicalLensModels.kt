package com.bishal.adaptivecamera.camera.lens

enum class PhysicalLensType {
    WIDE_MAIN,
    ULTRA_WIDE,
    TELEPHOTO,
    MACRO,
    FRONT,
    LOGICAL_BACK
}

data class DirectPhysicalLens(
    val cameraId: String,
    val type: PhysicalLensType,
    val label: String,
    val focalLengthMm: Float,
    val sensorSize: String,
    val isPhysicalSubCamera: Boolean
)
