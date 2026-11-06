package com.bishal.adaptivecamera.video.capability

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.bishal.adaptivecamera.video.config.VideoCodec
import com.bishal.adaptivecamera.video.config.VideoDynamicRange
import com.bishal.adaptivecamera.video.config.VideoFrameRate
import com.bishal.adaptivecamera.video.config.VideoResolution
import com.bishal.adaptivecamera.video.config.VideoStabilizationMode

/**
 * Audit result of hardware-supported video capabilities for a specific camera sensor.
 */
data class VideoSensorCapabilities(
    val supportedResolutions: List<VideoResolution>,
    val supportedFrameRates: Map<VideoResolution, List<VideoFrameRate>>,
    val supportedCodecs: List<VideoCodec>,
    val supportedStabilizationModes: List<VideoStabilizationMode>,
    val supportedDynamicRanges: List<VideoDynamicRange>,
    val maxBitrateBps: Long,
    val hasHardwareOis: Boolean,
    val hasSoftwareEis: Boolean,
    val hasPreviewStabilization: Boolean
) {
    fun isCombinationSupported(resolution: VideoResolution, frameRate: VideoFrameRate): Boolean {
        val availableFps = supportedFrameRates[resolution] ?: emptyList()
        return supportedResolutions.contains(resolution) && availableFps.contains(frameRate)
    }
}

/**
 * Inspects Camera2 CameraCharacteristics and system MediaCodec encoders to detect
 * valid video recording configurations without guessing or hardcoding.
 */
object VideoCapabilityDetector {
    private const val TAG = "VideoCapDetector"

    fun detect(characteristics: CameraCharacteristics): VideoSensorCapabilities {
        // 1. Detect Supported Resolutions via StreamConfigurationMap
        val streamMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val recorderSizes = streamMap?.getOutputSizes(MediaRecorder::class.java) ?: emptyArray()

        val supportedResolutions = VideoResolution.entries.filter { res ->
            recorderSizes.any { size -> size.width == res.width && size.height == res.height }
        }.ifEmpty {
            listOf(VideoResolution.FHD_1080P, VideoResolution.HD_720P)
        }

        // 2. Detect Supported FPS per resolution
        val fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            ?: emptyArray()
        val maxTargetFps = fpsRanges.maxOfOrNull { it.upper } ?: 30

        val supportedFrameRates = supportedResolutions.associateWith { res ->
            val minFrameDurationNanos = try {
                streamMap?.getOutputMinFrameDuration(
                    MediaRecorder::class.java,
                    android.util.Size(res.width, res.height)
                ) ?: 0L
            } catch (_: Exception) {
                0L
            }

            val maxFpsForRes = if (minFrameDurationNanos > 0L) {
                (1_000_000_000.0 / minFrameDurationNanos.toDouble()).toInt()
            } else {
                maxTargetFps
            }

            VideoFrameRate.entries.filter { rate ->
                rate.fps <= maxFpsForRes && rate.fps <= maxTargetFps
            }.ifEmpty {
                listOf(VideoFrameRate.FPS_30)
            }
        }

        // 3. Detect Supported Codecs via MediaCodecList
        val supportedCodecs = detectSupportedCodecs()

        // 4. Detect Stabilization (OIS, EIS, Combined)
        val oisModes = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
            ?: intArrayOf()
        val hasOis = oisModes.contains(CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON)

        val eisModes = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
            ?: intArrayOf()
        val hasEis = eisModes.contains(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON)

        val hasPreviewStab = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            eisModes.contains(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION)
        } else false

        val stabModes = mutableListOf(VideoStabilizationMode.OFF)
        if (hasOis) stabModes.add(VideoStabilizationMode.OIS)
        if (hasEis) stabModes.add(VideoStabilizationMode.EIS)
        if (hasOis && hasEis) stabModes.add(VideoStabilizationMode.COMBINED)

        // 5. Detect Dynamic Range Profiles (10-bit HDR / HLG)
        val supportedDynamicRanges = mutableListOf(VideoDynamicRange.SDR)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val profiles = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_DYNAMIC_RANGE_PROFILES)
            if (profiles != null) {
                val supportedProfiles = profiles.supportedProfiles
                if (supportedProfiles.contains(android.hardware.camera2.params.DynamicRangeProfiles.HLG10)) {
                    supportedDynamicRanges.add(VideoDynamicRange.HLG10)
                }
                if (supportedProfiles.contains(android.hardware.camera2.params.DynamicRangeProfiles.HDR10)) {
                    supportedDynamicRanges.add(VideoDynamicRange.HDR10)
                }
            }
        }

        // 6. Max Bitrate estimation from encoder capabilities
        val maxBitrate = detectMaxEncoderBitrate()

        return VideoSensorCapabilities(
            supportedResolutions = supportedResolutions,
            supportedFrameRates = supportedFrameRates,
            supportedCodecs = supportedCodecs,
            supportedStabilizationModes = stabModes,
            supportedDynamicRanges = supportedDynamicRanges,
            maxBitrateBps = maxBitrate,
            hasHardwareOis = hasOis,
            hasSoftwareEis = hasEis,
            hasPreviewStabilization = hasPreviewStab
        )
    }

    private fun detectSupportedCodecs(): List<VideoCodec> {
        val codecs = mutableListOf<VideoCodec>()
        try {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val encoderInfos = codecList.codecInfos.filter { it.isEncoder }

            val hasAvc = encoderInfos.any { info ->
                info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true) }
            }
            if (hasAvc) codecs.add(VideoCodec.H264_AVC)

            val hasHevc = encoderInfos.any { info ->
                info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_HEVC, ignoreCase = true) }
            }
            if (hasHevc) codecs.add(VideoCodec.H265_HEVC)

            val hasAv1 = encoderInfos.any { info ->
                info.supportedTypes.any { it.equals("video/av01", ignoreCase = true) }
            }
            if (hasAv1) codecs.add(VideoCodec.AV1)
        } catch (e: Exception) {
            Log.w(TAG, "Error discovering encoders via MediaCodecList, defaulting to AVC/HEVC", e)
            codecs.add(VideoCodec.H264_AVC)
            codecs.add(VideoCodec.H265_HEVC)
        }

        return codecs.ifEmpty { listOf(VideoCodec.H264_AVC) }
    }

    private fun detectMaxEncoderBitrate(): Long {
        return try {
            val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val hevcEncoder = codecList.codecInfos
                .filter { it.isEncoder }
                .firstOrNull { info ->
                    info.supportedTypes.any { it.equals(MediaFormat.MIMETYPE_VIDEO_HEVC, ignoreCase = true) }
                }

            val cap = hevcEncoder?.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_HEVC)
            cap?.videoCapabilities?.bitrateRange?.upper?.toLong() ?: 120_000_000L
        } catch (_: Exception) {
            100_000_000L
        }
    }
}
