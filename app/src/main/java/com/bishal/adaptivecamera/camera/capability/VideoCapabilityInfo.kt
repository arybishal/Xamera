package com.bishal.adaptivecamera.camera.capability

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.media.MediaRecorder
import android.os.Build

data class VideoQualityProfile(
    val qualityName: String,
    val resolution: CameraResolution,
    val isSupported: Boolean = true,
    val maxFps: Double? = null
)

data class VideoCapabilityInfo(
    val supportedQualities: List<VideoQualityProfile> = emptyList(),
    val maxVideoResolution: CameraResolution? = null,
    val supportedFpsRanges: List<FpsRange> = emptyList(),
    val videoStabilizationModes: List<Int> = emptyList(),
    val supportsVideoStabilization: Boolean = false,
    val supportsPreviewStabilization: Boolean = false,
    val status: CapabilityStatus = CapabilityStatus.SUPPORTED,
    val notes: String = ""
)

object VideoCapabilityAnalyzer {

    fun analyze(characteristics: CameraCharacteristics): VideoCapabilityInfo {
        val streamMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val recSizes = streamMap?.getOutputSizes(MediaRecorder::class.java)
            ?: streamMap?.getOutputSizes(SurfaceTexture::class.java)
            ?: emptyArray()

        val availableResolutions = recSizes.map { CameraResolution(it.width, it.height) }.toSet()
        val maxRes = availableResolutions.maxByOrNull { it.width * it.height }

        // Standard defined qualities to check against hardware output sizes
        val standardQualities = listOf(
            Pair("4K UHD", CameraResolution(3840, 2160)),
            Pair("1080p FHD", CameraResolution(1920, 1080)),
            Pair("720p HD", CameraResolution(1280, 720)),
            Pair("480p SD", CameraResolution(720, 480))
        )

        val supportedQualities = standardQualities.mapNotNull { (name, targetRes) ->
            val matching = availableResolutions.firstOrNull { it.width == targetRes.width && it.height == targetRes.height }
            if (matching != null) {
                // Find min frame duration to check max fps
                val minDuration = try {
                    streamMap?.getOutputMinFrameDuration(
                        MediaRecorder::class.java,
                        android.util.Size(targetRes.width, targetRes.height)
                    )
                } catch (_: Exception) { null }

                val maxFps = if (minDuration != null && minDuration > 0) {
                    1_000_000_000.0 / minDuration.toDouble()
                } else null

                VideoQualityProfile(
                    qualityName = name,
                    resolution = matching,
                    isSupported = true,
                    maxFps = maxFps
                )
            } else null
        }

        val fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            ?.map { FpsRange(it.lower, it.upper) } ?: emptyList()

        val stabModes = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)?.toList() ?: emptyList()
        val hasEis = stabModes.contains(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON)

        val hasPreviewStab = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            stabModes.contains(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION)
        } else false

        return VideoCapabilityInfo(
            supportedQualities = supportedQualities,
            maxVideoResolution = maxRes,
            supportedFpsRanges = fpsRanges,
            videoStabilizationModes = stabModes,
            supportsVideoStabilization = hasEis,
            supportsPreviewStabilization = hasPreviewStab,
            status = if (supportedQualities.isNotEmpty()) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED,
            notes = if (hasPreviewStab) "Hardware supports EIS and Preview Stabilization" else if (hasEis) "Hardware supports EIS video stabilization" else "Video stabilization unavailable"
        )
    }
}
