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
    val isFrontScreenFlashEnabled: Boolean = true
)
