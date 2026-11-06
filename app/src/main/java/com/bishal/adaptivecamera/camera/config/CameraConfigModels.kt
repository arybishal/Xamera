package com.bishal.adaptivecamera.camera.config

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture

enum class CameraFacing(val selector: CameraSelector) {
    BACK(CameraSelector.DEFAULT_BACK_CAMERA),
    FRONT(CameraSelector.DEFAULT_FRONT_CAMERA);

    fun toggle(): CameraFacing = if (this == BACK) FRONT else BACK
}

enum class FlashMode(val displayName: String, val imageCaptureFlashMode: Int) {
    OFF("Off", ImageCapture.FLASH_MODE_OFF),
    AUTO("Auto", ImageCapture.FLASH_MODE_AUTO),
    ON("On", ImageCapture.FLASH_MODE_ON),
    TORCH("Torch", ImageCapture.FLASH_MODE_OFF); // Torch handled via CameraControl

    fun next(supportsAuto: Boolean = true, supportsTorch: Boolean = true): FlashMode {
        val available = mutableListOf(OFF)
        if (supportsAuto) available.add(AUTO)
        available.add(ON)
        if (supportsTorch) available.add(TORCH)

        val currentIndex = available.indexOf(this)
        return if (currentIndex >= 0 && currentIndex < available.size - 1) {
            available[currentIndex + 1]
        } else {
            available.first()
        }
    }
}

enum class PhotoTimer(val seconds: Int, val displayName: String) {
    OFF(0, "Off"),
    SEC_3(3, "3s"),
    SEC_10(10, "10s");

    fun next(): PhotoTimer = when (this) {
        OFF -> SEC_3
        SEC_3 -> SEC_10
        SEC_10 -> OFF
    }
}

enum class CameraMode(val displayName: String) {
    PHOTO("PHOTO"),
    PRO("PRO"),
    VIDEO("VIDEO")
}

data class CameraConfiguration(
    val facing: CameraFacing = CameraFacing.BACK,
    val mode: CameraMode = CameraMode.PHOTO,
    val flashMode: FlashMode = FlashMode.OFF,
    val timer: PhotoTimer = PhotoTimer.OFF,
    val zoomRatio: Float = 1.0f,
    val minZoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 10.0f,
    val exposureCompensationIndex: Int = 0,
    val minExposureIndex: Int = 0,
    val maxExposureIndex: Int = 0,
    val exposureStepEv: Float = 0.5f,
    val isExposureCompensationSupported: Boolean = false,
    val isExposureStateReady: Boolean = false,
    val isTorchActive: Boolean = false,
    val isAudioEnabled: Boolean = true,
    val aspectRatio: AspectRatioConfig = AspectRatioConfig.RATIO_4_3,
    val gridType: CompositionGridType = CompositionGridType.OFF,
    val isVirtualHorizonEnabled: Boolean = true,
    val isHapticsEnabled: Boolean = true,
    val isFloatingShutterEnabled: Boolean = false,
    val floatingShutterX: Float = 0.82f,
    val floatingShutterY: Float = 0.65f,
    val isFrontScreenFlashActive: Boolean = false,
    val isZslEnabled: Boolean = true,
    val aebBracketCount: Int = 0,
    val aebEvStep: Float = 1.0f
) {
    val isExposureControlAvailable: Boolean
        get() = isExposureStateReady && isExposureCompensationSupported && minExposureIndex < maxExposureIndex

    val exposureEvDisplay: String
        get() {
            val ev = exposureCompensationIndex * exposureStepEv
            return if (ev > 0.001f) String.format(java.util.Locale.US, "+%.1f EV", ev)
            else if (ev < -0.001f) String.format(java.util.Locale.US, "%.1f EV", ev)
            else "0.0 EV"
        }
}
