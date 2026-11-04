package com.bishal.adaptivecamera.camera.capability

data class CameraResolution(
    val width: Int,
    val height: Int
) {
    val megapixels: Double
        get() = (width.toLong() * height.toLong()) / 1_000_000.0

    val aspectRatio: String
        get() {
            fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
            val d = gcd(width, height)
            val w = width / d
            val h = height / d
            return if ((w == 16 && h == 9) || (w == 4 && h == 3) || (w == 1 && h == 1)) {
                "$w:$h"
            } else {
                String.format("%.2f:1", width.toDouble() / height)
            }
        }

    override fun toString(): String = "${width}x${height} (${String.format("%.1f", megapixels)}MP)"
}

data class FpsRange(
    val lower: Int,
    val upper: Int
) {
    override fun toString(): String = "$lower-$upper fps"
}

data class RangeVal<T : Comparable<T>>(
    val lower: T,
    val upper: T
) {
    override fun toString(): String = "[$lower, $upper]"
}

data class CameraCapabilityProfile(
    val cameraId: String,
    val facing: Int, // 0 = BACK, 1 = FRONT, 2 = EXTERNAL
    val hardwareLevel: CameraHardwareLevel,
    val isLogicalCamera: Boolean,
    val physicalCameraIds: List<String> = emptyList(),
    val focalLengths: List<Float> = emptyList(),
    val sensorOrientation: Int = 90,
    val sensorActiveArraySize: String = "",
    val supportedFormats: List<Int> = emptyList(), // ImageFormat or PixelFormat
    val supportedPhotoSizes: List<CameraResolution> = emptyList(),
    val supportedVideoSizes: List<CameraResolution> = emptyList(),
    val supportedFpsRanges: List<FpsRange> = emptyList(),
    val supportsAutoFocus: Boolean = false,
    val supportsManualFocus: Boolean = false,
    val minimumFocusDistance: Float? = null, // diopters
    val supportsManualSensor: Boolean = false,
    val isoRange: RangeVal<Int>? = null,
    val exposureTimeRangeNs: RangeVal<Long>? = null,
    val exposureCompensationRange: RangeVal<Int>? = null,
    val exposureCompensationStep: Float? = null,
    val supportsRaw: Boolean = false,
    val rawFormats: List<Int> = emptyList(),
    val supportsFlash: Boolean = false,
    val supportsHdr: Boolean = false,
    val dynamicRangeProfilesSupported: List<String> = emptyList(),
    val opticalStabilizationModes: List<Int> = emptyList(),
    val videoStabilizationModes: List<Int> = emptyList(),
    val noiseReductionModes: List<Int> = emptyList(),
    val maxZoomRatio: Float = 1.0f,
    val minZoomRatio: Float = 1.0f,
    val verificationStatus: CapabilityStatus = CapabilityStatus.NOT_VERIFIED,
    val scannedAtTimestamp: Long = System.currentTimeMillis()
) {
    val facingLabel: String
        get() = when (facing) {
            0 -> "BACK"
            1 -> "FRONT"
            2 -> "EXTERNAL"
            else -> "UNKNOWN ($facing)"
        }

    val maxPhotoResolution: CameraResolution?
        get() = supportedPhotoSizes.maxByOrNull { it.width * it.height }

    val maxVideoResolution: CameraResolution?
        get() = supportedVideoSizes.maxByOrNull { it.width * it.height }

    val exposureTimeRangeDisplay: String
        get() {
            val range = exposureTimeRangeNs ?: return "Unavailable"
            val minSec = range.lower.toDouble() / 1_000_000_000.0
            val maxSec = range.upper.toDouble() / 1_000_000_000.0
            val minStr = if (minSec < 1.0) "1/${(1.0 / minSec).toInt()}s" else "${String.format("%.2f", minSec)}s"
            val maxStr = if (maxSec < 1.0) "1/${(1.0 / maxSec).toInt()}s" else "${String.format("%.2f", maxSec)}s"
            return "$minStr to $maxStr"
        }
}
