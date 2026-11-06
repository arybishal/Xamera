package com.bishal.adaptivecamera.core.settings

import com.bishal.adaptivecamera.camera.config.AspectRatioConfig
import com.bishal.adaptivecamera.camera.config.CompositionGridType

/**
 * Persisted camera user preferences.
 */
data class CameraPreferences(
    val gridType: CompositionGridType = CompositionGridType.OFF,
    val isVirtualHorizonEnabled: Boolean = true,
    val aspectRatio: AspectRatioConfig = AspectRatioConfig.RATIO_4_3,
    val isHapticsEnabled: Boolean = true,
    val isFloatingShutterEnabled: Boolean = false,
    val floatingShutterX: Float = 0.82f,
    val floatingShutterY: Float = 0.65f,
    val isFrontScreenFlashEnabled: Boolean = true,
    val isZslEnabled: Boolean = true,
    val aebBracketCount: Int = 3,
    val aebEvStep: Float = 1.0f,
    val captureFormat: String = "JPEG",
    val isWatermarkEnabled: Boolean = false,
    val watermarkType: String = "TEXT",
    val watermarkText: String = "Xamera Professional",
    val isExifPrivacyEnabled: Boolean = false,
    val stripGpsLocation: Boolean = true,
    val stripDeviceModel: Boolean = true
)
