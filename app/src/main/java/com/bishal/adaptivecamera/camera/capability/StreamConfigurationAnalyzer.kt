package com.bishal.adaptivecamera.camera.capability

import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Log

object StreamConfigurationAnalyzer {

    private const val TAG = "StreamConfigAnalyzer"

    fun analyze(
        characteristics: CameraCharacteristics,
        hardwareLevel: CameraHardwareLevel,
        isLogical: Boolean
    ): Pair<List<FormatStreamConfiguration>, List<StreamCombinationCapability>> {
        val streamMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        if (streamMap == null) {
            return Pair(emptyList(), emptyList())
        }

        val formatConfigurations = mutableListOf<FormatStreamConfiguration>()
        val outputFormats = streamMap.outputFormats ?: intArrayOf()

        for (format in outputFormats) {
            val formatName = getFormatName(format)
            val isRaw = isRawFormat(format)

            val normalSizes = try {
                streamMap.getOutputSizes(format)?.map { size ->
                    val minDuration = try {
                        streamMap.getOutputMinFrameDuration(format, size)
                    } catch (_: Exception) { null }

                    val stallDuration = try {
                        streamMap.getOutputStallDuration(format, size)
                    } catch (_: Exception) { null }

                    val maxFps = if (minDuration != null && minDuration > 0) {
                        1_000_000_000.0 / minDuration.toDouble()
                    } else null

                    StreamSizeInfo(
                        width = size.width,
                        height = size.height,
                        minFrameDurationNs = minDuration,
                        stallDurationNs = stallDuration,
                        maxFps = maxFps
                    )
                }?.sortedByDescending { it.width * it.height } ?: emptyList()
            } catch (e: Exception) {
                Log.w(TAG, "Failed reading sizes for format $format", e)
                emptyList()
            }

            val highResSizes = try {
                streamMap.getHighResolutionOutputSizes(format)?.map { size ->
                    val minDuration = try {
                        streamMap.getOutputMinFrameDuration(format, size)
                    } catch (_: Exception) { null }

                    val stallDuration = try {
                        streamMap.getOutputStallDuration(format, size)
                    } catch (_: Exception) { null }

                    val maxFps = if (minDuration != null && minDuration > 0) {
                        1_000_000_000.0 / minDuration.toDouble()
                    } else null

                    StreamSizeInfo(
                        width = size.width,
                        height = size.height,
                        minFrameDurationNs = minDuration,
                        stallDurationNs = stallDuration,
                        maxFps = maxFps
                    )
                }?.sortedByDescending { it.width * it.height } ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }

            formatConfigurations.add(
                FormatStreamConfiguration(
                    format = format,
                    formatName = formatName,
                    isRaw = isRaw,
                    sizes = normalSizes,
                    highResolutionSizes = highResSizes
                )
            )
        }

        // Sort: JPEG first, then PRIVATE, YUV, RAW, HEIC, others
        formatConfigurations.sortBy { formatPriority(it.format) }

        // Analyze mandatory & advanced stream combinations
        val combinations = evaluateStreamCombinations(characteristics, hardwareLevel, isLogical, outputFormats)

        return Pair(formatConfigurations, combinations)
    }

    private fun evaluateStreamCombinations(
        characteristics: CameraCharacteristics,
        hardwareLevel: CameraHardwareLevel,
        isLogical: Boolean,
        availableFormats: IntArray
    ): List<StreamCombinationCapability> {
        val combinations = mutableListOf<StreamCombinationCapability>()
        val hasJpeg = availableFormats.contains(ImageFormat.JPEG)
        val hasYuv = availableFormats.contains(ImageFormat.YUV_420_888)
        val hasPrivate = availableFormats.contains(ImageFormat.PRIVATE)
        val hasRaw = availableFormats.any { isRawFormat(it) }

        // 1. Preview + JPEG Photo Capture
        combinations.add(
            StreamCombinationCapability(
                name = "Preview + Photo Capture",
                description = "PRIV (Preview) + JPEG (Maximum Still)",
                status = if (hasJpeg && hasPrivate) CapabilityStatus.CONFIGURATION_VALIDATED else CapabilityStatus.UNSUPPORTED,
                guaranteedByHardwareLevel = true,
                reason = "Guaranteed on all Android camera devices"
            )
        )

        // 2. Preview + Video Recording
        val supportsPrivPriv = hardwareLevel == CameraHardwareLevel.LIMITED ||
                hardwareLevel == CameraHardwareLevel.FULL ||
                hardwareLevel == CameraHardwareLevel.LEVEL_3
        combinations.add(
            StreamCombinationCapability(
                name = "Preview + Video Recording",
                description = "PRIV (Preview) + PRIV (Record / MediaCodec)",
                status = if (supportsPrivPriv && hasPrivate) CapabilityStatus.CONFIGURATION_VALIDATED else CapabilityStatus.UNSUPPORTED,
                guaranteedByHardwareLevel = supportsPrivPriv,
                reason = if (supportsPrivPriv) "Guaranteed for LIMITED / FULL / LEVEL_3 devices" else "Legacy devices may not support concurrent private streams"
            )
        )

        // 3. Preview + Video + Video Snapshot
        combinations.add(
            StreamCombinationCapability(
                name = "Preview + Video + Snapshot",
                description = "PRIV (Preview) + PRIV (Record) + JPEG (Still Snapshot)",
                status = if (supportsPrivPriv && hasJpeg) CapabilityStatus.CONFIGURATION_VALIDATED else CapabilityStatus.UNSUPPORTED,
                guaranteedByHardwareLevel = supportsPrivPriv,
                reason = "Concurrent viewfinder, encoder recording, and still capture"
            )
        )

        // 4. Preview + YUV Analysis (Vision / ML)
        combinations.add(
            StreamCombinationCapability(
                name = "Preview + Analysis Stream",
                description = "PRIV (Preview) + YUV_420_888 (Real-time Frame Analysis)",
                status = if (hasPrivate && hasYuv) CapabilityStatus.CONFIGURATION_VALIDATED else CapabilityStatus.UNSUPPORTED,
                guaranteedByHardwareLevel = supportsPrivPriv,
                reason = "Used for scene analysis, QR, histogram and ML pipelines"
            )
        )

        // 5. RAW Capture + Preview
        val supportsRawStream = hasRaw && (hardwareLevel == CameraHardwareLevel.FULL || hardwareLevel == CameraHardwareLevel.LEVEL_3)
        combinations.add(
            StreamCombinationCapability(
                name = "RAW Capture + Viewfinder",
                description = "RAW_SENSOR (Maximum) + PRIV (Preview)",
                status = if (supportsRawStream) CapabilityStatus.CONFIGURATION_VALIDATED else CapabilityStatus.UNSUPPORTED,
                guaranteedByHardwareLevel = supportsRawStream,
                reason = if (supportsRawStream) "Hardware supports full sensor Bayer output concurrent with preview" else "Device lacks hardware RAW sensor stream capability"
            )
        )

        // 6. Logical Multi-Camera Physical Streams
        if (isLogical) {
            val isLevel3 = hardwareLevel == CameraHardwareLevel.LEVEL_3
            combinations.add(
                StreamCombinationCapability(
                    name = "Logical Multi-Camera Stream Synchronization",
                    description = "Logical preview with physical sub-camera stream targeting",
                    status = if (isLevel3) CapabilityStatus.CONFIGURATION_VALIDATED else CapabilityStatus.SUPPORTED,
                    guaranteedByHardwareLevel = isLevel3,
                    reason = "Allows switching or streaming individual physical sub-lenses"
                )
            )
        }

        return combinations
    }

    private fun isRawFormat(format: Int): Boolean {
        return format == ImageFormat.RAW_SENSOR ||
                format == ImageFormat.RAW10 ||
                format == ImageFormat.RAW12 ||
                format == ImageFormat.RAW_PRIVATE
    }

    private fun formatPriority(format: Int): Int {
        return when (format) {
            ImageFormat.JPEG -> 1
            ImageFormat.PRIVATE -> 2
            ImageFormat.YUV_420_888 -> 3
            ImageFormat.RAW_SENSOR -> 4
            ImageFormat.RAW10 -> 5
            ImageFormat.RAW12 -> 6
            ImageFormat.HEIC -> 7
            ImageFormat.DEPTH16 -> 8
            else -> 10
        }
    }

    fun getFormatName(format: Int): String {
        return when (format) {
            ImageFormat.JPEG -> "JPEG"
            ImageFormat.YUV_420_888 -> "YUV_420_888"
            ImageFormat.PRIVATE -> "PRIVATE"
            ImageFormat.RAW_SENSOR -> "RAW_SENSOR"
            ImageFormat.RAW10 -> "RAW10"
            ImageFormat.RAW12 -> "RAW12"
            ImageFormat.RAW_PRIVATE -> "RAW_PRIVATE"
            ImageFormat.DEPTH16 -> "DEPTH16"
            ImageFormat.DEPTH_JPEG -> "DEPTH_JPEG"
            ImageFormat.DEPTH_POINT_CLOUD -> "DEPTH_POINT_CLOUD"
            ImageFormat.HEIC -> "HEIC"
            PixelFormat.RGBA_8888 -> "RGBA_8888"
            PixelFormat.RGBX_8888 -> "RGBX_8888"
            PixelFormat.RGB_888 -> "RGB_888"
            PixelFormat.RGB_565 -> "RGB_565"
            else -> "FORMAT_$format"
        }
    }
}
