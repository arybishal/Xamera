package com.bishal.adaptivecamera.video.stabilization

import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import com.bishal.adaptivecamera.video.config.VideoStabilizationMode

/**
 * Resolved hardware flags for camera video stabilization.
 */
data class StabilizationIntent(
    val oisMode: Int,
    val eisMode: Int,
    val isOpticalActive: Boolean,
    val isElectronicActive: Boolean
)

/**
 * Coordinates video stabilization modes (Optical OIS and Electronic EIS)
 * through Camera2 CaptureRequest options applied to the active camera session.
 */
object VideoStabilizationCoordinator {

    /**
     * Resolves the hardware stabilization flags based on requested mode and hardware capability.
     */
    fun resolveStabilizationIntent(
        mode: VideoStabilizationMode,
        hasOis: Boolean = true,
        hasEis: Boolean = true
    ): StabilizationIntent {
        return when (mode) {
            VideoStabilizationMode.OFF -> StabilizationIntent(
                oisMode = CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF,
                eisMode = CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF,
                isOpticalActive = false,
                isElectronicActive = false
            )
            VideoStabilizationMode.OIS -> StabilizationIntent(
                oisMode = if (hasOis) CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON else CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF,
                eisMode = CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF,
                isOpticalActive = hasOis,
                isElectronicActive = false
            )
            VideoStabilizationMode.EIS -> StabilizationIntent(
                oisMode = CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF,
                eisMode = if (hasEis) CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON else CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF,
                isOpticalActive = false,
                isElectronicActive = hasEis
            )
            VideoStabilizationMode.COMBINED -> StabilizationIntent(
                oisMode = if (hasOis) CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON else CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF,
                eisMode = if (hasEis) CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON else CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_OFF,
                isOpticalActive = hasOis,
                isElectronicActive = hasEis
            )
        }
    }

    /**
     * Constructs the CaptureRequestOptions representing the desired stabilization mode.
     */
    fun createCaptureRequestOptions(
        mode: VideoStabilizationMode,
        hasOis: Boolean = true,
        hasEis: Boolean = true
    ): CaptureRequestOptions {
        val intent = resolveStabilizationIntent(mode, hasOis, hasEis)
        val builder = CaptureRequestOptions.Builder()
        builder.setCaptureRequestOption(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, intent.oisMode)
        builder.setCaptureRequestOption(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, intent.eisMode)
        return builder.build()
    }

    /**
     * Applies the stabilization configuration to Camera2CameraControl.
     */
    fun applyStabilization(
        camera2Control: Camera2CameraControl,
        mode: VideoStabilizationMode,
        hasOis: Boolean = true,
        hasEis: Boolean = true
    ) {
        val options = createCaptureRequestOptions(mode, hasOis, hasEis)
        camera2Control.setCaptureRequestOptions(options)
    }
}
