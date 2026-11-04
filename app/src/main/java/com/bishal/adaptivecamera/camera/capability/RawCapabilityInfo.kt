package com.bishal.adaptivecamera.camera.capability

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata

data class RawCapabilityInfo(
    val status: CapabilityStatus,
    val supportsRawSensor: Boolean,
    val supportedRawFormats: List<Int> = emptyList(),
    val maxRawResolution: CameraResolution? = null,
    val colorFilterArrangement: String? = null,
    val whiteLevel: Int? = null,
    val blackLevelPattern: String? = null,
    val notes: String = ""
)

object RawCapabilityAnalyzer {

    fun analyze(characteristics: CameraCharacteristics): RawCapabilityInfo {
        val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.toList() ?: emptyList()
        val hasRawCap = capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW)

        val streamMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val formats = streamMap?.outputFormats?.toList() ?: emptyList()

        val rawFormats = formats.filter {
            it == ImageFormat.RAW_SENSOR || it == ImageFormat.RAW10 || it == ImageFormat.RAW12 || it == ImageFormat.RAW_PRIVATE
        }

        val rawSizes = if (streamMap != null && rawFormats.contains(ImageFormat.RAW_SENSOR)) {
            streamMap.getOutputSizes(ImageFormat.RAW_SENSOR)?.map { CameraResolution(it.width, it.height) } ?: emptyList()
        } else emptyList()

        val maxRawRes = rawSizes.maxByOrNull { it.width * it.height }

        val cfaRaw = characteristics.get(CameraCharacteristics.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT)
        val cfaStr = when (cfaRaw) {
            CameraMetadata.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_RGGB -> "RGGB"
            CameraMetadata.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GRBG -> "GRBG"
            CameraMetadata.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_GBRG -> "GBRG"
            CameraMetadata.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_BGGR -> "BGGR"
            CameraMetadata.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_RGB -> "RGB"
            CameraMetadata.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_MONO -> "MONOCHROME"
            CameraMetadata.SENSOR_INFO_COLOR_FILTER_ARRANGEMENT_NIR -> "NIR"
            else -> if (cfaRaw != null) "CFA_$cfaRaw" else null
        }

        val whiteLevel = characteristics.get(CameraCharacteristics.SENSOR_INFO_WHITE_LEVEL)

        val blackPatternRaw = characteristics.get(CameraCharacteristics.SENSOR_BLACK_LEVEL_PATTERN)
        val blackPatternStr = if (blackPatternRaw != null) {
            val offsets = IntArray(4)
            blackPatternRaw.copyTo(offsets, 0)
            "[${offsets.joinToString(", ")}]"
        } else null

        val isRawSensorSupported = hasRawCap && rawFormats.contains(ImageFormat.RAW_SENSOR)
        val status = if (isRawSensorSupported) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED

        return RawCapabilityInfo(
            status = status,
            supportsRawSensor = isRawSensorSupported,
            supportedRawFormats = rawFormats,
            maxRawResolution = maxRawRes,
            colorFilterArrangement = cfaStr,
            whiteLevel = whiteLevel,
            blackLevelPattern = blackPatternStr,
            notes = if (isRawSensorSupported) "Full 16-bit DNG / RAW_SENSOR capture supported" else "RAW capture unsupported on this camera sensor"
        )
    }
}
