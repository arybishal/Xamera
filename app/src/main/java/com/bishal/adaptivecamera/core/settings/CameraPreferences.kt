package com.bishal.adaptivecamera.core.settings

import com.bishal.adaptivecamera.camera.config.AspectRatioConfig
import com.bishal.adaptivecamera.camera.config.CompositionGridType
import com.bishal.adaptivecamera.camera.config.HdrPlusMode
import com.bishal.adaptivecamera.photography.computational.ImageStackingFrameCount
import com.bishal.adaptivecamera.photography.computational.ImageStackingMode
import com.bishal.adaptivecamera.photography.portrait.PortraitAperture
import com.bishal.adaptivecamera.photography.portrait.PortraitLightingStyle
import com.bishal.adaptivecamera.ui.theme.AppThemeMode

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
    val stripDeviceModel: Boolean = true,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val hdrPlusMode: HdrPlusMode = HdrPlusMode.OFF,
    val imageStackingEnabled: Boolean = false,
    val imageStackingMode: ImageStackingMode = ImageStackingMode.SMART_AUTO,
    val imageStackingFrameCount: ImageStackingFrameCount = ImageStackingFrameCount.AUTO,
    val retainOriginalFrames: Boolean = false,
    val portraitAperture: PortraitAperture = PortraitAperture.F2_8,
    val portraitLightingStyle: PortraitLightingStyle = PortraitLightingStyle.NATURAL,
    val selectedLutId: String? = null,
    val isLutEnabled: Boolean = false,
    val lutIntensity: Float = 1.0f,
    val isGrainEnabled: Boolean = false,
    val grainIntensity: Float = 0.0f,
    val isHalationEnabled: Boolean = false,
    val halationIntensity: Float = 0.0f,
    val isLeftHandedModeEnabled: Boolean = false
)
