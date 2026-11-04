package com.bishal.adaptivecamera.camera.capability

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.os.Build

data class SensorInfo(
    val activeArraySize: String = "Unavailable",
    val pixelArraySize: String = "Unavailable",
    val physicalSizeMm: String = "Unavailable",
    val sensitivityRange: RangeVal<Int>? = null,
    val exposureTimeRangeNs: RangeVal<Long>? = null,
    val timestampSource: String = "UNKNOWN",
    val whiteLevel: Int? = null,
    val blackLevelPattern: String? = null,
    val colorFilterArrangement: String? = null,
    val sensorOrientation: Int = 90
)

data class LensInfo(
    val facing: Int = -1,
    val facingLabel: String = "UNKNOWN",
    val focalLengths: List<Float> = emptyList(),
    val availableApertures: List<Float> = emptyList(),
    val filterDensities: List<Float> = emptyList(),
    val minimumFocusDistance: Float? = null,
    val hyperfocalDistance: Float? = null,
    val opticalStabilizationModes: List<Int> = emptyList(),
    val opticalStabilizationSupported: Boolean = false,
    val focusDistanceCalibration: String? = null
)

object SensorLensAnalyzer {

    fun analyze(characteristics: CameraCharacteristics): Pair<SensorInfo, LensInfo> {
        // --- SENSOR INFO ---
        val activeArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
        val activeArrayStr = activeArray?.let { "${it.width()}x${it.height()} (${it.left},${it.top})" } ?: "Unavailable"

        val pixelArray = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
        val pixelArrayStr = pixelArray?.let { "${it.width}x${it.height}" } ?: "Unavailable"

        val physicalSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
        val physicalSizeStr = physicalSize?.let { "${String.format("%.2f", it.width)} x ${String.format("%.2f", it.height)} mm" } ?: "Unavailable"

        val isoRangeRaw = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val isoRange = isoRangeRaw?.let { RangeVal(it.lower, it.upper) }

        val expTimeRangeRaw = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val expTimeRange = expTimeRangeRaw?.let { RangeVal(it.lower, it.upper) }

        val timestampSourceRaw = characteristics.get(CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE)
        val timestampSource = when (timestampSourceRaw) {
            CameraMetadata.SENSOR_INFO_TIMESTAMP_SOURCE_REALTIME -> "REALTIME (CLOCK_BOOTTIME)"
            CameraMetadata.SENSOR_INFO_TIMESTAMP_SOURCE_UNKNOWN -> "UNKNOWN (CLOCK_MONOTONIC)"
            else -> "UNKNOWN"
        }

        val whiteLevel = characteristics.get(CameraCharacteristics.SENSOR_INFO_WHITE_LEVEL)

        val blackPatternRaw = characteristics.get(CameraCharacteristics.SENSOR_BLACK_LEVEL_PATTERN)
        val blackPatternStr = if (blackPatternRaw != null) {
            val offsets = IntArray(4)
            blackPatternRaw.copyTo(offsets, 0)
            "[${offsets.joinToString(", ")}]"
        } else null

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

        val orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 90

        val sensorInfo = SensorInfo(
            activeArraySize = activeArrayStr,
            pixelArraySize = pixelArrayStr,
            physicalSizeMm = physicalSizeStr,
            sensitivityRange = isoRange,
            exposureTimeRangeNs = expTimeRange,
            timestampSource = timestampSource,
            whiteLevel = whiteLevel,
            blackLevelPattern = blackPatternStr,
            colorFilterArrangement = cfaStr,
            sensorOrientation = orientation
        )

        // --- LENS INFO ---
        val rawFacing = characteristics.get(CameraCharacteristics.LENS_FACING) ?: -1
        val facingLabel = when (rawFacing) {
            CameraMetadata.LENS_FACING_BACK -> "BACK"
            CameraMetadata.LENS_FACING_FRONT -> "FRONT"
            CameraMetadata.LENS_FACING_EXTERNAL -> "EXTERNAL"
            else -> "UNKNOWN ($rawFacing)"
        }

        val focalLengths = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList() ?: emptyList()
        val apertures = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.toList() ?: emptyList()
        val filterDensities = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FILTER_DENSITIES)?.toList() ?: emptyList()
        val minFocusDist = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE)
        val hyperfocalDist = characteristics.get(CameraCharacteristics.LENS_INFO_HYPERFOCAL_DISTANCE)

        val oisModes = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)?.toList() ?: emptyList()
        val supportsOis = oisModes.any { it != CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_OFF }

        val focusCalibRaw = characteristics.get(CameraCharacteristics.LENS_INFO_FOCUS_DISTANCE_CALIBRATION)
        val focusCalibStr = when (focusCalibRaw) {
            CameraMetadata.LENS_INFO_FOCUS_DISTANCE_CALIBRATION_CALIBRATED -> "CALIBRATED"
            CameraMetadata.LENS_INFO_FOCUS_DISTANCE_CALIBRATION_APPROXIMATE -> "APPROXIMATE"
            CameraMetadata.LENS_INFO_FOCUS_DISTANCE_CALIBRATION_UNCALIBRATED -> "UNCALIBRATED"
            else -> "NONE"
        }

        val lensInfo = LensInfo(
            facing = rawFacing,
            facingLabel = facingLabel,
            focalLengths = focalLengths,
            availableApertures = apertures,
            filterDensities = filterDensities,
            minimumFocusDistance = minFocusDist,
            hyperfocalDistance = hyperfocalDist,
            opticalStabilizationModes = oisModes,
            opticalStabilizationSupported = supportsOis,
            focusDistanceCalibration = focusCalibStr
        )

        return Pair(sensorInfo, lensInfo)
    }
}
